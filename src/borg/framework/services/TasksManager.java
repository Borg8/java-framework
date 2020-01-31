package borg.framework.services;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import borg.framework.auxiliaries.Logging;

public class TasksManager
{
	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Public Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	private static final int BUFFER = 256;

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

	public static final class Descriptor<T>
	{
		/** task to run **/
		public final Task<T> task;

		/** task parameter **/
		@Nullable
		public final T param;

		Descriptor(@NotNull Task<T> task_, @Nullable T param_)
		{
			task = task_;
			param = param_;
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Fields
	//////////////////////////////////////////////////////////////////////////////////////////////////

	/** queue of tasks to run on the main thread **/
	private static final List<Descriptor<?>> sTasks = new ArrayList<>(BUFFER);

	/** main thread instance **/
	private static final Thread sMain = Thread.currentThread();

	/** thread mutex **/
	private static final Object sMutex = new Object();

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
	public static Thread runThread(@NotNull Task<Void> task_)
	{
		return runThread(task_, null);
	}

	/**
	 * run new task.
	 *
	 * @param task_  task to run.
	 * @param param_ parameter to pass to the task.
	 *
	 * @return task identifier or null if the task was not created.
	 */
	@NotNull
	@Contract("_, _ -> new")
	public static <T> Thread runThread(@NotNull Task<T> task_, @Nullable T param_)
	{
		Thread thread = new Thread(() ->
		{
			try
			{
				task_.run(param_);
			}
			catch (Throwable e)
			{
				Logging.logging(e);
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
			task_.run(param_);
		}
		else
		{
			// add task to queue
			addTask(task_, param_);
		}
	}

	/**
	 * @return next task to execute.
	 */
	@Contract(pure = true)
	@Nullable
	public static <T> Descriptor<T> pollTask()
	{
		if (sTasks.isEmpty() == false)
		{
			synchronized (sMutex)
			{
				//noinspection unchecked
				return (Descriptor<T>)sTasks.remove(0);
			}
		}

		return null;
	}

	private static <T> void addTask(@NotNull Task<T> task_, @Nullable T param_)
	{
		synchronized (sMutex)
		{
			sTasks.add(new Descriptor<>(task_, param_));
		}
	}
}
