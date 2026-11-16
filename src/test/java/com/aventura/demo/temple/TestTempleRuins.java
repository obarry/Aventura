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
package com.aventura.demo.temple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;

import org.junit.BeforeClass;
import org.junit.Test;

import com.aventura.context.PerspectiveContext;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.atmosphere.Fog;
import com.aventura.model.perspective.PerspectiveType;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.GeometryReport;
import com.aventura.model.world.GeometryStatistics;
import com.aventura.view.ImageView;

/**
 * Tests of the TempleRuins demo itself: its pure functions (cameras, sun, flight, colors), its scene (built once for
 * all the tests: it loads the textures of resources/texture, so the tests are run from the root of the project, as
 * Maven does) and a rendering without a window, in a small ImageView.
 *
 * @since Nov 2026
 */
public class TestTempleRuins {

	private static TempleRuins demo;

	@BeforeClass
	public static void createScene() {
		demo = new TempleRuins();
		demo.createScene();
	}

	// ***** Pure functions *****

	@Test
	public void testSunDirection_goesDownFromTheSunTowardsTheScene() {
		System.out.println("***** Test TempleRuins : sun direction *****");
		Vector3 direction = TempleRuins.sunDirection(TempleRuins.SUN_AZIMUTH, TempleRuins.SUN_ELEVATION);
		assertEquals(1f, direction.length(), 1e-5f);
		assertEquals("halfway up the sky", -Math.sin(Math.toRadians(TempleRuins.SUN_ELEVATION)), direction.getZ(), 1e-5f);
		// Sun in the direction of +X, on the horizon: its light travels towards -X
		Vector3 fromEast = TempleRuins.sunDirection(0, 0);
		assertEquals(-1f, fromEast.getX(), 1e-6f);
		assertEquals(0f, fromEast.getZ(), 1e-6f);
		assertEquals("at the zenith, straight down", -1f, TempleRuins.sunDirection(40, 90).getZ(), 1e-6f);
	}

	@Test
	public void testOrbitEyeAndLineOfSight() {
		System.out.println("***** Test TempleRuins : orbit and fly cameras *****");
		Vector4 poi = new Vector4(1, 2, 3, 1);
		Vector4 eye = TempleRuins.orbitEye(poi, 10, (float) Math.PI / 2, 0);
		assertEquals(1f, eye.getX(), 1e-5f);
		assertEquals(12f, eye.getY(), 1e-5f);
		assertEquals(3f, eye.getZ(), 1e-5f);
		assertEquals("always at the distance", 10f, TempleRuins.orbitEye(poi, 10, 0.7f, 0.4f).distance(poi), 1e-4f);
		assertEquals(13f, TempleRuins.orbitEye(poi, 10, 0.7f, (float) Math.PI / 2).getZ(), 1e-4f);

		Vector4 sight = TempleRuins.lineOfSight(0.3f, -0.5f);
		assertEquals("a direction", 0f, sight.getW(), 0f);
		assertEquals(1f, sight.V3().length(), 1e-5f);
		assertEquals(-Math.sin(0.5), sight.getZ(), 1e-5f);
		// Looking back from the eye of an orbit gives the point of interest
		Vector4 back = eye.plus(TempleRuins.lineOfSight((float) -Math.PI / 2, 0).times(10));
		assertEquals(0f, back.distance(poi), 1e-4f);
	}

	@Test
	public void testAdvance_constantSpeedLimitedStepAndLoop() {
		System.out.println("***** Test TempleRuins : advance on the flight path *****");
		assertEquals(12f, TempleRuins.advance(10, 0.2f, 10, 0.5f, 100), 1e-5f);
		assertEquals("a long image does not make the camera jump", 15f, TempleRuins.advance(10, 3f, 10, 0.5f, 100), 1e-5f);
		assertEquals("back to the start after a lap", 1f, TempleRuins.advance(99, 0.2f, 10, 0.5f, 100), 1e-4f);
		assertEquals(0.1f, TempleRuins.dragAngle(50, 1000, 2f), 1e-6f);
		assertEquals(5f, TempleRuins.clamp(7, 0, 5), 0f);
		assertEquals(2.5f, TempleRuins.interpolate(2, 4, 0.25f), 1e-6f);
	}

	@Test
	public void testGroundColor_followsTheNatureOfTheGround() {
		System.out.println("***** Test TempleRuins : ground colors *****");
		assertEquals("sand at the sea level", TempleRuins.SAND_TINT, TempleRuins.groundColor(50, 50, 0, 0));
		assertEquals("rock on a steep slope", TempleRuins.ROCK_TINT, TempleRuins.groundColor(50, 50, 10, 1.2f));
		// Flat and high: earth, grass, or a mix of both
		Color flat = TempleRuins.groundColor(12, -30, 10, 0);
		assertTrue(flat.getRed() >= TempleRuins.GRASS_TINT.getRed() && flat.getRed() <= TempleRuins.EARTH_TINT.getRed());
		assertEquals(0f, TempleRuins.distanceToTemple(0, 0), 0f);
		assertEquals(5f, TempleRuins.distanceToTemple(Temple.groundHalfLength() + 3, Temple.groundHalfWidth() + 4), 1e-4f);
	}

