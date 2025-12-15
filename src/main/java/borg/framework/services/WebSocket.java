package borg.framework.services;

import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.net.ssl.SSLSocketFactory;

import borg.framework.auxiliaries.Auxiliary;
import borg.framework.auxiliaries.Logger;
import borg.framework.auxiliaries.NetworkTools;
import borg.framework.collections.ByteArray;
import borg.framework.structures.HttpRequest;
import borg.framework.structures.HttpResponse;
import borg.framework.structures.NetworkResult;

public class WebSocket extends Socket
{
	/*************************************************************************************************
	 * Public Constants
	 ************************************************************************************************/

	/** websocket version **/
	public static final int VERSION_WEBSOCKET = 13;

	/** protocol version **/
	public static final String AGENT_WEBSOCKET = "borg_socket/1.0";

	/*************************************************************************************************
	 * Constants
	 ************************************************************************************************/

	private static final int LENGTH_KEY = 16;

	private static final String HEADER_HOST = "Host";

	private static final String HEADER_CONNECTION = "Connection";

	private static final String HEADER_PROTOCOL = "Sec-WebSocket-Protocol";

	private static final String HEADER_KEY = "Sec-WebSocket-Key";

	private static final String HEADER_VERSION = "Sec-WebSocket-Version";

	private static final String HEADER_UPGRADE = "Upgrade";

	private static final String HEADER_AGENT = "User-Agent";

	private static final int TIMEOUT_READ = 500;

	/*************************************************************************************************
	 * Definitions
	 ************************************************************************************************/

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
		 *
		 * @param this_ websocket.
		 */
		void disconnected(@NotNull WebSocket this_);

