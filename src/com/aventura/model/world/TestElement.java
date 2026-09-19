package com.aventura.model.world;

import static org.junit.Assert.*;

import org.junit.Test;

import com.aventura.math.transform.Rotation;
import com.aventura.math.transform.Scaling;
import com.aventura.math.transform.Transformation;
import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;

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
 * Tests for Element's transformation hierarchy: does "full" (the accumulated Element/local -> World
 * matrix) compose correctly across several levels, and does it stay correct regardless of the order in
 * which the tree is built.
 *
 * These live in this package (rather than e.g. com.aventura.test) specifically so they can read
 * Element's protected "transform" field (the LOCAL, not accumulated, matrix) directly -- that lets each
 * test recompute the expected "full" independently of Element.propagateTransformation(), instead of
 * calling the very method under test to build its own expected value.
 *
 * @author Olivier BARRY
 * @since 2026
 */
public class TestElement {

	private static final float EPS_FREE_POINT_W = 1f;

	@Test
	public void testHierarchyComposition_threeLevelsNonTrivialTransforms() {
		System.out.println("***** Test Element : testHierarchyComposition_threeLevelsNonTrivialTransforms *****");

		Element root = new Element("root");
		root.setTransformation(new Translation(new Vector3(10, 0, 0)));

		Element child = new Element("child");
		child.setTransformation(new Rotation((float) Math.PI / 2, Vector3.zAxis()));

		Element grandchild = new Element("grandchild");
		// Scaling extends Matrix4 directly (unlike Rotation/Translation, which extend Transformation),
		// so it must be wrapped to be passed to setTransformation()/combineTransformation().
		grandchild.setTransformation(new Transformation(new Scaling(2)));

		// Build the tree AFTER each Element already has its own local transform set, and in "root first"
		// order -- the straightforward case, which the pre-fix setParent() also handled correctly (the
		// bug it had was specific to attaching a subtree that already had its own children -- see the
		// regression test below).
		root.addElement(child);
		child.addElement(grandchild);

		// Recompute the expected World-space matrix independently: directly off each Element's own LOCAL
		// "transform" field, not off anyone's "full" -- so this does not just call
		// propagateTransformation() a second time and compare it to itself.
		Matrix4 expectedFull = root.transform.times(child.transform).times(grandchild.transform);

		Vector4 p = new Vector4(1, 0, 0, EPS_FREE_POINT_W);
		Vector4 expected = expectedFull.times(p);
		Vector4 actual = grandchild.getTransformation().times(p);

		System.out.println("Expected world position: " + expected);
		System.out.println("Actual world position:   " + actual);

		assertTrue("grandchild.getTransformation() (\"full\") must equal root.transform * child.transform * "
				+ "grandchild.transform, applied to a local point -- see Element's own documented "
				+ "[Element Matrix] * [SubElement Matrix] * OriginalVector formula",
				expected.equals(actual));
	}

	@Test
	public void testSetParent_propagatesToPreexistingGrandchildren_regression() {
		System.out.println("***** Test Element : testSetParent_propagatesToPreexistingGrandchildren_regression *****");

		// REGRESSION for the setParent()/addElement() propagation gap: before the fix, setParent() only
		// recomputed "full" for the direct child being attached, not for that child's own pre-existing
		// sub-Elements. Building a sub-tree (sub -> leaf) BEFORE attaching "sub" to "root" is exactly the
		// scenario that exposed it -- leaf.full used to keep reflecting "no parent" after root.addElement(sub).

		Element root = new Element("root");
		root.setTransformation(new Translation(new Vector3(5, 0, 0)));

		Element sub = new Element("sub");
		sub.setTransformation(new Rotation((float) Math.PI / 2, Vector3.zAxis()));

		Element leaf = new Element("leaf");
		leaf.setTransformation(new Transformation(new Scaling(3)));

		// leaf is attached to sub WHILE sub still has no parent of its own.
		sub.addElement(leaf);

		// Only now is the whole "sub" subtree attached to "root".
		root.addElement(sub);

		Matrix4 expectedLeafFull = root.transform.times(sub.transform).times(leaf.transform);

		Vector4 p = new Vector4(1, 0, 0, EPS_FREE_POINT_W);
		Vector4 expected = expectedLeafFull.times(p);
		Vector4 actual = leaf.getTransformation().times(p);

		System.out.println("Expected leaf world position (including root's transform): " + expected);
		System.out.println("Actual leaf world position:                                 " + actual);

		assertTrue("leaf.getTransformation() must include root's transform even though leaf was attached "
				+ "to sub BEFORE sub was attached to root -- setParent() must propagate to the whole "
				+ "subtree it is given, not just recompute its own direct child's \"full\"",
				expected.equals(actual));
	}
}
