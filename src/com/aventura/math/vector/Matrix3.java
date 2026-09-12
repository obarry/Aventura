package com.aventura.math.vector;

import java.util.Arrays;

import com.aventura.math.Constants;
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
 **/

public class Matrix3 {

	protected static final float[][] IDENTITY_ARRAY =
		{{1.0f, 0.0f, 0.0f},
		 {0.0f, 1.0f ,0.0f},
		 {0.0f, 0.0f, 1.0f}};

	public static final Matrix3 IDENTITY = new Matrix3(IDENTITY_ARRAY);

	protected float[][] array;

	/**
	 * Initialize a square Matrix of size s with 0 for all elements of the matrix
	 * @param s the size of the Matrix (number of rows and columns)
	 */
	public Matrix3() {
		initialize(0);
	}


	/**
	 * Initialize Matrix with a constant value for all elements of the matrix
	 * @param val the initialization value
	 */
	public Matrix3(float val) {
		initialize(val);
	}

	/**
	 * Initialize Matrix with a 2D array of double.
	 * Bug fix: this used to store the given array by reference, so any later mutation of the caller's
	 * array would silently corrupt this Matrix's state (see audit report, aliasing/encapsulation issue).
	 * A defensive copy is made instead - verified against every real call site in the codebase to have
	 * no observable effect there (the array passed in is always freshly built and never reused afterward).
	 * @param a the 2D array of double
	 */
	public Matrix3(float[][] a) {
		this.array = copyOfArray(a);
	}

	/**
	 * Defensive copy helper (new private method) used by the constructor above and by setArray().
	 * @param a the source array, expected to be Constants.SIZE_3 x Constants.SIZE_3
	 * @return a new array with the same content as a
	 */
	private static float[][] copyOfArray(float[][] a) {
		float[][] copy = new float[a.length][];
		for (int i=0; i<a.length; i++) {
			copy[i] = a[i].clone();
		}
		return copy;
	}

	/**
	 * Initialize Matrix with data from another Matrix
	 * @param a the other Matrix
	 */
	public Matrix3(Matrix3 a) {
		set(a);
	}

	/**
	 * Initialize a Matrix with a constant value for all elements of the matrix
	 * @param val the initialization value
	 */
	protected void initialize(float val) {
		// Create the array
		array = new float[Constants.SIZE_3][Constants.SIZE_3];
		// Initialize values
		for (int i=0; i<Constants.SIZE_3; i++) {
			for (int j=0; j<Constants.SIZE_3; j++) {
				array[i][j] = val;
			}
		}
	}

	public void setArray(float[][] a) throws MatrixArrayWrongSizeException {
		if (a.length != Constants.SIZE_3) throw new MatrixArrayWrongSizeException("Wrong array row size ("+a.length+") while creating Matrix3 from array");
		if (a[0].length != Constants.SIZE_3) throw new MatrixArrayWrongSizeException("Wrong array column size ("+a[0].length+") while creating Matrix3 from array");
		// Bug fix: same aliasing issue as the float[][] constructor above - now stores a defensive copy.
		this.array = copyOfArray(a);
	}

	/**
	 * Set Matrix from another Matrix4
	 * @param a the other Matrix
	 */
	public void set(Matrix3 a) {
		// Create the array
		array = new float[Constants.SIZE_3][Constants.SIZE_3];

		for (int i=0; i<Constants.SIZE_3; i++) {
			for (int j=0; j<Constants.SIZE_3; j++) {
				this.array[i][j] = a.array[i][j];
			}
		}
	}

	@Override
	public String toString() {
		String s = "[";
		s = s + Arrays.toString(array[0]) + "\n";
		for (int i=1; i<Constants.SIZE_3-1; i++) {
			s = s + " " + Arrays.toString(array[i]) + "\n";
		}
		s = s + " " + Arrays.toString(array[Constants.SIZE_3-1])+"]";
		return s;
	}

	/**
	 * Set value of an element of the Matrix
	 * @param i row indice
	 * @param j column indice
	 * @param val value to set
	 */
	public void set(int i, int j, float val) {
		array[i][j] = val;
	}

