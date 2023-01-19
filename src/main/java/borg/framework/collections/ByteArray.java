package borg.framework.collections;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

public class ByteArray
{
	private static final int MIN_SIZE_BUFFER = 16;

	private static final float MULTIPLIER_BUFFER = 1.5f;

	/** buffer to write to **/
	private byte[] mBuffer;

	/** current buffer index **/
	private int mIndex;

	public ByteArray()
	{
		this(MIN_SIZE_BUFFER);
	}

	public ByteArray(int capacity_)
	{
		mBuffer = new byte[capacity_];
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

	@Contract(pure = true)
	public int length()
	{
		return mIndex;
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

	public void clear()
	{
		mIndex = 0;
	}
}
