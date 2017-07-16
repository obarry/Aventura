package com.aventura.engine;

import java.awt.Color;

import com.aventura.context.GraphicContext;
import com.aventura.math.vector.Tools;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.Lighting;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.Vertex;
import com.aventura.model.world.shape.Segment;
import com.aventura.model.world.triangle.Triangle;
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
	
	// References
	protected GraphicContext graphic;
	protected View view;
	protected Lighting lighting;
	protected Camera camera;
	
	// Static data
	private static Color DARK_SHADING_COLOR = Color.BLACK;
	private static Color DEFAULT_SPECULAR_COLOR = Color.WHITE;
	
	// Z buffer
	private double[][] zBuffer;
	int zBuf_width, zBuf_height;
	private double zBuffer_init = 0;
	
	
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
	public Rasterizer(Camera camera, GraphicContext graphic, Lighting lighting) {
		this.camera = camera;
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
		zBuffer_init = graphic.getFar();
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "zBuffer init value: "+zBuffer_init);
		
		zBuf_width = 2*graphic.getPixelHalfWidth()+1;
		zBuf_height = 2*graphic.getPixelHalfHeight()+1;
		zBuffer = new double[zBuf_width][zBuf_height];
		
		// Initialization loop with initialization value ( 1 or -1 in homogeneous coordinates ?) that is the farest value for the view Frustum
		// Any value closer will be drawn and the zBuffer in this place will be updated by new value

		for (int i=0; i<zBuf_width; i++)  {
			for (int j=0; j<zBuf_height; j++) {
				zBuffer[i][j] = zBuffer_init;
			}
		}
	}
	
	protected double xScreen(Vertex v) {
		return xScreen(v.getProjPos());
	}
	
	protected double yScreen(Vertex v) {
		return yScreen(v.getProjPos());
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
		x1 = (int)(xScreen(v1));
		y1 = (int)(yScreen(v1));
		x2 = (int)(xScreen(v2));
		y2 = (int)(yScreen(v2));

		view.drawLine(x1, y1, x2, y2);
	}

	public void drawLine(Vertex v1, Vertex v2, Color c) {

		view.setColor(c);
		drawLine(v1, v2);
	}
	
	//
	// End methods for Segment only Rendering
	
	public void rasterizePlainTriangle(Triangle t, Color c) {
		rasterizeTriangle(t, c, 0, null, false, false); // No texture processing
	}
	
	public void rasterizeInterpolatedTriangle(Triangle t, Color c, float e, Color sc, boolean texture) {
		rasterizeTriangle(t, c, e, sc, true, texture);		
	}
	
	/**
	 * Triangle rasterization and zBuffering
	 * Inspired from:
	 * https://www.davrous.com/2013/06/21/tutorial-part-4-learning-how-to-write-a-3d-software-engine-in-c-ts-or-js-rasterization-z-buffering/
	 * 
	 * @param t the triangle to render
	 * @param c the base color of the triangle, may be inherited from the element or default
	 * @param e the specular exponent of the Element
	 * @param sc the specular color of the Element
	 * @param interpolate a boolean to indicate if interpolation of colors is activated (true) or not (false)
	 * @param texture a boolean to indicate if texture processing is activated (true) or not (false) 
	 **/
	protected void rasterizeTriangle(Triangle t, Color c, float e, Color sc, boolean interpolate, boolean texture) {
		
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "Rasterize triangle. Color: "+c);
		
		Color col = c; // Let's initialize the base color with the provided one (from triangle or default color)

		// Init pixel stats
		rendered_pixels = 0;
		discarded_pixels = 0;
		not_rendered_pixels = 0;
		
		// If no interpolation requested -> plain faces. Then:
		// - calculate normal at Triangle level for shading
		// - calculate shading color once for all triangle
		if (!interpolate || t.isTriangleNormal()) {
			Vector3 normal = t.getWorldNormal();
			Color shadedCol = computeShadedColor(col, normal, null, e, sc);
			// Then use the shaded color instead for whole triangle
			col = shadedCol;
		} else {
			
			// TODO Optimization: pre-calculate the viewer vectors and shaded colors to each Vertex before in 1 row
			// This will avoid to do the same calculation for a Vertex shared by several triangles (which is the general case)
			
			// Calculate viewer vectors
			Vector4 viewer1, viewer2, viewer3;
			viewer1 = camera.getEye().minus(t.getV1().getWorldPos()).normalize();
			viewer2 = camera.getEye().minus(t.getV2().getWorldPos()).normalize();
			viewer3 = camera.getEye().minus(t.getV3().getWorldPos()).normalize();
			
			// Calculate the 3 colors of the 3 vertices based on their respective normals and direction of the viewer
			t.getV1().setShadedCol(computeShadedColor(col, t.getV1().getWorldNormal(), viewer1.V3(), e, sc));
			t.getV2().setShadedCol(computeShadedColor(col, t.getV2().getWorldNormal(), viewer2.V3(), e, sc));
			t.getV3().setShadedCol(computeShadedColor(col, t.getV3().getWorldNormal(), viewer3.V3(), e, sc));					
		}

	    // Lets define v1, v2, v3 in order to always have this order on screen v1, v2 & v3 in screen coordinates
	    // with v1 always down (thus having the highest possible Y)
	    // then v2 between v1 & v3 (or same level if v2 and v3 on same ordinate)	
		Vertex v1, v2, v3;
		Vector4 vt1, vt2, vt3; // Same for Texture Vectors

		v1 = t.getV1();
		v2 = t.getV2();
		v3 = t.getV3();
		
		vt1 = t.getTexVec1();
		vt2 = t.getTexVec2();
		vt3 = t.getTexVec3();

		if (v2.getProjPos().get3DY()<v1.getProjPos().get3DY()) { // p2 lower than p1
			if (v3.getProjPos().get3DY()<v2.getProjPos().get3DY()) { // p3 lower than p2
				v1 = t.getV3();
				// No change for p2
				v3 = t.getV1();
				vt1 = t.getTexVec3();
				vt3 = t.getTexVec1();
			} else { // p2 lower than p3
				if (v3.getProjPos().get3DY()<v1.getProjPos().get3DY()) { // p3 lower than p1
					v1 = t.getV2();
					v2 = t.getV3();
					v3 = t.getV1();
					vt1 = t.getTexVec2();
					vt2 = t.getTexVec3();
					vt3 = t.getTexVec1();
				} else { // p1 higher than p3
					v1 = t.getV2();
					v2 = t.getV1();
					// No change for p3
					vt1 = t.getTexVec2();
					vt2 = t.getTexVec1();
				}
			}
		} else { // p1 lower than p2
			if (v3.getProjPos().get3DY()<v1.getProjPos().get3DY()) { // p3 lower than p1
				v1 = t.getV3();
				v2 = t.getV1();
				v3 = t.getV2();
				vt1 = t.getTexVec3();
				vt2 = t.getTexVec1();
				vt3 = t.getTexVec2();
			} else { // p1 lower than p3
				if (v3.getProjPos().get3DY()<v2.getProjPos().get3DY()) { // p3 lower than p2
					// No change for p1
					v2 = t.getV3();
					v3 = t.getV2();
					vt2 = t.getTexVec3();
					vt3 = t.getTexVec2();
				} else {
					// Else keep p1, p2 and p3 as defined
				}
			}
		}
		
	    // Slopes
	    double dP1P2, dP1P3;

	    // http://en.wikipedia.org/wiki/Slope
	    // Computing slopes
	    if (yScreen(v2) - yScreen(v1) > 0) {
	        dP1P2 = (xScreen(v2)-xScreen(v1))/(yScreen(v2)-yScreen(v1));
	    } else { // vertical segment, no slope
	        dP1P2 = 0;
	    }
	    
	    if (yScreen(v3) - yScreen(v1) > 0) {
	        dP1P3 = (xScreen(v3) - xScreen(v1)) / (yScreen(v3) - yScreen(v1));
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
	    	
	        for (int y = (int)yScreen(v1); y <= (int)yScreen(v3); y++) {
	            if (y < yScreen(v2)) {
	                rasterizeScanLine(y, v1, v3, v1, v2, vt1, vt3, vt1, vt2, t.getTexture(), col, interpolate && !t.isTriangleNormal(), texture, t.getTextureOrientation());
	            } else {
	                rasterizeScanLine(y, v1, v3, v2, v3, vt1, vt3, vt2, vt3, t.getTexture(), col, interpolate && !t.isTriangleNormal(), texture, t.getTextureOrientation());
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
	    	
	        for (int y = (int)yScreen(v1); y <= (int)yScreen(v3); y++) {
	            if (y < yScreen(v2)) {
	                rasterizeScanLine(y, v1, v2, v1, v3, vt1, vt2, vt1, vt3, t.getTexture(), col, interpolate && !t.isTriangleNormal(), texture, t.getTextureOrientation());
	            } else {
	                rasterizeScanLine(y, v2, v3, v1, v3, vt2, vt3, vt1, vt3, t.getTexture(), col, interpolate && !t.isTriangleNormal(), texture, t.getTextureOrientation());
	            }
	        }
	    }
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "Rendered pixels for this triangle: "+rendered_pixels+". Discarded: "+discarded_pixels+". Not rendered: "+not_rendered_pixels);
	}
	
	//protected void rasterizeScanLine(int y, Vertex va, Vertex vb, Vertex vc, Vertex vd, Color c, boolean interpolate) {
	protected void rasterizeScanLine(
			int y,					// ordinate of the scan line
			Vertex va,				// Vertex A of first segment: AB
			Vertex vb,				// Vertex B of first segment: AB
			Vertex vc,				// Vertex C of second segment: CD
			Vertex vd,				// Vertex D of second segment: CD
			Vector4 vta,			// Texture Vector of Vertex A
			Vector4 vtb,			// Texture Vector of Vertex B
			Vector4 vtc,			// Texture Vector of Vertex C
			Vector4 vtd,			// Texture Vector of Vertex D
			Texture t,				// Texture object for this triangle
			Color c,				// Base color
			boolean interpolate,	// Flag for interpolation (true) or not (false)
			boolean texture, 		// Flag for texture calculation (true) or not (false)
			int tex_orientation) {	// Flag for isotropic, vertical or horizontal texture interpolation

	    // Thanks to current Y, we can compute the gradient to compute others values like
	    // the starting X (sx) and ending X (ex) to draw between
	    // if pa.Y == pb.Y or pc.Y == pd.Y, gradient is forced to 1
		
	    float gradient1 = (float)(yScreen(va) != yScreen(vb) ? (y - yScreen(va)) / (yScreen(vb) - yScreen(va)) : 1);
	    float gradient2 = (float)(yScreen(vc) != yScreen(vd) ? (y - yScreen(vc)) / (yScreen(vd) - yScreen(vc)) : 1);

	    int sx = (int)Tools.interpolate(xScreen(va), xScreen(vb), gradient1);
	    int ex = (int)Tools.interpolate(xScreen(vc), xScreen(vd), gradient2);
	    
	    // Vertices z
	    float za = (float)va.getProjPos().getW();
	    float zb = (float)vb.getProjPos().getW();
	    float zc = (float)vc.getProjPos().getW();
	    float zd = (float)vd.getProjPos().getW();


	    // Starting Z & ending Z
	    float z1 = 1/Tools.interpolate(1/za, 1/zb, gradient1);
	    float z2 = 1/Tools.interpolate(1/zc, 1/zd, gradient2);

	    
	    // Starting Color & ending Color
    	Color ic1 = null;
    	Color ic2 = null;
    	if (interpolate) {
        	ic1 = ColorTools.interpolateColors(ColorTools.multColor(va.getShadedCol(),1/za), ColorTools.multColor(vb.getShadedCol(),1/zb), gradient1);
        	ic2 = ColorTools.interpolateColors(ColorTools.multColor(vc.getShadedCol(),1/zc), ColorTools.multColor(vd.getShadedCol(),1/zd), gradient2);	
	    }

	    // Starting Texture & ending Texture coordinates
    	Vector4 vt1 = null;
    	Vector4 vt2 = null;
    	Vector4 vt = null;
    	if (texture && t!=null) {
    		vt1 = Tools.interpolate(vta.times((double)1/za), vtb.times((double)1/zb), gradient1);
    		vt2 = Tools.interpolate(vtc.times((double)1/zc), vtd.times((double)1/zd), gradient2);

    	}

	    // drawing a line from left (sx) to right (ex) 
    	for (int x = sx; x < ex; x++) {
    		
    		float gradient = (float)(x-sx)/(float)(ex-sx);
    		float z = 1/Tools.interpolate(1/z1, 1/z2, gradient);

    		// If interpolation
    		if (interpolate) {
    			// Color interpolation
    			c = ColorTools.multColor(ColorTools.interpolateColors(ic1, ic2, gradient),z);
    		}

    		// Texture interpolation
    		if (texture && t!=null) {
    			Color ct = null;
    			vt = Tools.interpolate(vt1, vt2, gradient).times((double)z);
    			try {
    				// Projective Texture mapping using the fourth coordinate
    				// By default W of the texture vector is 1 but if not this will help to take account of the potential geometrical distortion of the texture
    				switch (tex_orientation) {
    				case Triangle.TEXTURE_ISOTROPIC: // Default for a triangle
    					ct = t.getInterpolatedColor(vt.getX()/vt.getW(), vt.getY()/vt.getW());
    					break;
    				case Triangle.TEXTURE_VERTICAL:
    					ct = t.getInterpolatedColor(vt.getX()/vt.getW(), vt.getY());
    					break;
    				case Triangle.TEXTURE_HORIZONTAL:
    					ct = t.getInterpolatedColor(vt.getX(), vt.getY()/vt.getW());
    					break;
    				default:
    					// Should never happen
    					if (Tracer.error) Tracer.traceError(this.getClass(), "Invalid Texture orientation for this triangle: "+tex_orientation);
    				}
    			} catch (Exception e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
    			// Combine the shaded color and the Texture color
    			Color cc = ColorTools.multColors(c, ct);
    			drawPoint(x, y, z, cc);
    		} else {
    			// Draw the point on the screen with calculated color
    			drawPoint(x, y, z, c);
    		}
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
				discarded_pixels++;
				return;
			}
			
			if (zBuf_y<0 || zBuf_y>=zBuf_height) {
				if (Tracer.error) Tracer.traceError(this.getClass(), "Invalid zBuffer_y value while drawing points: "+zBuf_y);
				discarded_pixels++;
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
	protected Color computeShadedColor(Color baseCol, Vector3 normal, Vector3 viewer, float e, Color sc) { // Should evolve to get the coordinates of the Vertex or surface for light type that depends on the location
		
		// Table of colors to be mixed
		Color [] c = new Color[3];
		// Default colors
		c[0] = DARK_SHADING_COLOR; // Ambient light
		c[1] = DARK_SHADING_COLOR; // Directional light
		c[2] = DARK_SHADING_COLOR; // Specular reflection from Directional light
		
		Color spc = sc == null ? DEFAULT_SPECULAR_COLOR : sc;
		
		if (lighting != null) { // If lighting exists
			
			// Primary shading: Diffuse Reflection
			
			float dotNL = 0;
			// Ambient light
			if (lighting.hasAmbient()) {
				c[0] = ColorTools.multColors(lighting.getAmbientLight().getLightColor(null), baseCol);
			}
			
			// Directional light
			if (lighting.hasDirectional()) {
				// Compute the dot product
				dotNL = (float)(lighting.getDirectionalLight().getLightVector(null)).dot(normal);
				if (dotNL > 0) {
					
					// Directional Light
					c[1] = ColorTools.multColor(baseCol, dotNL);
	
					
					// Secondary Shading: Specular reflection (from Directional light)
					if (e>0) { // If e=0 this is considered as no specular reflection
						float specular = 0;
						// Calculate reflection vector R = 2N-L and normalize it
						Vector3 r = normal.times(2.0).minus(lighting.getDirectionalLight().getLightVector(null)).normalize(); 
						float dotRV = (float)(r.dot(viewer));
						if (dotRV<0) dotRV = 0;
						specular = (float) Math.pow(dotRV, e);
						c[2] = ColorTools.multColor(spc, specular);
					}
				}
			}
			
		} else { // If no lighting, return base color
			return baseCol;
		}
		
		// returned color is an addition of Ambient and Directional lights
		return ColorTools.addColors(c);
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