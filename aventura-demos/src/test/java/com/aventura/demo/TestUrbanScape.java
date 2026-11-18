package com.aventura.demo;

import static org.junit.Assert.*;

import java.awt.Color;

import org.junit.Test;

import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.light.DirectionalLight;
import com.aventura.model.world.Element;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Box;

/**
 * Tests for UrbanScape's pure logic: the composition of the Elements (Window -> Floor -> Building), the
 * positions of their sub-Elements in the World once all the transformations of the tree are composed,
 * and the construction of the whole World. None of this needs a display.
 */
public class TestUrbanScape {

	private static final float EPS = 1e-4f;

	// Number of sub-Elements (direct children only) of parent that are instances of the class c
	private static int countChildren(Element parent, Class<?> c) {
		int nb = 0;
		if (parent.getSubElements() != null) {
			for (Element e : parent.getSubElements()) {
				if (c.isInstance(e)) nb++;
			}
		}
		return nb;
	}

	// Position in the World of the origin of the Element (all the transformations of its ancestors composed)
	private static Vector4 worldOrigin(Element e) {
		return e.getTransformation().times(new Vector4(0, 0, 0, 1));
	}

	// Number of leaves (Elements without sub-Elements) in the tree rooted at e
	private static int countLeaves(Element e) {
		if (e.isLeaf()) return 1;
		int nb = 0;
		for (Element sub : e.getSubElements()) {
			nb += countLeaves(sub);
		}
		return nb;
	}

	@Test
	public void testWindow_isMadeOfFrameGlassAndSill() {
		System.out.println("***** Test UrbanScape : testWindow_isMadeOfFrameGlassAndSill *****");

		UrbanScape.Window window = new UrbanScape.Window(0.5f, 0.5f);

		assertEquals(3, window.getSubElements().size());
		assertEquals(3, countChildren(window, Box.class));
	}

	@Test
	public void testFloor_hasWindowsOnFrontAndBackFacades() {
		System.out.println("***** Test UrbanScape : testFloor_hasWindowsOnFrontAndBackFacades *****");

		UrbanScape.Floor floor = new UrbanScape.Floor(5, 3.5f, 1, 5, false, Color.GRAY);

		assertEquals("5 windows on each of the 2 facades", 10, countChildren(floor, UrbanScape.Window.class));
		assertEquals("walls and ledge", 2, countChildren(floor, Box.class));
		assertEquals(12, floor.getSubElements().size());
	}

	@Test
	public void testGroundFloor_hasADoorInsteadOfTheMiddleFrontWindow() {
		System.out.println("***** Test UrbanScape : testGroundFloor_hasADoorInsteadOfTheMiddleFrontWindow *****");

		UrbanScape.Floor floor = new UrbanScape.Floor(5, 3.5f, 1, 5, true, Color.GRAY);

		assertEquals("one front window less", 9, countChildren(floor, UrbanScape.Window.class));
		assertEquals("walls, ledge, door and canopy", 4, countChildren(floor, Box.class));
	}

	@Test
	public void testBuilding_isAStackOfFloorsWithARoof() {
		System.out.println("***** Test UrbanScape : testBuilding_isAStackOfFloorsWithARoof *****");

		UrbanScape.Building plain = new UrbanScape.Building("plain", 3, 3f, 4, Color.LIGHT_GRAY, false);
		assertEquals(4, plain.getNbFloors());
		assertEquals(4, countChildren(plain, UrbanScape.Floor.class));
		assertEquals("4 floors and the roof", 5, plain.getSubElements().size());
		assertEquals(4 * UrbanScape.FLOOR_HEIGHT + UrbanScape.ROOF_HEIGHT, plain.getHeight(), EPS);
		assertEquals(3 * UrbanScape.WINDOW_PITCH, plain.getWidth(), EPS);

		UrbanScape.Building withBlock = new UrbanScape.Building("withBlock", 3, 3f, 4, Color.LIGHT_GRAY, true);
		assertEquals("4 floors, the roof and the roof block", 6, withBlock.getSubElements().size());
	}

