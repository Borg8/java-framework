package borg.framework.auxiliaries;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.CRC32;

import borg.framework.collections.ByteArray;
import borg.framework.serializers.RTyped;

@SuppressWarnings("ExplicitArrayFilling")
public final class BinaryParser
{
	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Public Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	/** size int8 value **/
	public static final int SIZE_INT8 = 1;

	/** size int16 value **/
	public static final int SIZE_INT16 = 2;

	/** size int24 value */
	public static final int SIZE_INT24 = 3;

	/** size int32 value **/
	public static final int SIZE_INT32 = 4;

	/** size int64 value **/
	public static final int SIZE_INT64 = 8;

	/** size int128 value **/
	public static final int SIZE_INT128 = 16;

	/** size of float value **/
	public static final int SIZE_FLOAT = 4;

	/** size of double value **/
	public static final int SIZE_DOUBLE = 8;

	/** length of array size value **/
	public static final int SIZE_ARRAY_LENGTH = 3;

	/** max value of byte */
	public static final int BYTE_MAX_SIZE = 255;

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Definitions
	//////////////////////////////////////////////////////////////////////////////////////////////////

	public interface BinarySerializable extends Serializable
	{
		/**
		 * serialize object to byte array.
		 *
		 * @param writer_ writer to write with.
		 *
		 * @return size of serialized object.
		 */
		default int serialize(@SuppressWarnings("unused") @NotNull Writer writer_)
		{
			return 0;
		}
	}

	public static final class Reader
	{
		/** buffer to read from **/
		private final byte[] mBuffer;

		/** current buffer index **/
		private int mIndex;

		public Reader(byte @NotNull [] buffer_)
		{
			mBuffer = buffer_;
			mIndex = 0;
		}

		@Contract(pure = true)
		byte read()
		{
			return mBuffer[mIndex++];
		}

		@Contract(pure = true)
		byte touch()
		{
			return mBuffer[mIndex];
		}

		@Contract(pure = true)
		public boolean hasNext()
		{
			return mIndex < mBuffer.length;
		}
	}

	public static class Writer extends ByteArray
	{
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Fields
	//////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Methods
	//////////////////////////////////////////////////////////////////////////////////////////////////

	private static final StringBuilder builder = new StringBuilder();

	private static final CRC32 crc32 = new CRC32();

	/**
	 * get array of byte data and returns crc32
	 *
	 * @param data_ data to calculate crc32
	 *
	 * @return crc32 value.
	 */
	@Contract(pure = true)
	public static long crcValue(byte @NotNull [] data_)
	{
		// reset last value
		crc32.reset();

		// update value
		crc32.update(data_);

		return crc32.getValue();
	}

	/**
	 * get integer as hex string in little endian representation.
	 *
	 * @param integer_ integer to convert.
	 * @param size_    size of integer in bytes.
	 *
	 * @return hex string represent the integer.
	 */
	@NotNull
	@Contract(pure = true)
	public static String integerToHex(long integer_, int size_)
	{
		builder.setLength(0);

		// convert to string
		for (; size_ > 0; --size_)
		{
			int i = (int)(integer_ & 0xff);
			integer_ >>= 8;

			// if 4 MSBs is 0
			if (i < 0x10)
			{
				builder.append('0');
			}

			builder.append(Integer.toHexString(i));
		}

		return builder.toString();
	}

	/**
	 * read integer value from byte array stored in little endian representation.
	 *
	 * @param reader_ reader to use.
	 * @param size_   - value size in bytes.
	 *
	 * @return read value.
	 */
	@Contract(pure = true)
	public static long readInteger(@NotNull Reader reader_, int size_)
	{
		long value = 0;
		for (int i = 0; i < size_; ++i)
		{
			value += (reader_.read() & 0xffL) << (8 * i);
		}

		return value;
	}

