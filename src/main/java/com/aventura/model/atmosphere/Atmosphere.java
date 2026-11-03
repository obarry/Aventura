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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The atmosphere of a World: the air between the camera and the objects, and its phenomena. It is organized like
 * the Lighting (one ambient light and several lights): a tree with two branches today,
 * <ul>
 * <li>the Haze: the fading with the distance to the camera, the same everywhere (one per Atmosphere);</li>
 * <li>the Fog: volumes at places of the World where the air is thick (several, each with a shape: SphericalFog).</li>
 * </ul>
 * Both are AtmosphericEffect and the model is meant to welcome other phenomena later (see AtmosphericEffect for how).
 *
 * It is a property of the scene (World.setAtmosphere()) and it is only drawn if RenderContext.setAtmosphere(true)
 * switches it on (see AtmosphereRenderer). Under that global switch, an application controls what is seen with the
 * switch of each branch, to a menu of its GUI for instance: setHaze(false) puts the haze out, setFog(false) puts out all
 * the fogs, and the switches of the effects themselves (AtmosphericEffect.setEnabled()) put out just one. The
 * switches are on by default and an effect is drawn when all the switches above it are on.
 *
 * <pre>
 * Atmosphere atmosphere = new Atmosphere(new Haze(220f).setStartDistance(8f));
 * atmosphere.addFog(new SphericalFog(new Vector4(0f, 20f, 2f, 1f), 12f, 15f));
 * world.setAtmosphere(atmosphere);
 * renderContext.setAtmosphere(true);
 * ...
 * atmosphere.setFog(false); // The haze stays, the fogs are out
 * </pre>
 *
 * @author Olivier BARRY
 * @since 2026
 */
public class Atmosphere {

	private Haze haze = null;
	private final List<Fog> fogs = new ArrayList<Fog>();
	private boolean hazeOn = true;
	private boolean fogOn = true;

	/**
	 * An atmosphere with no phenomenon: the air is perfectly clear. Add them with setHaze(Haze) and addFog(Fog).
	 */
	public Atmosphere() {
	}

	/**
	 * @param haze the haze of the atmosphere (null for none)
	 */
	public Atmosphere(Haze haze) {
		this.haze = haze;
	}

	// ----- Haze

	/**
	 * @return the haze of the atmosphere, null if it has none. Whether it is drawn or not is given by isHaze().
	 */
	public Haze getHaze() {
		return haze;
	}

	/**
	 * @param haze the haze of the atmosphere, null for none. The switch of the branch (setHaze(boolean)) is not changed.
	 */
	public Atmosphere setHaze(Haze haze) {
		this.haze = haze;
		return this;
	}

	/**
	 * The switch of the branch of the haze, on by default.
	 *
	 * @param on false to put the haze out, without removing it
	 */
	public Atmosphere setHaze(boolean on) {
		this.hazeOn = on;
		return this;
	}

	/**
	 * @return true if the haze is drawn: the atmosphere has one, the switch of the branch is on and the haze is enabled
	 */
	public boolean isHaze() {
		return hazeOn && haze != null && haze.isEnabled();
	}

	// ----- Fog

	/**
	 * Adds a fog. The fog is in the coordinates of the World and keeps the place it has: it is not linked to anything.
	 */
	public Atmosphere addFog(Fog fog) {
		if (fog == null) throw new IllegalArgumentException("The fog cannot be null");
		fogs.add(fog);
		return this;
	}

	public Atmosphere removeFog(Fog fog) {
		fogs.remove(fog);
		return this;
	}

	public Atmosphere clearFogs() {
		fogs.clear();
		return this;
	}

	/**
	 * @return all the fogs of the atmosphere, drawn or not, as a read-only list
	 */
	public List<Fog> getFogs() {
		return Collections.unmodifiableList(fogs);
	}

	/**
	 * The switch of the branch of the fogs, on by default. It puts out all the fogs and keeps their own switches.
	 *
	 * @param on false to put all the fogs out
	 */
	public Atmosphere setFog(boolean on) {
		this.fogOn = on;
		return this;
	}

	/**
	 * @return true if the fogs are drawn: the atmosphere has at least one fog that is enabled and the switch of the branch is on
	 */
	public boolean isFog() {
		if (!fogOn) return false;
		for (Fog f : fogs) {
			if (f.isEnabled()) return true;
		}
		return false;
	}

	/**
	 * @return the fogs that are drawn (the switch of the branch is on and the fog is enabled), a new list
	 */
	public List<Fog> getActiveFogs() {
		List<Fog> active = new ArrayList<Fog>();
		if (!fogOn) return active;
		for (Fog f : fogs) {
			if (f.isEnabled()) active.add(f);
		}
		return active;
	}

	// ----- All

	/**
	 * @return true if at least one effect is drawn
	 */
	public boolean isActive() {
		return isHaze() || isFog();
	}

	/**
	 * @return all the effects of the atmosphere (the haze if any, then the fogs), drawn or not, a new list
	 */
	public List<AtmosphericEffect> getEffects() {
		List<AtmosphericEffect> effects = new ArrayList<AtmosphericEffect>();
		if (haze != null) effects.add(haze);
		effects.addAll(fogs);
		return effects;
	}

	@Override
	public String toString() {
		return "Atmosphere: haze " + (haze == null ? "none" : (hazeOn ? "" : "(off) ") + haze) + ", " + fogs.size() + " fog(s)" + (fogOn ? "" : " (off)");
	}
}
