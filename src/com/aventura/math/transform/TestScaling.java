package com.aventura.math.transform;

import static org.junit.Assert.*;
import org.junit.Test;

import com.aventura.math.vector.Matrix4;

public class TestScaling {

	@Test
	public void testHomothety() {
		System.out.println("***** Test Scaling : testScaling *****");

		Scaling h = new Scaling(12);
		System.out.println("Scaling h: "+ h);

		float[][] a = {
				{12.0f, 0.0f, 0.0f, 0.0f}, 
				{0.0f, 12.0f, 0.0f, 0.0f},
				{0.0f, 0.0f, 12.0f, 0.0f},
				{0.0f, 0.0f, 0.0f, 1.0f}};

		Matrix4 m = new Matrix4(a);
		System.out.println("Matrix m: "+ m);

		assertTrue(h.equals(m));
		System.out.println("h = m -> success");			
	}

	@Test
	public void testSetScale() {
		System.out.println("***** Test Scaling : testSetScale *****");

		Scaling h = new Scaling(7);

		float[][] a = {
				{7.0f, 0.0f, 0.0f, 0.0f}, 
				{0.0f, 7.0f, 0.0f, 0.0f},
				{0.0f, 0.0f, 7.0f, 0.0f},
				{0.0f, 0.0f, 0.0f, 1.0f}};


		Matrix4 m = new Matrix4(a);
		System.out.println("Matrix m: "+ m);

		System.out.println("Scaling h(7): "+ h);

		assertTrue(h.equals(m));
		System.out.println("h = m -> success");
	}

}
