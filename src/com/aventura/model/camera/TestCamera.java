package com.aventura.model.camera;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import com.aventura.math.vector.Vector4;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2016-2025 Olivier BARRY
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * ------------------------------------------------------------------------------
 *
 * @author Olivier BARRY
 * @since July 2016
 * 
 */

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
