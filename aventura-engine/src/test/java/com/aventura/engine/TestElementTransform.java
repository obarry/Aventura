package com.aventura.engine;

import static org.junit.Assert.*;

import org.junit.Test;

import com.aventura.context.PerspectiveContext;
import com.aventura.math.transform.Rotation;
import com.aventura.math.transform.Scaling;
import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.perspective.PerspectiveType;
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
 * Tests of the transformation of the normals by ElementTransform: by the linear 3x3 part of the Model matrix (a
 * Matrix3), directly if it is orthogonal, through its inverse transpose otherwise. In particular, the translation of
 * the Model matrix must have no effect on the normals, in world space as in clip space: when the normal matrix was a
 * Matrix4, the translation gave the normals a non-zero w that corrupted the ORTHOGRAPHIC backface test.
 *
 * @author Olivier BARRY
 * @since November 2026
 */
public class TestElementTransform {

	private static final float DELTA = 1e-5f;

	/** An orthographic camera looking at the origin from an eye that is not at the origin: View and Projection both translate */
	private static ElementTransform orthographicTransform() {
		PerspectiveContext ctx = new PerspectiveContext(4f, 3f, 1, 50, PerspectiveType.ORTHOGRAPHIC, 100);
		Camera camera = new Camera(new Vector4(8, 6, 10, 1), new Vector4(0, 0, 0, 1), Vector4.zAxis());
		return new ElementTransform(new ViewProjection(camera, ctx.getPerspective()));
	}

	/** Transforms a Vertex with this normal by this Model matrix, normals included */
	private static Vertex transform(ElementTransform transform, Matrix4 model, Vector3 normal) {
		transform.setModel(model, true);
		Vertex v = new Vertex(new Vector4(1, 2, 3, 1), normal);
		transform.transformVertex(v, true);
		return v;
	}

	private static void assertSameDirection(String message, Vector3 expected, Vector3 actual) {
		Vector3 e = new Vector3(expected).normalize(), a = new Vector3(actual).normalize();
		assertEquals(message + " x", e.getX(), a.getX(), DELTA);
		assertEquals(message + " y", e.getY(), a.getY(), DELTA);
		assertEquals(message + " z", e.getZ(), a.getZ(), DELTA);
	}

	@Test
	public void testOrthogonalModel_normalsAreRotatedOnly() {
		System.out.println("***** Test ElementTransform : an orthogonal Model matrix rotates the normals, its translation does not move them *****");
		ElementTransform transform = orthographicTransform();
		Matrix4 rotation = new Rotation((float) Math.PI / 2, Vector3.zAxis());
		Matrix4 moved = new Translation(new Vector3(5, -7, 2)).times(rotation);

		Vertex v = transform(transform, moved, Vector3.xAxis());
		assertSameDirection("x axis turned a quarter around z", Vector3.yAxis(), v.getWorldNormal());
		assertEquals("still unit", 1f, v.getWorldNormal().length(), DELTA);
		assertSameDirection("clip normal", transform(transform, rotation, Vector3.xAxis()).getProjNormal(), v.getProjNormal());
	}

	@Test
	public void testNonUniformScaling_normalsUseTheInverseTranspose() {
		System.out.println("***** Test ElementTransform : under a non-uniform scaling, the normals follow the inverse transpose *****");
		ElementTransform transform = orthographicTransform();
		// The plane x + y = 1 (normal (1, 1, 0)) stretched twice along x becomes x/2 + y = 1, of normal (1/2, 1, 0)
		Matrix4 scaling = new Scaling(2, 1, 1);
		Vertex v = transform(transform, scaling, new Vector3(1, 1, 0).normalize());
		assertSameDirection("stretched plane", new Vector3(0.5f, 1, 0), v.getWorldNormal());
	}

	@Test
	public void testNonOrthogonalModel_translationDoesNotChangeTheNormals() {
		System.out.println("***** Test ElementTransform : the translation of a non-orthogonal Model matrix has no effect on the normals *****");
		ElementTransform transform = orthographicTransform();
		Matrix4 linear = new Rotation(0.7f, new Vector3(1, 2, 3)).times(new Scaling(2, 0.5f, 3));
		Matrix4 moved = new Translation(new Vector3(40, -25, 12)).times(linear);
		Vector3 normal = new Vector3(0.3f, -0.4f, 0.8f).normalize();

		Vertex atOrigin = transform(transform, linear, normal);
		Vertex translated = transform(transform, moved, normal);
		assertSameDirection("world normal", atOrigin.getWorldNormal(), translated.getWorldNormal());
		// The backface test of the ORTHOGRAPHIC projection reads the sign of the z of the clip-space normal
		assertSameDirection("clip normal", atOrigin.getProjNormal(), translated.getProjNormal());
	}
}
