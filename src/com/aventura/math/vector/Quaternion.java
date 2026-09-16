package com.aventura.math.vector;

import com.aventura.math.Constants;
import com.aventura.math.tools.MathTools;
import com.aventura.tools.tracing.Tracer;

/**
 * ------------------------------------------------------------------------------
 * MIT License
 *
 * Copyright (c) 2016-2026 Olivier BARRY
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * ------------------------------------------------------------------------------
 *
 * A Quaternion (x,y,z,w) represents a rotation: w is the scalar (real) part, (x,y,z) is the
 * vector (imaginary) part. Component order follows Vector4's (x,y,z,w), scalar last, for
 * visual/indexing consistency with the rest of this lib (and with the OpenGL/GLM convention).
 *
 * A Quaternion used to represent a rotation is expected to be unit length (normalize() makes
 * this so); most methods here (toMatrix3/4(), slerp(), toAxisAngle()) assume this and do not
 * re-normalize defensively, exactly like the rest of the lib assumes a Rotation's Matrix4 is
 * already a valid rotation matrix rather than re-checking it on every use.
 *
 * @author Olivier BARRY
 * @since September 2026
 *
 */
public class Quaternion {

	// Components of the Quaternion: w = scalar part, (x,y,z) = vector part
	protected float x;
	protected float y;
	protected float z;
	protected float w;

	/**
	 * Identity Quaternion (0,0,0,1) - no rotation.
	 */
	public Quaternion() {
		this.x = 0;
		this.y = 0;
		this.z = 0;
		this.w = 1;
	}

