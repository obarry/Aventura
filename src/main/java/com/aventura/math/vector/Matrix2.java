package com.aventura.math.vector;

import java.util.Arrays;

import com.aventura.math.Constants;

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
 * New class: the 2x2 counterpart to Matrix3/Matrix4, closing the last gap in this lib's Vector2/3/4
 * + Matrix2/3/4 family (see audit report - Matrix2 was deferred to its own phase). Mirrors Matrix3's
 * public surface and conventions exactly (identity() static accessor rather than a public mutable
 * constant, protected swapRows()/timesRow(), the shared GaussJordanSolver for inverse(), equals(Object)/
 * hashCode()) so it drops into the rest of the lib without introducing a second set of conventions.
 *
 * @author Olivier BARRY
 * @since September 2026
 *
 */
public class Matrix2 {

	private static final float[][] IDENTITY_ARRAY =
		{{1.0f, 0.0f},
		 {0.0f, 1.0f}};

	// Backing value for identity() below. Kept private so the mutable-Matrix-as-a-shared-constant
	// footgun (public static final field pointing to a mutable object) cannot happen: nothing outside
	// this class can ever hold a reference to this particular instance.
	private static final Matrix2 IDENTITY_VALUE = new Matrix2(IDENTITY_ARRAY);

	/**
	 * @return a new Matrix2 equal to the identity matrix - a fresh copy every call, safe to mutate.
	 */
	public static Matrix2 identity() {
		return new Matrix2(IDENTITY_VALUE);
	}

	protected float[][] array;

	/**
	 * Initialize a 2x2 Matrix with 0 for all elements of the matrix
	 */
	public Matrix2() {
		initialize(0);
	}

	/**
	 * Initialize Matrix with a constant value for all elements of the matrix
	 * @param val the initialization value
	 */
	public Matrix2(float val) {
		initialize(val);
	}

	/**
	 * Initialize Matrix with a 2D array of float. A defensive copy is made (consistent with
	 * Matrix3/Matrix4's constructor - see audit report on the aliasing/encapsulation issue their
	 * original versions had).
	 * @param a the 2D array of float
	 */
	public Matrix2(float[][] a) {
		this.array = copyOfArray(a);
	}

	/**
	 * Defensive copy helper used by the constructor above and by setArray().
	 * @param a the source array, expected to be Constants.SIZE_2 x Constants.SIZE_2
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
	public Matrix2(Matrix2 a) {
		set(a);
	}

	/**
	 * Initialize a Matrix with a constant value for all elements of the matrix
	 * @param val the initialization value
	 */
	protected void initialize(float val) {
		array = new float[Constants.SIZE_2][Constants.SIZE_2];
		for (int i=0; i<Constants.SIZE_2; i++) {
			for (int j=0; j<Constants.SIZE_2; j++) {
				array[i][j] = val;
			}
		}
	}

	public void setArray(float[][] a) throws MatrixArrayWrongSizeException {
		if (a.length != Constants.SIZE_2) throw new MatrixArrayWrongSizeException("Wrong array row size ("+a.length+") while creating Matrix2 from array");
		if (a[0].length != Constants.SIZE_2) throw new MatrixArrayWrongSizeException("Wrong array column size ("+a[0].length+") while creating Matrix2 from array");
		this.array = copyOfArray(a);
	}

	/**
	 * Set Matrix from another Matrix2
	 * @param a the other Matrix
	 */
	public void set(Matrix2 a) {
		array = new float[Constants.SIZE_2][Constants.SIZE_2];
		for (int i=0; i<Constants.SIZE_2; i++) {
			for (int j=0; j<Constants.SIZE_2; j++) {
				this.array[i][j] = a.array[i][j];
			}
		}
	}

	@Override
	public String toString() {
		String s = "[";
		s = s + Arrays.toString(array[0]) + "\n";
		s = s + " " + Arrays.toString(array[Constants.SIZE_2-1])+"]";
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
	}

	/**
	 * Get row of a Matrix2 in the format of a Vector2
	 * @param r the rank of the row
	 * @return a Vector2 representing the row
	 * @throws IndexOutOfBoundException
	 */
	public Vector2 getRow(int r) throws IndexOutOfBoundException {
		if (r<0 || r>=Constants.SIZE_2) throw new IndexOutOfBoundException("Index out of bound while getting Row ("+r+") of Matrix2");
		return new Vector2(this.array[r][0], this.array[r][1]);
	}

	/**
	 * Get column of a Matrix2 in the format of a Vector2
	 * @param c the rank of the column
	 * @return a Vector2 representing the column
	 * @throws IndexOutOfBoundException
	 */
	public Vector2 getColumn(int c) throws IndexOutOfBoundException {
		if (c<0 || c>=Constants.SIZE_2) throw new IndexOutOfBoundException("Index out of bound while getting Column ("+c+") of Matrix2");
		return new Vector2(this.array[0][c], this.array[1][c]);
	}

