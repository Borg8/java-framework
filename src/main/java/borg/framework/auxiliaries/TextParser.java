package borg.framework.auxiliaries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import borg.framework.services.TimeManager;

public final class TextParser
{

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Public Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	private static final String DAY = "d";

	private static final String HOUR = "h";

	private static final String MINUTE = "m";

	private static final String SECOND = "s";

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Definitions
	//////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Fields
	//////////////////////////////////////////////////////////////////////////////////////////////////

	private static final DecimalFormat sDecimalFormat;

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Methods
	//////////////////////////////////////////////////////////////////////////////////////////////////

	static
	{
		sDecimalFormat = new DecimalFormat("#.##########", new DecimalFormatSymbols(Locale.US));
	}

	private TextParser()
	{
		// private constructor to prevent instantiation
	}

	/**
	 * convert seconds to human time.
	 *
	 * @param timestamp_ given timestamp in milliseconds.
	 *
	 * @return string represents the given timestamp in human representation.
	 */
	@NotNull
	public static String timestampToTime(long timestamp_)
	{
		if (timestamp_ < 0)
		{
			timestamp_ = -timestamp_;
		}

		long mill = timestamp_ % 1000;
		timestamp_ /= 1000;

		int days = (int)(timestamp_ / (24 * 60 * 60));
		timestamp_ %= 24 * 60 * 60;

		int hours = (int)timestamp_ / (60 * 60);
		timestamp_ %= 60 * 60;

		int minutes = (int)(timestamp_ / 60);
		timestamp_ %= 60;

		StringBuilder builder = new StringBuilder();
		if (days > 0)
		{
			builder.append(days);
			builder.append(DAY);
			builder.append(' ');
		}

		if (hours > 0)
		{
			builder.append(hours);
			builder.append(HOUR);
			builder.append(' ');
		}

		if (minutes > 0)
		{
			builder.append(minutes);
			builder.append(MINUTE);
			builder.append(' ');
		}

		builder.append(timestamp_);

		builder.append('.');
		if (mill < 100)
		{
			builder.append('0');
		}
		if (mill < 10)
		{
			builder.append('0');
		}

		builder.append(mill);
		builder.append(SECOND);

		return builder.toString();
	}

	/**
	 * convert time stamp to date in human representation.
	 *
	 * @param timestamp_ time stamp to convert.
	 * @param timeSeparator_ separator between time parts. If {@code \0} provided, then time will not
	 *          be included.
	 * @param dateSeparator_ separator between date parts. If {@code \0} provided, then date will not
	 *          be included.
	 * @param precision_ required precision, from {@link Calendar#YEAR} to
	 *          {@link Calendar#MILLISECOND}.
	 *
	 * @return date in human representation.
	 */
	@NotNull
	public static synchronized String getHumanDate(long timestamp_,
		char timeSeparator_,
		char dateSeparator_,
		int precision_)
	{
		Calendar sCalendar = Calendar.getInstance(TimeZone.getTimeZone("Israel"));
		sCalendar.setTimeInMillis(timestamp_);
		StringBuilder builder = new StringBuilder();

		// if date shall be included
		if (dateSeparator_ != '\0')
		{
			if (precision_ >= Calendar.DAY_OF_MONTH)
			{
				int day = sCalendar.get(Calendar.DAY_OF_MONTH);
				if (day < 10)
				{
					builder.append('0');
				}
				builder.append(day);
				builder.append(dateSeparator_);
			}
			if (precision_ >= Calendar.MONTH)
			{
				int month = sCalendar.get(Calendar.MONTH) + 1;
				if (month < 10)
				{
					builder.append('0');
				}
				builder.append(month);
				builder.append(dateSeparator_);
			}
			builder.append(sCalendar.get(Calendar.YEAR));
			if (timeSeparator_ != '\0')
			{
				builder.append(' ');
			}
		}

		// if time shall be included
		if (timeSeparator_ != '\0')
		{
			int hours = sCalendar.get(Calendar.HOUR_OF_DAY);
			if (hours < 10)
			{
				builder.append('0');
			}
			builder.append(hours);
			if (precision_ >= Calendar.MINUTE)
			{
				builder.append(timeSeparator_);
				int minutes = sCalendar.get(Calendar.MINUTE);
				if (minutes < 10)
				{
					builder.append('0');
				}
				builder.append(minutes);
			}
			if (precision_ >= Calendar.SECOND)
			{
				builder.append(timeSeparator_);
				int seconds = sCalendar.get(Calendar.SECOND);
				if (seconds < 10)
				{
					builder.append('0');
				}
				builder.append(seconds);
			}
			if (precision_ >= Calendar.MILLISECOND)
			{
				builder.append(timeSeparator_);
				int mills = sCalendar.get(Calendar.MILLISECOND);
				if (mills < 10)
				{
					builder.append("00");
				}
				else
				{
					if (mills < 100)
					{
						builder.append('0');
					}
				}
				builder.append(mills);
			}
		}

		return builder.toString();
	}

	/**
	 * convert double to string. Up to 10 digits after floating point.
	 *
	 * @param d_ double to convert.
	 *
	 * @return string represents the double.
	 */
	@NotNull
	public static String toString(double d_)
	{
		return sDecimalFormat.format(d_);
	}

	/**
	 * convert long to hex string.
	 *
	 * @param l_ long to convert.
	 *
	 * @return string represents the long in hexadecimal base. The value can be parsed by
	 *         {@link Long#parseLong(String)}.
	 */
	@NotNull
	public static String toHexString(long l_)
	{
		// if long is negative
		if (l_ < 0)
		{
			return "-" + Long.toHexString(-l_);
		}

		return Long.toHexString(l_);
	}

	/**
	 * parse decimal integer from string.
	 *
	 * @param string_ string to parse.
	 *
	 * @return integer, if parsed.
	 */
	@Nullable
	public static Long parseInteger(@NotNull String string_)
	{
		// is string is empty
		int n = string_.length();
		if (n > 0)
		{
			// parse sign
			boolean minus = string_.charAt(0) == '-';

			// build integer
			long result = 0;
			for (int i = minus? 1: 0; i < n; ++i)
			{
				// if noo-digit character found
				char c = string_.charAt(i);
				if ((c < '0') || (c > '9'))
				{
					return null;
				}

				// assume that the number is not too big
				result = (result * 10) + c - '0';
			}

			// assume comparison is faster than multiplication
			return minus? -result: result;
		}

		return null;
	}

	/**
	 * get age.
	 * 
	 * @param birthday_ birthday in milliseconds.
	 * @return age in years.
	 */
	public static int getAge(long birthday_)
	{
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Israel/Jerusalem"));

		// get birth date
		calendar.setTimeInMillis(birthday_);
		int birthYear = calendar.get(Calendar.YEAR);
		int birthDays = calendar.get(Calendar.DAY_OF_YEAR);

		// get current date
		calendar.setTimeInMillis(TimeManager.getRealTime());
		int year = calendar.get(Calendar.YEAR);
		int days = calendar.get(Calendar.DAY_OF_YEAR);

		// compute age
		int age = year - birthYear;
		if (birthDays > days)
		{
			--age;
		}

		return age;
	}
}
