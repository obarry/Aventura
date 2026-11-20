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

import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.Element;
import com.aventura.model.world.shape.Box;

/**
 * The control room, in the middle of the ship: a square room higher than the corridors, crossed from south to north
 * by the passage between the two long corridors, with banks of consoles along its east and west walls.
 * <pre>
 *                  north door
 *        +---------[      ]---------+       ControlRoom
 *        | [C]                  [C] |       ├── floor, ceiling      Panel
 *        |      [T]        [T]      |       ├── walls (group)       Panel x 4, two of them with a doorway
 *        | [C]       ring       [C] |       ├── doors (group)       AirlockDoor x 2, open
 *        |      [T]        [T]      |       ├── consoles (group)    Console along the walls (C) and back to back
 *        | [C]                  [C] |       │                       in two islands (T)
 *        +---------[      ]---------+       ├── wall screens        Panel with a screen texture
 *                  south door               └── fittings            beams, the ring of light, pillars
 * </pre>
 *
 * @since Nov 2026
 */
public class ControlRoom extends Element {

	public static final float SIZE = 10f;
	public static final float HEIGHT = 3.4f;

	public static final Color WALL_COLOR = new Color(170, 172, 168);
	public static final Color FLOOR_COLOR = new Color(158, 160, 162);
	public static final Color CEILING_COLOR = new Color(120, 122, 124);
	public static final Color BEAM_COLOR = new Color(92, 104, 96);
	public static final Color RING_COLOR = new Color(230, 236, 255);
	public static final Color SCREEN_FRAME_COLOR = new Color(60, 64, 68);

	protected final Vector4 center;
	protected final List<AirlockDoor> doors = new ArrayList<AirlockDoor>();
	protected final List<Console> consoles = new ArrayList<Console>();

	/**
	 * @param name     name of the room
	 * @param center   the middle of its floor
	 * @param textures the textures of the ship
	 */
	public ControlRoom(String name, Vector4 center, ShipTextures textures) {
		super(name);
		this.center = center;
		float half = SIZE / 2;

		Panel floor = new Panel("floor", at(-half, -half, 0), Vector3.xAxis(), Vector3.yAxis(), SIZE, SIZE, null, textures.tread, ShipTextures.TREAD_TILE);
		floor.setColor(FLOOR_COLOR);
		addElement(floor);
		Panel ceiling = new Panel("ceiling", at(-half, half, HEIGHT), Vector3.xAxis(), Vector3.yOppAxis(), SIZE, SIZE, null, textures.ceilingPlates,
				ShipTextures.PLATE_TILE);
		ceiling.setColor(CEILING_COLOR);
		addElement(ceiling);

		// The walls, seen from inside (see Hub), a doorway in the north and south ones
		Element walls = new Element("walls");
		walls.setColor(WALL_COLOR);
		Element doorGroup = new Element("doors");
		for (Vector3 out : Hub.DIRECTIONS) {
			Vector3 u = out.cross(Vector3.zAxis());
			Vector4 middle = at(out.getX() * half, out.getY() * half, 0);
			boolean doorway = out.getY() != 0;
			float[][] outline = doorway ? ShipSections.octagon(half, ShipSections.CORRIDOR_WIDTH, ShipSections.CORRIDOR_HEIGHT,
					ShipSections.CORRIDOR_LOWER_CHAMFER, ShipSections.CORRIDOR_UPPER_CHAMFER) : null;
			walls.addElement(new Panel("wall", middle.minus(u.times(half)), u, Vector3.zAxis(), SIZE, HEIGHT, outline, textures.hullPlates,
					ShipTextures.PLATE_TILE));
			if (doorway) {
				AirlockDoor door = new AirlockDoor("door", middle, out, 1, textures);
				doors.add(door);
				doorGroup.addElement(door);
			}
		}
		addElement(walls);
		addElement(doorGroup);
		addElement(createConsoles(textures));
		addElement(createWallScreens(textures));
		addElement(createFittings(textures));
	}

