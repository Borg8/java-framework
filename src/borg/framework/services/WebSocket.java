package borg.framework.services;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import borg.framework.auxiliaries.Auxiliary;
import borg.framework.auxiliaries.Logging;
import borg.framework.auxiliaries.NetworkTools;
import borg.framework.structures.HttpRequest;
import borg.framework.structures.HttpResponse;
import borg.framework.structures.NetworkResult;

public class WebSocket
{
	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Public Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	/** protocol version **/
	public static final String VERSION_PROTOCOL = "ocpp1.6";

	/** websocket version **/
	static final int VERSION_WEBSOCKET = 13;

	/** protocol version **/
	static final String AGENT_WEBSOCKET = "borg_socket/1.0";

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	private static final String HEADER_HOST = "host";

	private static final String HEADER_CONNECTION = "connection";

	private static final String HEADER_PROTOCOL = "sec-websocket-protocol";

	private static final String HEADER_KEY = "sec-websocket-key";

	private static final String HEADER_VERSION = "sec-websocket-version";

	private static final String HEADER_UPGRADE = "upgrade";

	private static final String HEADER_AGENT = "user-agent";

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Definitions
	//////////////////////////////////////////////////////////////////////////////////////////////////

	public enum Opcode
	{
		/** 0: continuation **/
		CONTINUATION,

		/** 1: text **/
		TEXT,

		/** 2: binary **/
		BINARY,

		/** 3: reserved **/
		RESERVED3,

		/** 4: reserved **/
		RESERVED4,

		/** 5: reserved **/
		RESERVED5,

		/** 6: reserved **/
		RESERVED6,

		/** 7: reserved **/
		RESERVED7,

		/** 8: close **/
		CLOSE,

		/** 9: ping **/
		PING,

		/** 10: pong **/
		PONG
	}

	public interface Listener
	{
		/**
		 * data received.
		 *
		 * @param data_ received data.
		 */
		void dataReceived(@NotNull byte[] data_);

		/**
		 * socket disconnected.
		 */
		void disconnected();
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Fields
	//////////////////////////////////////////////////////////////////////////////////////////////////

	/** websocket URL **/
	public final URL url;

	/** socket events listener **/
	private final Listener mListener;

	/** communication socket **/
	private Socket mSocket;

	/** current socket key **/
	private String mKey;

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Methods
	//////////////////////////////////////////////////////////////////////////////////////////////////

	@Contract(pure = true)
	public WebSocket(@NotNull String url_, @NotNull Listener listener_)
	{
		this(createUrl(url_), listener_);
	}

	@Contract(pure = true)
	public WebSocket(@NotNull URL url_, @NotNull Listener listener_)
	{
		url = url_;
		mListener = listener_;

		mSocket = null;
	}

	/**
	 * @return {@code true} if the websocket is connected.
	 */
	@Contract(pure = true)
	public boolean isConnected()
	{
		return mSocket != null;
	}

	/**
	 * connect websocket. Blocking operation.
	 *
	 * @param timeout_ connection timeout, 0 for infinite.
	 *
	 * @return operation response.
	 */
	@NotNull
	public synchronized HttpResponse connect(long timeout_)
	{
		// set default parameters
		int code = -1;
		NetworkResult result;
		Map<String, String> headers = null;

		// if not connected
		if (isConnected() == false)
		{
			// create connection
			try
			{
				// create connection
				mSocket = new Socket();
				mSocket.connect(new InetSocketAddress(url.getHost(), url.getPort()), (int)timeout_);

				// generate key
				mKey = generateKey();

				// build request
				Map<String, String> requestHeaders = new HashMap<>();
				requestHeaders.put(HEADER_HOST, url.getHost());
				requestHeaders.put(HEADER_CONNECTION, "Upgrade");
				requestHeaders.put(HEADER_PROTOCOL, VERSION_PROTOCOL);
				requestHeaders.put(HEADER_VERSION, Integer.toString(VERSION_WEBSOCKET));
				requestHeaders.put(HEADER_KEY, mKey);
				requestHeaders.put(HEADER_UPGRADE, "websocket");
				requestHeaders.put(HEADER_AGENT, AGENT_WEBSOCKET);
				HttpRequest request = new HttpRequest("GET", url.getPath(), requestHeaders, null);

				// write request
				OutputStream output = mSocket.getOutputStream();
				output.write(request.serialize());

				// read response
				InputStream input = mSocket.getInputStream();
				HttpResponse response = HttpResponse.readResponse(input, NetworkTools.TIMEOUT_READ);
				code = response.code;
				headers = response.headers;

				// if code parsed successfully
				if (code > 0)
				{
					// if succeeded
					if (code < 300)
					{
						// start listening
						TasksManager.runOnThread(socketTask);

						result = NetworkResult.SUCCESS;
					}
					else
					{
						Logging.logging(Level.WARNING, "unexpected response: " + code);
						result = NetworkResult.UNEXPECTED_RESPONSE;
						disconnect();
					}
				}
				else
				{
					Logging.logging(Level.WARNING, "unable to parse code");
					result = NetworkResult.UNEXPECTED_RESPONSE;
					disconnect();
				}
			}
			catch (Exception e)
			{
				Logging.logging(Level.WARNING, e);
				result = NetworkResult.UNABLE_TO_CONNECT;
			}
		}
		else
		{
			Logging.logging(Level.WARNING, "already connected");
			result = NetworkResult.BUSY;
		}

		return new HttpResponse(result, code, headers, null);
	}

