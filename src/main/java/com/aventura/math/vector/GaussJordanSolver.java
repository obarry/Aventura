package com.aventura.math.vector;

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
 * New class (phase 2 - see audit report, section 6 "Inversion de matrice").
 *
 * Internal Gauss-Jordan elimination solver with partial pivoting, generalized to any square matrix
 * size and shared by Matrix3.inverse() and Matrix4.inverse(), which previously each carried an almost
 * identical, hand-duplicated copy of this same algorithm (one hardcoded to size 3, the other to size 4).
 *
 * Deliberately package-private and stateless: this is an implementation detail of Matrix3/Matrix4,
 * not part of the public API of this package. It operates purely on raw float[][] arrays (no
 * dependency on Matrix3/Matrix4), which is what makes a single implementation usable by both.
 *
 * Method reference: "Methode du Pivot de Gauss" - https://fr.wikipedia.org/wiki/%C3%89limination_de_Gauss-Jordan
 */
class GaussJordanSolver {

	private GaussJordanSolver() {
		// Not meant to be instantiated - all methods are static.
	}

	/**
	 * Inverts the given square matrix using Gauss-Jordan elimination with partial pivoting (the row
	 * holding the largest absolute value in the current column is chosen as pivot row, for numerical
	 * stability - see audit report on the previous version's pivot-selection bug).
	 *
	 * @param a the matrix to invert, as a size x size array; not modified (an internal working copy is made)
	 * @param size the dimension of the (square) matrix
	 * @param epsilon tolerance below which a pivot's absolute value is treated as zero (singular matrix).
	 *        An epsilon-based tolerance is used rather than a strict equality to 0: with accumulated
	 *        floating point rounding errors, a near-singular matrix can have a pivot that is not exactly
	 *        0 but numerically meaningless, which would otherwise silently produce an unstable
	 *        (Infinity/NaN-laden) result.
	 * @return a newly allocated size x size array holding the inverse of a
	 * @throws NotInvertibleMatrixException if a pivot's absolute value is below epsilon (singular matrix)
	 */
	static float[][] invert(float[][] a, int size, float epsilon) throws NotInvertibleMatrixException {
		float[][] matrix = copyOf(a, size);
		float[][] result = identityOf(size); // will end up holding the inverse, in place of the Matrix3/Matrix4
		                                      // "identity" working copy used by the previous, duplicated versions

		int r = 0; // last pivot row

		// Browsing columns one by one
		for (int j=0; j<size; j++) {
			int k = indiceOfMaxRowInColumn(matrix, size, j, r);
			float pivot = matrix[k][j];

			if (Math.abs(pivot) < epsilon) throw new NotInvertibleMatrixException();
			// Else if pivot is not null then continue

			// Divide all the row by the pivot to reduce the pivot to 1
			for (int col=0; col<size; col++) {
				matrix[k][col] /= pivot;
				result[k][col] /= pivot;
			}
			// Let's swap the rows k and r
			if (r != k) {
				swapRows(matrix, r, k);
				swapRows(result, r, k);
			}
			for (int i=0; i<size; i++) {
				if (i != r) {
					float matrix_ij = matrix[i][j];
					for (int col=0; col<size; col++) {
						matrix[i][col] -= matrix[r][col]*matrix_ij;
						result[i][col] -= result[r][col]*matrix_ij;
					}
				}
			}
			r++;
		}

		return result;
	}

	private static float[][] copyOf(float[][] a, int size) {
		float[][] c = new float[size][];
		for (int i=0; i<size; i++) {
			c[i] = a[i].clone();
		}
		return c;
	}

	private static float[][] identityOf(int size) {
		float[][] id = new float[size][size];
		for (int i=0; i<size; i++) {
			id[i][i] = 1.0f;
		}
		return id;
	}

	private static void swapRows(float[][] a, int r1, int r2) {
		float[] tmp = a[r1];
		a[r1] = a[r2];
		a[r2] = tmp;
	}

	/**
	 * Index of the row holding the largest absolute value in column col, searching from row "pivot"
	 * onward (inclusive - a past bug in the duplicated per-class versions started the search at
	 * pivot+1, missing the pivot row itself, see audit report).
	 */
	static int indiceOfMaxRowInColumn(float[][] m, int size, int col, int pivot) {
		float max = Math.abs(m[pivot][col]);
		int indiceMax = pivot;
		for (int i=pivot+1; i<size; i++) {
			float val = Math.abs(m[i][col]);
			if (max < val) {
				max = val;
				indiceMax = i;
			}
		}
		return indiceMax;
	}
}
