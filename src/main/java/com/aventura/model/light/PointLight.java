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
import com.aventura.engine.ElementTransform;
import com.aventura.engine.ViewProjection;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.perspective.Perspective;
import com.aventura.model.perspective.PerspectiveType;
import com.aventura.model.world.World;
import com.aventura.tools.tracing.Tracer;
import com.aventura.view.MapView;

/**
 * Point Light source is one that radiates light equally in every direction from a single point in space.
 * 
 * For a Point Light, the intensity is a parameter of the light source and it is a general multiplication factor.
 * Otherwise the intensity of light naturally decreases with distance according to defined law.
 * The most physically representative intensity decrease could be the inverse square law. But for the sake of visibility of the Point light
 * we will use a linear law in this implementation. 
 *
 * @author Olivier BARRY
 * @since July 2016
 * 
 */

public class PointLight extends ShadowingLight {
	
	Vector4 light_point; // The light source
	float max_distance; // The max distance were this light is generating light
	// Below this distance from the light source a "light vector" cannot be normalized reliably
	protected static final float MIN_DISTANCE = 1e-6f;
	// Intensity : for the PointLight, the intensity is a parameter of the light source.

	// ------------------------------------------------------------------
	// Shadows: a cube map (six independent Frustum shadow maps, one per axis-aligned face)
	// ------------------------------------------------------------------
	// A Point Light radiates in every direction, so one single Frustum shadow map (as used by
	// SpotLight, whose light is confined to a cone) cannot cover it: six Frustum maps are built
	// instead, one per face of a cube centered on the light, each with a 90 degree field of view
	// so the six faces exactly tile the full sphere of directions with no gap and no overlap (see
	// initShadowing()). ShadowingLight's generateShadowMapFace()/generateShadowMapElement()/
	// shadowFactorAt(MapView, ...) (extracted in phase 5 from the single-map methods used by
	// DirectionalLight/SpotLight) are reused as-is for each face: PointLight only adds the
	// per-face camera/perspective/view-projection/element-transform/map/bias arrays below and the
	// face-selection logic (selectFace()) that picks, for a given shadowed point, which of the six
	// to sample.

	/** Index into the per-face arrays below, and into FACE_FORWARD: +X, -X, +Y, -Y, +Z, -Z. */
	public static final int FACE_PLUS_X = 0;
	public static final int FACE_MINUS_X = 1;
	public static final int FACE_PLUS_Y = 2;
	public static final int FACE_MINUS_Y = 3;
	public static final int FACE_PLUS_Z = 4;
	public static final int FACE_MINUS_Z = 5;
	private static final int NB_FACES = 6;

	// Forward (viewing) direction of each face's camera, indexed by the FACE_* constants above.
	private static final Vector3[] FACE_FORWARD = {
		Vector3.xAxis(), Vector3.xOppAxis(),
		Vector3.yAxis(), Vector3.yOppAxis(),
		Vector3.zAxis(), Vector3.zOppAxis()
	};

	/**
	 * Default shadow map size, in pixels, of ONE of the six cube faces -- overrides
	 * ShadowingLight.DEFAULT_SHADOW_MAP_SIZE (1000), which is sized for a SINGLE map. A PointLight
	 * needs six, so at the same per-pixel cost 512 keeps the total texel budget (6 x 512 x 512 =~
	 * 1.57M) in the same order of magnitude as one 1000 x 1000 DirectionalLight map (1M), while
	 * still giving each face enough resolution to look sharp at the scale of the demo scenes. This
	 * resolves the audit's open "resolution per cube face" decision (512 px, rather than reusing
	 * 1000 px unchanged and paying six times the memory/generation cost for comparable quality).
	 */
	public static final int DEFAULT_SHADOW_MAP_SIZE = 512;

	protected Camera[] faceCamera;
	protected PerspectiveContext[] facePerspectiveCtx;
	protected ViewProjection[] faceViewProjection;
	protected ElementTransform[] faceElementTransform;
	protected MapView[] faceMap;
	protected float[] faceShadowBiasBase;
	
	public PointLight(Vector4 point, float max) {
		super(); // Intensity is default value (1.0, no multiplication factor)
		this.light_point = point;
		this.max_distance = max;
	}
	
	public PointLight(Vector4 point, float max, float intensity) {
		super(intensity);
		this.light_point = point;
		this.max_distance = max;
	}

	public PointLight(int shadowingBox_type, Vector4 point, float max, float intensity) {
		super(shadowingBox_type, intensity);
		this.light_point = point;
		this.max_distance = max;
	}

