package com.aventura.math.vector;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Tests for Matrix2, the 2x2 counterpart to Matrix3/Matrix4 (see audit report - Matrix2 was deferred
 * to its own phase). Mirrors TestMatrix3's coverage and style at 2x2 scale, plus a direct test of
 * times(Vector2) (Matrix3/Matrix4 have this cross-validated in a separate TestVector3Matrix3-style
 * class; Matrix2 has no such sibling yet, so that coverage lives here instead).
 */
public class TestMatrix2 {

	@Test
	public void testMatrix2_0() {
		System.out.println("***** Test Matrix2 : testMatrix2_0 *****");

		Matrix2 A = new Matrix2();
		Matrix2 B = new Matrix2(0);
		Matrix2 C = new Matrix2(7);
		System.out.println("A="+A);
		System.out.println("B="+B);
		System.out.println("C="+C);

		if (!A.equals(B)) fail("A does not equals B");
	}

	@Test
	public void testMatrix2_array_0() {
		System.out.println("***** Test Matrix2 : testMatrix2_array_0() *****");

		float[][] array = new float[2][2];

		for (int i=0; i<2; i++) {
			for (int j=0; j<2; j++) {
				array[i][j] = 0;
			}
		}

		Matrix2 A;
		Matrix2 B;

		A = new Matrix2(array);
		B = new Matrix2();
		System.out.println("A="+A);
		System.out.println("B="+B);

		if (!A.equals(B)) fail("A does not equals B");
	}

	@Test
	public void testMatrix2_array_value() {
		System.out.println("***** Test Matrix2 : testMatrix2_array_value() *****");

		float[][] array = new float[2][2];

		for (int i=0; i<2; i++) {
			for (int j=0; j<2; j++) {
				array[i][j] = 5;
			}
		}
		array[1][0] = 22.3f;

		Matrix2 A;
		Matrix2 B;

		A = new Matrix2(array);
		B = new Matrix2(5);
		B.set(1,0,22.3f);
		System.out.println("A="+A);
		System.out.println("B="+B);

		if (!A.equals(B)) fail("A does not equals B");
	}

	@Test
	public void testMatrix2_plus() {
		System.out.println("***** Test Matrix2 : testMatrix2_plus() *****");

		float[][] array = new float[2][2];

		for (int i=0; i<2; i++) {
			for (int j=0; j<2; j++) {
				array[i][j] = i+j;
			}
		}

		Matrix2 A;
		Matrix2 B;
		Matrix2 C;

		A = new Matrix2(array);
		B = new Matrix2(5);
		C = A.plus(B);
		System.out.println("A="+A);
		System.out.println("B="+B);
		System.out.println("C="+C);

		for (int i=0; i<2; i++) {
			for (int j=0; j<2; j++) {
				if (C.get(i,j) != i+j+5) fail("C does not equals A+B");
			}
		}
	}

	@Test
	public void testMatrix2_minus() {
		System.out.println("***** Test Matrix2 : testMatrix2_minus() *****");

		float[][] array = new float[2][2];

		for (int i=0; i<2; i++) {
			for (int j=0; j<2; j++) {
				array[i][j] = i+j;
			}
		}

		Matrix2 A;
		Matrix2 B;
		Matrix2 C;

		A = new Matrix2(array);
		B = new Matrix2(2);
		C = A.minus(B);

		System.out.println("A="+A);
		System.out.println("B="+B);
		System.out.println("C="+C);

		for (int i=0; i<2; i++) {
			for (int j=0; j<2; j++) {
				if (C.get(i,j) != i+j-2) fail("C does not equals A-B");
			}
		}
	}

