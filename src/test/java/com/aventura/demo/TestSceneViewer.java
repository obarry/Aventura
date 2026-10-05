package com.aventura.demo;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.function.Supplier;

import org.junit.Test;

import com.aventura.context.RenderContext.RenderingType;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.light.AmbientLight;
import com.aventura.model.light.DirectionalLight;
import com.aventura.model.light.Lighting;
import com.aventura.model.light.PointLight;
import com.aventura.model.light.ShadowFilter;

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
 * The logic of SceneViewer that does not need a display: camera movement, zoom, state, scene names.
 *
 * @author Olivier BARRY
 * @since 2026
 */
public class TestSceneViewer {

	private static final float EPS = 1e-4f;
	private static final Vector4 POI = new Vector4(1, 2, 0.5f, 1);
	private static final Vector4 EYE = new Vector4(7, -4, 3.5f, 1);

	private static float distance(Vector4 a, Vector4 b) {
		return a.minus(b).V3().length();
	}

	@Test
	public void testOrbitEye_noMovementKeepsTheEye() {
		System.out.println("***** Test SceneViewer : no yaw, pitch or zoom leaves the camera where it is *****");

		Vector4 e = SceneViewer.orbitEye(EYE, POI, 0, 0, 1);
		assertEquals(EYE.getX(), e.getX(), EPS);
		assertEquals(EYE.getY(), e.getY(), EPS);
		assertEquals(EYE.getZ(), e.getZ(), EPS);
		assertEquals(1, e.getW(), 0);
	}

	@Test
	public void testOrbitEye_yawTurnsAroundTheVerticalOfThePoi() {
		System.out.println("***** Test SceneViewer : yaw keeps the height and the distance, and turns by the angle around the vertical *****");

		float yaw = (float) Math.toRadians(70);
		Vector4 e = SceneViewer.orbitEye(EYE, POI, yaw, 0, 1);
		assertEquals(EYE.getZ(), e.getZ(), EPS);
		assertEquals(distance(EYE, POI), distance(e, POI), EPS);
		double before = Math.atan2(EYE.getY() - POI.getY(), EYE.getX() - POI.getX());
		double after = Math.atan2(e.getY() - POI.getY(), e.getX() - POI.getX());
		assertEquals(yaw, after - before, EPS);
	}

	@Test
	public void testOrbitEye_pitchRaisesTheCameraAndIsLimited() {
		System.out.println("***** Test SceneViewer : pitch changes the elevation, never beyond 89 degrees *****");

		double elevation0 = SceneViewer.elevation(EYE, POI);
		Vector4 higher = SceneViewer.orbitEye(EYE, POI, 0, 0.2f, 1);
		assertTrue(higher.getZ() > EYE.getZ());
		assertEquals(elevation0 + 0.2, SceneViewer.elevation(higher, POI), EPS);
		assertEquals(distance(EYE, POI), distance(higher, POI), EPS);

		Vector4 top = SceneViewer.orbitEye(EYE, POI, 0, 10, 1);
		assertEquals(SceneViewer.MAX_ELEVATION, SceneViewer.elevation(top, POI), EPS);
		Vector4 bottom = SceneViewer.orbitEye(EYE, POI, 0, -10, 1);
		assertEquals(-SceneViewer.MAX_ELEVATION, SceneViewer.elevation(bottom, POI), EPS);
	}

	@Test
	public void testClampPitch() {
		System.out.println("***** Test SceneViewer : the accumulated pitch stops at the elevation limits *****");

		double elevation0 = Math.toRadians(30);
		assertEquals(0.1f, SceneViewer.clampPitch(0.1f, elevation0), EPS);
		assertEquals(SceneViewer.MAX_ELEVATION - elevation0, SceneViewer.clampPitch(5, elevation0), EPS);
		assertEquals(-SceneViewer.MAX_ELEVATION - elevation0, SceneViewer.clampPitch(-5, elevation0), EPS);
	}

	@Test
	public void testZoom() {
		System.out.println("***** Test SceneViewer : each wheel notch multiplies or divides the distance by the zoom step *****");

		assertEquals(1, SceneViewer.zoomFactor(0), EPS);
		assertEquals(SceneViewer.ZOOM_STEP, SceneViewer.zoomFactor(1), EPS);
		assertEquals(1, SceneViewer.zoomFactor(3) * SceneViewer.zoomFactor(-3), EPS);

		Vector4 e = SceneViewer.orbitEye(EYE, POI, 0, 0, SceneViewer.zoomFactor(2));
		assertEquals(distance(EYE, POI) * SceneViewer.ZOOM_STEP * SceneViewer.ZOOM_STEP, distance(e, POI), EPS);
	}

	@Test
	public void testViewState_userChoicesWinOverTheSceneDefaults() {
		System.out.println("***** Test SceneViewer : the scene's options only fill the ones the user did not choose *****");

		SceneViewer.ViewState s = SceneViewer.ViewState.of("scene").withOptions(RenderingType.FLAT, null, false, null);
		SceneViewer.ViewState d = s.withDefaults(RenderingType.INTERPOLATE, true, true, true);
		assertEquals(RenderingType.FLAT, d.type);
		assertEquals(Boolean.TRUE, d.shadows);
		assertEquals(Boolean.FALSE, d.textures);
		assertEquals(Boolean.TRUE, d.landmarks);
		assertEquals("scene", d.scene);
	}

