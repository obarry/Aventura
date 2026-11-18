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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.aventura.model.texture.Texture;

/**
 * A report on the size of a scene, as a text to print or to display: what its geometry is made of (Elements,
 * triangles and vertices of each branch of the tree of a World, or of an Element) and its textures (size, memory,
 * triangles that use them).
 * <pre>
 *   System.out.println(GeometryReport.of(world));
 * </pre>
 * For a World of a few shapes it is a list of them; for a large scene it is its composition, as in this extract of the
 * report of the TempleRuins demo:
 * <pre>
 * GEOMETRY                                  elements   triangles    vertices   share
 * World                                          753      56,676      36,397  100.0%
 *   island                                       750      53,980      34,947   95.2%
 *     ground (Trellis)                             1      18,432       9,409   32.5%
 *     temple                                     396      12,516      11,770   22.1%
 *       colonnade                                217      10,416      10,370   18.4%
 *         column x 26                            216      10,416      10,370   18.4%
 *           drum (TaperedDrum) x 110             118       9,120       9,128   16.1%
 *     ...
 * TEXTURES                                        pixels      memory   triangles
 *   texture_stone_1023x852.jpg                1023 x 852      3.3 MB      14,496
 *   foliage (computed)                         256 x 256    256.0 KB      16,100
 *     ...
 * </pre>
 * All the numbers come from {@link GeometryStatistics}, which counts an Element and all its sub-Elements; this class
 * only walks the tree to choose the lines, and formats them.
 *
 * <p>A scene can have hundreds of Elements (a line for each of the 126 slabs of the platform of that temple would not
 * help), so the sub-Elements of the same kind are gathered on one line: "column x 26" is the 26 columns together, and
 * the line below it, "drum x 110", all the drums of these 26 columns (118 Elements, with the caps of some of them).
 * Two Elements are of the same kind if they have the same class and the same name (without the number that Aventura
 * adds to the name of each Element): give its own name to an Element ({@code setName()}) to get its own line.
 *
 * @since Nov 2026
 */
public final class GeometryReport {

	/** Width of the first column (the names), in characters; wider if a texture has a longer name. */
	static final int NAME_WIDTH = 40;

	private GeometryReport() {
	}

	/**
	 * The whole report of a World: its geometry, then its textures.
	 *
	 * @param world the World, already built (build() is what creates the vertices and the triangles of the shapes)
	 */
	public static String of(World world) {
		return geometry("World", world.getElements()) + "\n" + textures(world.getStatistics());
	}

	/**
	 * The whole report of one Element and its sub-Elements (a branch of the tree of a World): its geometry, then its
	 * textures.
	 *
	 * @param element the Element, already built
	 */
	public static String of(Element element) {
		return geometry(kindOf(element), element.isLeaf() ? new ArrayList<Element>() : element.getSubElements(), element.getStatistics())
				+ "\n" + textures(element.getStatistics());
	}

	// ****************************************************************************************************************
	// ***** Geometry
	// ****************************************************************************************************************

	/**
	 * The composition of a geometry: a line for the whole, then a line for each kind of Element of each level of the
	 * tree, with the number of Elements (itself and its sub-Elements), triangles and vertices, and its share of the
	 * triangles of the whole.
	 *
	 * @param title    name of the first line, the whole
	 * @param elements the Elements at the top of the tree
	 */
	public static String geometry(String title, List<Element> elements) {
		return geometry(title, elements, GeometryStatistics.of(elements));
	}

	/** The same, when the whole is more than the given Elements: an Element (the total) and its sub-Elements. */
	private static String geometry(String title, List<Element> elements, GeometryStatistics total) {
		StringBuilder report = new StringBuilder();
		report.append(String.format("%-" + NAME_WIDTH + "s%10s%12s%12s%8s%n", "GEOMETRY", "elements", "triangles", "vertices", "share"));
		appendLine(report, title, total, total.getNbTriangles());
		appendKinds(report, elements, 1, total.getNbTriangles());
		report.append(String.format(Locale.ROOT, "%d levels of Elements, %,d of them without triangles of their own (groups)%n",
				total.getDepth(), total.getNbGroups()));
		return report.toString();
	}

