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

import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.Vertex;

/**
 * A new shape: a flat rectangle, its texture repeated like tiles, optionally pierced by a doorway that goes down to its
 * bottom edge. The floors, the ceilings and the walls of the junctions and of the control room are made of it.
 * <pre>
 *   TL __________________ TR        The rectangle is width x height, in the plane of two axes u (along its
 *     |      o4____o3    |          bottom edge) and v (up its side), from a corner (origin). It is seen from
 *     |    o5/      \o2  |          the side of u x v.
 *     |     |        |   |
 *     |    o6\      /o1  |          The doorway is an octagon o0..o7, turning counter-clockwise in (u, v), whose
 *     |______o7    o0____|          first and last points are on the bottom edge (v = 0). The rest of the
 *   BL                    BR        rectangle is cut into 10 triangles around it.
 * </pre>
 * Without a doorway, the panel is two triangles. A Box would not do for a wall seen from inside a room: its faces turn
 * outwards, and its texture is stretched once over each face, whatever its size.
 *
 * @since Nov 2026
 */
public class Panel extends ShipShape {

	protected final Vector4 origin;
	protected final Vector3 axisU, axisV;
	protected final float width, height;
	protected final float[][] doorway;
	protected final Texture texture;
	protected final float tile;

	/**
	 * @param name    name of the Element
	 * @param origin  the corner of the rectangle at (u, v) = (0, 0)
	 * @param axisU   direction of the bottom edge (unit vector)
	 * @param axisV   direction of the side edge (unit vector, perpendicular to axisU)
	 * @param width   length along axisU
	 * @param height  length along axisV
	 * @param doorway the octagon of a doorway in (u, v) coordinates (see the class comment), or null
	 * @param texture texture of the panel, or null
	 * @param tile    the texture is repeated every tile meters; 0 to stretch it once over the whole rectangle (a screen,
	 *                a sign)
	 */
	public Panel(String name, Vector4 origin, Vector3 axisU, Vector3 axisV, float width, float height, float[][] doorway, Texture texture, float tile) {
		super(name);
		if (doorway != null && (doorway.length != 8 || doorway[0][1] != 0 || doorway[7][1] != 0)) {
			throw new IllegalArgumentException("A doorway is an octagon whose first and last points are on the bottom edge");
		}
		this.origin = origin;
		this.axisU = axisU;
		this.axisV = axisV;
		this.width = width;
		this.height = height;
		this.doorway = doorway;
		this.texture = texture;
		this.tile = tile;
	}

	@Override
	protected void generate() {
		Vector3 seenFrom = axisU.cross(axisV);
		float[] bl = { 0, 0 }, br = { width, 0 }, tr = { width, height }, tl = { 0, height };
		if (doorway == null) {
			triangle(bl, br, tr, seenFrom);
			triangle(bl, tr, tl, seenFrom);
			return;
		}
		float[][] o = doorway;
		// Right of the doorway: a fan from TR, then from BR
		triangle(tr, o[3], o[2], seenFrom);
		triangle(tr, o[2], o[1], seenFrom);
		triangle(tr, o[1], br, seenFrom);
		triangle(br, o[1], o[0], seenFrom);
		// Above it
		triangle(tr, tl, o[4], seenFrom);
		triangle(tr, o[4], o[3], seenFrom);
		// Left of it: a fan from TL, then from BL
		triangle(tl, o[5], o[4], seenFrom);
		triangle(tl, o[6], o[5], seenFrom);
		triangle(tl, bl, o[6], seenFrom);
		triangle(bl, o[7], o[6], seenFrom);
	}

	/** A triangle of points given in (u, v) coordinates; its texture coordinates are the same, in tiles. */
	private void triangle(float[] a, float[] b, float[] c, Vector3 seenFrom) {
		Vertex va = point(a), vb = point(b), vc = point(c);
		// Texture coordinates: the position in tiles, or in fractions of the rectangle when the texture is stretched once
		float su = tile > 0 ? tile : width, sv = tile > 0 ? tile : height;
		float[][] uv = { { a[0] / su, a[1] / sv }, { b[0] / su, b[1] / sv }, { c[0] / su, c[1] / sv } };
		addTriangle(va, vb, vc, seenFrom, texture, uv, null);
	}

	private Vertex point(float[] p) {
		return vertex(origin, axisU, axisV, Vector3.zeroVector(), p[0], p[1], 0);
	}
}
