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

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.aventura.math.transform.Rotation;
import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.world.Element;
import com.aventura.model.world.shape.Box;
import com.aventura.model.world.shape.ClosedCylinder;
import com.aventura.model.world.shape.Cylinder;
import com.aventura.model.world.shape.Disc;
import com.aventura.model.world.shape.Torus;

/**
 * The inside of the ship: its plan, and the tree of Elements built from it.
 * <pre>
 *                          (store)                          behind the closed door of N
 *        .-------------------[N]-------------------.        y = +12  north corridor
 *       /                     |                     \
 *      |                  +---+---+                  |
 *      |  west bend       |control|       east bend  |      the shortcut crosses the
 *      |  (R = 12)        | room  |       (R = 12)   |      control room, from the
 *      |                  +---+---+                  |      south junction to the
 *       \                     |                     /       north one
 *        '-------------------[S]-------------------'        y = -12  south corridor
 *                             |
 *                           (hatch)                         y = -19  airlock to the hold
 *       x = -20               x = 0              x = +20
 * </pre>
 * The corridors meet at two junctions ({@link Hub}), N and S. The plan is all in the constants below: the corridors
 * are PathSegments between them, which the walls, the pipes, the lights and the path of the visit all follow.
 * <pre>
 *   ship
 *    ├── corridors (group)   Corridor x 7: south, east bend, north (two parts), west bend, shortcut (two parts)
 *    ├── airlock corridor    Corridor, from the south junction to the hatch of the hold
 *    ├── store               Corridor, behind the closed door of the north junction: a dead end
 *    ├── store end           its end wall, and crates
 *    ├── junctions (group)   Hub x 2
 *    ├── control room        ControlRoom
 *    └── hatch               the round door of the hold, at the end of the airlock corridor, its warning lamp
 * </pre>
 *
 * @since Nov 2026
 */
public class Ship extends Element {

	/** The long corridors run at y = +/- LOOP_HALF_WIDTH, from x = -LOOP_HALF_LENGTH to +LOOP_HALF_LENGTH, then bend. */
	public static final float LOOP_HALF_LENGTH = 20f;
	public static final float LOOP_HALF_WIDTH = 12f;
	/** The airlock corridor goes south from the south junction, this long. */
	public static final float AIRLOCK_CORRIDOR_LENGTH = 5f;
	public static final float HATCH_RADIUS = 0.95f, HATCH_HEIGHT = 1.3f;
	/** The store room behind the closed door of the north junction. */
	public static final float STORE_LENGTH = 3.5f;
	public static final Color[] CRATE_COLORS = { new Color(150, 120, 70), new Color(90, 110, 90), new Color(160, 160, 150) };

	public static final Vector4 SOUTH_HUB = new Vector4(0, -LOOP_HALF_WIDTH, 0, 1);
	public static final Vector4 NORTH_HUB = new Vector4(0, LOOP_HALF_WIDTH, 0, 1);
	public static final Vector4 ROOM_CENTER = new Vector4(0, 0, 0, 1);

	public static final Color HATCH_COLOR = new Color(120, 126, 124);
	/** The warning lamp above the hatch: its color, and the light it emits by itself (see Glow). */
	public static final Color WARNING_LAMP_COLOR = new Color(150, 50, 40);
	public static final Color WARNING_LAMP_GLOW = new Color(255, 60, 40);
	public static final Color HATCH_RIM_COLOR = new Color(210, 168, 50);
	public static final Color WHEEL_COLOR = new Color(178, 62, 46);

	protected final List<Corridor> corridors = new ArrayList<Corridor>();
	protected final List<Hub> hubs = new ArrayList<Hub>();
	protected final ControlRoom room;
	protected Vector4 hatchLightPoint;

