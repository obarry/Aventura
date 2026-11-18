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

import java.awt.Color;
import java.util.Random;

import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Vector3;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.Element;
import com.aventura.model.world.shape.Box;

/**
 * The ruins of a Doric temple: a tree of Elements, three levels deep, built from a plan.
 * <pre>
 *   Temple                          origin: center of the temple, on the ground
 *    ├── platform (group)           three steps of marble slabs (Box)
 *    ├── colonnade (group)          one Column (itself a group of drums) for each column of the plan
 *    ├── entablature (group)        the beams still in place on top of the columns (Box)
 *    └── cella (group)              what remains of the walls of the inner room (Box)
 * </pre>
 * The long side of the temple is along the X axis; its main front, the best preserved one, is on the -X side (the
 * west, if +Y is the north).
 *
 * <p>The state of the ruin is not hard-coded in the geometry: it is read from {@link #PLAN}, a small map of the
 * temple seen from above, where each character is a column. Editing the map is enough to raise or to pull down
 * columns; the beams follow (there is a beam wherever two neighbor columns are complete).
 *
 * <p>All the dimensions are in meters. The Random given to the constructor only drives the small variations (tint
 * of the blocks, angle of the drums, height of the breaks): the same seed gives the same temple.
 *
 * @since Nov 2026
 */
public class Temple extends Element {

	/**
	 * The plan of the temple, seen from above: +X to the right, +Y to the top. One character per column:
	 * 'C' a complete column, with its capital; '1' to '4' a broken column, with that number of drums standing;
	 * '.' no column (inside the colonnade, or a column that has disappeared).
	 */
	static final String[] PLAN = {
			"CCCCCC3.2C1",
			"C.........4",
			"C.........C",
			"C.........C",
			"C.........2",
			"CCC41.3.CC.",
	};
	/** Number of columns along X and along Y. */
	static final int NB_ALONG_X = PLAN[0].length();
	static final int NB_ALONG_Y = PLAN.length;

	// ----- Dimensions -----

	/** Distance between the axes of two neighbor columns. */
	public static final float COLUMN_SPACING = 2.5f;

	/** Platform: three steps, the top one extending FLOOR_MARGIN beyond the axes of the outer columns. */
	static final int NB_STEPS = 3;
	static final float STEP_HEIGHT = 0.32f;
	static final float STEP_DEPTH = 0.45f;
	static final float FLOOR_MARGIN = 1.1f;
	/** Altitude of the floor of the temple (the top of the platform) above its origin. */
	public static final float FLOOR_ALTITUDE = NB_STEPS * STEP_HEIGHT;

	/** Entablature: the architrave (the beam resting on the capitals), then the frieze, then the cornice. */
	static final float ARCHITRAVE_HEIGHT = 0.75f, ARCHITRAVE_WIDTH = 1.15f;
	static final float FRIEZE_HEIGHT = 0.7f, FRIEZE_WIDTH = 1.05f;
	static final float CORNICE_HEIGHT = 0.22f, CORNICE_WIDTH = 1.6f;
	/** Share of the beams that still carry their frieze and cornice (all of them on the main front). */
	static final float FRIEZE_PROBABILITY = 0.55f;

	/** Cella (inner room): a rectangle of walls, each piece of wall being what remains of a stack of courses. */
	static final float CELLA_HALF_LENGTH = 3 * COLUMN_SPACING, CELLA_HALF_WIDTH = 1.25f * COLUMN_SPACING;
	static final float WALL_THICKNESS = 0.7f;
	static final float COURSE_HEIGHT = 0.55f;
	static final int MAX_COURSES = 7;

	// ----- Colors: the tint of the marble, multiplied with its texture -----

	static final Color MARBLE_TINT = new Color(255, 244, 222);
	static final Color FLOOR_TINT = new Color(225, 214, 196);
	static final Color WALL_TINT = new Color(236, 222, 198);
	/** The tint of each block varies by this share. */
	static final float TINT_VARIATION = 0.08f;

