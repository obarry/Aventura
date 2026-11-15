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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.Color;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import com.aventura.cookbook.ElementContractChecker;
import com.aventura.math.vector.Vector4;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.Element;
import com.aventura.model.world.shape.Box;
import com.aventura.model.world.triangle.Triangle;

/**
 * Tests of the geometry of the TempleRuins demo: the noise, the terrain, the new shapes (TiledTrellis, TaperedDrum,
 * Boulder), the assemblies (Column, Temple, OliveTree) and the flight path. None of them needs a window or an image
 * file: the textures are small empty ones.
 *
 * The shapes are checked like the examples of the Geometry Cookbook, with its ElementContractChecker: check() for
 * the contracts on vertices, normals and texture coordinates, and signedVolume() for the winding of a closed shape
 * (the volume enclosed by its triangles is the one of the shape only if they all face outwards).
 *
 * @since Nov 2026
 */
public class TestTempleGeometry {

	private static final Texture TEXTURE = new Texture(4, 4);

	private static int countTriangles(Element e) {
		int nb = e.getNbTriangles();
		if (!e.isLeaf()) {
			for (Element sub : e.getSubElements()) nb += countTriangles(sub);
		}
		return nb;
	}

	private static <T extends Element> int count(Element e, Class<T> type) {
		int nb = type.isInstance(e) ? 1 : 0;
		if (!e.isLeaf()) {
			for (Element sub : e.getSubElements()) nb += count(sub, type);
		}
		return nb;
	}

	// ***** Noise *****

	@Test
	public void testNoise_isDeterministicBoundedAndContinuous() {
		System.out.println("***** Test TempleRuins : noise *****");
		float previous = Noise.fbm(0, 0.3f, 0.7f, 4, 5);
		for (float x = 0; x < 20; x += 0.01f) {
			float value = Noise.fbm(x, 0.3f, 0.7f, 4, 5);
			assertTrue("in [0, 1[: " + value, value >= 0 && value < 1);
			assertEquals("same arguments, same value", value, Noise.fbm(x, 0.3f, 0.7f, 4, 5), 0f);
			assertTrue("no jump between two close points at x = " + x, Math.abs(value - previous) < 0.05f);
			previous = value;
		}
		assertTrue("another seed gives another value", Noise.fbm(1.5f, 2.5f, 3.5f, 3, 1) != Noise.fbm(1.5f, 2.5f, 3.5f, 3, 2));
		assertEquals(0f, Noise.smoothstep(2, 4, 1), 0f);
		assertEquals(0.5f, Noise.smoothstep(2, 4, 3), 1e-6f);
		assertEquals(1f, Noise.smoothstep(2, 4, 5), 0f);
		assertEquals("edges in decreasing order: 1 below, 0 above", 1f, Noise.smoothstep(4, 2, 1), 0f);
	}

	// ***** TiledTrellis *****

	@Test
	public void testMirror_isATriangleWave() {
		System.out.println("***** Test TempleRuins : mirrored tiles *****");
		assertEquals(0f, TiledTrellis.mirror(0), 0f);
		assertEquals(0.25f, TiledTrellis.mirror(0.25f), 1e-6f);
		assertEquals(1f, TiledTrellis.mirror(1), 0f);
		assertEquals(0.75f, TiledTrellis.mirror(1.25f), 1e-6f);
		assertEquals(0f, TiledTrellis.mirror(2), 0f);
		assertEquals(1f, TiledTrellis.mirror(7), 0f);
		assertEquals(0.5f, TiledTrellis.mirror(-0.5f), 1e-6f);
	}

