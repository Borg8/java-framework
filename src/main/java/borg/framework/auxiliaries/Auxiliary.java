package borg.framework.auxiliaries;

import org.jetbrains.annotations.Contract;

import java.util.Random;

public final class Auxiliary
{
	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Public Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

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
	 * get random in range.
	 *
	 * @param min_ minimum value.
	 * @param max_ maximum value.
	 *
	 * @return random real number in the given range.
	 */
	@Contract(pure = true)
	public static int random(int min_, int max_)
	{
		return (random() % (max_  - min_))  + min_;
	}

	/**
	 * get random number in range.
	 *
	 * @param max_ maximum value.
	 *
	 * @return random real number in range 0 to given max.
	 */
	@Contract(pure = true)
	public static int random(int max_)
	{
		return random() % max_;
	}

	/**
	 * get random in range.
	 *
	 * @param min_ minimum value.
	 * @param max_ maximum value.
	 *
	 * @return random real number in the given range.
	 */
	@Contract(pure = true)
	public static double random(double min_, double max_)
	{
		return sRandom.nextDouble() * (max_ - min_) + min_;
	}

	/**
	 * get random number in range.
	 *
	 * @param max_ maximum value.
	 *
	 * @return random real number in range 0 to given max.
	 */
	@Contract(pure = true)
	public static double random(double max_)
	{
		return sRandom.nextDouble() * max_;
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
