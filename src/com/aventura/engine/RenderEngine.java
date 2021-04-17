package com.aventura.engine;

import java.awt.Color;

import com.aventura.context.GraphicContext;
import com.aventura.context.RenderContext;
import com.aventura.math.transform.NotARotationException;
import com.aventura.math.transform.Rotation;
import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.Lighting;
import com.aventura.model.shadow.Shadowing;
import com.aventura.model.world.Vertex;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Cone;
import com.aventura.model.world.shape.Cylinder;
import com.aventura.model.world.shape.Element;
import com.aventura.model.world.shape.Segment;
import com.aventura.model.world.triangle.Triangle;
import com.aventura.tools.tracing.Tracer;
import com.aventura.view.View;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2016-2021 Olivier BARRY
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
 * - The 2 API contexts allowing to define all parameters before calling API methods
 * 		* a render context to provide information on how to render the world
 * 		* a graphic context to provide information on how to display the view
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
 *     +---------------------+		  |						  |			+---------------------+		         |		|
 *     |      Lighting       | <------+						  |											     v		|
 *     +---------------------+		  |		+---------------------+										 +---------------------+
 *                ^                   |-----|    RenderEngine     |- - - - - - - - - - - - - - - - - - ->|        View         |
 *                |          		  |		+---------------------+ 									 +---------------------+
 *     +---------------------+		  |	               |
 *     |       Shading       | <------+                |
 *     +---------------------+		  |	               |
 *                                    |                |
 *     						          |        		   v		
 *     +---------------------+ 		  |     +---------------------+
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
	private GraphicContext graphicContext;

	// Statistics
	private int nbt = 0; // Number of triangles processed
	private int nbt_in = 0; // Number of triangles finally displayed
	private int nbt_out = 0; // Number of triangles not displayed
	private int nbt_bf = 0; // Nb of triangles back facing (counted if backface culling is activated)
	private int nbe = 0; // Number of Elements processed
	// Model
	private World world;
	private Lighting lighting;
	private Camera camera;
	private Shadowing shadowing;
	
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
		this.graphicContext = graphic;
		this.world = world;
		this.lighting = lighting;
		this.camera = camera;
		
		// Create the Shading context if it is enabled
		if (renderContext.shading == RenderContext.SHADING_ENABLED) {
			// Build Shading using a reference to Lighting object
			this.shadowing = new Shadowing(graphicContext, lighting, camera);
		} else {
			this.shadowing = null;
		}
		
		// Create ModelView matrix with for View (World -> Camera) and Projection (Camera -> Homogeneous) Matrices
		this.modelView = new ModelView(camera.getMatrix(), graphic.getProjectionMatrix());
		
		// Delegate rasterization tasks to a dedicated engine
		// No shading in this constructor -> null
		this.rasterizer = new Rasterizer(camera, graphic, lighting, shadowing);
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
		nbt_bf = 0;
		nbe = 0;
		
		// Initialize backbuffer in the View
		view.setBackgroundColor(world.getBackgroundColor());
		view.initView();
		
		// zBuffer initialization (if applicable)
		if (renderContext.renderingType != RenderContext.RENDERING_TYPE_LINE) {
			rasterizer.initZBuffer();
		}
		
		// *** UNDER CONSTRUCTION ***
		// Shading initialization and Shadow map(s) calculation
		if (renderContext.shading == RenderContext.SHADING_ENABLED) {
			
			// To calculate the projection matrix (or matrices if several light sources) :
			// - Need to define the bounding box in which the elements will used to calculate the shadow map
			// 		* By default it could be a box containing just the view frustrum of the eye camera
			// 		* But there is a risk that elements outside of this box could generate shadows inside the box
			// 		* A costly solution could be to define a box containing all elements of the scene
			// 		* Otherwise some algorithm could be used for later improvement
			// - Then create the matrix
			// 		* LookAt from light source (View matrix)
			//		* Orthographic projection Matrix
			//		* View * Projection matrix
			//
			// Mat4 viewMatrix = LookAt(lighting.mCameraPosition,
			//							lighting.mCameraPosition + glm::normalize(directionalLight.mLightDirection),
			//							Vec3(0.0f, 1.0f, 0.0f));
			//							
			// Mat4 lightVP = CreateOrthographicMatrix(lighting.mCameraPosition.x - 25.0f, lighting.mCameraPosition.x + 25.0f, 
			//											lighting.mCameraPosition.y - 25.0f, lighting.mCameraPosition.y + 25.0f,
			// 											lighting.mCameraPosition.z + 25.0f, lighting.mCameraPosition.z - 25.0f)
			//					* viewMatrix;
			// Goal is to try to rely on ModelView class for part of the calculation and later use the methods of this class for
			// vertices transformation that will be used before rasterization and generation of the Shadow map
			
			// Initiate the Shading by calculating the light(s) camera/projection matrix(ces)
			shadowing.initShading();
			
			// Generate the shadow map
			shadowing.generateShadowMap(world); // need to recurse on each Element
		}
		// *** END UNDER CONSTRUCTION ***
		
		// For each element of the world
		for (int i=0; i<world.getElements().size(); i++) {			
			Element e = world.getElement(i);
			render(e, null, world.getColor()); // First model Matrix is the IDENTITY Matrix (to allow recursive calls)
		}
		
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "Rendered: "+nbe+" Element(s) and "+nbt+" triangles. Triangles in View Frustum: "+nbt_in+", Out: "+nbt_out+", Back face: "+nbt_bf);

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
	 * @param matrix, the model matrix, for recursive calls of sub-elements or should be null for root element
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
		Matrix4 model = null;
		if (matrix == null) {
			model = e.getTransformation();			
		} else {
			model = matrix.times(e.getTransformation());
		}
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
			render(e.getTriangle(j), col, e.getSpecularExp(), e.getSpecularColor(), e.isClosed());
			
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
	 * @param se the specular exponent of the Element
	 * @param sc the specular color of the Element
	 * @param isClosedElement a boolean to indicate if the Element to which triangle belongs is closed or not (to activate backface culling or not) 
	 * @return false if triangle is outside the View Frustum, else true
	 */
	public void render(Triangle t, Color c, float se, Color sc, boolean isClosedElement) {
		
		//if (Tracer.function) Tracer.traceFunction(this.getClass(), "Render triangle");
		
		// Priority to lowest level -> if color defined at triangle level, then this overrides the color of above (Element) level 
		Color color = t.getColor();
		if (color == null) color = c;
		
		// Back Face Culling if defined in RenderContext AND the Element is Closed
		boolean backfaceCulling = (renderContext.backfaceCulling == RenderContext.BACKFACE_CULLING_ENABLED) && isClosedElement;
		
		// Scissor test for the triangle
		// If triangle is totally or partially in the View Frustum
		// Then renderContext its fragments in the View
		if (isInViewFrustum(t)) { // Render triangle
			
			// If triangle normal then transform triangle normal
			if (renderContext.renderingType != RenderContext.RENDERING_TYPE_INTERPOLATE || t.isTriangleNormal() || backfaceCulling) {
				// Calculate normal if not calculated
				if (t.getNormal()==null) t.calculateNormal();
				modelView.transformNormal(t);
			}
			
			if (backfaceCulling && isBackFace(t)) {

				// Do not renderContext this triangle
				// Count Triangles stats (out view frustum)
				nbt_bf++;
				nbt_out++;
			
			} else { // Default case

				switch (renderContext.renderingType) {
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
					rasterizer.rasterizeTriangle(t, color,0, null, false, false);
					break;
				case RenderContext.RENDERING_TYPE_INTERPOLATE:
					// Draw triangles with shading and interpolation on the triangle face -> Gouraud's Shading
					if (renderContext.textureProcessing == RenderContext.TEXTURE_PROCESSING_ENABLED) {
						rasterizer.rasterizeTriangle(t, color, se, sc, true, true);						
					} else { // No Texture
						rasterizer.rasterizeTriangle(t, color, se, sc, true, false);						
					}
					break;
				default:
					// Invalid rendering type
					break;
				}
				
				if (renderContext.renderingLines == RenderContext.RENDERING_LINES_ENABLED && renderContext.renderingType != RenderContext.RENDERING_TYPE_LINE) {
					rasterizer.drawTriangleLines(t, color);				
				}

				// If DISPLAY_NORMALS is activated then renderContext normals
				if (renderContext.displayNormals == RenderContext.DISPLAY_NORMALS_ENABLED) {
					displayNormalVectors(t);
				}
				// Count Triangles stats (in view)
				nbt_in++;
			}

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
		float x = v.getProjPos().get3DX();
		float y = v.getProjPos().get3DY();
		float z = v.getProjPos().get3DZ();
		
		// Need all (homogeneous) coordinates to be within range [-1, 1]
		if ((x<=1 && x>=-1) && (y<=1 && y>=-1) && (z<=1 && z>=-1))
			return true;
		else
			return false;
	}
	
	/**
	 * Is true if triangle is "back face" with regards to its normal, else false
	 * 
	 * @param t the triangle
	 * @return true if triangle normal is in opposite direction of viewer
	 */
	protected boolean isBackFace(Triangle t) {
		// In homogeneous coordinates, the camera direction is Z axis		
		try {
			
			if (t.isTriangleNormal()) {
				// Take any vertex of the triangle -> same result as a triangle is a plan
				Vector3 ey = t.getV1().getWorldPos().minus(camera.getEye()).V3();
				return t.getWorldNormal().dot(ey)>0;
			} else {
				// return true if the Z coord all vertex normals are > 0 (more precise than triangle normal in order to not exclude triangles having visible vertices (sides)
				return t.getV1().getWorldNormal().dot(t.getV1().getWorldPos().minus(camera.getEye()).V3())>0 && t.getV2().getWorldNormal().dot(t.getV2().getWorldPos().minus(camera.getEye()).V3())>0 && t.getV3().getWorldNormal().dot(t.getV3().getWorldPos().minus(camera.getEye()).V3())>0;				
			}
			
		} catch (Exception e) { // If no Vertex normals, then use Triangle normal with same test
			Vector3 ey = t.getV1().getWorldPos().minus(camera.getEye()).V3();
			return t.getWorldNormal().dot(ey)>0;
		}
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
		
		final float arrow_length = 1;
		final float arrow_ray = 0.04f;
		final float spear_ray = 0.08f;
		final float spear_length = 0.2f;
		
		// X axis arrow
		Rotation r1 = new Rotation((float)Math.PI/2, Vector4.Y_AXIS);
		Element e1 = createAxisArrow(arrow_length, arrow_ray, spear_length, spear_ray, r1);	
		render(e1, null, renderContext.landmarkXColor);
		
		// Y axis arrow
		Rotation r2 = new Rotation((float)-Math.PI/2, Vector4.X_AXIS);
		Element e2 = createAxisArrow(arrow_length, arrow_ray, spear_length, spear_ray, r2);	
		render(e2, null, renderContext.landmarkYColor);
	
		// Z axis arrow
		Rotation r3 = null;
		try {
			r3 = new Rotation(Matrix4.IDENTITY);
		} catch (NotARotationException e) {
			// Nothing to do - should never happen
			e.printStackTrace();
		}
		Element e3 = createAxisArrow(arrow_length, arrow_ray, spear_length, spear_ray, r3);		
		render(e3, null, renderContext.landmarkZColor);

	}
	
	public Element createAxisArrow(float arrow_length, float arrow_ray, float spear_length, float spear_ray, Rotation r) {
		int nb_seg =16; 
		Element e = new Element();
		Element l = new Cylinder(arrow_length, arrow_ray, nb_seg);
		Translation tl = new Translation(new Vector3(0, 0, arrow_length/2));
		l.setTransformation(tl);
		Element c = new Cone(spear_length,spear_ray,nb_seg);
		Translation tc = new Translation(new Vector3(0, 0, arrow_length));
		c.setTransformation(tc);
		e.addElement(l);
		e.addElement(c);
		e.setTransformation(r);
		e.generate();
		return e;
}
	
	public void displayNormalVectors(Triangle t) {
		// Caution: in this section, we need to take the original triangle containing the normal and other attributes !!!
		
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "Display normals for triangle. Normal of triangle "+t.getNormal());
		
		if (t.isTriangleNormal()) { // Normal at Triangle level
			if (Tracer.info) Tracer.traceInfo(this.getClass(), "Normal at Triangle level. Normal: "+t.getNormal());
			
			// Create a vertex corresponding to the barycenter of the triangle
			// In this case the vertices are calculated from a single normal vector, the one at Triangle level
			Vertex c = t.getCenter();
			Vertex n = new Vertex(c.getPos().plus(t.getNormal())); // Before transformation -> using position and normals not yet transformed
			modelView.transform(c);
			modelView.transform(n);
			if (Tracer.info) Tracer.traceInfo(this.getClass(), "Normal display - Center of triangle"+c);
			if (Tracer.info) Tracer.traceInfo(this.getClass(), "Normal display - Arrow of normal"+n);
			Segment s = new Segment(c, n);
			rasterizer.drawLine(s, renderContext.normalsColor);
			
		} else { // Normals at Vertex level
			if (Tracer.info) Tracer.traceInfo(this.getClass(), "Normal at Vertex level");
			
			// Get the 3 vertices from Triangle
			Vertex p1 = t.getV1();
			Vertex p2 = t.getV2();
			Vertex p3 = t.getV3();
			
			// Create 3 vertices corresponding to the end point of the 3 normal vectors
			Vertex n1, n2, n3;
			n1 = new Vertex(p1.getPos().plus(p1.getNormal())); // Before transformation -> using position and normals not yet transformed
			n2 = new Vertex(p2.getPos().plus(p2.getNormal())); // Before transformation -> using position and normals not yet transformed
			n3 = new Vertex(p3.getPos().plus(p3.getNormal())); // Before transformation -> using position and normals not yet transformed
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
	
	public String renderStats() {		
		return "Processed: elements: "+nbe+", triangles: "+nbt+". Triangles: displayed: "+nbt_in+", not displayed: "+nbt_out+", backfacing: "+nbt_bf;

	}


}