	@Test
	public void testViewState_softShadowsOptionIsKeptAndDefaulted() {
		System.out.println("***** Test SceneViewer : the soft shadows option follows the camera and options changes, and the scene fills it only when undefined *****");

		SceneViewer.ViewState s = SceneViewer.ViewState.of("scene");
		assertNull(s.softShadows);
		SceneViewer.ViewState chosen = s.withSoftShadows(true).withCamera(1, 2, 3).withOptions(RenderingType.FLAT, true, true, true)
				.withDefaults(RenderingType.INTERPOLATE, false, false, false);
		assertEquals(Boolean.TRUE, chosen.softShadows);
		assertEquals(Boolean.TRUE, chosen.withSoftShadowsDefault(false).softShadows);
		assertEquals(Boolean.FALSE, s.withSoftShadowsDefault(false).softShadows);
		assertEquals(Boolean.TRUE, s.withSoftShadowsDefault(true).softShadows);
	}

	@Test
	public void testViewState_lightGlowOptionIsKeptAndDefaulted() {
		System.out.println("***** Test SceneViewer : the light glow option follows the other changes, and the scene fills it only when undefined *****");
		SceneViewer.ViewState s = SceneViewer.ViewState.of("urbanscape_street");
		assertNull(s.lightGlow);
		SceneViewer.ViewState chosen = s.withLightGlow(true).withCamera(1, 2, 3).withSoftShadows(true).withOptions(RenderingType.FLAT, true, true, true);
		assertEquals(Boolean.TRUE, chosen.lightGlow);
		assertEquals(Boolean.TRUE, chosen.withLightGlowDefault(false).lightGlow);
		assertEquals(Boolean.FALSE, s.withLightGlowDefault(false).lightGlow);
		assertEquals(Boolean.TRUE, s.withLightGlowDefault(true).lightGlow);
		assertEquals("Soft shadows are kept", Boolean.TRUE, chosen.withLightGlow(false).softShadows);
	}

	@Test
	public void testLightShafts_optionAndSceneDefault() {
		System.out.println("***** Test SceneViewer : the light shafts option follows the other changes, and the scene fills it only when undefined *****");
		SceneViewer.ViewState s = SceneViewer.ViewState.of("sun_street");
		assertNull(s.lightShafts);
		SceneViewer.ViewState chosen = s.withLightShafts(false).withCamera(1, 2, 3).withLightGlow(true).withOptions(RenderingType.FLAT, true, true, true);
		assertEquals(Boolean.FALSE, chosen.lightShafts);
		assertEquals(Boolean.FALSE, chosen.withLightShaftsDefault(true).lightShafts);
		assertEquals(Boolean.TRUE, s.withLightShaftsDefault(true).lightShafts);
		assertEquals("Light glow is kept", Boolean.TRUE, chosen.withLightShafts(true).lightGlow);

		DirectionalLight sun = new DirectionalLight(new Vector3(0, 0, -1), 1f);
		Lighting lighting = new Lighting(sun, new AmbientLight(0.1f));
		assertFalse(SceneViewer.hasLightShafts(lighting));
		sun.setAppearance(new com.aventura.model.light.LightAppearance().setShaftsGain(1f));
		assertTrue(SceneViewer.hasLightShafts(lighting));
		assertFalse(SceneViewer.hasLightShafts(null));
	}

	@Test
	public void testSoftShadows_appliedToEveryShadowingLight() {
		System.out.println("***** Test SceneViewer : the soft shadows option sets the filter of every light casting shadows *****");

		DirectionalLight sun = new DirectionalLight(new Vector3(0, 0, -1), 1f);
		PointLight lamp = new PointLight(new Vector4(0, 0, 5, 1), 20f);
		Lighting lighting = new Lighting(sun, new AmbientLight(0.1f));
		lighting.addPointLight(lamp);
		assertFalse("hard by default", SceneViewer.hasSoftShadows(lighting));

		SceneViewer.applySoftShadows(lighting, null);
		assertEquals(ShadowFilter.HARD, sun.getShadowFilter());
		SceneViewer.applySoftShadows(lighting, true);
		assertEquals(ShadowFilter.PCF_3X3, sun.getShadowFilter());
		assertEquals(ShadowFilter.PCF_3X3, lamp.getShadowFilter());
		assertTrue(SceneViewer.hasSoftShadows(lighting));
		lamp.setShadowFilter(ShadowFilter.HARD);
		assertFalse("not every light filters its shadows", SceneViewer.hasSoftShadows(lighting));
		SceneViewer.applySoftShadows(lighting, false);
		assertEquals(ShadowFilter.HARD, sun.getShadowFilter());
		assertFalse(SceneViewer.hasSoftShadows(null));
		assertFalse(SceneViewer.hasSoftShadows(new Lighting(new AmbientLight(0.1f))));
	}

	@Test
	public void testScenes_namesAndCameras() {
		System.out.println("***** Test SceneViewer : every scene of the documentation has a name and a camera to turn around *****");

		assertEquals("urbanscape_street", SceneViewer.sceneName("urbanscape_street.jpg"));
		Map<String, Supplier<DemoScene>> scenes = DocumentationImages.scenes();
		SceneViewer viewer = new SceneViewer(scenes, "spotlights");
		assertEquals(scenes.size(), viewer.getScenes().size());
		assertTrue(viewer.getScenes().containsKey("fractal_landscape"));

		DemoScene spot = viewer.getScenes().get("spotlights").get();
		assertTrue(distance(spot.getEye(), spot.getPoi()) > 1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testUnknownScene() {
		System.out.println("***** Test SceneViewer : an unknown scene name is rejected *****");
		new SceneViewer(DocumentationImages.scenes(), "no_such_scene");
	}
}
