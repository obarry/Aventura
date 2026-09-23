package com.aventura.context;

import static org.junit.Assert.*;

import java.awt.Color;

import org.junit.Test;

import com.aventura.context.RenderContext.RenderingType;

/**
 * Tests for RenderContext: immutable presets, complete copy constructor, fluent setters, defaults.
 */
public class TestRenderContext {

	@Test
	public void testDefaults() {
		RenderContext r = new RenderContext();
		assertEquals(RenderingType.INTERPOLATE, r.getRenderingType());
		assertTrue(r.isBackFaceCulling());
		assertFalse(r.isRenderingLines());
		assertFalse(r.isDisplayLandmark());
		assertFalse(r.isDisplayNormals());
		assertFalse(r.isDisplayLight());
		assertFalse(r.isTextureProcessing());
		assertFalse(r.isShadowing());
		assertFalse(r.isFrozen());
	}

	@Test
	public void testPresetsAreImmutable() {
		RenderContext[] presets = { RenderContext.RENDER_STANDARD_PLAIN, RenderContext.RENDER_STANDARD_PLAIN_SHADOWS,
				RenderContext.RENDER_STANDARD_PLAIN_WITH_LANDMARKS, RenderContext.RENDER_STANDARD_INTERPOLATE,
				RenderContext.RENDER_STANDARD_INTERPOLATE_SHADOWS, RenderContext.RENDER_STANDARD_INTERPOLATE_WITH_LANDMARKS,
				RenderContext.RENDER_DEFAULT, RenderContext.RENDER_DEFAULT_ALL_ENABLED };
		for (RenderContext preset : presets) {
			assertTrue(preset.isFrozen());
			boolean before = preset.isShadowing();
			try {
				preset.setShadowing(!before);
				fail("A preset must not be modifiable");
			} catch (IllegalStateException expected) {
				// expected
			}
			assertEquals(before, preset.isShadowing());
		}
	}

	@Test
	public void testPresetsContent() {
		assertEquals(RenderingType.INTERPOLATE, RenderContext.RENDER_STANDARD_INTERPOLATE_SHADOWS.getRenderingType());
		assertTrue(RenderContext.RENDER_STANDARD_INTERPOLATE_SHADOWS.isShadowing());
		assertEquals(RenderingType.LINE, RenderContext.RENDER_DEFAULT_ALL_ENABLED.getRenderingType());
		assertTrue(RenderContext.RENDER_DEFAULT_ALL_ENABLED.isDisplayLandmark());
		assertTrue(RenderContext.RENDER_DEFAULT_ALL_ENABLED.isDisplayNormals());
		assertTrue(RenderContext.RENDER_DEFAULT_ALL_ENABLED.isDisplayLight());
	}

	@Test
	public void testCopyOfPresetIsMutableAndFluent() {
		RenderContext r = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE_SHADOWS)
				.setTextureProcessing(true)
				.setRenderingType(RenderingType.FLAT);
		assertFalse(r.isFrozen());
		assertTrue(r.isShadowing()); // copied (used to be lost by the copy constructor)
		assertTrue(r.isTextureProcessing());
		assertEquals(RenderingType.FLAT, r.getRenderingType());
		// The preset itself is untouched
		assertEquals(RenderingType.INTERPOLATE, RenderContext.RENDER_STANDARD_INTERPOLATE_SHADOWS.getRenderingType());
		assertFalse(RenderContext.RENDER_STANDARD_INTERPOLATE_SHADOWS.isTextureProcessing());
	}

	@Test
	public void testCopyConstructorCopiesEverything() {
		RenderContext r = new RenderContext(RenderingType.PLAIN)
				.setRenderingLines(true)
				.setDisplayLandmark(true)
				.setDisplayNormals(true)
				.setDisplayLight(true)
				.setBackFaceCulling(false)
				.setTextureProcessing(true)
				.setShadowing(true)
				.setLandmarkColors(Color.CYAN, Color.MAGENTA, Color.ORANGE)
				.setNormalsColor(Color.PINK)
				.setLightVectorsColor(Color.GRAY);
		RenderContext c = new RenderContext(r);
		assertEquals(RenderingType.PLAIN, c.getRenderingType());
		assertTrue(c.isRenderingLines());
		assertTrue(c.isDisplayLandmark());
		assertTrue(c.isDisplayNormals());
		assertTrue(c.isDisplayLight());
		assertFalse(c.isBackFaceCulling());
		assertTrue(c.isTextureProcessing());
		assertTrue(c.isShadowing());
		assertEquals(Color.CYAN, c.getLandmarkXColor());
		assertEquals(Color.MAGENTA, c.getLandmarkYColor());
		assertEquals(Color.ORANGE, c.getLandmarkZColor());
		assertEquals(Color.PINK, c.getNormalsColor());
		assertEquals(Color.GRAY, c.getLightVectorsColor());
		assertEquals(r.toString(), c.toString());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullRenderingTypeRejected() {
		new RenderContext().setRenderingType(null);
	}
}
