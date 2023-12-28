package com.borg.framework.structures.references;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class DoubleRef
{
	public double value;

	public DoubleRef(double value_)
	{
		value = value_;
	}

	@Override
	@Contract(pure = true)
	@NotNull
	public String toString()
	{
		return Double.toString(value);
	}
}
