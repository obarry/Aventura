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
package com.aventura.model.material;

import java.awt.Color;

import com.aventura.engine.Fragment;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.triangle.Triangle;
import com.aventura.tools.color.ColorTools;

/**
 * What the materials of the engine share: the specular color and exponent, the ambient reflectivity, the emission,
 * and the way a map is read at the texture coordinates of a fragment. A subclass gives the base color of the surface
 * (baseColorAt()): uniform ({@link SolidMaterial}) or read in a texture ({@link TexturedMaterial}).
 *
 * <h2>Emission</h2>
 * A surface emits light by itself, added to the shaded color whatever the lights and the shadows (setEmission()):
 * <pre>
 *   color of the pixel = ambient + sum of the lights (diffuse, specular, shadows) + emission
 *   emission           = emission color x emission map at (u, v)     (either may be missing)
 * </pre>
 * <ul>
 * <li>an <b>emission color</b> alone: the whole surface glows uniformly (a lamp, a neon tube);</li>
 * <li>an <b>emission map</b>: a {@link Texture} read at the texture coordinates of the surface, the same ones as its
 *     texture: only its bright texels glow, black emits nothing (the digits of a dial, the lines of a screen). The
 *     emission color, if any, tints it.</li>
 * </ul>
 * The emission map is a Texture like the base texture: the same class, read the same way ({@link #read}), only its
 * role differs (see SurfaceMap).
 *
 * @author Olivier BARRY
 * @since Nov 2026
 */
public abstract class AbstractMaterial implements Material {

	protected final Color specularColor;
	protected final float specularExponent;
	protected final float ambientReflectivity;

	// The emission (see setEmission()): both null for a surface that emits nothing
	protected Color emissionColor = null;
	protected Texture emissionMap = null;
	// How the texture coordinates are read for the emission map (see read())
	protected int emissionMapOrientation = Triangle.TEXTURE_ISOTROPIC;
	protected boolean emissionMapMipmapping = false;

	protected AbstractMaterial(Color specularColor, float specularExponent, float ambientReflectivity) {
		this.specularColor = specularColor;
		this.specularExponent = specularExponent;
		this.ambientReflectivity = ambientReflectivity;
	}

	@Override
	public Color specularColorAt(Fragment fragment) {
		return specularColor;
	}

	@Override
	public float specularExponent() {
		return specularExponent;
	}

	@Override
	public float ambientReflectivity() {
		return ambientReflectivity;
	}

	// ------------------------------------------------------------------
	// Emission
	// ------------------------------------------------------------------

	/**
	 * Makes this surface emit light by itself: a color, a map, or both (the color then tints the map).
	 *
	 * @param color       the emitted color, or the tint of the map; null: white for a map, no emission without a map
	 * @param map         the emission map, read at the texture coordinates of the fragment (black emits nothing); null
	 *                    for a uniform emission. It is ignored on a fragment without texture coordinates.
	 * @param orientation how the texture coordinates are interpolated (Triangle.TEXTURE_ISOTROPIC, _VERTICAL or
	 *                    _HORIZONTAL), as for the texture of the triangle
	 * @param mipmapping  true to read the map at the level of detail of the pixel; its mipmaps must be built
	 * @return this material, to chain the call
	 */
	public AbstractMaterial setEmission(Color color, Texture map, int orientation, boolean mipmapping) {
		this.emissionColor = color;
		this.emissionMap = map;
		this.emissionMapOrientation = orientation;
		this.emissionMapMipmapping = mipmapping;
		return this;
	}

	/**
	 * Makes this surface emit a uniform color by itself (see setEmission(Color, Texture, int, boolean)).
	 * @return this material, to chain the call
	 */
	public AbstractMaterial setEmission(Color color) {
		return setEmission(color, null, Triangle.TEXTURE_ISOTROPIC, false);
	}

	public Color getEmissionColor() {
		return emissionColor;
	}

	public Texture getEmissionMap() {
		return emissionMap;
	}

	/** @return true if this surface emits light by itself (an emission color or an emission map) */
	public boolean isEmissive() {
		return emissionColor != null || emissionMap != null;
	}

	@Override
	public Color emissionAt(Fragment fragment) {
		if (emissionMap == null || !fragment.hasTexCoord()) {
			return emissionColor; // Uniform, or nothing (null)
		}
		Color sample = read(emissionMap, fragment, emissionMapOrientation, emissionMapMipmapping);
		return emissionColor != null ? ColorTools.multColors(emissionColor, sample) : sample;
	}

	// ------------------------------------------------------------------
	// Reading a map at the texture coordinates of a fragment
	// ------------------------------------------------------------------

	/**
	 * Reads a map at the texture coordinates of a fragment: the base texture of a TexturedMaterial, the emission map.
	 *
	 * The rasterizer gives the raw, homogeneous texture coordinates (u/w, v/w and 1/w interpolated linearly on the
	 * screen); the orientation says which of them to divide by w: both for a perspective-correct texture
	 * (TEXTURE_ISOTROPIC), only u or only v for the textures that are interpolated linearly along one direction.
	 *
	 * @param map         the map to read
	 * @param fragment    the fragment, with texture coordinates (Fragment.hasTexCoord())
	 * @param orientation Triangle.TEXTURE_ISOTROPIC, TEXTURE_VERTICAL or TEXTURE_HORIZONTAL
	 * @param mipmapping  true to read the level of detail that suits the size of the pixel on the surface
	 * @return the color of the map, never null
	 */
	protected static Color read(Texture map, Fragment fragment, int orientation, boolean mipmapping) {
		float rawU = fragment.getTexU();
		float rawV = fragment.getTexV();
		float w = fragment.getTexW();

		float u, v;
		switch (orientation) {
		case Triangle.TEXTURE_VERTICAL:
			u = rawU / w;
			v = rawV;
			break;
		case Triangle.TEXTURE_HORIZONTAL:
			u = rawU;
			v = rawV / w;
			break;
		case Triangle.TEXTURE_ISOTROPIC:
		default:
			u = rawU / w;
			v = rawV / w;
			break;
		}
		return mipmapping
				? map.getInterpolatedColor(u, v, fragment.getTexLog2PixelSize())
				: map.getInterpolatedColor(u, v);
	}
}
