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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.aventura.model.texture.Texture;
import com.aventura.model.world.triangle.Triangle;

/**
 * The size of a geometry: how many Elements, triangles and vertices an Element and all its sub-Elements hold (or a
 * whole World, or any set of Elements), how deep their tree is, and which textures their triangles use.
 *
 * <p>The counters of an Element ({@link Element#getNbTriangles()}, {@link Element#getNbVertices()}) only count what
 * the Element owns itself. A scene is a tree: its size is the sum over the whole tree, which this class computes in
 * one walk. It is a snapshot: computed when asked, not updated when the geometry changes afterwards (count after
 * {@code World.build()}, which is what creates the vertices and the triangles of the shapes).
 * <pre>
 *   GeometryStatistics all = world.getStatistics();           // The whole World
 *   GeometryStatistics temple = templeElement.getStatistics(); // One branch of its tree
 *   System.out.println(all);   // "753 elements (72 groups), 56,676 triangles, 36,397 vertices, depth 7, 7 textures (35.5 MB)"
 * </pre>
 * A texture shared by several Elements is counted once. The vertices are counted per Element: two Elements never
 * share a Vertex, and the vertices shared by the triangles of an Element are counted once.
 *
 * @since Nov 2026
 */
public class GeometryStatistics {

	private int nbElements = 0;
	private int nbGroups = 0;
	private int nbTriangles = 0;
	private int nbVertices = 0;
	private int nbTexturedTriangles = 0;
	private int depth = 0;
	// Number of triangles using each texture, in the order the textures are met in the tree. The Texture object itself
	// is the key (Texture does not redefine equals()): two textures with the same content are two textures in memory.
	private final Map<Texture, Integer> textureUsage = new LinkedHashMap<Texture, Integer>();

	private GeometryStatistics() {
	}

	/** The statistics of an Element and of all its sub-Elements, at every level. */
	public static GeometryStatistics of(Element element) {
		GeometryStatistics statistics = new GeometryStatistics();
		statistics.add(element, 1);
		return statistics;
	}

	/**
	 * The statistics of several Elements together, each one with all its sub-Elements: for instance the Elements of
	 * the same kind in a scene (all its columns, all its trees). The Elements must be distinct, and none of them a
	 * sub-Element of another one, or their geometry would be counted twice.
	 */
	public static GeometryStatistics of(Collection<? extends Element> elements) {
		GeometryStatistics statistics = new GeometryStatistics();
		for (Element element : elements) {
			statistics.add(element, 1);
		}
		return statistics;
	}

	/** The statistics of a whole World: all its Elements, at every level. */
	public static GeometryStatistics of(World world) {
		return of(world.getElements());
	}

	/** Adds an Element, found at the given level of the tree (1 for a root), then its sub-Elements, one level deeper. */
	private void add(Element element, int level) {
		nbElements++;
		if (element.getNbTriangles() == 0) nbGroups++;
		nbTriangles += element.getNbTriangles();
		nbVertices += element.getNbVertices();
		depth = Math.max(depth, level);

		// The texture belongs to each triangle (an Element given a texture gives it to its triangles when it is built)
		for (Triangle triangle : element.getTriangles()) {
			Texture texture = triangle.getTexture();
			if (texture != null) {
				nbTexturedTriangles++;
				textureUsage.merge(texture, 1, Integer::sum);
			}
		}

		if (!element.isLeaf()) {
			for (Element sub : element.getSubElements()) {
				add(sub, level + 1);
			}
		}
	}

	/** Number of Elements, the sub-Elements of every level included (and the root ones). */
	public int getNbElements() {
		return nbElements;
	}

	/** Number of Elements without any triangle of their own: usually the groups, that only hold other Elements. */
	public int getNbGroups() {
		return nbGroups;
	}

	/** Number of triangles. */
	public int getNbTriangles() {
		return nbTriangles;
	}

	/** Number of vertices. */
	public int getNbVertices() {
		return nbVertices;
	}

	/** Number of triangles that have a texture. */
	public int getNbTexturedTriangles() {
		return nbTexturedTriangles;
	}

	/** Number of levels of the tree: 1 for Elements without sub-Element, 0 if there is no Element at all. */
	public int getDepth() {
		return depth;
	}

	/** The textures used by at least one triangle, each one once, in the order they are met in the tree. */
	public Set<Texture> getTextures() {
		return Collections.unmodifiableSet(textureUsage.keySet());
	}

	/** Number of triangles using the given texture (0 if none). */
	public int getNbTrianglesUsing(Texture texture) {
		return textureUsage.getOrDefault(texture, 0);
	}

	/** Memory used by the texels of all the textures, in bytes (see {@link Texture#getMemorySize()}). */
	public long getTextureMemorySize() {
		long size = 0;
		for (Texture texture : textureUsage.keySet()) {
			size += texture.getMemorySize();
		}
		return size;
	}

	/** One line, e.g. "753 elements (72 groups), 56,676 triangles, 36,397 vertices, depth 7, 7 textures (35.5 MB)" */
	@Override
	public String toString() {
		return String.format(Locale.ROOT, "%,d elements (%,d groups), %,d triangles, %,d vertices, depth %d, %d textures (%.1f MB)",
				nbElements, nbGroups, nbTriangles, nbVertices, depth, textureUsage.size(), getTextureMemorySize() / (1024f * 1024f));
	}
}
