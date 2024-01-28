package com.aventura.math.vector;

import com.aventura.math.Constants;
import com.aventura.math.tools.MathTools;
import com.aventura.tools.tracing.Tracer;


/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2016-2024 Olivier BARRY
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
 * @author Olivier BARRY
 * @date May 2014
 *
 */
public class Vector3 {
	
	// *** Instrumentation ***
	public static int nb_vectors = 0; // count the number of created instances
	public static int nb_to_display = 0; // count before next display session
	public static final int DISPLAY_EVERY = 1000000; // nb of count between 2 display sessions

    public static final Vector3 X_AXIS = new Vector3(1,0,0);
    public static final Vector3 Y_AXIS = new Vector3(0,1,0);
    public static final Vector3 Z_AXIS = new Vector3(0,0,1);

    public static final Vector3 X_OPP_AXIS = new Vector3(-1,0,0);
    public static final Vector3 Y_OPP_AXIS = new Vector3(0,-1,0);
    public static final Vector3 Z_OPP_AXIS = new Vector3(0,0,-1);
    
	public static final Vector3 ZERO_VECTOR = new Vector3(0,0,0);

    // Components of the Vector
	protected float x;
	protected float y;
	protected float z;
	
	public Vector3() {
		this.x = 0;
		this.y = 0;
		this.z = 0;
		count();
	}

	public Vector3(float v) {
		this.x = v;
		this.y = v;
		this.z = v;
		count();
	}
	
	public Vector3(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;		
		count();
	}
		
	public Vector3(float[] array) throws VectorArrayWrongSizeException {
		if (array.length < Constants.SIZE_3) throw new VectorArrayWrongSizeException("Array passed in parameter of Vector3 constructor is out of bound: "+array.length); 
		this.x = array[0];
		this.y = array[1];
		this.z = array[2];
		count();
	}
	
	public Vector3(Vector3 v) {
		this.x = v.x;
		this.y = v.y;
		this.z = v.z;
		count();
	}

	public Vector3(Vector4 v) {
		this.x = v.x;
		this.y = v.y;
		this.z = v.z;
		// Ignore latest coordinate
		count();
	}

	public Vector3(int r, Matrix3 A) {
		this.x = A.get(r, 0);
		this.y = A.get(r, 1);
		this.z = A.get(r, 2);
		count();
	}
	
	public Vector3(Matrix3 A, int c) {
		this.x = A.get(0, c);
		this.y = A.get(1, c);
		this.z = A.get(2, c);
		count();
	}
	
	private static void count() {
		nb_vectors++;
		nb_to_display++;
		if (nb_to_display>=DISPLAY_EVERY) {
			if (Tracer.object) Tracer.traceObject(Vector3.class, "***** NB OF VECTOR3 (created since begining): "+nb_vectors);
			nb_to_display=0;
		}
	}
	
