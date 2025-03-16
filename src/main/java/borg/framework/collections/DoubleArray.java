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

	public DoubleArray(double @NotNull ... elements_)
	{
		mBuffer = elements_;
		mIndex = elements_.length;
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

	public void insert(int index_, double d_)
	{
		// insert double
		ensureSize(mIndex + 1);
		System.arraycopy(mBuffer, index_, mBuffer, index_ + 1, mIndex - index_);
		mBuffer[index_] = d_;
		++mIndex;
	}

	public void push(double d_)
	{
		// write double
		ensureSize(mIndex + 1);
		mBuffer[mIndex] = d_;
		++mIndex;
	}

	public void push(double @NotNull [] doubles_, int offset_, int length_)
	{
		// write bytes
		int length = mIndex + length_;
		ensureSize(length);
		System.arraycopy(doubles_, offset_, mBuffer, mIndex, length_);
		mIndex = length;
	}

	public void push(@NotNull DoubleArray array_)
	{
		// write array
		int n = array_.length();
		int length = mIndex + n;
		ensureSize(length);
		System.arraycopy(array_.mBuffer, 0, mBuffer, mIndex, n);
		mIndex = length;
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
	public <S extends PrimitiveArray<Double>> S subArray(int from_, int to_)
	{
		int length = to_ - from_;
		DoubleArray subArray = new DoubleArray(length);
		System.arraycopy(mBuffer, from_, subArray.mBuffer, 0, length);
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

	protected void ensureSize(int size_)
	{
		if (mBuffer.length < size_)
		{
			double[] buffer = new double[(int)Math.max(mBuffer.length * MULTIPLIER_BUFFER, size_)];
			System.arraycopy(mBuffer, 0, buffer, 0, mBuffer.length);
			mBuffer = buffer;
		}
	}
}
