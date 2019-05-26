package borg.framework.collections;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import borg.framework.resources.Constants;

public final class ThreadMap<K, V> implements Serializable
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

	/** map for each thread **/
	private final Map<Long, Map<K, V>> mMaps;

	/** map instance **/
	private final Map<K, V> mMap;

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Methods
	//////////////////////////////////////////////////////////////////////////////////////////////////

	public ThreadMap(@NonNull Map<K, V> map_)
	{
		mMaps = Collections.synchronizedMap(new HashMap<>());
		mMap = map_;
	}

	public void clear()
	{
		getMap().clear();
	}

	public boolean containsKey(@NonNull K key_)
	{
		return getMap().containsKey(key_);
	}

	public boolean containsValue(@NonNull V value_)
	{
		return getMap().containsValue(value_);
	}

	public Set<Entry<K, V>> entrySet()
	{
		return getMap().entrySet();
	}

	@Nullable
	public V get(@NonNull Object value_)
	{
		return getMap().get(value_);
	}

	public boolean isEmpty()
	{
		return getMap().isEmpty();
	}

	@NonNull
	public Set<K> keySet()
	{
		return getMap().keySet();
	}

	@Nullable
	public V put(@NonNull K key_, @NonNull V value_)
	{
		return getMap().put(key_, value_);
	}

	public void putAll(@NonNull Map<? extends K, ? extends V> map_)
	{
		getMap().putAll(map_);
	}

	@Nullable
	public V remove(@NonNull Object value_)
	{
		return getMap().remove(value_);
	}

	public int size()
	{
		return getMap().size();
	}

	@NonNull
	public Collection<V> values()
	{
		return getMap().values();
	}

	@SuppressWarnings("unchecked")
	@NonNull
	public Map<K, V> getMap()
	{
		long thread = Thread.currentThread().getId();
		Map<K, V> map = mMaps.get(thread);
		if (map == null)
		{
			try
			{
				// create new map
				Method clone = mMap.getClass().getDeclaredMethod("clone");
				map = (Map<K, V>)clone.invoke(mMap);
				mMaps.put(thread, map);
			}
			catch (Exception e)
			{
				throw new Error(e);
			}
		}

		return map;
	}
}
