/*
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
 */
package com.aventura.model.light;

import com.aventura.context.PerspectiveContext;
import com.aventura.engine.DepthOnlyConsumer;
import com.aventura.engine.ElementTransform;
import com.aventura.engine.NearPlaneClipper;
import com.aventura.engine.RasterizerStats;
import com.aventura.engine.TriangleRasterizer;
import com.aventura.engine.ViewProjection;
import com.aventura.engine.ZBuffer;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.perspective.Perspective;
import com.aventura.model.perspective.PerspectiveType;
import com.aventura.model.world.Element;
import com.aventura.model.world.World;
import com.aventura.model.world.triangle.Triangle;
import com.aventura.tools.tracing.Tracer;
import com.aventura.view.MapView;


/**
 * ShadowingLight is a type of light that can generate Shadows by opposite of other type of Lights e.g. Ambientlight
 * It is also this type of light that can generate "Shaded" light on the surface of World's Elements.
 * The method used for Light calculation is Shadow Mapping hence we will need to have a "Camera Light" that means a Camera corresponding to the Light direction and source
 * For a Directional Light : only a direction, no source (all light rays are parallel in space), the projection should be an Orthographic projection
 * For a Point Light or a Spot Light, a source and a direction is defined. The Camera is located at the source and pointing to the direction of light. A frustum projection
 * is used for the projection.
 * 
 * In this abstract class will be found all the necessary attributes and tools for Shadow generation as the Camera corresponding to the Light
 * the ModelViewProjection projection for this Light (should be Orthographic for a DirectionalLight), the view frustum and the Shadow Map itself.
 *
 * @author Olivier BARRY
 * @since April 2022
 * 
 */
public abstract class ShadowingLight extends Light {
	
	/**
	 * Default shadow map size, in pixels, of the LONGEST side of the map (see setShadowMapSize()).
	 * This is the default of the ShadowingLight types that do not declare their own: a subclass
	 * needing another default declares its own DEFAULT_SHADOW_MAP_SIZE constant and overrides
	 * getDefaultShadowMapSize() (e.g. planned for PointLight, whose cube map will need 6 maps).
	 * The resolution is a fixed number of pixels, independent of the light box's world-space
	 * extent (a fixed ppu would give huge maps for big scenes and tiny ones for small scenes).
	 */
	public static final int DEFAULT_SHADOW_MAP_SIZE = 1000;
	
	/** Smallest accepted shadow map size (pixels). */
	public static final int MIN_SHADOW_MAP_SIZE = 2;
	
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
	// No persistent rasterizer: a TriangleRasterizer is created fresh inside generateShadowMap(),
	// together with a fresh ZBuffer for that pass -- see the comment there for why.

	// Split from the former single ModelViewProjection into its two real roles (see their own
	// Javadoc): viewProjection_light for the per-fragment shadowFactorAt() test, elementTransform_light
	// for building each shadow-map triangle's screen position per Element, just like the main camera.
	// Both are constructed once (in initShadowing(), by each ShadowingLight subclass) since
	// view/projection for a light are fixed for its lifetime -- same immutability the legacy
	// ModelViewProjection already had, just made explicit.
	protected ViewProjection viewProjection_light;
	protected ElementTransform elementTransform_light;

	// View Frustum
	//protected Vector4[][] frustum;
	//protected Vector4 frustumCenter;
	
	// World that can cast shadows with that Light, only needed starting ShadowingLight in the class hierarchy
	World world = null;
	
	// Shadow map size requested through setShadowMapSize(), 0 = use getDefaultShadowMapSize()
	private int shadowMapSize = 0;
	// Filtering of the shadow map lookup, HARD (0 or 1) unless set through setShadowFilter()
	private ShadowFilter shadowFilter = ShadowFilter.HARD;
	protected MapView map; // As an attribute of the (Shadowing)Light, there will be multiple maps if multiple lights

	// Diagnostics for shadow map generation: reuses RasterizerStats (see its Javadoc) to give
	// both lifetime totals and "last generation" deltas -- e.g. getShadowMapStats().getTrianglesThisFrame()
	// tells you how many triangles went into the most recent generateShadowMap(World) call, which is a
	// quick way to spot an empty/all-far shadow map (a very common shadow-mapping bug) while the
	// shadow-calculation rework mentioned in the backlog is still pending.
	protected RasterizerStats shadowMapStats = new RasterizerStats();

