package com.aventura.model.shadow;

import com.aventura.context.GraphicContext;
import com.aventura.engine.ModelView;
import com.aventura.math.perspective.Orthographic;
import com.aventura.math.perspective.Perspective;
import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.camera.LookAt;
import com.aventura.model.light.Lighting;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Element;
import com.aventura.model.world.triangle.Triangle;
import com.aventura.tools.tracing.Tracer;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2019 Olivier BARRY
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
 * In parallel of Lighting class, the Shadowing class aims at providing the services to generate Shadows
 * More specifically and close to the Rasterizer class, Shodowing class aims at calculating a Shadow map (in future: several shadow maps), contained in this class.
 * 
 * Future steps / possibilities :
 * - If applicable, homogeneize classes Lighting and Shadowing to perform similar tasks (map generation -> not the case for Lighting as Rasterizer class has the role)
 * - Try to generalize the work to be done between Lighting and Shadowing (e.g. map generation), create common class / ancestor and use inheritance for specialization
 *
 * @author Olivier BARRY
 * @since July 2019
 * 
 */

public class Shadowing {
	
	protected GraphicContext graphicContext; // to get far, near, width, eight distances
	protected Camera camera_view; // to calculate the center of the view frustum using the far, near, width, eight distances
	protected Lighting lighting; // A reference to the lighting element of the RenderEngine
	//protected Vector4 up = Vector4.Y_AXIS; // Default is Y axis (as it is for default Camera up vector)
	
	// Future evolution : multiple directional light would mean multiple cameras
	protected Camera camera_light;
	protected Perspective perspective_light;
	
	// ModelView matrix and vertices conversion tool for the calculation of the Shadow map
	protected ModelView modelView;
	
	// Shadow map
	protected float[][] map;
	
	// View Frustum
	protected Vector4 frustumCenter;
	
	
	/**
	 * Constructor with lighting reference
	 * @param lighting
	 */
	public Shadowing(GraphicContext graphicContext, Lighting lighting, Camera cam) {
		this.graphicContext = graphicContext;
		this.camera_view = cam;
		this.lighting = lighting;
	}
	
	/**
	 * Set lighting manually if not passed in the Constructor
	 * @param lighting
	 */
	public void setLighting(Lighting lighting) {
		this.lighting = lighting;
	}
	
