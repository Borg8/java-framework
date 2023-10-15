package com.borg.framework.collections;

import com.borg.framework.Constants;

import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class ThreadCollection<E> implements Iterable<E>, Serializable
{
	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Public Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	@Serial
	private static final long serialVersionUID = Constants.VERSION_FRAMEWORK;

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Definitions
	//////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Fields
	//////////////////////////////////////////////////////////////////////////////////////////////////

	/** collection for each thread **/
	private final Map<Long, Collection<E>> mCollections;

	/** collection instance **/
	private final Collection<E> mCollection;

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Methods
	//////////////////////////////////////////////////////////////////////////////////////////////////

	public ThreadCollection(@NotNull Collection<E> collection_)
	{
		mCollections = Collections.synchronizedMap(new HashMap<>());

		mCollection = collection_;
	}

	public boolean add(@NotNull E object_)
	{
		return getCollection().add(object_);
	}

	public boolean addAll(@NotNull Collection<? extends E> collection_)
	{
		return getCollection().addAll(collection_);
	}

	public void clear()
	{
		getCollection().clear();
	}

	public boolean contains(@NotNull E object_)
	{
		return getCollection().contains(object_);
	}

	public boolean containsAll(@NotNull Collection<E> map_)
	{
		return getCollection().containsAll(map_);
	}

	public boolean isEmpty()
	{
		return getCollection().isEmpty();
	}

	@NotNull
	@Override
	public Iterator<E> iterator()
	{
		return getCollection().iterator();
	}

	public boolean remove(@NotNull E object_)
	{
		return getCollection().remove(object_);
	}

	public boolean removeAll(@NotNull Collection<E> collection_)
	{
		return getCollection().removeAll(collection_);
	}

	public boolean retainAll(@NotNull Collection<E> collection_)
	{
		return getCollection().retainAll(collection_);
	}

	public int size()
	{
		return getCollection().size();
	}

	@NotNull
	@SuppressWarnings("unchecked")
	public E[] toArray()
	{
		return (E[])getCollection().toArray();
	}

	@NotNull
	public E[] toArray(@NotNull E[] array_)
	{
		return getCollection().toArray(array_);
	}

	@NotNull
	public Collection<E> getCollection()
	{
		return getCollection(Thread.currentThread().threadId());
	}
	
	
	@SuppressWarnings("unchecked")
	@NotNull
	public Collection<E> getCollection(long thread_)
	{
		Collection<E> collection = mCollections.get(thread_);
		if (collection == null)
		{
			try
			{
				// create new collection
				Method clone = mCollection.getClass().getDeclaredMethod("clone");
				collection = (Collection<E>)clone.invoke(mCollection);
				mCollections.put(thread_, collection);
			}
			catch (Exception e)
			{
				throw new Error(e);
			}
		}

		return collection;
	}
}
