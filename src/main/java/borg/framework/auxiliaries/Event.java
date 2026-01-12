package borg.framework.auxiliaries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Borg
 */
public final class Event<T, S>
{
	/*************************************************************************************************
	 * Constants
	 ************************************************************************************************/

	/*************************************************************************************************
	 * Observer
	 ************************************************************************************************/

	@FunctionalInterface
	public interface Observer<T, S>
	{
		boolean action(T sender_, S param_);
	}

	/*************************************************************************************************
	 * Fields
	 ************************************************************************************************/

	/** owner of the event **/
	private final T mOwner;

	/** list of observers attached to the event **/
	private final Set<Observer<T, S>> mObservers;

	/*************************************************************************************************
	 * Methods
	 ************************************************************************************************/

	public Event(@Nullable T owner_)
	{
		mOwner = owner_;
		mObservers = Collections.synchronizedSet(new HashSet<>());
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
	 * @param observer_ the attached observer.
	 */
	public void attach(@NotNull Event.Observer<T, S> observer_)
	{
		mObservers.add(observer_);
	}

	/**
	 * detach attached method of specified observer.
	 *
	 * @param observer_ detached observer.
	 */
	public void detach(@NotNull Observer<T, S> observer_)
	{
		mObservers.remove(observer_);
	}

	/**
	 * detaches all observers from the event.
	 */
	public void detachAll()
	{
		mObservers.clear();
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
	public Throwable invoke(@Nullable S param_)
	{
		Throwable exception = null;

		// invokes attached observers
		for (Observer<T, S> observer : new ArrayList<>(mObservers)) // TODO optimize
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

		return exception;
	}
}
