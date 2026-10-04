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
package com.aventura.view;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import com.aventura.context.PerspectiveContext;
import com.aventura.tools.tracing.Tracer;

/**
* ImageView is a concrete, GUI-independent GUIView: it renders into java.awt.image.BufferedImage
* images (no Swing, no window), using the double buffer technique:
* - the RenderEngine draws the new frame into the BACK buffer (initView(), drawPixel(), drawLine()...);
* - renderView() swaps it to the FRONT buffer, which is then safe to read (getImageView()) while
*   the next frame is being drawn into a new back buffer. A front image is never modified after the
*   swap, so it can be read from another thread (e.g. a GUI thread) without tearing.
* 
* It can therefore be used as is:
* - headless / off-screen, e.g. to render into image files (saveImage()) or for tests;
* - with any GUI toolkit: register a frame listener (setFrameListener()) to be notified, after each
*   renderView(), of the new front image to display (e.g. convert it for JavaFX or SWT, or repaint a
*   component). SwingView is such a specialization for Swing.
* 
* Coordinates are centered on the image, Y axis up (see drawPixel()).
* 
* @author Olivier BARRY
* @since September 2026 (extracted from SwingView)
 */
public class ImageView extends GUIView {

	// Front buffer: the last complete frame, never modified after the swap (volatile: read by other threads)
	protected volatile BufferedImage frontbuffer;
	
	// Back buffer: the frame being drawn
	protected BufferedImage backbuffer;
	protected Graphics2D backgraph;
	
	// Optional notification of each new frame (called by renderView(), in the rendering thread)
	private Consumer<BufferedImage> frameListener = null;
	
