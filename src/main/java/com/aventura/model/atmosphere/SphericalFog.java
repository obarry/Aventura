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
package com.aventura.model.atmosphere;

import java.awt.Color;

import com.aventura.math.vector.Vector4;

/**
 * A fog in a sphere: a center (a point of the World) and a radius of action. The density is the highest at the
 * center and goes down to nothing at the surface of the sphere, following a parabola:
 *
 * <pre>
 *     density(r) = sigma0 . (1 - r^2 / R^2),   sigma0 = 3 / visibility
 * </pre>
 *
 * so that the fog has no edge: it fades gently into the clear air, with no visible limit of the sphere, whatever the
 * direction of the view. The optical depth of a ray is computed exactly (the integral of the parabola is a cubic
 * polynomial): the image does not depend on any sampling. Through the center the optical depth is 4R/visibility: a
 * fog of radius 10 and visibility 10 lets 2% of the light through over its diameter, a fog of radius 10 and
 * visibility 40 is a light veil (37% goes through).
 *
 * @author Olivier BARRY
 * @since 2026
 */
public class SphericalFog extends Fog {

	private final Vector4 center = new Vector4(0f, 0f, 0f, 1f);
	private float radius;

	/**
	 * @param center the center of the fog, a point in the coordinates of the World
	 * @param radius the radius of action, greater than 0
	 */
	public SphericalFog(Vector4 center, float radius) {
		this(center, radius, DEFAULT_VISIBILITY);
	}

	/**
	 * @param center     the center of the fog, a point in the coordinates of the World
	 * @param radius     the radius of action, greater than 0
	 * @param visibility the visibility in the densest part (the center), see Fog
	 */
	public SphericalFog(Vector4 center, float radius, float visibility) {
		super(visibility);
		setCenter(center);
		setRadius(radius);
	}

	/**
	 * @return a copy of the center of the fog (a point)
	 */
	public Vector4 getCenter() {
		return new Vector4(center);
	}

	public SphericalFog setCenter(Vector4 c) {
		if (c == null) throw new IllegalArgumentException("The center of a fog cannot be null");
		center.setX(c.getX());
		center.setY(c.getY());
		center.setZ(c.getZ());
		center.setW(1f);
		return this;
	}

	public float getRadius() {
		return radius;
	}

	public SphericalFog setRadius(float radius) {
		if (!(radius > 0f)) throw new IllegalArgumentException("The radius of a fog must be greater than 0: " + radius);
		this.radius = radius;
		return this;
	}

	@Override
	public Vector4 getBoundingCenter() {
		return getCenter();
	}

	@Override
	public float getBoundingRadius() {
		return radius;
	}

	@Override
	public double opticalDepth(double ox, double oy, double oz, double dx, double dy, double dz, double sMax) {
		double cx = center.getX() - ox, cy = center.getY() - oy, cz = center.getZ() - oz; // From the origin to the center
		double a = dx * dx + dy * dy + dz * dz;
		if (a <= 0.0) return 0.0;
		double cd = cx * dx + cy * dy + cz * dz;
		double sc = cd / a; // Parameter of the point of the ray closest to the center
		double r2 = (double) radius * radius;
		double b2 = cx * cx + cy * cy + cz * cz - cd * cd / a; // Squared distance of the ray to the center
		if (b2 >= r2) return 0.0;
		double h = Math.sqrt((r2 - b2) / a); // Half-length of the chord, in parameter
		double s0 = Math.max(0.0, sc - h);
		double s1 = Math.min(sMax, sc + h);
		if (s1 <= s0) return 0.0;
		double len = Math.sqrt(a);
		double u0 = (s0 - sc) * len, u1 = (s1 - sc) * len; // Real signed distances from the closest point
		double sigma0 = K_EXTINCTION / visibility;
		return sigma0 * ((1.0 - b2 / r2) * (u1 - u0) - (u1 * u1 * u1 - u0 * u0 * u0) / (3.0 * r2));
	}

	@Override
	public String toString() {
		return "SphericalFog: " + (enabled ? "" : "(disabled) ") + "center " + center + ", radius " + radius + ", visibility " + visibility + ", color " + color;
	}
}