	public PointLight(int shadowingBox_type, World world, Vector4 point, float max) {
		super(shadowingBox_type, world); // Intensity is default value (1.0, no multiplication factor)
		this.light_point = point;
		this.max_distance = max;
	}
	
	public PointLight(int shadowingBox_type, World world, float intensity, Vector4 point, float max) {
		super(shadowingBox_type, intensity, world);
		this.light_point = point;
		this.max_distance = max;
	}

	/**
	 * Return the normalized "light vector" at a given point
	 */
	@Override
	public Vector3 getLightVectorAtPoint(Vector4 point) {
		Vector3 light_dir = new Vector3(point, this.light_point);
		float distance = light_dir.length();
		if (distance < MIN_DISTANCE) {
			// The lit point is at the light's own position: there is no direction to speak of and
			// normalizing would give NaN. Return the zero vector: its dot product with any normal is 0,
			// so Lighting reads it as "not lit by this light" instead of propagating NaN into the shading.
			return new Vector3(0, 0, 0);
		}
		return light_dir.normalize();
	}
	
	/**
	 * A PointLight, and so a SpotLight, shows as a glowing halo by default (when the lights are made visible).
	 */
	@Override
	public LightGlowMode getDefaultGlowMode() {
		return LightGlowMode.HALO;
	}

	/** Position of the light source (world space). */
	public Vector4 getPosition() {
		return light_point;
	}

	/**
	 * Repositions this light (e.g. to animate it). Takes effect from the next render()/
	 * generateShadowMap(World) call: like the main camera, this light's shadow camera(s) are rebuilt
	 * from its CURRENT position every frame by initShadowing(), called once per frame by
	 * RenderEngine before generateShadowMap() -- see ShadowingLight's Javadoc.
	 * @param point new position of the light source
	 */
	public void setPosition(Vector4 point) {
		this.light_point = point;
	}
	
	/** Distance beyond which this light does not light anything. */
	public float getMaxDistance() {
		return max_distance;
	}
	
	/**
	 * This is the function implementing the intensity decreasing with distance law
	 * @param distance
	 * @return the attenuation factor
	 */
	protected float attenuationFunc(float distance) {
		// First implementation of attenuation as linear law
		float attenuation = max_distance - distance;
		// Clamp to 0 if negative and return normalized attenuation between [0, 1]
		return attenuation >= 0 ? attenuation/max_distance : 0;
	}

	@Override
	public float getIntensity(Vector4 point) {
		// TODO Auto-generated method stub
		Vector3 light_dir = new Vector3(point, this.light_point);
		float distance = light_dir.length();
		
		return attenuationFunc(distance) * intensity; // The intensity factor is used here
	}

	// getLightColorAtPoint() removed: was a broken stub returning null. Light's default
	// implementation (lightColor * getIntensity(point)) now correctly applies this light's
	// distance attenuation to its color, which this stub never did.

	@Override
	public void setLightVector(Vector3 light) {
		// N/A for a Point Light (no light vector, only a point where the light is located)
		
	}

	@Override
	public void setIntensity(float intensity) {
		this.intensity = intensity;
	}

	/**
	 * Default shadow map size of a PointLight: DEFAULT_SHADOW_MAP_SIZE above (512 px), NOT
	 * ShadowingLight's 1000 px default -- see that constant's Javadoc for why.
	 */
	@Override
	public int getDefaultShadowMapSize() {
		return DEFAULT_SHADOW_MAP_SIZE;
	}

	@Override
	public void initShadowing(Perspective perspective, Camera camera_view, World world) {
		this.world = world;
		initShadowing(perspective, camera_view);
	}

