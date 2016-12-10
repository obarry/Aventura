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
 * It provides the renderContext method
 * It needs to be initialized with proper:
 * - The world information
 * - A camera
 * - Some lighting
 * - some display and graphics (called View)
 * - a render context to provide information on how to renderContext the world
 * - a graphic context to provide information on how to display the view 
 *
 *																		    +---------------------+
 *     +---------------------+								  +------------>|    RenderContext    |		
 *     |        World        | <------+						  |				+---------------------+
 *     +---------------------+        |						  |						   |			
 *                					  |						  |				+---------------------+
 *                   				  |						  +------------>|      Rasterizer     |-----------------+
 *                					  |						  |				+---------------------+				    v
 *     +---------------------+		  |		+---------------------+										 +---------------------+
 *     |      Lighting       | <------+-----|    RenderEngine     |- - - - - - - - - - - - - - - - - - ->|        View         |
 *     +---------------------+		  |		+---------------------+ 									 +---------------------+
 *                					  				   |	  |													    |
 *                   				  |				   |	  |				+---------------------+					|
 *                	 				  				   |	  +------------>|   GraphicContext    |<----------------+
 *     						          |        		   v					+---------------------+
 *     +---------------------+ 		        +---------------------+
 *     |       Camera        | <------+-----|      ModelView      |
 *	   +---------------------+		    	+---------------------+
 *
 *          	 Model								 Engine						Context(s)							 View
 *			com.aventura.model					com.aventura.engine			com.aventura.context				com.aventura.view
 * 
 * @author Bricolage Olivier
 * @since May 2016
 */

public class RenderEngine {
	
	// Context's parameters
	private RenderContext renderContext;
	private GraphicContext graphicContext;

	// Statistics
	private int nbt = 0; // Number of triangles processed
	private int nbt_in = 0; // Number of triangles finally displayed
	private int nbt_out = 0; // Number of triangles not displayed
	private int nbe = 0; // Number of Elements processed
	// Model
	private World world;
	private Lighting light;
	//private Camera camera;
	
	// View
	private View view;
	
	// ModelView transformation
	private ModelView transformation;
	
	// Rasterizer
	private Rasterizer rasterizer;
	
	/**
	 * Create a Rendering Engine with required dependencies and context
	 * There should be a Rendering Engine for a single World, a single (consolidated) Lighting, a single Camera
	 * The parameters for the rendering and the display are respectively passed into the RenderContext and the GraphicContext
	 * 
	 * Rendering a World on different Views e.g. with several Cameras will require multiple RenderEngine instances
	 * 
	 * 
	 * @param world the world to renderContext
	 * @param light the lights lighting the world
	 * @param camera the camera watching the world
	 * @param renderContext the renderContext context containing parameters to renderContext the scene
	 * @param graphicContext the graphicContext context to contain parameters to display the scene
	 */
	public RenderEngine(World world, Lighting light, Camera camera, RenderContext render, GraphicContext graphic) {
		this.renderContext = render;
		this.graphicContext = graphic;
		this.world = world;
		this.light = light;
		//this.camera = camera;
		
		// Create ModelView matrix with for View (World -> Camera) and Projection (Camera -> Homogeneous) Matrices
		this.transformation = new ModelView(camera.getMatrix(), graphic.getProjectionMatrix());
		
		// Delegate rasterization tasks to a dedicated engine
		this.rasterizer = new Rasterizer(graphic, light);
	}
		
	public void setView(View v) {
		view = v;
		rasterizer.setView(v);
	}
	
	/**
	 * This method will do the computation. No args.
	 * 
	 * It processes all triangles of the World, Element by Element.
	 * For each Element it takes all Triangles one by one and renderContext them.
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
		
		// Initialize backbuffer in the View
		view.setBackgroundColor(world.getBackgroundColor());
		view.initView();
		
		// For each element of the world
		for (int i=0; i<world.getElements().size(); i++) {			
			Element e = world.getElement(i);
			render(e, Matrix4.IDENTITY, world.getColor()); // First model Matrix is the IDENTITY Matrix (to allow recursive calls)
		}
		
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "Rendered: "+nbe+" Element(s) and "+nbt+" triangles. Triangles in View Frustum: "+nbt_in+", Out: "+nbt_out);

		// Display the landmarks if enabled (GraphicContext)
		if (renderContext.getDisplayLandmark() == RenderContext.DISPLAY_LANDMARK_ENABLED) {
			displayLandMarkLines();
		}

		view.renderView();
	}
	
	/**
	 * Render a single Element and all its sub-elements recursively
	 * @param e the Element to renderContext
	 * @param matrix, the model matrix, for recursive calls of sub-elements or should be IDENTITY matrix for root element
	 * @param c (optional, should be null for shading calculation) the color for the various elements to be rendered
	 */
	public void render(Element e, Matrix4 matrix, Color c) {
		
		// Count Element stats
		nbe++;
		
		// Take color of the element else take super-element color passed in parameters
		Color col = c;
		if (e.getColor() != null) col = e.getColor();
		
		// zBuffer initialization (if applicable)
		if (renderContext.rendering_type != RenderContext.RENDERING_TYPE_LINE) {
			rasterizer.initZBuffer();
		}

		// Update ModelView matrix for this Element (Element <-> Model) by combining the one from this Element
		// with the previous one for recursive calls (initialized to IDENTITY at first call)
		Matrix4 model = matrix.times(e.getTransformation());
		transformation.setModel(model);
		transformation.computeTransformation(); // Compute the whole ModelView transformation matrix including Camera (view)
				
		// Process each Triangle
		for (int j=0; j<e.getTriangles().size(); j++) {
			
			// Render triangle 
			render(e.getTriangle(j), col);
			
			// Count Triangles stats (total, all triangles whatever in or out view frustum)
			nbt++;
		}
	
		// Do a recursive call for SubElements
		if (!e.isLeaf()) {
			if (Tracer.info) Tracer.traceInfo(this.getClass(), "Element #"+nbe+" has "+e.getSubElements().size()+" sub element(s).");
			for (int i=0; i<e.getSubElements().size(); i++) {
				// Recursive call
				render(e.getSubElements().get(i), model, col);
			}
		} else { // Leaf
			if (Tracer.info) Tracer.traceInfo(this.getClass(), "Element #"+nbe+" has no sub elements.");			
		}
	}
	
