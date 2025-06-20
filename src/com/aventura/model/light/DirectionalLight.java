package com.aventura.model.light;

import java.awt.Color;

import com.aventura.context.PerspectiveContext;
import com.aventura.engine.ModelViewProjection;
import com.aventura.engine.Rasterizer;
import com.aventura.math.projection.OrthographicProjection;
import com.aventura.math.tools.BoundingBox4;
import com.aventura.math.vector.GeometryTools;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.perspective.Perspective;
import com.aventura.model.world.World;
import com.aventura.tools.tracing.Tracer;

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
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating Directional Light. Direction : " + direction);
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
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating Directional Light. Direction : " + direction + " Intensity : " + intensity);
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
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating Directional Light. Direction : " + direction + " + World");
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
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "creating Directional Light. Direction : " + direction + " Intensity : " + intensity + " + World");
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
	public void initShadowing(Perspective perspectiveWorld, Camera camera_view, int map_size, World world) {
		this.world = world;
		initShadowing(perspectiveWorld, camera_view, map_size);
	}

	@Override
	public void initShadowing(Perspective perspectiveWorld, Camera camera_view, int map_size) {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "initShadowing");

		// map = new MapView(map_size, map_size);
		this.map_size = map_size;
		
		Vector4 [] box_world = null;
		
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "Creating Bounding Box. ShadowingBox type : " + toStringShadowingBoxType(this.shadowingBox_type));
		switch (this.shadowingBox_type) {
		
		case SHADOWING_BOX_VIEWFRUSTUM:
			// Define the bounding box for the light camera using the View Frustum (Camera of the scene)
			// For this let's use the 8 corner's of the View Frustum and transform them into the Light camera coordinates using the camera_light matrix
			Vector4[][] frustum = perspectiveWorld.getFrustumFromEye(camera_view);
			box_world = new Vector4[8]; // Create an array of Vectors that will contain all 4 vertices of the view Frustum projected in Light's coordinates
			// And take the min and max in each dimension of these vertices in light coordinates
			int k =0;
			for (int i=0; i<2; i++) {
				for (int j= 0; j<4; j++) {
					box_world[k] = camera_light.getMatrix().times(frustum[i][j]);
					k++;
				}
			}					
			break;
			
		case SHADOWING_BOX_WORLD:
			// Define the bounding box for the light camera
			box_world = new Vector4[8];
			float max = world.getMaxDistance();
			float min = -max;
			box_world[0] = new Vector4(min, min, min, 1);
			box_world[1] = new Vector4(max, min, min, 1);
			box_world[2] = new Vector4(max, max, min, 1);
			box_world[3] = new Vector4(min, max, min, 1);
			box_world[4] = new Vector4(min, min, max, 1);
			box_world[5] = new Vector4(max, min, max, 1);
			box_world[6] = new Vector4(max, max, max, 1);
			box_world[7] = new Vector4(min, max, max, 1);
			
			// For this create the min and max of the World (in World coordinates)	
//			box_world[0] = new Vector4(world.getMinX(), world.getMinY(), world.getMinZ(), 1);
//			box_world[1] = new Vector4(world.getMaxX(), world.getMinY(), world.getMinZ(), 1);
//			box_world[2] = new Vector4(world.getMaxX(), world.getMaxY(), world.getMinZ(), 1);
//			box_world[3] = new Vector4(world.getMinX(), world.getMaxY(), world.getMinZ(), 1);
//			box_world[4] = new Vector4(world.getMinX(), world.getMinY(), world.getMaxZ(), 1);
//			box_world[5] = new Vector4(world.getMaxX(), world.getMinY(), world.getMaxZ(), 1);
//			box_world[6] = new Vector4(world.getMaxX(), world.getMaxY(), world.getMaxZ(), 1);
//			box_world[7] = new Vector4(world.getMinX(), world.getMaxY(), world.getMaxZ(), 1);

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
		
		// Create the bounding box around the 8 vertices of the World "box" (in Light's coordinates)
		BoundingBox4 box = new BoundingBox4(box_world);

		if (Tracer.info) Tracer.traceInfo(this.getClass(), "Bounding Box : \n" + box);
	
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
		
		// Build Camera light
		// We need to calculate the camera light Eye
		// In order to calculate the camera light "eye", we start form the PoI : the centre of the gUIView frustum obtained before.
		// We then go back to the direction of light an amount equal to the distance between the near and far z planes of the gUIView frustum.
		// Information found at: https://lwjglgamedev.gitbooks.io/3d-game-development-with-lwjgl/content/chapter26/chapter26.html

		// Calculate the center of Frustum (geometrical center of the 8 points)
		Vector4 light_PoI = new Vector4(0,0,0,0); // FOR TESTING PURPOSE
		//Vector4 light_PoI = GeometryTools.center(perspectiveWorld.getFrustumFromEye(camera_view));		
		Vector4 light_dir = this.light_vector.times(-1).V4(); // Light direction is -light vector
		//Vector4 light_eye = light_PoI.minus(light_dir.times(perspectiveWorld.getFar()-perspectiveWorld.getNear()));
		Vector4 light_eye = light_PoI.minus(light_dir).times(3f); // FOR TESTING PURPOSE
		
		if (Tracer.info) Tracer.traceInfo(this.getClass(), "Calculate Camera Light: Ligth PoI: " + light_PoI + " Light direction: " + light_dir + " Light Eye: " + light_eye);

		// Define camera and LookAt matrix using light eye and PoI defined as center of the gUIView frustum and up vector of camera gUIView -> NO !!!!!
		
		// WARNING BUG MISTAKE ERROR ********************************************************************************************************************
		// TODO MISTAKE ON UP ASSUMPTION : IT CANNOT BE GUI CAMERA UP VECTOR BUT ANOTHER ONE TO BE DEFINED
		//camera_light = new Camera(light_eye, light_PoI, camera_view.getUp());
		
		camera_light = new Camera(light_eye, light_PoI, Vector4.Z_AXIS);
		
		//camera_light = new Camera(this.light_vector.times(-1).V4(), Vector4.Z_AXIS);
		// WARNING BUG MISTAKE ERROR ********************************************************************************************************************
		
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

		
		// At last initialize the Orthographic projection
		// Orthographic(float left, float right, float bottom, float top, float near, float far)
		
		// TODO PPU calculation is NOT DEFAULT_SHADOW_MAP_DIMENSION
		perspectiveCtx_light = new PerspectiveContext(box.getMaxY(), box.getMinY(), box.getMaxX(), box.getMinX(), box.getMaxZ()-box.getMinZ(), light_eye.length(), PerspectiveContext.PERSPECTIVE_TYPE_ORTHOGRAPHIC, DEFAULT_SHADOW_MAP_DIMENSION);
		//perspective_light = new Orthographic(box.getMinX(), box.getMaxX(), box.getMinY(), box.getMaxY(), box.getMinZ(), box.getMaxZ());
		
		rasterizer_light = new Rasterizer(camera_light, perspectiveCtx_light, null);

		// Create the MVP using this orthographic projection matrix
		mvp_light = new ModelViewProjection(camera_light.getMatrix(), perspectiveCtx_light.getPerspective().getProjection());
		
	}

	//public void calculateCameraLight(Perspective perspectiveWorld, Camera camera_view) {
	public void calculateCameraLight() {
		if (Tracer.function) Tracer.traceFunction(this.getClass(), "calculateCameraLight");
		
		// Build Camera light
		// We need to calculate the camera light Eye
		// In order to calculate the camera light "eye", we start form the PoI : the centre of the gUIView frustum obtained before.
		// We then go back to the direction of light an amount equal to the distance between the near and far z planes of the gUIView frustum.
		// Information found at: https://lwjglgamedev.gitbooks.io/3d-game-development-with-lwjgl/content/chapter26/chapter26.html

		// Calculate the center of Frustum (geometrical center of the 8 points)
		Vector4 light_PoI = new Vector4(0,0,0,0); // FOR TESTING PURPOSE
		//Vector4 light_PoI = GeometryTools.center(perspectiveWorld.getFrustumFromEye(camera_view));		
		Vector4 light_dir = this.light_vector.times(-1).V4(); // Light direction is -light vector
		//Vector4 light_eye = light_PoI.minus(light_dir.times(perspectiveWorld.getFar()-perspectiveWorld.getNear()));
		Vector4 light_eye = light_PoI.minus(light_dir).times(3f); // FOR TESTING PURPOSE
		
		// Define camera and LookAt matrix using light eye and PoI defined as center of the gUIView frustum and up vector of camera gUIView -> NO !!!!!
		
		// WARNING BUG MISTAKE ERROR ********************************************************************************************************************
		// TODO MISTAKE ON UP ASSUMPTION : IT CANNOT BE GUI CAMERA UP VECTOR BUT ANOTHER ONE TO BE DEFINED
		//camera_light = new Camera(light_eye, light_PoI, camera_view.getUp());
		camera_light = new Camera(light_eye, light_PoI, Vector4.Z_AXIS);
		//camera_light = new Camera(this.light_vector.times(-1).V4(), Vector4.Z_AXIS);
		// WARNING BUG MISTAKE ERROR ********************************************************************************************************************

	}
	
	public String toString() {
		return "Directional Light with light vector (= -direction of the light) : " + this.light_vector;
	}
}
