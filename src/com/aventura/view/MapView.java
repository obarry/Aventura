package com.aventura.view;

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
* MapView is a simple Map (array of values, generally int) adapted to the GUIView interface defined by the abstract class GUIView
* It allows to use the RenderEngine to use Rasterizer to generate in a simple Map while still using the usual GUIView interface.
* It is e.g. used for Shadow mapping rendering but could be used for any purpose when a Map needs to be rendered.
* 
*/

public class MapView extends View {
	
	protected float[][] map;
	
	public MapView(float[][] map) {
		this.map = map;
		
		this.width = map[1].length;
		this.height = map.length;
	}
	
	// Recopy constructor
	public MapView(MapView view) {
		this.width = view.width;
		this.height = view.height;
		
		this.map = new float[width][height];
		for (int i=0; i<width; i++) {
			for (int j=0; j<height; j++) {
				this.map[i][j] = view.get(i, j);
			}
		}
	}
	
	public MapView(int width, int height) {
		this.width = width;
		this.height = height;
		
		this.map = new float[width][height];
	}
	
	public void initView() {
		for (int i=0; i<width; i++) {
			for (int j=0; j<height; j++) {
				map[i][j] = 0;
			}
		}
	}
	
	public void initView(float f) {
		for (int i=0; i<width; i++) {
			for (int j=0; j<height; j++) {
				map[i][j] = f;
			}
		}

	}
	
	public float get(int x, int y) {
		return map[x][y];
	}

	public void set(int x, int y, float f) {
		map[x][y] = f;
	}
	
	public float[][] getMap() {
		return map;
	}
	
	public float getMax() {
		float max = map[0][0];
		
		for (int i=0; i<width; i++) {
			for (int j=0; j<height; j++) {
				if (map[i][j] > max) max = map[i][j];
				}
		}
		
		return max;
	}
	
	public float getMin() {
		float min = map[0][0];
		
		for (int i=0; i<width; i++) {
			for (int j=0; j<height; j++) {
				if (map[i][j] < min) min = map[i][j];
				}
		}
		
		return min;
	}
	
	// To normalize between 0 and 1 so that the map can be used for Colors
	public void normalizeMap() {
		float max = this.getMax();
		float min = this.getMin();
		
		for (int i=0; i<width; i++) {
			for (int j=0; j<height; j++) {
				map[i][j] = (map[i][j]-min)/(max-min);
			}
		}
	}
	
	// To zero values beyond far (e.g. for Zbuffering)
	public void removeFar(float far) {

		for (int i=0; i<width; i++) {
			for (int j=0; j<height; j++) {
				if (map[i][j] >= far) map[i][j] =0;
			}
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
		return getBilinearFilteredComponent(map[x0][y0], map[x0][y1], map[x1][y0], map[x1][y1], u_ratio, v_ratio);

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
		float result = (z11*u_opposite+z21*u_ratio)*v_opposite+(z12*u_opposite+z22*u_ratio)*v_ratio;
		return result;
	}


}
