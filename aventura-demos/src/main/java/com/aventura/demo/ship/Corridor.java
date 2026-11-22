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
import com.aventura.model.texture.Texture;
import com.aventura.model.world.Element;
import com.aventura.model.world.shape.Box;

/**
 * A corridor of the ship along a {@link PathSegment}, straight or curved: an Element that groups everything that runs
 * along it.
 * <pre>
 *   Corridor
 *    ├── floor            SweptSection: the grating, one edge of the octagon
 *    ├── walls            SweptSection: the 7 other edges, plates of the hull, seen from inside
 *    ├── ribs (group)     SectionFrame every RIB_SPACING meters, across the corridor
 *    ├── pipes (group)    SweptSection x 3: circles along the upper left corner, seen from outside
 *    ├── cable tray       SweptSection: an open U along the upper right corner
 *    ├── fixtures (group) Box under the ceiling every LIGHT_SPACING meters, where the lights hang
 *    └── cabinets (group) WallCabinet against the walls, between the ribs (added by the Ship, see addCabinet())
 * </pre>
 * Codes can be painted on the walls between the ribs ({@link #addMarking}): decals of the walls (see WallMarking).
 * The same few shapes, given the same path, make a straight corridor or a curved one: only the PathSegment changes.
 * The corridor also tells where its lights are ({@link #getLightPoints()}): the ship creates them (see ShipLights).
 *
 * @since Nov 2026
 */
public class Corridor extends Element {

	/** A rib of the hull every RIB_SPACING meters, the first one half of it from the start. */
	public static final float RIB_SPACING = 2.5f;
	public static final float RIB_DEPTH = 0.1f, RIB_THICKNESS = 0.22f;
	/** A ceiling light every LIGHT_SPACING meters, between two ribs. */
	public static final float LIGHT_SPACING = 5f;
	/** The lights hang this far below the ceiling, under their fixture. */
	public static final float LIGHT_DROP = 0.18f;

	/** Colors of the parts: they tint the textures (see ShipTextures). */
	public static final Color WALL_COLOR = new Color(196, 192, 182);
	public static final Color KICK_PLATE_COLOR = new Color(120, 118, 112);
	public static final Color UPPER_CHAMFER_COLOR = new Color(150, 152, 150);
	public static final Color CEILING_COLOR = new Color(150, 150, 146);
	public static final Color FLOOR_COLOR = new Color(150, 146, 138);
	public static final Color RIB_COLOR = new Color(92, 104, 96);
	public static final Color FIXTURE_COLOR = new Color(255, 246, 225);
	public static final Color TRAY_COLOR = new Color(110, 112, 116);
	public static final Color[] PIPE_COLORS = { new Color(196, 126, 80), new Color(170, 172, 176), new Color(178, 62, 46) };
	/** The pipes along the upper left corner: (lateral, height, radius). */
	static final float[][] PIPES = { { 1.0f, 2.25f, 0.07f }, { 0.77f, 2.47f, 0.05f }, { 1.19f, 2.0f, 0.035f } };
	static final int PIPE_SEGMENTS = 10;

	protected final PathSegment path;
	protected final List<Vector4> lightPoints = new ArrayList<Vector4>();
	protected boolean endClosed = false;
	protected SweptSection walls;
	protected final ShipTextures textures;
	protected Element fittings;      // The cabinets, created with the first one
	protected AirlockDoor door;

