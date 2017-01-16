package com.aventura.engine;

import java.awt.Color;

import com.aventura.context.GraphicContext;
import com.aventura.math.vector.Tools;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.light.Lighting;
import com.aventura.model.world.Segment;
import com.aventura.model.world.Triangle;
import com.aventura.model.world.Vertex;
import com.aventura.tools.color.ColorTools;
import com.aventura.tools.tracing.Tracer;
import com.aventura.view.View;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2017 Olivier BARRY
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
 * This class provides rasterization services to the RenderEngine.
 * It takes charge of all the algorithms to produce pixels from triangles.
 * It requires access to the View in order to draw pixels.
 * zBuffer is managed here.
 * It also behaves according to the provided parameters e.g. in the GraphicContext
 * 
 * 
 * @author Olivier BARRY
 * @since November 2016
 *
 */

public class Rasterizer {
	
	protected GraphicContext graphic;
	protected View view;
	protected Lighting lighting;
	private static double ZBUFFER_INIT_VALUE = 1.0;
	
	// Z buffer
	private double[][] zBuffer;
	int zBuf_width, zBuf_height;
	
	
	// Pixel statistics
	int rendered_pixels = 0;
	int discarded_pixels = 0;
	int not_rendered_pixels = 0;
	
	/**
	 * Creation of Rasterizer without Lighting for tests.
	 * @param graphic
	 */
	public Rasterizer(GraphicContext graphic) {
		this.graphic = graphic;
	}

	/**
	 * Creation of Rasterizer with requested references for run time.
	 * @param graphic
	 */
	public Rasterizer(GraphicContext graphic, Lighting lighting) {
		this.graphic = graphic;
		this.lighting = lighting;
	}
	
	public void setView(View v) {
		this.view = v;
	}
	
	/**
	 * Initialize zBuffer by creating the table. This method is deported from the constructor in order to use it only when necessary.
	 * It is not needed in case of line rendering.
	 */
	public void initZBuffer() {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating zBuffer. Width: "+graphic.getPixelWidth()+" Height: "+graphic.getPixelHeight());
		zBuf_width = 2*graphic.getPixelHalfWidth()+1;
		zBuf_height = 2*graphic.getPixelHalfHeight()+1;
		zBuffer = new double[zBuf_width][zBuf_height];
		
		// Initialization loop with initialization value ( 1 or -1 in homogeneous coordinates ?) that is the farest value for the view Frustum
		// Any value closer will be drawn and the zBuffer in this place will be updated by new value

		for (int i=0; i<zBuf_width; i++)  {
			for (int j=0; j<zBuf_height; j++) {
				zBuffer[i][j] = ZBUFFER_INIT_VALUE;
			}
		}
	}
	
	protected double xScreen(Vector4 v) {
		return v.get3DX()*graphic.getPixelHalfWidth();
	}
	
	protected double yScreen(Vector4 v) {
		return v.get3DY()*graphic.getPixelHalfHeight();
	}
	
	// Method for Segment only Rendering
	//
	
	public void drawTriangleLines(Triangle t, Color c) {
		
		view.setColor(c);
		drawLine(t.getV1(), t.getV2());
		drawLine(t.getV2(), t.getV3());
		drawLine(t.getV3(), t.getV1());
	}
	
	public void drawLine(Segment l) {
		drawLine(l.getV1(), l.getV2());
	}
	
	public void drawLine(Segment l, Color c) {
		drawLine(l.getV1(), l.getV2(), c);
	}
	
	public void drawLine(Vertex v1, Vertex v2) {

		int x1, y1, x2, y2;	
		x1 = (int)(xScreen(v1.getPosition()));
		y1 = (int)(yScreen(v1.getPosition()));
		x2 = (int)(xScreen(v2.getPosition()));
		y2 = (int)(yScreen(v2.getPosition()));

		view.drawLine(x1, y1, x2, y2);
		//bressenham_short((short)x1, (short)y1, (short)x2, (short)y2);
		//bressenham_int(x1, y1, x2, y2);
	}

