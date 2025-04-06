package borg.framework.structures.computers;

import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import borg.framework.collections.DoubleArray;

public class Gaussian
{
	/** all values **/
	private final DoubleArray mValues;

	/** summation of all values **/
	private double mSum;

	public Gaussian()
	{
		mValues = new DoubleArray();
	}

	/**
	 * @return number of values.
	 */
	@CheckReturnValue
	public int getCount()
	{
		return mValues.length();
	}

	/**
	 * @return all values.
	 */
	@CheckReturnValue
	@Unmodifiable
	public double @NotNull [] getValues()
	{
		return mValues.extractContent();
	}

	/**
	 * @return mean.
	 */
	@CheckReturnValue
	public double getMean()
	{
		int n = mValues.length();
		if (n > 0)
		{
			return mSum / n;
		}

		return Double.NaN;
	}

	/**
	 * @return variance.
	 */
	@CheckReturnValue
	public double getVariance()
	{
		int n = mValues.length();
		if (n > 0)
		{
			double mean = getMean();
			double sum = 0;
			for (int i = 0; i < n; i++)
			{
				double diff = mValues.get(i) - mean;
				sum += diff * diff;
			}
			return sum / n;
		}

		return Double.NaN;
	}

	/**
	 * @return variance of values than smaller than mean.
	 */
	@CheckReturnValue
	public double getLeftVariance()
	{
		int n = mValues.length();
		if (n > 0)
		{
			double mean = getMean();
			double sum = 0;
			int count = 0;
			for (int i = 0; i < n; i++)
			{
				double value = mValues.get(i);
				if (value <= mean)
				{
					double diff = value - mean;
					sum += diff * diff;
					++count;
				}
			}

			if (count > 0)
			{
				return sum / count;
			}
		}

		return Double.NaN;
	}

	/**
	 * @return variance of values than greater than mean.
	 */
	@CheckReturnValue
	public double getRightVariance()
	{
		int n = mValues.length();
		if (n > 0)
		{
			double mean = getMean();
			double sum = 0;
			int count = 0;
			for (int i = 0; i < n; i++)
			{
				double value = mValues.get(i);
				if (value >= mean)
				{
					double diff = value - mean;
					sum += diff * diff;
					++count;
				}
			}

			if (count > 0)
			{
				return sum / count;
			}
		}

		return Double.NaN;
	}

	/**
	 * add value.
	 *
	 * @param value_ value to add.
	 */
	public void add(double value_)
	{
		mValues.push(value_);
		mSum += value_;
	}
}
