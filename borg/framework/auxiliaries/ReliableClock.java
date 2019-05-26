package borg.framework.auxiliaries;

public final class ReliableClock
{
	/** second duration **/
	public static final long SECOND = 1000;

	/** minute duration **/
	public static final long MINUTE = SECOND * 60;

	/** hour duration **/
	public static final long HOUR = MINUTE * 60;

	/** day duration **/
	public static final long DAY = HOUR * 24;

	/** week duration **/
	public static final long WEEK = DAY * 7;

	private ReliableClock()
	{
		// private constructor to prevent instantiation
	}

	/**
	 * @return real time in milliseconds.
	 */
	public static long getRealTime()
	{
		return System.currentTimeMillis();
	}
}