	@Test
	public void testMatrix2_plusEquals() {
		System.out.println("***** Test Matrix2 : testMatrix2_plusEquals() *****");

		/*
		 * A=[[0.0, 1.0]
		 *    [1.0, 2.0]]
		 *
		 * B=[[7.0, 6.0]
		 *    [8.0, 7.0]]
		 */
		float[][] array1 = { {0f, 1f}, {1f, 2f} };
		float[][] array2 = { {7f, 6f}, {8f, 7f} };

		Matrix2 A = new Matrix2(array1);
		Matrix2 B = new Matrix2(array2);
		System.out.println("A="+A);
		System.out.println("B="+B);
		A.plusEquals(B);
		System.out.println("A="+A);
		System.out.println("B="+B);

		if (!(A.get(0,0) ==  7.0 && A.get(0,1) ==  7.0)) fail("A does not equals A+B");
		if (!(A.get(1,0) ==  9.0 && A.get(1,1) ==  9.0)) fail("A does not equals A+B");
	}

	@Test
	public void testMatrix2_minusEquals() {
		System.out.println("***** Test Matrix2 : testMatrix2_minusEquals() *****");

		float[][] array1 = { {0f, 1f}, {1f, 2f} };
		float[][] array2 = { {7f, 6f}, {8f, 7f} };

		Matrix2 A = new Matrix2(array1);
		Matrix2 B = new Matrix2(array2);
		System.out.println("A="+A);
		System.out.println("B="+B);
		A.minusEquals(B);
		System.out.println("A="+A);
		System.out.println("B="+B);

		if (!(A.get(0,0) == -7.0 && A.get(0,1) == -5.0)) fail("A does not equals A-B");
		if (!(A.get(1,0) == -7.0 && A.get(1,1) == -5.0)) fail("A does not equals A-B");
	}

	@Test
	public void testMatrix2_times() {
		System.out.println("***** Test Matrix2 : testMatrix2_times() *****");

		/*
		 * A=[[0.0, 1.0]
		 *    [1.0, 2.0]]
		 *
		 * B=[[7.0, 6.0]
		 *    [8.0, 7.0]]
		 */
		float[][] array1 = { {0f, 1f}, {1f, 2f} };
		float[][] array2 = { {7f, 6f}, {8f, 7f} };

		Matrix2 A = new Matrix2(array1);
		Matrix2 B = new Matrix2(array2);
		System.out.println("A="+A);
		System.out.println("B="+B);
		Matrix2 C = A.times(B);
		System.out.println("C="+C);

		// Row0 = [0,1]: [0*7+1*8, 0*6+1*7] = [8, 7]
		// Row1 = [1,2]: [1*7+2*8, 1*6+2*7] = [23, 20]
		if (!(C.get(0,0) == 8.0 && C.get(0,1) == 7.0)) fail("C does not equals A^B");
		if (!(C.get(1,0) == 23.0 && C.get(1,1) == 20.0)) fail("C does not equals A^B");
	}

	@Test
	public void testMatrix2_timesEquals() {
		System.out.println("***** Test Matrix2 : testMatrix2_timesEquals() *****");

		float[][] array1 = { {0f, 1f}, {1f, 2f} };
		float[][] array2 = { {7f, 6f}, {8f, 7f} };

		Matrix2 A = new Matrix2(array1);
		Matrix2 B = new Matrix2(array2);
		Matrix2 expected = A.times(B);
		A.timesEquals(B);

		if (!A.equals(expected)) fail("A.timesEquals(B) does not match A.times(B)");
	}

	@Test
	public void testMatrix2_times_scalar() {
		System.out.println("***** Test Matrix2 : times(float)/timesEquals(float) (scalar) *****");

		Matrix2 A = new Matrix2(new float[][] { {1f, 2f}, {3f, 4f} });
		Matrix2 B = A.times(2f);
		assertEquals(2f, B.get(0,0), 0f);
		assertEquals(4f, B.get(0,1), 0f);
		assertEquals(6f, B.get(1,0), 0f);
		assertEquals(8f, B.get(1,1), 0f);

		A.timesEquals(2f);
		if (!A.equals(B)) fail("A.timesEquals(2f) does not match A.times(2f)");
	}

