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

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.aventura.tools.tracing.Tracer;

/**
 * This class provides functionalites to load a texture file and then provide interpolated color using normalized coordinates
 * It is possible to load the bitmap file horizontally or vertically (just exchanging what is width and height, this is like portrait and landscape for pictures)
 * but also to "reverse" the bitmap (left/right) which is important for file having a "direction" (e.g. text written etc.) and how it is supposed to be used.
 * 
 * @author Olivier BARRY
 * @since April 2017
 */
public class Texture {
	
	public static final int TEXTURE_DIRECTION_HORIZONTAL = 1;
	public static final int TEXTURE_DIRECTION_VERTICAL = 2;
	public static final int TEXTURE_ORIENTATION_NORMAL = 1;
	public static final int TEXTURE_ORIENTATION_OPPOSITE = 2;
	
	// The ARGB values of the texels, row by row: texel (x, y) is tex[y * width + x]. The texels of a row are
	// neighbors in memory, as the bilinear filtering below reads them (performance audit A6; it used to be an
	// int[x][y] array).
	protected int[] tex;
	protected int width, height;
	// Name of the texture, for the reports (see getName()): the name of its file, or the one given by setName()
	protected String name = null;

	/**
	 * What a Texture shows outside of itself: at texture coordinates below 0 or above 1 (see setWrapMode()).
	 * <pre>
	 *   coordinate:   -1        0        1        2        3
	 *   CLAMP          aaaaaaaaa|abcdefgh|hhhhhhhhhhhhhhhhhh     the border texel goes on
	 *   REPEAT         abcdefgh |abcdefgh|abcdefgh|abcdefgh      the texture starts again
	 *   MIRROR         hgfedcba |abcdefgh|hgfedcba|abcdefgh      every other copy is flipped
	 * </pre>
	 */
	public enum WrapMode {
		/** The texture is there once, from 0 to 1; beyond, its border texels go on. The default. */
		CLAMP,
		/**
		 * The texture is repeated like tiles: only the fractional part of the coordinate counts (2.3 is read at 0.3).
		 * For a texture made to be repeated, whose opposite borders match; otherwise a seam shows between two tiles.
		 */
		REPEAT,
		/**
		 * The texture is repeated, every other tile flipped like in a mirror (2.3 is read at 0.3, but 1.3 at 0.7).
		 * Two neighbouring tiles then share the same border: no seam, whatever the texture, e.g. a photograph.
		 */
		MIRROR
	}

	protected WrapMode wrapMode = WrapMode.CLAMP;

	// Mipmaps: smaller and smaller copies of the texture, each one half the size of the previous one, down to 1 x 1
	// (see buildMipmaps()). mipTexels[n] holds the texels of level n, row by row like tex; level 0 is tex itself.
	// null until they are built: they are only needed by getInterpolatedColor(s, t, pixelSize).
	protected int[][] mipTexels = null;
	protected int[] mipWidths, mipHeights;

	
	/**
	 * Create an empty Texture of a given width and height
	 * @param width
	 * @param height
	 */
	public Texture(int width, int height) {
		this.width = width;
		this.height = height;
		tex = new int[width * height];
	}
	
	/**
	 * Create a Texture from a bitmap file
	 * @param fileName
	 * @throws TextureLoadException if the file cannot be read/decoded as an image -- fails fast
	 *         with a clear message rather than silently proceeding to a NullPointerException on
	 *         img.getHeight() (the previous behavior when ImageIO.read() failed).
	 */
	public Texture(String fileName) {
        BufferedImage img = null;
        try {
            img = ImageIO.read(new File(fileName));
        } catch (IOException e) {
        	throw new TextureLoadException("Failed to load texture file: " + fileName, e);
        }
        if (img == null) {
        	// ImageIO.read() returns null (no exception) when the file exists but isn't a
        	// recognized image format -- same fail-fast treatment as the IOException case above.
        	throw new TextureLoadException("Failed to load texture file (unrecognized image format): " + fileName);
        }
        this.height = img.getHeight();
        this.width = img.getWidth();
        this.name = new File(fileName).getName();
        
        // All the pixels in one call, already row by row (getRGB(x, y) converted them one by one)
        tex = img.getRGB(0, 0, width, height, null, 0, width);
        // Flush BufferedImage data, they are no longer needed
        img.flush();
	}

