package com.aventura.tools.vector3d.transform;

import static org.junit.Assert.*;

import org.junit.Test;

import com.aventura.tools.vector3d.Constants;
import com.aventura.tools.vector3d.WrongAxisException;
import com.aventura.tools.vector3d.vector.Vector3;

public class TestRotation {

	@Test
	public void testRotationDoubleVector3() {
		Rotation r11 = new Rotation(Math.PI/3, Vector3.X_AXIS); 
		Rotation r12 = new Rotation(Math.PI/4, Vector3.Y_AXIS); 
		Rotation r13 = new Rotation(Math.PI/5, Vector3.Z_AXIS);
		Rotation r21 = new Rotation();
		Rotation r22 = new Rotation();
		Rotation r23 = new Rotation();
		try {
			r21 = new Rotation(Math.PI/3, Constants.X_axis); 
			r22 = new Rotation(Math.PI/4, Constants.Y_axis);
			r23 = new Rotation(Math.PI/5, Constants.Z_axis);
		} catch (WrongAxisException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail("WrongAxisException");
		} 
		
		if (!r11.equals(r21)) fail("Rotation around x axis incorrect");
		if (!r12.equals(r22)) fail("Rotation around y axis incorrect");
		if (!r13.equals(r23)) fail("Rotation around z axis incorrect");
			
	}


}