	@Test
	public void testTiledTrellis_repeatsItsTextureWithoutLeavingIt() throws Exception {
		System.out.println("***** Test TempleRuins : TiledTrellis *****");
		float[][] altitudes = new float[9][7];
		for (int i = 0; i <= 8; i++) {
			for (int j = 0; j <= 6; j++) altitudes[i][j] = 0.3f * (float) Math.sin(i + j);
		}
		TiledTrellis trellis = new TiledTrellis(8, 6, 8, 6, altitudes, TEXTURE, 2);
		trellis.build();

		assertEquals("2 triangles per cell", 2 * 8 * 6, trellis.getNbTriangles());
		assertEquals(List.of(), ElementContractChecker.check(trellis));
		boolean reachesZero = false, reachesOne = false;
		for (Triangle t : trellis.getTriangles()) {
			assertTrue("the triangles face up", ElementContractChecker.faceNormal(t).getZ() > 0);
			for (Vector4 uv : new Vector4[] { t.getTexVec1(), t.getTexVec2(), t.getTexVec3() }) {
				assertTrue("texture coordinates in [0, 1]: " + uv, uv.getX() >= 0 && uv.getX() <= 1 && uv.getY() >= 0 && uv.getY() <= 1);
				reachesZero |= uv.getX() == 0;
				reachesOne |= uv.getX() == 1;
			}
		}
		assertTrue("a tile covers the whole texture", reachesZero && reachesOne);

		// rebuild() generates the triangles again, without duplicating them
		trellis.rebuild();
		assertEquals(2 * 8 * 6, trellis.getNbTriangles());
	}

	// ***** TaperedDrum *****

	@Test
	public void testTaperedDrum_smoothAndClosed_enclosesTheVolumeOfAConeFrustum() {
		System.out.println("***** Test TempleRuins : TaperedDrum, smooth *****");
		TaperedDrum drum = new TaperedDrum(1f, 0.6f, 2f, 64, TEXTURE).withCaps(true, true);
		drum.build();
		assertEquals("2 triangles per segment", 2 * 64, drum.getNbTriangles());
		assertEquals("and 2 caps of one triangle per segment", 4 * 64, countTriangles(drum));
		assertEquals(List.of(), ElementContractChecker.check(drum));
		assertFalse("smooth: normals at the vertices", drum.getTriangle(0).isTriangleNormal());
		// The polygon of 64 sides is a little smaller than the circle: 0.2% less volume
		assertEquals(drum.getFrustumVolume(), ElementContractChecker.signedVolume(drum), drum.getFrustumVolume() * 0.005f);
	}

	@Test
	public void testTaperedDrum_fluted_hasFlatFacesAndLessVolume() {
		System.out.println("***** Test TempleRuins : TaperedDrum, fluted *****");
		TaperedDrum drum = new TaperedDrum(0.5f, 0.45f, 1f, 40, 0.03f, TEXTURE, 4).withCaps(true, true);
		drum.build();
		assertEquals(List.of(), ElementContractChecker.check(drum));
		assertTrue("fluted: one normal per face", drum.getTriangle(0).isTriangleNormal());
		for (Triangle t : drum.getTriangles()) {
			Vector4 center = t.getCenter().getPos();
			assertTrue("the faces look outwards", t.getNormal().getX() * center.getX() + t.getNormal().getY() * center.getY() > 0);
		}
		float volume = ElementContractChecker.signedVolume(drum);
		assertTrue("positive volume (wound outwards), less than without the flutes: " + volume, volume > 0 && volume < drum.getFrustumVolume());
	}

	@Test
	public void testTaperedDrum_rejectsWrongParameters() {
		System.out.println("***** Test TempleRuins : TaperedDrum, parameters *****");
		float[][] wrong = { { 0.5f, 0.5f, 0, 40, 0, 2 }, { 0.5f, 0.02f, 1, 40, 0.03f, 4 }, { 0.5f, 0.5f, 1, 41, 0, 2 }, { 0.5f, 0.5f, 1, 40, 0, 3 }, { 0.5f, 0.5f, 1, 10, 0, 4 } };
		for (float[] w : wrong) {
			try {
				new TaperedDrum(w[0], w[1], w[2], (int) w[3], w[4], TEXTURE, (int) w[5]);
				fail("should be rejected: " + java.util.Arrays.toString(w));
			} catch (IllegalArgumentException expected) {
				// expected
			}
		}
	}

	// ***** Boulder *****