	// Anti-acne depth bias, expressed as a WORLD-space distance (a physically meaningful depth
	// tolerance, independent of how tight or loose this light's box happens to be) rather than a
	// fixed slice of the [0,1] NDC depth range. A fixed NDC epsilon (the legacy "10*EPSILON")
	// implicitly means a much SMALLER world-space bias for a tight-fit box than for a loose one
	// (world bias = ndcEpsilon * (far-near)) -- which is exactly what turned invisible once the
	// box calculation in DirectionalLight.initShadowing() started producing a tight-fit depth
	// range instead of the old, much deeper, arbitrary one: the same "10*EPSILON" that used to be
	// enough stopped being enough, producing self-shadowing acne on grazing-angle surfaces (e.g.
	// this class's flat Trellis floor under a near-horizontal DirectionalLight).
	protected static final float SHADOW_BIAS_WORLD = 0.02f;

	// Slope scaling: the depth-quantization error a fixed world bias needs to absorb grows
	// roughly as 1/dotNL as the surface tilts away from facing the light (dotNL -> 0 at a
	// grazing angle) -- a bias tuned for a well-lit surface (dotNL close to 1) can still be too
	// small for a much more grazing one, even one that doesn't look extreme (e.g. dotNL ~ 0.4,
	// ~66 degrees off the normal, already needed noticeably more bias than dotNL ~ 0.67 in
	// testing). shadowFactorAt() now divides the base bias by max(dotNL, DOT_NL_FLOOR) instead of
	// using one fixed value for every angle. DOT_NL_FLOOR caps this scaling at near-grazing
	// incidence (dotNL -> 0 would otherwise blow the bias up to infinity); hardcoded for now, see
	// backlog note about grouping this with SHADOW_BIAS_WORLD and other hardcoded tuning
	// constants once we revisit making them configurable.
	protected static final float DOT_NL_FLOOR = 0.1f;

	// Soft shadows: below this determinant (in texels), the receiving surface is seen too edge-on from the
	// light to measure its depth gradient (see shadowFactorAt()), and a wider bias is used instead
	protected static final float MIN_PLANE_DETERMINANT = 0.05f;

	// Soft shadows: the correction of the depth of a texel by the plane of the receiving surface is limited to this multiple
	// of the bias. Beyond the edge of a small surface (sill, ledge), the plane no longer exists: its extension would make
	// the neighbor texels look like occluders and the lit surface speckled.
	protected static final float PCF_MAX_PLANE_OFFSET = 2f;

	// Minimum base bias as a fraction of the world size of one shadow map texel (see generateShadowMap())
	protected static final float SHADOW_BIAS_TEXEL_FACTOR = 0.5f;

	// Base bias, BEFORE the per-fragment slope scaling above is applied, in whichever unit this
	// light's shadow map stores its depth in: for an orthographic light (DirectionalLight), the
	// map stores a [0,1]-normalized depth, so this is SHADOW_BIAS_WORLD converted using this
	// light's CURRENT (far-near) depth range; for a frustum light (Spot/Point, once their shadow
	// maps exist), the map stores the LINEAR view-space depth W directly (world units already),
	// so this stays a plain world-space distance -- see generateShadowMap()'s frustum branch.
	// (Re)computed once per generateShadowMap(World) call (perspectiveCtx_light is only valid
	// after initShadowing() has run), then reused -- with a different slope factor each time --
	// by every shadowFactorAt() call for that frame.
	protected float shadowBiasBase = 0f;
	
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
	 * Generates the shadow map(s) for this light against the given world. For a light with a
	 * single shadow camera (DirectionalLight, SpotLight) this fills in the single
	 * camera_light/perspectiveCtx_light/viewProjection_light/elementTransform_light/map/
	 * shadowBiasBase fields below, via generateShadowMapFace() -- the parameterized core this
	 * method and PointLight's six-face override both call (see that method's Javadoc; the
	 * extraction is phase 5's refactor, with no behavior change for Directional/Spot).
	 * @param world
	 */
	public void generateShadowMap(World world) {
		generateSingleShadowMap(world);
	}

