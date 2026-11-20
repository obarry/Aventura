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
package com.aventura.demo.ship;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.aventura.demo.LightBudget;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.light.AmbientLight;
import com.aventura.model.light.LightAppearance;
import com.aventura.model.light.Lighting;
import com.aventura.model.light.PointLight;
import com.aventura.model.light.ShadowFilter;
import com.aventura.model.light.ShadowingLight;
import com.aventura.model.light.SpotLight;

/**
 * The lights of the ship, and the budget that keeps only a few of them switched on.
 * <ul>
 * <li>A {@link PointLight} under each fixture of the corridors: a warm white, lighting all around up to
 * CORRIDOR_LIGHT_RANGE meters (its intensity falls linearly to 0 there).</li>
 * <li>A {@link SpotLight} in each junction, amber, pointing down: a cone of light on the floor, the walls in the half
 * light.</li>
 * <li>In the control room, a cold point light in the ring of the ceiling and two spot lights on the banks of consoles.</li>
 * <li>A red spot light above the hatch of the hold.</li>
 * <li>A faint bluish {@link AmbientLight}: without it, what no light reaches would be pure black.</li>
 * </ul>
 * Every light casts shadows: the walls stop the light of the next corridor. The ship does not move, so each light
 * keeps its shadow map from one image to the next (setStaticShadowCasters()): a map is drawn once, the first time its
 * light is switched on, then kept, even while the light is off. Only a door that opens obliges the lights near it to
 * draw their map again.
 *
 * <p>The {@link LightBudget} switches on the MAX_LIGHTS lights nearest to the camera before each image, those in front of
 * it first, faded so that none pops in or out, and switches the others off.
 *
 * @since Nov 2026
 */
public class ShipLights {

	/** Number of lights switched on at a time, and the distance over which a light fades in or out. */
	public static final int MAX_LIGHTS = 3;
	public static final float FADE_DISTANCE = 1.5f;

	public static final Color CORRIDOR_LIGHT_COLOR = new Color(255, 232, 198);
	public static final float CORRIDOR_LIGHT_INTENSITY = 1.25f;
	public static final float CORRIDOR_LIGHT_RANGE = 10f;

	public static final Color HUB_LIGHT_COLOR = new Color(255, 176, 92);
	public static final float HUB_LIGHT_INTENSITY = 1.6f;
	public static final float HUB_LIGHT_RANGE = 7f;
	public static final float HUB_LIGHT_ANGLE = 65; // Degrees, half-angle of the cone

	public static final Color ROOM_LIGHT_COLOR = new Color(210, 226, 255);
	public static final float ROOM_LIGHT_INTENSITY = 1.3f;
	public static final float ROOM_LIGHT_RANGE = 11f;
	public static final Color CONSOLE_LIGHT_COLOR = new Color(190, 214, 255);
	public static final float CONSOLE_LIGHT_INTENSITY = 1.1f;
	public static final float CONSOLE_LIGHT_RANGE = 6f;

	public static final Color HATCH_LIGHT_COLOR = new Color(255, 70, 50);
	public static final float HATCH_LIGHT_INTENSITY = 0.85f;
	public static final float HATCH_LIGHT_RANGE = 6f;

	public static final Color AMBIENT_COLOR = new Color(170, 190, 230);
	public static final float AMBIENT_INTENSITY = 0.12f;

	/** Size of the shadow maps: each face of the cube of a point light, or the map of a spot light. */
	public static final int SHADOW_MAP_SIZE = 512;

	protected final Lighting lighting;
	protected final AmbientLight ambient;
	protected final LightBudget budget = new LightBudget(MAX_LIGHTS, FADE_DISTANCE);
	protected final List<ShadowingLight> lights = new ArrayList<ShadowingLight>();

	/** Creates the lights of the ship, gathered in a Lighting, with specular highlights. */
	public ShipLights(Ship ship) {
		ambient = new AmbientLight(AMBIENT_INTENSITY);
		ambient.setLightColor(AMBIENT_COLOR);
		lighting = new Lighting(ambient);
		lighting.setSpecularLight(true);

		for (Corridor corridor : ship.getCorridors()) {
			for (Vector4 point : corridor.getLightPoints()) {
				add(new PointLight(point, CORRIDOR_LIGHT_RANGE, CORRIDOR_LIGHT_INTENSITY), CORRIDOR_LIGHT_COLOR, CORRIDOR_LIGHT_INTENSITY);
			}
		}
		Vector3 down = new Vector3(0, 0, -1);
		for (Hub hub : ship.getHubs()) {
			add(spot(hub.getLightPoint(), down, HUB_LIGHT_RANGE, HUB_LIGHT_INTENSITY, HUB_LIGHT_ANGLE), HUB_LIGHT_COLOR, HUB_LIGHT_INTENSITY);
		}
		ControlRoom room = ship.getRoom();
		add(new PointLight(room.getCenterLightPoint(), ROOM_LIGHT_RANGE, ROOM_LIGHT_INTENSITY), ROOM_LIGHT_COLOR, ROOM_LIGHT_INTENSITY);
		for (int side = -1; side <= 1; side += 2) {
			// Aimed at the consoles along the wall, from above the islands
			Vector3 towardsWall = new Vector3(side * 1.2f, 0, -1);
			add(spot(room.getConsoleLightPoint(side), towardsWall, CONSOLE_LIGHT_RANGE, CONSOLE_LIGHT_INTENSITY, 50), CONSOLE_LIGHT_COLOR,
					CONSOLE_LIGHT_INTENSITY);
		}
		// Above the hatch, aimed at it
		add(spot(ship.getHatchLightPoint(), new Vector3(0, -1, -0.8f), HATCH_LIGHT_RANGE, HATCH_LIGHT_INTENSITY, 45), HATCH_LIGHT_COLOR,
				HATCH_LIGHT_INTENSITY);
	}

	private static SpotLight spot(Vector4 point, Vector3 direction, float range, float intensity, float angleDegrees) {
		float outer = (float) Math.toRadians(angleDegrees);
		return new SpotLight(point, direction, range, intensity, outer, outer * 0.6f);
	}

	/**
	 * Gives a light its color, its shadows and its look in the picture (a small halo, when the lights are visible), and
	 * puts it in the Lighting and in the budget.
	 */
	private void add(PointLight light, Color color, float intensity) {
		light.setLightColor(color);
		light.setShadowMapSize(SHADOW_MAP_SIZE);
		// Soft shadows: without them, the shadows of the pipes and of the cable tray, cast at a grazing angle on the walls,
		// show the pixels of the shadow map as teeth. They cost about 30 % of the time of an image (Effects menu)
		light.setShadowFilter(ShadowFilter.PCF_3X3);
		light.setStaticShadowCasters(true); // The ship does not move: the map is drawn once
		light.setAppearance(new LightAppearance().setCoreRadius(0.05f).setGlowRadius(0.35f).setGain(0.55f));
		lighting.addPointLight(light); // A SpotLight is a PointLight: same list
		budget.add(light, intensity);
		lights.add(light);
	}

	public Lighting getLighting() {
		return lighting;
	}

	public AmbientLight getAmbient() {
		return ambient;
	}

	public LightBudget getBudget() {
		return budget;
	}

	/** All the lights but the ambient one. */
	public List<ShadowingLight> getLights() {
		return lights;
	}

	/** The lights must draw their shadow map again: something moved (a door). */
	public void invalidateShadowMaps() {
		for (ShadowingLight light : lights) light.invalidateShadowMap();
	}
}
