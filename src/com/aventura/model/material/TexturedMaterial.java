package com.aventura.model.material;

import java.awt.Color;

import com.aventura.engine.Fragment;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.triangle.Triangle;
import com.aventura.tools.tracing.Tracer;

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
 * A Material backed by a Texture. This is where the texture orientation
 * (Triangle.TEXTURE_ISOTROPIC / TEXTURE_VERTICAL / TEXTURE_HORIZONTAL) is now
 * handled — TriangleRasterizer only ever hands this class the raw, un-divided
 * homogeneous texture coordinates (Fragment.getTexU()/getTexV()/getTexW());
 * this class decides which of u, v (or both) to divide by w before sampling.
 *
 * ASSUMPTION: Texture.getInterpolatedColor(float u, float v) is the sampling
 * method, mirroring its use in the legacy Rasterizer. Adjust if the real
 * signature differs.
 *
 * @author Olivier BARRY
 * @since 2026
 *
 */
public class TexturedMaterial implements Material {

	private final Texture texture;
	private final int textureOrientation;
	private final Color specularColor;
	private final float specularExponent;
	private final float ambientReflectivity;

	public TexturedMaterial(Texture texture, int textureOrientation, Color specularColor, float specularExponent, float ambientReflectivity) {
		this.texture = texture;
		this.textureOrientation = textureOrientation;
		this.specularColor = specularColor;
		this.specularExponent = specularExponent;
		this.ambientReflectivity = ambientReflectivity;
	}

	@Override
	public Color baseColorAt(Fragment fragment) {

		if (!fragment.hasTexCoord()) {
			// Should not happen if this Material is only ever attached to triangles that do carry
			// texture coordinates, but fail loud-ish rather than silently returning black.
			if (Tracer.error) Tracer.traceError(this.getClass(), "TexturedMaterial.baseColorAt() called on a Fragment without texture coordinates.");
			return Color.MAGENTA;
		}

		float rawU = fragment.getTexU();
		float rawV = fragment.getTexV();
		float w = fragment.getTexW();

		float u, v;
		switch (textureOrientation) {
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

		try {
			return texture.getInterpolatedColor(u, v);
		} catch (Exception e) {
			// NOTE: the legacy Rasterizer caught this the same way (printStackTrace, then silently
			// left the pixel's texture color as whatever was left over from the previous pixel).
			// Here we log through Tracer and return a visible fallback instead, so a broken texture
			// sample is obvious in the render rather than silently blending stale data. Tell me if
			// you'd rather keep the original silent behavior for now.
			if (Tracer.error) Tracer.traceError(this.getClass(), "Error sampling texture at (" + u + ", " + v + "): " + e.getMessage());
			return Color.MAGENTA;
		}
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