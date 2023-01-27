package borg.framework.collections;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

public class LongArray extends PrimitiveArray<Long>
{
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
	@Unmodifiable
	public long @NotNull [] getContent()
	{
		return mBuffer;
	}

	@Contract(pure = true)
	public long @NotNull [] extractContent()
	{
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
		// if not enough space in the buffer
		if (mBuffer.length == mIndex)
		{
			// reallocate buffer
			long[] buffer = new long[(int)(mBuffer.length * MULTIPLIER_BUFFER)];
			System.arraycopy(mBuffer, 0, buffer, 0, mBuffer.length);
			mBuffer = buffer;
		}

		// write long
		mBuffer[mIndex] = b_;
		++mIndex;
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
}
