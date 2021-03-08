package borg.framework.auxiliaries;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.zip.CRC32;

import borg.framework.services.ArraysManager;

public final class BinaryParser
{
	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Public Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * size of array int8 value
	 **/
	public static final int SIZE_INT8 = 1;

	/**
	 * size of array int16 value
	 **/
	public static final int SIZE_INT16 = 2;

	/**
	 * size of array int24 value
	 */
	public static final int SIZE_INT24 = 3;

	/**
	 * size of array int32 value
	 **/
	public static final int SIZE_INT32 = 4;

	/**
	 * size of array int64 value
	 **/
	public static final int SIZE_INT64 = 8;

	/** size of array int128 value **/
	public static final int SIZE_INT128 = 16;

	/**
	 * length of array size value
	 **/
	public static final int SIZE_ARRAY_SIZE_LENGTH = SIZE_INT16;

	/**
	 * max value of byte
	 */
	public static final int BYTE_MAX_SIZE = 255;

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Definitions
	//////////////////////////////////////////////////////////////////////////////////////////////////

	public interface BinarySerializable extends Serializable
	{
		/**
		 * serialize object to byte array.
		 *
		 * @param buffer_ buffer to serialize object there.
		 *
		 * @return size of serialized object.
		 */
		int serialize(@NotNull List<Byte> buffer_);
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
	public static long crcValue(@NotNull List<Byte> data_)
	{
		// reset last value
		crc32.reset();

		// update value
		crc32.update(ArraysManager.bytesFromList(data_));

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
	 * read bits from bytes
	 *
	 * @param source_ the byte to read from
	 * @param index_  the index of bit to read
	 * @param offset_ the quantity of bits to read
	 *
	 * @return read byte.
	 */
	@Contract(pure = true)
	public static byte readByte(byte source_, int index_, int offset_)
	{
		byte result_ = (byte)(source_ >> index_);

		byte mask = (byte)(BYTE_MAX_SIZE >> (8 - offset_));

		result_ = (byte)(result_ & mask);

		return result_;
	}

	/**
	 * read integer value from byte array stored in little endian representation.
	 *
	 * @param iterator_ - iterator in buffer where the value is stored.
	 * @param size_     - value size in bytes.
	 *
	 * @return read value.
	 */
	@SuppressWarnings("RedundantThrows")
	@Contract(pure = true)
	public static long readInteger(@NotNull Iterator<Byte> iterator_, int size_) throws Exception
	{
		long value = 0;
		for (int i = 0; i < size_; ++i)
		{
			value += (iterator_.next() & 0xffL) << (8 * i);
		}

		return value;
	}

	/**
	 * read enum value from byte array
	 *
	 * @param iterator_        iterator in buffer where the value is stored.
	 * @param entityTypesEnum_ enum represents entity class types.
	 *
	 * @return read enumerator.
	 *
	 * @throws Exception if enum cannot be read
	 */
	@Contract(pure = true)
	@NotNull
	public static <T extends Enum<T>> T readEnum(@NotNull Iterator<Byte> iterator_,
		@NotNull Class<T> entityTypesEnum_)
		throws Exception
	{
		long value = 0;
		for (int i = 0; i < SIZE_INT8; ++i)
		{
			value += iterator_.next() & 0xffL;
		}

		// get entity class
		Method method = entityTypesEnum_.getMethod("values");
		@SuppressWarnings("unchecked")
		T[] values = (T[])method.invoke(null);

		return values[(int)value];
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
	 * @param iterator_ iterator in buffer where the array is stored.
	 * @param size_     list value size in bytes.
	 *
	 * @return read array.
	 */
	@NotNull
	@Contract(pure = true)
	public static List<Long> readList(@NotNull Iterator<@NotNull Byte> iterator_, int size_)
		throws Exception
	{
		// read array size
		int size = (int)readInteger(iterator_, SIZE_ARRAY_SIZE_LENGTH);

		// create array
		ArrayList<Long> list = new ArrayList<>(size);

		// read elements
		for (int i = 0; i < size; ++i)
		{
			list.add(readInteger(iterator_, size_));
		}

		return list;
	}

	/**
	 * read array of integer values stored in byte array.
	 *
	 * @param iterator_ iterator in buffer where the array is stored.
	 * @param size_     list value size in bytes.
	 *
	 * @return read array.
	 */
	@NotNull
	@Contract(pure = true)
	public static List<Byte> readByteList(@NotNull Iterator<@NotNull Byte> iterator_, int size_)
		throws Exception
	{
		// read array size
		int size = (int)readInteger(iterator_, SIZE_ARRAY_SIZE_LENGTH);

		// create array
		List<Byte> list = new ArrayList<>(size);

		// read elements
		for (int i = 0; i < size; ++i)
		{
			list.add((byte)readInteger(iterator_, size_));
		}

		return list;
	}

	/**
	 * read array of binary deserializable objects stored in byte array.
	 *
	 * @param iterator_ iterator in buffer where the array is stored.
	 * @param class_    deserializable object class.
	 *
	 * @return read array.
	 *
	 * @throws Exception if array can not be read.
	 */
	@NotNull
	@Contract(pure = true)
	public static <T> List<T> readList(@NotNull Iterator<@NotNull Byte> iterator_,
		@NotNull Class<T> class_) throws Exception
	{
		// read array size
		int size = (int)readInteger(iterator_, SIZE_ARRAY_SIZE_LENGTH);

		// create array
		ArrayList<T> list = new ArrayList<>(size);

		// read elements
		Constructor<T> constructor = class_.getDeclaredConstructor(Iterator.class);
		for (int i = 0; i < size; ++i)
		{
			// create object
			T object = constructor.newInstance(iterator_);
			list.add(object);
		}

		return list;
	}

	/**
	 * write object to byte array.
	 *
	 * @param object_ object to write.
	 * @param buffer_ buffer to write to.
	 *
	 * @return number of written bytes.
	 */
	public static int write(@NotNull List<@NotNull Byte> object_,
		@NotNull List<@NotNull Byte> buffer_)
	{
		buffer_.addAll(object_);

		return object_.size();
	}

	/**
	 * write integer value to byte array in little endian representation.
	 *
	 * @param value_  value to write.
	 * @param size_   value size.
	 * @param buffer_ buffer to write to.
	 *
	 * @return number of written bytes.
	 */
	public static int writeInteger(long value_, int size_, @NotNull List<Byte> buffer_)
	{
		for (int i = 0; i < size_; ++i)
		{
			buffer_.add((byte)(value_));
			value_ >>= 8;
		}

		return size_;
	}

	/**
	 * write enumerator value to byte array in little endian representation.
	 *
	 * @param enum_   value to write.
	 * @param buffer_ buffer to write to.
	 *
	 * @return number of written bytes.
	 */
	public static int writeEnum(@NotNull Enum<?> enum_, @NotNull List<Byte> buffer_)
	{
		return writeInteger(enum_.ordinal(), SIZE_INT8, buffer_);
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
	public static int writeInteger(long value_,
		int size_,
		int index_,
		@NotNull List<Byte> buffer_)
	{
		for (int i = 0; i < size_; ++i)
		{
			buffer_.set(i + index_, ((byte)(value_)));
			value_ >>= 8;
		}

		return size_;
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
	 * @param buffer_     buffer to write to.
	 *
	 * @return number of written bytes.
	 */
	public static <T extends BinarySerializable> int writeCollection(
		@NotNull Collection<@NotNull T> collection_,
		@NotNull List<Byte> buffer_)
	{
		int size = 0;

		// write size
		size += writeInteger(collection_.size(), SIZE_ARRAY_SIZE_LENGTH, buffer_);

		// write array
		for (T element : collection_)
		{
			size += element.serialize(buffer_);
		}

		return size;
	}

	/**
	 * write collection of primitive integer elements to buffer.
	 *
	 * @param collection_  collection of elements to write. Collection will be stored at the order that
	 *                     provided collection defines.
	 * @param elementSize_ size of every element.
	 * @param buffer_      buffer to write to .
	 *
	 * @return number of written bytes.
	 */
	public static <T extends @NotNull Number> int writeCollection(
		@NotNull List<@NotNull T> collection_,
		int elementSize_,
		@NotNull List<Byte> buffer_)
	{
		int size = 0;

		// write size
		size += writeInteger(collection_.size(), SIZE_ARRAY_SIZE_LENGTH, buffer_);

		// write array
		for (T element : collection_)
		{
			size += writeInteger(element.longValue(), elementSize_, buffer_);
		}

		return size;
	}

	/**
	 * write collection of enums elements to buffer.
	 *
	 * @param collection_ collection of elements to write. Collection will be stored at the order that
	 *                    provided collection defines.
	 * @param buffer_     buffer to write to .
	 *
	 * @return number of written bytes.
	 */
	public static <T extends Enum<?>> int writeCollection(@NotNull List<@NotNull T> collection_,
		@NotNull List<Byte> buffer_)
	{
		int size = 0;

		// write size
		size += writeInteger(collection_.size(), SIZE_ARRAY_SIZE_LENGTH, buffer_);

		// write array
		for (T element : collection_)
		{
			size += writeInteger(element.ordinal(), SIZE_INT8, buffer_);
		}

		return size;
	}

	/**
	 * write double value to byte array in little endian representation.
	 *
	 * @param value_  value to write.
	 * @param buffer_ buffer to write to.
	 *
	 * @return number of written bytes.
	 */
	public static int writeDouble(double value_, @NotNull List<Byte> buffer_)
	{
		long value = Double.doubleToRawLongBits(value_);

		for (int i = 0; i < SIZE_INT64; ++i)
		{
			buffer_.add((byte)(value));
			value >>= 8;
		}

		return SIZE_INT64;
	}

	/**
	 * read double value from byte array stored in little endian representation.
	 *
	 * @param iterator_ iterator in buffer where the value is stored.
	 *
	 * @return read value.
	 */
	@Contract(pure = true)
	public static double readDouble(@NotNull Iterator<@NotNull Byte> iterator_)
	{
		long value = 0;
		for (int i = 0; i < SIZE_INT64; ++i)
		{
			value += (iterator_.next() & 0xffL) << (8 * i);
		}

		return Double.longBitsToDouble(value);
	}
}