	@Test
	public void testBuilding_floorsAreTranslatedVertically() {
		System.out.println("***** Test UrbanScape : testBuilding_floorsAreTranslatedVertically *****");

		World world = new World();
		UrbanScape.Building building = UrbanScape.addBuilding(world, new UrbanScape.Building("b", 3, 3f, 5, Color.LIGHT_GRAY, false), 4f);

		int k = 0;
		for (Element e : building.getSubElements()) {
			if (e instanceof UrbanScape.Floor) {
				Vector4 origin = worldOrigin(e);
				// Building at x = 4, its front facade on the y = 0 line so its center is at y = depth/2. Floor k is at z = k * height
				assertEquals("floor " + k + " x", 4f, origin.getX(), EPS);
				assertEquals("floor " + k + " y", 1.5f, origin.getY(), EPS);
				assertEquals("floor " + k + " z", k * UrbanScape.FLOOR_HEIGHT, origin.getZ(), EPS);
				k++;
			}
		}
		assertEquals(5, k);
	}

	@Test
	public void testFloor_windowsAreOnTheFrontAndBackFacadesOfTheBuilding() {
		System.out.println("***** Test UrbanScape : testFloor_windowsAreOnTheFrontAndBackFacadesOfTheBuilding *****");

		World world = new World();
		float depth = 3f;
		UrbanScape.Building building = UrbanScape.addBuilding(world, new UrbanScape.Building("b", 3, depth, 3, Color.LIGHT_GRAY, false), -2f);

		// Second floor (k = 1): all its windows, in World coordinates
		Element floor = building.getSubElements().get(1);
		assertTrue(floor instanceof UrbanScape.Floor);

		int front = 0, back = 0;
		for (Element e : floor.getSubElements()) {
			if (e instanceof UrbanScape.Window) {
				Vector4 p = worldOrigin(e);
				assertEquals("altitude of the window", 1f + UrbanScape.WINDOW_CENTER_RATIO * UrbanScape.FLOOR_HEIGHT, p.getZ(), EPS);
				assertTrue("window inside the width of the building", Math.abs(p.getX() - (-2f)) < 1.5f);
				if (Math.abs(p.getY() - 0f) < EPS) front++; // The front facade of all the buildings is on the y = 0 line
				else if (Math.abs(p.getY() - depth) < EPS) back++;
				else fail("Window neither on the front nor on the back facade: y = " + p.getY());
			}
		}
		assertEquals(3, front);
		assertEquals(3, back);
	}

	@Test
	public void testBuilding_wallColorIsInheritedButNotWindowColor() {
		System.out.println("***** Test UrbanScape : testBuilding_wallColorIsInheritedButNotWindowColor *****");

		Color wall = new Color(200, 100, 50);
		UrbanScape.Building building = new UrbanScape.Building("b", 3, 3f, 2, wall, false);

		assertEquals(wall, building.getColor());
		Element floor = building.getSubElements().get(0);
		assertNull("a Floor does not define a color, it takes the one of its Building", floor.getColor());
		Element core = floor.getSubElements().get(0);
		assertNull("neither does the core of a Floor", core.getColor());
		Element window = null;
		for (Element e : floor.getSubElements()) {
			if (e instanceof UrbanScape.Window) window = e;
		}
		assertNotNull(window);
		assertNull("a Window itself has no color...", window.getColor());
		assertEquals("...but its glass has", UrbanScape.GLASS_COLOR, window.getSubElements().get(1).getColor());
	}

	@Test
	public void testWorld_containsGroundStreetAndThreeBuildingsOfDifferentHeights() {
		System.out.println("***** Test UrbanScape : testWorld_containsGroundStreetAndThreeBuildingsOfDifferentHeights *****");

		World world = UrbanScape.createWorld();

		assertEquals("ground, street and 3 buildings", 5, world.getNbElements());
		int nbBuildings = 0;
		float minHeight = Float.MAX_VALUE, maxHeight = 0;
		for (Element e : world.getElements()) {
			if (e instanceof UrbanScape.Building) {
				nbBuildings++;
				float h = ((UrbanScape.Building) e).getHeight();
				minHeight = Math.min(minHeight, h);
				maxHeight = Math.max(maxHeight, h);
			}
		}
		assertEquals(3, nbBuildings);
		assertTrue("the buildings must not all have the same height", maxHeight > minHeight);
	}