	private final Texture marble;
	private final Random random;
	private int nbColumns, nbCompleteColumns;

	/**
	 * @param marble texture of the whole temple
	 * @param seed   seed of the variations (see the class comment)
	 */
	public Temple(Texture marble, long seed) {
		super("temple");
		this.marble = marble;
		this.random = new Random(seed);
		addElement(createPlatform());
		addElement(createColonnade());
		addElement(createEntablature());
		addElement(createCella());
	}

	// ****************************************************************************************************************
	// ***** Geometry of the plan
	// ****************************************************************************************************************

	/** The character of the plan for the column of rank i along X and j along Y (j = 0 is the -Y side). */
	static char planAt(int i, int j) {
		return PLAN[NB_ALONG_Y - 1 - j].charAt(i); // The first line of the map is the +Y side
	}

	/** true if the plan has a complete column there (false outside of the plan). */
	static boolean isComplete(int i, int j) {
		return i >= 0 && i < NB_ALONG_X && j >= 0 && j < NB_ALONG_Y && planAt(i, j) == 'C';
	}

	/** Position on X of the axis of the columns of rank i: the colonnade is centered on the origin. */
	static float columnX(int i) {
		return (i - (NB_ALONG_X - 1) / 2f) * COLUMN_SPACING;
	}

	/** Position on Y of the axis of the columns of rank j. */
	static float columnY(int j) {
		return (j - (NB_ALONG_Y - 1) / 2f) * COLUMN_SPACING;
	}

	/** Half sizes of the floor of the temple (the top step), on X and on Y. */
	public static float floorHalfLength() {
		return columnX(NB_ALONG_X - 1) + FLOOR_MARGIN;
	}

	public static float floorHalfWidth() {
		return columnY(NB_ALONG_Y - 1) + FLOOR_MARGIN;
	}

	/** Half sizes of the whole platform on the ground (the bottom step). */
	public static float groundHalfLength() {
		return floorHalfLength() + (NB_STEPS - 1) * STEP_DEPTH;
	}

	public static float groundHalfWidth() {
		return floorHalfWidth() + (NB_STEPS - 1) * STEP_DEPTH;
	}

	// ****************************************************************************************************************
	// ***** The parts of the temple
	// ****************************************************************************************************************

	/**
	 * The platform: NB_STEPS layers of slabs, each layer STEP_DEPTH smaller than the one below on every side. A layer
	 * is paved with one slab per column spacing, rather than made of a single large box: each slab gets its own tint,
	 * and its own copy of the texture, which would otherwise be stretched over the whole floor. Only the top layer is
	 * paved all over: of the lower ones, only the border can be seen.
	 */
	private Element createPlatform() {
		Element platform = new Element("platform");
		for (int step = 0; step < NB_STEPS; step++) {
			float shrink = (NB_STEPS - 1 - step) * STEP_DEPTH; // The bottom step (0) is the largest
			float halfLength = floorHalfLength() + shrink, halfWidth = floorHalfWidth() + shrink;
			float slabLength = 2 * halfLength / NB_ALONG_X, slabWidth = 2 * halfWidth / NB_ALONG_Y;
			boolean top = step == NB_STEPS - 1;

			for (int i = 0; i < NB_ALONG_X; i++) {
				for (int j = 0; j < NB_ALONG_Y; j++) {
					boolean border = i == 0 || j == 0 || i == NB_ALONG_X - 1 || j == NB_ALONG_Y - 1;
					if (!top && !border) continue;
					platform.addElement(createBlock(slabLength, slabWidth, STEP_HEIGHT,
							-halfLength + (i + 0.5f) * slabLength, -halfWidth + (j + 0.5f) * slabWidth, (step + 0.5f) * STEP_HEIGHT, FLOOR_TINT));
				}
			}
		}
		return platform;
	}

