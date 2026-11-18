package com.aventura.engine;

import static org.junit.Assert.*;

import java.awt.Color;
import java.awt.image.BufferedImage;

import org.junit.Test;

import com.aventura.context.PerspectiveContext;
import com.aventura.context.RenderContext;
import com.aventura.context.RenderContext.RenderingType;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.AmbientLight;
import com.aventura.model.light.DirectionalLight;
import com.aventura.model.light.Lighting;
import com.aventura.model.perspective.PerspectiveType;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Sphere;
import com.aventura.model.world.shape.Trellis;
import com.aventura.view.SwingView;

/**
 * Off-screen rendering tests of the rendering types (backlog #5): UNLIT ignores the lights, MONOCHROME is a
 * hidden-line wireframe (single fill color + depth-tested edges), and the texture option is honored by every
 * filled rendering type (FLAT included, which replaces the former PLAIN that always forced textures).
 */
public class TestRenderingTypes {

	private static final Color BACKGROUND = Color.BLACK;
	private static final Color SPHERE_COLOR = new Color(220, 60, 60);

	private static Lighting lighting() {
		return new Lighting(new DirectionalLight(new Vector3(-1, 0.6f, -0.6f), 0.4f), new AmbientLight(0.1f));
	}

	private static BufferedImage render(World world, Camera camera, RenderContext rc) {
		System.setProperty("java.awt.headless", "true");
		PerspectiveContext p = new PerspectiveContext(0.8f, 0.45f, 1, 100, PerspectiveType.FRUSTUM, 200);
		SwingView view = new SwingView(p);
		RenderEngine engine = new RenderEngine(world, lighting(), camera, rc, p);
		engine.setView(view);
		engine.render();
		return view.getImageView();
	}

	/** A sphere at the origin, seen from the front and filling the centre of the image. */
	private static BufferedImage renderSphere(RenderContext rc) {
		World world = new World();
		world.setBackgroundColor(BACKGROUND);
		Sphere ball = new Sphere(1.5f, 16);
		ball.setColor(SPHERE_COLOR);
		world.addElement(ball);
		world.build();
		return render(world, new Camera(new Vector4(0, -8, 0, 1), new Vector4(0, 0, 0, 1), Vector4.zAxis()), rc);
	}

	/** A white floor with a green/blue checker texture, seen from above. */
	private static BufferedImage renderTexturedFloor(RenderContext rc) {
		Texture tex = new Texture(2, 2);
		tex.setColor(0, 0, Color.GREEN);
		tex.setColor(1, 1, Color.GREEN);
		tex.setColor(0, 1, Color.BLUE);
		tex.setColor(1, 0, Color.BLUE);
		World world = new World();
		world.setBackgroundColor(BACKGROUND);
		Trellis floor = new Trellis(8, 8, 4, 4, tex);
		floor.setColor(Color.WHITE);
		world.addElement(floor);
		world.build();
		return render(world, new Camera(new Vector4(0, -1, 10, 1), new Vector4(0, 0, 0, 1), Vector4.zAxis()), rc);
	}

	private static Color center(BufferedImage img) {
		return new Color(img.getRGB(img.getWidth() / 2, img.getHeight() / 2));
	}

	private static int count(BufferedImage img, Color c) {
		int n = 0;
		for (int y = 0; y < img.getHeight(); y++)
			for (int x = 0; x < img.getWidth(); x++)
				if (img.getRGB(x, y) == c.getRGB()) n++;
		return n;
	}

	@Test
	public void testUnlit_drawsTheBaseColorWhateverTheLights() {
		BufferedImage img = renderSphere(RenderContext.RENDER_STANDARD_UNLIT);
		assertEquals(SPHERE_COLOR, center(img));
		// No shading at all: every non-background pixel is exactly the sphere color
		int total = img.getWidth() * img.getHeight();
		assertEquals(total, count(img, SPHERE_COLOR) + count(img, BACKGROUND));
		assertTrue(count(img, SPHERE_COLOR) > 0);
	}

	@Test
	public void testFlat_isShaded() {
		// Weak lights: the lit sphere is darker than its base color
		Color c = center(renderSphere(RenderContext.RENDER_STANDARD_FLAT));
		assertTrue("FLAT must apply the lighting: " + c, c.getRed() < SPHERE_COLOR.getRed());
		// Sphere's triangles are wound inwards: the flat normal must still face outwards (lit like INTERPOLATE)
		Color smooth = center(renderSphere(RenderContext.RENDER_STANDARD_INTERPOLATE));
		assertTrue("FLAT sphere must be lit like the INTERPOLATE one: " + c + " vs " + smooth, Math.abs(c.getRed() - smooth.getRed()) < 40);
	}

	@Test
	public void testMonochrome_defaultFillIsTheBackgroundAndEdgesUseTheElementColor() {
		BufferedImage img = renderSphere(RenderContext.RENDER_MONOCHROME);
		int total = img.getWidth() * img.getHeight();
		int edges = count(img, SPHERE_COLOR);
		assertTrue("Edges must be drawn", edges > 0);
		assertEquals("Only the fill (= background) and edge colors are expected", total, edges + count(img, BACKGROUND));
	}

	@Test
	public void testMonochrome_fillColor() {
		BufferedImage img = renderSphere(new RenderContext(RenderContext.RENDER_MONOCHROME).setMonochromeColor(Color.BLUE));
		int total = img.getWidth() * img.getHeight();
		int fill = count(img, Color.BLUE);
		assertTrue("Faces must be filled with the monochrome color", fill > total / 20);
		assertEquals(total, fill + count(img, SPHERE_COLOR) + count(img, BACKGROUND));
	}

	@Test
	public void testMonochrome_hidesTheEdgesBehindTheFaces() {
		// Without backface culling, LINE shows the edges of the back of the sphere, MONOCHROME must hide them
		int lineEdges = count(renderSphere(new RenderContext(RenderingType.LINE).setBackFaceCulling(false)), SPHERE_COLOR);
		int monoEdges = count(renderSphere(new RenderContext(RenderContext.RENDER_MONOCHROME).setBackFaceCulling(false)), SPHERE_COLOR);
		System.out.println("Edge pixels: LINE " + lineEdges + ", MONOCHROME " + monoEdges);
		assertTrue("Hidden edges must not be drawn (" + monoEdges + " vs " + lineEdges + ")", monoEdges < lineEdges * 0.8);
		assertTrue(monoEdges > lineEdges * 0.3);
	}

	@Test
	public void testUnlit_textureOption() {
		assertEquals(Color.WHITE, center(renderTexturedFloor(RenderContext.RENDER_STANDARD_UNLIT)));
		Color textured = center(renderTexturedFloor(new RenderContext(RenderContext.RENDER_STANDARD_UNLIT).setTextureProcessing(true)));
		assertEquals("A texture sample (green or blue, no red): " + textured, 0, textured.getRed());
	}

	@Test
	public void testFlat_honorsTheTextureOption() {
		// The former PLAIN always forced the texture: FLAT must follow setTextureProcessing()
		Color plain = center(renderTexturedFloor(RenderContext.RENDER_STANDARD_FLAT));
		Color textured = center(renderTexturedFloor(new RenderContext(RenderContext.RENDER_STANDARD_FLAT).setTextureProcessing(true)));
		assertTrue("Without texture the white floor is grey (R = G = B): " + plain, plain.getRed() == plain.getGreen() && plain.getGreen() == plain.getBlue() && plain.getRed() > 0);
		assertEquals("With texture, no red component: " + textured, 0, textured.getRed());
	}
}
