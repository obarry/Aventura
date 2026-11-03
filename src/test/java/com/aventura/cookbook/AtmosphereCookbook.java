package com.aventura.cookbook;

import java.awt.Color;

import com.aventura.context.PerspectiveContext;
import com.aventura.context.RenderContext;
import com.aventura.demo.DemoScene;
import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.atmosphere.Atmosphere;
import com.aventura.model.atmosphere.Haze;
import com.aventura.model.atmosphere.SphericalFog;
import com.aventura.model.light.AmbientLight;
import com.aventura.model.light.DirectionalLight;
import com.aventura.model.light.Lighting;
import com.aventura.model.perspective.PerspectiveType;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Box;

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
 * ------------------------------------------------------------------------------ *
 * The examples of docs/ATMOSPHERE_COOKBOOK.md and of the tutorial of the README: a row of pillars on each side of a long
 * lawn, under a blue sky, and the atmosphere to put around it, step by step (a program and a library of examples,
 * checked by TestAtmosphereCookbook).
 *
 * The scene is deliberately plain (boxes only), so that the atmosphere is the only thing to look at: the pillars get
 * farther and farther from the camera, which shows the haze, and the fourth to sixth ones on each side stand in the bank of
 * fog.
 *
 * @author Olivier BARRY
 * @since 2026
 */
public class AtmosphereCookbook {

	/** The sky: the background color of the World, and the color of the haze */
	public static final Color SKY = new Color(130, 175, 225);

	/** The camera: at the start of the row, at the height of the eye of a walker, looking along the row */
	public static final Vector4 EYE = new Vector4(0f, -8f, 2.2f, 1f);
	public static final Vector4 POI = new Vector4(0f, 40f, 2.2f, 1f);

	/** Pillars on each side of the lawn: one every 8 units, from y = 0 to y = 96 */
	public static final int PILLARS_PER_SIDE = 13;
	public static final float PILLAR_SPACING = 8f;

	private static final Color[] PILLAR_COLORS = { new Color(165, 100, 80), new Color(150, 150, 145), new Color(185, 160, 115), new Color(95, 110, 135) };

	// ----- The scene, with clear air

	/**
	 * The World with no atmosphere: air perfectly clear. A lawn and two rows of pillars of 1.5 x 1.5 x 5 units, 6 units on each side of
	 * the axis of the camera.
	 */
	public static World createWorld() {
		World world = new World();
		world.setBackgroundColor(SKY);

		Box lawn = new Box(40f, 220f, 0.1f);
		lawn.setColor(new Color(105, 130, 90));
		lawn.setTransformation(new Translation(new Vector3(0f, 90f, -0.05f)));
		world.addElement(lawn);

		for (int side = -1; side <= 1; side += 2) {
			for (int k = 0; k < PILLARS_PER_SIDE; k++) {
				Box pillar = new Box(1.5f, 1.5f, 5f);
				pillar.setColor(PILLAR_COLORS[(k + (side + 1) / 2) % PILLAR_COLORS.length]);
				pillar.setTransformation(new Translation(new Vector3(side * 6f, k * PILLAR_SPACING, 2.5f)));
				world.addElement(pillar);
			}
		}
		world.build();
		return world;
	}

	public static Lighting createLighting() {
		return new Lighting(new DirectionalLight(new Vector3(-0.5f, 0.35f, -0.8f), 0.9f), new AmbientLight(0.4f));
	}

	/** 0.8 x 0.45 view plane (16:9), 150 units deep: the pillars are all in the field */
	public static PerspectiveContext createPerspective(int pixelsPerUnit) {
		return new PerspectiveContext(0.8f, 0.45f, 1, 150, PerspectiveType.FRUSTUM, pixelsPerUnit);
	}

	// ----- Recipe 1: the haze

	/**
	 * A haze: the contrast of an object falls to 5% at 80 units, and the first 5 units are clear. Its color is the one of the sky (the
	 * background color of the World), since it has none of its own.
	 */
	public static Haze createHaze() {
		return new Haze(80f).setStartDistance(5f);
	}

	// ----- Recipe 2: a bank of fog

	/**
	 * A bank of fog of 12 units of radius, centered on the lawn 30 units ahead, at the height of the eye. Through its center the
	 * contrast falls to 5% over 20 units: the optical depth there is 4 x 12 / 20 = 2.4.
	 */
	public static SphericalFog createFogBank() {
		return new SphericalFog(new Vector4(0f, 30f, 2.2f, 1f), 12f, 20f);
	}

	// ----- Recipe 3: the atmosphere

	/** The atmosphere of the examples: the haze and the bank of fog */
	public static Atmosphere createAtmosphere() {
		Atmosphere atmosphere = new Atmosphere(createHaze());
		atmosphere.addFog(createFogBank());
		return atmosphere;
	}

	/**
	 * The scene seen from its camera, with the given atmosphere (null for none). The atmosphere is given to the World, and the
	 * RenderContext draws it: nothing is drawn if it is not switched on.
	 *
	 * @param atmosphere    the air of the World, null for clear air
	 * @param pixelsPerUnit the resolution of the image: 1000 gives 800 x 450 pixels
	 */
	public static DemoScene createScene(Atmosphere atmosphere, int pixelsPerUnit) {
		World world = createWorld();
		world.setAtmosphere(atmosphere);
		RenderContext renderContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE).setAtmosphere(atmosphere != null);
		return new DemoScene(world, createLighting(), EYE, POI, renderContext, createPerspective(pixelsPerUnit));
	}

	// ----- Recipe 5: a new kind of fog

	/**
	 * The haze and a mist that hugs the ground (GroundMist), 4 units thick, thickest on the lawn: the camera is inside, at the height of
	 * its eye, so that the pillars lose their feet in it and the horizon disappears.
	 */
	public static Atmosphere createGroundMistAtmosphere() {
		Atmosphere atmosphere = new Atmosphere(createHaze());
		atmosphere.addFog(new GroundMist(0f, 4f, 30f));
		return atmosphere;
	}

	// ----- Recipe 4: a fog that moves

	/**
	 * The position of the center of the fog bank at image i of an animation where it drifts across the lawn, from the left of the
	 * row (x = -14) to its right (x = 14) in 100 images, and comes back.
	 */
	public static Vector4 driftingCenter(int i) {
		double phase = (1 - Math.cos(2 * Math.PI * i / 100)) / 2; // 0 -> 1 -> 0
		return new Vector4((float) (-14 + 28 * phase), 30f, 2.2f, 1f);
	}
}
