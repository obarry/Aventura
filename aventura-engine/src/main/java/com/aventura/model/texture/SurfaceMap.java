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
package com.aventura.model.texture;

/**
 * A map laid on a surface: a grid of values, width x height, read at texture coordinates (s, t) between 0 and 1, the
 * coordinates given to the vertices of the triangles. What its values mean depends on its subclass and on the role it
 * is given by the Material that reads it:
 * <ul>
 * <li>{@link Texture}: colors. The same class serves as the color of the surface (Triangle.setTexture()) and as its
 *     emission (Element.setEmissionMap(), Triangle.setEmissionMap()): the role is given by where the map is put, not
 *     by its class, as in glTF or OpenGL;</li>
 * <li>later, maps of numbers (one float per texel): bump or height maps (see BACKLOG §2).</li>
 * </ul>
 *
 * <h2>What all the maps share</h2>
 * Reading a map at (s, t) is the same whatever its values: s and t are turned into the indices of the texels around
 * the point, the indices outside of the map are folded back according to the {@link WrapMode} ({@link #wrap}), and the
 * texels are interpolated. On a surface seen from far away, a smaller copy of the map is read instead: the mipmaps, a
 * pyramid of copies each half the size of the previous one, down to 1 x 1 ({@link #buildMipmaps()}), the level being
 * chosen from the size of a pixel on the surface ({@link #mipmapLevel}). This class holds that common part: the size,
 * the name, the wrap mode, the sizes of the levels of the pyramid and the choice of the level. A subclass holds the
 * values themselves: how they are stored, averaged into the next level ({@link #buildLevels}) and interpolated.
 * <pre>
 *                  SurfaceMap                 size, name, wrap mode, wrap(), mipmap levels, mipmapLevel()
 *                      |
 *        +-------------+-------------+
 *        |                           |
 *     Texture                  (a map of floats)
 *   ARGB int per texel          a float per texel
 *   getInterpolatedColor()      (bump, height...)
 * </pre>
 *
 * @author Olivier BARRY
 * @since Nov 2026
 */
public abstract class SurfaceMap {

	/**
	 * What a map shows outside of itself: at texture coordinates below 0 or above 1 (see setWrapMode()).
	 * <pre>
	 *   coordinate:   -1        0        1        2        3
	 *   CLAMP          aaaaaaaaa|abcdefgh|hhhhhhhhhhhhhhhhhh     the border texel goes on
	 *   REPEAT         abcdefgh |abcdefgh|abcdefgh|abcdefgh      the map starts again
	 *   MIRROR         hgfedcba |abcdefgh|hgfedcba|abcdefgh      every other copy is flipped
	 * </pre>
	 */
	public enum WrapMode {
		/** The map is there once, from 0 to 1; beyond, its border texels go on. The default. */
		CLAMP,
		/**
		 * The map is repeated like tiles: only the fractional part of the coordinate counts (2.3 is read at 0.3).
		 * For a map made to be repeated, whose opposite borders match; otherwise a seam shows between two tiles.
		 */
		REPEAT,
		/**
		 * The map is repeated, every other tile flipped like in a mirror (2.3 is read at 0.3, but 1.3 at 0.7).
		 * Two neighbouring tiles then share the same border: no seam, whatever the map, e.g. a photograph.
		 */
		MIRROR
	}

	protected int width, height;
	// Name of the map, for the reports (see getName()): the name of its file, or the one given by setName()
	protected String name = null;
	protected WrapMode wrapMode = WrapMode.CLAMP;
	// Sizes of the levels of the mipmaps, level 0 being the map itself (see buildMipmaps()); null until they are built
	protected int[] mipWidths, mipHeights;

	/** For a subclass that knows its size only once its file is read: it sets width and height itself. */
	protected SurfaceMap() {
	}

