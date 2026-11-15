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

/**
 * Tests of the mipmaps of a Texture: the pyramid of levels (Texture.buildMipmaps()), the choice of a level from the
 * size of a pixel (Texture.mipmapLevel()) and the trilinear filtering (Texture.getInterpolatedColor(s, t, log2PixelSize)).
 */
public class TestTextureMipmaps {

	/** A checker of black and white texels: the worst case, the color changes at every texel */
	private static Texture checker(int width, int height) {
		Texture texture = new Texture(width, height);
		for (int y = 0; y < height; y++)
			for (int x = 0; x < width; x++)
				texture.setColor(x, y, (x + y) % 2 == 0 ? Color.WHITE : Color.BLACK);
		return texture;
	}

	private static float log2(double x) {
		return (float) (Math.log(x) / Math.log(2));
	}

	@Test
	public void testNoMipmapsUntilBuilt() {
		Texture texture = checker(8, 8);
		assertFalse(texture.hasMipmaps());
		assertEquals(1, texture.getNbMipmapLevels());
		assertEquals(4L * 64, texture.getMemorySize());
	}

	@Test
	public void testLevelsHalveDownToOneTexel() {
		Texture texture = checker(8, 8);
		texture.buildMipmaps();
		assertTrue(texture.hasMipmaps());
		assertEquals(4, texture.getNbMipmapLevels());
		int[] sizes = { 8, 4, 2, 1 };
		for (int level = 0; level < 4; level++) {
			assertEquals(sizes[level], texture.getMipmapWidth(level));
			assertEquals(sizes[level], texture.getMipmapHeight(level));
		}
		assertEquals("one third more memory", 4L * (64 + 16 + 4 + 1), texture.getMemorySize());
	}

	@Test
	public void testLevelsOfARectangleAndOfAnOddSize() {
		Texture wide = checker(8, 2);
		wide.buildMipmaps();
		assertEquals(4, wide.getNbMipmapLevels());
		assertEquals(4, wide.getMipmapWidth(1));
		assertEquals(1, wide.getMipmapHeight(1));
		assertEquals(2, wide.getMipmapWidth(2));
		assertEquals("the height stays 1 while the width is halved", 1, wide.getMipmapHeight(2));
		assertEquals(1, wide.getMipmapWidth(3));

		Texture odd = checker(5, 3);
		odd.buildMipmaps();
		assertEquals(3, odd.getNbMipmapLevels());
		assertEquals(2, odd.getMipmapWidth(1));
		assertEquals(1, odd.getMipmapHeight(1));
		assertEquals(1, odd.getMipmapWidth(2));

		Texture single = checker(1, 1);
		single.buildMipmaps();
		assertEquals(1, single.getNbMipmapLevels());
	}

	@Test
	public void testALevelIsTheAverageOfTheLevelBelow() {
		// 2 x 2 blocks of plain colors: level 1 is these 4 colors
		Texture texture = new Texture(4, 4);
		Color[] blocks = { new Color(200, 0, 0), new Color(0, 100, 0), new Color(0, 0, 60), new Color(40, 80, 120) };
		for (int y = 0; y < 4; y++)
			for (int x = 0; x < 4; x++)
				texture.setColor(x, y, blocks[(y / 2) * 2 + x / 2]);
		texture.buildMipmaps();
		float pixelOfLevel1 = log2(1.0 / 2), pixelOfLevel2 = log2(1.0);
		assertEquals(new Color(200, 0, 0), texture.getInterpolatedColor(0.25f, 0.25f, pixelOfLevel1));
		assertEquals(new Color(0, 100, 0), texture.getInterpolatedColor(0.75f, 0.25f, pixelOfLevel1));
		assertEquals(new Color(0, 0, 60), texture.getInterpolatedColor(0.25f, 0.75f, pixelOfLevel1));
		assertEquals(new Color(40, 80, 120), texture.getInterpolatedColor(0.75f, 0.75f, pixelOfLevel1));
		// Level 2 is the average of the 4 blocks, wherever it is read
		assertEquals(new Color(60, 45, 45), texture.getInterpolatedColor(0.1f, 0.9f, pixelOfLevel2));
	}

