package com.aventura.engine;

import java.awt.Color;
import java.util.ArrayList;

import com.aventura.context.PerspectiveContext;
import com.aventura.context.RenderContext;
import com.aventura.math.transform.NotARotationException;
import com.aventura.math.transform.Rotation;
import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.Lighting;
import com.aventura.model.light.ShadowingLight;
import com.aventura.model.world.Element;
import com.aventura.model.world.Vertex;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Cone;
import com.aventura.model.world.shape.Cylinder;
import com.aventura.model.world.shape.Segment;
import com.aventura.model.world.triangle.Triangle;
import com.aventura.tools.tracing.Tracer;
import com.aventura.view.GUIView;
import com.aventura.view.MapView;

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
 * 
 * This class is the core rendering engine of the Aventura API
 * Once all is initialized it provides the render() method to render the scene
 * The following needs to be initialized properly before rendering :
 * - A world needs to be built, made of Elements possibly hierarchically, with a transformation (rotation, translation, scaling) link together
 *   or simply positioned separately. Each Element is made of Triangles but several pre-built Elements are provided by the API.
 *   Some Texture can be applied on Elements and Color can be set at different levels (Element, Triangle, etc.), once set, the lowest level primes
 *   (e.g. if color is set at Triangle level, it supersedes the color defined at Element level). Colors and Textures will mix together at rendering time.
 * - A camera positioned in the World to capture the scene
 * - The lighting of the scene made of one or several Light of different types (Directional, Spot or Point light)
 * - The shadowing system to eventually 
 * - The ViewPort, with display and graphics capabilities (called GUIView) that can be adapted to different GUIs (so far only Java SWING is supported)
 * - 2 Contexts allowing to define all parameters before calling API methodsand passed to the API before rendering. These contexts can be pre-built and
 *   allow to render the same World differently e.g. with more or less time-consuming capabilities (texture, shading, shaodwing etc.) or different
 *   Geometry (projection, view frustum, etc.) :
 * 		* a Graphic or Geometry Context to provide information on how to show the world in the gUIView (perspective and projection, frustum, etc.)
 * 		* a Render Context to provide information on how to render the world (Rasterization), including activation/deactivation of shading, shadowing,
 *        textures, etc.
 * 
 * 
 *     +---------------------+		   				    	  				          +---------------------+					
 *     |     Perspective     | <------+-----------------------+ - - - - - - - - - - ->|   PerspectiveContext|<------+
 *     +---------------------+        |        		    	  | 			          +---------------------+		|
 *									  |						  |										^				|
 *									  |						  |			+---------------------+		|				|
 *     +---------------------+		  |						  +-------->|    RenderContext    |		|				|
 *     |        World        | <------+						  |			+---------------------+		|				|
 *     +---------------------+        |						  |			 		     |				|				|
 *                					  |						  |			+---------------------+		|				|
 *                   				  |						  +-------->|      Rasterizer     |-----+--------+		|
 *     +---------------------+		  |						  |			+---------------------+		         |		|
 *     |      Lighting       | <------+						  |											     v		|
 *     +---------------------+		  |		     +---------------------+								+---------------------+
 *                ^                   |----------|    RenderEngine     |- - - - - - - - - - - - - - - ->|        GUIView      |
 *                |          		  |		     +---------------------+ 								+---------------------+
 *                |                   |                     |
 *     			  |			          |        		        v		
 *     +---------------------+ 		  |     +-------------------------------+
 *     |       Camera        | <------+-----|      ModelViewProjection      |
 *	   +---------------------+		    	+-------------------------------+
 *
 *          	 Model								 Engine						Context(s)						 GUIView
 *			com.aventura.model					com.aventura.engine			com.aventura.context			com.aventura.view
 * 
 * @author Olivier BARRY
 * @since May 2016
 */

public class RenderEngine {
	
	// API Contexts
	private RenderContext renderContext;
	private PerspectiveContext perspectiveContext;

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
	
	// GUIView
	private GUIView gUIView;
	
	// ModelViewProjection modelViewProjection
	private ModelViewProjection modelViewProjection;
	
	// Rasterizer
	private Rasterizer rasterizer;
	
