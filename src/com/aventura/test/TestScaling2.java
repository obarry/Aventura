package com.aventura.test;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import com.aventura.context.PerspectiveContext;
import com.aventura.context.RenderContext;
import com.aventura.engine.RenderEngine;
import com.aventura.math.transform.Rotation;
import com.aventura.math.transform.Scaling;
import com.aventura.math.transform.Transformation;
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
 * Copyright (c) 2016-2026 Olivier BARRY
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

public class TestScaling2 {
	
	// GUIView to be displayed
	private SwingView view;

	public GUIView createView(PerspectiveContext context) {

		// Create the frame of the application 
		JFrame frame = new JFrame("Test Scaling");
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
		//Vector4 eye = new Vector4(-4,-8,5,1);
		//Vector4 eye = new Vector4(16,6,12,1);
		Vector4 eye = new Vector4(6,12,16,1);
		//Vector4 eye = new Vector4(3,2,2,1);
		Vector4 poi = new Vector4(0,0,0,1);
		Camera camera = new Camera(eye, poi, Vector4.Z_AXIS);		
				
		TestScaling2 test = new TestScaling2();
		
		System.out.println("********* Creating World");
		
		//Texture tex = new Texture("resources/texture/texture_bricks_204x204.jpg");
		//Texture tex = new Texture("resources/texture/texture_blueground_204x204.jpg");
		//Texture tex = new Texture("resources/texture/texture_woodfloor_160x160.jpg");
		//Texture tex = new Texture("resources/texture/texture_damier_600x591.gif");
		//Texture tex = new Texture("resources/texture/texture_grass_900x600.jpg");
		//Texture tex = new Texture("resources/texture/texture_ground_stone_600x600.jpg");
		//Texture tex = new Texture("resources/texture/texture_snow_590x590.jpg");
		//Texture tex = new Texture("resources/texture/texture_metal_mesh_463x463.jpg");
		//Texture tex = new Texture("resources/texture/texture_old_leather_box_800x610.jpg");
		//Texture tex = new Texture("resources/texture/texture_metal_plate_626x626.jpg");
		//Texture tex = new Texture("resources/texture/texture_stone1_1700x1133.jpg");
		//Texture tex = new Texture("resources/texture/texture_rock_stone_400x450.jpg");
		Texture tex = new Texture("resources/texture/texture_barnwood_576x358.jpg");
		
		// Create World
		World world = new World();
		
		//Box elm = new Box(3,2,1.5f, tex);
		//Sphere elm = new Sphere(2f,32, tex);
		Sphere elm = new Sphere(0.7f,32, tex);
		//elm.setColor(new Color(100,200,255));
		elm.setSpecularExp(8);
		world.addElement(elm);
		world.setBackgroundColor(new Color(20,10,5));
		
		System.out.println("********* Generating World");		
		world.build();
		System.out.println(world);
		System.out.println(elm);
		
		//DirectionalLight dl = new DirectionalLight(new Vector3(-0.5f,0,1f), 0.8f);
		DirectionalLight dl = new DirectionalLight(new Vector3(0.5f,0,-1f), 0.8f);
		AmbientLight al = new AmbientLight(0.2f);
		Lighting light = new Lighting(dl, al, true);
		
		PerspectiveContext pContext = new PerspectiveContext(0.8f, 0.45f, 1, 1000, PerspectiveContext.PERSPECTIVE_TYPE_FRUSTUM, 1250+625);
		GUIView gUIView = test.createView(pContext);

		//RenderContext rContext = new RenderContext(RenderContext.RENDER_DEFAULT);
		RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE);
		rContext.setTextureProcessing(RenderContext.TEXTURE_PROCESSING_ENABLED);
		//rContext.setDisplayNormals(RenderContext.DISPLAY_NORMALS_ENABLED);
		rContext.setDisplayLandmark(RenderContext.DISPLAY_LANDMARK_ENABLED);
		rContext.setDisplayLight(RenderContext.DISPLAY_LIGHT_VECTORS_ENABLED);

		RenderEngine renderer = new RenderEngine(world, light, camera, rContext, pContext);
		renderer.setView(gUIView);
		renderer.render();

		System.out.println("********* Rendering...");
		int nb_images = 90;
		//Scaling s = new Scaling(2,2,1);
		for (int i=0; i<=3*nb_images; i++) {
			Rotation r1 = new Rotation((float)Math.PI*2*(float)i/(float)nb_images, Vector3.X_AXIS);
			Rotation r2 = new Rotation((float)Math.PI*2*(float)i/(float)nb_images, Vector3.Z_AXIS);
			Scaling s = new Scaling(1+(float)i/180,1+(float)i/180,1);
			elm.setTransformation(new Transformation(r1.times(r2.times(s))));
			//elm.setTransformation(r1.times(r2));
			//elm.combineTransformation(s);
			renderer.render();
		}

		System.out.println("********* ENDING APPLICATION *********");
	}
}
