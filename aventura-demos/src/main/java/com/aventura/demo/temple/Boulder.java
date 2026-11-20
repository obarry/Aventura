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

import com.aventura.demo.Noise;
import java.util.IdentityHashMap;
import java.util.Map;

import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.Vertex;
import com.aventura.model.world.shape.GenerativeElement;
import com.aventura.model.world.triangle.Triangle;

/**
 * A new shape, written from scratch: an irregular rounded mass, a sphere whose surface is pushed in and out by a
 * fractal noise. With a stone texture it is a boulder; flattened by a Scaling and with a texture of leaves, it is a
 * clump of foliage of an olive tree.
 *
 * <h2>How it is made</h2>
 * <ul>
 * <li><b>Vertices</b>: like the parallels and the meridians of a globe: a vertex at each pole, and {@code rings - 1}
 *     rings of {@code segments} vertices in between. Each vertex is at the distance {@code radius} from the center,
 *     more or less the noise at that point ({@link #roughness}).</li>
 * <li><b>Triangles</b>: two per quadrilateral between two rings, and a fan of triangles around each pole. Triangles
 *     that would have two vertices at the pole (zero area, no normal) are avoided by this single pole vertex.</li>
 * <li><b>Normals</b>: there is no ideal surface to take the normals from, as for a sphere or a cylinder. For a smooth
 *     boulder, they are computed from the triangles themselves: the normal of a vertex is the average of the normals
 *     of the triangles around it ({@link #smoothNormals()}), a method that works for any mesh. A boulder can also
 *     keep its facets (one normal per triangle), which suits the rough masses of a foliage.</li>
 * </ul>
 *
 * @since Nov 2026
 */
public class Boulder extends GenerativeElement {

	/** Size of the bumps: number of noise cells across the radius (higher: more, smaller bumps). */
	static final float NOISE_FREQUENCY = 1.3f;

	protected final float radius, roughness;
	protected final int segments, rings, seed;
	protected final Texture texture;
	protected final boolean smooth;

	protected Vertex bottomPole, topPole;
	protected Vertex[][] ring; // ring[j][i]: ring j (1 .. rings - 1, from the bottom), vertex i around it

	/**
	 * @param radius    average radius
	 * @param roughness depth of the bumps, as a share of the radius (0: a sphere; 0.3: a rugged rock)
	 * @param segments  number of faces around the vertical axis, even
	 * @param rings     number of faces from one pole to the other, at least 3
	 * @param seed      seed of the noise: each seed gives another shape
	 * @param texture   the texture, or null
	 * @param smooth    true for a smooth surface (a normal per vertex), false for facets (a normal per triangle)
	 */
	public Boulder(float radius, float roughness, int segments, int rings, int seed, Texture texture, boolean smooth) {
		super("boulder", true); // A closed surface: its back faces are never seen, and can be culled
		if (radius <= 0 || roughness < 0 || roughness >= 1) {
			throw new IllegalArgumentException("Boulder radius must be positive and roughness in [0, 1[: " + radius + ", " + roughness);
		}
		if (segments < 4 || segments % 2 != 0 || rings < 3) {
			throw new IllegalArgumentException("A boulder needs an even number of segments (at least 4) and at least 3 rings: " + segments + ", " + rings);
		}
		this.radius = radius;
		this.roughness = roughness;
		this.segments = segments;
		this.rings = rings;
		this.seed = seed;
		this.texture = texture;
		this.smooth = smooth;
	}

	@Override
	public void generateVertices() {
		bottomPole = createVertex(surfacePoint(0, 0, -1));
		topPole = createVertex(surfacePoint(0, 0, 1));
		ring = new Vertex[rings][];
		for (int j = 1; j < rings; j++) {
			// Angle from the bottom pole (0) to the top pole (PI)
			double polar = Math.PI * j / rings;
			ring[j] = new Vertex[segments];
			for (int i = 0; i < segments; i++) {
				double around = 2 * Math.PI * i / segments;
				ring[j][i] = createVertex(surfacePoint(
						(float) (Math.sin(polar) * Math.cos(around)),
						(float) (Math.sin(polar) * Math.sin(around)),
						(float) -Math.cos(polar)));
			}
		}
	}

