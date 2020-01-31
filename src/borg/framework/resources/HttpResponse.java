package borg.framework.resources;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.HttpURLConnection;
import java.util.Map;

public final class HttpResponse
{
	/** communication result **/
	@NotNull
	public final NetworkResult result;

	/** final ULR from where the response received **/
	@NotNull
	public final String url;

	/** communication response **/
	@Nullable
	public final byte[] response;

	/** response headers **/
	@Nullable
	public final Map<String, String> headers;

	/** response code according {@link HttpURLConnection} constants **/
	public final int code;

	@Contract(pure = true)
	public HttpResponse(@NotNull NetworkResult result_,
		@NotNull String url_,
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

	@Contract(" -> new")
	@Override
	@NotNull
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
