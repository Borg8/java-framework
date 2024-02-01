package borg.framework.structures;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.Serial;
import java.io.Serializable;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import borg.framework.Constants;
import borg.framework.auxiliaries.Logger;
import borg.framework.auxiliaries.NetworkTools;
import borg.framework.collections.ByteArray;

public class HttpRequest implements Serializable
{
	@Serial
	private static final long serialVersionUID = Constants.VERSION_FRAMEWORK;

	/** request method **/
	public final String method;

	/** request URI **/
	public final URI uri;

	/** response headers **/
	@Nullable
	public final Map<String, String> headers;

	/** received data **/
	public final byte @Nullable [] content;

	@Contract(pure = true)
	public HttpRequest(@NotNull String method_,
		@NotNull URI uri_,
		@Nullable Map<String, String> headers_,
		byte @Nullable [] content_)
	{
		method = method_;
		uri = uri_;
		headers = headers_;
		content = content_;
	}

	/**
	 * read request from stream.
	 *
	 * @param stream_ stream to read the request from.
	 *
	 * @return read request, if parsed.
	 */
	@Nullable
	public static HttpRequest readRequest(@NotNull InputStream stream_)
	{
		try
		{
			// read header
			String title = NetworkTools.readLine(stream_);
			if (title != null)
			{
				// read method
				int e = title.indexOf(' ');
				String method = title.substring(0, e);

				// read path
				int i = e + 1;
				e = title.indexOf(' ', i);
				URI uri = new URI(title.substring(i, e));

				// read headers
				Map<String, String> headers = new HashMap<>();
				for (; ; )
				{
					// parse header
					Pair<String, String> header;
					String line = NetworkTools.readLine(stream_);
					assert line != null;
					header = NetworkTools.parseHeader(line);
					if (header != null)
					{
						headers.put(header.key, header.value);
					}
					else
					{
						break;
					}
				}

				// read content
				int length;
				byte[] content = null;
				try
				{
					length = Integer.parseInt(headers.get("content-length"));
					content = NetworkTools.readBytes(stream_, length);
				}
				catch (Exception e_)
				{
					// nothing to do here
				}

				// build request
				return new HttpRequest(method, uri, headers, content);
			}
		}
		catch (Exception e2_)
		{
			Logger.log(Level.FINE, e2_);
		}

		return null;
	}

	/**
	 * @return request serialized as bytes array.
	 */
	@Contract(pure = true)
	public byte @NotNull [] serialize()
	{
		ByteArray buffer = new ByteArray();
		byte[] separator = " ".getBytes();
		byte[] eol = "\r\n".getBytes();

		try
		{
			// write method
			buffer.push(method.getBytes());
			buffer.push(separator);

			// write uri
			String query = uri.toString();
			if (query != null)
			{
				buffer.push(query.getBytes());
			}
			buffer.push(separator);

			// write HTTP 1.1
			buffer.push("HTTP/1.1\r\n".getBytes());

			// write headers
			if (headers != null)
			{
				separator = ":".getBytes();
				for (Map.Entry<String, String> header : headers.entrySet())
				{
					buffer.push(header.getKey().getBytes());
					buffer.push(separator);
					buffer.push(header.getValue().getBytes());
					buffer.push(eol);
				}
				buffer.push(eol);
			}

			// write content
			if (content != null)
			{
				buffer.push(content);
			}
		}
		catch (Exception e)
		{
			Logger.log(e);
		}

		return buffer.extractContent();
	}

	@Contract(" -> new")
	@Override
	@NotNull
	public String toString()
	{
		return new String(serialize());
	}
}
