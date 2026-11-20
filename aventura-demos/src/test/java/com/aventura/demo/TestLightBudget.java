/*
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
 */
package com.aventura.demo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.aventura.math.vector.Vector4;
import com.aventura.model.light.PointLight;

/**
 * Tests of the LightBudget shared by the demos: which lights it switches on, and how it fades them.
 *
 * Five lights on a line along X, every 5 units, at x = 0, 5, 10, 15, 20. The camera moves along the line.
 *
 * @since Nov 2026
 */
public class TestLightBudget {

	private static final float INTENSITY = 1.2f;

	private PointLight[] lights;

	private LightBudget budget(int maxLights, float fade, float behindPenalty) {
		LightBudget budget = new LightBudget(maxLights, fade);
		budget.setBehindPenalty(behindPenalty);
		lights = new PointLight[5];
		for (int i = 0; i < lights.length; i++) {
			lights[i] = new PointLight(new Vector4(5 * i, 0, 0, 1), 10f, INTENSITY);
			budget.add(lights[i], INTENSITY);
		}
		return budget;
	}

	private static Vector4 at(float x) {
		return new Vector4(x, 0, 0, 1);
	}

	@Test
	public void testTheNearestLightsAreOn() {
		System.out.println("***** Test LightBudget : the nearest lights are switched on, the others off *****");
		LightBudget budget = budget(2, 0, 0);
		assertEquals(2, budget.update(at(11), at(30)));
		assertFalse(lights[0].isEnabled());
		assertTrue(lights[2].isEnabled()); // At 1
		assertTrue(lights[3].isEnabled()); // At 4
		assertFalse(lights[1].isEnabled()); // At 6
		assertEquals("no fading: full intensity", INTENSITY, lights[2].getIntensity(lights[2].getPosition()), 1e-6f);
	}

	@Test
	public void testFading_continuous() {
		System.out.println("***** Test LightBudget : a light fades out before it leaves the budget *****");
		LightBudget budget = budget(2, 2, 0);
		// At x = 12.4: lights at 2.4 (x = 10) and 2.6 (x = 15) are on; the first one left out is at 7.4 (x = 5)
		budget.update(at(12.4f), at(30));
		assertEquals("far from the limit: full", INTENSITY, intensity(2), 1e-5f);
		// At x = 7.6: the lights at x = 5 (2.6) and 10 (2.4) are on, the one at x = 15 is left out at 7.4: full
		// Between the two, at x = 10 - 2.5 + ... the third light comes as near as the second: it fades
		budget.update(at(7.5f), at(30)); // Lights at 2.5 (x = 5 and x = 10), 7.5 (x = 0 and x = 15)
		assertEquals(INTENSITY, intensity(1), 1e-5f);
		budget.update(at(12.5f), at(30)); // At 2.5 (x = 10, 15); at 7.5 (x = 5, 20)
		assertEquals(INTENSITY, intensity(2), 1e-5f);
		// Halfway between two lights in the budget and the next: x such that the 2nd light and the 3rd are as near
		budget.update(at(10), at(30)); // At 0 (x = 10), 5 (x = 5 and x = 15): a tie for the second place
		int on = 0;
		for (int i = 0; i < lights.length; i++) {
			if (lights[i].isEnabled()) on++;
		}
		assertEquals(2, on);
		float second = lights[1].isEnabled() ? intensity(1) : intensity(3);
		assertEquals("a tie: the second light is faded out completely", 0, second, 1e-5f);
		assertEquals("the nearest one is at full intensity", INTENSITY, intensity(2), 1e-5f);
	}

	@Test
	public void testLightsInFrontFirst() {
		System.out.println("***** Test LightBudget : the lights in front of the camera first *****");
		LightBudget budget = budget(1, 0, 1);
		// At x = 9, looking towards +X: the light at x = 10 (1 ahead) wins anyway
		budget.update(at(9), at(30));
		assertTrue(lights[2].isEnabled());
		// At x = 8, looking towards +X: the light behind (x = 5, at 3, counted as 6) loses against x = 10 (at 2)
		budget.update(at(7.8f), at(30));
		assertTrue(lights[2].isEnabled());
		// At x = 7, looking towards +X: x = 5 is at 2 behind (counted as 4), x = 10 at 3 ahead: the light ahead wins
		budget.update(at(7), at(30));
		assertTrue("the light ahead, though farther", lights[2].isEnabled());
		assertFalse(lights[1].isEnabled());
		// Looking towards -X, the light at x = 5 wins
		budget.update(at(7), at(-30));
		assertTrue(lights[1].isEnabled());
	}

	@Test
	public void testEnableAll_andMaxLights() {
		System.out.println("***** Test LightBudget : all lights on, change of size *****");
		LightBudget budget = budget(1, 1, 1);
		budget.update(at(0), at(30));
		budget.enableAll();
		for (int i = 0; i < lights.length; i++) {
			assertTrue(lights[i].isEnabled());
			assertEquals(INTENSITY, intensity(i), 1e-6f);
		}
		budget.setMaxLights(3);
		assertEquals(3, budget.update(at(0), at(30)));
		budget.setMaxLights(10);
		assertEquals("no more than there are", 5, budget.update(at(0), at(30)));
		assertEquals(5, budget.getNbLights());
	}

	private float intensity(int i) {
		return lights[i].getIntensity(lights[i].getPosition());
	}
}
