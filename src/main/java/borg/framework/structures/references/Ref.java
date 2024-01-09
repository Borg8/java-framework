package borg.framework.structures.references;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class Ref<T>
{
	public T value;

	public Ref(T value_)
	{
		value = value_;
	}

	@Override
	@Contract(pure = true)
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
