package borg.framework.auxiliaries;

import org.jetbrains.annotations.CheckReturnValue;

import borg.framework.services.TimeManager;

public class GlobalsHolder
{
	/** application start time **/
	public static final long START_TIME = TimeManager.getSystemTime();

	@CheckReturnValue
	public static long getUptime()
	{
		return TimeManager.getSystemTime() - START_TIME;
	}

	private GlobalsHolder()
	{
		// private constructor to avoid instantiation
	}
}
