package borg.framework.collections;

import org.jetbrains.annotations.CheckReturnValue;
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

	@CheckReturnValue
	public long get(int ix_)
	{
		if (ix_ < mIndex)
		{
			return mBuffer[ix_];
		}

		throw new ArrayIndexOutOfBoundsException(String.format("index %d of %d", ix_, mIndex));
	}

	@CheckReturnValue
	public long last()
	{
		return mBuffer[mIndex - 1];
	}

	@CheckReturnValue
	public long @NotNull [] getContent()
	{
		return mBuffer;
	}

	@CheckReturnValue
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

	public void insert(int index_, long l_)
	{
		// insert long
		ensureSize(mIndex + 1);
		System.arraycopy(mBuffer, index_, mBuffer, index_ + 1, mIndex - index_);
		mBuffer[index_] = l_;
		++mIndex;
	}

	public void push(long l_)
	{
		// write long
		ensureSize(mIndex + 1);
		mBuffer[mIndex] = l_;
		++mIndex;
	}

	public void push(long @NotNull [] longs_, int offset_, int length_)
	{
		// write bytes
		int length = mIndex + length_;
		ensureSize(length);
		System.arraycopy(longs_, offset_, mBuffer, mIndex, length_);
		mIndex = length;
	}

	public void push(@NotNull LongArray array_)
	{
		// write array
		int n = array_.length();
		int length = mIndex + n;
		ensureSize(length);
		System.arraycopy(array_.mBuffer, 0, mBuffer, mIndex, n);
		mIndex = length;
	}

	@CheckReturnValue
	public long pop()
	{
		--mIndex;
		return mBuffer[mIndex];
	}

	@Override
	@CheckReturnValue
	@NotNull
	public <S extends PrimitiveArray<Long>> S subArray(int from_, int to_)
	{
		int length = to_ - from_;
		LongArray subArray = new LongArray(length);
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
		mBuffer = new long[MIN_SIZE_BUFFER];
	}

	@Override
	@NotNull
	protected Long getObj(int ix_)
	{
		return mBuffer[ix_];
	}

	protected void ensureSize(int size_)
	{
		if (mBuffer.length < size_)
		{
			long[] buffer = new long[(int)Math.max(mBuffer.length * MULTIPLIER_BUFFER, size_)];
			System.arraycopy(mBuffer, 0, buffer, 0, mBuffer.length);
			mBuffer = buffer;
		}
	}
}
