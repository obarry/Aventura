package com.aventura.view;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.image.BufferedImage;

import com.aventura.tools.tracing.Tracer;

/**
* ------------------------------------------------------------------------------ 
* MIT License
* 
* Copyright (c) 2016 Olivier BARRY
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
* This class manages the front and back buffer image.
* While the back buffer image is being drawn, the front image can be safely displayed/rendered.
* As the rendering of the GUI is not under the control of the API, this method is safer and 
* avoids calculating the same image each time the display should be rendered (e.g. window moved).
* 
*  The implementation of this class is SWING based and specific, but the public methods are generic
*  and inherited from the abstract View class.
* 
*       Warning! SWING Graphic coords on screen are as follows:
*    
*                      |
*                      |
*                 -----+-----> X
*                      |
*                      |
*                      v
*                      
*                      Y
*/

public class SwingView extends View {

	// Swing component to which this View is associated. Used to pro-actively repaint when needed.
	Component component = null;
	
	// buffer image #1 to be displayed
	BufferedImage frontbuffer;
	Graphics2D frontgraph;
	
	// back buffer image #2 to be used while creating the view
	BufferedImage backbuffer;
	Graphics2D backgraph;
	
	
	public SwingView(int w, int h) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "Creating new SwingView w: "+w+" h: "+h);
		width = w;
		height = h;

		frontbuffer = new BufferedImage(w,h, BufferedImage.TYPE_INT_RGB);
		frontgraph = (Graphics2D)frontbuffer.getGraphics();
	
        // Translate origin of the graphic
		frontgraph.translate(width/2, height/2);
	}

	public SwingView(int w, int h, Component comp) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "Creating new SwingView w: "+w+" h: "+h);
		width = w;
		height = h;

		frontbuffer = new BufferedImage(w,h, BufferedImage.TYPE_INT_RGB);
		frontgraph = (Graphics2D)frontbuffer.getGraphics();
	
        // Translate origin of the graphic
		frontgraph.translate(width/2, height/2);
		
		// Initialize the Jcomponent to which this view is associated
		component = comp;
	}

	@Override
	public void initView() {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "Initializing SwingView");

		// Reinitialize the back buffer image #2 - need to define color of the back as per Graphic context definition
		backbuffer = new BufferedImage(width,height, BufferedImage.TYPE_INT_RGB);
		backgraph = (Graphics2D)backbuffer.getGraphics();

        // Translate origin of the graphic
		backgraph.translate(width/2, height/2);
	}

	@Override
	public void renderView() {
		// TODO Auto-generated method stub
		// Swap : copy the back buffer image #2 into #1 
		frontbuffer = backbuffer;
		frontgraph = backgraph;
		
		// Repaint the component since the buffer has been updated
		component.repaint();
	}

	@Override
	public void setColor(Color c) {
		// TODO Auto-generated method stub
		backgraph.setColor(c);
		
	}
	
	@Override
	public void drawPixel(int x, int y) {
		drawSwingLine(x,y,x,y);
	}


	@Override
	public void drawLine(int x1, int y1, int x2, int y2) {
		drawSwingLine(x1,y1,x2,y2);
	}
	
	/**
	 * This method convert from coordinates with Y axis up to Y axis down (SWING)
	 * 
	 *   ^ Y			  +------> X
	 *   |				  |
	 *   |			-->   |  (SWING coordinates)
	 *   |				  |
	 *   +------> X       v Y
	 * 
	 * @param x1
	 * @param y1 (Y axis up)
	 * @param x2
	 * @param y2 (Y axis up)
	 */
	protected void drawSwingLine(int x1, int y1, int x2, int y2) {
		backgraph.drawLine(x1,-y1,x2,-y2);
	}
	
	/**
	 * This is "THE" method triggering all the rendering !!!
	 * This method is GUI type specific : as this is a callback method, it is known by the GUI, not by the engine.
	 * 
	 * It is called by the JPanel while this component is repainting (the method paintComponent() is called)
	 * As we initialized the JPanel with a View (actually a SwingView as this is a Swing GUI), then all the
	 * Aventura related processing is done by the API implementation, using the provided Graphics context: graph
	 * (Graphics -> Graphics2D can be easily extracted).
	 * 
	 * This method should trigger the repaint() but not (necessarily) the rendering generation.
	 * This means that (assuming this is implemented by the API) there is a double buffering -> this method will display the generated view only
	 * not the one under construction (if any is under construction).
	 * 
	 * @param graph, the Swing interface graphic context to be used to display in the frame/panel
	 */
	public void draw(Graphics graph) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "draw Swing View");

		// Display the buffer image #1
		((Graphics2D)graph).drawImage(frontbuffer, null, null);
		
	}
}
