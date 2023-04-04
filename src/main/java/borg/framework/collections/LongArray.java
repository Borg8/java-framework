package borg.framework.collections;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;

public class LongArray extends PrimitiveArray<Long>
{
	@Serial
	private static final long serialVersionUID = 1;

	/** buffer to write to **/
	private long[] mBuffer;

	public LongArray()
	{
		this(MIN_SIZE_BUFFER);
	}

	public LongArray(int capacity_)
	{
		mBuffer = new long[capacity_];
	}

	public LongArray(long @NotNull ... elements_)
	{
		mBuffer = elements_;
		mIndex = elements_.length;
	}

	@Contract(pure = true)
	public long get(int ix_)
	{
		if (ix_ < mIndex)
		{
			return mBuffer[ix_];
		}

		throw new ArrayIndexOutOfBoundsException(String.format("index %d of %d", ix_, mIndex));
	}

	@Contract(pure = true)
	public long last()
	{
		return mBuffer[mIndex - 1];
	}

	@Contract(pure = true)
	public long @NotNull [] getContent()
	{
		return mBuffer;
	}

	@Contract(pure = true)
	public long @NotNull [] extractContent()
	{
		if (mIndex == mBuffer.length)
		{
			return mBuffer;
		}

		long[] content = new long[mIndex];

		if (mIndex > 0)
		{
			System.arraycopy(mBuffer, 0, content, 0, mIndex);
			mBuffer = content;
		}

		return content;
	}

	public void push(long b_)
	{
		// write long
		_ensureSize(mIndex + 1);
		mBuffer[mIndex] = b_;
		++mIndex;
	}

	public void push(long @NotNull [] longs_)
	{
		// write longs
		int length = mIndex + longs_.length;
		_ensureSize(length);
		System.arraycopy(longs_, 0, mBuffer, mIndex, longs_.length);
		mIndex = length;
	}

	@Contract(pure = true)
	public long pop()
	{
		--mIndex;
		return mBuffer[mIndex];
	}

	@Override
	@Contract(pure = true)
	@NotNull
	public <S extends PrimitiveArray<Long>> S subArray(int fromIx_, int toIx_)
	{
		int length = toIx_ - fromIx_;
		LongArray subArray = new LongArray(length);
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
		mBuffer = new long[MIN_SIZE_BUFFER];
	}

	@Override
	@NotNull
	protected Long getObj(int ix_)
	{
		return mBuffer[ix_];
	}

	private void _ensureSize(int minSize_)
	{
		if (mBuffer.length < minSize_)
		{
			long[] buffer = new long[(int)Math.max(mBuffer.length * MULTIPLIER_BUFFER, minSize_)];
			System.arraycopy(mBuffer, 0, buffer, 0, mBuffer.length);
			mBuffer = buffer;
		}
	}
}
