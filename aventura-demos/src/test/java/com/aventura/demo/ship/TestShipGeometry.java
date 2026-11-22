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
package com.aventura.demo.ship;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.World;
import com.aventura.model.world.triangle.Triangle;

/**
 * Tests of the geometry of the StarshipInterior demo: the paths, the shapes swept along them (which side of their
 * triangles is seen), the outlines, where a visitor can walk, and the path of the visit. Without rendering.
 *
 * @since Nov 2026
 */
public class TestShipGeometry {

	private static final float EPSILON = 1e-4f;
	private static Ship ship;

	@BeforeClass
	public static void createShip() {
		ship = new Ship(new ShipTextures());
		World world = new World("ship");
		world.addElement(ship);
		world.build();
	}

	// ***** PathSegment *****

	@Test
	public void testLine_pointsTangentLeftAndNearest() {
		System.out.println("***** Test ship geometry : a straight PathSegment *****");
		PathSegment line = PathSegment.line(1, 2, 1, 12);
		assertEquals(10, line.getLength(), EPSILON);
		assertEquals(7, line.point(5).getY(), EPSILON);
		assertEquals("along +Y", 1, line.tangent(3).getY(), EPSILON);
		assertEquals("the left of +Y is -X", -1, line.left(3).getX(), EPSILON);
		assertEquals(4, line.nearestAbscissa(5, 6), EPSILON);
		assertEquals("kept on the segment", 10, line.nearestAbscissa(0, 50), EPSILON);
		assertEquals("2 m to the left", 2, line.lateralOffset(-1, 6), EPSILON);
		assertEquals(0, line.curvature(), 0);
	}

	@Test
	public void testArc_pointsTangentCurvatureAndNearest() {
		System.out.println("***** Test ship geometry : a curved PathSegment *****");
		// A half circle of radius 12 around (20, 0), counter-clockwise from (20, -12) to (20, 12): the east bend
		PathSegment arc = PathSegment.arc(20, 0, 12, -90, 180);
		assertEquals((float) (Math.PI * 12), arc.getLength(), EPSILON);
		assertPoint(20, -12, arc.start());
		assertPoint(20, 12, arc.end());
		assertPoint(32, 0, arc.point(arc.getLength() / 2));
		assertEquals("eastwards at the start", 1, arc.tangent(0).getX(), EPSILON);
		assertEquals("northwards in the middle", 1, arc.tangent(arc.getLength() / 2).getY(), EPSILON);
		assertEquals("turning left: the left points to the center", -1, arc.left(arc.getLength() / 2).getX(), EPSILON);
		assertEquals(1 / 12f, arc.curvature(), EPSILON);
		assertEquals(arc.getLength() / 2, arc.nearestAbscissa(40, 0), EPSILON);
		assertEquals("1 m inside the bend", 1, arc.lateralOffset(31, 0), EPSILON);
		// Clockwise: the same half circle the other way
		PathSegment reversed = arc.reversed();
		assertPoint(20, 12, reversed.start());
		assertEquals(-1 / 12f, reversed.curvature(), EPSILON);
		assertEquals("1 m to the right, outside", -1, reversed.lateralOffset(31, 0), EPSILON);
		// A part
		PathSegment quarter = arc.part(0, arc.getLength() / 2);
		assertPoint(32, 0, quarter.end());
	}

	// ***** Outlines *****

	@Test
	public void testOutlines_turnCounterClockwise_insetKeepsTheFloor() {
		System.out.println("***** Test ship geometry : outlines *****");
		float[][] corridor = ShipSections.corridor();
		assertTrue("counter-clockwise", SweptSection.signedArea(corridor) > 0);
		assertTrue(SweptSection.signedArea(ShipSections.door(0)) > 0);
		assertTrue(SweptSection.signedArea(ShipSections.circle(1, 2, 0.1f, 10)) > 0);
		// Inset: every edge moved inwards by the depth (the right wall, at lateral 1.3, moves to 1.2)
		float[][] inset = SectionFrame.inset(corridor, 0.1f);
		assertEquals(ShipSections.CORRIDOR_WIDTH / 2 - 0.1f, inset[1][0], EPSILON);
		assertEquals(ShipSections.CORRIDOR_HEIGHT - 0.1f, inset[3][1], EPSILON);
		// Above the floor: the points of the floor stay on it
		float[][] rib = ShipSections.insetAboveFloor(corridor, 0.1f);
		assertEquals(0, rib[0][1], EPSILON);
		assertEquals(0, rib[7][1], EPSILON);
		assertEquals(rib[1][0], ShipSections.CORRIDOR_WIDTH / 2 - 0.1f, EPSILON);
	}

