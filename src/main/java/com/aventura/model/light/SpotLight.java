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


/**
 * A Spot Light is a Point Light with a preferred direction of radiation: it lights a cone, whose apex is
 * the light source, around a direction (the axis of the cone).
 *
 * Model (all angles are in radians, like everywhere else in Aventura's API):
 * - the axis of the cone is a unit vector, the direction in which the light PROPAGATES (from the light
 *   towards the lit scene). This is the same convention as the argument of DirectionalLight's constructor.
 * - the outer angle is the half-angle of the cone: nothing is lit outside of it.
 * - the inner angle (inner <= outer) is the half-angle of the fully lit core: the light fades smoothly
 *   (smoothstep on the cosine of the angle) from full intensity at the inner angle to zero at the outer angle.
 *   With inner == outer the edge of the cone is sharp.
 * - the intensity at a point is: (Point Light distance attenuation) x (intensity factor) x (cone factor).
 *   Since Lighting only goes through getLightVectorAtPoint() and getIntensity() / getLightColorAtPoint(),
 *   the cone is fully handled here and Lighting does not need to know about it.
 *
 * So it inherits from PointLight from a class standpoint: the light vector at a point, the distance
 * attenuation and the intensity setter are the ones of PointLight.
 *
 * Shadows: a Spot Light casts shadows through a single Frustum shadow map (see initShadowing()):
 * the light camera sits at the light's position, looks along its direction (the cone's axis), and its
 * field of view is set from the OUTER angle, so the near plane's EDGE (not just its corner) lands
 * exactly on the cone boundary -- reusing the shared Frustum pipeline already generalized in
 * ShadowingLight/NearPlaneClipper (phases 2/3), nothing Spot-specific needed there. The depth range
 * runs from a small near plane out to max_distance, beyond which PointLight's attenuation already
 * zeroes the light out, so nothing meaningful could be in shadow past it anyway.
 *
 * @author Olivier BARRY
 * @since July 2016
 * 
 */

public class SpotLight extends PointLight {

	/** Default half-angle of the cone: 45 degrees. */
	public static final float DEFAULT_OUTER_ANGLE = (float) Math.toRadians(45);
	/** When only the outer angle is given, the inner angle is this fraction of it (a soft edge). */
	public static final float DEFAULT_INNER_RATIO = 0.8f;

	// Below this squared length a direction vector is considered null
	private static final float MIN_DIRECTION_LENGTH_SQUARED = 1e-12f;

	private Vector3 direction; // Unit vector: direction of propagation of the light (the axis of the cone)
	private float outerAngle; // Half-angle of the cone (radians), in ]0, PI/2[
	private float innerAngle; // Half-angle of the fully lit core (radians), in [0, outerAngle]
	private float cosOuter; // Cosines of the two angles, kept to avoid recomputing them for each lit point
	private float cosInner;

	/**
	 * Create a Spot Light pointing downwards (-Z, Z being the vertical axis) with the default cone
	 * (outer angle 45 degrees, inner angle 80% of it) and the default intensity. Use setLightVector() to aim it.
	 * @param point position of the light source
	 * @param max distance beyond which the light does not light anything
	 */
	public SpotLight(Vector4 point, float max) {
		super(point, max);
		this.direction = new Vector3(0, 0, -1);
		setAngles(DEFAULT_OUTER_ANGLE, DEFAULT_OUTER_ANGLE * DEFAULT_INNER_RATIO);
	}

	/**
	 * Create a Spot Light with the default intensity and a soft edge (inner angle = 80% of the outer angle).
	 * @param point position of the light source
	 * @param direction direction of propagation of the light (any non null length: it is normalized)
	 * @param max distance beyond which the light does not light anything
	 * @param outerAngle half-angle of the cone in radians, in ]0, PI/2[
	 */
	public SpotLight(Vector4 point, Vector3 direction, float max, float outerAngle) {
		this(point, direction, max, outerAngle, outerAngle * DEFAULT_INNER_RATIO);
	}

	/**
	 * Create a Spot Light with the default intensity.
	 * @param point position of the light source
	 * @param direction direction of propagation of the light (any non null length: it is normalized)
	 * @param max distance beyond which the light does not light anything
	 * @param outerAngle half-angle of the cone in radians, in ]0, PI/2[
	 * @param innerAngle half-angle of the fully lit core in radians, in [0, outerAngle]
	 */
	public SpotLight(Vector4 point, Vector3 direction, float max, float outerAngle, float innerAngle) {
		super(point, max);
		setDirection(direction);
		setAngles(outerAngle, innerAngle);
	}