	@Test
	public void testMatrix2_transpose1() {
		System.out.println("***** Test Matrix2 : testMatrix2_transpose1() *****");

		Matrix2 A = new Matrix2(new float[][] { {2f, 1f}, {3f, 2f} });
		System.out.println("A="+A);
		Matrix2 B = A.transpose();
		System.out.println("B="+B);
		Matrix2 C = B.transpose();
		if (!A.equals(C)) fail("C = transpose(transpose(A)) does not equals A");
	}

	@Test
	public void testMatrix2_transpose2() {
		System.out.println("***** Test Matrix2 : testMatrix2_transpose2() *****");

		Matrix2 A = new Matrix2(new float[][] { {2f, 1f}, {3f, 2f} });
		Matrix2 B = new Matrix2(A); // Keep image of A before transposition
		A.transposeEquals();
		System.out.println("A transposed ="+A);
		Matrix2 C = A.transpose(); // Do not modify A for this transposition
		if (!B.equals(C)) fail("transpose(transpose(A)) does not equals A");
	}

	@Test
	public void testMatrix2_inverse1() {
		System.out.println("***** Test Matrix2 : testMatrix2_inverse1() *****");

		Matrix2 A = new Matrix2(new float[][] { {10f, 9f}, {0f, 7f} });
		Matrix2 B = null;

		System.out.println("A="+A);
		try {
			B = A.inverse();
			System.out.println("B="+B);
		} catch (NotInvertibleMatrixException e) {
			fail("Not invertible Matrix A");
		}
		try {
			Matrix2 C = B.inverse();
			System.out.println("C="+C);
			if (!A.equals(C)) fail("inverse(inverse(A)) does not equals A");
		} catch (NotInvertibleMatrixException e) {
			fail("Not invertible Matrix B");
		}
	}

	@Test
	public void testMatrix2_inverse2() {
		System.out.println("***** Test Matrix2 : testMatrix2_inverse2() *****");

		Matrix2 A = new Matrix2(new float[][] { {10f, 9f}, {0f, 7f} });
		System.out.println("A="+A);
		try {
			Matrix2 B = A.inverse();
			System.out.println("B="+B);
			Matrix2 C = B.times(A); // inverse(A).A = I
			System.out.println("C="+C);
			if (!C.equals(Matrix2.identity())) fail("A.inverse(A) does not equals I");
		} catch (NotInvertibleMatrixException e) {
			fail("Not invertible Matrix");
		}
	}

	@Test
	public void testMatrix2_inverse3() {
		System.out.println("***** Test Matrix2 : testMatrix2_inverse3() *****");

		Matrix2 A = new Matrix2(Matrix2.identity());
		System.out.println("A="+A);

		try {
			Matrix2 B = A.inverse();
			System.out.println("B="+B);
			if (!B.equals(Matrix2.identity())) fail("inverse of Identity does not equals I");
		} catch (NotInvertibleMatrixException e) {
			fail("Not invertible Matrix");
		}
	}

	@Test(expected = NotInvertibleMatrixException.class)
	public void testMatrix2_inverse_singularMatrix_throws() throws NotInvertibleMatrixException {
		System.out.println("***** Test Matrix2 : inverse() of a singular matrix must throw NotInvertibleMatrixException *****");

		// Second row is 2x the first row: this matrix is singular (determinant 0).
		Matrix2 singular = new Matrix2(new float[][] { {1f, 2f}, {2f, 4f} });
		singular.inverse();
	}

	@Test
	public void testMatrix2_inverse_precision_generalCase() throws NotInvertibleMatrixException {
		System.out.println("***** Test Matrix2 : A * inverse(A) == Identity on a general matrix *****");

		Matrix2 a = new Matrix2(new float[][] { {4f, 7f}, {2f, 6f} });
		Matrix2 invA = a.inverse();
		Matrix2 product = a.times(invA);
		if (!product.equals(Matrix2.identity())) fail("A * inverse(A) should equal Identity");
	}