	public Quaternion(float x, float y, float z, float w) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}

	/**
	 * Copy constructor
	 * @param q the Quaternion to copy
	 */
	public Quaternion(Quaternion q) {
		this.x = q.x;
		this.y = q.y;
		this.z = q.z;
		this.w = q.w;
	}

	/**
	 * Build the Quaternion representing a rotation of angleRadians around axis.
	 * axis does not need to be normalized, this constructor normalizes a copy of it.
	 * @param axis the rotation axis
	 * @param angleRadians the rotation angle, in radians
	 */
	public Quaternion(Vector3 axis, float angleRadians) {
		Vector3 a = new Vector3(axis);
		a.normalize();
		float half = angleRadians/2;
		float sinHalf = (float)Math.sin(half);
		this.x = a.getX()*sinHalf;
		this.y = a.getY()*sinHalf;
		this.z = a.getZ()*sinHalf;
		this.w = (float)Math.cos(half);
	}

	/**
	 * Build the Quaternion equivalent to a rotation Matrix3 (assumed to be a valid, orthonormal
	 * rotation matrix - see Rotation.isRotation()). Uses Shepperd's method (branch on the largest
	 * of trace and the 3 diagonal elements) to stay numerically stable across the whole rotation
	 * range, rather than a naive formula that loses precision or divides by ~0 near some angles.
	 * @param m the rotation Matrix3
	 */
	public Quaternion(Matrix3 m) {
		initFromRotationMatrix(
			m.get(0,0), m.get(0,1), m.get(0,2),
			m.get(1,0), m.get(1,1), m.get(1,2),
			m.get(2,0), m.get(2,1), m.get(2,2)
		);
	}

	/**
	 * Build the Quaternion equivalent to the rotation part (upper-left 3x3) of a Matrix4 (assumed
	 * to be a valid rotation matrix - see Rotation.isRotation()). See Quaternion(Matrix3) above.
	 * @param m the rotation Matrix4
	 */
	public Quaternion(Matrix4 m) {
		initFromRotationMatrix(
			m.get(0,0), m.get(0,1), m.get(0,2),
			m.get(1,0), m.get(1,1), m.get(1,2),
			m.get(2,0), m.get(2,1), m.get(2,2)
		);
	}

	/**
	 * Shepperd's method: branch on whichever of (trace, m00, m11, m22) is largest, to always divide
	 * by the largest possible value (avoids a near-zero divisor, which a naive single-formula
	 * conversion suffers from close to a 180-degree rotation around some axes).
	 */
	private void initFromRotationMatrix(float m00, float m01, float m02,
	                                     float m10, float m11, float m12,
	                                     float m20, float m21, float m22) {
		float trace = m00 + m11 + m22;
		if (trace > 0) {
			float s = (float)Math.sqrt(trace + 1.0) * 2; // s = 4*w
			this.w = 0.25f*s;
			this.x = (m21 - m12)/s;
			this.y = (m02 - m20)/s;
			this.z = (m10 - m01)/s;
		} else if (m00 > m11 && m00 > m22) {
			float s = (float)Math.sqrt(1.0 + m00 - m11 - m22) * 2; // s = 4*x
			this.x = 0.25f*s;
			this.y = (m01 + m10)/s;
			this.z = (m02 + m20)/s;
			this.w = (m21 - m12)/s;
		} else if (m11 > m22) {
			float s = (float)Math.sqrt(1.0 + m11 - m00 - m22) * 2; // s = 4*y
			this.y = 0.25f*s;
			this.x = (m01 + m10)/s;
			this.z = (m12 + m21)/s;
			this.w = (m02 - m20)/s;
		} else {
			float s = (float)Math.sqrt(1.0 + m22 - m00 - m11) * 2; // s = 4*z
			this.z = 0.25f*s;
			this.x = (m02 + m20)/s;
			this.y = (m12 + m21)/s;
			this.w = (m10 - m01)/s;
		}
	}

	@Override
	public String toString() {
		return "Quaternion ["+x+", "+y+", "+z+", "+w+"]";
	}

	/**
	 * Set the coordinate of rank i (0=x, 1=y, 2=z, 3=w) with value v
	 * @param i the rank of the coordinate to set
	 * @param v the value to set
	 * @throws IndexOutOfBoundException
	 */
	public void set(int i, float v) throws IndexOutOfBoundException {
		switch (i) {
		case 0:
			this.x = v;
			break;
		case 1:
			this.y = v;
			break;
		case 2:
			this.z = v;
			break;
		case 3:
			this.w = v;
			break;
		default:
			throw new IndexOutOfBoundException("Index out of bound while setting coordinate ("+i+") of Quaternion");
		}
	}

	/**
	 * Get the coordinate of rank i (0=x, 1=y, 2=z, 3=w)
	 * @param i the rank of the coordinate to get
	 * @return the value of the coordinate
	 * @throws IndexOutOfBoundException
	 */
	public float get(int i) throws IndexOutOfBoundException {
		switch (i) {
		case 0:
			return this.x;
		case 1:
			return this.y;
		case 2:
			return this.z;
		case 3:
			return this.w;
		default:
			throw new IndexOutOfBoundException("Index out of bound while getting coordinate ("+i+") of Quaternion");
		}
	}

	public float getX() {
		return this.x;
	}

	public float getY() {
		return this.y;
	}

	public float getZ() {
		return this.z;
	}

	public float getW() {
		return this.w;
	}

	public void setX(float v) {
		this.x = v;
	}

	public void setY(float v) {
		this.y = v;
	}

	public void setZ(float v) {
		this.z = v;
	}

	public void setW(float v) {
		this.w = v;
	}

	/**
	 * @return the length (norm) of this Quaternion
	 */
	public float length() {
		return (float)Math.sqrt(lengthSquared());
	}

	/**
	 * Avoids the sqrt() of length() when only a comparison is needed.
	 * @return the squared length (norm) of this Quaternion
	 */
	public float lengthSquared() {
		return this.x*this.x + this.y*this.y + this.z*this.z + this.w*this.w;
	}

	/**
	 * Normalize this Quaternion (same direction, length becomes 1). A rotation Quaternion is
	 * expected to be unit length; this is what makes it so. This Quaternion is modified.
	 * @return this Quaternion (modified)
	 */
	public Quaternion normalize() {
		float length = this.length();
		this.x/=length;
		this.y/=length;
		this.z/=length;
		this.w/=length;
		return this;
	}

	/**
	 * @return a new Quaternion, the conjugate of this one: (-x,-y,-z,w)
	 */
	public Quaternion conjugate() {
		return new Quaternion(-this.x, -this.y, -this.z, this.w);
	}

	/**
	 * @return a new Quaternion, the inverse of this one: conjugate()/lengthSquared(). For a unit
	 * Quaternion (the expected case for a rotation), this is equal to conjugate() (dividing by 1),
	 * but this method does not assume unit length.
	 */
	public Quaternion inverse() {
		float ls = this.lengthSquared();
		return new Quaternion(-this.x/ls, -this.y/ls, -this.z/ls, this.w/ls);
	}

	/**
	 * Hamilton product: composition of rotations. this.times(q) represents "this rotation, then
	 * q" is NOT the convention here -- as with Matrix.times(Matrix) elsewhere in this lib (e.g.
	 * Matrix4 C = A.times(B) means C = A*B), this.times(q) means this*q in the usual left-operand
	 * sense: applying the resulting Quaternion to a vector first applies q, then this.
	 * @param q the other Quaternion
	 * @return a new Quaternion, the Hamilton product this*q
	 */
	public Quaternion times(Quaternion q) {
		float rw = this.w*q.w - this.x*q.x - this.y*q.y - this.z*q.z;
		float rx = this.w*q.x + this.x*q.w + this.y*q.z - this.z*q.y;
		float ry = this.w*q.y - this.x*q.z + this.y*q.w + this.z*q.x;
		float rz = this.w*q.z + this.x*q.y - this.y*q.x + this.z*q.w;
		return new Quaternion(rx, ry, rz, rw);
	}

	/**
	 * Hamilton product this = this*q (see times(Quaternion) above). This Quaternion is modified.
	 * @param q the other Quaternion
	 */
	public void timesEquals(Quaternion q) {
		float rw = this.w*q.w - this.x*q.x - this.y*q.y - this.z*q.z;
		float rx = this.w*q.x + this.x*q.w + this.y*q.z - this.z*q.y;
		float ry = this.w*q.y - this.x*q.z + this.y*q.w + this.z*q.x;
		float rz = this.w*q.z + this.x*q.y - this.y*q.x + this.z*q.w;
		this.x = rx;
		this.y = ry;
		this.z = rz;
		this.w = rw;
	}

	/**
	 * @param q the other Quaternion
	 * @return the dot (scalar) product of this Quaternion with q
	 */
	public float dot(Quaternion q) {
		return this.x*q.x + this.y*q.y + this.z*q.z + this.w*q.w;
	}

	/**
	 * Spherical linear interpolation between q1 and q2, at t in [0,1] (t=0 returns q1's rotation,
	 * t=1 returns q2's rotation). Both q1 and q2 are expected to be unit Quaternions.
	 * Takes the shorter of the two paths around the 4D hypersphere (negates q2 if the dot product
	 * is negative - q and -q represent the same rotation, but interpolating naively between them
	 * can take the "long way round"). Falls back to linear interpolation (then normalizes) when q1
	 * and q2 are almost identical, since sin(omega) would otherwise be a near-zero divisor.
	 * @param q1 the starting Quaternion
	 * @param q2 the ending Quaternion
	 * @param t the interpolation factor, expected in [0,1]
	 * @return a new Quaternion, the interpolated rotation
	 */
	public static Quaternion slerp(Quaternion q1, Quaternion q2, float t) {
		float cosOmega = q1.dot(q2);

		float x2 = q2.x, y2 = q2.y, z2 = q2.z, w2 = q2.w;
		if (cosOmega < 0) {
			// q2 and -q2 represent the same rotation; negate to take the shorter path
			cosOmega = -cosOmega;
			x2 = -x2; y2 = -y2; z2 = -z2; w2 = -w2;
		}

		float scale0, scale1;
		if (cosOmega > 1.0f - Constants.EPSILON) {
			// q1 and q2 are almost identical: sin(omega) would be ~0, fall back to a plain lerp
			scale0 = 1.0f - t;
			scale1 = t;
		} else {
			float omega = (float)Math.acos(cosOmega);
			float sinOmega = (float)Math.sin(omega);
			scale0 = (float)Math.sin((1-t)*omega)/sinOmega;
			scale1 = (float)Math.sin(t*omega)/sinOmega;
		}

		Quaternion result = new Quaternion(
			scale0*q1.x + scale1*x2,
			scale0*q1.y + scale1*y2,
			scale0*q1.z + scale1*z2,
			scale0*q1.w + scale1*w2
		);
		return result.normalize();
	}

	/**
	 * @return a new Matrix3, the rotation matrix equivalent to this Quaternion (assumed unit length)
	 */
	public Matrix3 toMatrix3() {
		float xx=x*x, yy=y*y, zz=z*z;
		float xy=x*y, xz=x*z, yz=y*z;
		float wx=w*x, wy=w*y, wz=w*z;

		float[][] a = {
			{ 1-2*(yy+zz),   2*(xy-wz),   2*(xz+wy) },
			{   2*(xy+wz), 1-2*(xx+zz),   2*(yz-wx) },
			{   2*(xz-wy),   2*(yz+wx), 1-2*(xx+yy) }
		};
		return new Matrix3(a);
	}

	/**
	 * @return a new Matrix4, the rotation matrix equivalent to this Quaternion (assumed unit
	 * length), embedded in an otherwise-identity Matrix4 (no translation)
	 */
	public Matrix4 toMatrix4() {
		float xx=x*x, yy=y*y, zz=z*z;
		float xy=x*y, xz=x*z, yz=y*z;
		float wx=w*x, wy=w*y, wz=w*z;

		float[][] a = {
			{ 1-2*(yy+zz),   2*(xy-wz),   2*(xz+wy), 0f },
			{   2*(xy+wz), 1-2*(xx+zz),   2*(yz-wx), 0f },
			{   2*(xz-wy),   2*(yz+wx), 1-2*(xx+yy), 0f },
			{          0f,          0f,          0f, 1f }
		};
		return new Matrix4(a);
	}

	/**
	 * Recover the axis and angle this Quaternion (assumed unit length) represents. Writes the axis
	 * into the caller-provided Vector3 (avoids an allocation, consistent with Vector4.toArray(dest)
	 * elsewhere in this lib) and returns the angle. When this Quaternion is (very close to) the
	 * identity (no rotation), the angle is 0 and axis is left as the arbitrary X axis (there is no
	 * meaningful axis for a null rotation).
	 * @param axis a Vector3 to fill with the rotation axis
	 * @return the rotation angle, in radians, in [0, PI]
	 */
	public float toAxisAngle(Vector3 axis) {
		// Guard w slightly past +/-1 (float rounding on a Quaternion that is only approximately
		// unit length) so acos() never receives an out-of-domain argument and returns NaN.
		float w = this.w;
		if (w > 1f) w = 1f;
		if (w < -1f) w = -1f;

		float angle = 2*(float)Math.acos(w);
		float sinHalf = (float)Math.sqrt(1 - w*w);

		if (sinHalf < Constants.EPSILON) {
			// No meaningful axis for a null (or near-null) rotation
			axis.setX(1);
			axis.setY(0);
			axis.setZ(0);
		} else {
			axis.setX(this.x/sinHalf);
			axis.setY(this.y/sinHalf);
			axis.setZ(this.z/sinHalf);
		}
		return angle;
	}

	/**
	 * Compare this Quaternion with another
	 * @param q the other Quaternion
	 * @return true if all the elements of this Quaternion are equal (within MathTools.EPSILON tolerance) to q's
	 */
	public boolean equals(Quaternion q) {
		return MathTools.equals(this.x, q.x) && MathTools.equals(this.y, q.y) && MathTools.equals(this.z, q.z) && MathTools.equals(this.w, q.w);
	}

	/**
	 * Object contract override (same pattern as Vector3/Vector4.equals(Object) - see audit report).
	 * Delegates to equals(Quaternion) so behavior (including the epsilon tolerance) is identical.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Quaternion)) return false;
		return equals((Quaternion)o);
	}

	/**
	 * Object contract override (paired with equals(Object) above).
	 */
	@Override
	public int hashCode() {
		int result = Float.floatToIntBits(this.x);
		result = 31*result + Float.floatToIntBits(this.y);
		result = 31*result + Float.floatToIntBits(this.z);
		result = 31*result + Float.floatToIntBits(this.w);
		return result;
	}

}