	/**
	 * Create a Rendering Engine with required dependencies and context
	 * There should be a Rendering Engine for a single World, a single (consolidated) Lighting, a single Camera
	 * The parameters for the rendering and the display are respectively passed into the RenderContext and the PerspectiveContext
	 * 
	 * Rendering a World on different Views e.g. with several Cameras will require multiple RenderEngine instances
	 * 
	 * 
	 * @param world the World to renderContext
	 * @param lighting the lighting system to illuminate this world
	 * @param camera the Camera watching the world, actually the eye of the viewer
	 * @param renderCtx the RenderContext containing parameters to render the scene
	 * @param perspectiveCtx the PerspectiveContext context to contain parameters to display the scene
	 */
	public RenderEngine(World world, Lighting lighting, Camera camera, RenderContext renderCtx, PerspectiveContext perspectiveCtx) {
		this.renderContext = renderCtx;
		this.perspectiveContext = perspectiveCtx;
		this.world = world;
		this.lighting = lighting;
		this.camera = camera;
				
		// Create ModelViewProjection matrix with for GUIView (World -> Camera) and Projection (Camera -> Homogeneous) Matrices
		this.modelViewProjection = new ModelViewProjection(camera.getMatrix(), perspectiveCtx.getPerspective().getProjection());
		
		// Delegate rasterization tasks to a dedicated engine
		// No shading in this constructor -> null
		this.rasterizer = new Rasterizer(camera, perspectiveCtx, lighting);
		//this.rasterizer = new Rasterizer(camera, perspectiveCtx); // TESTING RASTERIZATION OF SHADOW MAP - TO BE REMOVED
	}
		

	public void setView(GUIView v) {
		gUIView = v;
		rasterizer.setView(v);
	}
	
	/**
	 * This method will do the computation. No args. But it now returns (new feature) the zBuffer MapView used for rendering / rasterization.
	 * 
	 * It processes all triangles of the World, Element by Element.
	 * For each Element it takes all Triangles one by one and renderContext them.
	 * - Full ModelViewProjection modelViewProjection into homogeneous coordinates
	 * - Rasterization
	 * It uses the parameters of PerspectiveContext and RenderContext:
	 * - GUIView information contained into PerspectiveContext
	 * - Rendering information (e.g. rendering modes etc) contained into RenderContext
	 * 
	 * It assumes initialization is already done through ModelViewProjection object and various contexts
	 * - Projection matrix
	 * - Screen and display area
	 * - etc.
	 * 
	 * But this method will also recalculate each time the full ModelViewProjection modelViewProjection Matrix including the Camera so any change
	 * will be taken into account.
	 * 
	 * @return the zBuffer in form of a MapView that can be easily displayed in GUI.
	 */
	public MapView render() {
		
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "Start rendering...");
		nbt = 0;
		nbt_in = 0;
		nbt_out = 0;
		nbt_bf = 0;
		nbe = 0;
		
		// Initialize backbuffer in the GUIView
		gUIView.setBackgroundColor(world.getBackgroundColor());
		gUIView.initView();
		
		// zBuffer initialization (if applicable)
		MapView zBuffer = null;
		if (renderContext.renderingType != RenderContext.RENDERING_TYPE_LINE) {
			zBuffer = rasterizer.initZBuffer();
		}
		
