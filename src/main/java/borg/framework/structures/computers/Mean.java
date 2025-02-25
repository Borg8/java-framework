package borg.framework.structures.computers;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class Mean
{
	/** summation of all values **/
	private double mSum;

	/** number of values **/
	private double mCounter;

	/** minimum value **/
	private double mMin;

	/** maximum  value **/
	private double mMax;

	public Mean()
	{
		reset();
	}

	@Contract(pure = true)
	public double getAverage()
	{
		if (mCounter > 0)
		{
			return mSum / mCounter;
		}

		return Double.NaN;
	}

	@Contract(pure = true)
	public double getCount()
	{
		return mCounter;
	}

	@Contract(pure = true)
	public double getSum()
	{
		return mSum;
	}

	@Contract(pure = true)
	public double getMin()
	{
		return mMin;
	}

	@Contract(pure = true)
	public double getMax()
	{
		return mMax;
	}

	public void add(double value_)
	{
		add(value_, 1);
	}

	public void add(double value_, double weight_)
	{
		mSum += value_;
		mCounter += weight_;

		if (mMax < value_)
		{
			mMax = value_;
		}

		if (mMin > value_)
		{
			mMin = value_;
		}
	}

	public void reset()
	{
		mSum = 0;
		mCounter = 0;
		mMin = Double.POSITIVE_INFINITY;
		mMax = Double.NEGATIVE_INFINITY;
	}

	@Override
	@Contract(pure = true)
	@NotNull
	public String toString()
	{
		return String.format("%f (%f - %f)", getAverage(), getMin(), getMax());
	}
}
