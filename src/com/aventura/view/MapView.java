package com.aventura.view;

import java.awt.Color;

/**
* ------------------------------------------------------------------------------ 
* MIT License
* 
* Copyright (c) 2016-2023 Olivier BARRY
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
* Mapiew is the a simple Map (square array of values, generally int) adapted to the View interface defined by the abstract class View
* It allows to use the RenderEngine to use Rasterizer to generate in a simple Map while still using the usual View interface.
* It is e.g. used for Shadow mapping rendering but could be used for any purpose when a Map needs to be rendered.
* 
*/

public class MapView extends View {
	
	int[][] map;
	Color drawingColor;
	Color backgroundColor;
	
	public MapView(int[][] map) {
		this.map = map;
		width = map[1].length;
		height = map.length;
		
		// Default colors
		drawingColor = new Color(1);
		backgroundColor = new Color(0);
	}
	
	public MapView(int width, int height) {
		this.width = width;
		this.height = height;
		map = new int[width][height];

		// Default colors
		drawingColor = new Color(1);
		backgroundColor = new Color(0);
	}
	
	@Override
	public void initView() {
		for (int i=0; i<width; i++) {
			for (int j=0; j<height; j++) {
				map[i][j] = backgroundColor.getRGB();
			}
		}

	}

	@Override
	public void renderView() {
		// TODO Auto-generated method stub
		// N/A for a MapView class (nothing to render) ?
	}
	
	@Override
	public void initView(View map) {
		for (int i=0; i<width; i++) {
			for (int j=0; j<height; j++) {
				this.drawPixel(i, j, map.getPixel(i, j));
			}
		}
	}

	@Override
	public Color getPixel(int x, int y) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setColor(Color c) {
		drawingColor = c;
	}

	@Override
	public void setBackgroundColor(Color c) {
		backgroundColor = c;
	}

	@Override
	public void drawPixel(int x, int y) {
		// TODO Auto-generated method stub
		map[x][y] = drawingColor.getRGB();

	}

	@Override
	public void drawPixel(int x, int y, Color c) {
		map[x][y] = c.getRGB();
	}

	@Override
	public void drawLine(int x1, int y1, int x2, int y2) {
		// TODO Auto-generated method stub

	}

}
