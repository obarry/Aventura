package com.aventura.cookbook;

import static org.junit.Assert.*;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.world.Element;
import com.aventura.model.world.Vertex;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.ClosedCone;
import com.aventura.model.world.shape.ClosedCylinder;
import com.aventura.model.world.shape.Disc;
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
 * Tests of the examples of docs/GEOMETRY_COOKBOOK.md: they are what a test of a new shape should
 * look like (contracts, volume, placement of the sub-Elements, off-screen rendering).
 *
 * @author Olivier BARRY
 * @since 2026
 */
public class TestGeometryCookbook {

	private static final List<String> NO_VIOLATION = Collections.emptyList();

	// ---- Recipe 1: a new primitive ----

	@Test
	public void testPyramidFrustum_geometryAndContracts() {
		System.out.println("***** Test GeometryCookbook : PyramidFrustum has 8 vertices, 12 triangles and fulfills the contracts *****");

		PyramidFrustum f = new PyramidFrustum(2, 1, 1, 0.5f, 1);
		f.build();

		assertEquals(8, f.getNbVertices());
		assertEquals(12, f.getNbTriangles());
		assertTrue(f.isClosed());
		assertEquals(NO_VIOLATION, ElementContractChecker.check(f));
		// Positive and exact: every triangle is wound counter-clockwise seen from outside
		assertEquals(f.getVolume(), ElementContractChecker.signedVolume(f), 1e-5f);
	}

	@Test
	public void testPyramidFrustum_normalsPointOutwards() {
		System.out.println("***** Test GeometryCookbook : every face normal of PyramidFrustum points away from its center *****");

		PyramidFrustum f = new PyramidFrustum(1.4f, 1.4f, 0.8f, 0.8f, 0.9f);
		f.build();
		for (Triangle t : f.getTriangles()) {
			Vector3 centroid = t.getV1().getPos().plus(t.getV2().getPos()).plus(t.getV3().getPos()).V3().times(1f / 3);
			assertTrue(t.getNormal().dot(centroid) > 0); // convex and centered on the origin
		}
	}

	@Test
	public void testPyramidFrustum_perFaceColor() {
		System.out.println("***** Test GeometryCookbook : a face color set before build() is given to the 2 triangles of that face only *****");

		PyramidFrustum f = new PyramidFrustum(1, 1, 0.5f, 0.5f, 1);
		f.setTopColor(Color.RED);
		f.build();
		int red = 0;
		for (Triangle t : f.getTriangles()) {
			if (Color.RED.equals(t.getColor())) {
				red++;
				assertTrue(t.getNormal().getZ() > 0.99f);
			} else {
				assertNull(t.getColor()); // inherits the Element's color
			}
		}
		assertEquals(2, red);
	}

