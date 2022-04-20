package com.aventura.model.light;

import java.awt.Color;

import com.aventura.context.GraphicContext;
import com.aventura.engine.ModelView;
import com.aventura.math.perspective.Orthographic;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2016-2021 Olivier BARRY
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
 * Directional Light also known as an infinite light source, radiates light in a single direction
 * from infinitely far away e.g. sun, whose rays can be considered parallel.
 * Since they have no position in space, directional directional have infinite range and the intensity
 * of light they radiate does not diminish over distance.
 *
 * @author Olivier BARRY
 * @since July 2016
 * 
 */

public class DirectionalLight extends ShadowingLight {
	
	protected Vector3 direction;
	protected Vector3 normalized_direction;
	
	public DirectionalLight(Vector3 direction) {
		super();
		this.direction = direction;
		this.normalized_direction = new Vector3(direction).normalize();
		this.intensity = direction.length();
	}
	
	public DirectionalLight(Vector3 direction, float intensity) {
		super();
		this.normalized_direction = new Vector3(direction).normalize();
		this.intensity = intensity;
		this.direction = this.normalized_direction.times(intensity);
	}

	@Override
	public Vector3 getLightVector(Vector4 point) {
		// Same direction vector in all world space by definition of Directional Light
		return this.direction;
	}
	
	@Override
	public Vector3 getLightNormalizedVector(Vector4 point) {
		// TODO Auto-generated method stub
		return this.normalized_direction;
	}

	@Override
	public float getIntensity(Vector4 point) {
		// Same intensity in any point of world space as Directional light have infinite range
		return intensity;
	}

	@Override
	public Color getLightColor(Vector4 point) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setLightVector(Vector3 light) {
		this.direction = light;
		this.normalized_direction = new Vector3(light).normalize();
		this.intensity = light.length();
	}

	@Override
	public void setLightNormalizedVector(Vector3 light) {
		// TODO Auto-generated method stub
		this.normalized_direction = light.normalize();
		this.direction = this.normalized_direction.times(intensity);
	}

	@Override
	public void setIntensity(float intensity) {
		this.intensity = intensity;
		this.direction = this.normalized_direction.times(intensity);
	}

	@Override
	public void setLightColor(Color c) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void initShadowing(GraphicContext graphicContext, Camera camera_view) {
		// TODO Auto-generated method stub
		
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
		
		//Vector4 light_dir = lighting.getDirectionalLight().getLightVector(null).V4();
		Vector4 light_dir = direction.V4();
		
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
		modelView = new ModelView(camera_light.getMatrix(), perspective_light);
		
	}

}