	/**
	 * Appends a line for each kind of Element found in the given Elements (all at the same level of the tree), each
	 * one followed by the lines of its sub-Elements, one level deeper: a recursive walk of the tree.
	 */
	private static void appendKinds(StringBuilder report, List<Element> elements, int level, int totalTriangles) {
		// The Elements of each kind, in the order the kinds are met (LinkedHashMap keeps the order of insertion)
		Map<String, List<Element>> kinds = new LinkedHashMap<String, List<Element>>();
		for (Element element : elements) {
			kinds.computeIfAbsent(kindOf(element), kind -> new ArrayList<Element>()).add(element);
		}

		for (Map.Entry<String, List<Element>> kind : kinds.entrySet()) {
			List<Element> sameKind = kind.getValue();
			String label = "  ".repeat(level) + kind.getKey() + (sameKind.size() > 1 ? " x " + sameKind.size() : "");
			appendLine(report, label, GeometryStatistics.of(sameKind), totalTriangles);

			// The sub-Elements of all the Elements of this kind, together
			List<Element> subElements = new ArrayList<Element>();
			for (Element element : sameKind) {
				if (!element.isLeaf()) subElements.addAll(element.getSubElements());
			}
			if (!subElements.isEmpty()) {
				appendKinds(report, subElements, level + 1, totalTriangles);
			}
		}
	}

	private static void appendLine(StringBuilder report, String label, GeometryStatistics statistics, int totalTriangles) {
		float share = totalTriangles > 0 ? 100f * statistics.getNbTriangles() / totalTriangles : 0;
		report.append(String.format(Locale.ROOT, "%-" + NAME_WIDTH + "s%,10d%,12d%,12d%7.1f%%%n",
				label, statistics.getNbElements(), statistics.getNbTriangles(), statistics.getNbVertices(), share));
	}

	/**
	 * The kind of an Element: its name, without the number that Aventura adds to it (the id of the Element, which
	 * makes each name unique), followed by its class when the class says more than the name ("ground (Trellis)",
	 * but "column" rather than "column (Column)", and no class for a plain Element, which is a group).
	 */
	public static String kindOf(Element element) {
		String name = element.getName();
		String id = String.valueOf(element.getId());
		if (name.endsWith(id)) name = name.substring(0, name.length() - id.length());

		String type = element.getClass().getSimpleName();
		boolean sameWords = type.equalsIgnoreCase(name.replace(" ", ""));
		return element.getClass() == Element.class || sameWords ? name : name + " (" + type + ")";
	}

	// ****************************************************************************************************************
	// ***** Textures
	// ****************************************************************************************************************

	/**
	 * The textures used by a geometry: a line for each one, with its size in pixels, the memory of its pixels (4
	 * bytes each) and the number of triangles that use it; then the triangles without a texture, and the total.
	 */
	public static String textures(GeometryStatistics statistics) {
		// The column of the names is as wide as the longest name (the name of a file can be long)
		int nameWidth = NAME_WIDTH;
		for (Texture texture : statistics.getTextures()) {
			nameWidth = Math.max(nameWidth, texture.getName().length() + 4);
		}
		String line = "%-" + nameWidth + "s%14s%12s%,12d%n";

		StringBuilder report = new StringBuilder();
		report.append(String.format("%-" + nameWidth + "s%14s%12s%12s%n", "TEXTURES", "pixels", "memory", "triangles"));
		for (Texture texture : statistics.getTextures()) {
			report.append(String.format(Locale.ROOT, line, "  " + texture.getName(), texture.getWidth() + " x " + texture.getHeight(),
					memory(texture.getMemorySize()), statistics.getNbTrianglesUsing(texture)));
		}
		report.append(String.format(Locale.ROOT, line, "  (no texture)", "", "", statistics.getNbTriangles() - statistics.getNbTexturedTriangles()));
		report.append(String.format(Locale.ROOT, line, statistics.getTextures().size() + " textures", "",
				memory(statistics.getTextureMemorySize()), statistics.getNbTriangles()));
		return report.toString();
	}

	/** A number of bytes as a text, in the unit that suits its size: "12 B", "256.0 KB", "3.3 MB". */
	public static String memory(long bytes) {
		if (bytes < 1024) return bytes + " B";
		if (bytes < 1024 * 1024) return String.format(Locale.ROOT, "%.1f KB", bytes / 1024f);
		return String.format(Locale.ROOT, "%.1f MB", bytes / (1024f * 1024f));
	}
}
