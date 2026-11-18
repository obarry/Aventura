package com.aventura.view;

import static org.junit.Assert.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.Test;

/**
 * Tests for ImageView (GUI-independent double buffered view), SwingView's repaint notification and MapView fixes.
 * All headless.
 */
public class TestImageView {

	@Test
	public void testDoubleBuffer() {
		ImageView v = new ImageView(20, 10);
		v.setBackgroundColor(Color.BLUE);
		v.initView();
		v.drawPixel(0, 0, Color.RED);
		assertEquals(Color.RED, v.getPixel(0, 0));
		assertEquals(Color.BLUE, v.getPixel(3, 2));
		// Not swapped yet: the front buffer is still the initial (black) image
		assertEquals(Color.BLACK.getRGB(), v.getImageView().getRGB(10, 5));

		v.renderView();
		BufferedImage front = v.getImageView();
		assertEquals(Color.RED.getRGB(), front.getRGB(10, 5)); // (0,0) is the center, Y axis up
		assertEquals(Color.BLUE.getRGB(), front.getRGB(0, 0));

		// Next frame: drawing into the new back buffer does not touch the displayed front image
		v.initView();
		v.drawPixel(0, 0, Color.GREEN);
		assertEquals(Color.RED.getRGB(), front.getRGB(10, 5));
		assertSame(front, v.getImageView());
	}

	/** Draws a frame with a red pixel at (x, 0) on a blue background, swaps it and returns the new front image */
	private static BufferedImage frame(ImageView v, int x) {
		v.initView();
		v.drawPixel(x, 0, Color.RED);
		v.renderView();
		return v.getImageView();
	}

	@Test
	public void testNewImageForEachFrameByDefault() {
		ImageView v = new ImageView(20, 10);
		v.setBackgroundColor(Color.BLUE);
		assertFalse(v.isImageReuse());
		BufferedImage first = frame(v, 0);
		for (int i = 1; i < 5; i++) {
			assertNotSame("A new image for each frame", first, frame(v, i));
		}
		assertEquals("The first image is never drawn again", Color.RED.getRGB(), first.getRGB(10, 5));
	}

	@Test
	public void testImageReuse() {
		ImageView v = new ImageView(20, 10);
		v.setBackgroundColor(Color.BLUE);
		v.setImageReuse(true);
		BufferedImage f1 = frame(v, 1), f2 = frame(v, 2), f3 = frame(v, 3);
		assertNotSame(f1, f2);
		assertNotSame(f2, f3);
		assertNotSame(f1, f3);
		// While the 4th frame is drawn, the last two images (on the screen, or being painted) are left untouched
		v.initView();
		v.drawPixel(4, 0, Color.RED);
		assertEquals(Color.RED.getRGB(), f2.getRGB(10 + 2, 5));
		assertEquals(Color.RED.getRGB(), f3.getRGB(10 + 3, 5));
		v.renderView();
		BufferedImage f4 = v.getImageView();
		assertSame("Three images in rotation: the 1st one is drawn again for the 4th frame", f1, f4);
		assertEquals("Cleared to the background before being drawn again", Color.BLUE.getRGB(), f4.getRGB(10 + 1, 5));
		assertEquals(Color.RED.getRGB(), f4.getRGB(10 + 4, 5));
		assertSame(f2, frame(v, 5));
		// A new size: new images
		v.initView(30, 10);
		v.renderView();
		assertEquals(30, v.getImageView().getWidth());
		// Off again: a new image for each frame
		v.setImageReuse(false);
		BufferedImage g1 = frame(v, 0);
		for (int i = 0; i < 4; i++) assertNotSame(g1, frame(v, 0));
	}

	@Test
	public void testYAxisUp() {
		ImageView v = new ImageView(20, 10);
		v.initView();
		v.drawPixel(0, 4, Color.WHITE); // 4 pixels above the center
		v.renderView();
		assertEquals(Color.WHITE.getRGB(), v.getImageView().getRGB(10, 1));
	}

	@Test
	public void testGetPixelOutsideIsNull() {
		ImageView v = new ImageView(20, 10);
		assertNull(v.getPixel(0, 0)); // no frame being drawn
		v.initView();
		assertNull(v.getPixel(100, 0));
		assertNotNull(v.getPixel(-10, -4));
	}

	@Test
	public void testFrameListener() {
		ImageView v = new ImageView(8, 8);
		List<BufferedImage> frames = new ArrayList<>();
		v.setFrameListener(frames::add);
		v.initView();
		v.renderView();
		v.initView();
		v.renderView();
		assertEquals(2, frames.size());
		assertSame(v.getImageView(), frames.get(1));
		v.setFrameListener(null);
		v.initView();
		v.renderView();
		assertEquals(2, frames.size());
	}

	@Test
	public void testSaveImage() throws Exception {
		ImageView v = new ImageView(16, 12);
		v.setBackgroundColor(Color.ORANGE);
		v.initView();
		v.renderView();
		File f = File.createTempFile("aventura", ".png");
		f.deleteOnExit();
		v.saveImage(f, "png");
		BufferedImage read = ImageIO.read(f);
		assertEquals(16, read.getWidth());
		assertEquals(12, read.getHeight());
		assertEquals(Color.ORANGE.getRGB(), read.getRGB(3, 3));
	}

	@Test
	public void testSwingViewRepaintsItsComponent() {
		final int[] repaints = { 0 };
		Component comp = new Component() {
			private static final long serialVersionUID = 1L;
			@Override
			public void repaint() {
				repaints[0]++;
			}
		};
		List<BufferedImage> frames = new ArrayList<>();
		SwingView v = new SwingView(8, 8, comp);
		v.setFrameListener(frames::add);
		v.initView();
		v.renderView();
		assertEquals(1, repaints[0]);
		assertEquals(1, frames.size()); // the listener works for a SwingView too
		assertTrue(v instanceof ImageView);
	}

	@Test
	public void testMapViewArrayConstructorDimensions() {
		MapView m = new MapView(new float[5][3]); // map[x][y]
		assertEquals(5, m.getViewWidth());
		assertEquals(3, m.getViewHeight());
		m.set(4, 2, 1f);
		assertEquals(1f, m.get(4, 2), 0f);
		assertEquals(1, new MapView(new float[1][7]).getViewWidth()); // used to crash
	}

	@Test
	public void testMapViewResize() {
		MapView m = new MapView(2, 2);
		m.initView(10, 6);
		assertEquals(10, m.getViewWidth());
		m.set(9, 5, 3f); // used to raise ArrayIndexOutOfBoundsException
		assertEquals(3f, m.get(9, 5), 0f);
		assertEquals(0f, m.get(0, 0), 0f);
	}

	@Test
	public void testMapViewNormalizeUniform() {
		MapView m = new MapView(3, 3);
		m.initView(0.7f);
		m.normalizeMap();
		assertEquals(0f, m.get(1, 1), 0f); // used to be NaN
	}
}
