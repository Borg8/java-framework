package borg.framework.serializers;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import borg.framework.Constants;
import borg.framework.auxiliaries.Logger;
import borg.framework.services.StorageManager;

public abstract class Serialized implements Serializable
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

	public interface Serializer
	{
		/**
		 * prepare object before serialization.
		 *
		 * @param object_ object to prepare.
		 */
		void prepare(@NotNull Serialized object_);

		/**
		 * finish object after deserialization.
		 *
		 * @param object_ object to finish.
		 */
		void finish(@NotNull Serialized object_);
	}

	public interface Encryptor
	{
		/**
		 * encrypt serialized object
		 *
		 * @param object_ serialized object to encrypt.
		 * @param data_   data represents the object.
		 *
		 * @return encrypted object to store.
		 */
		@Contract(pure = true)
		byte @Nullable [] encrypt(@NotNull Serialized object_, byte @NotNull [] data_);

		/**
		 * decrypt serialized object.
		 *
		 * @param object_ object to decrypt.
		 * @param data_   data represents the object.
		 *
		 * @return decrypted object to deserialize.
		 */
		@Contract(pure = true)
		byte @Nullable [] decrypt(@NotNull Serialized object_, byte @NotNull [] data_);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Fields
	//////////////////////////////////////////////////////////////////////////////////////////////////

	/** tag represents the instance. Have to be valid filename **/
	@NotNull
	public final String tag;

	/** file encryptor **/
	@Nullable
	private final transient Encryptor mEncryptor;

	/** object serializer **/
	@Nullable
	private final transient Serializer mSerializer;

	/** number of open transactions **/
	private transient int mTransactions;

	/** is state should be saved after transaction **/
	private transient boolean mIsSaved;

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Methods
	//////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * @param tag_       unique tag of the state. Have to be valid filename.
	 * @param encryptor_ encryptor to set.
	 * @param serliazer_ serializer to set.
	 */
	@Contract(pure = true)
	protected Serialized(@NotNull String tag_,
		@Nullable Encryptor encryptor_,
		@Nullable Serializer serliazer_)
	{
		tag = tag_;

		mEncryptor = encryptor_;
		mSerializer = serliazer_;

		mTransactions = 0;
	}

	/**
	 * @param tag_ unique tag of the state. Have to be valid filename.
	 */
	@Contract(pure = true)
	protected Serialized(@NotNull String tag_)
	{
		this(tag_, null, null);
	}

	/**
	 * open transaction.
	 */
	public final void openTransaction()
	{
		++mTransactions;
	}

	/**
	 * close transaction.
	 */
	public final void closeTransaction()
	{
		--mTransactions;

		// if last transaction closed
		if (mTransactions == 0)
		{
			// if state should be saved
			if (mIsSaved == true)
			{
				saveState();
			}
		}
	}

	/**
	 * save current state.
	 *
	 * @return {@code true} if state was saved successfully, {@code false} otherwise.
	 */
	@SuppressWarnings("null") // compiler issues
	public final boolean saveState()
	{
		// if transaction is closed
		if (mTransactions == 0)
		{
			mIsSaved = false;

			// if serializer is defined
			if (mSerializer != null)
			{
				mSerializer.prepare(this);
			}

			// serialize state
			byte[] data = serialize();

			// if serialization succeeded
			if (data != null)
			{
				// if encryptor is defined
				if (mEncryptor != null)
				{
					data = mEncryptor.encrypt(this, data);
				}

				// if data was encrypted successfully
				if (data != null)
				{
					try
					{
						// save object to storage
						StorageManager.createFile(tag, data);

						return true;
					}
					catch (Exception e)
					{
						Logger.log(e);
					}
				}
			}

			return false;
		}

		mIsSaved = true;

		return true;
	}

	/**
	 * read previous instance state.
	 *
	 * @return true if state exists, false otherwise.
	 */
	@SuppressWarnings("null") // compiler issues
	public final boolean readState()
	{
		// deserialize object
		byte[] data;
		try
		{
			// read data from storage
			data = StorageManager.readFile(tag);
		}
		catch (Exception e)
		{
			return false;
		}

		// if encryptor is defined
		if (mEncryptor != null)
		{
			// decrypt data
			data = mEncryptor.decrypt(this, data);
		}

		// if data was decrypted successfully
		if (data != null)
		{
			// deserialize object
			Serialized object = deserialize(data);

			// if object was deserialized successfully
			if (object != null)
			{
				// copy all fields from read object to this
				try
				{
					// get object class
					Class<?> objectClass = object.getClass();

					while ((objectClass != null) && (objectClass != Object.class))
					{
						// get all declared fields of object class
						Field[] fields = objectClass.getDeclaredFields();

						// copy all fields from object to the instance
						int modifiers = Modifier.TRANSIENT | Modifier.STATIC;
						for (Field field : fields)
						{
							// if field serializable
							if ((field.getModifiers() & modifiers) == 0)
							{
								field.setAccessible(true);
								field.set(this, field.get(object));
							}
						}

						// pass to object father
						objectClass = objectClass.getSuperclass();
					}

					// if serializer is defined
					if (mSerializer != null)
					{
						mSerializer.finish(this);
					}

					return true;
				}
				catch (Exception e)
				{
					Logger.log(e);
				}
			}
		}

		return false;
	}

	/**
	 * remove state of instance
	 */
	public final void removeState()
	{
		try
		{
			//noinspection ResultOfMethodCallIgnored
			StorageManager.getFile(tag).delete();
		}
		catch (Exception e)
		{
			Logger.log(e);
		}
	}

	@Contract(pure = true)
	protected abstract byte @Nullable [] serialize();

	@Contract(pure = true)
	@Nullable
	protected abstract Serialized deserialize(byte @NotNull [] data_);
}
