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
package com.aventura.model.world;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.aventura.model.texture.Texture;
import com.aventura.model.world.shape.Box;

/**
 * The statistics of a geometry (GeometryStatistics, Element.getStatistics(), World.getStatistics()) and what they
 * rely on in a Texture (its name and the memory of its texels).
 *
 * The scene of the tests: a World with a group of two boxes, one of them holding a third box, and a box of its own.
 * A Box has 12 triangles and 8 vertices.
 * <pre>
 *   World
 *    ├── group            (no triangle)
 *    │    ├── boxA        marble, 12 triangles
 *    │    │    └── boxC   wood, 12 triangles
 *    │    └── boxB        marble, 12 triangles
 *    └── boxD             no texture, 12 triangles
 * </pre>
 *
 * @since Nov 2026
 */
public class TestGeometryStatistics {

	private final Texture marble = new Texture(8, 4), wood = new Texture(2, 2);
	private Element group;
	private Box boxA, boxB, boxC, boxD;

	private World createWorld() {
		World world = new World();
		group = new Element("group");
		boxA = new Box(1, 1, 1, marble);
		boxB = new Box(1, 1, 1, marble);
		boxC = new Box(1, 1, 1, wood);
		boxD = new Box(1, 1, 1);
		boxA.addElement(boxC);
		group.addElement(boxA);
		group.addElement(boxB);
		world.addElement(group);
		world.addElement(boxD);
		world.build();
		return world;
	}

	@Test
	public void testStatisticsOfAnElement_countItsWholeBranch() {
		System.out.println("***** Test GeometryStatistics : an Element and all its sub-Elements *****");
		createWorld();
		GeometryStatistics statistics = group.getStatistics();
		assertEquals("the group and its 3 boxes", 4, statistics.getNbElements());
		assertEquals("the group has no triangle of its own", 1, statistics.getNbGroups());
		assertEquals(36, statistics.getNbTriangles());
		assertEquals(24, statistics.getNbVertices());
		assertEquals("group, boxA, boxC", 3, statistics.getDepth());
		assertEquals(36, statistics.getNbTexturedTriangles());

		// The counters of the Element itself only count what it owns
		assertEquals(0, group.getNbTriangles());
		assertEquals(12, boxA.getNbTriangles());
		assertEquals("boxA and boxC", 24, boxA.getStatistics().getNbTriangles());

		GeometryStatistics leaf = boxB.getStatistics();
		assertEquals(1, leaf.getNbElements());
		assertEquals(0, leaf.getNbGroups());
		assertEquals(1, leaf.getDepth());
	}

	@Test
	public void testStatisticsOfAWorld_andOfSeveralElements() {
		System.out.println("***** Test GeometryStatistics : a World, a list of Elements *****");
		World world = createWorld();
		GeometryStatistics statistics = world.getStatistics();
		assertEquals(5, statistics.getNbElements());
		assertEquals(48, statistics.getNbTriangles());
		assertEquals(32, statistics.getNbVertices());
		assertEquals(3, statistics.getDepth());
		assertEquals("boxD has no texture", 36, statistics.getNbTexturedTriangles());

		// Two Elements together: the sum of both branches
		GeometryStatistics both = GeometryStatistics.of(Arrays.asList(boxA, boxB));
		assertEquals(3, both.getNbElements());
		assertEquals(36, both.getNbTriangles());
		assertEquals("boxA and boxC below it", 2, both.getDepth());

		GeometryStatistics nothing = GeometryStatistics.of(new ArrayList<Element>());
		assertEquals(0, nothing.getNbElements());
		assertEquals(0, nothing.getDepth());
		assertTrue(nothing.getTextures().isEmpty());

		assertEquals("5 elements (1 groups), 48 triangles, 32 vertices, depth 3, 2 textures (0.0 MB)", statistics.toString());
	}

	@Test
	public void testTextures_eachOneOnceWithItsTriangles() {
		System.out.println("***** Test GeometryStatistics : the textures used *****");
		GeometryStatistics statistics = createWorld().getStatistics();
		List<Texture> textures = new ArrayList<Texture>(statistics.getTextures());
		assertEquals("marble is used by two boxes, and counted once", 2, textures.size());
		assertSame("in the order they are met in the tree", marble, textures.get(0));
		assertSame(wood, textures.get(1));
		assertEquals(24, statistics.getNbTrianglesUsing(marble));
		assertEquals(12, statistics.getNbTrianglesUsing(wood));
		assertEquals(0, statistics.getNbTrianglesUsing(new Texture(1, 1)));
		assertEquals("4 bytes per texel: 8 x 4 and 2 x 2 texels", 4 * 32 + 4 * 4, statistics.getTextureMemorySize());
	}

	@Test
	public void testStatisticsAreASnapshot() {
		System.out.println("***** Test GeometryStatistics : computed when asked *****");
		World world = createWorld();
		GeometryStatistics before = world.getStatistics();
		Box late = new Box(1, 1, 1);
		late.build();
		world.addElement(late);
		assertEquals("the statistics already computed do not change", 5, before.getNbElements());
		assertEquals(6, world.getStatistics().getNbElements());
		assertEquals(60, world.getStatistics().getNbTriangles());
	}

