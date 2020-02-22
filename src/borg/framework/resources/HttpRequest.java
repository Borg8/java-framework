package borg.framework.resources;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import borg.framework.auxiliaries.Logging;
import borg.framework.auxiliaries.NetworkTools;
import borg.framework.structures.Pair;

public class HttpRequest implements Serializable
{
	private static final long serialVersionUID = Constants.VERSION_FRAMEWORK;

	/** request method **/
	public final String method;

	/** request path **/
	public final String path;

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
	public HttpRequest(@NotNull String method_,
		@NotNull String path_,
		@Nullable Map<String, String> headers_,
		@Nullable byte[] content_)
	{
		method = method_;
		path = path_;
		headers = headers_;
		content = content_;

		mSerialization = null;
	}

	/**
	 * read request from stream.
	 *
	 * @param stream_  stream to read the request from.
	 * @param timeout_ timeout to read each byte.
	 *
	 * @return read request, if parsed.
	 */
	@Contract(pure = true)
	@Nullable
	public static HttpRequest readRequest(@NotNull InputStream stream_, long timeout_)
	{
		try
		{
			// read header
			String title = NetworkTools.readLine(stream_, timeout_);
			if (title != null)
			{
				// read method
				int e = title.indexOf(' ');
				String method = title.substring(0, e);

				// read path
				int i = e + 1;
				e = title.indexOf(' ', i);
				String path = title.substring(i, e);

				// read headers
				Map<String, String> headers = new HashMap<>();
				for (; ; )
				{
					// parse header
					Pair<String, String> header;
					String line = NetworkTools.readLine(stream_, timeout_);
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
				byte[] content = NetworkTools.readBytes(stream_, timeout_);

				// build request
				return new HttpRequest(method, path, headers, content);
			}
		}
		catch (Exception e)
		{
			Logging.logging(e);
		}

		return null;
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
				// write method
				stream.write(method.getBytes());
				stream.write(separator);

				// write path
				stream.write(path.getBytes());
				stream.write(separator);

				// write HTTP 1.1
				stream.write("HTTP/1.1\r\n".getBytes());

				// write headers
				if (headers != null)
				{
					separator = ":".getBytes();
					for (Map.Entry<String, String> header : headers.entrySet())
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
		return new String(serialize());
	}
}
