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
package com.aventura.demo.temple;

import com.aventura.demo.Noise;
import java.awt.Color;

import com.aventura.demo.DemoTextures;
import com.aventura.model.texture.Texture;

/**
 * The textures of the scene, each one loaded or computed once and shared by all the Elements that use it.
 *
 * <p>Two ways of getting a {@link Texture} are shown:
 * <ul>
 * <li><b>From an image file</b>: {@code new Texture(url)}, for the marble, the ground, the rock and the bark. The
 *     files are resources of the demos, read from the classpath ({@link DemoTextures}): they are found whatever the
 *     directory the program is run from, and from a jar as well.</li>
 * <li><b>Computed</b>: {@code new Texture(width, height)} creates an empty texture, filled pixel by pixel with
 *     {@code setColor(x, y, color)}. The sea ({@link #createSea}) and the foliage of the olive trees
 *     ({@link #createFoliage}) are made this way, from a noise.</li>
 * </ul>
 * A texture only gives the detail of a surface: its color is multiplied by the color of the Element (or of the
 * triangle), so the same marble image can be given a warmer or a darker tint from one block to the next.
 *
 * @since Nov 2026
 */
public class SceneTextures {

	static final String MARBLE_FILE = "texture_stone_1023x852.jpg";
	static final String GROUND_FILE = "texture_ground_stone_600x600.jpg";
	static final String ROCK_FILE = "texture_gray_concrete_wall_with_cracks_1024x1024.jpg";
	static final String BARK_FILE = "texture_barnwood_576x358.jpg";
	/** Size in pixels (the side of a square) and seed of the computed texture of the sea. */
	static final int SEA_SIZE = 512;
	static final int SEA_SEED = 3;
	/**
	 * Shades of the sea: the troughs of the waves, their crests, and the glints. They are multiplied by the tint of the
	 * sea Element (a medium blue, see TempleRuins.SEA_TINT), which is the color of the sea when the textures are
	 * switched off: the texture only makes it darker in the troughs, and leaves it as it is on the glints.
	 */
	static final Color SEA_DEEP = new Color(42, 108, 148);
	static final Color SEA_LIGHT = new Color(102, 178, 199);
	static final Color SEA_GLINT = new Color(236, 246, 255);

	/** Size in pixels (the side of a square) and seed of the computed texture of the foliage. */
	static final int FOLIAGE_SIZE = 256;
	static final int FOLIAGE_SEED = 5;
	/** Colors of the leaves of an olive tree: dark green in the shade of the tree, gray green, and silver. */
	static final Color LEAF_DARK = new Color(70, 86, 56);
	static final Color LEAF_GREEN = new Color(122, 142, 98);
	static final Color LEAF_SILVER = new Color(184, 196, 164);

	public final Texture marble;
	public final Texture ground;
	public final Texture rock;
	public final Texture bark;
	public final Texture sea;
	public final Texture foliage;

	/** Loads the image files and computes the other textures. */
	public SceneTextures() {
		marble = new Texture(DemoTextures.url(MARBLE_FILE));
		ground = new Texture(DemoTextures.url(GROUND_FILE));
		rock = new Texture(DemoTextures.url(ROCK_FILE));
		// The image shows horizontal planks: read with its two axes exchanged (TEXTURE_DIRECTION_HORIZONTAL), the grain
		// of the wood is vertical, along the trunks
		bark = new Texture(DemoTextures.url(BARK_FILE), Texture.TEXTURE_DIRECTION_HORIZONTAL, Texture.TEXTURE_ORIENTATION_NORMAL, Texture.TEXTURE_ORIENTATION_NORMAL);
		// A computed texture has no file name: it is given a name, for the statistics of the scene (see GeometryReport)
		sea = createSea(SEA_SIZE, SEA_SEED).setName("sea (computed)");
		foliage = createFoliage(FOLIAGE_SIZE, FOLIAGE_SEED).setName("foliage (computed)");

		// All these textures are repeated like tiles: over the ground and the sea (Trellis.setTextureRepeat()), around
		// the drums of the columns and the trunks (TaperedDrum), around the boulders and the foliage (Boulder). These
		// shapes give texture coordinates beyond 1; in MIRROR mode a Texture shows itself again there, every other
		// time flipped, so that two neighbouring tiles share the same border: no seam, even with a photograph that
		// was not made to be repeated.
		for (Texture repeated : new Texture[] { marble, ground, rock, bark, sea, foliage }) {
			repeated.setWrapMode(Texture.WrapMode.MIRROR);
		}
	}

