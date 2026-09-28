package com.aventura.cookbook;

import java.awt.Color;

import com.aventura.math.vector.Vector4;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.Element;
import com.aventura.model.world.Vertex;
import com.aventura.model.world.shape.GenerativeElement;
import com.aventura.model.world.triangle.RectangleMesh;

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
 * Cookbook recipe 1 (see docs/GEOMETRY_COOKBOOK.md): a new primitive shape written from scratch.
 *
 * A frustum of a rectangular pyramid: a rectangular bottom face, a smaller (or larger) rectangular
 * top face parallel to it, and 4 trapezoidal side faces. Also known as a truncated pyramid; with
 * equal top and bottom it degenerates into a Box, and it can model a lamp shade, a roof, a tower
 * base, a plinth...
 *
 *              +--------+            top face:    topX x topY    at z = +height/2
 *             /        /|
 *     +------+--------+ |
 *     |       \      /  |            4 trapezoidal side faces
 *     |        +----+   +
 *     |                /
 *     +---------------+              bottom face: bottomX x bottomY at z = -height/2
 *
 * Local frame (same conventions as Box and Pyramid): centered on the origin, Z up, faces named
 * as in the Shape interface: top (+Z), bottom (-Z), left (-Y), right (+Y), front (+X), back (-X).
 *
 * Geometry: 8 vertices, 6 quadrilateral faces of 2 triangles each = 12 triangles. Every face is
 * planar, so the flat normals computed by Element.calculateNormals() (the default, not overridden)
 * are exact. The element is closed (watertight, every triangle wound counter-clockwise when seen
 * from outside), so back-face culling can be enabled on it.
 *
 * @author Olivier BARRY
 * @since 2026
 */
public class PyramidFrustum extends GenerativeElement {

	protected static final String PYRAMID_FRUSTUM_DEFAULT_NAME = "pyramid frustum";

	// Parameters (set by the constructor, read by generateVertices())
	protected final float bottomX, bottomY, topX, topY, height;

	// Per-face appearance: optional, must be set BEFORE build() (read by generateVertices())
	protected Texture topTex, bottomTex, leftTex, rightTex, frontTex, backTex;
	protected Color topCol, bottomCol, leftCol, rightCol, frontCol, backCol;

	// Generated geometry, kept so that generateTriangles() (called again by rebuild()) can reuse it
	protected Vertex[][] bottom, top; // [i][j]: i = 0 for -X, 1 for +X ; j = 0 for -Y, 1 for +Y
	protected RectangleMesh topMesh, bottomMesh, leftMesh, rightMesh, frontMesh, backMesh;

	/**
	 * @param bottomX size of the bottom face along X (> 0)
	 * @param bottomY size of the bottom face along Y (> 0)
	 * @param topX    size of the top face along X (> 0: use Pyramid for a pointed top)
	 * @param topY    size of the top face along Y (> 0)
	 * @param height  distance between the two faces, along Z (> 0)
	 * @throws IllegalArgumentException if a dimension is not strictly positive
	 */
	public PyramidFrustum(float bottomX, float bottomY, float topX, float topY, float height) {
		super(PYRAMID_FRUSTUM_DEFAULT_NAME, true); // closed: watertight and consistently wound
		// Validate here, not in generateVertices(): the error then points at the caller's line.
		// A zero-sized top would create degenerate (zero-area) triangles whose normal is NaN.
		if (bottomX <= 0 || bottomY <= 0 || topX <= 0 || topY <= 0 || height <= 0) {
			throw new IllegalArgumentException("PyramidFrustum dimensions must be > 0: bottom " + bottomX + " x " + bottomY
					+ ", top " + topX + " x " + topY + ", height " + height);
		}
		this.bottomX = bottomX;
		this.bottomY = bottomY;
		this.topX = topX;
		this.topY = topY;
		this.height = height;
		// No geometry here: it is created by build() -> generateVertices() / generateTriangles().
	}

	/** Same, with one texture wrapped on each of the 6 faces. */
	public PyramidFrustum(float bottomX, float bottomY, float topX, float topY, float height, Texture tex) {
		this(bottomX, bottomY, topX, topY, height);
		topTex = bottomTex = leftTex = rightTex = frontTex = backTex = tex;
	}

