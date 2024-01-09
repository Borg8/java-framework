package borg.framework.structures;

import borg.framework.Constants;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;

public final class Pair<T, S> implements Serializable
{
	@Serial
	private static final long serialVersionUID = Constants.VERSION_FRAMEWORK;

	public T el1;

	public S el2;

	@Contract(pure = true)
	public Pair(T element1_, S element2_)
	{
		el1 = element1_;
		el2 = element2_;
	}

	@Contract(pure = true)
	@NotNull
	@Override
	public String toString()
	{
		return "{e1: " + el1 + ", e2: " + el2 + "}";
	}
}
