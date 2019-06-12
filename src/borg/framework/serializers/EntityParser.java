package borg.framework.serializers;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;

import borg.framework.auxiliaries.Logging;

public final class EntityParser
{
	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Public Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Definitions
	//////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Fields
	//////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Methods
	//////////////////////////////////////////////////////////////////////////////////////////////////

	private EntityParser()
	{
		// private constructor to prevent instantiation.
	}

	/**
	 * build JSON represent the entity map.
	 *
	 * @param map_ map represents given object.
	 *
	 * @return JSON object represent the entity.
	 */
	@Contract(value = "null->null", pure = true)
	public static JSONObject buildJson(@Nullable Map<String, Object> map_)
	{
		if (map_ != null)
		{
			JSONObject json = new JSONObject();
			for (Map.Entry<String, Object> entry : map_.entrySet())
			{
				try
				{
					json.putOpt(entry.getKey(), buildJson(entry.getValue()));
				}
				catch (Exception e)
				{
					Logging.logging(Level.WARNING, e);
				}
			}

			return json;
		}

		return null;
	}

	/**
	 * build JSON array represents the list.
	 *
	 * @param list_ list to build an JSON from it.
	 *
	 * @return JSON array represents the list.
	 */
	@Contract(value = "null->null", pure = true)
	public static JSONArray buildJson(@Nullable Collection<?> list_)
	{
		if (list_ != null)
		{
			JSONArray array = new JSONArray();
			for (Object object : list_)
			{
				try
				{
					array.put(buildJson(object));
				}
				catch (Exception e)
				{
					Logging.logging(Level.WARNING, e);
				}
			}

			return array;
		}

		return null;
	}

	/**
	 * build entity map from JSON represents the entity.
	 *
	 * @param json_ JSON to build the map from.
	 *
	 * @return built map.
	 */
	@Contract(value = "null->null", pure = true)
	public static HashMap<String, Object> buildMap(@Nullable JSONObject json_)
	{
		if (json_ != null)
		{
			HashMap<String, Object> map = new HashMap<>();
			Iterator<String> iterator = json_.keys();

			// build map
			while (iterator.hasNext())
			{
				try
				{
					// get mapped object
					String key = iterator.next();
					Object object = json_.get(key);

					// if object is not null
					if (object != null)
					{
						// map object
						map.put(key, serializeJson(object));
					}
				}
				catch (Exception e)
				{
					Logging.logging(Level.WARNING, e);
				}
			}

			return map;
		}

		return null;
	}

	/**
	 * build entity list from JSON array.
	 *
	 * @param array_ JSON array to build from.
	 *
	 * @return built list.
	 */
	@Contract(value = "null->null", pure = true)
	public static ArrayList<Object> buildMap(@Nullable JSONArray array_)
	{
		if (array_ != null)
		{
			int n = array_.length();
			ArrayList<Object> list = new ArrayList<>(n);
			for (int i = 0; i < n; ++i)
			{
				try
				{
					list.add(serializeJson(array_.get(i)));
				}
				catch (JSONException e)
				{
					Logging.logging(Level.WARNING, e);
				}
			}

			return list;
		}

		return null;
	}

	@Contract(value = "null->null", pure = true)
	private static Object buildJson(@Nullable Object object_)
	{
		// if object is entity
		if (object_ instanceof REntity)
		{
			return buildJson(((REntity)object_).toMap());
		}

		// if object is a map
		if (object_ instanceof Map)
		{
			return buildJson(object_);
		}

		// if object is a list
		if (object_ instanceof Collection)
		{
			return buildJson((Collection<?>)object_);
		}

		return object_;
	}

	@Contract(value = "null->null", pure = true)
	private static Object serializeJson(@Nullable Object object_)
	{
		if (object_ != null)
		{
			// if object is JSON
			if (object_ instanceof JSONObject)
			{
				return buildMap((JSONObject)object_);
			}

			// if object is JSON array
			if (object_ instanceof JSONArray)
			{
				return buildMap((JSONArray)object_);
			}

			return REntity.serializeObject(object_);
		}

		return null;
	}
}
