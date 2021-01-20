package borg.framework.structures;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.text.MessageFormat;

import borg.framework.Constants;

public class Vector2d implements Serializable
{
	private static final long serialVersionUID = Constants.VERSION_FRAMEWORK;

	private static final double HALF_PI = Math.PI / 2;

	/** x component **/
	public double x;

	/** y component **/
	public double y;

	/** x value for which size was computed **/
	private double mSizeX;

	/** y value for which size was computed **/
	private double mSizeY;

	/** last computed size **/
	private double mLastSize;

	/** last size square **/
	private double mLastSize2;

	/** x value for which direction was computed **/
	private double mDirX;

	/** y value for which direction was computed **/
	private double mDirY;

	/** last computed direction **/
	private double mLastDirection;

	/** last computed cos **/
	private double mLastCos;

	/** last computed sin **/
	private double mLastSin;

	/** angle for which the cos and sin were computed **/
	private double mLastAngle;

	public Vector2d()
	{
		x = 0;
		y = 0;
		mSizeX = 0;
		mSizeY = 0;
		mDirX = 0;
		mDirY = 0;
		mLastSize = 0;
		mLastSize2 = 0;
		mLastDirection = HALF_PI;
		mLastAngle = 0;
		mLastCos = 1;
		mLastSin = 0;
	}

	public Vector2d(double x_, double y_)
	{
		x = x_;
		y = y_;
		mSizeX = x_ + 1;
		mDirX = mSizeX;
	}

	/**
	 * update vector coordinates from given vector.
	 *
	 * @param vector_ given vector.
	 */
	public final void update(@NotNull Vector2d vector_)
	{
		x = vector_.x;
		y = vector_.y;
	}

	/**
	 * @return vector direction in radians. 0 radians when (x == 0) && (y < 0).
	 */
	@Contract(pure = true)
	public final double getDirection()
	{
		// if vector coordinates was changed
		if ((x != mDirX) || (y != mDirY))
		{
			if (y == 0)
			{
				if (x < 0)
				{
					mLastDirection = -HALF_PI;
				}
				else
				{
					mLastDirection = HALF_PI;
				}
			}
			else
			{
				double angle = Math.atan(x / y);
				mLastDirection = y < 0? -angle: Math.PI - angle;
			}

			// store vector coordinates
			mDirX = x;
			mDirY = y;
		}

		return mLastDirection;
	}

	/**
	 * @return vector length.
	 */
	@Contract(pure = true)
	public final double getSize()
	{
		// if vector coordinates was changed
		if ((x != mSizeX) || (y != mSizeY))
		{
			// compute size
			double size2 = x * x + y * y;
			if (mLastSize2 != size2)
			{
				mLastSize2 = size2;
				mLastSize = Math.sqrt(x * x + y * y);
			}

			// store vector coordinates
			mSizeX = x;
			mSizeY = y;
		}

		return mLastSize;
	}

	/**
	 * @return size^2 (works faster than {@link Vector2d#getSize()}).
	 */
	@Contract(pure = true)
	public final double getSize2()
	{
		double size = x * x + y * y;
		mLastSize2 = size;
		return size;
	}

	/**
	 * add vector to this one.
	 *
	 * @param vector_ vector two add.
	 *
	 * @return this vector after the addition.
	 */
	@Contract(pure = true)
	public final Vector2d add(@NotNull Vector2d vector_)
	{
		x += vector_.x;
		y += vector_.y;

		return this;
	}

	/**
	 * scalar product of vector.
	 *
	 * @param scalar_ scalar to product.
	 *
	 * @return this vector after production.
	 */
	@Contract(pure = true)
	public final Vector2d product(double scalar_)
	{
		x *= scalar_;
		y *= scalar_;

		return this;
	}

	/**
	 * dotting vector by given vector.
	 *
	 * @param vector_ given vector.
	 *
	 * @return dotting result.
	 */
	@Contract(pure = true)
	public final double product(@NotNull Vector2d vector_)
	{
		return (x * vector_.x) + (y * vector_.y);
	}

	/**
	 * resize the vector to given size.
	 *
	 * @param size_ size of vector after resizing.
	 *
	 * @return previous vector size.
	 */
	public final double resize(double size_)
	{
		double r = getSize();

		if (r > 0)
		{
			// get scale factor
			double factor = size_ / r;

			// change vector components
			x *= factor;
			y *= factor;

			// recompute parameters
			mLastSize2 *= factor * factor;
			mLastSize *= factor;
			mSizeX = x;
			mSizeY = y;
		}

		return r;
	}

	/**
	 * rotate vector by given angle.
	 *
	 * @param angle_ angle to rotate.
	 */
	public final void rotate(double angle_)
	{
		// if cos and sin were not computed
		if (angle_ != mLastAngle)
		{
			mLastCos = Math.cos(angle_);
			mLastSin = Math.sin(angle_);
			mLastAngle = angle_;
		}

		// rotate
		double temp = x;
		x = x * mLastCos - y * mLastSin;
		y = temp * mLastSin + y * mLastCos;

		// recompute parameters
		mLastDirection = angle_;
		mDirX = x;
		mDirY = y;
	}

	@Override
	@Contract(pure = true)
	@NotNull
	public String toString()
	{
		return MessageFormat.format("x = {0}, y = {1}\nsize = {2}, angle = {3}",
			Double.toString(x),
			Double.toString(y),
			getSize(),
			getDirection());
	}
}
