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
package com.aventura.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;

import org.junit.Test;

import com.aventura.context.PerspectiveContext;
import com.aventura.context.RenderContext;
import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.AmbientLight;
import com.aventura.model.light.DirectionalLight;
import com.aventura.model.light.Lighting;
import com.aventura.model.material.AbstractMaterial;
import com.aventura.model.material.SolidMaterial;
import com.aventura.model.material.TexturedMaterial;
import com.aventura.model.perspective.PerspectiveType;
import com.aventura.model.texture.SurfaceMap;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.Element;
import com.aventura.model.world.GeometryStatistics;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Box;
import com.aventura.model.world.shape.Trellis;
import com.aventura.model.world.triangle.Triangle;
import com.aventura.tools.tracing.Tracer;
import com.aventura.view.ImageView;

/**
 * Tests of the emission (Element.setEmissionColor(), setEmissionMap(), Material.emissionAt()): a surface that glows by
 * itself, whatever the lights and the shadows, and the maps it is read from (SurfaceMap, Texture).
 */
public class TestEmission {

	private static final Color GLOW = new Color(0, 200, 255);

	// ***** The materials *****

	@Test
	public void testMaterial_noEmissionByDefault() {
		System.out.println("***** Test emission : no emission by default *****");
		Fragment fragment = new Fragment();
		SolidMaterial solid = new SolidMaterial(Color.GRAY, Color.WHITE, 0, 1);
		assertNull(solid.emissionAt(fragment));
		assertTrue(!solid.isEmissive());
		assertSame(solid, solid.setEmission(GLOW));
		assertEquals(GLOW, solid.emissionAt(fragment));
	}

	@Test
	public void testMaterial_emissionMap_readLikeTheTexture() {
		System.out.println("***** Test emission : the emission map is read at the texture coordinates, tinted *****");
		Texture halves = halves();
		Fragment fragment = new Fragment();
		fragment.setTexCoord(0.125f, 0.5f, 1); // In the black half
		AbstractMaterial material = new TexturedMaterial(white(), Triangle.TEXTURE_ISOTROPIC, null, Color.WHITE, 0, 1)
				.setEmission(null, halves, Triangle.TEXTURE_ISOTROPIC, false);
		assertEquals(Color.BLACK, material.emissionAt(fragment));
		fragment.setTexCoord(0.875f, 0.5f, 1); // In the white half: the map as it is, white
		assertEquals(Color.WHITE, material.emissionAt(fragment));
		// Tinted by the emission color
		material.setEmission(GLOW, halves, Triangle.TEXTURE_ISOTROPIC, false);
		assertEquals(GLOW, material.emissionAt(fragment));
		// The homogeneous coordinates are divided by w, as for the texture
		fragment.setTexCoord(0.875f * 2, 0.5f * 2, 2);
		assertEquals(GLOW, material.emissionAt(fragment));
		// No texture coordinates: the map cannot be read, the color alone
		fragment.clearTexCoord();
		assertEquals(GLOW, material.emissionAt(fragment));
	}

	// ***** The maps *****

	@Test
	public void testTexture_isASurfaceMap() {
		System.out.println("***** Test emission : a Texture is a SurfaceMap *****");
		SurfaceMap map = new Texture(16, 8);
		assertEquals("16 x 8 texture", map.getName());
		// The chained setters keep the type of the Texture
		Texture texture = new Texture(4, 4).setWrapMode(SurfaceMap.WrapMode.REPEAT).setName("tiles");
		assertEquals("tiles", texture.getName());
		assertEquals(Texture.WrapMode.REPEAT, texture.getWrapMode());
		map.buildMipmaps();
		assertEquals(5, map.getNbMipmapLevels());
		assertEquals(1, map.getMipmapHeight(4));
	}

	// ***** The images *****

	@Test
	public void testDark_onlyTheEmissionShows() {
		System.out.println("***** Test emission : in the dark, the surface shows its emission color *****");
		Scene scene = new Scene();
		scene.plane.setEmissionColor(GLOW);
		int[] image = scene.render(new Lighting());
		int glowing = count(image, GLOW.getRGB());
		assertTrue("the plane covers most of the image: " + glowing, glowing > image.length / 2);
	}

	@Test
	public void testDark_emissionMap_onlyItsBrightTexelsGlow() {
		System.out.println("***** Test emission : an emission map, only its bright texels glow *****");
		Scene scene = new Scene();
		scene.plane.setEmissionMap(halves());
		scene.plane.setEmissionColor(GLOW); // The tint of the map
		int[] image = scene.render(new Lighting());
		int glowing = count(image, GLOW.getRGB()), black = count(image, Color.BLACK.getRGB());
		assertTrue("half of the plane glows: " + glowing, glowing > image.length / 4);
		assertTrue("the other half is black: " + black, black > image.length / 4);
		// The textures off: the map is not read, the tint alone glows everywhere
		scene.context.setTextureProcessing(false);
		int[] untextured = scene.render(new Lighting());
		assertTrue(count(untextured, GLOW.getRGB()) > image.length / 2);
	}

