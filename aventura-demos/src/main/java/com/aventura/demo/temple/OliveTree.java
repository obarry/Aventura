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

import com.aventura.demo.Noise;
import java.awt.Color;
import java.util.Random;

import com.aventura.math.transform.Rotation;
import com.aventura.math.transform.Scaling;
import com.aventura.math.transform.Transformation;
import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.world.Element;

/**
 * An olive tree: a short leaning trunk, a few thick branches, and a wide crown of silver green foliage. Like the
 * {@link Column}, it is a group of Elements; here the interesting part is how <b>transformations are combined</b> to
 * place each part.
 * <pre>
 *   OliveTree                    origin: foot of the trunk, on the ground
 *    ├── trunk      TaperedDrum  leaning
 *    ├── branch × n TaperedDrum  starting from the top of the trunk, spread around it
 *    └── clump × n   Boulder     flattened, one at the end of each branch and a few above the trunk
 * </pre>
 * A {@link TaperedDrum} is created upright and centered on its origin. To make a branch of it, starting from the top
 * of the trunk and pointing sideways, its transformations are applied in this order (each
 * {@code combineTransformation()} is applied after the previous ones):
 * <ol>
 * <li>a translation up by half its length: its base is now at the origin, which becomes the pivot of the
 *     rotations;</li>
 * <li>a rotation around Y, that tilts it away from the vertical;</li>
 * <li>a rotation around Z, that points it in its direction around the trunk;</li>
 * <li>a translation to the top of the trunk.</li>
 * </ol>
 * The foliage shows the third kind of transformation, the {@link Scaling}: a clump is a round {@link Boulder} made
 * 30% flatter. A Scaling that is not the same on the three axes distorts the normals; the engine corrects them
 * (see ElementTransform), so the shading stays right.
 *
 * <p>Trees of the same size but different seeds differ by the lean of the trunk, the number, direction and length of
 * the branches, and the shape of each clump.
 *
 * @since Nov 2026
 */
public class OliveTree extends Element {

	// ----- Proportions, for a tree of size 1 (about 4 m high); all lengths are multiplied by the size -----

	static final float TRUNK_HEIGHT = 1.2f;
	static final float TRUNK_BOTTOM_RADIUS = 0.42f, TRUNK_TOP_RADIUS = 0.3f;
	/** The trunk leans by up to this angle from the vertical. */
	static final float TRUNK_MAX_LEAN = (float) Math.toRadians(14);
	/** How deep the trunk goes into the ground, so that its base never shows above a sloping ground. */
	static final float TRUNK_SUNK = 0.35f;

	static final int MIN_BRANCHES = 4, MAX_BRANCHES = 6;
	static final float BRANCH_LENGTH = 1.5f;
	static final float BRANCH_BOTTOM_RADIUS = 0.15f, BRANCH_TOP_RADIUS = 0.07f;
	/** Angle of the branches from the vertical: between these two values. */
	static final float BRANCH_MIN_TILT = (float) Math.toRadians(35), BRANCH_MAX_TILT = (float) Math.toRadians(62);

	static final float CLUMP_RADIUS = 0.95f;
	static final float CLUMP_ROUGHNESS = 0.35f;
	static final float CLUMP_FLATTENING = 0.7f;
	/** Number of clumps of the middle of the crown, besides the one at the end of each branch. */
	static final int CROWN_CLUMPS = 4;
	/** Tessellation of the foliage: coarse, and the facets are kept (no smooth shading), for the look of masses of leaves. */
	static final int CLUMP_SEGMENTS = 10, CLUMP_RINGS = 6;

	static final Color BARK_TINT = new Color(150, 135, 118);

