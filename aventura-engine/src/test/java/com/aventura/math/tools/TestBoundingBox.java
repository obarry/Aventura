package com.aventura.math.tools;

import static org.junit.Assert.*;

import org.junit.Test;

import com.aventura.math.vector.Vector4;

public class TestBoundingBox {

	@Test
	public void testBuildingBox_0() {
		System.out.println("***** Test BuildingBox : testBuildingBox_0 *****");
		Vector4[] array = new Vector4[2];
		
		float min = 1;
		float max = 3;
		System.out.println("min = "+min+", max = "+max);
		array[0] = new Vector4(min,min,min,1);
		array[1] = new Vector4(max,max,max,1);
		BoundingBox4 box = new BoundingBox4(array);
		
		if (box.getMinX() != min) fail("Wrong minX: "+box.getMinX());
		if (box.getMinY() != min) fail("Wrong minY: "+box.getMinY());
		if (box.getMinZ() != min) fail("Wrong minZ: "+box.getMinZ());
		if (box.getMaxX() != max) fail("Wrong maxX: "+box.getMaxX());
		if (box.getMaxY() != max) fail("Wrong maxY: "+box.getMaxY());
		if (box.getMaxZ() != max) fail("Wrong maxZ: "+box.getMaxZ());
		
	}

	@Test
	public void testBuildingBox_1() {
		System.out.println("***** Test BuildingBox : testBuildingBox_1 *****");
		Vector4[] array = new Vector4[3];
		
		float min = 1;
		float max = 3;
		float middle = 2;
		System.out.println("min = "+min+", max = "+max+" middle = "+middle);
		array[0] = new Vector4(min,middle,max,1);
		array[1] = new Vector4(middle,min,max,1);
		array[2] = new Vector4(max,max,middle,1);
		BoundingBox4 box = new BoundingBox4(array);
		
		if (box.getMinX() != min) fail("Wrong minX: "+box.getMinX());
		if (box.getMinY() != min) fail("Wrong minY: "+box.getMinY());
		if (box.getMinZ() != middle) fail("Wrong minZ: "+box.getMinZ());
		if (box.getMaxX() != max) fail("Wrong maxX: "+box.getMaxX());
		if (box.getMaxY() != max) fail("Wrong maxY: "+box.getMaxY());
		if (box.getMaxZ() != max) fail("Wrong maxZ: "+box.getMaxZ());
		
	}

}
