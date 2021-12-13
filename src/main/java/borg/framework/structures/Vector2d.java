package borg.framework.structures;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

import borg.framework.Constants;

public class Vector2d implements Serializable
{
	private static final long serialVersionUID = Constants.VERSION_FRAMEWORK;

	private static final double HALF_PI = Math.PI / 2;

	/** x component **/
	public double x;

	/** y component **/
	public double y;

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
	private double mLastRotation;

	public Vector2d()
	{
		x = 0;
		y = 0;

		mLastSize = 0;
		mLastSize2 = 0;

		mDirX = 0;
		mDirY = 0;
		mLastDirection = 0;
		mLastRotation = HALF_PI;
		mLastCos = 0;
		mLastSin = 1;
	}

	public Vector2d(@NotNull Vector2d vector_)
	{
		x = vector_.x;
		y = vector_.y;

		mLastSize = vector_.mLastSize;
		mLastSize2 = vector_.mLastSize2;

		mDirX = vector_.mDirX;
		mDirY = vector_.mDirY;
		mLastDirection = vector_.mLastDirection;
		mLastRotation = vector_.mLastRotation;
		mLastCos = vector_.mLastCos;
		mLastSin = vector_.mLastSin;
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
			mLastDirection = Math.atan2(y, x);

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
		// compute size
		double size2 = x * x + y * y;
		if (mLastSize2 != size2)
		{
			mLastSize2 = size2;
			mLastSize = Math.sqrt(size2);
		}

		return mLastSize;
	}

	/**
	 * @return size^2 (works faster than {@link Vector2d#getSize()}).
	 */
	@Contract(pure = true)
	public final double getSize2()
	{
		return x * x + y * y;
	}

	/**
	 * add vector to this one.
	 *
	 * @param vector_ vector two add.
	 *
	 * @return this vector after the addition.
	 */
	@NotNull
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
	@NotNull
	public final Vector2d product(double scalar_)
	{
		x *= scalar_;
		y *= scalar_;

		return this;
	}

	/**
	 * inner multiplication if the vector by given vector.
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
			mLastSize *= factor;
			mLastSize2 = mLastSize * mLastSize;
		}
		else
		{
			// set vector components
			x = size_;
			y = 0;

			// recompute parameters
			mLastSize = size_;
			mLastSize2 = size_ * size_;
			mDirX = x;
			mDirY = y;
			mLastDirection = 0;
			mLastRotation = HALF_PI;
			mLastCos = 0;
			mLastSin = 1;
		}

		return r;
	}

	/**
	 * set vector direction.
	 *
	 * @param angle_ direction to set.
	 */
	public final void setDirection(double angle_)
	{
		// TODO optimize
		x = getSize();
		y = 0;

		// rotate
		rotate(angle_);

		// recompute parameters
		mLastDirection = angle_;
		mDirX = x;
		mDirY = y;
	}

	/**
	 * rotate vector by given angle.
	 *
	 * @param angle_ angle to rotate.
	 */
	public final void rotate(double angle_)
	{
		// if cos and sin were not computed
		if (angle_ != mLastRotation)
		{
			mLastCos = Math.cos(angle_);
			mLastSin = Math.sin(angle_);
			mLastRotation = angle_;
		}

		// rotate
		double temp = x;
		x = x * mLastCos - y * mLastSin;
		y = temp * mLastSin + y * mLastCos;

		// invalidate parameters
		mLastDirection = 0;
		mDirX = 0;
		mDirY = 0;
	}

	@Override
	@Contract(pure = true)
	@NotNull
	public String toString()
	{
		return String.format("x = %f, y = %f", x, y);
	}
}
