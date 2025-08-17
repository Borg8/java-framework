package borg.framework.services;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

import borg.framework.auxiliaries.Logger;

public class Lock
{
	/** read lock **/
	private final ReentrantReadWriteLock.ReadLock mReadLock;

	/** write lock **/
	private final ReentrantReadWriteLock.WriteLock mWriteLock;

	/** name of the lock, used in logging **/
	@Nullable
	private String mName;

	/** whether a stack trace should be logged **/
	private boolean mStack;

	public Lock()
	{
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
		mReadLock = lock.readLock();
		mWriteLock = lock.writeLock();
	}

	/**
	 * enable logging for the lock.
	 *
	 * @param name_  name of the lock, used in logging
	 * @param stack_ if {@code true}, stack trace will be printed.
	 */
	public void enableLogging(@NotNull String name_, boolean stack_)
	{
		mName = name_;
		mStack = stack_;
	}

	/**
	 * disable logging for the lock.
	 */
	public void disableLogging()
	{
		mName = null;
	}

	public void readLock()
	{
		_log("read lock");

		mReadLock.lock();
	}

	public void readUnlock()
	{
		_log("read unlock");

		mReadLock.unlock();
	}

	public void writeLock()
	{
		_log("write lock");

		mWriteLock.lock();
	}

	public void writeUnlock()
	{
		_log("write unlock");

		mWriteLock.unlock();
	}

	public void upgradeToWriteLock()
	{
		_log("upgrade to write lock");

		mReadLock.unlock();
		mWriteLock.lock();
	}

	public void downgradeToReadLock()
	{
		_log("downgrade to read lock");

		mWriteLock.unlock();
		mReadLock.lock();
	}

	private void _log(@NotNull String message_)
	{
		if (mName != null)
		{
			if (mStack)
			{
				Logger.snapshot(Level.FINE, mName + " " + message_);
			}
			else
			{
				Logger.log(Level.FINE, mName + " " + message_, null, 2);
			}
		}
	}
}
