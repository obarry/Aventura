package com.aventura.model.light;

import static org.junit.Assert.*;

import org.junit.Test;

import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.perspective.PerspectiveType;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Cube;

/**
 * Phase 5: PointLight's own shadow map -- a cube map of six independent Frustum shadow maps (one
 * per axis-aligned face: +X, -X, +Y, -Y, +Z, -Z), each with an exact 90 degree field of view so the
 * six faces tile the full sphere of directions around the light with no gap and no overlap (see
 * PointLight.initShadowing()). Reuses the shared single-map pipeline (ShadowingLight.
 * generateShadowMapFace()/generateShadowMapElement()/shadowFactorAt(MapView, ...), extracted in
 * this same phase from the methods DirectionalLight/SpotLight already used) once per face; the only
 * genuinely new logic is PointLight.selectFace() (which face a given direction belongs to) and
 * PointLight's facesNeeded() skip-empty-faces optimization.
 *
 * The main test below reproduces the audit's phase-5 validation criterion directly: a light in a
 * closed room with an object casting a shadow, checked in all six directions, without relying on
 * the renderer -- only on shadowFactorAt(), like TestSpotLightShadows did for phase 4.
 */
public class TestPointLightShadows {

	// ------------------------------------------------------------------
	// selectFace(): six axes and diagonals (the plan's explicit "choix de face" unit test)
	// ------------------------------------------------------------------

	@Test
	public void testSelectFace_sixAxes() {
		System.out.println("***** Test PointLight : selectFace() on the six pure axis directions *****");
		assertEquals(PointLight.FACE_PLUS_X, PointLight.selectFace(new Vector3(5, 0, 0)));
		assertEquals(PointLight.FACE_MINUS_X, PointLight.selectFace(new Vector3(-5, 0, 0)));
		assertEquals(PointLight.FACE_PLUS_Y, PointLight.selectFace(new Vector3(0, 5, 0)));
		assertEquals(PointLight.FACE_MINUS_Y, PointLight.selectFace(new Vector3(0, -5, 0)));
		assertEquals(PointLight.FACE_PLUS_Z, PointLight.selectFace(new Vector3(0, 0, 5)));
		assertEquals(PointLight.FACE_MINUS_Z, PointLight.selectFace(new Vector3(0, 0, -5)));
		System.out.println("PASS testSelectFace_sixAxes");
	}

	@Test
	public void testSelectFace_diagonals() {
		System.out.println("***** Test PointLight : selectFace() on diagonal/ambiguous directions (deterministic X > Y > Z tie-break) *****");
		// Perfect corner: all three components tie in magnitude -- X wins.
		assertEquals(PointLight.FACE_PLUS_X, PointLight.selectFace(new Vector3(1, 1, 1)));
		assertEquals(PointLight.FACE_MINUS_X, PointLight.selectFace(new Vector3(-1, -1, -1)));
		// Two-way tie not involving X -- Y wins over Z.
		assertEquals(PointLight.FACE_PLUS_Y, PointLight.selectFace(new Vector3(0, 1, 1)));
		assertEquals(PointLight.FACE_MINUS_Y, PointLight.selectFace(new Vector3(0, -1, -1)));
		// Two-way tie X vs Z (no Y component) -- X wins.
		assertEquals(PointLight.FACE_PLUS_X, PointLight.selectFace(new Vector3(1, 0, 1)));
		// Unambiguous diagonal, dominant axis away from a tie: must still follow the largest magnitude.
		assertEquals(PointLight.FACE_PLUS_Z, PointLight.selectFace(new Vector3(1, 1, 5)));
		assertEquals(PointLight.FACE_MINUS_Y, PointLight.selectFace(new Vector3(2, -8, 3)));
		System.out.println("PASS testSelectFace_diagonals");
	}

	// ------------------------------------------------------------------
	// initShadowing(): six square 90-degree Frustums
	// ------------------------------------------------------------------