	@Test
	public void testComputedTextures() {
		System.out.println("***** Test TempleRuins : computed textures *****");
		Texture foliage = SceneTextures.createFoliage(32, 1), sea = SceneTextures.createSea(32, 1);
		assertEquals(32, foliage.getWidth());
		assertEquals(32, sea.getHeight());
		boolean varied = false;
		for (int x = 0; x < 32; x++) {
			for (int y = 0; y < 32; y++) {
				Color leaf = foliage.getColor(x, y), water = sea.getColor(x, y);
				assertTrue("leaves are green", leaf.getGreen() > leaf.getRed() && leaf.getGreen() > leaf.getBlue());
				assertTrue("the sea is blue", water.getBlue() > water.getGreen() && water.getGreen() > water.getRed());
				varied |= !leaf.equals(foliage.getColor(0, 0));
			}
		}
		assertTrue(varied);

		// shrink(): each pixel is the average of a block; down to one pixel, the average of the whole texture
		Texture checker = new Texture(4, 4);
		for (int x = 0; x < 4; x++) {
			for (int y = 0; y < 4; y++) checker.setColor(x, y, x < 2 ? new Color(200, 0, 0) : new Color(0, 100, 0));
		}
		Texture half = SceneTextures.shrink(checker, 2);
		assertEquals(2, half.getWidth());
		assertEquals(new Color(200, 0, 0), half.getColor(0, 1));
		assertEquals(new Color(0, 100, 0), half.getColor(1, 0));
		assertEquals(new Color(100, 50, 0), SceneTextures.shrink(checker, 4).getColor(0, 0));

		assertEquals(new Color(50, 100, 150), SceneTextures.mix(new Color(0, 0, 100), new Color(100, 200, 200), 0.5f));
		assertEquals("limited to white", new Color(255, 200, 100), SceneTextures.shade(new Color(200, 100, 50), 2f));
	}

	// ***** The scene *****

	@Test
	public void testScene_treesBouldersAndMistAreWhereTheyShouldBe() {
		System.out.println("***** Test TempleRuins : content of the scene *****");
		assertEquals("island and sea", 2, demo.world.getElements().size());
		assertSame("the shadows of the sun are limited to the island", demo.island, demo.sun.getShadowingElement());
		assertEquals("ground, temple, debris, trees, boulders", 5, demo.island.getSubElements().size());
		assertEquals(2 * TempleRuins.GROUND_DIVISIONS * TempleRuins.GROUND_DIVISIONS, demo.ground.getNbTriangles());
		assertEquals(TempleRuins.NB_TREES, demo.island.getSubElements().get(3).getSubElements().size());
		assertEquals(TempleRuins.NB_BOULDERS, demo.island.getSubElements().get(4).getSubElements().size());

		assertTrue("the haze is on", demo.atmosphere.isHaze());
		assertEquals(TempleRuins.NB_MIST_BANKS, demo.atmosphere.getFogs().size());
		assertTrue("the mist is off at the start", demo.atmosphere.getActiveFogs().isEmpty());
		for (Fog bank : demo.atmosphere.getFogs()) {
			assertEquals("on the sea", 0f, bank.getBoundingCenter().getZ(), 0f);
		}
		assertEquals(0f, demo.groundAltitude(1000, 0), 0f);
	}

	@Test
	public void testSceneReport_composition() {
		System.out.println("***** Test TempleRuins : statistics of the scene *****");
		GeometryStatistics all = demo.world.getStatistics();
		String report = GeometryReport.of(demo.world);
		System.out.println(report);

		// The first line of the geometry is the World, with the totals; then its two branches, which add up to it
		String[] lines = report.split("\\R");
		assertTrue(lines[0].startsWith("GEOMETRY"));
		assertTrue(lines[1], lines[1].matches("World +" + String.format(java.util.Locale.ROOT, "%,d +%,d +%,d +100\\.0%%",
				all.getNbElements(), all.getNbTriangles(), all.getNbVertices())));
		GeometryStatistics island = demo.island.getStatistics(), sea = demo.world.getElement(1).getStatistics();
		assertEquals(all.getNbTriangles(), island.getNbTriangles() + sea.getNbTriangles());
		assertEquals(all.getNbElements(), island.getNbElements() + sea.getNbElements());
		assertEquals(all.getNbVertices(), island.getNbVertices() + sea.getNbVertices());

		// The Elements of the same kind are on one line
		assertTrue(report.contains("column x " + demo.temple.getNbColumns()));
		assertTrue(report.contains("olive tree x " + TempleRuins.NB_TREES));
		assertTrue(report.contains("boulder x " + TempleRuins.NB_BOULDERS));
		assertTrue(report.contains("ground (Trellis)"));
		assertEquals("no line for each of the " + all.getNbElements() + " Elements", -1, report.indexOf("box x 1 "));
		assertTrue(lines.length < 60);

		// The textures: the 4 files, the 2 computed ones and the average color of the sea; every triangle has one
		assertEquals(7, all.getTextures().size());
		assertEquals(all.getNbTriangles(), all.getNbTexturedTriangles());
		assertTrue(report.contains("texture_stone_1023x852.jpg"));
		assertTrue(report.contains("foliage (computed)"));
		assertTrue(report.contains("7 textures"));
		assertEquals(2 * TempleRuins.GROUND_DIVISIONS * TempleRuins.GROUND_DIVISIONS, all.getNbTrianglesUsing(demo.textures.ground));

		assertEquals("ground (Trellis)", GeometryReport.kindOf(demo.ground));
		assertEquals("temple", GeometryReport.kindOf(demo.temple));
	}