	@Test
	public void testMipmapLevelFollowsTheSizeOfAPixel() {
		Texture texture = checker(256, 256);
		assertEquals("a pixel of the size of a texel", 0f, texture.mipmapLevel(log2(1.0 / 256)), 1e-4f);
		assertEquals("a pixel of 2 texels", 1f, texture.mipmapLevel(log2(2.0 / 256)), 1e-4f);
		assertEquals("a pixel of 8 texels", 3f, texture.mipmapLevel(log2(8.0 / 256)), 1e-4f);
		assertEquals("a pixel of half a texel: magnified", -1f, texture.mipmapLevel(log2(0.5 / 256)), 1e-4f);
		assertEquals("a rectangle: the average of its two sides", 1f, new Texture(64, 16).mipmapLevel(log2(2.0 / 32)), 1e-4f);
	}

	@Test
	public void testMagnifiedTextureIsReadAsWithoutMipmaps() {
		Texture texture = checker(16, 16);
		texture.buildMipmaps();
		for (float s = 0.03f; s < 1f; s += 0.11f) {
			for (float t = 0.05f; t < 1f; t += 0.13f) {
				assertEquals(texture.getInterpolatedColor(s, t), texture.getInterpolatedColor(s, t, log2(1.0 / 16)));
				assertEquals(texture.getInterpolatedColor(s, t), texture.getInterpolatedColor(s, t, log2(1.0 / 500)));
			}
		}
	}

	@Test
	public void testWithoutMipmapsTheTextureItselfIsRead() {
		Texture texture = checker(16, 16);
		assertEquals(texture.getInterpolatedColor(0.4f, 0.7f), texture.getInterpolatedColor(0.4f, 0.7f, log2(8.0 / 16)));
		assertFalse("reading does not build the mipmaps", texture.hasMipmaps());
	}

	@Test
	public void testMinifiedCheckerIsGray() {
		// Seen from far away, a black and white checker is gray. Without mipmaps it is black or white, almost at random
		Texture texture = checker(64, 64);
		texture.buildMipmaps();
		Color gray = new Color(128, 128, 128);
		for (float s = 0.02f; s < 1f; s += 0.07f) {
			assertEquals(gray, texture.getInterpolatedColor(s, 1f - s, log2(2.0 / 64)));
			assertEquals(gray, texture.getInterpolatedColor(s, 1f - s, log2(11.0 / 64)));
			assertEquals("a pixel larger than the texture", gray, texture.getInterpolatedColor(s, 1f - s, log2(500)));
		}
	}

	@Test
	public void testTrilinearBlendsTwoLevels() {
		// Left half black, right half white, 4 texels wide: level 1 is (black, white), level 2 is gray
		Texture texture = new Texture(4, 4);
		for (int y = 0; y < 4; y++)
			for (int x = 0; x < 4; x++)
				texture.setColor(x, y, x < 2 ? Color.BLACK : Color.WHITE);
		texture.buildMipmaps();
		// At the center of the left texel of level 1: black in level 1, gray (128) in level 2
		assertEquals(0, texture.getInterpolatedColor(0.25f, 0.5f, log2(2.0 / 4)).getRed());
		assertEquals(128, texture.getInterpolatedColor(0.25f, 0.5f, log2(4.0 / 4)).getRed());
		// Half way between the two levels (in logarithm): a pixel of 2 x sqrt(2) texels
		assertEquals(64, texture.getInterpolatedColor(0.25f, 0.5f, log2(2.0 * Math.sqrt(2) / 4)).getRed());
		// The color changes smoothly with the size of the pixel: no jump from a level to the next
		int previous = 0;
		for (float size = 2f; size <= 4f; size += 0.05f) {
			int red = texture.getInterpolatedColor(0.25f, 0.5f, log2(size / 4)).getRed();
			assertTrue(red >= previous && red - previous <= 8);
			previous = red;
		}
	}

	@Test
	public void testSetColorDiscardsTheMipmaps() {
		Texture texture = checker(4, 4);
		texture.buildMipmaps();
		texture.setColor(0, 0, Color.RED);
		assertFalse(texture.hasMipmaps());
		for (int y = 0; y < 4; y++)
			for (int x = 0; x < 4; x++)
				texture.setColor(x, y, Color.RED);
		texture.buildMipmaps();
		assertEquals("built again from the new texels", Color.RED, texture.getInterpolatedColor(0.5f, 0.5f, log2(4.0)));
	}
}