		/**
		 * keepalive failed.
		 *
		 * @param this_ websocket.
		 */
		@SuppressWarnings("unused")
		default void keepaliveFailed(@NotNull WebSocket this_)
		{
		}
	}

	/*************************************************************************************************
	 * Fields
	 ************************************************************************************************/

	/** websocket URL **/
	public final URI uri;

	/** socket events listener **/
	private final Listener mListener;

	/** communication socket **/
	private Socket mSocket;

	/** keepalive interval **/
	private long mKeepalive;

	/** time when last pong was received **/
	private long mLastPong;

	/*************************************************************************************************
	 * Methods
	 ************************************************************************************************/

	public WebSocket(@NotNull String url_, @NotNull Listener listener_)
	{
		this(createUri(url_), listener_);
	}

	public WebSocket(@NotNull URI uri_, @NotNull Listener listener_)
	{
		uri = uri_;
		mListener = listener_;

		mSocket = null;
		mKeepalive = -1;
	}

	/**
	 * @return {@code true} if the websocket is connected.
	 */
	@CheckReturnValue
	public boolean isConnected()
	{
		return mSocket != null;
	}

	/**
	 * connect websocket. Blocking operation.
	 *
	 * @param timeout_  connection timeout, 0 for infinite.
	 * @param protocol_ sub-protocol to use.
	 * @param headers_  headers to send with the connection.
	 *
	 * @return operation response.
	 */
	@NotNull
	public synchronized HttpResponse connect(long timeout_,
		@NotNull String protocol_,
		@Nullable Map<String, String> headers_)
	{
		Logger.log("websocket: connect to " + uri.toString());

		// prepare
		int code = -1;
		NetworkResult result;
		Map<String, String> headers = null;
		final long start = TimeManager.getRealTime();

		// if not connected
		if (isConnected() == false)
		{
			try
			{
				// create connection
				int port = uri.getPort();
				if (uri.getScheme().equals("wss"))
				{
					mSocket = SSLSocketFactory.getDefault().createSocket();
					if (port < 0)
					{
						port = 443;
					}
				}
				else
				{
					mSocket = new Socket();
					if (port < 0)
					{
						port = 80;
					}
				}
				mSocket.setSoTimeout(NetworkTools.TIMEOUT_CONNECT);
				mSocket.connect(new InetSocketAddress(uri.getHost(), port), (int)timeout_);

				// generate key
				String key = generateKey();

				// build request
				Map<String, String> requestHeaders = new HashMap<>();
				requestHeaders.put(HEADER_HOST, uri.getHost());
				requestHeaders.put(HEADER_CONNECTION, "Upgrade");
				requestHeaders.put(HEADER_PROTOCOL, protocol_);
				requestHeaders.put(HEADER_VERSION, Integer.toString(VERSION_WEBSOCKET));
				requestHeaders.put(HEADER_KEY, key);
				requestHeaders.put(HEADER_UPGRADE, "websocket");
				requestHeaders.put(HEADER_AGENT, AGENT_WEBSOCKET);
				if (headers_ != null)
				{
					requestHeaders.putAll(headers_);
				}

				// build request
				HttpRequest request = new HttpRequest("GET", uri, requestHeaders, null);

				// write request
				OutputStream output = mSocket.getOutputStream();
				output.write(request.serialize());

				// read response
				long now = TimeManager.getRealTime();
				if (timeout_ > 0)
				{
					mSocket.setSoTimeout((int)(timeout_ - (now - start)));
				}
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
						Logger.log(Level.WARNING, "websocket: unexpected response: " + code);
						result = NetworkResult.UNEXPECTED_RESPONSE;
						disconnect();
					}
				}
				else
				{
					Logger.log(Level.WARNING, "websocket: unable to parse code");
					result = NetworkResult.UNEXPECTED_RESPONSE;
					disconnect();
				}
			}
			catch (Exception e)
			{
				Logger.log(e);
				result = NetworkResult.UNABLE_TO_CONNECT;
				disconnect();
			}
		}
		else
		{
			Logger.log(Level.WARNING, "websocket: already connected");
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

				mListener.disconnected(this);
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
		mLastPong = TimeManager.getRealTime();

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
		if (isConnected())
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

		return NetworkResult.NOT_CONNECTED;
	}

	@NotNull
	@CheckReturnValue
	private static String generateKey()
	{
		// generate key
		byte[] key = new byte[LENGTH_KEY];
		for (int i = 0; i < LENGTH_KEY; ++i)
		{
			key[i] = (byte)Auxiliary.random();
		}

		return Base64.getEncoder().encodeToString(key);
	}

	@SuppressWarnings("ConstantConditions")
	@CheckReturnValue
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

	@CheckReturnValue
	@NotNull
	private Thread _getSocketTask()
	{
		return new Thread(() ->
		{
			Thread.currentThread().setName(TasksManager.buildThreadName("websocket reader: " + uri));

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

					try
					{
						code = code & 0x0f;
						if (code < Opcode.values().length)
						{
							// read frame data
							mSocket.setSoTimeout(TIMEOUT_READ);
							byte[] data = _readData(input);

							switch (Opcode.values()[code])
							{
								case PING -> write(new byte[0], Opcode.PONG);
								case PONG -> mLastPong = TimeManager.getRealTime();
								case CLOSE ->
								{
									write(new byte[] { 3, (byte)232 }, Opcode.CLOSE);
									mListener.close(this, data);
								}
								default -> mListener.dataReceived(this, data);
							}
						}
						else
						{
							Logger.log(Level.WARNING, "websocket: invalid opcode: " + code);
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
		});
	}

	@NotNull
	@CheckReturnValue
	private static URI createUri(@NotNull String uri_)
	{
		try
		{
			return new URI(uri_);
		}
		catch (Throwable e)
		{
			throw new Error(e);
		}
	}

	@CheckReturnValue
	private static byte @NotNull [] _readData(@NotNull InputStream stream_)
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
			if (length > 0)
			{
				int res = stream_.read(data);
				if (res != length)
				{
					Logger.log(Level.WARNING,
						String.format("websocket: unable to read: %d of %d bytes", res, length));
				}
			}

			return data;
		}
		catch (Exception e)
		{
			Logger.log(e);
		}

		return new byte[0];
	}

	private final TimeManager.Handler<Void> _keepaliveWatchdog = new TimeManager.Handler<>()
	{
		@Override
		public void handle(int time_, Void param_)
		{
			if ((isConnected() == true) && (mKeepalive > 0))
			{
				// if pong wasn't received
				if (mLastPong == 0)
				{
					Logger.log(Level.WARNING, "websocket: no pong received");
					mListener.keepaliveFailed(WebSocket.this);
				}

				// send keepalive
				TasksManager.runOnThread("websocket write", (p_) -> write(new byte[0], Opcode.PING));

				// reschedule
				mLastPong = 0;
				TimeManager.asyncExecute(mKeepalive, _keepaliveWatchdog);
			}
		}
	};
}
