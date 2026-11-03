package com.aventura.cookbook;

import com.aventura.math.vector.Vector4;
import com.aventura.model.atmosphere.Fog;

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
 * ------------------------------------------------------------------------------ *
 * A new kind of fog, written outside the engine (recipe 5 of docs/ATMOSPHERE_COOKBOOK.md): a mist that hugs the ground, a layer
 * of air between two altitudes, as wide as the World, whose density is the highest on the ground and goes down linearly to nothing
 * at the top of the layer. A Fog only has to give its optical depth along a ray and a sphere that contains it: the renderer
 * does the rest (the color of the pixels, the order of several fogs, the threads).
 *
 * With sigma0 = 3 / visibility, the density at the altitude z of the layer is sigma0 . (1 - (z - baseZ) / height). The visibility is
 * the one of a horizontal ray at the foot of the layer. Along a ray the altitude changes linearly with the parameter s, and so
 * does the density: the integral is the length of the part of the ray in the layer times the average of the densities at its two
 * ends, exact.
 *
 * @author Olivier BARRY
 * @since 2026
 */
public class GroundMist extends Fog {

	// A sphere that "contains" the layer: as the layer has no limit sideways, a very large one. The renderer then looks for the mist on the whole image.
	private static final float HUGE = 1e6f;

	private final float baseZ;
	private final float height;

	/**
	 * @param baseZ      the altitude of the ground, where the mist is the densest
	 * @param height     the thickness of the layer: the density is nil at baseZ + height
	 * @param visibility the visibility of a horizontal ray at the foot of the layer
	 */
	public GroundMist(float baseZ, float height, float visibility) {
		super(visibility);
		if (!(height > 0f)) throw new IllegalArgumentException("The height of the layer must be greater than 0: " + height);
		this.baseZ = baseZ;
		this.height = height;
	}

	@Override
	public Vector4 getBoundingCenter() {
		return new Vector4(0f, 0f, baseZ + height / 2f, 1f);
	}

	@Override
	public float getBoundingRadius() {
		return HUGE;
	}

	@Override
	public double opticalDepth(double ox, double oy, double oz, double dx, double dy, double dz, double sMax) {
		double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (length <= 0.0) return 0.0;
		double sigma0 = K_EXTINCTION / visibility;
		double top = baseZ + height;

		// The part [sa, sb] of the ray, with s in [0, sMax], that is in the layer
		double sa, sb;
		if (dz == 0.0) { // Horizontal: all or nothing, with the same density all the way
			if (oz < baseZ || oz > top) return 0.0;
			sa = 0.0;
			sb = sMax; // Infinite for the sky: the optical depth is infinite, the pixel gets the color of the mist
			double density = 1.0 - (oz - baseZ) / height;
			if (density <= 0.0) return 0.0; // At the very top of the layer, where there is nothing (and not 0 x infinity)
			return sigma0 * density * length * (sb - sa);
		}
		double t0 = (baseZ - oz) / dz, t1 = (top - oz) / dz;
		sa = Math.max(0.0, Math.min(t0, t1));
		sb = Math.min(sMax, Math.max(t0, t1));
		if (sb <= sa) return 0.0;

		double densityA = 1.0 - (oz + sa * dz - baseZ) / height; // Relative densities at the ends of the part
		double densityB = 1.0 - (oz + sb * dz - baseZ) / height;
		return sigma0 * length * (sb - sa) * (densityA + densityB) / 2.0;
	}

	@Override
	public String toString() {
		return "GroundMist: " + (enabled ? "" : "(disabled) ") + "from " + baseZ + " to " + (baseZ + height) + ", visibility " + visibility + ", color " + color;
	}
}