	@Test
	public void testLit_emissionAddedWhateverTheLightsAndShadows() {
		System.out.println("***** Test emission : added to the shaded color, in the light and in the shadows *****");
		Scene scene = new Scene();
		int[] without = scene.render(sunAndAmbient());
		Color glow = new Color(40, 30, 20);
		scene.plane.setEmissionColor(glow);
		int[] with = scene.render(sunAndAmbient());
		int changed = 0;
		for (int i = 0; i < with.length; i++) {
			if (with[i] == without[i]) continue;
			changed++;
			assertEquals("red", Math.min(255, red(without[i]) + glow.getRed()), red(with[i]), 1);
			assertEquals("green", Math.min(255, green(without[i]) + glow.getGreen()), green(with[i]), 1);
			assertEquals("blue", Math.min(255, blue(without[i]) + glow.getBlue()), blue(with[i]), 1);
		}
		assertTrue("the plane, lit and in the shadow of the box: " + changed, changed > with.length / 2);
	}

	@Test
	public void testInheritance_elementThenTriangle() {
		System.out.println("***** Test emission : inherited by the sub-elements, overridden by a triangle *****");
		Scene scene = new Scene();
		scene.group.setEmissionColor(Color.RED); // The parent of the plane
		int[] inherited = scene.render(new Lighting());
		assertTrue(count(inherited, Color.RED.getRGB()) > inherited.length / 2);
		scene.plane.setEmissionColor(GLOW); // The plane overrides it
		for (Triangle t : scene.plane.getTriangles()) t.setEmissionColor(Color.GREEN); // ... and its triangles too
		int[] overridden = scene.render(new Lighting());
		// Only the box, beside the plane in the group, still has the emission of the group
		assertTrue(count(overridden, Color.RED.getRGB()) < overridden.length / 8);
		assertEquals(0, count(overridden, GLOW.getRGB()));
		assertTrue(count(overridden, Color.GREEN.getRGB()) > overridden.length / 2);
	}

	@Test
	public void testStatistics_theEmissionMapIsATexture() {
		System.out.println("***** Test emission : the memory of the emission maps is counted *****");
		Scene scene = new Scene();
		long before = GeometryStatistics.of(scene.world).getTextureMemorySize();
		Texture map = halves();
		scene.plane.setEmissionMap(map);
		assertEquals(before + map.getMemorySize(), GeometryStatistics.of(scene.world).getTextureMemorySize());
	}

	// ***** Helpers *****

	/** A plane seen from above, a box standing on it, in a group */
	static class Scene {
		final World world = new World();
		final Element group = new Element("group");
		final Trellis plane = new Trellis(12, 12, 6, 6, white());
		final RenderContext context = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE_SHADOWS);

		Scene() {
			context.setTextureProcessing(true);
			world.setBackgroundColor(Color.BLACK);
			plane.setColor(Color.GRAY);
			group.addElement(plane);
			Box box = new Box(2, 2, 3);
			box.setTransformation(new Translation(new Vector3(0, 0, 1.5f)));
			box.setColor(Color.DARK_GRAY);
			group.addElement(box);
			world.addElement(group);
			world.build();
		}

		int[] render(Lighting lighting) {
			PerspectiveContext perspective = new PerspectiveContext(1.6f, 0.9f, 1, 100, PerspectiveType.FRUSTUM, 60);
			Camera camera = new Camera(new Vector4(0, -0.01f, 9, 1), new Vector4(0, 0, 0, 1), Vector4.zAxis());
			ImageView view = new ImageView(perspective);
			RenderEngine engine = new RenderEngine(world, lighting, camera, context, perspective);
			engine.setView(view);
			boolean stats = Tracer.stats;
			Tracer.stats = false;
			try {
				engine.render();
			} finally {
				engine.close();
				Tracer.stats = stats;
			}
			BufferedImage image = view.getImageView();
			return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
		}
	}

	static Lighting sunAndAmbient() {
		DirectionalLight sun = new DirectionalLight(new Vector3(1, 0.5f, -1), 0.7f);
		sun.setShadowMapSize(256);
		return new Lighting(sun, new AmbientLight(0.2f));
	}

	/** A white texture */
	static Texture white() {
		Texture texture = new Texture(2, 2);
		for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) texture.setColor(x, y, Color.WHITE);
		return texture;
	}

	/** A texture black on its left half (s < 0.5), white on its right half */
	static Texture halves() {
		Texture texture = new Texture(8, 2);
		for (int x = 0; x < 8; x++) for (int y = 0; y < 2; y++) texture.setColor(x, y, x < 4 ? Color.BLACK : Color.WHITE);
		return texture;
	}

	static int count(int[] image, int rgb) {
		int n = 0;
		for (int pixel : image) if ((pixel & 0xFFFFFF) == (rgb & 0xFFFFFF)) n++;
		return n;
	}

	static int red(int rgb) { return (rgb >> 16) & 0xFF; }
	static int green(int rgb) { return (rgb >> 8) & 0xFF; }
	static int blue(int rgb) { return rgb & 0xFF; }
}
