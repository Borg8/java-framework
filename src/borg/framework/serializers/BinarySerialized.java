package borg.framework.serializers;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.logging.Level;

import borg.framework.auxiliaries.Logging;
import borg.framework.resources.Constants;

public class BinarySerialized extends Serialized
{
	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Public Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	private static final long serialVersionUID = Constants.VERSION_FRAMEWORK;

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Definitions
	//////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Fields
	//////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Methods
	//////////////////////////////////////////////////////////////////////////////////////////////////

	protected BinarySerialized(@NotNull String tag_,
		@Nullable Encryptor encryptor_,
		@Nullable Serializer serializer_)
	{
		super(tag_, encryptor_, serializer_);
	}

	protected BinarySerialized(@NotNull String tag_, @Nullable Encryptor encryptor_)
	{
		this(tag_, encryptor_, null);
	}

	protected BinarySerialized(@NotNull String tag_, @Nullable Serializer serializer_)
	{
		this(tag_, null, serializer_);
	}

	protected BinarySerialized(@NotNull String tag_)
	{
		this(tag_, null, null);
	}

	/**
	 * serialize object to array.
	 *
	 * @param object_ object to serialize.
	 *
	 * @return serialized object.
	 */
	@NotNull
	@Contract(pure = true)
	public static byte[] write(@NotNull Serializable object_)
	{
		// serialize code
		try
		{
			// create streams
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);

			// write the object
			objectStream.writeObject(object_);
			objectStream.flush();

			// close streams
			objectStream.close();
			byteStream.close();

			// return serialized object
			return byteStream.toByteArray();
		}
		catch (Exception e)
		{
			throw new Error(e);
		}
	}

	/**
	 * deserialize object from array.
	 *
	 * @param array_ array to deserialize from.
	 *
	 * @return deserialized object.
	 */
	@NotNull
	@Contract(pure = true)
	public static <T extends Serializable> T read(@NotNull byte[] array_)
	{
		try
		{
			// deserialize object
			ByteArrayInputStream stream = new ByteArrayInputStream(array_);
			//noinspection unchecked
			T object = (T)new ObjectInputStream(stream).readObject();
			stream.close();

			return object;
		}
		catch (Exception e)
		{
			throw new Error(e);
		}
	}

	@Override
	@Contract(pure = true)
	@Nullable
	protected byte[] serialize()
	{
		try
		{
			return write(this);
		}
		catch (Exception e)
		{
			Logging.logging(Level.WARNING, e);
		}

		return null;
	}

	@Override
	@Contract(pure = true)
	@Nullable
	protected Serialized deserialize(@NotNull byte[] data_)
	{
		try
		{
			return (Serialized)read(data_);
		}
		catch (Exception e)
		{
			Logging.logging(Level.WARNING, e);
		}

		return null;
	}
}
