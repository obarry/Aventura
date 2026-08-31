package com.aventura.model.light;

import com.aventura.context.PerspectiveContext;
import com.aventura.engine.DepthOnlyConsumer;
import com.aventura.engine.ElementTransform;
import com.aventura.engine.RasterizerStats;
import com.aventura.engine.TriangleRasterizer;
import com.aventura.engine.ViewProjection;
import com.aventura.engine.ZBuffer;
import com.aventura.math.Constants;
import com.aventura.math.projection.Projection;
import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.perspective.Perspective;
import com.aventura.model.world.Element;
import com.aventura.model.world.World;
import com.aventura.model.world.triangle.Triangle;
import com.aventura.tools.tracing.Tracer;
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
 * ShadowingLight is a type of light that can generate Shadows by opposite of other type of Lights e.g. Ambientlight
 * It is also this type of light that can generate "Shaded" light on the surface of World's Elements.
 * The method used for Light calculation is Shadow Mapping hence we will need to have a "Camera Light" that means a Camera corresponding to the Light direction and source
 * For a Directional Light : only a direction, no source (all light rays are parallel in space), the projection should be an Orthographic projection
 * For a Point Light or a Spot Light, a source and a direction is defined. The Camera is located at the source and pointing to the direction of light. A frustum projection
 * is used for the projection.
 * 
 * In this abstract class will be found all the necessary attributes and tools for Shadow generation as the Camera corresponding to the Light
 * the ModelViewProjection projection for this Light (should be Orthographic for a DirectionalLight), the gUIView frustrum and the Shadow Map itself.
 *
 * @author Olivier BARRY
 * @since April 2022
 * 
 */
public abstract class ShadowingLight extends Light {
	
	// Default Shadow Map dimension (Shadow Map is Square)
	public static final int DEFAULT_SHADOW_MAP_DIMENSION = 200;
	
	// Parameter for Shadow Mapping "box" definition (used for Light's camera and perspective calculation)
	public static final int SHADOWING_BOX_WORLD = 1; // Use the World's max dimensions to calculate the Light's view box
	public static final int SHADOWING_BOX_VIEWFRUSTUM = 2; // Use the View Frustum to calculate the "box" for this Light's view - Is DEFAULT
	public static final int SHADOWING_BOX_ELEMENT = 3; // Use any Element's max dimensions to calculate the Light's view box
	public static final int SHADOWING_BOX_SPECIFIC = 4; // Use a specific box to calculate the Light's view box

	//protected int shadowingBox_type = SHADOWING_BOX_VIEWFRUSTUM; // Is Default
	protected int shadowingBox_type = SHADOWING_BOX_WORLD; // Is Default
	
	// Fields related to Shadow generation
	protected Camera camera_light; // The corresponding "camera" from Light View's perspective
	protected PerspectiveContext perspectiveCtx_light; // The perspective from the light to generate the shadow map
	// NOTE: rasterizer_light (an owned Rasterizer instance) was removed as a persistent field.
	// TriangleRasterizer is now created fresh inside generateShadowMap(), together with a fresh
	// ZBuffer for that pass -- see the comment there for why.

	// Split from the former single ModelViewProjection into its two real roles (see their own
	// Javadoc): viewProjection_light for the per-fragment shadowFactorAt() test, elementTransform_light
	// for building each shadow-map triangle's screen position per Element, just like the main camera.
	// Both are constructed once (in initShadowing(), by each ShadowingLight subclass) since
	// view/projection for a light are fixed for its lifetime -- same immutability the legacy
	// ModelViewProjection already had, just made explicit.
	protected ViewProjection viewProjection_light;
	protected ElementTransform elementTransform_light;

	// GUIView Frustum
	//protected Vector4[][] frustum;
	//protected Vector4 frustumCenter;
	
	// World that can cast shadows with that Light, only needed starting ShadowingLight in the class hierarchy
	World world = null;
	
	// Shadow map
	int map_size = 0;
	protected MapView map; // As an attribute of the (Shadowing)Light, there will be multiple maps if multiple lights

	// Diagnostics for shadow map generation: reuses RasterizerStats (see its Javadoc) to give
	// both lifetime totals and "last generation" deltas -- e.g. getShadowMapStats().getTrianglesThisFrame()
	// tells you how many triangles went into the most recent generateShadowMap(World) call, which is a
	// quick way to spot an empty/all-far shadow map (a very common shadow-mapping bug) while the
	// shadow-calculation rework mentioned in the backlog is still pending.
	protected RasterizerStats shadowMapStats = new RasterizerStats();
	private int trianglesThisGeneration = 0; // reset at the start of each generateShadowMap(World) call
	
