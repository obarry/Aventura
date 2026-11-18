package com.aventura.engine;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.world.Vertex;

/**
 * Unit tests for NearPlaneClipper (phase 3): Sutherland-Hodgman clipping of a single triangle
 * against the homogeneous plane w >= near, covering the three cases named in the audit's
 * validation criterion -- 0, 1 and 2 corners behind the near plane -- plus a check that the
 * interpolated attributes at a cut corner are exact (not just "some in-between value").
 *
 * Reminder of the general Sutherland-Hodgman shape for a single half-plane cut of a triangle:
 * 0 corners removed -> 1 triangle (unchanged), 1 corner removed -> a quad -> 2 triangles,
 * 2 corners removed -> a triangle again -> 1 triangle, 3 corners removed -> nothing.
 */
public class TestNearPlaneClipper {

	private static Vertex vertex(float x, float y, float w) {
		Vertex v = new Vertex();
		v.setProjPos(new Vector4(x, y, 0, w));
		v.setWorldPos(new Vector4(x, y, 0, 1));
		return v;
	}

	@Test
	public void testAllInFront_returnsTheSameTriangleUnclipped() {
		System.out.println("***** Test NearPlaneClipper : all 3 corners in front (w >= near) -> 1 triangle, unchanged *****");
		Vertex v1 = vertex(0, 0, 10);
		Vertex v2 = vertex(1, 0, 10);
		Vertex v3 = vertex(0, 1, 10);
		Vector3 n1 = Vector3.xAxis(), n2 = Vector3.yAxis(), n3 = Vector3.zAxis();

		List<NearPlaneClipper.ClippedTriangle> result = NearPlaneClipper.clip(v1, v2, v3, n1, n2, n3, null, null, null, 1f);

		assertEquals(1, result.size());
		NearPlaneClipper.ClippedTriangle ct = result.get(0);
		assertSame(v1, ct.v1);
		assertSame(v2, ct.v2);
		assertSame(v3, ct.v3);
		assertSame(n1, ct.n1);
		assertSame(n2, ct.n2);
		assertSame(n3, ct.n3);
		System.out.println("PASS testAllInFront_returnsTheSameTriangleUnclipped");
	}

	@Test
	public void testAllBehind_returnsNoTriangle() {
		System.out.println("***** Test NearPlaneClipper : all 3 corners behind (w < near) -> 0 triangle *****");
		Vertex v1 = vertex(0, 0, 0.1f);
		Vertex v2 = vertex(1, 0, 0.2f);
		Vertex v3 = vertex(0, 1, 0.05f);

		List<NearPlaneClipper.ClippedTriangle> result = NearPlaneClipper.clip(v1, v2, v3, null, null, null, null, null, null, 1f);

		assertEquals(0, result.size());
		System.out.println("PASS testAllBehind_returnsNoTriangle");
	}

	@Test
	public void testOneCornerBehind_returnsTwoTrianglesWithExactInterpolatedCorners() {
		System.out.println("***** Test NearPlaneClipper : exactly 1 corner behind -> a quad -> 2 triangles, exact interpolated corners *****");
		// v1 is behind (w=0 < near=1), v2 and v3 are in front.
		Vertex v1 = vertex(0, 0, 0);
		Vertex v2 = vertex(4, 0, 4);
		Vertex v3 = vertex(0, 8, 8);
		Vector3 n1 = new Vector3(1, 0, 0), n2 = new Vector3(0, 1, 0), n3 = new Vector3(0, 0, 1);

		List<NearPlaneClipper.ClippedTriangle> result = NearPlaneClipper.clip(v1, v2, v3, n1, n2, n3, null, null, null, 1f);

		assertEquals(2, result.size());
		NearPlaneClipper.ClippedTriangle t0 = result.get(0);
		NearPlaneClipper.ClippedTriangle t1 = result.get(1);

		// Both triangles fan out from the same new corner: the v3->v1 edge intersection.
		assertSame(t0.v1, t1.v1);
		// v3->v1: w goes 8 -> 0, crosses near=1 at t=(1-8)/(0-8)=0.875 -> pos (0,8)+0.875*((0,0)-(0,8))=(0,1), w=1
		assertEquals(0f, t0.v1.getProjPos().getX(), 1e-6f);
		assertEquals(1f, t0.v1.getProjPos().getY(), 1e-6f);
		assertEquals(1f, t0.v1.getProjPos().getW(), 1e-6f);
		assertEquals(0.875f, t0.n1.getX(), 1e-6f);
		assertEquals(0f, t0.n1.getY(), 1e-6f);
		assertEquals(0.125f, t0.n1.getZ(), 1e-6f);

		// t0's other two corners: the v1->v2 intersection, then the original v2.
		// v1->v2: w goes 0 -> 4, crosses near=1 at t=(1-0)/(4-0)=0.25 -> pos (0,0)+0.25*((4,0)-(0,0))=(1,0), w=1
		assertEquals(1f, t0.v2.getProjPos().getX(), 1e-6f);
		assertEquals(0f, t0.v2.getProjPos().getY(), 1e-6f);
		assertEquals(1f, t0.v2.getProjPos().getW(), 1e-6f);
		assertEquals(0.75f, t0.n2.getX(), 1e-6f);
		assertEquals(0.25f, t0.n2.getY(), 1e-6f);
		assertSame(v2, t0.v3);
		assertSame(n2, t0.n3);

		// t1 closes the fan: same new corner, then the two original front vertices, untouched.
		assertSame(v2, t1.v2);
		assertSame(v3, t1.v3);
		assertSame(n2, t1.n2);
		assertSame(n3, t1.n3);

		System.out.println("PASS testOneCornerBehind_returnsTwoTrianglesWithExactInterpolatedCorners");
	}