	@Test
	public void testBoulder_withoutRoughnessIsASphere() {
		System.out.println("***** Test TempleRuins : Boulder, sphere *****");
		Boulder sphere = new Boulder(2f, 0f, 48, 24, 1, TEXTURE, true);
		sphere.build();
		assertEquals("a fan around each pole, 2 triangles per quadrilateral elsewhere", 2 * 48 + 2 * 48 * 22, sphere.getNbTriangles());
		assertEquals(List.of(), ElementContractChecker.check(sphere));
		float expected = (float) (4 * Math.PI * 8 / 3);
		assertEquals(expected, ElementContractChecker.signedVolume(sphere), expected * 0.02f);
		// On a sphere, the smooth normal of a vertex is the direction from the center
		for (int i = 0; i < sphere.getNbVertices(); i += 7) {
			Vector4 p = sphere.getVertex(i).getPos();
			assertEquals(1f, sphere.getVertex(i).getNormal().dot(p.V3()) / 2f, 0.01f);
		}
	}

	@Test
	public void testBoulder_roughSmoothOrFaceted_fulfillsTheContracts() {
		System.out.println("***** Test TempleRuins : Boulder, rough *****");
		for (boolean smooth : new boolean[] { true, false }) {
			Boulder boulder = new Boulder(1.5f, 0.35f, 12, 7, 42, TEXTURE, smooth);
			boulder.build();
			assertEquals(List.of(), ElementContractChecker.check(boulder));
			assertEquals(!smooth, boulder.getTriangle(0).isTriangleNormal());
			assertTrue("wound outwards", ElementContractChecker.signedVolume(boulder) > 0);
			for (int i = 0; i < boulder.getNbVertices(); i++) {
				float distance = boulder.getVertex(i).getPos().V3().length();
				assertTrue("within the roughness around the radius: " + distance, distance > 1.5f * 0.65f - 1e-4f && distance < 1.5f * 1.35f + 1e-4f);
			}
		}
		Boulder a = new Boulder(1f, 0.3f, 12, 7, 1, null, true), b = new Boulder(1f, 0.3f, 12, 7, 2, null, true);
		a.build();
		b.build();
		assertTrue("another seed, another shape", a.getVertex(5).getPos().V3().length() != b.getVertex(5).getPos().V3().length());
	}

	// ***** Column and Temple *****

	@Test
	public void testColumn_completeAndBroken() {
		System.out.println("***** Test TempleRuins : Column *****");
		Column complete = new Column(Column.SHAFT_DRUMS, TEXTURE, Color.WHITE, new Random(1));
		assertTrue(complete.isComplete());
		assertEquals(Column.HEIGHT, complete.getHeight(), 1e-5f);
		// The drums of the shaft, and the echinus of the capital, which is a TaperedDrum too
		assertEquals(Column.SHAFT_DRUMS + 1, count(complete, TaperedDrum.class));
		assertEquals("the abacus", 1, count(complete, Box.class));

		Column broken = new Column(2, TEXTURE, Color.WHITE, new Random(1));
		assertFalse(broken.isComplete());
		assertEquals(2, count(broken, TaperedDrum.class));
		assertEquals("no capital", 0, count(broken, Box.class));
		assertTrue("one whole drum and a part of the next: " + broken.getHeight(),
				broken.getHeight() > Column.DRUM_HEIGHT * 1.3f && broken.getHeight() < Column.DRUM_HEIGHT * 2);

		complete.build();
		broken.build();
		assertEquals(List.of(), ElementContractChecker.check(complete));
		assertEquals(List.of(), ElementContractChecker.check(broken));

		assertEquals(Column.BASE_RADIUS, Column.shaftRadius(0), 1e-6f);
		assertEquals(Column.NECK_RADIUS, Column.shaftRadius(Column.SHAFT_DRUMS * Column.DRUM_HEIGHT), 1e-6f);
		for (int nbDrums : new int[] { 0, Column.SHAFT_DRUMS + 1 }) {
			try {
				new Column(nbDrums, TEXTURE, Color.WHITE, new Random(1));
				fail("should be rejected: " + nbDrums + " drums");
			} catch (IllegalArgumentException expected) {
				// expected
			}
		}
	}