		// Shadowing initialization and Shadow map(s) calculation
		if (renderContext.shadowing == RenderContext.SHADOWING_ENABLED) {
			
			// To calculate the projection matrix (or matrices if several light sources) :
			// - Need to define the bounding box in which the elements will be used to calculate the shadow map
			// 		* By default it could be a box containing just the gUIView frustum of the eye camera
			// 		* But there is a risk that elements outside of this box could generate shadows inside the box
			// 		* A costly solution could be to define a box containing all elements of the scene
			// 		* Otherwise some algorithm could be used for later improvement
			// - Then create the matrix
			// 		* LookAt from light source (GUIView matrix)
			//		* Orthographic projection Matrix
			//		* GUIView * Projection matrix
			//
			// Mat4 viewMatrix = LookAt(lighting.mCameraPosition,
			//							lighting.mCameraPosition + glm::normalize(directionalLight.mLightDirection),
			//							Vec3(0.0f, 1.0f, 0.0f));
			//							
			// Mat4 lightVP = CreateOrthographicMatrix(lighting.mCameraPosition.x - 25.0f, lighting.mCameraPosition.x + 25.0f, 
			//											lighting.mCameraPosition.y - 25.0f, lighting.mCameraPosition.y + 25.0f,
			// 											lighting.mCameraPosition.z + 25.0f, lighting.mCameraPosition.z - 25.0f)
			//					* viewMatrix;
			// Goal is to try to rely on ModelViewProjection class for part of the calculation and later use the methods of this class for
			// vertices transformation that will be used before rasterization and generation of the Shadow map


			if (lighting.hasShadowing()) { // If there are Shadowing lights
				if (Tracer.info) Tracer.traceInfo(this.getClass(), "Rendering: lighting has shadowing - Generating Shadow Maps ***");
				
				ArrayList<ShadowingLight> shadowingLights = lighting.getShadowingLights();
				if (Tracer.info) Tracer.traceInfo(this.getClass(), "Rendering: lighting has shadowing. Number of shadowing lights: "+shadowingLights.size());
				
				for (int i = 0; i < shadowingLights.size(); i++) { // Loop on all Shadowing lights
					if (Tracer.info) Tracer.traceInfo(this.getClass(), "Shadowing Light #" + i + " : "+shadowingLights.get(i));

					// Initiate the Shadowing by calculating the light(s) camera/projection matrix(ces)
					shadowingLights.get(i).initShadowing(perspectiveContext.getPerspective(), camera, world);
					
					// Generate the shadow map
					// TODO optimization : build a world2 containing only the Elements that can cast shadows by using bouncing algorithm then generate shadow map for this world2
					shadowingLights.get(i).generateShadowMap(world); // need to recurse on each Element
				}
				if (Tracer.info) Tracer.traceInfo(this.getClass(), "Rendering: End Generating Shadow Maps ***");
			}
		}

