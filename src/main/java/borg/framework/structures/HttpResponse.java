package borg.framework.structures;

import borg.framework.Constants;
import borg.framework.auxiliaries.Logger;
import borg.framework.auxiliaries.NetworkTools;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Serial;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

public final class HttpResponse implements Serializable
{
	@Serial
	private static final long serialVersionUID = Constants.VERSION_FRAMEWORK;

	/** communication result **/
	@NotNull
	public final NetworkResult result;

	/** response code according {@link HttpURLConnection} constants **/
	public final int code;

	/** response headers **/
	@NotNull
	public final Map<String, String> headers;

	/** received data **/
	public final byte @Nullable [] content;

	@Contract(pure = true)
	public HttpResponse(@NotNull NetworkResult result_,
		int code_,
		@Nullable Map<String, String> headers_,
		byte @Nullable [] content_)
	{
		result = result_;
		code = code_;
		headers = new HashMap<>();
		if (headers_ != null)
		{
			headers.putAll(headers_);
		}
		content = content_;
		if (content_ != null)
		{
			headers.put("content-length", Integer.toString(content_.length));
		}
	}

	@Contract(pure = true)
	@NotNull
	public static HttpResponse readResponse(@NotNull InputStream stream_)
	{
		// prepare
		Map<String, String> headers = null;
		byte[] content = null;
		int code = -1;
		NetworkResult result = NetworkResult.UNABLE_TO_READ;

		// read code
		String line = NetworkTools.readLine(stream_);
		if (line != null)
		{
			code = NetworkTools.parseCode(line);
			if (code > 0)
			{
				// read headers
				headers = new HashMap<>();
				for (; ; )
				{
					// parse header
					Pair<String, String> header;
					line = NetworkTools.readLine(stream_);
					assert line != null;
					header = NetworkTools.parseHeader(line);
					if (header != null)
					{
						headers.put(header.key.toLowerCase(), header.value.toLowerCase());
					}
					else
					{
						break;
					}
				}

				// get length
				int length = -1;
				try
				{
					length = Integer.parseInt(headers.get("content-length"));
				}
				catch (Exception e)
				{
					// if chunked
					if ("chunked".equals(headers.get("transfer-encoding")) == true)
					{
						// read size
						line = NetworkTools.readLine(stream_);
						assert line != null;
						length = Integer.parseInt(line.trim(), 16);
					}
				}

				// read content
				if (length > 0)
				{
					content = NetworkTools.readBytes(stream_, length);
				}

				result = NetworkResult.SUCCESS;
			}
		}

		// build response
		return new HttpResponse(result, code, headers, content);
	}

	/**
	 * @return request serialized as bytes array.
	 */
	@Contract(pure = true)
	public byte @NotNull [] serialize()
	{
		ByteArrayOutputStream stream = new ByteArrayOutputStream(1024);
		byte[] separator = " ".getBytes();
		byte[] eol = "\r\n".getBytes();

		try
		{
			// write HTTP 1.1
			stream.write("HTTP/1.1".getBytes());
			stream.write(separator);

			// write code
			stream.write(Integer.toString(code).getBytes());
			stream.write(separator);
			stream.write(eol);

			// write headers
			separator = ":".getBytes();
			for (Map.Entry<String, String> header : headers.entrySet())
			{
				String key = header.getKey();
				if (key != null)
				{
					stream.write(key.getBytes());
					stream.write(separator);
					stream.write(header.getValue().getBytes());
					stream.write(eol);
				}
			}
			stream.write(eol);

			// write content
			if (content != null)
			{
				stream.write(content);
			}
		}
		catch (Exception e)
		{
			Logger.log(e);
		}

		return stream.toByteArray();
	}

	@Contract(" -> new")
	@Override
	@NotNull
	public String toString()
	{
		StringBuilder builder = new StringBuilder();

		builder.append("result: ");
		builder.append(result);
		builder.append("\n\n");
		builder.append(new String(serialize()));

		return new String(builder);
	}
}