	@Override
	public String toString() {
		return "Vector3 ["+x+", "+y+", "+z+"]";
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
		default :
			throw new IndiceOutOfBoundException("Indice out of bound while setting coordinate ("+i+") of Vector3");
		}
}

		
	/**
	 * Set x, the first coordinate of the Vector3, with value v
	 * @param v, the value to set
	 */
	public void setX(float v) {
		this.x = v;		
	}
	
	/**
	 * Set y, the second coordinate of the Vector3, with value v
	 * @param v, the value to set
	 */
	public void setY(float v) {
		this.y = v;		
	}
	
	/**
	 * Set z, the third coordinate of the Vector3, with value v
	 * @param v, the value to set
	 */
	public void setZ(float v) {
		this.z = v;		
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
		default :
			throw new IndiceOutOfBoundException("Indice out of bound while getting coordinate ("+i+") of Vector3");
		}
	}

		
	/**
	 * Get x, the first coordinate of the Vector3
	 * @return x
	 */
	public float getX() {
		return this.x;
	}
	
	/**
	 * Get y, the second coordinate of the Vector3
	 * @return y
	 */
	public float getY() {
		return this.y;
	}
	
	/**
	 * Get z, the third coordinate of the Vector3
	 * @return z
	 */
	public float getZ() {
		return this.z;
	}
		
	public float length() {
		return (float)Math.sqrt(this.x*this.x+this.y*this.y+this.z*this.z);
	}
	
	/**
	 * Normalize the vector. Vector is modified (same direction, length becomes 1)
	 * @return this vector (modified)
	 */
	public Vector3 normalize() {
		float length = this.length();
		// Normalize values
		this.x/=length;
		this.y/=length;
		this.z/=length;
		
		return this;
	}
	
	/**
	 * Compare this Vector3 with another
	 * @param w the other Vector3
	 * @return true if all the elements of this Vector3 are equals to the elements of V
	 */
	public boolean equals(Vector3 w) {
		return MathTools.equals(this.x , w.x) && MathTools.equals(this.y , w.y) && MathTools.equals(this.z , w.z);
	}

	/**
	 * Sum of this Vector3 V and another Vector3 W, returns a newly created Vector3 
	 * @param w
	 * @return V+W
	 */
	public Vector3 plus(Vector3 w) {
		Vector3 r = new Vector3();
		
		r.setX(this.x+w.x);
		r.setY(this.y+w.y);
		r.setZ(this.z+w.z);
		
		return r;
	}
	
	/**
	 * Vector3 addition V=V+W. This Vector3 (V) is modified and contains the result of the operation.
	 * @param w the Vector3 to be added to this Vector3
	 */
	public void plusEquals(Vector3 w) {
		this.x+=w.x;
		this.y+=w.y;
		this.z+=w.z;
	}
	
	/**
	 * Subtraction of this Vector3 V and another Vector3 W, returns a newly created Vector3 
	 * @param w
	 * @return V-W
	 */
	public Vector3 minus(Vector3 w) {
		Vector3 r = new Vector3();
		
		r.setX(this.x-w.x);
		r.setY(this.y-w.y);
		r.setZ(this.z-w.z);
		
		return r;
	}
	
	/**
	 * Vector3 subtraction V=V-W. This Vector3 (V) is modified and contains the result of the operation.
	 * @param w the Vector3 to be subtracted to this Vector3
	 */
	public void minusEquals(Vector3 w) {
		this.x-=w.x;
		this.y-=w.y;
		this.z-=w.z;
	}
	
	/**
	 * V.W : Dot product of this Vector3 with W, another Vector3. 
	 * @param w, the other Vector3
	 * @return s, the dot product (double)
	 */
	public float dot(Vector3 w) {
		return this.x*w.x+this.y*w.y+this.z*w.z;
	}
	
	public Vector3 times(float val) {
		Vector3 r = new Vector3();
		r.x = this.x*val;
		r.y = this.y*val;
		r.z = this.z*val;

		return r;
	}
	
	/**
	 * @param val
	 */
	public void timesEquals(float val) {
		this.x*=val;
		this.y*=val;
		this.z*=val;
	}
		
	/**
	 * V^W : Vector product of this Vector3 with W, another Vector3.
	 * @param w
	 * @return a new Vector3 corresponding to the product
	 */
	public Vector3 times(Vector3 w) {
		/*
		 * a=(a1,a2,a3) and b=(b1,b2,b3) then a^b=(a2b3−a3b2, a3b1−a1b3, a1b2−a2b1)
		 */
		Vector3 r = new Vector3();
		r.x = this.y*w.z-this.z*w.y;
		r.y = this.z*w.x-this.x*w.z;
		r.z = this.x*w.y-this.y*w.x;
		
		return r;
	}
	
	/**
	 *  V^W : Vector product of this Vector3 with W, another Vector3.
	 *  This Vector3 is modified and contains the result of the operation.
	 *  @param w
	 */
	public void timesEquals(Vector3 w) {
		float xp, yp, zp;

		xp = this.y*w.z-this.z*w.y;
		yp = this.z*w.x-this.x*w.z;
		zp = this.x*w.y-this.y*w.x;
		
		this.x = xp;
		this.y = yp;
		this.z = zp;
	}
	
	/**
	 * W = A.V; Multiplication of a this Vector3 V by a Matrix3 A
	 * @param A the Matrix3
	 * @return W, a new Vector3, result of the multiplication
	 */
	public Vector3 times(Matrix3 A) {
		Vector3 r = new Vector3();
		
		for (int i=0; i<Constants.SIZE_3; i++) {
			for (int j=0; j<Constants.SIZE_3; j++) {
				try {
					r.set(i, r.get(i)+A.get(i,j)*this.get(j));
				} catch (IndiceOutOfBoundException e) {
					// Do nothing, this won't happen as all arrays are controlled in size (coming from Vector3 and Matrix3)
					if (Tracer.error) Tracer.traceError(this.getClass(), "Unexpected exception: "+e);
					e.printStackTrace();
				}
			}
		}		
		return r;
	}
	
	/**
	 * V = A.V - Multiplication of a this Vector3 V by a Matrix3.
	 * This Vector3 is modified and contains the result of the operation.
	 * @param A the Matrix3
	 */
	public void timesEquals(Matrix3 A) {
		float[] array = new float[3];

		for (int i=0; i<Constants.SIZE_3; i++) {
			array[i] = 0;
			for (int j=0; j<Constants.SIZE_3; j++) {
				try {
					array[i]+=A.get(i,j)*this.get(j);
				} catch (IndiceOutOfBoundException e) {
					// Do nothing, this won't happen as all arrays are controlled in size (coming from Vector3 and Matrix3)
					if (Tracer.error) Tracer.traceError(this.getClass(), "Unexpected exception: "+e);
					e.printStackTrace();
				}
			}
		}		
		this.x = array[0];
		this.y = array[1];
		this.z = array[2];
	}
	
	/**
	 * Create a new Vector4 from this Vector3 containing 0 as w coordinate
	 * @return a newly created Vector 4
	 */
	public Vector4 V4() {
		return new Vector4(this);
	}
}
