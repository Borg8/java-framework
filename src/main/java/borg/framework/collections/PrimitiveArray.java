package borg.framework.collections;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.Iterator;

public abstract class PrimitiveArray<T> implements Serializable, Iterable<T>
{
	@Serial
	private static final long serialVersionUID = 1;

	protected static final int MIN_SIZE_BUFFER = 16;

	protected static final float MULTIPLIER_BUFFER = 1.2f;

	/** current buffer index **/
	protected int mIndex;

	@Contract(pure = true)
	public final int length()
	{
		return mIndex;
	}

	@Contract(pure = true)
	public final boolean isEmpty()
	{
		return mIndex == 0;
	}

	public abstract void removeRange(int from_, int to_);

	public abstract void clear();

	@Contract(pure = true)
	@NotNull
	protected abstract T getObj(int ix_);

	@Contract(pure = true)
	@NotNull
	public abstract <S extends PrimitiveArray<T>> S subArray(int fromIx_, int toIx_);

	@NotNull
	@Override
	public final Iterator<T> iterator()
	{
		return new Iterator<>()
		{
			private int ix = 0;

			@Override
			@Contract(pure = true)
			public boolean hasNext()
			{
				return ix < mIndex;
			}

			@Override
			@Contract(pure = true)
			@NotNull
			public T next()
			{
				return getObj(ix++);
			}
		};
	}

	@Override
	@Contract(pure = true)
	@NotNull
	public final String toString()
	{
		return "length: " + length();
	}
}