	/**
	 * The actual single-map generation body (DirectionalLight, SpotLight): package-visible via
	 * "final" rather than inlined directly into generateShadowMap(World) above, so that a
	 * ShadowingLight subclass whose OWN superclass is not ShadowingLight directly -- namely
	 * SpotLight, whose superclass PointLight overrides generateShadowMap(World) for its own
	 * six-face cube map -- can still reach this exact, unmodified single-map behavior by calling
	 * this method from its own override, instead of inheriting PointLight's cube-map one (see
	 * SpotLight.generateShadowMap(World) for why that override is necessary). "final" because no
	 * subclass has a reason to further customize the single-map algorithm itself -- only whether to
	 * use it at all (PointLight does not).
	 */
	protected final void generateSingleShadowMap(World world) {

		// A light whose initShadowing() does not (yet) set up its light camera/perspective/projection
		// has no shadow map to generate: leave the map null, which shadowFactorAt() reads as "fully lit".
		// (This is the case of PointLight and SpotLight until their shadow maps are implemented; before
		// this guard, asking for shadows with such a light raised a NullPointerException below.)
		if (perspectiveCtx_light == null || viewProjection_light == null || elementTransform_light == null) {
			if (Tracer.info) Tracer.traceInfo(this.getClass(), "No shadow map generated: this light does not support shadowing yet.");
			this.map = null;
			return;
		}

		ShadowMapFace face = generateShadowMapFace(world, camera_light, perspectiveCtx_light, viewProjection_light, elementTransform_light);
		this.map = face.map;
		this.shadowBiasBase = face.shadowBiasBase;

		// Diagnostics: each triangle was recorded by generateShadowMapElement(); snapshot this
		// generation's deltas.
		shadowMapStats.endFrame();
	}

	public RasterizerStats getShadowMapStats() {
		return shadowMapStats;
	}

	/**
	 * Result of generating one shadow map "face": the filled-in depth map and the base anti-acne
	 * bias to use when sampling it (see shadowBiasBase's Javadoc). Introduced in phase 5
	 * (PointLight's six-face cube map) so that generateShadowMapFace() can hand back a fresh
	 * map+bias pair without writing into this class's single camera_light/.../shadowBiasBase
	 * fields -- those stay exactly as before for DirectionalLight/SpotLight (see
	 * generateShadowMap(World) above), while PointLight keeps its own six-element arrays instead.
	 */
	protected static final class ShadowMapFace {
		protected final MapView map;
		protected final float shadowBiasBase;
		protected ShadowMapFace(MapView map, float shadowBiasBase) {
			this.map = map;
			this.shadowBiasBase = shadowBiasBase;
		}
	}