	/**
	 * Create a Texture from a bitmap file
	 * @param fileName
	 * @throws TextureLoadException if the file cannot be read/decoded as an image -- see the
	 *         other String constructor's Javadoc for why.
	 */
	public Texture(String fileName, int direction, int horizontal_orientation, int vertical_orientation) {
		BufferedImage img = null;
		try {
			img = ImageIO.read(new File(fileName));
		} catch (IOException e) {
			throw new TextureLoadException("Failed to load texture file: " + fileName, e);
		}
		if (img == null) {
			throw new TextureLoadException("Failed to load texture file (unrecognized image format): " + fileName);
		}
		this.name = new File(fileName).getName();

		if (direction == TEXTURE_DIRECTION_VERTICAL) {
			this.height = img.getHeight();
			this.width = img.getWidth();
		} else if (direction == TEXTURE_DIRECTION_HORIZONTAL) {
			// Reverse height and width
			this.height = img.getWidth();
			this.width = img.getHeight();
		} else {
			// Should never happen
		}

		tex = new int[width * height];

		// All the pixels of the image in one call, row by row: pixel (x, y) of the image is pixels[y * imageWidth + x]
		int imageWidth = img.getWidth();
		int[] pixels = img.getRGB(0, 0, imageWidth, img.getHeight(), null, 0, imageWidth);

		for (int h=0; h<height; h++) {
			for (int w=0; w<width; w++) {
				if (direction == TEXTURE_DIRECTION_VERTICAL) {
					
					if (horizontal_orientation == TEXTURE_ORIENTATION_NORMAL) {
						if (vertical_orientation == TEXTURE_ORIENTATION_NORMAL) {
							tex[h * width + w] = pixels[(h) * imageWidth + (w)];
						} else if (vertical_orientation == TEXTURE_ORIENTATION_OPPOSITE) {
							tex[h * width + w] = pixels[(height-h-1) * imageWidth + (w)];
						} else {
							// Should never happen
						}

					} else if (horizontal_orientation == TEXTURE_ORIENTATION_OPPOSITE) {
						if (vertical_orientation == TEXTURE_ORIENTATION_NORMAL) {
							tex[h * width + w] = pixels[(h) * imageWidth + (width-w-1)];
						} else if (vertical_orientation == TEXTURE_ORIENTATION_OPPOSITE) {
							tex[h * width + w] = pixels[(height-h-1) * imageWidth + (width-w-1)];
						} else {
							// Should never happen
						}

					} else {
						// Should never happen
					}
					
				} else if (direction == TEXTURE_DIRECTION_HORIZONTAL) {
					if (horizontal_orientation == TEXTURE_ORIENTATION_NORMAL) {
						if (vertical_orientation == TEXTURE_ORIENTATION_NORMAL) {
							tex[h * width + w] = pixels[(w) * imageWidth + (h)]; // Inverse h and w reading
						} else if (vertical_orientation == TEXTURE_ORIENTATION_OPPOSITE) {
							tex[h * width + w] = pixels[(w) * imageWidth + (height-h-1)];
						} else {
							// Should never happen
						}

					} else if (horizontal_orientation == TEXTURE_ORIENTATION_OPPOSITE) {
						if (vertical_orientation == TEXTURE_ORIENTATION_NORMAL) {
							tex[h * width + w] = pixels[(width-w-1) * imageWidth + (h)];
						} else if (vertical_orientation == TEXTURE_ORIENTATION_OPPOSITE) {
							tex[h * width + w] = pixels[(width-w-1) * imageWidth + (height-h-1)];
						} else {
							// Should never happen
						}

					} else {
						// Should never happen
					}
				} 
			}
		}
		// Flush BufferedImage data, they are no longer needed
		img.flush();
	}
	
