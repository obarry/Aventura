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
package com.aventura.view;

/**
* MapView is a simple Map (array of values, generally int) adapted to the GUIView interface defined by the abstract class GUIView
* It is used as storage by ZBuffer (depth buffer of the main pass, shadow maps), filled by TriangleRasterizer.
* It is e.g. used for Shadow mapping rendering but could be used for any purpose when a Map needs to be rendered.
*
* Storage: a single float[] of width x height values, row by row (index y * width + x). The rasterizer walks the
* pixels row by row (x varies, y fixed), so consecutive pixels are consecutive in memory: this is what the CPU
* caches are made for. (It used to be a float[x][y] array: each pixel of a row was then in a different Java array,
* with a cache miss almost every time. See docs/PERFORMANCE_AUDIT.md, D7.)
* The accessors keep the (x, y) convention: the storage is an internal detail.
* 
 */

public class MapView extends View {
	
	/** The values, row by row: the value of (x, y) is data[y * width + x] */
	protected float[] data;
	
	/**
	 * Creates a map from an existing array indexed map[x][y]: width = map.length, height = map[0].length.
	 * The values are copied (the map does not keep a reference to this array).
	 */
	public MapView(float[][] map) {
		this(map.length, map.length > 0 ? map[0].length : 0);
		for (int x=0; x<width; x++) {
			for (int y=0; y<height; y++) {
				set(x, y, map[x][y]);
			}
		}
	}
	
	// Recopy constructor
	public MapView(MapView view) {
		this.width = view.width;
		this.height = view.height;
		this.data = view.data.clone();
	}
	
	public MapView(int width, int height) {
		this.width = width;
		this.height = height;
		this.data = new float[width * height]; // Java initializes it with 0
	}
	
	public void initView() {
		fill(0);
	}
	
	/**
	 * Changes the dimensions of this map (a new array is allocated) and initializes it with 0.
	 * (It used to keep the old array: a larger size raised an ArrayIndexOutOfBoundsException.)
	 */
	@Override
	public void initView(int width, int height) {
		this.width = width;
		this.height = height;
		this.data = new float[width * height]; // Java initializes it with 0
	}

	public void initView(float f) {
		fill(f);
	}
	
	/** Sets every value of this map to f */
	public void fill(float f) {
		java.util.Arrays.fill(data, f);
	}
	
	public float get(int x, int y) {
		return data[y * width + x];
	}

	public void set(int x, int y, float f) {
		data[y * width + x] = f;
	}
	
	/**
	 * @return a copy of the values, as an array indexed [x][y] (the map itself is stored row by row, see the class
	 * description): changing this array does not change the map.
	 */
	public float[][] getMap() {
		float[][] map = new float[width][height];
		for (int x=0; x<width; x++) {
			for (int y=0; y<height; y++) {
				map[x][y] = get(x, y);
			}
		}
		return map;
	}
	
	public float getMax() {
		float max = data[0];
		for (int i=1; i<data.length; i++) {
			if (data[i] > max) max = data[i];
		}
		return max;
	}
	
	public float getMin() {
		float min = data[0];
		for (int i=1; i<data.length; i++) {
			if (data[i] < min) min = data[i];
		}
		return min;
	}

	public float getAverage() {
		float avg = 0;
		for (int i=0; i<data.length; i++) {
			avg += data[i];
		}
		return avg/(width*height);
	}
	
	public int getNbOfPixelsInRange(float min, float max) {
		int n = 0;
		for (int i=0; i<data.length; i++) {
			if (data[i]>=min && data[i]<=max) n++;
		}
		return n;
	}

	public int getNbOfPixels() {
		return width*height;
	}
	
	// To normalize between 0 and 1 so that the map can be used for Colors
	// A uniform map (max == min) becomes all 0 (it used to become all NaN, division by 0)
	public void normalizeMap() {
		float max = this.getMax();
		float min = this.getMin();
		
		if (max == min) {
			initView();
			return;
		}
		
		for (int i=0; i<data.length; i++) {
			data[i] = (data[i]-min)/(max-min);
		}
	}
	
	// To zero values beyond far (e.g. for Zbuffering)
	public void removeFar(float far, float replaceBy) {
		for (int i=0; i<data.length; i++) {
			if (data[i] >= far) data[i] = replaceBy;
		}
	}
	
	// TODO This is same algorithm than Texture bilinear filtering although using only floats. But this may be factored together. To be investigated : Design Pattern ?
	/**
	 * Calculate the bilinear interpolated Value of this Map at coordinates <s,t> with 0 <= s <= 1 and 0 <= t <= 1
	 * @param s
	 * @param t
	 * @return
	 */
	public float getInterpolation(float s, float t) {

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
		return getBilinearFilteredComponent(get(x0, y0), get(x0, y1), get(x1, y0), get(x1, y1), u_ratio, v_ratio);

	}

	// TODO this method below is the copy of the protected method in ColorTools -> could be factored in other place (common tools)
	// as this is more generic and not specific to Color
	/**
	 * Calculate one Bilinear filtered Color component
	 * 
	 * @param z11 First color sample on axis 1 (generally X)
	 * @param z12 Second color sample on axis 1 (generally X)
	 * @param z21 First color sample on axis 2 (generally Y)
	 * @param z22 Second color sample on axis 2 (generally Y)
	 * @param u_ratio Ratio of the first position on first axis (second position ratio is 1-u_ratio)
	 * @param v_ratio Ratio of the first position on second axis (second position ratio is 1-v_ratio)
	 * @return the interpolated Bi-linear filtered component
	 */
	protected static float getBilinearFilteredComponent(float z11, float z12, float z21, float z22, float u_ratio, float v_ratio) {
		
		float u_opposite = 1 - u_ratio;
		float v_opposite = 1 - v_ratio;

		// Calculate the interpolated value as per algorithm: f(x,y) = (1 - {x})((1 - {y})z11 + {y}z12) + {x}((1 - {y})z21 + {y}z22)
		// https://en.wikipedia.org/wiki/Bilinear_filtering
		float result = (z11 * u_opposite + z21 * u_ratio) * v_opposite + (z12 * u_opposite + z22 * u_ratio) * v_ratio;
		return result;
	}

}
