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

/**
 * The relief of the island: a height field (an altitude for each point of a regular grid), computed from a formula
 * rather than stored in a file, and the questions the scene asks about it (how high is the ground here? how steep?).
 *
 * <p>The altitude of a point is built in three steps ({@link #formula}):
 * <ol>
 * <li><b>An island</b>: a dome, highest at the center and below the sea level (altitude 0) beyond the shore. The
 *     distance to the shore is not the same in every direction (a noise makes the coast irregular, with capes and
 *     bays).</li>
 * <li><b>A relief</b>: fractal noise added to the dome, strong for the hills and the gullies, weak for the bumps of
 *     the ground.</li>
 * <li><b>A plateau</b>: around the temple, the ground is flattened at a constant altitude, and blended smoothly into
 *     the slopes around it.</li>
 * </ol>
 * The altitudes of the grid are then computed once ({@link #IslandTerrain}) and given to the Trellis of the ground.
 * {@link #altitude} reads them back, interpolated like the triangles that are drawn, to put the trees and the stones
 * on the ground and to keep the camera above it.
 *
 * <p>Units are meters. The island is centered on the origin, like the Trellis.
 *
 * @since Nov 2026
 */
public class IslandTerrain {

	// ----- Shape of the island -----

	/** Average distance from the center to the shore. */
	static final float SHORE_RADIUS = 92f;
	/** Irregularity of the coast: the distance to the shore varies by this share, more or less. */
	static final float COAST_IRREGULARITY = 0.22f;
	/** Altitude of the dome at its center, before the relief. */
	static final float DOME_HEIGHT = 17f;
	/** Depth of the sea bed far from the shore: the ground is never drawn lower (it is hidden by the sea anyway). */
	static final float SEA_BED = -6f;

	/** Hills and gullies: amplitude (from lowest to highest) and size. */
	static final float HILLS_AMPLITUDE = 17f;
	static final float HILLS_SIZE = 48f;
	/** Bumps of the ground: amplitude and size. */
	static final float BUMPS_AMPLITUDE = 2.4f;
	static final float BUMPS_SIZE = 9f;

	// ----- Plateau of the temple -----

	/** Altitude of the plateau. */
	public static final float PLATEAU_ALTITUDE = 16f;
	/** The plateau is a rectangle centered on the origin, of these half sizes on X and Y, with rounded corners... */
	static final float PLATEAU_HALF_LENGTH = 19f;
	static final float PLATEAU_HALF_WIDTH = 12.5f;
	/** ...and blends into the slopes over this distance. */
	static final float PLATEAU_BLEND = 16f;

	private final float size;
	private final int divisions;
	private final float[][] altitudes;

	/**
	 * Computes the altitudes of a square grid centered on the origin.
	 *
	 * @param size      side of the square, in meters
	 * @param divisions number of cells on each side
	 * @param seed      seed of the noise: another seed gives another island, with the same plateau
	 */
	public IslandTerrain(float size, int divisions, int seed) {
		this.size = size;
		this.divisions = divisions;
		this.altitudes = new float[divisions + 1][divisions + 1];
		for (int i = 0; i <= divisions; i++) {
			for (int j = 0; j <= divisions; j++) {
				altitudes[i][j] = formula(-size / 2 + i * size / divisions, -size / 2 + j * size / divisions, seed);
			}
		}
	}

