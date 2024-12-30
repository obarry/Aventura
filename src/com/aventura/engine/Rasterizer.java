package com.aventura.engine;

import java.awt.Color;
import java.util.ArrayList;

import com.aventura.context.GraphicContext;
import com.aventura.math.vector.Tools;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.DirectionalLight;
import com.aventura.model.light.Lighting;
import com.aventura.model.light.PointLight;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.Vertex;
import com.aventura.model.world.shape.Segment;
import com.aventura.model.world.triangle.Triangle;
import com.aventura.tools.color.ColorTools;
import com.aventura.tools.tracing.Tracer;
import com.aventura.view.GUIView;
import com.aventura.view.MapView;

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
 * It requires access to the GUIView in order to draw pixels.
 * zBuffer is managed here.
 * It also behaves according to the provided parameters e.g. in the GraphicContext
 * 
 * 
 * @author Olivier BARRY
 * @since November 2016
 *
 */

public class Rasterizer {
	
	// --------------------------------------------------------------------------------------------------------------------
	// Parameter classes (structures) to be used as parameters for rasterizing scan lines
	// For each triangle, the structures will be constructed with appropriate parameters then passed to rasterizeScanLine
	// --------------------------------------------------------------------------------------------------------------------
	//
	
	protected class VertexLightParam {
		public Color shadedColor;
		public Color specularColor;
		public Vector4 vl; // Projected position in light coordinates of Vertex
		public Vector4 vm; // Shadow Map vector (position in Shadow Map)
		public MapView map; // Shadow map of this Light
		
		public VertexLightParam() {
		}
		
		public VertexLightParam(Color shaded, Color specular, Vector4 vl, Vector4 vm, MapView m) {
			this.shadedColor = shaded;
			this.specularColor = specular;
			this.vl = vl;
			this.vm = vm;
			this.map = m;
		}
	}
	
	protected class VertexParam {
		public Vertex v; // Vertex
		public Vector4 t; // Texture vector
		public VertexLightParam [] l; // one parameter for each light (except ambient)
		
		public VertexParam() {
		}
		
		public VertexParam(Vertex v, Vector4 t) {
			this.v = v;
			this.t = t;
		}
		
		public VertexParam(Vertex v, Vector4 t, VertexLightParam[] l) {
			this.v = v;
			this.t = t;
			this.l = l;
		}
	}
	
	// End Parameter classes definition
	// --------------------------------------------------------------------------------------------------------------------

	
	// References
	protected GraphicContext graphic;
	protected GUIView gUIView;
	protected Lighting lighting;
	protected Camera camera;
	
	// Static data
	private static Color DARK_SHADING_COLOR = Color.BLACK;
	private static Color DEFAULT_SPECULAR_COLOR = Color.WHITE;
	
	// Z buffer
	private MapView zBuffer = null;
	int zBuf_width, zBuf_height;	
	
	// Pixel statistics
	int rendered_pixels = 0;
	int discarded_pixels = 0;
	int not_rendered_pixels = 0;
	int discarded_lines = 0;
	
	// Create locally some context variables exhaustively used during rasterization
	// TODO Be cautious here : if GraphicContext has changed between 2 calls to previously created Rasterizer, these 2 variables won't be refreshed accordingly -> potential bug
	int pixelHalfWidth = 0;
	int pixelHalfHeight = 0;
	
	/**
	 * Creation of Rasterizer with requested references for run time.
	 * @param camera : a pointer to the Camera created offline by user
	 * @param graphic : a pointer to the GraphicContext created offline by user
	 * @param lighting : a pointer to the Lighting system created offline by user
	 */
	public Rasterizer(Camera camera, GraphicContext graphic, Lighting lighting) {
		this.camera = camera;
		this.graphic = graphic;
		this.lighting = lighting;
		pixelHalfWidth = graphic.getPixelHalfWidth();
		pixelHalfHeight = graphic.getPixelHalfHeight();
		// TODO Be cautious here : if GraphicContext has changed between 2 calls to previously created Rasterizer, the 2 above variables won't be refreshed accordingly -> potential bug
	}
		
	public void setView(GUIView v) {
		this.gUIView = v;
	}
	
	/**
	 * Initialize zBuffer by creating the table. This method is deported from the constructor in order to use it only when necessary.
	 * It is not needed in case of line rendering.
	 */
	public MapView initZBuffer() {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating zBuffer. Width: "+graphic.getPixelWidth()+" Height: "+graphic.getPixelHeight());
		
		// zBuffer is initialized with far value of the perspective
		float zBuffer_init = graphic.getPerspective().getFar();
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "zBuffer init value: "+zBuffer_init);
		
		zBuf_width  = 2 * pixelHalfWidth  + 1;
		zBuf_height = 2 * pixelHalfHeight + 1;
		
		// Only create buffer if needed, otherwise reuse it, it will be reinitialized below
		//if (zBuffer == null) zBuffer = new float[zBuf_width][zBuf_height];
		if (zBuffer == null) zBuffer = new MapView(zBuf_width, zBuf_height);

