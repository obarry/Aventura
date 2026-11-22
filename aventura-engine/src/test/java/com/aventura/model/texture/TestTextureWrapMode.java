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
package com.aventura.model.texture;

import static org.junit.Assert.*;

import java.awt.Color;

import org.junit.Test;

import com.aventura.math.vector.Vector4;
import com.aventura.model.texture.SurfaceMap.WrapMode;
import com.aventura.model.world.shape.Trellis;
import com.aventura.model.world.triangle.Triangle;

/**
 * Tests of the wrap mode of a Texture (Texture.setWrapMode()): what it shows at texture coordinates outside of 0 to
 * 1, and of the repetition of a texture over a Trellis (Trellis.setTextureRepeat()).
 */
public class TestTextureWrapMode {

	/** 4 x 1 texels of plain colors, red, green, blue and white from left to right */
	private static final Color[] COLORS = { Color.RED, Color.GREEN, Color.BLUE, Color.WHITE };

	private static Texture stripes() {
		Texture texture = new Texture(4, 1);
		for (int x = 0; x < 4; x++) texture.setColor(x, 0, COLORS[x]);
		return texture;
	}

	/** The color at the center of a texel, counted from the left of the texture (texel 5 is the second of the next tile) */
	private static Color texel(Texture texture, int index) {
		return texture.getInterpolatedColor((index + 0.5f) / 4, 0.5f);
	}

	private static void assertTexels(Texture texture, int firstIndex, Color... expected) {
		for (int i = 0; i < expected.length; i++) {
			assertEquals("texel " + (firstIndex + i), expected[i], texel(texture, firstIndex + i));
		}
	}

	@Test
	public void testClampIsTheDefault() {
		Texture texture = stripes();
		assertEquals(WrapMode.CLAMP, texture.getWrapMode());
		assertTexels(texture, -3, Color.RED, Color.RED, Color.RED);
		assertTexels(texture, 0, COLORS);
		assertTexels(texture, 4, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE);
	}

	@Test
	public void testRepeat() {
		Texture texture = stripes().setWrapMode(WrapMode.REPEAT);
		assertEquals(WrapMode.REPEAT, texture.getWrapMode());
		assertTexels(texture, 0, COLORS);
		assertTexels(texture, 4, COLORS);
		assertTexels(texture, 40, COLORS);
		assertTexels(texture, -4, COLORS);
		assertTexels(texture, -2, Color.BLUE, Color.WHITE, Color.RED);
	}

	@Test
	public void testMirror() {
		Texture texture = stripes().setWrapMode(WrapMode.MIRROR);
		assertTexels(texture, 0, COLORS);
		assertTexels(texture, 4, Color.WHITE, Color.BLUE, Color.GREEN, Color.RED);
		assertTexels(texture, 8, COLORS);
		assertTexels(texture, -4, Color.WHITE, Color.BLUE, Color.GREEN, Color.RED);
		assertTexels(texture, -8, COLORS);
	}

	@Test
	public void testTheModesOnlyDifferOutsideOfTheTexture() {
		Texture clamp = stripes(), repeat = stripes().setWrapMode(WrapMode.REPEAT), mirror = stripes().setWrapMode(WrapMode.MIRROR);
		// From the center of the first texel to the center of the last one, the 4 texels read are inside the texture
		for (float s = 0.125f; s <= 0.875f; s += 0.01f) {
			assertEquals(clamp.getInterpolatedColor(s, 0.5f), repeat.getInterpolatedColor(s, 0.5f));
			assertEquals(clamp.getInterpolatedColor(s, 0.5f), mirror.getInterpolatedColor(s, 0.5f));
		}
	}

	@Test
	public void testInterpolationAtTheBorderOfATile() {
		// Exactly between two tiles (s = 1), the color is half of the texel on each side of the border
		Color whiteAndRed = new Color(255, 128, 128); // REPEAT: the last texel (white) then the first one (red)
		assertEquals(whiteAndRed, stripes().setWrapMode(WrapMode.REPEAT).getInterpolatedColor(1f, 0.5f));
		assertEquals("the same at any border", whiteAndRed, stripes().setWrapMode(WrapMode.REPEAT).getInterpolatedColor(3f, 0.5f));
		// MIRROR: the last texel on both sides, hence no seam
		assertEquals(Color.WHITE, stripes().setWrapMode(WrapMode.MIRROR).getInterpolatedColor(1f, 0.5f));
		assertEquals(Color.RED, stripes().setWrapMode(WrapMode.MIRROR).getInterpolatedColor(2f, 0.5f));
		// CLAMP: the last texel goes on
		assertEquals(Color.WHITE, stripes().getInterpolatedColor(1f, 0.5f));
	}

