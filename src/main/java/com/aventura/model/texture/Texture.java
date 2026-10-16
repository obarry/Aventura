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

		x0 = clampToWidth(x0);
		x1 = clampToWidth(x1);
		y0 = clampToHeight(y0);
		y1 = clampToHeight(y1);

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

	private int clampToWidth(int x) {
		if (x < 0) return 0;
		if (x >= width) return width - 1;
		return x;
	}

	private int clampToHeight(int y) {
		if (y < 0) return 0;
		if (y >= height) return height - 1;
		return y;
	}
	
	public Color getColor(int x, int y) {
		// TODO Implement x y validation and return exception if outside width and height
		return new Color(tex[y * width + x]);
	}
		
	public void setColor(int x, int y, Color c) {
		// TODO Implement x y validation and return exception if outside width and height
		tex[y * width + x] = c.getRGB();
	}
	
	public int getWidth() {
		return this.width;
	}
	
	public int getHeight() {
		return this.height;
	}

}