	/**
	 * Builds the Elements of the ship (the World still has to be built: see World.build()).
	 *
	 * @param textures the textures of the ship
	 */
	public Ship(ShipTextures textures) {
		super("ship");
		float l = LOOP_HALF_LENGTH, w = LOOP_HALF_WIDTH, h = Hub.SIZE / 2, r = ControlRoom.SIZE / 2;

		// The loop: the south corridor eastwards, the east bend, the north corridor westwards (in two parts, on each side
		// of the north junction), the west bend, and the west part of the south corridor
		Element loop = new Element("corridors");
		addCorridor(loop, "south corridor, east", PathSegment.line(h, -w, l, -w), textures);
		addCorridor(loop, "east bend", PathSegment.arc(l, 0, w, -90, 180), textures);
		addCorridor(loop, "north corridor, east", PathSegment.line(l, w, h, w), textures);
		addCorridor(loop, "north corridor, west", PathSegment.line(-h, w, -l, w), textures);
		addCorridor(loop, "west bend", PathSegment.arc(-l, 0, w, 90, 180), textures);
		addCorridor(loop, "south corridor, west", PathSegment.line(-l, -w, -h, -w), textures);
		// The shortcut, through the control room
		addCorridor(loop, "shortcut, south", PathSegment.line(0, -w + h, 0, -r), textures);
		addCorridor(loop, "shortcut, north", PathSegment.line(0, r, 0, w - h), textures);
		addElement(loop);

		// The airlock corridor, from the south junction to the hatch of the hold
		PathSegment airlock = PathSegment.line(0, -w - h, 0, -w - h - AIRLOCK_CORRIDOR_LENGTH);
		Corridor airlockCorridor = new Corridor("airlock corridor", airlock, textures, false);
		airlockCorridor.setEndClosed(true); // By the wall of the hatch
		corridors.add(airlockCorridor);
		addElement(airlockCorridor);
		addElement(createHatch(airlock.end(), textures));

		// The codes on the walls and the cabinets (the corridors must exist)
		addMarkingsAndCabinets();

		// Behind the closed door of the north junction: a short store room, a dead end with a few crates
		PathSegment store = PathSegment.line(0, w + h, 0, w + h + STORE_LENGTH);
		Corridor storeCorridor = new Corridor("store", store, textures, false);
		storeCorridor.setEndClosed(true);
		corridors.add(storeCorridor);
		addElement(storeCorridor);
		Element storeEnd = new Element("store end");
		storeEnd.addElement(endWall(store.end(), textures));
		storeEnd.addElement(createCrates(store, textures));
		addElement(storeEnd);

		// The junctions: east, north, west, south sides
		Element junctions = new Element("junctions");
		Hub south = new Hub("south junction", SOUTH_HUB,
				new Hub.Side[] { Hub.Side.PASSAGE, Hub.Side.OPEN_DOOR, Hub.Side.PASSAGE, Hub.Side.OPEN_DOOR }, textures);
		Hub north = new Hub("north junction", NORTH_HUB,
				new Hub.Side[] { Hub.Side.PASSAGE, Hub.Side.CLOSED_DOOR, Hub.Side.PASSAGE, Hub.Side.OPEN_DOOR }, textures);
		hubs.add(south);
		hubs.add(north);
		junctions.addElement(south);
		junctions.addElement(north);
		addElement(junctions);

		storeCorridor.setDoor(north.getDoors().get(0)); // The closed door of the north junction leads to the store

		room = new ControlRoom("control room", ROOM_CENTER, textures);
		addElement(room);
	}

	/**
	 * The codes painted on the walls and the cabinets, between the ribs: a few in each corridor and each bend, never at
	 * the same places in two of them, to break the monotony of the long corridors. Slot k is between rib k - 1 and rib
	 * k (6 slots in a straight corridor, 14 in a bend); LEFT is the inside of the loop (the side of the pipes).
	 */
	private void addMarkingsAndCabinets() {
		final int L = Corridor.LEFT, R = Corridor.RIGHT;
		final WallCabinet.Type LOW = WallCabinet.Type.LOW, HIGH = WallCabinet.Type.HIGH;
		corridor("south corridor, east").addMarking(1, R, "S-12", "DECK C - SECTION 2", WallMarking.YELLOW)
				.addCabinet(3, L, LOW, 1.7f, 1).addCabinet(4, R, HIGH, 1.3f, 2).addCabinet(6, R, LOW, 1.1f, 3);
		corridor("east bend").addMarking(2, L, "E-20", "DECK C - EAST RING", WallMarking.BLACK)
				.addCabinet(5, R, LOW, 1.8f, 4).addCabinet(6, R, LOW, 1.2f, 5).addCabinet(9, L, HIGH, 1.5f, 6)
				.addMarking(12, R, "E-28", "DECK C - EAST RING", WallMarking.YELLOW);
		corridor("north corridor, east").addCabinet(2, L, HIGH, 1.4f, 7).addMarking(4, R, "N-07", "DECK C - SECTION 5", WallMarking.YELLOW)
				.addCabinet(5, L, LOW, 1.6f, 8);
		corridor("north corridor, west").addCabinet(1, R, LOW, 1.3f, 9).addMarking(3, L, "N-15", "DECK C - SECTION 6", WallMarking.BLACK)
				.addCabinet(5, R, HIGH, 1.6f, 10);
		corridor("west bend").addCabinet(3, L, LOW, 1.5f, 11).addMarking(5, R, "W-33", "DECK C - WEST RING", WallMarking.YELLOW)
				.addCabinet(8, R, HIGH, 1.2f, 12).addCabinet(9, R, LOW, 1.7f, 13).addMarking(13, L, "W-39", "DECK C - WEST RING", WallMarking.BLACK);
		corridor("south corridor, west").addMarking(2, L, "S-04", "DECK C - SECTION 1", WallMarking.BLACK)
				.addCabinet(4, R, LOW, 1.4f, 14).addCabinet(5, L, HIGH, 1.2f, 15);
		corridor("shortcut, south").addMarking(1, L, "CTRL", "CONTROL ROOM", WallMarking.RED);
		corridor("airlock corridor").addMarking(1, R, "HOLD 2", "AIRLOCK - AUTHORIZED ONLY", WallMarking.RED);
	}