	@Test
	public void testPyramidFrustum_rebuildKeepsTheVertices() {
		System.out.println("***** Test GeometryCookbook : rebuild() regenerates the triangles from the SAME vertices *****");

		PyramidFrustum f = new PyramidFrustum(2, 2, 1, 1, 1);
		f.build();
		Vertex v = f.getVertex(0);
		v.setPos(v.getPos().plus(new Vector4(-0.5f, 0, 0, 0))); // move a vertex (the shape is no longer planar-faced)
		f.rebuild();
		assertEquals(8, f.getNbVertices());
		assertEquals(12, f.getNbTriangles());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testPyramidFrustum_rejectsAPointedTop() {
		System.out.println("***** Test GeometryCookbook : PyramidFrustum rejects a zero-sized top (degenerate triangles) *****");
		new PyramidFrustum(1, 1, 0, 0, 1);
	}

	// ---- Recipe 3: inheritance ----

	@Test
	public void testClosedConeFrustum_isClosedByItsTwoDiscs() {
		System.out.println("***** Test GeometryCookbook : ClosedConeFrustum = inherited lateral surface + 2 discs facing outwards *****");

		ClosedConeFrustum c = new ClosedConeFrustum(3f, 1.2f, 0.7f, 24);
		c.build();

		assertEquals(2, c.getSubElements().size());
		assertEquals(NO_VIOLATION, ElementContractChecker.check(c));
		// Watertight and consistently wound: the signed volume matches the frustum (within the tessellation error)
		assertEquals(c.getVolume(), ElementContractChecker.signedVolume(c), 0.01f * c.getVolume());
	}

	@Test
	public void testClosedConeFrustum_capsAreAtTheRightPlace() {
		System.out.println("***** Test GeometryCookbook : after worldProject(), the caps lie on the planes of the 2 circles *****");

		ClosedConeFrustum c = new ClosedConeFrustum(3f, 1.2f, 0.7f, 24);
		World world = new World();
		world.addElement(c);
		world.build();
		world.worldProject(); // computes Vertex.getWorldPos() for the whole tree

		for (Vertex v : c.getSubElements().get(0).getVertices()) {
			assertEquals(-0.3f, v.getWorldPos().getZ(), 1e-5f); // top: frustumHeight - coneHeight/2
		}
		for (Vertex v : c.getSubElements().get(1).getVertices()) {
			assertEquals(-1.5f, v.getWorldPos().getZ(), 1e-5f); // bottom: -coneHeight/2
		}
	}

	// ---- Built-in shapes closed by discs (CircularMesh and cap orientation fixes) ----

	@Test
	public void testUntexturedDisc_hasTriangles() {
		System.out.println("***** Test GeometryCookbook : an untextured Disc has its triangles *****");

		Disc d = new Disc(1, 12);
		d.build();
		assertEquals(24, d.getNbTriangles());
	}

	@Test
	public void testClosedCylinderAndClosedCone_areWoundOutwards() {
		System.out.println("***** Test GeometryCookbook : ClosedCylinder and ClosedCone enclose their volume (bottom cap facing outwards) *****");

		ClosedCylinder cyl = new ClosedCylinder(2, 1, 24);
		cyl.build();
		// Volume of the inscribed 48-gon prism: 2 * (48/2) sin(2 pi / 48)
		assertEquals(2 * 24 * Math.sin(2 * Math.PI / 48), ElementContractChecker.signedVolume(cyl), 1e-3);

		ClosedCone cone = new ClosedCone(3, 1, 24);
		cone.build();
		assertEquals(3.0 / 3 * 24 * Math.sin(2 * Math.PI / 48), ElementContractChecker.signedVolume(cone), 1e-3);
	}

	// ---- Recipe 4: assembly ----

	@Test
	public void testStreetLamp_treeAndContracts() {
		System.out.println("***** Test GeometryCookbook : StreetLamp is a tree of Elements; its own parts fulfill the contracts *****");

		StreetLamp lamp = new StreetLamp(2.6f, 0);
		lamp.build();

		assertEquals(0, lamp.getNbTriangles()); // a group: no geometry of its own
		assertEquals(3, lamp.getSubElements().size()); // base, pole, arm
		assertEquals(3, lamp.getArm().getSubElements().size()); // bar, shade, bulb

		// The bulb is a Sphere, which does not fulfill C6 (wound inwards, non-unit normals: known issues,
		// see docs/BACKLOG.md): check every other part.
		for (Element part : lamp.getSubElements()) {
			if (part != lamp.getArm()) {
				assertEquals(part.getName(), NO_VIOLATION, ElementContractChecker.check(part));
			}
		}
		for (Element part : lamp.getArm().getSubElements()) {
			if (part != lamp.getBulb()) {
				assertEquals(part.getName(), NO_VIOLATION, ElementContractChecker.check(part));
			}
		}
	}

	@Test
	public void testStreetLamp_transformationsComposeDownTheTree() {
		System.out.println("***** Test GeometryCookbook : the shade follows the pivot of the arm and the position of the lamp *****");

		float poleHeight = 2.6f;
		float jointZ = StreetLamp.BASE_HEIGHT + poleHeight;
		float tilt = (float) Math.toRadians(30);

		StreetLamp lamp = new StreetLamp(poleHeight, tilt);
		lamp.setTransformation(new Translation(new Vector3(10, 20, 0))); // set AFTER the parts: propagated down
		World world = new World();
		world.addElement(lamp);
		world.build();
		world.worldProject();

		// The average of the shade's 8 vertices is the origin of the shade's own frame (a PyramidFrustum is
		// symmetric around it in X and Y, and its 4 bottom + 4 top vertices average to z = 0), which
		// the shade's transformation places at (ARM_LENGTH, 0, -SHADE_HEIGHT/2) in the arm's frame.
		Vector4 center = new Vector4(0, 0, 0, 0);
		for (Vertex v : lamp.getShade().getVertices()) {
			center = center.plus(v.getWorldPos());
		}
		center = center.times(1f / lamp.getShade().getNbVertices());

		// Arm's frame: tilted by 'tilt' around -Y (the +X axis goes up), origin at the joint
		float ax = StreetLamp.ARM_LENGTH, az = -StreetLamp.SHADE_HEIGHT / 2;
		float x = (float) (ax * Math.cos(tilt) - az * Math.sin(tilt));
		float z = (float) (ax * Math.sin(tilt) + az * Math.cos(tilt));
		assertEquals(10 + x, center.getX(), 1e-4f);
		assertEquals(20, center.getY(), 1e-4f);
		assertEquals(jointZ + z, center.getZ(), 1e-4f);
	}

	@Test
	public void testStreetLamp_colorsAreInherited() {
		System.out.println("***** Test GeometryCookbook : parts without a color inherit the lamp's one, the bulb overrides it *****");

		StreetLamp lamp = new StreetLamp(2.6f, 0);
		assertEquals(StreetLamp.LAMP_COLOR, lamp.getColor());
		assertNull(lamp.getArm().getColor());   // resolved at rendering time: lamp's color
		assertNull(lamp.getShade().getColor()); // idem (except its "top" face, colored per face)
		assertEquals(StreetLamp.BULB_COLOR, lamp.getBulb().getColor());
	}

	// ---- Off-screen rendering of the whole gallery ----

	@Test
	public void testGallery_rendersOffScreen() {
		System.out.println("***** Test GeometryCookbook : the cookbook gallery renders off-screen *****");

		BufferedImage img = CookbookGallery.render(200).getImageView(); // 160 x 90 pixels: quick
		int lamp = 0;
		for (int x = 0; x < img.getWidth(); x++) {
			for (int y = 0; y < img.getHeight(); y++) {
				Color c = new Color(img.getRGB(x, y));
				if (c.getGreen() > c.getRed() + 10 && c.getGreen() > c.getBlue()) lamp++; // greenish lamp pixels
			}
		}
		assertTrue("lamp pixels: " + lamp, lamp > 20);
	}
}
