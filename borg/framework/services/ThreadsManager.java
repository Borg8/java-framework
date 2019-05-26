package borg.framework.services;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import borg.framework.auxiliaries.Event;
import borg.framework.auxiliaries.Logging;
import borg.framework.auxiliaries.Messages;
import borg.framework.compability.Build;
import borg.framework.compability.Contract;

public final class ThreadsManager
{
	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Public Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	/** number of tasks that can run simultaneously **/
	public static final int MAX_OPEN_TASKS_NUM = 50;

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////


	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Definitions
	//////////////////////////////////////////////////////////////////////////////////////////////////

	public abstract static class Task
	{
		public enum State
		{
			/** unknown state **/
			UNKNOWN,

			/** task was created **/
			CREATED,

			/** task is in queue waiting for execute **/
			QUEUED,

			/** task is running **/
			RUNNING,

			/** task is interrupted **/
			INTERRUPTED,

			/** task finished **/
			FINISHED
		}

		/** task state was changed event, will invoked on task thread: argument - current state **/
		public final Event<State> stateChangedEvent;

		/** max number of running tasks where the thread may be started **/
		public final int maxTask;

		/** name of task thread **/
		@NonNull
		public final String name;

		/** task state **/
		@NonNull
		private State mState;

		/** parameters passed to task **/
		@Nullable
		private Object[] mParameters;

		/** thread that execute the task **/
		@Nullable
		private AsyncTask<Void, Void, Void> mExecuteThread;

		public Task()
		{
			this(null, MAX_OPEN_TASKS_NUM);
		}

		public Task(int maxTask_)
		{
			this(null, maxTask_);
		}

		public Task(@Nullable String name_)
		{
			this(name_, MAX_OPEN_TASKS_NUM);
		}

		public Task(@Nullable String name_, int maxTask_)
		{
			name = buildThreadName(name_);
			stateChangedEvent = new Event<>();
			maxTask = maxTask_;
			mState = State.CREATED;
			mParameters = null;
		}

		protected abstract void action();

		/**
		 * @return current task state.
		 */
		@NonNull
		@Contract(pure = true)
		public final State getState()
		{
			return mState;
		}

		/**
		 * @return get task parameters.
		 */
		@Contract(pure = true)
		public final Object[] getParameters()
		{
			return mParameters;
		}

		private final void setState(@NonNull State state_)
		{
			// set task state
			mState = state_;
			stateChangedEvent.invoke(mState);
		}

		private final void updateName()
		{
			Thread.currentThread().setName(name + " - " + mState);
		}

		private void setParameters(@Nullable Object[] params_)
		{
			mParameters = params_;
		}

		@Override
		protected void finalize() throws Throwable
		{
			super.finalize();

			runOnMainThread(stateChangedEvent::detachAll);
		}
	}

