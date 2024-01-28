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
 * Copyright (c) 2016-2024 Olivier BARRY
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
	private float[][] zBuffer;
	int zBuf_width, zBuf_height;
	private float zBuffer_init = 0;
	
	
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
		zBuffer = new float[zBuf_width][zBuf_height];
		
		// Initialization loop with initialization value ( 1 or -1 in homogeneous coordinates ?) that is the farest value for the view Frustum
		// Any value closer will be drawn and the zBuffer in this place will be updated by new value

		for (int i=0; i<zBuf_width; i++)  {
			for (int j=0; j<zBuf_height; j++) {
				zBuffer[i][j] = zBuffer_init;
			}
		}
	}
	
	//
	// A few tools, some methods to simplify method calls
	//
	
	protected float xScreen(Vertex v) {
		//return xScreen(v.getProjPos());
		return v.getProjPos().get3DX()*graphic.getPixelHalfWidth();
	}
	
	protected float yScreen(Vertex v) {
		//return yScreen(v.getProjPos());
		return v.getProjPos().get3DY()*graphic.getPixelHalfHeight();
	}
	
//	protected float xScreen(Vector4 v) {
//		return v.get3DX()*graphic.getPixelHalfWidth();
//	}
//	
//	protected float yScreen(Vector4 v) {
//		return v.get3DY()*graphic.getPixelHalfHeight();
//	}
	
	// Z buffer is [0, width][0, height] while screen is centered to origin -> need translation
	protected int getXzBuf(int x) {
		return x + graphic.getPixelHalfWidth();
	}

	// Z buffer is [0, width][0, height] while screen is centered to origin -> need translation
	protected int getYzBuf(int y) {
		return y + graphic.getPixelHalfHeight();
	}
	
	//
	// End few tools
	//

	// 
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
	//
	
	/**
	 * Triangle rasterization and zBuffering
	 * Inspired from:
	 * https://www.davrous.com/2013/06/21/tutorial-part-4-learning-how-to-write-a-3d-software-engine-in-c-ts-or-js-rasterization-z-buffering/
	 * 
	 * @param t the triangle to render
	 * @param surfCol the base surface color of the triangle, may be inherited from the element or world (default)
	 * @param specExp the specular exponent of the Element
	 * @param specCol the specular color of the Element
	 * @param interpolate a boolean to indicate if interpolation of colors is activated (true) or not (false)
	 * @param texture a boolean to indicate if texture processing is activated (true) or not (false)
	 * @param shadows a boolean to indicate if shadowing is enabled (true) or not (false)
	 **/
	public void rasterizeTriangle(Triangle t, Color surfCol, float specExp, Color specCol, boolean interpolate, boolean texture, boolean shadows) {
		
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "Rasterize triangle. Color: "+surfCol);
		
		Color shadedCol = null;
		Color ambientCol = computeAmbientColor(surfCol); // Let's compute Ambient color once per triangle (not needed at each line or pixel)

		// Init pixel stats
		rendered_pixels = 0;
		discarded_pixels = 0;
		not_rendered_pixels = 0;
		
		Vertex v1, v2, v3;
		v1 = t.getV1();
		v2 = t.getV2();
		v3 = t.getV3();
		
		// TODO use color at Vertex level if defined. This requires to manage 3 colors for a triangle in this case
		
		
		// If no interpolation requested -> plain faces. Then:
		// - calculate normal at Triangle level for shading
		// - calculate shading color once for all triangle
		if (!interpolate || t.isTriangleNormal()) {
			Vector3 normal = t.getWorldNormal();
			shadedCol = computeDirectionalColor(surfCol, normal, t.isRectoVerso());			
			//TODO Specular reflection with plain faces.
			
		} else {

			// TODO Optimization: pre-calculate the viewer vectors and shaded colors to each Vertex before in 1 row
			// This will avoid to do the same calculation for a Vertex shared by several triangles (which is the general case)

			// Calculate viewer vectors
			Vector3 viewer1, viewer2, viewer3;
			viewer1 = camera.getEye().minus(t.getV1().getWorldPos()).V3();
			viewer2 = camera.getEye().minus(t.getV2().getWorldPos()).V3();
			viewer3 = camera.getEye().minus(t.getV3().getWorldPos()).V3();
			
			viewer1.normalize();
			viewer2.normalize();
			viewer3.normalize();
			
			// Calculate the 3 colors of the 3 vertices based on their respective normals and direction of the viewer
			v1.setShadedCol(computeDirectionalColor(surfCol, v1.getWorldNormal(), t.isRectoVerso()));
			v2.setShadedCol(computeDirectionalColor(surfCol, v2.getWorldNormal(), t.isRectoVerso()));
			v3.setShadedCol(computeDirectionalColor(surfCol, v3.getWorldNormal(), t.isRectoVerso()));					

			// Calculate the 3 colors of the 3 vertices based on their respective normals and direction of the viewer
			if (lighting.hasSpecular()) {
				v1.setSpecularCol(computeSpecularColor(v1.getWorldNormal(), viewer1, specExp, specCol, t.isRectoVerso()));
				v2.setSpecularCol(computeSpecularColor(v2.getWorldNormal(), viewer2, specExp, specCol, t.isRectoVerso()));
				v3.setSpecularCol(computeSpecularColor(v3.getWorldNormal(), viewer3, specExp, specCol, t.isRectoVerso()));
			}
		}

	    // Lets define v1, v2, v3 in order to always have this order on screen v1, v2 & v3 in screen coordinates
	    // with v1 always down (thus having the highest possible Y)
	    // then v2 between v1 & v3 (or same level if v2 and v3 on same ordinate)	
		Vector4 vt1, vt2, vt3; // Same for Texture Vectors
		
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
			} else { // p2 lower or equal than p3
				if (v3.getProjPos().get3DY()<v1.getProjPos().get3DY()) { // p3 lower than p1
					v1 = t.getV2();
					v2 = t.getV3();
					v3 = t.getV1();
					vt1 = t.getTexVec2();
					vt2 = t.getTexVec3();
					vt3 = t.getTexVec1();
				} else { // p1 higher or equal than p3
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
		
		// Shadows
		Vector4 vs1 = null, vs2 = null, vs3 = null;
		
		if (shadows) { // then do the needeed calculation to know if the element is in shadow or not

			// For each light - TODO in next evolution (one single light for instance)
			// For each of the 3 Vertices
			// Get the World position
			// translate in Light coordinates using the matrix in Shadowing class
			vs1 = lighting.getDirectionalLight().getModelView().project(v1);
			vs2 = lighting.getDirectionalLight().getModelView().project(v2);
			vs3 = lighting.getDirectionalLight().getModelView().project(v3);
			// Get the depth of the vertices in Light coordinates
			// Prepare the 3 depths to be interpolated in the rasterizeScanLine method
		}
		
	    // Slopes
	    float dP1P2, dP1P3;

	    // http://en.wikipedia.org/wiki/Slope
	    // Computing invert slopes
	    if (yScreen(v2) - yScreen(v1) > 0) {
	        dP1P2 = (xScreen(v2)-xScreen(v1))/(yScreen(v2)-yScreen(v1));
	    } else { // horizontal segment, infinite invert slope
	        dP1P2 = Float.MAX_VALUE;
	    }
	    
	    if (yScreen(v3) - yScreen(v1) > 0) {
	        dP1P3 = (xScreen(v3)-xScreen(v1))/(yScreen(v3)-yScreen(v1));
	    } else { // horizontal segment, infinite invert slope
	    	dP1P3 = Float.MAX_VALUE;
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
	                rasterizeScanLine(y, v1, v3, v1, v2, vt1, vt3, vt1, vt2, t.getTexture(), shadedCol, ambientCol, interpolate && !t.isTriangleNormal(), texture, t.getTextureOrientation(), shadows, vs1, vs3, vs1, vs2);
	            } else {
	                rasterizeScanLine(y, v1, v3, v2, v3, vt1, vt3, vt2, vt3, t.getTexture(), shadedCol, ambientCol, interpolate && !t.isTriangleNormal(), texture, t.getTextureOrientation(), shadows, vs1, vs3, vs2, vs3);
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
	                rasterizeScanLine(y, v1, v2, v1, v3, vt1, vt2, vt1, vt3, t.getTexture(), shadedCol, ambientCol, interpolate && !t.isTriangleNormal(), texture, t.getTextureOrientation(), shadows, vs1, vs2, vs1, vs3);
	            } else {
	                rasterizeScanLine(y, v2, v3, v1, v3, vt2, vt3, vt1, vt3, t.getTexture(), shadedCol, ambientCol, interpolate && !t.isTriangleNormal(), texture, t.getTextureOrientation(), shadows, vs2, vs3, vs1, vs3);
	            }
	        }
	    }
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "Rendered pixels for this triangle: "+rendered_pixels+". Discarded: "+discarded_pixels+". Not rendered: "+not_rendered_pixels);
	}
	

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
			Color shadedCol,		// Shaded color if Normal at triangle level (else should be null)
			Color ambientCol,		// Ambient color (independent of the position in space)
			boolean interpolate,	// Flag for interpolation (true) or not (false)
			boolean texture, 		// Flag for texture calculation (true) or not (false)
			int tex_orientation,	// Flag for isotropic, vertical or horizontal texture interpolation
			boolean shadows,		// Flag for shadowing enabled (true) or disabled (false)
			Vector4 vsa,			// Projected position in light coordinates of Vertex A
			Vector4 vsb,			// Projected position in light coordinates of Vertex B
			Vector4 vsc,			// Projected position in light coordinates of Vertex C
			Vector4 vsd)   {		// Projected position in light coordinates of Vertex D

		// Thanks to current Y, we can compute the gradient to compute others values like
		// the starting X (sx) and ending X (ex) to draw between
		// if pa.Y == pb.Y or pc.Y == pd.Y, gradient is forced to 1
		float ya = yScreen(va);
		float yb = yScreen(vb);
		float yc = yScreen(vc);
		float yd = yScreen(vd);

		float xa = xScreen(va);
		float xb = xScreen(vb);
		float xc = xScreen(vc);
		float xd = xScreen(vd);

		// Gradient 1 is the gradient on VA VB segment
		float gradient1 = ya != yb ? (y - ya) / (yb - ya) : 1;
		// Gradient 2 is the gradient on VC VD segment
		float gradient2 = yc != yd ? (y - yc) / (yd - yc) : 1;
		
		int sx = (int)Tools.interpolate(xa, xb, gradient1);
		int ex = (int)Tools.interpolate(xc, xd, gradient2);

		// To avoid gradient effect on x axis for small y variations (flat slopes) -> "cap" the sx and ex to x min and max of the triangle 
		int smin = (int)Math.min(xa,xb);
		int emax = (int)Math.max(xc, xd);
		if (sx<smin) sx=(int)smin;
		if (ex>emax) ex=(int)emax;

		// Instrumentation for Rasterizer artifact investigation (due to calculated gradient>1 fixed by rounding in gradient calculation)
		// TODO possible optimization in Rasterizer to avoid calculation in double, to avoid rounding and use int computation as most as possible then avoid duplicate calculation in several places (x and yScreen for example)
		
		float z1 = 0, z2 = 0, za = 0, zb = 0, zc = 0, zd = 0;


		switch (graphic.getPerspectiveType()) {

		case GraphicContext.PERSPECTIVE_TYPE_FRUSTUM :
			// Vertices z
			za = va.getProjPos().getW();
			zb = vb.getProjPos().getW();
			zc = vc.getProjPos().getW();
			zd = vd.getProjPos().getW();

			// Starting Z & ending Z
			z1 = 1/Tools.interpolate(1/za, 1/zb, gradient1);
			z2 = 1/Tools.interpolate(1/zc, 1/zd, gradient2);
			
			break;

		case GraphicContext.PERSPECTIVE_TYPE_ORTHOGRAPHIC :
			// Orthographic projection -> don't use W but use rather Z instead
			za = va.getProjPos().getZ();
			zb = vb.getProjPos().getZ();
			zc = vc.getProjPos().getZ();
			zd = vd.getProjPos().getZ();
			
			// Starting Z & ending Z
			z1 = Tools.interpolate(za, zb, gradient1);
			z2 = Tools.interpolate(zc, zd, gradient2);
			
			break;

		default :
			// Not implemented
			// TODO raise an UnimplementedException
		}

		// Shadows
		float zs1 = 0, zs2 = 0;
		if (shadows) { // then do the needeed calculation to know if the element is in shadow or not
			// TODO
			// For each light
			// Get the depth of the vertices in Light coordinates
			float zsa = vsa.getW();
			float zsb = vsb.getW();
			float zsc = vsc.getW();
			float zsd = vsd.getW();
			
			// Interpolate across the 2 segments using gradients
			// Starting Z & ending Z
			zs1 = 1/Tools.interpolate(1/zsa, 1/zsb, gradient1);
			zs2 = 1/Tools.interpolate(1/zsc, 1/zsd, gradient2);			
		}


		// Gouraud's shading (Vertex calculation and interpolation across triangle)
		// Starting Colors & ending Colors for Shaded color and Specular color
		Color ishc1 = null, ishc2 = null; // Shaded
		Color ispc1 = null, ispc2 = null; // Specular
		if (interpolate) {
			// Shaded color
			ishc1 = ColorTools.interpolateColors(ColorTools.multColor(va.getShadedCol(),1/za), ColorTools.multColor(vb.getShadedCol(),1/zb), gradient1);
			ishc2 = ColorTools.interpolateColors(ColorTools.multColor(vc.getShadedCol(),1/zc), ColorTools.multColor(vd.getShadedCol(),1/zd), gradient2);
			// Specular color
			if (lighting.hasSpecular()) {
				ispc1 = ColorTools.interpolateColors(ColorTools.multColor(va.getSpecularCol(),1/za), ColorTools.multColor(vb.getSpecularCol(),1/zb), gradient1);
				ispc2 = ColorTools.interpolateColors(ColorTools.multColor(vc.getSpecularCol(),1/zc), ColorTools.multColor(vd.getSpecularCol(),1/zd), gradient2);
			}
		}

		// Starting Texture & ending Texture coordinates
		Vector4 vt1 = null;
		Vector4 vt2 = null;
		Vector4 vt = null;
		if (texture && t!=null) {
			vt1 = Tools.interpolate(vta.times((float)1/za), vtb.times((float)1/zb), gradient1);
			vt2 = Tools.interpolate(vtc.times((float)1/zc), vtd.times((float)1/zd), gradient2);
		}

		Color csh = null; // Shaded color
		Color csp = null; // Specular color
		Color ctx = null; // Texture color
		Color cc; // Combined color to be drawn, result of the lighting and shading calculation

		// drawing a line from left (sx) to right (ex) 
		for (int x = sx; x < ex; x++) {

			// Eliminate pixels outside the view screen
			if (isInScreenX(x) && isInScreenY(y)) {
				// Z buffer is [0, width][0, height] while screen is centered to origin -> need translation
				int x_zBuf = getXzBuf(x);
				int y_zBuf = getYzBuf(y);

				// Protect against out of bounds (should not happen)
				if (x_zBuf>=0 && x_zBuf<zBuf_width && y_zBuf>=0 && y_zBuf<zBuf_height) {

					cc = null;

					float gradient = (float)(x-sx)/(float)(ex-sx);
					float z = 1/Tools.interpolate(1/z1, 1/z2, gradient);
					//float z=Tools.interpolate(z1,z2,gradient); // Used for debugging Orthographic projection

					// zBuffer elimination at ealiest stage of computation (as soon as we know z)
					if (z>zBuffer[getXzBuf(x)][getYzBuf(y)]) { // Discard pixel
						discarded_pixels++;

					} else { // Compute colors and draw pixel

						// If interpolation
						if (interpolate) {
							// Color interpolation
							csh = ColorTools.multColor(ColorTools.interpolateColors(ishc1, ishc2, gradient),z); // Shaded color
							if (lighting.hasSpecular()) {
								csp = ColorTools.multColor(ColorTools.interpolateColors(ispc1, ispc2, gradient),z); // Specular color
							} else {
								csp = DARK_SHADING_COLOR; // No specular
							}
						} else { // Else csh is the base color passed in arguments and csp won't be used
							csh = shadedCol; // Shaded color passed in argument
							// TODO specular color to be implemented
						}

						// Texture interpolation
						if (texture && t!=null) {

							vt = Tools.interpolate(vt1, vt2, gradient).times(z);
							try {
								// Projective Texture mapping using the fourth coordinate
								// By default W of the texture vector is 1 but if not this will help to take account of the potential geometric distortion of the texture
								switch (tex_orientation) {
								case Triangle.TEXTURE_ISOTROPIC: // Default for a triangle
									ctx = t.getInterpolatedColor(vt.getX()/vt.getW(), vt.getY()/vt.getW());
									break;
								case Triangle.TEXTURE_VERTICAL:
									ctx = t.getInterpolatedColor(vt.getX()/vt.getW(), vt.getY());
									break;
								case Triangle.TEXTURE_HORIZONTAL:
									ctx = t.getInterpolatedColor(vt.getX(), vt.getY()/vt.getW());
									break;
								default:
									// Should never happen
									if (Tracer.error) Tracer.traceError(this.getClass(), "Invalid Texture orientation for this triangle: "+tex_orientation);
								}
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

						} // End Texture interpolation
						
						// Shadowing
						if (shadows) { // then do the needful to know if the element is in shadow or not
							// TODO
							// For each light
							int xs = 0, ys = 0; // projected position for shadow map
							// Interpolate across the 2 segments using gradient
							float zs = 1/Tools.interpolate(1/zs1, 1/zs2, gradient);
							// Calculate xs and ys by
							// - projection using the Light coordinates matrix
							// - tranform from [-1.1] coordinates to [0,1] by multiplying the projection matrix appropriately
							// - transformation in integer indices of the size of the shadow map
							if (zs<lighting.getDirectionalLight().getMap(xs,ys)) {
								
							} else {
								// in shadow
							}
						
							// Is there a needed correction using W coordinate ?
							// Get the depth from the depth map using texture mapping interpolation technique
							// Compare the 2 depths and if depth of the fragment is deeper than depth map
							// then this fragment is in shadow and the corresponding shadow light should be 0
							// else this fragment is in the light and shadow light should be 1
							
						}

						// Combine colors with the following formula
						// Color K = DTA + CDT + S = DT(A+C) + S : WRONG old calculation 
						// Color K = DTA + C(DT + S) = DTA + DTC + SC = DT(A+C) + SC
						// ctx = T, csh  = C, ambientCol = A, csp = S
						// D: diffuse color, T: texture, A: Ambient color, C: color of the light source at point, S: Specular color
						//TODO need to decouple the Ambient light from the shaded color calculation. This is easy as Ambient light do not need any interpolation
						// This will allow to calculate the CS (shaded*specular) color

						if (texture && t!=null) {
							if (lighting.hasSpecular() && csp != null) {
								cc = ColorTools.addColors(ColorTools.multColors(ctx, ColorTools.addColors(ambientCol, csh)), ColorTools.multColors(csh,csp));
							} else {
								cc = ColorTools.multColors(ctx, ColorTools.addColors(ambientCol, csh));
							}
						} else {
							if (lighting.hasSpecular() && csp != null) {
								cc = ColorTools.addColors(ambientCol,ColorTools.addColors(csh, csp));	
							} else {
								cc = ColorTools.addColors(ambientCol,csh);
							}
						}

						// Draw the point with calculated Combined Color
						drawPoint(x, y, z, cc);
					} 

				} else { // Out of zBuffer range (should not happen)
					not_rendered_pixels++;	    		
					if (x_zBuf<0 || x_zBuf>=zBuf_width) {
						if (Tracer.error) Tracer.traceError(this.getClass(), "Invalid zBuffer_x value while drawing points: "+x_zBuf);
					}
					if (y_zBuf<0 || y_zBuf>=zBuf_height) {
						if (Tracer.error) Tracer.traceError(this.getClass(), "Invalid zBuffer_y value while drawing points: "+y_zBuf);
					}
				}
			} else { // Out of screen
				not_rendered_pixels++;
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
	protected void drawPoint(int x, int y, float z, Color c) {
		// Eliminate pixels outside the view screen is done before calling this method for optimization
		// So at this stage we only render pixel and update the zBuffer

		// Draw pixel in the View
		view.drawPixel(x,y,c);

		// Update zBuffer of this pixel to the new z
		zBuffer[getXzBuf(x)][getYzBuf(y)] = z;

		// Increment counter of rendered pixels
		rendered_pixels++;
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
	 * This method return the Color resulting from Ambient light
	 * It is agnostic to any position in space as per definition of Ambient light
	 * @param baseCol of the surface in this area
	 * @return Ambient Color
	 */
	protected Color computeAmbientColor(Color baseCol) {

		if (lighting != null && lighting.hasAmbient()) {
			return ColorTools.multColors(lighting.getAmbientLight().getLightColor(null), baseCol);
		} else {
			// Default color
			return DARK_SHADING_COLOR; // Ambient light
		}
	}

	/**
	 * This method calculates the Color for a given normal and a base color of the surface of the Element resulting from Directional light
	 * @param baseCol of the surface in this area
	 * @param normal of the surface in this area
	 * @return the Directional light at point
	 */
	protected Color computeDirectionalColor(Color baseCol, Vector3 normal, boolean rectoVerso) { // Should evolve to get the coordinates of the Vertex or surface for light type that depends on the location

		// Table of colors to be mixed
		Color c;
		
		// Default color
		c = DARK_SHADING_COLOR; // Directional light

		if (lighting != null) { // If lighting exists

			// Primary shading: Diffuse Reflection
			
			float dotNL = 0;

			// Directional light
			if (lighting.hasDirectional()) {

				//TODO Multiple directional colors -> loop

				// Compute the dot product
				dotNL = lighting.getDirectionalLight().getLightVector(null).dot(normal.normalize());
				if (rectoVerso) dotNL = Math.abs(dotNL);
				if (dotNL > 0) {
					// Directional Light
					c = ColorTools.multColor(baseCol, dotNL);
				}
			}
		} else { // If no lighting, return base color
			return baseCol;
		}

		return c;
	}

	/**
	 * @param normal the normal vector
	 * @param viewer normized vector
	 * @param e specular exponent
	 * @param sc specular color
	 * @param rectoVerso true if this triangle can be seen back side
	 * @return the specular color
	 */
	protected Color computeSpecularColor(Vector3 normal, Vector3 viewer, float e, Color sc, boolean rectoVerso) {

		Color c = DARK_SHADING_COLOR; // Specular reflection from Directional light
		Color spc = sc == null ? DEFAULT_SPECULAR_COLOR : sc;

		// Secondary Shading: Specular reflection (from Directional light)
		if (e>0) { // If e=0 this is considered as no specular reflection
			// R: Reflection vector, L: Light vector, N: Normal vector on the surface. R+L=N+N => R = 2N-L
			// Calculate reflection vector R = 2N-L and normalize it
			float dotNL = lighting.getDirectionalLight().getLightNormalizedVector(null).dot(normal.normalize());
			Vector3 r = (normal.times(2*dotNL)).minus(lighting.getDirectionalLight().getLightNormalizedVector(null)); 

			float dotRV = r.dot(viewer);
			if (rectoVerso)
				dotRV = Math.abs(dotRV);
			else
				if (dotRV <0) dotRV = 0; // Clamped to 0 if negative
			// Compute the dot product
			//TODO Problem here: the following calculation creates abrupt specular (but it seems normal situation with Gouraud's shading
			if (dotNL > 0) {
				float specular = (float) Math.pow(dotRV, e);
				float intensity = lighting.getDirectionalLight().getIntensity(null);
				c = ColorTools.multColor(spc, specular*intensity);
			}
		}
		return c;
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