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
import com.aventura.model.world.shape.GenerativeElement;
import com.aventura.model.world.triangle.Triangle;

/**
 * The common base of the flat-faced shapes of the ship ({@link SectionFrame}, {@link Panel}): a
 * {@link GenerativeElement} with a helper that adds a quadrilateral turned towards the side it is seen from.
 *
 * <p>In Aventura a triangle is seen from the side where its vertices turn counter-clockwise: its normal,
 * (v2 - v1) x (v3 - v1), points towards that side. Rather than working out the right order for each face of each
 * shape by hand, {@link #addQuad} is told which side the face is seen from, and puts the vertices in the right order.
 * The normals are those of the faces (flat shading, the default of an Element): the edges of these shapes are sharp.
 *
 * <p>These shapes are given their position in the World directly (an origin and axes), rather than built at the origin
 * and moved by a Transformation: their vertices are computed once, where they stand.
 *
 * @since Nov 2026
 */
public abstract class ShipShape extends GenerativeElement {

	protected ShipShape(String name) {
		// Closed: the faces turned away from the camera are skipped (back-face culling)
		super(name, true);
	}

	/**
	 * Adds the quadrilateral (a, b, c, d), the four vertices given in order around it, as two triangles turned towards
	 * the given side.
	 *
	 * @param seenFrom a vector pointing to the side the face is seen from (its normal, roughly)
	 * @param texture  the texture of the face, or null
	 * @param uv       the texture coordinates of a, b, c and d (u, v pairs), or null without texture
	 */
	protected void addQuad(Vertex a, Vertex b, Vertex c, Vertex d, Vector3 seenFrom, Texture texture, float[][] uv) {
		addQuad(a, b, c, d, seenFrom, texture, uv, null);
	}

	/**
	 * The same, with a color of its own (null to keep the color of the Element): a triangle can have its own color,
	 * which has priority over the color of its Element.
	 */
	protected void addQuad(Vertex a, Vertex b, Vertex c, Vertex d, Vector3 seenFrom, Texture texture, float[][] uv, Color color) {
		addTriangle(a, b, c, seenFrom, texture, uv == null ? null : new float[][] { uv[0], uv[1], uv[2] }, color);
		addTriangle(a, c, d, seenFrom, texture, uv == null ? null : new float[][] { uv[0], uv[2], uv[3] }, color);
	}

	/** Adds the triangle (a, b, c), turned towards the given side, with its own color (or null). */
	protected void addTriangle(Vertex a, Vertex b, Vertex c, Vector3 seenFrom, Texture texture, float[][] uv, Color color) {
		Vector3 ab = new Vector3(a.getPos(), b.getPos()), ac = new Vector3(a.getPos(), c.getPos());
		boolean reverse = ab.cross(ac).dot(seenFrom) < 0;
		Triangle t = reverse ? new Triangle(a, c, b) : new Triangle(a, b, c);
		if (texture != null && uv != null) {
			Vector4 ta = new Vector4(uv[0][0], uv[0][1], 0, 1), tb = new Vector4(uv[1][0], uv[1][1], 0, 1), tc = new Vector4(uv[2][0], uv[2][1], 0, 1);
			if (reverse) t.setTexture(texture, ta, tc, tb);
			else t.setTexture(texture, ta, tb, tc);
		}
		if (color != null) t.setColor(color);
		addTriangle(t);
	}

	/** A new vertex of this Element at origin + u * axisU + v * axisV + w * axisW. */
	protected Vertex vertex(Vector4 origin, Vector3 axisU, Vector3 axisV, Vector3 axisW, float u, float v, float w) {
		return createVertex(new Vector4(
				origin.getX() + u * axisU.getX() + v * axisV.getX() + w * axisW.getX(),
				origin.getY() + u * axisU.getY() + v * axisV.getY() + w * axisW.getY(),
				origin.getZ() + u * axisU.getZ() + v * axisV.getZ() + w * axisW.getZ(), 1));
	}

	/**
	 * Generates the vertices and the triangles in one go: for these shapes each face creates its own vertices, so the
	 * two steps are not separated. generateVertices() is where Element.build() starts.
	 */
	@Override
	public void generateVertices() {
		generate();
	}

	@Override
	public void generateTriangles() {
		// Already done by generate()
	}

	/** Creates the vertices and the triangles of the shape. */
	protected abstract void generate();
}
