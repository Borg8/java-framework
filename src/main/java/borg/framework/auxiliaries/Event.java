package borg.framework.auxiliaries;

import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;


/**
 * @author Borg
 */
public final class Event<T>
{
	/*************************************************************************************************
	 * Constants
	 ************************************************************************************************/

	/*************************************************************************************************
	 * Observer
	 ************************************************************************************************/

	@FunctionalInterface
	public interface Observer<T>
	{
		boolean action(Object sender_, T param_);
	}

	/*************************************************************************************************
	 * Fields
	 ************************************************************************************************/

	/** owner of the event  **/
	private final Object mOwner;

	/** list of observers attached to the event **/
	private final Set<Observer<T>> mObservers;

	/** clone list of observers to maintain the main list **/
	private final Set<Observer<T>> mObserversClone;

	/** sign whether observers list is dirty **/
	private boolean mIsObserversDirty;

	/** sign whether observers invocation is executed **/
	private boolean mIsDuringInvocation;

	/*************************************************************************************************
	 * Methods
	 ************************************************************************************************/

	public Event(@Nullable Object owner_)
	{
		mOwner = owner_;
		mObservers = new HashSet<>();
		mObserversClone = new HashSet<>();
		mIsObserversDirty = false;
		mIsDuringInvocation = false;
	}

	/**
	 * @return number of observers that observe that event.
	 */
	@CheckReturnValue
	public int getSize()
	{
		return mObservers.size();
	}

	/**
	 * attaches a new observer to the event.
	 *
	 * @param observer_ the attached observer.
	 */
	public void attach(@NotNull Event.Observer<T> observer_)
	{
		synchronized (this)
		{
			// if new observer was attached
			if (mObservers.add(observer_) == true)
			{
				// if invocation is not executed
				if (mIsDuringInvocation == false)
				{
					// add observer to observers clone list
					mObserversClone.add(observer_);
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
			if (mObservers.remove(observer_) == true)
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
			for (Observer<T> observer : mObserversClone)
			{
				// invoke method
				try
				{
					if (observer.action(mOwner, param_) == false)
					{
						detach(observer);
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
				mObserversClone.addAll(mObservers);
			}

			// roll back during invocation flag
			mIsDuringInvocation = prevDuringInvocation;
		}

		return exception;
	}
}
