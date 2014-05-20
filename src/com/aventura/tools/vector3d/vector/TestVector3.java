package com.aventura.tools.vector3d.vector;

import static org.junit.Assert.*;

import org.junit.Test;

import com.aventura.tools.vector3d.IndiceOutOfBoundException;

public class TestVector3 {


	@Test
	public void testVector() {
		System.out.println("***** Test Vector3 : testVector *****");
		
		Vector3 V1 = new Vector3();
		Vector3 V2 = new Vector3(0.0);
		Vector3 V3 = new Vector3(7.0);
		System.out.println("V1="+V1);
		System.out.println("V2="+V2);
		System.out.println("V3="+V3);
		
		if (!V1.equals(V2)) fail("V1 does not equals V2");
	}

	@Test
	public void testVector2() {
		System.out.println("***** Test Vector3 : testVector2 *****");
		
		double[] x_axis = {1.0, 0.0, 0.0};
		double[] y_axis = {0.0, 1.0, 0.0};
		double[] z_axis = {0.0, 0.0, 1.0};
		
		Vector3 V1 = Vector3.X_AXIS;
		Vector3 V2 = Vector3.Y_AXIS;
		Vector3 V3 = Vector3.Z_AXIS;
		
		System.out.println("V1="+V1);
		System.out.println("V2="+V2);
		System.out.println("V3="+V3);
		
		try {
			if (!V1.equals(new Vector3(x_axis))) fail("V1 does not equals X axis");
			if (!V2.equals(new Vector3(y_axis))) fail("V2 does not equals Y axis");
			if (!V3.equals(new Vector3(z_axis))) fail("V3 does not equals Z axis");
		} catch (VectorArrayWrongSizeException e) {
			fail("V1 array is out of bound");
		}
	}

	@Test
	public void testVector_array_0() {
		System.out.println("***** Test Vector3 : testVector_array_0 *****");
		
		double[] array = new double[3];
		
		for (int i=0; i<3; i++) {
			array[i] = 0;
		}
		
		Vector3 V1;
		Vector3 V2;
		try {
			V1 = new Vector3(array);
			V2 = new Vector3();
			System.out.println("V1="+V1);
			System.out.println("V2="+V2);
		
			if (!V1.equals(V2)) fail("V1 does not equals V2");
		} catch (VectorArrayWrongSizeException e) {
			fail("V1 array is out of bound");
		}
	}

	@Test
	public void testVector_array_value() {
		System.out.println("***** Test Vector3 : testVector_array_value() *****");
		
		double[] array = new double[3];
		
		for (int i=0; i<3; i++) {
			array[i] = 5;
		}
		array[1]=22.3;

		Vector3 V1;
		Vector3 V2;
		try {
			V1 = new Vector3(array);
			V2 = new Vector3(5.0);
			V2.set(1,22.3);
			System.out.println("V1="+V1);
			System.out.println("V2="+V2);
	
			if (!V1.equals(V2)) fail("V1 does not equals V2");
		} catch (VectorArrayWrongSizeException e) {
			fail("V1 array is out of bound");
		} catch (IndiceOutOfBoundException e) {
			fail("Indice out of bound");		
		}
	}
	
	@Test
	public void testVector_plus() {
		System.out.println("***** Test Vector3 : testVector_plus() *****");
		
		double[] array = new double[3];
		
		for (int i=0; i<3; i++) {
				array[i] = i;
		}

		Vector3 V1;
		Vector3 V2;
		int i=0;
		try {
			V1 = new Vector3(array);
			V2 = new Vector3(5.0);
			Vector3 V3 = V1.plus(V2);
			System.out.println("V1="+V1);
			System.out.println("V2="+V2);
			System.out.println("V3="+V3);
	
			for (i=0; i<3; i++) {
				if (V3.get(i) != i+5) fail("V3 does not equals V1+V2");
			}
		} catch (VectorArrayWrongSizeException e) {
			fail("V1 array is out of bound");
		} catch (IndiceOutOfBoundException e) {
			fail("V3 indice out of bound: "+i);		
		}
	}

