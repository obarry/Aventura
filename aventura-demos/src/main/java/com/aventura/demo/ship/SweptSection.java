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
package com.aventura.demo.ship;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.Vertex;
import com.aventura.model.world.shape.GenerativeElement;
import com.aventura.model.world.triangle.Triangle;

/**
 * A new shape: a cross-section swept along a {@link PathSegment}, straight or curved. One class, many parts of the
 * ship: the shell of a corridor (an octagon seen from inside), the pipes (circles seen from outside), the cable
 * trays, the handrails...
 * <pre>
 *        height                                        the section is given as a list of points
 *          ^      ___________                          (lateral, height): lateral is the distance to the
 *          |     /           \    the octagon of       left of the path, height the altitude above it.
 *          |    |             |   a corridor, seen
 *          |    |             |   in the direction     Each point of the section, at each step along the
 *          |     \___________/    of travel            path, becomes a vertex:
 *          +-------------------&gt; lateral                   point(s) + lateral * left(s) + height * Z
 * </pre>
 *
 * <h2>How it is made</h2>
 * <ul>
 * <li><b>Steps along the path</b>: a straight segment needs no intermediate step for its shape, but gets one every
 *     {@link #MAX_STEP_LENGTH} m to keep the triangles of a reasonable size; an arc gets one every
 *     {@link #MAX_STEP_ANGLE_DEGREES} degrees, so that its polygon follows the curve.</li>
 * <li><b>Which side is seen</b> ({@link Facing}): the triangles turn counter-clockwise when seen from the side that
 *     is meant to be seen (inside a corridor, outside a pipe). The Element is declared closed, so the engine skips the
 *     faces turned away from the camera (back-face culling): the walls of a corridor are invisible from outside,
 *     which is what lets the plan of the ship be seen from above, through the ceilings.</li>
 * <li><b>Normals</b> ({@link Shading}): a pipe is smooth all around (each vertex has the average normal of its two
 *     neighbor edges). The edges of a corridor are sharp: there each edge of the section has its own vertices, whose
 *     normal is the normal of that edge. In both cases the normals follow the path, so a curved corridor is smooth
 *     along its bend.</li>
 * <li><b>Textures</b>: each edge of the section can have its own texture (and its own color). The textures are
 *     repeated like tiles of a given size in meters, whatever the length of the corridor: the u coordinate is the
 *     distance along the path (measured on the edge itself: the outer wall of a bend is longer than the inner one),
 *     the v coordinate the distance along the section.</li>
 * <li><b>Decals</b> ({@link Decal}): on a piece of an edge, a texture shown once instead of the tiled one, upright as
 *     seen — the codes painted on the walls. The section gets a step at each end of a decal, so that it has triangles of
 *     its own.</li>
 * </ul>
 *
 * @since Nov 2026
 */
public class SweptSection extends GenerativeElement {

	/** Which side of the surface is seen. */
	public enum Facing {
		/** From inside, like the walls of a corridor */
		INSIDE,
		/** From outside, like a pipe */
		OUTSIDE
	}

	/** How the light falls on the surface across the section. */
	public enum Shading {
		/** Smooth all around: a pipe, a handrail */
		SMOOTH,
		/** A sharp edge at each point of the section: the panels of a corridor */
		SHARP_EDGES
	}

	/** Steps along a straight path at most this long (m), along an arc at most this angle (degrees). */
	public static final float MAX_STEP_LENGTH = 4f;
	public static final float MAX_STEP_ANGLE_DEGREES = 7.5f;

	protected final PathSegment path;
	protected final float[][] section;   // (lateral, height) points
	protected final boolean closedSection;
	protected final Facing facing;
	protected final Shading shading;
	protected final float altitude;      // Added to the heights: the path is at z = 0

	protected Texture[] edgeTextures;    // Per edge of the section, null for no texture
	protected Color[] edgeColors;        // Per edge of the section, null to inherit the color of the Element
	protected float tileAlong = 2f, tileAcross = 2f;

