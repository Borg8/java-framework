package borg.framework.collections;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

public class ByteArray extends PrimitiveArray<Byte>
{
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

	public ByteArray(byte... data_)
	{
		mBuffer = data_;
		mIndex = mBuffer.length;
	}

	@Contract(pure = true)
	public byte get(int ix_)
	{
		if (ix_ < mIndex)
		{
			return mBuffer[ix_];
		}

		throw new ArrayIndexOutOfBoundsException(String.format("%d of maximum %d", ix_, mIndex - 1));
	}

	@Contract(pure = true)
	public byte last()
	{
		return mBuffer[mIndex - 1];
	}

	@Contract(pure = true)
	@Unmodifiable
	public byte @NotNull [] getContent()
	{
		return mBuffer;
	}

	@Contract(pure = true)
	public byte @NotNull [] extractContent()
	{
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
		// if not enough space in the buffer
		if (mBuffer.length == mIndex)
		{
			// reallocate buffer
			byte[] buffer = new byte[(int)(mBuffer.length * MULTIPLIER_BUFFER)];
			System.arraycopy(mBuffer, 0, buffer, 0, mBuffer.length);
			mBuffer = buffer;
		}

		// write byte
		mBuffer[mIndex] = b_;
		++mIndex;
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

}