	protected SurfaceMap(int width, int height) {
		this.width = width;
		this.height = height;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	/**
	 * @return the name of this map, for a report or a trace: the name of the file it was loaded from (without its
	 *         directory), or the name given by setName(); "width x height texture" (or the kind of the map) without either
	 */
	public String getName() {
		return name != null ? name : width + " x " + height + " " + getKind();
	}

	/**
	 * Gives a name to this map (e.g. a map computed texel by texel, which has no file name).
	 * @return this map, to chain the call
	 */
	public SurfaceMap setName(String name) {
		this.name = name;
		return this;
	}

	/** @return what this map is, in a word, for its default name (e.g. "texture") */
	protected abstract String getKind();

	/**
	 * @return the memory used by the values of this map, in bytes, those of its mipmaps included if they are built,
	 *         whatever the size of the (compressed) file it was loaded from
	 */
	public abstract long getMemorySize();

	// ------------------------------------------------------------------
	// Wrap mode: the map outside of itself
	// ------------------------------------------------------------------

	/**
	 * Sets what this map shows at texture coordinates outside of 0 to 1 (see WrapMode): its border (CLAMP, the
	 * default), itself again (REPEAT) or itself again, every other time flipped (MIRROR).
	 *
	 * This is how a map is repeated over a large surface, like tiles: the surface gives texture coordinates that go
	 * beyond 1, e.g. from 0 to 24 for 24 tiles (see Trellis.setTextureRepeat()), and the map folds them back, texel by
	 * texel. The surface does not need a vertex at each border of a tile, and the number of tiles does not need to be
	 * an integer. The mode applies wherever the map is used; it has no effect on a surface whose coordinates stay
	 * between 0 and 1, except within half a texel of its borders, where the bilinear interpolation reads the texel of
	 * the other side (REPEAT) instead of the border one.
	 *
	 * To be set before rendering, like the values.
	 *
	 * @return this map, to chain the calls
	 */
	public SurfaceMap setWrapMode(WrapMode wrapMode) {
		if (wrapMode == null) throw new IllegalArgumentException("The wrap mode of a " + getKind() + " cannot be null");
		this.wrapMode = wrapMode;
		return this;
	}

	public WrapMode getWrapMode() {
		return wrapMode;
	}

	/**
	 * The index of the texel to read, in a row or a column of size texels, for an index that may be outside of it
	 * (0 to size - 1), according to the wrap mode. With size = 4:
	 * <pre>
	 *   index:    -3 -2 -1 | 0  1  2  3 | 4  5  6  7 | 8  9
	 *   CLAMP      0  0  0 | 0  1  2  3 | 3  3  3  3 | 3  3
	 *   REPEAT     1  2  3 | 0  1  2  3 | 0  1  2  3 | 0  1
	 *   MIRROR     2  1  0 | 0  1  2  3 | 3  2  1  0 | 0  1
	 * </pre>
	 * Working on the indices of the texels rather than on the coordinates makes the bilinear interpolation right at
	 * the border of a tile: the 4 texels read there are the neighbours in the repeated map.
	 */
	protected final int wrap(int index, int size) {
		if (index >= 0 && index < size) return index; // Inside the map: the same for all modes
		switch (wrapMode) {
		case REPEAT:
			return Math.floorMod(index, size); // The remainder of the division, positive also for a negative index
		case MIRROR:
			int inTwoTiles = Math.floorMod(index, 2 * size); // A tile and its flipped neighbour are repeated together
			return inTwoTiles < size ? inTwoTiles : 2 * size - 1 - inTwoTiles;
		case CLAMP:
		default:
			return index < 0 ? 0 : size - 1;
		}
	}

	// ------------------------------------------------------------------
	// Mipmapping
	// ------------------------------------------------------------------

	/**
	 * Builds the mipmaps of this map, if they are not built yet: a pyramid of copies of the map, each level half the
	 * size of the previous one (rounded down, at least 1), down to a single texel. A texel of a level is the average
	 * of the 2 x 2 texels it covers in the level below (a "box filter").
	 * <pre>
	 *   level 0: 8 x 8 (the map)   level 1: 4 x 4   level 2: 2 x 2   level 3: 1 x 1
	 * </pre>
	 * The pyramid adds one third to the memory of the map (1/4 + 1/16 + ...). It is built once: the RenderEngine calls
	 * this method before rasterizing, when mipmapping is enabled (RenderContext.setMipmapping()), so that the
	 * rasterization threads only read it. A change of the values discards it: it is built again at the next call.
	 *
	 * This method computes the sizes of the levels; the subclass averages its values into them (buildLevels()).
	 */
	public final synchronized void buildMipmaps() {
		if (hasMipmaps()) return;

		// Number of levels: the size is halved until both sides are 1
		int levels = 1;
		for (int size = Math.max(width, height); size > 1; size /= 2) levels++;

		int[] widths = new int[levels], heights = new int[levels];
		widths[0] = width;
		heights[0] = height;
		for (int level = 1; level < levels; level++) {
			widths[level] = Math.max(1, widths[level - 1] / 2);
			heights[level] = Math.max(1, heights[level - 1] / 2);
		}
		mipWidths = widths;
		mipHeights = heights;
		buildLevels(widths, heights);
	}

	/**
	 * Builds the levels of the mipmaps, of the given sizes (level 0 is the map itself), each texel of a level being the
	 * average of the 2 x 2 texels it covers in the level below. The levels must be published last (hasMipmaps() true
	 * only once they are all there): the rasterization threads may read them as soon as they see them.
	 */
	protected abstract void buildLevels(int[] widths, int[] heights);

	/** @return true if the mipmaps of this map are built (see buildMipmaps()) */
	public abstract boolean hasMipmaps();

	/** @return the number of mipmap levels, the map itself (level 0) included; 1 if the mipmaps are not built */
	public int getNbMipmapLevels() {
		return hasMipmaps() ? mipWidths.length : 1;
	}

	/** @return the width of a mipmap level, in texels (level 0 is the map itself); the mipmaps must be built */
	public int getMipmapWidth(int level) {
		return mipWidths[level];
	}

	/** @return the height of a mipmap level, in texels; the mipmaps must be built */
	public int getMipmapHeight(int level) {
		return mipHeights[level];
	}

	/**
	 * The mipmap level whose texels have the size of a pixel of the screen, for a surface seen at a given scale.
	 * The scale is the size of a pixel on the surface, in texture coordinates (the map is 1 wide and 1 high), as a
	 * base 2 logarithm: -10 means that a pixel covers 1/1024 of the map. A texel of level 0 covers 1/width x 1/height
	 * of the map, hence 1/sqrt(width x height) on average, and each level doubles it:
	 * <pre>
	 *   level = log2(pixel size / texel size of level 0) = log2PixelSize + log2(sqrt(width x height))
	 * </pre>
	 * The level is a float: 2.3 is between the levels 2 and 3, nearer to 2. A negative level means that a pixel is
	 * smaller than a texel (the map is magnified): level 0 is the finest there is.
	 *
	 * @param log2PixelSize base 2 logarithm of the size of a pixel, in texture coordinates (see Fragment.getTexLog2PixelSize())
	 */
	public float mipmapLevel(float log2PixelSize) {
		return log2PixelSize + 0.5f * (float) (Math.log((double) width * height) / LN_2);
	}

	private static final double LN_2 = Math.log(2);
}
