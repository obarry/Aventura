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
		
		calculateCameraLight(graphicContext, camera_view, direction);
		
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
		Vector4 frustumProj = new Vector4(); // The transformed frustum vertex in light coordinates
		// And take the min and max in each dimension of these vertices in light coordinates
		float maxX = 0, maxY = 0, maxZ = 0;
		float minX = 0, minY = 0, minZ = 0;
		for (int i=0; i<2; i++) {
			for (int j= 0; j<4; j++) {
				frustumProj = camera_light.getMatrix().times(frustum[i][j]);
				// Find the max and min X and Y of all the points in light coordinates -> this will become the right, left, top, bottom
				// of projection matrix
				// Find the max and min Z -> This will define the near and far of projection matrix
				maxX = maxX > frustumProj.getX() ? maxX : frustumProj.getX();
				maxY = maxY > frustumProj.getY() ? maxY : frustumProj.getY();
				maxZ = maxZ > frustumProj.getZ() ? maxZ : frustumProj.getZ();
				minX = minX < frustumProj.getX() ? minX : frustumProj.getX();
				minY = minY < frustumProj.getY() ? minY : frustumProj.getY();
				minZ = minZ < frustumProj.getZ() ? minZ : frustumProj.getZ();
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
