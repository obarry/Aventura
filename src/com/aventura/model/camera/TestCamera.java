package com.aventura.model.camera;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.Vector4;

public class TestCamera {

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testCameraLookAt() {
		fail("Not yet implemented");
	}

	@Test
	public void testCameraMatrix4() {
		fail("Not yet implemented");
	}

	@Test
	public void testCameraVector4Vector4Vector4() {
		
		Vector4 eye = new Vector4(1,0,0,1);
		Vector4 poi = new Vector4(0,0,0,1);
		Vector4 up = Vector4.Z_AXIS;
		
		System.out.println("Eye: "+eye);
		System.out.println("PoI: "+poi);
		System.out.println("Up:  "+up);

		Camera cam = new Camera(eye, poi, up);

		System.out.println("Camera:\n"+cam.getMatrix());

		//fail("Not yet implemented");
	}

	@Test
	public void testGetMatrix() {
		fail("Not yet implemented");
	}

}