	@Test
	public void testTemple_followsItsPlan() {
		System.out.println("***** Test TempleRuins : Temple *****");
		int columns = 0, complete = 0;
		for (String line : Temple.PLAN) {
			assertEquals("all the lines of the plan have the same length", Temple.NB_ALONG_X, line.length());
			for (char c : line.toCharArray()) {
				assertTrue("a character of the plan: " + c, c == 'C' || c == '.' || (c >= '1' && c < '0' + Column.SHAFT_DRUMS));
				if (c != '.') columns++;
				if (c == 'C') complete++;
			}
		}
		// The inside of the colonnade is empty
		for (int i = 1; i < Temple.NB_ALONG_X - 1; i++) {
			for (int j = 1; j < Temple.NB_ALONG_Y - 1; j++) assertEquals('.', Temple.planAt(i, j));
		}
		assertEquals("the first line of the plan is the +Y side", Temple.PLAN[0].charAt(0), Temple.planAt(0, Temple.NB_ALONG_Y - 1));

		Temple temple = new Temple(TEXTURE, 3);
		assertEquals(columns, temple.getNbColumns());
		assertEquals(complete, temple.getNbCompleteColumns());
		assertEquals(columns, count(temple, Column.class));
		assertEquals("platform, colonnade, entablature, cella", 4, temple.getSubElements().size());

		// The colonnade is centered, inside the floor, itself inside the platform on the ground
		assertEquals(-Temple.columnX(0), Temple.columnX(Temple.NB_ALONG_X - 1), 1e-5f);
		assertEquals(-Temple.columnY(0), Temple.columnY(Temple.NB_ALONG_Y - 1), 1e-5f);
		assertTrue(Temple.floorHalfLength() > Temple.columnX(Temple.NB_ALONG_X - 1) + Column.BASE_RADIUS);
		assertTrue(Temple.groundHalfLength() > Temple.floorHalfLength() && Temple.groundHalfWidth() > Temple.floorHalfWidth());
		assertFalse(Temple.isComplete(-1, 0));

		temple.build();
		assertEquals(List.of(), ElementContractChecker.check(temple));
		// Same seed, same temple
		Temple twin = new Temple(TEXTURE, 3);
		twin.build();
		assertEquals(countTriangles(temple), countTriangles(twin));
	}

	// ***** Terrain *****

	@Test
	public void testTerrain_isAnIslandWithAFlatPlateauForTheTemple() {
		System.out.println("***** Test TempleRuins : terrain *****");
		IslandTerrain terrain = new IslandTerrain(TempleRuins.GROUND_SIZE, TempleRuins.GROUND_DIVISIONS, TempleRuins.ISLAND_SEED);
		float half = TempleRuins.GROUND_SIZE / 2;

		// An island: under the sea level all along the border of the grid
		for (float t = -half; t <= half; t += 1f) {
			for (float[] p : new float[][] { { t, -half }, { t, half }, { -half, t }, { half, t } }) {
				assertTrue("border under the sea at " + p[0] + ", " + p[1], terrain.altitude(p[0], p[1]) < 0);
			}
		}
		assertTrue("nothing outside of the grid", Float.isNaN(terrain.altitude(half + 1, 0)));

		// Flat under the temple and the debris around it
		for (float x = -Temple.groundHalfLength() - 4; x <= Temple.groundHalfLength() + 4; x += 0.5f) {
			for (float y = -Temple.groundHalfWidth() - 4; y <= Temple.groundHalfWidth() + 4; y += 0.5f) {
				assertEquals("plateau at " + x + ", " + y, IslandTerrain.PLATEAU_ALTITUDE, terrain.altitude(x, y), 0.02f);
			}
		}
		assertEquals(0f, terrain.slope(0, 0), 1e-3f);

		// The altitude read back is the one of the grid at its vertices, and between its neighbors elsewhere
		float[][] grid = terrain.getAltitudes();
		float step = TempleRuins.GROUND_SIZE / TempleRuins.GROUND_DIVISIONS;
		assertEquals(grid[10][20], terrain.altitude(-half + 10 * step, -half + 20 * step), 1e-3f);
		float inside = terrain.altitude(-half + 10.3f * step, -half + 20.6f * step);
		float min = Math.min(Math.min(grid[10][20], grid[11][20]), Math.min(grid[10][21], grid[11][21]));
		float max = Math.max(Math.max(grid[10][20], grid[11][20]), Math.max(grid[10][21], grid[11][21]));
		assertTrue(inside >= min - 1e-4f && inside <= max + 1e-4f);
	}

