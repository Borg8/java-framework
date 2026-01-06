package borg.framework.auxiliaries;

import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import borg.framework.services.Lock;
import borg.framework.services.TimeManager;

import static java.util.logging.Logger.getLogger;

public final class Logger
{
	/*************************************************************************************************
	 * Constants
	 ************************************************************************************************/

	/** logs date formatter **/
	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

	/** logs time formatter **/
	public static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");

	/*************************************************************************************************
	 * FileFormatter
	 ************************************************************************************************/

	public static final class FileFormatter extends Formatter
	{
		@Override
		@NotNull
		public String format(@NotNull LogRecord record_)
		{
			Object[] params = record_.getParameters();
			long time = (long)params[0];
			Throwable e = (Throwable)params[1];
			return String.format("%s %s%s",
				DATE_FORMAT.format(time),
				record_.getMessage(),
				e == null? "": exceptionLog(e));
		}
	}

	/*************************************************************************************************
	 * ConsoleFormatter
	 ************************************************************************************************/

	public static final class ConsoleFormatter extends Formatter
	{
		@Override
		@NotNull
		public String format(@NotNull LogRecord record_)
		{
			String message = record_.getMessage();
			Throwable e = (Throwable)record_.getParameters()[1];
			if (e != null)
			{
				message += exceptionLog(e);
			}
			return message;
		}
	}

	/*************************************************************************************************
	 * Listener
	 ************************************************************************************************/

	public interface Listener
	{
		/**
		 * log message received.
		 *
		 * @param level_   log level.
		 * @param message_ log message.
		 * @param e_       log exception.
		 */
		void log(@NotNull Level level_, @NotNull String message_, @Nullable Throwable e_);
	}

	/*************************************************************************************************
	 * Variables
	 ************************************************************************************************/

	/** logger instance **/
	private static final java.util.logging.Logger sLogger = getLogger("borg.framework");

	/** stack holders **/
	private static final Map<Long, Throwable> sStackHolders = new HashMap<>();

	/** stack holders lock **/
	private static final Lock sLock = new Lock();

	/** root class to log **/
	private static Set<String> sRoots = null;

	/** maximum stack log depth **/
	private static int sDepth = 10;

	/** logs listeners **/
	private static final List<Listener> sListeners = new ArrayList<>();

	/*************************************************************************************************
	 * Methods
	 ************************************************************************************************/

	static
	{
		DATE_FORMAT.setTimeZone(TimeZone.getDefault());
		TIME_FORMAT.setTimeZone(TimeZone.getDefault());

		ConsoleHandler handler = new ConsoleHandler();
		handler.setLevel(Level.ALL);
		handler.setFormatter(new FileFormatter());
		sLogger.addHandler(handler);
		sLogger.setUseParentHandlers(false);
	}

	private Logger()
	{
		// nothing to do here
	}

	/**
	 * configure logger.
	 *
	 * @param depth_ exceptions depth.
	 * @param level_ minimum log level.
	 * @param file_  log output file to add, if {@code null} then all handlers will be removed.
	 * @param roots_ root package to log.
	 */
	public static void configure(int depth_,
		@NotNull Level level_,
		@Nullable String file_,
		String... roots_)
	{
		sLogger.setLevel(level_);

		if ((roots_ != null) && (roots_.length > 0))
		{
			sRoots = Set.of(roots_);
		}
		else
		{
			sRoots = null;
		}
		sDepth = depth_;

		if (file_ != null)
		{
			try
			{
				sLogger.setUseParentHandlers(false);
				for (Handler handler : sLogger.getHandlers())
				{
					sLogger.removeHandler(handler);
				}

				Handler handler = new FileHandler(file_);
				handler.setLevel(level_);
				handler.setFormatter(new FileFormatter());
				sLogger.addHandler(handler);

				handler = new ConsoleHandler();
				handler.setLevel(level_);
				handler.setFormatter(new ConsoleFormatter());
				sLogger.addHandler(handler);
			}
			catch (Exception e)
			{
				throw new Error(e);
			}
		}

		log(String.format("log configured: %s\n%s\n%s",
			level_,
			file_ == null? "": file_,
			roots_ == null? "": String.join(", ", roots_)));
	}

