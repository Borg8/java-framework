package borg.framework.services;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import borg.framework.auxiliaries.Auxiliary;
import borg.framework.auxiliaries.Logging;
import borg.framework.resources.HttpResponse;
import borg.framework.resources.NetworkResult;

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

	/** current connection **/
	private HttpURLConnection mConnection;

	/** current socket key **/
	private String mKey;

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Methods
	//////////////////////////////////////////////////////////////////////////////////////////////////

	@Contract(pure = true)
	public WebSocket(@NotNull String url_, @NotNull Listener listener_)
	{
		try
		{
			url = new URL(url_);
		}
		catch (MalformedURLException e)
		{
			throw new Error(e);
		}

		mListener = listener_;

		mConnection = null;

		// allow restricted headers
		System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
	}

	/**
	 * @return {@code true} if the websocket is connected.
	 */
	@Contract(pure = true)
	public boolean isConnected()
	{
		return mConnection != null;
	}

	/**
	 * connect websocket. Blocking operation.
	 *
	 * @param timeout_ connection timeout.
	 *
	 * @return operation response.
	 */
	@NotNull
	public HttpResponse connect(long timeout_)
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
				mConnection = (HttpURLConnection)url.openConnection();
				mConnection.setDoInput(true);
				mConnection.setDoOutput(true);
				mConnection.setConnectTimeout((int)timeout_);

				// generate key
				mKey = generateKey();

				// set headers
				mConnection.setRequestProperty(HEADER_HOST, url.getHost());
				mConnection.setRequestProperty(HEADER_CONNECTION, "Upgrade");
				mConnection.setRequestProperty(HEADER_PROTOCOL, VERSION_PROTOCOL);
				mConnection.setRequestProperty(HEADER_VERSION, Integer.toString(VERSION_WEBSOCKET));
				mConnection.setRequestProperty(HEADER_KEY, mKey);
				mConnection.setRequestProperty(HEADER_UPGRADE, "websocket");
				mConnection.setRequestProperty(HEADER_AGENT, AGENT_WEBSOCKET);

				// connect
				mConnection.setRequestMethod("GET");
				mConnection.connect();

				// if connection succeeded
				code = mConnection.getResponseCode();
				if (code < 300)
				{
					// read headers
					Map<String, List<String>> respHeaders = mConnection.getHeaderFields();
					if (respHeaders != null)
					{
						headers = new HashMap<>();
						for (Map.Entry<String, List<String>> entry : respHeaders.entrySet())
						{
							String key = entry.getKey();
							if (key != null)
							{
								key = key.toLowerCase();
							}
							headers.put(key, entry.getValue().get(0));
						}
					}

					// start listening
					TasksManager.runThread(socketTask);

					result = NetworkResult.SUCCESS;
				}
				else
				{
					byte[] error = Auxiliary.readFromStream(mConnection.getErrorStream());
					String message = "";
					if (error != null)
					{
						message = new String(error);
					}
					Logging.logging(Level.WARNING, "unable to connect: " + code + " - " + message);
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

		return new HttpResponse(result, url.toString(), null, headers, code);
	}

	/**
	 * disconnect web socket.
	 */
	public void disconnect()
	{
		// if connected
		if (isConnected() == true)
		{
			mConnection.disconnect();
			mConnection = null;
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
	public NetworkResult write(@NotNull byte[] data_, boolean encrypt_)
	{
		try
		{
			// get output stream
			OutputStream output = mConnection.getOutputStream();

			// if data should be encrypted
			if (encrypt_ == true)
			{
				// build frame
				data_ = buildFrame(true, false, false, false, Opcode.TEXT, true, data_);
			}

			// write data
			output.write(data_);

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
		byte b = (byte)((fin_? 1: 0) << 7);

		// add rsv1
		b |= (rsv1_? 1: 0) << 6;

		// add rsv2
		b |= (rsv2_? 1: 0) << 5;

		// add rsv3
		b |= (rsv3_? 1: 0) << 4;

		// add rsv4
		b |= opcode_.ordinal();

		// add byte
		buffer.add(b);

		// add key mask
		b = (byte)((mask_? 1: 0) << 7);

		// if 8 bits length
		if (payload_.length <= 0x7d)
		{
			// add length
			b |= payload_.length;
			buffer.add(b);
		}
		else
		{
			// if 16 bits length
			if (payload_.length <= 0xffff)
			{
				// add length
				b |= 0x7e;
				buffer.add(b);
				buffer.add((byte)(payload_.length & 0xff));
				buffer.add((byte)((payload_.length >> 8) & 0xff));
			}
			else
			{
				// add length
				b |= 0x7f;
				buffer.add(b);
				long l = payload_.length;
				for (int i = 0; i < 8; ++i)
				{
					buffer.add((byte)(l & 0xff));
					l >>= 8;
				}
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
			for (; ; )
			{
				try
				{
					// get input stream
					InputStream input = mConnection.getInputStream();

					// read data
					byte[] data = Auxiliary.readFromStream(input);

					// invoke observers
					if (data != null)
					{
						mListener.dataReceived(data);
					}
					else
					{
						break;
					}
				}
				catch (IOException e)
				{
					Logging.logging(e);
					break;
				}
			}

			// disconnect
			disconnect();
			mListener.disconnected();
		}
	};
}
