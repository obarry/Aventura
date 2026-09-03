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
import com.aventura.model.material.Material;
import com.aventura.model.material.SolidMaterial;
import com.aventura.model.material.TexturedMaterial;
import com.aventura.model.world.Element;
import com.aventura.model.world.Vertex;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Cone;
import com.aventura.model.world.shape.Cylinder;
import com.aventura.model.world.triangle.Triangle;
import com.aventura.tools.tracing.Tracer;
import com.aventura.view.GUIView;
import com.aventura.view.MapView;

/**
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
 *                   				  |						  +-------->|  TriangleRasterizer |-----+--------+		|
 *     +---------------------+		  |						  |			+---------------------+		         |		|
 *     |      Lighting       | <------+						  |											     v		|
 *     +---------------------+		  |		     +---------------------+								+---------------------+
 *                ^                   |----------|    RenderEngine     |- - - - - - - - - - - - - - - ->|        GUIView      |
 *                |          		  |		     +---------------------+ 								+---------------------+
 *                |                   |               |            |
 *     			  |			          |               v            v
 *     +---------------------+ 		  |     +-----------------+  +-------------------+
 *     |       Camera        | <------+-----|  ViewProjection |->|  ElementTransform |
 *	   +---------------------+		    	+-----------------+  +-------------------+
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
	
	// Split from the former single ModelViewProjection class into its two real roles:
	// - viewProjection: pure View*Projection projector, used by ScreenLineRenderer for debug vectors
	// - elementTransform: mutating per-Element pipeline (clip-space position + world normal), used
	//   in the main triangle-processing loop below
	private ViewProjection viewProjection;
	private ElementTransform elementTransform;

	// Former Rasterizer façade, now owned directly by RenderEngine (its sole remaining caller,
	// since ShadowingLight already builds its own TriangleRasterizer/ZBuffer for shadow maps).
	// mainZBuffer/triangleRasterizer are built once, here, and reused every frame -- see
	// initFrameZBuffer() for the per-frame clear() that replaces the old per-frame reallocation.
	private ZBuffer mainZBuffer;
	private TriangleRasterizer triangleRasterizer;
	private RasterizerStats stats = new RasterizerStats();

	// Legacy-parity constants, formerly on the Rasterizer façade -- see their original comments
	// there for why: no separate Ka coefficient existed, and a missing specular color fell back
	// to white rather than propagating null.
	private static final float LEGACY_AMBIENT_REFLECTIVITY = 1f;
	private static final Color DEFAULT_SPECULAR_COLOR = Color.WHITE;

	// Wireframe / debug vector drawing -- constructed once GUIView is known (see setView())
	private ScreenLineRenderer screenLineRenderer;
	
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
				
		// Create the pure View*Projection projector (for debug vectors) and the per-Element
		// mutation pipeline (for the main render loop) -- view/projection are constant for this
		// RenderEngine's whole lifetime (built for a single Camera), computed once, here, and
		// shared between the two (ElementTransform reuses viewProjection's matrix rather than
		// recomputing it).
		this.viewProjection = new ViewProjection(camera, perspectiveCtx.getPerspective());
		this.elementTransform = new ElementTransform(viewProjection);

		// Built once, here, rather than reallocated every frame (see initFrameZBuffer()) --
		// same width/height/init-value the legacy Rasterizer.initZBuffer() (no-arg) used to compute.
		int halfWidth = perspectiveCtx.getPixelHalfWidth();
		int halfHeight = perspectiveCtx.getPixelHalfHeight();
		this.mainZBuffer = new ZBuffer(2 * halfWidth + 1, 2 * halfHeight + 1, halfWidth, halfHeight, perspectiveCtx.getPerspective().getFar());
		this.triangleRasterizer = new TriangleRasterizer(perspectiveCtx, mainZBuffer);
	}
		

	public void setView(GUIView v) {
		gUIView = v;
		screenLineRenderer = new ScreenLineRenderer(perspectiveContext, viewProjection, v);
	}
	
	/**
	 * This method will do the computation. No args. But it now returns (new feature) the zBuffer MapView used for rendering / rasterization.
	 * 
	 * It processes all triangles of the World, Element by Element.
	 * For each Element it takes all Triangles one by one and renderContext them.
	 * - Full transformation (via ElementTransform) into homogeneous coordinates
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
	 * But this method will also recalculate each time the full ElementTransform matrix including the Camera so any change
	 * will be taken into account.
	 * 
	 * @return the zBuffer in form of a MapView that can be easily displayed in GUI.
	 */
	public MapView render() {
		
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "Start rendering...");
		long start_millisec = System.currentTimeMillis();
		nbt = 0;
		nbt_in = 0;
		nbt_out = 0;
		nbt_bf = 0;
		nbe = 0;

		// Recompute the View*Projection matrix from the camera's CURRENT state -- necessary
		// because Camera.updateCamera() mutates the same LookAt/Matrix4 object in place (rather
		// than replacing it), so a cached vp would silently go stale after e.g. a mouse-driven
		// camera move otherwise. Cheap (one matrix multiplication); done once per frame, shared by
		// both elementTransform (main render loop) and screenLineRenderer (debug vectors).
		viewProjection.refresh();
		
		// Geometry calculation : calculate World coordinates for all vertices of the World
		world.worldProject(); // To be done before potential Light's cameras calculation (need full world geometry available to calculate bounding boxes etc.)
		
		// Initialize backbuffer in the GUIView
		gUIView.setBackgroundColor(world.getBackgroundColor());
		gUIView.initView();
		
		// zBuffer clear (if applicable) -- mainZBuffer is built once, in the constructor; only
		// cleared here, every frame, rather than reallocated (as the legacy per-frame
		// rasterizer.initZBuffer() call used to do).
		MapView zBuffer = null;
		if (renderContext.renderingType != RenderContext.RENDERING_TYPE_LINE) {
			mainZBuffer.clear(perspectiveContext.getPerspective().getFar());
			zBuffer = mainZBuffer.getMapView();
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

		// MAIN LOOP : for each element of the world
		for (int i=0; i<world.getElements().size(); i++) {			
			Element e = world.getElement(i);
			//render(e, null, world.getColor()); // First model Matrix is the IDENTITY Matrix (to allow recursive calls)
			render(e, world.getColor()); // First model Matrix is the IDENTITY Matrix (to allow recursive calls)
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

		// Snapshot this frame's diagnostic deltas (RasterizerStats) -- see its Javadoc.
		stats.endFrame();
		
		long end_millisec = System.currentTimeMillis();
		
		long duration_millisec = end_millisec - start_millisec;
		if (Tracer.stats) Tracer.traceStats(this.getClass(), "Rendering duration : " + duration_millisec + " millisec, FPS : " + (float)1000/duration_millisec);			
		
		return zBuffer;
	}
	
	/**
	 * Render a single Element and all its sub-elements recursively
	 * @param e the Element to renderContext
	 * @param matrix, the model matrix, for recursive calls of sub-elements or should be null for root element
	 * @param c (optional, should be null for shading calculation) the color for the various elements to be rendered
	 */
	//public void render(Element e, Matrix4 matrix, Color c) {
	public void render(Element e, Color c) {
		
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "Rendering Element: "+e.getName());			
		// Count Element stats
		nbe++;
		
		// Take color of the element else take super-element color passed in parameters
		Color col = c;
		if (e.getColor() != null) col = e.getColor();
		
		// Update ModelViewProjection matrix for this Element (Element <-> Model) by combining the one from this Element
		// with the previous one for recursive calls (initialized to IDENTITY at first call)
//		Matrix4 model = null;
//		if (matrix == null) {
//			model = e.getTransformation();			
//		} else {
//			model = matrix.times(e.getTransformation());
//		}
		
		// Single call replaces the legacy setModel()+calculateNormalMatrix()+calculateMVPMatrix()
		// trio -- see ElementTransform.setModel()'s Javadoc.
		elementTransform.setModel(e.getTransformation(), true);
		// Then transform the Element with this MVP matrix
		elementTransform.transformElement(e, true); // Calculate projection for all vertices of this Element with normals calculation (and recursively for SubElements)
				
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
				//render(e.getSubElements().get(i), model, col);
				render(e.getSubElements().get(i), col);
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
	 * Pre-requisite: This assumes that the initialization of ElementTransform/ViewProjection is already done
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
				elementTransform.transformNormal(t);
			}
			
			// If RENDERING_TYPE_LINE then no backface culling
			if (renderContext.renderingType == RenderContext.RENDERING_TYPE_LINE) {
				screenLineRenderer.drawTriangleEdges(t, color);
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
						// NOTE: kept exactly as before (interpolate=true, texture forced on
						// unconditionally) for backward compatibility -- despite its name and
						// original comment, this does NOT actually force a single flat normal
						// unless the triangle happens to have isTriangleNormal() set; see
						// RENDERING_TYPE_FLAT below for a mode that genuinely always does.
						rasterizeShadedTriangle(t, color, se, sc, true, true, renderContext.shadowing == RenderContext.SHADOWING_ENABLED);
						break;
					case RenderContext.RENDERING_TYPE_FLAT:
						// Always uses the triangle's single flat normal (interpolate=false),
						// regardless of isTriangleNormal() -- genuine faceted/angular shading.
						// Respects textureProcessing the same way INTERPOLATE does, for consistency.
						rasterizeShadedTriangle(t, color, se, sc, false,
								renderContext.textureProcessing == RenderContext.TEXTURE_PROCESSING_ENABLED,
								renderContext.shadowing == RenderContext.SHADOWING_ENABLED);
						break;
					case RenderContext.RENDERING_TYPE_INTERPOLATE:
						// Draw triangles with shading and interpolation on the triangle face -> Gouraud's Shading
						rasterizeShadedTriangle(t, color, se, sc, true,
								renderContext.textureProcessing == RenderContext.TEXTURE_PROCESSING_ENABLED,
								renderContext.shadowing == RenderContext.SHADOWING_ENABLED);
						break;
					default:
						// Invalid rendering type
						break;
					}

					// Superimpose lines when enabled in the previous modes
					if (renderContext.renderingLines == RenderContext.RENDERING_LINES_ENABLED && renderContext.renderingType != RenderContext.RENDERING_TYPE_LINE) {
						screenLineRenderer.drawTriangleEdges(t, color);				
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
	 * Resolves a Material and the right normals from (color, se, sc, interpolate, texture),
	 * then rasterizes t through the direct pipeline (TriangleRasterizer + ShadingConsumer) --
	 * this is what used to be the Rasterizer façade's rasterizeTriangle(), inlined here since
	 * RenderEngine is now its only caller.
	 *
	 * @param t           the triangle to rasterize
	 * @param color       the surface color (D), tinting the texture sample if textured -- see
	 *                    TexturedMaterial's Javadoc; may be null (SolidMaterial falls back to white)
	 * @param se          specular exponent
	 * @param sc          specular color; falls back to white if null, matching the legacy
	 *                    computeSpecularColor()'s DEFAULT_SPECULAR_COLOR behavior
	 * @param interpolate false forces the triangle's single flat normal regardless of
	 *                    isTriangleNormal(); true interpolates per-vertex normals unless the
	 *                    triangle itself already forces flat via isTriangleNormal()
	 * @param texture     whether to sample t's texture (if it has one) at all
	 * @param shadows     whether shadow-map testing is applied for this triangle's lighting
	 */
	private void rasterizeShadedTriangle(Triangle t, Color color, float se, Color sc, boolean interpolate, boolean texture, boolean shadows) {

		// Resolve which 3 normals to interpolate -- see this method's Javadoc.
		Vector3 normal1, normal2, normal3;
		if (!interpolate || t.isTriangleNormal()) {
			Vector3 flat = t.getWorldNormal();
			normal1 = flat;
			normal2 = flat;
			normal3 = flat;
		} else {
			normal1 = t.getV1().getWorldNormal();
			normal2 = t.getV2().getWorldNormal();
			normal3 = t.getV3().getWorldNormal();
		}

		boolean useTexture = texture && t.getTexture() != null;
		Color effectiveSpecCol = sc != null ? sc : DEFAULT_SPECULAR_COLOR;

		// color (D) is ALWAYS used, even in textured mode -- it tints the texture sample (D*T),
		// never replaced by it. Only SolidMaterial needs a non-null fallback (white) since it has
		// no texture to fall back on if color is null.
		Material material = useTexture
				? new TexturedMaterial(t.getTexture(), t.getTextureOrientation(), color, effectiveSpecCol, se, LEGACY_AMBIENT_REFLECTIVITY)
				: new SolidMaterial(color != null ? color : Color.WHITE, effectiveSpecCol, se, LEGACY_AMBIENT_REFLECTIVITY);

		ShadingConsumer consumer = new ShadingConsumer(material, lighting, camera, mainZBuffer, gUIView, shadows);

		if (useTexture) {
			triangleRasterizer.rasterize(t, normal1, normal2, normal3, t.getTexVec1(), t.getTexVec2(), t.getTexVec3(), consumer);
		} else {
			triangleRasterizer.rasterize(t, normal1, normal2, normal3, consumer);
		}

		stats.recordTriangle(triangleRasterizer.getRenderedPixels(), triangleRasterizer.getDiscardedPixels());
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
					return elementTransform.projectNormal(t).getZ()>0;
				default:
					// Should never happen
					break;
				}
				// Should never happen
				return elementTransform.projectNormal(t).getZ()>0;
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
			return elementTransform.projectNormal(t).getZ()>0;
		}
	}
	

	public void displayLandMarkLines() {
		// Entirely in world space -- no Model matrix involved (drawVector uses View*Projection
		// only), so there is no need to reset the Model matrix to identity anymore.
		Vector4 origin = Vector4.ZERO_POINT;
		screenLineRenderer.drawVector(origin, Vector3.X_AXIS, renderContext.landmarkXColor);
		screenLineRenderer.drawVector(origin, Vector3.Y_AXIS, renderContext.landmarkYColor);
		screenLineRenderer.drawVector(origin, Vector3.Z_AXIS, renderContext.landmarkZColor);
	}
	
	public void displayLandMarkLinesInterpolate() {
		
		final float arrow_length = 1;
		final float arrow_ray = 0.04f;
		final float spear_ray = 0.08f;
		final float spear_length = 0.2f;
		
		// X axis arrow
		Rotation r1 = new Rotation((float)Math.PI/2, Vector4.Y_AXIS);
		Element e1 = createAxisArrow(arrow_length, arrow_ray, spear_length, spear_ray, r1);
		//render(e1, null, renderContext.landmarkXColor);
		e1.transform();
		render(e1, renderContext.landmarkXColor);
		
		// Y axis arrow
		Rotation r2 = new Rotation((float)-Math.PI/2, Vector4.X_AXIS);
		Element e2 = createAxisArrow(arrow_length, arrow_ray, spear_length, spear_ray, r2);	
		//render(e2, null, renderContext.landmarkYColor);
		e2.transform();
		render(e2, renderContext.landmarkYColor);
	
		// Z axis arrow
		Rotation r3 = null;
		try {
			r3 = new Rotation(Matrix4.IDENTITY);
		} catch (NotARotationException e) {
			// Nothing to do - should never happen
			e.printStackTrace();
		}
		Element e3 = createAxisArrow(arrow_length, arrow_ray, spear_length, spear_ray, r3);		
		//render(e3, null, renderContext.landmarkZColor);
		e3.transform();
		render(e3, renderContext.landmarkZColor);

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
		e.build();
		return e;
}
	
	public void displayNormalVectors(Triangle t) {
		// Entirely in world space now (getCenterWorldPos()/getWorldPos() + getWorldNormal()) --
		// the legacy version mixed model-space position (getPos()/getCenter()) with world-space
		// normal (getWorldNormal()) in BOTH branches below, which only happened to look right
		// when an Element's Model matrix was the identity. See the earlier discussion for details.

		if (Tracer.function) Tracer.traceFunction(this.getClass(), "Display normals for triangle. Normal of triangle (null if normal at Vertex level): "+t.getNormal());
		
		if (t.isTriangleNormal()) { // Normal at Triangle level
			if (Tracer.info) Tracer.traceInfo(this.getClass(), "Normal at Triangle level. Normal: "+t.getNormal());
			screenLineRenderer.drawVector(t.getCenterWorldPos(), t.getWorldNormal(), renderContext.normalsColor);

		} else { // Normals at Vertex level

			Vertex p1 = t.getV1();
			Vertex p2 = t.getV2();
			Vertex p3 = t.getV3();
			if (Tracer.info) Tracer.traceInfo(this.getClass(), "Normal at Vertex level. V1 normal: " + p1.getNormal() + " V2 normal: " + p2.getNormal() + " V3 normal: " + p3.getNormal());

			screenLineRenderer.drawVector(p1.getWorldPos(), p1.getWorldNormal(), renderContext.normalsColor);
			screenLineRenderer.drawVector(p2.getWorldPos(), p2.getWorldNormal(), renderContext.normalsColor);
			screenLineRenderer.drawVector(p3.getWorldPos(), p3.getWorldNormal(), renderContext.normalsColor);
		}
	}
		
	public void displayLight() {
		// Entirely in world space now, same simplification as displayLandMarkLines().
		Vector4 origin = Vector4.ZERO_POINT;
		for (int i=0; i<lighting.getDirectionalLights().size(); i++) {
			Vector3 lightDirection = lighting.getDirectionalLights().get(i).getLightVectorAtPoint(null);
			screenLineRenderer.drawVector(origin, lightDirection, renderContext.lightVectorsColor);
		}
	}
	
	public String renderStats() {		
		return "Render Engine - Processed: elements: "+nbe+", triangles: "+nbt+". Triangles: displayed: "+nbt_in+", not displayed: "+nbt_out+", backfacing: "+nbt_bf+"\n"+stats.toString();

	}

	/** Direct access to the Rasterizer's RasterizerStats for individual counters (lifetime totals, this-frame deltas). */
	public RasterizerStats getRasterizerStats() {
		return stats;
	}


}