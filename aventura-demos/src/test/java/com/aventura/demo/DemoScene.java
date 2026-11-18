package com.aventura.demo;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import com.aventura.context.PerspectiveContext;
import com.aventura.context.RenderContext;
import com.aventura.engine.RenderEngine;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.Lighting;
import com.aventura.model.world.World;
import com.aventura.view.GUIView;
import com.aventura.view.ImageView;

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
 * Everything needed to render a scene, independently of where the image goes: the World (built), its Lighting,
 * a camera position (eye and point of interest, Z up), the RenderContext and the PerspectiveContext (which gives
 * the size of the image).
 *
 * The same DemoScene can be rendered off-screen into an image (render(), used by DocumentationImages) or shown
 * in a window (SceneViewer, which moves the camera around the point of interest). That is why the camera is kept
 * as eye + point of interest rather than as a Camera: a viewer needs the point to turn around.
 *
 * An optional crop keeps only a part of the rendered image in render() (e.g. a wide band of a landscape for the
 * documentation); a viewer shows the whole view.
 *
 * @author Olivier BARRY
 * @since 2026
 */
public class DemoScene {

	private final World world;
	private final Lighting lighting;
	private final Vector4 eye;
	private final Vector4 poi;
	private final RenderContext renderContext;
	private final PerspectiveContext perspective;
	private Rectangle crop = null;

	/**
	 * @param world         the World, already built
	 * @param lighting      its lights
	 * @param eye           position of the camera
	 * @param poi           point the camera looks at (Z is up)
	 * @param renderContext rendering options; copied, so that a viewer can change them (the presets are frozen)
	 * @param perspective   projection and size of the image
	 */
	public DemoScene(World world, Lighting lighting, Vector4 eye, Vector4 poi, RenderContext renderContext, PerspectiveContext perspective) {
		this.world = world;
		this.lighting = lighting;
		this.eye = new Vector4(eye);
		this.poi = new Vector4(poi);
		this.renderContext = new RenderContext(renderContext);
		this.perspective = perspective;
	}

	/**
	 * Keeps only the given part of the image in render() (x, y: top left corner, in pixels).
	 * @return this, for chaining
	 */
	public DemoScene crop(int x, int y, int width, int height) {
		this.crop = new Rectangle(x, y, width, height);
		return this;
	}

	/** A new Camera at the scene's position (Z up) */
	public Camera createCamera() {
		return new Camera(new Vector4(eye), new Vector4(poi), Vector4.zAxis());
	}

	/** A RenderEngine for this scene, the given camera (e.g. moved by a viewer) and view */
	public RenderEngine createEngine(Camera camera, GUIView view) {
		RenderEngine engine = new RenderEngine(world, lighting, camera, renderContext, perspective);
		engine.setView(view);
		return engine;
	}

	/** Renders the scene off-screen, from its camera position, and returns the image (cropped if a crop is set) */
	public BufferedImage render() {
		ImageView view = new ImageView(perspective);
		createEngine(createCamera(), view).render();
		BufferedImage image = view.getImageView();
		if (crop == null) {
			return image;
		}
		BufferedImage cropped = new BufferedImage(crop.width, crop.height, BufferedImage.TYPE_INT_RGB);
		cropped.getGraphics().drawImage(image.getSubimage(crop.x, crop.y, crop.width, crop.height), 0, 0, null);
		return cropped;
	}

	public World getWorld() {
		return world;
	}

	public Lighting getLighting() {
		return lighting;
	}

	/** @return a copy of the position of the camera */
	public Vector4 getEye() {
		return new Vector4(eye);
	}

	/** @return a copy of the point the camera looks at */
	public Vector4 getPoi() {
		return new Vector4(poi);
	}

	/** @return the scene's own (mutable) rendering options, used by every engine created by createEngine() */
	public RenderContext getRenderContext() {
		return renderContext;
	}

	public PerspectiveContext getPerspective() {
		return perspective;
	}
}
