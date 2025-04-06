package borg.framework.structures.references;

import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;

public final class IntRef
{
	public int value;

	public IntRef(int value_)
	{
		value = value_;
	}

	@Override
	@CheckReturnValue
	@NotNull
	public String toString()
	{
		return Integer.toString(value);
	}
}
