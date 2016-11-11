package com.aventura.engine;

import java.awt.Color;

import com.aventura.context.GraphicContext;
import com.aventura.model.world.Line;
import com.aventura.model.world.Triangle;
import com.aventura.model.world.Vertex;
import com.aventura.tools.tracing.Tracer;
import com.aventura.view.View;

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
 * This class provides rasterization services to the RenderEngine.
 * It takes charge of all the algorithms to produce pixels from triangles.
 * It requires access to the View in order to draw pixels.
 * It also behaves according to the provided parameters e.g. in the GraphicContext
 * 
 * 
 * @author Bricolage Olivier
 * @since November 2016
 *
 */

public class Rasterizer {
	
	private GraphicContext graphic;
	private View view;
	
	// Z buffer
	private double[][] zBuffer;
	
	public Rasterizer(GraphicContext graphic) {
		this.graphic = graphic;
	}
	
	public void setView(View v) {
		this.view = v;
	}
	
	/**
	 * Initialize zBuffer by creating the table. This method is deported from the constructor in order to use it only when necessary.
	 * It is not needed in case of line rendering.
	 */
	public void initZBuffer() {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating zBuffer. Width: "+graphic.getPixelWidth()+" Height: "+graphic.getPixelHeight());	
		zBuffer = new double[graphic.getPixelWidth()][graphic.getPixelHeight()];
		
		// TODO initialization loop with the correct initialization value ( 1 or -1 in homogeneous coordinates ?) as farest value since
		// this value represent the far plane of the view frustum.
		// Caution: if this is -1 (TBC), then the direction of the test z < zBuffer in renderTriangle() method should be adapted
	}
	
	// Method for Line only Rendering
		
	public void drawTriangleLines(Triangle t, Color c) {
		
		view.setColor(c);
		drawLine(t.getV1(), t.getV2());
		drawLine(t.getV2(), t.getV3());
		drawLine(t.getV3(), t.getV1());
	}
	
	public void drawLine(Line l) {
		drawLine(l.getV1(), l.getV2());
	}
	
	public void drawLine(Vertex v1, Vertex v2) {

		int x1, y1, x2, y2;
		
		x1 = (int)(v1.getPosition().get3DX()*graphic.getPixelWidth()/2);
		y1 = (int)(v1.getPosition().get3DY()*graphic.getPixelHeight()/2);
		x2 = (int)(v2.getPosition().get3DX()*graphic.getPixelWidth()/2);
		y2 = (int)(v2.getPosition().get3DY()*graphic.getPixelHeight()/2);

		view.drawLine(x1, y1, x2, y2);
	}

	// End methods for Line only Rendering
	
	public void rasterizeTriangle(Triangle t, Color c) {
		
		// For each pixel(xpix, ypix) of the projected Triangle corresponding to a Position(xpos, ypos, zpos) on the triangle surface in 3D world space
		// Use bressenham for this rasterization
		// The below may need to be factored in a separate rasterizePixel method to be called from different parts of the bressenham algo
		
		// z = position.getZ()   // (zpos)
		
		// if z < zBuffer(xpix, ypix)
		
		// then 
		
		// Calculate Normal (by interpolation and based on Normal at Triangle level or at Vertex level
		// Calculate Lighting
		// Calculate Color from Normal
		// Calculate Color from Distance
		// Calculate etc.
		// ...
		
		// draw pixel with calculated Color
		
		// zBuffer(xpix, ypix) = z    // Update zBuffer with new value
		// Caution: if far plane is -1 (TBC), then the direction of the test z < zBuffer may need to be adapted
		
		// else
		
		// nothing to do
		
		// endif
		
	}
}
