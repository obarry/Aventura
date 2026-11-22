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
import com.aventura.model.world.Element;
import com.aventura.model.world.shape.Box;
import com.aventura.model.world.shape.Cylinder;

/**
 * A junction of the corridors: a square chamber, higher than the corridors, with a doorway, a door or a wall on each
 * of its four sides.
 * <pre>
 *                 NORTH                   Hub
 *          +-----[    ]-----+             ├── floor, ceiling   Panel
 *          |#              #|             ├── walls (group)    Panel x 4, with or without a doorway
 *     WEST [                ] EAST        ├── frames (group)   SectionFrame around each doorway
 *          |#              #|             ├── doors (group)    AirlockDoor in some doorways
 *          +-----[    ]-----+             └── pillars (group)  Box in the corners (#), pipes
 *                 SOUTH
 * </pre>
 * The walls are seen from inside only: from above, through the ceiling, the plan of the junction can be seen.
 *
 * @since Nov 2026
 */
public class Hub extends Element {

	/** What closes a side of the junction. */
	public enum Side {
		/** A doorway where a corridor arrives, with a heavy frame */
		PASSAGE,
		/** A doorway with an open airlock door */
		OPEN_DOOR,
		/** A doorway with a closed airlock door, nothing behind */
		CLOSED_DOOR,
		/** A plain wall */
		WALL
	}

	/** The four sides, in the order of the constructor: their outward direction. */
	public static final Vector3[] DIRECTIONS = { Vector3.xAxis(), Vector3.yAxis(), Vector3.xOppAxis(), Vector3.yOppAxis() };

	public static final float SIZE = 4f;
	public static final float HEIGHT = 3.2f;
	public static final float FRAME_THICKNESS = 0.3f, FRAME_WIDTH = 0.15f, FRAME_INSET = 0.03f;
	static final float PILLAR_SIZE = 0.35f;

	public static final Color FLOOR_COLOR = new Color(160, 156, 148);
	public static final Color WALL_COLOR = new Color(178, 176, 168);
	public static final Color CEILING_COLOR = new Color(128, 128, 126);
	public static final Color PILLAR_COLOR = new Color(92, 104, 96);
	public static final Color PIPE_COLOR = new Color(196, 126, 80);

	protected final Vector4 center;
	protected final List<AirlockDoor> doors = new ArrayList<AirlockDoor>();

	/**
	 * @param name     name of the junction
	 * @param center   the middle of its floor
	 * @param sides    what closes each side: east (+X), north (+Y), west (-X), south (-Y)
	 * @param textures the textures of the ship
	 */
	public Hub(String name, Vector4 center, Side[] sides, ShipTextures textures) {
		super(name);
		this.center = center;
		float half = SIZE / 2;

		// Floor, seen from above, and ceiling, seen from below: the order of the two axes says which side is seen
		Panel floor = new Panel("floor", at(-half, -half, 0), Vector3.xAxis(), Vector3.yAxis(), SIZE, SIZE, null, textures.tread, ShipTextures.TREAD_TILE);
		floor.setColor(FLOOR_COLOR);
		addElement(floor);
		Panel ceiling = new Panel("ceiling", at(-half, half, HEIGHT), Vector3.xAxis(), Vector3.yOppAxis(), SIZE, SIZE, null, textures.ceilingPlates,
				ShipTextures.PLATE_TILE);
		ceiling.setColor(CEILING_COLOR);
		addElement(ceiling);

		Element walls = new Element("walls");
		walls.setColor(WALL_COLOR);
		Element frames = new Element("frames");
		Element doorGroup = new Element("doors");
		float[][] corridor = ShipSections.corridor();
		for (int k = 0; k < 4; k++) {
			Vector3 out = DIRECTIONS[k];
			// The wall is seen from inside: its axes u (along the wall) and v (up) are such that u x v points inwards
			Vector3 u = out.cross(Vector3.zAxis());
			Vector4 middle = at(out.getX() * half, out.getY() * half, 0); // The middle of the wall, on the floor
			Vector4 corner = middle.minus(u.times(half));
			// The doorway, in the coordinates of the wall: the outline of a corridor, in the middle
			float[][] doorway = sides[k] == Side.WALL ? null : ShipSections.octagon(half, ShipSections.CORRIDOR_WIDTH,
					ShipSections.CORRIDOR_HEIGHT, ShipSections.CORRIDOR_LOWER_CHAMFER, ShipSections.CORRIDOR_UPPER_CHAMFER);
			walls.addElement(new Panel("wall", corner, u, Vector3.zAxis(), SIZE, HEIGHT, doorway, textures.hullPlates, ShipTextures.PLATE_TILE));

			if (sides[k] == Side.PASSAGE) {
				// A frame around the doorway, a little wider than the corridor: it hides the joint of the corridor and the wall.
				// Its inner band stands FRAME_INSET inside the walls of the corridor: half of the frame is in the corridor,
				// and a band in the plane of its walls would fight with them in the Z-buffer (z-fighting)
				float[][] outer = ShipSections.insetAboveFloor(corridor, -FRAME_WIDTH);
				float[][] inner = ShipSections.insetAboveFloor(corridor, FRAME_INSET);
				SectionFrame frame = new SectionFrame("frame", middle, out, outer, inner, FRAME_THICKNESS, textures.paintedMetal, 0.5f);
				frame.setOpen(true);
				frame.setBand(textures.hazardStripes, Color.WHITE);
				frame.setColor(AirlockDoor.FRAME_COLOR);
				frames.addElement(frame);
			} else if (sides[k] != Side.WALL) {
				AirlockDoor door = new AirlockDoor("door", middle, out, sides[k] == Side.OPEN_DOOR ? 1 : 0, textures);
				doors.add(door);
				doorGroup.addElement(door);
			}
		}
		addElement(walls);
		addElement(frames);
		addElement(doorGroup);
		addElement(createPillars(textures));
	}

	/** Pillars in the four corners, and a vertical pipe beside one of them. */
	private Element createPillars(ShipTextures textures) {
		Element pillars = new Element("pillars");
		pillars.setColor(PILLAR_COLOR);
		float offset = SIZE / 2 - PILLAR_SIZE / 2;
		for (int k = 0; k < 4; k++) {
			float sx = k % 2 == 0 ? 1 : -1, sy = k < 2 ? 1 : -1;
			Box pillar = new Box(PILLAR_SIZE, PILLAR_SIZE, HEIGHT, textures.paintedMetal, "pillar");
			pillar.setTransformation(new Translation(new Vector3(center.getX() + sx * offset, center.getY() + sy * offset, HEIGHT / 2)));
			pillars.addElement(pillar);
		}
		Cylinder pipe = new Cylinder(HEIGHT, 0.08f, 8, textures.paintedMetal);
		pipe.setColor(PIPE_COLOR);
		pipe.setTransformation(new Translation(new Vector3(center.getX() + offset - 0.35f, center.getY() + offset, HEIGHT / 2)));
		pillars.addElement(pipe);
		return pillars;
	}

	/** A point given relative to the middle of the floor of the junction. */
	private Vector4 at(float dx, float dy, float z) {
		return new Vector4(center.getX() + dx, center.getY() + dy, z, 1);
	}

	public Vector4 getCenter() {
		return center;
	}

	/** The airlock doors of the junction. */
	public List<AirlockDoor> getDoors() {
		return doors;
	}

	/** Where the light of the junction hangs: in the middle of its ceiling. */
	public Vector4 getLightPoint() {
		return at(0, 0, HEIGHT - 0.25f);
	}
}
