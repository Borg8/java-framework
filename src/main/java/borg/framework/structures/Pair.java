package borg.framework.structures;

import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;

import borg.framework.Constants;

public final class Pair<T, S> implements Serializable
{
	@Serial
	private static final long serialVersionUID = Constants.VERSION_FRAMEWORK;

	public T key;

	public S value;

	@CheckReturnValue
	public Pair(T element1_, S element2_)
	{
		key = element1_;
		value = element2_;
	}

	@CheckReturnValue
	@NotNull
	@Override
	public String toString()
	{
		return String.format("%s -> %s", key, value);
	}
}