	/**
	 * Builds this Point Light's six cube-face shadow cameras: one per axis direction (+X, -X, +Y,
	 * -Y, +Z, -Z), eye at the light's position, each with a square Frustum of EXACTLY 90 degrees
	 * field of view (halfExtent = near * tan(45 deg) = near, so size = 2*near) -- the angle at which
	 * six such frustums tile the full sphere of directions around the light with no gap and no
	 * overlap (this is what avoids a visible seam at the cube's edges: each world direction belongs
	 * to exactly one face, consistently, see selectFace()). near/far reuse the same formula as
	 * SpotLight.initShadowing(): far = max_distance (PointLight.attenuationFunc() already zeroes
	 * this light's contribution beyond it, so nothing meaningful could be in shadow past it), near a
	 * small fixed-ish fraction of far just to keep the projection well-conditioned. Same robust
	 * up-vector hint as DirectionalLight/SpotLight.initShadowing() (Z axis, falling back to Y for
	 * the two faces whose forward IS Z) so LookAt never collapses side = forward x up to near zero.
	 */
	@Override
	public void initShadowing(Perspective perspective, Camera camera_view) {
		faceCamera = new Camera[NB_FACES];
		facePerspectiveCtx = new PerspectiveContext[NB_FACES];
		faceViewProjection = new ViewProjection[NB_FACES];
		faceElementTransform = new ElementTransform[NB_FACES];
		faceMap = new MapView[NB_FACES];
		faceShadowBiasBase = new float[NB_FACES];

		float far = getMaxDistance();
		float near = Math.max(0.01f, 0.01f * far);
		if (near >= far) {
			near = far * 0.5f;
		}
		// 90 degree FOV (half-angle 45 degrees): halfExtent = near * tan(45) = near.
		float size = 2f * near;

		Vector4 eye = getPosition();
		for (int face = 0; face < NB_FACES; face++) {
			Vector3 forward = FACE_FORWARD[face]; // already a unit vector

			Vector3 upHint = Vector3.zAxis();
			if (Math.abs(forward.dot(Vector3.zAxis())) > 0.999f) {
				upHint = Vector3.yAxis();
			}

			Vector4 poi = eye.plus(forward); // any point further along forward works, LookAt only needs the direction
			faceCamera[face] = new Camera(eye, poi, upHint.V4());

			// Fixed PIXEL resolution regardless of the (tiny, by construction) near-plane window's
			// world-space size -- same reasoning as Directional/SpotLight.initShadowing(); square map
			// since the frustum itself is square (90 degree FOV on both axes).
			facePerspectiveCtx[face] = new PerspectiveContext(getShadowMapSize(), size, size, near, far - near, PerspectiveType.FRUSTUM);
			faceViewProjection[face] = new ViewProjection(faceCamera[face], facePerspectiveCtx[face].getPerspective());
			faceElementTransform[face] = new ElementTransform(faceViewProjection[face]);
		}
	}

	/**
	 * Generates this Point Light's six shadow map faces: reuses ShadowingLight.generateShadowMapFace()
	 * (the parameterized core shared with DirectionalLight/SpotLight -- see its Javadoc) once per
	 * face, EXCEPT faces that facesNeeded() proves cannot see any part of world (see its Javadoc):
	 * those are left/reset to null, which shadowFactorAt() below reads as "nothing to shadow here",
	 * saving the cost of rasterizing an empty map ("saut des faces vides" in the audit's plan).
	 */
	@Override
	public void generateShadowMap(World world) {
		if (facePerspectiveCtx == null) {
			// initShadowing() has not (yet) built the six faces: no shadow map to generate -- same
			// "not ready" convention as ShadowingLight.generateShadowMap(World) for Directional/Spot.
			if (Tracer.info) Tracer.traceInfo(this.getClass(), "No shadow map generated: this light does not support shadowing yet.");
			return;
		}

		boolean[] needed = facesNeeded(world);
		for (int face = 0; face < NB_FACES; face++) {
			if (needed[face]) {
				ShadowMapFace result = generateShadowMapFace(world, faceCamera[face], facePerspectiveCtx[face],
						faceViewProjection[face], faceElementTransform[face]);
				faceMap[face] = result.map;
				faceShadowBiasBase[face] = result.shadowBiasBase;
			} else {
				// No geometry can fall into this face this frame: skip rasterizing it entirely.
				faceMap[face] = null;
			}
		}
		shadowMapStats.endFrame();
	}

	/**
	 * Picks, for each of the six faces, whether ANY point of world's axis-aligned bounding box can
	 * possibly be classified to that face by selectFace()'s rule -- an EXACT test, not an
	 * approximation: since the box's x/y/z extents relative to the light are independent (it is an
	 * axis-aligned box), the question "does some point of the box select this face" reduces to
	 * picking, on the face's own axis, the most favorable (furthest in the face's direction) value,
	 * and on the other two axes, the value closest to zero (least likely to disqualify the face) --
	 * if even that best-case point fails the face's selection test, no point of the box can pass it
	 * either. This also naturally and correctly keeps every face "needed" when the light sits INSIDE
	 * the box (e.g. a lamp in a closed room: the per-axis range then straddles zero on every axis),
	 * without any special case -- which is exactly the plan's validation scenario.
	 */
	private boolean[] facesNeeded(World world) {
		Vector4[] bounds = world.getWorldBounds(); // bounds[0] = min corner, bounds[1] = max corner
		Vector4 lp = getPosition();
		float loX = bounds[0].getX() - lp.getX(), hiX = bounds[1].getX() - lp.getX();
		float loY = bounds[0].getY() - lp.getY(), hiY = bounds[1].getY() - lp.getY();
		float loZ = bounds[0].getZ() - lp.getZ(), hiZ = bounds[1].getZ() - lp.getZ();

		boolean[] needed = new boolean[NB_FACES];
		needed[FACE_PLUS_X]  = hiX >= 0 && hiX >= minAbs(loY, hiY) && hiX >= minAbs(loZ, hiZ);
		needed[FACE_MINUS_X] = loX <= 0 && -loX >= minAbs(loY, hiY) && -loX >= minAbs(loZ, hiZ);
		needed[FACE_PLUS_Y]  = hiY >= 0 && hiY >= minAbs(loX, hiX) && hiY >= minAbs(loZ, hiZ);
		needed[FACE_MINUS_Y] = loY <= 0 && -loY >= minAbs(loX, hiX) && -loY >= minAbs(loZ, hiZ);
		needed[FACE_PLUS_Z]  = hiZ >= 0 && hiZ >= minAbs(loX, hiX) && hiZ >= minAbs(loY, hiY);
		needed[FACE_MINUS_Z] = loZ <= 0 && -loZ >= minAbs(loX, hiX) && -loZ >= minAbs(loY, hiY);
		return needed;
	}