	/**
	 * Set row of a Matrix2 in the format of a Vector2
	 * @param r the rank of the row
	 * @param v a Vector2 representing the row
	 * @throws IndexOutOfBoundException
	 */
	public void setRow(int r, Vector2 v) throws IndexOutOfBoundException {
		if (r<0 || r>=Constants.SIZE_2) throw new IndexOutOfBoundException("Index out of bound while setting Row ("+r+") of Matrix2");
		this.array[r][0] = v.getX();
		this.array[r][1] = v.getY();
	}

	/**
	 * Set column of a Matrix2 in the format of a Vector2
	 * @param c the rank of the column
	 * @param v a Vector2 representing the column
	 * @throws IndexOutOfBoundException
	 */
	public void setColumn(int c, Vector2 v) throws IndexOutOfBoundException {
		if (c<0 || c>=Constants.SIZE_2) throw new IndexOutOfBoundException("Index out of bound while setting Column ("+c+") of Matrix2");
		this.array[0][c] = v.getX();
		this.array[1][c] = v.getY();
	}

	/**
	 * Get a defensive copy of the internal 2D array of this Matrix2.
	 * @return a new 2D array holding a copy of this Matrix2's elements
	 */
	public float[][] getArray() {
		return copyOfArray(this.array);
	}

	/**
	 * Sum of the diagonal elements of this Matrix
	 * @return the trace of this Matrix
	 */
	public float trace() {
		return array[0][0] + array[1][1];
	}

	/**
	 * Whether this Matrix is equal to the Identity matrix, within Constants.EPSILON tolerance
	 * @return true if this Matrix is the Identity matrix
	 */
	public boolean isIdentity() {
		return this.equals(IDENTITY_VALUE);
	}

	/**
	 * Determinant of this 2x2 Matrix: a*d - b*c.
	 * @return the determinant of this Matrix
	 */
	public float determinant() {
		return array[0][0]*array[1][1] - array[0][1]*array[1][0];
	}

	/**
	 * Compare this Matrix with another
	 * @param B the other Matrix
	 * @return true if all the elements of this Matrix are equals to the elements of B
	 */
	public boolean equals(Matrix2 B) {
		for (int i=0; i<Constants.SIZE_2; i++) {
			for (int j=0; j<Constants.SIZE_2; j++) {
				if (Math.abs(this.get(i,j) - B.get(i,j)) > Constants.EPSILON) return false;
			}
		}
		return true;
	}

	/**
	 * Object contract override. Delegates to equals(Matrix2) so behavior (including the epsilon
	 * tolerance) stays identical for callers that already use the typed overload. Same pattern, and
	 * same accepted equals()/hashCode()-contract tradeoff, as Matrix3/Matrix4.equals(Object).
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Matrix2)) return false;
		return equals((Matrix2)o);
	}

	/**
	 * Object contract override (paired with equals(Object) above).
	 */
	@Override
	public int hashCode() {
		int result = 1;
		for (int i=0; i<Constants.SIZE_2; i++) {
			for (int j=0; j<Constants.SIZE_2; j++) {
				result = 31*result + Float.floatToIntBits(this.array[i][j]);
			}
		}
		return result;
	}

	/**
	 * C=A^B
	 * @param b the other Matrix
	 * @return a new Matrix, product of this Matrix (A) and b (B)
	 */
	public Matrix2 times(Matrix2 b) {
		Matrix2 r = new Matrix2();
		for (int i=0; i<Constants.SIZE_2; i++) {
			for (int j=0; j<Constants.SIZE_2; j++) {
				r.set(i,j, this.get(i,0)*b.get(0,j) + this.get(i,1)*b.get(1,j));
			}
		}
		return r;
	}

	/**
	 * A=A^B
	 * @param b the other Matrix
	 */
	public void timesEquals(Matrix2 b) {
		float[][] array = new float[Constants.SIZE_2][Constants.SIZE_2];
		for (int i=0; i<Constants.SIZE_2; i++) {
			for (int j=0; j<Constants.SIZE_2; j++) {
				array[i][j] = this.get(i,0)*b.get(0,j) + this.get(i,1)*b.get(1,j);
			}
		}
		this.array = array;
	}

