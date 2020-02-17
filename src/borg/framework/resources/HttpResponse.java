package borg.framework.resources;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Map;

import borg.framework.auxiliaries.NetworkTools;

public final class HttpResponse extends HttpRequest
{
	/** communication result **/
	@NotNull
	public final NetworkResult result;

	/** response code according {@link HttpURLConnection} constants **/
	public final int code;

	@Contract(pure = true)
	public HttpResponse(@NotNull NetworkResult result_,
		int code_,
		@Nullable Map<String, String> headers_,
		@Nullable byte[] data_)
	{
		super(headers_, data_);

		result = result_;
		code = code_;
	}

	@Contract(pure = true)
	@NotNull
	public static HttpResponse readResponse(@NotNull InputStream stream_, long timeout_)
	{
		// read code
		String line = NetworkTools.readLine(stream_, NetworkTools.TIMEOUT_READ);
		assert line != null;
		int code = NetworkTools.parseCode(line);
		Map<String, String> headers = null;
		byte[] data = null;
		if (code > 0)
		{
			// read data
			HttpRequest request = HttpRequest.readRequest(stream_, timeout_);
			headers = request.headers;
			data = request.data;
		}

		// build response
		return new HttpResponse(NetworkResult.SUCCESS, code, headers, data);
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
		if (data != null)
		{
			builder.append(": ");
			builder.append(new String(data));
		}

		return new String(builder);
	}
}
