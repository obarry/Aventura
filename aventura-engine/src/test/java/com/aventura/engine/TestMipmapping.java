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
package com.aventura.engine;

import static org.junit.Assert.*;

import java.awt.Color;
import java.awt.image.BufferedImage;

import org.junit.Test;

import com.aventura.context.PerspectiveContext;
import com.aventura.context.RenderContext;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.AmbientLight;
import com.aventura.model.light.Lighting;
import com.aventura.model.perspective.PerspectiveType;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Trellis;
import com.aventura.view.SwingView;

/**
 * Tests of the mipmapping in the rendering pipeline: the size of a pixel on a textured triangle computed by the
 * TriangleRasterizer, and the images rendered with RenderContext.setMipmapping().
 */
public class TestMipmapping {

	private static final int TEXTURE_SIZE = 64;

	private static Vector4 uv(float u, float v) {
		return new Vector4(u, v, 0, 1);
	}

	private static float log2(double x) {
		return (float) (Math.log(x) / Math.log(2));
	}

	// ------------------------------------------------------------------
	// Size of a pixel on a triangle (TriangleRasterizer.texLog2PixelSize())
	// ------------------------------------------------------------------

	@Test
	public void testPixelSize_textureFacingTheScreen() {
		// The whole texture on a square of 100 x 100 pixels (half of it: a triangle): a pixel is 1/100 of the texture
		float size = TriangleRasterizer.texLog2PixelSize(0, 0, 100, 0, 0, 100, uv(0, 0), uv(1, 0), uv(0, 1), 1, 1, 1);
		assertEquals(log2(1.0 / 100), size, 1e-4f);
		// Twice as small on the screen: a pixel covers twice as much
		size = TriangleRasterizer.texLog2PixelSize(0, 0, 50, 0, 0, 50, uv(0, 0), uv(1, 0), uv(0, 1), 1, 1, 1);
		assertEquals(log2(1.0 / 50), size, 1e-4f);
		// The same wherever the triangle is and whatever the order of its corners
		size = TriangleRasterizer.texLog2PixelSize(30, 70, -20, 20, 30, 20, uv(0.5f, 0.5f), uv(0, 0), uv(0.5f, 0), 1, 1, 1);
		assertEquals(log2(1.0 / 100), size, 1e-4f);
	}

	@Test
	public void testPixelSize_stretchedTextureIsReadCoarser() {
		// The texture squeezed 4 times in one direction: a pixel covers 1/100 x 1/25 of it, 1/50 on average. A share
		// of the stretching (4) is added: size = average x sqrt(4) ^ TEX_STRETCH_SHARE
		float size = TriangleRasterizer.texLog2PixelSize(0, 0, 100, 0, 0, 25, uv(0, 0), uv(1, 0), uv(0, 1), 1, 1, 1);
		assertEquals(log2(1.0 / 50) + TriangleRasterizer.TEX_STRETCH_SHARE, size, 1e-4f);
		assertTrue("between the average and the largest size", size > log2(1.0 / 50) && size <= log2(1.0 / 25) + 1e-4f);
		// The stretching taken into account is limited
		size = TriangleRasterizer.texLog2PixelSize(0, 0, 1000, 0, 0, 1, uv(0, 0), uv(1, 0), uv(0, 1), 1, 1, 1);
		float average = log2(1.0 / Math.sqrt(1000));
		assertEquals(average + TriangleRasterizer.TEX_STRETCH_SHARE * 0.5f * log2(TriangleRasterizer.TEX_MAX_STRETCH), size, 1e-3f);
	}

	@Test
	public void testPixelSize_perspective() {
		// A triangle facing the camera at depth 4: Fragment adds 1.5 x log2(4) = 3 to the value at depth 1, which
		// must give the size computed without perspective
		Fragment fragment = new Fragment();
		float atUnitDepth = TriangleRasterizer.texLog2PixelSize(0, 0, 100, 0, 0, 100, uv(0, 0), uv(1, 0), uv(0, 1), 4, 4, 4);
		fragment.setScreen(10, 10, 4f);
		fragment.setTexScale(atUnitDepth, true);
		assertEquals(log2(1.0 / 100), fragment.getTexLog2PixelSize(), 1e-4f);
		// Farther on the same triangle (if it were tilted), a pixel covers more
		fragment.setScreen(10, 10, 8f);
		assertEquals(log2(1.0 / 100) + 1.5f, fragment.getTexLog2PixelSize(), 1e-4f);
		// Orthographic: the depth does not matter
		fragment.setTexScale(-5f, false);
		assertEquals(-5f, fragment.getTexLog2PixelSize(), 0f);
	}

