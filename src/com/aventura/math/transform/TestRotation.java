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
		Rotation r11 = new Rotation((float)Math.PI/3, Vector3.xAxis()); 
		System.out.println("r11 : "+r11);
		Rotation r12 = new Rotation((float)Math.PI/4, Vector3.yAxis()); 
		System.out.println("r12 : "+r12);
		Rotation r13 = new Rotation((float)Math.PI/5, Vector3.zAxis());
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
		Rotation r1 = new Rotation((float)Math.PI/4, Vector3.yAxis()); 
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

	// ----- Regression tests for the isRotation() exact-float-equality bug (fixed alongside the phase 2 rework) -----
	// isRotation() used to compare v1.length()/v2.length()/v3.length() to 1 and v1.dot(v2) to 0 with exact
	// float equality (!=). Floating-point noise from cos()/sin() or a Matrix4 copy almost never lands on
	// exactly 1.0f/0.0f, so this could reject perfectly valid rotation matrices (see testRotationMatrix above,
	// which failed before this fix). It now uses MathTools.equals() (same EPSILON tolerance already used by
	// Vector3/Vector4.equals() elsewhere in the lib) instead of exact comparison.

	@Test
	public void testRotationMatrix_toleratesFloatingPointNoise_regression() {
		System.out.println("***** Test Rotation : isRotation() tolerates noise within EPSILON (regression) *****");

		// An identity rotation matrix with a tiny perturbation (well within Constants.EPSILON = 1e-4) on one
		// diagonal entry -- mimics the kind of noise cos()/sin() or a Matrix4 copy actually produce.
		float noise = 3.0e-5f;
		float array[][] = { {1.0f + noise, 0.0f, 0.0f, 0.0f}, {0.0f, 1.0f, 0.0f, 0.0f}, {0.0f, 0.0f, 1.0f, 0.0f}, {0.0f, 0.0f, 0.0f, 1.0f} };

		Rotation r = null;
		try {
			r = new Rotation(array);
		} catch (MatrixArrayWrongSizeException e) {
			e.printStackTrace();
			fail("rotation array has wrong size");
		} catch (NotARotationException e) {
			e.printStackTrace();
			fail("a matrix within EPSILON of a valid rotation should not be rejected as NotARotationException");
		}
		assertFalse(r == null);
	}

	@Test
	public void testRotationMatrix_stillRejectsNoiseBeyondEpsilon_regression() {
		System.out.println("***** Test Rotation : isRotation() still rejects noise beyond EPSILON (regression) *****");

		// Same shape as above, but the perturbation is well beyond Constants.EPSILON (1e-4) -- must still be
		// rejected, to confirm the tolerance fix did not loosen the check into a no-op.
		float noise = 0.01f;
		float array[][] = { {1.0f + noise, 0.0f, 0.0f, 0.0f}, {0.0f, 1.0f, 0.0f, 0.0f}, {0.0f, 0.0f, 1.0f, 0.0f}, {0.0f, 0.0f, 0.0f, 1.0f} };

		Rotation r = null;
		try {
			r = new Rotation(array);
		} catch (MatrixArrayWrongSizeException e) {
			e.printStackTrace();
			fail("rotation array has wrong size");
		} catch (NotARotationException e) {
			// Expected
		}
		assertTrue(r == null);
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
