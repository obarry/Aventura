package com.aventura.model.light;

import static org.junit.Assert.*;

import org.junit.Test;

import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.perspective.PerspectiveType;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Sphere;
import com.aventura.model.world.shape.Trellis;

/**
 * Phase 4: SpotLight's own Frustum shadow map, built by initShadowing() from its position,
 * direction and outer angle alone (it ignores the scene's camera/perspective -- unlike
 * DirectionalLight, whose box is fitted to the scene, a Spot's shadow frustum is entirely defined
 * by its own cone). This reuses the shared Frustum pipeline already generalized in
 * ShadowingLight/NearPlaneClipper (phases 2/3): nothing Spot-specific was needed there.
 *
 * The main test below reproduces the audit's phase-4 validation criterion directly: a sphere on a
 * floor under a Spot Light, with the shadow's geometric center (the point exactly behind the
 * sphere's center, as seen from the light) computed by hand via similar triangles, independent of
 * the renderer.
 */
public class TestSpotLightShadows {

	private static World sphereOnFloor(Vector4 sphereCenter, float radius) {
		World world = new World();
		Trellis floor = new Trellis(20, 20, 4, 4); // XY plane, z=0 (see TestShadowingLightFrustum)
		world.addElement(floor);
		Sphere ball = new Sphere(radius, 16);
		ball.setTransformation(new Translation(sphereCenter));
		world.addElement(ball);
		world.build();
		return world;
	}

	@Test
	public void testInitShadowing_buildsASquareFrustumFromTheOuterAngleAlone() {
		System.out.println("***** Test SpotLight shadows : initShadowing() builds a Frustum sized from the outer angle, not the scene's camera *****");
		SpotLight spot = new SpotLight(new Vector4(0, 0, 10, 1), new Vector3(0, 0, -1), 20f, (float) Math.toRadians(60));
		// Deliberately pass null perspective/camera: a Spot's shadow frustum must not depend on
		// either (unlike DirectionalLight's SHADOWING_BOX_VIEWFRUSTUM option).
		spot.initShadowing(null, null, sphereOnFloor(new Vector4(0, 0, 1, 1), 1f));

		assertEquals(PerspectiveType.FRUSTUM, spot.perspectiveCtx_light.getPerspectiveType());
		assertEquals("the frustum is square: width == height", spot.perspectiveCtx_light.getPixelWidth(), spot.perspectiveCtx_light.getPixelHeight());
		assertEquals("far plane is this light's max_distance", 20f, spot.perspectiveCtx_light.getPerspective().getFar(), 0.01f);
		assertTrue("near plane is strictly positive and well before far", spot.perspectiveCtx_light.getPerspective().getNear() > 0f
				&& spot.perspectiveCtx_light.getPerspective().getNear() < 20f);
		System.out.println("PASS testInitShadowing_buildsASquareFrustumFromTheOuterAngleAlone");
	}

	@Test
	public void testShadowFactorAt_sphereOnFloor_shadowCenterMatchesHandCalculatedGeometry() {
		System.out.println("***** Test SpotLight shadows : sphere on a floor, shadow centre at the geometrically expected point *****");

		// Light at (5,0,10) aimed at the origin -- NOT directly overhead, so the shadow falls to one
		// side, away from a trivial "right below the sphere" case.
		Vector4 lightPos = new Vector4(5, 0, 10, 1);
		Vector3 direction = new Vector3(lightPos, new Vector4(0, 0, 0, 1)).normalize(); // light -> origin
		SpotLight spot = new SpotLight(lightPos, direction, 20f, (float) Math.toRadians(60));

		// A sphere of radius 1 sitting on the floor at the origin (centre at z = radius).
		Vector4 sphereCenter = new Vector4(0, 0, 1, 1);
		World world = sphereOnFloor(sphereCenter, 1f);
		spot.initShadowing(null, null, world);
		spot.generateShadowMap(world);

		// Hand-calculated umbra centre: the point where the line from the light through the
		// sphere's centre meets the floor (z=0), by similar triangles:
		// x = lightPos.x + (sphereCenter.x - lightPos.x) * lightPos.z / (lightPos.z - sphereCenter.z)
		// = 5 + (0-5) * 10/(10-1) = 5 - 50/9 = -5/9
		float shadowCenterX = 5f + (0f - 5f) * (10f / (10f - 1f));
		assertEquals(-5f / 9f, shadowCenterX, 1e-4f);
		Vector4 shadowCenter = new Vector4(shadowCenterX, 0, 0, 1);
		Vector3 up = Vector3.zAxis();

		assertEquals("the umbra centre, directly behind the sphere's own centre, must be shadowed",
				0f, spot.shadowFactorAt(shadowCenter, up), 0f);

		// A floor point well clear of the sphere's shadow (the umbra's extent here is only about 1
		// unit, see the class Javadoc's derivation; 4 units away is unambiguous), still well inside
		// this light's 60 degree cone at this distance: must read as lit, not shadowed.
		Vector4 clearlyOutside = new Vector4(shadowCenterX + 4f, 0, 0, 1);
		assertEquals("a floor point clear of the sphere's shadow must be lit",
				1f, spot.shadowFactorAt(clearlyOutside, up), 0f);

		System.out.println("PASS testShadowFactorAt_sphereOnFloor_shadowCenterMatchesHandCalculatedGeometry");
	}

	@Test
	public void testShadowFactorAt_pointOutsideThisLightsFrustum_isTreatedAsUnshadowed() {
		System.out.println("***** Test SpotLight shadows : a point outside this light's own frustum/cone is not shadowed by it *****");
		SpotLight spot = new SpotLight(new Vector4(0, 0, 10, 1), new Vector3(0, 0, -1), 20f, (float) Math.toRadians(20));
		World world = sphereOnFloor(new Vector4(0, 0, 1, 1), 1f);
		spot.initShadowing(null, null, world);
		spot.generateShadowMap(world);

		// Far to the side, well outside the (narrow, 20 degree) cone at this depth.
		Vector4 farAside = new Vector4(50, 50, 0, 1);
		assertEquals(1f, spot.shadowFactorAt(farAside, Vector3.zAxis()), 0f);
		System.out.println("PASS testShadowFactorAt_pointOutsideThisLightsFrustum_isTreatedAsUnshadowed");
	}

	@Test
	public void testGetDefaultShadowMapSize_isShadowingLightsOwn_notPointLightsSixFaceOne() {
		System.out.println("***** Test SpotLight shadows : default shadow map size is ShadowingLight's single-map default (1000), not PointLight's per-face one (512) *****");
		// SpotLight extends PointLight, which (phase 5) overrides getDefaultShadowMapSize() to 512 --
		// SpotLight must reassert ShadowingLight.DEFAULT_SHADOW_MAP_SIZE for its own single map (see
		// SpotLight.getDefaultShadowMapSize()'s Javadoc).
		SpotLight spot = new SpotLight(new Vector4(0, 0, 10, 1), new Vector3(0, 0, -1), 20f, (float) Math.toRadians(45));
		assertEquals(ShadowingLight.DEFAULT_SHADOW_MAP_SIZE, spot.getDefaultShadowMapSize());
		assertEquals(ShadowingLight.DEFAULT_SHADOW_MAP_SIZE, spot.getShadowMapSize());
		System.out.println("PASS testGetDefaultShadowMapSize_isShadowingLightsOwn_notPointLightsSixFaceOne");
	}
}
