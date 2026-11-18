package com.aventura.demo;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.Test;

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
 * Checks that every image of the documentation can still be generated (DocumentationImages), so that a change of
 * a demo or of the API does not silently break the program regenerating them. Does not write any file.
 *
 * @author Olivier BARRY
 * @since 2026
 */
public class TestDocumentationImages {

	@Test
	public void testEveryImageIsGenerated() {
		System.out.println("***** Test DocumentationImages : every image of the documentation is rendered, with its size and some content *****");

		for (Map.Entry<String, Supplier<BufferedImage>> image : DocumentationImages.images().entrySet()) {
			BufferedImage img = image.getValue().get();
			String name = image.getKey();
			assertTrue(name + " too small: " + img.getWidth() + " x " + img.getHeight(), img.getWidth() >= 800 && img.getHeight() >= 260);

			// Not a blank image: at least 20 different colors on a coarse grid of samples (the UrbanScape images, in plain colors, have about 40)
			Set<Integer> colors = new HashSet<>();
			for (int x = 0; x < img.getWidth(); x += 8) {
				for (int y = 0; y < img.getHeight(); y += 8) {
					colors.add(img.getRGB(x, y));
				}
			}
			assertTrue(name + ": only " + colors.size() + " colors", colors.size() >= 20);
		}
	}

	@Test
	public void testFractalLandscapeIsReproducible() {
		System.out.println("***** Test DocumentationImages : the fractal landscape is the same for the same seed *****");

		BufferedImage a = DocumentationImages.fractalLandscape(DocumentationImages.FRACTAL_SEED).render();
		BufferedImage b = DocumentationImages.fractalLandscape(DocumentationImages.FRACTAL_SEED).render();
		for (int x = 0; x < a.getWidth(); x += 4) {
			for (int y = 0; y < a.getHeight(); y += 4) {
				assertEquals(a.getRGB(x, y), b.getRGB(x, y));
			}
		}
	}
}