	@Test
	public void testInitShadowing_buildsSixSquareNinetyDegreeFrustums() {
		System.out.println("***** Test PointLight shadows : initShadowing() builds six square 90-degree Frustums *****");
		PointLight light = new PointLight(new Vector4(0, 0, 0, 1), 20f);
		// Deliberately pass null perspective/camera: like SpotLight, a Point Light's six shadow
		// frustums must not depend on either -- they are entirely defined by the light itself.
		light.initShadowing(null, null, closedRoom(3f, 0.6f));

		assertEquals(6, light.facePerspectiveCtx.length);
		for (int face = 0; face < 6; face++) {
			assertEquals("face " + face + " is a Frustum projection",
					PerspectiveType.FRUSTUM, light.facePerspectiveCtx[face].getPerspectiveType());
			assertEquals("face " + face + " is square (90 degree FOV on both axes)",
					light.facePerspectiveCtx[face].getPixelWidth(), light.facePerspectiveCtx[face].getPixelHeight());
			assertEquals("face " + face + " far plane is this light's max_distance",
					20f, light.facePerspectiveCtx[face].getPerspective().getFar(), 0.01f);
			assertTrue("face " + face + " near plane is strictly positive and well before far",
					light.facePerspectiveCtx[face].getPerspective().getNear() > 0f
					&& light.facePerspectiveCtx[face].getPerspective().getNear() < 20f);
		}
		System.out.println("PASS testInitShadowing_buildsSixSquareNinetyDegreeFrustums");
	}

	// ------------------------------------------------------------------
	// generateShadowMap() / shadowFactorAt(): the plan's "closed room with an object" validation
	// ------------------------------------------------------------------

	/**
	 * A light at the origin surrounded, on each of the six axes, by one small cube (so the plan's
	 * "closed room" is here six independent occluders rather than literal walls -- simpler to
	 * hand-verify, while still exercising all six faces the same validation asks for) at distance
	 * occluderDistance, of the given size.
	 */
	private static World closedRoom(float occluderDistance, float occluderSize) {
		World world = new World();
		Vector3[] directions = { Vector3.xAxis(), Vector3.xOppAxis(), Vector3.yAxis(), Vector3.yOppAxis(), Vector3.zAxis(), Vector3.zOppAxis() };
		for (Vector3 direction : directions) {
			Cube cube = new Cube(occluderSize);
			cube.setTransformation(new Translation(direction.times(occluderDistance).V4()));
			world.addElement(cube);
		}
		world.build();
		// facesNeeded() reads world.getWorldBounds(), which needs each vertex's world position to
		// have been computed at least once (see Element.accumulateWorldBounds()) -- RenderEngine
		// does this itself every frame (world.worldProject(), right before initShadowing()/
		// generateShadowMap()); a hand-written test must do the same.
		world.worldProject();
		return world;
	}

	/** A single cube occluder along +X only, at occluderCenter, of the given size -- used by the skip-empty-faces test. */
	private static World singleOccluderAlongPlusX(Vector4 occluderCenter, float occluderSize) {
		World world = new World();
		Cube cube = new Cube(occluderSize);
		cube.setTransformation(new Translation(occluderCenter));
		world.addElement(cube);
		world.build();
		world.worldProject(); // see closedRoom()'s comment above
		return world;
	}

	@Test
	public void testGenerateShadowMap_closedRoomWithObject_shadowsInAllSixDirections() {
		System.out.println("***** Test PointLight shadows : light surrounded by six occluders, shadow in all six directions, no seam *****");

		PointLight light = new PointLight(new Vector4(0, 0, 0, 1), 20f);
		World world = closedRoom(3f, 0.6f);
		light.initShadowing(null, null, world);
		light.generateShadowMap(world);

		Vector3 up = Vector3.zAxis(); // arbitrary normal, only used to slope-scale the bias

		// Directly behind each occluder (same axis, further out): must be shadowed.
		assertEquals("behind the +X occluder", 0f, light.shadowFactorAt(new Vector4(8, 0, 0, 1), up), 0f);
		assertEquals("behind the -X occluder", 0f, light.shadowFactorAt(new Vector4(-8, 0, 0, 1), up), 0f);
		assertEquals("behind the +Y occluder", 0f, light.shadowFactorAt(new Vector4(0, 8, 0, 1), up), 0f);
		assertEquals("behind the -Y occluder", 0f, light.shadowFactorAt(new Vector4(0, -8, 0, 1), up), 0f);
		assertEquals("behind the +Z occluder", 0f, light.shadowFactorAt(new Vector4(0, 0, 8, 1), up), 0f);
		assertEquals("behind the -Z occluder", 0f, light.shadowFactorAt(new Vector4(0, 0, -8, 1), up), 0f);

		// Off to the side of each occluder (same dominant face -- selectFace() still picks it, see
		// testSelectFace_sixAxes/_diagonals -- but outside the small cube's angular shadow): must be
		// lit. This also demonstrates there is no seam artifact at the face boundary itself: these
		// points are much closer, angularly, to the face's edge than the on-axis points above, yet
		// resolve correctly.
		assertEquals("beside the +X occluder, same face, must be lit", 1f, light.shadowFactorAt(new Vector4(8, 2, 0, 1), up), 0f);
		assertEquals("beside the -Z occluder, same face, must be lit", 1f, light.shadowFactorAt(new Vector4(0, 2, -8, 1), up), 0f);

		System.out.println("PASS testGenerateShadowMap_closedRoomWithObject_shadowsInAllSixDirections");
	}

