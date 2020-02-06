package borg.framework.serializers;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import borg.framework.auxiliaries.Logging;
import borg.framework.compability.CallSuper;
import borg.framework.resources.Constants;
import borg.framework.services.ArraysManager;

public abstract class REntity implements Serializable
{
	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Public Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	/** key mapped the type of the entity **/
	public static final String TAG_TYPE = "type";

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

	protected REntity()
	{
		// nothing to do here
	}

	protected REntity(@SuppressWarnings("unused") @Nullable HashMap<String, Object> map_)
	{
		// nothing to do here
	}

	@NotNull
	@Contract(pure = true)
	public final HashMap<String, Object> toMap()
	{
		// build map
		HashMap<String, Object> map = new HashMap<>();
		buildMap(map);

		return map;
	}

	@Override
	@NotNull
	public final String toString()
	{
		return EntityParser.buildJson(toMap()).toString();
	}

	/**
	 * build typed entity, where the type is mapped for key {@link REntity#TAG_TYPE}.
	 *
	 * @param map_     map to build entity from.
	 * @param types_   enum mapped entity type to entity class.
	 * @param default_ default value if built was not succeeded.
	 * @param <T>      the base type of the expected entity.
	 * @param <E>      the type of enumerator that mapped the entity type to the entity class.
	 *
	 * @return built entity.
	 */
	@Contract(value = "null, _, _ -> param3", pure = true)
	@SuppressWarnings("unchecked")
	public static <T extends REntity, E extends Enum<E> & Typed<? super T>> T buildTypedEntity(
		@Nullable HashMap<String, Object> map_,
		@NotNull Class<E> types_,
		@Nullable T default_)
	{
		if (map_ != null)
		{
			try
			{
				// get type of entity from map
				Object type = map_.get(TAG_TYPE);
				if (type != null)
				{
					// get entity class
					Method method = types_.getMethod("values");
					Typed<T>[] values = (Typed<T>[])method.invoke(null);
					assert values != null;
					Class<T> entityClass = (Class<T>)values[((Number)type).intValue()].entityClass();

					return buildEntity(map_, entityClass, default_);
				}
			}
			catch (Exception e)
			{
				Logging.logging(e);
			}
		}

		return default_;
	}

	/**
	 * build typed entity list, where the type is mapped for key {@link REntity#TAG_TYPE}.
	 *
	 * @param maps_  map to build list from.
	 * @param types_ enum mapped entity type to entity class.
	 * @param <T>    the base type of the expected entity.
	 * @param <E>    the type of enumerator that mapped the entity type to the entity class.
	 *
	 * @return built entity list.
	 */
	@Contract(pure = true)
	public static <T extends REntity, E extends Enum<E> & Typed<? super T>> ArrayList<T> buildTypedList(
		@Nullable ArrayList<HashMap<String, Object>> maps_,
		@NotNull Class<E> types_,
		@Nullable ArrayList<T> default_)
	{
		if (maps_ != null)
		{
			ArrayList<T> list = new ArrayList<>(maps_.size());
			for (HashMap<String, Object> map : maps_)
			{
				// build entity
				T entity = buildTypedEntity(map, types_, null);
				if (entity != null)
				{
					list.add(entity);
				}
			}

			return list;
		}

		return default_;
	}

	/**
	 * build entity.
	 *
	 * @param map_     map to build entity from.
	 * @param type_    class of the entity to build.
	 * @param default_ default value if built was not succeeded.
	 * @param <T>      the type of the expected entity.
	 *
	 * @return built entity.
	 */
	@Contract(value = "null, _, _ -> param3", pure = true)
	public static <T extends REntity> T buildEntity(@Nullable HashMap<String, Object> map_,
		@NotNull Class<T> type_,
		@Nullable T default_)
	{
		if (map_ != null)
		{
			try
			{
				// get entity constructor
				Constructor<T> constructor = type_.getConstructor(HashMap.class);
				constructor.setAccessible(true);
				return constructor.newInstance(map_);
			}
			catch (Exception e)
			{
				Logging.logging(e);
			}
		}

		return default_;
	}

