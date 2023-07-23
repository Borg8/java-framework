package com.borg.framework.structures.references;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class IntRef
{
	public int value;

	public IntRef(int value_)
	{
		value = value_;
	}

	@Override
	@Contract(pure = true)
	@NotNull
	public String toString()
	{
		return Integer.toString(value);
	}
}
