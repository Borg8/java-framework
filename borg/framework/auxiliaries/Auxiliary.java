package borg.framework.auxiliaries;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

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

	private static int sRandomLWord;

	private static int sRandomHWord;

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Methods
	//////////////////////////////////////////////////////////////////////////////////////////////////

	static
	{
		resetRandom(System.currentTimeMillis());
	}

	private Auxiliary()
	{
		// private constructor to prevent instantiation
	}

	/**
	 * reset random pseudo algorithm to some initial value. Initialization will failed if low value is
	 * 0 or 0x464fffff or high value is 0 or 0x9068ffff.
	 *
	 * @param l_ low initialization word. May not be 0 or 0x464fffff.
	 * @param h_ high initialization word. May not be 0 or 0x9068ffff.
	 */
	public static void resetRandom(int l_, int h_)
	{
		// if initiate values is valid
		if ((l_ != 0) && (l_ != 0x464fffff) && (h_ != 0) && (h_ != 0x9068ffff))
		{
			sRandomLWord = l_;
			sRandomHWord = h_;
		}
	}

	/**
	 * reset random pseudo algorithm to some initial value. Initialization will be failed if reset
	 * value is 0 or 0x9068ffff464fffffL.
	 *
	 * @param reset_ initialization value. May not be 0 or 0x9068ffff464fffffL.
	 */
	public static void resetRandom(long reset_)
	{
		// if reset value is valid
		if ((reset_ != 0) && (reset_ != 0x9068ffff464fffffL))
		{
			sRandomLWord = (int)(reset_);
			sRandomHWord = (int)(reset_ >> 32);
		}
	}

	/**
	 * @return pseudo random unsigned numbers in 31 bit
	 */
	public static int random()
	{
		sRandomHWord = 36969 * (sRandomHWord & 0xffff) + (sRandomHWord >> 16);
		sRandomLWord = 18000 * (sRandomLWord & 0xffff) + (sRandomLWord >> 16);

		return ((sRandomHWord << 16) + sRandomLWord) & 0x7fffffff;
	}

	/**
	 * read bytes from input stream.
	 * 
	 * @param stream_ stream to read from.
	 * @return read bytes or {@code null} if the stream is not readable.
	 */
	@Nullable
	public static byte[] readFromStream(@NonNull InputStream stream_)
	{
		ByteArrayOutputStream arrayStream = new ByteArrayOutputStream();

		// read from stream
		byte[] buffer = new byte[SIZE_CHUNK];

		try
		{
			for (;;)
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
