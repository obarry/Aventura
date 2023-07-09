package com.aventura.math.vector;

import com.aventura.math.Constants;
import com.aventura.tools.tracing.Tracer;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2016-2023 Olivier BARRY
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
 * A Vector 4 in 3D Graphics is a Vector 3 (x,y,z) + w, a fourth component that represent the point information: if null this is a Vector, else a Point
 * 
 * @author Olivier BARRY
 * @since May 2016
 *
 */
public class Vector4 {
	
	// *** Instrumentation ***
	public static int nb_vectors = 0; // count the number of created instances
	public static int nb_to_display = 0; // count before next display session
	public static final int DISPLAY_EVERY = 10000000; // nb of count between 2 display sessions

	
    public static final Vector4 X_AXIS = new Vector4(1,0,0,0);
    public static final Vector4 Y_AXIS = new Vector4(0,1,0,0);
    public static final Vector4 Z_AXIS = new Vector4(0,0,1,0);

    public static final Vector4 X_OPP_AXIS = new Vector4(-1,0,0,0);
    public static final Vector4 Y_OPP_AXIS = new Vector4(0,-1,0,0);
    public static final Vector4 Z_OPP_AXIS = new Vector4(0,0,-1,0);
	public static final Vector4 ZERO_VECTOR = new Vector4(0,0,0,0);
	public static final Vector4 ZERO_POINT = new Vector4(0,0,0,1);

    // Components of the Vector
	protected float x;
	protected float y;
	protected float z;
	protected float w;
	
	public Vector4() {
		this.x = 0;
		this.y = 0;
		this.z = 0;
		this.w = 0;
		count();
	}

	public Vector4(float v) {
		initialize(v);
		count();
	}
	
	public Vector4(float x, float y, float z, float w) {
		this.x = x;
		this.y = y;
		this.z = z;		
		this.w = w;		
		count();
	}
		
	public Vector4(float[] array) throws VectorArrayWrongSizeException {
		if (array.length < Constants.SIZE_4) throw new VectorArrayWrongSizeException("Array passed in parameter of Vector4 constructor is out of bound: "+array.length); 
		this.x = array[0];
		this.y = array[1];
		this.z = array[2];
		this.w = array[3];

		count();
	}
	
	public Vector4(Vector4 v) {
		this.x = v.x;
		this.y = v.y;
		this.z = v.z;
		this.w = v.w;
		count();
	}

	public Vector4(Vector3 v) {
		this.x = v.x;
		this.y = v.y;
		this.z = v.z;
		this.w = 0;
		count();
	}

	public Vector4(int r, Matrix4 A) {
		this.x = A.get(r, 0);
		this.y = A.get(r, 1);
		this.z = A.get(r, 2);
		this.w = A.get(r, 3);
		count();
	}
	
	public Vector4(Matrix4 A, int c) {
		this.x = A.get(0, c);
		this.y = A.get(1, c);
		this.z = A.get(2, c);
		this.w = A.get(3, c);
		count();
	}

	private static void count() {
		nb_vectors++;
		nb_to_display++;
		if (nb_to_display>=DISPLAY_EVERY) {
			if (Tracer.object) Tracer.traceObject(Vector4.class, "***** NB OF VECTOR4 (created since begining): "+nb_vectors);
			nb_to_display=0;
		}
	}

	/**
	 * Initialize a Vector4 with a constant value for all elements of the Vector4
	 * @param val the initialization value
	 */
	private void initialize(float val) {
		// Initialize values
		this.x = val;
		this.y = val;
		this.z = val;
		this.w = val;
	}

	
	@Override
	public String toString() {
		return "Vector4 ["+x+", "+y+", "+z+", "+w+"]";
	}
	
