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

import com.aventura.math.transform.Rotation;
import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Vector3;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.Element;
import com.aventura.model.world.shape.Box;

/**
 * A Doric column, standing or broken: an Element <b>assembled from other Elements</b> (Geometry Cookbook, recipe 4).
 * It has no vertex or triangle of its own: it is a group, a node of the tree of the scene that places its parts.
 * <pre>
 *   Column                       origin: center of the base, on the floor
 *    ├── drum 0  TaperedDrum     fluted, stacked from the floor up: each one is translated on top of the previous
 *    ├── drum 1                  one, and narrower (the shaft of a Doric column tapers towards the top)
 *    ├── ...
 *    └── capital (group)         only on a complete column
 *         ├── echinus  TaperedDrum, smooth, flaring out
 *         └── abacus   Box, the square slab that carries the beams
 * </pre>
 * As the ancient ones, the column is built from separate drums rather than carved in one piece: a ruined column is
 * simply the same column with fewer drums, the last one being broken at a random height and closed by a cap.
 *
 * <p>Each part is placed by its own transformation, in the frame of its parent. Moving the Column (to its place in
 * the temple) moves all of its parts.
 *
 * @since Nov 2026
 */
public class Column extends Element {

	// ----- Dimensions, in meters -----

	/** Number of drums of a complete shaft, and height of a drum. */
	public static final int SHAFT_DRUMS = 5;
	public static final float DRUM_HEIGHT = 1.05f;
	/** Radius of the shaft at its base and under the capital. */
	public static final float BASE_RADIUS = 0.52f;
	public static final float NECK_RADIUS = 0.41f;
	/** 20 flutes, as on a Doric column: 40 faces around a drum. */
	static final int FLUTES = 20;
	static final float FLUTE_DEPTH = 0.03f;

	/** Capital: the echinus flares out from the neck to the abacus, a square slab. */
	static final float ECHINUS_HEIGHT = 0.28f;
	static final float ECHINUS_TOP_RADIUS = 0.66f;
	static final int ECHINUS_SEGMENTS = 20;
	public static final float ABACUS_SIDE = 1.4f;
	static final float ABACUS_HEIGHT = 0.24f;

	/** Height of a complete column, capital included. */
	public static final float HEIGHT = SHAFT_DRUMS * DRUM_HEIGHT + ECHINUS_HEIGHT + ABACUS_HEIGHT;

	/**
	 * Each part goes this deep into the part below it. Two separate surfaces that only touch along a line can leave
	 * a gap of a pixel between them when they are drawn, through which the sky would show.
	 */
	static final float OVERLAP = 0.012f;

	/** Weathering: each drum is turned at random, and slightly off the axis. */
	static final float DRUM_MAX_SHIFT = 0.012f;
	/** The color of each drum varies by this share around the color of the column. */
	static final float TINT_VARIATION = 0.07f;

	private final int nbDrums;
	private final float height;

	/**
	 * @param nbDrums number of drums standing: SHAFT_DRUMS for a complete column, crowned by its capital; fewer for a
	 *                broken column (at least 1)
	 * @param marble  texture of the column
	 * @param tint    color of the column, multiplied with the texture
	 * @param random  source of the variations from one column to the other (angle and tint of the drums, height of
	 *                the break)
	 */
	public Column(int nbDrums, Texture marble, Color tint, Random random) {
		super("column");
		if (nbDrums < 1 || nbDrums > SHAFT_DRUMS) {
			throw new IllegalArgumentException("A column has 1 to " + SHAFT_DRUMS + " drums: " + nbDrums);
		}
		this.nbDrums = nbDrums;
		boolean complete = nbDrums == SHAFT_DRUMS;

		float z = 0; // Altitude of the top of the part of the column built so far
		for (int k = 0; k < nbDrums; k++) {
			boolean last = k == nbDrums - 1;
			// The last drum of a broken column is what remains of a drum: shorter
			float drumHeight = last && !complete ? DRUM_HEIGHT * (0.35f + 0.6f * random.nextFloat()) : DRUM_HEIGHT;
			float bottomRadius = shaftRadius(k * DRUM_HEIGHT), topRadius = shaftRadius(k * DRUM_HEIGHT + drumHeight);

			TaperedDrum drum = createDrum(bottomRadius, topRadius, drumHeight + OVERLAP, marble, tint, random);
			// Only the top of a broken column can be seen: the other ends are hidden by the next drum or by the floor
			drum.withCaps(last && !complete, false);
			// The origin of a drum is its center: turned around its axis first, then moved on top of the previous one
			drum.setTransformation(new Rotation(random.nextFloat() * (float) Math.PI, Vector3.zAxis()));
			drum.combineTransformation(new Translation(new Vector3(
					(random.nextFloat() - 0.5f) * 2 * DRUM_MAX_SHIFT, (random.nextFloat() - 0.5f) * 2 * DRUM_MAX_SHIFT, z + (drumHeight - OVERLAP) / 2)));
			addElement(drum);
			z += drumHeight;
		}

		if (complete) {
			Element capital = createCapital(marble, tint);
			capital.setTransformation(new Translation(Vector3.zAxis(), z - OVERLAP)); // The origin of the capital is its base
			addElement(capital);
			z += ECHINUS_HEIGHT + ABACUS_HEIGHT;
		}
		this.height = z;
	}

