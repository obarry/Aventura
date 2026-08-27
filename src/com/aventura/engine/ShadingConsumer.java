package com.aventura.engine;

import java.awt.Color;

import com.aventura.math.vector.Vector3;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.Lighting;
import com.aventura.model.light.ShadowingLight;
import com.aventura.model.material.Material;
import com.aventura.tools.color.ColorTools;
import com.aventura.view.GUIView;

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
 * DEPENDENCY NOTE: this class calls ShadowingLight.shadowFactorAt(Vector4),
 * which doesn't exist yet on ShadowingLight — it's part of the next step
 * (the Lighting/Light fixes + shadow factor addition). This class is
 * otherwise complete and won't need further changes once that method lands.
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

		// Ambient term: D.T.A — independent of any light or shadow.
		Color result = lighting.ambientContributionAt(fragment, material);

		// ASSUMPTION: Camera.getEye() returns a Vector4 (as used elsewhere, e.g. the legacy
		// specular viewer vector calculation) and Vector4 exposes .minus(Vector4).V3().
		Vector3 viewerDirection = camera.getEye().minus(fragment.getWorldPosition()).V3().normalize();

		if (lighting.getShadowingLights() != null) {
			for (ShadowingLight light : lighting.getShadowingLights()) {

				float shadowFactor = 1f;
				if (shadowsEnabled) {
					shadowFactor = light.shadowFactorAt(fragment.getWorldPosition());
					if (shadowFactor <= 0) {
						continue; // Fully in shadow for this light: no diffuse/specular to add
					}
				}

				Color contribution = lighting.contributionOf(light, fragment, viewerDirection, material);
				if (shadowFactor < 1f) {
					contribution = ColorTools.multColor(contribution, shadowFactor);
				}
				result = ColorTools.addColors(result, contribution);
			}
		}

		view.drawPixel(fragment.getScreenX(), fragment.getScreenY(), result);
		zBuffer.update(fragment.getScreenX(), fragment.getScreenY(), fragment.getZ());
	}
}