	/** Distance from 0 to the interval [lo, hi]: 0 if the interval straddles 0, else the nearer endpoint. */
	private static float minAbs(float lo, float hi) {
		if (lo <= 0 && hi >= 0) {
			return 0f;
		}
		return Math.min(Math.abs(lo), Math.abs(hi));
	}

	/**
	 * Picks which of the six cube faces a direction (from the light towards a tested point) falls
	 * into: standard cubemap face selection by the largest-magnitude axis component. Ties are broken
	 * deterministically towards X, then Y, then Z (so a direction exactly on an edge or corner of the
	 * cube, e.g. (1,1,1), always resolves to the same single face, on both sides of the tie -- this,
	 * together with the exact 90 degree FOV tiling in initShadowing(), is what avoids a visible seam:
	 * every direction is owned by exactly one face, consistently, never zero or two).
	 * @param lightToPoint direction from the light's position towards the tested point (any length;
	 *                      the zero vector, at the light's own position, deterministically resolves
	 *                      to FACE_PLUS_X, an arbitrary but harmless choice)
	 * @return one of FACE_PLUS_X, FACE_MINUS_X, FACE_PLUS_Y, FACE_MINUS_Y, FACE_PLUS_Z, FACE_MINUS_Z
	 */
	public static int selectFace(Vector3 lightToPoint) {
		float ax = Math.abs(lightToPoint.getX());
		float ay = Math.abs(lightToPoint.getY());
		float az = Math.abs(lightToPoint.getZ());
		if (ax >= ay && ax >= az) {
			return lightToPoint.getX() >= 0 ? FACE_PLUS_X : FACE_MINUS_X;
		}
		if (ay >= az) {
			return lightToPoint.getY() >= 0 ? FACE_PLUS_Y : FACE_MINUS_Y;
		}
		return lightToPoint.getZ() >= 0 ? FACE_PLUS_Z : FACE_MINUS_Z;
	}

	/**
	 * Shadow test for a Point Light: picks the one cube face worldPosition falls into (selectFace())
	 * and samples THAT face's map via ShadowingLight's parameterized shadowFactorAt() -- the same
	 * sampling math already proven for Directional/Spot, just applied to whichever of the six faces
	 * is relevant for this point.
	 */
	@Override
	public float shadowFactorAt(Vector4 worldPosition, Vector3 normal) {
		if (faceMap == null) {
			// No shadow map generated (yet) for this light -- treat as fully lit.
			return 1f;
		}

		Vector3 lightToPoint = new Vector3(getPosition(), worldPosition);
		int face = selectFace(lightToPoint);
		MapView map = faceMap[face];
		if (map == null) {
			// This face was skipped this frame (facesNeeded()): nothing there could cast a shadow.
			return 1f;
		}
		return shadowFactorAt(map, faceViewProjection[face], facePerspectiveCtx[face], faceShadowBiasBase[face], worldPosition, normal);
	}

	/**
	 * @return the shadow map of one of the six cube faces (see the FACE_* constants), or null if
	 *         initShadowing() has not run yet or that face was skipped on the last generateShadowMap()
	 *         (see facesNeeded()).
	 */
	public MapView getFaceMap(int face) {
		return faceMap != null ? faceMap[face] : null;
	}

	@Override
	public int getShadowMapWidth() {
		return facePerspectiveCtx != null ? facePerspectiveCtx[0].getPixelWidth() : 0;
	}

	@Override
	public int getShadowMapHeight() {
		return facePerspectiveCtx != null ? facePerspectiveCtx[0].getPixelHeight() : 0;
	}

}