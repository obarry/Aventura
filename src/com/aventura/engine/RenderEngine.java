package com.aventura.engine;

import java.awt.Color;

import com.aventura.context.GraphicContext;
import com.aventura.context.RenderContext;
import com.aventura.math.transform.Rotation;
import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.Lighting;
import com.aventura.model.world.Cone;
import com.aventura.model.world.Cylinder;
import com.aventura.model.world.Element;
import com.aventura.model.world.Segment;
import com.aventura.model.world.Triangle;
import com.aventura.model.world.Vertex;
import com.aventura.model.world.World;
import com.aventura.tools.tracing.Tracer;
import com.aventura.view.View;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2017 Olivier BARRY
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
 * 
 *                   				   				    	  				          +---------------------+					
 *                	 				  				    	  + - - - - - - - - - - ->|   GraphicContext    |<------+
 *     						                   		    	  | 			          +---------------------+		|
 *															  |										^				|
 *															  |			+---------------------+		|				|
 *     +---------------------+								  +-------->|    RenderContext    |		|				|
 *     |        World        | <------+						  |			+---------------------+		|				|
 *     +---------------------+        |						  |			 		     |				|				|
 *                					  |						  |			+---------------------+		|				|
 *                   				  |						  +-------->|      Rasterizer     |-----+--------+		|
 *                					  |						  |			+---------------------+		         |		|
 *                					  |						  |											     v		|
 *     +---------------------+		  |		+---------------------+										 +---------------------+
 *     |      Lighting       | <------+-----|    RenderEngine     |- - - - - - - - - - - - - - - - - - ->|        View         |
 *     +---------------------+		  |		+---------------------+ 									 +---------------------+
 *                					  				   |	  													    
 *                   				  |				   |	  								
 *                	 				  				   |	  
 *     						          |        		   v		
 *     +---------------------+ 		        +---------------------+
 *     |       Camera        | <------+-----|      ModelView      |
 *	   +---------------------+		    	+---------------------+
 *
 *          	 Model								 Engine						Context(s)							 View
 *			com.aventura.model					com.aventura.engine			com.aventura.context				com.aventura.view
 * 
 * @author Olivier BARRY
 * @since May 2016
 */

public class RenderEngine {
	
	// Context's parameters
	private RenderContext renderContext;
	//private GraphicContext graphicContext;

	// Statistics
	private int nbt = 0; // Number of triangles processed
	private int nbt_in = 0; // Number of triangles finally displayed
	private int nbt_out = 0; // Number of triangles not displayed
	private int nbe = 0; // Number of Elements processed
	// Model
	private World world;
	private Lighting lighting;
	//private Camera camera;
	
	// View
	private View view;
	
	// ModelView modelView
	private ModelView modelView;
	
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
	 * @param lighting the directional lighting the world
	 * @param camera the camera watching the world
	 * @param renderContext the renderContext context containing parameters to renderContext the scene
	 * @param graphicContext the graphicContext context to contain parameters to display the scene
	 */
	public RenderEngine(World world, Lighting lighting, Camera camera, RenderContext render, GraphicContext graphic) {
		this.renderContext = render;
		//this.graphicContext = graphic;
		this.world = world;
		this.lighting = lighting;
		//this.camera = camera;
		
		// Create ModelView matrix with for View (World -> Camera) and Projection (Camera -> Homogeneous) Matrices
		this.modelView = new ModelView(camera.getMatrix(), graphic.getProjectionMatrix());
		
		// Delegate rasterization tasks to a dedicated engine
		this.rasterizer = new Rasterizer(camera, graphic, lighting);
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
	 * - Full ModelView modelView into homogeneous coordinates
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
	 * But this method will also recalculate each time the full ModelView modelView Matrix including the Camera so any change
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
		
		// zBuffer initialization (if applicable)
		if (renderContext.rendering_type != RenderContext.RENDERING_TYPE_LINE) {
			rasterizer.initZBuffer();
		}
		
		// For each element of the world
		for (int i=0; i<world.getElements().size(); i++) {			
			Element e = world.getElement(i);
			render(e, Matrix4.IDENTITY, world.getColor()); // First model Matrix is the IDENTITY Matrix (to allow recursive calls)
		}
		
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "Rendered: "+nbe+" Element(s) and "+nbt+" triangles. Triangles in View Frustum: "+nbt_in+", Out: "+nbt_out);

		// Display the landmarks if enabled (RenderContext)
		if (renderContext.getDisplayLandmark() == RenderContext.DISPLAY_LANDMARK_ENABLED) {
			if (renderContext.getRendering() == RenderContext.RENDERING_TYPE_INTERPOLATE) {
				displayLandMarkLinesInterpolate();							
			} else { // Default
				displayLandMarkLines();			
			}
		}

		// Display the Light vectors if enabled (RenderContext)
		if (renderContext.getDisplayLight() == RenderContext.DISPLAY_LIGHT_VECTORS_ENABLED) {
			displayLight();
		}

		// Switch back and front buffers and request GUI repaint
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
		