	/** The columns of the plan, each one translated to its place on the floor. */
	private Element createColonnade() {
		Element colonnade = new Element("colonnade");
		for (int i = 0; i < NB_ALONG_X; i++) {
			for (int j = 0; j < NB_ALONG_Y; j++) {
				char c = planAt(i, j);
				if (c == '.') continue;
				int nbDrums = c == 'C' ? Column.SHAFT_DRUMS : c - '0';
				Column column = new Column(nbDrums, marble, MARBLE_TINT, random);
				column.setTransformation(new Translation(new Vector3(columnX(i), columnY(j), FLOOR_ALTITUDE)));
				colonnade.addElement(column);
				nbColumns++;
				if (column.isComplete()) nbCompleteColumns++;
			}
		}
		return colonnade;
	}

	/**
	 * The entablature: a beam between two neighbor columns wherever both are complete, along the four sides of the
	 * colonnade.
	 */
	private Element createEntablature() {
		Element entablature = new Element("entablature");
		int lastX = NB_ALONG_X - 1, lastY = NB_ALONG_Y - 1;

		// Long sides (j = 0 and j = lastY): beams along X, between the columns i and i + 1
		for (int j = 0; j <= lastY; j += lastY) {
			for (int i = 0; i < lastX; i++) {
				if (isComplete(i, j) && isComplete(i + 1, j)) {
					addBeam(entablature, true, columnX(i), columnY(j), i == 0, i + 1 == lastX, false);
				}
			}
		}
		// Fronts (i = 0, the main front, and i = lastX): beams along Y, between the columns j and j + 1
		for (int i = 0; i <= lastX; i += lastX) {
			for (int j = 0; j < lastY; j++) {
				if (isComplete(i, j) && isComplete(i, j + 1)) {
					addBeam(entablature, false, columnX(i), columnY(j), j == 0, j + 1 == lastY, i == 0);
				}
			}
		}
		return entablature;
	}

	/**
	 * Adds the beam going from the column at (x, y) to the next one, along X or along Y: an architrave, and for some
	 * of them a frieze and a cornice on top.
	 *
	 * At a corner of the temple, a beam of a long side and a beam of a front meet on the same capital. If both went
	 * up to the axis of the column, they would overlap there, with their top faces at the same altitude: two faces at
	 * the same place flicker, the depth test choosing one or the other from pixel to pixel ("z-fighting"). So the
	 * beams of the fronts are made longer, to cover the whole capital of the corner, and those of the long sides
	 * shorter, to stop where the others start (each layer by half its own width).
	 *
	 * @param alongX       true for a beam of a long side (along X), false for a beam of a front (along Y)
	 * @param cornerAtStart true if the column the beam starts from is a corner of the temple
	 * @param cornerAtEnd  true if the column the beam goes to is a corner of the temple
	 * @param complete     true to always add the frieze and the cornice
	 */
	private void addBeam(Element entablature, boolean alongX, float x, float y, boolean cornerAtStart, boolean cornerAtEnd, boolean complete) {
		boolean withFrieze = complete || random.nextFloat() < FRIEZE_PROBABILITY;
		float[][] layers = withFrieze
				? new float[][] { { ARCHITRAVE_WIDTH, ARCHITRAVE_HEIGHT }, { FRIEZE_WIDTH, FRIEZE_HEIGHT }, { CORNICE_WIDTH, CORNICE_HEIGHT } }
				: new float[][] { { ARCHITRAVE_WIDTH, ARCHITRAVE_HEIGHT } };

		float z = FLOOR_ALTITUDE + Column.HEIGHT; // On top of the capitals
		for (float[] layer : layers) {
			float width = layer[0], height = layer[1];
			// Change of length at each end: half the width of the layer more (fronts) or less (long sides) at a corner
			float change = (alongX ? -1 : 1) * width / 2;
			float atStart = cornerAtStart ? change : 0, atEnd = cornerAtEnd ? change : 0;
			float length = COLUMN_SPACING + atStart + atEnd;
			float middle = COLUMN_SPACING / 2 + (atEnd - atStart) / 2; // From the start column, along the beam
			entablature.addElement(alongX
					? createBlock(length, width, height, x + middle, y, z + height / 2, MARBLE_TINT)
					: createBlock(width, length, height, x, y + middle, z + height / 2, MARBLE_TINT));
			z += height;
		}
	}

