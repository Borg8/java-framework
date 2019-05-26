package borg.framework.structures;

import java.io.Serializable;

import org.eclipse.jdt.annotation.NonNull;

import borg.framework.resources.Constants;

public final class Pair<T extends Serializable, S extends Serializable> implements Serializable
{
	private static final long serialVersionUID = Constants.VERSION_FRAMEWORK;

	public T el1;

	public S el2;

	public Pair(T element1_, S element2_)
	{
		el1 = element1_;
		el2 = element2_;
	}

	@NonNull
	@Override
	public String toString()
	{
		return "{e1: " + el1 + ", e2: " + el2 + "}";
	}
}