	private static final class Executor extends Handler
	{
		@Override
		public void handleMessage(Message msg_)
		{
			try
			{
				((Runnable)msg_.obj).run();
			}
			catch (Throwable e)
			{
				Logging.logging(Log.WARN, e);
			}
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Fields
	//////////////////////////////////////////////////////////////////////////////////////////////////

	/** number of open tasks **/
	private static volatile int sNOpenTasks;

	/** queue of tasks **/
	private static final List<Task> sTaskQueue = Collections.synchronizedList(new LinkedList<>());

	/** instance of main looper **/
	private static final Looper sMainLooper = Looper.getMainLooper();

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Methods
	//////////////////////////////////////////////////////////////////////////////////////////////////

	static
	{
		// create threads executor
		THREADS_EXECUTOR = new ThreadPoolExecutor(MAX_OPEN_TASKS_NUM,
			MAX_OPEN_TASKS_NUM,
			1,
			TimeUnit.SECONDS,
			new LinkedBlockingQueue<>(MAX_OPEN_TASKS_NUM));
	}

	private ThreadsManager()
	{
		// private constructor to avoid instantiation
	}

	/**
	 * initialize thread manager. This method must be called from main thread.
	 */
	public static void init()
	{
		// if not running from main thread
		if (Looper.getMainLooper() != Looper.myLooper())
		{
			throw new RuntimeException(Messages.exceptionOnlyMainThread());
		}
	}

	/**
	 * @return true if the method was executed from main thread, false otherwise.
	 */
	@Contract(pure = true)
	public static boolean isMainThread()
	{
		return Looper.myLooper() == sMainLooper;
	}

	/**
	 * starts asynchronous task if possible. If too many tasks already started, task will starts once other
	 * task will finished.
	 *
	 * @param task_   task to start.
	 * @param params_ parameters to pass to task.
	 */
	public static void startAsyncTask(@NonNull Task task_, Object... params_)
	{
		// set task parameters
		task_.setParameters(params_);

		// if number of created task achieved maximum
		if (sNOpenTasks >= task_.maxTask)
		{
			task_.setState(Task.State.QUEUED);

			// add task to task queue
			sTaskQueue.add(task_);
		}
		else
		{
			createTask(task_);
		}
	}

	/**
	 * run task synchronously on current thread.
	 *
	 * @param task_   task to start.
	 * @param params_ parameters to pass to task.
	 */
	public static void runTask(@NonNull Task task_, Object... params_)
	{
		// set task parameters
		task_.setParameters(params_);

		// run task
		task_.action();
	}

	/**
	 * interrupt the task. Task will only receive the interrupt signal.
	 *
	 * @param task_ task to interrupt.
	 */
	public static void interruptTask(@NonNull Task task_)
	{
		// if task was created
		if (task_.mExecuteThread != null)
		{
			// interrupt task thread
			task_.mExecuteThread.cancel(true);
		}

		// change task state
		task_.setState(Task.State.INTERRUPTED);
	}

	/**
	 * execute runnable of main thread. If called from main thread then will be executed synchronously.
	 *
	 * @param runnable_ runnable to execute.
	 */
	public static void runOnMainThread(@NonNull Runnable runnable_)
	{
		if (isMainThread() == false)
		{
			runOnMainThreadAsync(runnable_);
		}
		else
		{
			runnable_.run();
		}
	}

	/**
	 * execute runnable of main thread asynchronously/
	 *
	 * @param runnable_ runnable to execute.
	 */
	public static void runOnMainThreadAsync(@NonNull Runnable runnable_)
	{
		Message msg = new Message();
		msg.obj = runnable_;
		executeHandler.sendMessage(msg);
	}

	/**
	 * build thread name.
	 *
	 * @param message_ message to add to thread name, or null if no message has to be added.
	 *
	 * @return built string.
	 */
	@NonNull
	@Contract(pure = true)
	public static String buildThreadName(@Nullable String message_)
	{
		// get stack trace element
		StackTraceElement ste = Thread.currentThread().getStackTrace()[5];

		StringBuilder builder = new StringBuilder();
		builder.append(MessageFormat.format("file: {0}, line: {1}",
			ste.getFileName(),
			ste.getLineNumber()));

		// if message is not null
		if (message_ != null)
		{
			builder.append(MessageFormat.format(", message: {0}", message_));
		}

		return builder.toString();
	}

	private static void startNextTask()
	{
		// if task queue is not empty
		if (sTaskQueue.isEmpty() == false)
		{
			// start next task
			Task task = sTaskQueue.remove(0);
			startAsyncTask(task, task.mParameters);
		}
	}

	private static void createTask(@NonNull final Task task_)
	{
		// create execution thread
		task_.mExecuteThread = new AsyncTask<Void, Void, Void>()
		{
			@Override
			protected Void doInBackground(Void... params)
			{
				// set thread details
				task_.setState(Task.State.RUNNING);
				task_.updateName();

				// set priority
				if (task_.maxTask == 1)
				{
					Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
				}

				try
				{
					// do task
					task_.action();
				}
				catch (Throwable e)
				{
					Logging.logging(Log.WARN, task_.name, e);
				}

				// set thread details
				task_.setState(Task.State.FINISHED);
				task_.updateName();

				// decrease number of running tasks
				synchronized (sTaskQueue)
				{
					--sNOpenTasks;
				}

				// notify awaiting on task
				synchronized (task_)
				{
					task_.notifyAll();
				}

				// start new task
				runOnMainThread(finishTaskHandler);

				return null;
			}
		};

		// start the task
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		{
			task_.mExecuteThread.executeOnExecutor(THREADS_EXECUTOR);
		}
		else
		{
			task_.mExecuteThread.execute();
		}

		synchronized (sTaskQueue)
		{
			++sNOpenTasks;
		}
	}

	private static final Runnable finishTaskHandler = ThreadsManager::startNextTask;

	private static final Executor executeHandler = new Executor();
}
