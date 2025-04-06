package borg.framework.structures.references;

import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;

public final class Ref<T>
{
	public T value;

	public Ref()
	{
		this(null);
	}

	public Ref(T value_)
	{
		value = value_;
	}

	@Override
	@CheckReturnValue
	@NotNull
	public String toString()
	{
		if (value != null)
		{
			return value.toString();
		}

		return "";
	}
}
