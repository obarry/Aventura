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
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.Test;

/**
 * Tests of the loading of a Texture: from a file name, and from a URL, in particular a resource of the classpath
 * (what an application packaged as a jar does).
 */
public class TestTextureLoading {

	/** A small image of 3 x 2 pixels, each of its own color, saved as PNG (lossless) in a new temporary folder */
	private static File writeImage(String fileName) throws IOException {
		BufferedImage image = new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB);
		int[][] colors = { { 0xFF0000, 0x00FF00, 0x0000FF }, { 0xFFFF00, 0x00FFFF, 0xFF00FF } };
		for (int y = 0; y < 2; y++)
			for (int x = 0; x < 3; x++)
				image.setRGB(x, y, colors[y][x]);
		Path folder = Files.createTempDirectory("aventura-texture");
		File file = folder.resolve(fileName).toFile();
		assertTrue(ImageIO.write(image, "png", file));
		file.deleteOnExit();
		folder.toFile().deleteOnExit();
		return file;
	}

	private static void assertSameTexels(Texture expected, Texture actual) {
		assertEquals(expected.getWidth(), actual.getWidth());
		assertEquals(expected.getHeight(), actual.getHeight());
		for (int y = 0; y < expected.getHeight(); y++)
			for (int x = 0; x < expected.getWidth(); x++)
				assertEquals("texel " + x + ", " + y, expected.getColor(x, y), actual.getColor(x, y));
	}

	@Test
	public void testUrlGivesTheSameTextureAsTheFile() throws IOException {
		File file = writeImage("tiles.png");
		Texture fromFile = new Texture(file.getPath());
		Texture fromUrl = new Texture(file.toURI().toURL());
		assertSameTexels(fromFile, fromUrl);
		assertEquals(3, fromUrl.getWidth());
		assertEquals(2, fromUrl.getHeight());
		assertEquals(new Color(0x00FF00), fromUrl.getColor(1, 0));
		assertEquals("named after the file", "tiles.png", fromUrl.getName());
	}

	@Test
	public void testUrlWithDirectionAndOrientation() throws IOException {
		File file = writeImage("tiles.png");
		Texture fromFile = new Texture(file.getPath(), Texture.TEXTURE_DIRECTION_HORIZONTAL, Texture.TEXTURE_ORIENTATION_OPPOSITE,
				Texture.TEXTURE_ORIENTATION_NORMAL);
		Texture fromUrl = new Texture(file.toURI().toURL(), Texture.TEXTURE_DIRECTION_HORIZONTAL, Texture.TEXTURE_ORIENTATION_OPPOSITE,
				Texture.TEXTURE_ORIENTATION_NORMAL);
		assertSameTexels(fromFile, fromUrl);
		assertEquals("width and height exchanged", 2, fromUrl.getWidth());
		assertEquals("tiles.png", fromUrl.getName());
	}

	@Test
	public void testResourceOfTheClasspath() throws IOException {
		// A folder put on a class path, as the resources of an application are (in its classes or in its jar)
		File file = writeImage("stone wall.png");
		try (URLClassLoader classPath = new URLClassLoader(new URL[] { file.getParentFile().toURI().toURL() }, null)) {
			URL resource = classPath.getResource("stone wall.png");
			assertNotNull(resource);
			Texture texture = new Texture(resource);
			assertSameTexels(new Texture(file.getPath()), texture);
			assertEquals("the name is decoded (%20 is a space)", "stone wall.png", texture.getName());
		}
	}

	@Test
	public void testMissingResourceFailsWithAClearMessage() {
		// getResource() returns null for a resource that does not exist
		URL missing = TestTextureLoading.class.getResource("/no/such/texture.jpg");
		assertNull(missing);
		try {
			new Texture(missing);
			fail("a null URL must be refused");
		} catch (TextureLoadException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("not found"));
		}
		try {
			new Texture(missing, Texture.TEXTURE_DIRECTION_VERTICAL, Texture.TEXTURE_ORIENTATION_NORMAL, Texture.TEXTURE_ORIENTATION_NORMAL);
			fail("a null URL must be refused");
		} catch (TextureLoadException e) {
			// Expected
		}
	}

	@Test(expected = TextureLoadException.class)
	public void testUrlOfSomethingElseThanAnImage() throws IOException {
		Path text = Files.createTempFile("aventura-texture", ".txt");
		Files.writeString(text, "not an image");
		text.toFile().deleteOnExit();
		new Texture(text.toUri().toURL());
	}
}