	/**
	 * Calculate the bilinear interpolated Color of this Texture at coordinates <s,t> with 0 <= s <= 1 and 0 <= t <= 1
	 *
	 * PERF NOTE: works directly on the packed ARGB ints stored in tex[], extracting R/G/B as
	 * floats via bit shifts rather than constructing 4 intermediate Color objects (one per
	 * sample) and extracting their components via getRGBColorComponents() (which itself
	 * allocates a float[3] per call). Only one Color is ever created here, at the very end --
	 * same "allocate once, at the end" approach already used for Fragment/RGBAccumulator
	 * elsewhere in the rendering pipeline. This matters because this method runs once per
	 * textured pixel, potentially the hottest per-pixel path in the whole renderer.
	 *
	 * @param s
	 * @param t
	 * @return the interpolated Color, never null (a texture with degenerate 0 width/height would
	 *         still fail, but that was already true before and is out of scope here)
	 */
	public Color getInterpolatedColor(float s, float t) {

		// Calculate the coordinates within the texture (-0.5 as per bressenham)
		float u = s * this.width - 0.5f;
		float v = t * this.height - 0.5f;

		// Calculate the integer value of u and v
		int x0 = (int) Math.floor(u);
		int y0 = (int) Math.floor(v);
		int x1 = x0 + 1;
		int y1 = y0 + 1;
		
		// Calculate the frac value of u and v (their respective complement to 1 will be computed in getBilinearFilteredComponent directly)
		float u_ratio = u - x0;
		float v_ratio = v - y0;

		// Texels outside of the texture: according to the wrap mode (clamped to the border by default)
		x0 = wrap(x0, width);
		x1 = wrap(x1, width);
		y0 = wrap(y0, height);
		y1 = wrap(y1, height);

		int argb00 = tex[y0 * width + x0];
		int argb01 = tex[y1 * width + x0];
		int argb10 = tex[y0 * width + x1];
		int argb11 = tex[y1 * width + x1];

		// Bilinear filtering is a weighted sum of 4 RGB samples (weights (1-u)(1-v), (1-u)v, u(1-v), uv).
		float u_opposite = 1f - u_ratio;
		float v_opposite = 1f - v_ratio;

		// Local sums rather than a shared accumulator object: no state in this Texture, so several threads can
		// sample it at once (performance audit A6). Same operations, in the same order, as RGBAccumulator.add().
		float r = 0f, g = 0f, b = 0f;
		float w00 = u_opposite * v_opposite, w01 = u_opposite * v_ratio, w10 = u_ratio * v_opposite, w11 = u_ratio * v_ratio;
		r += red(argb00) * w00;   g += green(argb00) * w00;   b += blue(argb00) * w00;
		r += red(argb01) * w01;   g += green(argb01) * w01;   b += blue(argb01) * w01;
		r += red(argb10) * w10;   g += green(argb10) * w10;   b += blue(argb10) * w10;
		r += red(argb11) * w11;   g += green(argb11) * w11;   b += blue(argb11) * w11;

		return new Color(clamp01(r), clamp01(g), clamp01(b));
	}

	// ------------------------------------------------------------------
	// Mipmapping
	// ------------------------------------------------------------------

	/**
	 * Builds the mipmaps of this texture, if they are not built yet: a pyramid of copies of the texture, each level
	 * half the size of the previous one (rounded down, at least 1), down to a single texel. A texel of a level is the
	 * average of the 2 x 2 texels it covers in the level below (a "box filter").
	 * <pre>
	 *   level 0: 8 x 8 (the texture)   level 1: 4 x 4   level 2: 2 x 2   level 3: 1 x 1
	 * </pre>
	 * The pyramid adds one third to the memory of the texture (1/4 + 1/16 + ...). It is built once: the RenderEngine
	 * calls this method before rasterizing, when mipmapping is enabled (RenderContext.setMipmapping()), so that the
	 * rasterization threads only read it. setColor() discards it: it is built again at the next call.
	 */
	public synchronized void buildMipmaps() {
		if (mipTexels != null) return;

		// Number of levels: the size is halved until both sides are 1
		int levels = 1;
		for (int size = Math.max(width, height); size > 1; size /= 2) levels++;

		int[][] texels = new int[levels][];
		int[] widths = new int[levels], heights = new int[levels];
		texels[0] = tex;
		widths[0] = width;
		heights[0] = height;
		for (int level = 1; level < levels; level++) {
			int w = Math.max(1, widths[level - 1] / 2), h = Math.max(1, heights[level - 1] / 2);
			widths[level] = w;
			heights[level] = h;
			texels[level] = halve(texels[level - 1], widths[level - 1], heights[level - 1], w, h);
		}
		mipWidths = widths;
		mipHeights = heights;
		mipTexels = texels; // Assigned last: a texture with mipTexels has all its levels
	}

