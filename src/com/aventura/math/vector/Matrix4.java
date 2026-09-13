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
public class Matrix4 {

    private static final float[][] IDENTITY_ARRAY =
    		{{1.0f, 0.0f, 0.0f, 0.0f},
    		 {0.0f, 1.0f ,0.0f, 0.0f},
    		 {0.0f, 0.0f, 1.0f, 0.0f},
    		 {0.0f, 0.0f, 0.0f, 1.0f}};

    // Backing value for identity() below. Kept private so the mutable-Matrix4-as-a-shared-constant
    // footgun (public static final field pointing to a mutable object - see audit report) cannot happen:
    // nothing outside this class can ever hold a reference to this particular instance.
    private static final Matrix4 IDENTITY_VALUE = new Matrix4(IDENTITY_ARRAY);

    /**
     * @return a new Matrix4 equal to the identity matrix - a fresh copy every call, safe to mutate.
     */
    public static Matrix4 identity() {
    	return new Matrix4(IDENTITY_VALUE);
    }

	protected float[][] array;

	/**
	 * Initialize a Matrix4 with 0 for all elements of the matrix.
	 * (Javadoc fixed: this no-arg constructor takes no size parameter; a Matrix4 is always 4x4.
	 * Java already zero-fills a newly created float[][], so allocating the array is enough here -
	 * no explicit loop is needed, unlike initialize(val) which is used for a non-zero value.)
	 */
	public Matrix4() {
		// Only create the array: relies on Java zero-filling it by default (optimization vs initialize(0))
		array = new float[Constants.SIZE_4][Constants.SIZE_4];
	}

		
	/**
	 * Initialize Matrix with a constant value for all elements of the matrix
	 * @param val the initialization value
	 */
	public Matrix4(float val) {
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
	public Matrix4(float[][] a) {
		this.array = copyOfArray(a);
	}

	/**
	 * Defensive copy helper (new private method) used by the constructor above, setArray() and getArray().
	 * @param a the source array, expected to be Constants.SIZE_4 x Constants.SIZE_4
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
	public Matrix4(Matrix4 a) {
		set(a);
	}
		
	/**
	 * Initialize a Matrix with a constant value for all elements of the matrix
	 * @param val the initialization value
	 */
	protected void initialize(float val) {
		// Create the array
		array = new float[Constants.SIZE_4][Constants.SIZE_4];
		// Initialize values
		for (int i=0; i<Constants.SIZE_4; i++) {
			for (int j=0; j<Constants.SIZE_4; j++) {
				array[i][j] = val;
			}
		}
	}
	
	public void setArray(float[][] a) throws MatrixArrayWrongSizeException {
		if (a.length != Constants.SIZE_4) throw new MatrixArrayWrongSizeException("Wrong array row size ("+a.length+") while creating Matrix4 from array");
		if (a[0].length != Constants.SIZE_4) throw new MatrixArrayWrongSizeException("Wrong array column size ("+a[0].length+") while creating Matrix4 from array");
		// Bug fix: same aliasing issue as the float[][] constructor above - now stores a defensive copy.
		this.array = copyOfArray(a);
	}
	
	@Override
	public String toString() {
		String s = "[";
		s = s + Arrays.toString(array[0]) + "\n";
		for (int i=1; i<Constants.SIZE_4-1; i++) {
			s = s + " " + Arrays.toString(array[i]) + "\n";
		}
		s = s + " " + Arrays.toString(array[Constants.SIZE_4-1])+"]";
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
	 * Set Matrix from another Matrix4
	 * @param a the other Matrix
	 */
	public void set(Matrix4 a) {
		// Create the array
		array = new float[Constants.SIZE_4][Constants.SIZE_4];
		
		for (int i=0; i<Constants.SIZE_4; i++) {
			for (int j=0; j<Constants.SIZE_4; j++) {
				this.array[i][j] = a.array[i][j];
			}
		}
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
	 * Bug fix: this used to return the internal array by reference, letting any caller mutate this
	 * Matrix's state from the outside without going through set()/setArray() (see audit report).
	 * Now returns a defensive copy, aligned with the new Matrix3.getArray(). Verified against the only
	 * 2 real call sites in the codebase (LookAt.java, both on a disposable temporary Matrix4): harmless.
	 * @return a new 2D array holding a copy of this Matrix4's elements
	 */
	public float[][] getArray() {
		return copyOfArray(array);
	}
	
	/**
	 * Set all elements of the diagonal of this Matrix to a given value
	 * @param v the value to set
	 */
	public void setDiagonal(float v) {
		// Bug fix: the loop bound was Constants.SIZE_4-1, which left the last diagonal element (3,3)
		// untouched (Matrix3.setDiagonal, in comparison, always set all elements correctly - see audit report).
		for (int i=0; i<Constants.SIZE_4; i++) {
			array[i][i] = v;
		}
	}
	
	/**
	 * Get row of a Matrix4 in the format of a Vector4
	 * @param r the rank of the row
	 * @return a Vector4 representing the row
	 * @throws IndexOutOfBoundException
	 */
	public Vector4 getRow(int r) throws IndexOutOfBoundException {
		if (r<0 || r>=Constants.SIZE_4) throw new IndexOutOfBoundException("Index out of bound while getting Row ("+r+") of Matrix4");
		float[] array = new float[Constants.SIZE_4];
		Vector4 v = null;
		// No loop for optimization
		array[0] = this.array[r][0];
		array[1] = this.array[r][1];
		array[2] = this.array[r][2];
		array[3] = this.array[r][3];
		
		try {
			v = new Vector4(array);
		} catch (VectorArrayWrongSizeException e) {
			// Do nothing, this won't happen as all arrays are controlled in size (coming from Vector4 and Matrix4)
			if (Tracer.error) Tracer.traceError(this.getClass(), "Unexpected exception: "+e);
			e.printStackTrace();
		}
		 return v;
	}
	
	/**
	 * Set row of a Matrix4 in the format of a Vector4
	 * @param r the rank of the row
	 * @param v a Vector4 representing the row
	 */
	public void setRow(int r, Vector4 v) throws IndexOutOfBoundException {
		// Bug fix: this method declared IndexOutOfBoundException but never actually validated the
		// index before, so an out-of-range r fell through to a raw ArrayIndexOutOfBoundsException instead.
		if (r<0 || r>=Constants.SIZE_4) throw new IndexOutOfBoundException("Index out of bound while setting Row ("+r+") of Matrix4");
		// No loop for optimization
		this.array[r][0] = v.get(0);
		this.array[r][1] = v.get(1);
		this.array[r][2] = v.get(2);
		this.array[r][3] = v.get(3);
	}
	
	/**
	 * Get column of a Matrix4 in the format of a Vector4
	 * @param c the rank of the column
	 * @return a Vector4 representing the column
	 * @throws IndexOutOfBoundException
	 */
	public Vector4 getColumn(int c) throws IndexOutOfBoundException {
		if (c<0 || c>=Constants.SIZE_4) throw new IndexOutOfBoundException("Index out of bound while getting Column ("+c+") of Matrix4");
		float[] array = new float[Constants.SIZE_4];
		Vector4 v = null;
		// No loop for optimization
		array[0] = this.array[0][c];
		array[1] = this.array[1][c];
		array[2] = this.array[2][c];
		array[3] = this.array[3][c];
		
		try {
			v = new Vector4(array);
		} catch (VectorArrayWrongSizeException e) {
			// Do nothing, this won't happen as all arrays are controlled in size (coming from Vector4 and Matrix4)
			if (Tracer.error) Tracer.traceError(this.getClass(), "Unexpected exception: "+e);
			e.printStackTrace();
		}
		return v;	
	}
	
	/**
	 * Set column of a Matrix4 in the format of a Vector4
	 * @param c the rank of the column
	 * @param v a Vector4 representing the column
	 * @throws IndexOutOfBoundException
	 */
	public void setColumn(int c, Vector4 v) throws IndexOutOfBoundException {
		// Bug fix: same missing validation issue as setRow above.
		if (c<0 || c>=Constants.SIZE_4) throw new IndexOutOfBoundException("Index out of bound while setting Column ("+c+") of Matrix4");
		// No loop for optimization
		this.array[0][c] = v.get(0);
		this.array[1][c] = v.get(1);
		this.array[2][c] = v.get(2);
		this.array[3][c] = v.get(3);
	}
		
	/**
	 * Sum of the diagonal elements of this Matrix (new method, mirrors Matrix3.trace)
	 * @return the trace of this Matrix
	 */
	public float trace() {
		return array[0][0] + array[1][1] + array[2][2] + array[3][3];
	}

	/**
	 * Whether this Matrix is equal to the Identity matrix, within Constants.EPSILON tolerance
	 * (new method, mirrors Matrix3.isIdentity)
	 * @return true if this Matrix is the Identity matrix
	 */
	public boolean isIdentity() {
		return this.equals(IDENTITY_VALUE);
	}

	/**
	 * Determinant of this 4x4 Matrix, computed by cofactor expansion along the first row (new method).
	 * Useful on its own (e.g. to detect a degenerate/non-invertible transform cheaply) without paying
	 * for a full inverse() just to find out the Matrix is singular.
	 * @return the determinant of this Matrix
	 */
	public float determinant() {
		float det = 0f;
		for (int col=0; col<Constants.SIZE_4; col++) {
			float sign = (col % 2 == 0) ? 1f : -1f;
			det += sign * array[0][col] * minorDeterminant3x3(0, col);
		}
		return det;
	}

	/**
	 * Determinant of the 3x3 minor obtained by removing the given row and column (new private helper,
	 * used only by determinant()).
	 * @param skipRow the row to remove
	 * @param skipCol the column to remove
	 * @return the determinant of the resulting 3x3 minor
	 */
	private float minorDeterminant3x3(int skipRow, int skipCol) {
		float[][] m = new float[3][3];
		int mi = 0;
		for (int i=0; i<Constants.SIZE_4; i++) {
			if (i == skipRow) continue;
			int mj = 0;
			for (int j=0; j<Constants.SIZE_4; j++) {
				if (j == skipCol) continue;
				m[mi][mj] = array[i][j];
				mj++;
			}
			mi++;
		}
		return m[0][0]*(m[1][1]*m[2][2] - m[1][2]*m[2][1])
		     - m[0][1]*(m[1][0]*m[2][2] - m[1][2]*m[2][0])
		     + m[0][2]*(m[1][0]*m[2][1] - m[1][1]*m[2][0]);
	}

	/**
	 * Compare this Matrix with another
	 * @param B the other Matrix
	 * @return true if all the elements of this Matrix are equals to the elements of B
	 */
	public boolean equals(Matrix4 B) {

		for (int i=0; i<Constants.SIZE_4; i++) {
			for (int j=0; j<Constants.SIZE_4; j++) {
				if (Math.abs(this.get(i,j) - B.get(i,j)) > Constants.EPSILON) return false;
			}
		}
		return true;
	}

	/**
	 * Object contract override (new method - see audit report: equals(Matrix4) above is a same-type overload,
	 * not an override of Object.equals(Object), which silently breaks the general contract). Delegates to
	 * equals(Matrix4) so behavior (including the epsilon tolerance) stays identical for callers that already
	 * use the typed overload.
	 * Note: because of that epsilon tolerance, equals() is not a strict mathematical equivalence relation, which
	 * is an inherent tension with the equals()/hashCode() contract when floating-point comparisons use a
	 * tolerance. This is accepted here as a practical tradeoff, consistent with how equals(Matrix4) already worked.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Matrix4)) return false;
		return equals((Matrix4)o);
	}

	/**
	 * Object contract override (new method, paired with equals(Object) above).
	 */
	@Override
	public int hashCode() {
		int result = 1;
		for (int i=0; i<Constants.SIZE_4; i++) {
			for (int j=0; j<Constants.SIZE_4; j++) {
				result = 31*result + Float.floatToIntBits(this.array[i][j]);
			}
		}
		return result;
	}
	
	/**
	 * Matrix cross product: C=A^B
	 * @param b the Matrix B
	 * @return C the cross product of this Matrix A with Matrix B provided in parameter 
	 */
	public Matrix4 times(Matrix4 b) {
		Matrix4 r = new Matrix4();
		
		for (int i=0; i<Constants.SIZE_4; i++) {
			for (int j=0; j<Constants.SIZE_4; j++) {
				r.set(i,j, this.get(i,0)*b.get(0,j) + this.get(i,1)*b.get(1,j) + this.get(i,2)*b.get(2,j) + this.get(i,3)*b.get(3, j));
			}
		}
		return r;
	}
	
	/**
	 * A=A^B
	 * @param B
	 */
	public void timesEquals(Matrix4 b) {
		float[][] array = new float[Constants.SIZE_4][Constants.SIZE_4];
		for (int i=0; i<Constants.SIZE_4; i++) {
			for (int j=0; j<Constants.SIZE_4; j++) {
				array[i][j] = this.get(i,0)*b.get(0,j) + this.get(i,1)*b.get(1,j) + this.get(i,2)*b.get(2,j) + this.get(i,3)*b.get(3, j);
			}
		}
		this.array = array;
	}
	
	/**
	 * Matrix transposition
	 * @return a new Matrix corresponding to the transposition of the current Matrix 
	 */
	public Matrix4 transpose() {
		Matrix4 r = new Matrix4();
		
		for (int i=0; i<Constants.SIZE_4; i++) {
			for (int j=0; j<Constants.SIZE_4; j++) {
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
		float[][] array = new float[Constants.SIZE_4][Constants.SIZE_4];
		
		for (int i=0; i<Constants.SIZE_4; i++) {
			for (int j=0; j<Constants.SIZE_4; j++) {
				array[i][j] = this.get(j,i);
			}
		}
		this.array = array;
	}

	/**
	 * Multiply a Matrix by a scalar B=A*s
	 * @param s the scalar value
	 * @return a new Matrix B=A*s
	 */
	public Matrix4 times(float s) {
		Matrix4 r = new Matrix4();
		for (int i=0; i<Constants.SIZE_4; i++) {
			for (int j=0; j<Constants.SIZE_4; j++) {
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
		for (int i=0; i<Constants.SIZE_4; i++) {
			for (int j=0; j<Constants.SIZE_4; j++) {
				this.array[i][j] = this.array[i][j]*s;
			}
		}
	}
	
	/**
	 * Matrix addition C=A+B. Do not modify this Matrix (A), this Matrix and return C a newly created Matrix.
	 * @param B the Matrix to be added
	 * @return C, a new Matrix, sum of this Matrix (A) and B Matrix
	 */
	public Matrix4 plus(Matrix4 B) {
		Matrix4 r = new Matrix4();
		for (int i=0; i<Constants.SIZE_4; i++) {
			for (int j=0; j<Constants.SIZE_4; j++) {
				r.set(i,j, this.array[i][j]+B.get(i,j));
			}
		}
		return r;		
	}

	/**
	 * Matrix addition A=A+B. This Matrix (A) is modified and contains the result of the operation.
	 * @param B the Matrix to be added to this Matrix
	 */
	public void plusEquals(Matrix4 B) {
		for (int i=0; i<Constants.SIZE_4; i++) {
			for (int j=0; j<Constants.SIZE_4; j++) {
				this.array[i][j] = this.array[i][j]+B.get(i,j);
			}
		}
	}

	/**
	 * Matrix subtraction C=A-B. Do not modify this Matrix (A), this Matrix and return C a newly created Matrix.
	 * @param B the Matrix to be subtracted
	 * @return C, a new Matrix, subtraction of this Matrix (A) and B Matrix
	 */
	public Matrix4 minus(Matrix4 B) {
		Matrix4 r = new Matrix4();
		for (int i=0; i<Constants.SIZE_4; i++) {
			for (int j=0; j<Constants.SIZE_4; j++) {
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
	public void minusEquals(Matrix4 B) {
		for (int i=0; i<Constants.SIZE_4; i++) {
			for (int j=0; j<Constants.SIZE_4; j++) {
				this.array[i][j] = this.array[i][j]-B.get(i,j);
			}
		}
	}

	public Vector4 times(Vector4 v) {
		// Rely on the service provided by the Vector4D class
		// Optimal call is to use Vector4D method directly
		return v.times(this);
	}
	
	/**
	 * Provide the subMatrix 3x3 of the matrix 4x4 (first 3 lines and 3 columns)
	 * @return a new Matrix corresponding to the subMatrix 3x3
	 */
	public Matrix3 getMatrix3() {
		Matrix3 r = new Matrix3();
		for (int i=0; i<Constants.SIZE_3; i++) {
			for (int j=0; j<Constants.SIZE_3; j++) {
				r.set(i,j,this.array[i][j]);
			}
		}
		return r;
	}
	
	/**
	 * Swap rows a and b of the matrix.
	 * Visibility reduced from public to protected (new - see audit report): this is a row-operation
	 * implementation detail, not part of the public API. Confirmed via a full scan of the codebase that
	 * nothing outside this package ever called it.
	 * @param a first row to swap
	 * @param b second row to swap
	 */
	protected void swapRows(int a, int b) {
		float row_a;
		for (int j=0; j<Constants.SIZE_4; j++) {
			row_a = this.array[a][j];
			this.array[a][j] = this.array[b][j];
			this.array[b][j] = row_a;
		}
	}

	/**
	 * Multiply entire row a by value s.
	 * Visibility reduced from public to protected (new - see audit report): this is a row-operation
	 * implementation detail, not part of the public API. Confirmed via a full scan of the codebase that
	 * nothing outside this package ever called it.
	 * @param a row
	 * @param s value
	 * @throws IndexOutOfBoundException
	 */
	protected void timesRow(int a, float s) throws IndexOutOfBoundException {
		if (a<0 || a>=Constants.SIZE_4) throw new IndexOutOfBoundException("Index out of bound while multiplying Row ("+a+") of Matrix4");

		for (int j=0; j<Constants.SIZE_4; j++) {
			this.array[a][j]*=s;
		}
	}

	/**
	 * Invert this Matrix using Gauss-Jordan elimination with partial pivoting.
	 * The algorithm itself now lives in GaussJordanSolver (new - see audit report), shared with Matrix3
	 * instead of being duplicated almost identically in both classes. This method's signature and
	 * behavior (including the NotInvertibleMatrixException on a singular Matrix) are unchanged.
	 * @return a newly created Matrix, inverse of this Matrix
	 * @throws NotInvertibleMatrixException if this Matrix is singular (not invertible)
	 */
	public Matrix4 inverse() throws NotInvertibleMatrixException {
		float[][] inv = GaussJordanSolver.invert(this.array, Constants.SIZE_4, Constants.EPSILON);
		return new Matrix4(inv);
	}

}
