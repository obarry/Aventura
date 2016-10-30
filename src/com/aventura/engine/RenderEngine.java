package com.aventura.engine;

import java.awt.Color;

import com.aventura.context.GraphicContext;
import com.aventura.context.RenderContext;
import com.aventura.math.vector.Matrix4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.Lighting;
import com.aventura.model.world.Element;
import com.aventura.model.world.Line;
import com.aventura.model.world.Triangle;
import com.aventura.model.world.Vertex;
import com.aventura.model.world.World;
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
	private RenderContext render;
	private GraphicContext graphic;

	// Statistics
	private int nbt = 0; // Number of triangles processed
	private int nbt_in = 0; // Number of triangles finally displayed
	private int nbt_out = 0; // Number of triangles not displayed
	private int nbe = 0; // Number of Elements processed
	// Model
	private World world;
	private Lighting light;
	private Camera camera;
	
	// View
	private View view;
	
	// ModelView transformation
	private ModelView transformation;
	
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
	
	public void setView(View v) {
		view = v;
	}
	
	/**
	 * This method will do the computation. No args.
	 * 
	 * It processes all triangles of the World, Element by Element.
	 * For each Element it takes all Triangles one by one and render them.
	 * - Full ModelView transformation into homogeneous coordinates
	 * - Rasterization
	 * It uses the parameters of GraphicContext and RenderContext:
	 * - View information contained into GraphicContext
	 * - Rendering information (e.g. rendering modes etc) contained into RenderContext
	 * 
	 * It assumes initialization is already done through ModelView object and various contexts
	 * - Projection matrix
	 * - Screen and display area
	 * - etc.
	 * 
	 * But this method will also recalculate each time the full ModelView transformation Matrix including the Camera so any change
	 * will be taken into account.
	 * 
	 */
	public void render() {
		
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "Start rendering...");
		nbt = 0;
		nbt_in = 0;
		nbt_out = 0;
		nbe = 0;
		
		view.initView();
		
		// For each element of the world
		for (int i=0; i<world.getElements().size(); i++) {
			Element e = world.getElement(i);
			renderElement(e);
		}
		
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "Rendered: "+nbe+" Element(s) and "+nbt+" triangles. Triangles in View Frustum: "+nbt_in+", Out: "+nbt_out);

		// TODO The landmark display should later be delegated to a dedicated function
		if (graphic.getDisplayLandmark() == GraphicContext.DISPLAY_LANDMARK_ENABLED) {
			
			// Set the Model Matrix to IDENTITY (no translation)
			transformation.setModel(Matrix4.IDENTITY);
			transformation.computeTransformation();
			
			// Create Vertices to draw unit segments
			Vertex o = new Vertex(0,0,0);
			Vertex x = new Vertex(1,0,0);
			Vertex y = new Vertex(0,1,0);
			Vertex z = new Vertex(0,0,1);
			// Create 3 unit segments
			Line line_x = new Line(o, x);
			Line line_y = new Line(o, y);
			Line line_z = new Line(o, z);
			Line lx = transformation.transform(line_x);
			Line ly = transformation.transform(line_y);
			Line lz = transformation.transform(line_z);
			// Draw segments with different colors (x=RED, y=GREEN, z=BLUE) for mnemotechnic
			view.setColor(Color.RED);
			drawLine(lx);
			view.setColor(Color.GREEN);
			drawLine(ly);
			view.setColor(Color.BLUE);
			drawLine(lz);
		}

		view.renderView();
	}
	
	/**
	 * Render a single Element and all its subelements recursively
	 * @param e the Element to render
	 */
	public void renderElement(Element e) {
		
		nbe++;
		
		// Calculate the ModelView matrix for this Element (Element <-> Model)		
		transformation.setModel(e.getTransformationMatrix()); // set the Model Matrix (the one attached to each Element) in the ModelView Transformation
		transformation.computeTransformation(); // Compute the whole ModelView transformation matrix including Camera (view)
				
		// Process each Triangle
		for (int j=0; j<e.getTriangles().size(); j++) {				
			boolean ret = render(e.getTriangle(j));
			nbt++;
			if (ret) nbt_in++; else nbt_out++;
		}
	
		// Do a recursive call for SubElements

		if (!e.isLeaf()) {
			if (Tracer.info) Tracer.traceInfo(this.getClass(), "Element #"+nbe+" has "+e.getSubElements().size()+" sub element(s).");
			for (int i=0; i<e.getSubElements().size(); i++) {
				// Recursive call
				renderElement(e.getSubElements().get(i));
			}
		} else {
			if (Tracer.info) Tracer.traceInfo(this.getClass(), "Element #"+nbe+" has no sub elements.");			
		}
	}
	
	/**
	 * Rasterization of a single Triangle
	 * This assumes that the initialization is already done
	 * @param t the triangle to rasterize
	 * @return false if triangl is outside the View Frustum, else true
	 */
	public boolean render(Triangle t) {
		
		//if (Tracer.function) Tracer.traceFunction(this.getClass(), "Render triangle");
		
		Triangle triangle; // The projected model view triangle in homogeneous coordinates 
		
		// Project this Triangle in the View in homogeneous coordinates
		// This new triangle contains vertices that are transformed
		triangle = transformation.transform(t);
		
		// Scissor test for the triangle
		// If triangle is totally or partially in the View Frustum
		// Then render its fragments in the View
		if (isInViewFrustum(triangle)) {
			// Render triangle
			
			// If the rendering type is LINE, then draw lines directly
			if (render.rendering_type == RenderContext.RENDERING_TYPE_LINE) {
				drawTriangleLines(triangle);
			} else {
				//TODO to be implemented
				
				// if triangle is Ntriangle then calculate the normal of the triangle ?
				
				rasterize(triangle);
			}
			return true;
		} else {
			// Do not render this triangle
			return false;
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
	
	/**
	 * Is true if at least one Vertex of the Triangle is in the View Frustum
	 * 
	 * @param t the Triangle
	 * @return true if triangle is at least partially inside the View Frustum, else false
	 */
	protected boolean isInViewFrustum(Triangle t) {

		// Need at least one vertice to be in the view frustum
		if (isInViewFrustum(t.getV1()) || isInViewFrustum(t.getV1()) || isInViewFrustum(t.getV3()))
			return true;
		else
			return false;
	}
	
	/**
	 * Is true if the Vertex is in the View Frustum
	 * 
	 * @param v the Vertex
	 * @return true if Vertex is inside the View Frustum, else false
	 */
	protected boolean isInViewFrustum(Vertex v) {
		
		// Get homogeneous coordinates of the Vertex
		double x = v.getPosition().get3DX();
		double y = v.getPosition().get3DY();
		double z = v.getPosition().get3DZ();
		
		// Need all (homogeneous) coordinates to be within range [-1, 1]
		if ((x<=1 && x>=-1) && (y<=1 && y>=-1) && (z<=1 && z>=-1))
			return true;
		else
			return false;
	}
	
	// These methods should be later encapsulated in a dedicated class -> e.g. RenderView
	// To Be Encapsulated
	//
	
	protected void drawTriangleLines(Triangle t) {

		//if (Tracer.function) Tracer.traceFunction(this.getClass(), "drawTriangleLines(t)");
		//if (Tracer.info) Tracer.traceInfo(this.getClass(), "Drawing triangle. "+ t);
		
		view.setColor(Color.WHITE);
		drawLine(t.getV1(), t.getV2());
		drawLine(t.getV2(), t.getV3());
		drawLine(t.getV3(), t.getV1());
	}
	
	protected void drawLine(Line l) {
		//if (Tracer.function) Tracer.traceFunction(this.getClass(), "drawLine(l)");
		//if (Tracer.info) Tracer.traceInfo(this.getClass(), "Drawing Line. "+ l);

		drawLine(l.getV1(), l.getV2());
	}
	
	protected void drawLine(Vertex v1, Vertex v2) {

		//if (Tracer.function) Tracer.traceFunction(this.getClass(), "drawLine(v1,v2)");
		
		int x1, y1, x2, y2;
		//if (Tracer.info) Tracer.traceInfo(this.getClass(), "v1.getPosition().getX() : "+ v1.getPosition().get3DX());
		//if (Tracer.info) Tracer.traceInfo(this.getClass(), "v1.getPosition().getY() : "+ v1.getPosition().get3DY());
		//if (Tracer.info) Tracer.traceInfo(this.getClass(), "v2.getPosition().getX() : "+ v2.getPosition().get3DX());
		//if (Tracer.info) Tracer.traceInfo(this.getClass(), "v2.getPosition().getY() : "+ v2.getPosition().get3DY());
		
		x1 = (int)(v1.getPosition().get3DX()*graphic.getPixelWidth()/2);
		y1 = (int)(v1.getPosition().get3DY()*graphic.getPixelHeight()/2);
		x2 = (int)(v2.getPosition().get3DX()*graphic.getPixelWidth()/2);
		y2 = (int)(v2.getPosition().get3DY()*graphic.getPixelHeight()/2);

		view.drawLine(x1, y1, x2, y2);
	}

	//
	// End to be Encapsulated

}
