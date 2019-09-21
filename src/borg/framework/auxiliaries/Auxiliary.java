package borg.framework.auxiliaries;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

public final class Auxiliary
{
	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Public Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	private static final int SIZE_CHUNK = 8192;

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Definitions
	//////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Fields
	//////////////////////////////////////////////////////////////////////////////////////////////////

	private static final Random sRandom = new Random();

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Methods
	//////////////////////////////////////////////////////////////////////////////////////////////////

	static
	{
		resetRandom(System.currentTimeMillis());
	}

	@Contract(pure = true)
	private Auxiliary()
	{
		// private constructor to prevent instantiation
	}

	/**
	 * reset random pseudo algorithm to some initial value.
	 *
	 * @param reset_ initialization value.
	 */
	public static void resetRandom(long reset_)
	{
		// if reset value is valid
		if ((reset_ != 0) && (reset_ != 0x9068ffff464fffffL))
		{
			sRandom.setSeed(reset_);
		}
	}

	/**
	 * @return pseudo random unsigned numbers in 31 bit
	 */
	@Contract(pure = true)
	public static int random()
	{
		int random = sRandom.nextInt();
		if (random < 0)
		{
			return -random;
		}

		return random;
	}

	/**
	 * read bytes from input stream.
	 *
	 * @param stream_ stream to read from.
	 *
	 * @return read bytes or {@code null} if the stream is not readable.
	 */
	@Nullable
	public static byte[] readFromStream(@NotNull InputStream stream_)
	{
		ByteArrayOutputStream arrayStream = new ByteArrayOutputStream();

		// read from stream
		byte[] buffer = new byte[SIZE_CHUNK];

		try
		{
			for (; ; )
			{
				// read chunk
				int len;
				len = stream_.read(buffer, 0, buffer.length);

				// if last chunk was read
				if (len < 0)
				{
					break;
				}

				// write chunk
				arrayStream.write(buffer, 0, len);
			}
		}
		catch (Exception e)
		{
			Logging.logging(arrayStream.size() + " bytes read", e);
			try
			{
				arrayStream.close();
			}
			catch (IOException e1)
			{
				Logging.logging(e);
			}
			return null;
		}

		// read stream and close
		buffer = arrayStream.toByteArray();
		try
		{
			arrayStream.close();
		}
		catch (IOException e)
		{
			Logging.logging(e);
		}

		return buffer;
	}

	/**
	 * sleep on thread.
	 *
	 * @param time_ how much time to sleep.
	 */
	public static void sleep(long time_)
	{
		try
		{
			Thread.sleep(time_);
		}
		catch (Exception e)
		{
			throw new Error(e);
		}
	}
}
