package com.borg.framework.auxiliaries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Borg
 */
public final class Event<T>
{
	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Observer
	//////////////////////////////////////////////////////////////////////////////////////////////////

	@FunctionalInterface
	public interface Observer<T>
	{
		boolean action(T param_);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Observer
	//////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class Details<T>
	{
		/** observer owner **/
		@Nullable
		final Object owner;

		/** observer to execute **/
		final Observer<T> observer;

		Details(@Nullable Object owner_, @NotNull Observer<T> observer_)
		{
			owner = owner_;
			observer = observer_;
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Fields
	//////////////////////////////////////////////////////////////////////////////////////////////////

	/** list of observers attached to the event **/
	private final Map<Observer<T>, Details<T>> mObservers;

	/** clone list of observers to maintain the main list **/
	private final Map<Observer<T>, Details<T>> mObserversClone;

	/** sign whether observers list is dirty **/
	private boolean mIsObserversDirty;

	/** sign whether observers invocation is executed **/
	private boolean mIsDuringInvocation;

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Methods
	//////////////////////////////////////////////////////////////////////////////////////////////////

	public Event()
	{
		mObservers = new HashMap<>();
		mObserversClone = new HashMap<>();
		mIsObserversDirty = false;
		mIsDuringInvocation = false;
	}

	/**
	 * @return number of observers that observe that event.
	 */
	public int getSize()
	{
		return mObservers.size();
	}

	/**
	 * attaches a new observer to the event.
	 *
	 * @param owner_ observer owner.
	 * @param observer_ the attached observer.
	 */
	public void attach(@Nullable Object owner_, @NotNull Event.Observer<T> observer_)
	{
		synchronized (this)
		{
			// if observer was attached
			Details<T> details = new Details<>(owner_, observer_);
			if (mObservers.put(observer_, details) == null)
			{
				// if invocation is not executed
				if (mIsDuringInvocation == false)
				{
					// add observer to observers clone list
					mObserversClone.put(observer_, details);
				}
				else
				{
					// sign observers list as dirty
					mIsObserversDirty = true;
				}
			}
		}
	}

	/**
	 * detach attached method of specified observer.
	 *
	 * @param observer_ detached observer.
	 */
	public void detach(@NotNull Observer<T> observer_)
	{
		synchronized (this)
		{
			// if observer was removed
			if (mObservers.remove(observer_) != null)
			{
				// if invocation is not executed
				if (mIsDuringInvocation == false)
				{
					// remove observer from observers clone list
					mObserversClone.remove(observer_);
				}
				else
				{
					// sign observers list as dirty
					mIsObserversDirty = true;
				}
			}
		}
	}

	/**
	 * removes all observer of given owner.
	 *
	 * @param owner_ instance of object that owned the removed observers.
	 */
	public void detach(@NotNull Object owner_)
	{
		synchronized (this)
		{
			// detach all observers of the owner from observers list
			for (Details<T> observer : new ArrayList<>(mObservers.values()))
			{
				// if observer belongs to the owner
				if (observer.owner == owner_)
				{
					detach(observer.observer);
				}
			}
		}
	}

	/**
	 * detaches all observers from the event.
	 */
	public void detachAll()
	{
		synchronized (this)
		{
			mObservers.clear();

			// if invocation is not executed
			if (mIsDuringInvocation == false)
			{
				// remove all observers
				mObserversClone.clear();
			}
			else
			{
				// sign observers list as dirty
				mIsObserversDirty = true;
			}
		}
	}

	/**
	 * invokes all attached observers.
	 *
	 * @param param_ parameters to pass to observers.
	 *
	 * @return first occurred exception or {@code null} if no exception occurred during the
	 * invocation.
	 */
	@Nullable
	public Throwable invoke(@Nullable T param_)
	{
		Throwable exception = null;

		synchronized (this)
		{
			boolean prevDuringInvocation = mIsDuringInvocation;

			// set that invocation is executed
			mIsDuringInvocation = true;

			// invokes attached observers
			for (Details<T> observer : mObserversClone.values())
			{
				// invoke method
				try
				{
					if (observer.observer.action(param_) == false)
					{
						detach(observer.observer);
					}
				}
				catch (Throwable e)
				{
					if (exception == null)
					{
						exception = e;
					}
					Logger.log(e);
				}
			}

			// if observers list was changed
			if (mIsObserversDirty == true)
			{
				mObserversClone.clear();

				// build new observer clone list
				mObserversClone.putAll(mObservers);
			}

			// roll back during invocation flag
			mIsDuringInvocation = prevDuringInvocation;
		}

		return exception;
	}
}
