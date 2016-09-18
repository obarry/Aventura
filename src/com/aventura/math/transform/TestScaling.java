package com.aventura.math.transform;

import static org.junit.Assert.*;
import org.junit.Test;

import com.aventura.math.vector.Matrix3;
import com.aventura.math.vector.MatrixArrayWrongSizeException;

public class TestScaling {

	@Test
	public void testHomothety() {
		System.out.println("***** Test Scaling : testScaling *****");
		
		Scaling h = new Scaling(12);
		System.out.println("Scaling h: "+ h);
		
		double[][] a = {
				{12.0, 0.0, 0.0}, 
	            {0.0, 12.0, 0.0},
				{0.0, 0.0, 12.0}};
		
		try {
			Matrix3 m = new Matrix3(a);
			System.out.println("Matrix m: "+ m);
			
			assertTrue(h.equals(m));
			System.out.println("h = m -> success");
			
		} catch (MatrixArrayWrongSizeException e) {
			e.printStackTrace();
			fail("WrongSizeException");
		} 
			
	}

	@Test
	public void testSetScale() {
		System.out.println("***** Test Scaling : testSetScale *****");
		
		Scaling h = new Scaling(7);
		
		double[][] a = {
				{7.0, 0.0, 0.0}, 
	            {0.0, 7.0, 0.0},
				{0.0, 0.0, 7.0}};
		
		try {
			Matrix3 m = new Matrix3(a);
			System.out.println("Matrix m: "+ m);
			
			System.out.println("Scaling h(7): "+ h);
		
			assertTrue(h.equals(m));
			System.out.println("h = m -> success");
			
		} catch (MatrixArrayWrongSizeException e) {
			e.printStackTrace();
			fail("WrongSizeException");
		} 
			
	}

}
