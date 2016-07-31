package com.aventura.engine;

import com.aventura.context.GraphicContext;
import com.aventura.context.RenderContext;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.Lighting;
import com.aventura.model.world.Element;
import com.aventura.model.world.Triangle;
import com.aventura.model.world.World;
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
 * 
 * This class is the core rendering engine of the Aventura API
 * It provides the render method
 * It needs to be initialized with proper:
 * - The world information
 * - A camera
 * - Some lighting
 * - some display and graphics (called View)
 * - a render context to provide information on how to render the world
 * - a graphic context to provide information on how to display the view 
 *
 *     +---------------------+														
 *     |        World        | <------+											
 *     +---------------------+        |										+---------------------+
 *                					  |						  +------------>|    RenderContext    |
 *                   				  |						  |				+---------------------+
 *                					  |						  |
 *     +---------------------+		  |		+---------------------+							   			   +---------------------+
 *     |      Lighting       | <-----+------|    RenderEngine     |--------------------------------------->|        View         |
 *     +---------------------+		  |		+---------------------+ 									   +---------------------+
 *                					  |				   |	  |														  |
 *                   				  |				   |	  |				+---------------------+					  |
 *                	 				  |				   |	  +------------>|   GraphicContext    |<------------------+
 *     +---------------------+        |        		   v					+---------------------+
 *     |       Camera        | <------+     +---------------------+
 *     +---------------------+			    |      ModelView      |
 *					   				    	+---------------------+
 *
 *          	 Model								 Engine						Context(s)							 View
 *			com.aventura.model					com.aventura.engine			com.aventura.context				com.aventura.view
 * 
 * @author Bricolage Olivier
 * @since May 2016
 */
public class RenderEngine {
	
	// Context's parameters
	RenderContext render;
	GraphicContext graphic;
	
	// Model
	World world;
	Lighting light;
	Camera camera;
	
	// View
	View view;
	
	// ModelView transformation
	ModelView transformation;
	
	/**
	 * Create a Rendering Engine with required dependencies and context
	 * There should be a Rendering Engine for a single World, a single (consolidated) Lighting, a single Camera
	 * The parameters for the rendering and the display are respectively passed into the RenderContext and the GraphicContext
	 * 
	 * Rendering a World on different Views e.g. with several Cameras will require multiple RenderEngine instances
	 * 
	 * 
	 * @param world the world to render
	 * @param light the lights lighting the world
	 * @param camera the camera watching the world
	 * @param render the render context containing parameters to render the scene
	 * @param graphic the graphic context to contain parameters to display the scene
	 */
	public RenderEngine(World world, Lighting light, Camera camera, RenderContext render, GraphicContext graphic) {
		this.render = render;
		this.graphic = graphic;
		this.world = world;
		this.light = light;
		this.camera = camera;
		
		// Create ModelView matrix with for View (World -> Camera) and Projection (Camera -> Homogeneous) Matrices
		this.transformation = new ModelView(camera.getMatrix(), graphic.getProjectionMatrix()); 
	}
	
	/**
	 * 
	 * Rasterization of all triangles of the world
	 * This assumes that the initialization is already done
	 * - Projection matrix
	 * - Screen and display area
	 * - etc.
	 */
	public void render() {
		
		// For each element of the world
		for (int i=0; i<world.getElements().size(); i++) {
			
			// Calculate the ModelView matrix for this Element (Element <-> Model)		
			Element e = world.getElement(i);
			transformation.setModel(e.getTransformationMatrix()); // set the Model Matrix (the one attached to each Element) in the ModelView Transformation
			transformation.computeTransformation(); // Compute the whole ModelView transformation matrix
			
			// TODO Do a recursive call for SubElements
			
			// Process each Triangle
			for (int j=0; j<e.getTriangles().size(); j++) {				
				render(e.getTriangle(j));	
			}
		}
	}
	
	/**
	 * Rasterization of a single Triangle
	 * This assumes that the initialization is already done
	 * @param t the triangle to rasterize
	 */
	public void render(Triangle t) {
		
		Triangle triangle; // The projected model view triangle in homogeneous coordinates 
		
		// Project this Triangle in the View in homogeneous coordinates
		// This new triangle contains vertices that are transformed
		triangle = transformation.transform(t);
		
		// Scissor test for the triangle
		// If triangle is totally or partially in the View Frustum
		// Then render its fragments in the View
		if (isInView(triangle)) {
			// Render triangle
			
			// If the rendering type is LINE, then draw lines directly
			if (render.rendering_type == RenderContext.RENDERING_TYPE_LINE) {
				drawTriangleLines(triangle);
			} else {
				//TODO to be implemented
				
				// if triangle is Ntriangle then calculate the normal of the triangle ?
				
				rasterize(triangle);
			}
		} else {
			// Do not render this triangle
		}
	}
	
	protected void rasterize(Triangle t) {
		
		switch (render.rendering_type) {
		case RenderContext.RENDERING_TYPE_MONOCHROME:
			//TODO To be implemented
			break;
		case RenderContext.RENDERING_TYPE_PLAIN:
			//TODO To be implemented
			break;
		case RenderContext.RENDERING_TYPE_INTERPOLATE:
			//TODO To be implemented
			break;
		default:
			// Invalid rendering type
			break;
		}
	}
	
	protected void drawTriangleLines(Triangle t) {
		//view.drawLine(t.getV1(), t.getV2());
		//view.drawLine(t.getV2(), t.getV3());
		//view.drawLine(t.getV3(), t.getV1());
	}
	
	/**
	 * Is true if all Vertices of the Triangle is within the defined View
	 * 
	 * @param t the Triangle
	 * @return
	 */
	protected boolean isInView(Triangle t) {
		//TODO implementation
		return false;
	}

}