	/**
	 * Get value of an element of the Matrix
	 * @param i row indice
	 * @param j column indice
	 * @return the value of the element
	 */
	public float get(int i, int j) {
		return array[i][j];
	}

	/**
	 * Set all elements of the diagonal of this Matrix to a given value
	 * @param v the value to set
	 */
	public void setDiagonal(float v) {
		array[0][0] = v; 
		array[1][1] = v; 
		array[2][2] = v; 
	}


	/**
	 * Get row of a Matrix3 in the format of a Vector3
	 * @param r the rank of the row
	 * @return a Vector3 representing the row
	 * @throws IndexOutOfBoundException
	 */
	public Vector3 getRow(int r) throws IndexOutOfBoundException {
		if (r<0 || r>=Constants.SIZE_3) throw new IndexOutOfBoundException("Index out of bound while getting Row ("+r+") of Matrix3");
		float[] array = new float[Constants.SIZE_3];
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
	 * @throws IndexOutOfBoundException
	 */
	public Vector3 getColumn(int c) throws IndexOutOfBoundException {
		if (c<0 || c>=Constants.SIZE_3) throw new IndexOutOfBoundException("Index out of bound while getting Column ("+c+") of Matrix3");
		float[] array = new float[Constants.SIZE_3];
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
	 * Set row of a Matrix3 in the format of a Vector3
	 * (new method, added to align Matrix3 services with Matrix4.setRow)
	 * @param r the rank of the row
	 * @param v a Vector3 representing the row
	 * @throws IndexOutOfBoundException
	 */
	public void setRow(int r, Vector3 v) throws IndexOutOfBoundException {
		if (r<0 || r>=Constants.SIZE_3) throw new IndexOutOfBoundException("Index out of bound while setting Row ("+r+") of Matrix3");
		// No loop for optimization
		this.array[r][0] = v.getX();
		this.array[r][1] = v.getY();
		this.array[r][2] = v.getZ();
	}

	/**
	 * Set column of a Matrix3 in the format of a Vector3
	 * (new method, added to align Matrix3 services with Matrix4.setColumn)
	 * @param c the rank of the column
	 * @param v a Vector3 representing the column
	 * @throws IndexOutOfBoundException
	 */
	public void setColumn(int c, Vector3 v) throws IndexOutOfBoundException {
		if (c<0 || c>=Constants.SIZE_3) throw new IndexOutOfBoundException("Index out of bound while setting Column ("+c+") of Matrix3");
		// No loop for optimization
		this.array[0][c] = v.getX();
		this.array[1][c] = v.getY();
		this.array[2][c] = v.getZ();
	}

	/**
	 * Get a defensive copy of the internal 2D array of this Matrix3.
	 * (new method, added to align Matrix3 services with Matrix4.getArray; unlike Matrix4.getArray()
	 * this returns a copy, not the internal reference, to avoid exposing internal state - see audit report)
	 * @return a new 2D array holding a copy of this Matrix3's elements
	 */
	public float[][] getArray() {
		float[][] copy = new float[Constants.SIZE_3][Constants.SIZE_3];
		for (int i=0; i<Constants.SIZE_3; i++) {
			for (int j=0; j<Constants.SIZE_3; j++) {
				copy[i][j] = this.array[i][j];
			}
		}
		return copy;
	}

	/**
	 * Sum of the diagonal elements of this Matrix (new method)
	 * @return the trace of this Matrix
	 */
	public float trace() {
		return array[0][0] + array[1][1] + array[2][2];
	}

	/**
	 * Whether this Matrix is equal to the Identity matrix, within Constants.EPSILON tolerance (new method)
	 * @return true if this Matrix is the Identity matrix
	 */
	public boolean isIdentity() {
		return this.equals(IDENTITY);
	}

	/**
	 * Determinant of this 3x3 Matrix, computed by cofactor expansion along the first row (new method).
	 * Useful on its own (e.g. to detect a degenerate/non-invertible transform cheaply) without paying
	 * for a full inverse() just to find out the Matrix is singular.
	 * @return the determinant of this Matrix
	 */
	public float determinant() {
		return array[0][0] * (array[1][1]*array[2][2] - array[1][2]*array[2][1])
		     - array[0][1] * (array[1][0]*array[2][2] - array[1][2]*array[2][0])
		     + array[0][2] * (array[1][0]*array[2][1] - array[1][1]*array[2][0]);
	}

	/**
	 * Compare this Matrix with another
	 * @param B the other Matrix
	 * @return true if all the elements of this Matrix are equals to the elements of B
	 */
	public boolean equals(Matrix3 B) {
		
		for (int i=0; i<Constants.SIZE_3; i++) {
			for (int j=0; j<Constants.SIZE_3; j++) {
				if (Math.abs(this.get(i,j) - B.get(i,j)) > Constants.EPSILON) return false;
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

		for (int i=0; i<Constants.SIZE_3; i++) {
			for (int j=0; j<Constants.SIZE_3; j++) {
				r.set(i,j, this.get(i,0)*b.get(0,j) + this.get(i,1)*b.get(1,j) + this.get(i,2)*b.get(2,j));
			}
		}

		return r;
	}

	/**
	 * A=A^B
	 * @param B
	 */
	public void timesEquals(Matrix3 b) {
		float[][] array = new float[Constants.SIZE_3][Constants.SIZE_3];
		for (int i=0; i<Constants.SIZE_3; i++) {
			for (int j=0; j<Constants.SIZE_3; j++) {
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
	public Matrix3 times(float s) {
		Matrix3 r = new Matrix3();
		for (int i=0; i<Constants.SIZE_3; i++) {
			for (int j=0; j<Constants.SIZE_3; j++) {
				r.set(i,j, this.array[i][j]*s);
			}
		}
		return r;
	}

	/**
	 * Multiply this Matrix by a scalar A = A*s
	 * @param s the scalar value
	 */
	public void timesEquals(float s) {
		for (int i=0; i<Constants.SIZE_3; i++) {
			for (int j=0; j<Constants.SIZE_3; j++) {
				this.array[i][j] = this.array[i][j]*s;
			}
		}
	}

	/**
	 * Matrix transposition
	 * @return a new Matrix corresponding to the transposition of the current Matrix 
	 */
	public Matrix3 transpose() {
		Matrix3 r = new Matrix3();

		for (int i=0; i<Constants.SIZE_3; i++) {
			for (int j=0; j<Constants.SIZE_3; j++) {
				r.set(i,j, this.get(j, i));
			}
		}
		return r;
	}

	/**
	 * Matrix transposition
	 *Transpose the current Matrix 
	 */
	public void transposeEquals() {
		float[][] array = new float[Constants.SIZE_3][Constants.SIZE_3];

		for (int i=0; i<Constants.SIZE_3; i++) {
			for (int j=0; j<Constants.SIZE_3; j++) {
				array[i][j] = this.get(j,i);
			}
		}
		this.array = array;
	}


	/**
	 * Matrix addition C=A+B. Do not modify this Matrix (A), this Matrix and return C a newly created Matrix.
	 * @param B the Matrix to be added
	 * @return C, a new Matrix, sum of this Matrix (A) and B Matrix
	 */
	public Matrix3 plus(Matrix3 B) {
		Matrix3 r = new Matrix3();
		for (int i=0; i<Constants.SIZE_3; i++) {
			for (int j=0; j<Constants.SIZE_3; j++) {
				r.set(i,j, this.array[i][j]+B.get(i,j));
			}
		}
		return r;		
	}

	/**
	 * Matrix addition A=A+B. This Matrix (A) is modified and contains the result of the operation.
	 * @param B the Matrix to be added to this Matrix
	 */
	public void plusEquals(Matrix3 B) {
		for (int i=0; i<Constants.SIZE_3; i++) {
			for (int j=0; j<Constants.SIZE_3; j++) {
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
		for (int i=0; i<Constants.SIZE_3; i++) {
			for (int j=0; j<Constants.SIZE_3; j++) {
				r.set(i,j, this.array[i][j]-B.get(i,j));
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
		for (int i=0; i<Constants.SIZE_3; i++) {
			for (int j=0; j<Constants.SIZE_3; j++) {
				this.array[i][j] = this.array[i][j]-B.get(i,j);
			}
		}
	}


	public Vector3 times(Vector3 v) {
		// Rely on the service provided by the Vector3D class
		// Optimal call is to use Vector3D method directly
		return v.times(this);
	}

	/**
	 * Swap rows a and b of the matrix
	 * @param a first row to swap
	 * @param b second row to swap
	 */
	public void swapRows(int a, int b) {
		float row_a;
		for (int j=0; j<Constants.SIZE_3; j++) {
			row_a = this.array[a][j];
			this.array[a][j] = this.array[b][j];
			this.array[b][j] = row_a;
		}
	}

	/**
	 * Multiply entire row a by value s
	 * @param a row
	 * @param s value
	 * @throws IndexOutOfBoundException
	 */
	public void timesRow(int a, float s) throws IndexOutOfBoundException {
		if (a<0 || a>=Constants.SIZE_3) throw new IndexOutOfBoundException("Index out of bound while multiplying Row ("+a+") of Matrix3");

		for (int j=0; j<Constants.SIZE_3; j++) {
			this.array[a][j]*=s;
		}
	}

	public Matrix3 inverse() throws NotInvertibleMatrixException {
		Matrix3 identity = new Matrix3(IDENTITY);
		Matrix3 matrix = new Matrix3(this); // copy of the current Matrix to not modify the original

		// Methode du Pivot de Gauss
		// From this reference:  https://fr.wikipedia.org/wiki/%C3%89limination_de_Gauss-Jordan

		int r = 0; // last pivot row
		float pivot = 0; // Pivot value

		// Browsing columns one by one
		for (int j=0; j<Constants.SIZE_3; j++) {
			int k = indiceOfMaxRowInColumn(matrix,j, r);
			pivot = matrix.get(k, j); // Pivot

			// Use an epsilon-based tolerance rather than a strict equality to 0: with accumulated floating point
			// rounding errors, a near-singular Matrix can have a pivot that is not exactly 0 but numerically
			// meaningless, which would otherwise silently produce an unstable (Infinity/NaN-laden) result.
			if (Math.abs(pivot) < Constants.EPSILON) throw new NotInvertibleMatrixException();
			// Else if pivot is not null then continue

			// Divide all the row by the pivot to reduce the pivot to 1
			for (int col=0; col < Constants.SIZE_3; col++) {
				matrix.set(k,col, matrix.get(k,col)/pivot);
				identity.set(k,col, identity.get(k,col)/pivot);
			}
			// Let's swap the rows k and r
			if (r != k) {
				matrix.swapRows(r,k);
				identity.swapRows(r,k);
			}
			for (int i=0; i<Constants.SIZE_3; i++) {
				if (i != r) {
					float matrix_ij = matrix.get(i, j);
					for (int col=0; col < Constants.SIZE_3; col++) {
						matrix.set(i,col, matrix.get(i,col) - matrix.get(r,col)*matrix_ij);
						identity.set(i,col, identity.get(i,col) - identity.get(r,col)*matrix_ij);
					}					
				}
			}
			r++;
		}

		return identity;
	}

	/** 
	 * Calculate the indice of the max abs value in column col starting at indice pivot in Matrix m
	 * @param m
	 * @param col
	 * @param pivot
	 * @return
	 */
	static protected int indiceOfMaxRowInColumn(Matrix3 m, int col, int pivot) {
		// Bug fix: the search must also consider the pivot row itself, not only the rows below it,
		// otherwise the row actually holding the largest absolute value in the column can be missed
		// and the partial pivoting loses its numerical stability benefit (see audit report).
		float max = Math.abs(m.get(pivot, col));
		int indiceMax = pivot;
		for (int i=pivot+1; i < Constants.SIZE_3; i++) {
			float val = Math.abs(m.get(i, col));
			if (max < val) {
				max = val;
				indiceMax = i;
			}
		}
		return indiceMax;
	}

}
