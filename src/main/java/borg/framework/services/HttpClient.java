package borg.framework.services;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import borg.framework.auxiliaries.Logger;
import borg.framework.structures.HttpResponse;
import borg.framework.structures.NetworkResult;

public final class HttpClient
{
	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Public Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Definitions
	//////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Fields
	//////////////////////////////////////////////////////////////////////////////////////////////////

	/** connection timeout **/
	private int mConnectionTimeout;

	/** read timeout **/
	private int mReadTimeout;

	/** single instance of HttpPoster */
	private static HttpClient sInstance = null;

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Methods
	//////////////////////////////////////////////////////////////////////////////////////////////////

	@Contract(pure = true)
	private HttpClient()
	{
		// private constructor to prevent instantiation
	}

	/**
	 * @return single instance of HttpPoster.
	 */
	@NotNull
	public static synchronized HttpClient getInstance()
	{
		if (sInstance == null)
		{
			// create single instance of HttpPoster
			sInstance = new HttpClient();
		}

		return sInstance;
	}

	/**
	 * send HTTP POST (blocking operation).
	 *
	 * @param url_      URL to send to post.
	 * @param method_   request method.
	 * @param headers_  request headers.
	 * @param content_  request content.
	 * @param redirect_ if {@code true} then redirection will be followed (even between different
	 *                  protocols).
	 *
	 * @return response on HTTP request.
	 */
	@NotNull
	public HttpResponse sendRequest(@NotNull String url_,
		@NotNull String method_,
		@Nullable Map<String, String> headers_,
		byte @Nullable [] content_,
		boolean redirect_)
	{
		// initiate pessimistic response parameters
		NetworkResult result = NetworkResult.NOT_CONNECTED;
		byte[] response = null;
		HashMap<String, String> headers = null;
		int code = -1;

		// send request
		HttpURLConnection connection;
		OutputStream out = null;
		InputStream in = null;
		//noinspection ConstantConditions
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
					for (Entry<String, String> entry : headers_.entrySet())
					{
						connection.setRequestProperty(entry.getKey(), entry.getValue());
					}
				}

				// connect
				try
				{
					connection.setRequestMethod(method_);
					connection.connect();
				}
				catch (Exception e)
				{
					Logger.log(e);
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
						Logger.log(e);

						// unable to send
						result = NetworkResult.UNABLE_TO_SEND;
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
								return sendRequest(url_, method_, headers_, content_, redirect_);
							}

							in = connection.getInputStream();
							result = NetworkResult.UNEXPECTED_RESPONSE;
						}
						else
						{
							in = connection.getInputStream();
							result = NetworkResult.SUCCESS;
						}
					}
					else
					{
						in = connection.getErrorStream();
						result = NetworkResult.UNEXPECTED_RESPONSE;
					}
					if (in != null)
					{
						response = StorageManager.readFile(in);
					}
				}
				catch (Exception e)
				{
					Logger.log(e);

					// unable to read
					result = NetworkResult.UNABLE_TO_READ;
					break;
				}

				// read headers
				Map<String, List<String>> respHeaders = connection.getHeaderFields();
				if (respHeaders != null)
				{
					headers = new HashMap<>();
					for (Entry<String, List<String>> entry : respHeaders.entrySet())
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
				Logger.log(e);
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
				Logger.log(e);
			}
		}
		if (connection != null)
		{
			connection.disconnect();
		}

		return new HttpResponse(result, code, headers, response);
	}

	/**
	 * create HTTP connection to URL (blocking operation).
	 *
	 * @param url_ URL to create connection to.
	 *
	 * @return created connection to URL or {@code null} if connection cannot be created.
	 */
	@Nullable
	@Contract(pure = true)
	public HttpURLConnection createConnection(@NotNull String url_)
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
			Logger.log(e);
		}

		return null;
	}

	/**
	 * set timeout rules to network process.
	 *
	 * @param connection_ connection timeout.
	 * @param read_       read timeout.
	 */
	public void setTimeouts(int connection_, int read_)
	{
		mConnectionTimeout = connection_;
		mReadTimeout = read_;
	}
}
