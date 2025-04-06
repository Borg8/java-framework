package borg.framework.structures.references;

import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;

public final class DoubleRef
{
	public double value;

	public DoubleRef(double value_)
	{
		value = value_;
	}

	@Override
	@CheckReturnValue
	@NotNull
	public String toString()
	{
		return Double.toString(value);
	}
}