		// Initialization loop with initialization value ( 1 or -1 in homogeneous coordinates ?) that is the farest value for the gUIView Frustum
		// Any value closer will be drawn and the zBuffer in this place will be updated by new value
		for (int i=0; i<zBuf_width; i++)  {
			for (int j=0; j<zBuf_height; j++) {
				zBuffer.set(i, j, zBuffer_init); // Far value of the perspective
			}
		}
		return zBuffer;
	}
	
	//
	// A few tools, some methods to simplify method calls
	//
	// TODO Shouldn't this transformation be handled through the projection matrix to avoid additional computation for each pixel ?
	protected float xScreen(Vertex v) {
		//return xScreen(v.getProjPos());
		return v.getProjPos().get3DX()*pixelHalfWidth;
	}
	
	protected float yScreen(Vertex v) {
		//return yScreen(v.getProjPos());
		return v.getProjPos().get3DY()*pixelHalfHeight;
	}
	

	// Z buffer is [0, width][0, height] while screen is centered to origin -> need translation
	protected  int getXzBuf(int x) {
		return x + pixelHalfWidth;
	}

	// Z buffer is [0, width][0, height] while screen is centered to origin -> need translation
	protected int getYzBuf(int y) {
		return y + pixelHalfHeight;
	}
	
	//
	// End few tools
	//

	// 
	// Method for Segment only Rendering
	//
	
	public void drawTriangleLines(Triangle t, Color c) {
		
		gUIView.setColor(c);
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

		gUIView.drawLine(x1, y1, x2, y2);
	}

	public void drawLine(Vertex v1, Vertex v2, Color c) {

		gUIView.setColor(c);
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
	public void rasterizeTriangle(
			Triangle t,
			Color surfCol,
			float specExp,
			Color specCol,
			boolean interpolate,
			boolean texture,
			boolean shadows) {
		
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "Rasterize triangle. Color: "+surfCol);
		
		Color shadedCol = null;
		Color ambientCol = computeAmbientColor(surfCol); // Let's compute Ambient color once per triangle (not needed at each line or pixel)

		// Init pixel stats
		rendered_pixels = 0;
		discarded_pixels = 0;
		not_rendered_pixels = 0;
		
	    // Lets define v1, v2, v3 in order to always have this order on screen v1, v2 & v3 in screen coordinates
	    // with v1 always down (thus having the highest possible Y)
	    // then v2 between v1 & v3 (or same level if v2 and v3 on same ordinate)	
		Vertex v1, v2, v3;
		v1 = t.getV1();
		v2 = t.getV2();
		v3 = t.getV3();
		// TODO use color at Vertex level if defined. This requires to manage 3 colors for a triangle in this case
		
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
		
		// Initialize 3 VertexParam structures containing n VertexParamLight structures, one for each light
		// This will be used as parameters to be passed to rasterizeScanLight function
		ArrayList<PointLight> pointLights = lighting.getPointLights();
		int nb_pl = pointLights.size();
		VertexParam vp1, vp2, vp3;
		vp1 = new VertexParam(v1, vt1);
		vp2 = new VertexParam(v2, vt2);
		vp3 = new VertexParam(v3, vt3);
		VertexLightParam [] vlp1 = new VertexLightParam[nb_pl];
		VertexLightParam [] vlp2 = new VertexLightParam[nb_pl];
		VertexLightParam [] vlp3 = new VertexLightParam[nb_pl];
		// For each Light
		for (int i=0; i<nb_pl; i++) {
			vlp1[i] = new VertexLightParam();
			vlp2[i] = new VertexLightParam();
			vlp3[i] = new VertexLightParam();	
		}
		vp1.l = vlp1;
		vp2.l = vlp2;
		vp3.l = vlp3;
		// End initialize VertexParam structures
		
		// If no interpolation requested -> plain faces. Then:
		// - calculate normal at Triangle level for shading
		// - calculate shading color once for all triangle
		if (!interpolate || t.isTriangleNormal()) {
			Vector3 normal = t.getWorldNormal();
			shadedCol = computeShadedColor(surfCol, t.getCenterWorldPos(), normal, t.isRectoVerso(), null);
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
			//v1.setShadedCol(computeShadedColor(surfCol, v1.getWorldPos(), v1.getWorldNormal(), t.isRectoVerso()));
			//v2.setShadedCol(computeShadedColor(surfCol, v2.getWorldPos(), v2.getWorldNormal(), t.isRectoVerso()));
			//v3.setShadedCol(computeShadedColor(surfCol, v3.getWorldPos(), v3.getWorldNormal(), t.isRectoVerso()));		

			// Calculate the 3 colors of the 3 vertices based on their respective normals and direction of the viewer
			//if (lighting.hasSpecular()) {
			//	v1.setSpecularCol(computeSpecularColor(v1.getWorldNormal(), viewer1, v1.getWorldPos(), specExp, specCol, t.isRectoVerso()));
			//	v2.setSpecularCol(computeSpecularColor(v2.getWorldNormal(), viewer2, v2.getWorldPos(), specExp, specCol, t.isRectoVerso()));
			//	v3.setSpecularCol(computeSpecularColor(v3.getWorldNormal(), viewer3, v3.getWorldPos(), specExp, specCol, t.isRectoVerso()));
			//}

			// For each Light
			for (int i=0; i<nb_pl; i++) {
				vlp1[i].shadedColor = computeShadedColor(surfCol, v1.getWorldPos(), v1.getWorldNormal(), t.isRectoVerso(), pointLights.get(i));
				vlp2[i].shadedColor = computeShadedColor(surfCol, v2.getWorldPos(), v2.getWorldNormal(), t.isRectoVerso(), pointLights.get(i));
				vlp3[i].shadedColor = computeShadedColor(surfCol, v3.getWorldPos(), v3.getWorldNormal(), t.isRectoVerso(), pointLights.get(i));	

				if (lighting.hasSpecular()) {
					vlp1[i].specularColor = computeSpecularColor(v1.getWorldNormal(), viewer1, v1.getWorldPos(), specExp, specCol, t.isRectoVerso(), pointLights.get(i));
					vlp2[i].specularColor = computeSpecularColor(v2.getWorldNormal(), viewer2, v2.getWorldPos(), specExp, specCol, t.isRectoVerso(), pointLights.get(i));
					vlp3[i].specularColor = computeSpecularColor(v3.getWorldNormal(), viewer3, v3.getWorldPos(), specExp, specCol, t.isRectoVerso(), pointLights.get(i));
				}

			}

		}

		
		// Shadows
		//Vector4 vs1_d = null, vs2_d = null, vs3_d = null;
		Vector4[] vs1_p = null, vs2_p = null, vs3_p = null;
		
		//
		// TODO SHADOWS : THIS SHOULD BE DONE BEFORE REORDERING THE 3 VERTICES SO THAT THIS IS MATCHING !!!
		//
		if (shadows) { // then do the needed calculation to know if the element is in shadow or not
			
			// For shadows we use same approach than Textures : at triangle level (here) : do a projection each of the 3 vertices in (each) light coordinates
			// Then, at pixel level ( when rasterizing the scan lines) : interpolate the position in light coordinates and then get the shadow map value using the
			// interpolated coordinates in light coordinates. Interpolation should also have depth correction.
			
			// Directional light - (one single Directional light for instance) TODO in next evolution: multiple Directional Lights

			// For each of the 3 Vertices
			// Get the World position
			// translate in Light coordinates using the matrix in Shadowing class
//			vs1_d = lighting.getDirectionalLight().getModelView().project(v1);
//			vs2_d = lighting.getDirectionalLight().getModelView().project(v2);
//			vs3_d = lighting.getDirectionalLight().getModelView().project(v3);

			// For each Point light
			//int nb_pl = lighting.getPointLights().size();
			
			if (nb_pl >0) {
				
				vs1_p = new Vector4[nb_pl];
				vs2_p = new Vector4[nb_pl];
				vs3_p = new Vector4[nb_pl];

				// For each of the 3 Vertices
				// Get the World position
				// translate in Light coordinates using the matrix in Shadowing class
				for (int i=0; i<nb_pl; i++) {
					vs1_p[i] = lighting.getDirectionalLight().getModelView().project(v1);
					vs2_p[i] = lighting.getDirectionalLight().getModelView().project(v2);
					vs3_p[i] = lighting.getDirectionalLight().getModelView().project(v3);
				}

			}
			
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
	            	rasterizeScanLine_new(y, vp1, vp3, vp1, vp2, t.getTexture(), shadedCol, ambientCol, interpolate && !t.isTriangleNormal(), texture, t.getTextureOrientation(), shadows, nb_pl);
	                //rasterizeScanLine(y, v1, v3, v1, v2, vt1, vt3, vt1, vt2, t.getTexture(), shadedCol, ambientCol, interpolate && !t.isTriangleNormal(), texture, t.getTextureOrientation(), shadows, vs1_d, vs3_d, vs1_d, vs2_d, vs1_p, vs3_p, vs1_p, vs2_p);
	            } else {
	                rasterizeScanLine_new(y, vp1, vp3, vp2, vp3, t.getTexture(), shadedCol, ambientCol, interpolate && !t.isTriangleNormal(), texture, t.getTextureOrientation(), shadows, nb_pl);
	                //rasterizeScanLine(y, v1, v3, v2, v3, vt1, vt3, vt2, vt3, t.getTexture(), shadedCol, ambientCol, interpolate && !t.isTriangleNormal(), texture, t.getTextureOrientation(), shadows, vs1_d, vs3_d, vs2_d, vs3_d, vs1_p, vs3_p, vs2_p, vs3_p);
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
	                rasterizeScanLine_new(y, vp1, vp2, vp1, vp3, t.getTexture(), shadedCol, ambientCol, interpolate && !t.isTriangleNormal(), texture, t.getTextureOrientation(), shadows, nb_pl);
	                //rasterizeScanLine(y, v1, v2, v1, v3, vt1, vt2, vt1, vt3, t.getTexture(), shadedCol, ambientCol, interpolate && !t.isTriangleNormal(), texture, t.getTextureOrientation(), shadows, vs1_d, vs2_d, vs1_d, vs3_d, vs1_p, vs2_p, vs1_p, vs3_p);
	            } else {
	                rasterizeScanLine_new(y, vp2, vp3, vp1, vp3, t.getTexture(), shadedCol, ambientCol, interpolate && !t.isTriangleNormal(), texture, t.getTextureOrientation(), shadows, nb_pl);
	                //rasterizeScanLine(y, v2, v3, v1, v3, vt2, vt3, vt1, vt3, t.getTexture(), shadedCol, ambientCol, interpolate && !t.isTriangleNormal(), texture, t.getTextureOrientation(), shadows, vs2_d, vs3_d, vs1_d, vs3_d, vs2_p, vs3_p, vs1_p, vs3_p);
	            }
	        }
	    }
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "Rendered pixels for this triangle: "+rendered_pixels+". Discarded: "+discarded_pixels+". Not rendered: "+not_rendered_pixels+". Discarded lines: "+discarded_lines);
	}
	