	/**
	 * Initialize the Shading object by calculating the projection matrix(ces) for the light source(s)
	 * 
	 * As of first implementation, only (1 single) directional light will be used for shading
	 */
	public void initShading() {
		
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
		Vector4[][] P = new Vector4[2][4];
		// TODO : later, this calculation could be done and points provided through methods in the "Frustum" class or any class
		// directly related to the view Frustum
		// P11 = Eye + cam_dir*near + up*half_height_near + side*half_width_near
		P[0][0] = eye.plus(fwd.times(near)).plus(up.times(half_eight_near)).plus(side.times(half_width_near));
		// P12 = Eye + cam_dir*near + up*half_height_near - side*half_width_near
		P[0][1] = eye.plus(fwd.times(near)).plus(up.times(half_eight_near)).minus(side.times(half_width_near));
		// P13 = Eye + cam_dir*near - up*half_height_near - side*half_width_near
		P[0][2] = eye.plus(fwd.times(near)).minus(up.times(half_eight_near)).minus(side.times(half_width_near));
		// P14 = Eye + cam_dir*near - up*half_height_near + side*half_width_near
		P[0][3] = eye.plus(fwd.times(near)).minus(up.times(half_eight_near)).plus(side.times(half_width_near));
		//
		// P21 = Eye + cam_dir*far + up*half_height_far + side*half_width_far
		P[1][0] = eye.plus(fwd.times(far)).plus(up.times(half_height_far)).plus(side.times(half_width_far));
		// P22 = Eye + cam_dir*far + up*half_height_far - side*half_width_far
		P[1][1] = eye.plus(fwd.times(far)).plus(up.times(half_height_far)).minus(side.times(half_width_far));
		// P23 = Eye + cam_dir*far - up*half_height_far - side*half_width_far
		P[1][2] = eye.plus(fwd.times(far)).minus(up.times(half_height_far)).minus(side.times(half_width_far));
		// P24 = Eye + cam_dir*far - up*half_height_far + side*half_width_far
		P[1][3] = eye.plus(fwd.times(far)).minus(up.times(half_height_far)).plus(side.times(half_width_far));
		
		// Then the center of this Frustrum is (P11+P12+P13+P14 + P21+P22+P23+P24)/8
		// We take it as PoI for the Camera light
		Vector4 light_PoI = (P[0][0].plus(P[0][1]).plus(P[0][2]).plus(P[0][3]).plus(P[1][0]).plus(P[1][1]).plus(P[1][2]).plus(P[1][3])).times(1/8);
		
		Vector4 light_dir = lighting.getDirectionalLight().getLightVector(null).V4();
		
		// Build Camera light
		// We need to calculate the camera light Eye
		// In order to calculate the camera light "eye", we start form the PoI : the centre of the view frustum obtained before.
		// We then go back to the direction of light an amount equal to the distance between the near and far z planes of the view frustum.
		// Information found at: https://lwjglgamedev.gitbooks.io/3d-game-development-with-lwjgl/content/chapter26/chapter26.html
		Vector4 light_eye = light_PoI.minus(light_dir.times(far-near));
		
		// Define camera and LookAt matrix using light eye and PoI defined as center of the view frustum and up vector of camera view
		camera_light = new Camera(light_eye, light_PoI, up);
				
		/*
		 * Mat4 viewMatrix = LookAt(lighting.mCameraPosition,
		 * 							lighting.mCameraPosition + glm::normalize(directionalLight.mLightDirection),
		 * 							Vec3(0.0f, 1.0f, 0.0f));
		 * 
		 * Mat4 lightVP = CreateOrthographicMatrix(lighting.mCameraPosition.x - 25.0f,
		 * 											lighting.mCameraPosition.x + 25.0f,
		 * 											lighting.mCameraPosition.y - 25.0f,
		 * 											lighting.mCameraPosition.y + 25.0f,
		 * 											lighting.mCameraPosition.z + 25.0f,
		 * 											lighting.mCameraPosition.z - 25.0f) * viewMatrix;
		 */

		// Define the bounding box for the light camera
		// For this let's transform the 8 vertices of the view frustrum in light coordinates

		// Calculate the left, right, bottom, top, near, far distances depending on the View's frustrum planes in the Light's coordinates
		// For this let's use the corner's of the View Frustum and transform them into the Light camera coordinates using the camera_light
		// matrix
		Vector4 [][] Q = new Vector4[2][4]; // The transformed frustum vertices in light coordinates
		// And take the min and max in each dimension of these vertices in light coordinates
		float maxX = 0, maxY = 0, maxZ = 0;
		float minX = 0, minY = 0, minZ = 0;
		for (int i=0; i<2; i++) {
			for (int j= 0; j<4; j++) {
				Q[i][j] = camera_light.getMatrix().times(P[i][j]);
				// Find the max and min X and Y of all the points in light coordinates -> this will become the right, left, top, bottom
				// of projection matrix
				// Find the max and min Z -> This will define the near and far of projection matrix
				maxX = maxX > Q[i][j].getX() ? maxX : Q[i][j].getX();
				maxY = maxY > Q[i][j].getY() ? maxY : Q[i][j].getY();
				maxZ = maxZ > Q[i][j].getZ() ? maxZ : Q[i][j].getZ();
				minX = minX < Q[i][j].getX() ? minX : Q[i][j].getX();
				minY = minY < Q[i][j].getY() ? minY : Q[i][j].getY();
				minZ = minZ < Q[i][j].getZ() ? minZ : Q[i][j].getZ();
				// TODO Note that another possibility for X and Y is to calculate only their absolute max and use it as half width and half height
			}
		}
		
		/*
		 * From: https://community.khronos.org/t/directional-light-and-shadow-mapping-view-projection-matrices/71386
		 * 
		 Think of light’s orthographic frustum as a bounding box that encloses all objects visible by the camera,
		 plus objects not visible but potentially casting shadows. For the simplicity let’s disregard the latter.
		 
		 So to find this frustum:
		 - find all objects that are inside the current camera frustum
		 - find minimal aa bounding box that encloses them all
		 - transform corners of that bounding box to the light’s space (using light’s view matrix)
		 - find aa bounding box in light’s space of the transformed (now obb) bounding box
		 - this aa bounding box is your directional light’s orthographic frustum.
		 
		 Note that actual translation component in light view matrix doesn’t really matter as you’ll only get different Z values
		 for the frustum but the boundaries will be the same in world space. For the convenience, when building light view matrix,
		 you can assume the light “position” is at the center of the bounding box enclosing all visible objects.
		 */
		
		// At last initialize the Orthographic projection
		// Orthographic(float left, float right, float bottom, float top, float near, float far)
		perspective_light = new Orthographic(minX, maxX, minY, maxY, minZ, maxZ);

		// Create the orthographic projection matrix
		//modelview = new ModelView(camera_light.getMatrix(), projection);
		
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
		modelView.setModel(model);
		modelView.computeTransformation(); // Compute the whole ModelView modelView matrix including Camera (view)

		// Calculate projection for all vertices of this Element
		modelView.transformVertices(e); // Calculate prj_pos of each vertex
		// TODO Verify that modelView.transformVertices does not calculate normals (not needed here) projection
				
		// Process each Triangle
		for (int j=0; j<e.getTriangles().size(); j++) {
			Triangle t = e.getTriangle(j);
			// Scissor test for the triangle
			// If triangle is totally or partially in the View Frustum
			// Then shadowmap this triangle
			if (t.isInViewFrustum()) {
				
				// TBD
				
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
	
}