	public void drawLine(Vertex v1, Vertex v2, Color c) {

		view.setColor(c);
		drawLine(v1, v2);
	}

	
	public void drawVectorFromPosition(Vertex position, Vector3 vector, Color c) {
		
		view.setColor(c);
		drawVectorFromPosition(position, vector);
	}

	public void drawVectorFromPosition(Vertex position, Vector3 vector) {
		
		int x1, y1, x2, y2;
		x1 = (int)(xScreen(position.getPosition()));
		y1 = (int)(yScreen(position.getPosition()));
		
		Vector4 p = position.getPosition().plus(vector); 
		x2 = (int)(xScreen(p));
		y2 = (int)(yScreen(p));

		view.drawLine(x1, y1, x2, y2);
		//bressenham_short((short)x1, (short)y1, (short)x2, (short)y2);
		//bressenham_int(x1, y1, x2, y2);
	}
	
	//
	// End methods for Segment only Rendering
	
	/**
	 * Triangle rasterizetion and zBuffering
	 * Extrapolated from:
	 * https://www.davrous.com/2013/06/21/tutorial-part-4-learning-how-to-write-a-3d-software-engine-in-c-ts-or-js-rasterization-z-buffering/
	 * 
	 * @param tf the (transformed) triangle to render
	 * @param to the (original) triangle
	 * @param col
	 */
	public void rasterizeTriangle(Triangle tf, Triangle to, Color c, boolean interpolate) {
		
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "Rasterize triangle. Color: "+c);
		
		Color col = c; // Let's initialize the base color with the one of the triangle
		Color c1 = null, c2 = null, c3 = null; // Colors to store Vertex colors, if interpolation is requested, else will be kept null

		// Init pixel stats
		rendered_pixels = 0;
		discarded_pixels = 0;
		not_rendered_pixels = 0;
		
		// If no interpolation requested -> plain faces. Then:
		// - calculate normal at Triangle level for shading
		// - calculate shading color once for all triangle
		if (!interpolate || to.isTriangleNormal()) {
			// Calculate normal if not calculated
			if (to.getNormal()==null) to.calculateNormal();
			Vector3 normal = to.getNormal();
			Color shadedCol = computeShadedColor(col, normal);
			// Then use the shaded color instead for whole triangle
			col = shadedCol;
		} else {
			
			// As p1, p2 and p3 may be reshuffled and sorted, this is meaningless to calculate the 3 colors at this stage so we postpone
			// the calculation to later, which increases the complexity and readibility of the algorithm
			
			// TODO Further design improvements will allow to simplify this processing by designing direct link between tf and to
			// (instead of carrying both triangles) e.g. by consolidating at Vertex level both the initial and projected position Vectors
			// This will also help to reduce the memory usage by duplicating position Vectors only (and not the full Vertices)
		}

	    // Lets define p1, p2, p3 in order to always have this order on screen p1, p2 & p3
	    // with p1 always down (thus having the highest possible Y)
	    // then p2 between p1 & p3 (or same level if p2 and p3 on same ordinate)
		
		Vector4 p1, p2, p3;

		p1 = tf.getV1().getPosition();
		p2 = tf.getV2().getPosition();
		p3 = tf.getV3().getPosition();
				
