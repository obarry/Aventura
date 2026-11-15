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
package com.aventura.model.light;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.Element;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Box;
import com.aventura.model.world.shape.Trellis;

/**
 * The shadow map of a DirectionalLight limited to the bounding box of an Element (ShadowingLight.setShadowingElement(),
 * SHADOWING_BOX_ELEMENT): a small group of objects in a World much larger than it. The whole map then covers the
 * group, instead of the World: its texels are small, the group casts its shadow, and what is outside of the box is lit.
 *
 * The scene: a floor of 2000 x 2000 units, and near its middle a "site" (a slab of 20 x 20 with a pole of 1 x 1 x 6
 * standing on it). The sun is at 45 degrees, its light travelling towards +X: the shadow of the pole falls on the
 * slab, up to 6 units from it.
 *
 * @since Nov 2026
 */
public class TestShadowingElement {

	static final int MAP_SIZE = 200;

	private Element site;
	private World world;

	private DirectionalLight sunOver(boolean limitedToTheSite) {
		world = new World();
		Trellis floor = new Trellis(2000, 2000, 4, 4, (Texture) null); // XY plane, z = 0
		world.addElement(floor);

		site = new Element("site");
		Box slab = new Box(20, 20, 0.2f);
		slab.setTransformation(new Translation(new Vector3(0, 0, 0.1f)));
		site.addElement(slab);
		Box pole = new Box(1, 1, 6);
		pole.setTransformation(new Translation(new Vector3(0, 0, 3.2f)));
		site.addElement(pole);
		world.addElement(site);
		world.build();
		world.worldProject();

		DirectionalLight sun = new DirectionalLight(new Vector3(1, 0, -1), 1f);
		sun.setShadowMapSize(MAP_SIZE);
		if (limitedToTheSite) sun.setShadowingElement(site);
		sun.initShadowing(null, null, world);
		sun.generateShadowMap(world);
		return sun;
	}

	@Test
	public void testSetShadowingElement_setsTheBoxType_andNullGoesBackToTheWorld() {
		System.out.println("***** Test shadowing element : the setter chooses the type of box *****");
		DirectionalLight sun = new DirectionalLight(new Vector3(1, 0, -1), 1f);
		assertNull(sun.getShadowingElement());
		assertEquals(ShadowingLight.SHADOWING_BOX_WORLD, sun.shadowingBox_type);

		Element element = new Element("e");
		sun.setShadowingElement(element);
		assertSame(element, sun.getShadowingElement());
		assertEquals(ShadowingLight.SHADOWING_BOX_ELEMENT, sun.shadowingBox_type);

		sun.setShadowingElement(null);
		assertNull(sun.getShadowingElement());
		assertEquals(ShadowingLight.SHADOWING_BOX_WORLD, sun.shadowingBox_type);
	}

	@Test
	public void testTheMapCoversTheElementOnly() {
		System.out.println("***** Test shadowing element : the box of the light is the one of the Element, not of the World *****");
		DirectionalLight world = sunOver(false);
		DirectionalLight limited = sunOver(true);
		float worldWidth = world.perspectiveCtx_light.getPerspective().getWidth();
		float limitedWidth = limited.perspectiveCtx_light.getPerspective().getWidth();
		assertTrue("the box of the World is at least as wide as its floor: " + worldWidth, worldWidth >= 2000f);
		// The site is 20 x 20 x 6.2: seen from the light, less than its diagonal (about 29)
		assertTrue("the box of the site is about as wide as the site: " + limitedWidth, limitedWidth >= 20f && limitedWidth < 30f);
	}

	@Test
	public void testTheElementCastsItsShadow_whichTheWorldBoxIsTooCoarseToShow() {
		System.out.println("***** Test shadowing element : the pole casts its shadow on the slab *****");
		Vector3 up = Vector3.zAxis();
		// On the slab (z = 0.2), 3 units behind the pole, in the middle of its shadow; and beside it, in the sun
		Vector4 inShadow = new Vector4(3, 0, 0.2f, 1);
		Vector4 inTheSun = new Vector4(3, 5, 0.2f, 1);

		DirectionalLight limited = sunOver(true);
		assertEquals("in the shadow of the pole", 0f, limited.shadowFactorAt(inShadow, up), 0f);
		assertEquals("beside the shadow of the pole", 1f, limited.shadowFactorAt(inTheSun, up), 0f);

		// With the box of the World, a texel of the map is more than 10 units wide: the pole (1 unit) is lost
		DirectionalLight wholeWorld = sunOver(false);
		float texel = wholeWorld.perspectiveCtx_light.getPerspective().getWidth() / wholeWorld.getShadowMapWidth();
		assertTrue("a texel of the map of the World is larger than the pole: " + texel, texel > 1f);
	}

	@Test
	public void testOutsideOfTheBoxIsLit() {
		System.out.println("***** Test shadowing element : the floor around the site is lit, on every side and under the box *****");
		DirectionalLight limited = sunOver(true);
		Vector3 up = Vector3.zAxis();
		// Far from the site: outside of the map
		for (float[] p : new float[][] { { 500, 0 }, { -500, 0 }, { 0, 500 }, { 0, -500 }, { 900, 900 } }) {
			assertEquals("far floor at " + p[0] + ", " + p[1], 1f, limited.shadowFactorAt(new Vector4(p[0], p[1], 0, 1), up), 0f);
		}
		// Along the direction of the light, on both sides of the site: inside the map (same rays as the site), but
		// in front of the box or beyond it
		for (float x = -60; x <= 60; x += 0.5f) {
			if (Math.abs(x) <= 10.5f) continue; // Under the slab
			assertEquals("floor in line with the site at x = " + x, 1f, limited.shadowFactorAt(new Vector4(x, 0, 0, 1), up), 0f);
		}
	}
}