	@Test
	public void testVector_plusEquals() {
		System.out.println("***** Test Vector3 : testVector_plusEquals() *****");
		
		double[] array1 = new double[3];
		double[] array2 = new double[3];
		
		for (int i=0; i<3; i++) {
				array1[i] = 7-i; // (7, 6, 5)
		}

		for (int i=0; i<3; i++) {
			array2[i] = i*2; // (0, 2, 4)
	}

		Vector3 V1;
		Vector3 V2;
		int i=0;
		try {
			V1 = new Vector3(array1);
			V2 = new Vector3(array2);
			System.out.println("V1="+V1);
			System.out.println("V2="+V2);
			V1.plusEquals(V2);
			System.out.println("V1="+V1);
			System.out.println("V2="+V2);

			if (!(V1.get(0) == 7.0 && V1.get(1) == 8.0 && V1.get(2) == 9.0)) fail("V1 does not equals V1+V2");

		} catch (VectorArrayWrongSizeException e) {
			fail("V1 array is out of bound");
		} catch (IndiceOutOfBoundException e) {
			fail("V3 indice out of bound: "+i);		
		}
	}
	
	@Test
	public void testVector_minus() {
		System.out.println("***** Test Vector3 : testVector_minus() *****");
		
		double[] array = new double[3];
		
		for (int i=0; i<3; i++) {
			array[i] = i;
		}

		Vector3 V1;
		Vector3 V2;
		int i=0;
		try {
			V1 = new Vector3(array);
			V2 = new Vector3(2.0);
			Vector3 V3 = V1.minus(V2);
			System.out.println("V1="+V1);
			System.out.println("V2="+V2);
			System.out.println("V3="+V3);
	
			for (i=0; i<3; i++) {
				if (V3.get(i) != i-2) fail("V3 does not equals V1-V2");
			}
		} catch (VectorArrayWrongSizeException e) {
			fail("V1 array is out of bound");
		} catch (IndiceOutOfBoundException e) {
			fail("V3 indice out of bound: "+i);		
		}
	}
	
	@Test
	public void testVector_minusEquals() {
		System.out.println("***** Test Vector3 : testVector_minusEquals() *****");
		
		double[] array1 = new double[3];
		double[] array2 = new double[3];
		
		for (int i=0; i<3; i++) {
				array1[i] = 7-i; // (7, 6, 5)
		}

		for (int i=0; i<3; i++) {
			array2[i] = i*2-3; // (-3, -1, 1)
	}

		Vector3 V1;
		Vector3 V2;
		int i=0;
		try {
			V1 = new Vector3(array1);
			V2 = new Vector3(array2);
			System.out.println("V1="+V1);
			System.out.println("V2="+V2);
			V1.minusEquals(V2);
			System.out.println("V1="+V1);
			System.out.println("V2="+V2);

			if (!(V1.get(0) == 10.0 && V1.get(1) == 7.0 && V1.get(2) == 4.0)) fail("V1 does not equals V1-V2");

		} catch (VectorArrayWrongSizeException e) {
			fail("V1 array is out of bound");
		} catch (IndiceOutOfBoundException e) {
			fail("V3 indice out of bound: "+i);		
		}
	}
	


	@Test
	public void testVector_times() {
		System.out.println("***** Test Vector3 : testVector_times() *****");
		
		double[] array = new double[3];
		
		for (int i=0; i<3; i++) {
			array[i] = i+1; // (1, 2, 3)
		}

		Vector3 V1;
		try {
			V1 = new Vector3(array);
			Vector3 V2 = V1.times(7.0);
			System.out.println("V1="+V1);
			System.out.println("V2="+V2);
	
			if (!(V2.get(0) == 7.0 && V2.get(1) == 14.0 && V2.get(2) == 21.0)) fail("V2 does not equals V1*7.0");
		} catch (VectorArrayWrongSizeException e) {
			fail("V1 array is out of bound");
		} catch (IndiceOutOfBoundException e) {
			fail("V2 indice out of bound");		
		}
	}