	// ***** Which side of a swept shape is seen *****

	@Test
	public void testCorridorWalls_seenFromInside() {
		System.out.println("***** Test ship geometry : the walls of a corridor are seen from inside *****");
		for (Corridor corridor : ship.getCorridors()) {
			PathSegment path = corridor.getPath();
			SweptSection walls = (SweptSection) corridor.getSubElements().get(1);
			for (Triangle t : walls.getTriangles()) {
				// The normal of a triangle points towards the middle of the corridor (at mid height)
				Vector4 center = centerOf(t);
				float s = path.nearestAbscissa(center.getX(), center.getY());
				Vector4 axis = path.point(s);
				Vector3 towardsAxis = new Vector3(axis.getX() - center.getX(), axis.getY() - center.getY(), ShipSections.CORRIDOR_HEIGHT / 2 - center.getZ());
				assertTrue(corridor.getName() + ": a wall seen from inside", normalOf(t).dot(towardsAxis) > 0);
			}
		}
	}

	@Test
	public void testPipes_seenFromOutside() {
		System.out.println("***** Test ship geometry : the pipes are seen from outside *****");
		Corridor corridor = ship.getCorridors().get(1); // The east bend
		PathSegment path = corridor.getPath();
		float[] pipe = Corridor.PIPES[0];
		SweptSection section = (SweptSection) corridor.getSubElements().get(3).getSubElements().get(0);
		for (Triangle t : section.getTriangles()) {
			Vector4 center = centerOf(t);
			float s = path.nearestAbscissa(center.getX(), center.getY());
			Vector4 axis = path.point(s);
			Vector3 left = path.left(s);
			Vector4 pipeAxis = new Vector4(axis.getX() + pipe[0] * left.getX(), axis.getY() + pipe[0] * left.getY(), pipe[1], 1);
			Vector3 outwards = new Vector3(pipeAxis, center);
			assertTrue("a pipe seen from outside", normalOf(t).dot(outwards) > 0);
		}
	}

	@Test
	public void testFloors_seenFromAbove_ceilingsFromBelow() {
		System.out.println("***** Test ship geometry : floors seen from above, ceilings from below *****");
		for (Hub hub : ship.getHubs()) {
			for (Triangle t : hub.getSubElements().get(0).getTriangles()) assertTrue(normalOf(t).getZ() > 0.99f);
			for (Triangle t : hub.getSubElements().get(1).getTriangles()) assertTrue(normalOf(t).getZ() < -0.99f);
		}
		for (Corridor corridor : ship.getCorridors()) {
			for (Triangle t : corridor.getSubElements().get(0).getTriangles()) assertTrue(normalOf(t).getZ() > 0.99f);
		}
	}

	// ***** Where a visitor can walk *****

	@Test
	public void testKeepInside() {
		System.out.println("***** Test ship geometry : where a visitor can walk *****");
		Vector4 inCorridor = new Vector4(10, -12.3f, 1.65f, 1);
		assertPoint(10, -12.3f, ship.keepInside(inCorridor));
		// Into the wall of the south corridor: back to the limit of the walk
		assertPoint(10, -12 - Ship.CORRIDOR_WALK_HALF_WIDTH, ship.keepInside(new Vector4(10, -14, 1.65f, 1)));
		// Into the wall of the east bend, outside: back on its radius + the half width
		Vector4 bend = ship.keepInside(new Vector4(35, 0, 1.65f, 1));
		assertPoint(20 + Ship.LOOP_HALF_WIDTH + Ship.CORRIDOR_WALK_HALF_WIDTH, 0, bend);
		// In the control room
		assertPoint(3, 2, ship.keepInside(new Vector4(3, 2, 1.65f, 1)));
		// In the doorway of the room, near the frame: kept away from it
		Vector4 door = ship.keepInside(new Vector4(0.75f, ControlRoom.SIZE / 2, 1.65f, 1));
		assertEquals(Ship.DOOR_WALK_HALF_WIDTH, door.getX(), EPSILON);
		assertEquals("the altitude is kept", 1.65f, door.getZ(), 0);
	}

