package borg.framework.services;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

public final class StorageManager
{
	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Public Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	/** chunk size (32 kb) **/
	private static final int SIZE_CHUNK = 32 * 1024;

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Definitions
	//////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Fields
	//////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Methods
	//////////////////////////////////////////////////////////////////////////////////////////////////

	@Contract(pure = true)
	private StorageManager()
	{
		// private constructor to prevent instantiation
	}

	/**
	 * get file by its name.
	 *
	 * @param name_ name of file to get.
	 *
	 * @return file according to given file name.
	 *
	 * @throws Exception if file name is not valid.
	 */
	@NotNull
	@Contract(pure = true)
	public static File getFile(@NotNull String name_) throws Exception
	{
		return new File(name_).getCanonicalFile();
	}

	/**
	 * creating directory with given path. If file with given name exists, then it will be removed and
	 * directory will be created instead.
	 *
	 * @param path_ directory path.
	 */
	public static void createDirectory(String path_) throws Exception
	{
		createDirectory(getFile(path_));
	}

	/**
	 * creating directory. If file with given name exists, then it will be removed and directory will
	 * be created instead.
	 *
	 * @param directory_ directory to create.
	 */
	public static void createDirectory(@NotNull File directory_) throws Exception
	{
		// if directory is not exists
		if (directory_.isDirectory() == false)
		{
			// if creation was failed
			if (directory_.mkdirs() == false)
			{
				// if fail to recreate directory
				if ((directory_.delete() == false) || (directory_.mkdirs() == false))
				{
					throw new Exception("unable to create directory: " + directory_.getAbsolutePath());
				}
			}
		}
	}

	/**
	 * delete file or directory.
	 *
	 * @param file_ file to delete.
	 *
	 * @return {@code true} if the file or whole directory was deleted, {@code false} otherwise, but
	 * if the file is directory, then it is possible that some files were deleted.
	 */
	public static boolean delete(@NotNull File file_)
	{
		// if file is directory
		if (file_.isDirectory() == true)
		{
			// delete all its files
			for (File file : Objects.requireNonNull(file_.listFiles()))
			{
				if (delete(file) == false)
				{
					return false;
				}
			}
		}

		return file_.delete();
	}

	/**
	 * rename file.
	 *
	 * @param file_     file to rename.
	 * @param filename_ new file path.
	 *
	 * @return {@code true} if the file was renamed, {@code false} otherwise.
	 */
	public static boolean rename(@NotNull File file_, @NotNull String filename_)
	{
		return file_.renameTo(new File(filename_));
	}

	/**
	 * create file with given name from byte cByteStream.
	 *
	 * @param name_    the path to the given file.
	 * @param content_ file content.
	 *
	 * @return created file.
	 *
	 * @throws Exception when file was not created.
	 */
	@NotNull
	public static File createFile(@NotNull String name_, byte @NotNull [] content_) throws Exception
	{
		File file = getFile(name_);
		createFile(file, content_);

		return file;
	}

	/**
	 * create file from byte cByteStream.
	 *
	 * @param file_    file to create.
	 * @param content_ file content.
	 *
	 * @throws Exception when file was not created.
	 */
	public static void createFile(@NotNull File file_, byte @NotNull [] content_) throws Exception
	{
		writeFile(file_, content_, false);
	}

	/**
	 * create file with given name from input stream.
	 *
	 * @param name_   the path to the given file.
	 * @param stream_ input stream for file content.
	 *
	 * @return created file.
	 *
	 * @throws Exception when file was not created.
	 */
	@NotNull
	public static File createFile(@NotNull String name_, @NotNull InputStream stream_)
		throws Exception
	{
		File file = getFile(name_);
		createFile(file, stream_);

		return file;
	}

	/**
	 * create file from input stream.
	 *
	 * @param file_   file to create.
	 * @param stream_ input stream for file content.
	 *
	 * @throws Exception when file was not created.
	 */
	public static void createFile(@NotNull File file_, @NotNull InputStream stream_)
		throws Exception
	{
		writeFile(file_, stream_, false);
	}

	/**
	 * append to file with given name given byte cByteStream.
	 *
	 * @param name_    the path to the given file.
	 * @param content_ file content.
	 *
	 * @return appended file.
	 *
	 * @throws Exception when file was not created.
	 */
	@NotNull
	public static File appendFile(@NotNull String name_, byte @NotNull [] content_) throws Exception
	{
		File file = getFile(name_);
		appendFile(file, content_);

		return file;
	}

	/**
	 * append to file given byte cByteStream.
	 *
	 * @param file_    file to append to it.
	 * @param content_ file content.
	 *
	 * @throws Exception when file was not created.
	 */
	public static void appendFile(@NotNull File file_, byte @NotNull [] content_) throws Exception
	{
		writeFile(file_, content_, true);
	}

	/**
	 * append to file with given name from input stream.
	 *
	 * @param name_   the path to the given file.
	 * @param stream_ input stream for file content.
	 *
	 * @return appended file.
	 *
	 * @throws Exception when file was not created.
	 */
	@NotNull
	public static File appendFile(@NotNull String name_, @NotNull InputStream stream_)
		throws Exception
	{
		File file = getFile(name_);
		appendFile(file, stream_);

		return file;
	}

