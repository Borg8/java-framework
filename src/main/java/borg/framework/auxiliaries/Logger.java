package borg.framework.auxiliaries;

import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import borg.framework.services.TimeManager;

import static java.util.logging.Logger.getLogger;

public final class Logger
{
	/*************************************************************************************************
	 * Constants
	 ************************************************************************************************/

	public static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");

	/*************************************************************************************************
	 * LogsFormatter
	 ************************************************************************************************/

	public static final class LogsFormatter extends Formatter
	{
		@Override
		@NotNull
		public String format(@NotNull LogRecord record_)
		{
			return String.format("%s%s\n\n",
				record_.getMessage(),
				record_.getThrown() == null? "": exceptionLog(record_.getThrown()));
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

	/** stack holder **/
	private static final Throwable sStackHolder = new Throwable();

	/** root class to log **/
	private static Set<String> sRoots = null;

	/** maximum stack log depth **/
	private static int sDepth = 10;

	/** is stack ready **/
	private static boolean sStackReady = false;

	/** logs listeners **/
	private static final List<Listener> sListeners = new ArrayList<>();

	/*************************************************************************************************
	 * Methods
	 ************************************************************************************************/

	static
	{
		TIME_FORMAT.setTimeZone(TimeZone.getDefault());

		ConsoleHandler handler = new ConsoleHandler();
		handler.setLevel(Level.ALL);
		handler.setFormatter(new LogsFormatter());
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
				FileHandler handler = new FileHandler(file_);
				handler.setLevel(level_);
				handler.setFormatter(new LogsFormatter());
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
		buildStack();
		StackTraceElement[] stackTrace = sStackHolder.getStackTrace();
		String stack = stackTrace(stackTrace, 2);

		// create message
		String message = String.format("%s\n%s\n\n%s\n%s",
			message_,
			state == null? "": state,
			_systemDetails(),
			stack);

		// log
		_log(level_, message, null);
	}

	/**
	 * assertion log.
	 *
	 * @param condition_ condition to test.
	 * @param message_   message to log if the condition is {@code false}.
	 */
	public static void assertLog(boolean condition_, @NotNull String message_)
	{
		if (condition_ == false)
		{
			buildStack();
			log(Level.SEVERE, message_);
		}
	}

	/**
	 * log message.
	 *
	 * @param message_ message to log.
	 */
	public static void log(@NotNull String message_)
	{
		buildStack();
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
		buildStack();
		log(level_, message_, null);
	}

	/**
	 * log exception.
	 *
	 * @param level_ log level.
	 * @param e_     exception to log.
	 */
	public static void log(@NotNull Level level_, @NotNull Throwable e_)
	{
		buildStack();
		log(level_, null, e_);
	}

	/**
	 * log exception.
	 *
	 * @param e_ exception to log.
	 */
	public static void log(@NotNull Throwable e_)
	{
		buildStack();
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
		buildStack();
		log(Level.SEVERE, message_, e_);
	}

	/**
	 * assertion log.
	 *
	 * @param condition_ condition to test.
	 * @param message_   message to log if the condition is {@code false}.
	 */
	public static void log(boolean condition_, @NotNull String message_)
	{
		if (condition_ == false)
		{
			buildStack();
			log(Level.SEVERE, message_);
		}
	}

	/**
	 * log message.
	 *
	 * @param level_   log level.
	 * @param message_ message to log.
	 * @param e_       exception to log.
	 */
	public static void log(@NotNull Level level_, @Nullable String message_, @Nullable Throwable e_)
	{
		if (message_ == null)
		{
			message_ = "";
		}

		// add stack title to the log
		buildStack();
		StackTraceElement element = sStackHolder.getStackTrace()[2];
		long now = TimeManager.getRealTime();
		String message = String.format("%s: (%s) %s:%d (%s)\n%s",
			TIME_FORMAT.format(now),
			level_.getName(),
			element.getFileName(),
			element.getLineNumber(),
			_systemDetails(),
			message_);

		// log
		_log(level_, message, e_);
	}

	private static void _log(@NotNull Level level_, @NotNull String message_, @Nullable Throwable e_)
	{
		sStackReady = false;

		sLogger.log(level_, message_, e_);

		for (Listener listener : sListeners)
		{
			listener.log(level_, message_, e_);
		}
	}

	private static void buildStack()
	{
		if (sStackReady == false)
		{
			sStackHolder.fillInStackTrace();
			sStackReady = true;
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
		return String.format("%d-%s | %s",
			TimeManager.getTick() - GlobalsHolder.START_TIME,
			Thread.currentThread().getName(),
			TextParser.timestampToTime(GlobalsHolder.getUptime()));
	}
}
