package com.aventura.engine;

import java.awt.Color;
import java.util.ArrayList;

import com.aventura.context.PerspectiveContext;
import com.aventura.math.vector.Tools;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.Lighting;
import com.aventura.model.light.ShadowingLight;
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
 * Copyright (c) 2016-2025 Olivier BARRY
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
 * It also behaves according to the provided parameters e.g. in the PerspectiveContext
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
		// Shadowing parameters
		public Vector4 vl; // Projected position in light coordinates of Vertex
		public MapView map; // Shadow map of this Light

		public VertexLightParam() {
		}

		public VertexLightParam(Color shaded, Color specular, Vector4 vl, Vector4 vm, MapView m) {
			this.shadedColor = shaded; // Shaded color of the light at Vertex position (calculated with normal)
			this.specularColor = specular; // Specular color of the light at Vertex position
			this.vl = vl; // 
			this.map = m;
		}
	}

	protected class VertexParam {
		public Vertex v; // Vertex
		public Vector4 t; // Texture vector
		public VertexLightParam [] l; // one parameter for each light (except ambient)

		public VertexParam() {
		}

		public VertexParam(Vertex v) {
			this.v = v;
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
	protected PerspectiveContext perspectiveCtx;
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
	int outOfBounds_pixels = 0;
	int calculated_pixels = 0;
	int processed_pixels = 0;
	int discarded_lines = 0;
	int rasterized_lines = 0;
	int lines_with_no_pixel = 0;
	int reversed_lines = 0;
	int rendered_triangles = 0;
	int triangles_with_lines = 0;
	int triangles_with_pixels = 0;

	// Create locally some context variables exhaustively used during rasterization
	// TODO Be cautious here : if PerspectiveContext has changed between 2 calls to previously created Rasterizer, these 2 variables won't be refreshed accordingly -> potential bug
	int pixelHalfWidth = 0;
	int pixelHalfHeight = 0;

	/**
	 * Creation of Rasterizer with requested references for run time.
	 * @param camera : a pointer to the Camera created offline by user
	 * @param perspectiveCtx : a pointer to the PerspectiveContext created offline by user
	 * @param lighting : a pointer to the Lighting system created offline by user
	 */
	public Rasterizer(Camera camera, PerspectiveContext graphic, Lighting lighting) {
		this.camera = camera;
		this.perspectiveCtx = graphic;
		this.lighting = lighting;
		pixelHalfWidth = graphic.getPixelHalfWidth();
		pixelHalfHeight = graphic.getPixelHalfHeight();
		// TODO Be cautious here : if PerspectiveContext has changed between 2 calls to previously created Rasterizer, the 2 above variables won't be refreshed accordingly -> potential bug
	}

	/**
	 * Creation of minimal Rasterizer for shadow map rendering
	 */
	public Rasterizer(Camera camera, PerspectiveContext graphic) {
		this.camera = camera;
		this.perspectiveCtx = graphic;
		this.lighting = null;
		pixelHalfWidth = graphic.getPixelHalfWidth();
		pixelHalfHeight = graphic.getPixelHalfHeight();
	}

	public void setView(GUIView v) {
		this.gUIView = v;
	}

	/**
	 * Initialize zBuffer using the pixelHalfWidth and pixelHalfHeight values from the Constructor
	 */
	public MapView initZBuffer() {

		return initZBuffer(2 * pixelHalfWidth  + 1, 2 * pixelHalfHeight + 1);
	}
	
	/**
	 * Initialize zBuffer by creating the table. This method is deported from the constructor in order to use it only when necessary.
	 * It is not needed in case of line rendering.
	 * @param zBuf_width the width of the zBuffer to create
	 * @param zBuf_height the height of the zBuffer to create
	 */
	public MapView initZBuffer(int width, int height) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating zBuffer. Width: " + width + " Height: " + height);

		this.zBuf_width = width;
		this.zBuf_height = height;
		// zBuffer is initialized with far value of the perspectiveCtx
		float zBuffer_init = perspectiveCtx.getPerspective().getFar();
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "zBuffer init value: "+zBuffer_init);

		// Only create buffer if needed, otherwise reuse it, it will be reinitialized below
		if (zBuffer == null) zBuffer = new MapView(zBuf_width, zBuf_height);

		// Initialization loop with initialization value ( 1 or -1 in homogeneous coordinates ?) that is the farest value for the gUIView Frustum
		// Any value closer will be drawn and the zBuffer in this place will be updated by new value
		for (int i=0; i<zBuf_width; i++)  {
			for (int j=0; j<zBuf_height; j++) {
				zBuffer.set(i, j, zBuffer_init); // Far value of the perspectiveCtx
			}
		}
		return zBuffer;
	}


	//
	// A few tools, some methods to simplify method calls
	//
	// TODO Shouldn't this transformation be handled through the projection matrix to avoid additional computation for each pixel ?
	protected float xScreen(Vertex v) {
		return v.getProjPos().get3DX()*pixelHalfWidth;
	}

	protected float yScreen(Vertex v) {
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
	 * @param t				the triangle to render
	 * @param surfCol		the base surface color of the triangle, may be inherited from the element or world (default)
	 * @param specExp		the specular exponent of the Element
	 * @param specCol		the specular color of the Element
	 * @param interpolate	a boolean to indicate if interpolation of colors is activated (true) or not (false)
	 * @param texture		a boolean to indicate if texture processing is activated (true) or not (false)
	 * @param shadows		a boolean to indicate if shadowing is enabled (true) or not (false)
	 * @param shadowmap		a boolean to indicate whether this is a rasterization only for a shadow map (true) or not (false). Rasterizing a shadow map involves a simplified algorithm.
	 * 
	 **/
	public void rasterizeTriangle(
			Triangle t,
			Color surfCol,
			float specExp,
			Color specCol,
			boolean interpolate,
			boolean texture,
			boolean shadows,
			boolean shadowmap) {

		if (Tracer.debug) Tracer.traceDebug(this.getClass(), "Rasterize triangle. Color: "+surfCol);

		// Init pixel stats
		rendered_pixels = 0;
		discarded_pixels = 0;
		not_rendered_pixels = 0;
		outOfBounds_pixels = 0;
		calculated_pixels = 0;
		processed_pixels = 0;
		rasterized_lines = 0;
		lines_with_no_pixel = 0;
		reversed_lines = 0;

		Color shadedCol = null;
		Color ambientCol = null; // Let's compute Ambient color once per triangle (not needed at each line or pixel)
		VertexParam vpa, vpb, vpc = null;
		int nb_sl = 0; // Number of Shadowing Lights, obtained later if not rasterizing only a Shadow Map

		if (shadowmap) { // Rasterize a Shadowmap
			vpa = new VertexParam(t.getV1());
			vpb = new VertexParam(t.getV2());
			vpc = new VertexParam(t.getV3());

		} else { // Generic case
			ambientCol = computeAmbientColor(surfCol); // Let's compute Ambient color once per triangle (not needed at each line or pixel)

			// Let's create 3 VertexParam "containers", one for each of the 3 Vertices of the triangle to be rendered and start building them with Vertex and Texture vectors
			// They will be used as parameters to be passed to rasterizeScanLight function containing a structure set of data
			vpa = new VertexParam(t.getV1(), t.getTexVec1());
			vpb = new VertexParam(t.getV2(), t.getTexVec2());
			vpc = new VertexParam(t.getV3(), t.getTexVec3());
		} 
		
		// Lets order them to always have this order on screen v1, v2 & v3 in screen coordinates
		// with v1 always down (thus having the highest possible Y)
		// then v2 between v1 & v3 (or same level if v2 and v3 on same ordinate)	
		VertexParam vp1, vp2, vp3; // Ordered Vertex containers

		// TODO use color at Vertex level if defined. This requires to manage 3 colors for a triangle in this case

		if (vpb.v.getProjPos().get3DY()<vpa.v.getProjPos().get3DY()) { // p2 lower than p1
			if (vpc.v.getProjPos().get3DY()<vpb.v.getProjPos().get3DY()) { // p3 lower than p2
				vp1 = vpc;
				vp2 = vpb;
				vp3 = vpa;

			} else { // p2 lower or equal than p3
				if (vpc.v.getProjPos().get3DY()<vpa.v.getProjPos().get3DY()) { // p3 lower than p1
					vp1 = vpb;
					vp2 = vpc;
					vp3 = vpa;

				} else { // p1 higher or equal than p3
					vp1 = vpb;
					vp2 = vpa;
					vp3 = vpc;

				}
			}
		} else { // p1 lower than p2
			if (vpc.v.getProjPos().get3DY()<vpa.v.getProjPos().get3DY()) { // p3 lower than p1
				vp1 = vpc;
				vp2 = vpa;
				vp3 = vpb;

			} else { // p1 lower than p3
				if (vpc.v.getProjPos().get3DY()<vpb.v.getProjPos().get3DY()) { // p3 lower than p2
					vp1 = vpa;
					vp2 = vpc;
					vp3 = vpb;

				} else {
					vp1 = vpa;
					vp2 = vpb;
					vp3 = vpc;

				}
			}
		}

		if (!shadowmap) { // Generic case (not rendering a shadow map)
			// Initialize n VertexParamLight structures, one for each light, for each VertexParam "container" previously created
			ArrayList<ShadowingLight> shadowingLights = lighting.getShadowingLights();

			// If there are Directional or Point Lights
			if (lighting.hasShadowing()) {

				nb_sl = shadowingLights.size();

				vp1.l = new VertexLightParam[nb_sl];
				vp2.l = new VertexLightParam[nb_sl];
				vp3.l = new VertexLightParam[nb_sl];

				// For each Light
				for (int i=0; i<nb_sl; i++) {
					vp1.l[i] = new VertexLightParam();
					vp2.l[i] = new VertexLightParam();
					vp3.l[i] = new VertexLightParam();	
				}

			} else {

				nb_sl = 0;

				vp1.l = null;
				vp2.l = null;
				vp3.l = null;
			}


			// If no interpolation requested -> plain faces. Then:
			// - calculate normal at Triangle level for shading
			// - calculate shading color once for all triangle
			if (!interpolate || t.isTriangleNormal()) {
				Vector3 normal = t.getWorldNormal();

				if (lighting.hasShadowing()) {
					Color[] cols = new Color[nb_sl];
					// For each Light
					for (int i=0; i<nb_sl; i++) {
						cols[i] = computeShadedColor(surfCol, t.getCenterWorldPos(), normal, t.isRectoVerso(), shadowingLights.get(i));
					}
					shadedCol = ColorTools.addColors(cols);
				} else {
					shadedCol = surfCol;
				}
				//TODO Specular reflection with plain faces.

			} else {

				// TODO Potential optimization: pre-calculate the viewer vectors and shaded colors to each Vertex before in 1 row - May not be optimal TBC
				// This will avoid to do the same calculation for a Vertex shared by several triangles (which is the general case)

				// Calculate viewer vectors
				Vector3 viewer1, viewer2, viewer3;
				// TODO TO BE VERIFIED : Viewers vectors are calculated on triangle's V1, V2, V3 Vertices then used below for VP1, VP2 VP3 that might be in different order... PROBLEM ?
				viewer1 = camera.getEye().minus(t.getV1().getWorldPos()).V3();
				viewer2 = camera.getEye().minus(t.getV2().getWorldPos()).V3();
				viewer3 = camera.getEye().minus(t.getV3().getWorldPos()).V3();

				viewer1.normalize();
				viewer2.normalize();
				viewer3.normalize();

				// For each Light
				for (int i=0; i<nb_sl; i++) {

					ShadowingLight sl = shadowingLights.get(i); // Used several times

					vp1.l[i].shadedColor = computeShadedColor(surfCol, vp1.v.getWorldPos(), vp1.v.getWorldNormal(), t.isRectoVerso(), sl);
					vp2.l[i].shadedColor = computeShadedColor(surfCol, vp2.v.getWorldPos(), vp2.v.getWorldNormal(), t.isRectoVerso(), sl);
					vp3.l[i].shadedColor = computeShadedColor(surfCol, vp3.v.getWorldPos(), vp3.v.getWorldNormal(), t.isRectoVerso(), sl);	

					if (lighting.hasSpecular()) {
						vp1.l[i].specularColor = computeSpecularColor(vp1.v.getWorldNormal(), viewer1, vp1.v.getWorldPos(), specExp, specCol, t.isRectoVerso(), sl);
						vp2.l[i].specularColor = computeSpecularColor(vp2.v.getWorldNormal(), viewer2, vp2.v.getWorldPos(), specExp, specCol, t.isRectoVerso(), sl);
						vp3.l[i].specularColor = computeSpecularColor(vp3.v.getWorldNormal(), viewer3, vp3.v.getWorldPos(), specExp, specCol, t.isRectoVerso(), sl);
					}
				}
			}

			if (shadows) {
				// For each Light
				for (int i=0; i<nb_sl; i++) {

					ShadowingLight sl = shadowingLights.get(i); // Used several times

					// Transform the World position of the Vertex in this Light's coordinates
					vp1.l[i].vl = sl.getModelView().projectVertex(vp1.v);
					vp2.l[i].vl = sl.getModelView().projectVertex(vp2.v);
					vp3.l[i].vl = sl.getModelView().projectVertex(vp3.v);

					// Provide the link to Shadow Map for this Light
					vp1.l[i].map = sl.getMap();
					vp2.l[i].map = sl.getMap();
					vp3.l[i].map = sl.getMap();
				}
			}

		}

		// Shadows
		//
		// TODO SHADOWS : THIS SHOULD BE DONE BEFORE REORDERING THE 3 VERTICES SO THAT THIS IS MATCHING !!!
		//
		//		if (shadows) { // then do the needed calculation to know if the element is in shadow or not
		//
		//			// For shadows we use same approach than Textures : at triangle level (here) : do a projection each of the 3 vertices in (each) light coordinates
		//			// Then, at pixel level ( when rasterizing the scan lines) : interpolate the position in light coordinates and then get the shadow map value using the
		//			// interpolated coordinates in light coordinates. Interpolation should also have depth correction.
		//
		//			// Directional light - (one single Directional light for instance) TODO in next evolution: multiple Directional Lights
		//
		//			// For each of the 3 Vertices
		//			// Get the World position
		//			// translate in Light coordinates using the matrix in Shadowing class
		//
		//			// For each Point light
		//			if (nb_sl >0) {
		//
		//				vs1_p = new Vector4[nb_sl];
		//				vs2_p = new Vector4[nb_sl];
		//				vs3_p = new Vector4[nb_sl];
		//
		//				// For each of the 3 Vertices
		//				// Get the World position
		//				// translate in Light coordinates using the matrix in Shadowing class
		//				for (int i=0; i<nb_sl; i++) {
		//					vs1_p[i] = lighting.getDirectionalLight().getModelView().project(vp1.v);
		//					vs2_p[i] = lighting.getDirectionalLight().getModelView().project(vp2.v);
		//					vs3_p[i] = lighting.getDirectionalLight().getModelView().project(vp3.v);
		//				}
		//			}
		//
		//			// Get the depth of the vertices in Light coordinates
		//			// Prepare the 3 depths to be interpolated in the rasterizeScanLine method
		//		}


		// Invert slopes
		float dP1P2, dP1P3;

		// http://en.wikipedia.org/wiki/Slope
		// Computing invert slopes
		if (yScreen(vp2.v) - yScreen(vp1.v) > 0) {
			dP1P2 = (xScreen(vp2.v)-xScreen(vp1.v))/(yScreen(vp2.v)-yScreen(vp1.v));
		} else { // horizontal segment, infinite invert slope
			dP1P2 = Float.MAX_VALUE;
		}

		if (yScreen(vp3.v) - yScreen(vp1.v) > 0) {
			dP1P3 = (xScreen(vp3.v)-xScreen(vp1.v))/(yScreen(vp3.v)-yScreen(vp1.v));
		} else { // horizontal segment, infinite invert slope
			dP1P3 = Float.MAX_VALUE;
		}

		if (dP1P2 > dP1P3) {

			// First case where triangle is like that:
			//   P3
			//   +
			//   | \
			//   |   \
			//   |     \
			//   |       + P2
			//   |     /
			//   |   /
			//   | /
			//   +
			//   P1

			for (int y = (int)yScreen(vp1.v); y <= (int)yScreen(vp3.v); y++) {
				if (y < yScreen(vp2.v)) {
					rasterizeScanLine(
							y,
							vp1,
							vp3,
							vp1,
							vp2,
							t.getTexture(),
							shadedCol,
							ambientCol,
							interpolate && !t.isTriangleNormal(),
							texture,
							t.getTextureOrientation(),
							shadows,
							nb_sl,
							shadowmap);
				} else {
					rasterizeScanLine(
							y,
							vp1,
							vp3,
							vp2,
							vp3,
							t.getTexture(),
							shadedCol,
							ambientCol,
							interpolate && !t.isTriangleNormal(),
							texture,
							t.getTextureOrientation(),
							shadows,
							nb_sl,
							shadowmap);
				}
			}

		} else {

			// Second case where triangle is like that:
			//             P3
			//             +
			//           / | 
			//         /   |
			//       /     |
			//  P2 +       | 
			//       \     |
			//         \   |
			//           \ |
			//             +
			//             P1

			for (int y = (int)yScreen(vp1.v); y <= (int)yScreen(vp3.v); y++) {
				if (y < yScreen(vp2.v)) {
					rasterizeScanLine(
							y,
							vp1,
							vp2,
							vp1,
							vp3,
							t.getTexture(),
							shadedCol,
							ambientCol,
							interpolate && !t.isTriangleNormal(),
							texture,
							t.getTextureOrientation(),
							shadows,
							nb_sl,
							shadowmap);
				} else {
					rasterizeScanLine(
							y,
							vp2,
							vp3,
							vp1,
							vp3,
							t.getTexture(),
							shadedCol,
							ambientCol,
							interpolate && !t.isTriangleNormal(),
							texture,
							t.getTextureOrientation(),
							shadows,
							nb_sl,
							shadowmap);
				}
			}
		}
		
		// Global stats for Rasterizer
		rendered_triangles++;
		if (rasterized_lines >0) triangles_with_lines++;
		if (rendered_pixels > 0) triangles_with_pixels++;

		if (Tracer.debug) Tracer.traceDebug(this.getClass(), "Rendered pixels for this triangle: "+rendered_pixels+". Discarded: "+discarded_pixels+". Not rendered: "+not_rendered_pixels+". Out of bounds pixels: "+outOfBounds_pixels+". Rasterized lines: "+rasterized_lines+". Discarded lines: "+discarded_lines);
		if (rendered_pixels == 0) if (Tracer.info) Tracer.traceInfo(this.getClass(), "No pixels rendered for this triangle : discarded: " + discarded_pixels + ", not rendered: " + not_rendered_pixels + ", out of bound: " + outOfBounds_pixels + ", calculated: " + calculated_pixels + ", processed: " + processed_pixels + " Lines : rasterized: " + rasterized_lines + ", discarded: " + discarded_lines + " with no pixels: " + lines_with_no_pixel + ", reversed: " + reversed_lines);

	}


	/**
	 * Rasterization of an horizontal "scan line" of pixels.
	 * The scan line is at ordinate y and will start x from segment AB to segment CD
	 * 
	 *  Y
	 *  ^	            B		D
	 *  |	            +		+
	 *  |	          / 		 \
	 *	|	        /   		  \
	 *	+ y	      /----------------\	Scan Line
	 *	|		/					\
	 * 	|	 A +					 + C
	 *	|
	 * 	+---------+----------------+-------------> X
	 * 			  xAB			   xCD
	 * 
	 * @param y					Ordinate of the scan line
	 * @param vpa				VertexParam of Vertex A of first segment: AB
	 * @param vpb				VertexParam of Vertex B of first segment: AB
	 * @param vpc				VertexParam of Vertex C of second segment: CD
	 * @param vpd				VertexParam of Vertex D of second segment: CD
	 * @param tex				Texture object for this triangle
	 * @param shadedCol			Shaded color if Normal at triangle level (else should be null)
	 * @param ambientCol		Ambient color (independent of the position in space)
	 * @param interpolate		Flag for interpolation (true) or not (false), also for normal at triangle level (false)
	 * @param texture			Flag for texture calculation (true) or not (false)
	 * @param tex_orientation	Flag for isotropic, vertical or horizontal texture interpolation
	 * @param shadows			Flag for shadowing enabled (true) or disabled (false)
	 * @param nb_lights			Number of Lights (except ambient)
	 * @param shadowmap			Flag for shadow map rasterization (true) or full rasterization (false)
	 */
	protected void rasterizeScanLine(
			int 		y,
			VertexParam vpa,
			VertexParam vpb,
			VertexParam vpc,
			VertexParam vpd,
			Texture 	tex,
			Color 		shadedCol,
			Color 		ambientCol,
			boolean 	interpolate,
			boolean 	texture,
			int 		tex_orientation,
			boolean 	shadows,
			int 		nb_lights,
			boolean 	shadowmap) {


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
			rasterized_lines++;
			
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
			if (Tracer.debug) Tracer.traceDebug(this.getClass(), "Rasterizing Scan Line for y = " + y + ". xa: " + xa + " ya: " + ya + " xb: " + xb + " yb: " + yb + " xc: " + xc + " yc: " + yc + " xd: " + xd + " yd: " + yd);

			// Gradient 1 is the gradient on VA VB segment
			float gradient1 = ya != yb ? (y - ya) / (yb - ya) : 1;
			// Gradient 2 is the gradient on VC VD segment
			float gradient2 = yc != yd ? (y - yc) / (yd - yc) : 1;
			
			int sx, ex, smin, smax, emin, emax;
			
			//if ((xa+xb) <= (xc+xd)) {

			sx = (int)Tools.interpolate(xa, xb, gradient1);
			ex = (int)Tools.interpolate(xc, xd, gradient2);

			// To avoid gradient effect on x axis for small y variations (flat slopes) -> "cap" the sx and ex to x min and max of the triangle 
			smin = (int)Math.min(xa, xb);
			smax = (int)Math.max(xa, xb);
			emin = (int)Math.min(xc, xd);
			emax = (int)Math.max(xc, xd);

			//} else {
				
			//	sx = (int)Tools.interpolate(xc, xd, gradient2);
			//	ex = (int)Tools.interpolate(xa, xb, gradient1);

				// To avoid gradient effect on x axis for small y variations (flat slopes) -> "cap" the sx and ex to x min and max of the triangle 
			//	smin = (int)Math.min(xc, xd);
			//	emax = (int)Math.max(xa, xb);

			//}
			
			if (sx < smin) sx = smin;
			if (sx > smax) sx = smax;
			if (ex < emin) ex = emin;
			if (ex > emax) ex = emax;

			// Instrumentation for Rasterizer artifact investigation (due to calculated gradient>1 fixed by rounding in gradient calculation)
			// TODO possible optimization in Rasterizer to avoid calculation in double, to avoid rounding and use int computation as most as possible then avoid duplicate calculation in several places (x and yScreen for example)

			float z1 = 0, z2 = 0, za = 0, zb = 0, zc = 0, zd = 0;


			switch (perspectiveCtx.getPerspectiveType()) {

			case PerspectiveContext.PERSPECTIVE_TYPE_FRUSTUM :
				// Vertices z
				za = vpa.v.getProjPos().getW();
				zb = vpb.v.getProjPos().getW();
				zc = vpc.v.getProjPos().getW();
				zd = vpd.v.getProjPos().getW();

				// Starting Z & ending Z
				z1 = 1/Tools.interpolate(1/za, 1/zb, gradient1);
				z2 = 1/Tools.interpolate(1/zc, 1/zd, gradient2);

				break;

			case PerspectiveContext.PERSPECTIVE_TYPE_ORTHOGRAPHIC :
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

			// Light vectors at begining and end of the segment to interpolate
			Vector4 [] vl1 = null;
			Vector4 [] vl2 = null;
			
			// Starting Texture & ending Texture coordinates
			Vector4 vt1 = null;
			Vector4 vt2 = null;
			Vector4 vt = null;

			if (!shadowmap) {

				if (interpolate) {
					ishc1 = new Color [nb_lights];
					ishc2 = new Color [nb_lights];
					ispc1 = new Color [nb_lights];
					ispc2 = new Color [nb_lights];
				}

				if (shadows) {
					vl1 = new Vector4 [nb_lights];
					vl2 = new Vector4 [nb_lights];
				}

				// For each Light
				for (int i=0; i<nb_lights; i++) {

					if (interpolate) {
						// Shaded color
						ishc1[i] = ColorTools.interpolateColors(ColorTools.multColor(vpa.l[i].shadedColor,1/za), ColorTools.multColor(vpb.l[i].shadedColor,1/zb), gradient1);
						ishc2[i] = ColorTools.interpolateColors(ColorTools.multColor(vpc.l[i].shadedColor,1/zc), ColorTools.multColor(vpd.l[i].shadedColor,1/zd), gradient2);
						// Specular color
						if (lighting.hasSpecular()) {
							ispc1[i] = ColorTools.interpolateColors(ColorTools.multColor(vpa.l[i].specularColor,1/za), ColorTools.multColor(vpb.l[i].specularColor,1/zb), gradient1);
							ispc2[i] = ColorTools.interpolateColors(ColorTools.multColor(vpc.l[i].specularColor,1/zc), ColorTools.multColor(vpd.l[i].specularColor,1/zd), gradient2);
						}
					} // Else (!interpolate) : do nothing (no interpolation or normal at triangle level)

					if (shadows) {
						// Interpolate on each [VA, VB] and [VC, VD] segments for each Light
						vl1[i] = Tools.interpolate(vpa.l[i].vl.times(1/za), vpb.l[i].vl.times(1/zb), gradient1);
						vl2[i] = Tools.interpolate(vpc.l[i].vl.times(1/zc), vpd.l[i].vl.times(1/zd), gradient2);	
					}
				} // End for each Light

				//
				// If texture enabled, calculate Texture vectors at beginning and end of the scan line
				//
				if (texture && tex!=null) {
					vt1 = Tools.interpolate(vpa.t.times(1/za), vpb.t.times(1/zb), gradient1);
					vt2 = Tools.interpolate(vpc.t.times(1/zc), vpd.t.times(1/zd), gradient2);
				}
			}
			// Resulting colors
			Color csp = DARK_SHADING_COLOR; // Specular color
			Color ctx = null; // Texture color
			Color cc; // Combined color to be drawn, result of the lighting and shading calculation
			
			
			// drawing a line from left (sx) to right (ex)
			if (sx == ex) {
				lines_with_no_pixel++;
				if (Tracer.debug) Tracer.traceDebug(this.getClass(), "sx = ex, no pixels drawn on rasterized scan line");
			}
			
			// To loop from real min to real max
			int startx = Math.min(sx,  ex);
			int endx = Math.max(sx, ex);
			
			for (int x = startx; x < endx; x++) {
				
				processed_pixels++;

				// Eliminate pixels outside the gUIView screen (in y dimension, the lines outside GUIView have already been eliminated earlier)
				if (isInScreenX(x)) {
					// Z buffer is [0, width][0, height] while screen is centered to origin -> need translation
					int x_zBuf = getXzBuf(x);
					int y_zBuf = getYzBuf(y);

					// Protect against out of bounds (should not happen)
					if (x_zBuf>=0 && x_zBuf<zBuf_width && y_zBuf>=0 && y_zBuf<zBuf_height) {

						// Interpolation gradient on scan line from sx (0) to ex (1)
						float gradient = (float)(x-sx)/(float)(ex-sx);
						// Calculate z using 1/z interpolation
						float z = 1/Tools.interpolate(1/z1, 1/z2, gradient);

						// zBuffer elimination at earliest stage of computation (as soon as we know z)
						if (z>zBuffer.get(getXzBuf(x), getYzBuf(y))) { // Discard pixel
							discarded_pixels++;
							// Exit here

						} else { // General case : compute colors and draw pixel


							if (!shadowmap) { // General rasterization case, not a shadow map rasterization
								
								calculated_pixels++;
								
								cc = null;

								// Texture interpolation
								if (texture && tex!=null) {

									vt = Tools.interpolate(vt1, vt2, gradient).times(z);
									try {
										// Projective Texture mapping using the fourth coordinate
										// By default W of the texture vector is 1 but if not this will help to take account of the potential geometric distortion of the texture
										switch (tex_orientation) {
										case Triangle.TEXTURE_ISOTROPIC: // Default for a triangle
											ctx = tex.getInterpolatedColor(vt.getX()/vt.getW(), vt.getY()/vt.getW());
											break;
										case Triangle.TEXTURE_VERTICAL:
											ctx = tex.getInterpolatedColor(vt.getX()/vt.getW(), vt.getY());
											break;
										case Triangle.TEXTURE_HORIZONTAL:
											ctx = tex.getInterpolatedColor(vt.getX(), vt.getY()/vt.getW());
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

								// --------------------------
								// ---- Color Combination ---
								// --------------------------

								// Combine colors with the following formula
								// One Light : Color K = DTA + C(DT + S) = DTA + DTC + SC = DT(A+C) + SC
								// Multiple Lights : Color K = DTA + SUM(Ci(DT + Si)) = DTA + SUM(CiDT) + SUM(CiSi)
								// D: diffuse color, T: texture, A: Ambient color, Ci: color of the Light(i) source at point, Si: Specular color of the Light(i)
								// Old : ctx = T, csh_l[i]  = Ci, ambientCol = A, csp_l[i] = Si

								// Combine the multiple Light's Colors
								// ------------------------------------
								// Table of Colors for each light : Ci x D x T and Ci x Si respectively that will be combined later							
								Color[] c_CiDT = new Color[nb_lights];
								Color[] c_CiSi = new Color[nb_lights];

								Color csh_l = null; // shaded color for the light
								Color csp_l = null; // specular color for the light

								for (int i=0; i<nb_lights; i++) {

									float shadowCoef = 1;

									if (shadows) { // then do the needful to know if the element is in shadow or not
										Vector4 vl = Tools.interpolate(vl1[i], vl2[i], gradient).times(z);
										shadowCoef = vpa.l[i].map.getInterpolation(vl.getX()/vl.getW(), vl.getY()/vl.getW());

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

									// Calculate the shaded color for this Light - Gouraud's shading
									// If interpolation
									if (interpolate) {
										// Color interpolation
										csh_l = ColorTools.multColor(ColorTools.interpolateColors(ishc1[i], ishc2[i], gradient),z); // Shaded color of this light
										if (lighting.hasSpecular()) {
											csp_l = ColorTools.multColor(ColorTools.interpolateColors(ispc1[i], ispc2[i], gradient),z); // Specular color of this light
										} else {
											csp_l = DARK_SHADING_COLOR; // No specular
										}
									} else {  // no interpolation or normal at triangle level
										// In this case csh is the base color passed in arguments and csp won't be used
										csh_l = shadedCol; // Shaded color passed in argument
										csp_l = DARK_SHADING_COLOR; // No specular

										// TODO specular color to be implemented in case of normal at triangle level
									}
									
									if (shadows) {
										csh_l = ColorTools.multColor(csh_l,  shadowCoef);
										if (lighting.hasSpecular()) {
											csp_l = ColorTools.multColor(csp_l,  shadowCoef);
										}
									}

									if (texture && tex!=null) {
										c_CiDT[i] = ColorTools.multColors(csh_l, ctx);
									} else {
										c_CiDT[i] = csh_l;
									}

									if (lighting.hasSpecular()) {
										c_CiSi[i] = ColorTools.multColors(csh_l, csp_l);
									}

								} // End for each Light

								// Combine each element of the formula to get one Color
								// ----------------------------------------------------
								// Multiple Lights : Color K = DTA + SUM(CiDT) + SUM(CiSi)

								Color c_DTA = null;
								Color c_CiDT_sum = null;
								Color c_CiSi_sum = null;

								// DTA calculation
								if (texture && tex != null && ambientCol != null) {
									c_DTA = ColorTools.multColors(ctx, ambientCol);
								} else if (ambientCol != null) {
									c_DTA = ambientCol;
								} else {
									c_DTA = DARK_SHADING_COLOR; // In case there is no ambient color
								}

								// SUM(CiDT) and SUM(CiSi) calculation, then sum the total
								//if (interpolate) {
								c_CiDT_sum = ColorTools.addColors(c_CiDT); // Add all CiDT elements together
								if (lighting.hasSpecular()) c_CiSi_sum = ColorTools.addColors(c_CiSi); // Add all CiSi elements together

								// Sum the total
								if (lighting.hasSpecular() && csp != null) {
									// General case with all type of light (shaded and specular)
									cc = ColorTools.addColors(c_DTA, ColorTools.addColors(c_CiDT_sum, c_CiSi_sum));
								} else { // No Specular light, formula is simplified
									cc = ColorTools.addColors(c_DTA, c_CiDT_sum);
								}
								
								// ----------------------------------------------
								// Pixel drawing
								// Draw the point with calculated Combined Color
								// ----------------------------------------------
								drawPoint(x, y, z, cc);

							} else { // Shadow map rasterization
								
								drawMap(x, y, z); // Only update the zBuffer that will become the shadow map at the end of the rasterization

							}
						} 

					} else { // Out of zBuffer range (should not happen)
						outOfBounds_pixels++;;	    		
						if (x_zBuf<0 || x_zBuf>=zBuf_width) {
							if (Tracer.error) Tracer.traceError(this.getClass(), "Invalid zBuffer_x value: " + x_zBuf + " while drawing points. zBuf_width: " + zBuf_width);
						}
						if (y_zBuf<0 || y_zBuf>=zBuf_height) {
							if (Tracer.error) Tracer.traceError(this.getClass(), "Invalid zBuffer_y value: " + y_zBuf + " while drawing points. zBuf_height: " + zBuf_height);
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

	/**
	 * Draw Map only (zBuffer)
	 * @param x X screen coordinate (origin is in the center of the screen)
	 * @param y Y screen coordinate (origin is in the center of the screen)
	 * @param z Z homogeneous coordinate for Z buffering
	 * @param c Color of the pixel
	 */
	protected void drawMap(int x, int y, float z) {

		// Drawing the map means only updating zBuffer of this pixel to the new z
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
	 * @param sl the ShadowingLight to be used for calculation
	 * @return the resulting color from Directional light
	 */
	protected Color computeShadedColor(Color baseCol, Vector4 point, Vector3 normal, boolean rectoVerso, ShadowingLight sl) { // Should evolve to get the coordinates of the Vertex or surface for light type that depends on the location

		// Table of colors to be mixed
		Color c; // resulting color

		// Initialize to DARK
		c = DARK_SHADING_COLOR;

		// Shadowing light (Point or Directional Lights)
		if (lighting != null && lighting.hasShadowing() && sl != null) { // If lighting exists

			// Compute the dot product of this Light's vector at current point and the normal vector
			float dotNL = sl.getLightVectorAtPoint(point).dot(normal.normalize());
			if (rectoVerso) dotNL = Math.abs(dotNL);
			if (dotNL > 0) {
				c = ColorTools.multColors(baseCol, sl.getLightColor());
				// Multiply the color by the new dotNL
				c = ColorTools.multColor(c, dotNL);
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
	 * @param sl the ShadowingLight to be used for calculation
	 * @return the specular color
	 */
	protected Color computeSpecularColor(Vector3 normal, Vector3 viewer, Vector4 point, float e, Color sc, boolean rectoVerso, ShadowingLight sl) {

		Color c = DARK_SHADING_COLOR; // Specular reflection from Directional light
		Color spc = sc == null ? DEFAULT_SPECULAR_COLOR : sc;

		// Secondary Shading: Specular reflection (from Directional light)
		if (e>0) { // If e=0 this is considered as no specular reflection

			// Specular reflection algorithm for 1 Light
			// R: Reflection vector, L: Light vector, N: Normal vector on the surface. R+L=N+N => R = 2N-L
			// This has to be applied to each Light source (having a light vector so except Ambient)

			// Shadowing light (Point or Directional Lights)
			if (lighting != null && lighting.hasShadowing() && sl != null) {

				// Calculate reflection vector R = 2N-L and normalize it
				Vector3 lightVector = sl.getLightVectorAtPoint(point).normalize();
				float dotNL = lightVector.dot(normal.normalize());
				Vector3 r = (normal.times(2*dotNL)).minus(sl.getLightVectorAtPoint(point)); 

				float dotRV = r.dot(viewer);
				if (rectoVerso)
					dotRV = Math.abs(dotRV);
				else
					if (dotRV <0) dotRV = 0; // Clamped to 0 if negative

				if (dotNL > 0 && dotRV >0) {
					float specular = (float) Math.pow(dotRV, e);
					float intensity = sl.getIntensity(point);
					c = ColorTools.multColor(spc, specular*intensity);
				}
			}
		}

		return c;
	}

	public String renderStats() {		
		return "Rasterizer - Triangles: rendered: "+rendered_triangles+", rendered with lines: "+triangles_with_lines+", rendered with pixels: "+triangles_with_pixels;

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