	/**
	 * Generates ONE shadow map by rasterizing world's elements, depth-only, from the given
	 * camera/perspective/view-projection/element-transform -- the parameterized core extracted
	 * from the former (single-map) generateShadowMap(World) body, so that:
	 * - generateShadowMap(World) above calls it once, with this light's own single
	 *   camera_light/perspectiveCtx_light/viewProjection_light/elementTransform_light (Directional, Spot);
	 * - PointLight.generateShadowMap(World) calls it up to six times, once per cube face, with
	 *   that face's own camera/perspective/view-projection/element-transform.
	 * Behavior, bias formula and diagnostics (shadowMapStats) are exactly the ones already proven
	 * in phases 2-4 -- only the source of the camera/perspective/etc. moved from "this light's
	 * fields" to explicit parameters. Does NOT call shadowMapStats.endFrame(): the caller does
	 * that once, after every face it needs has been generated (so a six-face PointLight reports
	 * one frame's totals across all of its faces, not six separate "frames").
	 */
	protected ShadowMapFace generateShadowMapFace(World world, Camera camera, PerspectiveContext perspectiveCtx,
			ViewProjection viewProjection, ElementTransform elementTransform) {

		// Fresh ZBuffer + TriangleRasterizer for this generation pass -- rebuilt every time rather
		// than reused across frames, since the shadow map must not carry over stale depth from a
		// previous frame in a scene with moving lights/geometry.
		// Map size from this face's perspective context: width x height pixels, possibly
		// rectangular (see setShadowMapSize()). The ZBuffer covers the centered screen space
		// [-half, half] on each axis, hence 2*half+1 cells (same convention as the main pass in
		// RenderEngine) -- the former width = 2*half allocation made the last column and row
		// (x = +half, y = +half, accepted by TriangleRasterizer) fall out of the buffer.
		int halfWidth = perspectiveCtx.getPixelHalfWidth();
		int halfHeight = perspectiveCtx.getPixelHalfHeight();
		Perspective lightPerspective = perspectiveCtx.getPerspective();
		boolean frustum = perspectiveCtx.getPerspectiveType() == PerspectiveType.FRUSTUM;
		// Orthographic projections in this engine always normalize NDC depth to [0, 1] (see
		// OrthographicProjection's matrix -- z_ndc = 0 at near, 1 at far, regardless of the actual
		// near/far world-space values chosen). A small margin above 1.0 guards against a fragment
		// exactly at the box's far corner (z_ndc mathematically == 1.0) failing the depth test
		// (z <= stored) purely from floating-point rounding.
		// Frustum projections (Spot, Point) store the LINEAR view-space depth W instead of a
		// normalized NDC z (see shadowFactorAt() below, and the pipeline-generalization table in
		// the audit doc): "far" is then simply this face's far plane distance, with the same
		// small relative margin.
		final float SHADOW_MAP_FAR_VALUE = frustum ? lightPerspective.getFar() * (1.0f + 1e-4f) : 1.0f + 1e-4f;
		ZBuffer shadowZBuffer = new ZBuffer(2 * halfWidth + 1, 2 * halfHeight + 1, halfWidth, halfHeight, SHADOW_MAP_FAR_VALUE);
		MapView faceMap = shadowZBuffer.getMapView(); // getMap()/getMap(x,y) keep working exactly as before
		TriangleRasterizer rasterizer = new TriangleRasterizer(perspectiveCtx, shadowZBuffer);
		DepthOnlyConsumer consumer = new DepthOnlyConsumer(shadowZBuffer);

		// See SHADOW_BIAS_WORLD's Javadoc: converts the fixed world-space bias into this frame's
		// depth units. Orthographic stores a [0,1]-normalized depth, so dividing by the (far-near)
		// depth range turns the world-space bias into that same normalized unit. Frustum stores the
		// linear W directly (already world units), so the bias is used as-is -- see
		// shadowBiasBase's Javadoc. This is the BASE bias only -- shadowFactorAt() further scales it
		// per-fragment by the surface's slope relative to this light (see DOT_NL_FLOOR's Javadoc).
		// The base world bias also grows with the size of a texel (world size covered by one shadow
		// map pixel): with a coarse map the depth of a tilted surface varies within one texel by
		// more than a fixed bias, which produces acne. lightPerspective.getWidth()/getHeight() are
		// the NEAR-plane extent for a frustum, so this measures the texel at its smallest (nearest
		// the light); a texel covers proportionally more world space further away, which
		// shadowFactorAt() does not compensate for.
		float texelWorldSize = Math.max(lightPerspective.getWidth() / perspectiveCtx.getPixelWidth(),
				lightPerspective.getHeight() / perspectiveCtx.getPixelHeight());
		float worldBias = Math.max(SHADOW_BIAS_WORLD, SHADOW_BIAS_TEXEL_FACTOR * texelWorldSize);
		float faceBiasBase;
		if (frustum) {
			faceBiasBase = worldBias;
		} else {
			float depthRange = lightPerspective.getDepth();
			faceBiasBase = depthRange > 0 ? worldBias / depthRange : worldBias;
		}

		// Recompute this face's View*Projection from its camera's CURRENT state, in case the light
		// itself moved since the last generation (same reasoning as RenderEngine.render()'s
		// viewProjection.refresh() call -- see ViewProjection's Javadoc). Cheap; done once per
		// generation regardless of whether this light actually moved.
		viewProjection.refresh();

		// For each element of the world
		for (int i=0; i<world.getElements().size(); i++) {
			Element e = world.getElement(i);
			generateShadowMapElement(e, rasterizer, consumer, elementTransform, perspectiveCtx); // First model Matrix is the IDENTITY Matrix (to allow recursive calls)
		}

		return new ShadowMapFace(faceMap, faceBiasBase);
	}

	/**
	 * Legacy single-map entry point: generates this light's single shadow map Element pass using
	 * its own camera_light/perspectiveCtx_light/elementTransform_light. Kept as a thin wrapper
	 * (phase 5) over the parameterized generateShadowMapElement() below, for any external caller
	 * relying on this exact signature (none in this codebase currently, but this is a protected
	 * method of a public API).
	 * @param e
	 * @param rasterizer
	 * @param consumer
	 */
	protected void generateShadowMap(Element e, TriangleRasterizer rasterizer, DepthOnlyConsumer consumer) {
		generateShadowMapElement(e, rasterizer, consumer, elementTransform_light, perspectiveCtx_light);
	}