	// Default constructor
	public ShadowingLight() {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating ShadowingLight without any parameters.");
		// Nothing else to do here, most of the initialization is done by initShadowing, triggered when needed by RenderEngine (only when shadowing is activated)
	}
		
	/**
	 * Default constructor with intensity
	 * @param intensity
	 */
	public ShadowingLight(float intensity) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating ShadowingLight. Intensity : " + intensity);
		this.intensity = intensity;
	}

	/**
	 * Constructor with specification of the ShodowingBox type (see constants)
	 * @param shadowingBox_type
	 */
	public ShadowingLight(int shadowingBox_type) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating ShadowingLight. ShadowingBox type : "+toStringShadowingBoxType(shadowingBox_type));
		this.shadowingBox_type = shadowingBox_type;
	}

	/**
	 * Default constructor with intensity
	 * @param intensity
	 */
	public ShadowingLight(float intensity, int shadowingBox_type) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating ShadowingLight. Intensity : " + intensity + ", ShadowingBox type : "+toStringShadowingBoxType(shadowingBox_type));
		this.intensity = intensity;
		this.shadowingBox_type = shadowingBox_type;
	}


	/**
	 * Constructor + Link to the World : to be used when ShadowingBox is of type SHADOWING_BOX_WORLD
	 * @param shadowingBox_type
	 * @param world the World to be used as shadowing box to calculate the shadow map and its perspective
	 */
	public ShadowingLight(int shadowingBox_type, World world) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating ShadowingLight. ShadowingBox type : "+toStringShadowingBoxType(shadowingBox_type) + " + World");
		this.shadowingBox_type = shadowingBox_type;
		this.world = world;
	}

	/**
	 * Generic constructor with specification of the ShodowingBox type (see constants) and intensity of the Light
	 * @param shadowingBox_type
	 * @param intensity
	 */
	public ShadowingLight(int shadowingBox_type, float intensity) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating ShadowingLight. ShadowingBox type : "+toStringShadowingBoxType(shadowingBox_type) + " Intensity : " + intensity);
		this.shadowingBox_type = shadowingBox_type;
		this.intensity = intensity;
	}
	
	/**
	 * Generic Constructor + Link to the World : to be used when ShadowingBox is of type SHADOWING_BOX_WORLD
	 * @param shadowingBox_type
	 * @param intensity
	 * @param world the World to be used as shadowing box to calculate the shadow map and its perspective
	 */
	public ShadowingLight(int shadowingBox_type, float intensity, World world) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating ShadowingLight. ShadowingBox type : " + toStringShadowingBoxType(shadowingBox_type)+" Intensity : " + intensity + " + World");
		this.shadowingBox_type = shadowingBox_type;
		this.intensity = intensity;
		this.world = world;
	}
	
	public ShadowingLight(float intensity, World world) {
		this.intensity = intensity;
		this.world = world;
	}
	
	public abstract void initShadowing(Perspective perspective, Camera camera_view);
	
	public abstract void initShadowing(Perspective perspective, Camera camera_view, World world);
	
	//public abstract void calculateCameraLight(Perspective perspective, Camera camera_view); 
	
	/**
	 * This method will generate the shadow map for the elements of the world passed in parameter with the camera light previously
	 * initiated and light matrix calculated.
	 * It will use similar recursive algorithm than RenderEngine algorithm for rendering world but will only calculate a shadow map without
	 * any more rendering or rasterization calculation.
	 * @param world
	 */
	public void generateShadowMap(World world) {

		// Fresh ZBuffer + TriangleRasterizer for this generation pass -- rebuilt every time rather
		// than reused across frames, since the shadow map must not carry over stale depth from a
		// previous frame in a scene with moving lights/geometry. This replaces the old
		// rasterizer_light.initZBuffer(...) call (which mutated a persistent Rasterizer instance).
		int half = map_size / 2;
		ZBuffer shadowZBuffer = new ZBuffer(map_size, map_size, half, half, Float.MAX_VALUE);
		this.map = shadowZBuffer.getMapView(); // getMap()/getMap(x,y) keep working exactly as before
		TriangleRasterizer rasterizer = new TriangleRasterizer(perspectiveCtx_light, shadowZBuffer);
		DepthOnlyConsumer consumer = new DepthOnlyConsumer(shadowZBuffer);

		// NOTE: no VP calculation needed here at all anymore -- ViewProjection computes it once,
		// at construction (in initShadowing()), rather than needing an explicit recalculation call
		// per pass (an earlier version of this method still recomputed it per-fragment inside
		// shadowFactorAt(), which was pure waste; this removes even the once-per-pass call).
		trianglesThisGeneration = 0;

		// For each element of the world
		for (int i=0; i<world.getElements().size(); i++) {			
			Element e = world.getElement(i);
			generateShadowMap(e, rasterizer, consumer); // First model Matrix is the IDENTITY Matrix (to allow recursive calls)
		}

		// Diagnostics: recorded as a single batch since TriangleRasterizer's pixel counters
		// accumulate over the whole pass (never reset per-triangle here), unlike the main render
		// pass which resets and reads them triangle by triangle.
		shadowMapStats.recordBatch(trianglesThisGeneration, rasterizer.getRenderedPixels(), rasterizer.getDiscardedPixels());
		shadowMapStats.endFrame();
	}

	public RasterizerStats getShadowMapStats() {
		return shadowMapStats;
	}

	protected void generateShadowMap(Element e, TriangleRasterizer rasterizer, DepthOnlyConsumer consumer) {

		// Single call replaces the legacy setModel()+calculateMVPMatrix() pair -- withNormals=false
		// since shadow map generation never needs normals (matches the legacy behavior, which
		// never called calculateNormalMatrix() for mvp_light either).
		elementTransform_light.setModel(e.getTransformation(), false);

		// Calculate projection for all vertices of this Element
		elementTransform_light.transformElement(e, false); // Calculate prj_pos of each vertex of this Element

		// Process each Triangle (this will update the shadow map's ZBuffer)
		for (int j=0; j<e.getTriangles().size(); j++) {
			Triangle t = e.getTriangle(j);
			// Scissor test: only shadow-map triangles at least partially in the GUIView Frustum
			if (t.isInViewFrustum()) {
				// Depth-only pass: no normal/world-position interpolation is done at all (see
				// TriangleRasterizer's depth-only rasterize() overload) -- convenient, since
				// normals aren't even computed for this triangle during shadow map generation
				// (transformElement(e, false) above deliberately skips that).
				rasterizer.rasterize(t, consumer);
				trianglesThisGeneration++;
			}
		}

		// Do a recursive call for SubElements
		if (!e.isLeaf()) {
			for (int i=0; i<e.getSubElements().size(); i++) {
				generateShadowMap(e.getSubElements().get(i), rasterizer, consumer);
			}
		}
	}

	/**
	 * Returns how lit (1) or shadowed (0) a world-space point is according to this light's shadow
	 * map. This is what replaces the old, buggy vertex-level shadow projection/interpolation dance
	 * (VertexLightParam.vl, interpolated per scan line): TriangleRasterizer already interpolates
	 * Fragment.getWorldPosition() correctly in true world space (Model matrix included), so this
	 * method only needs to apply this light's own View*Projection to it -- no separate Model
	 * matrix handling is needed here, which is what the legacy code was missing.
	 *
	 * @param worldPosition world-space position to test (e.g. Fragment.getWorldPosition())
	 * @return 1.0 if fully lit, 0.0 if in shadow
	 */
	public float shadowFactorAt(Vector4 worldPosition) {

		if (map == null) {
			// No shadow map generated (yet) for this light -- treat as fully lit rather than
			// silently discarding all lighting for every fragment.
			return 1f;
		}

		Vector4 posInLightSpace = viewProjection_light.project(worldPosition);

		// Map is stored in [0, 1] while clip-space coordinates are in [-1, 1].
		float depth = map.getInterpolation((posInLightSpace.getX() + 1) / 2, (posInLightSpace.getY() + 1) / 2);

		// Epsilon bias to avoid "shadow acne" (self-shadowing) -- same value and reasoning as the
		// legacy code.
		if (posInLightSpace.getZ() > depth + 10 * Constants.EPSILON) {
			return 0f;
		}
		return 1f;
	}

	public float getMap(int x, int y) {
		return map.get(x, y);
	}
	
	public MapView getMap() {
		return map;
	}
	
	public String toStringShadowingBoxType(int shadowingBoxType) {

		String shadowingBoxType_string;

		switch (shadowingBoxType) {
		case SHADOWING_BOX_VIEWFRUSTUM:
			shadowingBoxType_string = "SHADOWING_BOX_VIEWFRUSTUM";
			break;
		case SHADOWING_BOX_WORLD:
			shadowingBoxType_string = "SHADOWING_BOX_WORLD";
			break;
		case SHADOWING_BOX_ELEMENT:
			shadowingBoxType_string = "SHADOWING_BOX_ELEMENT";
			break;
		case SHADOWING_BOX_SPECIFIC:
			shadowingBoxType_string = "SHADOWING_BOX_SPECIFIC";
			break;
		default:
			shadowingBoxType_string = "UNKNOWON SHADOWING BOX TYPE";
		}

		return shadowingBoxType_string;
	}

}