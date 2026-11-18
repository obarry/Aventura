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
 * A fog: a volume of the Atmosphere, at a place of the World, where the air is loaded with a haze that dims what is
 * seen through it, and lights up with the color of the fog (a light grey by default). A camera that flies through a
 * bank of fog sees the objects fade and the sky itself disappear, which the Haze (a pure function of the distance to the camera) cannot do.
 *
 * The fog is not an Element: it does not belong to the tree of the Geometry and it does not move with it. It is
 * given in the coordinates of the World, to the Atmosphere (Atmosphere.addFog()), like the lights are given to the
 * Lighting. The subclasses give the shape (SphericalFog today); this class holds what the shapes share: a color and a
 * density.
 *
 * The density is expressed as a visibility, like in meteorology and like for the Haze: the distance in the densest
 * part of the fog at which the contrast of an object falls to 5%. The smaller, the thicker the fog.
 *
 * A shape gives its optical depth along a ray (the integral of the density), from which AtmosphereRenderer draws
 * the fog: the light that comes from the objects is multiplied by the transmittance exp(-optical depth) and completed
 * by the color of the fog.
 *
 * @author Olivier BARRY
 * @since 2026
 */
public abstract class Fog extends AtmosphericEffect {

	public static final float DEFAULT_VISIBILITY = 10f;
	public static final Color DEFAULT_COLOR = new Color(200, 205, 210);

	// The contrast of an object falls to e^-3 = 5% across the visibility: the extinction coefficient of the densest part is 3 / visibility
	protected static final double K_EXTINCTION = 3.0;

	protected float visibility = DEFAULT_VISIBILITY;
	protected Color color = DEFAULT_COLOR;

	protected Fog() {
	}

	protected Fog(float visibility) {
		setVisibility(visibility);
	}

	public float getVisibility() {
		return visibility;
	}

	/**
	 * @param visibility distance (scene units) in the densest part of the fog at which the contrast falls to 5%, greater than 0
	 */
	public Fog setVisibility(float visibility) {
		if (!(visibility > 0f)) throw new IllegalArgumentException("The visibility of a fog must be greater than 0: " + visibility);
		this.visibility = visibility;
		return this;
	}

	public Color getColor() {
		return color;
	}

	/**
	 * @param color the color of the fog (not null). A fog in front of a sky tends to look good with a color close to it, or a grey.
	 */
	public Fog setColor(Color color) {
		if (color == null) throw new IllegalArgumentException("The color of a fog cannot be null");
		this.color = color;
		return this;
	}

	/**
	 * @return the center of a sphere (a point, in the coordinates of the World) that contains the fog
	 */
	public abstract Vector4 getBoundingCenter();

	/**
	 * @return the radius of that sphere, in scene units. The fog is looked for in the part of the image where this sphere projects.
	 */
	public abstract float getBoundingRadius();

	/**
	 * The optical depth of the fog along a ray: the integral of the extinction coefficient of the air along the part
	 * of the ray between its origin and sMax. The transmittance of the fog is exp(-optical depth).
	 *
	 * The points of the ray are O + s.D, for s in [0, sMax]. D is not required to be normalized, the optical depth is
	 * the one of the real length of the path.
	 *
	 * @param ox   X of the origin of the ray (the eye)
	 * @param oy   Y of the origin
	 * @param oz   Z of the origin
	 * @param dx   X of the direction
	 * @param dy   Y of the direction
	 * @param dz   Z of the direction
	 * @param sMax the parameter of the end of the ray: where it meets a surface, Double.POSITIVE_INFINITY for a ray that goes to the sky
	 * @return the optical depth, 0 if the ray does not meet the fog
	 */
	public abstract double opticalDepth(double ox, double oy, double oz, double dx, double dy, double dz, double sMax);
}