		if (p2.get3DY()<p1.get3DY()) { // p2 lower than p1
			if (p3.get3DY()<p2.get3DY()) { // p3 lower than p2
				p1 = tf.getV3().getPosition();
				p2 = tf.getV2().getPosition();
				p3 = tf.getV1().getPosition();
				if (interpolate && !to.isTriangleNormal()) {
					// Calculate the 3 colors of the 3 Vertex normals
					c1 = computeShadedColor(col, to.getV3().getNormal());
					c2 = computeShadedColor(col, to.getV2().getNormal());
					c3 = computeShadedColor(col, to.getV1().getNormal());					
				}
			} else { // p2 lower than p3
				if (p3.get3DY()<p1.get3DY()) { // p3 lower than p1
					p1 = tf.getV2().getPosition();
					p2 = tf.getV3().getPosition();
					p3 = tf.getV1().getPosition();
					if (interpolate && !to.isTriangleNormal()) {
						// Calculate the 3 colors of the 3 Vertex normals
						c1 = computeShadedColor(col, to.getV2().getNormal());
						c2 = computeShadedColor(col, to.getV3().getNormal());
						c3 = computeShadedColor(col, to.getV1().getNormal());					
					}
				} else { // p1 higher than p3
					p1 = tf.getV2().getPosition();
					p2 = tf.getV1().getPosition();
					p3 = tf.getV3().getPosition();
					if (interpolate && !to.isTriangleNormal()) {
						// Calculate the 3 colors of the 3 Vertex normals
						c1 = computeShadedColor(col, to.getV2().getNormal());
						c2 = computeShadedColor(col, to.getV1().getNormal());
						c3 = computeShadedColor(col, to.getV3().getNormal());					
					}
				}
			}
		} else { // p1 lower than p2
			if (p3.get3DY()<p1.get3DY()) { // p3 lower than p1
				p1 = tf.getV3().getPosition();
				p2 = tf.getV1().getPosition();
				p3 = tf.getV2().getPosition();
				if (interpolate && !to.isTriangleNormal()) {
					// Calculate the 3 colors of the 3 Vertex normals
					c1 = computeShadedColor(col, to.getV3().getNormal());
					c2 = computeShadedColor(col, to.getV1().getNormal());
					c3 = computeShadedColor(col, to.getV2().getNormal());					
				}
			} else { // p1 lower than p3
				if (p3.get3DY()<p2.get3DY()) { // p3 lower than p2
					// No change for p1
					p2 = tf.getV3().getPosition();
					p3 = tf.getV2().getPosition();
					if (interpolate && !to.isTriangleNormal()) {
						// Calculate the 3 colors of the 3 Vertex normals
						c1 = computeShadedColor(col, to.getV1().getNormal());
						c2 = computeShadedColor(col, to.getV3().getNormal());
						c3 = computeShadedColor(col, to.getV2().getNormal());					
					}
				} else {
					// Else keep p1, p2 and p3 as defined
					if (interpolate && !to.isTriangleNormal()) {
						// Calculate the 3 colors of the 3 Vertex normals
						c1 = computeShadedColor(col, to.getV1().getNormal());
						c2 = computeShadedColor(col, to.getV2().getNormal());
						c3 = computeShadedColor(col, to.getV3().getNormal());					
					}
				}
			}
		}
		
	    // Slopes
	    double dP1P2, dP1P3;

	    // http://en.wikipedia.org/wiki/Slope
	    // Computing slopes
	    if (yScreen(p2) - yScreen(p1) > 0) {
	        dP1P2 = (xScreen(p2)-xScreen(p1))/(yScreen(p2)-yScreen(p1));
	    } else { // vertical segment, no slope
	        dP1P2 = 0;
	    }
	    
	    if (yScreen(p3) - yScreen(p1) > 0) {
	        dP1P3 = (xScreen(p3) - xScreen(p1)) / (yScreen(p3) - yScreen(p1));
	    } else { // vertical segment, no slope
	        dP1P3 = 0;
	    }

