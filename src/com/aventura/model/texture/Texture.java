package com.aventura.model.texture;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.aventura.tools.color.ColorTools;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2017 Olivier BARRY
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
 * @author Olivier BARRY
 * @since April 2017
 */
public class Texture {
	
	// array containing data e.g. rgb values
	protected Color[][] tex;
	protected int width, height;
	
	/**
	 * Create an empty Texture of a given width and height
	 * @param width
	 * @param height
	 */
	public Texture(int width, int height) {
		this.width = width;
		this.height = height;
		tex = new Color[width][height];
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
        
        tex = new Color[width][height];
        
        for (int h=0; h<height; h++) {
            for (int w=0; w<width; w++) {
            	tex[w][h] = new Color(img.getRGB(w, h));
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
	public Color getInterpolatedColor(double s, double t) throws Exception {

		// Calculate the coordinates within the texture (-0.5 as per bressenham)
		double u = s * this.width - 0.5;
		double v = t * this.height - 0.5;

		// Calculate the integer value of u and v
		int x0 = (int) Math.floor(u);
		int y0 = (int) Math.floor(v);
		int x1 = x0+1;
		int y1 = y0+1;
		
		// Calculate the frac value of u and v (their respective complement to 1 will be computed in the getBilinearFilteredColor method directly)
		float u_ratio = (float)u - x0;
		float v_ratio = (float)v - y0;
		
		if (x0<0) x0=0;
		if (y0<0) y0=0;
		if (x1>=this.width) x1=this.width-1;
		if (y1>=this.height) y1=this.height-1;

		// Calculate the interpolated value as per Bilinear Filtered algorithm
		Color result = ColorTools.getBilinearFilteredColor(tex[x0][y0], tex[x0][y1], tex[x1][y0], tex[x1][y1], u_ratio, v_ratio);
		return result;
	}
	
	public Color getColor(int x, int y) {
		// TODO Implement x y validation and return exception if outside width and height
		return tex[x][y];
	}
		
	public void setColor(int x, int y, Color c) {
		// TODO Implement x y validation and return exception if outside width and height
		tex[x][y] = c;
	}
	
	public int getWidth() {
		return this.width;
	}
	
	public int getHeight() {
		return this.height;
	}

}
