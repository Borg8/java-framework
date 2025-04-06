package borg.framework.structures.references;

import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;

public final class LongRef
{
	public long value;

	public LongRef(long value_)
	{
		value = value_;
	}

	@Override
	@CheckReturnValue
	@NotNull
	public String toString()
	{
		return Long.toString(value);
	}
}
