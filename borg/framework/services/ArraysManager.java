package borg.framework.services;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

import borg.framework.auxiliaries.Logging;

public final class ArraysManager
{
	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Public Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	public static final int LENGTH_AES_BLOCK = 16;

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	private static final String NAME_HASH_ALGORITHM = "SHA-256";

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Definitions
	//////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Fields
	//////////////////////////////////////////////////////////////////////////////////////////////////

	/** digest to compute sha256 hash **/
	private static final MessageDigest sSha256Digest;

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Methods
	//////////////////////////////////////////////////////////////////////////////////////////////////

	static
	{
		MessageDigest digest;
		try
		{
			digest = MessageDigest.getInstance(NAME_HASH_ALGORITHM);
		}
		catch (Exception e)
		{
			// nothing to do here
			digest = null;
		}
		sSha256Digest = digest;
	}

	private ArraysManager()
	{
		// private constructor to avoid instantiation
	}

	/**
	 * convert byte array to it hex representation. After conversion all byte in array will be
	 * represented as two digit hexadecimal number, when all byte in array is an unsigned byte.
	 *
	 * @param array_ given array.
	 *
	 * @return string of hex representation of array.
	 */
	@NonNull
	public static String getArrayAsHex(@NonNull byte[] array_)
	{
		StringBuilder builder = new StringBuilder();

		// convert to string
		for (byte b: array_)
		{
			// append 4 MSB
			int i = (b & 0xf0) >> 4;
			if (i < 10)
			{
				builder.append((char)('0' + i));
			}
			else
			{
				builder.append((char)('a' + i - 10));
			}

			// append 4 LSB
			i = b & 0xf;
			if (i < 10)
			{
				builder.append((char)('0' + i));
			}
			else
			{
				builder.append((char)('a' + i - 10));
			}
		}

		return builder.toString();
	}

	/**
	 * build byte array from it hex representation. All byte in string must be represented by two
	 * digit hexadecimal number.
	 *
	 * @param hex_ hex representation of array.
	 *
	 * @return array from given hex representation.
	 */
	public static byte[] buildArrayFromHex(@NonNull String hex_)
	{
		// create array
		int n = hex_.length();
		byte[] array = new byte[n / 2];

		int j = 0;
		for (int i = 0; i < n; i += 2)
		{
			// build byte
			char msd = hex_.charAt(i);

			// if MSD is a number
			if (msd <= '9')
			{
				msd -= '0';
			}
			else
			{
				// if MSD is in a higher case
				if (msd <= 'F')
				{
					msd -= 'A' - 10;
				}
				else
				{
					// MSD is in a lower case
					msd -= 'a' - 10;
				}
			}

			char lsd = hex_.charAt(i + 1);

			// if LSD is a number
			if (lsd <= '9')
			{
				lsd -= '0';
			}
			else
			{
				// if LSD is in a higher case
				if (lsd <= 'F')
				{
					lsd -= 'A' - 10;
				}
				else
				{
					// LSD is in a lower case
					lsd -= 'a' - 10;
				}
			}
			// set byte
			array[j] = (byte)((msd << 4) + lsd);
			++j;
		}

		return array;
	}

	/**
	 * build list of bytes from it hex representation. All bytes in string must be represented by two
	 * digit hexadecimal number.
	 *
	 * @param hex_ hex representation of list.
	 *
	 * @return list of bytes from given hex representation.
	 */
	@NonNull
	public static ArrayList<Byte> buildListFromHex(@NonNull String hex_)
	{
		// create array
		int n = hex_.length();
		ArrayList<Byte> list = new ArrayList<>(n / 2);

		for (int i = 0; i < n; i += 2)
		{
			// build byte
			int msd = hex_.charAt(i);

			// if MSD is a number
			if (msd <= '9')
			{
				msd -= '0';
			}
			else
			{
				// if MSD is in a higher case
				if (msd <= 'F')
				{
					msd -= 'A' - 10;
				}
				else
				{
					// MSD is in a lower case
					msd -= 'a' - 10;
				}
			}

			int lsd = hex_.charAt(i + 1);

			// if LSD is a number
			if (lsd <= '9')
			{
				lsd -= '0';
			}
			else
			{
				// if LSD is in a higher case
				if (lsd <= 'F')
				{
					lsd -= 'A' - 10;
				}
				else
				{
					// LSD is in a lower case
					lsd -= 'a' - 10;
				}
			}

			// set byte
			list.add((byte)((msd << 4) + lsd));
		}

		return list;
	}

	/**
	 * check whether first collection contains second collection.
	 *
	 * @param first_ first collection.
	 * @param second_ second collection.
	 *
	 * @return true if first collection contain second collection. False otherwise.
	 */
	public static <T> boolean isContain(@NonNull Collection<T> first_, @NonNull Collection<T> second_)
	{
		// if size of first collection smaller than second
		if (first_.size() < second_.size())
		{
			return false;
		}

		// check if every element from second exists in first
		for (T element: second_)
		{
			if (first_.contains(element) == false)
			{
				return false;
			}
		}

		return true;
	}

	/**
	 * check whether two collections are equal.
	 *
	 * @param first_ first collection.
	 * @param second_ second collection.
	 *
	 * @return true if both collection contains same elements, false otherwise.
	 */
	public static <T> boolean areEqual(@NonNull Collection<T> first_,
		@NonNull Collection<T> second_)
	{
		// if both collection is same size
		if (first_.size() == second_.size())
		{
			// check if first collection contain second
			return isContain(first_, second_);
		}

		return false;
	}

	/**
	 * check whether two lists are equal.
	 *
	 * @param first_ first collection.
	 * @param second_ second collection.
	 *
	 * @return true if both lists contains same elements in same order, false otherwise.
	 */
	public static <T> boolean areEqual(@NonNull List<T> first_, @NonNull List<T> second_)
	{
		// if both lists is same size
		if (first_.size() == second_.size())
		{
			// check elements of lists
			Iterator<T> iterator1 = first_.iterator();
			Iterator<T> iterator2 = second_.iterator();
			while (iterator1.hasNext())
			{
				// if first element is not null
				T first = iterator1.next();
				T second = iterator2.next();
				if (first != null)
				{
					// if different element was found
					if (first.equals(second) == false)
					{
						return false;
					}
				}
				else
				{
					// if second element is not null
					if (second != null)
					{
						return false;
					}
				}
			}

			return true;
		}

		return false;
	}

	/**
	 * compute a hash of byte array with sha256 algorithm.
	 *
	 * @param array_ given byte array.
	 *
	 * @return computed hash.
	 */
	public synchronized static @NonNull byte[] getArraySha256(@NonNull byte[] array_)
	{
		byte[] sha256 = new byte[sSha256Digest.getDigestLength()];

		try
		{
			// compute hash as byte array
			sSha256Digest.update(array_);
			sSha256Digest.digest(sha256, 0, sha256.length);
			sSha256Digest.reset();
		}
		catch (Exception e)
		{
			Logging.logging(e);
		}

		return sha256;
	}
}
