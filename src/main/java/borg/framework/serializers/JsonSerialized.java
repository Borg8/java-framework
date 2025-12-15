package borg.framework.serializers;

import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;

import borg.framework.auxiliaries.Logger;
import borg.framework.services.StorageManager;
import borg.framework.services.TimeManager;

public abstract class JsonSerialized extends REntity
{
	/*************************************************************************************************
	 * Fields
	 ************************************************************************************************/

	/** file to store object there **/
	private final File mFile;

	/*************************************************************************************************
	 * Methods
	 ************************************************************************************************/

	protected JsonSerialized(@NotNull String file_)
	{
		try
		{
			mFile = StorageManager.getFile(file_);
		}
		catch (Throwable e)
		{
			throw new Error(e);
		}
	}

	public final void save(long delay_)
	{
		TimeManager.asyncExecute(delay_, _saveHandler);
	}

	/** @noinspection ResultOfMethodCallIgnored*/
	public synchronized final void save()
	{
		// cancel handler
		TimeManager.cancel(_saveHandler);

		try
		{
			// create file content
			byte[] content = EntityParser.buildJson(toMap()).toString().getBytes();

			// create backup
			File backUp = _getBackup();
			if (backUp.exists())
			{
				backUp.delete();
			}
			mFile.renameTo(backUp);

			// store file
			StorageManager.createFile(mFile, content);
		}
		catch (Throwable e)
		{
			Logger.log(e);
		}
	}

	@Nullable
	@CheckReturnValue
	protected HashMap<String, Object> readMap()
	{
		try
		{
			return _readMap(mFile);
		}
		catch (Throwable e)
		{
			try
			{
				return _readMap(_getBackup());
			}
			catch (FileNotFoundException e_)
			{
				// nothing to do
			}
			catch (Throwable e1)
			{
				Logger.log(e1);
			}
		}

		return null;
	}

	@Nullable
	@CheckReturnValue
	private static HashMap<String, Object> _readMap(@NotNull File file_) throws Exception
	{
		JSONObject json = new JSONObject(new String(StorageManager.readFile(file_)));
		return EntityParser.buildMap(json);
	}

	@NotNull
	@CheckReturnValue
	private File _getBackup()
	{
		return new File(mFile + ".bak");
	}

	private final TimeManager. Handler<Void> _saveHandler = (time_, param_) -> save();
}