	/**
	 * A copy of half the size of the given texels: each texel of the copy is the average of the block of 2 x 2
	 * texels it covers (2 x 1 or 1 x 2 when the source is a single row or column of texels). When a side of the
	 * source is odd, its last row or column is left out.
	 */
	private static int[] halve(int[] source, int sourceWidth, int sourceHeight, int width, int height) {
		int[] half = new int[width * height];
		for (int y = 0; y < height; y++) {
			int y0 = Math.min(2 * y, sourceHeight - 1), y1 = Math.min(2 * y + 1, sourceHeight - 1);
			for (int x = 0; x < width; x++) {
				int x0 = Math.min(2 * x, sourceWidth - 1), x1 = Math.min(2 * x + 1, sourceWidth - 1);
				int a = source[y0 * sourceWidth + x0], b = source[y0 * sourceWidth + x1];
				int c = source[y1 * sourceWidth + x0], d = source[y1 * sourceWidth + x1];
				// The average of each color component (8 bits each, in a packed ARGB int), rounded to the nearest
				int red = (((a >> 16) & 0xFF) + ((b >> 16) & 0xFF) + ((c >> 16) & 0xFF) + ((d >> 16) & 0xFF) + 2) / 4;
				int green = (((a >> 8) & 0xFF) + ((b >> 8) & 0xFF) + ((c >> 8) & 0xFF) + ((d >> 8) & 0xFF) + 2) / 4;
				int blue = ((a & 0xFF) + (b & 0xFF) + (c & 0xFF) + (d & 0xFF) + 2) / 4;
				half[y * width + x] = 0xFF000000 | (red << 16) | (green << 8) | blue;
			}
		}
		return half;
	}

	/** @return true if the mipmaps of this texture are built (see buildMipmaps()) */
	public boolean hasMipmaps() {
		return mipTexels != null;
	}

	/** @return the number of mipmap levels, the texture itself (level 0) included; 1 if the mipmaps are not built */
	public int getNbMipmapLevels() {
		return mipTexels != null ? mipTexels.length : 1;
	}

	/** @return the width of a mipmap level, in texels (level 0 is the texture itself); the mipmaps must be built */
	public int getMipmapWidth(int level) {
		return mipWidths[level];
	}

	/** @return the height of a mipmap level, in texels; the mipmaps must be built */
	public int getMipmapHeight(int level) {
		return mipHeights[level];
	}

	/**
	 * The mipmap level whose texels have the size of a pixel of the screen, for a surface seen at a given scale.
	 * The scale is the size of a pixel on the surface, in texture coordinates (the texture is 1 wide and 1 high), as
	 * a base 2 logarithm: -10 means that a pixel covers 1/1024 of the texture. A texel of level 0 covers
	 * 1/width x 1/height of the texture, hence 1/sqrt(width x height) on average, and each level doubles it:
	 * <pre>
	 *   level = log2(pixel size / texel size of level 0) = log2PixelSize + log2(sqrt(width x height))
	 * </pre>
	 * The level is a float: 2.3 is between the levels 2 and 3, nearer to 2. A negative level means that a pixel is
	 * smaller than a texel (the texture is magnified): level 0 is the finest there is.
	 *
	 * @param log2PixelSize base 2 logarithm of the size of a pixel, in texture coordinates (see Fragment.getTexLog2PixelSize())
	 */
	public float mipmapLevel(float log2PixelSize) {
		return log2PixelSize + 0.5f * (float) (Math.log((double) width * height) / LN_2);
	}

