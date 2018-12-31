package com.aventura.math.vector;

import static org.junit.Assert.*;

import org.junit.Test;

import com.aventura.math.Constants;

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
			if (!A.equalsEpsilon(C, Constants.EPSILON)) fail("inverse(inverse(A)) does not equals A");
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
			if (!C.equalsEpsilon(Matrix3.IDENTITY, Constants.EPSILON)) fail("A.inverse(A) does not equals I");
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
}
