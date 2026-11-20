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

import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.Vertex;

/**
 * A new shape: a ring standing across a corridor, between an outer outline and an inner one. With the outline of the
 * corridor outside and the same outline a little smaller inside, it is a rib of the hull, repeated along the
 * corridors; with the outline of a doorway inside, it is the heavy frame of a door.
 * <pre>
 *     outer outline                    seen along the corridor:     seen from above:
 *       ___________                       front face (towards -t)
 *      / _________ \                      back face (towards +t)      ====|  |====   the ring is
 *     | |         | |  inner outline      inner band (towards the     ====|  |====   "thickness" long
 *     | |         | |                     middle of the corridor)          &lt;--&gt;      along the corridor
 *      \_\_______/_/
 * </pre>
 * The inner band can have a texture and a color of its own ({@link #setBand}).
 * Both outlines have the same number of points, given as (lateral, height) pairs like the sections of a
 * {@link SweptSection}, turning counter-clockwise: point i of the outer outline is joined to point i of the inner one.
 * The outer band of the ring is not made: it is against the wall of the corridor, where nobody can see it.
 *
 * @since Nov 2026
 */
public class SectionFrame extends ShipShape {

	protected final Vector4 center;
	protected final Vector3 forward, left, up = Vector3.zAxis();
	protected final float[][] outer, inner;
	protected final float thickness;
	protected final Texture texture;
	protected final float tile;
	protected boolean open = false;
	protected Texture bandTexture;
	protected Color bandColor;

	/**
	 * @param name      name of the Element
	 * @param center    the point of the floor, in the middle of the corridor, where the frame stands
	 * @param forward   the direction of the corridor there (horizontal)
	 * @param outer     the outer outline, (lateral, height) points turning counter-clockwise
	 * @param inner     the inner outline, as many points
	 * @param thickness length of the ring along the corridor
	 * @param texture   texture of the faces, or null
	 * @param tile      the texture is repeated every tile meters
	 */
	public SectionFrame(String name, Vector4 center, Vector3 forward, float[][] outer, float[][] inner, float thickness, Texture texture, float tile) {
		super(name);
		if (outer.length != inner.length) throw new IllegalArgumentException("The two outlines need as many points");
		this.center = center;
		this.forward = new Vector3(forward).normalize();
		this.left = Vector3.zAxis().cross(this.forward);
		this.outer = outer;
		this.inner = inner;
		this.thickness = thickness;
		this.texture = texture;
		this.tile = tile;
	}

	/**
	 * Leaves out the last edge of the ring, from the last point back to the first one: the floor, for a rib that must
	 * not stick out of the floor, or the sill of a doorway. To be called before the World is built.
	 */
	public SectionFrame setOpen(boolean open) {
		this.open = open;
		return this;
	}

	/**
	 * Gives the inner band of the ring its own texture and color (by default it has those of the faces): the edge of a
	 * door painted with hazard stripes, on a frame of plain metal. The color has priority over the color of the Element
	 * (it is given to the triangles of the band). To be called before the World is built.
	 */
	public SectionFrame setBand(Texture texture, Color color) {
		this.bandTexture = texture;
		this.bandColor = color;
		return this;
	}

	@Override
	protected void generate() {
		int n = outer.length;
		float half = thickness / 2;
		// The middle of the inner outline: the inner band is seen from there
		float[] middle = new float[2];
		for (float[] p : inner) {
			middle[0] += p[0] / n;
			middle[1] += p[1] / n;
		}
		float around = 0; // Distance along the inner outline, for the texture of the band
		for (int i = 0; i < (open ? n - 1 : n); i++) {
			int j = (i + 1) % n;
			// Front and back faces: each one has its own vertices (a vertex has a single normal, and these edges are sharp)
			for (float x : new float[] { -half, half }) {
				Vertex o1 = point(outer[i], x), o2 = point(outer[j], x), i2 = point(inner[j], x), i1 = point(inner[i], x);
				Vector3 seenFrom = forward.times(x < 0 ? -1 : 1);
				addQuad(o1, o2, i2, i1, seenFrom, texture, uv(outer[i], outer[j], inner[j], inner[i]));
			}
			// Inner band, from the front face to the back face, seen from the middle of the corridor
			float length = (float) Math.hypot(inner[j][0] - inner[i][0], inner[j][1] - inner[i][1]);
			float[] mid = { (inner[i][0] + inner[j][0]) / 2, (inner[i][1] + inner[j][1]) / 2 };
			Vector3 towardsMiddle = left.times(middle[0] - mid[0]).plus(up.times(middle[1] - mid[1]));
			Vertex a = point(inner[i], -half), b = point(inner[j], -half), c = point(inner[j], half), d = point(inner[i], half);
			float u0 = around / tile, u1 = (around + length) / tile, v0 = 0, v1 = thickness / tile;
			addQuad(a, b, c, d, towardsMiddle, bandTexture != null ? bandTexture : texture, new float[][] { { u0, v0 }, { u1, v0 }, { u1, v1 }, { u0, v1 } },
					bandColor);
			around += length;
		}
	}

	private Vertex point(float[] p, float x) {
		return vertex(center, forward, left, up, x, p[0], p[1]);
	}

	/** Texture coordinates of points of the front or back face: their (lateral, height) position, in tiles. */
	private float[][] uv(float[]... points) {
		float[][] uv = new float[points.length][];
		for (int k = 0; k < points.length; k++) uv[k] = new float[] { points[k][0] / tile, points[k][1] / tile };
		return uv;
	}

	/**
	 * The outline moved inwards by the given depth: each edge is moved parallel to itself, and the new points are where
	 * two neighbor edges meet again. The outline must turn counter-clockwise and be convex.
	 */
	public static float[][] inset(float[][] outline, float depth) {
		int n = outline.length;
		float[][] result = new float[n][];
		for (int i = 0; i < n; i++) {
			// The two edges that meet at point i: from the previous point, and to the next one
			float[] p = outline[(i + n - 1) % n], q = outline[i], r = outline[(i + 1) % n];
			float[] n1 = leftNormal(p, q), n2 = leftNormal(q, r);
			// The point at the distance depth from both edges: along the bisector of the two normals
			float bx = n1[0] + n2[0], by = n1[1] + n2[1];
			float cos = (bx * n1[0] + by * n1[1]); // = 1 + cos(angle between the normals)
			float scale = depth / cos;
			result[i] = new float[] { q[0] + bx * scale, q[1] + by * scale };
		}
		return result;
	}

	/** Unit normal on the left of the edge from a to b: the inside of an outline turning counter-clockwise. */
	private static float[] leftNormal(float[] a, float[] b) {
		float dx = b[0] - a[0], dy = b[1] - a[1], length = (float) Math.hypot(dx, dy);
		return new float[] { -dy / length, dx / length };
	}
}
