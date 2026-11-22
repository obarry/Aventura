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
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import com.aventura.tools.tracing.Tracer;

/**
 * This class provides functionalites to load a texture file and then provide interpolated color using normalized coordinates
 * It is possible to load the bitmap file horizontally or vertically (just exchanging what is width and height, this is like portrait and landscape for pictures)
 * but also to "reverse" the bitmap (left/right) which is important for file having a "direction" (e.g. text written etc.) and how it is supposed to be used.
 * <p>
 * A Texture is the map of colors of the engine (see {@link SurfaceMap}: the size, the name, the wrap mode and the
 * mipmap levels are common to all the maps). Its role is given by where it is put: the color of a surface
 * (Triangle.setTexture()), or the light the surface emits by itself (Element.setEmissionMap()).
 * 
 * @author Olivier BARRY
 * @since April 2017
 */
public class Texture extends SurfaceMap {
	
	public static final int TEXTURE_DIRECTION_HORIZONTAL = 1;
	public static final int TEXTURE_DIRECTION_VERTICAL = 2;
	public static final int TEXTURE_ORIENTATION_NORMAL = 1;
	public static final int TEXTURE_ORIENTATION_OPPOSITE = 2;
	
	// The ARGB values of the texels, row by row: texel (x, y) is tex[y * width + x]. The texels of a row are
	// neighbors in memory, as the bilinear filtering below reads them (performance audit A6; it used to be an
	// int[x][y] array).
	protected int[] tex;
	// Mipmaps: smaller and smaller copies of the texture, each one half the size of the previous one, down to 1 x 1
	// (see SurfaceMap.buildMipmaps()). mipTexels[n] holds the texels of level n, row by row like tex; level 0 is tex
	// itself. null until they are built: they are only needed by getInterpolatedColor(s, t, pixelSize).
	protected int[][] mipTexels = null;

	
	/**
	 * Create an empty Texture of a given width and height
	 * @param width
	 * @param height
	 */
	public Texture(int width, int height) {
		super(width, height);
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
		this(readImage(fileName), new File(fileName).getName());
	}

	/**
	 * Create a Texture from an image found by a URL: typically a resource of the classpath, which works the same
	 * whether the application runs from its classes or from a jar, and whatever the current directory.
	 * <pre>
	 *   Texture marble = new Texture(MyApp.class.getResource("/texture/marble.jpg"));
	 * </pre>
	 * Class.getResource() is the way to read a resource of the caller's own module or jar: it is given the class
	 * of the application, so that the engine does not need to see the resources of the application.
	 *
	 * @param url the image, in any format read by ImageIO (JPEG, PNG, GIF...)
	 * @throws TextureLoadException if the URL is null (e.g. a resource that was not found: getResource() returns
	 *         null), or if the image cannot be read or decoded
	 * @since 3.3
	 */
	public Texture(URL url) {
		this(readImage(url), nameOf(url));
	}

	/** Create a Texture from an image already read, with the name of its source (see getName()) */
	private Texture(BufferedImage img, String name) {
		this.height = img.getHeight();
		this.width = img.getWidth();
		this.name = name;

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
		this(readImage(fileName), new File(fileName).getName(), direction, horizontal_orientation, vertical_orientation);
	}

	/**
	 * Create a Texture from an image found by a URL (see Texture(URL)), with a direction and orientations (see
	 * Texture(String, int, int, int)).
	 *
	 * @throws TextureLoadException if the URL is null or if the image cannot be read or decoded
	 * @since 3.3
	 */
	public Texture(URL url, int direction, int horizontal_orientation, int vertical_orientation) {
		this(readImage(url), nameOf(url), direction, horizontal_orientation, vertical_orientation);
	}

	/** Create a Texture from an image already read, with a direction and orientations */
	private Texture(BufferedImage img, String name, int direction, int horizontal_orientation, int vertical_orientation) {
		this.name = name;

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
	 * Reads an image file.
	 * @throws TextureLoadException if the file cannot be read/decoded as an image: fails fast with a clear message
	 *         rather than a NullPointerException later on (ImageIO.read() returns null, without an exception, when
	 *         the file exists but is not a recognized image format).
	 */
	private static BufferedImage readImage(String fileName) {
		BufferedImage img;
		try {
			img = ImageIO.read(new File(fileName));
		} catch (IOException e) {
			throw new TextureLoadException("Failed to load texture file: " + fileName, e);
		}
		if (img == null) {
			throw new TextureLoadException("Failed to load texture file (unrecognized image format): " + fileName);
		}
		return img;
	}

	/** Reads an image from a URL, with the same checks as readImage(String) */
	private static BufferedImage readImage(URL url) {
		if (url == null) {
			throw new TextureLoadException("Failed to load texture: no URL (a resource that was not found?)");
		}
		BufferedImage img;
		try {
			img = ImageIO.read(url);
		} catch (IOException e) {
			throw new TextureLoadException("Failed to load texture: " + url, e);
		}
		if (img == null) {
			throw new TextureLoadException("Failed to load texture (unrecognized image format): " + url);
		}
		return img;
	}

	/** The name of the file a URL points to: what follows its last '/' (e.g. "marble.jpg") */
	private static String nameOf(URL url) {
		String path = url.getPath();
		String name = path.substring(path.lastIndexOf('/') + 1);
		return URLDecoder.decode(name, StandardCharsets.UTF_8);
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

	/** The levels of the mipmaps: each one half of the previous one (see SurfaceMap.buildMipmaps()). */
	@Override
	protected void buildLevels(int[] widths, int[] heights) {
		int[][] texels = new int[widths.length][];
		texels[0] = tex;
		for (int level = 1; level < widths.length; level++) {
			texels[level] = halve(texels[level - 1], widths[level - 1], heights[level - 1], widths[level], heights[level]);
		}
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

	@Override
	public boolean hasMipmaps() {
		return mipTexels != null;
	}

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

	/**
	 * Sets what this Texture shows at texture coordinates outside of 0 to 1 (see SurfaceMap.setWrapMode()).
	 * @return this Texture, to chain the calls
	 */
	@Override
	public Texture setWrapMode(WrapMode wrapMode) {
		super.setWrapMode(wrapMode);
		return this;
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
	
	/**
	 * Gives a name to this texture (e.g. a texture computed pixel by pixel, which has no file name).
	 * @return this texture, to chain the call
	 */
	@Override
	public Texture setName(String name) {
		super.setName(name);
		return this;
	}

	@Override
	protected String getKind() {
		return "texture";
	}

	/**
	 * @return the memory used by the texels of this texture, in bytes: 4 bytes (an ARGB int) for each of its
	 *         width x height texels, whatever the size of the (compressed) file it was loaded from, plus those of its
	 *         mipmaps if they are built (about one third more, see buildMipmaps())
	 */
	@Override
	public long getMemorySize() {
		long texels = tex.length;
		int[][] mipmaps = mipTexels;
		if (mipmaps != null) {
			for (int level = 1; level < mipmaps.length; level++) texels += mipmaps[level].length;
		}
		return 4L * texels;
	}
	
}