package borg.framework.structures;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class TypedEntity<T extends Enum<T>>
{
	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Definitions
	//////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Fields
	//////////////////////////////////////////////////////////////////////////////////////////////////

	/** entity type **/
	@NotNull
	public final T type;

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Methods
	//////////////////////////////////////////////////////////////////////////////////////////////////

	@Contract(pure = true)
	public TypedEntity(@NotNull T type_)
	{
		type = type_;
	}

	@Contract(value = "null -> false", pure = true)
	@Override
	public boolean equals(Object object_)
	{
		if (object_ instanceof TypedEntity)
		{
			return type == ((TypedEntity<?>)object_).type;

		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return type.ordinal();
	}

	@Override
	public String toString()
	{
		return "type: " + type;
	}
}