	/**
	 * Parameterized recursive Element pass for ONE shadow map face: transforms e (and its
	 * sub-Elements) through elementTransform, clips/rasterizes each in-frustum Triangle depth-only
	 * into rasterizer/consumer. Extracted (phase 5) from the former generateShadowMap(Element, ...)
	 * so both the single-map path (via the thin wrapper above) and PointLight's six-face path can
	 * share it, each with their own elementTransform/perspectiveCtx -- see generateShadowMapFace().
	 */
	protected void generateShadowMapElement(Element e, TriangleRasterizer rasterizer, DepthOnlyConsumer consumer,
			ElementTransform elementTransform, PerspectiveContext perspectiveCtx) {

		// Single call replaces the legacy setModel()+calculateMVPMatrix() pair -- withNormals=false
		// since shadow map generation never needs normals (matches the legacy behavior, which
		// never called calculateNormalMatrix() for mvp_light either).
		elementTransform.setModel(e.getTransformation(), false);

		// Calculate projection for all vertices of this Element
		elementTransform.transformElement(e, false); // Calculate prj_pos of each vertex of this Element

		// Near-plane clipping (phase 3): only meaningful for a Frustum projection -- see
		// NearPlaneClipper's class Javadoc for why Orthographic (w always 1) never needs it. No-op
		// for DirectionalLight today (always Orthographic), exercised for Spot (phase 4) and
		// PointLight's six Frustum faces (phase 5).
		boolean frustum = perspectiveCtx.getPerspectiveType() == PerspectiveType.FRUSTUM;
		float near = frustum ? perspectiveCtx.getPerspective().getNear() : 0f;

		// Process each Triangle (this will update the shadow map's ZBuffer)
		for (int j=0; j<e.getTriangles().size(); j++) {
			Triangle t = e.getTriangle(j);
			// Scissor test: only shadow-map triangles at least partially in the View Frustum
			if (t.isInViewFrustum()) {
				// Depth-only pass: no normal/world-position interpolation is done at all (see
				// TriangleRasterizer's depth-only rasterize() overload) -- convenient, since
				// normals aren't even computed for this triangle during shadow map generation
				// (transformElement(e, false) above deliberately skips that).
				// Counters reset per triangle so that recordTriangle() gets THIS triangle's counts
				// (same pattern as the main render pass in RenderEngine).
				rasterizer.resetStats();
				if (frustum) {
					for (NearPlaneClipper.ClippedTriangle ct : NearPlaneClipper.clip(
							t.getV1(), t.getV2(), t.getV3(), null, null, null, null, null, null, near)) {
						rasterizer.rasterize(ct.v1, ct.v2, ct.v3, null, null, null, null, null, null, consumer);
					}
				} else {
					rasterizer.rasterize(t, consumer);
				}
				shadowMapStats.recordTriangle(rasterizer.getRasterizedLines(), rasterizer.getRenderedPixels(), rasterizer.getDiscardedPixels());
			}
		}

		// Do a recursive call for SubElements
		if (!e.isLeaf()) {
			for (int i=0; i<e.getSubElements().size(); i++) {
				generateShadowMapElement(e.getSubElements().get(i), rasterizer, consumer, elementTransform, perspectiveCtx);
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
	 * @param normal        world-space surface normal at worldPosition (e.g. Fragment.getNormal()),
	 *                      used to slope-scale the anti-acne bias -- see DOT_NL_FLOOR's Javadoc.
	 * @return 1.0 if fully lit, 0.0 if in shadow, and, with a soft shadow filter (see
	 *         setShadowFilter()), any value in between at the edge of a shadow
	 */
	public float shadowFactorAt(Vector4 worldPosition, Vector3 normal) {
		return singleShadowFactorAt(worldPosition, normal);
	}

	/**
	 * The actual single-map sampling body (DirectionalLight, SpotLight) -- see
	 * generateSingleShadowMap()'s Javadoc for why this needs to be its own, non-overridden ("final")
	 * method rather than inlined into shadowFactorAt(Vector4, Vector3) above: SpotLight's own
	 * override calls this directly, to use ShadowingLight's single-map behavior instead of
	 * inheriting PointLight's six-face one.
	 */
	protected final float singleShadowFactorAt(Vector4 worldPosition, Vector3 normal) {

		if (map == null) {
			// No shadow map generated (yet) for this light -- treat as fully lit rather than
			// silently discarding all lighting for every fragment.
			return 1f;
		}

		return shadowFactorAt(map, viewProjection_light, perspectiveCtx_light, shadowBiasBase, worldPosition, normal);
	}

	/**
	 * Parameterized core of shadowFactorAt(Vector4, Vector3): samples the given map (as generated
	 * by generateShadowMapFace() from the given viewProjection/perspectiveCtx/shadowBiasBase) at
	 * worldPosition. Extracted (phase 5) so PointLight.shadowFactorAt() can reuse it once it has
	 * picked, via its own face-selection logic, which of its six faces to sample -- see
	 * PointLight.selectFace(). map is assumed non-null: callers check that first (see the
	 * single-map overload above, and PointLight.shadowFactorAt()).
	 */
	protected float shadowFactorAt(MapView map, ViewProjection viewProjection, PerspectiveContext perspectiveCtx,
			float shadowBiasBase, Vector4 worldPosition, Vector3 normal) {

		Vector4 posInLightSpace = viewProjection.project(worldPosition);

		// Orthographic: w is always 1, so x/y/z are already the final coordinates -- no divide
		// needed, and z is the stored (normalized) depth. Frustum: x and y need the perspective
		// divide to land in [-1, 1]; the depth compared against the map is w itself (linear
		// view-space depth), NOT z/w (which would be the non-linear NDC depth) -- see the
		// pipeline-generalization table in the audit doc.
		boolean frustum = perspectiveCtx.getPerspectiveType() == PerspectiveType.FRUSTUM;
		float w = posInLightSpace.getW();
		float xNdc = frustum ? posInLightSpace.getX() / w : posInLightSpace.getX();
		float yNdc = frustum ? posInLightSpace.getY() / w : posInLightSpace.getY();
		float depthValue = frustum ? w : posInLightSpace.getZ();

		// Bounds test: a point projecting outside this light's box/frustum is not something this
		// light (or, for PointLight, this face) can shadow -- treat it as unshadowed by THIS light
		// rather than sampling a repeated edge texel (MapView.getInterpolation() clamps s/t, which
		// would silently borrow whatever depth happens to sit on the map's border). Harmless for
		// DirectionalLight (its orthographic box already encloses the whole scene it was built
		// from). Load-bearing for Spot (its frustum is tighter than the scene) and for PointLight:
		// selectFace() already guarantees worldPosition falls within the chosen face's 90-degree
		// frustum (modulo floating-point at the exact seam -- see selectFace()'s Javadoc), so this
		// branch is not expected to trigger there either, but stays as the same safety net.
		if (xNdc < -1f || xNdc > 1f || yNdc < -1f || yNdc > 1f) {
			return 1f;
		}

		// Sample the map where the rasterizer wrote this position. TriangleRasterizer follows the
		// pixel-center convention: centered pixel x holds the depth sampled exactly at the
		// continuous position x = x_ndc * halfWidth, and is stored in buffer cell k = x + halfWidth
		// (see ZBuffer). So continuous position p = x_ndc * halfWidth + halfWidth falls exactly on
		// cell index p, for any sign of x. getInterpolation() takes normalized coordinates s with
		// index = s * width - 0.5, hence s = (p + 0.5) / width. Written per axis since the map may
		// be rectangular (see generateShadowMapFace()).
		int halfWidth = perspectiveCtx.getPixelHalfWidth();
		int halfHeight = perspectiveCtx.getPixelHalfHeight();
		float s = (xNdc * halfWidth + halfWidth + 0.5f) / map.getViewWidth();
		float t = (yNdc * halfHeight + halfHeight + 0.5f) / map.getViewHeight();
		float depth = map.getInterpolation(s, t);

		// Slope-scaled epsilon bias to avoid "shadow acne" (self-shadowing) -- see
		// DOT_NL_FLOOR's Javadoc for why a single fixed bias (shadowBiasBase alone) isn't
		// enough across every incidence angle.
		float dotNL = getLightVectorAtPoint(worldPosition).dot(normal.normalize());
		float shadowBias = shadowBiasBase / Math.max(dotNL, DOT_NL_FLOOR);

		int radius = shadowFilter.getRadius();
		if (radius == 0) {
			if (depthValue > depth + shadowBias) {
				return 0f;
			}
			return 1f;
		}

		// Percentage-closer filtering (PCF): the fraction of the texels around the sampled position that
		// see the point lit. Only the RESULTS of the depth comparisons are averaged: each texel is
		// compared on its own stored depth (no interpolation of depths), and a comparison result is
		// weighted bilinearly like a depth would be, so that the factor changes smoothly, not by steps
		// of one texel, when the point moves across the map.
		//
		// Written as the sum, over the (2r+1)^2 taps at whole-texel offsets from the sampled position,
		// of one bilinear lookup of comparison results (the 4 texels around the tap); grouped by texel,
		// this is a weight wx(cx) * wy(cy) on each texel of a (2r+2)^2 block, with wx = 1 - fu on the
		// first column, fu on the last, and 1 on the others (fu: fractional part of the position), the
		// total weight being (2r+1)^2.
		//
		// A texel of the block is up to r + 1 texels away from the point, where a tilted surface is
		// deeper (or less deep) than at the point. Comparing the depth of the point with those texels
		// would then make a tilted surface shadow itself (acne). Multiplying the bias to hide it would
		// detach the shadows of the small reliefs (window sills, cornices) from their casters, since
		// the bias would exceed their thickness. Instead, the depth expected at each texel is that of
		// the plane of the receiving surface (receiver plane depth bias): the depth of the point plus
		// the depth gradient of the plane, per texel of the map, times the offset to that texel. The
		// bias of the single test (shadowBias) is then enough, as for hard shadows.
		float u = s * map.getViewWidth() - 0.5f;
		float v = t * map.getViewHeight() - 0.5f;
		float gradX = 0f, gradY = 0f; // depth change per texel along the map axes
		float pcfBias = shadowBias;
		Vector3 n = normal.normalize();
		Vector3 helper = Math.abs(n.getZ()) < 0.9f ? Vector3.zAxis() : Vector3.xAxis();
		Vector3 t1 = n.cross(helper).normalize();
		Vector3 t2 = n.cross(t1).normalize();
		// A step of about one texel along each tangent of the plane, in world units at the depth of the point
		Perspective lightPerspective = perspectiveCtx.getPerspective();
		float texelWorld = Math.max(lightPerspective.getWidth() / perspectiveCtx.getPixelWidth(),
				lightPerspective.getHeight() / perspectiveCtx.getPixelHeight());
		float step = frustum ? texelWorld * w / lightPerspective.getNear() : texelWorld;
		Vector4 p1 = posInLightSpace1(viewProjection, worldPosition, t1, step);
		Vector4 p2 = posInLightSpace1(viewProjection, worldPosition, t2, step);
		if (!frustum || (p1.getW() > 0f && p2.getW() > 0f)) {
			float tx1 = ((frustum ? p1.getX() / p1.getW() : p1.getX()) - xNdc) * halfWidth;
			float ty1 = ((frustum ? p1.getY() / p1.getW() : p1.getY()) - yNdc) * halfHeight;
			float tx2 = ((frustum ? p2.getX() / p2.getW() : p2.getX()) - xNdc) * halfWidth;
			float ty2 = ((frustum ? p2.getY() / p2.getW() : p2.getY()) - yNdc) * halfHeight;
			float dd1 = (frustum ? p1.getW() : p1.getZ()) - depthValue;
			float dd2 = (frustum ? p2.getW() : p2.getZ()) - depthValue;
			float det = tx1 * ty2 - ty1 * tx2;
			if (Math.abs(det) > MIN_PLANE_DETERMINANT) {
				gradX = (dd1 * ty2 - ty1 * dd2) / det;
				gradY = (tx1 * dd2 - dd1 * tx2) / det;
			} else {
				// A surface seen edge-on from the light: its depth gradient cannot be measured, the
				// wide bias of the previous version is the fallback
				pcfBias = shadowBias * 2 * (radius + 1);
			}
		}
		float fu = u - (float) Math.floor(u);
		float fv = v - (float) Math.floor(v);
		int x0 = (int) Math.floor(u);
		int y0 = (int) Math.floor(v);
		int maxX = map.getViewWidth() - 1;
		int maxY = map.getViewHeight() - 1;
		float litWeight = 0f;
		for (int j = -radius; j <= radius + 1; j++) {
			float wy = (j == -radius) ? 1f - fv : (j == radius + 1 ? fv : 1f);
			int cy = Math.min(Math.max(y0 + j, 0), maxY);
			for (int i = -radius; i <= radius + 1; i++) {
				float wx = (i == -radius) ? 1f - fu : (i == radius + 1 ? fu : 1f);
				int cx = Math.min(Math.max(x0 + i, 0), maxX);
				// Depth expected at this texel if the surface continues as a plane (receiver plane depth bias)
				float offset = gradX * (x0 + i - u) + gradY * (y0 + j - v);
				float maxOffset = PCF_MAX_PLANE_OFFSET * shadowBias;
				float expected = depthValue + (offset > maxOffset ? maxOffset : (offset < -maxOffset ? -maxOffset : offset));
				if (expected <= map.get(cx, cy) + pcfBias) {
					litWeight += wx * wy;
				}
			}
		}
		int taps = (2 * radius + 1) * (2 * radius + 1);
		return Math.min(litWeight / taps, 1f);
	}

	/** Light-space position (see ViewProjection.project()) of the world point moved by step along the direction */
	private static Vector4 posInLightSpace1(ViewProjection viewProjection, Vector4 worldPosition, Vector3 direction, float step) {
		return viewProjection.project(new Vector4(worldPosition.getX() + direction.getX() * step, worldPosition.getY() + direction.getY() * step,
				worldPosition.getZ() + direction.getZ() * step, 1f));
	}

	// ------------------------------------------------------------------
	// Shadow filtering
	// ------------------------------------------------------------------

	/**
	 * Sets how the shadow map of this light is filtered (see ShadowFilter): HARD (the default) gives
	 * sharp shadow edges, PCF_3X3 a penumbra two or three texels wide, at the cost of sixteen texel reads per lookup instead of one bilinear one.
	 * Taken into account from the next rendered frame, and valid for every kind of light, including
	 * the six faces of a PointLight.
	 *
	 * @param filter the filter, not null
	 * @throws IllegalArgumentException if filter is null
	 */
	public void setShadowFilter(ShadowFilter filter) {
		if (filter == null) {
			throw new IllegalArgumentException("Shadow filter must not be null");
		}
		this.shadowFilter = filter;
	}

	/**
	 * @return the filter of this light's shadow map, ShadowFilter.HARD unless set otherwise
	 */
	public ShadowFilter getShadowFilter() {
		return shadowFilter;
	}

	// ------------------------------------------------------------------
	// Shadow map size
	// ------------------------------------------------------------------

	/**
	 * Default shadow map size of this type of light: DEFAULT_SHADOW_MAP_SIZE (1000 pixels) unless
	 * the light class declares its own default (check this method or the class Javadoc).
	 * @return the default size, in pixels, of the longest side of the shadow map
	 */
	public int getDefaultShadowMapSize() {
		return DEFAULT_SHADOW_MAP_SIZE;
	}

	/**
	 * Sets the resolution of this light's shadow map: size is the number of pixels of its LONGEST
	 * side. The other side is derived from the proportions of the area the light has to cover
	 * (recomputed at each frame), so the map is rectangular when that area is: no pixel is wasted
	 * and the texel density is the same on both axes. Higher values give sharper shadows at the cost
	 * of memory (width x height floats) and shadow map generation time.
	 * Taken into account from the next rendered frame.
	 *
	 * @param size number of pixels of the longest side, at least MIN_SHADOW_MAP_SIZE
	 * @throws IllegalArgumentException if size < MIN_SHADOW_MAP_SIZE
	 */
	public void setShadowMapSize(int size) {
		if (size < MIN_SHADOW_MAP_SIZE) {
			throw new IllegalArgumentException("Shadow map size must be at least " + MIN_SHADOW_MAP_SIZE + " pixels: " + size);
		}
		this.shadowMapSize = size;
	}

	/**
	 * Goes back to the default shadow map size of this type of light (see getDefaultShadowMapSize()).
	 */
	public void resetShadowMapSize() {
		this.shadowMapSize = 0;
	}

	/**
	 * @return the shadow map size in use: the one set by setShadowMapSize(), or the default of this
	 *         type of light (getDefaultShadowMapSize()) if none was set
	 */
	public int getShadowMapSize() {
		return shadowMapSize > 0 ? shadowMapSize : getDefaultShadowMapSize();
	}

	/**
	 * @return the pixel width of the current shadow map (as computed by the last initShadowing()), 0 if none yet
	 */
	public int getShadowMapWidth() {
		return perspectiveCtx_light != null ? perspectiveCtx_light.getPixelWidth() : 0;
	}

	/**
	 * @return the pixel height of the current shadow map (as computed by the last initShadowing()), 0 if none yet
	 */
	public int getShadowMapHeight() {
		return perspectiveCtx_light != null ? perspectiveCtx_light.getPixelHeight() : 0;
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