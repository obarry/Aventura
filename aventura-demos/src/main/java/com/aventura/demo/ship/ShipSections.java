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

/**
 * The cross-sections of the ship, as lists of (lateral, height) points turning counter-clockwise (see
 * {@link SweptSection}): the outline of the corridors, of the doorways, of the pipes and of the cable trays. They are
 * shared by all the parts that must fit together: a corridor ends on the doorway of a junction that has its outline.
 * <pre>
 *            o4 ________ o3            The corridor: an octagon, flat floor and ceiling, vertical walls, and
 *              /        \              cut corners (chamfers), larger at the top, where the pipes run.
 *          o5 |          | o2
 *             |          |             The doorways have the same shape, smaller.
 *          o6 |          | o1
 *              \________/              Point 0 is on the floor, on the left of the direction of travel (positive
 *            o7          o0            lateral): the floor is the edge from o7 to o0.
 * </pre>
 *
 * @since Nov 2026
 */
public final class ShipSections {

	/** The corridors: width, height, and the size of the chamfers at the bottom and at the top (meters). */
	public static final float CORRIDOR_WIDTH = 2.6f;
	public static final float CORRIDOR_HEIGHT = 2.7f;
	public static final float CORRIDOR_LOWER_CHAMFER = 0.35f;
	public static final float CORRIDOR_UPPER_CHAMFER = 0.6f;

	/** The doors of the airlocks: smaller than the corridor, so that their frame is a heavy ring. */
	public static final float DOOR_WIDTH = 1.5f;
	public static final float DOOR_HEIGHT = 2.15f;
	public static final float DOOR_LOWER_CHAMFER = 0.2f;
	public static final float DOOR_UPPER_CHAMFER = 0.45f;

	/** Index of the edge of the floor in an octagon (from o7 to o0). */
	public static final int FLOOR_EDGE = 7;

	private ShipSections() {
	}

	/**
	 * An octagon standing on the floor: width x height, centered on the lateral position center, with chamfers of the
	 * given sizes at the bottom and at the top. Its points turn counter-clockwise, o0 and o7 on the floor.
	 */
	public static float[][] octagon(float center, float width, float height, float lowerChamfer, float upperChamfer) {
		float r = center + width / 2, l = center - width / 2;
		return new float[][] {
			{ r - lowerChamfer, 0 },              // o0
			{ r, lowerChamfer },                  // o1
			{ r, height - upperChamfer },         // o2
			{ r - upperChamfer, height },         // o3
			{ l + upperChamfer, height },         // o4
			{ l, height - upperChamfer },         // o5
			{ l, lowerChamfer },                  // o6
			{ l + lowerChamfer, 0 },              // o7
		};
	}

	/** The outline of a corridor. */
	public static float[][] corridor() {
		return octagon(0, CORRIDOR_WIDTH, CORRIDOR_HEIGHT, CORRIDOR_LOWER_CHAMFER, CORRIDOR_UPPER_CHAMFER);
	}

	/** The outline of a door, centered at the given lateral position. */
	public static float[][] door(float center) {
		return octagon(center, DOOR_WIDTH, DOOR_HEIGHT, DOOR_LOWER_CHAMFER, DOOR_UPPER_CHAMFER);
	}

	/**
	 * The outline moved inwards by the given depth, its points on the floor kept on the floor: a rib of the hull that
	 * does not stick out of the floor (see SectionFrame.inset()).
	 */
	public static float[][] insetAboveFloor(float[][] outline, float depth) {
		int n = outline.length;
		float[][] inner = SectionFrame.inset(outline, depth);
		for (int i = 0; i < n; i++) {
			if (outline[i][1] != 0) continue;
			// Slide the point along its edge that is not the floor, down to the floor
			int next = (i + 1) % n, previous = (i + n - 1) % n;
			int other = outline[next][1] != 0 ? next : previous;
			float[] a = inner[other], b = inner[i];
			float t = a[1] / (a[1] - b[1]);
			inner[i] = new float[] { a[0] + t * (b[0] - a[0]), 0 };
		}
		return inner;
	}

	/**
	 * A circle of the given radius around (lateral, height), with the given number of points, turning
	 * counter-clockwise: the section of a pipe.
	 */
	public static float[][] circle(float lateral, float height, float radius, int points) {
		float[][] circle = new float[points][];
		for (int i = 0; i < points; i++) {
			double angle = 2 * Math.PI * i / points;
			circle[i] = new float[] { lateral + radius * (float) Math.cos(angle), height + radius * (float) Math.sin(angle) };
		}
		return circle;
	}

	/**
	 * The section of a cable tray hanging along a wall: an open U, seen from outside, its bottom at the given height.
	 */
	public static float[][] tray(float lateral, float height, float width, float depth) {
		float half = width / 2;
		return new float[][] {
			{ lateral - half, height + depth }, { lateral - half, height }, { lateral + half, height }, { lateral + half, height + depth },
		};
	}
}
