package com.aventura.model.light;

import com.aventura.context.PerspectiveContext;
import com.aventura.engine.ElementTransform;
import com.aventura.engine.ViewProjection;
import com.aventura.math.tools.BoundingBox4;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.perspective.Perspective;
import com.aventura.model.world.World;
import com.aventura.tools.tracing.Tracer;

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
 * Directional Light also known as an infinite light source, radiates light in a single direction
 * from infinitely far away e.g. sun, whose rays can be considered parallel.
 * Since they have no position in space, directional directional have infinite range and the intensity
 * of light they radiate does not diminish over distance.
 *
 * @author Olivier BARRY
 * @since July 2016
 * 
 */

public class DirectionalLight extends ShadowingLight {
	
	//protected Vector3 direction;
	protected Vector3 light_vector; // = -direction
	
	/**
	 * Create Directional Light using direction as vector of the light, with default intensity.
	 *
	 * CHANGED BEHAVIOR: this used to derive intensity from the norm of the direction vector
	 * (super(direction.length())) -- a fragile, implicit API: normalizing your vector before
	 * passing it here would silently zero out the light's intensity with no error. Intensity is
	 * now always Light.DEFAULT_LIGHT_INTENSITY here; use the (direction, intensity) constructor
	 * below for explicit control. If existing scene-building code relied on passing a
	 * non-unit-length vector specifically to encode brightness, it needs to move to that
	 * constructor instead.
	 *
	 * @param direction is where the light comes from (vector from viewer to light source)
	 */
	public DirectionalLight(Vector3 direction) {
		super(Light.DEFAULT_LIGHT_INTENSITY);
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating Directional Light. Direction : " + direction);
		this.light_vector = direction.times(-1).normalize(); // light vector (the opposite) is normalized
	}
	
	/**
	 * Create Directional Light using direction as vector of the light and separated scalar for intensity
	 * @param direction is where the light comes from (vector from viewer to light source)
	 * @param intensity
	 */
	public DirectionalLight(Vector3 direction, float intensity) {
		super(intensity);
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating Directional Light. Direction : " + direction + " Intensity : " + intensity);
		//this.direction = new Vector3(direction).normalize(); // direction vector is normalized
		this.light_vector = direction.times(-1).normalize(); // light vector (the opposite) is normalized
	}

	/**
	 * Create Directional Light using direction as vector of the light, with default intensity.
	 * See the (Vector3) constructor's Javadoc for why intensity is no longer derived from the
	 * vector's length.
	 * @param direction is where the light comes from (vector from viewer to light source)
	 * @param shadowingBox_type
	 */
	public DirectionalLight(Vector3 direction, int shadowingBox_type) {
		super(Light.DEFAULT_LIGHT_INTENSITY, shadowingBox_type);
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating Directional Light. Direction : " + direction);
		this.light_vector = direction.times(-1).normalize(); // light vector (the opposite) is normalized
	}
	
	/**
	 * Create Directional Light using direction as vector of the light and separated scalar for intensity
	 * @param direction is where the light comes from (vector from viewer to light source)
	 * @param intensity
	 * @param shadowingBox_type
	 */
	public DirectionalLight(Vector3 direction, float intensity, int shadowingBox_type) {
		super(intensity, shadowingBox_type);
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating Directional Light. Direction : " + direction + " Intensity : " + intensity);
		//this.direction = new Vector3(direction).normalize(); // direction vector is normalized
		this.light_vector = direction.times(-1).normalize(); // light vector (the opposite) is normalized
	}

//	/**
//	 * Create Directional Light using direction as vector of the light
//	 * The intensity of the light will be extrapolate from the norm of the provided direction vector
//	 * @param direction is where the light comes from (vector from viewer to light source)
//	 */
//	public DirectionalLight(Vector3 direction, World world) {
//		super(direction.length(), world); // Intensity is taken from the norm of the direction vector
//		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating Directional Light. Direction : " + direction + " + World");
//		//this.direction = new Vector3(direction).normalize(); // direction vector is normalized
//		this.light_vector = direction.times(-1).normalize(); // light vector (the opposite) is normalized
//	}
//	
//	/**
//	 * Create Directional Light using direction as vector of the light and separated scalar for intensity
//	 * @param direction is where the light comes from (vector from viewer to light source)
//	 * @param intensity
//	 */
//	public DirectionalLight(Vector3 direction, float intensity, World world) {
//		super(intensity, world);
//		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating Directional Light. Direction : " + direction + " Intensity : " + intensity + " + World");
//		//this.direction = new Vector3(direction).normalize(); // direction vector is normalized
//		this.light_vector = direction.times(-1).normalize(); // light vector (the opposite) is normalized
//	}