	/**
	 * Rendering a single Triangle.
	 * 
	 * This method will calculate transformed triangle (which consists in transforming each vertex) then it delegates
	 * the low level rasterization of the triangle to the Rasterizer, using appropriate methods based on the type of
	 * rendering that is expected (lines, plain faces, interpolation, etc.). 
	 * Pre-requisite: This assumes that the initialization of ModelView transformation is already done
	 * 
	 * @param to the triangle to render
	 * @param c the color of the Element, can be overiden if color defined (not null) at Triangle level
	 * @return false if triangle is outside the View Frustum, else true
	 */
	public void render(Triangle to, Color c) {
		
		//if (Tracer.function) Tracer.traceFunction(this.getClass(), "Render triangle");
		
		// Priority to lowest level -> if color defined at triangle level, then this overrides the color of above (Element) level 
		Color color = to.getColor();
		if (color == null) color = c;
		
		Triangle tf; // The projected model view triangle in homogeneous coordinates 
		
		// Project this Triangle in the View in homogeneous coordinates
		// This new triangle contains vertices that are transformed
		tf = transformation.transform(to);
		
		// Scissor test for the triangle
		// If triangle is totally or partially in the View Frustum
		// Then renderContext its fragments in the View
		if (isInViewFrustum(tf)) { // Render triangle

			switch (renderContext.rendering_type) {
			case RenderContext.RENDERING_TYPE_LINE:
				rasterizer.drawTriangleLines(tf, color);
				break;
			case RenderContext.RENDERING_TYPE_MONOCHROME:
				//TODO To be implemented
				// Render faces with only face (or default) color + plain lines to show the faces
				break;
			case RenderContext.RENDERING_TYPE_PLAIN:
				//TODO To be implemented
				// Draw triangles with shading full face, no interpolation.
				// This forces the mode to be normal at Triangle level even if the normals are at Vertex level
				rasterizer.rasterizeTriangle(tf, color, false);
				break;
			case RenderContext.RENDERING_TYPE_INTERPOLATE:
				rasterizer.rasterizeTriangle(tf, color, true);
				break;
			default:
				// Invalid rendering type
				break;
			}

			// If DISPLAY_NORMALS is activated then renderContext normals
			if (renderContext.displayNormals == RenderContext.DISPLAY_NORMALS_ENABLED) {
				displayNormalVectors(to);
			}
			// Count Triangles stats (in view)
			nbt_in++;

		} else {
			// Do not renderContext this triangle
			// Count Triangles stats (out view frustum)
			nbt_out++;
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
	

	public void displayLandMarkLines() {
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
		rasterizer.drawLine(lx, renderContext.landmarkXColor);
		rasterizer.drawLine(ly, renderContext.landmarkYColor);
		rasterizer.drawLine(lz, renderContext.landmarkZColor);

	}
	
	public void displayNormalVectors(Triangle to) {
		// Caution: in this section, we need to take the original triangle containing the normal and other attributes !!!
		
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "Display normals for triangle. Normal of triangle "+to.getNormal());
		
		// Get the 3 vertices from Triangle
		Vertex p1 = to.getV1();
		Vertex p2 = to.getV2();
		Vertex p3 = to.getV3();
		Vertex n1, n2, n3;
		
		if (to.getNormal() == null) { // Normal at Vertex level
			if (Tracer.info) Tracer.traceInfo(this.getClass(), "Normal at Vertex level");
			// Create 3 vertices corresponding to the end point of the 3 normal vectors
			n1 = new Vertex(p1.getPosition().plus(p1.getNormal()));
			n2 = new Vertex(p2.getPosition().plus(p2.getNormal()));
			n3 = new Vertex(p3.getPosition().plus(p3.getNormal()));
			
			// Create 3 segments corresponding to normal vectors
			Line line1 = new Line(p1, n1);
			Line line2 = new Line(p2, n2);
			Line line3 = new Line(p3, n3);
			// Transform the 3 normals
			Line l1 = transformation.transform(line1);
			Line l2 = transformation.transform(line2);
			Line l3 = transformation.transform(line3);
			
			// Draw each normal vector starting from their corresponding vertex  
			rasterizer.drawLine(l1, renderContext.normalsColor);
			rasterizer.drawLine(l2, renderContext.normalsColor);
			rasterizer.drawLine(l3, renderContext.normalsColor);

			
		} else { // Normals at Triangle level
			if (Tracer.info) Tracer.traceInfo(this.getClass(), "Normal at Triangle level. Normal: "+to.getNormal());
			// Create 3 vertices corresponding to the end point of the 3 normal vectors
			// In this case these vertices are calculated from a single normal vector, the one at Triangle level
			Vertex c = to.getCenter();
			Vertex n = new Vertex(c.getPosition().plus(to.getNormal()));
			Line line = new Line(c, n);
			Line l = transformation.transform(line);
			rasterizer.drawLine(l, renderContext.normalsColor);
//			n1 = new Vertex(p1.getPosition().plus(to.getNormal()));
//			n2 = new Vertex(p2.getPosition().plus(to.getNormal()));
//			n3 = new Vertex(p3.getPosition().plus(to.getNormal()));
		}
		
	}
}
