package com.aventura.math.vector;

import java.util.Arrays;

import com.aventura.math.Constants;
import com.aventura.tools.tracing.Tracer;

/**
 * A Vector 4 in 3D Graphics is a Vector 3 (x,y,z) + a fourth component that represent the point information: if null this is a Vector, else a Point
 * 
 * @author Bricolage Olivier
 * @since May 2016
 *
 */
public class Vector4 {
    public static final Vector4 X_AXIS = new Vector4(1,0,0,0);
    public static final Vector4 Y_AXIS = new Vector4(0,1,0,0);
    public static final Vector4 Z_AXIS = new Vector4(0,0,1,0);

    // Components of the Vector
	protected double[] c;
	
	public Vector4() {
		// Create the array
		c = new double[Constants.SIZE_4];
	}

	public Vector4(double v) {
		initialize(v);
	}
	
	public Vector4(double x, double y, double z, double t) {
		c = new double[Constants.SIZE_4];
		this.c[0] = x;
		this.c[1] = y;
		this.c[2] = z;		
		this.c[3] = t;		
	}
		
	public Vector4(double[] array) throws VectorArrayWrongSizeException {
		// TBD Add size control for the array
		if (array.length != Constants.SIZE_4) throw new VectorArrayWrongSizeException("Array passed in parameter of Vector4 constructor is out of bound: "+array.length); 
		this.c = array;

	}
	
	public Vector4(Vector4 v) {
		// Create the array
		c = new double[Constants.SIZE_4];
		this.c[0] = v.c[0];
		this.c[1] = v.c[1];
		this.c[2] = v.c[2];
		this.c[3] = v.c[3];
	}

	public Vector4(int r, Matrix4 A) {
		this.c[0] = A.get(r, 0);
		this.c[1] = A.get(r, 1);
		this.c[2] = A.get(r, 2);
		this.c[3] = A.get(r, 3);
	}
	
	public Vector4(Matrix4 A, int c) {
		this.c[0] = A.get(0, c);
		this.c[1] = A.get(1, c);
		this.c[2] = A.get(2, c);
		this.c[3] = A.get(3, c);
	}
	
	/**
	 * Initialize a Vector4 with a constant value for all elements of the Vector4
	 * @param val the initialization value
	 */
	private void initialize(double val) {
		// Create the array
		c = new double[Constants.SIZE_4];
		
		// Initialize values
		this.c[0] = val;
		this.c[1] = val;
		this.c[2] = val;
		this.c[3] = val;
	}

	
	@Override
	public String toString() {
		String s = Arrays.toString(c);
		return s;
	}
	
	/**
	 * Set the coordinate of rank i with value v
	 * @param i the rank of the coordinate to set value
	 * @param v, the value to set
	 * @throws IndiceOutOfBoundException
	 */
	public void set(int i, double v) throws IndiceOutOfBoundException {
		if (i<0 || i>Constants.SIZE_4) throw new IndiceOutOfBoundException("Indice out of bound while setting coordinate ("+i+") of Vector4"); 
		c[i]= v;
	}
	
	/**
	 * Set x, the first coordinate of the Vector4, with value v
	 * @param v, the value to set
	 */
	public void setX(double v) {
		c[0]= v;		
	}
	
	/**
	 * Set y, the second coordinate of the Vector4, with value v
	 * @param v, the value to set
	 */
	public void setY(double v) {
		c[1]= v;		
	}
	
	/**
	 * Set z, the third coordinate of the Vector4, with value v
	 * @param v, the value to set
	 */
	public void setZ(double v) {
		c[2]= v;		
	}
	
	/**
	 * Set t, the fourth coordinate of the Vector4, with value v
	 * @param v, the value to set
	 */
	public void setT(double v) {
		c[3]= v;		
	}
	
	/**
	 * Get the coordinate of rank i
	 * @param i
	 * @return
	 * @throws IndiceOutOfBoundException
	 */
	public double get(int i) throws IndiceOutOfBoundException {
		if (i<0 || i>Constants.SIZE_4) throw new IndiceOutOfBoundException("Indice out of bound while getting coordinate ("+i+") of Vector4"); 
		return c[i];
	}
	
	/**
	 * Get x, the first coordinate of the Vector4
	 * @return x
	 */
	public double getX() {
		return c[0];
	}
	
	/**
	 * Get y, the second coordinate of the Vector4
	 * @return y
	 */
	public double getY() {
		return c[1];
	}
	
	/**
	 * Get z, the third coordinate of the Vector4
	 * @return z
	 */
	public double getZ() {
		return c[2];
	}
	
	/**
	 * Get t, the fourth coordinate of the Vector4
	 * @return z
	 */
	public double getT() {
		return c[3];
	}
	
	public double length() {
		return Math.sqrt(c[0]*c[0]+c[1]*c[1]+c[2]*c[2]+c[3]*c[3]);
	}
	
