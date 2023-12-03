package com.borg.framework.services;

import com.borg.framework.structures.HttpResponse;
import com.borg.framework.structures.NetworkResult;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

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
	private long mConnectionTimeout;

	/** read timeout **/
	private long mReadTimeout;

	/** default headers **/
	private final Map<String, String> mDefaultHeaders;

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Methods
	//////////////////////////////////////////////////////////////////////////////////////////////////

	public HttpClient()
	{
		setTimeouts(TimeManager.SECOND * 5, TimeManager.SECOND * 5);

		mDefaultHeaders = new HashMap<>();
	}

	/**
	 * set default headers.
	 *
	 * @param headers_ default headers.
	 */
	public void setDefaultHeaders(@NotNull Map<String, String> headers_)
	{
		mDefaultHeaders.clear();
		mDefaultHeaders.putAll(headers_);
	}

	/**
	 * send HTTP POST (blocking operation).
	 *
	 * @param url_     URL to send to post.
	 * @param method_  request method.
	 * @param headers_ request headers.
	 * @param content_ request content.
	 *
	 * @return response on HTTP request.
	 */
	@NotNull
	public HttpResponse sendRequest(@NotNull String url_,
		@NotNull String method_,
		@Nullable Map<String, String> headers_,
		byte @Nullable [] content_)
	{
		// build client
		OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
		clientBuilder.connectTimeout(mConnectionTimeout, TimeUnit.MILLISECONDS);
		clientBuilder.readTimeout(mReadTimeout, TimeUnit.MILLISECONDS);
		OkHttpClient client = clientBuilder.build();

		// build request
		Request.Builder requestBuilder = new Request.Builder().url(url_);

		// add headers
		for (Map.Entry<String, String> entry : mDefaultHeaders.entrySet())
		{
			requestBuilder.addHeader(entry.getKey(), entry.getValue());
		}
		if (headers_ != null)
		{
			for (Map.Entry<String, String> entry : headers_.entrySet())
			{
				requestBuilder.addHeader(entry.getKey(), entry.getValue());
			}
		}

		// send the request
		if (content_ == null)
		{
			content_ = new byte[0];
		}
		Request request = switch (method_)
		{
			case "GET" -> requestBuilder.get().build();

			case "POST" -> requestBuilder.post(RequestBody.create(content_)).build();

			case "PUT" -> requestBuilder.put(RequestBody.create(content_)).build();

			case "PATCH" -> requestBuilder.patch(RequestBody.create(content_)).build();

			case "DELETE" -> requestBuilder.delete().build();

			case "HEAD" -> requestBuilder.head().build();

			default -> requestBuilder.method(method_, RequestBody.create(content_)).build();
		};

		// process the response
		try (Response response = client.newCall(request).execute())
		{
			// build result
			NetworkResult result = NetworkResult.SUCCESS;
			if (response.code() >= 300)
			{
				result = NetworkResult.UNEXPECTED_RESPONSE;
			}

			// build headers
			Map<String, String> headers = new HashMap<>();
			for (Map.Entry<String, List<String>> header : response.headers().toMultimap().entrySet())
			{
				StringBuilder stringBuilder = new StringBuilder();
				for (String value : header.getValue())
				{
					stringBuilder.append(value);
					stringBuilder.append(";");
				}
				headers.put(header.getKey(), stringBuilder.toString());
			}

			// build body
			byte[] body = response.body() == null? null: response.body().bytes();

			return new HttpResponse(result, response.code(), headers, body);
		}
		catch (Exception e_)
		{
			byte[] message = e_.getMessage() == null? null: e_.getMessage().getBytes();
			return new HttpResponse(NetworkResult.FAILURE, -1, null, message);
		}
	}

	/**
	 * set timeout rules to network process.
	 *
	 * @param connection_ connection timeout.
	 * @param read_       read timeout.
	 */
	public void setTimeouts(long connection_, long read_)
	{
		mConnectionTimeout = connection_;
		mReadTimeout = read_;
	}
}