	/**
	 * Set the coordinate of rank i with value v
	 * @param i the rank of the coordinate to set value
	 * @param v, the value to set
	 * @throws IndiceOutOfBoundException
	 */
	public void set(int i, float v) throws IndiceOutOfBoundException {
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
		default :
			throw new IndiceOutOfBoundException("Indice out of bound while setting coordinate ("+i+") of Vector4");
		}
	}
	
	/**
	 * Set x, the first coordinate of the Vector4, with value v
	 * @param v, the value to set
	 */
	public void setX(float v) {
		x = v;		
	}
	
	/**
	 * Set y, the second coordinate of the Vector4, with value v
	 * @param v, the value to set
	 */
	public void setY(float v) {
		y = v;		
	}
	
	/**
	 * Set z, the third coordinate of the Vector4, with value v
	 * @param v, the value to set
	 */
	public void setZ(float v) {
		z = v;		
	}
	
	/**
	 * Set t, the fourth coordinate of the Vector4, with value v
	 * @param v, the value to set
	 */
	public void setW(float v) {
		w = v;		
	}
	
	/**
	 * Get the coordinate of rank i
	 * @param i
	 * @return
	 * @throws IndiceOutOfBoundException
	 */
	public float get(int i) throws IndiceOutOfBoundException {
		switch (i) {
		case 0:
			return this.x;
		case 1:
			return this.y;
		case 2:
			return this.z;
		case 3:
			return this.w;
		default :
			throw new IndiceOutOfBoundException("Indice out of bound while getting coordinate ("+i+") of Vector4");
		}
	}
	
	/**
	 * Get x, the first coordinate of the Vector4
	 * @return x
	 */
	public float getX() {
		return this.x;
	}
	
	/**
	 * Get y, the second coordinate of the Vector4
	 * @return y
	 */
	public float getY() {
		return this.y;
	}
	
	/**
	 * Get z, the third coordinate of the Vector4
	 * @return z
	 */
	public float getZ() {
		return this.z;
	}
	
	/**
	 * Get t, the fourth coordinate of the Vector4
	 * @return z
	 */
	public float getW() {
		return this.w;
	}
	
	/**
	 * Get x in 3D coordinates
	 * @return x
	 */
	public float get3DX() {
		return this.x/this.w;
	}
	
	/**
	 * Get y in 3D coordinates
	 * @return y
	 */
	public float get3DY() {
		return this.y/this.w;
	}
	
	/**
	 * Get z in 3D coordinates
	 * @return z
	 */
	public float get3DZ() {
		return this.z/this.w;
	}
	
	/**
	 * Get a new Vector3 representing the 3 first coordinates of this Vector divided by the 4th coordinate (3D Point)
	 * @return z
	 */
	public Vector3 get3DPoint() {
		if (this.w != 0) {
			return new Vector3(this.x/this.w, this.y/this.w, this.z/this.w);
		} else {
			return null;
		}
	}

	/**
	 * Get length of the vector4 including 4th coordinate
	 * @return the length or 'norm' of the Vector4
	 */
	public float length() {
		return (float)Math.sqrt(this.x*this.x+this.y*this.y+this.z*this.z+this.w*this.w);
	}
	
	/**
	 * Normalize the vector. Vector is modified (same direction, length becomes 1)
	 * @return this vector (modified)
	 */
	/**
	 * @return
	 */
	public Vector4 normalize() {
		float length = this.length();
		// Normalize values
		this.x/=length;
		this.y/=length;
		this.z/=length;
		this.w/=length;
		
		return this;
	}
	
	/**
	 * Compare this Vector4 with another
	 * @param w the other Vector3
	 * @return true if all the elements of this Vector4 are equals to the elements of V
	 */
	public boolean equals(Vector4 w) {
		return this.x==w.x && this.y==w.y && this.z==w.z && this.w==w.w;
	}

	/**
	 * Sum of this Vector4 V and another Vector4 W, returns a newly created Vector4 
	 * @param w
	 * @return V+W
	 */
	public Vector4 plus(Vector4 w) {
		Vector4 r = new Vector4();
		
		r.setX(this.x+w.x);
		r.setY(this.y+w.y);
		r.setZ(this.z+w.z);
		r.setW(this.w+w.w);
		
		return r;
	}
	
	/**
	 * Sum of this Vector4 P and another Vector3 V, returns a newly created Vector4
	 * Useful to "move" a point P (Vector4) from a vector V (Vector3)
	 * The w coordinate of P is left unchanged
	 * @param v
	 * @return P+V
	 */
	public Vector4 plus(Vector3 v) {
		Vector4 r = new Vector4();
		
		r.setX(this.x+v.x);
		r.setY(this.y+v.y);
		r.setZ(this.z+v.z);
		r.setW(this.w);
		
		return r;
	}
	
	/**
	 * Vector4 addition V=V+W. This Vector4 (V) is modified and contains the result of the operation.
	 * @param w the Vector4 to be added to this Vector4
	 */
	public void plusEquals(Vector4 w) {
		this.x+=w.x;
		this.y+=w.y;
		this.z+=w.z;
		this.w+=w.w;
	}
	
	/**
	 * Subtraction of this Vector4 V and another Vector4 W, returns a newly created Vector4 
	 * @param w
	 * @return V-W
	 */
	public Vector4 minus(Vector4 w) {
		Vector4 r = new Vector4();
		
		r.setX(this.x-w.x);
		r.setY(this.y-w.y);
		r.setZ(this.z-w.z);
		r.setW(this.w-w.w);
		
		return r;
	}

	/**
	 * Subtraction of this Vector4 P and another Vector3 V, returns a newly created Vector4
	 * Useful to "move" a point P (Vector4) with a vector V (Vector3), actually -V as this is a subtraction
	 * The w coordinate of P is left unchanged
	 * @param v
	 * @return P-V
	 */
	public Vector4 minus(Vector3 v) {
		Vector4 r = new Vector4();
		
		r.setX(this.x-v.x);
		r.setY(this.y-v.y);
		r.setZ(this.z-v.z);
		r.setW(this.w);
		
		return r;
	}

	/**
	 * Vector4 subtraction V=V-W. This Vector4 (V) is modified and contains the result of the operation.
	 * @param w the Vector4 to be subtracted to this Vector4
	 */
	public void minusEquals(Vector4 w) {
		this.x-=w.x;
		this.y-=w.y;
		this.z-=w.z;
		this.w-=w.w;
	}
	
	/**
	 * V.W : Dot product of this Vector4 with W, another Vector4. 
	 * @param w, the other Vector4
	 * @return s, the dot product (double)
	 */
	public float dot(Vector4 w) {
		return this.x*w.x+this.y*w.y+this.z*w.z+this.w*w.w;
	}
	
	public Vector4 times(float val) {
		Vector4 r = new Vector4();
		r.setX(this.x*val);
		r.setY(this.y*val);
		r.setZ(this.z*val);
		r.setW(this.w*val);

		return r;
	}
	
	/**
	 * @param val
	 */
	public void timesEquals(float val) {
		this.x*=val;
		this.y*=val;
		this.z*=val;
		this.w*=val;
	}
		
	/**
	 * Assuming this Vector 4 is a Vector (t=0), not a point (t<>0)
	 * V^W : Vector product of this Vector4 with W, another Vector4.
	 * @param w
	 * @return a new Vector4 corresponding to the product
	 */
	public Vector4 times(Vector4 w) {
		/*
		 * a=(a1,a2,a3) and b=(b1,b2,b3) then a^b=(a2b3-a3b2, a3b1-a1b3, a1b2-a2b1)
		 */
		Vector4 r = new Vector4();
		r.setX(this.y*w.z-this.z*w.y);
		r.setY(this.z*w.x-this.x*w.z);
		r.setZ(this.x*w.y-this.y*w.x);
		r.setW(0); // Assuming Vector, not Point
		
		return r;
	}
	
	/**
	 * Assuming this Vector 4 is a Vector (t=0), not a point (t<>0)
	 * V^W : Vector product of this Vector4 with W, another Vector4.
	 * This Vector4 is modified and contains the result of the operation.
	 * @param w
	 */
	public void timesEquals(Vector4 w) {
		float xp, yp, zp;

		xp = this.y*w.z-this.z*w.y;
		yp = this.z*w.x-this.x*w.z;
		zp = this.x*w.y-this.y*w.x;
		
		this.x = xp;
		this.y = yp;
		this.z = zp;

		this.w = 0; // Assuming Vector, not Point
	}
	
	/**
	 * W = A.V; Multiplication of a this Vector4 V by a Matrix4 A
	 * @param A the Matrix4
	 * @return W, a new Vector4, result of the multiplication
	 */
	public Vector4 times(Matrix4 A) {
		Vector4 r = new Vector4();
		
		for (int i=0; i<Constants.SIZE_4; i++) {
			for (int j=0; j<Constants.SIZE_4; j++) {
				try {
					r.set(i, r.get(i)+A.get(i,j)*this.get(j));
				} catch (IndiceOutOfBoundException e) {
					// Do nothing, this won't happen as all arrays are controlled in size (coming from Vector4 and Matrix4)
					if (Tracer.error) Tracer.traceError(this.getClass(), "Unexpected exception: "+e);
					e.printStackTrace();
				}
			}
		}		
		return r;
	}
	
	/**
	 * V = A.V - Multiplication of a this Vector4 V by a Matrix4.
	 * This Vector4 is modified and contains the result of the operation.
	 * @param A the Matrix4
	 */
	public void timesEquals(Matrix4 A) {
		float[] array = new float[Constants.SIZE_4];

		for (int i=0; i<Constants.SIZE_4; i++) {
			array[i] = 0;
			for (int j=0; j<Constants.SIZE_4; j++) {
				try {
					array[i]+=A.get(i,j)*this.get(j);
				} catch (IndiceOutOfBoundException e) {
					// Do nothing, this won't happen as all arrays are controlled in size (coming from Vector4 and Matrix4)
					if (Tracer.error) Tracer.traceError(this.getClass(), "Unexpected exception: "+e);
					e.printStackTrace();
				}
			}
		}		
		this.x = array[0];
		this.y = array[1];
		this.z = array[2];
		this.w = array[3];
	}
	
	/**
	 * Get a new Vector3 representing the 3 first coordinates of this Vector
	 * @return z
	 */
	public Vector3 V3() {
		return new Vector3(this);
	}

}
