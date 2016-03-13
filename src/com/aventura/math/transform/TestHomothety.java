package com.aventura.math.transform;

import static org.junit.Assert.*;
import org.junit.Test;

import com.aventura.math.vector.Matrix3;
import com.aventura.math.vector.MatrixArrayWrongSizeException;

public class TestHomothety {

	@Test
	public void testHomothety() {
		System.out.println("***** Test Homotethy : testHomotethy *****");
		
		Homothety h = new Homothety(12);
		System.out.println("Homothety h: "+ h);
		
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
		System.out.println("***** Test Homotethy : testSetScale *****");
		
		Homothety h = new Homothety();
		System.out.println("Homothety h: "+ h);
		
		double[][] a = {
				{7.0, 0.0, 0.0}, 
	            {0.0, 7.0, 0.0},
				{0.0, 0.0, 7.0}};
		
		try {
			Matrix3 m = new Matrix3(a);
			System.out.println("Matrix m: "+ m);
			
			h.setScale(7);
			System.out.println("Homothety h after setScale(7): "+ h);
		
			assertTrue(h.equals(m));
			System.out.println("h = m -> success");
			
		} catch (MatrixArrayWrongSizeException e) {
			e.printStackTrace();
			fail("WrongSizeException");
		} 
			
	}

}
