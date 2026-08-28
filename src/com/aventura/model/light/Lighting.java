package com.aventura.model.light;

import java.awt.Color;
import java.util.ArrayList;

import com.aventura.engine.Fragment;
import com.aventura.math.vector.Vector3;
import com.aventura.model.material.Material;
import com.aventura.tools.color.ColorTools;
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
 * Central system and management of all Lighting in Aventura
 * 
 * The lighting system is centralizing all Lights of a scene. Multiple lighting systems are required to render multiple scenes.
 * The Lighting (system) can have one or several lights with the following restrictions (current implementation):
 * - One (or no) Ambient Light for the whole scene
 * - One (or no) Directional Light for the whole scene (future possible evolution with multiple Directional lights)
 * - Multiple (or no) Point Lights (either pure Point Lights or Spot Lights that are specific Point Lights)
 * 
 * All created Lights should be registered here. This is where Rasterizer will find information about all light sources.
 * This is also where the shadowing (when shadoows are activated) will find the list of Lights to calculate the associated ShadowMaps
 * 
 * Future evolution: Lights should also be part of the "extended" World as they may need to be rendered as well if part of the scene (Point Lights or Spot Lights)
 *
 * @author Olivier BARRY
 * @since July 2016
 * 
 */

public class Lighting {
	
	// One single AmbientLight
	protected AmbientLight ambient;
	// One Directional light in first approach. Future evolution is to support multiple directional lights
	// Initialized eagerly (not lazily per-constructor) to avoid the NPE that hasDirectional() used
	// to be exposed to when called on a Lighting built with the no-arg constructor.
	protected ArrayList<DirectionalLight> directionalLights = new ArrayList<DirectionalLight>();
	// Multiple Point Lights (includes Point and Spot Lights since the 2nd one is a sub-classs of the first one).
	protected ArrayList<PointLight> pointLights = new ArrayList<PointLight>();
	// All point lights and directional lights are shadowing lights, let's have a list of them (used by Rasterizer)
	protected ArrayList<ShadowingLight> shadowingLights = new ArrayList<ShadowingLight>();

	// specularLight is a GLOBAL kill switch, complementary to Material's per-surface specular
	// control: this lets a scene force specular off everywhere (e.g. a "fast" quality mode, or to
	// isolate diffuse-only shading for debugging) regardless of what any individual Material
	// specifies. The two are combined in contributionOf() below: specular is computed only when
	// BOTH this flag is true AND the material's own specularExponent() > 0.
	protected boolean specularLight = false;
	
	public Lighting() {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating Lighting System without any Light.");
	}
	