	/**
	 * The cella: what remains of the four walls of the inner room, one piece of wall per column spacing. The height
	 * of each piece is a number of courses of stone taken from a noise along the wall, so that neighbor pieces have
	 * close heights, like a wall that has crumbled, rather than unrelated ones. The middle of the wall of the main
	 * front is the door.
	 */
	private Element createCella() {
		Element cella = new Element("cella");
		int piecesAlongX = Math.round(2 * CELLA_HALF_LENGTH / COLUMN_SPACING);
		int piecesAlongY = Math.round(2 * CELLA_HALF_WIDTH / COLUMN_SPACING);

		// Long walls, at y = -CELLA_HALF_WIDTH and y = +CELLA_HALF_WIDTH
		for (int side = -1; side <= 1; side += 2) {
			for (int i = 0; i < piecesAlongX; i++) {
				float x = -CELLA_HALF_LENGTH + (i + 0.5f) * COLUMN_SPACING;
				addWallPiece(cella, COLUMN_SPACING, WALL_THICKNESS, x, side * CELLA_HALF_WIDTH, wallCourses(i, side));
			}
		}
		// Short walls, at x = -CELLA_HALF_LENGTH (with the door) and x = +CELLA_HALF_LENGTH. They fit between the
		// long walls (hence shorter by a wall thickness), so that no two faces overlap.
		float shortLength = (2 * CELLA_HALF_WIDTH - WALL_THICKNESS) / piecesAlongY;
		for (int side = -1; side <= 1; side += 2) {
			for (int j = 0; j < piecesAlongY; j++) {
				boolean door = side == -1 && j == piecesAlongY / 2;
				if (door) continue;
				float y = -CELLA_HALF_WIDTH + WALL_THICKNESS / 2 + (j + 0.5f) * shortLength;
				addWallPiece(cella, WALL_THICKNESS, shortLength, side * CELLA_HALF_LENGTH, y, wallCourses(j + 20, side * 2));
			}
		}
		return cella;
	}

	/** Number of courses left of the piece of wall of the given rank, on the given wall (see createCella()). */
	private static int wallCourses(int rank, int wall) {
		return 1 + Math.round((MAX_COURSES - 1) * Noise.fbm(rank * 0.45f, wall * 3.7f, 0, 2, 11));
	}

	private void addWallPiece(Element cella, float sizeX, float sizeY, float x, float y, int courses) {
		float height = courses * COURSE_HEIGHT;
		cella.addElement(createBlock(sizeX, sizeY, height, x, y, FLOOR_ALTITUDE + height / 2, WALL_TINT));
	}

	/**
	 * A block of marble: a Box of the given sizes, centered at (x, y, z), of the given tint, more or less a random
	 * variation.
	 */
	private Box createBlock(float sizeX, float sizeY, float sizeZ, float x, float y, float z, Color tint) {
		Box block = new Box(sizeX, sizeY, sizeZ, marble);
		block.setColor(SceneTextures.shade(tint, 1 + (random.nextFloat() - 0.5f) * 2 * TINT_VARIATION));
		block.setTransformation(new Translation(new Vector3(x, y, z)));
		return block;
	}

	/** Number of columns of the temple, complete or broken. */
	public int getNbColumns() {
		return nbColumns;
	}

	/** Number of complete columns. */
	public int getNbCompleteColumns() {
		return nbCompleteColumns;
	}
}