	/**
	 * add log listener.
	 *
	 * @param listener_ listener to add.
	 */
	public static void addListener(@NotNull Listener listener_)
	{
		sListeners.add(listener_);
	}

	/**
	 * get stack trace as string.
	 *
	 * @param traceElements_ stack trace elements.
	 * @param start_         element to start from.
	 *
	 * @return built string.
	 */
	@Contract("_, _ -> new")
	@NotNull
	public static String stackTrace(StackTraceElement @NotNull [] traceElements_, int start_)
	{
		StringBuilder builder = new StringBuilder();

		// get build stack trace
		int n = traceElements_.length - 1;
		StackTraceElement filtred = null;
		for (int i = start_; i < n; ++i)
		{
			StackTraceElement element = traceElements_[i];
			boolean log = sRoots == null;
			if (log == false)
			{
				String name = element.getClassName();
				for (String root : sRoots)
				{
					if (name.startsWith(root))
					{
						log = true;
						break;
					}
				}
			}
			if (log)
			{
				// if element was filtered
				if (filtred != null)
				{
					_addElement(builder, filtred);
					filtred = null;
				}
				_addElement(builder, element);
			}
			else
			{
				filtred = element;
			}
		}

		return new String(builder);
	}

	/**
	 * build exception report.
	 *
	 * @param e_ exception.
	 *
	 * @return built string.
	 */
	@NotNull
	@CheckReturnValue
	public static String exceptionLog(@NotNull Throwable e_)
	{
		return exceptionLog(Thread.currentThread(), e_);
	}

	/**
	 * build exception report.
	 *
	 * @param thread_ thread where exception was occurred.
	 * @param e_      - exception.
	 *
	 * @return built string.
	 */
	@NotNull
	@CheckReturnValue
	public static String exceptionLog(@NotNull Thread thread_, @NotNull Throwable e_)
	{
		StringBuilder builder = new StringBuilder();

		// append thread
		builder.append("thread: \"");
		builder.append(thread_.getName());
		builder.append('"');

		// append exception
		builder.append(", exception: ");
		builder.append(e_);

		// add causes
		int i = 0;
		Throwable cause = e_.getCause();
		while ((cause != null) && (i < sDepth))
		{
			// append cause
			builder.append(",\n\ncause: ");
			builder.append(cause);

			// get next cause
			e_ = cause;
			cause = cause.getCause();
			++i;
		}

		// append stack trace
		builder.append("\n");
		builder.append(stackTrace(e_.getStackTrace(), 0));

		return builder.toString();
	}

	/**
	 * log snapshot.
	 *
	 * @param level_   snapshot level.
	 * @param message_ message to log.
	 * @param state_   snapshot variables state. Every odd object is an variable name, following even
	 *                 object is the variable value.
	 */
	public static void snapshot(@NotNull Level level_, @Nullable String message_, Object... state_)
	{
		if (message_ == null)
		{
			message_ = "";
		}

		// build state
		String state = null;
		if (state_ != null)
		{
			int n = state_.length;
			if (n > 0)
			{
				StringBuilder builder = new StringBuilder();
				for (int i = 0; i < n; i += 2)
				{
					builder.append(state_[i]);
					builder.append(": ");
					builder.append(state_[i + 1]);
					builder.append("\n");
				}
				state = new String(builder);
			}
		}

		// build stack trace
		_buildStack();
		StackTraceElement[] stackTrace = _getStack();
		String stack = stackTrace(stackTrace, 2);

		// create message
		long now = TimeManager.getRealTime();
		String message = String.format("%s: (%s) %s\n%s\n%s\n%s\n\n",
			TIME_FORMAT.format(now),
			level_,
			_systemDetails(),
			message_,
			state == null? "": state,
			stack);

		// log
		_log(level_, now, message, null);
	}

	/**
	 * assertion log.
	 *
	 * @param expected_ expected condition to be {@code true}.
	 * @param message_  message to log if the condition is not {@code false}.
	 */
	public static void assertLog(@Nullable Boolean expected_, @NotNull String message_)
	{
		if (Boolean.TRUE.equals(expected_) != true)
		{
			_buildStack();
			log(Level.SEVERE, String.format("assertion failed (%s): %s", expected_, message_));
		}
	}