	/**
	 * Create a Spot Light.
	 * @param point position of the light source
	 * @param direction direction of propagation of the light (any non null length: it is normalized)
	 * @param max distance beyond which the light does not light anything
	 * @param intensity general multiplication factor of the light
	 * @param outerAngle half-angle of the cone in radians, in ]0, PI/2[
	 * @param innerAngle half-angle of the fully lit core in radians, in [0, outerAngle]
	 */
	public SpotLight(Vector4 point, Vector3 direction, float max, float intensity, float outerAngle, float innerAngle) {
		super(point, max, intensity);
		setDirection(direction);
		setAngles(outerAngle, innerAngle);
	}

	public SpotLight(int shadowingBox_type, World world, Vector4 point, float max) {
		super(shadowingBox_type, world, point, max);
		this.direction = new Vector3(0, 0, -1);
		setAngles(DEFAULT_OUTER_ANGLE, DEFAULT_OUTER_ANGLE * DEFAULT_INNER_RATIO);
	}

	/**
	 * Direction of propagation of the light: unit vector, from the light towards the scene (axis of the cone).
	 * @return a copy of the direction
	 */
	public Vector3 getDirection() {
		return new Vector3(direction);
	}

	/**
	 * Aim the Spot Light. Same as setLightVector().
	 * @param direction direction of propagation of the light (from the light towards the scene), any non null length: it is normalized
	 * @throws IllegalArgumentException if the vector is null or has a null length
	 */
	public void setDirection(Vector3 direction) {
		if (direction == null || direction.lengthSquared() < MIN_DIRECTION_LENGTH_SQUARED) {
			throw new IllegalArgumentException("A SpotLight direction cannot be null or a zero vector: " + direction);
		}
		this.direction = new Vector3(direction).normalize();
	}

	/**
	 * Set the direction of this Spot Light, like the argument of DirectionalLight's constructor: the
	 * direction of propagation of the light (from the light towards the scene), NOT the vector towards the light.
	 */
	@Override
	public void setLightVector(Vector3 light) {
		setDirection(light);
	}

	/** Half-angle of the cone (radians): nothing is lit outside of it. */
	public float getOuterAngle() {
		return outerAngle;
	}

	/** Half-angle of the fully lit core of the cone (radians). */
	public float getInnerAngle() {
		return innerAngle;
	}

	/**
	 * Set the two angles of the cone.
	 * @param outerAngle half-angle of the cone in radians, in ]0, PI/2[
	 * @param innerAngle half-angle of the fully lit core in radians, in [0, outerAngle]
	 * @throws IllegalArgumentException if the angles are out of these ranges
	 */
	public void setAngles(float outerAngle, float innerAngle) {
		if (!(outerAngle > 0 && outerAngle < (float) Math.PI / 2)) {
			throw new IllegalArgumentException("SpotLight outer angle must be in ]0, PI/2[ radians: " + outerAngle);
		}
		if (!(innerAngle >= 0 && innerAngle <= outerAngle)) {
			throw new IllegalArgumentException("SpotLight inner angle must be in [0, outer angle] radians: " + innerAngle + " (outer: " + outerAngle + ")");
		}
		this.outerAngle = outerAngle;
		this.innerAngle = innerAngle;
		this.cosOuter = (float) Math.cos(outerAngle);
		this.cosInner = (float) Math.cos(innerAngle);
	}

	/**
	 * The "spot light effect": how much of the light a point receives because of its angle to the axis
	 * of the cone. 1 in the core (angle <= inner angle), 0 outside of the cone (angle >= outer angle) and
	 * a smoothstep of the cosine of the angle in between.
	 * @param point the lit point (world space)
	 * @return the cone factor, in [0, 1]
	 */
	public float coneFactor(Vector4 point) {
		// Unit vector from the lit point towards the light (zero vector if the point is the light's position)
		Vector3 toLight = super.getLightVectorAtPoint(point);
		if (toLight.lengthSquared() == 0) {
			return 1f; // At the apex: nothing to attenuate (and no light vector, so Lighting ignores it anyway)
		}
		float cosTheta = -toLight.dot(direction); // Cosine of the angle between the axis and the direction light -> point

		if (cosTheta >= cosInner) {
			return 1f;
		}
		if (cosTheta <= cosOuter) {
			return 0f;
		}
		// cosInner > cosTheta > cosOuter here, so the divisor is strictly positive
		float t = (cosTheta - cosOuter) / (cosInner - cosOuter);
		return t * t * (3 - 2 * t); // smoothstep
	}

	/**
	 * Intensity at a point: distance attenuation and intensity factor (PointLight) x cone factor.
	 */
	@Override
	public float getIntensity(Vector4 point) {
		float coneFactor = coneFactor(point);
		if (coneFactor == 0f) {
			return 0f; // Outside of the cone: skip the rest
		}
		return super.getIntensity(point) * coneFactor;
	}

	// getLightVectorAtPoint(), setIntensity() are the ones of PointLight.
	// getLightColorAtPoint() removed: was a broken stub returning null, now covered by Light's
	// default implementation.
	// setLightColor(Color) removed: was an empty override silently no-op'ing Light's working
	// implementation -- same bug as the one found and fixed in AmbientLight.

