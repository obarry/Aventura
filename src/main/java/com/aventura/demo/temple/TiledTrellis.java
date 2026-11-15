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
package com.aventura.demo.temple;

import com.aventura.math.vector.Vector4;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.Vertex;
import com.aventura.model.world.WrongArraySizeException;
import com.aventura.model.world.shape.Trellis;
import com.aventura.model.world.triangle.Triangle;

/**
 * A {@link Trellis} whose texture is repeated instead of being stretched once over the whole surface: an example of a
 * built-in shape <b>enriched by inheritance</b> (Geometry Cookbook, recipe 3).
 *
 * <p>A Trellis maps its texture once on its whole grid, which is right for a small object but gives a blurred ground
 * when the grid is a landscape of hundreds of meters. Here the texture covers a square "tile" of a few cells, and the
 * tiles are laid side by side.
 *
 * <p>The texture coordinates of Aventura go from 0 to 1 (beyond, the border of the texture is repeated), so a tile
 * cannot simply be given the coordinates 1 to 2. Instead, <b>every other tile is mirrored</b>: the coordinate goes
 * from 0 to 1 in a tile, then back from 1 to 0 in the next one ({@link #mirror}). A side effect is welcome: the
 * borders of two neighbor tiles show the same pixels, so no seam is visible, whatever the image.
 * <pre>
 *  tile:        0       1       2       3
 *  u:       0 -----> 1 -----> 0 -----> 1 -----> 0
 * </pre>
 *
 * <p>Only {@link #generateTriangles()} is overridden: the vertices and the smooth normals are the ones of the Trellis.
 * The texture coordinates belong to the triangles (three per triangle), not to the vertices, which is why a vertex
 * shared by two tiles needs no duplication.
 *
 * @since Nov 2026
 */
public class TiledTrellis extends Trellis {

	protected final Texture tileTexture;
	protected final int cellsPerTile;

	/**
	 * A tiled Trellis with altitudes, centered on the origin.
	 *
	 * @param width        size on the X axis
	 * @param length       size on the Y axis
	 * @param nx           number of cells on the X axis
	 * @param ny           number of cells on the Y axis
	 * @param altitudes    altitudes of the (nx + 1) x (ny + 1) vertices
	 * @param texture      the texture of a tile
	 * @param cellsPerTile number of cells of the side of a tile, at least 1
	 */
	public TiledTrellis(float width, float length, int nx, int ny, float[][] altitudes, Texture texture, int cellsPerTile)
			throws WrongArraySizeException {
		super(width, length, nx, ny, altitudes, texture);
		if (cellsPerTile < 1) throw new IllegalArgumentException("A tile is at least one cell: " + cellsPerTile);
		this.tileTexture = texture;
		this.cellsPerTile = cellsPerTile;
	}

	/** A flat tiled Trellis (altitude 0), centered on the origin: see the other constructor for the parameters. */
	public TiledTrellis(float width, float length, int nx, int ny, Texture texture, int cellsPerTile) {
		super(width, length, nx, ny, texture);
		if (cellsPerTile < 1) throw new IllegalArgumentException("A tile is at least one cell: " + cellsPerTile);
		this.tileTexture = texture;
		this.cellsPerTile = cellsPerTile;
	}

	/**
	 * Two triangles per cell, as in a Trellis: counter-clockwise seen from above (the face normal points up), cut
	 * along a diagonal that alternates from one cell to the next, like a checkerboard, so that the relief has no
	 * preferred direction.
	 * <pre>
	 *  j+1  d---c        d---c
	 *       | / |   or   | \ |
	 *   j   a---b        a---b
	 *       i  i+1
	 * </pre>
	 */
	@Override
	public void generateTriangles() {
		for (int i = 0; i < nx; i++) {
			for (int j = 0; j < ny; j++) {
				Vertex a = rectangleMesh.getVertex(i, j), b = rectangleMesh.getVertex(i + 1, j);
				Vertex c = rectangleMesh.getVertex(i + 1, j + 1), d = rectangleMesh.getVertex(i, j + 1);

				// Texture coordinates of the 4 corners of the cell, in the mirrored tiles
				float u0 = mirror((float) i / cellsPerTile), u1 = mirror((float) (i + 1) / cellsPerTile);
				float v0 = mirror((float) j / cellsPerTile), v1 = mirror((float) (j + 1) / cellsPerTile);
				Vector4 ta = new Vector4(u0, v0, 0, 1), tb = new Vector4(u1, v0, 0, 1);
				Vector4 tc = new Vector4(u1, v1, 0, 1), td = new Vector4(u0, v1, 0, 1);

				if ((i + j) % 2 == 0) {
					addTexturedTriangle(a, b, c, ta, tb, tc);
					addTexturedTriangle(c, d, a, tc, td, ta);
				} else {
					addTexturedTriangle(d, a, b, td, ta, tb);
					addTexturedTriangle(b, c, d, tb, tc, td);
				}
			}
		}
	}

	private void addTexturedTriangle(Vertex v1, Vertex v2, Vertex v3, Vector4 t1, Vector4 t2, Vector4 t3) {
		Triangle triangle = new Triangle(v1, v2, v3);
		triangle.setTexture(tileTexture, t1, t2, t3);
		addTriangle(triangle);
	}

	/**
	 * Texture coordinate of a position counted in tiles, with every other tile mirrored: a triangle wave between 0
	 * and 1 (0 at even integers, 1 at odd ones).
	 */
	public static float mirror(float tiles) {
		float t = tiles % 2;
		if (t < 0) t += 2;
		return t <= 1 ? t : 2 - t;
	}
}