	/** The corridor of the given name. */
	private Corridor corridor(String name) {
		for (Corridor corridor : corridors) {
			if (corridor.getName().startsWith(name)) return corridor;
		}
		throw new IllegalArgumentException("No corridor " + name);
	}

	private void addCorridor(Element group, String name, PathSegment path, ShipTextures textures) {
		Corridor corridor = new Corridor(name, path, textures, false);
		corridors.add(corridor);
		group.addElement(corridor);
	}

	/**
	 * The end of the airlock corridor: a wall (a Panel without doorway) and, on it, the round hatch of the hold. The
	 * hatch is made of built-in shapes: a Disc for the door, a Torus for its rim and another for its wheel, Cylinders for
	 * the spokes of the wheel and for the hinges. They are built around the Z axis and turned to face the corridor.
	 */
	private Element createHatch(Vector4 end, ShipTextures textures) {
		Element hatch = new Element("hatch");
		hatch.addElement(endWall(end, textures));

		// The parts of the hatch, around the Z axis (the axis of the door), then turned so that Z points north (+Y): a
		// rotation of -90 degrees around X
		Element door = new Element("hatch door");
		Disc leaf = new Disc(HATCH_RADIUS, 16, textures.paintedMetal);
		leaf.setColor(HATCH_COLOR);
		leaf.setTransformation(new Translation(new Vector3(0, 0, 0.04f)));
		door.addElement(leaf);
		Torus rim = new Torus(HATCH_RADIUS + 0.05f, 0.08f, 24, 6, textures.hazardStripes);
		rim.setColor(HATCH_RIM_COLOR);
		door.addElement(rim);
		Torus wheel = new Torus(0.38f, 0.035f, 16, 5);
		wheel.setColor(WHEEL_COLOR);
		wheel.setTransformation(new Translation(new Vector3(0, 0, 0.16f)));
		door.addElement(wheel);
		for (int k = 0; k < 3; k++) {
			// A spoke: a Cylinder along Z, laid in the plane of the wheel (around X), then turned around the axis
			Cylinder spoke = new Cylinder(0.76f, 0.02f, 4);
			spoke.setColor(WHEEL_COLOR);
			spoke.setTransformation(new Rotation((float) Math.PI / 2, Vector3.xAxis()));
			spoke.combineTransformation(new Rotation((float) Math.PI * k / 3, Vector3.zAxis()));
			spoke.combineTransformation(new Translation(new Vector3(0, 0, 0.16f)));
			door.addElement(spoke);
		}
		Cylinder hub = new Cylinder(0.14f, 0.06f, 8);
		hub.setColor(WHEEL_COLOR);
		hub.setTransformation(new Translation(new Vector3(0, 0, 0.11f)));
		door.addElement(hub);
		door.setTransformation(new Rotation((float) -Math.PI / 2, Vector3.xAxis()));
		door.combineTransformation(new Translation(new Vector3(end.getX(), end.getY() + 0.02f, HATCH_HEIGHT)));
		hatch.addElement(door);

		hatchLightPoint = new Vector4(end.getX(), end.getY() + 1.2f, ShipSections.CORRIDOR_HEIGHT - 0.35f, 1);
		// The warning lamp, hanging from the ceiling on a rod, just above the red light (which points down, away from it):
		// a small cylinder that glows red (see Glow)
		float lampTop = ShipSections.CORRIDOR_HEIGHT, lampBottom = hatchLightPoint.getZ() + 0.06f;
		ClosedCylinder lamp = new ClosedCylinder(0.12f, 0.07f, 6);
		lamp.setName("warning lamp");
		lamp.setColor(WARNING_LAMP_COLOR);
		Glow.lamp(lamp, WARNING_LAMP_GLOW);
		lamp.setTransformation(new Translation(new Vector3(hatchLightPoint.getX(), hatchLightPoint.getY(), lampBottom + 0.06f)));
		hatch.addElement(lamp);
		Cylinder rod = new Cylinder(lampTop - lampBottom - 0.12f, 0.012f, 4);
		rod.setColor(HATCH_COLOR);
		rod.setTransformation(new Translation(new Vector3(hatchLightPoint.getX(), hatchLightPoint.getY(), (lampTop + lampBottom + 0.12f) / 2)));
		hatch.addElement(rod);
		return hatch;
	}

