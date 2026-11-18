package com.aventura.model.light;

import static org.junit.Assert.*;

import org.junit.Test;

import com.aventura.context.PerspectiveContext;
import com.aventura.engine.ElementTransform;
import com.aventura.engine.ViewProjection;
import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.perspective.Perspective;
import com.aventura.model.perspective.PerspectiveType;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Trellis;
import com.aventura.view.MapView;

/**
 * Tests for the FRUSTUM-projection branch of ShadowingLight's phase-2 generalization: linear
 * depth (W) storage and comparison, the x/w, y/w perspective divide used for sampling, the bounds
 * test, and the far-plane initial value. None of this is exercised yet by any concrete Light
 * (PointLight and SpotLight don't implement their shadow maps yet, see initShadowing() stubs), so
 * this test wires up a minimal ShadowingLight subclass directly with a FRUSTUM
 * PerspectiveContext -- the same way DirectionalLight.initShadowing() wires an ORTHOGRAPHIC one --
 * ahead of phase 4/5 actually connecting Spot/Point to this code path.
 */
public class TestShadowingLightFrustum {

	/** Minimal concrete ShadowingLight: just enough Light behavior to call shadowFactorAt(). */
	private static class FrustumProbeLight extends ShadowingLight {
		@Override public Vector3 getLightVectorAtPoint(Vector4 point) { return Vector3.zAxis(); } // "up", toward the light
		@Override public float getIntensity(Vector4 point) { return 1f; }
		@Override public void setLightVector(Vector3 light) { }
		@Override public void setIntensity(float intensity) { }
		@Override public void initShadowing(Perspective perspective, Camera camera_view) { }
		@Override public void initShadowing(Perspective perspective, Camera camera_view, World world) { }

		/** Wires up a symmetric frustum, eye at (0,0,eyeZ) looking straight down (-Z), up = Y. */
		void setupFrustum(float eyeZ, float halfFovDegrees, float near, float far, int pixels) {
			Vector4 eye = new Vector4(0, 0, eyeZ, 1);
			Vector4 poi = new Vector4(0, 0, eyeZ - 1, 1);
			camera_light = new Camera(eye, poi, Vector4.yAxis());
			float halfExtentAtNear = near * (float) Math.tan(Math.toRadians(halfFovDegrees));
			perspectiveCtx_light = new PerspectiveContext(pixels, 2 * halfExtentAtNear, 2 * halfExtentAtNear, near, far - near, PerspectiveType.FRUSTUM);
			viewProjection_light = new ViewProjection(camera_light, perspectiveCtx_light.getPerspective());
			elementTransform_light = new ElementTransform(viewProjection_light);
		}
	}

	private static World emptyWorld() {
		World world = new World();
		world.build();
		world.worldProject();
		return world;
	}

	/** A flat occluder in the XY plane (as Trellis's default orientation gives, see TestShadowMapSize), translated to the given Z. */
	private static World occluderAt(float z, float size) {
		World world = new World();
		Trellis wall = new Trellis(size, size, 4, 4);
		wall.setTransformation(new Translation(new Vector4(0, 0, z, 0)));
		world.addElement(wall);
		world.build();
		world.worldProject();
		return world;
	}

	@Test
	public void testFarValue_usesPerspectiveFar_notOne() {
		FrustumProbeLight light = new FrustumProbeLight();
		light.setupFrustum(5f, 30f, 1f, 10f, 64);
		light.generateShadowMap(emptyWorld()); // nothing drawn: every cell keeps the initial "far" value
		MapView map = light.getMap();
		float untouched = light.getMap(map.getViewWidth() / 2, map.getViewHeight() / 2);
		assertEquals("frustum shadow map should initialize to this light's far plane distance, not an NDC-style 1.0",
				10f, untouched, 0.01f);
	}

	@Test
	public void testShadowFactorAt_outOfFrustumBounds_isTreatedAsUnshadowed() {
		FrustumProbeLight light = new FrustumProbeLight();
		light.setupFrustum(5f, 10f, 1f, 10f, 64); // narrow 10 degree half-angle cone
		light.generateShadowMap(emptyWorld());
		// Far outside the (narrow) cone at this depth: must not be read as shadowed by border clamping.
		Vector4 farAside = new Vector4(50, 50, 0, 1);
		assertEquals(1f, light.shadowFactorAt(farAside, new Vector3(0, 0, 1)), 0f);
	}

	@Test
	public void testShadowFactorAt_frustum_occluderCastsShadowOnlyBehindIt() {
		FrustumProbeLight light = new FrustumProbeLight();
		light.setupFrustum(5f, 30f, 1f, 10f, 200);
		World world = occluderAt(2f, 4f); // a wall at z=2, between the light (z=5) and z=0
		light.generateShadowMap(world);

		Vector3 up = new Vector3(0, 0, 1);
		// Behind the occluder (from the light's point of view): in its shadow.
		assertEquals("point behind the occluder should be shadowed",
				0f, light.shadowFactorAt(new Vector4(0.5f, 0.5f, 0f, 1), up), 0f);
		// In front of the occluder (closer to the light than the wall): nothing blocks it yet.
		assertEquals("point between the light and the occluder should be lit",
				1f, light.shadowFactorAt(new Vector4(0.5f, 0.5f, 3f, 1), up), 0f);
	}
}
