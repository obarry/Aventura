package com.aventura.math.transform;

import static org.junit.Assert.*;

import org.junit.Test;

import com.aventura.math.Constants;
import com.aventura.math.vector.MatrixArrayWrongSizeException;
import com.aventura.math.vector.Vector3;

public class TestRotation {

	@Test
	public void testRotationDoubleVector3() {
		System.out.println("***** Test Rotation : testRotationDoubleVector3 *****");
		Rotation r11 = new Rotation((float)Math.PI/3, Vector3.X_AXIS); 
		System.out.println("r11 : "+r11);
		Rotation r12 = new Rotation((float)Math.PI/4, Vector3.Y_AXIS); 
		System.out.println("r12 : "+r12);
		Rotation r13 = new Rotation((float)Math.PI/5, Vector3.Z_AXIS);
		System.out.println("r13 : "+r13);
		Rotation r21 = new Rotation();
		Rotation r22 = new Rotation();
		Rotation r23 = new Rotation();
		try {
			r21 = new Rotation((float)Math.PI/3, Constants.X_axis); 
			System.out.println("r21 : "+r21);
			r22 = new Rotation((float)Math.PI/4, Constants.Y_axis);
			System.out.println("r22 : "+r22);
			r23 = new Rotation((float)Math.PI/5, Constants.Z_axis);
			System.out.println("r23 : "+r23);
		} catch (WrongAxisException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail("WrongAxisException");
		} 
		
		if (!r11.equals(r21)) fail("Rotation around x axis incorrect");
		if (!r12.equals(r22)) fail("Rotation around y axis incorrect");
		if (!r13.equals(r23)) fail("Rotation around z axis incorrect");
	}

	@Test
	public void testRotationMatrix() {
		System.out.println("***** Test Rotation : testRotationMatrix *****");
		Rotation r1 = new Rotation((float)Math.PI/4, Vector3.Y_AXIS); 
		System.out.println("r1 : "+r1);
		
		Rotation r2 = null;
		try {
			r2 = new Rotation(r1);
		} catch (NotARotationException e) {
			e.printStackTrace();
			fail("r2 is not a rotation");
		}
		assertFalse(r2==null);
	}

	@Test
	public void testRotationMatrixFail() {
		System.out.println("***** Test Rotation : testRotationMatrixFail *****");
		
		float array[][] = {{3.0f, -4.0f, 1.0f, 0.0f}, {5.0f, 3.0f, -7.0f, 0.0f}, {-9.0f, 2.0f, 6.0f, 0.0f}, {0.0f, 0.0f, 0.0f, 1.0f}};
		
		Rotation r = null;
		try {
			r = new Rotation(array);
		} catch (MatrixArrayWrongSizeException e) {
			e.printStackTrace();
			fail("rotation array has wrong size");		
		} catch (NotARotationException e) {
			e.printStackTrace();
		}
		assertTrue(r==null);
	}
	
}
