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
import com.aventura.model.world.shape.Trellis;
import com.aventura.view.SwingView;
import com.aventura.view.GUIView;
import com.aventura.model.perspective.PerspectiveType;

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

public class Test3StonesRotationShadows {
	
	// GUIView to be displayed
	private SwingView view;

	public GUIView createView(PerspectiveContext context) {

		// Create the frame of the application 
		JFrame frame = new JFrame("Test 3 Stones Rotation with Shadows");
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
		Camera camera = new Camera(eye, poi, Vector4.zAxis());		
				
		Test3StonesRotationShadows test = new Test3StonesRotationShadows();
		
		System.out.println("********* Creating World");
		
		Texture tex = new Texture("resources/texture/texture_stone_1023x852.jpg");
		
		// Create World
		World world = new World();
		
		Trellis trellis = new Trellis(5, 5, 10, 10, tex);
		trellis.setColor(new Color(170, 170, 180));
		trellis.setTransformation(new Translation(new Vector3(0,0,-1.5f)));
		world.addElement(trellis);
		
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
		world.build();
		
		// Print the world and each element's characteristics
		System.out.println(world);
		System.out.println(elm1);
		System.out.println(elm2);
		System.out.println(elm3);
		
		// Create some lighting
		DirectionalLight dl = new DirectionalLight(new Vector3(0.5f,0,-1.0f), 0.8f);
		AmbientLight al = new AmbientLight(0.2f);
		Lighting light = new Lighting(dl, al, false);
		
		PerspectiveContext pContext = new PerspectiveContext(0.8f, 0.45f, 1, 100, PerspectiveType.FRUSTUM, 1250+625);
		GUIView gUIView = test.createView(pContext);

		//RenderContext rContext = new RenderContext(RenderContext.RENDER_DEFAULT);
		RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE);
		//RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_PLAIN);
		//RenderContext rContext = new RenderContext(RenderContext.RENDER_DEFAULT);
		rContext.setTextureProcessing(true);
		//rContext.setDisplayNormals(true);
		//rContext.setDisplayLandmark(true);
		//rContext.setRenderingLines(true);
		rContext.setShadowing(true);
		
		RenderEngine renderer = new RenderEngine(world, light, camera, rContext, pContext);
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
			Rotation rx1 = new Rotation(alpha*i, Vector3.xAxis());
			Rotation ry1 = new Rotation(beta*i, Vector3.yAxis());
			Rotation rz1 = new Rotation(gamma*i, Vector3.zAxis());
			Rotation rx2 = new Rotation(alpha*i+offset1, Vector3.xAxis());
			Rotation ry2 = new Rotation(beta*i+offset1, Vector3.yAxis());
			Rotation rz2 = new Rotation(gamma*i+offset1, Vector3.zAxis());
			Rotation rx3 = new Rotation(alpha*i+offset2, Vector3.xAxis());
			Rotation ry3 = new Rotation(beta*i+offset2, Vector3.yAxis());
			Rotation rz3 = new Rotation(gamma*i+offset2, Vector3.zAxis());
			elm1.setTransformation(new Transformation(rx1.times(ry1).times(rz1).times(s)));
			elm2.setTransformation(new Transformation(rx2.times(ry2).times(rz2).times(s)));
			elm3.setTransformation(new Transformation(rx3.times(ry3).times(rz3).times(s)));
			Vector4 orbit1 = new Vector4(Vector4.xAxis());
			Vector4 orbit2 = new Vector4(Vector4.xAxis());
			Vector4 orbit3 = new Vector4(Vector4.xAxis());
			orbit1.timesEquals(distance);
			orbit2.timesEquals(distance);
			orbit3.timesEquals(distance);
			Rotation ro1 = new Rotation(delta*i, Vector3.zAxis());
			Rotation ro2 = new Rotation(delta*i+offset1, Vector3.zAxis());
			Rotation ro3 = new Rotation(delta*i+offset2, Vector3.zAxis());
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
