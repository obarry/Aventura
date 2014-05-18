package com.aventura.tools.vector3d.vector;

import java.util.Arrays;




import com.aventura.tools.vector3d.IndiceOutOfBoundException;
//import com.aventura.tools.vector3d.matrix.Matrix;
import com.aventura.tools.vector3d.matrix.Matrix3;
import com.aventura.tools.tracing.Tracer;


/**
 * @author Bricolage Olivier
 * @date May 2014
 *
 */
public class Vector3 {

	// Size of the matrix is 3x3
	public static final int SIZE = 3;
	
	double[] array;
	
	public Vector3() {
		// Create the array
		array = new double[SIZE];
	}

	public Vector3(double v) {
		initialize(v);
	}
		
	public Vector3(double[] array) throws VectorArrayWrongSizeException {
		// TBD Add size control for the array
		if (array.length != SIZE) throw new VectorArrayWrongSizeException("Array passed in parameter of Vector3 constructor is out of bound: "+array.length); 
		this.array = array;

	}
	
	public Vector3(int r, Matrix3 A) {
		this.array[0] = A.get(r, 0);
		this.array[1] = A.get(r, 1);
		this.array[2] = A.get(r, 2);
	}
	
	public Vector3(Matrix3 A, int c) {
		this.array[0] = A.get(0, c);
		this.array[1] = A.get(1, c);
		this.array[2] = A.get(2, c);
	}
	
	
	/**
	 * Initialize a Vector3 with a constant value for all elements of the Vector3
	 * @param val the initialization value
	 */
	private void initialize(double val) {
		// Create the array
		array = new double[SIZE];
		
		// Initialize values
		this.array[0] = val;
		this.array[1] = val;
		this.array[2] = val;
	}

	
	@Override
	public String toString() {
		String s = Arrays.toString(array);
		return s;
	}
	
	/**
	 * Set the coordinate of rank i with value v
	 * @param i the rank of the coordinate to set value
	 * @param v, the value to set
	 * @throws IndiceOutOfBoundException
	 */
	public void set(int i, double v) throws IndiceOutOfBoundException {
		if (i<0 || i>SIZE) throw new IndiceOutOfBoundException("Indice out of bound while setting coordinate ("+i+") of Vector3"); 
		array[i]= v;
	}
	
	/**
	 * Set x, the first coordinate of the Vector3, with value v
	 * @param v, the value to set
	 */
	public void setX(double v) {
		array[0]= v;		
	}
	
	/**
	 * Set y, the second coordinate of the Vector3, with value v
	 * @param v, the value to set
	 */
	public void setY(double v) {
		array[1]= v;		
	}
	
	/**
	 * Set z, the third coordinate of the Vector3, with value v
	 * @param v, the value to set
	 */
	public void setZ(double v) {
		array[2]= v;		
	}
	
	/**
	 * Get the coordinate of rank i
	 * @param i
	 * @return
	 * @throws IndiceOutOfBoundException
	 */
	public double get(int i) throws IndiceOutOfBoundException {
		if (i<0 || i>SIZE) throw new IndiceOutOfBoundException("Indice out of bound while getting coordinate ("+i+") of Vector3"); 
		return array[i];
	}
	
	/**
	 * Get x, the first coordinate of the Vector3
	 * @return x
	 */
	public double getX() {
		return array[0];
	}
	
	/**
	 * Get y, the second coordinate of the Vector3
	 * @return y
	 */
	public double getY() {
		return array[1];
	}
	
	/**
	 * Get z, the third coordinate of the Vector3
	 * @return z
	 */
	public double getZ() {
		return array[2];
	}
	
	public double length() {
		return Math.sqrt(array[0]*array[0]+array[1]*array[1]+array[2]*array[2]);
	}
	
	/**
	 * Compare this Vector3 with another
	 * @param w the other Vector3
	 * @return true if all the elements of this Vector3 are equals to the elements of V
	 */
	public boolean equals(Vector3 w) {
		return this.getX()==w.getX() && this.getY()==w.getY() && this.getZ()==w.getZ();
	}

