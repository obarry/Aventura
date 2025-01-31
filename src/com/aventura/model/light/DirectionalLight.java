package com.aventura.model.light;

import java.awt.Color;

import com.aventura.engine.ModelViewProjection;
import com.aventura.math.projection.Orthographic;
import com.aventura.math.tools.BoundingBox4;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.perspective.Perspective;
import com.aventura.model.world.World;
import com.aventura.tools.tracing.Tracer;
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
	
	//protected Vector3 direction;
	protected Vector3 light_vector; // = -direction
	
	/**
	 * Create Directional Light using direction as vector of the light
	 * The intensity of the light will be extrapolate from the norm of the provided direction vector
	 * @param direction
	 */
	public DirectionalLight(Vector3 direction) {
		super(direction.length()); // Intensity is taken from the norm of the direction vector
		//this.direction = new Vector3(direction).normalize(); // direction vector is normalized
		this.light_vector = direction.times(-1).normalize(); // light vector (the opposite) is normalized
	}
	
	/**
	 * Create Directional Light using direction as vector of the light and separated scalar for intensity
	 * @param direction
	 * @param intensity
	 */
	public DirectionalLight(Vector3 direction, float intensity) {
		super(intensity);
		//this.direction = new Vector3(direction).normalize(); // direction vector is normalized
		this.light_vector = direction.times(-1).normalize(); // light vector (the opposite) is normalized
	}

	/**
	 * Create Directional Light using direction as vector of the light
	 * The intensity of the light will be extrapolate from the norm of the provided direction vector
	 * @param direction
	 */
	public DirectionalLight(Vector3 direction, World world) {
		super(direction.length(), world); // Intensity is taken from the norm of the direction vector
		//this.direction = new Vector3(direction).normalize(); // direction vector is normalized
		this.light_vector = direction.times(-1).normalize(); // light vector (the opposite) is normalized
	}
	
	/**
	 * Create Directional Light using direction as vector of the light and separated scalar for intensity
	 * @param direction
	 * @param intensity
	 */
	public DirectionalLight(Vector3 direction, float intensity, World world) {
		super(intensity, world);
		//this.direction = new Vector3(direction).normalize(); // direction vector is normalized
		this.light_vector = direction.times(-1).normalize(); // light vector (the opposite) is normalized
	}

	/**
	 * The returned vector is normalized
	 */
	@Override
	public Vector3 getLightVectorAtPoint(Vector4 point) {
		// Same direction vector in all world space by definition of Directional Light
		return this.light_vector;
	}
	
	@Override
	public float getIntensity(Vector4 point) {
		// Same intensity in any point of world space as Directional light have infinite range
		return intensity;
	}

	@Override
	public Color getLightColorAtPoint(Vector4 point) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setLightVector(Vector3 light) {
		this.intensity = light.length();
		this.light_vector = new Vector3(light).normalize();
		//this.direction = this.light_vector.times(-1);
	}

	@Override
	public void setIntensity(float intensity) {
		this.intensity = intensity;
	}
	
	@Override
	public void initShadowing(Perspective perspective, Camera camera_view, int map_size, World world) {
		this.world = world;
		initShadowing( perspective, camera_view, map_size);
	}

	@Override
	public void initShadowing(Perspective perspective, Camera camera_view, int map_size) {

		map = new MapView(map_size, map_size);
		
		calculateCameraLight(perspective, camera_view, light_vector.times(-1));
		
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
		BoundingBox4 box = null;
		switch (this.shadowingBox_type) {
		
		case SHADOWING_BOX_VIEWFRUSTUM:
			
			// Define the bounding box for the light camera using the View Frustum (Camera of the scene)
			// For this let's use the 8 corner's of the View Frustum and transform them into the Light camera coordinates using the camera_light matrix
			Vector4 [] frustumProj = new Vector4[8]; // Create an array of Vectors that will contain all 4 vertices of the view Frustum projected in Light's coordinates
			// And take the min and max in each dimension of these vertices in light coordinates
			int k =0;
			for (int i=0; i<2; i++) {
				for (int j= 0; j<4; j++) {
					frustumProj[k] = camera_light.getMatrix().times(frustum[i][j]);
					k++;
				}
			}
			
			// The create the bounding box around the 8 vertices of the view Frustum (in Light's coordinates)
			box = new BoundingBox4(frustumProj);
			

			break;
		case SHADOWING_BOX_WORLD:
			// Define the bounding box for the light camera
			// For this create the min and max of the World (in World coordinates)	
			Vector4 [] worldBox = new Vector4[8];
			worldBox[0] = new Vector4(world.getMinX(), world.getMinY(), world.getMinZ(), 1);
			worldBox[0] = new Vector4(world.getMaxX(), world.getMinY(), world.getMinZ(), 1);
			worldBox[0] = new Vector4(world.getMaxX(), world.getMaxY(), world.getMinZ(), 1);
			worldBox[0] = new Vector4(world.getMinX(), world.getMaxY(), world.getMinZ(), 1);
			worldBox[0] = new Vector4(world.getMinX(), world.getMinY(), world.getMaxZ(), 1);
			worldBox[0] = new Vector4(world.getMaxX(), world.getMinY(), world.getMaxZ(), 1);
			worldBox[0] = new Vector4(world.getMaxX(), world.getMaxY(), world.getMaxZ(), 1);
			worldBox[0] = new Vector4(world.getMinX(), world.getMaxY(), world.getMaxZ(), 1);
			
			// Then transform the Points into Light's coordinates using the camera_light matrix
			for (int i=0; i<8; i++) {
				worldBox[i] = camera_light.getMatrix().times(worldBox[i]);
			}
			
			// Create the bounding box around the 8 vertices of the World "box" (in Light's coordinates)
			box = new BoundingBox4(worldBox);
			break;
		case SHADOWING_BOX_ELEMENT: // Not implemented yet
			if (Tracer.error) Tracer.traceError(this.getClass(), "Not implemented yet: SHADOWING_BOX_ELEMENT");
			break;
		case SHADOWING_BOX_SPECIFIC: // Not implemented yet
			if (Tracer.error) Tracer.traceError(this.getClass(), "Not implemented yet: SHADOWING_BOX_SPECIFIC");
			break;
		default:
			if (Tracer.error) Tracer.traceError(this.getClass(), "Unknown Shadowing Box Type: " + this.shadowingBox_type);
			break;
		}
		
		/*
		 * From: https://community.khronos.org/t/directional-light-and-shadow-mapping-gUIView-projection-matrices/71386
		 * 
		 Think of light’s orthographic frustum as a bounding box that encloses all objects visible by the camera,
		 plus objects not visible but potentially casting shadows. For the simplicity let’s disregard the latter.
		 
		 So to find this frustum:
		 - find all objects that are inside the current camera frustum
		 - find minimal bounding box that encloses them all
		 - transform corners of that bounding box to the light’s space (using light’s gUIView matrix)
		 - find bounding box in light’s space of the transformed (now obb) bounding box
		 - this bounding box is your directional light’s orthographic frustum.
		 
		 Note that actual translation component in light gUIView matrix doesn’t really matter as you’ll only get different Z values
		 for the frustum but the boundaries will be the same in world space. For the convenience, when building light gUIView matrix,
		 you can assume the light “position” is at the center of the bounding box enclosing all visible objects.
		 */
		
		// At last initialize the Orthographic projection
		// Orthographic(float left, float right, float bottom, float top, float near, float far)
		perspective_light = new Orthographic(box.getMinX(), box.getMaxX(), box.getMinY(), box.getMaxY(), box.getMinZ(), box.getMaxZ());

		// Create the MVP using this orthographic projection matrix
		modelViewProjection = new ModelViewProjection(camera_light.getMatrix(), perspective_light);
		
	}

	public void calculateCameraLight(Perspective perspective, Camera camera_view, Vector3 lightDirection) {
		// Calculate the camera position so that if it has the direction of light, it is targeting the middle of the gUIView frustum
		
		// For this calculate the 8 points of the GUIView frustum in World coordinates
		// - The 4 points of the near plane		
		// - The 4 points of the fare plane
		
		// To calculate the 8 vertices we need:
		// - The eye position
		Vector4 eye = camera_view.getEye();
		// - The eye-point of interest (camera direction) normalized vector
		Vector4 fwd = camera_view.getForward().normalize();
		
		// TODO can we move out the Projection class from the graphic context  ?
		// In order to have something more generic and more consistent
		// TODO strange to get Near and Far from graphicContext and Up from Camera_view. Clean-up required ?

		// - The distance to the near plane
		float near = perspective.getNear();
		// - The distance to the far plane
		float far = perspective.getFar();
		// - The up vector and side vectors
		Vector4 up = camera_view.getUp();
		Vector4 side = fwd.times(up).normalize();
		// - the half width and half eight of the near plane
		float half_eight_near = perspective.getHeight()/2;
		float half_width_near = perspective.getWidth()/2;
		
		// - the half width and half eight of the far plane
		// Calculate the width and height on far plane using Thales: knowing that width and height are defined on the near plane
		float half_height_far = half_eight_near * far/near; // height_far = height_near * far/near
		float half_width_far = half_width_near * far/near; // width_far = width_near * far/near
		
		// TODO Calculating the gUIView frustum should be a method from the Perspective itself
		// Calculate all 8 points, vertices of the GUIView Frustum
		frustum = new Vector4[2][4];
		// TODO : later, this calculation could be done and points provided through methods in the "Frustum" class or any class
		// directly related to the gUIView Frustum
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
		// In order to calculate the camera light "eye", we start form the PoI : the centre of the gUIView frustum obtained before.
		// We then go back to the direction of light an amount equal to the distance between the near and far z planes of the gUIView frustum.
		// Information found at: https://lwjglgamedev.gitbooks.io/3d-game-development-with-lwjgl/content/chapter26/chapter26.html
		Vector4 light_eye = light_PoI.minus(light_dir.times(far-near));
		
		// Define camera and LookAt matrix using light eye and PoI defined as center of the gUIView frustum and up vector of camera gUIView
		camera_light = new Camera(light_eye, light_PoI, up);

	}
	
}