	/**
	 * @param name     name of the corridor
	 * @param path     the middle of its floor
	 * @param textures the textures of the ship
	 * @param ribsAtEnds true to put a rib at each end too (where the corridor meets another one, the rib hides the joint)
	 */
	public Corridor(String name, PathSegment path, ShipTextures textures, boolean ribsAtEnds) {
		super(name);
		this.path = path;
		this.textures = textures;
		float[][] section = ShipSections.corridor();

		// The floor: the edge from o7 to o0 alone, an open profile (its order says it is seen from above)
		SweptSection floor = new SweptSection("floor", path, new float[][] { section[7], section[0] }, false,
				SweptSection.Facing.INSIDE, SweptSection.Shading.SHARP_EDGES);
		floor.setTexture(textures.grating, ShipTextures.GRATING_TILE, ShipTextures.GRATING_TILE);
		floor.setColor(FLOOR_COLOR);
		addElement(floor);

		// The walls: the 7 other edges, from o0 up the left side, across the ceiling and down the right side to o7
		walls = new SweptSection("walls", path, section, false, SweptSection.Facing.INSIDE, SweptSection.Shading.SHARP_EDGES);
		Texture plates = textures.hullPlates;
		walls.setTextures(new Texture[] { plates, plates, plates, textures.ceilingPlates, plates, plates, plates },
				ShipTextures.PLATE_TILE, ShipTextures.PLATE_TILE);
		walls.setEdgeColors(new Color[] { KICK_PLATE_COLOR, null, UPPER_CHAMFER_COLOR, CEILING_COLOR, UPPER_CHAMFER_COLOR, null, KICK_PLATE_COLOR });
		walls.setColor(WALL_COLOR);
		addElement(walls);

		addElement(createRibs(section, textures, ribsAtEnds));
		addElement(createPipes(textures));

		// The cable tray, along the upper right corner
		SweptSection tray = new SweptSection("cable tray", path, ShipSections.tray(-0.9f, 2.2f, 0.3f, 0.08f), false,
				SweptSection.Facing.OUTSIDE, SweptSection.Shading.SHARP_EDGES);
		tray.setTexture(textures.paintedMetal, 0.5f, 0.5f);
		tray.setColor(TRAY_COLOR);
		addElement(tray);

		addElement(createFixtures());
	}

	/** The ribs: SectionFrames across the corridor, standing on the floor, every RIB_SPACING meters. */
	private Element createRibs(float[][] section, ShipTextures textures, boolean atEnds) {
		Element ribs = new Element("ribs");
		ribs.setColor(RIB_COLOR);
		float[][] inner = ShipSections.insetAboveFloor(section, RIB_DEPTH);
		List<Float> places = new ArrayList<Float>();
		int count = Math.max(1, Math.round(path.getLength() / RIB_SPACING));
		float spacing = path.getLength() / count; // The ribs are spread evenly over the length of the corridor
		for (int k = 0; k < count; k++) places.add(spacing * (k + 0.5f));
		if (atEnds) {
			places.add(0, RIB_THICKNESS / 2);
			places.add(path.getLength() - RIB_THICKNESS / 2);
		}
		for (float s : places) {
			SectionFrame rib = new SectionFrame("rib", path.point(s), path.tangent(s), section, inner, RIB_THICKNESS, textures.paintedMetal, 0.5f);
			rib.setOpen(true); // No rib across the floor (see ShipSections.FLOOR_EDGE: the last edge of the octagon)
			ribs.addElement(rib);
		}
		return ribs;
	}

	// ****************************************************************************************************************
	// ***** Markings and cabinets, between the ribs
	// ****************************************************************************************************************

	/** The walls: +1 the left one (edge o1-o2 of the octagon), -1 the right one (edge o5-o6), in the direction of the path. */
	public static final int LEFT = 1, RIGHT = -1;
	/** The width of a marking on the wall, along the corridor. */
	public static final float MARKING_WIDTH = 1.9f;

	/** The number of places between two ribs: slots 1 to getNbSlots() (slot k is between rib k - 1 and rib k). */
	public int getNbSlots() {
		return Math.max(1, Math.round(path.getLength() / RIB_SPACING)) - 1;
	}

	/** The distance along the path of the middle of the slot k, between two ribs (see createRibs()). */
	public float slotDistance(int slot) {
		if (slot < 1 || slot > getNbSlots()) throw new IllegalArgumentException("No slot " + slot + " in " + getName());
		return path.getLength() / (getNbSlots() + 1) * slot;
	}

	/**
	 * Paints a code on a wall, in the slot k (see WallMarking). To be called before the World is built.
	 *
	 * @param side    LEFT or RIGHT
	 * @param code    the code, in large letters
	 * @param caption a smaller line under it, or null
	 */
	public Corridor addMarking(int slot, int side, String code, String caption, Color paint) {
		int edge = side == LEFT ? 1 : 5;
		float s = slotDistance(slot);
		float from = s - MARKING_WIDTH / 2, to = s + MARKING_WIDTH / 2;
		walls.addDecal(new SweptSection.Decal(edge, from, to,
				WallMarking.create(walls, edge, from, to, textures.hullPlates, code, caption, paint, code.hashCode())));
		return this;
	}

