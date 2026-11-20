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
package com.aventura.demo.ship;

import java.awt.Color;

import com.aventura.demo.DemoTextures;
import com.aventura.demo.Noise;
import com.aventura.model.texture.Texture;

/**
 * The textures of the ship, each one loaded or computed once and shared by all the Elements that use it.
 *
 * <p>Most of them are <b>computed</b>, pixel by pixel ({@code new Texture(width, height)} then
 * {@code setColor(x, y, color)}): the plates of the hull with their seams and rivets, the grating of the floor, the
 * stripes of the doors, the screens of the consoles. A computed texture is drawn exactly for the size it is repeated
 * at (a hull plate is one meter wide, its seam a few millimeters), and the same noise ({@link Noise}) makes all of
 * them look worn in the same way. Two of them are <b>photographs</b>, read from the resources of the demos
 * ({@link DemoTextures}): rust, whose colors are mixed into the streaks under the rivets of the hull.
 *
 * <p>The colors of the textures are close to white or gray: the Elements give them their color (a texture is
 * multiplied by the color of its Element, or of its triangle). The same plates are light gray on the walls of the
 * corridors and dark green on the ribs.
 *
 * <p>Every texture is repeated like tiles (the shapes of the ship give texture coordinates in meters divided by the size
 * of a tile, beyond 1): they are drawn so that their left and right edges, and their top and bottom edges, follow each
 * other without a seam (REPEAT mode). The photographs of TempleRuins are mirrored instead (MIRROR mode), which is fine
 * for stone but would turn the diagonal ridges of a tread plate into chevrons.
 *
 * @since Nov 2026
 */
public class ShipTextures {

	static final String RUST_FILE = "texture_rust_960x539.jpg";

	/** Size of the computed textures, in pixels (the side of a square), and the size of the tile they cover, in meters. */
	static final int PLATE_SIZE = 512;
	public static final float PLATE_TILE = 2f;   // 2 x 2 plates of 1 m
	static final int GRATING_SIZE = 256;
	public static final float GRATING_TILE = 1f;
	static final int STRIPES_SIZE = 128;
	static final int TREAD_SIZE = 256;
	public static final float TREAD_TILE = 0.6f;
	static final int SCREEN_WIDTH = 256, SCREEN_HEIGHT = 160;

	static final int SEED = 11;

	/** The plates of the hull (walls, ceilings, ribs): seams, rivets, grime and rust. */
	public final Texture hullPlates;
	/** The plates of the ceilings: the same, with the slots of the vents. */
	public final Texture ceilingPlates;
	/** The grating of the floors of the corridors. */
	public final Texture grating;
	/** The tread plate of the floors of the junctions and of the control room: raised lozenges in a herringbone pattern. */
	public final Texture tread;
	/** Yellow and black stripes, worn: the frames of the doors. */
	public final Texture hazardStripes;
	/** A door leaf: plates, a band of stripes, a small window. */
	public final Texture doorLeaf;
	/** Painted metal, chipped: the consoles, the brackets. */
	public final Texture paintedMetal;
	/** Screens of the consoles: a radar, curves, lines of text. */
	public final Texture screenRadar, screenGraphs, screenText;

	/** Loads the photographs and computes the other textures. */
	public ShipTextures() {
		Texture rust = DemoTextures.load(RUST_FILE);
		hullPlates = createPlates(PLATE_SIZE, rust, false, SEED).setName("hull plates (computed)");
		ceilingPlates = createPlates(PLATE_SIZE, rust, true, SEED + 1).setName("ceiling plates (computed)");
		grating = createGrating(GRATING_SIZE, SEED + 2).setName("grating (computed)");
		tread = createTreadPlate(TREAD_SIZE, SEED + 8).setName("tread plate (computed)");
		hazardStripes = createStripes(STRIPES_SIZE, SEED + 3).setName("hazard stripes (computed)");
		doorLeaf = createDoorLeaf(256, 512, SEED + 4).setName("door leaf (computed)");
		paintedMetal = createPaintedMetal(256, SEED + 5).setName("painted metal (computed)");
		screenRadar = createRadarScreen(SCREEN_WIDTH, SCREEN_HEIGHT).setName("screen: radar (computed)");
		screenGraphs = createGraphScreen(SCREEN_WIDTH, SCREEN_HEIGHT, SEED + 6).setName("screen: graphs (computed)");
		screenText = createTextScreen(SCREEN_WIDTH, SCREEN_HEIGHT, SEED + 7).setName("screen: text (computed)");

		for (Texture repeated : new Texture[] { hullPlates, ceilingPlates, grating, tread, hazardStripes, paintedMetal }) {
			repeated.setWrapMode(Texture.WrapMode.REPEAT);
		}
	}