	protected float[] abscissas;         // The distances along the path where the section is placed: the steps
	protected Vertex[][][] vertices;     // [step][edge][0 = start of the edge, 1 = end of the edge]
	protected final List<Decal> decals = new ArrayList<Decal>();

	/**
	 * A decal: a texture shown once on a piece of an edge of the section, between two distances along the path, instead
	 * of the tiled texture of the edge — a sign, a code painted on a wall. Its texture is a picture, as seen from the
	 * side the surface is seen from: its first row at the top, its first column on the left (see addDecal()).
	 */
	public static class Decal {
		final int edge;
		final float from, to;
		final Texture texture;

		public Decal(int edge, float from, float to, Texture texture) {
			this.edge = edge;
			this.from = from;
			this.to = to;
			this.texture = texture;
		}
	}

	/**
	 * @param name          name of the Element
	 * @param path          the path the section is swept along
	 * @param section       the points of the section, (lateral, height), at least 2
	 * @param closedSection true if the last point of the section is joined to the first one (a pipe), false for an
	 *                      open profile (a cable tray, a single wall)
	 * @param facing        which side of the surface is seen
	 * @param shading       smooth or sharp edges across the section
	 */
	public SweptSection(String name, PathSegment path, float[][] section, boolean closedSection, Facing facing, Shading shading) {
		this(name, path, section, closedSection, facing, shading, 0);
	}

	/**
	 * The same, the whole shape raised by the given altitude (the heights of the section are measured from it).
	 */
	public SweptSection(String name, PathSegment path, float[][] section, boolean closedSection, Facing facing, Shading shading, float altitude) {
		// Closed: back-face culling applies (see the class comment)
		super(name, true);
		if (section.length < 2) throw new IllegalArgumentException("A section needs at least 2 points");
		this.path = path;
		this.section = section;
		this.closedSection = closedSection;
		this.facing = facing;
		this.shading = shading;
		this.altitude = altitude;
	}

	/**
	 * One texture (or null) for each edge of the section, repeated every tileAlong meters along the path and
	 * tileAcross meters along the section. To be called before the World is built.
	 */
	public SweptSection setTextures(Texture[] textures, float tileAlong, float tileAcross) {
		if (textures.length != nbEdges()) throw new IllegalArgumentException("One texture per edge: " + nbEdges());
		this.edgeTextures = textures;
		this.tileAlong = tileAlong;
		this.tileAcross = tileAcross;
		return this;
	}

	/** The same texture on every edge. */
	public SweptSection setTexture(Texture texture, float tileAlong, float tileAcross) {
		Texture[] textures = new Texture[nbEdges()];
		java.util.Arrays.fill(textures, texture);
		return setTextures(textures, tileAlong, tileAcross);
	}

	/** One color (or null, to inherit the color of the Element) for each edge of the section. */
	public SweptSection setEdgeColors(Color[] colors) {
		if (colors.length != nbEdges()) throw new IllegalArgumentException("One color per edge: " + nbEdges());
		this.edgeColors = colors;
		return this;
	}

	/** Number of edges of the section: one less than its points, unless it is closed. */
	public int nbEdges() {
		return closedSection ? section.length : section.length - 1;
	}

	public PathSegment getPath() {
		return path;
	}

	/**
	 * Adds a decal on an edge of the section, from the distance from to the distance to along the path. To be called
	 * before the World is built: the section gets a step at each end of the decal, so that the decal has triangles of its
	 * own. Meant for an edge that stands upright (a wall): the picture of the decal is shown upright, as seen from the
	 * side the surface is seen from, whatever the direction of the path and of the edge.
	 */
	public SweptSection addDecal(Decal decal) {
		if (decal.edge < 0 || decal.edge >= nbEdges() || decal.from < 0 || decal.to > path.getLength() || decal.to <= decal.from) {
			throw new IllegalArgumentException("A decal must be on an edge and within the path");
		}
		decals.add(decal);
		return this;
	}