	/**
	 * serialize object.
	 *
	 * @param object_ object to serialize.
	 *
	 * @return object serialization.
	 */
	@Contract(value = "null->null", pure = true)
	@Nullable
	public static Object serializeObject(@Nullable Object object_)
	{
		// if object is real
		if (object_ instanceof Integer)
		{
			return serialize((long)(int)object_);
		}

		// if object is real
		if (object_ instanceof Float)
		{
			return serialize(((Float)object_).doubleValue());
		}

		// if object is boolean
		if (object_ instanceof Boolean)
		{
			return serialize((boolean)object_);
		}

		// if object is enumerator
		if (object_ instanceof Enum)
		{
			return serialize((Enum<?>)object_);
		}

		// if object is entity
		if (object_ instanceof REntity)
		{
			return serialize(((REntity)object_));
		}

		// if object is byte array
		if (object_ instanceof byte[])
		{
			return serialize((byte[])object_);
		}

		// if object is list
		if (object_ instanceof List)
		{
			return serialize((List<?>)object_);
		}

		// if object is map
		if (object_ instanceof Map)
		{
			//noinspection unchecked
			return serialize((Map<String, ?>)object_);
		}

		return serialize(object_);
	}

	@Contract(value = "null->null", pure = true)
	protected static <T> T serialize(@Nullable T object_)
	{
		return object_;
	}

	@Contract(value = "null->null", pure = true)
	@Nullable
	protected static Double serialize(@Nullable Float number_)
	{
		if (number_ != null)
		{
			return number_.doubleValue();
		}

		return null;
	}

	@Contract(pure = true)
	protected static int serialize(boolean boolean_)
	{
		return serialize(BooleanType.fromBoolean(boolean_).ordinal());
	}

	@Nullable
	@Contract(value = "null->null", pure = true)
	protected static Integer serialize(@Nullable Enum<?> enum_)
	{
		if (enum_ != null)
		{
			return serialize(enum_.ordinal());
		}

		return null;
	}

	@Nullable
	@Contract(value = "null->null", pure = true)
	protected static <T extends REntity> HashMap<String, Object> serialize(@Nullable T entity_)
	{
		if (entity_ != null)
		{
			return entity_.toMap();
		}

		return null;
	}

	@Nullable
	@Contract(value = "null->null", pure = true)
	protected static String serialize(@Nullable byte[] array_)
	{
		if ((array_ != null) && (array_.length > 0))
		{
			return ArraysManager.getArrayAsHex(array_);
		}

		return null;
	}

	@Nullable
	@Contract(value = "null->null", pure = true)
	protected static ArrayList<Object> serialize(@Nullable Collection<?> list_)
	{
		if ((list_ != null) && (list_.isEmpty() == false))
		{
			ArrayList<Object> list = new ArrayList<>(list_.size());
			for (Object object : list_)
			{
				list.add(serializeObject(object));
			}

			return list;
		}

		return null;
	}

	@Nullable
	@Contract(value = "null->null", pure = true)
	protected static HashMap<String, Object> serialize(@Nullable Map<String, ?> map_)
	{
		if ((map_ != null) && (map_.isEmpty() == false))
		{
			HashMap<String, Object> map = new HashMap<>(map_.size());
			for (Map.Entry<String, ?> entry : map_.entrySet())
			{
				map.put(entry.getKey(), serializeObject(entry.getValue()));
			}

			return map;
		}

		return null;
	}

	@Contract(pure = true)
	protected static <T> T readField(@Nullable HashMap<String, Object> map_,
		@NotNull String key_,
		@Nullable T default_)
	{
		if (map_ != null)
		{
			Object field = map_.get(key_);
			if (field != null)
			{
				//noinspection unchecked
				return (T)field;
			}
		}

		return default_;
	}

	@Contract(pure = true)
	protected static double readField(@Nullable HashMap<String, Object> map_,
		@NotNull String key_,
		double default_)
	{
		if (map_ != null)
		{
			Object field = map_.get(key_);
			if (field != null)
			{
				return ((Number)field).doubleValue();
			}
		}

		return default_;
	}

	@Contract(pure = true)
	protected static long readField(@Nullable HashMap<String, Object> map_,
		@NotNull String key_,
		long default_)
	{
		if (map_ != null)
		{
			Object field = map_.get(key_);
			if (field != null)
			{
				return ((Number)field).longValue();
			}
		}

		return default_;
	}

