package borg.framework.services;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.security.AlgorithmParameters;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import borg.framework.auxiliaries.Auxiliary;
import borg.framework.auxiliaries.Logger;
import borg.framework.structures.Pair;

public final class ArraysManager
{
	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Public Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	public static final int LENGTH_AES_BLOCK = 16;

	public static final String DEFAULT_ENCRYPTION_TYPE = "AES";

	public static final String DEFAULT_TRANSFORMATION = "AES/CTR/NoPadding";

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

	/** type of encryption **/
	private static String sEncryptType = null;

	/** current cipher **/
	private static Cipher sCipher = null;

	/** encryption algorithm parameters **/
	private static AlgorithmParameters sCipherParams;

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
			setEncryptConfig(DEFAULT_ENCRYPTION_TYPE, DEFAULT_TRANSFORMATION);
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
	 * convert byte array to it hex representation. After conversion each byte in the array will be
	 * represented as two digit hexadecimal number, when each byte in the array is an unsigned byte.
	 *
	 * @param array_ given array.
	 *
	 * @return string of hex representation of array.
	 */
	@NotNull
	@Contract(pure = true)
	public static String getArrayAsHex(byte @NotNull [] array_)
	{
		StringBuilder builder = new StringBuilder();

		// convert to string
		for (byte b : array_)
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
	@Contract(pure = true)
	public static byte @NotNull [] buildArrayFromHex(@NotNull String hex_)
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
	 * check whether first collection contains second collection.
	 *
	 * @param first_  first collection.
	 * @param second_ second collection.
	 *
	 * @return true if first collection contain second collection. False otherwise.
	 */
	@Contract(pure = true)
	public static <T> boolean isContain(@NotNull Collection<T> first_, @NotNull Collection<T> second_)
	{
		// if size of first collection smaller than second
		if (first_.size() < second_.size())
		{
			return false;
		}

		// check if every element from second exists in first
		for (T element : second_)
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
	 * @param first_  first collection.
	 * @param second_ second collection.
	 *
	 * @return true if both collection contains same elements, false otherwise.
	 */
	@Contract(pure = true)
	public static <T> boolean areEqual(@NotNull Collection<T> first_,
		@NotNull Collection<T> second_)
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
	 * @param first_  first collection.
	 * @param second_ second collection.
	 *
	 * @return true if both lists contains same elements in same order, false otherwise.
	 */
	@Contract(pure = true)
	public static <T> boolean areEqual(@NotNull List<T> first_, @NotNull List<T> second_)
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
	public synchronized static byte @NotNull [] getArraySha256(byte @NotNull [] array_)
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
			Logger.log(e);
		}

		return sha256;
	}

	/**
	 * set encryption / decryption configuration.
	 *
	 * @param type_           encryption type to set.
	 * @param transformation_ encryption transformation to set.
	 *
	 * @throws Exception if parameters for cipher is not supported.
	 */
	public static void setEncryptConfig(@NotNull String type_, @NotNull String transformation_)
		throws Exception
	{
		sEncryptType = type_;

		// create cipher
		sCipher = Cipher.getInstance(transformation_);
		sCipherParams = sCipher.getParameters();
	}

	/**
	 * encrypt byte array with given key by algorithm AES.
	 *
	 * @param array_ array to encrypt
	 * @param key_   secret key, must be 16 bytes at least.
	 *
	 * @return pair: e1 - is a encrypt array that can be decrypted with same key, e2 - is an initial
	 * vector.
	 *
	 * @throws Exception if encryption was failed.
	 */
	@NotNull
	@Contract(pure = true)
	public static Pair<byte[], byte[]> encrypt(byte @NotNull [] array_, byte @NotNull [] key_)
		throws Exception
	{
		byte[] encryptedArray;
		byte[] iv;

		// prepare valid key vector
		byte[] key = Arrays.copyOf(key_, LENGTH_AES_BLOCK);

		// create certificate
		SecretKey certificate = new SecretKeySpec(key, sEncryptType);

		// if params is null because of Android issue 58191
		if (sCipherParams == null)
		{
			// create custom initial vector
			iv = new byte[]
				{
					(byte)Auxiliary.random(),
					(byte)Auxiliary.random(),
					(byte)Auxiliary.random(),
					(byte)Auxiliary.random(),
					(byte)Auxiliary.random(),
					(byte)Auxiliary.random(),
					(byte)Auxiliary.random(),
					(byte)Auxiliary.random(),
					(byte)Auxiliary.random(),
					(byte)Auxiliary.random(),
					(byte)Auxiliary.random(),
					(byte)Auxiliary.random(),
					(byte)Auxiliary.random(),
					(byte)Auxiliary.random(),
					(byte)Auxiliary.random(),
					(byte)Auxiliary.random()
				};
		}
		else
		{
			iv = sCipherParams.getParameterSpec(IvParameterSpec.class).getIV();
		}

		// initialize cipher with certificate and initial vector
		IvParameterSpec ivSpec = new IvParameterSpec(iv);
		sCipher.init(Cipher.ENCRYPT_MODE, certificate, ivSpec);
		encryptedArray = sCipher.doFinal(array_);

		return new Pair<>(encryptedArray, iv);
	}

	/**
	 * encrypt byte array with given key by algorithm AES.
	 *
	 * @param array_ array to encrypt.
	 * @param key_   secret key, must be 16 bytes at least.
	 * @param iv_    initial vector, vector of 16 bytes.
	 *
	 * @return encrypted array.
	 *
	 * @throws Exception if encryption was failed.
	 */
	@Contract(pure = true)
	public static byte @NotNull [] encrypt(byte @NotNull [] array_,
		byte @NotNull [] key_,
		byte @NotNull [] iv_)
		throws Exception
	{
		byte[] encryptedArray;

		// prepare valid key vector
		byte[] key = Arrays.copyOf(key_, LENGTH_AES_BLOCK);

		// create certificate
		SecretKey certificate = new SecretKeySpec(key, sEncryptType);
		IvParameterSpec ivSpec = new IvParameterSpec(iv_);

		// initialize cipher with given initial vector
		sCipher.init(Cipher.ENCRYPT_MODE, certificate, ivSpec);

		encryptedArray = sCipher.doFinal(array_);

		return encryptedArray;
	}

	/**
	 * decrypt byte array with given key by algorithm AES.
	 *
	 * @param array_ array to decrypt.
	 * @param key_   secret key, must be 16 bytes at least.
	 *
	 * @return decrypted array.
	 *
	 * @throws Exception when decryption failed.
	 */
	@Contract(pure = true)
	public static byte @NotNull [] decrypt(byte @NotNull [] array_,
		byte @NotNull [] key_,
		byte @NotNull [] iv_)
		throws Exception
	{
		byte[] decryptedArray;

		// prepare valid key vector
		byte[] key = Arrays.copyOf(key_, LENGTH_AES_BLOCK);

		// create certificate
		SecretKey certificate = new SecretKeySpec(key, sEncryptType);

		// decryption
		sCipher.init(Cipher.DECRYPT_MODE, certificate, new IvParameterSpec(iv_));
		decryptedArray = sCipher.doFinal(array_);

		return decryptedArray;
	}
}
