package com.borg.framework.auxiliaries;

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

	public static final Random random = new Random();

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Methods
	//////////////////////////////////////////////////////////////////////////////////////////////////

	static
	{
		seedRandom(System.currentTimeMillis());
	}

	@Contract(pure = true)
	private Auxiliary()
	{
		// private constructor to prevent instantiation
	}

	/**
	 * seed pseudo random algorithm.
	 *
	 * @param seed_ seed to set.
	 */
	public static void seedRandom(long seed_)
	{
		// if reset value is valid
		if ((seed_ != 0) && (seed_ != 0x9068ffff464fffffL))
		{
			random.setSeed(seed_);
		}
	}

	/**
	 * get random number from Gauss distribution.
	 *
	 * @param mu_    number offset.
	 * @param sigma_ number multiplier.
	 *
	 * @return random number from Gauss distribution.
	 */
	@Contract(pure = true)
	public static double randomGauss(double mu_, double sigma_)
	{
		return mu_ + random.nextGaussian() * sigma_;
	}

	/**
	 * @return random positive integer of 31 bit
	 */
	@Contract(pure = true)
	public static int random()
	{
		return random.nextInt() & 0x7fffffff;
	}

	/**
	 * get random in range.
	 *
	 * @param min_ minimum value.
	 * @param max_ maximum value.
	 *
	 * @return random integer in the given range (from min_ to max_ - 1).
	 */
	@Contract(pure = true)
	public static int random(int min_, int max_)
	{
		return (random() % (max_ - min_)) + min_;
	}

	/**
	 * get random number in range.
	 *
	 * @param max_ maximum value, not included.
	 *
	 * @return random integer in range 0 to given max.
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
		return random.nextDouble() * (max_ - min_) + min_;
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
		return random.nextDouble() * max_;
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