	@Contract(pure = true)
	protected static Boolean readField(@Nullable HashMap<String, Object> map_,
		@NotNull String key_,
		@Nullable Boolean default_)
	{
		if (map_ != null)
		{
			Object field = map_.get(key_);
			if (field != null)
			{
				return BooleanType.values()[((Number)field).intValue()].bool;
			}
		}

		return default_;
	}

	@Contract(pure = true)
	protected static byte[] readField(@Nullable HashMap<String, Object> map_,
		@NotNull String key_,
		@Nullable byte[] default_)
	{
		if (map_ != null)
		{
			Object field = map_.get(key_);
			if (field != null)
			{
				return ArraysManager.buildArrayFromHex((String)field);
			}
		}

		return default_;
	}

	@Nullable
	@Contract(pure = true)
	protected static <T extends REntity> T readField(@Nullable HashMap<String, Object> map_,
		@NotNull String key_,
		@NotNull Class<T> class_)
	{
		if (map_ != null)
		{
			// get map
			HashMap<String, Object> map = readField(map_, key_);

			if (map != null)
			{
				try
				{
					// get constructor
					Constructor<T> constructor = class_.getConstructor(HashMap.class);
					constructor.setAccessible(true);

					// return instance
					return constructor.newInstance(map);
				}
				catch (Exception e)
				{
					Logging.logging(e);
				}
			}
		}

		return null;
	}

	@Nullable
	@Contract(pure = true)
	protected static <T> T readField(@Nullable HashMap<String, Object> map_, @NotNull String key_)
	{
		if (map_ != null)
		{
			Object field = map_.get(key_);
			if (field != null)
			{
				//noinspection unchecked
				return (T)field;
			}
		}

		return null;
	}

	@SuppressWarnings("unchecked") // exception will be thrown
	@Contract(pure = true)
	protected static <T> ArrayList<T> readField(@Nullable HashMap<String, Object> map_,
		@NotNull String key_,
		@Nullable ArrayList<T> default_)
	{
		if (map_ != null)
		{
			Object field = map_.get(key_);
			if (field != null)
			{
				return (ArrayList<T>)field;
			}
		}

		return default_;
	}

	@Contract(pure = true)
	protected static <T extends REntity> ArrayList<T> readField(
		@Nullable HashMap<String, Object> map_,
		@NotNull String key_,
		@NotNull Class<T> class_,
		@Nullable ArrayList<T> default_)
	{
		if (map_ != null)
		{
			ArrayList<HashMap<String, Object>> maps;
			//noinspection unchecked
			maps = (ArrayList<HashMap<String, Object>>)map_.get(key_);
			if (maps != null)
			{
				ArrayList<T> objects = new ArrayList<>(maps.size());
				try
				{
					// get constructor
					Constructor<T> constructor = class_.getConstructor(HashMap.class);
					constructor.setAccessible(true);

					// parse all maps
					for (HashMap<String, Object> map : maps)
					{
						// create an object
						objects.add(constructor.newInstance(map));
					}
				}
				catch (Exception e)
				{
					Logging.logging(e);
				}

				return objects;
			}
		}

		return default_;
	}

	@Contract(pure = true)
	@NotNull
	protected static <T extends Enum<T>> T readField(@Nullable HashMap<String, Object> map_,
		@NotNull String key_,
		@NotNull T default_)
	{
		try
		{
			// get values
			Method method = default_.getClass().getMethod("values");
			//noinspection unchecked
			T[] values = (T[])method.invoke(null);

			assert values != null;
			return values[(int)readField(map_, key_, default_.ordinal())];
		}
		catch (Exception e)
		{
			Logging.logging(e);
		}

		return default_;
	}

	@Contract(pure = true)
	@NotNull
	public static List<Integer> readIntegers(@Nullable HashMap<String, Object> map_,
		@NotNull String key_)
	{
		List<Long> list = readField(map_, key_, new ArrayList<>());

		List<Integer> integers = new ArrayList<>(list.size());
		for (long l : list)
		{
			integers.add((int)l);
		}

		return integers;
	}

	@CallSuper
	protected void buildMap(@SuppressWarnings("unused") @NotNull HashMap<String, Object> map_)
	{
		// nothing to do here
	}
}
