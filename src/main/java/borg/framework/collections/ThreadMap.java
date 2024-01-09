package borg.framework.collections;

import borg.framework.Constants;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class ThreadMap<K, V> implements Serializable
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

	/** map for each thread **/
	private final Map<Long, Map<K, V>> mMaps;

	/** map instance **/
	private final Map<K, V> mMap;

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Methods
	//////////////////////////////////////////////////////////////////////////////////////////////////

	public ThreadMap(@NotNull Map<K, V> map_)
	{
		mMaps = Collections.synchronizedMap(new HashMap<>());
		mMap = map_;
	}

	public void clear()
	{
		getMap().clear();
	}

	public boolean containsKey(@NotNull K key_)
	{
		return getMap().containsKey(key_);
	}

	public boolean containsValue(@NotNull V value_)
	{
		return getMap().containsValue(value_);
	}

	@NotNull
	public Set<Map.Entry<K, V>> entrySet()
	{
		return getMap().entrySet();
	}

	@Nullable
	public V get(@NotNull K value_)
	{
		return getMap().get(value_);
	}

	public boolean isEmpty()
	{
		return getMap().isEmpty();
	}

	@NotNull
	public Set<K> keySet()
	{
		return getMap().keySet();
	}

	@Nullable
	public V put(@NotNull K key_, @NotNull V value_)
	{
		return getMap().put(key_, value_);
	}

	public void putAll(@NotNull Map<? extends K, ? extends V> map_)
	{
		getMap().putAll(map_);
	}

	@Nullable
	public V remove(@NotNull K value_)
	{
		return getMap().remove(value_);
	}

	public int size()
	{
		return getMap().size();
	}

	@NotNull
	public Collection<V> values()
	{
		return getMap().values();
	}

	@SuppressWarnings("unchecked")
	@NotNull
	public Map<K, V> getMap()
	{
		long thread = Thread.currentThread().threadId();
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