	/**
	 * read enum value from byte array
	 *
	 * @param reader_          reader to use.
	 * @param entityTypesEnum_ enum represents entity class types.
	 *
	 * @return read enumerator.
	 */
	@Contract(pure = true)
	@NotNull
	public static <T extends Enum<T>> T readEnum(@NotNull Reader reader_,
		@NotNull Class<T> entityTypesEnum_)
	{

		long value = 0;
		for (int i = 0; i < SIZE_INT8; ++i)
		{
			value += reader_.read() & 0xffL;
		}

		// get entity class
		try
		{
			Method method = entityTypesEnum_.getMethod("values");
			@SuppressWarnings("unchecked")
			T[] values = (T[])method.invoke(null);

			return values[(int)value];
		}
		catch (Exception e)
		{
			throw new Error(e);
		}
	}

	/**
	 * read integer value from byte array stored in little endian representation.
	 *
	 * @param source_ source array where the integer is stored.
	 * @param size_   value size in bytes.
	 * @param offset_ offset in source array where the integer is stored.
	 *
	 * @return read value.
	 */
	@Contract(pure = true)
	public static long readInteger(byte @NotNull [] source_, int size_, int offset_)
	{
		long value = 0;
		for (--size_; size_ >= 0; --size_)
		{
			value = (value << 8) + (source_[size_ + offset_] & 0xff);
		}

		return value;
	}

	/**
	 * read array of integer values stored in byte array.
	 *
	 * @param reader_ reader to use.
	 * @param size_   array value size in bytes.
	 *
	 * @return read array.
	 */
	@Contract(pure = true)
	public static long @NotNull [] readIntegers(@NotNull Reader reader_, int size_)
	{
		// read array size
		int size = (int)readInteger(reader_, SIZE_ARRAY_LENGTH);

		// create array
		long[] numbers = new long[size];

		// read elements
		for (int i = 0; i < size; ++i)
		{
			numbers[i] = readInteger(reader_, size_);
		}

		return numbers;
	}

	/**
	 * read array of bytes stored in byte array with constant length.
	 *
	 * @param reader_ reader to use.
	 * @param length_ number of bytes to read.
	 *
	 * @return read bytes array.
	 */
	@Contract(pure = true)
	public static byte @NotNull [] readBytes(@NotNull Reader reader_, int length_)
	{
		// create array
		byte[] bytes = new byte[length_];

		// read elements
		for (int i = 0; i < length_; ++i)
		{
			bytes[i] = (byte)readInteger(reader_, SIZE_INT8);
		}

		return bytes;
	}

	/**
	 * read array of bytes stored in byte array.
	 *
	 * @param reader_ reader to use.
	 *
	 * @return read bytes array.
	 */
	@Contract(pure = true)
	public static byte @NotNull [] readBytes(@NotNull Reader reader_)
	{
		// read array size
		int size = (int)readInteger(reader_, SIZE_ARRAY_LENGTH);

		// create array
		byte[] bytes = new byte[size];

		// read elements
		for (int i = 0; i < size; ++i)
		{
			bytes[i] = (byte)readInteger(reader_, SIZE_INT8);
		}

		return bytes;
	}

	/**
	 * read array of real numbers stored in byte array.
	 *
	 * @param reader_ reader to use.
	 *
	 * @return read array.
	 */
	@Contract(pure = true)
	public static double @NotNull [] readReals(@NotNull Reader reader_)
	{
		// read array size
		int size = (int)readInteger(reader_, SIZE_ARRAY_LENGTH);

		// create array
		double[] reals = new double[size];

		// read elements
		for (int i = 0; i < size; ++i)
		{
			reals[i] = readReal(reader_);
		}

		return reals;
	}

	/**
	 * read typed object from byte array.
	 *
	 * @param reader_ reader to use.
	 * @param types_  object types enumerator.
	 *
	 * @return read object.
	 */
	@SuppressWarnings("unchecked")
	@NotNull
	@Contract(pure = true)
	public static <T extends BinarySerializable, E extends Enum<E> & RTyped<? super T>> T readTyped(
		@NotNull Reader reader_,
		@NotNull Class<E> types_)
	{
		try
		{
			// get entity class
			Method method = types_.getMethod("values");
			RTyped<T>[] values = (RTyped<T>[])method.invoke(null);
			assert values != null;
			byte type = reader_.touch();
			Class<T> entityClass = (Class<T>)values[((Number)type).intValue()].entityClass();

			// get entity constructor
			Constructor<T> constructor = entityClass.getConstructor(Reader.class);
			constructor.setAccessible(true);
			return constructor.newInstance(reader_);
		}
		catch (Exception e)
		{
			throw new Error(e);
		}
	}

