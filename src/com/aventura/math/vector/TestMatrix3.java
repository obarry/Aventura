package com.aventura.math.vector;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestMatrix3 {

	@Test
	public void testMatrix3_0() {
		System.out.println("***** Test Matrix3 : testMatrix3_0 *****");

		Matrix3 A = new Matrix3();
		Matrix3 B = new Matrix3(0);
		Matrix3 C = new Matrix3(7);
		System.out.println("A="+A);
		System.out.println("B="+B);
		System.out.println("C="+C);

		if (!A.equals(B)) fail("A does not equals B");
	}

	@Test
	public void testMatrix3_array_0() {
		System.out.println("***** Test Matrix3 : testMatrix3_array() *****");

		float[][] array = new float[3][3];

		for (int i=0; i<3; i++) {
			for (int j=0; j<3; j++) {
				array[i][j] = 0;
			}
		}

		Matrix3 A;
		Matrix3 B;
		
		A = new Matrix3(array);
		B = new Matrix3();
		System.out.println("A="+A);
		System.out.println("B="+B);

		if (!A.equals(B)) fail("A does not equals B");

	}

	@Test
	public void testMatrix3_array_value() {
		System.out.println("***** Test Matrix3 : testMatrix3_array_value() *****");

		float[][] array = new float[3][3];

		for (int i=0; i<3; i++) {
			for (int j=0; j<3; j++) {
				array[i][j] = 5;
			}
		}
		array[1][2]=22.3f;

		Matrix3 A;
		Matrix3 B;
		
		A = new Matrix3(array);
		B = new Matrix3(5);
		B.set(1,2,22.3f);
		System.out.println("A="+A);
		System.out.println("B="+B);

		if (!A.equals(B)) fail("A does not equals B");
	}

	@Test
	public void testMatrix3_plus() {
		System.out.println("***** Test Matrix3 : testMatrix3_plus() *****");

		float[][] array = new float[3][3];

		for (int i=0; i<3; i++) {
			for (int j=0; j<3; j++) {
				array[i][j] = i+j;
			}
		}

		Matrix3 A;
		Matrix3 B;
		Matrix3 C;
		
		A = new Matrix3(array);
		B = new Matrix3(5);
		C = A.plus(B);
		System.out.println("A="+A);
		System.out.println("B="+B);
		System.out.println("C="+C);

		for (int i=0; i<3; i++) {
			for (int j=0; j<3; j++) {
				if (C.get(i,j) != i+j+5) fail("C does not equals A+B");
			}
		}
	}

	@Test
	public void testMatrix3_minus() {
		System.out.println("***** Test Matrix3 : testMatrix3_minus() *****");

		float[][] array = new float[3][3];

		for (int i=0; i<3; i++) {
			for (int j=0; j<3; j++) {
				array[i][j] = i+j;
			}
		}

		Matrix3 A;
		Matrix3 B;
		Matrix3 C;

		A = new Matrix3(array);
		B = new Matrix3(2);
		C = A.minus(B);

		System.out.println("A="+A);
		System.out.println("B="+B);
		System.out.println("C="+C);

		for (int i=0; i<3; i++) {
			for (int j=0; j<3; j++) {
				if (C.get(i,j) != i+j-2) fail("C does not equals A-B");
			}
		}
	}

	@Test
	public void testMatrix3_plusEquals() {
		System.out.println("***** Test Matrix3 : testMatrix3_plusEquals() *****");

		float[][] array1 = new float[3][3];
		float[][] array2 = new float[3][3];

		for (int i=0; i<3; i++) {
			for (int j=0; j<3; j++) {
				array1[i][j] = i+j;
				array2[i][j] = i-j +7;
			}
		}

		/* 
		 * A=[[0.0, 1.0, 2.0]
		 *    [1.0, 2.0, 3.0]
		 *    [2.0, 3.0, 4.0]]
		 *    
		 * B=[[7.0, 6.0, 5.0]
		 *    [8.0, 7.0, 6.0]
		 *    [9.0, 8.0, 7.0]]
		 */ 

		Matrix3 A;
		Matrix3 B;

		A = new Matrix3(array1);
		B = new Matrix3(array2);
		System.out.println("A="+A);
		System.out.println("B="+B);
		A.plusEquals(B);
		System.out.println("A="+A);
		System.out.println("B="+B);

		if (!(A.get(0,0) ==  7.0 && A.get(0,1) ==  7.0 && A.get(0,2) ==  7.0)) fail("A does not equals A+B");
		if (!(A.get(1,0) ==  9.0 && A.get(1,1) ==  9.0 && A.get(1,2) ==  9.0)) fail("A does not equals A+B");
		if (!(A.get(2,0) == 11.0 && A.get(2,1) == 11.0 && A.get(2,2) == 11.0)) fail("A does not equals A+B");
	}

	@Test
	public void testMatrix3_minusEquals() {
		System.out.println("***** Test Matrix3 : testMatrix3_minusEquals() *****");

		float[][] array1 = new float[3][3];
		float[][] array2 = new float[3][3];

		for (int i=0; i<3; i++) {
			for (int j=0; j<3; j++) {
				array1[i][j] = i+j;
				array2[i][j] = i-j +7;
			}
		}

		/* 
		 * A=[[0.0, 1.0, 2.0]
		 *    [1.0, 2.0, 3.0]
		 *    [2.0, 3.0, 4.0]]
		 *    
		 * B=[[7.0, 6.0, 5.0]
		 *    [8.0, 7.0, 6.0]
		 *    [9.0, 8.0, 7.0]]
		 */ 

		Matrix3 A;
		Matrix3 B;

		A = new Matrix3(array1);
		B = new Matrix3(array2);
		System.out.println("A="+A);
		System.out.println("B="+B);
		A.minusEquals(B);
		System.out.println("A="+A);
		System.out.println("B="+B);

		if (!(A.get(0,0) == -7.0 && A.get(0,1) == -5.0 && A.get(0,2) == -3.0)) fail("A does not equals A-B");
		if (!(A.get(1,0) == -7.0 && A.get(1,1) == -5.0 && A.get(1,2) == -3.0)) fail("A does not equals A-B");
		if (!(A.get(2,0) == -7.0 && A.get(2,1) == -5.0 && A.get(2,2) == -3.0)) fail("A does not equals A-B");
	}


	@Test
	public void testMatrix3_times() {
		System.out.println("***** Test Matrix3 : testMatrix3_times() *****");

		float[][] array1 = new float[3][3];
		float[][] array2 = new float[3][3];

		for (int i=0; i<3; i++) {
			for (int j=0; j<3; j++) {
				array1[i][j] = i+j;
				array2[i][j] = i-j +7;
			}
		}

		/* 
		 * A=[[0.0, 1.0, 2.0]
		 *    [1.0, 2.0, 3.0]
		 *    [2.0, 3.0, 4.0]]
		 *    
		 * B=[[7.0, 6.0, 5.0]
		 *    [8.0, 7.0, 6.0]
		 *    [9.0, 8.0, 7.0]]
		 */ 


		Matrix3 A;
		Matrix3 B;

		A = new Matrix3(array1);
		B = new Matrix3(array2);
		System.out.println("A="+A);
		System.out.println("B="+B);
		Matrix3 C = A.times(B);
		System.out.println("C="+C);

		if (!(C.get(0,0) == 26.0 && C.get(0,1) == 23.0 && C.get(0,2) == 20.0)) fail("C does not equals A^B");
		if (!(C.get(1,0) == 50.0 && C.get(1,1) == 44.0 && C.get(1,2) == 38.0)) fail("C does not equals A^B");
		if (!(C.get(2,0) == 74.0 && C.get(2,1) == 65.0 && C.get(2,2) == 56.0)) fail("C does not equals A^B");
	}

	@Test
	public void testMatrix3_transpose1() {
		System.out.println("***** Test Matrix3 : testMatrix3_transpose1() *****");

		float[][] array = new float[3][3];

		for (int i=0; i<3; i++) {
			for (int j=0; j<3; j++) {
				array[i][j] = i-j+2;
			}
		}

		/* 
		 * A=[[2.0, 1.0, 0.0]
		 *    [3.0, 2.0, 1.0]
		 *    [4.0, 3.0, 2.0]]
		 */ 

		Matrix3 A;

		A = new Matrix3(array);
		System.out.println("A="+A);
		Matrix3 B = A.transpose();
		System.out.println("B="+B);
		Matrix3 C = B.transpose();
		if (!A.equals(C)) fail("C =transpose(transpose(A)) does not equals A");

	}

	@Test
	public void testMatrix3_transpose2() {
		System.out.println("***** Test Matrix3 : testMatrix3_transpose2() *****");

		float[][] array = new float[3][3];

		for (int i=0; i<3; i++) {
			for (int j=0; j<3; j++) {
				array[i][j] = i-j+2;
			}
		}

		/* 
		 * A=[[2.0, 1.0, 0.0]
		 *    [3.0, 2.0, 1.0]
		 *    [4.0, 3.0, 2.0]]
		 */ 

		Matrix3 A;

		A = new Matrix3(array);
		System.out.println("A="+A);
		Matrix3 B = new Matrix3(A); // Keep image of A before transposition
		A.transposeEquals();
		System.out.println("A transposed ="+A);
		Matrix3 C = A.transpose(); // Do not modify A for this transposition
		if (!B.equals(C)) fail("transpose(transpose(A)) does not equals A");

	}

	@Test
	public void testMatrix3_inverse1() {
		System.out.println("***** Test Matrix3 : testMatrix3_inverse1() *****");

		float[][] array = new float[3][3];

		for (int i=0; i<3; i++) {
			for (int j=0; j<3; j++) {
				if (i>j) {
					array[i][j] = 0;
				} else {
					array[i][j] = 10-2*i-j;
				}
			}
		}

		/* 
		 * A=[[10.0, 9.0, 8.0]
		 *    [0.0, 7.0, 6.0]
		 *    [0.0, 0.0, 4.0]
		 *    [0.0, 0.0, 0.0]]
		 */ 

		Matrix3 A, B, C;

		A = new Matrix3(array);
		B= null;
		
		System.out.println("A="+A);
		try {
			B = A.inverse(); // Calculate inverse
			System.out.println("B="+B);
		} catch (NotInvertibleMatrixException e) {
			fail("Not invertible Matrix A");
		}
		try {
			C = B.inverse(); // Inverse the inverse
			System.out.println("C ="+C);
			if (!A.equals(C)) fail("inverse(inverse(A)) does not equals A");
		} catch (NotInvertibleMatrixException e) {
			fail("Not invertible Matrix B");
		}

	}
	
	@Test
	public void testMatrix3_inverse2() {
		System.out.println("***** Test Matrix3 : testMatrix3_inverse2() *****");

		float[][] array = new float[3][3];

		for (int i=0; i<3; i++) {
			for (int j=0; j<3; j++) {
				if (i>j) {
					array[i][j] = 0;
				} else {
					array[i][j] = 10-2*i-j;
				}
			}
		}

		/* 
		 * A=[[10.0, 9.0, 8.0, 7.0]
		 *    [0.0, 7.0, 6.0, 5.0]
		 *    [0.0, 0.0, 4.0, 3.0]
		 *    [0.0, 0.0, 0.0, 1.0]]
		 */ 

		Matrix3 A;

		A = new Matrix3(array);
		System.out.println("A="+A);
		try {
			Matrix3 B = A.inverse(); // Calculate inverse
			System.out.println("B="+B);
			Matrix3 C = B.times(A); // inverse(A).A = I
			System.out.println("C ="+C);
			if (!C.equals(Matrix3.IDENTITY)) fail("A.inverse(A) does not equals I");
		} catch (NotInvertibleMatrixException e) {
			fail("Not invertible Matrix");
		}

	}

	@Test
	public void testMatrix3_inverse3() {
		System.out.println("***** Test Matrix3 : testMatrix3_inverse3() *****");

		/* 
		 * A=[[1.0, 0.0, 0.0]
		 *    [0.0, 1.0, 0.0]
		 *    [0.0, 0.0, 1.0]]
		 */ 

		Matrix3 A;

		A = new Matrix3(Matrix3.IDENTITY);		
		System.out.println("A="+A);
		
		try {
			Matrix3 B = A.inverse(); // Calculate inverse
			System.out.println("B="+B);
			if (!B.equals(Matrix3.IDENTITY)) fail("inverse of Identity does not equals I");
		} catch (NotInvertibleMatrixException e) {
			fail("Not invertible Matrix");
		}

	}

	// ----- Additional tests added to blindage the coverage (bounds regressions, new methods, singular matrix) -----

	@Test(expected = IndexOutOfBoundException.class)
	public void testMatrix3_getRow_invalidIndex_throws() throws IndexOutOfBoundException {
		System.out.println("***** Test Matrix3 : getRow(3) out of bound must throw (off-by-one regression) *****");

		Matrix3 m = new Matrix3(Matrix3.IDENTITY);
		m.getRow(3); // valid indices are 0..2
	}

	@Test(expected = IndexOutOfBoundException.class)
	public void testMatrix3_getColumn_invalidIndex_throws() throws IndexOutOfBoundException {
		System.out.println("***** Test Matrix3 : getColumn(3) out of bound must throw (off-by-one regression) *****");

		Matrix3 m = new Matrix3(Matrix3.IDENTITY);
		m.getColumn(3); // valid indices are 0..2
	}

	@Test(expected = IndexOutOfBoundException.class)
	public void testMatrix3_timesRow_invalidIndex_throws() throws IndexOutOfBoundException {
		System.out.println("***** Test Matrix3 : timesRow(3, s) out of bound must throw (off-by-one regression) *****");

		Matrix3 m = new Matrix3(Matrix3.IDENTITY);
		m.timesRow(3, 2f);
	}

	@Test
	public void testMatrix3_setRow_setColumn() throws IndexOutOfBoundException {
		System.out.println("***** Test Matrix3 : setRow/setColumn (new methods) *****");

		Matrix3 m = new Matrix3(0f);
		m.setRow(1, new Vector3(1f, 2f, 3f));
		assertEquals(1f, m.get(1,0), 0f);
		assertEquals(2f, m.get(1,1), 0f);
		assertEquals(3f, m.get(1,2), 0f);

		m.setColumn(2, new Vector3(7f, 8f, 9f));
		assertEquals(7f, m.get(0,2), 0f);
		assertEquals(8f, m.get(1,2), 0f);
		assertEquals(9f, m.get(2,2), 0f);

		// round-trip: what we just set via setRow/setColumn must be readable via getRow/getColumn
		Vector3 col2 = m.getColumn(2);
		assertEquals(7f, col2.getX(), 0f);
		assertEquals(8f, col2.getY(), 0f);
		assertEquals(9f, col2.getZ(), 0f);
	}

	@Test(expected = IndexOutOfBoundException.class)
	public void testMatrix3_setRow_invalidIndex_throws() throws IndexOutOfBoundException {
		System.out.println("***** Test Matrix3 : setRow(3, v) out of bound must throw (new method) *****");

		Matrix3 m = new Matrix3(0f);
		m.setRow(3, Vector3.ZERO_VECTOR);
	}

	@Test(expected = IndexOutOfBoundException.class)
	public void testMatrix3_setColumn_invalidIndex_throws() throws IndexOutOfBoundException {
		System.out.println("***** Test Matrix3 : setColumn(3, v) out of bound must throw (new method) *****");

		Matrix3 m = new Matrix3(0f);
		m.setColumn(3, Vector3.ZERO_VECTOR);
	}

	@Test
	public void testMatrix3_getArray_isDefensiveCopy() {
		System.out.println("***** Test Matrix3 : getArray() returns a defensive copy (new method) *****");

		Matrix3 m = new Matrix3(Matrix3.IDENTITY);
		float[][] arr = m.getArray();
		arr[0][0] = 999f; // mutate the returned array

		// The Matrix itself must be unaffected: getArray() must not leak the internal reference
		assertEquals(1f, m.get(0,0), 0f);
	}

	@Test
	public void testMatrix3_trace() {
		System.out.println("***** Test Matrix3 : trace() (new method) *****");

		assertEquals(3f, Matrix3.IDENTITY.trace(), 0.00001f);

		Matrix3 m = new Matrix3(new float[][] {
			{2f, 0f, 0f},
			{0f, 5f, 0f},
			{0f, 0f, 9f}
		});
		assertEquals(16f, m.trace(), 0.00001f);
	}

	@Test
	public void testMatrix3_isIdentity() {
		System.out.println("***** Test Matrix3 : isIdentity() (new method) *****");

		assertTrue(Matrix3.IDENTITY.isIdentity());
		assertTrue(new Matrix3(Matrix3.IDENTITY).isIdentity());
		assertFalse(new Matrix3(0f).isIdentity());
		assertFalse(new Matrix3(1f).isIdentity()); // all-ones is not the Identity
	}

	@Test
	public void testMatrix3_equals_negativeCase() {
		System.out.println("***** Test Matrix3 : equals negative case *****");

		Matrix3 a = new Matrix3(1f);
		Matrix3 b = new Matrix3(2f);
		if (a.equals(b)) fail("a should not equal b");
	}

	@Test
	public void testMatrix3_times_isNotCommutative() {
		System.out.println("***** Test Matrix3 : A*B != B*A in general *****");

		Matrix3 a = new Matrix3(new float[][] {
			{1f, 2f, 0f},
			{0f, 1f, 0f},
			{0f, 0f, 1f}
		});
		Matrix3 b = new Matrix3(new float[][] {
			{1f, 0f, 0f},
			{3f, 1f, 0f},
			{0f, 0f, 1f}
		});

		Matrix3 ab = a.times(b);
		Matrix3 ba = b.times(a);
		if (ab.equals(ba)) fail("A*B should not equal B*A for these matrices");
	}

	@Test(expected = NotInvertibleMatrixException.class)
	public void testMatrix3_inverse_singularMatrix_throws() throws NotInvertibleMatrixException {
		System.out.println("***** Test Matrix3 : inverse() of a singular matrix must throw NotInvertibleMatrixException *****");

		// Third row is a linear combination (sum) of the first two: this matrix is singular (determinant 0).
		Matrix3 singular = new Matrix3(new float[][] {
			{1f, 2f, 3f},
			{4f, 5f, 6f},
			{5f, 7f, 9f}
		});
		singular.inverse();
	}

	@Test
	public void testMatrix3_inverse_pivotSelection_regression() {
		System.out.println("***** Test Matrix3 : partial pivoting must pick the true max-abs row (pivot bug regression) *****");

		// Column 0 has abs values [2, 5, 1] starting at pivot row 0: row 1 (value 5) must be selected,
		// not row 0 itself by default (this was the bug: the search never compared against the pivot row).
		Matrix3 m = new Matrix3(new float[][] {
			{2f, 1f, 1f},
			{5f, 1f, 1f},
			{1f, 1f, 1f}
		});
		int chosen = Matrix3.indiceOfMaxRowInColumn(m, 0, 0);
		assertEquals(1, chosen);

		// And when the pivot row already holds the max value, it must be correctly kept (not overlooked).
		Matrix3 m2 = new Matrix3(new float[][] {
			{9f, 1f, 1f},
			{5f, 1f, 1f},
			{1f, 1f, 1f}
		});
		int chosen2 = Matrix3.indiceOfMaxRowInColumn(m2, 0, 0);
		assertEquals(0, chosen2);
	}

	@Test
	public void testMatrix3_inverse_precision_generalCase() throws NotInvertibleMatrixException {
		System.out.println("***** Test Matrix3 : A * inverse(A) == Identity on a general (non-trivial-pivot) matrix *****");

		Matrix3 a = new Matrix3(new float[][] {
			{4f, 7f, 2f},
			{3f, 5f, 1f},
			{2f, 3f, 1f}
		});
		Matrix3 invA = a.inverse();
		Matrix3 product = a.times(invA);
		if (!product.equals(Matrix3.IDENTITY)) fail("A * inverse(A) should equal Identity");
	}

	@Test
	public void testMatrix3_constructor_array_isDefensiveCopy() {
		System.out.println("***** Test Matrix3 : constructor(float[][]) makes a defensive copy (aliasing bug fix) *****");

		float[][] source = new float[][] {
			{1f, 2f, 3f},
			{4f, 5f, 6f},
			{7f, 8f, 9f}
		};
		Matrix3 m = new Matrix3(source);

		// Mutate the source array after construction: the Matrix must be unaffected.
		source[0][0] = 999f;
		assertEquals(1f, m.get(0,0), 0f);
	}

	@Test
	public void testMatrix3_setArray_isDefensiveCopy() throws MatrixArrayWrongSizeException {
		System.out.println("***** Test Matrix3 : setArray(float[][]) makes a defensive copy (aliasing bug fix) *****");

		Matrix3 m = new Matrix3(0f);
		float[][] source = new float[][] {
			{1f, 2f, 3f},
			{4f, 5f, 6f},
			{7f, 8f, 9f}
		};
		m.setArray(source);

		source[0][0] = 999f;
		assertEquals(1f, m.get(0,0), 0f);
	}

	@Test
	public void testMatrix3_determinant() {
		System.out.println("***** Test Matrix3 : determinant() (new method) *****");

		assertEquals(1f, Matrix3.IDENTITY.determinant(), 0.00001f);

		Matrix3 m = new Matrix3(new float[][] {
			{2f, 0f, 0f},
			{0f, 3f, 0f},
			{0f, 0f, 4f}
		});
		assertEquals(24f, m.determinant(), 0.00001f); // diagonal matrix: det = product of diagonal

		// A known singular matrix (third row = row1+row2) must have determinant 0
		Matrix3 singular = new Matrix3(new float[][] {
			{1f, 2f, 3f},
			{4f, 5f, 6f},
			{5f, 7f, 9f}
		});
		assertEquals(0f, singular.determinant(), 0.00001f);
	}
}