	/**
	 * Builds this Spot Light's shadow camera/frustum/projection: eye at the light's position,
	 * looking along direction (the cone's axis), field of view = 2 x outerAngle on both axes (a
	 * square frustum -- the cone, being rotationally symmetric, inscribes within it; the square's
	 * corners see a little beyond the cone, which is harmless since getIntensity()'s cone factor
	 * already zeroes those points out regardless of the shadow test).
	 *
	 * near is a small, fixed-ish fraction of max_distance (not a physically meaningful bound --
	 * just far enough from the apex to keep the projection well-conditioned); far is max_distance
	 * itself, since PointLight.attenuationFunc() already zeroes this light's contribution beyond
	 * it. Same robust up-vector hint as DirectionalLight.initShadowing() (Z axis, falling back to Y
	 * when the cone points along Z) so LookAt never collapses side = forward x up to near zero.
	 */
	@Override
	public void initShadowing(Perspective perspective, Camera camera_view, World world) {
		this.world = world;
		initShadowing(perspective, camera_view);
	}

	@Override
	public void initShadowing(Perspective perspective, Camera camera_view) {
		Vector3 forward = getDirection(); // already a unit vector (setDirection() normalizes it)

		Vector3 upHint = Vector3.zAxis();
		if (Math.abs(forward.dot(Vector3.zAxis())) > 0.999f) {
			upHint = Vector3.yAxis();
		}

		Vector4 eye = getPosition();
		Vector4 poi = eye.plus(forward); // any point further along forward works, LookAt only needs the direction
		camera_light = new Camera(eye, poi, upHint.V4());

		float far = getMaxDistance();
		float near = Math.max(0.01f, 0.01f * far);
		if (near >= far) {
			near = far * 0.5f;
		}

		// Near-plane half extent so the square frustum's EDGE reaches the outer angle exactly:
		// halfExtent = near * tan(outerAngle) (not the corner/diagonal, which would need a larger
		// angle and let the cone touch past the window's edges).
		float halfExtent = near * (float) Math.tan(getOuterAngle());
		float size = 2f * halfExtent;

		// Fixed PIXEL resolution regardless of the (tiny, by construction) near-plane window's
		// world-space size -- same reasoning as DirectionalLight.initShadowing(); square map since
		// the frustum itself is square here (unlike Directional's scene-fitted rectangular box).
		perspectiveCtx_light = new PerspectiveContext(getShadowMapSize(), size, size, near, far - near, PerspectiveType.FRUSTUM);

		viewProjection_light = new ViewProjection(camera_light, perspectiveCtx_light.getPerspective());
		elementTransform_light = new ElementTransform(viewProjection_light);
	}

	/**
	 * Re-asserts ShadowingLight's single-map shadow generation: without this override, SpotLight
	 * would inherit generateShadowMap(World) from its OWN superclass PointLight, which (phase 5)
	 * overrides it for PointLight's six-face cube map -- the wrong model for a SpotLight, whose
	 * shadow is the single Frustum map built by initShadowing() above. generateSingleShadowMap() is
	 * the exact, unmodified single-map body (see its Javadoc in ShadowingLight).
	 */
	@Override
	public void generateShadowMap(World world) {
		generateSingleShadowMap(world);
	}

	/**
	 * Re-asserts ShadowingLight's single-map shadow sampling -- see generateShadowMap(World)
	 * above for why this override is needed (PointLight, SpotLight's superclass, overrides this
	 * method too, for its own six-face cube map).
	 */
	@Override
	public float shadowFactorAt(Vector4 worldPosition, Vector3 normal) {
		return singleShadowFactorAt(worldPosition, normal);
	}

	/**
	 * Re-asserts ShadowingLight's own default shadow map size (1000 px): without this override,
	 * SpotLight would inherit PointLight's getDefaultShadowMapSize() override (512 px, sized for
	 * ONE of six cube faces -- see its Javadoc), which has no bearing on a SpotLight's single map.
	 */
	@Override
	public int getDefaultShadowMapSize() {
		return ShadowingLight.DEFAULT_SHADOW_MAP_SIZE;
	}

	/**
	 * Re-asserts ShadowingLight's own single-map width/height accessors: without these overrides,
	 * SpotLight would inherit PointLight's (reading its faceXxx arrays, which a SpotLight never
	 * populates -- they would silently read as 0). perspectiveCtx_light is the single Frustum this
	 * class's own initShadowing() builds.
	 */
	@Override
	public int getShadowMapWidth() {
		return perspectiveCtx_light != null ? perspectiveCtx_light.getPixelWidth() : 0;
	}

	@Override
	public int getShadowMapHeight() {
		return perspectiveCtx_light != null ? perspectiveCtx_light.getPixelHeight() : 0;
	}

}