	// ****************************************************************************************************************
	// ***** Where a visitor can walk
	// ****************************************************************************************************************

	/** A visitor stays this far from the middle of a corridor, of a doorway, and this far from the walls of the rooms. */
	public static final float CORRIDOR_WALK_HALF_WIDTH = 0.8f;
	public static final float DOOR_WALK_HALF_WIDTH = ShipSections.DOOR_WIDTH / 2 - 0.25f;
	public static final float ROOM_WALL_CLEARANCE = 0.6f;
	/** The walk along a corridor goes on beyond its ends, through the doorway, into the room or the junction it opens on. */
	public static final float CORRIDOR_WALK_EXTENSION = 1f;

	/**
	 * The point where a visitor who wanted to go to (x, y) can actually be: the same point if it is in a corridor, a
	 * junction or the control room, otherwise the nearest point that is. Then, in a doorway, the visitor is kept away
	 * from the frame. The altitude is kept.
	 */
	public Vector4 keepInside(Vector4 wanted) {
		float x = wanted.getX(), y = wanted.getY();
		float bestDistance = Float.MAX_VALUE;
		float[] best = null;
		// The corridors: within CORRIDOR_WALK_HALF_WIDTH of their middle line, and a little beyond their ends
		for (Corridor corridor : corridors) {
			if (corridor.getDoor() != null && corridor.getDoor().getOpening() < 0.6f) continue; // Behind a closed door
			PathSegment path = corridor.getPath();
			float s = path.nearestAbscissa(x, y);
			float lateral = Math.max(-CORRIDOR_WALK_HALF_WIDTH, Math.min(CORRIDOR_WALK_HALF_WIDTH, path.lateralOffset(x, y)));
			Vector4 p = path.point(s);
			Vector3 left = path.left(s), tangent = path.tangent(s);
			// Beyond its ends, the walk goes on for CORRIDOR_WALK_EXTENSION: through the doorway, into the next room
			float along = (x - p.getX()) * tangent.getX() + (y - p.getY()) * tangent.getY();
			if (s <= 0) along = clamp(along, -CORRIDOR_WALK_EXTENSION, 0);
			else if (s >= path.getLength()) along = clamp(along, 0, corridor.isEndClosed() ? 0 : CORRIDOR_WALK_EXTENSION);
			else along = 0;
			float[] candidate = { p.getX() + along * tangent.getX() + lateral * left.getX(), p.getY() + along * tangent.getY() + lateral * left.getY() };
			float distance = (float) Math.hypot(candidate[0] - x, candidate[1] - y);
			if (distance < bestDistance) {
				bestDistance = distance;
				best = candidate;
			}
		}
		// The junctions and the room: squares, smaller than the walls by a clearance
		List<float[]> squares = new ArrayList<float[]>();
		for (Hub hub : hubs) squares.add(new float[] { hub.getCenter().getX(), hub.getCenter().getY(), Hub.SIZE / 2 - ROOM_WALL_CLEARANCE });
		squares.add(new float[] { ROOM_CENTER.getX(), ROOM_CENTER.getY(), ControlRoom.SIZE / 2 - ROOM_WALL_CLEARANCE });
		for (float[] square : squares) {
			float[] candidate = { clamp(x, square[0] - square[2], square[0] + square[2]), clamp(y, square[1] - square[2], square[1] + square[2]) };
			float distance = (float) Math.hypot(candidate[0] - x, candidate[1] - y);
			if (distance < bestDistance) {
				bestDistance = distance;
				best = candidate;
			}
		}
		// In a doorway (within the thickness of the frame and a little more), away from the frame
		for (AirlockDoor door : getDoors()) {
			Vector4 c = door.getCenter();
			Vector3 across = door.getAcross();
			float dx = best[0] - c.getX(), dy = best[1] - c.getY();
			float side = dx * across.getX() + dy * across.getY();               // Along the frame
			float through = (float) Math.hypot(dx - side * across.getX(), dy - side * across.getY()); // Across it
			if (through < AirlockDoor.FRAME_THICKNESS && Math.abs(side) < ShipSections.CORRIDOR_WIDTH / 2) {
				float kept = clamp(side, -DOOR_WALK_HALF_WIDTH, DOOR_WALK_HALF_WIDTH);
				best[0] += (kept - side) * across.getX();
				best[1] += (kept - side) * across.getY();
			}
		}
		return new Vector4(best[0], best[1], wanted.getZ(), 1);
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}