	/**
	 * log message.
	 *
	 * @param message_ message to log.
	 */
	public static void log(@NotNull String message_)
	{
		_buildStack();
		log(Level.ALL, message_);
	}

	/**
	 * log message.
	 *
	 * @param level_   log level.
	 * @param message_ message to log.
	 */
	public static void log(@NotNull Level level_, @NotNull String message_)
	{
		_buildStack();
		log(level_, message_, null, 0);
	}

	/**
	 * log exception.
	 *
	 * @param level_ log level.
	 * @param e_     exception to log.
	 */
	public static void log(@NotNull Level level_, @NotNull Throwable e_)
	{
		_buildStack();
		log(level_, null, e_, 0);
	}

	/**
	 * log exception.
	 *
	 * @param e_ exception to log.
	 */
	public static void log(@NotNull Throwable e_)
	{
		_buildStack();
		log(Level.SEVERE, e_);
	}

	/**
	 * log exception.
	 *
	 * @param message_ message to log.
	 * @param e_       exception to log.
	 */
	public static void log(@Nullable String message_, @NotNull Throwable e_)
	{
		_buildStack();
		log(Level.SEVERE, message_, e_, 0);
	}

	/**
	 * log message.
	 *
	 * @param level_       log level.
	 * @param message_     message to log.
	 * @param e_           exception to log.
	 * @param stackOffset_ offset in the stack trace to start from.
	 */
	public static void log(@NotNull Level level_,
		@Nullable String message_,
		@Nullable Throwable e_,
		int stackOffset_)
	{
		if (message_ == null)
		{
			message_ = "";
		}

		// add stack title to the log
		_buildStack();
		StackTraceElement element = _getStack()[2 + stackOffset_];
		long now = TimeManager.getRealTime();
		String message = String.format("%s: (%s) %s:%d (%s)\n%s\n\n",
			TIME_FORMAT.format(now),
			level_.getName(),
			element.getFileName(),
			element.getLineNumber(),
			_systemDetails(),
			message_);

		// log
		_log(level_, now, message, e_);
	}

	private static void _log(@NotNull Level level_,
		long time_,
		@NotNull String message_,
		@Nullable Throwable e_)
	{
		sLogger.log(level_, message_, new Object[] { time_, e_ });

		sStackHolders.remove(Thread.currentThread().threadId());

		for (Listener listener : sListeners)
		{
			listener.log(level_, message_, e_);
		}
	}

	private static void _buildStack()
	{
		sLock.readLock();

		Throwable throwable = sStackHolders.get(Thread.currentThread().threadId());
		if (throwable == null)
		{
			throwable = new Throwable();
			throwable.fillInStackTrace();

			sLock.upgradeToWriteLock();
			sStackHolders.put(Thread.currentThread().threadId(), throwable);
			sLock.writeUnlock();
		}
		else
		{
			sLock.readUnlock();
		}
	}

	private static void _addElement(@NotNull StringBuilder builder_,
		@NotNull StackTraceElement element_)
	{
		String file = element_.getFileName();
		if (file != null)
		{
			builder_.append(element_.getMethodName());
			builder_.append('(');
			builder_.append(element_.getFileName());
			builder_.append(':');
			builder_.append(element_.getLineNumber());
			builder_.append(")\n");
		}
	}

	@NotNull
	@CheckReturnValue
	private static String _systemDetails()
	{
		Runtime runtime = Runtime.getRuntime();
		return String.format("%d-%s | %s | %d MB of %d MB",
			TimeManager.getTick() - GlobalsHolder.START_TIME,
			Thread.currentThread().getName(),
			TextParser.timestampToTime(GlobalsHolder.getUptime()),
			runtime.freeMemory() / 1048576,
			runtime.totalMemory() / 1048576);
	}

	@NotNull
	@CheckReturnValue
	private static StackTraceElement[] _getStack()
	{
		Throwable throwable = sStackHolders.get(Thread.currentThread().threadId());
		assert throwable != null;
		return throwable.getStackTrace();
	}
}
