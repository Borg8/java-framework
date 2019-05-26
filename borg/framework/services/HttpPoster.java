package borg.framework.services;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import borg.framework.auxiliaries.Auxiliary;
import borg.framework.auxiliaries.Logging;

public final class HttpPoster
{
	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Public Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	/** connection timeout **/
	public static final int TIMEOUT_CONNECTION = 10 * 1000;

	/** read timeout **/
	public static final int TIMEOUT_READ = 30 * 1000;

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Definitions
	//////////////////////////////////////////////////////////////////////////////////////////////////

	public enum ResultType
	{
		/** unknown result **/
		UNKNOWN,

		/** operation was succeeded **/
		SUCCESS,

		/** no Internet connection **/
		NOT_CONNECTED,

		/** unable to connect to the host **/
		UNABLE_TO_CONNECT,

		/** unable to send data **/
		UNABLE_TO_SEND,

		/** unable to read data **/
		UNABLE_TO_READ,

		/** unexpected server response **/
		UNEXPECTED_RESPONSE
	}

	public static final class Response
	{
		/** communication result **/
		@NonNull
		public final ResultType result;

		/** final ULR from where the response received **/
		@NonNull
		public final String url;

		/** communication response **/
		@Nullable
		public final byte[] response;

		/** response headers **/
		@Nullable
		public final Map<String, String> headers;

		/** response code according {@link HttpURLConnection} constants **/
		public final int code;

		public Response(@NonNull ResultType result_,
			@NonNull String url_,
			@Nullable byte[] response_,
			@Nullable Map<String, String> headers_,
			int code_)
		{
			result = result_;
			url = url_;
			response = response_;
			headers = headers_;
			code = code_;
		}

		@Override
		@NonNull
		public String toString()
		{
			StringBuilder builder = new StringBuilder();
			builder.append("result: ");
			builder.append(result);
			builder.append(" (");
			builder.append(code);
			builder.append(')');
			if (response != null)
			{
				builder.append(": ");
				builder.append(new String(response));
			}

			return new String(builder);
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Fields
	//////////////////////////////////////////////////////////////////////////////////////////////////

	/** connection timeout **/
	private int mConnectionTimeout;

	/** read timeout **/
	private int mReadTimeout;

	/** single instance of HttpPoster */
	private static HttpPoster sInstance = null;

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Methods
	//////////////////////////////////////////////////////////////////////////////////////////////////

	private HttpPoster()
	{
		// private constructor to prevent instantiation
	}

	/**
	 * @return single instance of HttpPoster.
	 */
	@NonNull
	public static synchronized HttpPoster getInstance()
	{
		if (sInstance == null)
		{
			// create single instance of HttpPoster
			sInstance = new HttpPoster();
		}

		return sInstance;
	}

	/**
	 * send HTTP POST (blocking operation).
	 * 
	 * @param url_ URL to send to post.
	 * @param headers_ request headers.
	 * @param content_ request content.
	 * @param redirect_ if {@code true} then redirection will be followed (even between different
	 *          protocols).
	 * @return response on HTTP request.
	 */
	@NonNull
	public Response post(@NonNull String url_,
		@Nullable Map<String, String> headers_,
		@Nullable byte[] content_,
		boolean redirect_)
	{
		// initiate pessimistic response parameters
		ResultType result = ResultType.NOT_CONNECTED;
		byte[] response = null;
		HashMap<String, String> headers = null;
		int code = -1;

		// send request
		HttpURLConnection connection;
		OutputStream out = null;
		InputStream in = null;
		do
		{
			// create connection
			connection = createConnection(url_);

			// if connection created
			if (connection != null)
			{
				// add headers
				if (headers_ != null)
				{
					for (Entry<String, String> entry: headers_.entrySet())
					{
						connection.setRequestProperty(entry.getKey(), entry.getValue());
					}
				}

				// connect
				try
				{
					connection.connect();
				}
				catch (Exception e)
				{
					Logging.logging(e);
					break;
				}

				// send request
				if (content_ != null)
				{
					try
					{
						out = connection.getOutputStream();
						out.write(content_);
					}
					catch (Exception e)
					{
						Logging.logging(e);

						// unable to send
						result = ResultType.UNABLE_TO_SEND;
						break;
					}
				}

				// read response
				try
				{
					code = connection.getResponseCode();

					// if error not occurred
					if (code < 400)
					{
						// if redirection occurred
						// TODO consider perform redirection before data sending
						if ((redirect_ == true) && (code >= 300))
						{
							// get redirection location
							List<String> respHeaders = connection.getHeaderFields().get("Location");
							if ((respHeaders != null) && (respHeaders.isEmpty() == false))
							{
								// close connection
								if (out != null)
								{
									out.close();
								}
								connection.disconnect();

								// get new URL
								url_ = respHeaders.get(0);

								// redirect
								return post(url_, headers_, content_, redirect_);
							}

							in = connection.getInputStream();
							result = ResultType.UNEXPECTED_RESPONSE;
						}
						else
						{
							in = connection.getInputStream();
							result = ResultType.SUCCESS;
						}
					}
					else
					{
						in = connection.getErrorStream();
						result = ResultType.UNEXPECTED_RESPONSE;
					}
					if (in != null)
					{
						response = Auxiliary.readFromStream(in);
					}
				}
				catch (Exception e)
				{
					Logging.logging(e);

					// unable to read
					result = ResultType.UNABLE_TO_READ;
					break;
				}

				// read headers
				Map<String, List<String>> respHeaders = connection.getHeaderFields();
				if (respHeaders != null)
				{
					headers = new HashMap<>();
					for (Entry<String, List<String>> entry: respHeaders.entrySet())
					{
						String key = entry.getKey();
						if (key != null)
						{
							key = key.toLowerCase();
						}
						headers.put(key, entry.getValue().get(0));
					}
				}
			}

		} while (false);

		// free resources
		if (in != null)
		{
			try
			{
				in.close();
			}
			catch (Exception e)
			{
				Logging.logging(e);
			}
		}
		if (out != null)
		{
			try
			{
				out.close();
			}
			catch (Exception e)
			{
				Logging.logging(e);
			}
		}
		if (connection != null)
		{
			connection.disconnect();
		}

		return new Response(result, url_, response, headers, code);
	}

	/**
	 * create HTTP connection to URL (blocking operation).
	 *
	 * @param url_ URL to create connection to.
	 *
	 * @return created connection to URL or {@code null} if connection cannot be created.
	 *
	 */
	@Nullable
	public HttpURLConnection createConnection(@NonNull String url_)
	{
		try
		{
			// create the connection
			HttpURLConnection connection = (HttpURLConnection)new URL(url_).openConnection();

			// set timeouts
			connection.setConnectTimeout(mConnectionTimeout);
			connection.setReadTimeout(mReadTimeout);

			// set connection properties
			connection.setDoOutput(true);
			connection.setDoInput(true);

			return connection;
		}
		catch (Exception e)
		{
			Logging.logging(e);
		}

		return null;
	}

	/**
	 * set timeout rules to network process.
	 *
	 * @param connection_ connection timeout.
	 * @param read_ read timeout.
	 */
	public void setTimeouts(int connection_, int read_)
	{
		mConnectionTimeout = connection_;
		mReadTimeout = read_;
	}
}