	/**
	 * Multiply a Matrix by a scalar B=A*s
	 * @param s the scalar value
	 * @return a new Matrix B=A*s
	 */
	public Matrix2 times(float s) {
		Matrix2 r = new Matrix2();
		for (int i=0; i<Constants.SIZE_2; i++) {
			for (int j=0; j<Constants.SIZE_2; j++) {
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
		for (int i=0; i<Constants.SIZE_2; i++) {
			for (int j=0; j<Constants.SIZE_2; j++) {
				this.array[i][j] = this.array[i][j]*s;
			}
		}
	}

	/**
	 * Matrix transposition
	 * @return a new Matrix corresponding to the transposition of the current Matrix
	 */
	public Matrix2 transpose() {
		Matrix2 r = new Matrix2();
		for (int i=0; i<Constants.SIZE_2; i++) {
			for (int j=0; j<Constants.SIZE_2; j++) {
				r.set(i,j, this.get(j, i));
			}
		}
		return r;
	}

	/**
	 * Transpose the current Matrix
	 */
	public void transposeEquals() {
		float[][] array = new float[Constants.SIZE_2][Constants.SIZE_2];
		for (int i=0; i<Constants.SIZE_2; i++) {
			for (int j=0; j<Constants.SIZE_2; j++) {
				array[i][j] = this.get(j,i);
			}
		}
		this.array = array;
	}

	/**
	 * Matrix addition C=A+B. Does not modify this Matrix (A), returns C a newly created Matrix.
	 * @param B the Matrix to be added
	 * @return C, a new Matrix, sum of this Matrix (A) and B Matrix
	 */
	public Matrix2 plus(Matrix2 B) {
		Matrix2 r = new Matrix2();
		for (int i=0; i<Constants.SIZE_2; i++) {
			for (int j=0; j<Constants.SIZE_2; j++) {
				r.set(i,j, this.array[i][j]+B.get(i,j));
			}
		}
		return r;
	}

	/**
	 * Matrix addition A=A+B. This Matrix (A) is modified and contains the result of the operation.
	 * @param B the Matrix to be added to this Matrix
	 */
	public void plusEquals(Matrix2 B) {
		for (int i=0; i<Constants.SIZE_2; i++) {
			for (int j=0; j<Constants.SIZE_2; j++) {
				this.array[i][j] = this.array[i][j]+B.get(i,j);
			}
		}
	}

	/**
	 * Matrix subtraction C=A-B. Does not modify this Matrix (A), returns C a newly created Matrix.
	 * @param B the Matrix to be subtracted
	 * @return C, a new Matrix, subtraction of this Matrix (A) and B Matrix
	 */
	public Matrix2 minus(Matrix2 B) {
		Matrix2 r = new Matrix2();
		for (int i=0; i<Constants.SIZE_2; i++) {
			for (int j=0; j<Constants.SIZE_2; j++) {
				r.set(i,j, this.array[i][j]-B.get(i,j));
			}
		}
		return r;
	}

	/**
	 * Matrix subtraction A=A-B. This Matrix (A) is modified and contains the result of the operation.
	 * @param B the Matrix to be subtracted to this Matrix
	 */
	public void minusEquals(Matrix2 B) {
		for (int i=0; i<Constants.SIZE_2; i++) {
			for (int j=0; j<Constants.SIZE_2; j++) {
				this.array[i][j] = this.array[i][j]-B.get(i,j);
			}
		}
	}

	/**
	 * W = A.V; Multiplication of this Matrix2 A by a Vector2 V.
	 * Delegates to Vector2.times(Matrix2), the same design already used by Matrix3.times(Vector3)/
	 * Matrix4... (Vector4.times(Matrix4) equivalent) - the Vector class owns the actual multiplication.
	 * @param v the Vector2
	 * @return a new Vector2, result of the multiplication
	 */
	public Vector2 times(Vector2 v) {
		return v.times(this);
	}

	/**
	 * Swap rows a and b of the matrix. Package-private implementation detail of inverse() below
	 * (protected from the start, unlike Matrix3/Matrix4's original public version - see audit report:
	 * this class is new, so there is no legacy public caller to preserve compatibility with).
	 * @param a first row to swap
	 * @param b second row to swap
	 */
	protected void swapRows(int a, int b) {
		float row_a;
		for (int j=0; j<Constants.SIZE_2; j++) {
			row_a = this.array[a][j];
			this.array[a][j] = this.array[b][j];
			this.array[b][j] = row_a;
		}
	}

	/**
	 * Multiply entire row a by value s. Package-private implementation detail (see swapRows above).
	 * @param a row
	 * @param s value
	 * @throws IndexOutOfBoundException
	 */
	protected void timesRow(int a, float s) throws IndexOutOfBoundException {
		if (a<0 || a>=Constants.SIZE_2) throw new IndexOutOfBoundException("Index out of bound while multiplying Row ("+a+") of Matrix2");
		for (int j=0; j<Constants.SIZE_2; j++) {
			this.array[a][j]*=s;
		}
	}

	/**
	 * Invert this Matrix using Gauss-Jordan elimination with partial pivoting. Shares the same
	 * GaussJordanSolver used by Matrix3.inverse()/Matrix4.inverse() (generic to any square size).
	 * @return a newly created Matrix, inverse of this Matrix
	 * @throws NotInvertibleMatrixException if this Matrix is singular (not invertible)
	 */
	public Matrix2 inverse() throws NotInvertibleMatrixException {
		float[][] inv = GaussJordanSolver.invert(this.array, Constants.SIZE_2, Constants.EPSILON);
		return new Matrix2(inv);
	}

}