	/** The point of the surface in the direction (x, y, z), a unit vector: at the radius, more or less the noise. */
	private Vector4 surfacePoint(float x, float y, float z) {
		float noise = Noise.fbm(x * NOISE_FREQUENCY, y * NOISE_FREQUENCY, z * NOISE_FREQUENCY, 3, seed);
		float distance = radius * (1 + roughness * (2 * noise - 1));
		return new Vector4(x * distance, y * distance, z * distance, 1);
	}

	@Override
	public void generateTriangles() {
		for (int i = 0; i < segments; i++) {
			int next = (i + 1) % segments;
			// Texture: twice around the boulder (u from 0 to 2: the Texture must repeat itself, see
			// Texture.setWrapMode(); in MIRROR mode the second time is flipped, and the surface closes without a
			// seam), and once from the bottom to the top
			float u0 = 2f * i / segments, u1 = 2f * (i + 1) / segments;
			float uMiddle = (u0 + u1) / 2;

			// Fans around the poles. Seen from outside (from below for the bottom pole), counter-clockwise.
			addTriangle(bottomPole, ring[1][next], ring[1][i], uMiddle, 0, u1, v(1), u0, v(1));
			addTriangle(topPole, ring[rings - 1][i], ring[rings - 1][next], uMiddle, 1, u0, v(rings - 1), u1, v(rings - 1));

			// Quadrilaterals between the rings j and j + 1: (a, b, c) and (c, d, a), as for a TaperedDrum
			for (int j = 1; j < rings - 1; j++) {
				Vertex a = ring[j][i], b = ring[j][next], c = ring[j + 1][next], d = ring[j + 1][i];
				addTriangle(a, b, c, u0, v(j), u1, v(j), u1, v(j + 1));
				addTriangle(c, d, a, u1, v(j + 1), u0, v(j + 1), u0, v(j));
			}
		}
	}

	/** Texture coordinate v of the ring j. */
	private float v(int j) {
		return (float) j / rings;
	}

	private void addTriangle(Vertex v1, Vertex v2, Vertex v3, float s1, float t1, float s2, float t2, float s3, float t3) {
		Triangle triangle = new Triangle(v1, v2, v3);
		if (texture != null) {
			triangle.setTexture(texture, new Vector4(s1, t1, 0, 1), new Vector4(s2, t2, 0, 1), new Vector4(s3, t3, 0, 1));
		}
		addTriangle(triangle);
	}

	@Override
	public void calculateNormals() {
		if (smooth) {
			smoothNormals();
		} else {
			super.calculateNormals(); // The default of an Element: one normal per triangle
		}
	}

	/**
	 * Smooth normals of any mesh: the normal of a vertex is the sum of the normals of the triangles that share it,
	 * normalized. The cross product of two sides of a triangle is its normal, with a length of twice its area: summing
	 * the cross products as they are gives more weight to the large triangles, as it should.
	 */
	private void smoothNormals() {
		// IdentityHashMap: the key is the Vertex object itself (two vertices at the same place stay distinct)
		Map<Vertex, Vector3> sums = new IdentityHashMap<Vertex, Vector3>();
		for (Triangle triangle : getTriangles()) {
			Vector4 p1 = triangle.getV1().getPos(), p2 = triangle.getV2().getPos(), p3 = triangle.getV3().getPos();
			Vector3 cross = new Vector3(p1, p2).cross(new Vector3(p1, p3));
			for (Vertex vertex : new Vertex[] { triangle.getV1(), triangle.getV2(), triangle.getV3() }) {
				Vector3 sum = sums.get(vertex);
				if (sum == null) {
					sums.put(vertex, new Vector3(cross));
				} else {
					sum.plusEquals(cross);
				}
			}
		}
		for (Map.Entry<Vertex, Vector3> entry : sums.entrySet()) {
			entry.getKey().setNormal(entry.getValue().normalize());
		}
	}
}
