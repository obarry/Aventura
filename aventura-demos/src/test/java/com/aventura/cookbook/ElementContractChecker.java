package com.aventura.cookbook;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.world.Element;
import com.aventura.model.world.Vertex;
import com.aventura.model.world.triangle.Triangle;

/**
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
 *
 * Checks the contracts of docs/GEOMETRY_COOKBOOK.md on a BUILT Element (and its sub-Elements),
 * and returns the list of violations (empty if all is well). Meant for unit tests of new shapes:
 *
 *     element.build();
 *     assertEquals(List.of(), ElementContractChecker.check(element));
 *     assertEquals(expectedVolume, ElementContractChecker.signedVolume(element), tolerance);
 *
 * What check() verifies, for the Element and recursively for every sub-Element:
 * - C3 ownership: the 3 vertices of each triangle are registered in the Element (createVertex()),
 *   otherwise the engine never transforms them.
 * - C5/C6 normals: no zero-area triangle with a triangle-level normal (its normal would be NaN);
 *   a triangle with a triangle-level normal has a finite, unit normal; otherwise
 *   each of its vertices has one. Vertex normals must agree with the winding (point to the same
 *   side as the face normal V1V2 x V1V3): this catches a mesh wound inwards.
 * - C8 texture: a textured triangle has its 3 texture coordinates.
 * - C9 tree: each sub-Element's parent is the Element that holds it.
 *
 * signedVolume() checks the winding of a closed shape as a whole (C5, C7): by the divergence
 * theorem, the sum over all triangles of V1 . (V2 x V3) / 6 is the enclosed volume when every
 * triangle is wound counter-clockwise seen from outside, and is wrong (smaller, or negative) as
 * soon as some are wound the other way. Sub-Elements are included, placed by their own
 * transformation relative to the Element checked, so a shape closed by sub-Elements (e.g. a
 * cylinder closed by 2 discs) can be checked as a whole.
 *
 * @author Olivier BARRY
 * @since 2026
 */
public final class ElementContractChecker {

	private ElementContractChecker() {
	}

	public static List<String> check(Element e) {
		List<String> violations = new ArrayList<>();
		check(e, violations);
		return violations;
	}

	private static void check(Element e, List<String> violations) {
		Set<Vertex> owned = new HashSet<>(e.getVertices());
		String where = e.getName();

		for (int k = 0; k < e.getNbTriangles(); k++) {
			Triangle t = e.getTriangle(k);
			String tri = where + " triangle #" + k;

			for (Vertex v : new Vertex[] { t.getV1(), t.getV2(), t.getV3() }) {
				if (!owned.contains(v)) {
					violations.add(tri + ": vertex not created by the Element (use createVertex() or a Mesh)");
				}
			}

			Vector3 face = faceNormal(t);
			if (face.length() < 1e-12f) {
				// A zero-area triangle draws nothing. Harmless with vertex normals (e.g. at the poles
				// of a UV sphere), but its flat normal is NaN, which breaks FLAT shading.
				if (t.isTriangleNormal()) {
					violations.add(tri + ": degenerate (zero area) with a triangle normal (NaN)");
				}
				continue;
			}

			if (t.isTriangleNormal()) {
				Vector3 n = t.getNormal();
				if (n == null || !isUnit(n)) {
					violations.add(tri + ": triangle normal missing or not unit: " + n);
				}
			} else {
				for (Vertex v : new Vertex[] { t.getV1(), t.getV2(), t.getV3() }) {
					Vector3 n = v.getNormal();
					if (n == null || !isUnit(n)) {
						violations.add(tri + ": vertex normal missing or not unit (smooth triangle): " + n);
					} else if (n.dot(face) <= 0) {
						violations.add(tri + ": vertex normal opposite to the winding (mesh wound inwards?)");
					}
				}
			}

			if (t.getTexture() != null && (t.getTexVec1() == null || t.getTexVec2() == null || t.getTexVec3() == null)) {
				violations.add(tri + ": texture without its 3 texture coordinates");
			}
		}

		if (e.getSubElements() != null) {
			for (Element sub : e.getSubElements()) {
				if (sub.getParent() != e) {
					violations.add(sub.getName() + ": parent is not " + where);
				}
				check(sub, violations);
			}
		}
	}

	/**
	 * Signed volume enclosed by the triangles of e and of its sub-Elements, in e's local frame
	 * (sub-Elements placed by their transformation relative to e). Equal to the volume of the shape
	 * if it is closed and every triangle is wound counter-clockwise seen from outside.
	 */
	public static float signedVolume(Element e) {
		return signedVolume(e, Matrix4.identity());
	}

	private static float signedVolume(Element e, Matrix4 toRoot) {
		double volume = 0;
		for (Triangle t : e.getTriangles()) {
			Vector3 a = toRoot.times(t.getV1().getPos()).V3();
			Vector3 b = toRoot.times(t.getV2().getPos()).V3();
			Vector3 c = toRoot.times(t.getV3().getPos()).V3();
			volume += a.dot(b.cross(c)) / 6.0;
		}
		if (e.getSubElements() != null) {
			for (Element sub : e.getSubElements()) {
				// A sub-Element's LOCAL transformation is not exposed (getTransformation() returns the
				// full one, from the root): full(parent)^-1 . full(sub). With no transformation at all
				// on e's ancestors (the usual case in a unit test) full(e) is just e's own matrix.
				Matrix4 relative;
				try {
					relative = e.getTransformation().inverse().times(sub.getTransformation());
				} catch (Exception ex) {
					throw new IllegalStateException("Transformation of " + e.getName() + " is not invertible", ex);
				}
				volume += signedVolume(sub, toRoot.times(relative));
			}
		}
		return (float) volume;
	}

	/** V1V2 x V1V3 (not normalized): the orientation given by the winding of the triangle. */
	public static Vector3 faceNormal(Triangle t) {
		Vector4 p1 = t.getV1().getPos(), p2 = t.getV2().getPos(), p3 = t.getV3().getPos();
		return p2.minus(p1).V3().cross(p3.minus(p1).V3());
	}

	private static boolean isUnit(Vector3 n) {
		float l = n.length();
		return !Float.isNaN(l) && Math.abs(l - 1) < 1e-3f;
	}
}
