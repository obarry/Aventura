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
package com.aventura.model.light;

/**
 * How a light is made visible in the picture (a light has no geometry: it is only a point, or a direction,
 * in the model, so it is drawn by a pass on the screen after the scene, see LightGlowRenderer). The modes
 * are exclusive: each one draws the same bright core, with more and more around it.
 * <ul>
 * <li>NONE: the light is invisible (the default of the engine until the glow is switched on).</li>
 * <li>EMISSIVE: only the bright core, at the position of the light. It needs a position, so it does not
 * apply to a DirectionalLight.</li>
 * <li>HALO: the core and a soft glow around it, fading when an object passes in front of the light. The
 * default of PointLight and SpotLight.</li>
 * <li>SUN: the disc and the halo of a light infinitely far away, for a DirectionalLight, at the place of the
 * sky where its direction points (seen only where the sky is visible, so hidden by the buildings and the
 * hills in front of it). The size is an angle (LightAppearance.setSunDiscAngle(), setSunGlowAngle()). The
 * default of DirectionalLight. A lens flare (ghosts on the axis between the sun and the center of the screen) is
 * drawn with it, see LightAppearance.setFlareGain(), and optionally light shafts, see LightAppearance.setShaftsGain().</li>
 * </ul>
 *
 * The mode in use for a light is the first one defined among: its own LightAppearance, the RenderContext
 * (RenderContext.setLightGlowMode()), and the default of its type (Light.getDefaultGlowMode()).
 *
 * @author Olivier BARRY
 * @since 2026
 */
public enum LightGlowMode {
	NONE,
	EMISSIVE,
	HALO,
	SUN
}