	// ***** The visit *****

	@Test
	public void testTour_closedWithoutCorners() {
		System.out.println("***** Test ship geometry : the visit is a closed path without corners, but at its stop *****");
		ShipTour tour = new ShipTour();
		int n = tour.getSegments().size();
		float length = 0;
		for (int k = 0; k < n; k++) {
			PathSegment a = tour.getSegments().get(k), b = tour.getSegments().get((k + 1) % n);
			assertPoint("segment " + k + " ends where the next starts", b.start().getX(), b.start().getY(), a.end(), 1e-3f);
			Vector3 ta = a.tangent(a.getLength()), tb = b.tangent(0);
			boolean stop = Math.abs(tour.startOf((k + 1) % n) - tour.getNorthDoorStop().getDistance()) < 1e-3f;
			if (!stop) assertEquals("segment " + k + ": same direction", 1, ta.dot(tb), 1e-4f);
			length += a.getLength();
		}
		assertEquals(length, tour.getLength(), 1e-3f);
		assertEquals("the visit loops", tour.eye(10).getX(), tour.eye(10 + tour.getLength()).getX(), 1e-3f);
	}

	@Test
	public void testTour_staysInside() {
		System.out.println("***** Test ship geometry : the visit stays in the corridors and the rooms *****");
		ShipTour tour = new ShipTour();
		for (float d = 0; d < tour.getLength(); d += 0.5f) {
			Vector4 eye = tour.eye(d);
			assertEquals(ShipTour.EYE_HEIGHT, eye.getZ(), 0);
			Vector4 kept = ship.keepInside(eye);
			assertPoint("at " + d + " m", eye.getX(), eye.getY(), kept, 1e-3f);
			Vector4 target = tour.target(d);
			assertTrue("looks ahead, not at its feet", new Vector3(eye, target).length() > 1);
		}
	}