	/**
	 * @param size     scale of the tree: 1 for a tree of about 4 m
	 * @param textures the textures of the scene (bark and foliage)
	 * @param seed     seed of the shape of this tree
	 */
	public OliveTree(float size, SceneTextures textures, long seed) {
		super("olive tree");
		Random random = new Random(seed);

		// The trunk leans by leanAngle in the direction leanHeading. It is made longer by the part sunk in the ground.
		float leanAngle = random.nextFloat() * TRUNK_MAX_LEAN;
		float leanHeading = random.nextFloat() * 2 * (float) Math.PI;
		float trunkLength = (TRUNK_HEIGHT + TRUNK_SUNK) * size;
		TaperedDrum trunk = new TaperedDrum(TRUNK_BOTTOM_RADIUS * size, TRUNK_TOP_RADIUS * size, trunkLength, 10, textures.bark);
		trunk.setColor(BARK_TINT);
		Transformation trunkPose = pose(trunkLength, leanAngle, leanHeading, new Vector3(0, 0, -TRUNK_SUNK * size));
		trunk.setTransformation(trunkPose);
		addElement(trunk);

		// The top of the trunk: the same transformation, applied to the point at the top of the upright drum
		Vector4 trunkTop = trunkPose.times(new Vector4(0, 0, trunkLength / 2, 1));

		// The branches, spread around the trunk, each one ending in a clump of foliage
		int nbBranches = MIN_BRANCHES + random.nextInt(MAX_BRANCHES - MIN_BRANCHES + 1);
		float firstHeading = random.nextFloat() * 2 * (float) Math.PI;
		for (int b = 0; b < nbBranches; b++) {
			float heading = firstHeading + (b + (random.nextFloat() - 0.5f) * 0.5f) * 2 * (float) Math.PI / nbBranches;
			float tilt = Noise.lerp(BRANCH_MIN_TILT, BRANCH_MAX_TILT, random.nextFloat());
			float length = BRANCH_LENGTH * size * (0.8f + 0.4f * random.nextFloat());

			TaperedDrum branch = new TaperedDrum(BRANCH_BOTTOM_RADIUS * size, BRANCH_TOP_RADIUS * size, length, 8, textures.bark);
			branch.setColor(BARK_TINT);
			Transformation branchPose = pose(length, tilt, heading, trunkTop.V3());
			branch.setTransformation(branchPose);
			addElement(branch);

			Vector4 branchEnd = branchPose.times(new Vector4(0, 0, length / 2, 1));
			addElement(createClump(CLUMP_RADIUS * size * (0.8f + 0.35f * random.nextFloat()), branchEnd.V3(), textures, random));
		}

		// More clumps above the trunk, at random around the middle of the crown, to fill it
		Vector3 crownCenter = trunkTop.V3().plus(new Vector3(0, 0, BRANCH_LENGTH * 0.8f * size));
		for (int c = 0; c < CROWN_CLUMPS; c++) {
			Vector3 offset = new Vector3(random.nextFloat() - 0.5f, random.nextFloat() - 0.5f, (random.nextFloat() - 0.5f) * 0.5f).times(1.6f * size);
			addElement(createClump(CLUMP_RADIUS * size * (0.8f + 0.35f * random.nextFloat()), crownCenter.plus(offset), textures, random));
		}
	}

	/**
	 * The transformation that makes a limb (trunk or branch) of an upright drum of the given length, centered on its
	 * origin: its base is brought to the origin, it is tilted from the vertical, turned around the vertical to its
	 * heading, then its base is moved to the given point (see the class comment).
	 *
	 * The Transformations are matrices: "apply A, then B" is the product B · A, written here from the last one applied
	 * to the first. Element.combineTransformation() would do the same, one step at a time.
	 */
	static Transformation pose(float length, float tilt, float heading, Vector3 base) {
		return new Transformation(new Translation(base)
				.times(new Rotation(heading, Vector3.zAxis()))
				.times(new Rotation(tilt, Vector3.yAxis()))
				.times(new Translation(Vector3.zAxis(), length / 2)));
	}

	/** A clump of foliage: a Boulder of leaves, flattened, turned at random and centered on the given point. */
	private static Boulder createClump(float radius, Vector3 center, SceneTextures textures, Random random) {
		Boulder clump = new Boulder(radius, CLUMP_ROUGHNESS, CLUMP_SEGMENTS, CLUMP_RINGS, random.nextInt(), textures.foliage, false);
		// No color of its own: the texture gives it; only a shade, lighter or darker from one clump to the other
		clump.setColor(SceneTextures.shade(Color.WHITE, 0.82f + 0.18f * random.nextFloat()));
		// Scaling, Rotation and Translation, in that order (S, then R, then T), as one Transformation
		clump.setTransformation(new Transformation(
				new Scaling(1, 1, CLUMP_FLATTENING),
				new Rotation(random.nextFloat() * 2 * (float) Math.PI, Vector3.zAxis()),
				new Translation(center)));
		return clump;
	}
}