	/** Radius of the shaft at a given height above the base: from BASE_RADIUS down to NECK_RADIUS at the top. */
	static float shaftRadius(float altitude) {
		return Noise.lerp(BASE_RADIUS, NECK_RADIUS, altitude / (SHAFT_DRUMS * DRUM_HEIGHT));
	}

	/** A fluted drum of marble, of a tint slightly different from one drum to the other. */
	static TaperedDrum createDrum(float bottomRadius, float topRadius, float height, Texture marble, Color tint, Random random) {
		// The texture goes 4 times around the drum (about 0.8 m each time, for a drum 1 m high: it is not stretched)
		TaperedDrum drum = new TaperedDrum(bottomRadius, topRadius, height, 2 * FLUTES, FLUTE_DEPTH, marble, 4);
		drum.setColor(SceneTextures.shade(tint, 1 + (random.nextFloat() - 0.5f) * 2 * TINT_VARIATION));
		return drum;
	}

	/** The capital: a group of two Elements, whose origin is the center of its base. */
	private static Element createCapital(Texture marble, Color tint) {
		Element capital = new Element("capital");

		// Smooth (no flute), flaring out. Its top is hidden by the abacus; its bottom is wider than the shaft and can
		// be seen from below, so it is closed by a cap
		TaperedDrum echinus = new TaperedDrum(NECK_RADIUS + 0.02f, ECHINUS_TOP_RADIUS, ECHINUS_HEIGHT + OVERLAP, ECHINUS_SEGMENTS, marble);
		echinus.withCaps(false, true);
		echinus.setTransformation(new Translation(Vector3.zAxis(), (ECHINUS_HEIGHT + OVERLAP) / 2));
		capital.addElement(echinus);

		Box abacus = new Box(ABACUS_SIDE, ABACUS_SIDE, ABACUS_HEIGHT, marble);
		abacus.setTransformation(new Translation(Vector3.zAxis(), OVERLAP + ECHINUS_HEIGHT + ABACUS_HEIGHT / 2));
		capital.addElement(abacus);

		capital.setColor(tint); // Inherited by the echinus and the abacus, which have no color of their own
		return capital;
	}

	/**
	 * A drum fallen from a column, lying on its side: closed at both ends, turned by a quarter turn around the Y axis
	 * (its axis is then along X, horizontal), then around the vertical by the given angle. Its origin is its center:
	 * it rests on a flat ground when translated at the altitude of its radius.
	 *
	 * @param level   rank of the drum in the shaft it comes from (0 for the bottom one), which gives its radiuses
	 * @param heading direction of the axis of the lying drum, as an angle around the vertical, in radians
	 */
	public static TaperedDrum createFallenDrum(int level, float heading, Texture marble, Color tint, Random random) {
		TaperedDrum drum = createDrum(shaftRadius(level * DRUM_HEIGHT), shaftRadius((level + 1) * DRUM_HEIGHT), DRUM_HEIGHT, marble, tint, random);
		drum.withCaps(true, true);
		drum.setTransformation(new Rotation((float) Math.PI / 2, Vector3.yAxis()));
		drum.combineTransformation(new Rotation(heading, Vector3.zAxis()));
		return drum;
	}

	/** Number of drums standing. */
	public int getNbDrums() {
		return nbDrums;
	}

	/** true for a column with all its drums and its capital. */
	public boolean isComplete() {
		return nbDrums == SHAFT_DRUMS;
	}

	/** Height of what stands of this column. */
	public float getHeight() {
		return height;
	}
}
