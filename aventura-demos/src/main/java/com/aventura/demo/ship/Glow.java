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
import java.util.ArrayList;
import java.util.List;

import com.aventura.model.texture.Texture;
import com.aventura.model.world.Element;

/**
 * What glows in the ship by itself, whatever the lights: the screens, the lenses of the ceiling lights, the ring of the
 * control room, a few buttons, the warning lamp of the hatch. The engine adds the emission of a surface to its shaded
 * color (Element.setEmissionColor(), setEmissionMap()):
 * <pre>
 *   color of the pixel = ambient + lights (diffuse, specular, shadows) + emission
 * </pre>
 * <ul>
 * <li>a <b>screen</b> emits its own picture: the texture of the screen is also its emission map. Its dark background
 *     emits nearly nothing, its curves and its letters glow. The glass itself is darker (it reflects the lights a little):
 *     in the dark, only the picture is seen;</li>
 * <li>a <b>lamp</b> emits a uniform color: a lens under a fixture, the ring of light, a lit button.</li>
 * </ul>
 * The emission lights nothing: the lights of the ship (ShipLights) do. But a fixture whose light is out of the budget
 * still glows: from afar, the corridor is dotted with lamps, as it should be.
 *
 * <p>{@link Switch} switches all the emissions of a tree of Elements off and on again (Effects menu), to compare.
 *
 * @since Nov 2026
 */
public final class Glow {

	/** The color of the glass of a screen, tinting its picture where the lights reach it. */
	public static final Color SCREEN_GLASS = new Color(110, 110, 110);
	/** The brightness of the picture of a screen, as an emission (white: the picture as it is). */
	public static final Color SCREEN_EMISSION = new Color(215, 215, 215);

	private Glow() {
	}

	/** Makes a screen glow: its picture is its emission map, its glass darker. */
	public static void screen(Element screen, Texture picture) {
		screen.setColor(SCREEN_GLASS);
		screen.setEmissionMap(picture);
		screen.setEmissionColor(SCREEN_EMISSION);
	}

	/** Makes a lamp glow evenly, with the given color. */
	public static void lamp(Element lamp, Color color) {
		lamp.setEmissionColor(color);
	}

	/** A color darker by the given factor (0 to 1), e.g. for the glow of a colored button. */
	public static Color dim(Color color, float factor) {
		return new Color(Math.round(color.getRed() * factor), Math.round(color.getGreen() * factor), Math.round(color.getBlue() * factor));
	}

	/**
	 * All the emissions of a tree of Elements, found once, switched off and on again: the Elements keep their colors,
	 * only their emission color and map are removed, then given back.
	 */
	public static class Switch {

		private final List<Element> elements = new ArrayList<Element>();
		private final List<Color> colors = new ArrayList<Color>();
		private final List<Texture> maps = new ArrayList<Texture>();
		private boolean on = true;

		/** Finds the Elements of the tree that emit light (their emission as it is now: on). */
		public Switch(Element root) {
			collect(root);
		}

		private void collect(Element element) {
			if (element.getEmissionColor() != null || element.getEmissionMap() != null) {
				elements.add(element);
				colors.add(element.getEmissionColor());
				maps.add(element.getEmissionMap());
			}
			if (!element.isLeaf()) {
				for (Element sub : element.getSubElements()) collect(sub);
			}
		}

		public void setOn(boolean on) {
			this.on = on;
			for (int i = 0; i < elements.size(); i++) {
				elements.get(i).setEmissionColor(on ? colors.get(i) : null);
				elements.get(i).setEmissionMap(on ? maps.get(i) : null);
			}
		}

		public boolean isOn() {
			return on;
		}

		/** The number of Elements that glow. */
		public int size() {
			return elements.size();
		}

		/** The number of Elements that glow with an emission map (the screens). */
		public int getNbMaps() {
			int n = 0;
			for (Texture map : maps) if (map != null) n++;
			return n;
		}
	}
}
