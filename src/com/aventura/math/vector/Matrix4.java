package com.aventura.math.vector;

import java.util.Arrays;

import com.aventura.math.Constants;

public class Matrix4 {

	protected double[][] array;

	/**
	 * Initialize a square Matrix of size s with 0 for all elements of the matrix
	 * @param s the size of the Matrix (number of rows and columns)
	 */
	public Matrix4() {
		//initialize(0); // optimize -> no init or use Matrix4(0) instead
	}

		
	/**
	 * Initialize Matrix with a constant value for all elements of the matrix
	 * @param val the initialization value
	 */
	public Matrix4(double val) {
		initialize(val);
	}

	/**
	 * Initialize Matrix with a 2D array of double
	 * @param a the 2D array of double
	 */
	public Matrix4(double[][] a) throws MatrixArrayWrongSizeException {
		if (a.length != Constants.SIZE_4) throw new MatrixArrayWrongSizeException("Wrong array row size ("+a.length+") while creating Matrix4 from array"); 
		if (a[0].length != Constants.SIZE_4) throw new MatrixArrayWrongSizeException("Wrong array column size ("+a[0].length+") while creating Matrix4 from array"); 

		this.array = a;
	}

	/**
	 * Initialize Matrix with data from another Matrix
	 * @param a the other Matrix
	 */
	public Matrix4(Matrix4 a) {
		// Create the array
		array = new double[Constants.SIZE_4][Constants.SIZE_4];
		
		for (int i=0; i<Constants.SIZE_4; i++) {
			for (int j=0; j<Constants.SIZE_4; j++) {
				this.array[i][j] = a.array[i][j];
			}
		}
	}
		
	/**
	 * Initialize a Matrix with a constant value for all elements of the matrix
	 * @param val the initialization value
	 */
	protected void initialize(double val) {
		// Create the array
		array = new double[Constants.SIZE_4][Constants.SIZE_4];
		// Initialize values
		for (int i=0; i<Constants.SIZE_4; i++) {
			for (int j=0; j<Constants.SIZE_4; j++) {
				array[i][j] = val;
			}
		}
	}
	
	public void setArray(double[][] a) throws MatrixArrayWrongSizeException {
		if (a.length != Constants.SIZE_4) throw new MatrixArrayWrongSizeException("Wrong array row size ("+a.length+") while creating Matrix4 from array"); 
		if (a[0].length != Constants.SIZE_4) throw new MatrixArrayWrongSizeException("Wrong array column size ("+a[0].length+") while creating Matrix4 from array"); 
		this.array = a;
	}
	
	@Override
	public String toString() {
		String s = "[";
		for (int i=0; i<Constants.SIZE_4-1; i++) {
			s = s + Arrays.toString(array[i]);
			s = s+"\n";
		}
		s = s + Arrays.toString(array[Constants.SIZE_4-1])+"]";
		return s;
	}
	
	/**
	 * Set value of an element of the Matrix
	 * @param i row indice
	 * @param j column indice
	 * @param val value to set
	 */
	public void set(int i, int j, double val) throws IndiceOutOfBoundException {
		if (i<0 || i>Constants.SIZE_4) throw new IndiceOutOfBoundException("Row indice ("+i+") out of bound while setting element ("+i+","+j+") of Matrix4"); 
		if (j<0 || j>Constants.SIZE_4) throw new IndiceOutOfBoundException("Column indice ("+j+") out of bound while setting element ("+i+","+j+") of Matrix4"); 
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
		for (int i=0; i<Constants.SIZE_4-1; i++) {
			array[i][i] = v;
		}
	}
	


}
