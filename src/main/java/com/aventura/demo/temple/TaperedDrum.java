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

import com.aventura.math.transform.Rotation;
import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.Vertex;
import com.aventura.model.world.shape.Disc;
import com.aventura.model.world.shape.GenerativeElement;
import com.aventura.model.world.triangle.Triangle;

/**
 * A new shape, written from scratch (Geometry Cookbook, recipe 1): a section of a round column, around the Z axis,
 * wider or narrower at the top than at the bottom, optionally fluted. It is the building block of the demo: the drums
 * of the columns, their capitals, the trunks and the branches of the olive trees.
 * <pre>
 *        top radius
 *        ___________              seen from above, with flutes:
 *       /           \                    . ' ' .
 *      /             \   height        <         >     the radius alternates between the
 *     /_______________\                  ' . . '       ridges and the bottom of the grooves
 *       bottom radius
 * </pre>
 * The origin is the center of the shape (bottom at z = -height / 2, top at z = +height / 2), as for the built-in
 * shapes.
 *
 * <h2>How it is made</h2>
 * <ul>
 * <li><b>Vertices</b> ({@link #generateVertices()}): two rings of {@code segments} vertices, one at the bottom and one
 *     at the top. The vertices are shared by the neighbor faces: there is no duplicated vertex where the surface
 *     closes on itself, because in Aventura the texture coordinates belong to the triangles, not to the vertices.</li>
 * <li><b>Triangles</b> ({@link #generateTriangles()}): two per segment, counter-clockwise seen from outside. The
 *     texture goes around the drum several times ({@code textureSpans}), mirrored every other time like the tiles of
 *     a {@link TiledTrellis}, so that it is not stretched.</li>
 * <li><b>Normals</b> ({@link #calculateNormals()}): two cases. Without flutes the surface is smooth: each vertex gets
 *     the normal of the ideal cone at that point, and the shading is interpolated between the vertices. With flutes,
 *     each face keeps its own normal (flat shading), which is what makes the sharp ridges of the flutes catch the
 *     light.</li>
 * <li><b>Caps</b>: the top and the bottom can be closed by a {@link Disc}, a sub-Element with its own vertices and
 *     its own flat normal (a vertex has only one normal: a sharp edge needs separate vertices). They are optional:
 *     between two stacked drums of a column they would never be seen.</li>
 * </ul>
 *
 * @since Nov 2026
 */
public class TaperedDrum extends GenerativeElement {

	protected final float bottomRadius, topRadius, height;
	protected final int segments;
	protected final float fluteDepth;
	protected final Texture texture;
	protected final int textureSpans;

	protected Vertex[] bottomRing, topRing;

	/**
	 * A smooth drum (no flute), open at both ends unless caps are added ({@link #withCaps}).
	 *
	 * @param bottomRadius radius at the bottom
	 * @param topRadius    radius at the top
	 * @param height       height, on the Z axis
	 * @param segments     number of faces around the drum, even
	 * @param texture      the texture of the side and of the caps, or null
	 */
	public TaperedDrum(float bottomRadius, float topRadius, float height, int segments, Texture texture) {
		this(bottomRadius, topRadius, height, segments, 0, texture, 2);
	}

	/**
	 * @param bottomRadius radius at the bottom (of the ridges, for a fluted drum)
	 * @param topRadius    radius at the top
	 * @param height       height, on the Z axis
	 * @param segments     number of faces around the drum, even. A fluted drum has segments / 2 flutes.
	 * @param fluteDepth   depth of the flutes, 0 for a smooth drum
	 * @param texture      the texture of the side and of the caps, or null
	 * @param textureSpans number of times the texture goes around the drum, even (every other span is mirrored), and
	 *                     such that a span is a whole number of segments
	 */
	public TaperedDrum(float bottomRadius, float topRadius, float height, int segments, float fluteDepth, Texture texture, int textureSpans) {
		// Closed: with back-face culling, the faces turned away from the camera are skipped. The inside of a drum
		// without caps would then be invisible: leave the caps out only where the inside cannot be seen.
		super("drum", true);
		if (bottomRadius <= fluteDepth || topRadius <= fluteDepth || height <= 0 || fluteDepth < 0) {
			throw new IllegalArgumentException("Drum radiuses must be larger than the flute depth, and the height positive: "
					+ bottomRadius + ", " + topRadius + ", " + height + ", flutes " + fluteDepth);
		}
		if (segments < 4 || segments % 2 != 0) {
			throw new IllegalArgumentException("The number of segments must be even and at least 4: " + segments);
		}
		if (textureSpans < 2 || textureSpans % 2 != 0 || segments % textureSpans != 0) {
			throw new IllegalArgumentException("The texture spans must be even and divide the " + segments + " segments: " + textureSpans);
		}
		this.bottomRadius = bottomRadius;
		this.topRadius = topRadius;
		this.height = height;
		this.segments = segments;
		this.fluteDepth = fluteDepth;
		this.texture = texture;
		this.textureSpans = textureSpans;
	}

