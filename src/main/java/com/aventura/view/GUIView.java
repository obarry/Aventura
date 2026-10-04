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

import java.awt.Color;

import com.aventura.context.PerspectiveContext;

/**
* GUIView is the (abstract) class handled by the rendering engine to display the pixels while rendering elements (rasterization)
* 
* A typical 'session' of rendering a frame in a view is to:
* 	1) initialize the view, this will setup a new image buffer of the size to be rendered
* 		initView()
* 	2) display pixels (lines, pixels, surfaces, with colors etc.)
* 		drawPixel(), drawLine(), etc.
* 	3) render the image, e.g. swap the back buffer to the front buffer and notify the GUI
* 		renderView()
* 
* The abstract class GUIView remains GUI type independent. Implementations:
* - ImageView: concrete and GUI-independent, double buffered BufferedImage images; usable headless
*   (e.g. image files) or with any GUI toolkit through its frame listener;
* - SwingView: an ImageView that also repaints a Swing Component after each frame.
* Other display technologies can derive from ImageView (simplest) or directly from GUIView.
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
	 * Create the view based on PerspectiveContext to get its pixel width and height
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
	
	/**
	 * Adds a light contribution to a pixel (centered coordinates, Y axis up): each channel of the current pixel
	 * is increased by the given amount (0 = nothing, 1 = full scale) and saturates at full scale. Pixels outside
	 * of the image are ignored. Generic implementation based on getPixel() / drawPixel(); the specializations
	 * may provide a faster one.
	 */
	public void addPixel(int x, int y, float r, float g, float b) {
		Color c = getPixel(x, y);
		if (c == null) return;
		drawPixel(x, y, new Color(
				Math.min(255, c.getRed() + Math.round(r * 255)),
				Math.min(255, c.getGreen() + Math.round(g * 255)),
				Math.min(255, c.getBlue() + Math.round(b * 255))));
	}

}