		// For each element of the world
		for (int i=0; i<world.getElements().size(); i++) {			
			Element e = world.getElement(i);
			render(e, null, world.getColor()); // First model Matrix is the IDENTITY Matrix (to allow recursive calls)
		}
		
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "Rendered: "+nbe+" Element(s) and "+nbt+" triangles. Triangles in GUIView Frustum: "+nbt_in+", Out: "+nbt_out+", Back face: "+nbt_bf);

		// Display the landmarks if enabled (RenderContext)
		if (renderContext.getDisplayLandmark() == RenderContext.DISPLAY_LANDMARK_ENABLED) {
			if (renderContext.getRenderingType() == RenderContext.RENDERING_TYPE_INTERPOLATE) {
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
		gUIView.renderView();
		
		return zBuffer;
	}
	
	/**
	 * Render a single Element and all its sub-elements recursively
	 * @param e the Element to renderContext
	 * @param matrix, the model matrix, for recursive calls of sub-elements or should be null for root element
	 * @param c (optional, should be null for shading calculation) the color for the various elements to be rendered
	 */
	public void render(Element e, Matrix4 matrix, Color c) {
		
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "Rendering Element: "+e.getName());			
		// Count Element stats
		nbe++;
		
		// Take color of the element else take super-element color passed in parameters
		Color col = c;
		if (e.getColor() != null) col = e.getColor();
		
		// Update ModelViewProjection matrix for this Element (Element <-> Model) by combining the one from this Element
		// with the previous one for recursive calls (initialized to IDENTITY at first call)
		Matrix4 model = null;
		if (matrix == null) {
			model = e.getTransformation();			
		} else {
			model = matrix.times(e.getTransformation());
		}
		
		modelViewProjection.setModel(model); // Set the Model matrix (Element to World)
		modelViewProjection.calculateNormalMatrix(); // Calculate the Normal matrix
		modelViewProjection.calculateMVPMatrix(); // Compute the whole ModelViewProjection matrix including Model matrix (Element to World transformation)
		// Then transform the Element with this MVP matrix
		modelViewProjection.transformElement(e, true); // Calculate projection for all vertices of this Element with normals calculation (and recursively for SubElements)
				
		// Now all vertices of this Element are "transformed" into Clip coordinates, then process each Triangle
		for (int j=0; j<e.getTriangles().size(); j++) {
			
			// Render triangle 
			render(e.getTriangle(j), col, e.getSpecularExp(), e.getSpecularColor(), e.isClosed());
			
			// Count Triangles stats (total, all triangles whatever in or out gUIView frustum)
			nbt++;
		}
	
		// Do this recursively for all SubElements
		if (!e.isLeaf()) {
			if (Tracer.info) Tracer.traceInfo(this.getClass(), "Element #"+nbe+" has "+e.getSubElements().size()+" sub element(s).");
			for (int i=0; i<e.getSubElements().size(); i++) {
				// Recursive call
				render(e.getSubElements().get(i), model, col);
			}
		} else { // Leaf
			if (Tracer.info) Tracer.traceInfo(this.getClass(), "Element #"+e.getName()+" has no sub elements.");			
		}
	}
	
	/**
	 * Rendering a single Triangle.
	 * 
	 * This method will calculate transformed triangle (which consists in transforming each vertex) then it delegates
	 * the low level rasterization of the triangle to the Rasterizer, using appropriate methods based on the type of
	 * rendering that is expected (lines, plain faces, interpolation, etc.). 
	 * Pre-requisite: This assumes that the initialization of ModelViewProjection modelViewProjection is already done
	 * 
	 * @param to the triangle to render
	 * @param c the color of the Element, can be overridden if color defined (not null) at Triangle level
	 * @param se the specular exponent of the Element
	 * @param sc the specular color of the Element
	 * @param isClosedElement a boolean to indicate if the Element to which triangle belongs is closed or not (to activate backface culling or not) 
	 * @return false if triangle is outside the GUIView Frustum, else true
	 */
	public void render(Triangle t, Color c, float se, Color sc, boolean isClosedElement) {
		
		//if (Tracer.function) Tracer.traceFunction(this.getClass(), "Render triangle");
		
		// Priority to lowest level -> if color defined at triangle level, then this overrides the color of above (Element) level 
		Color color = t.getColor();
		if (color == null) color = c;
		
		// Back Face Culling if defined in RenderContext AND the Element is Closed
		boolean backfaceCulling = (renderContext.backfaceCulling == RenderContext.BACKFACE_CULLING_ENABLED) && isClosedElement;
		
		// Scissor test for the triangle
		// If triangle is totally or partially in the GUIView Frustum
		// Then renderContext its fragments in the GUIView
		if (t.isInViewFrustum()) { // Render triangle
			
			// If triangle normal then transform triangle normal
			if (renderContext.renderingType != RenderContext.RENDERING_TYPE_INTERPOLATE || t.isTriangleNormal() || backfaceCulling) {
				// Calculate normal if not calculated
				if (t.getNormal()==null) t.calculateNormal();
				modelViewProjection.transformNormal(t);
			}
			
			// If RENDERING_TYPE_LINE then no backface culling
			if (renderContext.renderingType == RenderContext.RENDERING_TYPE_LINE) {
				rasterizer.drawTriangleLines(t, color);
				nbt_in++;

			} else {

				// Let's immediately get rid of non visible faces (back faced triangles)
				if (backfaceCulling && isBackFace(t)) {

					// Do not renderContext this triangle
					// Count Triangles stats (out gUIView frustum)
					nbt_bf++;
					nbt_out++;

				} else { // Generic case

					switch (renderContext.renderingType) {
					case RenderContext.RENDERING_TYPE_MONOCHROME:
						//TODO To be implemented
						//TODO To be renamed into NO_SHADING ?
						// Render faces with only face (or default) color + plain lines to show the faces
						// No shading
						break;
					case RenderContext.RENDERING_TYPE_PLAIN:
						// Draw triangles with shading full face, no interpolation.
						// This forces the mode to be normal at Triangle level even if the normals are at Vertex level
						rasterizer.rasterizeTriangle(t, color, se, sc, true, true, renderContext.shadowing == 1 ? true : false, false);
						//rasterizer.rasterizeTriangle(t, color, se, sc, true, true, renderContext.shadowing == 1 ? true : false, true); // TESTING RASTERIZATION OF SHADOW MAP - TO BE REMOVED
						break;
					case RenderContext.RENDERING_TYPE_INTERPOLATE:
						// Draw triangles with shading and interpolation on the triangle face -> Gouraud's Shading
						if (renderContext.textureProcessing == RenderContext.TEXTURE_PROCESSING_ENABLED) {
							rasterizer.rasterizeTriangle(t, color, se, sc, true, true, renderContext.shadowing == 1 ? true : false, false);
							//rasterizer.rasterizeTriangle(t, color, se, sc, true, true, renderContext.shadowing == 1 ? true : false, true); // TESTING RASTERIZATION OF SHADOW MAP - TO BE REMOVED
						} else { // No Texture
							rasterizer.rasterizeTriangle(t, color, se, sc, true, false, renderContext.shadowing == 1 ? true : false, false);
							//rasterizer.rasterizeTriangle(t, color, se, sc, true, false, renderContext.shadowing == 1 ? true : false, true); // TESTING RASTERIZATION OF SHADOW MAP - TO BE REMOVED
						}
						break;
					default:
						// Invalid rendering type
						break;
					}

					// Superimpose lines when enabled in the previous modes
					if (renderContext.renderingLines == RenderContext.RENDERING_LINES_ENABLED && renderContext.renderingType != RenderContext.RENDERING_TYPE_LINE) {
						rasterizer.drawTriangleLines(t, color);				
					}

					// If DISPLAY_NORMALS is activated then renderContext normals
					if (renderContext.displayNormals == RenderContext.DISPLAY_NORMALS_ENABLED) {
						displayNormalVectors(t);
					}
					// Count Triangles stats (in gUIView)
					nbt_in++;
				}
			}

		} else {
			// Do not renderContext this triangle
			// Count Triangles stats (out gUIView frustum)
			nbt_out++;
		}
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
				switch (perspectiveContext.getPerspectiveType()) {
				case PerspectiveContext.PERSPECTIVE_TYPE_FRUSTUM:
					// Take any vertex of the triangle -> same result as a triangle is a plan
					Vector3 ey = t.getV1().getWorldPos().minus(camera.getEye()).V3();
					return t.getWorldNormal().dot(ey)>0;
				case PerspectiveContext.PERSPECTIVE_TYPE_ORTHOGRAPHIC:
					// Need only to test the normal in homogeneous coordinate has a non-null positive Z component (hence pointing behind camera)
					return modelViewProjection.projectNormal(t).getZ()>0;
				default:
					// Should never happen
					break;
				}
				// Should never happen
				return modelViewProjection.projectNormal(t).getZ()>0;
			} else {
				switch (perspectiveContext.getPerspectiveType()) {
				case PerspectiveContext.PERSPECTIVE_TYPE_FRUSTUM:
					// return true if the Z coord all vertex normals are > 0 (more precise than triangle normal in order to not exclude triangles having visible vertices (sides)
					return t.getV1().getWorldNormal().dot(t.getV1().getWorldPos().minus(camera.getEye()).V3())>0 && t.getV2().getWorldNormal().dot(t.getV2().getWorldPos().minus(camera.getEye()).V3())>0 && t.getV3().getWorldNormal().dot(t.getV3().getWorldPos().minus(camera.getEye()).V3())>0;
				case PerspectiveContext.PERSPECTIVE_TYPE_ORTHOGRAPHIC:
					return t.getV1().getProjNormal().getZ() > 0 && t.getV2().getProjNormal().getZ() > 0 && t.getV3().getProjNormal().getZ() > 0;
				default:
					// Should never happen
					break;
				}
				// Should never happen
				return t.getV1().getProjNormal().getZ() > 0 && t.getV2().getProjNormal().getZ() > 0 && t.getV3().getProjNormal().getZ() > 0;				
			}

		} catch (Exception e) { // If no Vertex normals, then use Triangle normal with same test
			//Vector3 ey = t.getV1().getWorldPos().minus(camera.getEye()).V3();
			//return t.getWorldNormal().dot(ey)>0;
			return modelViewProjection.projectNormal(t).getZ()>0;
		}
	}
	

	public void displayLandMarkLines() {
		// Set the Model Matrix to IDENTITY (no translation)
		modelViewProjection.setModel(Matrix4.IDENTITY);
		modelViewProjection.calculateNormalMatrix();
		modelViewProjection.calculateMVPMatrix();

		// Create Vertices to draw unit segments
		Vertex o = new Vertex(0,0,0);
		Vertex x = new Vertex(1,0,0);
		Vertex y = new Vertex(0,1,0);
		Vertex z = new Vertex(0,0,1);
		modelViewProjection.transformVertex(o, true);
		modelViewProjection.transformVertex(x, true);
		modelViewProjection.transformVertex(y, true);
		modelViewProjection.transformVertex(z, true);
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
		
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "Display normals for triangle. Normal of triangle (null if normal at Vertex level): "+t.getNormal());
		
		if (t.isTriangleNormal()) { // Normal at Triangle level
			if (Tracer.info) Tracer.traceInfo(this.getClass(), "Normal at Triangle level. Normal: "+t.getNormal());
			
			// Create a vertex corresponding to the barycenter of the triangle
			// In this case the vertices are calculated from a single normal vector, the one at Triangle level
			Vertex c = t.getCenter();
			//Vertex n = new Vertex(c.getPos().plus(t.getNormal())); // Before transformation -> using position and normals not yet transformed
			Vertex n = new Vertex(c.getPos().plus(t.getWorldNormal())); // Before transformation -> using position and normals not yet transformed
			modelViewProjection.transformVertex(c, true);
			modelViewProjection.transformVertex(n, true);
			if (Tracer.info) Tracer.traceInfo(this.getClass(), "Normal display - Center of triangle"+c);
			if (Tracer.info) Tracer.traceInfo(this.getClass(), "Normal display - Arrow of normal"+n);
			Segment s = new Segment(c, n);
			rasterizer.drawLine(s, renderContext.normalsColor);
			
		} else { // Normals at Vertex level
			
			// Get the 3 vertices from Triangle
			Vertex p1 = t.getV1();
			Vertex p2 = t.getV2();
			Vertex p3 = t.getV3();
			if (Tracer.info) Tracer.traceInfo(this.getClass(), "Normal at Vertex level. V1 normal: " + p1.getNormal() + " V2 normal: " + p2.getNormal() + " V3 normal: " + p3.getNormal());
			
			// Create 3 vertices corresponding to the end point of the 3 normal vectors
			Vertex n1, n2, n3;
//			n1 = new Vertex(p1.getPos().plus(p1.getNormal())); // Before transformation -> using position and normals not yet transformed
//			n2 = new Vertex(p2.getPos().plus(p2.getNormal())); // Before transformation -> using position and normals not yet transformed
//			n3 = new Vertex(p3.getPos().plus(p3.getNormal())); // Before transformation -> using position and normals not yet transformed
			n1 = new Vertex(p1.getPos().plus(p1.getWorldNormal())); // Before transformation -> using position and normals not yet transformed
			n2 = new Vertex(p2.getPos().plus(p2.getWorldNormal())); // Before transformation -> using position and normals not yet transformed
			n3 = new Vertex(p3.getPos().plus(p3.getWorldNormal())); // Before transformation -> using position and normals not yet transformed
			modelViewProjection.transformVertex(n1, true);
			modelViewProjection.transformVertex(n2, true);
			modelViewProjection.transformVertex(n3, true);
			
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
		modelViewProjection.setModel(Matrix4.IDENTITY);
		modelViewProjection.calculateNormalMatrix();
		modelViewProjection.calculateMVPMatrix();
		Vertex v = new Vertex(lighting.getDirectionalLight().getLightVectorAtPoint(null));
		Vertex o = new Vertex(0,0,0);
		modelViewProjection.transformVertex(v, true);
		modelViewProjection.transformVertex(o, true);
		Segment s = new Segment(o, v);
		rasterizer.drawLine(s, renderContext.lightVectorsColor);
	}
	
	public String renderStats() {		
		return "Render Engine - Processed: elements: "+nbe+", triangles: "+nbt+". Triangles: displayed: "+nbt_in+", not displayed: "+nbt_out+", backfacing: "+nbt_bf+"\n"+rasterizer.renderStats();

	}


}
