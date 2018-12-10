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
		try {
			A = new Matrix4(array);
			B = new Matrix4(5);
			B.set(1,2,22.3f);
			System.out.println("A="+A);
			System.out.println("B="+B);

			if (!A.equals(B)) fail("A does not equals B");
		} catch (IndiceOutOfBoundException e) {
			fail("Indice out of bound");		
		}
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
		try {
			Matrix4 B = A.inverse(); // Calculate inverse
			System.out.println("B="+B);
			Matrix4 C = B.inverse(); // Inverse the inverse
			System.out.println("C ="+C);
			if (!B.equals(C)) fail("inverse(inverse(A)) does not equals A");
		} catch (NotInvertibleMatrixException e) {
			fail("Not invertible Matrix");
		}

	}
	
	@Test
	public void testMatrix4_inverse2() {
		System.out.println("***** Test Matrix4 : testMatrix4_inverse2() *****");

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
		try {
			Matrix4 B = A.inverse(); // Calculate inverse
			System.out.println("B="+B);
			Matrix4 C = B.times(A); // Inverse the inverse
			System.out.println("C ="+C);
			if (!C.equals(Matrix4.IDENTITY)) fail("A.inverse(A) does not equals I");
		} catch (NotInvertibleMatrixException e) {
			fail("Not invertible Matrix");
		}

	}
	
}
