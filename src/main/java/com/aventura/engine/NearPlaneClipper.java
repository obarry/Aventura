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
package com.aventura.engine;

import java.util.ArrayList;
import java.util.List;

import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.world.Vertex;

/**
 * Clips a triangle against the single near plane w = near, in homogeneous (clip)
 * space, BEFORE the perspective divide. Needed only for a FRUSTUM projection:
 * under Orthographic, w is always 1 and get3DX()/get3DY()/get3DZ() -- the divide
 * used everywhere downstream, e.g. TriangleRasterizer.xScreen()/yScreen(),
 * Vertex.isInViewFrustum() -- never blows up. Under Frustum, w is this engine's
 * linear view-space depth (see ShadowingLight's phase-2 generalization and
 * TriangleRasterizer's own frustum branch): a vertex with w &lt; near is behind
 * the eye, and dividing by its (small, zero or negative) w produces an enormous,
 * undefined-sign screen position -- not reliably caught by
 * Vertex.isInViewFrustum() or TriangleRasterizer's own pixel/screen clamping,
 * both of which only look at the ALREADY-DIVIDED coordinates.
 *
 * Sutherland-Hodgman against one plane: clipping a triangle produces a convex
 * polygon of 0 (fully behind), 3 (unclipped, or exactly one corner cut) or 4
 * (two corners cut) vertices -- i.e. 0, 1 or 2 triangles once the quad is
 * fanned. Every attribute carried on a Vertex that is an affine function of
 * this triangle's own corners along a straight edge (clip-space position,
 * world position, and -- when supplied -- a per-corner normal or texture
 * coordinate) is linear in the SAME parameter t solved from w, because w
 * itself is an affine function of t along a straight 3D edge under a linear
 * (projective) transform -- so every attribute is interpolated with ordinary
 * (non-perspective) linear interpolation at that one t; TriangleRasterizer
 * still does its own, separate, perspective-correct interpolation later, at
 * the per-pixel level.
 *
 * Scope: ONLY the near plane. Far/side clipping is not addressed here (same
 * risk-limiting choice as the rest of the Lighting/Shadows plan) and relies,
 * as before, on the coarse Triangle.isInViewFrustum() scissor plus
 * TriangleRasterizer's own screen/depth bounds.
 *
 * @author Olivier BARRY
 * @since October 2026
 */
public final class NearPlaneClipper {

	private NearPlaneClipper() {
	}

	/**
	 * One clipped triangle: 3 corners, each either one of the original Vertex (untouched) or a
	 * new, temporary one built at a near-plane crossing (see NearPlaneClipper's class Javadoc) --
	 * never a shared Vertex mutated in place. n1..3/t1..3 are null together (all three) exactly
	 * when the caller's n1..3/tx1..3 were null (all three): this class never introduces an
	 * attribute the caller did not ask for, nor drops one it did.
	 */
	public static final class ClippedTriangle {
		public final Vertex v1, v2, v3;
		public final Vector3 n1, n2, n3;
		public final Vector4 t1, t2, t3;

		ClippedTriangle(Vertex v1, Vertex v2, Vertex v3, Vector3 n1, Vector3 n2, Vector3 n3, Vector4 t1, Vector4 t2, Vector4 t3) {
			this.v1 = v1;
			this.v2 = v2;
			this.v3 = v3;
			this.n1 = n1;
			this.n2 = n2;
			this.n3 = n3;
			this.t1 = t1;
			this.t2 = t2;
			this.t3 = t3;
		}
	}

	/** One corner bundled with its (optional) normal/texCoord, for the Sutherland-Hodgman walk below. */
	private static final class Corner {
		final Vertex vertex;
		final Vector3 normal;
		final Vector4 texCoord;

		Corner(Vertex vertex, Vector3 normal, Vector4 texCoord) {
			this.vertex = vertex;
			this.normal = normal;
			this.texCoord = texCoord;
		}
	}

	/**
	 * Clips one triangle against w = near.
	 *
	 * @param v1, v2, v3    the triangle's corners, already projected (Vertex.getProjPos() set) and,
	 *                      if normal/texCoord interpolation is wanted, with Vertex.getWorldPos() set
	 * @param n1, n2, n3    per-corner normals to carry through, or null (all three) if none
	 * @param tx1, tx2, tx3 per-corner texture coordinates to carry through, or null (all three) if none
	 * @param near          this projection's near-plane distance (Perspective.getNear())
	 * @return an empty list if the triangle is entirely behind near, one ClippedTriangle if it was
	 *         entirely in front or had exactly one corner cut off, two if it had two corners cut off
	 */
	public static List<ClippedTriangle> clip(
			Vertex v1, Vertex v2, Vertex v3,
			Vector3 n1, Vector3 n2, Vector3 n3,
			Vector4 tx1, Vector4 tx2, Vector4 tx3,
			float near) {

		Corner[] input = {
				new Corner(v1, n1, tx1),
				new Corner(v2, n2, tx2),
				new Corner(v3, n3, tx3)
		};

		List<Corner> output = new ArrayList<Corner>(4);
		for (int i = 0; i < input.length; i++) {
			Corner current = input[i];
			Corner previous = input[(i + input.length - 1) % input.length];
			boolean currentIn = current.vertex.getProjPos().getW() >= near;
			boolean previousIn = previous.vertex.getProjPos().getW() >= near;
			if (currentIn != previousIn) {
				output.add(intersect(previous, current, near));
			}
			if (currentIn) {
				output.add(current);
			}
		}

		List<ClippedTriangle> triangles = new ArrayList<ClippedTriangle>(2);
		for (int i = 1; i + 1 < output.size(); i++) {
			Corner a = output.get(0), b = output.get(i), c = output.get(i + 1);
			triangles.add(new ClippedTriangle(a.vertex, b.vertex, c.vertex, a.normal, b.normal, c.normal, a.texCoord, b.texCoord, c.texCoord));
		}
		return triangles;
	}

	/** A new, temporary Vertex (and matching normal/texCoord) at the point where edge from->to crosses w = near. */
	private static Corner intersect(Corner from, Corner to, float near) {
		float wFrom = from.vertex.getProjPos().getW();
		float wTo = to.vertex.getProjPos().getW();
		float t = (near - wFrom) / (wTo - wFrom);

		Vertex v = new Vertex();
		v.setProjPos(lerp(from.vertex.getProjPos(), to.vertex.getProjPos(), t));
		if (from.vertex.getWorldPos() != null && to.vertex.getWorldPos() != null) {
			v.setWorldPos(lerp(from.vertex.getWorldPos(), to.vertex.getWorldPos(), t));
		}

		Vector3 normal = (from.normal != null && to.normal != null) ? lerp(from.normal, to.normal, t) : null;
		Vector4 texCoord = (from.texCoord != null && to.texCoord != null) ? lerp(from.texCoord, to.texCoord, t) : null;
		return new Corner(v, normal, texCoord);
	}

	private static Vector4 lerp(Vector4 a, Vector4 b, float t) {
		return a.plus(b.minus(a).times(t));
	}

	private static Vector3 lerp(Vector3 a, Vector3 b, float t) {
		return a.plus(b.minus(a).times(t));
	}
}
