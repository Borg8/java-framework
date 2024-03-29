package borg.framework.services;

import borg.framework.auxiliaries.Auxiliary;
import borg.framework.auxiliaries.Logger;
import borg.framework.auxiliaries.NetworkTools;
import borg.framework.collections.ByteArray;
import borg.framework.structures.HttpRequest;
import borg.framework.structures.HttpResponse;
import borg.framework.structures.NetworkResult;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.net.ssl.SSLSocketFactory;

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

	private static final int LENGTH_KEY = 16;

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
		 * @param this_ websocket.
		 * @param data_ received data.
		 */
		void dataReceived(@NotNull WebSocket this_, byte @NotNull [] data_);

		/**
		 * socket closing message.
		 *
		 * @param this_    websocket.
		 * @param message_ close message.
		 */
		@SuppressWarnings("unused")
		default void close(@NotNull WebSocket this_, byte @NotNull [] message_)
		{
		}

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

	/** keepalive interval **/
	private long mKeepalive;

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Methods
	//////////////////////////////////////////////////////////////////////////////////////////////////

	public WebSocket(@NotNull String url_, @NotNull Listener listener_)
	{
		this(createUrl(url_), listener_);
	}

	public WebSocket(@NotNull URL url_, @NotNull Listener listener_)
	{
		url = url_;
		mListener = listener_;

		mSocket = null;
		mKeepalive = -1;
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
	 * @param headers_ headers to send with the connection.
	 *
	 * @return operation response.
	 */
	@NotNull
	public synchronized HttpResponse connect(long timeout_, @Nullable Map<String, String> headers_)
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
				if (url.getProtocol().equals("https"))
				{
					mSocket = SSLSocketFactory.getDefault().createSocket();
				}
				else
				{
					mSocket = new Socket();
				}
				mSocket.setSoTimeout(NetworkTools.TIMEOUT_CONNECT);

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
				if (headers_ != null)
				{
					requestHeaders.putAll(headers_);
				}

				HttpRequest request = new HttpRequest("GET", new URI(url.getPath()), requestHeaders, null);

				// write request
				OutputStream output = mSocket.getOutputStream();
				output.write(request.serialize());

				// read response
				mSocket.setSoTimeout(NetworkTools.TIMEOUT_READ);
				HttpResponse response = HttpResponse.readResponse(mSocket.getInputStream());
				code = response.code;
				headers = response.headers;

				// if code parsed successfully
				if (code > 0)
				{
					// if succeeded
					if (code < 300)
					{
						// start listening
						_getSocketTask().start();

						// start keepalive
						setKeepalive(mKeepalive);

						result = NetworkResult.SUCCESS;
					}
					else
					{
						Logger.log(Level.WARNING, "unexpected response: " + code);
						result = NetworkResult.UNEXPECTED_RESPONSE;
						disconnect();
					}
				}
				else
				{
					Logger.log(Level.WARNING, "unable to parse code");
					result = NetworkResult.UNEXPECTED_RESPONSE;
					disconnect();
				}
			}
			catch (Exception e)
			{
				Logger.log(Level.WARNING, e);
				result = NetworkResult.UNABLE_TO_CONNECT;
				disconnect();
			}
		}
		else
		{
			Logger.log(Level.WARNING, "already connected");
			result = NetworkResult.BUSY;
			disconnect();
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

				// disable watchdog
				TimeManager.cancel(_keepaliveWatchdog);
			}
			catch (Exception e)
			{
				Logger.log(e);
			}
		}
	}

	/**
	 * set websocket keepalive.
	 *
	 * @param interval_ keepalive interval, -1 to disable.
	 */
	public void setKeepalive(long interval_)
	{
		mKeepalive = interval_;

		// if connected
		if ((isConnected() == true) && (mKeepalive > 0))
		{
			// start watchdog
			TimeManager.asyncExecute(0, _keepaliveWatchdog);
		}
	}

	/**
	 * write data to socket. Blocking operation.
	 *
	 * @param message_ data to write.
	 * @param encrypt_ if {@code true} then the data will be encrypted.
	 *
	 * @return operation result.
	 */
	@NotNull
	public NetworkResult write(@NotNull String message_, boolean encrypt_)
	{
		return write(message_.getBytes(), encrypt_? Opcode.TEXT: null);
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
	public NetworkResult write(byte @NotNull [] data_, boolean encrypt_)
	{
		return write(data_, encrypt_? Opcode.BINARY: null);
	}

	@NotNull
	private synchronized NetworkResult write(byte @NotNull [] data_, @Nullable Opcode opcode_)
	{
		try
		{
			// get output stream
			OutputStream output = mSocket.getOutputStream();

			// if data should be encrypted
			if (opcode_ != null)
			{
				// build frame
				data_ = _buildFrame(true, false, false, false, opcode_, true, data_);
			}

			// write data
			output.write(data_);
			output.flush();

			return NetworkResult.SUCCESS;
		}
		catch (Exception e)
		{
			Logger.log(e);
			return NetworkResult.UNABLE_TO_SEND;
		}
	}

	@NotNull
	@Contract(pure = true)
	private static String generateKey()
	{
		// generate key
		byte[] key = new byte[LENGTH_KEY];
		//noinspection ExplicitArrayFilling
		for (int i = 0; i < LENGTH_KEY; ++i)
		{
			key[i] = (byte)Auxiliary.random();
		}
		return Base64.getEncoder().encodeToString(key);
	}

	@SuppressWarnings("ConstantConditions")
	@Contract(pure = true)
	private static byte @NotNull [] _buildFrame(boolean fin_,
		boolean rsv1_,
		boolean rsv2_,
		boolean rsv3_,
		@NotNull Opcode opcode_,
		boolean mask_,
		byte @NotNull [] payload_)
	{
		ByteArray buffer = new ByteArray();

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
		buffer.push((byte)b);

		// add key mask
		b = (mask_? 1: 0) << 7;

		// if 8 bits length
		if (payload_.length <= 0x7d)
		{
			// add length
			b |= payload_.length;
			buffer.push((byte)b);
		}
		else
		{
			// if 16 bits length
			if (payload_.length <= 0xffff)
			{
				// add length
				b |= 0x7e;
				buffer.push((byte)b);
				buffer.push((byte)((payload_.length >>> 8) & 0xff));
				buffer.push((byte)(payload_.length & 0xff));
			}
			else
			{
				// add length
				b |= 0x7f;
				buffer.push((byte)b);
				long l = payload_.length;
				buffer.push((byte)((l >>> 56) & 0xff));
				buffer.push((byte)((l >>> 48) & 0xff));
				buffer.push((byte)((l >>> 40) & 0xff));
				buffer.push((byte)((l >>> 32) & 0xff));
				buffer.push((byte)((l >>> 24) & 0xff));
				buffer.push((byte)((l >>> 16) & 0xff));
				buffer.push((byte)((l >>> 8) & 0xff));
				buffer.push((byte)(l & 0xff));
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
			buffer.push(value);
		}

		// add encrypted data
		int j = 0;
		for (byte value : payload_)
		{
			buffer.push((byte)(value ^ key[j]));
			j = j == 3? 0: j + 1;
		}

		return buffer.extractContent();
	}

	@Contract(pure = true)
	@NotNull
	private Thread _getSocketTask()
	{
		return new Thread(() ->
		{
			Thread.currentThread().setName("websocket reader from " + url);

			for (; ; )
			{
				try
				{
					// get input stream
					InputStream input = mSocket.getInputStream();

					// read data
					mSocket.setSoTimeout(0);
					int code = input.read();
					if (code < 0)
					{
						break;
					}
					mSocket.setSoTimeout(NetworkTools.TIMEOUT_READ);
					byte[] data = _readData(input);
					if (data == null)
					{
						data = new byte[0];
					}

					try
					{
						code = code & 0x0f;
						if (code < Opcode.values().length)
						{
							switch (Opcode.values()[code])
							{
								case PING:
									// send keepalive
									write(new byte[0], Opcode.PONG);
									break;

								case PONG:
									break;

								case CLOSE:
									// send 1000
									write(new byte[] { 3, (byte)232 }, Opcode.CLOSE);
									mListener.close(this, data);
									break;

								default:
									// invoke observers
									mListener.dataReceived(this, data);
									break;
							}
						}
						else
						{
							Logger.log(Level.WARNING, "invalid opcode: " + code);

							// invoke observers
							mListener.dataReceived(this, data);
						}
					}
					catch (Exception e)
					{
						Logger.log(e);
					}
				}
				catch (Exception e)
				{
					// if the socket was not disconnected
					if (mSocket != null)
					{
						Logger.log(e);
					}

					break;
				}
			}

			// disconnect
			disconnect();
			mListener.disconnected();
		});
	}

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

	@Contract(pure = true)
	private static byte @Nullable [] _readData(@NotNull InputStream stream_)
	{
		try
		{
			// read length
			int length = stream_.read() & 0x7f;
			if (length == 0x7e)
			{
				int msb = stream_.read();
				int lsb = stream_.read();
				length = lsb + (msb << 8);
			}

			// read data
			byte[] data = new byte[length];
			int res = stream_.read(data);
			if (res != length)
			{
				Logger.log(Level.WARNING, String.format("unable to read: %d of %d bytes", res, length));
			}

			return data;
		}
		catch (Exception e)
		{
			Logger.log(e);
		}

		return null;
	}

	private final TimeManager.Handler<Void> _keepaliveWatchdog = new TimeManager.Handler<>()
	{
		@Override
		public void handle(int time_, Void param_)
		{
			if ((isConnected() == true) && (mKeepalive > 0))
			{
				// send keepalive
				write(new byte[0], Opcode.PING);

				// reschedule
				TimeManager.asyncExecute(mKeepalive, _keepaliveWatchdog);
			}
		}
	};
}