	@Override
	public void generateVertices() {
		float bx = bottomX / 2, by = bottomY / 2, tx = topX / 2, ty = topY / 2, h = height / 2;

		// Every Vertex is created through createVertex(): this registers it in the Element, which
		// is what makes the engine transform (and project) it. Positions are points: w = 1.
		bottom = new Vertex[2][2];
		top = new Vertex[2][2];
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 2; j++) {
				float sx = i == 0 ? -1 : 1, sy = j == 0 ? -1 : 1;
				bottom[i][j] = createVertex(new Vector4(sx * bx, sy * by, -h, 1));
				top[i][j] = createVertex(new Vector4(sx * tx, sy * ty, h, 1));
			}
		}

		// One RectangleMesh (2 x 2 vertices) per face. A RectangleMesh on vertices a[i][j] builds the
		// triangles (a00, a10, a11) and (a11, a01, a00): their normal is U x V, where U is the direction
		// of the FIRST index (a00 -> a10) and V the direction of the SECOND one (a00 -> a01). So for each
		// face, U and V are chosen such that U x V points OUTWARDS. For the side faces, V goes from the
		// bottom to the top (so that textures stand upright) and U is the horizontal direction that
		// makes U x Z outward.
		//                                            U (1st index)   V (2nd index)   U x V
		topMesh    = face(top[0][0],    top[1][0],    top[0][1],    top[1][1],    topTex,    topCol);    //  +X    +Y    +Z
		bottomMesh = face(bottom[0][0], bottom[0][1], bottom[1][0], bottom[1][1], bottomTex, bottomCol); //  +Y    +X    -Z
		leftMesh   = face(bottom[0][0], bottom[1][0], top[0][0],    top[1][0],    leftTex,   leftCol);   //  +X    up    -Y
		rightMesh  = face(bottom[1][1], bottom[0][1], top[1][1],    top[0][1],    rightTex,  rightCol);  //  -X    up    +Y
		frontMesh  = face(bottom[1][0], bottom[1][1], top[1][0],    top[1][1],    frontTex,  frontCol);  //  +Y    up    +X
		backMesh   = face(bottom[0][1], bottom[0][0], top[0][1],    top[0][0],    backTex,   backCol);   //  -Y    up    -X
	}

	/**
	 * Creates the mesh of one quadrilateral face from its 4 corners: a00 the origin corner, a10 the
	 * corner reached along U, a01 the corner reached along V, a11 the opposite corner.
	 */
	private RectangleMesh face(Vertex a00, Vertex a10, Vertex a01, Vertex a11, Texture tex, Color col) {
		RectangleMesh mesh = new RectangleMesh(this, new Vertex[][] { { a00, a01 }, { a10, a11 } }, tex);
		mesh.setCol(col); // null: the face inherits the color of the Element (or of its parents)
		return mesh;
	}

	@Override
	public void generateTriangles() {
		// Only triangles here, from the vertices created above: rebuild() calls this method again
		// (after clearing the triangles) but NOT generateVertices().
		topMesh.createTriangles(RectangleMesh.MESH_ORIENTED_TRIANGLES);
		bottomMesh.createTriangles(RectangleMesh.MESH_ORIENTED_TRIANGLES);
		leftMesh.createTriangles(RectangleMesh.MESH_ORIENTED_TRIANGLES);
		rightMesh.createTriangles(RectangleMesh.MESH_ORIENTED_TRIANGLES);
		frontMesh.createTriangles(RectangleMesh.MESH_ORIENTED_TRIANGLES);
		backMesh.createTriangles(RectangleMesh.MESH_ORIENTED_TRIANGLES);
	}

	// calculateNormals() is not overridden: Element's default (one flat normal per triangle) is exact
	// for planar faces with sharp edges.

	/** Exact volume (prismatoid formula), handy to check the winding: see ElementContractChecker. */
	public float getVolume() {
		float midX = (bottomX + topX) / 2, midY = (bottomY + topY) / 2;
		return height / 6 * (bottomX * bottomY + 4 * midX * midY + topX * topY);
	}

	// Shape interface: per-face color and texture. They only record the value, read by
	// generateVertices(): call them BEFORE build(). Returning this allows chaining.

	@Override public Element setTopTexture(Texture tex)    { topTex = tex;    return this; }
	@Override public Element setBottomTexture(Texture tex) { bottomTex = tex; return this; }
	@Override public Element setLeftTexture(Texture tex)   { leftTex = tex;   return this; }
	@Override public Element setRightTexture(Texture tex)  { rightTex = tex;  return this; }
	@Override public Element setFrontTexture(Texture tex)  { frontTex = tex;  return this; }
	@Override public Element setBackTexture(Texture tex)   { backTex = tex;   return this; }

	@Override public Element setTopColor(Color c)    { topCol = c;    return this; }
	@Override public Element setBottomColor(Color c) { bottomCol = c; return this; }
	@Override public Element setLeftColor(Color c)   { leftCol = c;   return this; }
	@Override public Element setRightColor(Color c)  { rightCol = c;  return this; }
	@Override public Element setFrontColor(Color c)  { frontCol = c;  return this; }
	@Override public Element setBackColor(Color c)   { backCol = c;   return this; }
}