	private static final double LN_2 = Math.log(2);

	/**
	 * The Color of this Texture at coordinates <s,t> (0 <= s <= 1 and 0 <= t <= 1) for a surface seen at a given
	 * scale: with mipmapping, also called trilinear filtering.
	 *
	 * getInterpolatedColor(s, t) reads the 4 texels around the point. This is right as long as a texel is larger
	 * than a pixel of the screen. On a surface seen from far away, a pixel covers many texels, of which only these 4
	 * would be read, almost at random: the texture turns into noise, which flickers when the camera moves. Here the
	 * color is read in the mipmap level whose texels have the size of the pixel, where each texel is already the
	 * average of all the texels of the texture that it covers. The level is usually between two levels of the
	 * pyramid (see mipmapLevel()): the color is read in both (bilinear interpolation in each) and interpolated
	 * between the two, so that no line shows on a surface where the level changes.
	 *
	 * When a pixel is smaller than a texel (level 0 or below), the result is exactly getInterpolatedColor(s, t).
	 * The mipmaps must be built (buildMipmaps()); otherwise the texture itself is read.
	 *
	 * @param log2PixelSize base 2 logarithm of the size of a pixel, in texture coordinates (see mipmapLevel())
	 * @return the Color, never null
	 */
	public Color getInterpolatedColor(float s, float t, float log2PixelSize) {
		int[][] texels = mipTexels;
		float level = mipmapLevel(log2PixelSize);
		if (texels == null || level <= 0f) {
			return getInterpolatedColor(s, t);
		}

		// The two levels around the wanted one, and the share of the coarser one. Beyond the last level (a pixel
		// larger than the whole texture), the last level only: the average color of the texture.
		int last = texels.length - 1;
		int fine = Math.min((int) level, last);
		int coarse = Math.min(fine + 1, last);
		float share = fine == last ? 0f : level - fine;

		int fineColor = bilinear(texels[fine], mipWidths[fine], mipHeights[fine], s, t);
		if (share == 0f) {
			return new Color(fineColor);
		}
		int coarseColor = bilinear(texels[coarse], mipWidths[coarse], mipHeights[coarse], s, t);
		return new Color(
				mix((fineColor >> 16) & 0xFF, (coarseColor >> 16) & 0xFF, share),
				mix((fineColor >> 8) & 0xFF, (coarseColor >> 8) & 0xFF, share),
				mix(fineColor & 0xFF, coarseColor & 0xFF, share));
	}

	/** The value a share (0 to 1) of the way from a to b, two color components (0 to 255), rounded to the nearest */
	private static int mix(int a, int b, float share) {
		return Math.round(a + (b - a) * share);
	}

	/**
	 * Bilinear interpolation of the 4 texels around the point <s,t> in a mipmap level: the same computation as
	 * getInterpolatedColor(s, t), on the texels of that level. Returns a packed RGB int rather than a Color, so that
	 * the two levels of a trilinear filtering create a single Color, at the end.
	 */
	private int bilinear(int[] texels, int width, int height, float s, float t) {
		float u = s * width - 0.5f;
		float v = t * height - 0.5f;
		int x0 = (int) Math.floor(u);
		int y0 = (int) Math.floor(v);
		float uRatio = u - x0, vRatio = v - y0;
		int x1 = wrap(x0 + 1, width);
		int y1 = wrap(y0 + 1, height);
		x0 = wrap(x0, width);
		y0 = wrap(y0, height);

		int argb00 = texels[y0 * width + x0], argb01 = texels[y1 * width + x0];
		int argb10 = texels[y0 * width + x1], argb11 = texels[y1 * width + x1];
		float w00 = (1f - uRatio) * (1f - vRatio), w01 = (1f - uRatio) * vRatio, w10 = uRatio * (1f - vRatio), w11 = uRatio * vRatio;
		int red = Math.round(((argb00 >> 16) & 0xFF) * w00 + ((argb01 >> 16) & 0xFF) * w01 + ((argb10 >> 16) & 0xFF) * w10 + ((argb11 >> 16) & 0xFF) * w11);
		int green = Math.round(((argb00 >> 8) & 0xFF) * w00 + ((argb01 >> 8) & 0xFF) * w01 + ((argb10 >> 8) & 0xFF) * w10 + ((argb11 >> 8) & 0xFF) * w11);
		int blue = Math.round((argb00 & 0xFF) * w00 + (argb01 & 0xFF) * w01 + (argb10 & 0xFF) * w10 + (argb11 & 0xFF) * w11);
		return (red << 16) | (green << 8) | blue;
	}

