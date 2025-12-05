package borg.framework.auxiliaries;

import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Random;

public final class Auxiliary
{
	/*************************************************************************************************
	 * Public Constants
	 ************************************************************************************************/

	/*************************************************************************************************
	 * Constants
	 ************************************************************************************************/

	/*************************************************************************************************
	 * Definitions
	 ************************************************************************************************/

	/*************************************************************************************************
	 * Fields
	 ************************************************************************************************/

	public static final Random random = new Random();

	/*************************************************************************************************
	 * Methods
	 ************************************************************************************************/

	static
	{
		seedRandom(System.currentTimeMillis());
	}

	@CheckReturnValue
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
		random.setSeed(seed_);
	}

	/**
	 * get random number from Gauss distribution.
	 *
	 * @param mu_    number offset.
	 * @param sigma_ number multiplier.
	 *
	 * @return random number from Gauss distribution.
	 */
	@CheckReturnValue
	public static double randomGauss(double mu_, double sigma_)
	{
		return mu_ + random.nextGaussian() * sigma_;
	}

	/**
	 * @return random positive integer of 31 bit
	 */
	@CheckReturnValue
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
	@CheckReturnValue
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
	@CheckReturnValue
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
	 * @return random long number in the given range.
	 */
	@CheckReturnValue
	public static long random(long min_, long max_)
	{
		// TODO test for nextLong produces negative numbers
		return ((random.nextLong() & 0x7fffffffffffffffL) % (max_ - min_)) + min_;
	}

	/**
	 * get random number in range.
	 *
	 * @param max_ maximum value.
	 *
	 * @return random long number in range 0 to given max.
	 */
	@CheckReturnValue
	public static long random(long max_)
	{
		return (random.nextLong() & 0x7fffffffffffffffL) % max_;
	}

	/**
	 * get random in range.
	 *
	 * @param min_ minimum value.
	 * @param max_ maximum value.
	 *
	 * @return random real number in the given range.
	 */
	@CheckReturnValue
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
	@CheckReturnValue
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
			if (time_ > 0)
			{
				Thread.sleep(time_);
			}
		}
		catch (Exception e)
		{
			throw new Error(e);
		}
	}

	/**
	 * build URL.
	 *
	 * @param components_ components of the URL.
	 *
	 * @return URL.
	 */
	@NotNull
	@CheckReturnValue
	public static String buildPath(String @NotNull ... components_)
	{
		StringBuilder builder = new StringBuilder();
		char s = File.separatorChar;
		for (String component : components_)
		{
			component = component.trim();
			if (component.isEmpty() == false)
			{
				int last = builder.length() - 1;
				if (last >= 0)
				{
					if (component.charAt(0) != s)
					{
						if (builder.charAt(last) != s)
						{
							builder.append(s);
						}
					}
					else
					{
						if (builder.charAt(last) == s)
						{
							builder.deleteCharAt(last);
						}
					}
				}
				builder.append(component);
			}
		}

		return builder.toString();
	}
}
