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
package com.aventura.engine;

import java.awt.Color;

import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.Lighting;
import com.aventura.model.light.ShadowingLight;
import com.aventura.model.material.Material;
import com.aventura.tools.color.RGBAccumulator;
import com.aventura.view.GUIView;

/**
 * The "normal rendering" FragmentConsumer: for each fragment, combines the
 * ambient contribution with each ShadowingLight's diffuse + specular
 * contribution (optionally weighted by that light's shadow factor), draws the
 * resulting pixel, and updates the ZBuffer.
 *
 * This replaces the per-pixel color-combination block that used to live
 * directly inside Rasterizer.rasterizeScanLine() (the "DTA + SUM(CiDT) +
 * SUM(CiSi)" formula) — the formula itself hasn't changed, it has just moved
 * to Lighting.ambientContributionAt()/contributionOf(), with this class only
 * orchestrating the loop over lights and the shadow weighting.
 *
 * @author Olivier BARRY
 * @since 2026
 *
 */
public class ShadingConsumer implements FragmentConsumer {

	private final Material material;
	private final Lighting lighting;
	private final Camera camera;
	private final ZBuffer zBuffer;
	private final GUIView view;
	private final boolean shadowsEnabled;

	// Reused across every pixel this consumer processes -- see its own class Javadoc, same
	// lifecycle pattern as Fragment.
	private final RGBAccumulator accumulator = new RGBAccumulator();

	public ShadingConsumer(Material material, Lighting lighting, Camera camera, ZBuffer zBuffer, GUIView view, boolean shadowsEnabled) {
		this.material = material;
		this.lighting = lighting;
		this.camera = camera;
		this.zBuffer = zBuffer;
		this.view = view;
		this.shadowsEnabled = shadowsEnabled;
	}

	@Override
	public void consume(Fragment fragment) {

		accumulator.reset();
		Vector4 position = fragment.getWorldPosition();

		// What does not depend on the light is computed once for the pixel, not once per light
		// (performance audit A2): the base color of the material (a texture lookup for a TexturedMaterial),
		// the unit normal, and the viewer direction (only needed by the specular term).
		Color baseColor = material.baseColorAt(fragment);
		Vector3 normal = fragment.getNormal().normalize();
		boolean specular = lighting.hasSpecular() && material.specularExponent() > 0;
		Vector3 viewerDirection = specular ? camera.getEye().minus(position).V3().normalize() : null;

		// Ambient term: D.T.A — independent of any light or shadow.
		lighting.accumulateAmbient(position, baseColor, accumulator);

		if (lighting.getShadowingLights() != null) {
			for (ShadowingLight light : lighting.getShadowingLights()) {

				// From the cheapest test to the most expensive one (performance audit N2): a light that
				// cannot light this point is skipped before its shadow map is read.
				Vector3 lightVector = light.getLightVectorAtPoint(position);
				float dotNL = lightVector.dot(normal);
				if (dotNL <= 0) {
					continue; // The surface faces away from this light
				}
				float intensity = light.getIntensity(position);
				if (intensity <= 0) {
					continue; // Out of the range of a point light, or out of the cone of a spot light
				}

				float lightFactor = 1f;
				if (shadowsEnabled) {
					lightFactor = light.shadowFactorAt(position, normal, dotNL);
					if (lightFactor <= 0) {
						continue; // Fully in shadow for this light -- nothing to add
					}
				}

				// lightFactor is 1 for a lit point, and between 0 and 1 at the edge of a soft shadow (see ShadowFilter)
				Color lightColor = light.getLightColorAtPoint(position, intensity);
				lighting.accumulateContribution(lightColor, baseColor, normal, lightVector, dotNL, viewerDirection,
						material, fragment, accumulator, lightFactor);
			}
		}

		view.drawPixel(fragment.getScreenX(), fragment.getScreenY(), accumulator.toRGB());
		zBuffer.update(fragment.getScreenX(), fragment.getScreenY(), fragment.getZ());
	}
}