	public Lighting(DirectionalLight directional) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating Lighting System with Directional Light : " + directional);
		this.shadowingLights.add(directional);
		this.directionalLights.add(directional);
	}
	
	public Lighting(AmbientLight ambient) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating Lighting System with Ambient Light : " + ambient);
		this.ambient = ambient;
	}
	
	public Lighting(DirectionalLight directional, AmbientLight ambient) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating Lighting System with Directional Light : " + directional + " and Ambient Light : " + ambient);
		this.shadowingLights.add(directional);
		this.directionalLights.add(directional);
		this.ambient = ambient;
	}
	
	// NOTE: these two constructors mirror the two above, adding the global specular kill switch.
	public Lighting(DirectionalLight directional, boolean specularLight) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating Lighting System with Directional Light : " + directional + "Specular " + (specularLight ? "enabled" : "disabled"));
		this.shadowingLights.add(directional);
		this.directionalLights.add(directional);
		this.specularLight = specularLight;
	}

	public Lighting(DirectionalLight directional, AmbientLight ambient, boolean specularLight) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating Lighting System with Ambient Light : " + ambient + "Specular " + (specularLight ? "enabled" : "disabled"));
		this.shadowingLights.add(directional);
		this.directionalLights.add(directional);
		this.specularLight = specularLight;
		this.ambient = ambient;
	}

	public boolean hasAmbient() {
		return ambient != null;
	}
	
	public boolean hasDirectional() {
		return !directionalLights.isEmpty();
	}
	
	public boolean hasPoint() {
		return !pointLights.isEmpty();
	}
	
	public boolean hasShadowing() {
		return !shadowingLights.isEmpty();
	}

	/** Global specular kill switch — see the specularLight field's comment for how this combines with Material. */
	public boolean hasSpecular() {
		return specularLight;
	}

	public void setSpecularLight(boolean specularLight) {
		this.specularLight = specularLight;
	}

	public AmbientLight getAmbientLight() {
		return ambient;
	}
	
	public void setAmbientLight(AmbientLight ambient) {
		this.ambient = ambient;
	}

	public ArrayList<DirectionalLight> getDirectionalLights() {
		return directionalLights;
	}
	
	public void addDirectionalLight(DirectionalLight directional) {
		this.directionalLights.add(directional);
		this.shadowingLights.add(directional);
	}
	
	public void addPointLight(PointLight pointLight) {
		this.pointLights.add(pointLight);
		this.shadowingLights.add(pointLight);
	}
	
	public ArrayList<PointLight> getPointLights() {
		return pointLights;
	}
	
	public ArrayList<ShadowingLight> getShadowingLights() {
		return shadowingLights;
	}

	// --------------------------------------------------------------------------------------------------------------------
	// Shading combination — added for the new Fragment-based rendering pipeline (ShadingConsumer).
	// This is where the old Rasterizer's computeShadedColor()/computeSpecularColor() combination
	// logic now lives, generalized to work from a Fragment + Material instead of a triangle's
	// pre-interpolated per-vertex colors.
	//
	// Formula (unchanged from the legacy Rasterizer, see its rasterizeScanLine() color combination
	// comment): K = D.T.A + SUM(Ci.D.T) + SUM(Ci.Si), where D.T is folded into Material.baseColorAt().
	// --------------------------------------------------------------------------------------------------------------------

	/**
	 * The ambient contribution at this fragment: A * baseColor. Zero (black) if there is no
	 * ambient light configured.
	 */
	public Color ambientContributionAt(Fragment fragment, Material material) {
		Color baseColor = material.baseColorAt(fragment);
		if (!hasAmbient()) {
			return ColorTools.multColor(baseColor, 0f);
		}
		Color ambientLightColor = ambient.getLightColorAtPoint(fragment.getWorldPosition());
		return ColorTools.multColors(ambientLightColor, baseColor);
	}

	/**
	 * The diffuse + specular contribution of a single light at this fragment, WITHOUT any shadow
	 * factor applied — the caller (ShadingConsumer) is responsible for querying
	 * ShadowingLight.shadowFactorAt(...) itself and weighting the result, since not every Light is
	 * a ShadowingLight (AmbientLight isn't) and shadows may be globally disabled for the scene.
	 *
	 * @param light            the light to evaluate (any Light except AmbientLight — pass ambient
	 *                         light through ambientContributionAt() instead)
	 * @param fragment         the fragment being shaded
	 * @param viewerDirection  normalized vector from the fragment towards the camera, used for the
	 *                         specular reflection term
	 * @param material         the surface's material at this fragment
	 */
	public Color contributionOf(Light light, Fragment fragment, Vector3 viewerDirection, Material material) {

		Color baseColor = material.baseColorAt(fragment);
		Vector3 normal = fragment.getNormal().normalize();

		Vector3 lightVector = light.getLightVectorAtPoint(fragment.getWorldPosition());
		float dotNL = lightVector.dot(normal);

		if (dotNL <= 0) {
			return ColorTools.multColor(baseColor, 0f); // Not lit by this light -- full dark contribution
		}

		Color lightColor = light.getLightColorAtPoint(fragment.getWorldPosition());

		// Diffuse term: Ci . D.T . dotNL
		Color diffuse = ColorTools.multColor(ColorTools.multColors(lightColor, baseColor), dotNL);

		Color specular = ColorTools.multColor(baseColor, 0f); // Dark by default (no specular)
		if (hasSpecular() && material.specularExponent() > 0) {
			// Reflection vector R = 2N.dotNL - L
			Vector3 reflection = normal.times(2 * dotNL).minus(lightVector);
			float dotRV = reflection.dot(viewerDirection);
			if (dotRV < 0) dotRV = 0;

			if (dotRV > 0) {
				float specularFactor = (float) Math.pow(dotRV, material.specularExponent());
				Color specularColor = material.specularColorAt(fragment);
				// Defensive null-check: a Material implementation that doesn't set an explicit
				// specular color (e.g. ported from legacy code where it defaulted to
				// DEFAULT_SPECULAR_COLOR at the call site rather than inside the color itself)
				// would otherwise NPE here. Treat a null specular color as "no specular"
				// rather than crash the whole render.
				if (specularColor != null) {
					// Ci . Si  (kept as a separate term from the diffuse one, per the documented
					// formula -- the legacy code actually multiplied the diffuse-shaded color into
					// this term too, which looked like an unintended coupling; flagging this as a
					// behavior change, see accompanying message)
					specular = ColorTools.multColor(ColorTools.multColors(lightColor, specularColor), specularFactor);
				}
			}
		}

		return ColorTools.addColors(diffuse, specular);
	}

}