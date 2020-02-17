package borg.framework.resources;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import borg.framework.auxiliaries.NetworkTools;
import borg.framework.structures.Pair;

public class HttpRequest
{
	/** received data **/
	@Nullable
	public final byte[] data;

	/** response headers **/
	@Nullable
	public final Map<String, String> headers;

	@Contract(pure = true)
	public HttpRequest(@Nullable Map<String, String> headers_, @Nullable byte[] data_)
	{
		data = data_;
		headers = headers_;
	}

	@Contract(pure = true)
	@NotNull
	public static HttpRequest readRequest(@NotNull InputStream stream_, long timeout_)
	{
		// read headers
		Map<String, String> headers = new HashMap<>();
		for (; ; )
		{
			// parse header
			Pair<String, String> header;
			String line = NetworkTools.readLine(stream_, NetworkTools.TIMEOUT_READ);
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

		// read data
		byte[] data = NetworkTools.readBytes(stream_, timeout_);

		// build request
		return new HttpRequest(headers, data);
	}

	@Contract(" -> new")
	@Override
	@NotNull
	public String toString()
	{
		if (data != null)
		{
			return "data: " + new String(data);
		}

		return "empty";
	}
}
