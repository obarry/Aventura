package com.aventura.tools.vector3d.matrix;

import java.util.Arrays;

import com.aventura.tools.tracing.Tracer;
import com.aventura.tools.vector3d.Constants;
import com.aventura.tools.vector3d.IndiceOutOfBoundException;
import com.aventura.tools.vector3d.vector.Vector3;
import com.aventura.tools.vector3d.vector.VectorArrayWrongSizeException;

public class Matrix3 {
	
	// Some constants
	//double dbl [][]  = {{0.0, 0.0, 0.0},{0.0, 0.0, 0.0},{0.0, 0.0, 0.0}};
	//public static final Matrix  MATRIX_NULL = new Matrix(double[][] {{0.0, 0.0, 0.0},{0.0, 0.0, 0.0},{0.0, 0.0, 0.0}});
	//public static final Matrix  MATRIX_ONE = new Matrix ({{1, 0, 0},{0, 1, 0},{0, 0, 1}});
	
	double[][] array;
	
	/**
	 * Initialize a square Matrix of size s with 0 for all elements of the matrix
	 * @param s the size of the Matrix (number of rows and columns)
	 */
	public Matrix3() {
		initialize(0);
	}

		
	/**
	 * Initialize a Matrix with a constant value for all elements of the matrix
	 * @param val the initialization value
	 */
	public Matrix3(double val) {
		initialize(val);
	}
	
	/**
	 * Initialize a Matrix with a 2D array of double
	 * @param a the 2D array of double
	 */
	public Matrix3(double[][] a) throws MatrixArrayWrongSizeException {
		if (a.length != Constants.SIZE) throw new MatrixArrayWrongSizeException("Wrong array row size ("+a.length+") while creating Matrix3 from array"); 
		if (a[0].length != Constants.SIZE) throw new MatrixArrayWrongSizeException("Wrong array column size ("+a[0].length+") while creating Matrix3 from array"); 

		this.array = a;
	}
		
	/**
	 * Initialize a Matrix with a constant value for all elements of the matrix
	 * @param val the initialization value
	 */
	protected void initialize(double val) {
		// Create the array
		array = new double[Constants.SIZE][Constants.SIZE];
		// Initialize values
		for (int i=0; i<Constants.SIZE; i++) {
			for (int j=0; j<Constants.SIZE; j++) {
				array[i][j] = val;
			}
		}
	}
	
	public void setArray(double[][] a) throws MatrixArrayWrongSizeException {
		if (a.length != Constants.SIZE) throw new MatrixArrayWrongSizeException("Wrong array row size ("+a.length+") while creating Matrix3 from array"); 
		if (a[0].length != Constants.SIZE) throw new MatrixArrayWrongSizeException("Wrong array column size ("+a[0].length+") while creating Matrix3 from array"); 
		this.array = a;
	}
	
	@Override
	public String toString() {
		String s = "[";
		for (int i=0; i<Constants.SIZE-1; i++) {
			s = s + Arrays.toString(array[i]);
			s = s+"\n";
		}
		s = s + Arrays.toString(array[Constants.SIZE-1])+"]";
		return s;
	}
	
	/**
	 * Set value of an element of the Matrix
	 * @param i row indice
	 * @param j column indice
	 * @param val value to set
	 */
	public void set(int i, int j, double val) throws IndiceOutOfBoundException {
		if (i<0 || i>Constants.SIZE) throw new IndiceOutOfBoundException("Row indice ("+i+") out of bound while setting element ("+i+","+j+") of Matrix3"); 
		if (j<0 || j>Constants.SIZE) throw new IndiceOutOfBoundException("Column indice ("+j+") out of bound while setting element ("+i+","+j+") of Matrix3"); 
		array[i][j] = val;
	}

	/**
	 * Get value of an element of the Matrix
	 * @param i row indice
	 * @param j column indice
	 * @return the value of the element
	 */
	public double get(int i, int j) {
		return array[i][j];
	}
	
	/**
	 * Set all elements of the diagonal of this Matrix to a given value
	 * @param v the value to set
	 */
	public void setDiagonal(double v) {
		array[0][0] = v; 
		array[1][1] = v; 
		array[2][2] = v; 
	}
	
	
	/**
	 * Get row of a Matrix3 in the format of a Vector3
	 * @param r the rank of the row
	 * @return a Vector3 representing the row
	 * @throws IndiceOutOfBoundException
	 */
	public Vector3 getRow(int r) throws IndiceOutOfBoundException {
		if (r<0 || r>Constants.SIZE) throw new IndiceOutOfBoundException("Indice out of bound while getting Row ("+r+") of Matrix3"); 
		double[] array = new double[Constants.SIZE];
		Vector3 v = null;
		// No loop for optimization
		array[0] = this.array[r][0];
		array[1] = this.array[r][1];
		array[2] = this.array[r][2];
		
		try {
			v = new Vector3(array);
		} catch (VectorArrayWrongSizeException e) {
			// Do nothing, this won't happen as all arrays are controlled in size (coming from Vector3 and Matrix3)
			if (Tracer.error) Tracer.traceError(this.getClass(), "Unexpected exception: "+e);
			e.printStackTrace();
		}
		 return v;
	}
	
