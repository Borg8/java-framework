package borg.framework.collections;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

public class DoubleArray
{
	private static final int MIN_SIZE_BUFFER = 16;

	private static final float MULTIPLIER_BUFFER = 1.5f;

	/** buffer to write to **/
	private double[] mBuffer;

	/** current buffer index **/
	private int mIndex;

	public DoubleArray()
	{
		this(MIN_SIZE_BUFFER);
	}

	public DoubleArray(int capacity_)
	{
		mBuffer = new double[capacity_];
	}

	@Contract(pure = true)
	@Unmodifiable
	public double @NotNull [] getContent()
	{
		return mBuffer;
	}

	@Contract(pure = true)
	public double @NotNull [] extractContent()
	{
		double[] content = new double[mIndex];

		if (mIndex > 0)
		{
			System.arraycopy(mBuffer, 0, content, 0, mIndex);
			mBuffer = content;
		}

		return content;
	}

	@Contract(pure = true)
	public int length()
	{
		return mIndex;
	}

	public void add(double b_)
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

	public void clear()
	{
		mIndex = 0;
	}
}