	@Test
	public void testGenerateShadowMap_skipsFacesWithNoGeometry() {
		System.out.println("***** Test PointLight shadows : faces with no geometry in them are skipped (\"saut des faces vides\") *****");

		PointLight light = new PointLight(new Vector4(0, 0, 0, 1), 20f);
		World world = singleOccluderAlongPlusX(new Vector4(3, 0, 0, 1), 0.6f);
		light.initShadowing(null, null, world);
		light.generateShadowMap(world);

		assertNotNull("the +X face has geometry: it must have been generated", light.getFaceMap(PointLight.FACE_PLUS_X));
		assertNull("the -X face has no geometry: it must have been skipped", light.getFaceMap(PointLight.FACE_MINUS_X));
		assertNull("the +Y face has no geometry: it must have been skipped", light.getFaceMap(PointLight.FACE_PLUS_Y));
		assertNull("the -Y face has no geometry: it must have been skipped", light.getFaceMap(PointLight.FACE_MINUS_Y));
		assertNull("the +Z face has no geometry: it must have been skipped", light.getFaceMap(PointLight.FACE_PLUS_Z));
		assertNull("the -Z face has no geometry: it must have been skipped", light.getFaceMap(PointLight.FACE_MINUS_Z));

		// A skipped face still reads as "fully lit" (nothing could be there to cast a shadow), not
		// as an error or an exception.
		assertEquals(1f, light.shadowFactorAt(new Vector4(0, 8, 0, 1), Vector3.zAxis()), 0f);

		System.out.println("PASS testGenerateShadowMap_skipsFacesWithNoGeometry");
	}

	@Test
	public void testGenerateShadowMap_lightInsideTheBoundingBox_everyFaceIsStillConsidered() {
		System.out.println("***** Test PointLight shadows : light inside the scene's bounding box -- facesNeeded() must not wrongly skip a face *****");

		// The light sits INSIDE the box formed by the six occluders (closedRoom()): facesNeeded()'s
		// per-axis range then straddles zero on every axis, which must keep every face eligible --
		// see its Javadoc. Re-verifies the "closed room" scenario from a different angle: by
		// construction here every face DOES have geometry, so none should ever be (incorrectly)
		// skipped.
		PointLight light = new PointLight(new Vector4(0, 0, 0, 1), 20f);
		World world = closedRoom(3f, 0.6f);
		light.initShadowing(null, null, world);
		light.generateShadowMap(world);

		for (int face = 0; face < 6; face++) {
			assertNotNull("face " + face + " has an occluder and the light is inside the box: it must not be skipped",
					light.getFaceMap(face));
		}
		System.out.println("PASS testGenerateShadowMap_lightInsideTheBoundingBox_everyFaceIsStillConsidered");
	}

	@Test
	public void testGetDefaultShadowMapSize_isItsOwnPerFaceDefault_notShadowingLightsSingleMapOne() {
		System.out.println("***** Test PointLight shadows : default shadow map size is PointLight's own per-face default (512), resolving the audit's open decision *****");
		PointLight light = new PointLight(new Vector4(0, 0, 0, 1), 20f);
		assertEquals(PointLight.DEFAULT_SHADOW_MAP_SIZE, light.getDefaultShadowMapSize());
		assertEquals(512, light.getDefaultShadowMapSize());
		assertEquals(PointLight.DEFAULT_SHADOW_MAP_SIZE, light.getShadowMapSize());

		// getShadowMapWidth()/getShadowMapHeight() report one face's resolution (they are all equal:
		// every face is square) once initShadowing() has built the six faces.
		assertEquals(0, light.getShadowMapWidth()); // no face built yet
		light.initShadowing(null, null, closedRoom(3f, 0.6f));
		assertEquals(PointLight.DEFAULT_SHADOW_MAP_SIZE, light.getShadowMapWidth());
		assertEquals(PointLight.DEFAULT_SHADOW_MAP_SIZE, light.getShadowMapHeight());
		System.out.println("PASS testGetDefaultShadowMapSize_isItsOwnPerFaceDefault_notShadowingLightsSingleMapOne");
	}
}
