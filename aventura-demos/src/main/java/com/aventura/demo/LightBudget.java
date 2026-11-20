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
package com.aventura.demo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.aventura.math.vector.Vector4;
import com.aventura.model.light.Light;
import com.aventura.model.light.PointLight;

/**
 * Keeps only the lights near the camera switched on, among the many lights of a large scene: a "light budget". Shared
 * by the demos.
 *
 * <p>Each light switched on costs a little at each pixel (its contribution, and a lookup in its shadow map), and each
 * point light costs a shadow map of six faces. A ship with forty ceiling lights cannot afford to light every pixel with
 * all of them, and does not need to: the walls of a corridor hide the lights of the other corridors. So, before each
 * image, {@link #update(Vector4)} switches on the {@code maxLights} lights nearest to the camera
 * ({@link Light#setEnabled(boolean)}) and switches the others off. A light switched off keeps its shadow map: it costs
 * nothing until it is switched on again.
 *
 * <h2>No light pops in or out</h2>
 * Switching a light on or off at once, when it enters or leaves the budget, would make the scene flash. The lights are
 * faded instead: a light in the budget gets its full intensity only if it is clearly nearer than the first light left
 * out. Its intensity is
 * <pre>
 *   full intensity x min(1, (d(first light left out) - d(this light)) / fadeDistance)
 * </pre>
 * When the camera moves and a light is about to leave the budget, the next light is about as near: its intensity has
 * already gone down to almost 0. The light that comes in starts at 0 the same way. The change is continuous.
 *
 * <h2>The lights in front first</h2>
 * The lights are ranked by their distance to the camera, made longer for the lights behind it: a light behind the
 * camera lights less of what the camera sees than a light in front of it, as near. The distance is multiplied by
 * <pre>
 *   1 + behindPenalty x (1 - cos a) / 2      a: the angle between the line of sight and the direction of the light
 * </pre>
 * 1 for a light straight ahead, 1 + behindPenalty for a light straight behind. The distance is the straight distance:
 * it ignores the walls. A light behind a wall may take a place in the budget; the walls cast shadows, so it does not
 * light through them, but it is a place lost.
 *
 * @since Nov 2026
 */
public class LightBudget {

	/** A light managed by the budget, with its full intensity. */
	static class Entry {
		final PointLight light;
		final float fullIntensity;
		float distance;  // Ranking distance at the last update() (see the class comment)
		float fade = 1; // Share of the full intensity given at the last update()

		Entry(PointLight light, float fullIntensity) {
			this.light = light;
			this.fullIntensity = fullIntensity;
		}
	}

	private final List<Entry> entries = new ArrayList<Entry>();
	private int maxLights;
	private float fadeDistance;
	private float behindPenalty = 1;

	/**
	 * @param maxLights    the number of lights switched on at a time
	 * @param fadeDistance the distance over which a light fades in or out (see the class comment), in the units of the
	 *                     World
	 */
	public LightBudget(int maxLights, float fadeDistance) {
		this.maxLights = maxLights;
		this.fadeDistance = fadeDistance;
	}

	/**
	 * Adds a light to manage (spot lights are point lights too).
	 *
	 * @param fullIntensity the intensity of the light when it is not faded: the budget changes the intensity of the light
	 */
	public LightBudget add(PointLight light, float fullIntensity) {
		entries.add(new Entry(light, fullIntensity));
		return this;
	}

	/**
	 * How much farther a light behind the camera is considered (see the class comment): 0 to rank the lights by their
	 * distance only. 1 by default: a light straight behind counts as twice as far.
	 */
	public void setBehindPenalty(float behindPenalty) {
		this.behindPenalty = behindPenalty;
	}

	public int getMaxLights() {
		return maxLights;
	}

	/** Changes the number of lights switched on at a time (taken into account at the next update()). */
	public void setMaxLights(int maxLights) {
		this.maxLights = maxLights;
	}

	public int getNbLights() {
		return entries.size();
	}

	/**
	 * Switches on the lights nearest to the camera, the lights in front of it first, faded (see the class comment), and
	 * switches off the others. To be called before each image.
	 *
	 * @param eye    the position of the camera
	 * @param target the point it looks at
	 * @return the number of lights switched on
	 */
	public int update(Vector4 eye, Vector4 target) {
		Vector4 sight = target.minus(eye).normalize();
		for (Entry entry : entries) {
			Vector4 toLight = entry.light.getPosition().minus(eye);
			float distance = toLight.length();
			float cos = distance > 0 ? (toLight.getX() * sight.getX() + toLight.getY() * sight.getY() + toLight.getZ() * sight.getZ()) / distance : 1;
			entry.distance = distance * (1 + behindPenalty * (1 - cos) / 2);
		}
		// The nearest first
		Entry[] sorted = entries.toArray(new Entry[0]);
		Arrays.sort(sorted, Comparator.comparingDouble(e -> e.distance));
		int on = Math.min(maxLights, sorted.length);
		// The first light left out sets the fading; without one, nothing fades
		float limit = on < sorted.length ? sorted[on].distance : Float.MAX_VALUE;
		for (int i = 0; i < sorted.length; i++) {
			Entry entry = sorted[i];
			boolean enabled = i < on;
			entry.light.setEnabled(enabled);
			if (enabled) {
				entry.fade = fadeDistance > 0 ? Math.min(1, (limit - entry.distance) / fadeDistance) : 1;
				entry.light.setIntensity(entry.fullIntensity * entry.fade);
			}
		}
		return on;
	}

	/** Switches all the lights on, at their full intensity (to see the whole scene lit, whatever the cost). */
	public void enableAll() {
		for (Entry entry : entries) {
			entry.light.setEnabled(true);
			entry.light.setIntensity(entry.fullIntensity);
			entry.fade = 1;
		}
	}

	/**
	 * A line per light switched on, nearest first: its ranking distance at the last update() (see the class comment)
	 * and the share of its full intensity it was given. For the statistics.
	 */
	public String report() {
		StringBuilder report = new StringBuilder();
		entries.stream().filter(e -> e.light.isEnabled()).sorted(Comparator.comparingDouble(e -> e.distance)).forEach(e -> report.append(
				String.format(java.util.Locale.ROOT, "  %s, ranked at %.1f m, %.0f %% of its intensity%n", e.light.getClass().getSimpleName(), e.distance,
						100 * e.fade)));
		return report.toString();
	}
}