	/**
	 * The wall that closes a corridor going south, at its end. It is seen from the north, from inside the corridor: its
	 * axes are -X (along the bottom edge) and Z (up), and (-X) x Z = +Y.
	 */
	private static Panel endWall(Vector4 end, ShipTextures textures) {
		float halfWidth = ShipSections.CORRIDOR_WIDTH / 2;
		// For a corridor going north (the store), the wall is seen from the south: +X and Z, and X x Z = -Y
		boolean north = end.getY() > 0;
		Vector4 corner = new Vector4(end.getX() + (north ? -halfWidth : halfWidth), end.getY(), 0, 1);
		Panel wall = new Panel("end wall", corner, north ? Vector3.xAxis() : Vector3.xOppAxis(), Vector3.zAxis(), ShipSections.CORRIDOR_WIDTH,
				ShipSections.CORRIDOR_HEIGHT, null, textures.hullPlates, ShipTextures.PLATE_TILE);
		wall.setColor(Corridor.WALL_COLOR);
		return wall;
	}

	/** A few crates piled at the end of the store: Boxes of several sizes and colors, slightly turned. */
	private static Element createCrates(PathSegment store, ShipTextures textures) {
		Element crates = new Element("crates");
		// x, distance from the end wall, size, angle (degrees), on top of the previous one
		float[][] places = { { 0.55f, 0.5f, 0.8f, 5, 0 }, { -0.5f, 0.55f, 0.7f, -8, 0 }, { 0.5f, 0.55f, 0.55f, 20, 1 }, { 0.1f, 1.5f, 0.5f, 35, 0 } };
		Vector4 end = store.end();
		float base = 0;
		for (int k = 0; k < places.length; k++) {
			float[] p = places[k];
			Box crate = new Box(p[2], p[2], p[2], textures.paintedMetal, "crate");
			crate.setColor(CRATE_COLORS[k % CRATE_COLORS.length]);
			float z = p[4] > 0 ? base + p[2] / 2 : p[2] / 2;
			if (p[4] == 0) base = p[2];
			crate.setTransformation(new Rotation((float) Math.toRadians(p[3]), Vector3.zAxis()));
			crate.combineTransformation(new Translation(new Vector3(end.getX() + p[0], end.getY() - p[1], z)));
			crates.addElement(crate);
		}
		return crates;
	}

	public List<Corridor> getCorridors() {
		return corridors;
	}

	public List<Hub> getHubs() {
		return hubs;
	}

	public ControlRoom getRoom() {
		return room;
	}

	/** Where the warning light above the hatch of the hold hangs. */
	public Vector4 getHatchLightPoint() {
		return hatchLightPoint;
	}

	/** The airlock doors of the ship: those of the junctions and of the control room. */
	public List<AirlockDoor> getDoors() {
		List<AirlockDoor> doors = new ArrayList<AirlockDoor>();
		for (Hub hub : hubs) doors.addAll(hub.getDoors());
		doors.addAll(room.getDoors());
		return doors;
	}
}