		// Update ModelView matrix for this Element (Element <-> Model) by combining the one from this Element
		// with the previous one for recursive calls (initialized to IDENTITY at first call)
		Matrix4 model = matrix.times(e.getTransformation());
		modelView.setModel(model);
		modelView.computeTransformation(); // Compute the whole ModelView modelView matrix including Camera (view)
		
		// TODO NEW TO BE ADDED AND COMPUTED
		// TRANSFORMATION OF ALL VERTICES OF THE ELEMENT
		// PRIOR TO TRIANGLE RENDERING
		// NOW THE TRANSFORMATION IS AT VERTEX LEVEL AND NEEDS TO BE PROCESSED BEFORE RENDERING
		// THIS IS MORE OPTIMAL AS VERTICES BELONGING TO SEVERAL TRIANGLES ARE ONLY PROJECTED ONCE
		// THIS REQUIRES TO IMPLEMENT LIST OF VERTICES AT ELEMENT LEVEL
		
		// Calculate projection for all vertices of this Element
		modelView.transformVertices(e);
				
		// Process each Triangle
		for (int j=0; j<e.getTriangles().size(); j++) {
			
			// Render triangle 
			render(e.getTriangle(j), col, e.getSpecularExp(), e.getSpecularColor());
			
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
	 * Pre-requisite: This assumes that the initialization of ModelView modelView is already done
	 * 
	 * @param to the triangle to render
	 * @param c the color of the Element, can be overridden if color defined (not null) at Triangle level
	 * @param e the specular exponent of the Element
	 * @param sc the specular color of the Element 
	 * @return false if triangle is outside the View Frustum, else true
	 */
	public void render(Triangle t, Color c, float e, Color sc) {
		
		//if (Tracer.function) Tracer.traceFunction(this.getClass(), "Render triangle");
		
		// Priority to lowest level -> if color defined at triangle level, then this overrides the color of above (Element) level 
		Color color = t.getColor();
		if (color == null) color = c;
		
		// Scissor test for the triangle
		// If triangle is totally or partially in the View Frustum
		// Then renderContext its fragments in the View
		if (isInViewFrustum(t)) { // Render triangle
			
			// If triangle normal then transform triangle normal
			if (renderContext.rendering_type != RenderContext.RENDERING_TYPE_INTERPOLATE || t.isTriangleNormal()) {
				// Calculate normal if not calculated
				if (t.getNormal()==null) t.calculateNormal();
				modelView.transformNormal(t);
			}

			switch (renderContext.rendering_type) {
			case RenderContext.RENDERING_TYPE_LINE:
				rasterizer.drawTriangleLines(t, color);
				break;
			case RenderContext.RENDERING_TYPE_MONOCHROME:
				//TODO To be implemented
				//TODO To be renamed into NO_SHADING ?
				// Render faces with only face (or default) color + plain lines to show the faces
				// No shading
				break;
			case RenderContext.RENDERING_TYPE_PLAIN:
				// Draw triangles with shading full face, no interpolation.
				// This forces the mode to be normal at Triangle level even if the normals are at Vertex level
				rasterizer.rasterizePlainTriangle(t, color);
				break;
			case RenderContext.RENDERING_TYPE_INTERPOLATE:
				// Draw triangles with shading and interpolation on the triangle face -> Gouraud's Shading
				rasterizer.rasterizeInterpolatedTriangle(t, color, e, sc);
				break;
			default:
				// Invalid rendering type
				break;
			}

			// If DISPLAY_NORMALS is activated then renderContext normals
			if (renderContext.displayNormals == RenderContext.DISPLAY_NORMALS_ENABLED) {
				displayNormalVectors(t);
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
		if (isInViewFrustum(t.getV1()) || isInViewFrustum(t.getV2()) || isInViewFrustum(t.getV3()))
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
		double x = v.getProjPos().get3DX();
		double y = v.getProjPos().get3DY();
		double z = v.getProjPos().get3DZ();
		
		// Need all (homogeneous) coordinates to be within range [-1, 1]
		if ((x<=1 && x>=-1) && (y<=1 && y>=-1) && (z<=1 && z>=-1))
			return true;
		else
			return false;
	}
	

	public void displayLandMarkLines() {
		// Set the Model Matrix to IDENTITY (no translation)
		modelView.setModel(Matrix4.IDENTITY);
		modelView.computeTransformation();

		// Create Vertices to draw unit segments
		Vertex o = new Vertex(0,0,0);
		Vertex x = new Vertex(1,0,0);
		Vertex y = new Vertex(0,1,0);
		Vertex z = new Vertex(0,0,1);
		modelView.transform(o);
		modelView.transform(x);
		modelView.transform(y);
		modelView.transform(z);
		// Create 3 unit segments
		Segment lx = new Segment(o, x);
		Segment ly = new Segment(o, y);
		Segment lz = new Segment(o, z);
		// Draw segments with different colors (x=RED, y=GREEN, z=BLUE) for mnemotechnic
		rasterizer.drawLine(lx, renderContext.landmarkXColor);
		rasterizer.drawLine(ly, renderContext.landmarkYColor);
		rasterizer.drawLine(lz, renderContext.landmarkZColor);

	}
	
	public void displayLandMarkLinesInterpolate() {
		
		final double arrow_length = 1;
		final double arrow_ray = 0.04;
		final double spear_ray = 0.08;
		final double spear_length = 0.2;
		final int nb_seg =16; 
		
		// X axis arrow
		Rotation r1 = new Rotation(Math.PI/2, Vector4.Y_AXIS);
		Element e1 = new Element();
		Element l1 = new Cylinder(arrow_length, arrow_ray, nb_seg);
		Translation tl1 = new Translation(new Vector3(arrow_length/2, 0, 0));
		l1.setTransformation(tl1.times(r1));
		Element c1 = new Cone(spear_length,spear_ray,nb_seg);
		Translation tc1 = new Translation(new Vector3(arrow_length, 0, 0));
		c1.setTransformation(tc1.times(r1));
		e1.addElement(l1);
		e1.addElement(c1);
		e1.calculateNormals();		
		render(e1, Matrix4.IDENTITY, renderContext.landmarkXColor);
		
		// Y axis arrow
		Rotation r2 = new Rotation(-Math.PI/2, Vector4.X_AXIS);
		Element e2 = new Element();
		Element l2 = new Cylinder(arrow_length, arrow_ray, nb_seg);
		Translation tl2 = new Translation(new Vector3(0, arrow_length/2, 0));
		l2.setTransformation(tl2.times(r2));
		Element c2 = new Cone(spear_length,spear_ray,nb_seg);
		Translation tc2 = new Translation(new Vector3(0, arrow_length, 0));
		c2.setTransformation(tc2.times(r2));
		e2.addElement(l2);
		e2.addElement(c2);
		e2.calculateNormals();		
		render(e2, Matrix4.IDENTITY, renderContext.landmarkYColor);
		
		// Z axis arrow
		Element e3 = new Element();
		Element l3 = new Cylinder(arrow_length, arrow_ray, nb_seg);
		Translation tl3 = new Translation(new Vector3(0, 0, arrow_length/2));
		l3.setTransformation(tl3);
		Element c3 = new Cone(spear_length,spear_ray,nb_seg);
		Translation tc3 = new Translation(new Vector3(0, 0, arrow_length));
		c3.setTransformation(tc3);
		e3.addElement(l3);
		e3.addElement(c3);
		e3.calculateNormals();		
		render(e3, Matrix4.IDENTITY, renderContext.landmarkZColor);
	}
	
	public void displayNormalVectors(Triangle t) {
		// Caution: in this section, we need to take the original triangle containing the normal and other attributes !!!
		
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "Display normals for triangle. Normal of triangle "+t.getNormal());
		
		// Get the 3 vertices from Triangle
		Vertex p1 = t.getV1();
		Vertex p2 = t.getV2();
		Vertex p3 = t.getV3();
		
		if (t.isTriangleNormal()) { // Normal at Triangle level
			if (Tracer.info) Tracer.traceInfo(this.getClass(), "Normal at Triangle level. Normal: "+t.getNormal());
			// Create 3 vertices corresponding to the end point of the 3 normal vectors
			// In this case these vertices are calculated from a single normal vector, the one at Triangle level
			Vertex c = t.getCenter();
			modelView.transform(c);
			Vertex n = new Vertex(c.getPos().plus(t.getNormal()));
			modelView.transform(n);
			if (Tracer.info) Tracer.traceInfo(this.getClass(), "Normal display - Center of triangle"+c);
			if (Tracer.info) Tracer.traceInfo(this.getClass(), "Normal display - Arrow of normal"+n);
			Segment s = new Segment(c, n);
			rasterizer.drawLine(s, renderContext.normalsColor);
			
		} else { // Normals at Vertex level
			Vertex n1, n2, n3;
			if (Tracer.info) Tracer.traceInfo(this.getClass(), "Normal at Vertex level");
			// Create 3 vertices corresponding to the end point of the 3 normal vectors
			n1 = new Vertex(p1.getPos().plus(p1.getNormal()));
			n2 = new Vertex(p2.getPos().plus(p2.getNormal()));
			n3 = new Vertex(p3.getPos().plus(p3.getNormal()));
			modelView.transform(n1);
			modelView.transform(n2);
			modelView.transform(n3);
			
			// Create 3 segments corresponding to normal vectors
			Segment l1 = new Segment(p1, n1);
			Segment l2 = new Segment(p2, n2);
			Segment l3 = new Segment(p3, n3);
			
			// Draw each normal vector starting from their corresponding vertex  
			rasterizer.drawLine(l1, renderContext.normalsColor);
			rasterizer.drawLine(l2, renderContext.normalsColor);
			rasterizer.drawLine(l3, renderContext.normalsColor);
		}
	}
		
	public void displayLight() {
		// Set the Model Matrix to IDENTITY (no translation)
		modelView.setModel(Matrix4.IDENTITY);
		modelView.computeTransformation();
		Vertex v = new Vertex(lighting.getDirectionalLight().getLightVector(null));
		Vertex o = new Vertex(0,0,0);
		modelView.transform(v);
		modelView.transform(o);
		Segment s = new Segment(o, v);
		rasterizer.drawLine(s, renderContext.lightVectorsColor);
	}


}