	/**
	 * read array of binary deserializable objects stored in byte array.
	 *
	 * @param reader_ reader to use.
	 * @param class_  deserializable object class.
	 *
	 * @return read array.
	 */
	@NotNull
	@Contract(pure = true)
	public static <T extends BinarySerializable> List<T> readObjects(@NotNull Reader reader_,
		@NotNull Class<T> class_)
	{
		// read array size
		int size = (int)readInteger(reader_, SIZE_ARRAY_LENGTH);

		// create array
		ArrayList<T> list = new ArrayList<>(size);

		// read elements
		try
		{
			Constructor<T> constructor = class_.getDeclaredConstructor(Reader.class);
			constructor.setAccessible(true);
			for (int i = 0; i < size; ++i)
			{
				// create object
				T object = constructor.newInstance(reader_);
				list.add(object);
			}

			return list;
		}
		catch (Exception e)
		{
			throw new Error(e);
		}
	}

	/**
	 * read objects stored in byte array.
	 *
	 * @param reader_ reader to use.
	 * @param types_  types enumerator.
	 *
	 * @return read typed objects.
	 */
	@NotNull
	@Contract(pure = true)
	public static <T extends BinarySerializable, E extends Enum<E> & RTyped<? super T>> List<T> readTypedObjects(
		@NotNull Reader reader_,
		@NotNull Class<E> types_)
	{
		// read array size
		int size = (int)readInteger(reader_, SIZE_ARRAY_LENGTH);

		// read elements
		ArrayList<T> list = new ArrayList<>(size);
		for (int i = 0; i < size; ++i)
		{
			list.add(readTyped(reader_, types_));
		}

		return list;
	}

	/**
	 * write integer value to byte array in little endian representation.
	 *
	 * @param value_  value to write.
	 * @param size_   value size.
	 * @param writer_ writer to write with.
	 *
	 * @return number of written bytes.
	 */
	public static int writeInteger(long value_, int size_, @NotNull Writer writer_)
	{
		for (int i = 0; i < size_; ++i)
		{
			writer_.push((byte)(value_));
			value_ >>= 8;
		}

		return size_;
	}

	/**
	 * write enumerator value to byte array in little endian representation.
	 *
	 * @param enum_   value to write.
	 * @param writer_ writer to write with.
	 *
	 * @return number of written bytes.
	 */
	public static int writeEnum(@NotNull Enum<?> enum_, @NotNull Writer writer_)
	{
		return writeInteger(enum_.ordinal(), SIZE_INT8, writer_);
	}

	/**
	 * write integer value to byte array in little endian representation.
	 *
	 * @param value_  value to write.
	 * @param size_   value size.
	 * @param index_  index in array to write the value there.
	 * @param buffer_ buffer to write to.
	 *
	 * @return number of written bytes.
	 */
	public static int writeInteger(long value_, int size_, int index_, byte @NotNull [] buffer_)
	{
		for (int i = 0; i < size_; ++i)
		{
			buffer_[i + index_] = (byte)(value_);
			value_ >>= 8;
		}

		return size_;
	}

	/**
	 * write collection of binary serializable objects to buffer. Collection will be stored at the
	 * order that provided collection defines.
	 *
	 * @param collection_ collection of elements to write.
	 * @param writer_     writer to write with.
	 *
	 * @return number of written bytes.
	 */
	public static <T extends BinarySerializable> int writeObjects(@NotNull Collection<T> collection_,
		@NotNull Writer writer_)
	{
		int size = 0;

		// write size
		size += writeInteger(collection_.size(), SIZE_ARRAY_LENGTH, writer_);

		// write array
		for (T element : collection_)
		{
			size += element.serialize(writer_);
		}

		return size;
	}

