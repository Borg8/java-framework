package borg.framework.services;

import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MemCache<K, V>
{
	/*************************************************************************************************
	 * Entry
	 ************************************************************************************************/

	private class Entry
	{
		/** key of the entry **/
		final K key;

		/** value of the entry **/
		V value;

		/** previous entry linked in the list **/
		Entry prev;

		/** next entry linked in the list **/
		Entry next;

		Entry(@NotNull K key_)
		{
			key = key_;
		}
	}

	/*************************************************************************************************
	 * Fields
	 ************************************************************************************************/

	/** cache map **/
	private final Map<K, Entry> mCache;

	/** maximum cache size **/
	private final int mSize;

	/** head of the linked list **/
	private Entry mHead;

	/** tail of the linked list **/
	private Entry mTail;

	/*************************************************************************************************
	 * Methods
	 ************************************************************************************************/

	public MemCache(int size_)
	{
		mCache = new HashMap<>();
		mSize = size_;
	}

	/**
	 * get value from the cache by key.
	 *
	 * @param key_ key to search for in the cache.
	 *
	 * @return value associated with the key, or {@code null} if not found.
	 */
	@CheckReturnValue
	@Nullable
	public V get(@NotNull K key_)
	{
		synchronized (mCache)
		{
			Entry entry = mCache.get(key_);
			if (entry != null)
			{
				_moveToTail(entry);
				return entry.value;
			}
		}

		return null;
	}

	/**
	 * set value in the cache by key.
	 *
	 * @param key_   key to associate with the value.
	 * @param value_ value to store in the cache.
	 */
	public void set(@NotNull K key_, @NotNull V value_)
	{
		synchronized (mCache)
		{
			// if entry already exists
			Entry entry = mCache.get(key_);
			if (entry != null)
			{
				// update value and move to tail
				entry.value = value_;
				_moveToTail(entry);
			}
			else
			{
				// if cache is full
				if (mCache.size() >= mSize)
				{
					// evict the oldest entry
					_evictOldest();
				}

				// create new entry
				entry = new Entry(key_);
				entry.value = value_;
				mCache.put(key_, entry);
				_addToTail(entry);
			}
		}
	}

	private void _moveToTail(@NotNull Entry entry_)
	{
		if (entry_ == mTail)
		{
			return;
		}

		_removeEntry(entry_);
		_addToTail(entry_);
	}

	private void _removeEntry(@NotNull Entry entry_)
	{
		if (entry_.prev != null)
		{
			entry_.prev.next = entry_.next;
		}
		else
		{
			mHead = entry_.next;
		}

		if (entry_.next != null)
		{
			entry_.next.prev = entry_.prev;
		}
		else
		{
			mTail = entry_.prev;
		}

		entry_.prev = null;
		entry_.next = null;
	}

	private void _addToTail(@NotNull Entry entry_)
	{
		if (mTail != null)
		{
			mTail.next = entry_;
			entry_.prev = mTail;
		}
		else
		{
			mHead = entry_;
		}
		mTail = entry_;
	}

	private void _evictOldest()
	{
		if (mHead != null)
		{
			mCache.remove(mHead.key);
			_removeEntry(mHead);
		}
	}
}
