package com.aventura.model.light;

import com.aventura.context.GraphicContext;
import com.aventura.engine.ModelView;
import com.aventura.math.perspective.Perspective;
import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Element;
import com.aventura.model.world.triangle.Triangle;


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
 * ShadowingLight is a type of light that can generate Shadow by opposite of other type of Lights e.g. Ambientlight.
 * In this abstract class will be found all the necessary attributes and tools for Shadow generation as the Camera corresponding to the Light
 * the ModelView projection for this Light (should be Orthographic for a DirectionalLight), the view frustrum and the Shadow Map itself.
 *
 * @author Olivier BARRY
 * @since April 2022
 * 
 */
public abstract class ShadowingLight extends Light {
	
	public static final int DEFAULT_SHADOW_MAP_DIMENSION = 200;
	
	// Fields related to Shadow generation
	protected Camera camera_light;
	protected Perspective perspective_light;
	
	// ModelView matrix and vertices conversion tool for the calculation of the Shadow map
	protected ModelView modelView;

	// View Frustum
	protected Vector4[][] frustum;
	protected Vector4 frustumCenter;
	
	// Shadow map
	protected float[][] map; // TODO multiple maps if multiple lights (actually this is already a ShadowingLight attribute so if multiple light there will be multiple maps)
	// TODO to be replaced by MapView class ?
	
	public abstract void initShadowing(GraphicContext graphicContext, Camera camera_view, int map_size);
	
	public void calculateCameraLight(GraphicContext graphicContext, Camera camera_view, Vector3 lightDirection) {
		// Calculate the camera position so that if it has the direction of light, it is targeting the middle of the view frustrum
		
		// For this calculate the 8 points of the View frustum in World coordinates
		// - The 4 points of the near plane		
		// - The 4 points of the fare plane
		
		// To calculate the 8 vertices we need:
		// - The eye position
		Vector4 eye = camera_view.getEye();
		// - The eye-point of interest (camera direction) normalized vector
		Vector4 fwd = camera_view.getForward().normalize();
		
		// TODO can we move out the Perspective class from the graphic context  ?
		// In order to have something more generic and more consistent

		// - The distance to the near plane
		float near = graphicContext.getNear();
		// - The distance to the far plane
		float far = graphicContext.getFar();
		// - The up vector and side vectors
		Vector4 up = camera_view.getUp();
		Vector4 side = fwd.times(up).normalize();
		// - the half width and half eight of the near plane
		float half_eight_near = graphicContext.getHeight()/2;
		float half_width_near = graphicContext.getWidth()/2;
		
		// - the half width and half eight of the far plane
		// Calculate the width and height on far plane using Thales: knowing that width and height are defined on the near plane
		float half_height_far = half_eight_near * far/near; // height_far = height_near * far/near
		float half_width_far = half_width_near * far/near; // width_far = width_near * far/near
		
		// Calculate all 8 points, vertices of the View Frustum
		frustum = new Vector4[2][4];
		// TODO : later, this calculation could be done and points provided through methods in the "Frustum" class or any class
		// directly related to the view Frustum
		// P11 = Eye + cam_dir*near + up*half_height_near + side*half_width_near
		frustum[0][0] = eye.plus(fwd.times(near)).plus(up.times(half_eight_near)).plus(side.times(half_width_near));
		// P12 = Eye + cam_dir*near + up*half_height_near - side*half_width_near
		frustum[0][1] = eye.plus(fwd.times(near)).plus(up.times(half_eight_near)).minus(side.times(half_width_near));
		// P13 = Eye + cam_dir*near - up*half_height_near - side*half_width_near
		frustum[0][2] = eye.plus(fwd.times(near)).minus(up.times(half_eight_near)).minus(side.times(half_width_near));
		// P14 = Eye + cam_dir*near - up*half_height_near + side*half_width_near
		frustum[0][3] = eye.plus(fwd.times(near)).minus(up.times(half_eight_near)).plus(side.times(half_width_near));
		//
		// P21 = Eye + cam_dir*far + up*half_height_far + side*half_width_far
		frustum[1][0] = eye.plus(fwd.times(far)).plus(up.times(half_height_far)).plus(side.times(half_width_far));
		// P22 = Eye + cam_dir*far + up*half_height_far - side*half_width_far
		frustum[1][1] = eye.plus(fwd.times(far)).plus(up.times(half_height_far)).minus(side.times(half_width_far));
		// P23 = Eye + cam_dir*far - up*half_height_far - side*half_width_far
		frustum[1][2] = eye.plus(fwd.times(far)).minus(up.times(half_height_far)).minus(side.times(half_width_far));
		// P24 = Eye + cam_dir*far - up*half_height_far + side*half_width_far
		frustum[1][3] = eye.plus(fwd.times(far)).minus(up.times(half_height_far)).plus(side.times(half_width_far));
		
		// Then the center of this Frustrum is (P11+P12+P13+P14 + P21+P22+P23+P24)/8
		// We take it as PoI for the Camera light
		Vector4 light_PoI = (frustum[0][0].plus(frustum[0][1]).plus(frustum[0][2]).plus(frustum[0][3]).plus(frustum[1][0]).plus(frustum[1][1]).plus(frustum[1][2]).plus(frustum[1][3])).times(1/8);
		
		//Vector4 light_dir = lighting.getDirectionalLight().getLightVector(null).V4();
		Vector4 light_dir = lightDirection.V4();
		
		// Build Camera light
		// We need to calculate the camera light Eye
		// In order to calculate the camera light "eye", we start form the PoI : the centre of the view frustum obtained before.
		// We then go back to the direction of light an amount equal to the distance between the near and far z planes of the view frustum.
		// Information found at: https://lwjglgamedev.gitbooks.io/3d-game-development-with-lwjgl/content/chapter26/chapter26.html
		Vector4 light_eye = light_PoI.minus(light_dir.times(far-near));
		
		// Define camera and LookAt matrix using light eye and PoI defined as center of the view frustum and up vector of camera view
		camera_light = new Camera(light_eye, light_PoI, up);

	}
	
