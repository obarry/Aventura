package com.aventura.model;

import static org.junit.Assert.*;

import org.junit.Test;

import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Box;

/**
 * ------------------------------------------------------------------------------
 * MIT License
 *
 * Copyright (c) 2016-2026 Olivier BARRY
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
 * Vector4.normalize() and Vector4.point() modify their Vector4: a method must not apply them to a Vector4 it
 * received, which belongs to its caller. These methods used to.
 *
 * @author Olivier BARRY
 * @since November 2026
 */
public class TestArgumentsNotModified {

	@Test
	public void testCamera_forwardVectorOfTheCallerIsNotNormalized() {
		System.out.println("***** Test arguments : new Camera(forward, up) does not normalize the caller's forward vector *****");
		Vector4 forward = new Vector4(0, 3, -4, 0);
		new Camera(forward, Vector4.zAxis());
		assertTrue(new Vector4(0, 3, -4, 0).equals(forward));
	}

	@Test
	public void testWorld_maxDistanceDoesNotTurnTheCallersVectorIntoAPoint() {
		System.out.println("***** Test arguments : World.getMaxDistance(vector) does not modify the vector *****");
		World world = new World();
		world.addElement(new Box(2, 2, 2));
		world.build();
		world.worldProject();
		Vector4 from = new Vector4(10, 0, 0, 0);
		float distance = world.getMaxDistance(from);
		assertEquals("the vector is taken as the point (10, 0, 0)", (float) Math.sqrt(11 * 11 + 1 + 1), distance, 1e-4f);
		assertEquals("its w is unchanged", 0f, from.getW(), 0f);
	}
}