	/**
	 * Where a point of the picture of a decal falls on the surface: its distance along the path and its position along the
	 * edge (from 0 at the start of the edge to 1 at its end).
	 *
	 * @param x from 0 (left of the picture) to 1 (right), as seen
	 * @param y from 0 (top of the picture) to 1 (bottom), as seen
	 * @return { distance along the path, fraction along the edge }
	 */
	public float[] pictureToSurface(int edge, float from, float to, float x, float y) {
		boolean[] orientation = pictureOrientation(edge, (from + to) / 2);
		float along = orientation[0] ? x : 1 - x;   // Fraction of the decal along the path
		float across = orientation[1] ? 1 - y : y;  // Fraction along the edge
		return new float[] { from + along * (to - from), across };
	}

	/**
	 * The coordinates, in the tiled texture of the edge, of a point of the surface (see generateTriangles()): what a decal
	 * must show at that point to continue the texture around it.
	 */
	public float[] tileCoordinates(int edge, float distance, float fraction) {
		float[] a = section[edge], b = section[(edge + 1) % section.length];
		float before = 0;
		for (int e = 0; e < edge; e++) {
			float[] p = section[e], q = section[(e + 1) % section.length];
			before += (float) Math.hypot(q[0] - p[0], q[1] - p[1]);
		}
		float edgeLength = (float) Math.hypot(b[0] - a[0], b[1] - a[1]);
		float lengthFactor = 1 - (a[0] + b[0]) / 2 * path.curvature();
		return new float[] { distance * lengthFactor / tileAlong, (before + fraction * edgeLength) / tileAcross };
	}

	/**
	 * How a picture is laid on an edge, seen from the side the surface is seen from: { true if the path goes from the
	 * left of the picture to its right, true if the edge goes from the bottom of the picture to its top }. The viewer
	 * looks against the normal of the edge (N), the vertical up: the right of the picture is (-N) x Z.
	 */
	private boolean[] pictureOrientation(int edge, float distance) {
		float[] a = section[edge], b = section[(edge + 1) % section.length];
		float[] n = edgeNormal(a, b);
		Vector3 left = path.left(distance), tangent = path.tangent(distance);
		Vector3 normal = new Vector3(n[0] * left.getX(), n[0] * left.getY(), n[1]);
		Vector3 right = normal.times(-1).cross(Vector3.zAxis());
		return new boolean[] { tangent.dot(right) > 0, b[1] > a[1] };
	}

	/** Number of steps along the path (see the class comment). */
	static int stepsFor(PathSegment path) {
		if (path.isArc()) {
			float degrees = (float) Math.toDegrees(path.getLength() / path.getRadius());
			return Math.max(1, (int) Math.ceil(degrees / MAX_STEP_ANGLE_DEGREES));
		}
		return Math.max(1, (int) Math.ceil(path.getLength() / MAX_STEP_LENGTH));
	}

	/**
	 * Twice the signed area of the section, in the (lateral, height) plane: positive if its points turn
	 * counter-clockwise.
	 */
	static float signedArea(float[][] points) {
		float area = 0;
		for (int i = 0; i < points.length; i++) {
			float[] a = points[i], b = points[(i + 1) % points.length];
			area += a[0] * b[1] - b[0] * a[1];
		}
		return area;
	}

	/**
	 * The normal of the edge from a to b, in the (lateral, height) plane, on the side that is seen. Turning the edge by
	 * +90 degrees gives the normal on its left, which is the inside of a section that turns counter-clockwise (see
	 * generateTriangles()).
	 */
	float[] edgeNormal(float[] a, float[] b) {
		float nl = -(b[1] - a[1]), nh = b[0] - a[0];
		boolean counterClockwise = signedArea(section) >= 0;
		// For an open profile the order of its points says which side is seen: the left of its edges for INSIDE
		boolean flip = (facing == Facing.INSIDE) != counterClockwise;
		if (!closedSection) flip = facing == Facing.OUTSIDE;
		float length = (float) Math.hypot(nl, nh);
		return flip ? new float[] { -nl / length, -nh / length } : new float[] { nl / length, nh / length };
	}