	private static float clamp01(float v) {
		return v < 0f ? 0f : (v > 1f ? 1f : v);
	}

	private static float red(int argb) {
		return ((argb >> 16) & 0xFF) / 255f;
	}

	private static float green(int argb) {
		return ((argb >> 8) & 0xFF) / 255f;
	}

	private static float blue(int argb) {
		return (argb & 0xFF) / 255f;
	}

	// ------------------------------------------------------------------
	// Wrap mode: the texture outside of itself
	// ------------------------------------------------------------------

	/**
	 * Sets what this Texture shows at texture coordinates outside of 0 to 1 (see WrapMode): its border (CLAMP, the
	 * default), itself again (REPEAT) or itself again, every other time flipped (MIRROR).
	 *
	 * This is how a texture is repeated over a large surface, like tiles: the surface gives texture coordinates that
	 * go beyond 1, e.g. from 0 to 24 for 24 tiles (see Trellis.setTextureRepeat()), and the Texture folds them back,
	 * texel by texel. The surface does not need a vertex at each border of a tile, and the number of tiles does not
	 * need to be an integer. The mode applies wherever the Texture is used; it has no effect on a surface whose
	 * coordinates stay between 0 and 1, except within half a texel of its borders, where the bilinear interpolation
	 * reads the texel of the other side (REPEAT) instead of the border one.
	 *
	 * To be set before rendering, like the texels.
	 *
	 * @return this Texture, to chain the calls
	 */
	public Texture setWrapMode(WrapMode wrapMode) {
		if (wrapMode == null) throw new IllegalArgumentException("The wrap mode of a Texture cannot be null");
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
	 * the border of a tile: the 4 texels read there are the neighbours in the repeated image.
	 */
	private int wrap(int index, int size) {
		if (index >= 0 && index < size) return index; // Inside the texture: the same for all modes
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
	
	public Color getColor(int x, int y) {
		// TODO Implement x y validation and return exception if outside width and height
		return new Color(tex[y * width + x]);
	}
		
	public void setColor(int x, int y, Color c) {
		// TODO Implement x y validation and return exception if outside width and height
		tex[y * width + x] = c.getRGB();
		mipTexels = null; // The mipmaps, if any, no longer match: they will be built again (see buildMipmaps())
	}
	
	public int getWidth() {
		return this.width;
	}

	/**
	 * @return the name of this texture, for a report or a trace: the name of the file it was loaded from (without its
	 *         directory), or the name given by setName(); "width x height texture" for a texture without either
	 */
	public String getName() {
		return name != null ? name : width + " x " + height + " texture";
	}

	/**
	 * Gives a name to this texture (e.g. a texture computed pixel by pixel, which has no file name).
	 * @return this texture, to chain the call
	 */
	public Texture setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * @return the memory used by the texels of this texture, in bytes: 4 bytes (an ARGB int) for each of its
	 *         width x height texels, whatever the size of the (compressed) file it was loaded from, plus those of its
	 *         mipmaps if they are built (about one third more, see buildMipmaps())
	 */
	public long getMemorySize() {
		long texels = tex.length;
		int[][] mipmaps = mipTexels;
		if (mipmaps != null) {
			for (int level = 1; level < mipmaps.length; level++) texels += mipmaps[level].length;
		}
		return 4L * texels;
	}
	
	public int getHeight() {
		return this.height;
	}

}