//	protected void rasterizeScanLine(
//			int y,					// ordinate of the scan line
//			Vertex va,				// Vertex A of first segment: AB
//			Vertex vb,				// Vertex B of first segment: AB
//			Vertex vc,				// Vertex C of second segment: CD
//			Vertex vd,				// Vertex D of second segment: CD
//			Vector4 vta,			// Texture Vector of Vertex A
//			Vector4 vtb,			// Texture Vector of Vertex B
//			Vector4 vtc,			// Texture Vector of Vertex C
//			Vector4 vtd,			// Texture Vector of Vertex D
//			Texture t,				// Texture object for this triangle
//			Color shadedCol,		// Shaded color if Normal at triangle level (else should be null)
//			Color ambientCol,		// Ambient color (independent of the position in space)
//			boolean interpolate,	// Flag for interpolation (true) or not (false)
//			boolean texture, 		// Flag for texture calculation (true) or not (false)
//			int tex_orientation,	// Flag for isotropic, vertical or horizontal texture interpolation
//			boolean shadows,		// Flag for shadowing enabled (true) or disabled (false)
//			Vector4 vsa_d,			// Projected position in Directional light coordinates of Vertex A
//			Vector4 vsb_d,			// Projected position in Directional light coordinates of Vertex B
//			Vector4 vsc_d,			// Projected position in Directional light coordinates of Vertex C
//			Vector4 vsd_d,			// Projected position in Directional light coordinates of Vertex D
//			Vector4[] vsa_p,		// Array of projected position in Point light coordinates of Vertex A
//			Vector4[] vsb_p,		// Array of projected position in Point light coordinates of Vertex B
//			Vector4[] vsc_p,		// Array of projected position in Point light coordinates of Vertex C
//			Vector4[] vsd_p) {		// Array of projected position in Point light coordinates of Vertex D
//
//		// Thanks to current Y, we can compute the gradient to compute others values like
//		// the starting X (sx) and ending X (ex) to draw between
//		// if pa.Y == pb.Y or pc.Y == pd.Y, gradient is forced to 1
//		float ya = yScreen(va);
//		float yb = yScreen(vb);
//		float yc = yScreen(vc);
//		float yd = yScreen(vd);
//
//		float xa = xScreen(va);
//		float xb = xScreen(vb);
//		float xc = xScreen(vc);
//		float xd = xScreen(vd);
//
//		// Gradient 1 is the gradient on VA VB segment
//		float gradient1 = ya != yb ? (y - ya) / (yb - ya) : 1;
//		// Gradient 2 is the gradient on VC VD segment
//		float gradient2 = yc != yd ? (y - yc) / (yd - yc) : 1;
//		
//		int sx = (int)Tools.interpolate(xa, xb, gradient1);
//		int ex = (int)Tools.interpolate(xc, xd, gradient2);
//
//		// To avoid gradient effect on x axis for small y variations (flat slopes) -> "cap" the sx and ex to x min and max of the triangle 
//		int smin = (int)Math.min(xa,xb);
//		int emax = (int)Math.max(xc, xd);
//		if (sx<smin) sx=smin;
//		if (ex>emax) ex=emax;
//
//		// Instrumentation for Rasterizer artifact investigation (due to calculated gradient>1 fixed by rounding in gradient calculation)
//		// TODO possible optimization in Rasterizer to avoid calculation in double, to avoid rounding and use int computation as most as possible then avoid duplicate calculation in several places (x and yScreen for example)
//		
//		float z1 = 0, z2 = 0, za = 0, zb = 0, zc = 0, zd = 0;
//
//
//		switch (graphic.getPerspectiveType()) {
//
//		case GraphicContext.PERSPECTIVE_TYPE_FRUSTUM :
//			// Vertices z
//			za = va.getProjPos().getW();
//			zb = vb.getProjPos().getW();
//			zc = vc.getProjPos().getW();
//			zd = vd.getProjPos().getW();
//
//			// Starting Z & ending Z
//			z1 = 1/Tools.interpolate(1/za, 1/zb, gradient1);
//			z2 = 1/Tools.interpolate(1/zc, 1/zd, gradient2);
//			
//			break;
//
//		case GraphicContext.PERSPECTIVE_TYPE_ORTHOGRAPHIC :
//			// Orthographic projection -> don't use W but use rather Z instead
//			za = va.getProjPos().getZ();
//			zb = vb.getProjPos().getZ();
//			zc = vc.getProjPos().getZ();
//			zd = vd.getProjPos().getZ();
//			
//			// Starting Z & ending Z
//			z1 = Tools.interpolate(za, zb, gradient1);
//			z2 = Tools.interpolate(zc, zd, gradient2);
//			
//			break;
//
//		default :
//			// Not implemented
//			// TODO raise an UnimplementedException
//		}
//
//		// Shadows
//		float zs1 = 0, zs2 = 0;
//		if (shadows) { // then do the needeed calculation to know if the element is in shadow or not
//			// TODO
//			// For each light
//			// Get the depth of the vertices in Light coordinates
//			float zsa = vsa_d.getW();
//			float zsb = vsb_d.getW();
//			float zsc = vsc_d.getW();
//			float zsd = vsd_d.getW();
//			
//			// Interpolate across the 2 segments using gradients
//			// Starting Z & ending Z
//			zs1 = 1/Tools.interpolate(1/zsa, 1/zsb, gradient1);
//			zs2 = 1/Tools.interpolate(1/zsc, 1/zsd, gradient2);			
//		}
//
//
//		// Gouraud's shading (Vertex calculation and interpolation across triangle)
//		// Starting Colors & ending Colors for Shaded color and Specular color
//		Color ishc1 = null, ishc2 = null; // Shaded
//		Color ispc1 = null, ispc2 = null; // Specular
//		if (interpolate) {
//			// Shaded color
//			ishc1 = ColorTools.interpolateColors(ColorTools.multColor(va.getShadedCol(),1/za), ColorTools.multColor(vb.getShadedCol(),1/zb), gradient1);
//			ishc2 = ColorTools.interpolateColors(ColorTools.multColor(vc.getShadedCol(),1/zc), ColorTools.multColor(vd.getShadedCol(),1/zd), gradient2);
//			// Specular color
//			if (lighting.hasSpecular()) {
//				ispc1 = ColorTools.interpolateColors(ColorTools.multColor(va.getSpecularCol(),1/za), ColorTools.multColor(vb.getSpecularCol(),1/zb), gradient1);
//				ispc2 = ColorTools.interpolateColors(ColorTools.multColor(vc.getSpecularCol(),1/zc), ColorTools.multColor(vd.getSpecularCol(),1/zd), gradient2);
//			}
//		}
//
//		// Starting Texture & ending Texture coordinates
//		Vector4 vt1 = null;
//		Vector4 vt2 = null;
//		Vector4 vt = null;
//		if (texture && t!=null) {
//			vt1 = Tools.interpolate(vta.times(1/za), vtb.times(1/zb), gradient1);
//			vt2 = Tools.interpolate(vtc.times(1/zc), vtd.times(1/zd), gradient2);
//		}
//
//		Color csh = null; // Shaded color
//		Color csp = null; // Specular color
//		Color ctx = null; // Texture color
//		Color cc; // Combined color to be drawn, result of the lighting and shading calculation
//
//		// drawing a line from left (sx) to right (ex) 
//		for (int x = sx; x < ex; x++) {
//
//			// Eliminate pixels outside the gUIView screen
//			if (isInScreenX(x) && isInScreenY(y)) {
//				// Z buffer is [0, width][0, height] while screen is centered to origin -> need translation
//				int x_zBuf = getXzBuf(x);
//				int y_zBuf = getYzBuf(y);
//
//				// Protect against out of bounds (should not happen)
//				if (x_zBuf>=0 && x_zBuf<zBuf_width && y_zBuf>=0 && y_zBuf<zBuf_height) {
//
//					cc = null;
//
//					float gradient = (float)(x-sx)/(float)(ex-sx);
//					float z = 1/Tools.interpolate(1/z1, 1/z2, gradient);
//					//float z=Tools.interpolate(z1,z2,gradient); // Used for debugging Orthographic projection
//
//					// zBuffer elimination at earliest stage of computation (as soon as we know z)
//					if (z>zBuffer.get(getXzBuf(x), getYzBuf(y))) { // Discard pixel
//						discarded_pixels++;
//
//					} else { // Compute colors and draw pixel
//
//						// If interpolation
//						if (interpolate) {
//							// Color interpolation
//							csh = ColorTools.multColor(ColorTools.interpolateColors(ishc1, ishc2, gradient),z); // Shaded color
//							if (lighting.hasSpecular()) {
//								csp = ColorTools.multColor(ColorTools.interpolateColors(ispc1, ispc2, gradient),z); // Specular color
//							} else {
//								csp = DARK_SHADING_COLOR; // No specular
//							}
//						} else { // Else csh is the base color passed in arguments and csp won't be used
//							csh = shadedCol; // Shaded color passed in argument
//							// TODO specular color to be implemented
//						}
//
//						// Texture interpolation
//						if (texture && t!=null) {
//
//							vt = Tools.interpolate(vt1, vt2, gradient).times(z);
//							try {
//								// Projective Texture mapping using the fourth coordinate
//								// By default W of the texture vector is 1 but if not this will help to take account of the potential geometric distortion of the texture
//								switch (tex_orientation) {
//								case Triangle.TEXTURE_ISOTROPIC: // Default for a triangle
//									ctx = t.getInterpolatedColor(vt.getX()/vt.getW(), vt.getY()/vt.getW());
//									break;
//								case Triangle.TEXTURE_VERTICAL:
//									ctx = t.getInterpolatedColor(vt.getX()/vt.getW(), vt.getY());
//									break;
//								case Triangle.TEXTURE_HORIZONTAL:
//									ctx = t.getInterpolatedColor(vt.getX(), vt.getY()/vt.getW());
//									break;
//								default:
//									// Should never happen
//									if (Tracer.error) Tracer.traceError(this.getClass(), "Invalid Texture orientation for this triangle: "+tex_orientation);
//								}
//							} catch (Exception e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//							}
//
//						} // End Texture interpolation
//						
//						// Shadowing
//						if (shadows) { // then do the needful to know if the element is in shadow or not
//							// TODO Work in Progress - To Be Completed
//							// For each light
//							int xs = 0, ys = 0; // projected position for shadow map
//							// Interpolate across the 2 segments using gradient
//							float zs = 1/Tools.interpolate(1/zs1, 1/zs2, gradient);
//							// Calculate xs and ys by
//							// - projection using the Light coordinates matrix
//							// - tranform from [-1.1] coordinates to [0,1] by multiplying the projection matrix appropriately
//							// - transformation in integer indices of the size of the shadow map
//							if (zs<lighting.getDirectionalLight().getMap(xs,ys)) {
//								
//							} else {
//								// in shadow
//							}
//						
//							// Is there a needed correction using W coordinate ?
//							// Get the depth from the depth map using texture mapping interpolation technique
//							// Compare the 2 depths and if depth of the fragment is deeper than depth map
//							// then this fragment is in shadow and the corresponding shadow light should be 0
//							// else this fragment is in the light and shadow light should be 1
//							
//						}
//
//						// Combine colors with the following formula
//						// Color K = DTA + CDT + S = DT(A+C) + S : WRONG old calculation 
//						// Color K = DTA + C(DT + S) = DTA + DTC + SC = DT(A+C) + SC
//						// ctx = T, csh  = C, ambientCol = A, csp = S
//						// D: diffuse color, T: texture, A: Ambient color, C: color of the light source at point, S: Specular color
//						//TODO need to decouple the Ambient light from the shaded color calculation. This is easy as Ambient light do not need any interpolation
//						// This will allow to calculate the CS (shaded*specular) color
//
//						if (texture && t!=null) {
//							if (lighting.hasSpecular() && csp != null) {
//								cc = ColorTools.addColors(ColorTools.multColors(ctx, ColorTools.addColors(ambientCol, csh)), ColorTools.multColors(csh,csp));
//							} else {
//								cc = ColorTools.multColors(ctx, ColorTools.addColors(ambientCol, csh));
//							}
//						} else {
//							if (lighting.hasSpecular() && csp != null) {
//								cc = ColorTools.addColors(ambientCol,ColorTools.addColors(csh, csp));	
//							} else {
//								cc = ColorTools.addColors(ambientCol,csh);
//							}
//						}
//						// TODO also add the shadowing color
//
//						// Draw the point with calculated Combined Color
//						drawPoint(x, y, z, cc);
//					} 
//
//				} else { // Out of zBuffer range (should not happen)
//					not_rendered_pixels++;	    		
//					if (x_zBuf<0 || x_zBuf>=zBuf_width) {
//						if (Tracer.error) Tracer.traceError(this.getClass(), "Invalid zBuffer_x value while drawing points: "+x_zBuf);
//					}
//					if (y_zBuf<0 || y_zBuf>=zBuf_height) {
//						if (Tracer.error) Tracer.traceError(this.getClass(), "Invalid zBuffer_y value while drawing points: "+y_zBuf);
//					}
//				}
//			} else { // Out of screen
//				not_rendered_pixels++;
//			}
//		}
//	}

	protected void rasterizeScanLine_new(
			int 		y,				// Ordinate of the scan line
			VertexParam vpa,			// VertexParam of Vertex A of first segment: AB
			VertexParam vpb,			// VertexParam of Vertex B of first segment: AB
			VertexParam vpc,			// VertexParam of Vertex C of second segment: CD
			VertexParam vpd,			// VertexParam of Vertex D of second segment: CD
			Texture 	t,				// Texture object for this triangle
			Color 		shadedCol,		// Shaded color if Normal at triangle level (else should be null)
			Color 		ambientCol,		// Ambient color (independent of the position in space)
			boolean 	interpolate,	// Flag for interpolation (true) or not (false)
			boolean 	texture, 		// Flag for texture calculation (true) or not (false)
			int 		tex_orientation,// Flag for isotropic, vertical or horizontal texture interpolation
			boolean 	shadows,		// Flag for shadowing enabled (true) or disabled (false)
			int 		nb_lights) {	// Number of Lights (except ambient)

		// ***************************************************
		//     Global Rasterization of Scan Line Algorithm
		// ***************************************************
		//
		// If shadows enabled, for each light
		// - declare an array of ShadowMap vectors -> Done in VertexParam / VertexLightParam inner classes used as parameters to rasterizeScanLine
		// - calculate ShadowMap vectors at beginning and end of the scan line
		// - interpolate at each pixel
		//
		// For each pixel of the scan line from left to right
		// - Eliminate pixel out of screen
		// - Protect to avoid if out of bounds (should not happen)
		// - zBuffer elimination
		// - Declare an array of csh and csp colors for each Light
		// - For each Light
		// 		* If shadows enabled
		//			. Calculate if pixel is in shadow based on ShadowMap
		//			. If pixel in shadow -> color for this light is dark -> exit / go to next light iteration
		// 		* Otherwise (either because shadows are disabled or pixel not in shadow)
		// 			. Calculate csh (shaded color) for this Light
		// 			. Calculate csp (specular color) for this Light
		// - If texture enabled, calculate ctx (texture color) for this Pixel based on Texture Map
		//
		// Once all colors are calculated
		// - Add/multiply various light colors + ambient colors as needed to get Pixel's color
		//
		// Draw pixel with resulting color

		
		if (isInScreenY(y)) { // Eliminate immediately lines of pixels outside the gUIView screen

			// Thanks to current Y, we can compute the gradient to compute others values like
			// the starting X (sx) and ending X (ex) to draw between
			// if pa.Y == pb.Y or pc.Y == pd.Y, gradient is forced to 1
			float ya = yScreen(vpa.v);
			float yb = yScreen(vpb.v);
			float yc = yScreen(vpc.v);
			float yd = yScreen(vpd.v);

			float xa = xScreen(vpa.v);
			float xb = xScreen(vpb.v);
			float xc = xScreen(vpc.v);
			float xd = xScreen(vpd.v);

			// Gradient 1 is the gradient on VA VB segment
			float gradient1 = ya != yb ? (y - ya) / (yb - ya) : 1;
			// Gradient 2 is the gradient on VC VD segment
			float gradient2 = yc != yd ? (y - yc) / (yd - yc) : 1;

			int sx = (int)Tools.interpolate(xa, xb, gradient1);
			int ex = (int)Tools.interpolate(xc, xd, gradient2);

			// To avoid gradient effect on x axis for small y variations (flat slopes) -> "cap" the sx and ex to x min and max of the triangle 
			int smin = (int)Math.min(xa, xb);
			int emax = (int)Math.max(xc, xd);
			if (sx<smin) sx=smin;
			if (ex>emax) ex=emax;

			// Instrumentation for Rasterizer artifact investigation (due to calculated gradient>1 fixed by rounding in gradient calculation)
			// TODO possible optimization in Rasterizer to avoid calculation in double, to avoid rounding and use int computation as most as possible then avoid duplicate calculation in several places (x and yScreen for example)

			float z1 = 0, z2 = 0, za = 0, zb = 0, zc = 0, zd = 0;


			switch (graphic.getPerspectiveType()) {

			case GraphicContext.PERSPECTIVE_TYPE_FRUSTUM :
				// Vertices z
				za = vpa.v.getProjPos().getW();
				zb = vpb.v.getProjPos().getW();
				zc = vpc.v.getProjPos().getW();
				zd = vpd.v.getProjPos().getW();

				// Starting Z & ending Z
				z1 = 1/Tools.interpolate(1/za, 1/zb, gradient1);
				z2 = 1/Tools.interpolate(1/zc, 1/zd, gradient2);

				break;

			case GraphicContext.PERSPECTIVE_TYPE_ORTHOGRAPHIC :
				// Orthographic projection -> don't use W but use rather Z instead
				za = vpa.v.getProjPos().getZ();
				zb = vpb.v.getProjPos().getZ();
				zc = vpc.v.getProjPos().getZ();
				zd = vpd.v.getProjPos().getZ();

				// Starting Z & ending Z
				z1 = Tools.interpolate(za, zb, gradient1);
				z2 = Tools.interpolate(zc, zd, gradient2);

				break;

			default :
				// Not implemented, should never happen
				// TODO raise an UnimplementedException
			}

			// Gouraud's shading (Vertex calculation and interpolation across triangle)
			// Starting Colors & ending Colors for Shaded color and Specular color
			// Arrays of shaded colors for each Light to be interpolated
			Color [] ishc1 = null;
			Color [] ishc2 = null;
			// Arrays of specular colors for each light to be interpolated
			Color [] ispc1 = null;
			Color [] ispc2 = null;
			
			if (interpolate) {

				ishc1 = new Color [nb_lights];
				ishc2 = new Color [nb_lights];
				ispc1 = new Color [nb_lights];
				ispc2 = new Color [nb_lights];

				// For each Light
				for (int i=0; i<nb_lights; i++) {

					// Shaded color
					ishc1[i] = ColorTools.interpolateColors(ColorTools.multColor(vpa.l[i].shadedColor,1/za), ColorTools.multColor(vpb.l[i].shadedColor,1/zb), gradient1);
					ishc2[i] = ColorTools.interpolateColors(ColorTools.multColor(vpc.l[i].shadedColor,1/zc), ColorTools.multColor(vpd.l[i].shadedColor,1/zd), gradient2);
					// Specular color
					if (lighting.hasSpecular()) {
						ispc1[i] = ColorTools.interpolateColors(ColorTools.multColor(vpa.l[i].specularColor,1/za), ColorTools.multColor(vpb.l[i].specularColor,1/zb), gradient1);
						ispc2[i] = ColorTools.interpolateColors(ColorTools.multColor(vpc.l[i].specularColor,1/zc), ColorTools.multColor(vpd.l[i].specularColor,1/zd), gradient2);
					}
				} // End for each Light
			}

			// Shadows
			//		float zs1 = 0, zs2 = 0;
			if (shadows) { // then do the needed calculation to know if the element is in shadow or not
				// Any calculation before scanning the line ?
			}

			//
			// If texture enabled, calculate Texture vectors at beginning and end of the scan line
			//
			// Starting Texture & ending Texture coordinates
			Vector4 vt1 = null;
			Vector4 vt2 = null;
			Vector4 vt = null;
			if (texture && t!=null) {
				vt1 = Tools.interpolate(vpa.t.times(1/za), vpb.t.times(1/zb), gradient1);
				vt2 = Tools.interpolate(vpc.t.times(1/zc), vpd.t.times(1/zd), gradient2);
			}

			Color csh = null; // Shaded color
			Color csp = null; // Specular color
			Color ctx = null; // Texture color
			Color cc; // Combined color to be drawn, result of the lighting and shading calculation

			// drawing a line from left (sx) to right (ex) 
			for (int x = sx; x < ex; x++) {

				// Eliminate pixels outside the gUIView screen (in y dimension, the lines outside GUIView have already been eliminated earlier)
				if (isInScreenX(x)) {
					// Z buffer is [0, width][0, height] while screen is centered to origin -> need translation
					int x_zBuf = getXzBuf(x);
					int y_zBuf = getYzBuf(y);

					// Protect against out of bounds (should not happen)
					if (x_zBuf>=0 && x_zBuf<zBuf_width && y_zBuf>=0 && y_zBuf<zBuf_height) {

						cc = null;

						// Interpolation gradient on scan line from sx (0) to ex (1)
						float gradient = (float)(x-sx)/(float)(ex-sx);
						// Calculate z using 1/z interpolation
						float z = 1/Tools.interpolate(1/z1, 1/z2, gradient);
						// zBuffer elimination at earliest stage of computation (as soon as we know z)
						if (z>zBuffer.get(getXzBuf(x), getYzBuf(y))) { // Discard pixel
							discarded_pixels++;
							// Exit here
							
						} else { // General case : compute colors and draw pixel

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

							// For each light
							for (int i=0; i<nb_lights; i++) {

								// Calculate the shaded color for this Light - Gouraud's shading
								// If interpolation
								if (interpolate) {
									// Color interpolation
									csh = ColorTools.multColor(ColorTools.interpolateColors(ishc1[i], ishc2[i], gradient),z); // Shaded color
									if (lighting.hasSpecular()) {
										csp = ColorTools.multColor(ColorTools.interpolateColors(ispc1[i], ispc2[i], gradient),z); // Specular color
									} else {
										csp = DARK_SHADING_COLOR; // No specular
									}
								} else { // Else csh is the base color passed in arguments and csp won't be used
									csh = shadedCol; // Shaded color passed in argument
									// TODO specular color to be implemented
								}


								// Shadowing
								// TODO
								// Get the Map vector for each light

								if (shadows) { // then do the needful to know if the element is in shadow or not
									// TODO Work in Progress - To Be Completed
									// For each light
									//							int xs = 0, ys = 0; // projected position for shadow map
									// Interpolate across the 2 segments using gradient
									//							float zs = 1/Tools.interpolate(1/zs1, 1/zs2, gradient);
									// Calculate xs and ys by
									// - projection using the Light coordinates matrix
									// - tranform from [-1.1] coordinates to [0,1] by multiplying the projection matrix appropriately
									// - transformation in integer indices of the size of the shadow map
									//							if (zs<lighting.getDirectionalLight().getMap(xs,ys)) {

									//							} else {
									// in shadow
									//							}

									// Is there a needed correction using W coordinate ?
									// Get the depth from the depth map using texture mapping interpolation technique
									// Compare the 2 depths and if depth of the fragment is deeper than depth map
									// then this fragment is in shadow and the corresponding shadow light should be 0
									// else this fragment is in the light and shadow light should be 1

									// TODO
									// For each light
									// Get the depth of the vertices in Light coordinates
									//							float zsa = vsa_d.getW();
									//							float zsb = vsb_d.getW();
									//							float zsc = vsc_d.getW();
									//							float zsd = vsd_d.getW();

									// Interpolate across the 2 segments using gradients
									// Starting Z & ending Z
									//							zs1 = 1/Tools.interpolate(1/zsa, 1/zsb, gradient1);
									//							zs2 = 1/Tools.interpolate(1/zsc, 1/zsd, gradient2);			
								}

								// Combine colors with the following formula
								// Color K = DTA + CDT + S = DT(A+C) + S : WRONG old calculation 
								// Color K = DTA + C(DT + S) = DTA + DTC + SC = DT(A+C) + SC
								// ctx = T, csh  = C, ambientCol = A, csp = S
								// D: diffuse color, T: texture, A: Ambient color, C: color of the light source at point, S: Specular color
								//TODO need to decouple the Ambient light from the shaded color calculation. This is easy as Ambient light do not need any interpolation
								// This will allow to calculate the CS (shaded*specular) color

							} // End for each Light


							// Calculation of picel's color based on each color element
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
							// TODO also add the shadowing color

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

		} else {
			discarded_lines++;
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
		// Eliminate pixels outside the gUIView screen is done before calling this method for optimization
		// So at this stage we only render pixel and update the zBuffer

		// Draw pixel in the GUIView
		gUIView.drawPixel(x,y,c);

		// Update zBuffer of this pixel to the new z
		zBuffer.set(getXzBuf(x), getYzBuf(y), z);

		// Increment counter of rendered pixels
		rendered_pixels++;
	}

	protected boolean isInScreenX(int x) {
		if (Math.abs(x)>pixelHalfWidth) return false;
		return true;
	}

	protected boolean isInScreenY(int y) {
		if (Math.abs(y)>pixelHalfHeight) return false;
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
			return ColorTools.multColors(lighting.getAmbientLight().getLightColorAtPoint(null), baseCol);
		} else {
			// Default color
			return DARK_SHADING_COLOR; // Ambient light
		}
	}

	/**
	 * This method calculates the Color for a given normal and a base color of the surface of the Element resulting from Directional light
	 * @param baseCol of the surface in this area
	 * @param point the Vertex position where to calculate the specular reflection
	 * @param normal of the surface in this area
	 * @param rectoVerso if both sides of the triangle can be illuminated (normally false for "closed" elements like Box or Sphere)
	 * @return the resulting color from Directional light
	 */
	protected Color computeShadedColor(Color baseCol, Vector4 point, Vector3 normal, boolean rectoVerso, PointLight p) { // Should evolve to get the coordinates of the Vertex or surface for light type that depends on the location

		// Table of colors to be mixed
		Color c; // resulting color
		
		// Initialize to DARK
		c = DARK_SHADING_COLOR;

		if (lighting != null) { // If lighting exists

			// Primary shading: Diffuse Reflection (computed in computeAmbientColor method)
			
			float dotNL = 0;

			// Directional light
			if (lighting.hasDirectional()) {

				//As of now only 1 Directional Light
				DirectionalLight dir_light = lighting.getDirectionalLight();
				
				// Compute the dot product
				// Normal is normalized so the dotNL result is in the range [-1,1]
				dotNL = dir_light.getLightVectorAtPoint(null).dot(normal.normalize());
				if (rectoVerso) dotNL = Math.abs(dotNL);
				if (dotNL > 0) {
					// Multiply the base col by the Directional Light color
					c = ColorTools.multColors(baseCol, dir_light.getLightColor());
					// Multiply the color by this dot product -> this is an attenuation
					c = ColorTools.multColor(baseCol, dotNL);
				}
			}

			// Point light
			if (p != null) {

				// Compute the dot product of this Light's vector at current point and the normal vector
				dotNL = p.getLightVectorAtPoint(point).dot(normal.normalize());
				if (rectoVerso) dotNL = Math.abs(dotNL);
				if (dotNL > 0) {
					Color col = ColorTools.multColors(baseCol, p.getLightColor());
					// Multiply the color by the new dotNL
					col = ColorTools.multColor(col, dotNL);
					// Add this attenuated Color to all other Lights at this point
					c = ColorTools.addColors(c, col);
				}
			}

		} else { // If no lighting, return base color
			return baseCol;
		}

		return c;
	}
	

	/**
	 * @param normal the normal vector
	 * @param viewer normalized vector
	 * @param point the Vertex position where to calculate the specular reflection
	 * @param e specular exponent
	 * @param sc specular color
	 * @param rectoVerso true if this triangle can be seen back side
	 * @return the specular color
	 */
	protected Color computeSpecularColor(Vector3 normal, Vector3 viewer, Vector4 point, float e, Color sc, boolean rectoVerso, PointLight p) {

		Color c = DARK_SHADING_COLOR; // Specular reflection from Directional light
		Color spc = sc == null ? DEFAULT_SPECULAR_COLOR : sc;

		// Secondary Shading: Specular reflection (from Directional light)
		if (e>0) { // If e=0 this is considered as no specular reflection

			// Specular reflection algorithm for 1 Light
			// R: Reflection vector, L: Light vector, N: Normal vector on the surface. R+L=N+N => R = 2N-L
			// This has to be applied to each Light source (having a light vector so except Ambient)

			// Directional light
			if (lighting.hasDirectional()) {
				
				// Calculate reflection vector R = 2N-L and normalize it
				float dotNL = lighting.getDirectionalLight().getLightVectorAtPoint(null).dot(normal.normalize());
				Vector3 r = (normal.times(2*dotNL)).minus(lighting.getDirectionalLight().getLightVectorAtPoint(null)); 

				float dotRV = r.dot(viewer);
				if (rectoVerso)
					dotRV = Math.abs(dotRV);
				else
					if (dotRV <0) dotRV = 0; // Clamped to 0 if negative
				// Compute the dot product
				//TODO Problem here: the following calculation creates abrupt specular (but it seems normal situation with Gouraud's shading
				if (dotNL > 0 && dotRV >0) {
					float specular = (float) Math.pow(dotRV, e);
					float intensity = lighting.getDirectionalLight().getIntensity(null);
					c = ColorTools.multColor(spc, specular*intensity);
				}
			}

			// Point light
			if (p != null) {

				// Calculate reflection vector R = 2N-L and normalize it
				Vector3 lightVector = p.getLightVectorAtPoint(point).normalize();
				float dotNL = lightVector.dot(normal.normalize());
				Vector3 r = (normal.times(2*dotNL)).minus(p.getLightVectorAtPoint(point)); 

				float dotRV = r.dot(viewer);
				if (rectoVerso)
					dotRV = Math.abs(dotRV);
				else
					if (dotRV <0) dotRV = 0; // Clamped to 0 if negative

				if (dotNL > 0 && dotRV >0) {
					float specular = (float) Math.pow(dotRV, e);
					float intensity = p.getIntensity(point);
					Color col = ColorTools.multColor(spc, specular*intensity);
					//System.out.println("Point light specular color calculation. Specular: " + specular + " Intensity: "+ intensity);
					c = ColorTools.addColors(c, col);
				}
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
		gUIView.drawPixel(x, y);
	}
	
	//
	// **** END BRESSENHAM ***
	//

}