	/**
	 * disconnect web socket.
	 */
	public synchronized void disconnect()
	{
		// if connected
		if (isConnected() == true)
		{
			try
			{
				// close socket
				Socket socket = mSocket;
				mSocket = null;
				socket.close();
			}
			catch (IOException e)
			{
				Logging.logging(e);
			}
		}
	}

	/**
	 * write data to socket. Blocking operation.
	 *
	 * @param data_    data to write.
	 * @param encrypt_ if {@code true} then the data will be encrypted.
	 *
	 * @return operation result.
	 */
	@NotNull
	public synchronized NetworkResult write(@NotNull byte[] data_, boolean encrypt_)
	{
		try
		{
			// get output stream
			OutputStream output = mSocket.getOutputStream();

			// if data should be encrypted
			if (encrypt_ == true)
			{
				// build frame
				data_ = buildFrame(true, false, false, false, Opcode.TEXT, true, data_);
			}

			// write data
			output.write(data_);
			output.flush();

			return NetworkResult.SUCCESS;
		}
		catch (IOException e)
		{
			Logging.logging(e);
			return NetworkResult.UNABLE_TO_SEND;
		}
	}

	@NotNull
	@Contract(pure = true)
	private static String generateKey()
	{
		// TODO
		return "12345";
	}

	@NotNull
	@Contract(pure = true)
	private static byte[] buildFrame(boolean fin_,
		boolean rsv1_,
		boolean rsv2_,
		boolean rsv3_,
		@NotNull Opcode opcode_,
		boolean mask_,
		@NotNull byte[] payload_)
	{
		List<Byte> buffer = new ArrayList<>();

		// add fin
		int b = (fin_? 1: 0) << 7;

		// add rsv1
		b |= (rsv1_? 1: 0) << 6;

		// add rsv2
		b |= (rsv2_? 1: 0) << 5;

		// add rsv3
		b |= (rsv3_? 1: 0) << 4;

		// add rsv4
		b |= opcode_.ordinal();

		// add byte
		buffer.add((byte)b);

		// add key mask
		b = (mask_? 1: 0) << 7;

		// if 8 bits length
		if (payload_.length <= 0x7d)
		{
			// add length
			b |= payload_.length;
			buffer.add((byte)b);
		}
		else
		{
			// if 16 bits length
			if (payload_.length <= 0xffff)
			{
				// add length
				b |= 0x7e;
				buffer.add((byte)b);
				buffer.add((byte)((payload_.length >>> 8) & 0xff));
				buffer.add((byte)(payload_.length & 0xff));
			}
			else
			{
				// add length
				b |= 0x7f;
				buffer.add((byte)b);
				long l = payload_.length;
				buffer.add((byte)((l >>> 56) & 0xff));
				buffer.add((byte)((l >>> 48) & 0xff));
				buffer.add((byte)((l >>> 40) & 0xff));
				buffer.add((byte)((l >>> 32) & 0xff));
				buffer.add((byte)((l >>> 24) & 0xff));
				buffer.add((byte)((l >>> 16) & 0xff));
				buffer.add((byte)((l >>> 8) & 0xff));
				buffer.add((byte)(l & 0xff));
			}
		}

		// add key
		byte[] key =
			{
				(byte)Auxiliary.random(),
				(byte)Auxiliary.random(),
				(byte)Auxiliary.random(),
				(byte)Auxiliary.random()
			};
		for (byte value : key)
		{
			buffer.add(value);
		}

		// add encrypted data
		int j = 0;
		for (byte value : payload_)
		{
			buffer.add((byte)(value ^ key[j]));
			j = j == 3? 0: j + 1;
		}

		return ArraysManager.bytesFromList(buffer);
	}

	private final TasksManager.Task<Void> socketTask = new TasksManager.Task<>()
	{
		@Override
		public void run(Void param_)
		{
			Thread.currentThread().setName("websocket to " + url);

			for (; ; )
			{
				try
				{
					// get input stream
					InputStream input = mSocket.getInputStream();

					// read data
					int b = input.read();
					if (b < 0)
					{
						break;
					}
					byte[] bytes = NetworkTools.readBytes(input, NetworkTools.TIMEOUT_READ);
					byte[] data;
					if (bytes == null)
					{
						data = new byte[] { (byte)b };
					}
					else
					{
						data = new byte[bytes.length + 1];
						data[0] = (byte)b;
						System.arraycopy(bytes, 0, data, 1, bytes.length);
					}

					// invoke observers
					mListener.dataReceived(data);
				}
				catch (IOException e)
				{
					// if the socket was not disconnected
					if (mSocket != null)
					{
						Logging.logging(e);
					}

					break;
				}
			}

			// disconnect
			disconnect();
			mListener.disconnected();
		}
	};

	@NotNull
	@Contract(pure = true)
	private static URL createUrl(String url_)
	{
		try
		{
			return new URL(url_);
		}
		catch (MalformedURLException e)
		{
			throw new Error(e);
		}
	}
}