	/**
	 * The returned vector is normalized
	 */
	@Override
	public Vector3 getLightVectorAtPoint(Vector4 point) {
		// Same direction vector in all world space by definition of Directional Light
		return this.light_vector;
	}
	
	@Override
	public float getIntensity(Vector4 point) {
		// Same intensity in any point of world space as Directional light have infinite range
		return intensity;
	}

	// getLightColorAtPoint() removed: was a broken stub returning null, now covered by Light's
	// default implementation.

	@Override
	public void setLightVector(Vector3 light) {
		// NOTE: no longer derives intensity from the vector's length -- same fix as the
		// constructor below, see its Javadoc for why.
		this.light_vector = new Vector3(light).normalize();
	}

	@Override
	public void setIntensity(float intensity) {
		this.intensity = intensity;
	}
	
	@Override
	public void initShadowing(Perspective perspectiveWorld, Camera camera_view, World world) {
		this.world = world;
		initShadowing(perspectiveWorld, camera_view);
	}

	@Override
	public void initShadowing(Perspective perspectiveWorld, Camera camera_view) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "initShadowing");

		// Forward = propagation direction of the light (from source into the scene) = -light_vector
		Vector3 forward = this.light_vector.times(-1); // already unit length (light_vector is normalized)
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "Light forward direction: " + forward);

		// Robust up hint: Z_AXIS by convention (matches the rest of the engine), falling back to
		// Y_AXIS when forward is (nearly) parallel to Z_AXIS -- this is what used to make side = f×u
		// collapse to a near-zero vector and blow up normalize() with NaN/Infinity (the
		// "WARNING BUG MISTAKE ERROR" that used to be flagged here).
		Vector3 upHint = Vector3.Z_AXIS;
		if (Math.abs(forward.dot(Vector3.Z_AXIS)) > 0.999f) {
			upHint = Vector3.Y_AXIS;
		}

		// Build this light's own orthonormal basis exactly the way LookAt does internally
		// (side = f x u, up = side x f), so it is guaranteed consistent with the actual camera matrix
		// built by new Camera(eye, poi, up) below.
		Vector3 side = forward.times(upHint).normalize();
		Vector3 up = side.times(forward).normalize();

		// Gather the 8 corners (world space) of the box to fit, depending on shadowingBox_type.
		Vector4[] corners = computeBoxCorners(perspectiveWorld, camera_view);

		// Project every corner onto (forward, side, up) to get this light's tight-fit extents.
		// For an AABB-derived corner set (center +/- half-extents on each axis), the projection
		// onto ANY unit axis is symmetric around the projection of the center -- so left=-right
		// and bottom=-top fall out naturally here, without assuming it up front.
		float tmin = Float.MAX_VALUE, tmax = -Float.MAX_VALUE;
		float smin = Float.MAX_VALUE, smax = -Float.MAX_VALUE;
		float umin = Float.MAX_VALUE, umax = -Float.MAX_VALUE;
		Vector3 centerSum = new Vector3(0, 0, 0);
		for (Vector4 c : corners) {
			Vector3 c3 = c.V3();
			float t = forward.dot(c3);
			float s = side.dot(c3);
			float u = up.dot(c3);
			if (t < tmin) tmin = t;
			if (t > tmax) tmax = t;
			if (s < smin) smin = s;
			if (s > smax) smax = s;
			if (u < umin) umin = u;
			if (u > umax) umax = u;
			centerSum.plusEquals(c3);
		}
		Vector3 boxCenter = centerSum.times(1f / corners.length);
		float sCenter = side.dot(boxCenter);
		float uCenter = up.dot(boxCenter);
		float tCenter = forward.dot(boxCenter);

		// Small margin so the near plane never sits exactly on the box's surface (avoids a
		// degenerate near=0 and gives a little slack for elements right at the box edge).
		float margin = Math.max(0.01f, 0.05f * (tmax - tmin));
		float eyeT = tmin - margin;

		float near = margin;                // = tmin - eyeT
		float far = (tmax - tmin) + margin; // = tmax - eyeT
		float left = smin - sCenter;
		float right = smax - sCenter;
		float bottom = umin - uCenter;
		float top = umax - uCenter;

		// eye = boxCenter shifted along forward only, so its own (side, up) coordinates match
		// boxCenter's -- which is exactly what makes left/right/bottom/top above (computed
		// without explicitly subtracting eye) correct.
		Vector3 eye3 = boxCenter.plus(forward.times(eyeT - tCenter));
		Vector4 eye = new Vector4(eye3.getX(), eye3.getY(), eye3.getZ(), 1);
		Vector4 poi = eye.plus(forward); // any point further along forward works, LookAt only needs the direction

		if (Tracer.info) Tracer.traceInfo(this.getClass(), "Light eye: " + eye + " poi: " + poi + " up: " + up);
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "Light box: left=" + left + " right=" + right + " bottom=" + bottom + " top=" + top + " near=" + near + " far=" + far);

		camera_light = new Camera(eye, poi, up.V4());

		// At last initialize the Orthographic projection using the exact (possibly asymmetric)
		// extents computed above.
		// TODO: ppu is fixed arbitrarily here (map resolution = box extent * ppu, not a fixed
		// pixel count) -- tying this to DEFAULT_SHADOW_MAP_DIMENSION instead is left for a later,
		// separate step (backlog item, not part of this patch).
		int ppu = 1000;
		perspectiveCtx_light = new PerspectiveContext(top, bottom, right, left, far, near, PerspectiveContext.PERSPECTIVE_TYPE_ORTHOGRAPHIC, ppu);

		map_size = perspectiveCtx_light.getPixelWidth();
		if (perspectiveCtx_light.getPixelWidth() != perspectiveCtx_light.getPixelHeight()) {
			// Should never happen
			if (Tracer.error) Tracer.traceError(this.getClass(), "perspectiveLight pixel width: " + perspectiveCtx_light.getPixelWidth() + " is different than pixel height: " + perspectiveCtx_light.getPixelHeight());			
		}

		// NOTE: rasterizer_light is no longer constructed here. TriangleRasterizer needs a ZBuffer
		// at construction time, and that ZBuffer should be fresh for every shadow map generation
		// (not reused stale across frames for a moving scene) -- so it's now built inside
		// ShadowingLight.generateShadowMap(), right when map_size is used, instead of here.

		// Create the MVP using this orthographic projection matrix
		// Both constructed once, here, for this light's lifetime -- see their fields' Javadoc on
		// ShadowingLight for why two objects now cover what a single ModelViewProjection used to.
		// elementTransform_light reuses viewProjection_light's matrix rather than recomputing it.
		viewProjection_light = new ViewProjection(camera_light, perspectiveCtx_light.getPerspective());
		elementTransform_light = new ElementTransform(viewProjection_light);
		
	}

	/**
	 * Returns the 8 world-space corners of the box to use for this light's shadow frustum,
	 * according to shadowingBox_type.
	 *
	 * SHADOWING_BOX_ELEMENT and SHADOWING_BOX_SPECIFIC are not implemented yet -- both fall back
	 * to SHADOWING_BOX_WORLD (with an error trace) rather than returning a null/empty box, which
	 * would otherwise blow up the projection below.
	 */
	private Vector4[] computeBoxCorners(Perspective perspectiveWorld, Camera camera_view) {

		if (Tracer.info) Tracer.traceInfo(this.getClass(), "Creating Bounding Box. ShadowingBox type : " + toStringShadowingBoxType(this.shadowingBox_type));

		switch (this.shadowingBox_type) {

		case SHADOWING_BOX_VIEWFRUSTUM: {
			// Define the bounding box for the light camera using the View Frustum (Camera of the scene):
			// its 8 corners, in world space.
			Vector4[][] frustum = perspectiveWorld.getFrustumFromEye(camera_view);
			Vector4[] corners = new Vector4[8];
			int k = 0;
			for (int i = 0; i < 2; i++) {
				for (int j = 0; j < 4; j++) {
					corners[k++] = frustum[i][j];
				}
			}
			return corners;
		}

		case SHADOWING_BOX_ELEMENT:
			if (Tracer.error) Tracer.traceError(this.getClass(), "Not implemented yet: SHADOWING_BOX_ELEMENT -- falling back to SHADOWING_BOX_WORLD");
			// Fall through to SHADOWING_BOX_WORLD

		case SHADOWING_BOX_SPECIFIC:
			if (this.shadowingBox_type == SHADOWING_BOX_SPECIFIC && Tracer.error) Tracer.traceError(this.getClass(), "Not implemented yet: SHADOWING_BOX_SPECIFIC -- falling back to SHADOWING_BOX_WORLD");
			// Fall through to SHADOWING_BOX_WORLD

		case SHADOWING_BOX_WORLD:
		default: {
			if (this.shadowingBox_type != SHADOWING_BOX_WORLD && Tracer.error) Tracer.traceError(this.getClass(), "Unknown Shadowing Box Type: " + this.shadowingBox_type + " -- falling back to SHADOWING_BOX_WORLD");
			// World-space AABB of the whole World (all Elements, recursively) -- see World.getWorldBounds()'s Javadoc.
			BoundingBox4 box = new BoundingBox4(world.getWorldBounds());
			return new Vector4[] {
					box.getP11(), box.getP12(), box.getP13(), box.getP14(),
					box.getP21(), box.getP22(), box.getP23(), box.getP24()
			};
		}
		}
	}

	public String toString() {
		return "Directional Light with light vector (= -direction of the light) : " + this.light_vector;
	}
}