	/**
	 * Closes the drum with caps. To be called right after the constructor: the caps are sub-Elements, created before
	 * the geometry is built.
	 *
	 * @param top    true to close the top
	 * @param bottom true to close the bottom
	 * @return this drum
	 */
	public TaperedDrum withCaps(boolean top, boolean bottom) {
		// A Disc lies in the XY plane and faces +Z. Its ring has the same number of vertices as the drum, at the
		// same angles, so that the edges of the cap and of the side coincide.
		if (top) {
			Disc cap = new Disc(topRadius, segments / 2, texture);
			cap.setTransformation(new Translation(Vector3.zAxis(), height / 2));
			addElement(cap);
		}
		if (bottom) {
			// The bottom cap must face down: turned upside down around X first, then moved down
			Disc cap = new Disc(bottomRadius, segments / 2, texture);
			cap.setTransformation(new Rotation((float) Math.PI, Vector3.xAxis()));
			cap.combineTransformation(new Translation(Vector3.zAxis(), -height / 2));
			addElement(cap);
		}
		return this;
	}

	@Override
	public void generateVertices() {
		bottomRing = new Vertex[segments];
		topRing = new Vertex[segments];
		for (int i = 0; i < segments; i++) {
			double angle = 2 * Math.PI * i / segments;
			float cos = (float) Math.cos(angle), sin = (float) Math.sin(angle);
			// Flutes: every other vertex is at the bottom of a groove
			float groove = i % 2 == 1 ? fluteDepth : 0;
			// createVertex() registers the Vertex in this Element: only registered vertices are transformed by the engine
			bottomRing[i] = createVertex(new Vector4((bottomRadius - groove) * cos, (bottomRadius - groove) * sin, -height / 2, 1));
			topRing[i] = createVertex(new Vector4((topRadius - groove) * cos, (topRadius - groove) * sin, height / 2, 1));
		}
	}

	/**
	 * One quadrilateral per segment, made of two triangles (a, b, c) and (c, d, a). Seen from outside, a to b goes to
	 * the right (the angle increases counter-clockwise seen from above) and a to d goes up: the triangles turn
	 * counter-clockwise, so their normal points outwards.
	 * <pre>
	 *   d---c   top ring
	 *   | / |
	 *   a---b   bottom ring
	 *   i  i+1
	 * </pre>
	 */
	@Override
	public void generateTriangles() {
		for (int i = 0; i < segments; i++) {
			int next = (i + 1) % segments; // The last segment closes the ring on its first vertex
			Vertex a = bottomRing[i], b = bottomRing[next], c = topRing[next], d = topRing[i];

			Triangle lower = new Triangle(a, b, c);
			Triangle upper = new Triangle(c, d, a);
			if (texture != null) {
				// u: around the drum, textureSpans times, mirrored. v: from the bottom (0) to the top (1)
				float u0 = TiledTrellis.mirror((float) i * textureSpans / segments);
				float u1 = TiledTrellis.mirror((float) (i + 1) * textureSpans / segments);
				Vector4 ta = new Vector4(u0, 0, 0, 1), tb = new Vector4(u1, 0, 0, 1);
				Vector4 tc = new Vector4(u1, 1, 0, 1), td = new Vector4(u0, 1, 0, 1);
				lower.setTexture(texture, ta, tb, tc);
				upper.setTexture(texture, tc, td, ta);
			}
			addTriangle(lower);
			addTriangle(upper);
		}
	}

	@Override
	public void calculateNormals() {
		if (fluteDepth > 0) {
			// Fluted: one normal per face (the default of an Element), for sharp ridges
			super.calculateNormals();
			return;
		}
		// Smooth: the normal of the ideal cone, the same along a line from the bottom to the top. It points away from
		// the axis, and leans up when the drum gets narrower towards the top (down when it gets wider): it is
		// perpendicular to the slanted side, whose direction is (topRadius - bottomRadius, height) in the plane
		// (distance to the axis, z).
		for (int i = 0; i < segments; i++) {
			double angle = 2 * Math.PI * i / segments;
			Vector3 normal = new Vector3(height * (float) Math.cos(angle), height * (float) Math.sin(angle), bottomRadius - topRadius);
			normal.normalize();
			bottomRing[i].setNormal(normal);
			topRing[i].setNormal(normal);
		}
	}

	public float getBottomRadius() {
		return bottomRadius;
	}

	public float getTopRadius() {
		return topRadius;
	}

	public float getHeight() {
		return height;
	}

	/** Volume of the solid of revolution with the radiuses of the ridges (a cone frustum), for the tests. */
	public float getFrustumVolume() {
		return (float) (Math.PI * height * (bottomRadius * bottomRadius + bottomRadius * topRadius + topRadius * topRadius) / 3);
	}
}
