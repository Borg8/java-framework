package borg.framework.structures;

import org.intellij.lang.annotations.Flow;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

public class RangeArrayList<T> extends ArrayList<T>
{
	public RangeArrayList(@NotNull @Flow(sourceIsContainer = true) Collection<? extends T> collection_)
	{
		super(collection_);
	}

	public RangeArrayList()
	{
		super();
	}

	public RangeArrayList(int capacity_)
	{
		super(capacity_);
	}

	public void removeRange(int from_, int to_)
	{
		super.removeRange(from_, to_);
	}
}

