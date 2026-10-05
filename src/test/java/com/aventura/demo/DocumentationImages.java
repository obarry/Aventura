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
import com.aventura.cookbook.CookbookGallery;
import com.aventura.math.transform.Transformation;
import com.aventura.math.vector.Vector4;
import com.aventura.model.perspective.PerspectiveType;
import com.aventura.model.world.World;
import com.aventura.model.world.WrongArraySizeException;
import com.aventura.model.world.shape.Trellis;
import com.aventura.model.light.PointLight;
import com.aventura.model.light.ShadowFilter;
import com.aventura.model.light.SpotLight;
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
 *     fractal_landscape.jpg   FractalLandscape_MouseMoving, landscape from a fixed random seed, seen from afar
 *     spotlights.jpg          TestLightingSpot1
 *     spot_shadows.jpg        TestLightingSpot3Shadows, one image of its animation (shadows of a SpotLight)
 *     soft_shadows.png        TestLightingSpot3Shadows, hard (left) and PCF 3x3 (right) shadows with a small shadow map
 *     point_shadows.jpg       TestLightingPoint1Shadows, one image of its animation (cube-map shadows of a PointLight)
 *     sun_street.jpg          TestLightingSun, the visible sun (disc, halo, lens flare, light shafts) in a street between tower blocks
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
	/** Images of the TestLightingSpot3Shadows and TestLightingPoint1Shadows animations (0 .. NB_IMAGES) */
	static final int SPOT_SHADOWS_IMAGE = 20;
	static final int POINT_SHADOWS_IMAGE = 45;
	/** Angle of the camera of the TestLightingSun sweep (degrees, from -30 to +5): the sun is partly hidden by a building, with its flare and shafts */
	static final float SUN_STREET_YAW = -12.5f;
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
		scenes.put("cookbook_gallery.png", () -> CookbookGallery.createScene(1000));
		return scenes;
	}

	/** The images, by file name, in the order of generation: the README's program, then the scenes rendered off-screen */
	static Map<String, Supplier<BufferedImage>> images() {
		Map<String, Supplier<BufferedImage>> images = new LinkedHashMap<>();
		images.put("hello_aventura.png", DocumentationImages::helloAventura);
		images.put("soft_shadows.png", DocumentationImages::softShadows);
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
	 * The fractal landscape of FractalLandscape_MouseMoving (same size, resolution, sea level, colors and light),
	 * generated from a fixed seed, seen from farther than the demo's initial position so that it is fully visible
	 * (1280 x 720 pixels). The image of the documentation is the 960 x 260 band around it (crop).
	 */
	static DemoScene fractalLandscape(long seed) {
		float size = 4;
		int n = 128;
		float[][] altitudes = new float[n + 1][n + 1];
		FractalLandscape_MouseMoving.createLandscape(altitudes, size, n, new Random(seed));
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
		FractalLandscape_MouseMoving.colorTrianglesByAltitude(trellis);

		PerspectiveContext perspective = new PerspectiveContext(0.8f, 0.45f, 0.8f, 100, PerspectiveType.FRUSTUM, 1600);
		RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE);
		rContext.setTextureProcessing(false);
		return new DemoScene(world, FractalLandscape_MouseMoving.createLighting(), new Vector4(7, -2, 2.4f, 1), new Vector4(0, 0, 1.3f, 1),
				rContext, perspective).crop(241, 190, 960, 260);
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
