package com.borg.framework.structures.references;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class LongRef
{
	public long value;

	public LongRef(long value_)
	{
		value = value_;
	}

	@Override
	@Contract(pure = true)
	@NotNull
	public String toString()
	{
		return Long.toString(value);
	}
}