	/**
	 * This method will generate the shadow map for the elements of the world passed in parameter with the camera light previously
	 * initiated and light matrix calculated.
	 * It will use similar recursive algorithm than RenderEngine algorithm for rendering world but will only calculate a shadow map without
	 * any more rendering or rasterization calculation.
	 * @param world
	 */
	public void generateShadowMap(World world) {
	
		// For each element of the world
		for (int i=0; i<world.getElements().size(); i++) {			
			Element e = world.getElement(i);
			generateShadowMap(e, null); // First model Matrix is the IDENTITY Matrix (to allow recursive calls)
		}
	}

	protected void generateShadowMap(Element e, Matrix4 matrix) {
		
		// Update ModelView matrix for this Element (Element <-> Model) by combining the one from this Element
		// with the previous one for recursive calls (initialized to IDENTITY at first call)
		Matrix4 model = null;
		if (matrix == null) {
			model = e.getTransformation();			
		} else {
			model = matrix.times(e.getTransformation());
		}
		modelView.setModelWithoutNormals(model);
		modelView.computeTransformation(); // Compute the whole ModelView modelView matrix including Camera (view)

		// Calculate projection for all vertices of this Element
		modelView.transformVerticesWithoutNormals(e); // Calculate prj_pos of each vertex
		// TODO Verify that modelView.transformVertices does not calculate normals (not needed here) projection
				
		// Process each Triangle
		for (int j=0; j<e.getTriangles().size(); j++) {
			Triangle t = e.getTriangle(j);
			// Scissor test for the triangle
			// If triangle is totally or partially in the View Frustum
			// Then shadowmap this triangle
			if (t.isInViewFrustum()) {
				
				// TBD
				// Use Rasterizer with a dedicated algorithm for Shadow Map generation (simpler than full rasterization) but trying to reuse the
				// common parts.
				// "Strategy" design pattern could be used with a generic interface implementing different rasterization strategies (to be passed
				// in parameter of the generic method) and still keeping only one RasterizeTriangle method. E.g. :
				//     rasterizer.rasterizeTriangle(triangle, rasterizationStrategy);
				
			}
		}
	
		// Do a recursive call for SubElements
		if (!e.isLeaf()) {
			for (int i=0; i<e.getSubElements().size(); i++) {
				// Recursive call
				generateShadowMap(e.getSubElements().get(i), model);
			}
		}
	}

	public ModelView getModelView() {
		return modelView;
	}
	
	public float getMap(int x, int y) {
		return map[x][y];
	}

}
