package borg.framework.collections;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;

import borg.framework.resources.Constants;

public final class ThreadCollection<E> implements Iterable<E>, Serializable
{
	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Public Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

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

	public ThreadCollection(@NonNull Collection<E> collection_)
	{
		mCollections = Collections.synchronizedMap(new HashMap<>());

		mCollection = collection_;
	}

	public boolean add(@NonNull E object_)
	{
		return getCollection().add(object_);
	}

	public boolean addAll(@NonNull Collection<? extends E> collection_)
	{
		return getCollection().addAll(collection_);
	}

	public void clear()
	{
		getCollection().clear();
	}

	public boolean contains(@NonNull E object_)
	{
		return getCollection().contains(object_);
	}

	public boolean containsAll(@NonNull Collection<?> map_)
	{
		return getCollection().containsAll(map_);
	}

	public boolean isEmpty()
	{
		return getCollection().isEmpty();
	}

	@NonNull
	@Override
	public Iterator<E> iterator()
	{
		return getCollection().iterator();
	}

	public boolean remove(@NonNull E object_)
	{
		return getCollection().remove(object_);
	}

	public boolean removeAll(@NonNull Collection<?> collection_)
	{
		return getCollection().removeAll(collection_);
	}

	public boolean retainAll(@NonNull Collection<?> collection_)
	{
		return getCollection().retainAll(collection_);
	}

	public int size()
	{
		return getCollection().size();
	}

	@NonNull
	@SuppressWarnings("unchecked")
	public E[] toArray()
	{
		return (E[])getCollection().toArray();
	}

	@NonNull
	public E[] toArray(@NonNull E[] array_)
	{
		return getCollection().toArray(array_);
	}

	@NonNull
	public Collection<E> getCollection()
	{
		return getCollection(Thread.currentThread().getId());
	}
	
	
	@SuppressWarnings("unchecked")
	@NonNull
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