	// ***** Flight path *****

	private static final float[][] SQUARE = { { 10, 0, 5, 0, 0, 0 }, { 0, 10, 5, 0, 0, 0 }, { -10, 0, 5, 0, 0, 0 }, { 0, -10, 5, 0, 0, 0 } };

	@Test
	public void testFlightPath_goesThroughItsWaypointsAndLoops() {
		System.out.println("***** Test TempleRuins : flight path *****");
		FlightPath path = new FlightPath(SQUARE);
		float side = (float) Math.sqrt(200);
		assertEquals(4 * side, path.getLength(), 1e-3f);
		for (int k = 0; k < 4; k++) {
			Vector4 eye = path.eye(k * side);
			assertEquals(SQUARE[k][0], eye.getX(), 1e-3f);
			assertEquals(SQUARE[k][1], eye.getY(), 1e-3f);
			assertEquals(5f, eye.getZ(), 1e-3f);
		}
		// An endless loop: a lap later, or earlier, the camera is at the same place
		assertEquals(0f, path.eye(7f).distance(path.eye(7f + path.getLength())), 1e-3f);
		assertEquals(0f, path.eye(-3f).distance(path.eye(path.getLength() - 3f)), 1e-3f);
		// No jump: two close positions are close, all along the loop (waypoints included)
		for (float d = 0; d < path.getLength(); d += 0.1f) {
			assertTrue("continuous at " + d, path.eye(d).distance(path.eye(d + 0.1f)) < 0.2f);
		}
		// The spline through the corners of a square bulges out of it
		assertTrue(path.eye(side / 2).V3().length() > 7.5f);
		assertEquals("the target is the center all along", 0f, path.target(13f).V3().length(), 1e-4f);

		assertEquals(1.5f, FlightPath.catmullRom(0, 1, 2, 3, 0.5f), 1e-6f);
		try {
			new FlightPath(new float[][] { SQUARE[0], SQUARE[1], SQUARE[2] });
			fail("3 waypoints should be rejected");
		} catch (IllegalArgumentException expected) {
			// expected
		}
	}

	@Test
	public void testFlightPath_banksTowardsTheInsideOfTheTurn() {
		System.out.println("***** Test TempleRuins : flight path, banking *****");
		// The square is travelled counter-clockwise seen from above: a turn to the left all along
		FlightPath left = new FlightPath(SQUARE);
		float d = 3f;
		assertTrue("turning left", left.turnRate(d) > 0);
		Vector4 up = left.up(d, 5f, 0.3f);
		assertEquals("a unit vector", 1f, up.V3().length(), 1e-4f);
		assertTrue("still mostly up", up.getZ() > 0.9f);
		// Seen from the camera, the left is the cross product of the up axis and of the line of sight
		Vector4 sight = left.target(d).minus(left.eye(d));
		Vector4 leftSide = Vector4.zAxis().cross(sight);
		assertTrue("leaning to the left", up.dot(leftSide) > 0);
		assertTrue("limited to the maximum bank", Math.acos(up.getZ()) <= 0.3f + 1e-3f);

		// The same square the other way round: a turn to the right, leaning to the right
		FlightPath right = new FlightPath(new float[][] { SQUARE[3], SQUARE[2], SQUARE[1], SQUARE[0] });
		assertTrue("turning right", right.turnRate(d) < 0);
		Vector4 sightRight = right.target(d).minus(right.eye(d));
		assertTrue("leaning to the right", right.up(d, 5f, 0.3f).dot(Vector4.zAxis().cross(sightRight)) < 0);

		assertEquals("no bank without a gain", 1f, left.up(d, 0f, 0.3f).getZ(), 1e-5f);
	}
}
