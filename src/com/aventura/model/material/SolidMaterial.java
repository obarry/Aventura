package com.aventura.model.material;

import java.awt.Color;

import com.aventura.engine.Fragment;

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
 * ------------------------------------------------------------------------------
 * 
 * A Material with a uniform, flat color across the whole surface — no
 * texture. The Fragment is deliberately ignored by baseColorAt()/
 * specularColorAt(): the color is always the same regardless of where on the
 * surface it's evaluated.
 *
 * @author Olivier BARRY
 * @since 2026
 *
 */
public class SolidMaterial implements Material {

	private final Color diffuseColor;
	private final Color specularColor;
	private final float specularExponent;
	private final float ambientReflectivity;

	public SolidMaterial(Color diffuseColor, Color specularColor, float specularExponent, float ambientReflectivity) {
		this.diffuseColor = diffuseColor;
		this.specularColor = specularColor;
		this.specularExponent = specularExponent;
		this.ambientReflectivity = ambientReflectivity;
	}

	@Override
	public Color baseColorAt(Fragment fragment) {
		return diffuseColor;
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
}