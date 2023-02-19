package borg.framework.collections;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;

public class DoubleArray extends PrimitiveArray<Double>
{
	@Serial
	private static final long serialVersionUID = 1;

	/** buffer to write to **/
	private double[] mBuffer;

	public DoubleArray()
	{
		this(MIN_SIZE_BUFFER);
	}

	public DoubleArray(int capacity_)
	{
		mBuffer = new double[capacity_];
	}

	@Contract(pure = true)
	public double get(int ix_)
	{
		if (ix_ < mIndex)
		{
			return mBuffer[ix_];
		}

		throw new ArrayIndexOutOfBoundsException(String.format("index %d of %d", ix_, mIndex));
	}

	@Contract(pure = true)
	public double last()
	{
		return mBuffer[mIndex - 1];
	}

	@Contract(pure = true)
	public double @NotNull [] getContent()
	{
		return mBuffer;
	}

	@Contract(pure = true)
	public double @NotNull [] extractContent()
	{
		if (mIndex == mBuffer.length)
		{
			return mBuffer;
		}

		double[] content = new double[mIndex];

		if (mIndex > 0)
		{
			System.arraycopy(mBuffer, 0, content, 0, mIndex);
			mBuffer = content;
		}

		return content;
	}

	public void push(double b_)
	{
		// if not enough space in the buffer
		if (mBuffer.length == mIndex)
		{
			// reallocate buffer
			double[] buffer = new double[(int)(mBuffer.length * MULTIPLIER_BUFFER)];
			System.arraycopy(mBuffer, 0, buffer, 0, mBuffer.length);
			mBuffer = buffer;
		}

		// write double
		mBuffer[mIndex] = b_;
		++mIndex;
	}

	@Contract(pure = true)
	public double pop()
	{
		--mIndex;
		return mBuffer[mIndex];
	}

	@Override
	@Contract(pure = true)
	@NotNull
	public <S extends PrimitiveArray<Double>> S subArray(int fromIx_, int toIx_)
	{
		int length = toIx_ - fromIx_;
		DoubleArray subArray = new DoubleArray(length);
		System.arraycopy(mBuffer, fromIx_, subArray.mBuffer, 0, length);
		subArray.mIndex = length;

		//noinspection unchecked
		return (S)subArray;
	}

	@Override
	public void removeRange(int from_, int to_)
	{
		System.arraycopy(mBuffer, to_, mBuffer, from_, mIndex - to_);
		mIndex -= to_ - from_;
	}

	@Override
	public void clear()
	{
		mIndex = 0;
		mBuffer = new double[MIN_SIZE_BUFFER];
	}

	@Override
	@NotNull
	protected Double getObj(int ix_)
	{
		return mBuffer[ix_];
	}
}
