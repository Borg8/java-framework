package borg.framework.auxiliaries;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import borg.framework.services.TimeManager;
import borg.framework.structures.Pair;

public final class NetworkTools
{
	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Public Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	/** read connection **/
	public static final long TIMEOUT_CONNECT = 10 * TimeManager.SECOND;

	/** read timeout **/
	public static final long TIMEOUT_READ = 10;

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	private static final int SIZE_CHUNK = 8192;

	private static final long MIN_TIMEOUT_READ = 1000;

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Methods
	//////////////////////////////////////////////////////////////////////////////////////////////////

	private NetworkTools()
	{
		// private constructor to prevent instantiation
	}

	/**
	 * read bytes from input stream. Blocking operation.
	 *
	 * @param stream_  stream to read from.
	 * @param timeout_ timeout to read each byte.
	 *
	 * @return read bytes, or {@code null} if the stream is not readable.
	 */
	@Nullable
	@Contract(pure = true)
	public static byte[] readBytes(@NotNull InputStream stream_, long timeout_)
	{
		return read(stream_, timeout_, 0, (char)-1);
	}

	/**
	 * read line from stream. Blocking operation.
	 *
	 * @param stream_  stream to read from.
	 * @param timeout_ timeout to read each byte.
	 *
	 * @return read line or {@code null} if the stream is not readable.
	 */
	@Nullable
	@Contract(pure = true)
	public static String readLine(@NotNull InputStream stream_, long timeout_)
	{
		byte[] line = read(stream_, timeout_, 0, '\n');
		if (line != null)
		{
			return new String(line);
		}

		return null;
	}

	/**
	 * parse HTTP request.
	 *
	 * @param line_ line to parse code from.
	 *
	 * @return HTTP request code or negative number if was not parsed.
	 */
	@Contract(pure = true)
	public static int parseCode(@NotNull String line_)
	{
		try
		{
			return Integer.parseInt(line_.substring(9, 12));
		}
		catch (Exception e)
		{
			return -1;
		}
	}

	/**
	 * parse HTTP header.
	 *
	 * @param line_ line to parse the header from.
	 *
	 * @return parsed header, if succeeded.
	 */
	@Nullable
	@Contract(pure = true)
	public static Pair<String, String> parseHeader(@NotNull String line_)
	{
		// define flags
		final int NEW = 0;
		final int HEADER = 1;
		final int COLON = 2;
		final int VALUE = 3;

		int n = line_.length();
		int state = NEW;
		StringBuilder header = new StringBuilder();
		StringBuilder value = new StringBuilder();
		for (int i = 0; i < n; ++i)
		{
			// get valid character
			char c = line_.charAt(i);
			if ((c < ' ') || (c > 127))
			{
				continue;
			}

			// if colon found
			if (c == ':')
			{
				// if header head parsed
				if (state == HEADER)
				{
					state = COLON;
					continue;
				}
				else
				{
					if (state == VALUE)
					{
						continue;
					}
				}
			}
			else
			{
				switch (state)
				{
					// new header found
					case NEW:
						header.append(lower(c));

						state = HEADER;
						continue;

						// new value found
					case COLON:
						state = VALUE;
						continue;

						// next value character found
					case VALUE:
						value.append(lower(c));
						continue;

						// next header character found
					default:
						header.append(lower(c));
						continue;
				}
			}

			// unable to parse
			return null;
		}

		// if header received
		if (state == VALUE)
		{
			// build header
			return new Pair<>(header.toString(), value.toString());
		}

		return null;
	}

	@Contract(pure = true)
	private static char lower(char c_)
	{
		if ((c_ >= 'A') && (c_ <= 'Z'))
		{
			c_ += 'a' - 'A';
		}

		return c_;
	}

	@Nullable
	@Contract(pure = true)
	private static byte[] read(@NotNull InputStream stream_, long timeout_, int size_, char eof_)
	{
		// prepare
		long now = TimeManager.getSystemTime() + MIN_TIMEOUT_READ;
		ByteArrayOutputStream buffer = new ByteArrayOutputStream(SIZE_CHUNK);
		if (size_ <= 0)
		{
			size_ = Integer.MAX_VALUE;
		}
		if (timeout_ <= 0)
		{
			timeout_ = Long.MAX_VALUE;
		}

		try
		{
			do
			{
				// FIXME even if stream_.available() == 0, stream_.read() can succeed.

				// if data available
				if (stream_.available() > 0)
				{
					// read byte
					int b = stream_.read();

					// if end of stream
					if ((b <= 0) || (b == eof_))
					{
						break;
					}

					// add byte
					buffer.write(b);

					// if all bytes read
					if (buffer.size() >= size_)
					{
						break;
					}

					now = TimeManager.getSystemTime();
				}
				else
				{
					Auxiliary.sleep(10);
				}
			} while (TimeManager.getSystemTime() - now <= timeout_);
		}
		catch (Exception e)
		{
			Logger.log(e);
			return null;
		}

		// close buffer
		byte[] array = buffer.toByteArray();
		try
		{
			buffer.close();
		}
		catch (Exception e)
		{
			Logger.log(e);
		}
		return array;
	}
}