	@Test(expected = IndexOutOfBoundException.class)
	public void testMatrix2_getRow_invalidIndex_throws() throws IndexOutOfBoundException {
		System.out.println("***** Test Matrix2 : getRow(2) out of bound must throw *****");

		Matrix2 m = new Matrix2(Matrix2.identity());
		m.getRow(2); // valid indices are 0..1
	}

	@Test(expected = IndexOutOfBoundException.class)
	public void testMatrix2_getColumn_invalidIndex_throws() throws IndexOutOfBoundException {
		System.out.println("***** Test Matrix2 : getColumn(2) out of bound must throw *****");

		Matrix2 m = new Matrix2(Matrix2.identity());
		m.getColumn(2); // valid indices are 0..1
	}

	@Test(expected = IndexOutOfBoundException.class)
	public void testMatrix2_timesRow_invalidIndex_throws() throws IndexOutOfBoundException {
		System.out.println("***** Test Matrix2 : timesRow(2, s) out of bound must throw *****");

		Matrix2 m = new Matrix2(Matrix2.identity());
		m.timesRow(2, 2f);
	}

	@Test
	public void testMatrix2_setRow_setColumn() throws IndexOutOfBoundException {
		System.out.println("***** Test Matrix2 : setRow/setColumn *****");

		Matrix2 m = new Matrix2(0f);
		m.setRow(1, new Vector2(1f, 2f));
		assertEquals(1f, m.get(1,0), 0f);
		assertEquals(2f, m.get(1,1), 0f);

		m.setColumn(1, new Vector2(7f, 8f));
		assertEquals(7f, m.get(0,1), 0f);
		assertEquals(8f, m.get(1,1), 0f);

		// round-trip: what we just set via setRow/setColumn must be readable via getRow/getColumn
		Vector2 col1 = m.getColumn(1);
		assertEquals(7f, col1.getX(), 0f);
		assertEquals(8f, col1.getY(), 0f);
	}

	@Test(expected = IndexOutOfBoundException.class)
	public void testMatrix2_setRow_invalidIndex_throws() throws IndexOutOfBoundException {
		System.out.println("***** Test Matrix2 : setRow(2, v) out of bound must throw *****");

		Matrix2 m = new Matrix2(0f);
		m.setRow(2, new Vector2(0f, 0f));
	}

	@Test(expected = IndexOutOfBoundException.class)
	public void testMatrix2_setColumn_invalidIndex_throws() throws IndexOutOfBoundException {
		System.out.println("***** Test Matrix2 : setColumn(2, v) out of bound must throw *****");

		Matrix2 m = new Matrix2(0f);
		m.setColumn(2, new Vector2(0f, 0f));
	}

	@Test
	public void testMatrix2_getArray_isDefensiveCopy() {
		System.out.println("***** Test Matrix2 : getArray() returns a defensive copy *****");

		Matrix2 m = new Matrix2(Matrix2.identity());
		float[][] arr = m.getArray();
		arr[0][0] = 999f; // mutate the returned array

		// The Matrix itself must be unaffected: getArray() must not leak the internal reference
		assertEquals(1f, m.get(0,0), 0f);
	}

	@Test
	public void testMatrix2_trace() {
		System.out.println("***** Test Matrix2 : trace() *****");

		assertEquals(2f, Matrix2.identity().trace(), 0.00001f);

		Matrix2 m = new Matrix2(new float[][] { {2f, 0f}, {0f, 5f} });
		assertEquals(7f, m.trace(), 0.00001f);
	}

	@Test
	public void testMatrix2_isIdentity() {
		System.out.println("***** Test Matrix2 : isIdentity() *****");

		assertTrue(Matrix2.identity().isIdentity());
		assertTrue(new Matrix2(Matrix2.identity()).isIdentity());
		assertFalse(new Matrix2(0f).isIdentity());
		assertFalse(new Matrix2(1f).isIdentity()); // all-ones is not the Identity
	}

