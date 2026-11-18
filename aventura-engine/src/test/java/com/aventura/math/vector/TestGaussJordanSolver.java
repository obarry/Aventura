package com.aventura.math.vector;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * New test class (phase 2 - see audit report, section 6). Covers GaussJordanSolver directly,
 * the internal, size-generic Gauss-Jordan solver shared by Matrix3.inverse() and Matrix4.inverse().
 * The pivot-selection regression tests here replace the ones that used to live in TestMatrix3/TestMatrix4
 * against the (now removed) per-class indiceOfMaxRowInColumn helpers.
 */
public class TestGaussJordanSolver {

	@Test
	public void testPivotSelection_picksTrueMaxAbsRow_regression() {
		System.out.println("***** Test GaussJordanSolver : partial pivoting must pick the true max-abs row (pivot bug regression) *****");

		// Column 0 has abs values [2, 5, 1] starting at pivot row 0: row 1 (value 5) must be selected,
		// not row 0 itself by default (this was the bug: the search never compared against the pivot row).
		float[][] m = {
			{2f, 1f, 1f},
			{5f, 1f, 1f},
			{1f, 1f, 1f}
		};
		int chosen = GaussJordanSolver.indiceOfMaxRowInColumn(m, 3, 0, 0);
		assertEquals(1, chosen);

		// And when the pivot row already holds the max value, it must be correctly kept (not overlooked).
		float[][] m2 = {
			{9f, 1f, 1f},
			{5f, 1f, 1f},
			{1f, 1f, 1f}
		};
		int chosen2 = GaussJordanSolver.indiceOfMaxRowInColumn(m2, 3, 0, 0);
		assertEquals(0, chosen2);
	}

	@Test
	public void testPivotSelection_searchStartsAtGivenPivot() {
		System.out.println("***** Test GaussJordanSolver : pivot search only considers rows from the given pivot onward *****");

		float[][] m = {
			{9f, 1f, 1f, 1f},
			{5f, 1f, 1f, 1f},
			{7f, 1f, 1f, 1f},
			{2f, 1f, 1f, 1f}
		};
		// Starting the search at pivot=2: only rows 2 and 3 are candidates (values 7 and 2), row 2 wins,
		// even though rows 0 and 1 (values 9 and 5) hold larger values overall.
		int chosen = GaussJordanSolver.indiceOfMaxRowInColumn(m, 4, 0, 2);
		assertEquals(2, chosen);
	}

	@Test
	public void testInvert_3x3_matchesIdentityRoundTrip() throws NotInvertibleMatrixException {
		System.out.println("***** Test GaussJordanSolver : invert() 3x3, A * inverse(A) == Identity *****");

		float[][] a = {
			{4f, 7f, 2f},
			{3f, 5f, 1f},
			{2f, 3f, 1f}
		};
		float[][] inv = GaussJordanSolver.invert(a, 3, 1.0E-4f);
		Matrix3 A = new Matrix3(a);
		Matrix3 invA = new Matrix3(inv);
		if (!A.times(invA).equals(Matrix3.identity())) fail("A * invert(A) should equal Identity (3x3)");
	}

	@Test
	public void testInvert_4x4_matchesIdentityRoundTrip() throws NotInvertibleMatrixException {
		System.out.println("***** Test GaussJordanSolver : invert() 4x4, A * inverse(A) == Identity *****");

		float[][] a = {
			{2f, 0f, 0f, 0f},
			{0f, 3f, 0f, 0f},
			{0f, 0f, 4f, 0f},
			{1f, 1f, 1f, 1f}
		};
		float[][] inv = GaussJordanSolver.invert(a, 4, 1.0E-4f);
		Matrix4 A = new Matrix4(a);
		Matrix4 invA = new Matrix4(inv);
		if (!A.times(invA).equals(Matrix4.identity())) fail("A * invert(A) should equal Identity (4x4)");
	}

	@Test(expected = NotInvertibleMatrixException.class)
	public void testInvert_singularMatrix_throws() throws NotInvertibleMatrixException {
		System.out.println("***** Test GaussJordanSolver : invert() on a singular matrix must throw *****");

		// Third row is a linear combination (sum) of the first two: singular (determinant 0).
		float[][] a = {
			{1f, 2f, 3f},
			{4f, 5f, 6f},
			{5f, 7f, 9f}
		};
		GaussJordanSolver.invert(a, 3, 1.0E-4f);
	}

	@Test
	public void testInvert_doesNotMutateInputArray() throws NotInvertibleMatrixException {
		System.out.println("***** Test GaussJordanSolver : invert() must not mutate the array passed in *****");

		float[][] a = {
			{4f, 7f},
			{2f, 6f}
		};
		float[][] original = {{4f, 7f}, {2f, 6f}};
		GaussJordanSolver.invert(a, 2, 1.0E-4f);
		assertArrayEquals(original[0], a[0], 0.00001f);
		assertArrayEquals(original[1], a[1], 0.00001f);
	}
}
