package borg.framework.collections;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

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
	@Unmodifiable
	public int @NotNull [] getContent()
	{
		return mBuffer;
	}

	@Contract(pure = true)
	public int @NotNull [] extractContent()
	{
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
		// if not enough space in the buffer
		if (mBuffer.length == mIndex)
		{
			// reallocate buffer
			int[] buffer = new int[(int)(mBuffer.length * MULTIPLIER_BUFFER)];
			System.arraycopy(mBuffer, 0, buffer, 0, mBuffer.length);
			mBuffer = buffer;
		}

		// write int
		mBuffer[mIndex] = b_;
		++mIndex;
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
}
