package com.borg.framework.collections;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;

public class ByteArray extends PrimitiveArray<Byte>
{
	@Serial
	private static final long serialVersionUID = 1;

	/** buffer to write to **/
	private byte[] mBuffer;

	public ByteArray()
	{
		this(MIN_SIZE_BUFFER);
	}

	public ByteArray(int capacity_)
	{
		mBuffer = new byte[capacity_];
	}

	public ByteArray(byte @NotNull ... elements_)
	{
		mBuffer = elements_;
		mIndex = elements_.length;
	}

	@Contract(pure = true)
	public byte get(int ix_)
	{
		if (ix_ < mIndex)
		{
			return mBuffer[ix_];
		}

		throw new ArrayIndexOutOfBoundsException(String.format("index %d of %d", ix_, mIndex));
	}

	@Contract(pure = true)
	public byte last()
	{
		return mBuffer[mIndex - 1];
	}

	@Contract(pure = true)
	public byte @NotNull [] getContent()
	{
		return mBuffer;
	}

	@Contract(pure = true)
	public byte @NotNull [] extractContent()
	{
		if (mIndex == mBuffer.length)
		{
			return mBuffer;
		}

		byte[] content = new byte[mIndex];

		if (mIndex > 0)
		{
			System.arraycopy(mBuffer, 0, content, 0, mIndex);
			mBuffer = content;
		}

		return content;
	}

	public void push(byte b_)
	{
		// write byte
		_ensureSize(mIndex + 1);
		mBuffer[mIndex] = b_;
		++mIndex;
	}

	public void push(byte @NotNull [] bytes_)
	{
		// write bytes
		int length = mIndex + bytes_.length;
		_ensureSize(length);
		System.arraycopy(bytes_, 0, mBuffer, mIndex, bytes_.length);
		mIndex = length;
	}

	@Contract(pure = true)
	public byte pop()
	{
		--mIndex;
		return mBuffer[mIndex];
	}

	@Override
	@Contract(pure = true)
	@NotNull
	public <S extends PrimitiveArray<Byte>> S subArray(int fromIx_, int toIx_)
	{
		int length = toIx_ - fromIx_;
		ByteArray subArray = new ByteArray(length);
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
		mBuffer = new byte[MIN_SIZE_BUFFER];
	}

	@Override
	@NotNull
	protected Byte getObj(int ix_)
	{
		return mBuffer[ix_];
	}

	private void _ensureSize(int minSize_)
	{
		if (mBuffer.length < minSize_)
		{
			byte[] buffer = new byte[(int)Math.max(mBuffer.length * MULTIPLIER_BUFFER, minSize_)];
			System.arraycopy(mBuffer, 0, buffer, 0, mBuffer.length);
			mBuffer = buffer;
		}
	}
}
