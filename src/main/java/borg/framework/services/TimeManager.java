package borg.framework.services;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TimeManager
{
	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Public Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	/** second duration **/
	public static final long SECOND = 1000;

	/** minute duration **/
	public static final long MINUTE = SECOND * 60;

	/** hour duration **/
	public static final long HOUR = MINUTE * 60;

	/** day duration **/
	public static final long DAY = HOUR * 24;

	/** week duration **/
	public static final long WEEK = DAY * 7;

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Definitions
	//////////////////////////////////////////////////////////////////////////////////////////////////

	@FunctionalInterface
	public interface Handler<T>
	{
		/**
		 * handle timer.
		 *
		 * @param time_  ID of the time.
		 * @param param_ timer parameter.
		 */
		void handle(int time_, T param_);
	}

	@FunctionalInterface
	public interface Clock
	{
		/**
		 * @return system time in milliseconds.
		 */
		@Contract(pure = true)
		long getTime();
	}

	private static final class Timer<T>
	{
		/** timer ID **/
		final int id;

		/** timer handler **/
		@NotNull
		final Handler<T> handler;

		/** timer parameters **/
		final T param;

		/** time when the timer should be executed **/
		final long timeToExecute;

		/** timeout node **/
		Node timeout;

		Timer(int id_,
			@NotNull Handler<T> handler_,
			T param_,
			long timeToExecute_)
		{
			id = id_;
			handler = handler_;
			param = param_;
			timeToExecute = timeToExecute_;
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Fields
	//////////////////////////////////////////////////////////////////////////////////////////////////

	/** active timers. Map from timer ID to the timer descriptor **/
	private static final Map<Integer, Timer<?>> sTimers = new HashMap<>();

	/** list of timeouts **/
	private final static LinkedList sTimeouts = new LinkedList();

	/** queue of timers to execute in current loop **/
	private final static List<Timer<?>> sQueue = new ArrayList<>();

	/** active asynchronous executor timers. Map from timer handler to the timer descriptor **/
	private static final Map<Handler<?>, Timer<?>> sAsyncExecutors = new HashMap<>();

	/** system clock function **/
	private static Clock sClock = null;

	/** system clock tick of current cycle **/
	private static long sTick = 0;

	/** real time offset **/
	private static long sTimeOffset = 0;

	/** last free timer ID **/
	private static int sLastId = 0;

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Methods
	//////////////////////////////////////////////////////////////////////////////////////////////////

	@Contract(pure = true)
	private TimeManager()
	{
		// private constructor to prevent instantiation
	}

	/**
	 * @return system time.
	 */
	public static long getSystemTime()
	{
		return sClock.getTime();
	}

	/**
	 * @return system clock tick of current cycle.
	 */
	public static long getTick()
	{
		return sTick;
	}

	/**
	 * @return real time.
	 */
	public static long getRealTime()
	{
		return sTimeOffset + getSystemTime();
	}

	/**
	 * set real time.
	 *
	 * @param time_ time to set.
	 */
	public static void setRealTime(long time_)
	{
		sTimeOffset = time_ - getSystemTime();
	}

	/**
	 * set clock.
	 *
	 * @param clock_ clock to set.
	 */
	public static void setClock(@NotNull Clock clock_)
	{
		sClock = clock_;
	}

	/**
	 * get remaining timer delay.
	 *
	 * @param timer_ ID of the timer which delay to get.
	 *
	 * @return how much time left until the timer execution or -1 if no such timer exists.
	 */
	public static long getDelay(int timer_)
	{
		// if timer found
		Timer<?> timer = sTimers.get(timer_);
		if (timer != null)
		{
			return getSystemTime() - timer.timeToExecute;
		}

		return -1;
	}

	/**
	 * create timer.
	 *
	 * @param delay_   delay after which the timer will be executed.
	 * @param handler_ timer execution handler.
	 *
	 * @return return ID of created timer.
	 */
	public static int create(long delay_, @NotNull Handler<Void> handler_)
	{
		return _create(delay_, handler_, null).id;
	}

	/**
	 * create timer.
	 *
	 * @param delay_   delay after which the timer will be executed.
	 * @param handler_ timer execution handler.
	 * @param param_   parameter that will be passed to the handler during the execution.
	 *
	 * @return return ID of created timer.
	 */
	public static <T> int create(long delay_, @NotNull Handler<T> handler_, @Nullable T param_)
	{
		return _create(delay_, handler_, param_).id;
	}

	/**
	 * delete timer.
	 *
	 * @param timer_ ID of the timer to delete.
	 *
	 * @return provided timer parameters, if found.
	 */
	@Nullable
	public static <T> T remove(int timer_)
	{
		Object param;
		synchronized (sTimeouts)
		{
			// remove timer
			Timer<?> timer = sTimers.remove(timer_);

			// if time found
			if (timer != null)
			{
				// store parameters
				param = timer.param;

				// remove timer
				sTimeouts.remove(timer.timeout);
			}
			else
			{
				param = null;
			}
		}

		//noinspection unchecked
		return (T)param;
	}

	/**
	 * execute handler asynchronously.
	 *
	 * @param delay_   delay before execution.
	 * @param handler_ handler to execute.
	 */
	public static void asyncExecute(long delay_, @NotNull Handler<Void> handler_)
	{
		asyncExecute(delay_, handler_, null);
	}

	/**
	 * execute handler asynchronously.
	 *
	 * @param delay_   delay before execution.
	 * @param handler_ handler to execute.
	 * @param param_   parameter that will be passed to the handler during the execution.
	 */
	public static <T> void asyncExecute(long delay_, @NotNull Handler<T> handler_, @Nullable T param_)
	{
		synchronized (sTimeouts)
		{
			// remove previous executor
			_cancelExecution(handler_);

			// create handler execution timer
			Timer<T> timer = _create(delay_, handler_, param_);

			// add executor
			sAsyncExecutors.put(handler_, timer);
		}
	}

	/**
	 * cancel execution.
	 *
	 * @param handler_ handler to cancel.
	 */
	public static void cancel(@NotNull Handler<?> handler_)
	{
		synchronized (sTimeouts)
		{
			_cancelExecution(handler_);
		}
	}

	/**
	 * timer loop.
	 */
	public static void  loop()
	{
		// update system tick
		sTick = getSystemTime();

		// copy relevant timers
		int n = 0;
		synchronized (sTimeouts)
		{
			while (sTimeouts.first != null)
			{
				// if timer is relevant
				Timer<?> timer = sTimeouts.first.timer;
				if (timer.timeToExecute <= sTick)
				{
					// if queue should be expanded
					if (sQueue.size() <= n)
					{
						// expand queue
						sQueue.add(null);
					}

					// add timer to the queue
					sQueue.set(n, timer);
					++n;

					// remove timer
					sTimeouts.remove(sTimeouts.first);
					sTimers.remove(timer.id);

					// if execute for the timer defined
					Timer<?> executor = sAsyncExecutors.get(timer.handler);
					if ((executor != null) && (executor.id == timer.id))
					{
						sAsyncExecutors.remove(timer.handler);
					}
				}
				else
				{
					break;
				}
			}
		}

		// invoke timers
		for (int i = 0; i < n; ++i)
		{
			// get timer
			//noinspection unchecked
			Timer<Object> timer = (Timer<Object>)sQueue.get(i);

			// invoke handler
			timer.handler.handle(timer.id, timer.param);
		}
	}

	@NotNull
	@Contract("_, _, _ -> new")
	private static <T> Timer<T> _create(long delay_, @NotNull Handler<T> handler_, T param_)
	{
		// create timer
		long timeToExecute = getSystemTime() + delay_;
		Timer<T> timer;

		// insert timeout
		synchronized (sTimeouts)
		{
			timer = new Timer<>(sLastId, handler_, param_, timeToExecute);
			++sLastId;

			Node node;
			for (node = sTimeouts.first; node != null; node = node.next)
			{
				// if later timer found
				Timer<?> current = node.timer;
				if (current.timeToExecute >= timeToExecute)
				{
					break;
				}
			}

			// if the timer is latest
			if (node == null)
			{
				timer.timeout = sTimeouts.insertAfter(null, timer);
			}
			else
			{
				timer.timeout = sTimeouts.insertBefore(node, timer);
			}

			// set timer
			sTimers.put(timer.id, timer);
		}

		return timer;
	}

	private static void _cancelExecution(@NotNull Handler<?> handler_)
	{
		// remove handler executor
		Timer<?> timer = sAsyncExecutors.remove(handler_);

		// if handler already scheduled
		if (timer != null)
		{
			// delete timer
			remove(timer.id);
		}
	}

	private static final class Node
	{
		Node prev;
		Node next;
		Timer<?> timer;
	}

	private static final class LinkedList
	{
		Node first = null;
		Node last = null;

		@NotNull
		Node insertBefore(@Nullable Node node_, @NotNull Timer<?> timer_)
		{
			// locate next node
			Node next;
			if (node_ == null)
			{
				next = first;
			}
			else
			{
				next = node_;
			}

			// create node
			Node node = new Node();
			node.timer = timer_;

			// set next pointer
			node.next = next;

			// if next node exists
			if (next != null)
			{
				// if previous node is exists
				if (next.prev != null)
				{
					next.prev.next = node;
				}

				node.prev = next.prev;
				next.prev = node;
			}
			else
			{
				// set node as first
				node.prev = null;

				// create last node
				last = node;
			}

			// if next node is the first
			if (next == first)
			{
				first = node;
			}

			return node;
		}

		@NotNull
		Node insertAfter(@Nullable Node node_, @NotNull Timer<?> timer_)
		{
			// locate previous node
			Node prev;
			if (node_ == null)
			{
				prev = last;
			}
			else
			{
				prev = node_;
			}

			// create node
			Node node = new Node();
			node.timer = timer_;

			// set previous pointer
			node.prev = prev;

			// if previous node exists
			if (prev != null)
			{
				// if next node is exists
				if (prev.next != null)
				{
					prev.next.prev = node;
				}

				node.next = prev.next;
				prev.next = node;
			}
			else
			{
				// set node as last
				node.next = null;

				// create first
				first = node;
			}

			// if previous node is the last
			if (prev == last)
			{
				last = node;
			}

			return node;
		}

		@NotNull
		Timer<?> remove(@NotNull Node node_)
		{
			// if previous node exists
			if (node_.prev != null)
			{
				node_.prev.next = node_.next;
			}
			else
			{
				// set first node
				first = node_.next;
			}

			// if next node exists
			if (node_.next != null)
			{
				node_.next.prev = node_.prev;
			}
			else
			{
				// set last node
				last = node_.prev;
			}

			return node_.timer;
		}
	}
}
