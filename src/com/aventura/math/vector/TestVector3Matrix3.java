package com.aventura.math.vector;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestVector3Matrix3 {
	
	@Test
	public void testVectorMatrix_MatrixTimesVector() {
		System.out.println("***** Test Vector3Matrix3 : testMatrixTimesVector *****");
		
		double[] array1 = new double[3];
		double[][] array2 = new double[3][3];
		
		for (int i=0; i<3; i++) {
			array1[i] = i+1;
		}
		for (int i=0; i<3; i++) {
			for (int j=0; j<3; j++) {
				array2[i][j] = i+j+1;
			}
		}
		/*
		 * A=[[1.0, 2.0, 3.0]
		 *    [2.0, 3.0, 4.0]
		 *    [3.0, 4.0, 5.0]]
		 */
		
		Matrix3 A;
		Vector3 V1;
		Vector3 V2;
		try {
			V1 = new Vector3(array1);
			System.out.println("V1="+V1);
			A = new Matrix3(array2);
			System.out.println("A="+A);
			V2 = A.times(V1);
			System.out.println("V1="+V1);
			System.out.println("A="+A);
			System.out.println("V2="+V2);
		
			
			if (!(V2.get(0) == 14.0 && V2.get(1) == 20.0 && V2.get(2) == 26.0)) fail("V2 does not equals A^V1");
			
		} catch (VectorArrayWrongSizeException e) {
			fail("Vector V1 array is out of bound");
		} catch (MatrixArrayWrongSizeException e) {
		fail("Matrix3 A array is out of bound");
		} catch (IndiceOutOfBoundException e) {
			fail("V2 indice out of bound");		
		}
	}

	@Test
	public void testVectorMatrix_MatrixGetVector() {
		System.out.println("***** Test Vector3Matrix3 : testMatrixGetVector *****");
		
		double[][] array = new double[3][3];
		
		for (int i=0; i<3; i++) {
			for (int j=0; j<3; j++) {
				array[i][j] = i+j+1;
			}
		}
		/*
		 * A=[[1.0, 2.0, 3.0]
		 *    [2.0, 3.0, 4.0]
		 *    [3.0, 4.0, 5.0]]
		 */
		
		Matrix3 A;
		Vector3 V1;
		Vector3 V2;
		try {
			A = new Matrix3(array);
			System.out.println("A="+A);
			V1 = A.getRow(1); // Second row
			System.out.println("V1="+V1);
			if (!(V1.get(0) == 2.0 && V1.get(1) == 3.0 && V1.get(2) == 4.0)) fail("V1 does not equals to second row");

			V2 = A.getColumn(2); // Third row
			System.out.println("V2="+V2);
			if (!(V2.get(0) == 3.0 && V2.get(1) == 4.0 && V2.get(2) == 5.0)) fail("V2 does not equals to third column");
			
		} catch (MatrixArrayWrongSizeException e) {
		fail("Matrix3 A array is out of bound");
		} catch (IndiceOutOfBoundException e) {
			fail("V2 indice out of bound");		
		}
	}


}