	@Test
	public void testTour_staysAboveTheGroundTheSeaAndTheTemple() {
		System.out.println("***** Test TempleRuins : the helicopter tour never goes through the scene *****");
		float templeTop = IslandTerrain.PLATEAU_ALTITUDE + Temple.FLOOR_ALTITUDE + Column.HEIGHT
				+ Temple.ARCHITRAVE_HEIGHT + Temple.FRIEZE_HEIGHT + Temple.CORNICE_HEIGHT;
		for (float d = 0; d < demo.tour.getLength(); d += 0.5f) {
			Vector4 eye = demo.tour.eye(d);
			float clearance = eye.getZ() - demo.groundAltitude(eye.getX(), eye.getY());
			assertTrue("above the ground and the sea at " + d + " m: " + clearance, clearance >= TempleRuins.GROUND_CLEARANCE);
			// Over the platform of the temple, the camera is higher than the temple; beside it, away from its columns
			float toTemple = TempleRuins.distanceToTemple(eye.getX(), eye.getY());
			assertTrue("not through the temple at " + d + " m", toTemple > 1.5f || eye.getZ() > templeTop + 1);
			assertTrue("the camera never looks at itself", demo.tour.target(d).distance(eye) > 5f);
		}
		assertTrue("a lap lasts between half a minute and two minutes",
				demo.tour.getLength() / TempleRuins.TOUR_SPEED > 30 && demo.tour.getLength() / TempleRuins.TOUR_SPEED < 120);
	}

	@Test
	public void testRender_withoutAWindow() {
		System.out.println("***** Test TempleRuins : rendering in an ImageView *****");
		// A small image: the same view plane as the application, with 5 times fewer pixels per unit (200 x 112)
		PerspectiveContext small = new PerspectiveContext(TempleRuins.NEAR_DISTANCE, TempleRuins.NEAR_DISTANCE * 0.5625f,
				TempleRuins.NEAR_DISTANCE, TempleRuins.FAR_DISTANCE, PerspectiveType.FRUSTUM, 2000);
		ImageView view = new ImageView(small);
		com.aventura.engine.RenderEngine renderer = new com.aventura.engine.RenderEngine(demo.world, demo.lighting, demo.camera, demo.rContext, small);
		renderer.setView(view);
		renderer.render();
		BufferedImage image = view.getImageView();
		int width = image.getWidth(), height = image.getHeight();

		// The initial camera looks at the temple from the west: sky at the top, ground at the bottom, marble in the middle
		assertEquals("the sky", TempleRuins.SKY_COLOR, new Color(image.getRGB(width / 10, 2)));
		Color ground = new Color(image.getRGB(width / 2, height - 3));
		assertTrue("the ground, earth or grass: " + ground, ground.getRed() > ground.getBlue() && ground.getGreen() > ground.getBlue());
		int marble = 0, shade = 0;
		for (int x = 0; x < width; x++) {
			for (int y = height / 4; y < height * 3 / 4; y++) {
				Color c = new Color(image.getRGB(x, y));
				int spread = Math.max(c.getRed(), Math.max(c.getGreen(), c.getBlue())) - Math.min(c.getRed(), Math.min(c.getGreen(), c.getBlue()));
				if (spread < 40 && c.getRed() > 170) marble++; // Light and almost gray: marble in the sun
				if (spread < 40 && c.getRed() < 120) shade++;  // Dark and almost gray: marble in the shade
			}
		}
		assertTrue("marble in the sun: " + marble + " pixels", marble > 300);
		assertTrue("and in the shade: " + shade + " pixels", shade > 50);

		// Without the shadows, the image is different: they were drawn
		demo.rContext.setShadowing(false);
		renderer.render();
		BufferedImage noShadow = view.getImageView();
		demo.rContext.setShadowing(true);
		int different = 0;
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if (image.getRGB(x, y) != noShadow.getRGB(x, y)) different++;
			}
		}
		assertNotEquals("the shadows change the image", 0, different);
	}
}
