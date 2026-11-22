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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import com.aventura.demo.Noise;
import com.aventura.model.texture.Texture;

/**
 * A code painted on a wall of a corridor, with a stencil: the name of the place (S-12, E-28...) and a smaller line
 * under it (the deck, the section). It is a {@link SweptSection.Decal}: a texture shown once on a piece of the wall.
 *
 * <h2>Painted on the plates, not stuck on them</h2>
 * The engine has no transparency: a texture with the letters alone, on a panel in front of the wall, would show its
 * background. So the texture of the decal is the wall itself: for each of its pixels, the point of the wall it covers is
 * found ({@link SweptSection#pictureToSurface}), and the pixel of the tiled texture of the wall at that point
 * ({@link SweptSection#tileCoordinates}) is copied. The plates, their seams and their rivets go on through the decal as
 * if it were not there. Then the letters are painted over it.
 *
 * <h2>The letters</h2>
 * They are drawn with Java2D ({@code Graphics2D.drawString()}) in a mask, in the font Black Ops One (SIL Open Font
 * License, in the resources of the demos with its license): a military stencil, its letters cut by the bridges of the
 * stencil. The mask is then mixed with the plates pixel by pixel, the paint worn at places (a noise lets the metal show
 * through), so that the code looks painted long ago. Java2D only draws the picture: the engine never sees it, it sees a
 * Texture.
 *
 * @since Nov 2026
 */
public class WallMarking {

	/** The font, read once from the resources of the demos. */
	public static final String FONT_RESOURCE = "/font/BlackOpsOne-Regular.ttf";
	private static Font stencil;

	/** Pixels per meter of the texture of a decal: a pixel is 5 mm. */
	static final int PIXELS_PER_METER = 200;

	/** The paints. */
	public static final Color YELLOW = new Color(236, 186, 52);
	public static final Color BLACK = new Color(40, 40, 42); // White would not show on the light plates
	public static final Color RED = new Color(206, 58, 40);

	/** The height of the letters of the code and of the caption, in meters, and where the code stands on the wall. */
	static final float CODE_HEIGHT = 0.34f, CAPTION_HEIGHT = 0.09f;
	/** Height of the middle of the code above the bottom of the wall (the wall starts 0.35 m above the floor). */
	static final float CODE_CENTER = 1.05f;

	private WallMarking() {
	}

	/** The stencil font (Black Ops One), loaded from the resources of the demos the first time. */
	public static synchronized Font stencilFont() {
		if (stencil == null) {
			try (InputStream in = WallMarking.class.getResourceAsStream(FONT_RESOURCE)) {
				if (in == null) throw new IllegalStateException("Font not found in the resources: " + FONT_RESOURCE);
				stencil = Font.createFont(Font.TRUETYPE_FONT, in);
			} catch (IOException | java.awt.FontFormatException e) {
				throw new IllegalStateException("Cannot read the font " + FONT_RESOURCE, e);
			}
		}
		return stencil;
	}

	/**
	 * Creates the texture of a marking on a piece of an edge of a SweptSection: the tiled texture of the edge, with the
	 * code and the caption painted over it.
	 *
	 * @param walls   the SweptSection of the walls
	 * @param edge    the edge of the wall
	 * @param from    distance along the path where the decal starts
	 * @param to      distance where it ends
	 * @param plates  the tiled texture of the edge
	 * @param code    the code, in large letters
	 * @param caption the line under it, in small letters, or null
	 * @param paint   the color of the paint
	 * @param seed    seed of the wear of the paint
	 */
	public static Texture create(SweptSection walls, int edge, float from, float to, Texture plates, String code, String caption, Color paint,
			int seed) {
		float[][] section = walls.section;
		float[] a = section[edge], b = section[(edge + 1) % section.length];
		float edgeLength = (float) Math.hypot(b[0] - a[0], b[1] - a[1]);
		int width = Math.round((to - from) * PIXELS_PER_METER), height = Math.round(edgeLength * PIXELS_PER_METER);

		// 1. The background: the plates of the wall, pixel for pixel, as they are around the decal
		int[] background = new int[width * height];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				float[] surface = walls.pictureToSurface(edge, from, to, (x + 0.5f) / width, (y + 0.5f) / height);
				float[] uv = walls.tileCoordinates(edge, surface[0], surface[1]);
				int tx = Math.floorMod((int) Math.floor(uv[0] * plates.getWidth()), plates.getWidth());
				int ty = Math.floorMod((int) Math.floor(uv[1] * plates.getHeight()), plates.getHeight());
				background[y * width + x] = plates.getColor(tx, ty).getRGB();
			}
		}

		// 2. The letters, white on black in a mask, drawn by Java2D
		BufferedImage mask = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		Graphics2D g = mask.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g.setColor(Color.WHITE);
		// The wall starts at the top of the lower chamfer: the code is placed from the bottom of the picture
		int codeCenter = height - Math.round(CODE_CENTER * PIXELS_PER_METER);
		drawCentered(g, code, CODE_HEIGHT, width, codeCenter);
		if (caption != null) drawCentered(g, caption, CAPTION_HEIGHT, width, codeCenter + Math.round((CODE_HEIGHT * 0.62f + CAPTION_HEIGHT) * PIXELS_PER_METER));
		g.dispose();

		// 3. The paint over the plates, worn: a noise takes it off at places, a finer one thins it
		Texture texture = new Texture(width, height);
		int[] coverage = mask.getRaster().getPixels(0, 0, width, height, (int[]) null);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				Color plate = new Color(background[y * width + x]);
				float ink = coverage[y * width + x] / 255f;
				if (ink > 0) {
					float wear = Noise.fbm(x / 18f, y / 18f, 0, 4, seed);
					float grain = Noise.hash(x, y, 1, seed);
					float paintLeft = Noise.smoothstep(0.24f, 0.36f, wear) * (0.8f + 0.2f * grain);
					// The paint takes the shade of the plate under it: lit or grimy, it is the same metal
					float shade = (plate.getRed() + plate.getGreen() + plate.getBlue()) / (3 * 255f) / 0.88f;
					Color painted = ShipTextures.shadeColor(paint, Math.min(1.1f, shade));
					// The rust of the streaks runs over the paint too
					if (plate.getRed() > plate.getBlue() + 25) painted = ShipTextures.mix(painted, plate, 0.6f);
					texture.setColor(x, y, ShipTextures.mix(plate, painted, ink * paintLeft));
				} else {
					texture.setColor(x, y, plate);
				}
			}
		}
		return texture.setName("marking " + code + " (computed)");
	}

	/** Draws a line of text, centered horizontally, its letters of the given height (meters) centered on centerY. */
	private static void drawCentered(Graphics2D g, String text, float letterHeight, int width, int centerY) {
		// The size of a font is about the height of its capitals divided by 0.7: measured, rather than assumed
		Font font = stencilFont().deriveFont(100f);
		float capitals = (float) font.createGlyphVector(g.getFontRenderContext(), "H").getVisualBounds().getHeight();
		font = font.deriveFont(100f * letterHeight * PIXELS_PER_METER / capitals);
		g.setFont(font);
		FontMetrics metrics = g.getFontMetrics();
		int textWidth = metrics.stringWidth(text);
		float capitalHeight = letterHeight * PIXELS_PER_METER;
		g.drawString(text, (width - textWidth) / 2f, centerY + capitalHeight / 2);
	}
}
