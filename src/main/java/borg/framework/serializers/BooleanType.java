package borg.framework.serializers;

import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

	@CheckReturnValue
	BooleanType(@Nullable Boolean bool_)
	{
		bool = bool_;
	}

	@CheckReturnValue
	@NotNull
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
