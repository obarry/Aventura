package com.aventura.view;

import java.awt.Color;

import com.aventura.context.GraphicContext;

/**
* ------------------------------------------------------------------------------ 
* MIT License
* 
* Copyright (c) 2016-2022 Olivier BARRY
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
* View is the (abstract) class handled by the rendering engine to display the pixels while rendering elements (rasterization)
* 
* A typical 'session' of creating a view is to:
* 	1) initializing view, this will setup a new image buffer of the size to be rendered
* 		initView()
* 	2) display pixels (lines, pixels, surfaces, with colors etc.)
* 		drawPixel(), drawLine(), etc.
* 	3) allowing to render the image, this will copy the buffer image into the graphic buffer of the GUI interface
* 
* The abstract class View remains GUI type independent.
* This class should be derived to create a display specific class (e.g. SWING or SWT or any display device)
* 
*/
public abstract class View {
	
	protected int width;
	protected int height;
	
	/**
	 * Create the view based on GraphicContext to get width and height information of the view frustum
	 * Indeed the View is expected to match exactly these dimensions. 
	 * 
	 * @param context
	 */
	public View(GraphicContext context) {
		
		// Both width and height are cast to (int) for the View that is pixel based
		width = context.getPixelWidth();
		height = context.getPixelHeight();
	}
	
	public View(float[][] map) {
		width = map[1].length;
		height = map.length;
	}
	
	public int getViewWidth() {
		return width;
	}
	
	public int getViewHeight() {
		return height;
	}
	
	public abstract void initView(); // init back buffer
	public abstract void renderView(); // swap back buffer to front buffer, ready to display once the GUI will refresh
	public abstract void setColor(Color c); // Using java.awt.Color class
	public abstract void setBackgroundColor(Color c); // Using java.awt.Color class
	public abstract void drawPixel(int x, int y);
	public abstract void drawPixel(int x, int y, Color c);
	public abstract void drawLine(int x1, int y1, int x2, int y2);

}
