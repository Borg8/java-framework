package borg.framework.auxiliaries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

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
		/**
		 * @return {@code true} to continue listening, {@code false} to auto-detach.
		 */
		boolean action(T sender_, S param_);
	}

	/*************************************************************************************************
	 * Disposable
	 ************************************************************************************************/

	@FunctionalInterface
	public interface Disposable
	{
		void dispose();
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
		mObservers = new CopyOnWriteArraySet<>();
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
	@NotNull
	public Disposable attach(@NotNull Event.Observer<T, S> observer_)
	{
		mObservers.add(observer_);
		return () -> detach(observer_);
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
		for (Observer<T, S> observer : mObservers)
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
