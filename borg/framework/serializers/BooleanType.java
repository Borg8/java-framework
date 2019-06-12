package borg.framework.serializers;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import borg.framework.compability.Contract;

public enum BooleanType
{
	/** 0: unknown boolean value **/
	UNKNOWN(null),

	/** 1: boolean false **/
	FALSE(false),

	/** 2: boolean true **/
	TRUE(true);

	@Nullable
	public final Boolean bool;

	BooleanType(@Nullable Boolean bool_)
	{
		bool = bool_;
	}

	@Contract(pure = true)
	@NonNull
	public static BooleanType fromBoolean(@Nullable Boolean bool_)
	{
		if (bool_ != null)
		{
			if (bool_ == true)
			{
				return TRUE;
			}

			return FALSE;
		}

		return UNKNOWN;
	}
}