	/**
	 * Get column of a Matrix3 in the format of a Vector3
	 * @param c the rank of the column
	 * @return a Vector3 representing the column
	 * @throws IndiceOutOfBoundException
	 */
	public Vector3 getColumn(int c) throws IndiceOutOfBoundException {
		if (c<0 || c>Constants.SIZE) throw new IndiceOutOfBoundException("Indice out of bound while getting Column ("+c+") of Matrix3"); 
		double[] array = new double[Constants.SIZE];
		Vector3 v = null;
		// No loop for optimization
		array[0] = this.array[0][c];
		array[1] = this.array[1][c];
		array[2] = this.array[2][c];
		
		try {
			v = new Vector3(array);
		} catch (VectorArrayWrongSizeException e) {
			// Do nothing, this won't happen as all arrays are controlled in size (coming from Vector3 and Matrix3)
			if (Tracer.error) Tracer.traceError(this.getClass(), "Unexpected exception: "+e);
			e.printStackTrace();
		}
		 return v;	
	}
	
	/**
	 * Compare this Matrix with another
	 * @param B the other Matrix
	 * @return true if all the elements of this Matrix are equals to the elements of B
	 */
	public boolean equals(Matrix3 B) {
		
		for (int i=0; i<Constants.SIZE; i++) {
			for (int j=0; j<Constants.SIZE; j++) {
				if (this.get(i,j) != B.get(i,j)) return false;
			}
		}
		return true;
		
	}
	
	/**
	 * C=A^B
	 * @param B
	 * @return
	 */
	public Matrix3 times(Matrix3 b) {
		Matrix3 r = new Matrix3();
		
		for (int i=0; i<Constants.SIZE; i++) {
			for (int j=0; j<Constants.SIZE; j++) {
				try {
					r.set(i,j, this.get(i,0)*b.get(0,j) + this.get(i,1)*b.get(1,j) + this.get(i,2)*b.get(2,j));
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
	 * A=A^B
	 * @param B
	 */
	public void timesEquals(Matrix3 b) {
		double[][] array = new double[Constants.SIZE][Constants.SIZE];
		for (int i=0; i<Constants.SIZE; i++) {
			for (int j=0; j<Constants.SIZE; j++) {
				array[i][j] = this.get(i,0)*b.get(0,j) + this.get(i,1)*b.get(1,j) + this.get(i,2)*b.get(2,j);
			}
		}
		this.array = array;
	}
	
	/**
	 * Multiply a Matrix by a scalar B=A*s
	 * @param s the scalar value
	 * @return a new Matrix B=A*s
	 */
	public Matrix3 times(double s) {
		Matrix3 r = new Matrix3();
		for (int i=0; i<Constants.SIZE; i++) {
			for (int j=0; j<Constants.SIZE; j++) {
				try {
					r.set(i,j, this.array[i][j]*s);
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
	 * Multiply this Matrix by a scalar A = A*s
	 * @param s the scalar value
	 */
	public void timesEquals(double s) {
		for (int i=0; i<Constants.SIZE; i++) {
			for (int j=0; j<Constants.SIZE; j++) {
				this.array[i][j] = this.array[i][j]*s;
			}
		}
	}
	
	/**
	 * Matrix addition C=A+B. Do not modify this Matrix (A), this Matrix and return C a newly created Matrix.
	 * @param B the Matrix to be added
	 * @return C, a new Matrix, sum of this Matrix (A) and B Matrix
	 */
	public Matrix3 plus(Matrix3 B) {
		Matrix3 r = new Matrix3();
		for (int i=0; i<Constants.SIZE; i++) {
			for (int j=0; j<Constants.SIZE; j++) {
				try {
					r.set(i,j, this.array[i][j]+B.get(i,j));
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
	 * Matrix addition A=A+B. This Matrix (A) is modified and contains the result of the operation.
	 * @param B the Matrix to be added to this Matrix
	 */
	public void plusEquals(Matrix3 B) {
		for (int i=0; i<Constants.SIZE; i++) {
			for (int j=0; j<Constants.SIZE; j++) {
				this.array[i][j] = this.array[i][j]+B.get(i,j);
			}
		}
	}

	/**
	 * Matrix subtraction C=A-B. Do not modify this Matrix (A), this Matrix and return C a newly created Matrix.
	 * @param B the Matrix to be subtracted
	 * @return C, a new Matrix, subtraction of this Matrix (A) and B Matrix
	 */
	public Matrix3 minus(Matrix3 B) {
		Matrix3 r = new Matrix3();
		for (int i=0; i<Constants.SIZE; i++) {
			for (int j=0; j<Constants.SIZE; j++) {
				try {
					r.set(i,j, this.array[i][j]-B.get(i,j));
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
	 * Matrix subtraction A=A-B. This Matrix (A) is modified and contains the result of the operation.
	 * @param B the Matrix to be subtracted to this Matrix
	 */
	/**
	 * @param B
	 */
	public void minusEquals(Matrix3 B) {
		for (int i=0; i<Constants.SIZE; i++) {
			for (int j=0; j<Constants.SIZE; j++) {
				this.array[i][j] = this.array[i][j]-B.get(i,j);
			}
		}
	}


	public Vector3 times(Vector3 v) {
		// Rely on the service provided by the Vector3D class
		// Optimal call is to use Vector3D method directly
		return v.times(this);
	}
	
}
