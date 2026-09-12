package com.aventura.math.vector;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestMatrix4 {

	@Test
	public void testMatrix4_0() {
		System.out.println("***** Test Matrix4 : testMatrix4_0 *****");

		Matrix4 A = new Matrix4();
		Matrix4 B = new Matrix4(0);
		Matrix4 C = new Matrix4(7);
		System.out.println("A="+A);
		System.out.println("B="+B);
		System.out.println("C="+C);

		if (!A.equals(B)) fail("A does not equals B");
	}

	@Test
	public void testMatrix4_array_0() {
		System.out.println("***** Test Matrix4 : testMatrix4_array() *****");

		float[][] array = new float[4][4];

		for (int i=0; i<4; i++) {
			for (int j=0; j<4; j++) {
				array[i][j] = 0;
			}
		}

		Matrix4 A;
		Matrix4 B;
		A = new Matrix4(array);
		B = new Matrix4();
		System.out.println("A="+A);
		System.out.println("B="+B);

		if (!A.equals(B)) fail("A does not equals B");

	}

	@Test
	public void testMatrix4_array_value() {
		System.out.println("***** Test Matrix4 : testMatrix4_array_value() *****");

		float[][] array = new float[4][4];

		for (int i=0; i<4; i++) {
			for (int j=0; j<4; j++) {
				array[i][j] = 5;
			}
		}
		array[1][2]=22.3f;

		Matrix4 A;
		Matrix4 B;
//		try {
			A = new Matrix4(array);
			B = new Matrix4(5);
			B.set(1,2,22.3f);
			System.out.println("A="+A);
			System.out.println("B="+B);

			if (!A.equals(B)) fail("A does not equals B");
//		} catch (IndexOutOfBoundException e) {
//			fail("Index out of bound");		
//		}
	}

	@Test
	public void testMatrix4_plus() {
		System.out.println("***** Test Matrix4 : testMatrix4_plus() *****");

		float[][] array = new float[4][4];

		for (int i=0; i<4; i++) {
			for (int j=0; j<4; j++) {
				array[i][j] = i+j;
			}
		}

		Matrix4 A;
		Matrix4 B;
		Matrix4 C;
		A = new Matrix4(array);
		B = new Matrix4(5);
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
	public void testMatrix4_minus() {
		System.out.println("***** Test Matrix4 : testMatrix4_minus() *****");

		float[][] array = new float[4][4];

		for (int i=0; i<4; i++) {
			for (int j=0; j<4; j++) {
				array[i][j] = i+j;
			}
		}

		Matrix4 A;
		Matrix4 B;
		Matrix4 C;
		A = new Matrix4(array);
		B = new Matrix4(2);
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
	public void testMatrix4_plusEquals() {
		System.out.println("***** Test Matrix4 : testMatrix4_plusEquals() *****");

		float[][] array1 = new float[4][4];
		float[][] array2 = new float[4][4];

		for (int i=0; i<4; i++) {
			for (int j=0; j<4; j++) {
				array1[i][j] = i+j;
				array2[i][j] = i-j +7;
			}
		}

		/* 
		 * A=[[0.0, 1.0, 2.0, 3.0]
		 *    [1.0, 2.0, 3.0, 4.0]
		 *    [2.0, 3.0, 4.0, 5.0]
		 *    [3.0, 4.0, 5.0, 6.0]]
		 *    
		 * B=[[7.0, 6.0, 5.0, 4.0]
		 *    [8.0, 7.0, 6.0, 5.0]
		 *    [9.0, 8.0, 7.0, 6.0]
		 *    [10.0, 9.0, 9.0, 7.0]]
		 */ 

		Matrix4 A;
		Matrix4 B;
		A = new Matrix4(array1);
		B = new Matrix4(array2);
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
	public void testMatrix4_minusEquals() {
		System.out.println("***** Test Matrix4 : testMatrix4_minusEquals() *****");

		float[][] array1 = new float[4][4];
		float[][] array2 = new float[4][4];

		for (int i=0; i<4; i++) {
			for (int j=0; j<4; j++) {
				array1[i][j] = i+j;
				array2[i][j] = i-j +7;
			}
		}

		/* 
		 * A=[[0.0, 1.0, 2.0, 3.0]
		 *    [1.0, 2.0, 3.0, 4.0]
		 *    [2.0, 3.0, 4.0, 5.0]
		 *    [3.0, 4.0, 5.0, 6.0]]
		 *    
		 * B=[[7.0, 6.0, 5.0, 4.0]
		 *    [8.0, 7.0, 6.0, 5.0]
		 *    [9.0, 8.0, 7.0, 6.0]
		 *    [10.0, 9.0, 9.0, 7.0]]
		 */ 

		Matrix4 A;
		Matrix4 B;
		A = new Matrix4(array1);
		B = new Matrix4(array2);
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
	public void testMatrix4_times() {
		System.out.println("***** Test Matrix4 : testMatrix4_times() *****");

		float[][] array1 = new float[4][4];
		float[][] array2 = new float[4][4];

		for (int i=0; i<4; i++) {
			for (int j=0; j<4; j++) {
				array1[i][j] = i+j;
				array2[i][j] = i-j +7;
			}
		}

		/* 
		 * A=[[0.0, 1.0, 2.0, 3.0]
		 *    [1.0, 2.0, 3.0, 4.0]
		 *    [2.0, 3.0, 4.0, 5.0]
		 *    [3.0, 4.0, 5.0, 6.0]]
		 *    
		 * B=[[7.0, 6.0, 5.0, 4.0]
		 *    [8.0, 7.0, 6.0, 5.0]
		 *    [9.0, 8.0, 7.0, 6.0]
		 *    [10.0, 9.0, 9.0, 7.0]]
		 */ 

		Matrix4 A;
		Matrix4 B;
		A = new Matrix4(array1);
		B = new Matrix4(array2);
		System.out.println("A="+A);
		System.out.println("B="+B);
		Matrix4 C = A.times(B);
		System.out.println("C="+C);

		if (!(C.get(0,0) == 56.0 && C.get(0,1) == 50.0 && C.get(0,2) == 44.0 && C.get(0,3) == 38.0)) fail("C does not equals A^B");
		if (!(C.get(1,0) == 90.0 && C.get(1,1) == 80.0 && C.get(1,2) == 70.0 && C.get(1,3) == 60.0)) fail("C does not equals A^B");
		if (!(C.get(2,0) == 124.0 && C.get(2,1) == 110.0 && C.get(2,2) == 96.0 && C.get(2,3) == 82.0)) fail("C does not equals A^B");
		if (!(C.get(3,0) == 158.0 && C.get(3,1) == 140.0 && C.get(3,2) == 122.0 && C.get(3,3) == 104.0)) fail("C does not equals A^B");
	}
	@Test
	public void testMatrix4_transpose1() {
		System.out.println("***** Test Matrix4 : testMatrix4_transpose1() *****");

		float[][] array = new float[4][4];

		for (int i=0; i<4; i++) {
			for (int j=0; j<4; j++) {
				array[i][j] = i-j+3;
			}
		}

		/* 
		 * A=[[3.0, 2.0, 1.0, 0.0]
		 *    [4.0, 3.0, 2.0, 1.0]
		 *    [5.0, 4.0, 3.0, 2.0]
		 *    [6.0, 5.0, 4.0, 3.0]]
		 */ 

		Matrix4 A;

		A = new Matrix4(array);
		System.out.println("A="+A);
		Matrix4 B = A.transpose();
		System.out.println("B="+B);
		Matrix4 C = B.transpose();
		if (!A.equals(C)) fail("C =transpose(transpose(A)) does not equals A");
	}

	@Test
	public void testMatrix4_transpose2() {
		System.out.println("***** Test Matrix4 : testMatrix4_transpose2() *****");

		float[][] array = new float[4][4];

		for (int i=0; i<4; i++) {
			for (int j=0; j<4; j++) {
				array[i][j] = i-j+2;
			}
		}

		/* 
		 * A=[[3.0, 2.0, 1.0, 0.0]
		 *    [4.0, 3.0, 2.0, 1.0]
		 *    [5.0, 4.0, 3.0, 2.0]
		 *    [6.0, 5.0, 4.0, 3.0]]
		 */ 

		Matrix4 A;

		A = new Matrix4(array);
		System.out.println("A="+A);
		Matrix4 B = new Matrix4(A); // Keep image of A before transposition
		A.transposeEquals();
		System.out.println("A transposed ="+A);
		Matrix4 C = A.transpose(); // Do not modify A for this transposition
		if (!B.equals(C)) fail("transpose(transpose(A)) does not equals A");

	}
	
	@Test
	public void testMatrix4_inverse1() {
		System.out.println("***** Test Matrix4 : testMatrix4_inverse1() *****");

		float[][] array = new float[4][4];

		for (int i=0; i<4; i++) {
			for (int j=0; j<4; j++) {
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

		Matrix4 A, B, C;

		A = new Matrix4(array);
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
	public void testMatrix4_inverse2() {
		System.out.println("***** Test Matrix4 : testMatrix4_inverse2() *****");

		float[][] array = new float[4][4];

		for (int i=0; i<4; i++) {
			for (int j=0; j<4; j++) {
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

		Matrix4 A;

		A = new Matrix4(array);
		System.out.println("A="+A);
		try {
			Matrix4 B = A.inverse(); // Calculate inverse
			System.out.println("B="+B);
			Matrix4 C = B.times(A); // inverse(A).A = I
			System.out.println("C ="+C);
			if (!C.equals(Matrix4.IDENTITY)) fail("A.inverse(A) does not equals I");
		} catch (NotInvertibleMatrixException e) {
			fail("Not invertible Matrix");
		}

	}

	@Test
	public void testMatrix4_inverse3() {
		System.out.println("***** Test Matrix4 : testMatrix4_inverse3() *****");

		/* 
		 * A=[[1.0, 0.0, 0.0, 0.0]
		 *    [0.0, 1.0, 0.0, 0.0]
		 *    [0.0, 0.0, 1.0, 0.0]
		 *    [0.0, 0.0, 0.0, 1.0]]
		 */ 

		Matrix4 A;

		A = new Matrix4(Matrix4.IDENTITY);		
		System.out.println("A="+A);
		
		try {
			Matrix4 B = A.inverse(); // Calculate inverse
			System.out.println("B="+B);
			if (!B.equals(Matrix4.IDENTITY)) fail("inverse of Identity does not equals I");
		} catch (NotInvertibleMatrixException e) {
			fail("Not invertible Matrix");
		}

	}

	// ----- Additional tests added to blindage the coverage (bounds regressions, new methods, singular matrix) -----

	@Test
	public void testMatrix4_setDiagonal_regression_lastElement() {
		System.out.println("***** Test Matrix4 : setDiagonal must set (3,3) too (regression) *****");

		Matrix4 m = new Matrix4(0f);
		m.setDiagonal(7f);
		assertEquals(7f, m.get(0,0), 0f);
		assertEquals(7f, m.get(1,1), 0f);
		assertEquals(7f, m.get(2,2), 0f);
		assertEquals(7f, m.get(3,3), 0f); // this element was left at 0 before the fix
	}

	@Test(expected = IndexOutOfBoundException.class)
	public void testMatrix4_getRow_invalidIndex_throws() throws IndexOutOfBoundException {
		System.out.println("***** Test Matrix4 : getRow(4) out of bound must throw (off-by-one regression) *****");

		Matrix4 m = new Matrix4(Matrix4.IDENTITY);
		m.getRow(4); // valid indices are 0..3
	}

	@Test(expected = IndexOutOfBoundException.class)
	public void testMatrix4_getColumn_invalidIndex_throws() throws IndexOutOfBoundException {
		System.out.println("***** Test Matrix4 : getColumn(4) out of bound must throw (off-by-one regression) *****");

		Matrix4 m = new Matrix4(Matrix4.IDENTITY);
		m.getColumn(4); // valid indices are 0..3
	}

	@Test(expected = IndexOutOfBoundException.class)
	public void testMatrix4_timesRow_invalidIndex_throws() throws IndexOutOfBoundException {
		System.out.println("***** Test Matrix4 : timesRow(4, s) out of bound must throw (off-by-one regression) *****");

		Matrix4 m = new Matrix4(Matrix4.IDENTITY);
		m.timesRow(4, 2f);
	}

	@Test(expected = IndexOutOfBoundException.class)
	public void testMatrix4_setRow_invalidIndex_throws() throws IndexOutOfBoundException {
		System.out.println("***** Test Matrix4 : setRow(4, v) out of bound must throw (was previously never validated) *****");

		Matrix4 m = new Matrix4(0f);
		m.setRow(4, Vector4.ZERO_VECTOR);
	}

	@Test(expected = IndexOutOfBoundException.class)
	public void testMatrix4_setColumn_invalidIndex_throws() throws IndexOutOfBoundException {
		System.out.println("***** Test Matrix4 : setColumn(4, v) out of bound must throw (was previously never validated) *****");

		Matrix4 m = new Matrix4(0f);
		m.setColumn(4, Vector4.ZERO_VECTOR);
	}

	@Test
	public void testMatrix4_setRow_setColumn_roundTrip() throws IndexOutOfBoundException {
		System.out.println("***** Test Matrix4 : setRow/setColumn round-trip with getRow/getColumn *****");

		Matrix4 m = new Matrix4(0f);
		m.setRow(2, new Vector4(1f, 2f, 3f, 4f));
		Vector4 row2 = m.getRow(2);
		assertEquals(1f, row2.getX(), 0f);
		assertEquals(4f, row2.getW(), 0f);

		m.setColumn(3, new Vector4(5f, 6f, 7f, 8f));
		Vector4 col3 = m.getColumn(3);
		assertEquals(5f, col3.getX(), 0f);
		assertEquals(8f, col3.getW(), 0f);
	}

	@Test
	public void testMatrix4_getMatrix3_subMatrix() {
		System.out.println("***** Test Matrix4 : getMatrix3() extracts the top-left 3x3 sub-matrix *****");

		Matrix4 m = new Matrix4(new float[][] {
			{1f, 2f, 3f, 99f},
			{4f, 5f, 6f, 99f},
			{7f, 8f, 9f, 99f},
			{0f, 0f, 0f, 1f}
		});
		Matrix3 sub = m.getMatrix3();
		assertEquals(1f, sub.get(0,0), 0f);
		assertEquals(5f, sub.get(1,1), 0f);
		assertEquals(9f, sub.get(2,2), 0f);
		assertEquals(8f, sub.get(2,1), 0f);
	}

	@Test
	public void testMatrix4_trace() {
		System.out.println("***** Test Matrix4 : trace() (new method) *****");

		assertEquals(4f, Matrix4.IDENTITY.trace(), 0.00001f);

		Matrix4 m = new Matrix4(new float[][] {
			{2f,0f,0f,0f},
			{0f,3f,0f,0f},
			{0f,0f,4f,0f},
			{0f,0f,0f,5f}
		});
		assertEquals(14f, m.trace(), 0.00001f);
	}

	@Test
	public void testMatrix4_isIdentity() {
		System.out.println("***** Test Matrix4 : isIdentity() (new method) *****");

		assertTrue(Matrix4.IDENTITY.isIdentity());
		assertTrue(new Matrix4(Matrix4.IDENTITY).isIdentity());
		assertFalse(new Matrix4(0f).isIdentity());
	}

	@Test
	public void testMatrix4_equals_negativeCase() {
		System.out.println("***** Test Matrix4 : equals negative case *****");

		Matrix4 a = new Matrix4(1f);
		Matrix4 b = new Matrix4(2f);
		if (a.equals(b)) fail("a should not equal b");
	}

	@Test
	public void testMatrix4_times_isNotCommutative() {
		System.out.println("***** Test Matrix4 : A*B != B*A in general *****");

		Matrix4 a = new Matrix4(new float[][] {
			{1f,2f,0f,0f},
			{0f,1f,0f,0f},
			{0f,0f,1f,0f},
			{0f,0f,0f,1f}
		});
		Matrix4 b = new Matrix4(new float[][] {
			{1f,0f,0f,0f},
			{3f,1f,0f,0f},
			{0f,0f,1f,0f},
			{0f,0f,0f,1f}
		});

		Matrix4 ab = a.times(b);
		Matrix4 ba = b.times(a);
		if (ab.equals(ba)) fail("A*B should not equal B*A for these matrices");
	}

	@Test(expected = NotInvertibleMatrixException.class)
	public void testMatrix4_inverse_singularMatrix_throws() throws NotInvertibleMatrixException {
		System.out.println("***** Test Matrix4 : inverse() of a singular matrix must throw NotInvertibleMatrixException *****");

		// Last row is entirely 0: this matrix is singular (determinant 0).
		Matrix4 singular = new Matrix4(new float[][] {
			{1f, 2f, 3f, 4f},
			{5f, 6f, 7f, 8f},
			{9f, 10f, 11f, 12f},
			{0f, 0f, 0f, 0f}
		});
		singular.inverse();
	}

	@Test
	public void testMatrix4_inverse_pivotSelection_regression() {
		System.out.println("***** Test Matrix4 : partial pivoting must pick the true max-abs row (pivot bug regression) *****");

		Matrix4 m = new Matrix4(new float[][] {
			{2f, 1f, 1f, 1f},
			{5f, 1f, 1f, 1f},
			{1f, 1f, 1f, 1f},
			{1f, 1f, 1f, 1f}
		});
		int chosen = Matrix4.indiceOfMaxRowInColumn(m, 0, 0);
		assertEquals(1, chosen);

		Matrix4 m2 = new Matrix4(new float[][] {
			{9f, 1f, 1f, 1f},
			{5f, 1f, 1f, 1f},
			{1f, 1f, 1f, 1f},
			{1f, 1f, 1f, 1f}
		});
		int chosen2 = Matrix4.indiceOfMaxRowInColumn(m2, 0, 0);
		assertEquals(0, chosen2);
	}

	@Test
	public void testMatrix4_inverse_precision_generalCase() throws NotInvertibleMatrixException {
		System.out.println("***** Test Matrix4 : A * inverse(A) == Identity on a general (non-trivial-pivot) matrix *****");

		Matrix4 a = new Matrix4(new float[][] {
			{4f, 7f, 2f, 1f},
			{3f, 5f, 1f, 2f},
			{2f, 3f, 1f, 0f},
			{1f, 0f, 2f, 3f}
		});
		Matrix4 invA = a.inverse();
		Matrix4 product = a.times(invA);
		if (!product.equals(Matrix4.IDENTITY)) fail("A * inverse(A) should equal Identity");
	}

	@Test
	public void testMatrix4_constructor_array_isDefensiveCopy() {
		System.out.println("***** Test Matrix4 : constructor(float[][]) makes a defensive copy (aliasing bug fix) *****");

		float[][] source = new float[][] {
			{1f, 0f, 0f, 0f},
			{0f, 1f, 0f, 0f},
			{0f, 0f, 1f, 0f},
			{0f, 0f, 0f, 1f}
		};
		Matrix4 m = new Matrix4(source);

		source[0][0] = 999f;
		assertEquals(1f, m.get(0,0), 0f);
	}

	@Test
	public void testMatrix4_setArray_isDefensiveCopy() throws MatrixArrayWrongSizeException {
		System.out.println("***** Test Matrix4 : setArray(float[][]) makes a defensive copy (aliasing bug fix) *****");

		Matrix4 m = new Matrix4(0f);
		float[][] source = new float[][] {
			{1f, 2f, 3f, 4f},
			{5f, 6f, 7f, 8f},
			{9f, 10f, 11f, 12f},
			{13f, 14f, 15f, 16f}
		};
		m.setArray(source);

		source[0][0] = 999f;
		assertEquals(1f, m.get(0,0), 0f);
	}

	@Test
	public void testMatrix4_getArray_isDefensiveCopy() {
		System.out.println("***** Test Matrix4 : getArray() returns a defensive copy (aliasing bug fix, aligned with Matrix3) *****");

		Matrix4 m = new Matrix4(Matrix4.IDENTITY);
		float[][] arr = m.getArray();
		arr[0][0] = 999f;

		assertEquals(1f, m.get(0,0), 0f);
	}

	@Test
	public void testMatrix4_determinant() {
		System.out.println("***** Test Matrix4 : determinant() (new method) *****");

		assertEquals(1f, Matrix4.IDENTITY.determinant(), 0.00001f);

		Matrix4 m = new Matrix4(new float[][] {
			{2f, 0f, 0f, 0f},
			{0f, 3f, 0f, 0f},
			{0f, 0f, 4f, 0f},
			{0f, 0f, 0f, 5f}
		});
		assertEquals(120f, m.determinant(), 0.0001f); // diagonal matrix: det = product of diagonal

		// Last row all zero -> singular -> determinant 0
		Matrix4 singular = new Matrix4(new float[][] {
			{1f, 2f, 3f, 4f},
			{5f, 6f, 7f, 8f},
			{9f, 10f, 11f, 12f},
			{0f, 0f, 0f, 0f}
		});
		assertEquals(0f, singular.determinant(), 0.0001f);
	}

	@Test
	public void testMatrix4_setArrayOfGetArray_realWorldPattern() throws MatrixArrayWrongSizeException {
		System.out.println("***** Test Matrix4 : A.setArray(B.times(C).getArray()) still works correctly *****");

		// This mirrors the exact pattern used in LookAt.java (real codebase): combine two matrices,
		// then adopt the result's array into a third Matrix via getArray()/setArray(). With both methods
		// now doing a defensive copy, the values must still end up correct (just with one harmless
		// extra copy instead of a shared reference).
		Matrix4 b = new Matrix4(new float[][] {
			{1f,0f,0f,2f},
			{0f,1f,0f,3f},
			{0f,0f,1f,4f},
			{0f,0f,0f,1f}
		});
		Matrix4 c = new Matrix4(Matrix4.IDENTITY);

		Matrix4 a = new Matrix4(0f);
		a.setArray(b.times(c).getArray());

		if (!a.equals(b)) fail("a should equal b*Identity after the getArray()/setArray() round-trip");
	}

}