	@Test
	public void testTour_stopInFrontOfTheNorthDoor() {
		System.out.println("***** Test ship geometry : the stop of the visit in front of the north door *****");
		ShipTour tour = new ShipTour();
		ShipTour.Stop stop = tour.getNorthDoorStop();
		// In the north junction, 2 to 3 m from the door
		Vector4 eye = tour.eye(stop.getDistance());
		Vector4 door = new Vector4(0, Ship.LOOP_HALF_WIDTH + Hub.SIZE / 2, eye.getZ(), 1);
		float distance = new Vector3(eye, door).length();
		assertTrue("near the door: " + distance, distance > 1.5f && distance < 3);
		assertEquals("in its axis", 0, eye.getX(), 1e-4f);
		// The timeline: the look turns to the door, the door opens, the look comes back
		float turn = ShipTour.Stop.TURN_SECONDS, open = ShipTour.Stop.OPEN_SECONDS;
		assertEquals(0, stop.lookWeight(0), 0);
		assertEquals(1, stop.lookWeight(turn + open), 1e-6f);
		assertEquals(0, stop.lookWeight(ShipTour.Stop.DURATION), 1e-6f);
		assertEquals("closed while the camera turns", 0, stop.doorOpening(turn), 0);
		assertEquals("open before the look comes back", 1, stop.doorOpening(turn + open), 1e-6f);
		assertEquals(1, stop.doorClosing(0), 0);
		assertEquals(0, stop.doorClosing(open), 1e-6f);
		// The visitor slows down near the stop, and walks at full speed far from it
		assertEquals(ShipTour.Stop.MIN_SPEED_FACTOR, tour.speedFactor(stop.getDistance()), 1e-6f);
		assertTrue(tour.speedFactor(stop.getDistance() - 1) < 1);
		assertEquals(1, tour.speedFactor(stop.getDistance() + ShipTour.Stop.EASE_DISTANCE), 1e-6f);
		assertEquals(1, tour.speedFactor(0), 1e-6f);
		// Arriving: looking ahead, westwards, not round the corner
		Vector3 arriving = new Vector3(tour.eye(stop.getDistance() - 1), tour.target(stop.getDistance() - 1)).normalize();
		assertTrue("westwards", arriving.getX() < -0.95f);
		// The look: westwards, then to the door (north), then back past the west to the south, without a jump
		Vector3 previous = null;
		float maxStep = 0;
		for (float t = 0; t <= ShipTour.Stop.DURATION; t += 0.05f) {
			Vector3 sight = new Vector3(eye, tour.stopTarget(t)).normalize();
			if (previous != null) maxStep = Math.max(maxStep, (float) Math.acos(Math.min(1, previous.dot(sight))));
			previous = sight;
			assertEquals("upright", 0, tour.stopUp(t).dot(new Vector4(sight.getX(), sight.getY(), sight.getZ(), 0)), 1e-5f);
			assertTrue("never eastwards: no turn on himself", sight.getX() < 0.05f);
		}
		assertTrue("no jump: " + Math.toDegrees(maxStep), Math.toDegrees(maxStep) < 10);
		assertTrue("at the start, westwards", new Vector3(eye, tour.stopTarget(0)).normalize().getX() < -0.95f);
		assertTrue("then at the door, northwards", new Vector3(eye, tour.stopTarget(ShipTour.Stop.DURATION / 2)).normalize().getY() > 0.95f);
		Vector3 leaving = new Vector3(eye, tour.stopTarget(ShipTour.Stop.DURATION)).normalize();
		assertTrue("at the end, southwards", leaving.getY() < -0.95f);
		// And the visit goes on in the same direction
		Vector3 after = new Vector3(tour.eye(stop.getDistance() + 1e-3f), tour.target(stop.getDistance() + 1e-3f)).normalize();
		assertEquals(1, leaving.dot(after), 1e-3f);
	}

	// ***** Codes on the walls, cabinets *****

	@Test
	public void testDecal_ownTrianglesAndUpright() {
		System.out.println("***** Test ship geometry : a decal on a wall, upright as seen *****");
		PathSegment path = PathSegment.line(0, 0, 10, 0);
		SweptSection walls = new SweptSection("walls", path, ShipSections.corridor(), false, SweptSection.Facing.INSIDE,
				SweptSection.Shading.SHARP_EDGES);
		Texture decal = new Texture(4, 4);
		walls.addDecal(new SweptSection.Decal(1, 4.5f, 6.5f, decal));
		World world = new World("decal");
		world.addElement(walls);
		world.build();
		int withDecal = 0;
		for (Triangle t : walls.getTriangles()) {
			if (t.getTexture() == decal) {
				withDecal++;
				Vector4 c = centerOf(t);
				assertTrue("on the piece of wall", c.getX() > 4.5f && c.getX() < 6.5f && c.getY() > 1.29f);
			}
		}
		assertEquals("its own two triangles: the section got a step at each end", 2, withDecal);
		// The left wall (edge 1), seen from inside (looking towards +Y): the path goes to the right of the picture, the
		// edge goes up. The top left corner of the picture is at the start of the decal, at the top of the wall
		float[] topLeft = walls.pictureToSurface(1, 4.5f, 6.5f, 0, 0);
		assertEquals(4.5f, topLeft[0], EPSILON);
		assertEquals("top of the edge", 1, topLeft[1], EPSILON);
		// The right wall (edge 5): seen from inside (towards -Y), the path goes to the left, the edge goes down
		float[] right = walls.pictureToSurface(5, 4.5f, 6.5f, 0, 0);
		assertEquals(6.5f, right[0], EPSILON);
		assertEquals("the edge starts at the top", 0, right[1], EPSILON);
		// The background of a decal continues the tiled texture: same coordinates as the wall around it
		float[] uv = walls.tileCoordinates(1, 4.5f, 0);
		assertEquals(4.5f / 2f, uv[0], EPSILON);
	}

