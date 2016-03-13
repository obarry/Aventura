package com.aventura.math.transform;

import static org.junit.Assert.*;

import org.junit.Test;

import com.aventura.math.vector.Vector3;

public class TestTranslation {


	@Test
	public void testTranslationVector3() {
		System.out.println("***** Test Rotation : testTranslationVector3 *****");
		Vector3 v = new Vector3(1,2,3);
		System.out.println("Vector v="+v);
		Translation t = new Translation(v);
		System.out.println("Translation t="+t);
		assertTrue(v.equals(t));
	}
	
	@Test
	public void testTranslationAddition() {
		System.out.println("***** Test Rotation : testTranslationAddition *****");
		Vector3 v1 = new Vector3(1,2,3);
		System.out.println("Vector v1="+v1);
		Vector3 v2 = new Vector3(3,2,1);
		System.out.println("Vector v2="+v2);
		Vector3 v3 = new Vector3(4,4,4);
		System.out.println("Vector v3="+v3);
		
		Translation t1 = new Translation(v1);
		Translation t2 = new Translation(v2);
		
		Translation t3 = t1.plus(t2);
		System.out.println("Translation t3="+t3);
		assertTrue(v3.equals(t3));
	}
	
}