	/**
	 * A computed texture: the sea seen from above, wavelets longer than wide, all in the same direction. The level of
	 * the water at each pixel is a fractal noise, stretched along the waves; the color goes from the dark shade of the
	 * troughs to the lighter one of the crests, and the highest crests carry a glint.
	 *
	 * Unlike a photograph, this texture has no remarkable detail (a streak of foam, a reflection): repeated over the
	 * whole sea, it shows no pattern.
	 *
	 * @param size side of the square texture, in pixels
	 * @param seed seed of the noise
	 */
	public static Texture createSea(int size, int seed) {
		Texture texture = new Texture(size, size);
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				// About 9 waves across the texture, three times longer (along x) than wide
				float level = Noise.fbm(x * 9f / size, y * 27f / size, 0, 4, seed);
				Color color = mix(SEA_DEEP, SEA_LIGHT, Noise.smoothstep(0.3f, 0.7f, level));
				color = mix(color, SEA_GLINT, Noise.smoothstep(0.66f, 0.8f, level));
				texture.setColor(x, y, color);
			}
		}
		return texture;
	}

	/**
	 * A computed texture: the leaves of an olive tree seen from a distance, small strokes of silver and dark green.
	 * Each pixel takes its color from two noises: a fine one, stretched in one direction for the look of leaves, that
	 * chooses between dark and light; and a coarse one that makes whole areas greener or more silver.
	 *
	 * @param size side of the square texture, in pixels
	 * @param seed seed of the noise
	 */
	public static Texture createFoliage(int size, int seed) {
		Texture texture = new Texture(size, size);
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				// Coordinates in "leaves": about 90 leaves across the texture, three times longer than wide, and
				// tilted (the x + y / 2 term) so that they are not aligned with the pixels
				float leafX = (x + y / 2f) * 90f / size, leafY = y * 30f / size;
				float leaves = Noise.fbm(leafX, leafY, 0, 2, seed);
				float areas = Noise.fbm(x * 9f / size, y * 9f / size, 9, 2, seed);

				// From dark to green to silver as the fine noise increases, the coarse noise shifting the balance
				float light = Noise.smoothstep(0.2f, 0.8f, leaves + (areas - 0.5f) * 0.35f);
				Color color = light < 0.5f
						? mix(LEAF_DARK, LEAF_GREEN, light * 2)
						: mix(LEAF_GREEN, LEAF_SILVER, light * 2 - 1);
				texture.setColor(x, y, color);
			}
		}
		return texture;
	}

	/**
	 * A smaller copy of a texture: each pixel of the copy is the average color of a block of factor x factor pixels of
	 * the source (the pixels of an incomplete block at the right and bottom borders are dropped).
	 *
	 * @param source the texture to reduce, unchanged
	 * @param factor number of pixels of the source for one pixel of the copy, on each axis
	 */
	public static Texture shrink(Texture source, int factor) {
		int width = source.getWidth() / factor, height = source.getHeight() / factor;
		Texture copy = new Texture(width, height);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int red = 0, green = 0, blue = 0;
				for (int dy = 0; dy < factor; dy++) {
					for (int dx = 0; dx < factor; dx++) {
						Color color = source.getColor(x * factor + dx, y * factor + dy);
						red += color.getRed();
						green += color.getGreen();
						blue += color.getBlue();
					}
				}
				int count = factor * factor;
				copy.setColor(x, y, new Color(red / count, green / count, blue / count));
			}
		}
		return copy;
	}

	/** The color a fraction t (in [0, 1]) of the way from a to b. */
	static Color mix(Color a, Color b, float t) {
		t = Math.max(0, Math.min(1, t));
		return new Color(
				Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
				Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
				Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
	}

	/** The color multiplied by a factor (below 1: darker, above: lighter, limited to white). */
	static Color shade(Color color, float factor) {
		return new Color(
				Math.min(255, Math.round(color.getRed() * factor)),
				Math.min(255, Math.round(color.getGreen() * factor)),
				Math.min(255, Math.round(color.getBlue() * factor)));
	}
}
