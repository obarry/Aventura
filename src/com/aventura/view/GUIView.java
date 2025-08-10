package com.aventura.view;

import java.awt.Color;

import com.aventura.context.PerspectiveContext;

/**
* ------------------------------------------------------------------------------ 
* MIT License
* 
* Copyright (c) 2016-2025 Olivier BARRY
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
* GUIView is the (abstract) class handled by the rendering engine to display the pixels while rendering elements (rasterization)
* 
* A typical 'session' of creating a gUIView is to:
* 	1) initializing gUIView, this will setup a new image buffer of the size to be rendered
* 		initView()
* 	2) display pixels (lines, pixels, surfaces, with colors etc.)
* 		drawPixel(), drawLine(), etc.
* 	3) allowing to render the image, this will copy the buffer image into the graphic buffer of the GUI interface
* 
* The abstract class GUIView remains GUI type independent.
* This class should be derived to create a display specific class (e.g. SWING or SWT or any display device)
* 
*/
public abstract class GUIView extends View {
	
	// Static data
	public static Color DEFAULT_BACKGROUND_COLOR = Color.BLACK;
	
	// Color
	protected Color backgroundColor = null;
	
	public GUIView() {
		// Do nothing
	}
	
	/**
	 * Create the gUIView based on PerspectiveContext to get width and height information of the gUIView frustum
	 * Indeed the GUIView is expected to match exactly these dimensions. 
	 * 
	 * @param context
	 */
	public GUIView(PerspectiveContext context) {
		
		// Both width and height are cast to (int) for the GUIView that is pixel based
		this.width  = context.getPixelWidth();
		this.height = context.getPixelHeight();
		
	}
	
	public GUIView(int width, int height) {
		
		// Both width and height are cast to (int) for the GUIView that is pixel based
		this.width  = width;
		this.height = height;
		
	}
		
	public abstract void initView(MapView map); // init back buffer with another map of MapView type

	public abstract void renderView(); // swap back buffer to front buffer, ready to display once the GUI will refresh
	
	public abstract void setColor(Color c); // Using java.awt.Color class
	public abstract void setBackgroundColor(Color c); // Using java.awt.Color class
	
	public abstract Color getPixel(int x, int y); // Return Color of the pixel
	public abstract void drawPixel(int x, int y);
	
	public abstract void drawPixel(int x, int y, Color c);
	public abstract void drawLine(int x1, int y1, int x2, int y2);

}
