package borg.framework.serializers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

	@Override
	protected byte[] serialize()
	{
		try
		{
			// try to serialize object
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);

			// write the object
			objectStream.writeObject(this);
			objectStream.flush();
			objectStream.close();
			byteStream.close();

			// return serialized object
			return byteStream.toByteArray();
		}
		catch (Exception e)
		{
			Logging.logging(Level.WARNING, e);
		}

		return null;
	}

	@Override
	protected Serialized deserialize(@NotNull byte[] data_)
	{
		try
		{
			// deserialize object
			ByteArrayInputStream stream = new ByteArrayInputStream(data_);
			Object object = new ObjectInputStream(stream).readObject();
			stream.close();

			return (BinarySerialized)object;
		}
		catch (Exception e)
		{
			Logging.logging(Level.WARNING, e);
		}

		return null;
	}
}
