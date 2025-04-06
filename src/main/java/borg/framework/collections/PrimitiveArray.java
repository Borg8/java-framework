package borg.framework.collections;

import org.jetbrains.annotations.CheckReturnValue;
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

	@CheckReturnValue
	public final int length()
	{
		return mIndex;
	}

	@CheckReturnValue
	public final boolean isEmpty()
	{
		return mIndex == 0;
	}

	public final void allocate(int capacity_)
	{
		ensureSize(mIndex + capacity_);
	}

	// TODO test from_ to_ bounds
	public abstract void removeRange(int from_, int to_);

	public abstract void clear();

	// TODO test from_ to_ bounds
	@CheckReturnValue
	@NotNull
	public abstract <S extends PrimitiveArray<T>> S subArray(int from_, int to_);

	@CheckReturnValue
	@NotNull
	protected abstract T getObj(int ix_);

	protected abstract void ensureSize(int size_);

	@NotNull
	@Override
	public final Iterator<T> iterator()
	{
		return new Iterator<>()
		{
			private int ix = 0;

			@Override
			@CheckReturnValue
			public boolean hasNext()
			{
				return ix < mIndex;
			}

			@Override
			@CheckReturnValue
			@NotNull
			public T next()
			{
				return getObj(ix++);
			}
		};
	}

	@Override
	@CheckReturnValue
	@NotNull
	public final String toString()
	{
		return "length: " + length();
	}
}
