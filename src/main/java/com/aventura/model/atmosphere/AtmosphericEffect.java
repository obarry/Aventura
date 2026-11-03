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

/**
 * A phenomenon of the Atmosphere of a World: the root of the family of the atmospheric effects, like Light is for
 * the effects of the Lighting. Today there are two branches: the Haze (the distance fading, one per Atmosphere) and
 * the Fog (a volume at a place of the World, several per Atmosphere). The model is meant to grow with other
 * phenomena: clouds, rain or snow, a mist that hugs the ground, a layer of smoke...
 *
 * What every effect shares is the on/off flag. It is the switch of this one effect (a fog bank that the scene
 * designer puts out, for instance), under the switches of the Atmosphere for each branch (Atmosphere.setFog(false)
 * for all the fogs) and the global one of the RenderContext (RenderContext.setAtmosphere(false) for all).
 *
 * To add a phenomenon:
 * <ol>
 * <li>write its class here, extending AtmosphericEffect (or Fog for one more shape of fog);</li>
 * <li>give a branch to the Atmosphere (a field and a switch like Atmosphere.setHaze(), or a list like the fogs);</li>
 * <li>draw it in AtmosphereRenderer, as one more pass over the pixels, in the order of the physical layers: from the
 * farthest to the nearest to the camera;</li>
 * <li>give it a checkbox in SceneViewer and a test.</li>
 * </ol>
 *
 * @author Olivier BARRY
 * @since 2026
 */
public abstract class AtmosphericEffect {

	protected boolean enabled = true;

	/**
	 * @return true if the effect is drawn (it is by default)
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * @param enabled true to draw the effect, false to leave it out without removing it from the Atmosphere
	 */
	public AtmosphericEffect setEnabled(boolean enabled) {
		this.enabled = enabled;
		return this;
	}
}
