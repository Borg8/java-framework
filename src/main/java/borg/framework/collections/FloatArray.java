package borg.framework.collections;

import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;

public class FloatArray extends PrimitiveArray<Float>
{
	@Serial
	private static final long serialVersionUID = 1;

	/** buffer to write to **/
	private float[] mBuffer;

	public FloatArray()
	{
		this(MIN_SIZE_BUFFER);
	}

	public FloatArray(int capacity_)
	{
		mBuffer = new float[capacity_];
	}

	public FloatArray(float @NotNull ... elements_)
	{
		mBuffer = elements_;
		mIndex = elements_.length;
	}

	@CheckReturnValue
	public float get(int ix_)
	{
		if (ix_ < mIndex)
		{
			return mBuffer[ix_];
		}

		throw new ArrayIndexOutOfBoundsException(String.format("index %d of %d", ix_, mIndex));
	}

	@CheckReturnValue
	public float last()
	{
		return mBuffer[mIndex - 1];
	}

	@CheckReturnValue
	public float @NotNull [] getContent()
	{
		return mBuffer;
	}

	@CheckReturnValue
	public float @NotNull [] extractContent()
	{
		if (mIndex == mBuffer.length)
		{
			return mBuffer;
		}

		float[] content = new float[mIndex];

		if (mIndex > 0)
		{
			System.arraycopy(mBuffer, 0, content, 0, mIndex);
			mBuffer = content;
		}

		return content;
	}

	public void insert(int index_, float f_)
	{
		// insert float
		ensureSize(mIndex + 1);
		System.arraycopy(mBuffer, index_, mBuffer, index_ + 1, mIndex - index_);
		mBuffer[index_] = f_;
		++mIndex;
	}

	public void push(float f_)
	{
		// write float
		ensureSize(mIndex + 1);
		mBuffer[mIndex] = f_;
		++mIndex;
	}

	public void push(float @NotNull [] floats, int offset_, int length_)
	{
		// write bytes
		int length = mIndex + length_;
		ensureSize(length);
		System.arraycopy(floats, offset_, mBuffer, mIndex, length_);
		mIndex = length;
	}

	public void push(@NotNull FloatArray array_)
	{
		// write array
		int n = array_.length();
		int length = mIndex + n;
		ensureSize(length);
		System.arraycopy(array_.mBuffer, 0, mBuffer, mIndex, n);
		mIndex = length;
	}

	@CheckReturnValue
	public float pop()
	{
		--mIndex;
		return mBuffer[mIndex];
	}

	@Override
	@CheckReturnValue
	@NotNull
	public <S extends PrimitiveArray<Float>> S subArray(int from_, int to_)
	{
		int length = to_ - from_;
		FloatArray subArray = new FloatArray(length);
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
		mBuffer = new float[MIN_SIZE_BUFFER];
	}

	@Override
	@NotNull
	protected Float getObj(int ix_)
	{
		return mBuffer[ix_];
	}

	protected void ensureSize(int size_)
	{
		if (mBuffer.length < size_)
		{
			float[] buffer = new float[(int)Math.max(mBuffer.length * MULTIPLIER_BUFFER, size_)];
			System.arraycopy(mBuffer, 0, buffer, 0, mBuffer.length);
			mBuffer = buffer;
		}
	}
}