	    if (dP1P2 > dP1P3) {
	    	
		    // First case where triangle is like that:
		    // P3
		    // +
		    // |\
		    // | \
		    // |  \
		    // |   + P2
		    // |  /
		    // | /
		    // |/
			// +
		    // P1
	    	
	        for (int y = (int)yScreen(p1); y <= (int)yScreen(p3); y++) {
	            if (y < yScreen(p2)) {
	                rasterizeScanLine(y, p1, p3, p1, p2, c1, c3, c1, c2, col, interpolate && !to.isTriangleNormal());
	            } else {
	                rasterizeScanLine(y, p1, p3, p2, p3, c1, c3, c2, c3, col, interpolate && !to.isTriangleNormal());
	            }
	        }

	    } else {
	    	
		    // Second case where triangle is like that:
		    //       P3
		    //        +
		    //       /| 
		    //      / |
		    //     /  |
		    // P2 +   | 
		    //     \  |
		    //      \ |
		    //       \|
			//        +
		    //       P1
	    	
	        for (int y = (int)yScreen(p1); y <= (int)yScreen(p3); y++) {
	            if (y < yScreen(p2)) {
	                rasterizeScanLine(y, p1, p2, p1, p3, c1, c2, c1, c3, col, interpolate && !to.isTriangleNormal());
	            } else {
	                rasterizeScanLine(y, p2, p3, p1, p3, c2, c3, c1, c3, col, interpolate && !to.isTriangleNormal());
	            }
	        }
	    }
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "Rendered pixels for this triangle: "+rendered_pixels+". Discarded: "+discarded_pixels+". Not rendered: "+not_rendered_pixels);
	}
	
	protected void rasterizeScanLine(int y, Vector4 pa, Vector4 pb, Vector4 pc, Vector4 pd, Color ca, Color cb, Color cc, Color cd, Color c, boolean interpolate) {
		
	    // Thanks to current Y, we can compute the gradient to compute others values like
	    // the starting X (sx) and ending X (ex) to draw between
	    // if pa.Y == pb.Y or pc.Y == pd.Y, gradient is forced to 1
		
	    float gradient1 = (float)(yScreen(pa) != yScreen(pb) ? (y - yScreen(pa)) / (yScreen(pb) - yScreen(pa)) : 1);
	    float gradient2 = (float)(yScreen(pc) != yScreen(pd) ? (y - yScreen(pc)) / (yScreen(pd) - yScreen(pc)) : 1);

	    int sx = (int)Tools.interpolate(xScreen(pa), xScreen(pb), gradient1);
	    int ex = (int)Tools.interpolate(xScreen(pc), xScreen(pd), gradient2);

	    // starting Z & ending Z
	    float z1 = Tools.interpolate((float)pa.get3DZ(), (float)pb.get3DZ(), gradient1);
	    float z2 = Tools.interpolate((float)pc.get3DZ(), (float)pd.get3DZ(), gradient2);
	    
	    // drawing a line from left (sx) to right (ex) 
	    for (int x = sx; x < ex; x++) {
	        float gradient = (x-sx)/(float)(ex-sx);
	        float z = Tools.interpolate(z1, z2, gradient);
	        
	        // If color interpolation
	        if (interpolate) {
	        	Color c1, c2;
	        	c1 = ColorTools.interpolateColors(ca, cb, gradient1);
	        	c2 = ColorTools.interpolateColors(cc, cd, gradient2);
	        	c = ColorTools.interpolateColors(c1, c2, gradient);
	        }
	        drawPoint(x, y, z, c);
	    }
	}

	/**
	 * Draw point with zBuffer management
	 * @param x X screen coordinate (origin is in the center of the screen)
	 * @param y Y screen coordinate (origin is in the center of the screen)
	 * @param z Z homogeneous coordinate for Z buffering
	 * @param c Color of the pixel
	 */
	protected void drawPoint(int x, int y, double z, Color c) {
		// Eliminate pixels outside the view screen
		if (isInScreenX(x) && isInScreenY(y)) {
			// Z buffer is [0, width][0, height]
			int zBuf_x = x + graphic.getPixelHalfWidth();
			int zBuf_y = y + graphic.getPixelHalfHeight();
			
			if (zBuf_x<0 || zBuf_x>=zBuf_width) {
				if (Tracer.error) Tracer.traceError(this.getClass(), "Invalid zBuffer_x value while drawing points: "+zBuf_x);
				return;
			}
			
			if (zBuf_y<0 || zBuf_y>=zBuf_height) {
				if (Tracer.error) Tracer.traceError(this.getClass(), "Invalid zBuffer_y value while drawing points: "+zBuf_y);
				return;
			}
			
			if (z>zBuffer[zBuf_x][zBuf_y]) { // Discard pixel
				discarded_pixels++;
				return;
			}
			// Else render pixel
			if (c !=null ) {
				view.setColor(c);
			} else {
			// Compute color at this stage to avoid unused pre-processing (if pixel finally not rendered)
			// Color is computed from
			// - natural color of the pixel/triangle
			// - ambient (uniform) light
			// - one or several Directional/Point/Spot light(s)
			// - generating diffuse and specular reflection
			// Calculating shading requires to know the normal of the point at the pixel being rendered
			// - for this we use triangle interpolation
				
			}

			view.drawPixel(x, y);
			zBuffer[zBuf_x][zBuf_y] = z;
			//System.out.println("Pixel x: "+x+", y: "+y+". zBuffer: "+z);
			rendered_pixels++;
			
		} else {
			not_rendered_pixels++;
		}
	}
	
	protected boolean isInScreenX(int x) {
		if (Math.abs(x)>graphic.getPixelHalfWidth()) return false;
		return true;
	}
	
	protected boolean isInScreenY(int y) {
		if (Math.abs(y)>graphic.getPixelHalfHeight()) return false;
		return true;
	}
	
	/**
	 * This method calculates the Color for a given normal and a base color of the surface of the Element
	 * It interfaces with the Lighting object to get the ambient, directional and other types of colors
	 * @param baseCol of the surface in this area
	 * @param normal of the surface in this area
	 * @return
	 */
	protected Color computeShadedColor(Color baseCol, Vector3 normal) { // Should evolve to get the coordinates of the Vertex or surface for light type that depends on the location
		
		Color ca = null, cd = null;
		
		if (lighting != null) { // If lighting exists
			
			// Ambient light
			if (lighting.hasAmbient()) {
				ca = ColorTools.multColors(lighting.getAmbientLight().getLightColor(null), baseCol);
			} else {
				ca = Color.BLACK; // No Ambient light
			}
			// Directional light
			if (lighting.hasDirectional()) {
				// Compute the dot product
				float dot = (float)(lighting.getDirectionalLight().getLightVector(null).normalize()).dot(normal.normalize());
				cd = ColorTools.multColor(baseCol, dot);
			} else {
				cd = Color.BLACK; // No Directional light
			}
			
		} else { // If no lighting, return base color
			return baseCol;
		}
		
		// returned color is an addition of Ambient and Directional lights
		return ColorTools.addColors(ca, cd);
	}

	//
	// **** BRESSENHAM ***
	//

	protected void bressenham_int(int xi, int yi, int xf, int yf) {
		int dx, dy, i, xinc, yinc, cumul, x, y;
		x = xi;
		y = yi;
		dx = xf - xi;
		dy = yf - yi;
		xinc = (dx>0) ? 1 : -1;
		yinc = (dy>0) ? 1 : -1;
		dx = Math.abs(dx);
		dy = Math.abs(dy);
		
		allume_pixel(x,y);
		
		if (dx>dy) {
			cumul = dx/2;
			for (i=1; i<=dx; i++) {
				x+=xinc;
				cumul+=dy;
				if (cumul>=dx) {
					cumul-=dx;
					y+=yinc;
				}
				allume_pixel(x,y);
			}
		} else {
			cumul=dy/2;
			for (i=1; i<=dy; i++) {
				y+=yinc;
				cumul+=dx;
				if (cumul>=dy) {
					cumul-=dy;
					x+=xinc;
				}
				allume_pixel(x,y);
			}
		}

	}
	protected void bressenham_short(short xi, short yi, short xf, short yf) {
		short dx, dy, i, xinc, yinc, cumul, x, y;
		x = xi;
		y = yi;
		dx = (short)(xf - xi);
		dy = (short)(yf - yi);
		xinc = (short) ((dx>0) ? 1 : -1);
		yinc = (short) ((dy>0) ? 1 : -1);
		dx = (short) Math.abs(dx);
		dy = (short) Math.abs(dy);
		
		allume_pixel(x,y);
		
		if (dx>dy) {
			cumul = (short) (dx/2);
			for (i=1; i<=dx; i++) {
				x+=xinc;
				cumul+=dy;
				if (cumul>=dx) {
					cumul-=dx;
					y+=yinc;
				}
				allume_pixel(x,y);
			}
		} else {
			cumul=(short) (dy/2);
			for (i=1; i<=dy; i++) {
				y+=yinc;
				cumul+=dx;
				if (cumul>=dy) {
					cumul-=dy;
					x+=xinc;
				}
				allume_pixel(x,y);
			}
		}
	}

	protected void allume_pixel(int x, int y) {
		view.drawPixel(x, y);
	}
	
	//
	// **** END BRESSENHAM ***
	//

}