	/**
	 * Sum of this Vector3 V and another Vector3 W, returns a newly created Vector3 
	 * @param w
	 * @return V+W
	 */
	public Vector3 plus(Vector3 w) {
		Vector3 r = new Vector3();
		
		r.setX(this.array[0]+w.getX());
		r.setY(this.array[1]+w.getY());
		r.setZ(this.array[2]+w.getZ());
		
		return r;
	}
	
	/**
	 * Vector3 addition V=V+W. This Vector3 (V) is modified and contains the result of the operation.
	 * @param w the Vector3 to be added to this Vector3
	 */
	public void plusEquals(Vector3 w) {
		this.array[0]+=w.getX();
		this.array[1]+=w.getY();
		this.array[2]+=w.getZ();
	}
	
	/**
	 * Subtraction of this Vector3 V and another Vector3 W, returns a newly created Vector3 
	 * @param w
	 * @return V-W
	 */
	public Vector3 minus(Vector3 w) {
		Vector3 r = new Vector3();
		
		r.setX(this.array[0]-w.getX());
		r.setY(this.array[1]-w.getY());
		r.setZ(this.array[2]-w.getZ());
		
		return r;
	}
	
	/**
	 * Vector3 subtraction V=V-W. This Vector3 (V) is modified and contains the result of the operation.
	 * @param w the Vector3 to be subtracted to this Vector3
	 */
	public void minusEquals(Vector3 w) {
		this.array[0]-=w.getX();
		this.array[1]-=w.getY();
		this.array[2]-=w.getZ();
	}
	
	/**
	 * Scalar product of this Vector3 with W, another Vector3: s = V.W
	 * @param w, the other Vector3
	 * @return s, the scalar product (double)
	 */
	public double scalar(Vector3 w) {
		return array[0]*w.getX()+array[1]*w.getY()+array[2]*w.getY();
	}
	
	public Vector3 times(double val) {
		Vector3 r = new Vector3();
		r.setX(this.array[0]*val);
		r.setY(this.array[1]*val);
		r.setZ(this.array[2]*val);

		return r;
	}
	
	/**
	 * @param val
	 */
	public void timesEquals(double val) {
		this.array[0]*=val;
		this.array[1]*=val;
		this.array[2]*=val;
	}
		
	/**
	 * 
	 * @param w
	 * @return
	 */
	public Vector3 times(Vector3 w) {
		/*
		 * a=(a1,a2,a3) and b=(b1,b2,b3) then a^b=(a2b3−a3b2, a3b1−a1b3, a1b2−a2b1)
		 */
		Vector3 r = new Vector3();
		r.setX(this.getY()*w.getZ()-this.getZ()*w.getY());
		r.setY(this.getZ()*w.getX()-this.getX()*w.getZ());
		r.setZ(this.getX()*w.getY()-this.getY()*w.getX());
		
		return r;
	}
	
	/**
	 * 
	 * @param w
	 * @return
	 */
	public void timesEquals(Vector3 w) {
		double[] array = new double[SIZE];
		array[0] = this.getY()*w.getZ()-this.getZ()*w.getY();
		array[1] = this.getZ()*w.getX()-this.getX()*w.getZ();
		array[2] = this.getX()*w.getY()-this.getY()*w.getX();
		
		this.array[0] = array[0];
		this.array[1] = array[1];
		this.array[2] = array[2];
	}
	
	/**
	 * W = A.V; Multiplication of a this Vector3 V by a Matrix3 A
	 * @param A the Matrix3
	 * @return W, a new Vector3, result of the multiplication
	 */
	public Vector3 times(Matrix3 A) {
		Vector3 r = new Vector3();
		
		for (int i=0; i<SIZE; i++) {
			for (int j=0; j<SIZE; j++) {
				try {
					r.set(i, r.get(i)+A.get(i,j)*this.array[j]);
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
		double[] array = new double[3];
		
		for (int i=0; i<SIZE; i++) {
			array[i] = 0;
			for (int j=0; j<SIZE; j++) {
				array[i]+=A.get(i,j)*this.array[j];
			}
		}
		
		this.array[0] = array[0];
		this.array[1] = array[1];
		this.array[2] = array[2];
	}	
}