	/**
	 * Three consoles along each of the east and west walls, facing the middle of the room, and two islands of two
	 * consoles back to back, on each side of the passage.
	 */
	private Element createConsoles(ShipTextures textures) {
		Element group = new Element("consoles");
		Texture[] screens = { textures.screenRadar, textures.screenGraphs, textures.screenText };
		float wall = SIZE / 2 - Console.DEPTH / 2 - 0.05f;
		int n = 0;
		for (int side = -1; side <= 1; side += 2) {
			for (int k = -1; k <= 1; k++) {
				// Along the wall: the east wall (side +1) faces west (heading PI), the west wall faces east (heading 0)
				Console console = new Console("wall console", 1.6f, screens[(n + 3) % 3], textures, 100 + n);
				console.place(center.getX() + side * wall, center.getY() + k * 2.6f, side > 0 ? (float) Math.PI : 0);
				consoles.add(console);
				group.addElement(console);
				n++;
			}
			// An island: two consoles back to back, one facing the passage, one facing the wall
			for (int face = -1; face <= 1; face += 2) {
				Console console = new Console("island console", 1.2f, screens[n % 3], textures, 100 + n);
				float x = center.getX() + side * (2.3f + face * (Console.DEPTH / 2 + 0.01f));
				console.place(x, center.getY(), face * side > 0 ? 0 : (float) Math.PI);
				consoles.add(console);
				group.addElement(console);
				n++;
			}
		}
		return group;
	}

	/** Large screens on the east and west walls, above the consoles, each in a dark frame. */
	private Element createWallScreens(ShipTextures textures) {
		Element group = new Element("wall screens");
		float half = SIZE / 2, width = 2.4f, height = 1.1f, z = 1.75f;
		for (int side = -1; side <= 1; side += 2) {
			Vector3 inward = new Vector3(-side, 0, 0);
			Vector3 u = inward.cross(Vector3.zAxis()).times(-1); // Along the wall, so that u x Z points inwards
			Vector4 corner = at(side * (half - 0.04f), 0, z).minus(u.times(width / 2));
			Panel screen = new Panel("wall screen", corner, u, Vector3.zAxis(), width, height, null, side > 0 ? textures.screenRadar : textures.screenGraphs, 0);
			group.addElement(screen);
			Box frame = new Box(0.05f, width + 0.12f, height + 0.12f, "screen frame");
			frame.setColor(SCREEN_FRAME_COLOR);
			frame.setTransformation(new Translation(new Vector3(center.getX() + side * (half - 0.015f), center.getY(), z + height / 2)));
			group.addElement(frame);
		}
		return group;
	}

	/** Beams across the ceiling, the ring of light in the middle, pillars in the corners. */
	private Element createFittings(ShipTextures textures) {
		Element group = new Element("fittings");
		float half = SIZE / 2;
		for (int k = -1; k <= 1; k += 2) {
			Box beam = new Box(SIZE, 0.3f, 0.35f, textures.paintedMetal, "beam");
			beam.setColor(BEAM_COLOR);
			beam.setTransformation(new Translation(new Vector3(center.getX(), center.getY() + k * 2.8f, HEIGHT - 0.175f)));
			group.addElement(beam);
		}
		for (int k = 0; k < 4; k++) {
			float sx = k % 2 == 0 ? 1 : -1, sy = k < 2 ? 1 : -1;
			Box pillar = new Box(0.45f, 0.45f, HEIGHT, textures.paintedMetal, "pillar");
			pillar.setColor(BEAM_COLOR);
			pillar.setTransformation(new Translation(new Vector3(center.getX() + sx * (half - 0.225f), center.getY() + sy * (half - 0.225f), HEIGHT / 2)));
			group.addElement(pillar);
		}
		// The ring of light: a small circle swept along a full circle of 1.4 m, a torus made with SweptSection
		PathSegment circle = PathSegment.arc(center.getX(), center.getY(), 1.4f, 0, 360);
		SweptSection ring = new SweptSection("ring of light", circle, ShipSections.circle(0, 0, 0.07f, 8), true,
				SweptSection.Facing.OUTSIDE, SweptSection.Shading.SMOOTH, HEIGHT - 0.3f);
		ring.setColor(RING_COLOR);
		group.addElement(ring);
		return group;
	}

	private Vector4 at(float dx, float dy, float z) {
		return new Vector4(center.getX() + dx, center.getY() + dy, z, 1);
	}

	public Vector4 getCenter() {
		return center;
	}

	public List<AirlockDoor> getDoors() {
		return doors;
	}

	/** The light in the middle of the room, in the ring. */
	public Vector4 getCenterLightPoint() {
		return at(0, 0, HEIGHT - 0.9f);
	}

	/** The spot lights above the two banks of consoles, east (+1) and west (-1). */
	public Vector4 getConsoleLightPoint(int side) {
		return at(side * (SIZE / 2 - 1.6f), 0, HEIGHT - 0.4f);
	}
}
