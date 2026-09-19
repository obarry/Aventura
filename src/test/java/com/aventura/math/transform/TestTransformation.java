package com.aventura.math.transform;

import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;

import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.world.Element;

public class TestTransformation {

	@Test
	public void testTransformation() {
		System.out.println("***** Test Transformation : testTransformation (Scale/Rotate/Translate application order) *****");

		// The Transformation(Scaling, Rotation, Translation) convenience constructor and the incremental
		// setTransformation(scaling) + combineTransformation(rotation) + combineTransformation(translation)
		// path are two ways to build "the same" transformation from the same three ingredients. They must
		// both apply Scale first, then Rotate, then Translate -- see Transformation's own class Javadoc --
		// and therefore must agree on the result for the same point.
		//
		// REGRESSION (2026): before the constructor's H.R.T -> T.R.H fix, this did NOT hold -- the
		// constructor applied Translation first and Scaling last (the reverse order), while
		// combineTransformation() already composed correctly.

		Scaling h = new Scaling(2);
		Rotation r = new Rotation((float) Math.PI / 2, Vector3.zAxis());
		Translation t = new Translation(new Vector3(5, 0, 0));

		Transformation viaConstructor = new Transformation(h, r, t);

		Element probe = new Element("probe");
		// Scaling extends Matrix4 directly (unlike Rotation/Translation, which extend Transformation),
		// so it must be wrapped to be passed to setTransformation()/combineTransformation().
		probe.setTransformation(new Transformation(h));
		probe.combineTransformation(r);
		probe.combineTransformation(t);
		Transformation viaCombine = probe.getTransformation(); // probe has no parent: full == its own composed transform

		Vector4 p = new Vector4(1, 0, 0, 1);
		Vector4 resultConstructor = viaConstructor.transform(p);
		Vector4 resultCombine = viaCombine.transform(p);

		System.out.println("Result via Transformation(Scaling,Rotation,Translation): " + resultConstructor);
		System.out.println("Result via setTransformation+combineTransformation:      " + resultCombine);

		assertTrue("Transformation(Scaling,Rotation,Translation) must apply Scale, then Rotate, then "
				+ "Translate -- matching Element's documented Model Matrix order and "
				+ "combineTransformation()'s incremental composition -- for the same inputs",
				resultConstructor.equals(resultCombine));
	}

	@Test
	@Ignore("Not yet implemented")
	public void testUpdateTransformation() {
		fail("Not yet implemented");
	}

	@Test
	@Ignore("Not yet implemented")
	public void testTransform() {
		fail("Not yet implemented");
	}

	@Test
	public void testTransformEquals() {
		System.out.println("***** Test Transformation : testTransformEquals *****");
		
		Rotation r1 = new Rotation((float)Math.PI/3, Vector3.xAxis()); 
		System.out.println("Rotation: "+r1);
		
		
		Vector3 v_translation = new Vector3(1.0f, -1.0f, 2.0f);
		Translation t1 = new Translation(v_translation);
		System.out.println("Translation: "+t1);
		
		Scaling h1 = new Scaling(23);
		System.out.println("Scaling: "+h1);
		
		Transformation t = null; // TBD		
		Vector4 v1 = new Vector4(1.0f, -1.0f, 1.0f, 0.0f);		
		System.out.println("Vector v1: "+v1);

		t = new Transformation(h1, r1, t1);

		Vector4 v2 = t.transform(v1);
		System.out.println("Vector v2 (resulting from the transfomation t(h1,r1,t1): "+v2);

		t.transformEquals(v1);
		System.out.println("Vector v1 (resulting from the same transfomation Equals : "+v1);
		
		assertTrue(v1.equals(v2)); // Check that transformations are same

	}

}
