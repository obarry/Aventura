package com.aventura.model.world.shape;

import com.aventura.model.world.Element;

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
 * Base class for an Element that generates its OWN geometry from constructor parameters (a Sphere, a
 * Box, a Cylinder...), as opposed to a plain Element used as a group/container node with no geometry of
 * its own (which stays a perfectly valid, supported use of Element directly -- see its own Javadoc).
 *
 * Element itself provides silent, do-nothing default bodies for generateVertices()/generateTriangles(),
 * which is the right behavior for a group node but meant that a new shape class forgetting to override
 * one of them failed silently (empty geometry, no warning) instead of failing to compile. Extending
 * GenerativeElement instead of Element directly turns that into a compile-time error: both methods are
 * redeclared abstract here, so a concrete subclass MUST implement them.
 *
 * All of this codebase's built-in primitive shapes (Sphere, Box, Cylinder, Cone, ConeFrustum, Torus,
 * Pyramid, Trellis, Disc, and their subclasses) extend this class rather than Element directly. Write any
 * new shape class the same way: extend GenerativeElement, not Element, unless the class is genuinely a
 * group/container with no geometry generation of its own.
 *
 * calculateNormals() is intentionally NOT redeclared abstract here: Element's default implementation
 * (flat per-Triangle normals) is a legitimate, correct choice for sharp-edged shapes (Box, Pyramid, Disc
 * all rely on it as-is) -- see the single rule documented on Generable.calculateNormals() for what an
 * override may and may not do.
 *
 * @author Olivier BARRY
 * @since 2026
 */
public abstract class GenerativeElement extends Element {

	public GenerativeElement() {
		super();
	}

	public GenerativeElement(String name) {
		super(name);
	}

	public GenerativeElement(boolean isClosed) {
		super(isClosed);
	}

	public GenerativeElement(String name, boolean isClosed) {
		super(name, isClosed);
	}

	@Override
	public abstract void generateVertices();

	@Override
	public abstract void generateTriangles();

}