	/**
	 * Normalize the vector. Vector is modified (same direction, length becomes 1)
	 * @return this vector (modified)
	 */
	public Vector4 normalize() {
		double length = this.length();
		// Normalize values
		this.c[0]/=length;
		this.c[1]/=length;
		this.c[2]/=length;
		this.c[3]/=length;
		
		return this;
	}
	
	/**
	 * Compare this Vector4 with another
	 * @param w the other Vector3
	 * @return true if all the elements of this Vector4 are equals to the elements of V
	 */
	public boolean equals(Vector4 w) {
		return this.c[0]==w.c[0] && this.c[1]==w.c[1] && this.c[2]==w.c[2] && this.c[3]==w.c[3];
	}

	/**
	 * Sum of this Vector4 V and another Vector4 W, returns a newly created Vector4 
	 * @param w
	 * @return V+W
	 */
	public Vector4 plus(Vector4 w) {
		Vector4 r = new Vector4();
		
		r.setX(this.c[0]+w.c[0]);
		r.setY(this.c[1]+w.c[1]);
		r.setZ(this.c[2]+w.c[2]);
		r.setT(this.c[3]+w.c[3]);
		
		return r;
	}
	
	/**
	 * Vector4 addition V=V+W. This Vector4 (V) is modified and contains the result of the operation.
	 * @param w the Vector4 to be added to this Vector4
	 */
	public void plusEquals(Vector4 w) {
		this.c[0]+=w.c[0];
		this.c[1]+=w.c[1];
		this.c[2]+=w.c[2];
		this.c[3]+=w.c[3];
	}
	
	/**
	 * Subtraction of this Vector4 V and another Vector4 W, returns a newly created Vector4 
	 * @param w
	 * @return V-W
	 */
	public Vector4 minus(Vector4 w) {
		Vector4 r = new Vector4();
		
		r.setX(this.c[0]-w.c[0]);
		r.setY(this.c[1]-w.c[1]);
		r.setZ(this.c[2]-w.c[2]);
		r.setT(this.c[3]-w.c[3]);
		
		return r;
	}
	
	/**
	 * Vector4 subtraction V=V-W. This Vector4 (V) is modified and contains the result of the operation.
	 * @param w the Vector4 to be subtracted to this Vector4
	 */
	public void minusEquals(Vector4 w) {
		this.c[0]-=w.c[0];
		this.c[1]-=w.c[1];
		this.c[2]-=w.c[2];
		this.c[3]-=w.c[3];
	}
	
	/**
	 * V.W : Dot product of this Vector4 with W, another Vector4. 
	 * @param w, the other Vector4
	 * @return s, the dot product (double)
	 */
	public double dot(Vector4 w) {
		return c[0]*w.c[0]+c[1]*w.c[1]+c[2]*w.c[2]+c[3]*w.c[3];
	}
	
	public Vector4 times(double val) {
		Vector4 r = new Vector4();
		r.setX(this.c[0]*val);
		r.setY(this.c[1]*val);
		r.setZ(this.c[2]*val);
		r.setT(this.c[3]*val);

		return r;
	}
	
	/**
	 * @param val
	 */
	public void timesEquals(double val) {
		this.c[0]*=val;
		this.c[1]*=val;
		this.c[2]*=val;
		this.c[3]*=val;
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
		r.setX(this.c[1]*w.c[2]-this.c[2]*w.c[1]);
		r.setY(this.c[2]*w.c[0]-this.c[0]*w.c[2]);
		r.setZ(this.c[0]*w.c[1]-this.c[1]*w.c[0]);
		r.setT(0); // Assuming Vector, not Point
		
		return r;
	}
	
	/**
	 * Assuming this Vector 4 is a Vector (t=0), not a point (t<>0)
	 * V^W : Vector product of this Vector4 with W, another Vector4.
	 * This Vector4 is modified and contains the result of the operation.
	 * @param w
	 */
	public void timesEquals(Vector4 w) {
		double[] array = new double[Constants.SIZE_4];
		array[0] = this.c[1]*w.c[2]-this.c[2]*w.c[1];
		array[1] = this.c[2]*w.c[0]-this.c[0]*w.c[2];
		array[2] = this.c[0]*w.c[1]-this.c[1]*w.c[0];
		
		this.c[0] = array[0];
		this.c[1] = array[1];
		this.c[2] = array[2];
		this.c[3] = 0; // Assuming Vector, not Point
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
					r.set(i, r.get(i)+A.get(i,j)*this.c[j]);
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
	 * V = A.V - Multiplication of a this Vector3 V by a Matrix4.
	 * This Vector4 is modified and contains the result of the operation.
	 * @param A the Matrix4
	 */
	public void timesEquals(Matrix4 A) {
		double[] array = new double[Constants.SIZE_4];
		
		for (int i=0; i<Constants.SIZE_4; i++) {
			array[i] = 0;
			for (int j=0; j<Constants.SIZE_4; j++) {
				array[i]+=A.get(i,j)*this.c[j];
			}
		}		
		this.c[0] = array[0];
		this.c[1] = array[1];
		this.c[2] = array[2];
		this.c[3] = array[3];
	}	

}