	/**
	 * Create an ImageView of the pixel size of the PerspectiveContext.
	 */
	public ImageView(PerspectiveContext context) {
		super(context);
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "Creating new " + getClass().getSimpleName() + ". Width: "+width+", Height: "+height);
		initFront();
	}

	/**
	 * Create an ImageView of the given pixel size.
	 */
	public ImageView(int width, int height) {
		super(width, height);
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "Creating new " + getClass().getSimpleName() + ". Width: "+width+", Height: "+height);
		initFront();
	}

	@Override
	public void initView() {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "Initializing " + getClass().getSimpleName());
		initBack();
	}
	
	@Override
	public void initView(int width, int height) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "Initializing " + getClass().getSimpleName());
		this.width  = width;
		this.height = height;
		initBack();
	}
	
	protected void initFront() {
		frontbuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
	}
	
	protected void initBack() {
		// A NEW back buffer image for each frame: the previous one became the front buffer and must
		// stay untouched while it may be displayed
		backbuffer = new BufferedImage(width,height, BufferedImage.TYPE_INT_RGB);
		backgraph = (Graphics2D)backbuffer.getGraphics();
		// Fill image with background color pixels
		backgraph.setColor(backgroundColor != null ? backgroundColor : DEFAULT_BACKGROUND_COLOR);
		backgraph.fillRect(0, 0, width, height);
		// Translate origin of the graphic to the center of the image
		backgraph.translate(width/2, height/2);
	}

	/**
	 * Swap: the back buffer (the frame just drawn) becomes the front buffer, then frameRendered() is called.
	 */
	@Override
	public void renderView() {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "Render " + getClass().getSimpleName());
		if (backbuffer == null) return; // nothing drawn yet
		frontbuffer = backbuffer;
		frameRendered(frontbuffer);
	}
	
	/**
	 * Called by renderView() after each swap, in the rendering thread. Notifies the frame listener, if any.
	 * Subclasses (e.g. SwingView) can override it to notify their GUI, calling super.frameRendered().
	 * @param image the new front image
	 */
	protected void frameRendered(BufferedImage image) {
		Consumer<BufferedImage> listener = frameListener;
		if (listener != null) listener.accept(image);
	}
	
	/**
	 * Registers a listener called after each rendered frame (in the rendering thread) with the new front image,
	 * e.g. to hand it over to any GUI toolkit or to save it. Replaces the previous listener; null to remove it.
	 */
	public void setFrameListener(Consumer<BufferedImage> listener) {
		this.frameListener = listener;
	}
	
	/**
	 * @return the Front Buffer image: the last complete frame (to be displayed or saved)
	 */
	public BufferedImage getImageView() {
		return frontbuffer;
	}
	
	/**
	 * Saves the last complete frame (front buffer) into an image file.
	 * @param file the file to write
	 * @param format an ImageIO informal format name, e.g. "png" or "jpg"
	 * @throws IOException if the image cannot be written, or if no writer exists for this format
	 */
	public void saveImage(File file, String format) throws IOException {
		if (!ImageIO.write(frontbuffer, format, file)) {
			throw new IOException("No image writer for format: " + format);
		}
	}

	@Override
	public void setColor(Color c) {
		backgraph.setColor(c);
	}
	
	@Override
	public void setBackgroundColor(Color c) {
		this.backgroundColor = c;
	}
	
	private boolean inImage(int x, int y) {
		return x>=-width/2 && x<width/2 && y<=height/2 && y>-height/2;
	}
	
	@Override
	public void drawPixel(int x, int y) {
		drawImageLine(x,y,x,y);
	}

	/**
	 * Draw a pixel with coordinates centered on the image, Y axis up (converted to the image coordinates,
	 * Y axis down):
	 * 
	 *   ^ Y			  +------> X
	 *   |				  |
	 *   |			-->   |  (image coordinates)
	 *   |				  |
	 *   +------> X       v Y
	 * 
	 * @param x
	 * @param y (Y axis up)
	 * @param c the Color of the pixel to draw
	 */
	@Override
	public void drawPixel(int x, int y, Color c) {
		if (inImage(x, y)) backbuffer.setRGB(x+width/2, -y+height/2, c.getRGB());
	}

	@Override
	public void drawLine(int x1, int y1, int x2, int y2) {
		drawImageLine(x1,y1,x2,y2);
	}
	
	/**
	 * Draw a line with coordinates centered on the image, Y axis up (see drawPixel()).
	 */
	protected void drawImageLine(int x1, int y1, int x2, int y2) {
		backgraph.drawLine(x1,-y1,x2,-y2);
	}

	/**
	 * @return the Color of the pixel (centered coordinates, Y axis up) in the frame being drawn (back buffer),
	 *         or null if outside of the image or if no frame is being drawn
	 */
	@Override
	public Color getPixel(int x, int y) {
		if (backbuffer == null || !inImage(x, y)) return null;
		return new Color(backbuffer.getRGB(x+width/2, -y+height/2));
	}

	/**
	 * Direct (no Color allocation) version of GUIView.addPixel() on the back buffer.
	 */
	@Override
	public void addPixel(int x, int y, float r, float g, float b) {
		if (backbuffer == null || !inImage(x, y)) return;
		int ix = x + width / 2, iy = -y + height / 2;
		int p = backbuffer.getRGB(ix, iy);
		int pr = Math.min(255, ((p >> 16) & 255) + Math.round(r * 255));
		int pg = Math.min(255, ((p >> 8) & 255) + Math.round(g * 255));
		int pb = Math.min(255, (p & 255) + Math.round(b * 255));
		backbuffer.setRGB(ix, iy, (pr << 16) | (pg << 8) | pb);
	}

	/**
	 * Fill the back buffer with the content of a MapView, as grey levels. Caution: the MapView should be
	 * normalized (values in [0, 1], see MapView.normalizeMap()) before the call. If the map is larger than
	 * the view, it is cropped.
	 */
	@Override
	public void initView(MapView map) {
		
		initBack();
		
		int minX = map.width < this.width ? map.width : this.width;
		int minY = map.height < this.height ? map.height : this.height;

		for (int x=0; x<minX; x++ ) {
			for (int y=0; y<minY; y++) {		
				float v = map.get(x,y);
				Color c = new Color(v, v, v);
				backbuffer.setRGB(x, minY-y-1, c.getRGB());
			}
		}
	}

}
