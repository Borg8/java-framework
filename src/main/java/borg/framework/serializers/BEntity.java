package borg.framework.serializers;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import borg.framework.Constants;
import borg.framework.auxiliaries.BinaryParser;
import borg.framework.compability.CallSuper;

public abstract class BEntity implements Serializable
{
	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Public Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	private static final long serialVersionUID = Constants.VERSION_FRAMEWORK;

	private static final double REAL_PRECISION = 1e6;

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Definitions
	//////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Fields
	//////////////////////////////////////////////////////////////////////////////////////////////////

	public static final class DataIterator implements Iterator<Byte>
	{
		/** iterator instance **/
		final List<Byte> list;

		/** list size **/
		final int size;

		/** current index **/
		int index;

		public DataIterator(@NotNull List<Byte> list_)
		{
			list = list_;
			size = list_.size();
			index = 0;
		}

		@Override
		public boolean hasNext()
		{
			return index < size;
		}

		@Override
		@NotNull
		@Contract(pure = true)
		public Byte next()
		{
			Byte b = list.get(index);
			++index;

			return b;
		}

		public void back(int bytes_)
		{
			index -= bytes_;
		}

		@Override
		public void remove()
		{
			throw new Error("not implemented");
		}

		@Override
		public void forEachRemaining(Consumer<? super Byte> action_)
		{
			throw new Error("not implemented");
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Methods
	//////////////////////////////////////////////////////////////////////////////////////////////////

	protected BEntity()
	{
		// nothing to do here
	}

	protected BEntity(@SuppressWarnings("unused") @NotNull BEntity.DataIterator iterator_)
		throws Exception
	{
		// nothing to do here
	}

	@CallSuper
	public int serialize(@SuppressWarnings("unused") @NotNull List<Byte> buffer_)
	{
		return 0;
	}

	@Contract(pure = true)
	protected static long read(@NotNull BEntity.DataIterator iterator_, int size_) throws Exception
	{
		return BinaryParser.readInteger(iterator_, size_);
	}

	@Contract(pure = true)
	protected static double read(@NotNull BEntity.DataIterator iterator_) throws Exception
	{
		return read(iterator_, BinaryParser.SIZE_INT64) / REAL_PRECISION;
	}

	@Contract(pure = true)
	protected static <T extends Enum<T>> T read(@NotNull BEntity.DataIterator iterator_,
		Class<T> enum_)
		throws Exception
	{
		Method method = enum_.getMethod("values");
		//noinspection unchecked
		T[] values = (T[])method.invoke(null);
		assert values != null;
		return values[(int)read(iterator_, BinaryParser.SIZE_INT8)];
	}

	@Contract(pure = true)
	@NotNull
	protected static <T extends Number> List<T> readList(@NotNull BEntity.DataIterator iterator_,
		int size_) throws Exception
	{
		// read list length
		int length = (int)read(iterator_, BinaryParser.SIZE_INT16);

		// read elements
		List<T> list = new ArrayList<>(length);
		for (int i = 0; i < length; ++i)
		{
			//noinspection unchecked
			list.add((T)(Long)read(iterator_, size_));
		}

		return list;
	}

	@Contract(pure = true)
	@NotNull
	protected static String readString(@NotNull BEntity.DataIterator iterator_) throws Exception
	{
		// read list length
		int length = (int)read(iterator_, BinaryParser.SIZE_INT16);

		// read elements
		byte[] array = new byte[length];
		for (int i = 0; i < length; ++i)
		{
			array[i] = (byte)read(iterator_, BinaryParser.SIZE_INT8);
		}

		return new String(array);
	}

	@Contract(pure = true)
	@NotNull
	protected static <T extends BEntity> List<T> readList(@NotNull BEntity.DataIterator iterator_,
		@NotNull Class<T> class_) throws Exception
	{
		// read list length
		int length = (int)read(iterator_, BinaryParser.SIZE_INT16);

		// create constructor
		Constructor<T> constructor = class_.getConstructor(DataIterator.class);
		constructor.setAccessible(true);

		// read elements
		List<T> list = new ArrayList<>(length);
		for (int i = 0; i < length; ++i)
		{
			list.add(constructor.newInstance(iterator_));
		}

		return list;
	}

	@Contract(pure = true)
	@NotNull
	protected static <T extends BEntity, E extends Enum<E> & BTyped<? super T>> List<T> readTypedList(
		@NotNull BEntity.DataIterator iterator_,
		@NotNull Class<E> class_) throws Exception
	{
		// read list length
		int length = (int)read(iterator_, BinaryParser.SIZE_INT16);

		// read elements
		List<T> list = new ArrayList<>(length);
		for (int i = 0; i < length; ++i)
		{
			list.add(buildTypedEntity(iterator_, class_));
		}

		return list;
	}

	@SuppressWarnings("unchecked")
	@Contract(pure = true)
	@NotNull
	public static <T extends BEntity, E extends Enum<E> & BTyped<? super T>> T buildTypedEntity(
		@NotNull BEntity.DataIterator iterator_,
		@NotNull Class<E> types_) throws Exception
	{
		// read type
		int type = (int)read(iterator_, BinaryParser.SIZE_INT8);
		iterator_.back(BinaryParser.SIZE_INT8);

		// get entity class
		Method method = types_.getMethod("values");
		BTyped<T>[] values = (BTyped<T>[])method.invoke(null);
		assert values != null;
		Class<T> entityClass = (Class<T>)values[type].entityClass();

		// build entity
		Constructor<T> constructor = entityClass.getConstructor(DataIterator.class);
		constructor.setAccessible(true);

		return constructor.newInstance(iterator_);
	}

	protected static int write(long integer_, int size_, @NotNull List<Byte> buffer_)
	{
		return BinaryParser.writeInteger(integer_, size_, buffer_);
	}

	protected static int write(@NotNull Double real_, @NotNull List<Byte> buffer_)
	{
		return write((long)(real_ * REAL_PRECISION), BinaryParser.SIZE_INT64, buffer_);
	}

	protected static int write(@NotNull Enum<?> enum_, @NotNull List<Byte> buffer_)
	{
		return BinaryParser.writeEnum(enum_, buffer_);
	}

	protected static <T extends BEntity> int write(@NotNull T entity_, @NotNull List<Byte> buffer_)
	{
		return entity_.serialize(buffer_);
	}

	protected static <T extends Number> int write(@NotNull Collection<T> list_,
		int size_,
		@NotNull List<Byte> buffer_)
	{
		// write number of elements
		final int n = list_.size();
		int size = write(n, BinaryParser.SIZE_INT16, buffer_);

		// write elements
		for (T number : list_)
		{
			size += write((long)number, size_, buffer_);
		}

		return size;
	}

	protected static <T extends BEntity> int write(@NotNull Collection<T> list_,
		@NotNull List<Byte> buffer_)
	{
		// write number of elements
		final int n = list_.size();
		int size = write(n, BinaryParser.SIZE_INT16, buffer_);

		// write elements
		for (T entity : list_)
		{
			size += write(entity, buffer_);
		}

		return size;
	}

	protected static int write(@NotNull String string_, @NotNull List<Byte> buffer_)
	{
		// write number of elements
		final int n = string_.length();
		int size = write(n, BinaryParser.SIZE_INT16, buffer_);

		// write elements
		for (int i = 0; i < n; ++i)
		{
			size += write(string_.charAt(i), BinaryParser.SIZE_INT8, buffer_);
		}

		return size;
	}
}
