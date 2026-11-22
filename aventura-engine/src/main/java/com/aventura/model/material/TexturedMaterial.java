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
import com.aventura.tools.tracing.Tracer;

/**
 * A Material backed by a Texture, tinted by a diffuse color — matching the
 * legacy Rasterizer's behavior where the surface color (D) is ALWAYS
 * multiplied with the texture sample (T), never replaced by it: final base
 * color = D * T (or just T if D is null, see below).
 *
 * This is what a "colored Element built from several specifically-colored
 * Triangles" relies on: the same texture can be tinted differently per
 * Element/Triangle by varying only the diffuse color, without needing a
 * different texture asset.
 *
 * TriangleRasterizer only ever hands this class the raw, un-divided
 * homogeneous texture coordinates (Fragment.getTexU()/getTexV()/getTexW());
 * AbstractMaterial.read() decides which of u, v (or both) to divide by w before
 * sampling, based on textureOrientation.
 *
 * @author Olivier BARRY
 * @since 2026
 *
 */
public class TexturedMaterial extends AbstractMaterial {

	private final Texture texture;
	private final int textureOrientation;
	private final Color diffuseTint; // "D" -- may be null, see baseColorAt()
	private final boolean mipmapping;

	/**
	 * @param diffuseTint  the surface color (D) to multiply with every texture sample (T) --
	 *                     matches the legacy surfCol parameter's role even in textured mode.
	 *                     May be null, in which case the texture sample is used as-is (no tint),
	 *                     equivalent to D being pure white.
	 */
	public TexturedMaterial(Texture texture, int textureOrientation, Color diffuseTint, Color specularColor, float specularExponent, float ambientReflectivity) {
		this(texture, textureOrientation, diffuseTint, specularColor, specularExponent, ambientReflectivity, false);
	}

	/**
	 * @param mipmapping true to read the texture at the level of detail that suits the size of each pixel on the
	 *                   surface (see Texture.getInterpolatedColor(s, t, log2PixelSize)); the mipmaps of the texture
	 *                   must be built (Texture.buildMipmaps()). false reads the texture itself, whatever the distance.
	 */
	public TexturedMaterial(Texture texture, int textureOrientation, Color diffuseTint, Color specularColor, float specularExponent,
			float ambientReflectivity, boolean mipmapping) {
		super(specularColor, specularExponent, ambientReflectivity);
		this.mipmapping = mipmapping;
		this.texture = texture;
		this.textureOrientation = textureOrientation;
		this.diffuseTint = diffuseTint;
	}

	@Override
	public Color baseColorAt(Fragment fragment) {

		if (!fragment.hasTexCoord()) {
			// Should not happen if this Material is only ever attached to triangles that do carry
			// texture coordinates, but fail loud-ish rather than silently returning black.
			if (Tracer.error) Tracer.traceError(this.getClass(), "TexturedMaterial.baseColorAt() called on a Fragment without texture coordinates.");
			return Color.MAGENTA;
		}

		// The texture at the texture coordinates of the fragment, divided by w according to the orientation (see
		// AbstractMaterial.read(), which reads the emission map the same way)
		Color textureSample = read(texture, fragment, textureOrientation, mipmapping);

		// D * T -- diffuseTint (D) is ALWAYS applied when present, matching the legacy behavior;
		// null means "no tint", i.e. the texture is shown as-is (equivalent to D = white).
		return diffuseTint != null ? ColorTools.multColors(diffuseTint, textureSample) : textureSample;
	}
}
