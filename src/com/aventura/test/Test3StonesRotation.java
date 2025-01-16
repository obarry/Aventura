package com.aventura.test;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import com.aventura.context.GraphicContext;
import com.aventura.context.RenderContext;
import com.aventura.engine.RenderEngine;
import com.aventura.math.transform.Rotation;
import com.aventura.math.transform.Scaling;
import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.AmbientLight;
import com.aventura.model.light.DirectionalLight;
import com.aventura.model.light.Lighting;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Sphere;
import com.aventura.view.SwingView;
import com.aventura.view.GUIView;

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
 * This class is a Test class demonstrating usage of the API of the Aventura rendering engine 
 */

public class Test3StonesRotation {
	
	// GUIView to be displayed
	private SwingView view;

	public GUIView createView(GraphicContext context) {

		// Create the frame of the application 
		JFrame frame = new JFrame("Test 3 Stones Rotation");
		// Set the size of the frame
		frame.setSize(1500,900);
		
		// Create the gUIView to be displayed
		view = new SwingView(context, frame);
		
		// Create a panel and add it to the frame
		JPanel panel = new JPanel() {
			
		    public void paintComponent(Graphics graph) {
		    	graph.drawImage(view.getImageView(), 0, 0, null);
		    }
		};
		frame.getContentPane().add(panel);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
 
		// Locate application frame in the center of the screen
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setLocation(dim.width/2 - frame.getWidth()/2, dim.height/2 - frame.getHeight()/2);
		
		// Render the frame on the display
		frame.setVisible(true);
		
		return view;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		System.out.println("********* STARTING APPLICATION *********");

		// Camera
		Vector4 eye = new Vector4(-6,-12,8,1);
		Vector4 poi = new Vector4(0,0,0,1);
		Camera camera = new Camera(eye, poi, Vector4.Z_AXIS);		
				
		Test3StonesRotation test = new Test3StonesRotation();
		
		System.out.println("********* Creating World");
		
		Texture tex = new Texture("resources/texture/texture_stone_1023x852.jpg");
		
		// Create World
		World world = new World();
		
		// Create 3 boxes with texture
		Sphere elm1 = new Sphere(1,20, tex);
		Sphere elm2 = new Sphere(1,20, tex);
		Sphere elm3 = new Sphere(1,20, tex);
		
		// Scaling for stones not round
		Scaling s = new Scaling(1,0.6f,0.3f);
//		elm1.setTransformation(s);
//		elm2.setTransformation(s);
//		elm3.setTransformation(s);
		
		// Set some colors on some faces of the boxes
		elm2.setColor(Color.ORANGE);
		elm3.setColor(Color.BLUE);
		
		// Add boxes to the World
		world.addElement(elm1);
		world.addElement(elm2);
		world.addElement(elm3);
		world.setBackgroundColor(new Color(20,10,5));
		
		// Generate the world geometry (including vertices and triangles) based on initialization data
		System.out.println("********* Calculating normals");
		world.generate();
		
		// Print the world and each element's characteristics
		System.out.println(world);
		System.out.println(elm1);
		System.out.println(elm2);
		System.out.println(elm3);
		
		// Create some lighting
		DirectionalLight dl = new DirectionalLight(new Vector3(0.5f,0,-1f), 0.8f);
		AmbientLight al = new AmbientLight(0.2f);
		Lighting light = new Lighting(dl, al, false);
		
		GraphicContext gContext = new GraphicContext(0.8f, 0.45f, 1, 100, GraphicContext.PERSPECTIVE_TYPE_FRUSTUM, 1250+625);
		GUIView gUIView = test.createView(gContext);

		//RenderContext rContext = new RenderContext(RenderContext.RENDER_DEFAULT);
		RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE);
		//RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_PLAIN);
		//RenderContext rContext = new RenderContext(RenderContext.RENDER_DEFAULT);
		rContext.setTextureProcessing(RenderContext.TEXTURE_PROCESSING_ENABLED);
		//rContext.setDisplayNormals(RenderContext.DISPLAY_NORMALS_ENABLED);
		//rContext.setDisplayLandmark(RenderContext.DISPLAY_LANDMARK_ENABLED);
		//rContext.setRenderingLines(RenderContext.RENDERING_LINES_ENABLED);
		
		RenderEngine renderer = new RenderEngine(world, light, camera, rContext, gContext);
		renderer.setView(gUIView);
		//renderer.render();

		System.out.println("********* Rendering...");
		int nb_images = 180;
		float alpha = (float)Math.PI*2/(float)nb_images;
		float beta = (float)Math.PI*2/(float)nb_images*2;
		float gamma = (float)Math.PI*2/(float)nb_images/3;
		float delta = (float)Math.PI*2/(float)nb_images;
		float offset1 = (float)Math.PI*2/3;
		float offset2 = 2*offset1;
		float distance = 1.5f;
		for (int i=0; i<=3*nb_images; i++) {
			Rotation rx1 = new Rotation(alpha*i, Vector3.X_AXIS);
			Rotation ry1 = new Rotation(beta*i, Vector3.Y_AXIS);
			Rotation rz1 = new Rotation(gamma*i, Vector3.Z_AXIS);
			Rotation rx2 = new Rotation(alpha*i+offset1, Vector3.X_AXIS);
			Rotation ry2 = new Rotation(beta*i+offset1, Vector3.Y_AXIS);
			Rotation rz2 = new Rotation(gamma*i+offset1, Vector3.Z_AXIS);
			Rotation rx3 = new Rotation(alpha*i+offset2, Vector3.X_AXIS);
			Rotation ry3 = new Rotation(beta*i+offset2, Vector3.Y_AXIS);
			Rotation rz3 = new Rotation(gamma*i+offset2, Vector3.Z_AXIS);
			elm1.setTransformation(rx1.times(ry1).times(rz1).times(s));
			elm2.setTransformation(rx2.times(ry2).times(rz2).times(s));
			elm3.setTransformation(rx3.times(ry3).times(rz3).times(s));
			Vector4 orbit1 = new Vector4(Vector4.X_AXIS);
			Vector4 orbit2 = new Vector4(Vector4.X_AXIS);
			Vector4 orbit3 = new Vector4(Vector4.X_AXIS);
			orbit1.timesEquals(distance);
			orbit2.timesEquals(distance);
			orbit3.timesEquals(distance);
			Rotation ro1 = new Rotation(delta*i, Vector3.Z_AXIS);
			Rotation ro2 = new Rotation(delta*i+offset1, Vector3.Z_AXIS);
			Rotation ro3 = new Rotation(delta*i+offset2, Vector3.Z_AXIS);
			orbit1.timesEquals(ro1);
			orbit2.timesEquals(ro2);
			orbit3.timesEquals(ro3);
			Translation to1 = new Translation(orbit1);
			Translation to2 = new Translation(orbit2);
			Translation to3 = new Translation(orbit3);
			elm1.combineTransformation(to1);
			elm2.combineTransformation(to2);
			elm3.combineTransformation(to3);
			renderer.render();
		}

		System.out.println("********* ENDING APPLICATION *********");
	}
}