	@Test
	public void testMatrix2_equals_negativeCase() {
		System.out.println("***** Test Matrix2 : equals negative case *****");

		Matrix2 a = new Matrix2(1f);
		Matrix2 b = new Matrix2(2f);
		if (a.equals(b)) fail("a should not equal b");
	}

	@Test
	public void testMatrix2_times_isNotCommutative() {
		System.out.println("***** Test Matrix2 : A*B != B*A in general *****");

		Matrix2 a = new Matrix2(new float[][] { {1f, 2f}, {0f, 1f} });
		Matrix2 b = new Matrix2(new float[][] { {1f, 0f}, {3f, 1f} });

		Matrix2 ab = a.times(b);
		Matrix2 ba = b.times(a);
		if (ab.equals(ba)) fail("A*B should not equal B*A for these matrices");
	}

	@Test
	public void testMatrix2_determinant() {
		System.out.println("***** Test Matrix2 : determinant() *****");

		assertEquals(1f, Matrix2.identity().determinant(), 0.00001f);

		Matrix2 m = new Matrix2(new float[][] { {2f, 0f}, {0f, 3f} });
		assertEquals(6f, m.determinant(), 0.00001f); // diagonal matrix: det = product of diagonal

		// A known singular matrix (row1 = 2x row0) must have determinant 0
		Matrix2 singular = new Matrix2(new float[][] { {1f, 2f}, {2f, 4f} });
		assertEquals(0f, singular.determinant(), 0.00001f);
	}

