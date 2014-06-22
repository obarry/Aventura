package com.aventura.tools.vector3d.transform;

import static org.junit.Assert.*;

import org.junit.Test;

import com.aventura.tools.vector3d.vector.Vector3;

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
		Transformation t = null; // TBD
		Vector3 v1 = null; // TBD

		Vector3 v2 = t.transform(v1);
		t.transformEquals(v1);
		
		assertTrue(v1.equals(v2)); // Check that transformations are same

	}

}