	// ----- Computed textures -----

	/**
	 * The plates of the hull: a 2 x 2 grid of plates, 1 m each when the texture is repeated every PLATE_TILE meters.
	 * <ul>
	 * <li>Each plate has its own shade (a value of noise per plate), so that the wall is not uniform.</li>
	 * <li>The seams between the plates are dark lines, with a light edge below them (a bevel lit from above).</li>
	 * <li>Rivets line the seams; under some of them a streak of rust runs down, its colors taken from the photograph.</li>
	 * <li>A large noise adds grime, darker at the bottom of each plate.</li>
	 * </ul>
	 * The pattern repeats exactly at the edges of the texture (all the noises are periodic over the texture, or drawn
	 * per plate), so that the tiles join without a seam other than the seams of the plates.
	 *
	 * @param size  side of the square texture, in pixels
	 * @param rust  the photograph of rust, for the colors of the streaks
	 * @param vents true to add the slots of a vent in one plate out of four (the ceilings)
	 */
	public static Texture createPlates(int size, Texture rust, boolean vents, int seed) {
		Texture texture = new Texture(size, size);
		int plate = size / 2;            // Pixels per plate
		float px = 1f / plate;           // One pixel, in plates (a plate is 1 m: one pixel is 4 mm)
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				int col = x / plate, row = y / plate;
				float fx = (x % plate) * px, fy = (y % plate) * px;      // Position inside the plate, 0 to 1
				// Base shade of the plate, and large grime (periodic: the noise is read on a torus)
				float shade = 0.88f + 0.1f * Noise.hash(col, row, 0, seed);
				float grime = periodicNoise(x, y, size, 3, seed);
				float value = shade * (0.86f + 0.2f * grime);
				value *= 1 - 0.12f * Noise.smoothstep(0.55f, 1f, fy) ;  // Darker towards the bottom of the plate (y down)
				// Fine grain
				value *= 0.96f + 0.08f * Noise.hash(x, y, 1, seed);

				// Seams: dark lines at the edges of the plate, a light line just inside (bevel)
				float edge = Math.min(Math.min(fx, 1 - fx), Math.min(fy, 1 - fy));
				if (edge < 1.5f * px) value *= 0.35f;
				else if (edge < 3.5f * px) value *= 1.12f;

				// Rivets: small dots every 12.5 cm along the seams, 2.5 cm inside the plate
				float rivet = rivetDistance(fx, fy);
				Color color;
				if (rivet < 0.011f) {
					value *= rivet < 0.006f ? 1.2f : 0.6f; // A lit dome, with a dark ring
					color = gray(value);
				} else {
					color = gray(value);
					// A streak of rust under a rivet of the top row of some plates
					float streak = rustStreak(fx, fy, col, row, seed);
					if (streak > 0) {
						Color rustColor = rust.getColor(x * 7 % rust.getWidth(), y * 3 % rust.getHeight());
						color = mix(color, multiply(rustColor, value * 1.1f), 0.75f * streak);
					}
				}
				// Vent slots in one plate out of four of the ceiling
				if (vents && col == 1 && row == 0 && fx > 0.2f && fx < 0.8f && fy > 0.25f && fy < 0.75f) {
					float slot = (fy - 0.25f) / 0.5f * 9;               // 9 slots
					if (slot - (int) slot < 0.5f) color = gray(0.12f);
				}
				texture.setColor(x, y, color);
			}
		}
		return texture;
	}

	/** Distance (in plates) from a point of a plate to the nearest rivet. */
	static float rivetDistance(float fx, float fy) {
		float inset = 0.025f, step = 0.125f;
		float best = Float.MAX_VALUE;
		// The rivets of the four rows along the edges of the plate
		for (float line : new float[] { inset, 1 - inset }) {
			float along = Math.round((fx - 0.0625f) / step) * step + 0.0625f;
			best = Math.min(best, (float) Math.hypot(fx - along, fy - line));
			along = Math.round((fy - 0.0625f) / step) * step + 0.0625f;
			best = Math.min(best, (float) Math.hypot(fx - line, fy - along));
		}
		return best;
	}

	/** Strength (0 to 1) of a streak of rust running down from a rivet of the top row, for some rivets of some plates. */
	static float rustStreak(float fx, float fy, int col, int row, int seed) {
		float step = 0.125f;
		int rivet = Math.round((fx - 0.0625f) / step);
		if (Noise.hash(col * 8 + rivet, row, 2, seed) > 0.22f) return 0;   // Most rivets have no streak
		float center = rivet * step + 0.0625f;
		float length = 0.2f + 0.5f * Noise.hash(col * 8 + rivet, row, 3, seed);
		float down = fy - 0.03f;
		if (down < 0 || down > length) return 0;
		float width = 0.012f + 0.02f * down;                               // Wider as it runs down
		float across = Math.abs(fx - center) / width;
		if (across > 1) return 0;
		return (1 - across) * (1 - down / length);
	}

	/** The grating of a floor: bars 4 cm apart across, held by thicker bars every 25 cm along, the dark void below. */
	public static Texture createGrating(int size, int seed) {
		Texture texture = new Texture(size, size);
		float px = 1f / size; // One pixel, in meters (the texture covers 1 m)
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				float fx = x * px, fy = y * px;
				float bar = Math.abs((fy * 25) - Math.round(fy * 25)) * 2;    // 0 on a bar, 1 between two bars
				float beam = Math.abs((fx * 4) - Math.round(fx * 4)) * 8;     // 0 on a beam
				float value;
				if (beam < 0.12f) value = 0.75f;                             // The thick bars
				else if (bar < 0.3f) value = 0.85f - 0.4f * bar;              // The thin bars, rounded
				else value = 0.1f;                                            // The void under the grating
				value *= 0.85f + 0.3f * periodicNoise(x, y, size, 4, seed);   // Wear
				texture.setColor(x, y, gray(value));
			}
		}
		return texture;
	}

	/**
	 * A tread plate: raised lozenges, 8 x 8 of them over the texture, every other one turned by 90 degrees (a herringbone
	 * pattern). A lozenge is lit on its upper left side and in the shade on the lower right one: the relief is drawn in
	 * the texture, not modeled.
	 */
	public static Texture createTreadPlate(int size, int seed) {
		Texture texture = new Texture(size, size);
		int cells = 8;
		float cell = (float) size / cells;
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				int cx = (int) (x / cell), cy = (int) (y / cell);
				float fx = (x - cx * cell) / cell - 0.5f, fy = (y - cy * cell) / cell - 0.5f; // -0.5 to 0.5 in the cell
				// The axis of the lozenge: one diagonal or the other, alternately
				boolean turned = (cx + cy) % 2 == 0;
				float along = turned ? (fx + fy) : (fx - fy), across = turned ? (fx - fy) : (fx + fy);
				float lozenge = Math.abs(along) / 0.62f + Math.abs(across) / 0.16f; // < 1 inside
				float value = 0.62f + 0.1f * periodicNoise(x, y, size, 3, seed);
				if (lozenge < 1) {
					// The side of the ridge towards the upper left is lit, the other one in the shade
					float slope = across * (turned ? 1 : -1) + along * 0.2f;
					value *= 1.05f + (slope < 0 ? 0.25f : -0.25f) * Math.min(1, Math.abs(across) / 0.06f);
				} else if (lozenge < 1.15f) {
					value *= 0.8f; // A thin shadow around the ridge
				}
				value *= 0.95f + 0.1f * Noise.hash(x, y, 0, seed);
				texture.setColor(x, y, gray(value));
			}
		}
		return texture;
	}

	/** Diagonal yellow and black stripes, worn, 10 cm wide: a texture repeated every 40 cm. */
	public static Texture createStripes(int size, int seed) {
		Texture texture = new Texture(size, size);
		Color yellow = new Color(232, 182, 40), black = new Color(34, 32, 30);
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				// Two periods of stripes across the texture, at 45 degrees: the pattern repeats at the edges
				float stripe = ((x + y) * 4f / size) % 2;
				Color color = stripe < 1 ? yellow : black;
				// Wear: patches where the paint is gone, showing the gray metal
				float wear = periodicNoise(x, y, size, 4, seed);
				if (wear > 0.68f) color = mix(color, new Color(120, 118, 112), Noise.smoothstep(0.68f, 0.75f, wear));
				texture.setColor(x, y, shadeColor(color, 0.9f + 0.15f * Noise.hash(x, y, 0, seed)));
			}
		}
		return texture;
	}

	/**
	 * A door leaf, mapped once on the front of its Box (width x height): a plate with a frame, a band of stripes at mid
	 * height, a small window at the top, and the grime of the hands around the middle.
	 */
	public static Texture createDoorLeaf(int width, int height, int seed) {
		Texture texture = new Texture(width, height);
		Color yellow = new Color(232, 182, 40), black = new Color(34, 32, 30);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				float fx = (float) x / width, fy = (float) y / height;  // fy = 0 at the top of the image
				float value = 0.82f + 0.12f * Noise.fbm(fx * 6, fy * 12, 0, 3, seed);
				Color color = gray(value);
				// Frame of the leaf: a raised border
				float border = Math.min(Math.min(fx, 1 - fx), Math.min(fy, 1 - fy) * height / width);
				if (border < 0.04f) color = gray(value * 0.75f);
				else if (border < 0.055f) color = gray(value * 1.15f);
				// Band of stripes
				if (fy > 0.52f && fy < 0.6f) {
					float stripe = ((x + y) * 10f / width) % 2;
					color = shadeColor(stripe < 1 ? yellow : black, value);
				}
				// The window: a dark glass with a lighter reflection, in a thick frame
				float wx = (fx - 0.5f) / 0.22f, wy = (fy - 0.2f) / 0.1f;
				float window = Math.max(Math.abs(wx), Math.abs(wy));
				if (window < 1) color = mix(new Color(20, 28, 34), new Color(70, 90, 100), Noise.smoothstep(-1, 1, wx - wy));
				else if (window < 1.25f) color = gray(value * 0.55f);
				texture.setColor(x, y, color);
			}
		}
		return texture;
	}

	/**
	 * Painted metal: a smooth paint with a faint mottling, chipped along small patches where the bare metal shows
	 * through, lighter. The texture is stretched once over a face of a Box (a console, a pillar): its details are kept
	 * small, so that they read as wear and not as a pattern.
	 */
	public static Texture createPaintedMetal(int size, int seed) {
		Texture texture = new Texture(size, size);
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				float chips = periodicNoise(x, y, size, 16, seed);
				float value = 0.86f + 0.06f * periodicNoise(x, y, size, 5, seed + 1);
				Color color = gray(value);
				if (chips > 0.74f) color = mix(color, gray(1f), 0.6f * Noise.smoothstep(0.74f, 0.77f, chips)); // Bare metal
				texture.setColor(x, y, shadeColor(color, 0.96f + 0.08f * Noise.hash(x, y, 0, seed)));
			}
		}
		return texture;
	}

	/** A radar screen: rings, a cross, a sweep fading behind it, a few echoes. Green on black. */
	public static Texture createRadarScreen(int width, int height) {
		Texture texture = new Texture(width, height);
		Color phosphor = new Color(90, 255, 140);
		float cx = width / 2f, cy = height / 2f, radius = height * 0.45f;
		double sweep = Math.toRadians(35);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				float dx = x - cx, dy = y - cy, r = (float) Math.hypot(dx, dy);
				float light = 0.05f;
				if (r < radius) {
					light = 0.12f;
					// Rings every quarter of the radius, and the cross
					float ring = Math.abs(r / radius * 4 - Math.round(r / radius * 4)) * radius / 4;
					if (ring < 0.8f || Math.abs(dx) < 0.6f || Math.abs(dy) < 0.6f) light = 0.45f;
					// The sweep: bright on its line, fading counter-clockwise behind it
					double angle = Math.atan2(-dy, dx);
					double behind = ((sweep - angle) % (2 * Math.PI) + 2 * Math.PI) % (2 * Math.PI);
					light = Math.max(light, (float) Math.max(0, 1 - behind / 1.2) * 0.8f);
				}
				// Echoes
				for (float[] echo : new float[][] { { 0.55f, 0.3f }, { 0.25f, 0.7f }, { 0.7f, 0.65f } }) {
					if (Math.hypot(x - echo[0] * width, y - echo[1] * height) < 2.5f) light = 1;
				}
				if (Math.abs(r - radius) < 1) light = 0.6f;
				texture.setColor(x, y, multiply(phosphor, light));
			}
		}
		return texture;
	}

	/** A screen of curves: three traces on a grid, amber on black. */
	public static Texture createGraphScreen(int width, int height, int seed) {
		Texture texture = new Texture(width, height);
		Color amber = new Color(255, 170, 60);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				float light = 0.08f;
				if (x % 32 == 0 || y % 32 == 0) light = 0.22f;
				for (int curve = 0; curve < 3; curve++) {
					float base = height * (0.25f + 0.25f * curve);
					float value = base + height * 0.1f * (Noise.fbm(x / 30f, curve * 10, 0, 3, seed) - 0.5f) * 3;
					if (Math.abs(y - value) < 1.2f) light = 1 - 0.25f * curve;
				}
				texture.setColor(x, y, multiply(amber, light));
			}
		}
		return texture;
	}

	/** A screen of text: lines of "words" (blocks of pixels of random lengths), cyan on dark blue. */
	public static Texture createTextScreen(int width, int height, int seed) {
		Texture texture = new Texture(width, height);
		Color cyan = new Color(110, 220, 255), background = new Color(8, 20, 40);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int line = y / 9, inLine = y % 9;
				Color color = background;
				if (inLine >= 2 && inLine <= 6 && x > 8 && x < width - 8) {
					// Words: the hash of the position of a 6-pixel cell decides if it is a letter; spaces between words
					int cell = (x - 8) / 6;
					float lineLength = 0.4f + 0.6f * Noise.hash(line, 0, 0, seed);
					boolean letter = Noise.hash(cell, line, 1, seed) > 0.18f && (x - 8f) / (width - 16) < lineLength;
					boolean pixel = Noise.hash(x, y, 2, seed) > 0.35f && (x - 8) % 6 < 5;
					if (letter && pixel) color = line == 0 ? Color.WHITE : cyan;
				}
				texture.setColor(x, y, color);
			}
		}
		return texture;
	}

	// ----- Helpers -----

	/**
	 * Fractal noise that repeats over the texture: the noise is read on a circle for x and another for y (a torus in
	 * 4 dimensions would be exact; two angles mixed into 3 coordinates are enough for a texture of grime).
	 *
	 * @param scale number of large features across the texture
	 */
	static float periodicNoise(int x, int y, int size, float scale, int seed) {
		double ax = 2 * Math.PI * x / size, ay = 2 * Math.PI * y / size;
		float r = scale / (float) (2 * Math.PI);
		return Noise.fbm((float) (r * Math.cos(ax) + r * Math.sin(ay)), (float) (r * Math.sin(ax)), (float) (r * Math.cos(ay)), 4, seed);
	}

	static Color gray(float value) {
		int v = Math.max(0, Math.min(255, Math.round(value * 255)));
		return new Color(v, v, v);
	}

	static Color mix(Color a, Color b, float t) {
		t = Math.max(0, Math.min(1, t));
		return new Color(Math.round(a.getRed() + (b.getRed() - a.getRed()) * t), Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
				Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
	}

	static Color multiply(Color color, float factor) {
		return shadeColor(color, factor);
	}

	static Color shadeColor(Color color, float factor) {
		return new Color(Math.min(255, Math.max(0, Math.round(color.getRed() * factor))), Math.min(255, Math.max(0, Math.round(color.getGreen() * factor))),
				Math.min(255, Math.max(0, Math.round(color.getBlue() * factor))));
	}
}