	@Test
	public void testMatrix2_equalsObject_and_hashCode() {
		System.out.println("***** Test Matrix2 : equals(Object) override + hashCode() *****");

		Matrix2 a = Matrix2.identity();
		Matrix2 b = Matrix2.identity();
		Matrix2 c = new Matrix2(0f);

		Object ob = b;
		Object oc = c;
		assertTrue(a.equals(ob));
		assertFalse(a.equals(oc));

		Object nullObj = null;
		assertFalse(a.equals(nullObj)); // must go through equals(Object), not the more specific equals(Matrix2)
		assertFalse(a.equals("not a Matrix2"));
		assertTrue(a.equals(a));

		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void testMatrix2_identity_isFreshIndependentCopyEachCall() {
		System.out.println("***** Test Matrix2 : identity() returns a fresh, independent copy every call *****");

		Matrix2 i1 = Matrix2.identity();
		Matrix2 i2 = Matrix2.identity();
		if (i1 == i2) fail("identity() should return a fresh instance every call, not a shared one");
		assertTrue(i1.equals(i2));

		// Mutating one call's result must never affect a later call's result.
		i1.set(0, 0, 999f);
		Matrix2 i3 = Matrix2.identity();
		assertEquals(1f, i3.get(0,0), 0f);
	}

	@Test
	public void testMatrix2_swapRows_timesRow_areProtected_notPublicApi() {
		System.out.println("***** Test Matrix2 : swapRows()/timesRow() visibility is protected *****");

		// Called from within the same package (this test class), which protected visibility still allows.
		Matrix2 m = new Matrix2(new float[][] { {1f, 2f}, {3f, 4f} });
		m.swapRows(0, 1);
		assertEquals(3f, m.get(0,0), 0f);
		assertEquals(1f, m.get(1,0), 0f);
	}

	@Test
	public void testMatrix2_constructor_array_isDefensiveCopy() {
		System.out.println("***** Test Matrix2 : constructor(float[][]) makes a defensive copy *****");

		float[][] source = new float[][] { {1f, 2f}, {3f, 4f} };
		Matrix2 m = new Matrix2(source);

		// Mutate the source array after construction: the Matrix must be unaffected.
		source[0][0] = 999f;
		assertEquals(1f, m.get(0,0), 0f);
	}

	@Test
	public void testMatrix2_setArray_isDefensiveCopy() throws MatrixArrayWrongSizeException {
		System.out.println("***** Test Matrix2 : setArray(float[][]) makes a defensive copy *****");

		Matrix2 m = new Matrix2(0f);
		float[][] source = new float[][] { {1f, 2f}, {3f, 4f} };
		m.setArray(source);

		source[0][0] = 999f;
		assertEquals(1f, m.get(0,0), 0f);
	}

	@Test(expected = MatrixArrayWrongSizeException.class)
	public void testMatrix2_setArray_wrongRowSize_throws() throws MatrixArrayWrongSizeException {
		System.out.println("***** Test Matrix2 : setArray() with wrong row size must throw *****");

		Matrix2 m = new Matrix2(0f);
		m.setArray(new float[][] { {1f, 2f}, {3f, 4f}, {5f, 6f} }); // 3 rows instead of 2
	}

	@Test(expected = MatrixArrayWrongSizeException.class)
	public void testMatrix2_setArray_wrongColumnSize_throws() throws MatrixArrayWrongSizeException {
		System.out.println("***** Test Matrix2 : setArray() with wrong column size must throw *****");

		Matrix2 m = new Matrix2(0f);
		m.setArray(new float[][] { {1f, 2f, 3f}, {4f, 5f, 6f} }); // 3 columns instead of 2
	}

	@Test
	public void testMatrix2_setDiagonal() {
		System.out.println("***** Test Matrix2 : setDiagonal() *****");

		Matrix2 m = new Matrix2(0f);
		m.setDiagonal(5f);
		assertEquals(5f, m.get(0,0), 0f);
		assertEquals(5f, m.get(1,1), 0f);
		assertEquals(0f, m.get(0,1), 0f);
		assertEquals(0f, m.get(1,0), 0f);
	}

	// ----- times(Vector2): Matrix2's own version of the Matrix3/TestVector3Matrix3 cross-check -----

	@Test
	public void testMatrix2_times_vector2_matchesManualComputation() {
		System.out.println("***** Test Matrix2 : times(Vector2) W=A.V matches manual computation *****");

		Matrix2 A = new Matrix2(new float[][] { {2f, 3f}, {4f, 5f} });
		Vector2 v = new Vector2(1f, 2f);

		Vector2 w = A.times(v);

		// W = A.V : w.x = 2*1+3*2 = 8 ; w.y = 4*1+5*2 = 14
		assertEquals(8f, w.getX(), 0.0001f);
		assertEquals(14f, w.getY(), 0.0001f);
	}

	@Test
	public void testMatrix2_times_vector2_identityIsNoOp() {
		System.out.println("***** Test Matrix2 : Identity.times(V) == V *****");

		Vector2 v = new Vector2(3.5f, -2.25f);
		Vector2 w = Matrix2.identity().times(v);

		assertTrue(w.equals(v));
	}

	@Test
	public void testMatrix2_times_vector2_matchesVector2TimesMatrix2() {
		System.out.println("***** Test Matrix2 : A.times(v) delegates to and matches v.times(A) *****");

		Matrix2 A = new Matrix2(new float[][] { {1f, 2f}, {3f, 4f} });
		Vector2 v = new Vector2(5f, 6f);

		Vector2 fromMatrix = A.times(v);
		Vector2 fromVector = v.times(A);

		assertTrue(fromMatrix.equals(fromVector));
	}

	@Test
	public void testMatrix2_timesEquals_vector2_matchesTimes() throws IndexOutOfBoundException {
		System.out.println("***** Test Matrix2 : Vector2.timesEquals(Matrix2) matches Vector2.times(Matrix2) *****");

		Matrix2 A = new Matrix2(new float[][] { {2f, 0f}, {0f, 3f} });
		Vector2 v = new Vector2(4f, 5f);

		Vector2 expected = v.times(A);
		v.timesEquals(A);

		assertTrue(v.equals(expected));
	}
}
