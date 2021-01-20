package borg.framework.auxiliaries;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

public final class Logger
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

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Fields
	//////////////////////////////////////////////////////////////////////////////////////////////////

	/** logger instance **/
	private static final java.util.logging.Logger sLogger =
		java.util.logging.Logger.getLogger("borg.framework");

	/** stack holder **/
	private static final Throwable sStackHolder = new Throwable();

	/**
	 *
	 **/
	private static String sRoot = null;

	/**
	 *
	 **/
	private static int sDepth = 0;

	/** is stack ready **/
	private static boolean sStackReady = false;

	/** started sessions. Map from thread ID to the session name **/
	private static final Map<Long, String> sSessions = new HashMap<>();

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Methods
	//////////////////////////////////////////////////////////////////////////////////////////////////

	@Contract(pure = true)
	private Logger()
	{
		// private constructor to avoid instantiation
	}

	/**
	 * configure logger.
	 *
	 * @param root_  root class to log.
	 * @param depth_ exceptions depth.
	 * @param level_ minimum log level.
	 * @param file_  log output file to add, if {@code null} then all handlers will be removed.
	 */
	public static void configure(@Nullable Class<?> root_,
		int depth_,
		@NotNull Level level_,
		@Nullable String file_)
	{
		sLogger.setLevel(level_);

		if (root_ != null)
		{
			sRoot = root_.getName();
		}
		else
		{
			sRoot = null;
		}
		sDepth = depth_;
		if (file_ != null)
		{
			try
			{
				FileHandler handler = new FileHandler(file_);
				handler.setFormatter(new SimpleFormatter());
				sLogger.addHandler(handler);
			}
			catch (Exception e)
			{
				Logger.log(e);
			}
		}
		else
		{
			for (Handler handler : sLogger.getHandlers())
			{
				sLogger.removeHandler(handler);
				handler.close();
			}
		}
	}

	/**
	 * @return current session name.
	 */
	@Nullable
	public static String getSession()
	{
		return sSessions.get(Thread.currentThread().getId());
	}

	/**
	 * start session for the thread.
	 *
	 * @param session_ session to start, {@code null} to stop session.
	 */
	public static void startSession(@Nullable String session_)
	{
		long id = Thread.currentThread().getId();
		if (session_ != null)
		{
			sSessions.put(id, session_);
		}
		else
		{
			sSessions.remove(id);
		}
	}

	/**
	 * get stack trace as string.
	 *
	 * @param traceElements_ stack trace elements.
	 * @param start_         number of elements to skip.
	 *
	 * @return built string.
	 */
	@Contract("_, _ -> new")
	@NotNull
	public static String stackTrace(@NotNull StackTraceElement[] traceElements_, int start_)
	{
		StringBuilder builder = new StringBuilder();

		// get build stack trace
		int n = traceElements_.length - 1;
		for (int i = start_ + 1; i < n; ++i)
		{
			StackTraceElement element = traceElements_[i];
			if (element.getClassName().equals(sRoot) == false)
			{
				builder.append(element.getMethodName());
				builder.append('(');
				builder.append(element.getFileName());
				builder.append(':');
				builder.append(element.getLineNumber());
				builder.append(")\n");
			}
			else
			{
				break;
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
	@Contract(pure = true)
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
	@Contract(pure = true)
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
			builder.append(", cause: ");
			builder.append(cause);

			// get next cause
			e_ = cause;
			cause = cause.getCause();
			++i;
		}

		// append stack trace
		builder.append(";stack: ");
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
		// build stack trace
		buildStack();
		StackTraceElement[] stackTrace = sStackHolder.getStackTrace();
		String stack = stackTrace(stackTrace, 1);

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

		// build function
		String className = stackTrace[2].getClassName();
		String method = stackTrace[2].getMethodName();
		int line = stackTrace[2].getLineNumber();

		// build message
		String message = String.format("snapshot from: %s#%s:%d%s\n\n%s\n\n%s",
			className,
			method,
			line,
			message_ == null? "": " - " + message_,
			state == null? "": state,
			stack);

		// log
		log(level_, message);
	}

	/**
	 * log message.
	 *
	 * @param message_ message to log.
	 */
	public static void log(@NotNull Object message_)
	{
		buildStack();
		log(Level.INFO, message_);
	}

	/**
	 * log message.
	 *
	 * @param level_   log level.
	 * @param message_ message to log.
	 */
	public static void log(@NotNull Level level_, @NotNull Object message_)
	{
		buildStack();
		String session = sSessions.get(Thread.currentThread().getId());
		StackTraceElement element = sStackHolder.getStackTrace()[2];
		sLogger.log(level_, String.format("%s:%d, %s\n%s%s\n",
			element.getClassName(),
			element.getLineNumber(),
			Thread.currentThread().getName(),
			session != null? session + "\n\n": "",
			message_));
		sStackReady = false;
	}

	/**
	 * log exception.
	 *
	 * @param e_ exception to log.
	 */
	public static void log(@NotNull Throwable e_)
	{
		log((String)null, e_);
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
		String session = sSessions.get(Thread.currentThread().getId());
		StackTraceElement element = sStackHolder.getStackTrace()[2];
		sLogger.log(Level.SEVERE, String.format("%s:%d\n%s%s\n",
			element.getClassName(),
			element.getLineNumber(),
			session != null? session + "\n\n": "",
			message_), e_);
		sStackReady = false;
	}

	/**
	 * assertion log.
	 *
	 * @param condition_ condition to test.
	 * @param message_   message to log if the condition is false.
	 */
	public static void log(boolean condition_, @NotNull Object message_)
	{
		if (condition_ == false)
		{
			log(Level.SEVERE, message_);
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
}
