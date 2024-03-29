package borg.framework.collections;

import org.jetbrains.annotations.Contract;
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

	@Contract(pure = true)
	public int get(int ix_)
	{
		if (ix_ < mIndex)
		{
			return mBuffer[ix_];
		}

		throw new ArrayIndexOutOfBoundsException(String.format("index %d of %d", ix_, mIndex));
	}

	@Contract(pure = true)
	public int last()
	{
		return mBuffer[mIndex - 1];
	}

	@Contract(pure = true)
	public int @NotNull [] getContent()
	{
		return mBuffer;
	}

	@Contract(pure = true)
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

	public void push(int b_)
	{
		// write int
		_ensureSize(mIndex + 1);
		mBuffer[mIndex] = b_;
		++mIndex;
	}

	public void push(int @NotNull [] ints_)
	{
		// write ints
		int length = mIndex + ints_.length;
		_ensureSize(length);
		System.arraycopy(ints_, 0, mBuffer, mIndex, ints_.length);
		mIndex = length;
	}

	@Contract(pure = true)
	public int pop()
	{
		--mIndex;
		return mBuffer[mIndex];
	}

	@Override
	@Contract(pure = true)
	@NotNull
	public <S extends PrimitiveArray<Integer>> S subArray(int fromIx_, int toIx_)
	{
		int length = toIx_ - fromIx_;
		IntArray subArray = new IntArray(length);
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
		mBuffer = new int[MIN_SIZE_BUFFER];
	}

	@Override
	@NotNull
	protected Integer getObj(int ix_)
	{
		return mBuffer[ix_];
	}

	private void _ensureSize(int minSize_)
	{
		if (mBuffer.length < minSize_)
		{
			int[] buffer = new int[(int)Math.max(mBuffer.length * MULTIPLIER_BUFFER, minSize_)];
			System.arraycopy(mBuffer, 0, buffer, 0, mBuffer.length);
			mBuffer = buffer;
		}
	}
}
