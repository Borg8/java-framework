package borg.framework.auxiliaries;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

public final class Logging
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

	private static final Logger sLogger = Logger.getAnonymousLogger();

	private static String sRoot = null;

	private static int sDepth = 0;

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Methods
	//////////////////////////////////////////////////////////////////////////////////////////////////

	private Logging()
	{
		// private constructor to avoid instantiation
	}

	/**
	 * configure logger.
	 * 
	 * @param root_ root class to log.
	 * @param depth_ exceptions depth.
	 */
	public static void configure(@NonNull Class<?> root_, int depth_)
	{
		sRoot = root_.getName();
		sDepth = depth_;
	}

	/**
	 * get stack trace as string.
	 *
	 * @param traceElements_ stack trace elements.
	 * @param start_ number of elements to skip.
	 * @return built string.
	 */
	@NonNull
	public static String buildStackTrace(@NonNull StackTraceElement[] traceElements_, int start_)
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
	@NonNull
	public static String buildExceptionReport(@NonNull Throwable e_)
	{
		return buildExceptionReport(Thread.currentThread(), e_);
	}

	/**
	 * build exception report.
	 *
	 * @param thread_ thread where exception was occurred.
	 * @param e_ - exception.
	 *
	 * @return built string.
	 */
	@NonNull
	public static String buildExceptionReport(@NonNull Thread thread_, @NonNull Throwable e_)
	{
		StringBuilder builder = new StringBuilder();

		// append thread
		builder.append("thread: \"");
		builder.append(thread_.getName());
		builder.append('"');

		// append exception
		builder.append(", exception: ");
		builder.append(e_.toString());

		// add causes
		int i = 0;
		Throwable cause = e_.getCause();
		while ((cause != null) && (i < sDepth))
		{
			// append cause
			builder.append(", cause: ");
			builder.append(cause.toString());

			// get next cause
			e_ = cause;
			cause = cause.getCause();
			++i;
		}

		// append stack trace
		builder.append(";stack: ");
		builder.append(buildStackTrace(e_.getStackTrace(), 0));

		return builder.toString();
	}

	/**
	 * log snapshot.
	 * 
	 * @param level_ snapshot level.
	 * @param message_ message to log.
	 * @param state_ snapshot variables state. Every odd object is an variable name, following even
	 *          object is the variable value.
	 */
	public static void snapshot(@NonNull Level level_, @Nullable String message_, Object... state_)
	{
		// build stack trace
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		String stack = buildStackTrace(stackTrace, 1);

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
		String message = MessageFormat.format("snapshot from: {0}#{1}:{2}{3}\n\n{4}\n\n{5}",
			className,
			method,
			Integer.toString(line),
			message_ == null? "": " - " + message_,
			state == null? "": state,
			stack);

		// log
		logging(level_, message);
	}

	/**
	 * log message.
	 * 
	 * @param message_ message to log.
	 */
	public static void logging(@NonNull Object message_)
	{
		logging(Level.INFO, message_);
	}

	/**
	 * log message.
	 * 
	 * @param level_ log level.
	 * @param message_ message to log.
	 */
	public static void logging(@NonNull Level level_, @NonNull Object message_)
	{
		sLogger.log(level_, "\n" + message_.toString());
	}

	/**
	 * log exception.
	 * 
	 * @param e_ exception to log.
	 */
	public static void logging(@NonNull Throwable e_)
	{
		logging((String)null, e_);
	}

	/**
	 * log exception.
	 * 
	 * @param message_ message to log.
	 * @param e_ exception to log.
	 */
	public static void logging(@Nullable String message_, @NonNull Throwable e_)
	{
		sLogger.log(Level.SEVERE, "\n" + message_, e_);
	}
}
