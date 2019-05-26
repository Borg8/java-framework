package borg.framework.auxiliaries;

import org.eclipse.jdt.annotation.NonNull;

public final class Messages
{
	private Messages()
	{
		// private constructor to avoid instantiation
	}

	/**
	 * build exception message - attempt to release released object.
	 *
	 * @return message.
	 */
	@NonNull
	public static String exceptionAlreadyReleased()
	{
		return "the singleton already released";
	}

	/**
	 * build exception message - unable to create directory.
	 *
	 * @return message.
	 */
	@NonNull
	public static String exceptionCannotCreateDirectory()
	{
		return "unable to create directory";
	}

	/**
	 * build exception message - connection failed.
	 *
	 * @return message.
	 */
	@NonNull
	public static String exceptionConnectionFailed()
	{
		return "connection failed";
	}

	/**
	 * build exception message - shall be called on main thread only.
	 *
	 * @return message.
	 */
	@NonNull
	public static String exceptionOnlyMainThread()
	{
		return "shall be called only on main thread";
	}

	/**
	 * build exception message - shall be called on main thread only.
	 *
	 * @return message.
	 */
	@NonNull
	public static String exceptionInvalidArgument()
	{
		return "invalid argument";
	}
}