	@Test
	public void testMirrorIsContinuous() {
		// No jump of color anywhere over 3 tiles of a gradient, borders included (REPEAT would jump from white to black)
		Texture gradient = new Texture(16, 1);
		for (int x = 0; x < 16; x++) gradient.setColor(x, 0, new Color(x * 17, x * 17, x * 17));
		gradient.setWrapMode(WrapMode.MIRROR);
		int previous = gradient.getInterpolatedColor(0f, 0.5f).getRed();
		for (float s = 0.005f; s <= 3f; s += 0.005f) {
			int red = gradient.getInterpolatedColor(s, 0.5f).getRed();
			assertTrue("at " + s + ": " + previous + " then " + red, Math.abs(red - previous) <= 2);
			previous = red;
		}
	}

	@Test
	public void testWrapModeAppliesToTheMipmaps() {
		// A checker of 8 x 8 texels whose left half is darker: level 1 and above are read with the same wrap mode
		Texture texture = new Texture(8, 8);
		for (int y = 0; y < 8; y++)
			for (int x = 0; x < 8; x++)
				texture.setColor(x, y, x < 4 ? Color.BLACK : Color.WHITE);
		texture.setWrapMode(WrapMode.REPEAT);
		texture.buildMipmaps();
		float pixelOfLevel1 = (float) (Math.log(2.0 / 8) / Math.log(2));
		// Center of the first texel of level 1 (black), and the same place 1 and 5 tiles farther
		Color inside = texture.getInterpolatedColor(0.125f, 0.5f, pixelOfLevel1);
		assertEquals(Color.BLACK, inside);
		assertEquals(inside, texture.getInterpolatedColor(1.125f, 0.5f, pixelOfLevel1));
		assertEquals(inside, texture.getInterpolatedColor(5.125f, 0.5f, pixelOfLevel1));
		// MIRROR: the next tile is flipped, the white half comes first
		texture.setWrapMode(WrapMode.MIRROR);
		assertEquals(Color.WHITE, texture.getInterpolatedColor(1.125f, 0.5f, pixelOfLevel1));
		assertEquals(Color.BLACK, texture.getInterpolatedColor(2.125f, 0.5f, pixelOfLevel1));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullWrapModeIsRefused() {
		stripes().setWrapMode(null);
	}

	// ------------------------------------------------------------------
	// Trellis.setTextureRepeat()
	// ------------------------------------------------------------------

	private static float[] maxTextureCoordinates(Trellis trellis) {
		float maxU = 0, maxV = 0;
		for (Triangle t : trellis.getTriangles()) {
			for (Vector4 uv : new Vector4[] { t.getTexVec1(), t.getTexVec2(), t.getTexVec3() }) {
				assertTrue(uv.getX() >= 0 && uv.getY() >= 0);
				maxU = Math.max(maxU, uv.getX());
				maxV = Math.max(maxV, uv.getY());
			}
		}
		return new float[] { maxU, maxV };
	}

	@Test
	public void testTrellisCoversItsTextureOnceByDefault() {
		Trellis trellis = new Trellis(10, 6, 5, 3, stripes());
		trellis.build();
		assertArrayEquals(new float[] { 1f, 1f }, maxTextureCoordinates(trellis), 1e-6f);
	}

	@Test
	public void testTrellisRepeatsItsTexture() {
		Trellis trellis = new Trellis(10, 6, 5, 3, stripes());
		trellis.setTextureRepeat(4, 2.5f); // Neither related to the 5 x 3 cells, nor an integer on Y
		trellis.build();
		assertEquals(2 * 5 * 3, trellis.getNbTriangles());
		assertArrayEquals(new float[] { 4f, 2.5f }, maxTextureCoordinates(trellis), 1e-5f);
		// Each cell covers 4 / 5 of the texture on X and 2.5 / 3 on Y
		Triangle first = trellis.getTriangles().get(0);
		assertEquals(0.8f, first.getTexVec2().getX() - first.getTexVec1().getX(), 1e-6f);
	}
}