	@Test
	public void testTwoCornersBehind_returnsOneTriangleWithExactInterpolatedCorners() {
		System.out.println("***** Test NearPlaneClipper : exactly 2 corners behind -> back to a single triangle *****");
		// v1 is in front (w=4 >= near=1), v2 and v3 are behind.
		Vertex v1 = vertex(0, 0, 4);
		Vertex v2 = vertex(4, 0, 0);
		Vertex v3 = vertex(0, 4, 0);

		List<NearPlaneClipper.ClippedTriangle> result = NearPlaneClipper.clip(v1, v2, v3, null, null, null, null, null, null, 1f);

		assertEquals(1, result.size());
		NearPlaneClipper.ClippedTriangle ct = result.get(0);

		// The one surviving corner is the original v1, kept by reference, in the middle slot.
		assertSame(v1, ct.v2);

		// v3->v1: w goes 0 -> 4, t=(1-0)/(4-0)=0.25 -> pos (0,4)+0.25*((0,0)-(0,4))=(0,3), w=1
		assertEquals(0f, ct.v1.getProjPos().getX(), 1e-6f);
		assertEquals(3f, ct.v1.getProjPos().getY(), 1e-6f);
		assertEquals(1f, ct.v1.getProjPos().getW(), 1e-6f);

		// v1->v2: w goes 4 -> 0, t=(1-4)/(0-4)=0.75 -> pos (0,0)+0.75*((4,0)-(0,0))=(3,0), w=1
		assertEquals(3f, ct.v3.getProjPos().getX(), 1e-6f);
		assertEquals(0f, ct.v3.getProjPos().getY(), 1e-6f);
		assertEquals(1f, ct.v3.getProjPos().getW(), 1e-6f);

		System.out.println("PASS testTwoCornersBehind_returnsOneTriangleWithExactInterpolatedCorners");
	}

	@Test
	public void testExactlyOnThePlane_isTreatedAsInFront() {
		System.out.println("***** Test NearPlaneClipper : a corner exactly at w == near is kept (boundary is inclusive) *****");
		Vertex v1 = vertex(0, 0, 1); // exactly at near
		Vertex v2 = vertex(1, 0, 10);
		Vertex v3 = vertex(0, 1, 10);

		List<NearPlaneClipper.ClippedTriangle> result = NearPlaneClipper.clip(v1, v2, v3, null, null, null, null, null, null, 1f);

		assertEquals(1, result.size());
		assertSame(v1, result.get(0).v1);
		assertSame(v2, result.get(0).v2);
		assertSame(v3, result.get(0).v3);
		System.out.println("PASS testExactlyOnThePlane_isTreatedAsInFront");
	}

	@Test
	public void testWorldPosAndTexCoordAreAlsoInterpolated_withTheSameT() {
		System.out.println("***** Test NearPlaneClipper : world position and texture coordinates are interpolated too, with the same t as the clip position *****");
		Vertex v1 = vertex(0, 0, 0); // behind
		v1.setWorldPos(new Vector4(100, 0, 0, 1));
		Vertex v2 = vertex(4, 0, 4); // front
		v2.setWorldPos(new Vector4(200, 0, 0, 1));
		Vertex v3 = vertex(0, 8, 8); // front
		v3.setWorldPos(new Vector4(300, 0, 0, 1));

		Vector4 tx1 = new Vector4(0, 0, 0, 1);
		Vector4 tx2 = new Vector4(10, 0, 0, 4);
		Vector4 tx3 = new Vector4(0, 10, 0, 8);

		List<NearPlaneClipper.ClippedTriangle> result = NearPlaneClipper.clip(v1, v2, v3, null, null, null, tx1, tx2, tx3, 1f);

		assertEquals(2, result.size());
		NearPlaneClipper.ClippedTriangle t0 = result.get(0);

		// t0.v2 is the v1->v2 intersection (t=0.25, see testOneCornerBehind... for the derivation).
		assertEquals(125f, t0.v2.getWorldPos().getX(), 1e-5f); // 100 + 0.25*(200-100)
		assertEquals(2.5f, t0.t2.getX(), 1e-6f); // 0 + 0.25*(10-0)

		System.out.println("PASS testWorldPosAndTexCoordAreAlsoInterpolated_withTheSameT");
	}
}