	@Test
	public void testWorld_isBuiltDownToTheLeaves() {
		System.out.println("***** Test UrbanScape : testWorld_isBuiltDownToTheLeaves *****");

		World world = UrbanScape.createWorld();

		for (Element e : world.getElements()) {
			if (e instanceof UrbanScape.Building) {
				// World.build() must have reached every Box of the tree, whatever its depth: each one has its 12 triangles
				assertEquals(12 * countLeaves(e), UrbanScape.countTriangles(e));
				// And the groups themselves have no triangle of their own
				assertEquals(0, e.getNbTriangles());
				assertEquals(0, e.getSubElements().get(0).getNbTriangles());
			}
		}
	}

	@Test
	public void testWorld_worldBoundsIncludeTheWholeHierarchy() {
		System.out.println("***** Test UrbanScape : testWorld_worldBoundsIncludeTheWholeHierarchy *****");

		World world = UrbanScape.createWorld();
		world.worldProject();
		Vector4[] bounds = world.getWorldBounds();

		// The highest point of the World is the top of the roof block of the tower (10 floors, the roof slab and a 0.5 block)
		float expectedTop = 10 * UrbanScape.FLOOR_HEIGHT + UrbanScape.ROOF_HEIGHT + 0.5f;
		assertEquals(expectedTop, bounds[1].getZ(), EPS);
		// The lowest is the ground
		assertEquals(0f, bounds[0].getZ(), EPS);
		// The ground is the widest element on X
		assertEquals(-UrbanScape.GROUND_WIDTH / 2, bounds[0].getX(), EPS);
		assertEquals(UrbanScape.GROUND_WIDTH / 2, bounds[1].getX(), EPS);
	}

	@Test
	public void testSun_isAtTheRequestedElevationAndHeading() {
		System.out.println("***** Test UrbanScape : testSun_isAtTheRequestedElevationAndHeading *****");

		DirectionalLight sun = UrbanScape.createSun(90f, 45f, 1f);
		// Light vector: from the scene towards the sun. The rays travel towards +Y at heading 90, going down
		Vector3 toSun = sun.getLightVectorAtPoint(null);

		assertEquals(0f, toSun.getX(), EPS);
		assertEquals(-Math.cos(Math.PI / 4), toSun.getY(), EPS);
		assertEquals(Math.sin(Math.PI / 4), toSun.getZ(), EPS);
		assertEquals("the light vector is normalized", 1f, toSun.length(), EPS);

		Vector3 defaultSun = UrbanScape.createSun(UrbanScape.SUN_HEADING, UrbanScape.SUN_ELEVATION, 1f).getLightVectorAtPoint(null);
		assertTrue("the sun is above the ground", defaultSun.getZ() > 0);
	}

	// ****************************************
	// ***** Helicopter flight of the camera *****
	// ****************************************

