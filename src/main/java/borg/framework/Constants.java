package borg.framework;

import org.jetbrains.annotations.Contract;

public final class Constants
{
	/** framework version **/
	public static final int VERSION_FRAMEWORK = 1;

	@Contract(pure = true)
	private Constants()
	{
		// private constructor to prevent instantiation
	}
}
