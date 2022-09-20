package borg.framework.services;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import borg.framework.auxiliaries.Logger;

public class TasksManager
{
	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Public Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Definitions
	//////////////////////////////////////////////////////////////////////////////////////////////////

	@FunctionalInterface
	public interface Task<T>
	{
		/**
		 * run task.
		 *
		 * @param param_ task parameter.
		 */
		void run(T param_);
	}

	private static final class Descriptor<T>
	{
		/** task to run **/
		final Task<T> task;

		/** task session **/
		@Nullable
		final String session;

		/** task parameter **/
		@Nullable
		final T param;

		Descriptor(@NotNull Task<T> task_, @Nullable String session_, @Nullable T param_)
		{
			task = task_;
			session = session_;
			param = param_;
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Fields
	//////////////////////////////////////////////////////////////////////////////////////////////////

	/** queue of tasks to run on the main thread **/
	private static final LinkedList<Descriptor<?>> sTasks = new LinkedList<>();

	/** running loopers. Map from the looper to tasks queue for the looper **/
	private static final Map<Object, LinkedList<Descriptor<?>>> sLoopers =
		Collections.synchronizedMap(new HashMap<>());

	/** is main loop sleep allowed **/
	private static boolean sSleep = true;

	/** main thread instance **/
	private static final Thread sMain = Thread.currentThread();

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Methods
	//////////////////////////////////////////////////////////////////////////////////////////////////

	@Contract(pure = true)
	private TasksManager()
	{
		// private constructor to prevent instantiation
	}

	/**
	 * initialize the manager.
	 */
	public static void init()
	{
		// nothing to do here
	}

	/**
	 * run new task.
	 *
	 * @param task_ task to run.
	 *
	 * @return task identifier or null if the task was not created.
	 */
	@NotNull
	@Contract("_, -> new")
	public static Thread runOnThread(@NotNull Task<Void> task_)
	{
		return runOnThread(task_, null);
	}

	/**
	 * run new task.
	 *
	 * @param task_  task to run.
	 * @param param_ parameter to pass to the task.
	 *
	 * @return task identifier or {@code null} if the task was not created.
	 */
	@NotNull
	@Contract("_, _ -> new")
	public static <T> Thread runOnThread(@NotNull Task<T> task_, @Nullable T param_)
	{
		String session = Logger.getSession();
		Thread thread = new Thread(() ->
		{
			// run the task
			try
			{
				Logger.startSession(session);
				task_.run(param_);
			}
			catch (Throwable e)
			{
				Logger.log(e);
			}
		});

		thread.start();
		return thread;
	}

	/**
	 * run task on main thread. If called from main thread, then the task will executed synchronously.
	 *
	 * @param task_ task to run.
	 */
	public static void runOnMain(@NotNull Task<Void> task_)
	{
		runOnMain(task_, null);
	}

	/**
	 * run task on main thread. If called from main thread, then the task will executed synchronously.
	 *
	 * @param task_  task to run.
	 * @param param_ task parameter.
	 */
	public static <T> void runOnMain(@NotNull Task<T> task_, @Nullable T param_)
	{
		// if on main
		if (Thread.currentThread() == sMain)
		{
			// run the task
			try
			{
				task_.run(param_);
			}
			catch (Throwable e)
			{
				throw new Error(e);
			}
		}
		else
		{
			// add task to queue
			addTask(task_, param_);
		}
	}

	/**
	 * start new looper.
	 *
	 * @return started looper.
	 */
	@Contract(pure = true)
	@NotNull
	public static Thread startLooper()
	{
		// create looper thread
		String session = Logger.getSession();
		Thread looper = new Thread(() ->
		{
			// poll task
			Thread thread = Thread.currentThread();
			thread.setName("looper " + thread.getId());

			Logger.startSession(session);
			for (; ; )
			{
				// get looper queue
				LinkedList<Descriptor<?>> queue;
				queue = sLoopers.get(thread);

				// if queue exists
				if (queue != null)
				{
					// execute queue
					for (; ; )
					{
						// get descriptor
						Descriptor<Object> descriptor;
						//noinspection SynchronizationOnLocalVariableOrMethodParameter
						synchronized (thread)
						{
							// if tasks exists
							//noinspection unchecked
							descriptor = (Descriptor<Object>)queue.pollFirst();
						}

						// if the descriptor exists
						if (descriptor != null)
						{
							// execute the task
							try
							{
								descriptor.task.run(descriptor.param);
							}
							catch (Throwable e)
							{
								Logger.log(e);
							}
							thread.setName("looper " + thread.getId());
						}
						else
						{
							break;
						}
					}

					// if the looper still exists
					if (sLoopers.containsKey(thread))
					{
						//noinspection SynchronizationOnLocalVariableOrMethodParameter
						synchronized (thread)
						{
							// wait for tasks
							try
							{
								thread.wait();
							}
							catch (InterruptedException e)
							{
								break;
							}
						}
					}
					else
					{
						break;
					}
				}
				else
				{
					break;
				}
			}

			Logger.log("looper: " + thread.getName() + " stopped");
		});

		// start looper
		sLoopers.put(looper, new LinkedList<>());
		looper.start();

		return looper;
	}

	/**
	 * stop running looper and dismiss all its tasks.
	 *
	 * @param looper_ looper to stop.
	 *
	 * @return {@code true} if the looper stopped, {@code false} if no such looper found.
	 */
	public static boolean stopLooper(@NotNull Thread looper_)
	{
		// remove looper
		List<Descriptor<?>> queue = sLoopers.remove(looper_);

		// if looper was removed
		boolean exists = queue != null;
		if (exists == true)
		{
			//noinspection SynchronizationOnLocalVariableOrMethodParameter
			synchronized (looper_)
			{
				looper_.notify();
			}
		}

		return exists;
	}

	/**
	 * run task on looper, if running from looper thread, then the task will be executed synchronously.
	 *
	 * @param looper_ looper to run the task on.
	 * @param task_   task to run.
	 *
	 * @return {@code true} if the task queued, {@code false} if no such looper found.
	 */
	public static boolean runOnLooper(@NotNull Thread looper_, @NotNull Task<Void> task_)
	{
		return runOnLooper(looper_, task_, null);
	}

	/**
	 * run task on looper, if running from looper thread, then the task will be executed synchronously.
	 *
	 * @param looper_ looper to run the task on.
	 * @param task_   task to run.
	 * @param param_  task parameter.
	 *
	 * @return {@code true} if the task queued, {@code false} if no such looper found.
	 */
	public static <T> boolean runOnLooper(@NotNull Thread looper_,
		@NotNull Task<T> task_,
		@Nullable T param_)
	{
		// if from looper thread
		String session = Logger.getSession();
		if (looper_ == Thread.currentThread())
		{
			// run the task
			try
			{
				Logger.startSession(session);
				task_.run(param_);
			}
			catch (Throwable e)
			{
				throw new Error(e);
			}

			return true;
		}

		// get looper queue
		LinkedList<Descriptor<?>> queue = sLoopers.get(looper_);

		// if the looper found
		if (queue != null)
		{
			// invoke looper
			//noinspection SynchronizationOnLocalVariableOrMethodParameter
			synchronized (looper_)
			{
				// add task to the queue
				queue.add(new Descriptor<>(task_, session, param_));

				// invoke looper
				looper_.notify();
			}
		}
		else
		{
			Logger.snapshot(Level.INFO, "no looper found",
				"name",
				looper_.getName(),
				"id",
				looper_.getId());
		}

		return queue != null;
	}

	/**
	 * main loop.
	 */
	public static void loop()
	{
		// start main loop
		//noinspection InfiniteLoopStatement
		for (; ; )
		{
			// TODO sleep in no tasks

			// loop timer
			long next = TimeManager.loop();

			// run main thread tasks
			runTask();

			synchronized (sTasks)
			{
				// if may sleep
				if ((sSleep == true) && (next != 0) && (sTasks.isEmpty()))
				{
					try
					{
						if (next < 0)
						{
							sTasks.wait();
						}
						else
						{
							sTasks.wait(next);
						}
					}
					catch (Exception e)
					{
						// nothing to do here
					}
				}
				else
				{
					sSleep = true;
				}
			}
		}
	}

	public static void continueLoop()
	{
		sSleep = false;
	}

	private static boolean runTask()
	{
		if (sTasks.isEmpty() == false)
		{
			// poll task
			Descriptor<Object> task;
			synchronized (sTasks)
			{
				//noinspection unchecked
				task = (Descriptor<Object>)sTasks.pollFirst();
			}

			try
			{
				// execute task
				assert task != null;
				Logger.startSession(task.session);
				task.task.run(task.param);
			}
			catch (Throwable e)
			{
				throw new Error(e);
			}

			return true;
		}

		return false;
	}

	private static <T> void addTask(@NotNull Task<T> task_, @Nullable T param_)
	{
		synchronized (sTasks)
		{
			sTasks.add(new Descriptor<>(task_, Logger.getSession(), param_));
			sTasks.notify();
		}
	}
}