	@Test
	public void testPixelSize_degenerateTriangle() {
		// No area on the screen, or in the texture: the finest level of detail, no NaN
		float size = TriangleRasterizer.texLog2PixelSize(0, 0, 100, 0, 50, 0, uv(0, 0), uv(1, 0), uv(0, 1), 1, 1, 1);
		assertTrue(size < -100f);
		size = TriangleRasterizer.texLog2PixelSize(0, 0, 100, 0, 0, 100, uv(0, 0), uv(1, 0), uv(0.5f, 0), 1, 1, 1);
		assertTrue(size < -100f);
	}

	// ------------------------------------------------------------------
	// Rendered images
	// ------------------------------------------------------------------

	/** A black and white checker, each texel a square of the checker */
	private static Texture checker() {
		Texture texture = new Texture(TEXTURE_SIZE, TEXTURE_SIZE);
		for (int y = 0; y < TEXTURE_SIZE; y++)
			for (int x = 0; x < TEXTURE_SIZE; x++)
				texture.setColor(x, y, (x + y) % 2 == 0 ? Color.WHITE : Color.BLACK);
		return texture;
	}

	/**
	 * Renders a square floor of 8 x 8 covered by the checker, seen from above at the given distance, without lights
	 * (UNLIT: the colors of the image are those of the texture), in an image of 200 x 200 pixels.
	 */
	private static BufferedImage renderFloor(Texture texture, float distance, boolean mipmapping, int threads) {
		System.setProperty("java.awt.headless", "true");
		World world = new World();
		world.setBackgroundColor(Color.BLUE);
		Trellis floor = new Trellis(8, 8, 4, 4, texture);
		floor.setColor(Color.WHITE);
		world.addElement(floor);
		world.build();

		PerspectiveContext perspective = new PerspectiveContext(1f, 1f, 1, 1000, PerspectiveType.FRUSTUM, 200);
		RenderContext context = new RenderContext(RenderContext.RENDER_DEFAULT);
		context.setRenderingType(RenderContext.RenderingType.UNLIT);
		context.setDisplayLandmark(false);
		context.setTextureProcessing(true);
		context.setMipmapping(mipmapping);
		context.setRenderThreads(threads);
		SwingView view = new SwingView(perspective);
		Camera camera = new Camera(new Vector4(0, 0, distance, 1), new Vector4(0, 0, 0, 1), Vector4.yAxis());
		RenderEngine engine = new RenderEngine(world, new Lighting(new AmbientLight(1f)), camera, context, perspective);
		engine.setView(view);
		engine.render();
		engine.close();
		return view.getImageView();
	}

	/** The contrast of the pixels of the floor: the standard deviation of their red component (0: a plain color) */
	private static double contrast(BufferedImage image) {
		double sum = 0, sumOfSquares = 0;
		int n = 0;
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				Color color = new Color(image.getRGB(x, y));
				if (color.equals(Color.BLUE)) continue; // Background
				sum += color.getRed();
				sumOfSquares += color.getRed() * color.getRed();
				n++;
			}
		}
		assertTrue("the floor is in the image", n > 100);
		double mean = sum / n;
		return Math.sqrt(Math.max(0, sumOfSquares / n - mean * mean));
	}

	private static void assertSameImage(BufferedImage expected, BufferedImage actual) {
		for (int y = 0; y < expected.getHeight(); y++)
			for (int x = 0; x < expected.getWidth(); x++)
				assertEquals("pixel " + x + ", " + y, expected.getRGB(x, y), actual.getRGB(x, y));
	}

	@Test
	public void testDisabledByDefault() {
		assertFalse(new RenderContext().isMipmapping());
		assertFalse(RenderContext.RENDER_DEFAULT.isMipmapping());
		assertTrue("copied by the copy constructor", new RenderContext(new RenderContext().setMipmapping(true)).isMipmapping());
		Texture texture = checker();
		renderFloor(texture, 60, false, 1);
		assertFalse("the mipmaps are only built when they are used", texture.hasMipmaps());
		renderFloor(texture, 60, true, 1);
		assertTrue(texture.hasMipmaps());
	}

	@Test
	public void testFarFloorIsNoisyWithoutMipmappingAndSmoothWith() {
		// At a distance of 60, the floor of 8 covers about 27 pixels: more than 2 texels of the checker per pixel
		Texture texture = checker();
		double without = contrast(renderFloor(texture, 60, false, 1));
		double with = contrast(renderFloor(texture, 60, true, 1));
		assertTrue("without mipmapping: black and white noise (contrast " + without + ")", without > 20);
		assertTrue("with mipmapping: gray (contrast " + with + ")", with < 5);
	}

	@Test
	public void testNearFloorIsTheSameWithAndWithoutMipmapping() {
		// At a distance of 2, a texel of the checker covers about 12 pixels: the texture is magnified, level 0 only
		Texture texture = checker();
		assertSameImage(renderFloor(texture, 2, false, 1), renderFloor(texture, 2, true, 1));
	}

	@Test
	public void testSameImageWithSeveralThreads() {
		Texture texture = checker();
		assertSameImage(renderFloor(texture, 25, true, 1), renderFloor(texture, 25, true, 4));
	}
}
