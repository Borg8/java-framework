package borg.framework.collections;

import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;

public class IntArray extends PrimitiveArray<Integer>
{
	@Serial
	private static final long serialVersionUID = 1;

	/** buffer to write to **/
	private int[] mBuffer;

	public IntArray()
	{
		this(MIN_SIZE_BUFFER);
	}

	public IntArray(int capacity_)
	{
		mBuffer = new int[capacity_];
	}

	public IntArray(int @NotNull ... elements_)
	{
		mBuffer = elements_;
		mIndex = elements_.length;
	}

	@CheckReturnValue
	public int get(int ix_)
	{
		if (ix_ < mIndex)
		{
			return mBuffer[ix_];
		}

		throw new ArrayIndexOutOfBoundsException(String.format("index %d of %d", ix_, mIndex));
	}

	@CheckReturnValue
	public int last()
	{
		return mBuffer[mIndex - 1];
	}

	@CheckReturnValue
	public int @NotNull [] getContent()
	{
		return mBuffer;
	}

	@CheckReturnValue
	public int @NotNull [] extractContent()
	{
		if (mIndex == mBuffer.length)
		{
			return mBuffer;
		}

		int[] content = new int[mIndex];

		if (mIndex > 0)
		{
			System.arraycopy(mBuffer, 0, content, 0, mIndex);
			mBuffer = content;
		}

		return content;
	}

	public void insert(int index_, int i_)
	{
		// insert int
		ensureSize(mIndex + 1);
		System.arraycopy(mBuffer, index_, mBuffer, index_ + 1, mIndex - index_);
		mBuffer[index_] = i_;
		++mIndex;
	}

	public void push(int i_)
	{
		// write int
		ensureSize(mIndex + 1);
		mBuffer[mIndex] = i_;
		++mIndex;
	}

	public void push(int @NotNull [] ints_, int offset_, int length_)
	{
		// write bytes
		int length = mIndex + length_;
		ensureSize(length);
		System.arraycopy(ints_, offset_, mBuffer, mIndex, length_);
		mIndex = length;
	}

	public void push(@NotNull IntArray array_)
	{
		// write array
		int n = array_.length();
		int length = mIndex + n;
		ensureSize(length);
		System.arraycopy(array_.mBuffer, 0, mBuffer, mIndex, n);
		mIndex = length;
	}

	@CheckReturnValue
	public int pop()
	{
		--mIndex;
		return mBuffer[mIndex];
	}

	@Override
	@CheckReturnValue
	@NotNull
	public <S extends PrimitiveArray<Integer>> S subArray(int from_, int to_)
	{
		int length = to_ - from_;
		IntArray subArray = new IntArray(length);
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
		mBuffer = new int[MIN_SIZE_BUFFER];
	}

	@Override
	@NotNull
	protected Integer getObj(int ix_)
	{
		return mBuffer[ix_];
	}

	protected void ensureSize(int size_)
	{
		if (mBuffer.length < size_)
		{
			int[] buffer = new int[(int)Math.max(mBuffer.length * MULTIPLIER_BUFFER, size_)];
			System.arraycopy(mBuffer, 0, buffer, 0, mBuffer.length);
			mBuffer = buffer;
		}
	}
}