	/**
	 * write array of  integer elements to buffer.
	 *
	 * @param array_  array of elements to write.
	 * @param writer_ writer to write with.
	 *
	 * @return number of written bytes.
	 */
	public static int writeIntegers(int @NotNull [] array_, @NotNull Writer writer_)
	{
		int size = 0;

		// write size
		size += writeInteger(array_.length, SIZE_ARRAY_LENGTH, writer_);

		// write array
		for (int element : array_)
		{
			size += writeInteger(element, SIZE_INT32, writer_);
		}

		return size;
	}

	/**
	 * write array of bytes elements to buffer.
	 *
	 * @param array_  array of elements to write.
	 * @param writer_ writer to write with.
	 *
	 * @return number of written bytes.
	 */
	public static int writeIntegers(byte @NotNull [] array_, @NotNull Writer writer_)
	{
		int size = 0;

		// write size
		size += writeInteger(array_.length, SIZE_ARRAY_LENGTH, writer_);

		// write array
		for (byte element : array_)
		{
			size += writeInteger(element, SIZE_INT8, writer_);
		}

		return size;
	}

	/**
	 * write collection of double elements to buffer.
	 *
	 * @param collection_ collection of elements to write. Collection will be stored at the order that
	 *                    provided collection defines.
	 * @param writer_     writer to write with.
	 *
	 * @return number of written bytes.
	 */
	public static <T extends Number> int writeReals(@NotNull List<T> collection_,
		@NotNull Writer writer_)
	{
		int size = 0;

		// write size
		size += writeInteger(collection_.size(), SIZE_ARRAY_LENGTH, writer_);

		// write array
		for (T d : collection_)
		{
			size += writeReal((Double)d, writer_);
		}

		return size;
	}

	/**
	 * write array of double elements to buffer.
	 *
	 * @param array_  array of elements to write. Collection will be stored at the order that
	 *                provided collection defines.
	 * @param writer_ writer to write with.
	 *
	 * @return number of written bytes.
	 */
	public static int writeReals(double @NotNull [] array_, @NotNull Writer writer_)
	{
		int size = 0;

		// write size
		size += writeInteger(array_.length, SIZE_ARRAY_LENGTH, writer_);

		// write array
		for (double d : array_)
		{
			size += writeReal(d, writer_);
		}

		return size;
	}

	/**
	 * write collection of enums elements to buffer.
	 *
	 * @param collection_ collection of elements to write. Collection will be stored at the order that
	 *                    provided collection defines.
	 * @param writer_     writer to write with.
	 *
	 * @return number of written bytes.
	 */
	public static <T extends Enum<?>> int writeEnums(@NotNull List<T> collection_,
		@NotNull Writer writer_)
	{
		int size = 0;

		// write size
		size += writeInteger(collection_.size(), SIZE_ARRAY_LENGTH, writer_);

		// write array
		for (T element : collection_)
		{
			size += writeInteger(element.ordinal(), SIZE_INT8, writer_);
		}

		return size;
	}

	/**
	 * write real value to byte array.
	 *
	 * @param value_  value to write.
	 * @param writer_ writer to write with.
	 *
	 * @return number of written bytes.
	 */
	public static int writeReal(double value_, @NotNull Writer writer_)
	{
		return writeInteger(Double.doubleToRawLongBits(value_), SIZE_DOUBLE, writer_);
	}

	/**
	 * read real value from byte array.
	 *
	 * @param reader_ reader to use.
	 *
	 * @return read value.
	 */
	@Contract(pure = true)
	public static double readReal(@NotNull Reader reader_)
	{
		return Double.longBitsToDouble(readInteger(reader_, SIZE_DOUBLE));
	}

	/**
	 * write float value to byte array.
	 *
	 * @param value_  value to write.
	 * @param writer_ writer to write with.
	 *
	 * @return number of written bytes.
	 */
	public static int writeFloat(float value_, @NotNull Writer writer_)
	{
		return writeInteger(Float.floatToRawIntBits(value_), SIZE_FLOAT, writer_);
	}

	/**
	 * read float value from byte array.
	 *
	 * @param reader_ reader to use.
	 *
	 * @return read value.
	 */
	@Contract(pure = true)
	public static double readFloat(@NotNull Reader reader_)
	{
		return Float.intBitsToFloat((int)readInteger(reader_, SIZE_FLOAT));
	}
}