	/** The altitude of the island at (x, y), as defined by the formula (see the class comment). */
	static float formula(float x, float y, int seed) {

		// 1. The island: a dome over an irregular disc. d is the distance to the center, counted in "shore radius of
		// this direction": 0 at the center, 1 on the shore
		float coast = 1 + COAST_IRREGULARITY * (2 * Noise.fbm(x / 70, y / 70, 0, 3, seed) - 1);
		float d = (float) Math.sqrt(x * x + y * y) / (SHORE_RADIUS * coast);
		float dome = DOME_HEIGHT * (1 - d * d);

		// 2. The relief. It fades out under the sea, to keep the sea bed below the sea level
		float landShare = Noise.smoothstep(1.25f, 0.85f, d);
		float hills = HILLS_AMPLITUDE * (Noise.fbm(x / HILLS_SIZE, y / HILLS_SIZE, 7, 4, seed) - 0.5f);
		float bumps = BUMPS_AMPLITUDE * (Noise.fbm(x / BUMPS_SIZE, y / BUMPS_SIZE, 13, 3, seed) - 0.5f);
		float altitude = Math.max(SEA_BED, dome + landShare * (hills + bumps));

		// 3. The plateau: its weight is 1 inside the rectangle, 0 farther than PLATEAU_BLEND from it
		float weight = Noise.smoothstep(PLATEAU_BLEND, 0, distanceToPlateau(x, y));
		return Noise.lerp(altitude, PLATEAU_ALTITUDE, weight);
	}

	/** Distance from (x, y) to the rectangle of the plateau: 0 inside. */
	static float distanceToPlateau(float x, float y) {
		float dx = Math.max(0, Math.abs(x) - PLATEAU_HALF_LENGTH);
		float dy = Math.max(0, Math.abs(y) - PLATEAU_HALF_WIDTH);
		return (float) Math.sqrt(dx * dx + dy * dy);
	}

	/** The altitudes of the grid, for the Trellis: [i][j] is the vertex of column i (along X) and row j (along Y). */
	public float[][] getAltitudes() {
		return altitudes;
	}

	public float getSize() {
		return size;
	}

	public int getDivisions() {
		return divisions;
	}

	/**
	 * Altitude of the ground at (x, y), never below the surface actually drawn. Each cell of the grid is drawn as 2
	 * triangles, cut along one diagonal or the other: the altitude returned is the higher of the two possible cuts
	 * (each one a planar interpolation in the triangle containing the point), so that a camera kept above it is above
	 * the drawn triangles whatever the cut.
	 *
	 * @return the altitude, NaN outside of the grid
	 */
	public float altitude(float x, float y) {
		// Position in cells from the corner of the grid
		float u = (x + size / 2) / size * divisions;
		float v = (y + size / 2) / size * divisions;
		if (u < 0 || v < 0 || u > divisions || v > divisions) return Float.NaN;
		int i = Math.min((int) u, divisions - 1); // The last cell for the far edge
		int j = Math.min((int) v, divisions - 1);
		float fu = u - i, fv = v - j; // Position inside the cell, in [0, 1]
		float a = altitudes[i][j], b = altitudes[i + 1][j], c = altitudes[i][j + 1], d = altitudes[i + 1][j + 1];
		// Cut along the diagonal a-d: triangle a-b-d below the diagonal (fu >= fv), a-c-d above
		float cutAD = fu >= fv ? a + (b - a) * fu + (d - b) * fv : a + (c - a) * fv + (d - c) * fu;
		// Cut along the diagonal b-c: triangle a-b-c near the corner a (fu + fv <= 1), d-c-b near the corner d
		float cutBC = fu + fv <= 1 ? a + (b - a) * fu + (c - a) * fv : d + (c - d) * (1 - fu) + (b - d) * (1 - fv);
		return Math.max(cutAD, cutBC);
	}

	/**
	 * Steepness of the ground at (x, y): the slope (rise over run, 1 is 45 degrees) measured over one cell around the
	 * point. Used to choose where the rock shows and where a tree can grow.
	 */
	public float slope(float x, float y) {
		float step = size / divisions;
		float dzdx = (altitude(x + step / 2, y) - altitude(x - step / 2, y)) / step;
		float dzdy = (altitude(x, y + step / 2) - altitude(x, y - step / 2)) / step;
		float slope = (float) Math.sqrt(dzdx * dzdx + dzdy * dzdy);
		return Float.isNaN(slope) ? 0 : slope;
	}
}
