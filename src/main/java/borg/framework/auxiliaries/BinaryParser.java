package borg.framework.auxiliaries;

import org.jetbrains.annotations.CheckReturnValue;
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
	/*************************************************************************************************
	 * Public Constants
	 ************************************************************************************************/

	/** size 8 bit value **/
	public static final int SIZE_UINT8 = 1;
	public static final int SIZE_INT8 = -1;

	/** size 16 bit value **/
	public static final int SIZE_UINT16 = 2;
	public static final int SIZE_INT16 = -2;

	/** size 32 bit value **/
	public static final int SIZE_UINT32 = 4;
	public static final int SIZE_INT32 = -4;

	/** size 64 bit value **/
	public static final int SIZE_UINT64 = 8;
	public static final int SIZE_INT64 = -8;

	/** size of float value **/
	public static final int SIZE_FLOAT = 4;

	/** size of double value **/
	public static final int SIZE_DOUBLE = 8;

	/** length of array size value **/
	public static final int SIZE_ARRAY_LENGTH = 3;

	/*************************************************************************************************
	 * Definitions
	 ************************************************************************************************/

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
		final byte[] mBuffer;

		/** current buffer index **/
		int mIndex;

		public Reader(byte @NotNull [] buffer_)
		{
			mBuffer = buffer_;
			mIndex = 0;
		}

		@CheckReturnValue
		byte read()
		{
			return mBuffer[mIndex++];
		}

		@CheckReturnValue
		public int getIndex()
		{
			return mIndex;
		}

		@CheckReturnValue
		public int getLeft()
		{
			return mBuffer.length - mIndex;
		}
	}

	public static class Writer extends ByteArray
	{
	}

	/*************************************************************************************************
	 * Fields
	 ************************************************************************************************/

	/*************************************************************************************************
	 * Methods
	 ************************************************************************************************/

	private static final StringBuilder builder = new StringBuilder();

	private static final CRC32 crc32 = new CRC32();

	/**
	 * get array of byte data and returns crc32
	 *
	 * @param data_ data to calculate crc32
	 *
	 * @return crc32 value.
	 */
	@CheckReturnValue
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
	@CheckReturnValue
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
	@CheckReturnValue
	public static long readInteger(@NotNull Reader reader_, int size_)
	{
		int size = size_ < 0? -size_: size_;
		long value = 0;
		for (int i = 0; i < size; ++i)
		{
			value += (reader_.read() & 0xffL) << (8 * i);
		}

		// if signed value
		if (size_ < 0)
		{
			long shift = 1L << (size * 8 - 1);
			if (value >= shift)
			{
				value -= shift << 1;
			}
		}

		return value;
	}

	/**
	 * read enum value from byte array
	 *
	 * @param reader_ reader to use.
	 * @param enum_   enum represents entity class types.
	 *
	 * @return read enumerator.
	 */
	@CheckReturnValue
	@NotNull
	public static <T extends Enum<T>> T readEnum(@NotNull Reader reader_, @NotNull Class<T> enum_)
	{

		long value = 0;
		for (int i = 0; i < SIZE_UINT8; ++i)
		{
			value += reader_.read() & 0xffL;
		}

		// get entity class
		try
		{
			Method method = enum_.getMethod("values");
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
	@CheckReturnValue
	public static long readInteger(byte @NotNull [] source_, int size_, int offset_)
	{
		int size = size_ < 0? -size_: size_;
		long value = 0;
		for (--size; size >= 0; --size)
		{
			value = (value << 8) + (source_[size + offset_] & 0xff);
		}

		// if signed value
		if (size_ < 0)
		{
			long shift = 1L << (size * 8 - 1);
			if (value >= shift)
			{
				value -= shift << 1;
			}
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
	@CheckReturnValue
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
	@CheckReturnValue
	public static byte @NotNull [] readByteArray(@NotNull Reader reader_, int length_)
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
	@CheckReturnValue
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
	 * read array of doubles stored in byte array.
	 *
	 * @param reader_ reader to use.
	 *
	 * @return read array.
	 */
	@CheckReturnValue
	public static double @NotNull [] readDoubles(@NotNull Reader reader_)
	{
		// read array size
		int size = (int)readInteger(reader_, SIZE_ARRAY_LENGTH);

		// create array
		double[] reals = new double[size];

		// read elements
		for (int i = 0; i < size; ++i)
		{
			reals[i] = readDouble(reader_);
		}

		return reals;
	}

	/**
	 * read array of floats stored in byte array.
	 *
	 * @param reader_ reader to use.
	 *
	 * @return read array.
	 */
	@CheckReturnValue
	public static float @NotNull [] readFloats(@NotNull Reader reader_)
	{
		// read array size
		int size = (int)readInteger(reader_, SIZE_ARRAY_LENGTH);

		// create array
		float[] floats = new float[size];

		// read elements
		for (int i = 0; i < size; ++i)
		{
			floats[i] = readFloat(reader_);
		}

		return floats;
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
	@CheckReturnValue
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
			byte type = reader_.mBuffer[reader_.mIndex];
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
	@CheckReturnValue
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
	@CheckReturnValue
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
		int size = size_ < 0? -size_: size_;
		for (int i = 0; i < size; ++i)
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
		return writeInteger(enum_.ordinal(), SIZE_UINT8, writer_);
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
		int size = size_ < 0? -size_: size_;
		for (int i = 0; i < size; ++i)
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
	 * write array of bytes elements to buffer with constant length.
	 *
	 * @param array_  array of elements to write.
	 * @param length_ number of elements to write.
	 * @param writer_ writer to write with.
	 *
	 * @return number of written bytes.
	 */
	public static int writeByteArray(byte @NotNull [] array_, int length_, @NotNull Writer writer_)
	{
		int size = 0;

		// write array
		int i;
		int n = Math.min(length_, array_.length);
		for (i = 0; i < n; ++i)
		{
			byte b = array_[i];
			size += writeInteger(b, SIZE_INT8, writer_);
		}
		for (; i < length_; ++i)
		{
			size += writeInteger(0, SIZE_INT8, writer_);
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
	public static int writeBytes(byte @NotNull [] array_, @NotNull Writer writer_)
	{
		int size = 0;

		// write size
		size += writeInteger(array_.length, SIZE_ARRAY_LENGTH, writer_);

		// write array
		size += writeByteArray(array_, array_.length, writer_);

		return size;
	}

	/**
	 * write collection of doubles to buffer.
	 *
	 * @param collection_ collection of elements to write. Collection will be stored at the order that
	 *                    provided collection defines.
	 * @param writer_     writer to write with.
	 *
	 * @return number of written bytes.
	 */
	public static <T extends Number> int writeDoubles(@NotNull List<T> collection_,
		@NotNull Writer writer_)
	{
		int size = 0;

		// write size
		size += writeInteger(collection_.size(), SIZE_ARRAY_LENGTH, writer_);

		// write array
		for (T d : collection_)
		{
			size += writeDouble((Double)d, writer_);
		}

		return size;
	}

	/**
	 * write array of doubles to buffer.
	 *
	 * @param array_  array of elements to write. Collection will be stored at the order that
	 *                provided collection defines.
	 * @param writer_ writer to write with.
	 *
	 * @return number of written bytes.
	 */
	public static int writeDoubles(double @NotNull [] array_, @NotNull Writer writer_)
	{
		int size = 0;

		// write size
		size += writeInteger(array_.length, SIZE_ARRAY_LENGTH, writer_);

		// write array
		for (double d : array_)
		{
			size += writeDouble(d, writer_);
		}

		return size;
	}

	/**
	 * write array of floats to buffer.
	 *
	 * @param array_  array of elements to write. Collection will be stored at the order that
	 *                provided collection defines.
	 * @param writer_ writer to write with.
	 *
	 * @return number of written bytes.
	 */
	public static int writeFloats(float @NotNull [] array_, @NotNull Writer writer_)
	{
		int size = 0;

		// write size
		size += writeInteger(array_.length, SIZE_ARRAY_LENGTH, writer_);

		// write array
		for (float f : array_)
		{
			size += writeFloat(f, writer_);
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
			size += writeInteger(element.ordinal(), SIZE_UINT8, writer_);
		}

		return size;
	}

	/**
	 * write double value to byte array.
	 *
	 * @param value_  value to write.
	 * @param writer_ writer to write with.
	 *
	 * @return number of written bytes.
	 */
	public static int writeDouble(double value_, @NotNull Writer writer_)
	{
		return writeInteger(Double.doubleToRawLongBits(value_), SIZE_DOUBLE, writer_);
	}

	/**
	 * read double value from byte array.
	 *
	 * @param reader_ reader to use.
	 *
	 * @return read value.
	 */
	@CheckReturnValue
	public static double readDouble(@NotNull Reader reader_)
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
	@CheckReturnValue
	public static float readFloat(@NotNull Reader reader_)
	{
		return Float.intBitsToFloat((int)readInteger(reader_, SIZE_FLOAT));
	}
}
