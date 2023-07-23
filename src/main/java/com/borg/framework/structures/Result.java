package com.borg.framework.structures;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;

public class Result<T extends Enum<T>> extends TypedEntity<T>
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

	/** object that create the result **/
	@NotNull
	public final Object actor;

	/** result free text **/
	@Nullable
	public final String message;

	/** result that causes the result **/
	@Nullable
	public final Result<? extends Enum<?>> cause;

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Methods
	//////////////////////////////////////////////////////////////////////////////////////////////////

	public Result(@NotNull Object actor_, @NotNull T type_)
	{
		this(actor_, type_, null, null);
	}

	public Result(@NotNull Object actor_, @NotNull T type_, @Nullable String message_)
	{
		this(actor_, type_, message_, null);
	}

	public Result(@NotNull Object actor_,
		@NotNull T type_,
		@Nullable Result<? extends Enum<?>> cause_)
	{
		this(actor_, type_, null, cause_);
	}

	public Result(@NotNull Object actor_,
		@NotNull T type_,
		@Nullable String message_,
		@Nullable Result<? extends Enum<?>> cause_)
	{
		super(type_);

		actor = actor_;
		message = message_;
		cause = cause_;
	}

	@SuppressWarnings("null") // compiler issue
	@Override
	@NotNull
	public String toString()
	{
		StringBuilder builder = new StringBuilder();

		Result<? extends Enum<?>> result = this;
		for (;;)
		{
			// append current result
			builder.append(MessageFormat.format("actor: {0}, type: {1} ({2}: {3})",
				result.actor.getClass().getSimpleName(),
				result.type.ordinal(),
				result.type.getDeclaringClass().getSimpleName(),
				result.type.name()));

			// if message is defined
			if (result.message != null)
			{
				builder.append(", message: ");
				builder.append(result.message);
			}

			// if cause exists
			if (result.cause != null)
			{
				// append result-cause connector
				builder.append(" <- ");

				// get cause
				result = result.cause;
			}
			else
			{
				break;
			}
		}

		return builder.toString();
	}
}