	@Test
	public void testMarkings_painted() {
		System.out.println("***** Test ship geometry : the codes painted on the walls *****");
		assertTrue("the stencil font is read from the resources", WallMarking.stencilFont().getFamily().toLowerCase().contains("black ops"));
		int decals = 0;
		for (Corridor corridor : ship.getCorridors()) {
			SweptSection walls = (SweptSection) corridor.getSubElements().get(1);
			java.util.Set<Texture> textures = new java.util.HashSet<Texture>();
			for (Triangle t : walls.getTriangles()) {
				if (t.getTexture() != null && t.getTexture().getName().startsWith("marking")) textures.add(t.getTexture());
			}
			decals += textures.size();
		}
		assertEquals("8 codes in the loop, 1 in the shortcut, 1 in the airlock corridor", 10, decals);
	}

	@Test
	public void testCabinets_doNotReachTheFloorWherePeopleWalk() {
		System.out.println("***** Test ship geometry : the cabinets stay within the chamfers *****");
		// No deeper than the chamfers (the low ones stand on the lower chamfer, their front 1 cm further, the Panel)
		assertTrue(WallCabinet.Type.LOW.getDepth() + 0.01f <= ShipSections.CORRIDOR_LOWER_CHAMFER);
		assertTrue(WallCabinet.Type.HIGH.getDepth() + 0.01f <= ShipSections.CORRIDOR_UPPER_CHAMFER);
		// The high ones under the thinnest pipe of the left wall
		float[] pipe = Corridor.PIPES[2];
		assertTrue(WallCabinet.Type.HIGH.bottom + WallCabinet.Type.HIGH.height < pipe[1] - pipe[2]);
		int cabinets = 0;
		for (Corridor corridor : ship.getCorridors()) {
			PathSegment path = corridor.getPath();
			for (com.aventura.model.world.Element e : corridor.getSubElements()) {
				if (!e.getName().startsWith("cabinets")) continue;
				for (com.aventura.model.world.Element cabinet : e.getSubElements()) {
					cabinets++;
					// The corners of its front (the Panel, 1 cm in front of the Box), placed by the transformation of the
					// cabinet (built at the origin, its back on x = 0, its front towards +X)
					WallCabinet wallCabinet = (WallCabinet) cabinet;
					float front = wallCabinet.getType().getDepth() + 0.01f, half = wallCabinet.getWidth() / 2;
					for (float y : new float[] { -half, 0, half }) {
						Vector4 p = cabinet.getTransformation().transform(new Vector4(front, y, 0, 1));
						float lateral = Math.abs(path.lateralOffset(p.getX(), p.getY()));
						assertTrue(corridor.getName() + ": " + lateral,
								lateral >= ShipSections.CORRIDOR_WIDTH / 2 - ShipSections.CORRIDOR_LOWER_CHAMFER - 0.02f);
						assertTrue("beyond the walk", lateral > Ship.CORRIDOR_WALK_HALF_WIDTH);
					}
				}
			}
		}
		assertEquals(15, cabinets);
	}

	// ***** Helpers *****

	private static Vector4 centerOf(Triangle t) {
		Vector4 a = t.getV1().getPos(), b = t.getV2().getPos(), c = t.getV3().getPos();
		return new Vector4((a.getX() + b.getX() + c.getX()) / 3, (a.getY() + b.getY() + c.getY()) / 3, (a.getZ() + b.getZ() + c.getZ()) / 3, 1);
	}

	/** The normal of the triangle given by the order of its vertices, the side the engine sees: (v2 - v1) x (v3 - v1). */
	private static Vector3 normalOf(Triangle t) {
		Vector3 ab = new Vector3(t.getV1().getPos(), t.getV2().getPos()), ac = new Vector3(t.getV1().getPos(), t.getV3().getPos());
		return ab.cross(ac).normalize();
	}

	private static void assertPoint(float x, float y, Vector4 p) {
		assertPoint("", x, y, p, EPSILON);
	}

	private static void assertPoint(String message, float x, float y, Vector4 p, float epsilon) {
		assertEquals(message + " x", x, p.getX(), epsilon);
		assertEquals(message + " y", y, p.getY(), epsilon);
	}
}