	// Distance from a point to a Building (to the box containing it), 0 if the point is inside it
	private static float distanceToBuilding(Vector4 p, UrbanScape.Building b) {
		Vector4 c = worldOrigin(b);
		float dx = Math.max(0, Math.abs(p.getX() - c.getX()) - b.getWidth() / 2);
		float dy = Math.max(0, Math.abs(p.getY() - c.getY()) - b.getDepth() / 2);
		float dz = Math.max(0, Math.max(-p.getZ(), p.getZ() - b.getHeight()));
		return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	private static float rad(float degrees) {
		return (float) Math.toRadians(degrees);
	}

	@Test
	public void testCentralBuilding_isTheOneInTheMiddleOfTheWorld() {
		System.out.println("***** Test UrbanScape : testCentralBuilding_isTheOneInTheMiddleOfTheWorld *****");

		World world = UrbanScape.createWorld();
		UrbanScape.Building central = UrbanScape.findCentralBuilding(world);

		assertNotNull(central);
		assertTrue("the middle building is the medium one: " + central.getName(), central.getName().startsWith("block"));

		// Its center is at mid height, in the middle of its footprint: x = -1 and y = depth / 2 (front facade on y = 0)
		Vector4 center = UrbanScape.getWorldCenter(central);
		assertEquals(-1f, center.getX(), EPS);
		assertEquals(1.5f, center.getY(), EPS);
		assertEquals(central.getHeight() / 2, center.getZ(), EPS);

		assertNull("no building, no central building", UrbanScape.findCentralBuilding(new World()));
	}

	@Test
	public void testFlight_cameraStaysOnTheOrbitAndLooksAtTheCentralBuilding() {
		System.out.println("***** Test UrbanScape : testFlight_cameraStaysOnTheOrbitAndLooksAtTheCentralBuilding *****");

		World world = UrbanScape.createWorld();
		UrbanScape.HelicopterFlight flight = UrbanScape.createFlight(world);

		Vector4 center = UrbanScape.getWorldCenter(UrbanScape.findCentralBuilding(world));
		assertTrue("the camera looks at the center of the middle building", flight.getFocus().equals(center));

		for (int degrees = 0; degrees <= 720; degrees += 10) {
			Vector4 eye = flight.getEye(rad(degrees));
			assertEquals("distance to the center of the orbit at " + degrees, flight.getRadius(), eye.minus(flight.getCenter()).length(), 1e-3f);
		}
	}

	@Test
	public void testFlight_lapIsClosed() {
		System.out.println("***** Test UrbanScape : testFlight_lapIsClosed *****");

		UrbanScape.HelicopterFlight flight = UrbanScape.createFlight(UrbanScape.createWorld());

		for (int laps = 1; laps <= UrbanScape.FLIGHT_LAPS; laps++) {
			Vector4 start = flight.getEye(0);
			Vector4 end = flight.getEye((float) (2 * Math.PI * laps));
			assertEquals(0f, end.minus(start).length(), 1e-2f);
		}
	}

	@Test
	public void testFlight_orbitIsNotCenteredOnTheBuildings_cameraGetsCloserThenFarther() {
		System.out.println("***** Test UrbanScape : testFlight_orbitIsNotCenteredOnTheBuildings_cameraGetsCloserThenFarther *****");

		UrbanScape.HelicopterFlight flight = UrbanScape.createFlight(UrbanScape.createWorld());

		float min = Float.MAX_VALUE, max = 0;
		int argMin = 0, argMax = 0;
		for (int degrees = 0; degrees < 360; degrees++) {
			float d = flight.getEye(rad(degrees)).minus(flight.getFocus()).length();
			if (d < min) { min = d; argMin = degrees; }
			if (d > max) { max = d; argMax = degrees; }
		}
		assertTrue("distance to the building must at least double: " + min + " -> " + max, max > 2 * min);
		// Closest at the start of the flight (in front of the buildings), farthest half a lap later (behind them)
		assertTrue("closest at " + argMin, Math.abs(argMin) < 15 || argMin > 345);
		assertTrue("farthest at " + argMax, Math.abs(argMax - 180) < 15);
	}

	@Test
	public void testFlight_tiltedAxis_cameraIsLowestWhenClosestToTheBuildings() {
		System.out.println("***** Test UrbanScape : testFlight_tiltedAxis_cameraIsLowestWhenClosestToTheBuildings *****");

		UrbanScape.HelicopterFlight flight = UrbanScape.createFlight(UrbanScape.createWorld());

		float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
		for (int degrees = 0; degrees < 360; degrees++) {
			float z = flight.getEye(rad(degrees)).getZ();
			min = Math.min(min, z);
			max = Math.max(max, z);
		}
		assertTrue("the altitude must vary a lot: " + min + " -> " + max, max - min > 5);
		assertEquals("lowest at the start, where the camera is the closest", min, flight.getEye(0).getZ(), 0.1f);
		assertEquals("highest half a lap later", max, flight.getEye((float) Math.PI).getZ(), 0.1f);
		assertTrue("the camera is always above the ground", min > 1);
	}

	@Test
	public void testFlight_verticalAxis_altitudeIsConstant() {
		System.out.println("***** Test UrbanScape : testFlight_verticalAxis_altitudeIsConstant *****");

		World world = UrbanScape.createWorld();
		Vector4 focus = UrbanScape.getWorldCenter(UrbanScape.findCentralBuilding(world));
		// Same trajectory but with no tilt: horizontal orbit
		UrbanScape.HelicopterFlight flat = new UrbanScape.HelicopterFlight(focus, UrbanScape.FLIGHT_CENTER_SHIFT, UrbanScape.FLIGHT_RADIUS, 0f, 0f);

		for (int degrees = 0; degrees < 360; degrees += 10) {
			assertEquals(focus.getZ() + UrbanScape.FLIGHT_CENTER_SHIFT.getZ(), flat.getEye(rad(degrees)).getZ(), 1e-3f);
		}
	}

	@Test
	public void testFlight_neverGetsCloseToABuilding() {
		System.out.println("***** Test UrbanScape : testFlight_neverGetsCloseToABuilding *****");

		World world = UrbanScape.createWorld();
		UrbanScape.HelicopterFlight flight = UrbanScape.createFlight(world);

		for (int tenth = 0; tenth < 3600; tenth++) {
			Vector4 eye = flight.getEye(rad(tenth / 10f));
			for (Element e : world.getElements()) {
				if (e instanceof UrbanScape.Building) {
					// The near plane of the view is at distance 1: stay far away from it
					assertTrue("camera too close to " + e.getName() + " at " + tenth / 10f + " degrees",
							distanceToBuilding(eye, (UrbanScape.Building) e) > 4f);
				}
			}
		}
	}

	@Test
	public void testNextAngle_movesForwardAtConstantSpeed() {
		System.out.println("***** Test UrbanScape : testNextAngle_movesForwardAtConstantSpeed *****");

		float speed = 2f; // radians per second
		assertEquals("no time, no move", 1f, UrbanScape.nextAngle(1f, 0, speed, 10f), EPS);
		assertEquals(1.5f, UrbanScape.nextAngle(1f, 250, speed, 10f), EPS);
		assertEquals(2f, UrbanScape.nextAngle(1f, 500, speed, 10f), EPS);

		// Two renderings of 250 ms make the same distance as a single one of 500 ms: the speed does not depend on the rendering time
		float twice = UrbanScape.nextAngle(UrbanScape.nextAngle(1f, 250, speed, 10f), 250, speed, 10f);
		assertEquals(UrbanScape.nextAngle(1f, 500, speed, 10f), twice, EPS);
	}

	@Test
	public void testNextAngle_stepIsLimited() {
		System.out.println("***** Test UrbanScape : testNextAngle_stepIsLimited *****");

		// A rendering exceptionally long (10 s) does not make the camera jump
		assertEquals(1.1f, UrbanScape.nextAngle(1f, 10000, 2f, 0.1f), EPS);
	}

	@Test
	public void testFlight_defaultSpeedIsNotLimitedForARenderingOf400ms() {
		System.out.println("***** Test UrbanScape : testFlight_defaultSpeedIsNotLimitedForARenderingOf400ms *****");

		float speed = rad(UrbanScape.FLIGHT_SPEED);
		float maxStep = rad(UrbanScape.FLIGHT_MAX_STEP);

		// Renderings of 400 ms (usual) or even 600 ms are followed at the requested speed...
		assertEquals(0.4f * speed, UrbanScape.nextAngle(0, 400, speed, maxStep), EPS);
		assertEquals(0.6f * speed, UrbanScape.nextAngle(0, 600, speed, maxStep), EPS);
		// ...and each image moves the camera by a few degrees only, which is what keeps the movement smooth
		assertTrue(Math.toDegrees(UrbanScape.nextAngle(0, 400, speed, maxStep)) < 6);
		// A lap takes 30 seconds
		assertEquals(30f, 360f / UrbanScape.FLIGHT_SPEED, 1e-3f);
	}
}
