package com.aventura.tools.vector3d.transform;

import static org.junit.Assert.*;

import org.junit.Test;

import com.aventura.tools.vector3d.matrix.MatrixArrayWrongSizeException;
import com.aventura.tools.vector3d.vector.Vector3;
import com.aventura.tools.vector3d.vector.VectorArrayWrongSizeException;

public class TestTransformation {

	@Test
	public void testTransformation() {
		fail("Not yet implemented");
	}

	@Test
	public void testUpdateTransformation() {
		fail("Not yet implemented");
	}

	@Test
	public void testTransform() {
		fail("Not yet implemented");
	}

	@Test
	public void testTransformEquals() {
		System.out.println("***** Test Transformation : testTransformEquals *****");
		
		Rotation r1 = new Rotation(Math.PI/3, Vector3.X_AXIS); 
		System.out.println("Rotation: "+r1);
		
		
		Vector3 v_translation = new Vector3(1.0, -1.0, 2.0);
		Translation t1 = new Translation(v_translation);
		System.out.println("Translation: "+t1);
		
		Homothety h1 = new Homothety(23);
		System.out.println("Homothety: "+h1);
		
		Transformation t = null; // TBD		
		Vector3 v1 = new Vector3(1.0, -1.0, 1.0);		
		System.out.println("Vector v1: "+v1);

		t = new Transformation(h1, r1, t1);

		Vector3 v2 = t.transform(v1);
		System.out.println("Vector v2 (resulting from the transfomation t(h1,r1,t1): "+v2);

		t.transformEquals(v1);
		System.out.println("Vector v1 (resulting from the same transfomation Equals : "+v1);
		
		assertTrue(v1.equals(v2)); // Check that transformations are same

	}

}
