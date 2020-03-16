package borg.framework.resources;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import borg.framework.auxiliaries.Logging;
import borg.framework.auxiliaries.NetworkTools;
import borg.framework.structures.Pair;

public final class HttpResponse implements Serializable
{
	private static final long serialVersionUID = Constants.VERSION_FRAMEWORK;

	/** communication result **/
	@NotNull
	public final NetworkResult result;

	/** response code according {@link HttpURLConnection} constants **/
	public final int code;

	/** response headers **/
	@Nullable
	public final Map<String, String> headers;

	/** received data **/
	@Nullable
	public final byte[] content;

	/** serialization of the request **/
	@Nullable
	private transient byte[] mSerialization;

	@Contract(pure = true)
	public HttpResponse(@NotNull NetworkResult result_,
		int code_,
		@Nullable Map<String, String> headers_,
		@Nullable byte[] content_)
	{
		result = result_;
		code = code_;
		headers = headers_;
		content = content_;

		mSerialization = null;
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
		byte[] content = null;
		if (code > 0)
		{
			// read headers
			headers = new HashMap<>();
			for (; ; )
			{
				// parse header
				Pair<String, String> header;
				line = NetworkTools.readLine(stream_, timeout_);
				assert line != null;
				header = NetworkTools.parseHeader(line);
				if (header != null)
				{
					headers.put(header.el1, header.el2);
				}
				else
				{
					break;
				}
			}

			// read content
			content = NetworkTools.readBytes(stream_, timeout_);
		}

		// build response
		return new HttpResponse(NetworkResult.SUCCESS, code, headers, content);
	}

	/**
	 * @return request serialized as bytes array.
	 */
	@NotNull
	@Contract(pure = true)
	public byte[] serialize()
	{
		if (mSerialization == null)
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
				if (headers != null)
				{
					separator = ":".getBytes();
					for (Map.Entry<String, String> header: headers.entrySet())
					{
						stream.write(header.getKey().getBytes());
						stream.write(separator);
						stream.write(header.getValue().getBytes());
						stream.write(eol);
					}
					stream.write(eol);
				}

				// write content
				if (content != null)
				{
					stream.write(content);
				}
			}
			catch (Exception e)
			{
				Logging.logging(e);
			}

			mSerialization = stream.toByteArray();
		}

		return mSerialization;
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