	@Test
	public void testWorldCounters_directAndAll() {
		System.out.println("***** Test GeometryStatistics : the counters of the World *****");
		World world = createWorld();
		// Directly in the World: the group and boxD
		assertEquals(2, world.getNbElements());
		assertEquals("boxD only: the group has none of its own", 12, world.getNbTriangles());
		assertEquals(8, world.getNbVertices());
		// Sub-Elements of every level included
		assertEquals(5, world.getNbAllElements());
		assertEquals(48, world.getNbAllTriangles());
		assertEquals(32, world.getNbAllVertices());
		// Below a given Element, itself excluded
		assertEquals(3, world.getNbElements(group));
		assertEquals(36, world.getNbTriangles(group));
		assertEquals(24, world.getNbVertices(group));
		assertEquals("boxC, below boxA", 12, world.getNbTriangles(boxA));
		assertEquals(0, world.getNbElements(boxD));
	}

	@Test
	public void testReport_oneLinePerKindOfElement() {
		System.out.println("***** Test GeometryStatistics : the report of a World *****");
		World world = createWorld();
		marble.setName("marble");
		String report = GeometryReport.of(world);
		System.out.println(report);
		String[] lines = report.split("\\R");

		// Geometry: the whole, then each kind of Element of each level, indented by its level
		assertTrue(lines[0].matches("GEOMETRY +elements +triangles +vertices +share"));
		assertTrue(lines[1], lines[1].matches("World +5 +48 +32 +100\\.0%"));
		assertTrue(lines[2], lines[2].matches("  group +4 +36 +24 +75\\.0%"));
		assertTrue("boxA and boxB together, with boxC below boxA: " + lines[3], lines[3].matches("    box x 2 +3 +36 +24 +75\\.0%"));
		assertTrue(lines[4], lines[4].matches("      box +1 +12 +8 +25\\.0%"));
		assertTrue("boxD, directly in the World: " + lines[5], lines[5].matches("  box +1 +12 +8 +25\\.0%"));
		assertEquals("3 levels of Elements, 1 of them without triangles of their own (groups)", lines[6]);

		// Textures: each one with its size, its memory and its triangles, then the triangles without texture, and the total
		assertTrue(lines[8].matches("TEXTURES +pixels +memory +triangles"));
		assertTrue(lines[9], lines[9].matches("  marble +8 x 4 +128 B +24"));
		assertTrue(lines[10], lines[10].matches("  2 x 2 texture +2 x 2 +16 B +12"));
		assertTrue(lines[11], lines[11].matches("  \\(no texture\\) +12"));
		assertTrue(lines[12], lines[12].matches("2 textures +144 B +48"));

		// The report of a branch: the same, from an Element
		String branch = GeometryReport.of(boxA);
		assertTrue(branch, branch.split("\\R")[1].matches("box +2 +24 +16 +100\\.0%"));
		assertTrue(branch, branch.split("\\R")[2].matches("  box +1 +12 +8 +50\\.0%"));
		String leaf = GeometryReport.of(boxD);
		assertTrue(leaf, leaf.split("\\R")[1].matches("box +1 +12 +8 +100\\.0%"));
	}

	@Test
	public void testReport_kindsAndUnits() {
		System.out.println("***** Test GeometryStatistics : kinds of Elements, units of memory *****");
		createWorld();
		assertEquals("the name, without the id that makes it unique", "group", GeometryReport.kindOf(group));
		assertEquals("the class says no more than the name", "box", GeometryReport.kindOf(boxA));
		boxA.setName("altar");
		assertEquals("the class of a shape with a name of its own", "altar (Box)", GeometryReport.kindOf(boxA));
		Element plain = new Element("lamp");
		assertEquals("lamp", GeometryReport.kindOf(plain));

		assertEquals("12 B", GeometryReport.memory(12));
		assertEquals("256.0 KB", GeometryReport.memory(256 * 1024));
		assertEquals("3.5 MB", GeometryReport.memory(3 * 1024 * 1024 + 512 * 1024));
	}

	@Test
	public void testTextureNameAndMemory() {
		System.out.println("***** Test GeometryStatistics : name and memory of a Texture *****");
		Texture computed = new Texture(16, 8);
		assertEquals("a default name from its size", "16 x 8 texture", computed.getName());
		assertSame("setName() returns the texture", computed, computed.setName("clouds"));
		assertEquals("clouds", computed.getName());
		assertEquals(4L * 16 * 8, computed.getMemorySize());

		// From a file: the name of the file, without its directory; 4 bytes per texel, whatever the size of the file
		Texture file = new Texture("resources/texture/texture_woodfloor_160x160.jpg");
		assertEquals("texture_woodfloor_160x160.jpg", file.getName());
		assertEquals(4L * 160 * 160, file.getMemorySize());
		Texture turned = new Texture("resources/texture/texture_woodfloor_160x160.jpg", Texture.TEXTURE_DIRECTION_HORIZONTAL,
				Texture.TEXTURE_ORIENTATION_NORMAL, Texture.TEXTURE_ORIENTATION_NORMAL);
		assertEquals("texture_woodfloor_160x160.jpg", turned.getName());
	}
}