	/**
	 * The distances where the section is placed: regular steps (see stepsFor()), plus the two ends of each decal.
	 */
	private float[] computeAbscissas() {
		int steps = stepsFor(path);
		java.util.TreeSet<Float> set = new java.util.TreeSet<Float>();
		for (int k = 0; k <= steps; k++) set.add(path.getLength() * k / steps);
		for (Decal decal : decals) {
			set.add(decal.from);
			set.add(decal.to);
		}
		float[] result = new float[set.size()];
		int i = 0;
		for (float s : set) result[i++] = s;
		return result;
	}

	@Override
	public void generateVertices() {
		abscissas = computeAbscissas();
		int steps = abscissas.length - 1;
		int edges = nbEdges();
		vertices = new Vertex[steps + 1][edges][2];
		for (int k = 0; k <= steps; k++) {
			float s = abscissas[k];
			Vector4 p = path.point(s);
			Vector3 left = path.left(s);
			// One vertex per point of the section, shared by its two edges when the shading is smooth
			Vertex[] points = new Vertex[section.length];
			for (int i = 0; i < section.length; i++) {
				if (shading == Shading.SMOOTH) points[i] = createVertex(position(p, left, section[i]));
			}
			for (int e = 0; e < edges; e++) {
				int i = e, j = (e + 1) % section.length;
				if (shading == Shading.SMOOTH) {
					vertices[k][e][0] = points[i];
					vertices[k][e][1] = points[j];
				} else {
					// Sharp edges: this edge has its own two vertices (a vertex has only one normal)
					vertices[k][e][0] = createVertex(position(p, left, section[i]));
					vertices[k][e][1] = createVertex(position(p, left, section[j]));
				}
			}
		}
	}

	private Vector4 position(Vector4 p, Vector3 left, float[] point) {
		return new Vector4(p.getX() + point[0] * left.getX(), p.getY() + point[0] * left.getY(), altitude + point[1], 1);
	}

