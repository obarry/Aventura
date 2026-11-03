package com.aventura.demo;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

import javax.imageio.ImageIO;

import com.aventura.context.PerspectiveContext;
import com.aventura.context.RenderContext;
import com.aventura.cookbook.AtmosphereCookbook;
import com.aventura.cookbook.CookbookGallery;
import com.aventura.math.transform.Transformation;
import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.atmosphere.Atmosphere;
import com.aventura.model.atmosphere.Fog;
import com.aventura.model.perspective.PerspectiveType;
import com.aventura.model.world.World;
import com.aventura.model.world.WrongArraySizeException;
import com.aventura.model.world.shape.Trellis;
import com.aventura.model.light.AmbientLight;
import com.aventura.model.light.DirectionalLight;
import com.aventura.model.light.Lighting;
import com.aventura.model.light.PointLight;
import com.aventura.model.light.ShadowFilter;
import com.aventura.model.light.SpotLight;
import com.aventura.test.TestAtmosphere;
import com.aventura.test.TestFog;
import com.aventura.test.TestLightingPoint1Shadows;
import com.aventura.test.TestLightingSpot1;
import com.aventura.test.TestLightingSpot3Shadows;
import com.aventura.test.TestLightingSun;

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
 * Regenerates the images of the documentation (README, docs/DESIGN.md, docs/GEOMETRY_COOKBOOK.md), stored in
 * resources/doc/images. Each image is rendered off-screen (no window) from the same scene as the demo or test
 * program it illustrates, which provides its World, lights and settings:
 *
 *     hello_aventura.png      HelloAventura (the README's example program)
 *     aventura_demo.jpg       AventuraDemo, one image of its animation
 *     urbanscape_street.jpg   UrbanScape, camera low in front of the buildings
 *     urbanscape_flight.jpg   UrbanScape, camera high on the side (shadows of the buildings)
 *     fractal_landscape.jpg   FractalLandscape, landscape from a fixed random seed, seen from afar
 *     spotlights.jpg          TestLightingSpot1
 *     spot_shadows.jpg        TestLightingSpot3Shadows, one image of its animation (shadows of a SpotLight)
 *     soft_shadows.png        TestLightingSpot3Shadows, hard (left) and PCF 3x3 (right) shadows with a small shadow map
 *     point_shadows.jpg       TestLightingPoint1Shadows, one image of its animation (cube-map shadows of a PointLight)
 *     sun_street.jpg          TestLightingSun, the visible sun (disc, halo, lens flare, light shafts) in a street between tower blocks
 *     haze_avenue.jpg         TestAtmosphere, the haze of the distance on a long avenue between buildings
 *     haze_landscape.jpg      FractalLandscape, the haze of the distance on the hills and the sea (as the Atmosphere option of the demo)
 *     fog_avenue.jpg          TestFog, banks of fog (SphericalFog) over the avenue, with the haze
 *     fog_landscape.jpg       FractalLandscape, random banks of fog on the hills (as Landscape / Generate fog of the demo), with the haze
 *     atmosphere_steps.jpg    AtmosphereCookbook, the same scene in four steps: clear air, haze, fog, haze and fog
 *     ground_mist.jpg         AtmosphereCookbook, a new kind of fog (GroundMist) written outside the engine: a mist that hugs the ground
 *     haze_compare.jpg        TestAtmosphere, the avenue without the atmosphere (left) and with its haze (right)
 *     fog_compare.jpg         TestFog, the avenue without any atmosphere (left) and with its banks of fog only (right)
 *     cookbook_gallery.png    CookbookGallery (the examples of the Geometry Cookbook)
 *
 * Run it from the project root (the textures are read from resources/texture), headless:
 *
 *     java -Djava.awt.headless=true -cp target/classes:target/test-classes com.aventura.demo.DocumentationImages [directory] [image...]
 *
 * With no argument, all the images are written to resources/doc/images. A directory can be given (e.g. to compare
 * with the current images before replacing them), and optionally the names of the images to regenerate.
 * The format (PNG or JPEG) follows the extension of the name.
 *
 * @author Olivier BARRY
 * @since 2026
 */
public class DocumentationImages {

	public static final String DEFAULT_DIRECTORY = "resources/doc/images";

	// ***** Choices of the images (camera positions, animation image, random seed) *****

	/** Image of the AventuraDemo animation (0 .. AventuraDemo.NB_IMAGES) */
	static final int AVENTURA_DEMO_IMAGE = 178;
	/** Angles of the UrbanScape helicopter flight, in degrees from the start of the flight (see UrbanScape.HelicopterFlight) */
	static final float URBANSCAPE_STREET_ANGLE = 30;
	static final float URBANSCAPE_FLIGHT_ANGLE = 121;
	/**
	 * Seed of the random generator of the fractal landscape: the same seed always gives the same landscape. This one
	 * gives an island (sea level all along the border): the trellis is an open surface, so a hill cut by the border
	 * would show its edge, with nothing below it.
	 */
	static final long FRACTAL_SEED = 1862;
	/** Seed of the random fogs of the fog landscape image: banks that show well on the hills (see FractalLandscape.generateFogs()) */
	static final long FOG_LANDSCAPE_SEED = 5;
	/** Images of the TestLightingSpot3Shadows and TestLightingPoint1Shadows animations (0 .. NB_IMAGES) */
	static final int SPOT_SHADOWS_IMAGE = 20;
	static final int POINT_SHADOWS_IMAGE = 45;
	/** Angle of the camera of the TestLightingSun sweep (degrees, from -30 to +5): the sun is partly hidden by a building, with its flare and shafts */
	static final float SUN_STREET_YAW = -12.5f;
	/** Camera of the fog avenue image: in the avenue, just before the first bank of fog */
	static final Vector4 FOG_AVENUE_EYE = new Vector4(0f, -6f, 2.2f, 1);
	static final Vector4 FOG_AVENUE_POI = new Vector4(0f, 38f, 3f, 1);
	/** Camera of the haze landscape image: the one of the demo (FractalLandscape.INITIAL_EYE), a little to the side */
	static final Vector4 HAZE_LANDSCAPE_EYE = new Vector4(6.2f, -1.6f, 3.0f, 1);
	static final Vector4 HAZE_LANDSCAPE_POI = FractalLandscape.POINT_OF_INTEREST;
	/** Camera of the fog landscape image: closer and higher than the haze one, above the banks of fog */
	static final Vector4 FOG_LANDSCAPE_EYE = new Vector4(3.4f, -3.4f, 3.4f, 1);
	static final Vector4 FOG_LANDSCAPE_POI = new Vector4(0, 0, 1.4f, 1);
	/** Size of the shadow map of the spot light in the soft shadows image: coarse on purpose, to make the filtering visible */
	static final int SOFT_SHADOWS_MAP_SIZE = 200;

	// ***** The scenes and the images *****

	/**
	 * The scenes of the images, by image file name, in the order of generation. They can also be shown in a window,
	 * with a camera moved by the mouse: see SceneViewer. hello_aventura.png is not a scene: HelloAventura is the
	 * README's program, kept as is (see images()).
	 */
	public static Map<String, Supplier<DemoScene>> scenes() {
		Map<String, Supplier<DemoScene>> scenes = new LinkedHashMap<>();
		scenes.put("aventura_demo.jpg", () -> aventuraDemo(AVENTURA_DEMO_IMAGE, 800));
		scenes.put("urbanscape_street.jpg", () -> urbanScape(URBANSCAPE_STREET_ANGLE, 500));
		scenes.put("urbanscape_flight.jpg", () -> urbanScape(URBANSCAPE_FLIGHT_ANGLE, 500));
		scenes.put("fractal_landscape.jpg", () -> fractalLandscape(FRACTAL_SEED));
		scenes.put("spotlights.jpg", () -> spotLights(1000));
		scenes.put("spot_shadows.jpg", () -> spotShadows(SPOT_SHADOWS_IMAGE, 1000));
		scenes.put("point_shadows.jpg", () -> pointShadows(POINT_SHADOWS_IMAGE, 1000));
		scenes.put("sun_street.jpg", () -> sunStreet(SUN_STREET_YAW, 1250));
		scenes.put("haze_avenue.jpg", () -> hazeAvenue(1000));
		scenes.put("haze_landscape.jpg", () -> hazeLandscape(FRACTAL_SEED));
		scenes.put("fog_avenue.jpg", () -> fogAvenue(1000));
		scenes.put("ground_mist.jpg", () -> AtmosphereCookbook.createScene(AtmosphereCookbook.createGroundMistAtmosphere(), 1000));
		scenes.put("fog_landscape.jpg", () -> fogLandscape(FRACTAL_SEED, FOG_LANDSCAPE_SEED));
		scenes.put("cookbook_gallery.png", () -> CookbookGallery.createScene(1000));
		return scenes;
	}

	/** The images, by file name, in the order of generation: the README's program, then the scenes rendered off-screen */
	static Map<String, Supplier<BufferedImage>> images() {
		Map<String, Supplier<BufferedImage>> images = new LinkedHashMap<>();
		images.put("hello_aventura.png", DocumentationImages::helloAventura);
		images.put("soft_shadows.png", DocumentationImages::softShadows);
		images.put("atmosphere_steps.jpg", DocumentationImages::atmosphereSteps);
		images.put("haze_compare.jpg", DocumentationImages::hazeCompare);
		images.put("fog_compare.jpg", DocumentationImages::fogCompare);
		for (Map.Entry<String, Supplier<DemoScene>> scene : scenes().entrySet()) {
			images.put(scene.getKey(), () -> scene.getValue().get().render());
		}
		return images;
	}

	/** The README's program, run as is: it writes its own image, which is read back. */
	static BufferedImage helloAventura() {
		try {
			File file = File.createTempFile("hello_aventura", ".png");
			file.deleteOnExit();
			HelloAventura.main(new String[] { file.getPath() });
			return ImageIO.read(file);
		} catch (Exception e) {
			throw new IllegalStateException("HelloAventura failed", e);
		}
	}

	/**
	 * One image of the AventuraDemo animation, as the demo shows it (same World, lights, rendering, and the same
	 * rotation of the World and position of the camera at that image), with landmarks.
	 *
	 * @param image number of the image in the animation
	 * @param width width of the image in pixels (the demo's 1.5 x 0.9 view plane gives the height)
	 */
	static DemoScene aventuraDemo(int image, int width) {
		World world = AventuraDemo.createWorld();
		Transformation rotation = AventuraDemo.createImageRotation();
		for (int i = 0; i < image; i++) {
			world.expandTransformation(rotation);
		}
		PerspectiveContext perspective = new PerspectiveContext(width, 1.5f, 0.9f, 1, 100, PerspectiveType.FRUSTUM);
		return new DemoScene(world, AventuraDemo.createLighting(), AventuraDemo.getEye(image), AventuraDemo.POI,
				AventuraDemo.createRenderContext(), perspective);
	}

	/**
	 * The UrbanScape scene seen from a point of the helicopter flight of the camera.
	 *
	 * @param angle         angle traveled since the start of the flight, in degrees (0: in front of the buildings, low)
	 * @param pixelsPerUnit resolution: the demo's 1.6 x 0.9 view plane gives 800 x 450 pixels at 500
	 */
	static DemoScene urbanScape(float angle, int pixelsPerUnit) {
		World world = UrbanScape.createWorld();
		UrbanScape.HelicopterFlight flight = UrbanScape.createFlight(world);
		PerspectiveContext perspective = new PerspectiveContext(1.6f, 0.9f, 1, 100, PerspectiveType.FRUSTUM, pixelsPerUnit);
		return new DemoScene(world, UrbanScape.createLighting(), flight.getEye((float) Math.toRadians(angle)), flight.getFocus(),
				UrbanScape.createRenderContext(), perspective);
	}

	/**
	 * The fractal landscape of FractalLandscape (same size, resolution, sea level, colors and light),
	 * generated from a fixed seed, seen from farther than the demo's initial position so that it is fully visible
	 * (1280 x 720 pixels). The image of the documentation is the 960 x 260 band around it (crop).
	 */
	static DemoScene fractalLandscape(long seed) {
		float size = 4;
		int n = 128;
		float[][] altitudes = new float[n + 1][n + 1];
		FractalLandscape.generateFractalAltitudes(altitudes, size, n, new Random(seed));
		Trellis trellis;
		try {
			trellis = new Trellis(size, size, n, n, altitudes);
		} catch (WrongArraySizeException e) {
			throw new IllegalStateException(e);
		}
		trellis.setSpecularExp(8);
		World world = new World();
		world.addElement(trellis);
		world.build();
		FractalLandscape.colorTrianglesByAltitude(trellis);

		PerspectiveContext perspective = new PerspectiveContext(0.8f, 0.45f, 0.8f, 100, PerspectiveType.FRUSTUM, 1600);
		RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE);
		rContext.setTextureProcessing(false);
		return new DemoScene(world, fractalLandscapeLighting(), new Vector4(7, -2, 2.4f, 1), new Vector4(0, 0, 1.3f, 1),
				rContext, perspective).crop(241, 190, 960, 260);
	}

	/**
	 * The lighting of the fractal landscape image: a sun behind the camera of that image, lighting the slopes it sees.
	 * The interactive demo uses a lower sun instead, that can be seen in the sky (see FractalLandscape.createSun()).
	 */
	static Lighting fractalLandscapeLighting() {
		return new Lighting(new DirectionalLight(new Vector3(-1, 1, -0.5f), 0.8f), new AmbientLight(0.2f), false);
	}

	/** The scene of TestLightingSpot1: two spot lights (soft and sharp edge) and a point light, without landmarks. */
	static DemoScene spotLights(int pixelsPerUnit) {
		PerspectiveContext perspective = new PerspectiveContext(0.8f, 0.45f, 1, 100, PerspectiveType.FRUSTUM, pixelsPerUnit);
		RenderContext rContext = new RenderContext(TestLightingSpot1.createRenderContext());
		rContext.setDisplayLandmark(false);
		return new DemoScene(TestLightingSpot1.createWorld(), TestLightingSpot1.createLighting(), TestLightingSpot1.EYE, TestLightingSpot1.POI,
				rContext, perspective);
	}

	/**
	 * The scene of TestLightingSun: a street between tower blocks with a low sun (disc, halo, lens flare and light shafts), seen from a
	 * camera turned by the given angle (degrees) around the vertical axis.
	 */
	static DemoScene sunStreet(float yawDegrees, int pixelsPerUnit) {
		PerspectiveContext perspective = new PerspectiveContext(0.8f, 0.45f, 1, 100, PerspectiveType.FRUSTUM, pixelsPerUnit);
		RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE).setLightGlow(true);
		return new DemoScene(TestLightingSun.createWorld(), TestLightingSun.createLighting(), TestLightingSun.EYE, TestLightingSun.poiForYaw(yawDegrees),
				rContext, perspective);
	}

	/**
	 * The scene of TestAtmosphere: a long avenue between two rows of buildings, seen from its start. The World has an Atmosphere and the
	 * scene switches it on: the buildings fade towards the sky color as they get farther (the Atmosphere option of the SceneViewer
	 * shows the difference).
	 */
	static DemoScene hazeAvenue(int pixelsPerUnit) {
		PerspectiveContext perspective = new PerspectiveContext(0.8f, 0.45f, 1, 260, PerspectiveType.FRUSTUM, pixelsPerUnit);
		RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE).setAtmosphere(true);
		return new DemoScene(TestAtmosphere.createWorld(), TestAtmosphere.createLighting(), TestAtmosphere.eyeAt(0), TestAtmosphere.poiAt(0),
				rContext, perspective);
	}

	/**
	 * The scene of TestFog: the avenue of TestAtmosphere with four banks of fog (SphericalFog) in the World, seen from a point of the
	 * avenue just before the first bank. The atmosphere has the haze and the fogs, switched on (the Haze and Fog options of the
	 * SceneViewer switch each of them off).
	 */
	static DemoScene fogAvenue(int pixelsPerUnit) {
		PerspectiveContext perspective = new PerspectiveContext(0.8f, 0.45f, 1, 260, PerspectiveType.FRUSTUM, pixelsPerUnit);
		RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE).setAtmosphere(true);
		return new DemoScene(TestFog.createWorld(), TestAtmosphere.createLighting(), FOG_AVENUE_EYE, FOG_AVENUE_POI, rContext, perspective);
	}

	/**
	 * The landscape of FractalLandscape (the same land colors, sea, sky, sun and atmosphere, from a fixed seed), seen from the side
	 * of the island, so that the far hills are farther than the near ones: with the atmosphere they get paler and
	 * bluer, like in a real landscape (the Atmosphere option of the SceneViewer shows the difference). The sun is visible, as in
	 * the demo. The image of the documentation is the 1000 x 260 band around the island (crop): the SceneViewer shows the whole view.
	 */
	static DemoScene hazeLandscape(long seed) {
		return landscapeWithAtmosphere(seed, null, HAZE_LANDSCAPE_EYE, HAZE_LANDSCAPE_POI, true);
	}

	/**
	 * The same landscape, with the random fogs of the demo (FractalLandscape.generateFogs()) made from the given seed, switched on with the
	 * haze: the Landscape / Generate fog menu of the demo, at the seed that gives this image.
	 */
	static DemoScene fogLandscape(long seed, long fogSeed) {
		return fogLandscape(seed, fogSeed, FOG_LANDSCAPE_EYE, FOG_LANDSCAPE_POI);
	}

	static DemoScene fogLandscape(long seed, long fogSeed, Vector4 eye, Vector4 poi) {
		return landscapeWithAtmosphere(seed, fogSeed, eye, poi, false);
	}

	private static DemoScene landscapeWithAtmosphere(long seed, Long fogSeed, Vector4 eye, Vector4 poi, boolean band) {
		float size = FractalLandscape.LANDSCAPE_SIZE;
		int n = FractalLandscape.LANDSCAPE_DIVISIONS;
		float[][] altitudes = new float[n + 1][n + 1];
		FractalLandscape.generateFractalAltitudes(altitudes, size, n, new Random(seed));
		Trellis land;
		try {
			land = new Trellis(size, size, n, n, altitudes);
		} catch (WrongArraySizeException e) {
			throw new IllegalStateException(e);
		}
		land.setSpecularExp(FractalLandscape.LAND_SPECULAR_EXP);
		land.setSpecularColor(FractalLandscape.LAND_SPECULAR_COLOR);
		Trellis sea = new Trellis(size, size, 4, 4);
		sea.setColor(FractalLandscape.SEA_COLOR);
		sea.setSpecularExp(FractalLandscape.SEA_SPECULAR_EXP);
		sea.setSpecularColor(FractalLandscape.SEA_SPECULAR_COLOR);
		sea.setTransformation(new Translation(new Vector3(0, 0, FractalLandscape.INITIAL_SEA_LEVEL)));
		World world = new World();
		world.setBackgroundColor(FractalLandscape.SKY_COLOR);
		world.addElement(land);
		world.addElement(sea);
		world.setAtmosphere(FractalLandscape.createAtmosphere());
		world.build();
		FractalLandscape.colorTrianglesByAltitude(land);
		if (fogSeed != null) {
			for (Fog fog : FractalLandscape.generateFogs(new Random(fogSeed), size, FractalLandscape.INITIAL_SEA_LEVEL, land.getMaxZ())) {
				world.getAtmosphere().addFog(fog);
			}
		}

		RenderContext rContext = FractalLandscape.createRenderContext().setAtmosphere(true);
		DemoScene scene = new DemoScene(world, FractalLandscape.createLighting(FractalLandscape.createSun()), eye, poi, rContext,
				FractalLandscape.createPerspectiveContext());
		return band ? scene.crop(0, 140, 1000, 260) : scene;
	}

	/** The scene of TestLightingSpot3Shadows: a spot light (and a dim moonlight) sweeping a floor of five objects, at image i of the animation. */
	static DemoScene spotShadows(int image, int pixelsPerUnit) {
		return spotShadows(image, pixelsPerUnit, ShadowFilter.HARD, 0);
	}

	/** Same scene with the given filter on the shadow map of the spot, and the given shadow map size (0 = the default one) */
	static DemoScene spotShadows(int image, int pixelsPerUnit, ShadowFilter filter, int shadowMapSize) {
		PerspectiveContext perspective = new PerspectiveContext(0.8f, 0.45f, 1, 100, PerspectiveType.FRUSTUM, pixelsPerUnit);
		RenderContext rContext = new RenderContext(TestLightingSpot3Shadows.createRenderContext());
		rContext.setDisplayLandmark(false);
		SpotLight spot = TestLightingSpot3Shadows.createSpot();
		TestLightingSpot3Shadows.aimSpot(spot, image);
		spot.setShadowFilter(filter);
		if (shadowMapSize > 0) {
			spot.setShadowMapSize(shadowMapSize);
		}
		return new DemoScene(TestLightingSpot3Shadows.createWorld(), TestLightingSpot3Shadows.createLighting(spot), new Vector4(9, -7, 6, 1),
				new Vector4(0, 0, 0, 1), rContext, perspective);
	}

	/**
	 * Hard shadows (left) and soft shadows with a 3x3 percentage-closer filter (right) of the same spot light, on the same crop of the
	 * TestLightingSpot3Shadows scene. A small shadow map (SOFT_SHADOWS_MAP_SIZE) makes the texels of the map visible, which is where the
	 * filter helps: it lets a light use a smaller map without staircase edges.
	 */
	static BufferedImage softShadows() {
		BufferedImage hard = spotShadows(SPOT_SHADOWS_IMAGE, 1000, ShadowFilter.HARD, SOFT_SHADOWS_MAP_SIZE).render();
		BufferedImage soft = spotShadows(SPOT_SHADOWS_IMAGE, 1000, ShadowFilter.PCF_3X3, SOFT_SHADOWS_MAP_SIZE).render();
		int x = 430, y = 100, w = 370, h = 230;
		int scale = 2;
		int gap = 6;
		BufferedImage out = new BufferedImage(2 * w * scale + gap, h * scale, BufferedImage.TYPE_INT_RGB);
		java.awt.Graphics2D g = out.createGraphics();
		g.setColor(java.awt.Color.WHITE);
		g.fillRect(0, 0, out.getWidth(), out.getHeight());
		g.drawImage(hard.getSubimage(x, y, w, h), 0, 0, w * scale, h * scale, null);
		g.drawImage(soft.getSubimage(x, y, w, h), w * scale + gap, 0, w * scale, h * scale, null);
		g.dispose();
		return out;
	}

	/**
	 * The avenue of TestAtmosphere without the atmosphere (left) and with its haze (right): the same camera, the buildings fade into the
	 * sky as they get farther. Each half is the 600 x 338 image of the scene.
	 */
	static BufferedImage hazeCompare() {
		DemoScene without = hazeAvenue(750);
		without.getRenderContext().setAtmosphere(false);
		return sideBySide(without.render(), hazeAvenue(750).render());
	}

	/**
	 * The avenue of TestFog without any atmosphere (left) and with its banks of fog only (right, the haze is put out with
	 * Atmosphere.setHaze(false)): a fog dims what is behind it, the sky included, and has no visible edge.
	 */
	static BufferedImage fogCompare() {
		DemoScene without = fogAvenue(750);
		without.getRenderContext().setAtmosphere(false);
		DemoScene with = fogAvenue(750);
		with.getWorld().getAtmosphere().setHaze(false);
		return sideBySide(without.render(), with.render());
	}

	/**
	 * The scene of the tutorial of the README (AtmosphereCookbook) in four steps, in a grid: clear air, the haze, the bank of fog,
	 * then both. Each panel is 600 x 337 pixels.
	 */
	static BufferedImage atmosphereSteps() {
		Atmosphere haze = new Atmosphere(AtmosphereCookbook.createHaze());
		Atmosphere fog = new Atmosphere().addFog(AtmosphereCookbook.createFogBank());
		BufferedImage clear = AtmosphereCookbook.createScene(null, 750).render();
		BufferedImage hazy = AtmosphereCookbook.createScene(haze, 750).render();
		BufferedImage foggy = AtmosphereCookbook.createScene(fog, 750).render();
		BufferedImage both = AtmosphereCookbook.createScene(AtmosphereCookbook.createAtmosphere(), 750).render();
		BufferedImage top = sideBySide(clear, hazy), bottom = sideBySide(foggy, both);
		BufferedImage out = new BufferedImage(top.getWidth(), top.getHeight() * 2 + 6, BufferedImage.TYPE_INT_RGB);
		java.awt.Graphics2D g = out.createGraphics();
		g.setColor(java.awt.Color.WHITE);
		g.fillRect(0, 0, out.getWidth(), out.getHeight());
		g.drawImage(top, 0, 0, null);
		g.drawImage(bottom, 0, top.getHeight() + 6, null);
		g.dispose();
		return out;
	}

	/** Two images of the same size side by side, with a thin white gap */
	static BufferedImage sideBySide(BufferedImage left, BufferedImage right) {
		int gap = 6;
		BufferedImage out = new BufferedImage(left.getWidth() + gap + right.getWidth(), left.getHeight(), BufferedImage.TYPE_INT_RGB);
		java.awt.Graphics2D g = out.createGraphics();
		g.setColor(java.awt.Color.WHITE);
		g.fillRect(0, 0, out.getWidth(), out.getHeight());
		g.drawImage(left, 0, 0, null);
		g.drawImage(right, left.getWidth() + gap, 0, null);
		g.dispose();
		return out;
	}

	/** The scene of TestLightingPoint1Shadows: a point light orbiting in an open-top room with four objects, at image i of the animation. */
	static DemoScene pointShadows(int image, int pixelsPerUnit) {
		PerspectiveContext perspective = new PerspectiveContext(0.8f, 0.45f, 1, 100, PerspectiveType.FRUSTUM, pixelsPerUnit);
		RenderContext rContext = new RenderContext(TestLightingPoint1Shadows.createRenderContext());
		rContext.setDisplayLandmark(false);
		PointLight light = TestLightingPoint1Shadows.createPointLight();
		TestLightingPoint1Shadows.moveLight(light, image);
		return new DemoScene(TestLightingPoint1Shadows.createWorld(), TestLightingPoint1Shadows.createLighting(light), new Vector4(10, -10, 18, 1),
				new Vector4(0, 0, 1, 1), rContext, perspective);
	}

	// ***** Tools *****

	static void save(BufferedImage image, File file) throws IOException {
		String name = file.getName();
		String format = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
		if (!ImageIO.write(image, format.equals("jpg") ? "jpeg" : format, file)) {
			throw new IOException("No writer for the format of " + file);
		}
	}

	/**
	 * @param args [directory] [image...]: output directory (default: resources/doc/images), then the names of the
	 *             images to generate (default: all)
	 */
	public static void main(String[] args) throws IOException {
		File directory = new File(args.length > 0 ? args[0] : DEFAULT_DIRECTORY);
		List<String> selection = args.length > 1 ? Arrays.asList(args).subList(1, args.length) : null;
		if (!directory.isDirectory() && !directory.mkdirs()) {
			throw new IOException("Cannot create the directory " + directory);
		}
		Map<String, Supplier<BufferedImage>> images = images();
		if (selection != null && !images.keySet().containsAll(selection)) {
			throw new IllegalArgumentException("Unknown image in " + selection + ", known images: " + images.keySet());
		}
		for (Map.Entry<String, Supplier<BufferedImage>> image : images.entrySet()) {
			if (selection == null || selection.contains(image.getKey())) {
				File file = new File(directory, image.getKey());
				save(image.getValue().get(), file);
				System.out.println("Saved " + file);
			}
		}
	}
}
