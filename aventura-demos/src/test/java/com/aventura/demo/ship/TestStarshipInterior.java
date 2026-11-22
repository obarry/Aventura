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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;

import org.junit.BeforeClass;
import org.junit.Test;

import com.aventura.model.light.PointLight;
import com.aventura.model.perspective.Perspective;
import com.aventura.model.world.GeometryStatistics;
import com.aventura.view.ImageView;

/**
 * Tests of the StarshipInterior demo: its scene (built once for all the tests), its lights and their budget, the door
 * that opens, and renderings without a window, in a small ImageView.
 *
 * @since Nov 2026
 */
public class TestStarshipInterior {

	private static StarshipInterior demo;
	private static ImageView view;

	@BeforeClass
	public static void createScene() {
		demo = new StarshipInterior();
		demo.createScene();
		view = new ImageView(StarshipInterior.createPerspectiveContext(160));
		demo.setView(view);
	}

	@Test
	public void testScene() {
		System.out.println("***** Test StarshipInterior : the scene *****");
		Ship ship = demo.getShip();
		assertEquals("the loop in 6 corridors, the shortcut in 2, the airlock corridor, the store", 10, ship.getCorridors().size());
		assertEquals(2, ship.getHubs().size());
		assertEquals("4 doors in the junctions, 2 in the room", 6, ship.getDoors().size());
		GeometryStatistics statistics = GeometryStatistics.of(demo.getWorld());
		assertTrue(statistics.getNbTriangles() > 10000);
	}

	@Test
	public void testLights_aFewOnAtATime() {
		System.out.println("***** Test StarshipInterior : the lights, a few switched on at a time *****");
		int nbLights = demo.getLighting().getPointLights().size();
		assertTrue("dozens of lights", nbLights > 30);
		demo.showVisitAt(30);
		demo.renderImage();
		assertEquals(ShipLights.MAX_LIGHTS, enabledLights());
		demo.showPlan();
		demo.renderImage();
		assertEquals("all of them in the plan view", nbLights, enabledLights());
	}

	@Test
	public void testRendering_visitAndPlan() {
		System.out.println("***** Test StarshipInterior : rendering without a window *****");
		for (float distance : new float[] { 0, 30, 88 }) {
			demo.showVisitAt(distance);
			demo.renderImage();
			assertTrue("an image is drawn at " + distance + " m", meanBrightness(view.getImageView()) > 10);
		}
		demo.showPlan();
		demo.renderImage();
		assertTrue(meanBrightness(view.getImageView()) > 3);
	}

	@Test
	public void testDoor_opensAndInvalidatesTheShadowsNearIt() {
		System.out.println("***** Test StarshipInterior : the north door opens, the lights near it draw their maps again *****");
		AirlockDoor door = demo.northDoor;
		assertEquals("closed at the start", 0, door.getOpening(), 0);
		// Render once near the door, so that the lights near it have their maps
		demo.showWalk(0, Ship.LOOP_HALF_WIDTH - 0.5f, 90, 0);
		demo.renderImage();
		PointLight hubLight = null;
		for (PointLight light : demo.getLighting().getPointLights()) {
			if (light.getPosition().minus(demo.getShip().getHubs().get(1).getLightPoint()).length() < 1e-3f) hubLight = light;
		}
		long drawn = hubLight.getDrawnShadowMapFaces();
		demo.renderImage();
		assertEquals("static: kept", drawn, hubLight.getDrawnShadowMapFaces());
		door.setOpening(1);
		demo.invalidateShadowsNear(door.getCenter());
		demo.renderImage();
		assertTrue("drawn again once the door moved", hubLight.getDrawnShadowMapFaces() > drawn);
		door.setOpening(0);
		demo.invalidateShadowsNear(door.getCenter());
	}

	@Test
	public void testFieldOfView() {
		System.out.println("***** Test StarshipInterior : the field of view *****");
		assertEquals(StarshipInterior.DEFAULT_FIELD_OF_VIEW, demo.getFieldOfView(), 0);
		for (float degrees : new float[] { 90, 50, StarshipInterior.DEFAULT_FIELD_OF_VIEW }) {
			demo.setFieldOfView(degrees);
			demo.showVisitAt(10);
			demo.renderImage();
			// The field of view of the image rendered, from its view plane: 2 atan((width / 2) / distance)
			Perspective perspective = demo.pContext.getPerspective();
			assertEquals("the view plane follows the image", 160, Math.round(perspective.getWidth() * demo.pContext.getPPU()));
			float measured = (float) Math.toDegrees(2 * Math.atan(perspective.getWidth() / 2 / perspective.getDist()));
			assertEquals(degrees, measured, 0.01f);
		}
		demo.setFieldOfView(500);
		assertEquals("kept within its limits", StarshipInterior.MAX_FIELD_OF_VIEW, demo.getFieldOfView(), 0);
		demo.setFieldOfView(StarshipInterior.DEFAULT_FIELD_OF_VIEW);
	}

	@Test
	public void testVisit_stopsInFrontOfTheNorthDoor() {
		System.out.println("***** Test StarshipInterior : the visit stops in front of the north door, which opens *****");
		ShipTour.Stop stop = demo.getTour().getNorthDoorStop();
		demo.showVisitAt(stop.getDistance() - 3);
		assertEquals(0, demo.northDoor.getOpening(), 0);
		// Walk to the stop, a tenth of a second at a time: it is reached, and the visitor stands on it
		float time = 0;
		while (demo.stopTime < 0 && time < 10) {
			demo.advanceTour(0.1f);
			time += 0.1f;
		}
		assertTrue("the stop is reached", demo.stopTime >= 0);
		assertTrue("slowly: more than 3 m at full speed would take", time > 3 / StarshipInterior.VISIT_SPEED);
		assertEquals(stop.getDistance(), demo.tourDistance, 1e-4f);
		// The stop: standing still, the door opens
		float opening = 0;
		for (float t = 0; t < ShipTour.Stop.DURATION - 0.2f; t += 0.1f) {
			demo.advanceTour(0.1f);
			assertEquals("standing still", stop.getDistance(), demo.tourDistance, 1e-4f);
			assertTrue("the door does not close during the stop", demo.northDoor.getOpening() >= opening);
			opening = demo.northDoor.getOpening();
		}
		assertEquals("open", 1, opening, 1e-6f);
		demo.renderImage();
		// The visit goes on, the door closes behind
		for (float t = 0; t < 4; t += 0.1f) demo.advanceTour(0.1f);
		assertTrue("walking again", demo.tourDistance > stop.getDistance() + 1);
		assertEquals("closed", 0, demo.northDoor.getOpening(), 0);
		assertEquals(-1, demo.stopTime, 0);
	}

	private int enabledLights() {
		int n = 0;
		for (PointLight light : demo.getLighting().getPointLights()) {
			if (light.isEnabled()) n++;
		}
		return n;
	}

	private static double meanBrightness(BufferedImage image) {
		long sum = 0;
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int rgb = image.getRGB(x, y);
				sum += ((rgb >> 16) & 255) + ((rgb >> 8) & 255) + (rgb & 255);
			}
		}
		return sum / 3.0 / (image.getWidth() * image.getHeight());
	}
}
