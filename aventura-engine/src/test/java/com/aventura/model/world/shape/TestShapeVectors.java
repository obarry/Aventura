package com.aventura.model.world.shape;

import static org.junit.Assert.*;

import org.junit.Test;

import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.world.Element;
import com.aventura.model.world.Vertex;

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
 * The shapes keep their offsets ("center", "position") as Vector3 displacements and their vertices as points
 * (Vector4, w = 1): checks that no w leaks into a position or a normal. Two bugs came from such a leak: the vertex
 * normals of a Sphere were shorter than 1 (the w of the point was normalized with x, y and z), and a ConeFrustum
 * created with a point as center (w = 1) gave every Vertex w = 2.
 *
 * @author Olivier BARRY
 * @since November 2026
 */
public class TestShapeVectors {

	private static final float EPS = 1e-5f;

	/** Every Vertex is a point (w = 1) and, if it has one, a unit normal */
	private static void assertPointsAndUnitNormals(String name, Element e) {
		assertTrue(name + " has vertices", e.getNbVertices() > 0);
		for (int i = 0; i < e.getNbVertices(); i++) {
			Vertex v = e.getVertex(i);
			assertEquals(name + " vertex " + i + " w", 1f, v.getPos().getW(), EPS);
			if (v.getNormal() != null) {
				assertEquals(name + " vertex " + i + " normal length", 1f, v.getNormal().length(), EPS);
			}
		}
	}

	@Test
	public void testSphere_vertexNormalsAreUnitWhateverTheRay() {
		System.out.println("***** Test shapes : the vertex normals of a Sphere are unit, whatever its ray *****");
		for (float ray : new float[] { 0.5f, 1f, 3f }) {
			Sphere sphere = new Sphere(ray, 8);
			sphere.build();
			assertPointsAndUnitNormals("Sphere of ray " + ray, sphere);
		}
	}

	@Test
	public void testSphere_vertexNormalsPointOutwards() {
		System.out.println("***** Test shapes : the vertex normal of a Sphere is the direction of its vertex from the center *****");
		Sphere sphere = new Sphere(2f, 8);
		sphere.build();
		for (int i = 0; i < sphere.getNbVertices(); i++) {
			Vertex v = sphere.getVertex(i);
			Vector3 radial = v.getPos().V3().times(1f / 2f);
			assertTrue("vertex " + i, radial.equals(v.getNormal()));
		}
	}

	@Test
	public void testConeFrustum_pointCenterGivesTheSameShapeAsVectorCenter() {
		System.out.println("***** Test shapes : a ConeFrustum moved by a point (w = 1) is the one moved by the same Vector3 *****");
		ConeFrustum byPoint = new ConeFrustum(2f, 1f, 1f, 8, new Vector4(0, 0, 5, 1));
		ConeFrustum byVector = new ConeFrustum(2f, 1f, 1f, 8, new Vector3(0, 0, 5));
		byPoint.build();
		byVector.build();
		assertPointsAndUnitNormals("ConeFrustum moved by a point", byPoint);
		assertEquals(byVector.getNbVertices(), byPoint.getNbVertices());
		for (int i = 0; i < byPoint.getNbVertices(); i++) {
			assertTrue("vertex " + i, byVector.getVertex(i).getPos().equals(byPoint.getVertex(i).getPos()));
		}
	}

	@Test
	public void testConeFrustum_normalsDoNotDependOnTheCenter() {
		System.out.println("***** Test shapes : moving a ConeFrustum does not change its normals *****");
		ConeFrustum centered = new ConeFrustum(2f, 1f, 1f, 8);
		ConeFrustum moved = new ConeFrustum(2f, 1f, 1f, 8, new Vector3(3, -2, 5));
		centered.build();
		moved.build();
		assertEquals(centered.getNbVertices(), moved.getNbVertices());
		for (int i = 0; i < centered.getNbVertices(); i++) {
			Vector3 n = centered.getVertex(i).getNormal();
			if (n != null) {
				assertTrue("vertex " + i, n.equals(moved.getVertex(i).getNormal()));
			}
		}
	}

	@Test
	public void testOtherShapes_verticesArePointsWithUnitNormals() {
		System.out.println("***** Test shapes : Cone, Cylinder, Disc and Trellis vertices are points, with unit normals *****");
		Element[] shapes = { new Cone(2f, 1f, 8), new Cylinder(2f, 1f, 8), new Disc(1.5f, 8), new Trellis(4f, 3f, 4, 3) };
		for (Element shape : shapes) {
			((GenerativeElement) shape).build();
			assertPointsAndUnitNormals(shape.getClass().getSimpleName(), shape);
		}
	}
}
