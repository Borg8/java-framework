package borg.framework.auxiliaries;

import org.jetbrains.annotations.Contract;

import borg.framework.services.TimeManager;

public class GlobalsHolder
{
	private static final long sStartTime = TimeManager.getSystemTime();

	@Contract(pure = true)
	public static long getUptime()
	{
		return TimeManager.getSystemTime() - sStartTime;
	}

	private GlobalsHolder()
	{
		// private constructor to avoid instantiation
	}
}
