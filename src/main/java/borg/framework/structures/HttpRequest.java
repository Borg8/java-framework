package borg.framework.structures;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import borg.framework.Constants;
import borg.framework.auxiliaries.Logger;
import borg.framework.auxiliaries.NetworkTools;

public class HttpRequest implements Serializable
{
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

	/** serialization of the request **/
	private transient byte @Nullable [] mSerialization;

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

		mSerialization = null;
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
						headers.put(header.el1, header.el2);
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

				// write uri
				stream.write(uri.getRawQuery().getBytes());
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
				Logger.log(e);
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