	/**
	 * append to file from input stream.
	 *
	 * @param file_   file to append to it.
	 * @param stream_ input stream for file content.
	 *
	 * @throws Exception when file was not created.
	 */
	public static void appendFile(@NotNull File file_, @NotNull InputStream stream_) throws Exception
	{
		writeFile(file_, stream_, true);
	}

	@Contract(pure = true)
	public static byte @NotNull [] readFile(@NotNull String name_) throws Exception
	{
		return readFile(getFile(name_));
	}

	/**
	 * read file content.
	 *
	 * @param file_ file to read.
	 *
	 * @return read file content.
	 *
	 * @throws Exception when file was not found.
	 */
	@Contract(pure = true)
	public static byte @NotNull [] readFile(@NotNull File file_) throws Exception
	{
		try (FileInputStream stream = getFileInputStream(file_))
		{
			return readFile(stream);
		}
	}

	/**
	 * read file from stream.
	 *
	 * @param stream_ given input stream.
	 *
	 * @return read file.
	 *
	 * @throws Exception when file was not found.
	 */
	@Contract(pure = true)
	public static byte @NotNull [] readFile(@NotNull final InputStream stream_) throws Exception
	{
		try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream())
		{
			byte[] buffer = new byte[SIZE_CHUNK];

			for (; ; )
			{
				// read chunk
				int len;
				len = stream_.read(buffer, 0, buffer.length);

				// if last chunk was read
				if (len < 0)
				{
					break;
				}

				// write chunk
				byteStream.write(buffer, 0, len);
			}

			return byteStream.toByteArray();
		}
	}

	/**
	 * skip in file.
	 *
	 * @param stream_ file input stream.
	 * @param bytes_  number of bytes to skip (can be negative).
	 */
	public static void skip(@NotNull FileInputStream stream_, long bytes_) throws Exception
	{
		do
		{
			long skipped = stream_.skip(bytes_);
			bytes_ -= skipped;
		} while (bytes_ != 0);
	}

	/**
	 * create output stream for given file.
	 *
	 * @param name_   - file path.
	 * @param append_ true if file must be appended, false otherwise if recreated.
	 *
	 * @return file output stream.
	 *
	 * @throws Exception some IO exception.
	 */
	@NotNull
	@Contract(pure = true)
	public static FileOutputStream getFileOutputStream(@NotNull String name_, boolean append_)
		throws Exception
	{
		return getFileOutputStream(getFile(name_), append_);
	}

	/**
	 * create output stream for given file.
	 *
	 * @param file_   - file to create output stream to it.
	 * @param append_ true if file must be appended, false otherwise if recreated.
	 *
	 * @return file output stream.
	 *
	 * @throws Exception some IO exception.
	 */
	@NotNull
	@Contract(pure = true)
	public static FileOutputStream getFileOutputStream(@NotNull File file_, boolean append_)
		throws Exception
	{
		// create directory for file
		createDirectory(file_.getParent());

		return new FileOutputStream(file_.getCanonicalPath(), append_);
	}

	/**
	 * create file input stream for given file.
	 *
	 * @param name_ file path.
	 *
	 * @return built input stream.
	 *
	 * @throws Exception some IO exception.
	 */
	@NotNull
	@Contract(pure = true)
	public static FileInputStream getFileInputStream(@NotNull String name_) throws Exception
	{
		return getFileInputStream(getFile(name_));
	}

	/**
	 * create file input stream for given file.
	 *
	 * @param file_ file to create input stream for it.
	 *
	 * @return built input stream.
	 *
	 * @throws Exception some IO exception.
	 */
	@NotNull
	@Contract(pure = true)
	public static FileInputStream getFileInputStream(@NotNull File file_) throws Exception
	{
		// create at the external storage
		return new FileInputStream(file_);
	}

	/**
	 * check if string is valid file name.
	 *
	 * @param name_ name to check.
	 *
	 * @return true if string is valid filename, false otherwise.
	 */
	@Contract(pure = true)
	public static boolean isValidFilename(@NotNull String name_)
	{
		if (name_.isEmpty() == false)
		{
			String regex = "^(?!(?:CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\.[^.]*)?$)[^<>:\"/\\\\|?*\\x00-\\x1F]*[^<>:\"/\\\\|?*\\x00-\\x1F .]$";
			return name_.matches(regex);
		}

		return false;
	}

	private static void writeFile(@NotNull File file_, byte @NotNull [] content_, boolean append_)
		throws Exception
	{
		try (OutputStream stream = getFileOutputStream(file_, append_))
		{
			stream.write(content_);
		}
	}

	private static void writeFile(@NotNull File file_,
		@NotNull InputStream stream_,
		boolean append_) throws Exception
	{
		try (FileOutputStream outputStream = getFileOutputStream(file_, append_))
		{
			byte[] buffer = new byte[SIZE_CHUNK];

			for (; ; )
			{
				// read chunk
				int size = stream_.read(buffer, 0, SIZE_CHUNK);

				// if end of file was not reached
				if (size > 0)
				{
					outputStream.write(buffer, 0, size);
				}
				else
				{
					break;
				}
			}
		}
	}
}