	@Test
	public void testVector_timesEquals() {
		System.out.println("***** Test Vector3 : testVector_timesEquals() *****");
		
		double[] array = new double[3];
		
		for (int i=0; i<3; i++) {
			array[i] = i-1; // (-1, 0, 1)
		}

		Vector3 V1;
		try {
			V1 = new Vector3(array);
			System.out.println("V1="+V1);
			V1.timesEquals(3.0);
			System.out.println("V1="+V1);
	
			if (!(V1.get(0) == -3.0 && V1.get(1) == 0.0 && V1.get(2) == 3.0)) fail("V1 does not equals V1*3.0");
		} catch (VectorArrayWrongSizeException e) {
			fail("V1 array is out of bound");
		} catch (IndiceOutOfBoundException e) {
			fail("V1 indice out of bound");		
		}
	}

	@Test
	public void testVector_scalar() {
		System.out.println("***** Test Vector3 : testVector_scalar() *****");
		
		double[] array = new double[3];
		
		for (int i=0; i<3; i++) {
			array[i] = i;
		}

		Vector3 V1;
		Vector3 V2;
		try {
			V1 = new Vector3(array);
			V2 = new Vector3(2.0);
			double scal = V1.scalar(V2);
			System.out.println("V1="+V1);
			System.out.println("V2="+V2);
			System.out.println("scalar produc ="+scal);
	
			if (scal != 6) fail("Wrong scalar product");
			
		} catch (VectorArrayWrongSizeException e) {
			fail("V1 array is out of bound");
		}
	}
	
	@Test
	public void testVector_timesVector() {
		System.out.println("***** Test Vector3 : testVector_timesVector() *****");
		
	/*
	 * a=(a1,a2,a3) and b=(b1,b2,b3) then a^b=(a2b3−a3b2, a3b1−a1b3, a1b2−a2b1)
	 */
		
		double[] array1 = new double[3];
		double[] array2 = new double[3];
		
		for (int i=0; i<3; i++) {
			array1[i] = i;
			array2[i] = 3-i;
		}

		Vector3 V1; // V1=(0,1,2)
		Vector3 V2; // V2=(3,2,1)
		try {
			V1 = new Vector3(array1);
			V2 = new Vector3(array2);
			Vector3 V3 = V1.times(V2); // V3=(1x1-2x2, 2x3-0x1, 0x2-1x3)=(-3,6,-3)
			System.out.println("V1="+V1);
			System.out.println("V2="+V2);
			System.out.println("V3="+V3);
	
			if (!(V3.get(0) == -3.0 && V3.get(1) == 6.0 && V3.get(2) == -3.0)) fail("V3 does not equals V1^V2");
		} catch (VectorArrayWrongSizeException e) {
			fail("V1 array is out of bound");
		} catch (IndiceOutOfBoundException e) {
			fail("V3 indice out of bound");		
		}
	}

	@Test
	public void testVector_timesEqualsVector() {
		System.out.println("***** Test Vector3 : testVector_timesEqualsVector() *****");
		
	/*
	 * a=(a1,a2,a3) and b=(b1,b2,b3) then a^b=(a2b3−a3b2, a3b1−a1b3, a1b2−a2b1)
	 */
		
		double[] array1 = new double[3];
		double[] array2 = new double[3];
		
		for (int i=0; i<3; i++) {
			array1[i] = i+1;
			array2[i] = 3-i;
		}

		Vector3 V1; // V1=(1,2,3)
		Vector3 V2; // V2=(3,2,1)
		try {
			V1 = new Vector3(array1);
			V2 = new Vector3(array2);
			System.out.println("V1="+V1);
			System.out.println("V2="+V2);
			V1.timesEquals(V2); // Result=(2x1-3x2, 3x3-1x1, 1x2-2x3)=(-4,8,-4)
			System.out.println("V1="+V1);
			System.out.println("V2="+V2);
	
			if (!(V1.get(0) == -4.0 && V1.get(1) == 8.0 && V1.get(2) == -4.0)) fail("V1 does not equals V1^V2");
		} catch (VectorArrayWrongSizeException e) {
			fail("V1 array is out of bound");
		} catch (IndiceOutOfBoundException e) {
			fail("V1 indice out of bound");		
		}
	}

}
