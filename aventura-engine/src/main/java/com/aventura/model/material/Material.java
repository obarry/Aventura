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

/**
 * A Material answers a single question: what is the base (diffuse) color of a
 * surface at a given Fragment, before any light is applied, and how does that
 * surface react to light (specular color and exponent, ambient reflectivity)?
 *
 * This replaces the old "texture" boolean and the scattered Ka/Kd/Ks/exponent
 * parameters that used to be passed individually into rasterizeTriangle(): a
 * Material is now a single object attached to an Element, resolved once, and
 * handed to ShadingConsumer for the whole triangle.
 *
 * baseColorAt()/specularColorAt() take a Fragment (rather than just raw UV
 * coordinates) so that a future Material implementation can use whatever it
 * needs from it (world position for triplanar mapping, normal for a Fresnel
 * effect, etc.) without changing this interface.
 *
 * A surface may also emit light by itself: emissionAt() (none by default). The emission is added to the shaded
 * color, whatever the lights and the shadows: a screen, a dial, an indicator lamp glows in the dark.
 *
 * AbstractMaterial holds what the materials of the engine share (specular, ambient, emission, reading a map at the
 * texture coordinates of a fragment); SolidMaterial and TexturedMaterial differ only by their base color.
 *
 * @author Olivier BARRY
 * @since 2026
 *
 */
public interface Material {

	/** The surface's own color at this fragment, before lighting is applied (diffuse color, or a texture sample). */
	Color baseColorAt(Fragment fragment);

	/** The color used for this surface's specular highlights. */
	Color specularColorAt(Fragment fragment);

	/** Phong specular exponent ("shininess") — higher values produce a smaller, sharper highlight. */
	float specularExponent();

	/** Ka: how strongly this surface reacts to ambient light, typically in [0, 1]. */
	float ambientReflectivity();

	/**
	 * The light this surface emits by itself at this fragment, added to the shaded color after the lights: not lit by
	 * the lights, not darkened by the shadows (see AbstractMaterial.setEmission()). Black, or null, emits nothing.
	 *
	 * @return the emitted color, or null if this surface emits nothing (the default)
	 * @since Nov 2026
	 */
	default Color emissionAt(Fragment fragment) {
		return null;
	}

}