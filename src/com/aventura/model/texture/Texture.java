package com.aventura.model.texture;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.aventura.tools.color.ColorTools;
import com.aventura.tools.tracing.Tracer;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2016-2024 Olivier BARRY
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
	
	// array containing data e.g. rgb values
	protected int[][] tex;
	protected int width, height;
	
	/**
	 * Create an empty Texture of a given width and height
	 * @param width
	 * @param height
	 */
	public Texture(int width, int height) {
		this.width = width;
		this.height = height;
		tex = new int[width][height];
	}
	
	/**
	 * Create a Texture from a bitmap file
	 * @param fileName
	 */
	public Texture(String fileName) {
        BufferedImage img = null;
        try {
            img = ImageIO.read(new File(fileName));
        } catch (IOException e) {
        	// TODO To be implemented, manage exception and safely return with an Error
        }
        this.height = img.getHeight();
        this.width = img.getWidth();
        
        tex = new int[width][height];
        
        for (int h=0; h<height; h++) {
            for (int w=0; w<width; w++) {
            	tex[w][h] = img.getRGB(w, h);
            }
        }
        // Flush BufferedImage data, they are no longer needed
        img.flush();
	}

	/**
	 * Create a Texture from a bitmap file
	 * @param fileName
	 */
	public Texture(String fileName, int direction, int horizontal_orientation, int vertical_orientation) {
		BufferedImage img = null;
		try {
			img = ImageIO.read(new File(fileName));
		} catch (IOException e) {
			// TODO To be implemented, manage exception and safely return with an Error
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

		tex = new int[width][height];

		for (int h=0; h<height; h++) {
			for (int w=0; w<width; w++) {
				if (direction == TEXTURE_DIRECTION_VERTICAL) {
					
					if (horizontal_orientation == TEXTURE_ORIENTATION_NORMAL) {
						if (vertical_orientation == TEXTURE_ORIENTATION_NORMAL) {
							tex[w][h] = img.getRGB(w, h);
						} else if (vertical_orientation == TEXTURE_ORIENTATION_OPPOSITE) {
							tex[w][h] = img.getRGB(w, height-h-1);
						} else {
							// Should never happen
						}

					} else if (horizontal_orientation == TEXTURE_ORIENTATION_OPPOSITE) {
						if (vertical_orientation == TEXTURE_ORIENTATION_NORMAL) {
							tex[w][h] = img.getRGB(width-w-1, h);
						} else if (vertical_orientation == TEXTURE_ORIENTATION_OPPOSITE) {
							tex[w][h] = img.getRGB(width-w-1, height-h-1);
						} else {
							// Should never happen
						}

					} else {
						// Should never happen
					}
					
				} else if (direction == TEXTURE_DIRECTION_HORIZONTAL) {
					if (horizontal_orientation == TEXTURE_ORIENTATION_NORMAL) {
						if (vertical_orientation == TEXTURE_ORIENTATION_NORMAL) {
							tex[w][h] = img.getRGB(h, w); // Inverse h and w reading
						} else if (vertical_orientation == TEXTURE_ORIENTATION_OPPOSITE) {
							tex[w][h] = img.getRGB(height-h-1,w);
						} else {
							// Should never happen
						}

					} else if (horizontal_orientation == TEXTURE_ORIENTATION_OPPOSITE) {
						if (vertical_orientation == TEXTURE_ORIENTATION_NORMAL) {
							tex[w][h] = img.getRGB(h, width-w-1);
						} else if (vertical_orientation == TEXTURE_ORIENTATION_OPPOSITE) {
							tex[w][h] = img.getRGB(height-h-1, width-w-1);
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
	 * @param s
	 * @param t
	 * @return
	 */
	public Color getInterpolatedColor(float s, float t) throws Exception {

		// Calculate the coordinates within the texture (-0.5 as per bressenham)
		float u = s * this.width - 0.5f;
		float v = t * this.height - 0.5f;

		// Calculate the integer value of u and v
		int x0 = (int) Math.floor(u);
		int y0 = (int) Math.floor(v);
		int x1 = x0 + 1;
		int y1 = y0 + 1;
		
		// Calculate the frac value of u and v (their respective complement to 1 will be computed in the getBilinearFilteredColor method directly)
		float u_ratio = (float)u - x0;
		float v_ratio = (float)v - y0;
		
		if (x0<0) x0 = 0;
		if (y0<0) y0 = 0;
		if (x0>=this.width)  x0 = this.width - 1;
		if (y0>=this.height) y0 = this.height - 1;
		if (x1<0) x1 = 0;
		if (y1<0) y1 = 0;
		if (x1>=this.width)  x1 = this.width - 1;
		if (y1>=this.height) y1 = this.height - 1;

		// Calculate the interpolated value as per Bilinear Filtering algorithm
		Color result = null;
		try {
		 result = ColorTools.getBilinearFilteredColor(new Color(tex[x0][y0]), new Color(tex[x0][y1]), new Color(tex[x1][y0]), new Color(tex[x1][y1]), u_ratio, v_ratio);
		} catch (java.lang.ArrayIndexOutOfBoundsException e) {
			if (Tracer.error) Tracer.traceError(this.getClass(), "Exception getting bilinear filtered color for: x0="+x0+" y0="+y0+" x1="+x1+" y1="+y1+". Texture width: "+this.width+" height:"+this.height);
			e.printStackTrace();
		}
		return result;
	}
	
	public Color getColor(int x, int y) {
		// TODO Implement x y validation and return exception if outside width and height
		return new Color(tex[x][y]);
	}
		
	public void setColor(int x, int y, Color c) {
		// TODO Implement x y validation and return exception if outside width and height
		tex[x][y] = c.getRGB();
	}
	
	public int getWidth() {
		return this.width;
	}
	
	public int getHeight() {
		return this.height;
	}

}