	/**
	 * Two triangles per edge of the section and per step: (a, b, c) and (c, d, a), where a and d are the two ends of the
	 * edge at one step, b and c at the next one. Their normal (b - a) x (c - a) is the normal of the edge given by
	 * edgeNormal() when the section turns the right way; otherwise the order of the vertices is reversed.
	 * <pre>
	 *   d ---- c     j: end of the edge
	 *   |    / |
	 *   |  /   |          ----&gt; along the path
	 *   a ---- b     i: start of the edge
	 *   step k   step k+1
	 * </pre>
	 */
	@Override
	public void generateTriangles() {
		int edges = nbEdges();
		float curvature = path.curvature();
		float[] across = new float[edges + 1]; // Distance along the section at the start of each edge, for the v coordinate
		for (int e = 0; e < edges; e++) {
			float[] a = section[e], b = section[(e + 1) % section.length];
			across[e + 1] = across[e] + (float) Math.hypot(b[0] - a[0], b[1] - a[1]);
		}
		for (int e = 0; e < edges; e++) {
			float[] pa = section[e], pb = section[(e + 1) % section.length];
			// The direction the triangles must turn: compare the normal wanted with the one (a, b, c) would give
			float[] wanted = edgeNormal(pa, pb);
			float[] natural = { -(pb[1] - pa[1]), pb[0] - pa[0] };
			boolean reverse = wanted[0] * natural[0] + wanted[1] * natural[1] < 0;
			Texture texture = edgeTextures == null ? null : edgeTextures[e];
			// Length of this edge along the path: longer on the outside of a bend (see the class comment)
			float lateral = (pa[0] + pb[0]) / 2;
			float lengthFactor = 1 - lateral * curvature;
			for (int k = 0; k < abscissas.length - 1; k++) {
				Vertex a = vertices[k][e][0], d = vertices[k][e][1], b = vertices[k + 1][e][0], c = vertices[k + 1][e][1];
				Triangle t1 = reverse ? new Triangle(a, c, b) : new Triangle(a, b, c);
				Triangle t2 = reverse ? new Triangle(c, a, d) : new Triangle(c, d, a);
				Decal decal = decalAt(e, (abscissas[k] + abscissas[k + 1]) / 2);
				if (decal != null) {
					// The picture of the decal, once over its piece of the edge, upright as seen (see pictureOrientation())
					boolean[] orientation = pictureOrientation(e, (decal.from + decal.to) / 2);
					float x0 = (abscissas[k] - decal.from) / (decal.to - decal.from), x1 = (abscissas[k + 1] - decal.from) / (decal.to - decal.from);
					if (!orientation[0]) {
						x0 = 1 - x0;
						x1 = 1 - x1;
					}
					float yStart = orientation[1] ? 1 : 0, yEnd = 1 - yStart; // The start of the edge at the bottom, or at the top
					Vector4 ta = new Vector4(x0, yStart, 0, 1), tb = new Vector4(x1, yStart, 0, 1);
					Vector4 tc = new Vector4(x1, yEnd, 0, 1), td = new Vector4(x0, yEnd, 0, 1);
					if (reverse) {
						t1.setTexture(decal.texture, ta, tc, tb);
						t2.setTexture(decal.texture, tc, ta, td);
					} else {
						t1.setTexture(decal.texture, ta, tb, tc);
						t2.setTexture(decal.texture, tc, td, ta);
					}
				} else if (texture != null) {
					float u0 = abscissas[k] * lengthFactor / tileAlong;
					float u1 = abscissas[k + 1] * lengthFactor / tileAlong;
					float v0 = across[e] / tileAcross, v1 = across[e + 1] / tileAcross;
					Vector4 ta = new Vector4(u0, v0, 0, 1), tb = new Vector4(u1, v0, 0, 1);
					Vector4 tc = new Vector4(u1, v1, 0, 1), td = new Vector4(u0, v1, 0, 1);
					if (reverse) {
						t1.setTexture(texture, ta, tc, tb);
						t2.setTexture(texture, tc, ta, td);
					} else {
						t1.setTexture(texture, ta, tb, tc);
						t2.setTexture(texture, tc, td, ta);
					}
				}
				if (edgeColors != null && edgeColors[e] != null) {
					t1.setColor(edgeColors[e]);
					t2.setColor(edgeColors[e]);
				}
				addTriangle(t1);
				addTriangle(t2);
			}
		}
	}

	/**
	 * The normals of the vertices: the normal of the section in the (lateral, height) plane, turned to follow the path
	 * at each step. With sharp edges each vertex belongs to one edge and takes its normal; with smooth shading a vertex
	 * takes the average of the normals of its two edges.
	 */
	/** The decal on the given edge at the given distance, or null. */
	private Decal decalAt(int edge, float distance) {
		for (Decal decal : decals) {
			if (decal.edge == edge && distance > decal.from && distance < decal.to) return decal;
		}
		return null;
	}

	@Override
	public void calculateNormals() {
		int edges = nbEdges();
		float[][] normals = new float[edges][];
		for (int e = 0; e < edges; e++) {
			normals[e] = edgeNormal(section[e], section[(e + 1) % section.length]);
		}
		for (int k = 0; k < abscissas.length; k++) {
			Vector3 left = path.left(abscissas[k]);
			for (int e = 0; e < edges; e++) {
				for (int end = 0; end < 2; end++) {
					float[] n = normals[e];
					if (shading == Shading.SMOOTH) {
						// The other edge of this point: the previous one for the start, the next one for the end
						int other = end == 0 ? e - 1 : e + 1;
						if (closedSection) other = (other + edges) % edges;
						if (other >= 0 && other < edges) {
							float[] m = normals[other];
							n = new float[] { n[0] + m[0], n[1] + m[1] };
						}
					}
					Vector3 normal = new Vector3(n[0] * left.getX(), n[0] * left.getY(), n[1]);
					normal.normalize();
					vertices[k][e][end].setNormal(normal);
				}
			}
		}
	}
}