	/**
	 * Puts a cabinet against a wall, in the slot k. To be called before the World is built.
	 *
	 * @param side  LEFT or RIGHT
	 * @param type  low or high
	 * @param width its width along the wall (at most the space between two ribs)
	 */
	public Corridor addCabinet(int slot, int side, WallCabinet.Type type, float width, int seed) {
		float s = slotDistance(slot);
		Vector3 left = path.left(s);
		float wall = ShipSections.CORRIDOR_WIDTH / 2;
		// In a bend, the wall is curved and the back of the cabinet flat: on the inner wall (convex) the back is pushed in
		// by the sag of the curve, so that its corners touch the wall rather than float in front of it
		float curvature = path.curvature() * side;
		if (curvature > 0) {
			float radius = 1 / curvature - wall;
			wall += width * width / (8 * radius);
		}
		Vector4 p = path.point(s);
		Vector4 back = new Vector4(p.getX() + side * wall * left.getX(), p.getY() + side * wall * left.getY(), 0, 1);
		java.util.Random random = new java.util.Random(seed);
		Texture doors = ShipTextures.createCabinetDoors(width, type.height, type.doors, type == WallCabinet.Type.LOW, seed);
		WallCabinet cabinet = new WallCabinet("cabinet", type, width, doors, textures, WallCabinet.COLORS[random.nextInt(WallCabinet.COLORS.length)]);
		cabinet.place(back, left.times(-side));
		if (fittings == null) {
			fittings = new Element("cabinets");
			addElement(fittings);
		}
		fittings.addElement(cabinet);
		return this;
	}

	/**
	 * The pipes: circles swept along the same path as the corridor, offset to its upper left corner. Smooth, seen from
	 * outside, each one of its own color.
	 */
	private Element createPipes(ShipTextures textures) {
		Element pipes = new Element("pipes");
		for (int i = 0; i < PIPES.length; i++) {
			float[] p = PIPES[i];
			SweptSection pipe = new SweptSection("pipe", path, ShipSections.circle(p[0], p[1], p[2], PIPE_SEGMENTS), true,
					SweptSection.Facing.OUTSIDE, SweptSection.Shading.SMOOTH);
			pipe.setTexture(textures.paintedMetal, 1f, 0.3f);
			pipe.setColor(PIPE_COLORS[i % PIPE_COLORS.length]);
			pipes.addElement(pipe);
		}
		return pipes;
	}

	/**
	 * The fixtures of the ceiling lights: a Box each, built at the origin like every built-in shape, then turned to
	 * the direction of the corridor and moved under the ceiling by a Transformation (rotation, then translation). The
	 * light itself hangs just below (see getLightPoints()).
	 */
	private Element createFixtures() {
		Element fixtures = new Element("light fixtures");
		fixtures.setColor(FIXTURE_COLOR);
		int count = Math.max(1, Math.round(path.getLength() / LIGHT_SPACING));
		float spacing = path.getLength() / count;
		float height = ShipSections.CORRIDOR_HEIGHT;
		for (int k = 0; k < count; k++) {
			float s = spacing * (k + 0.5f);
			Vector4 p = path.point(s);
			Vector3 t = path.tangent(s);
			Box fixture = new Box(0.9f, 0.22f, 0.06f, "fixture"); // Long side along X, then turned along the corridor
			float heading = (float) Math.atan2(t.getY(), t.getX());
			fixture.setTransformation(new Rotation(heading, Vector3.zAxis()));
			fixture.combineTransformation(new Translation(new Vector3(p.getX(), p.getY(), height - 0.03f)));
			fixtures.addElement(fixture);
			lightPoints.add(new Vector4(p.getX(), p.getY(), height - LIGHT_DROP, 1));
		}
		return fixtures;
	}

	/** True if the end of the corridor is a wall, not a doorway (see Ship.keepInside()). */
	public boolean isEndClosed() {
		return endClosed;
	}

	public void setEndClosed(boolean endClosed) {
		this.endClosed = endClosed;
	}

	/** The door at the start of the corridor, if there is one: nobody can walk in while it is closed. */
	public AirlockDoor getDoor() {
		return door;
	}

	public void setDoor(AirlockDoor door) {
		this.door = door;
	}

	public PathSegment getPath() {
		return path;
	}

	/** Where the ceiling lights of this corridor hang. */
	public List<Vector4> getLightPoints() {
		return lightPoints;
	}
}
