package com.aventura.view;

import java.awt.Color;
import java.lang.reflect.Array;

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

}
