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
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.AmbientLight;
import com.aventura.model.light.DirectionalLight;
import com.aventura.model.light.Lighting;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.Element;
import com.aventura.model.world.World;
import com.aventura.model.world.triangle.FanMesh;
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
 * Test FanMesh class
 * 
 * Create FanMesh with 1 single triangle and apply texture
 * Check that texture is correctly wrapped:
 * - evenly distributed from summit to base
 * - concentrated on the tip and full width at the bottom
 * 
 * @author Olivier Barry
 *
 */

public class TestFanMesh1 {
	
	// GUIView to be displayed
	private SwingView view;

	public GUIView createView(PerspectiveContext context) {

		// Create the frame of the application 
		JFrame frame = new JFrame("Test FanMesh 1");
		// Set the size of the frame
		frame.setSize(1000,600);
		
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

//		Tracer.info = true;
//		Tracer.function = true;

		// Camera
		//Vector4 eye = new Vector4(4,2,3,1);
		Vector4 eye = new Vector4(0,0,5,1);
		Vector4 poi = new Vector4(0,0,0,1);
		//Camera camera = new Camera(eye, poi, Vector4.Z_AXIS);		
		Camera camera = new Camera(eye, poi, Vector4.Y_AXIS);		
				
		TestFanMesh1 test = new TestFanMesh1();
		
		System.out.println("********* Creating World");
		
		Texture tex = new Texture("resources/texture/texture_bricks_204x204.jpg");
		//Texture tex = new Texture("resources/texture/texture_blueground_204x204.jpg");
		//Texture tex = new Texture("resources/texture/texture_woodfloor_160x160.jpg");
		//Texture tex = new Texture("resources/texture/texture_damier_600x591.gif");
		//Texture tex = new Texture("resources/texture/texture_grass_900x600.jpg");
		//Texture tex = new Texture("resources/texture/texture_ground_stone_600x600.jpg");
		//Texture tex = new Texture("resources/texture/texture_snow_590x590.jpg");
		
		// Create World
		World world = new World();
		Element e = new Element();
		
		//
		// Create a Triangle
		//
		//         ^ Y
		//  V2  |\ |
		//      |  |
		//      |  | \
		//      |  |   \ V1
		//  ----+--+----+------> X
		//      |  |   /
		//      |    /
		//      |  /
		//  V3  |/
		//      
		
		float c = (float)Math.cos(2*(Math.PI)/3); // -0.5
		float s = (float)Math.sin(2*(Math.PI)/3); // 0.866
		
		Vector4 vec1 = new Vector4(1,0,0,1);
		Vector4 vec2 = new Vector4(c,s,0,1);
		Vector4 vec3 = new Vector4(c,-s,0,1);
				
		FanMesh fan = new FanMesh(e,1, tex); // 1 triangle
		fan.setSummit(vec1);
		fan.getVertex(0).setPos(vec2);
		fan.getVertex(1).setPos(vec3);
		fan.createTriangles(FanMesh.MESH_ORIENTED_TRIANGLES);
				
		world.addElement(e);
		world.setBackgroundColor(new Color(110,0,220));
		
		System.out.println("********* Calculating normals");
		world.generate();
		
		DirectionalLight dl = new DirectionalLight(new Vector3(-1,-1,-1), 0.5f);
		AmbientLight al = new AmbientLight(0.5f);
		Lighting light = new Lighting(dl, al);
		
		PerspectiveContext pContext = new PerspectiveContext(0.8f, 0.45f, 1, 100, PerspectiveContext.PERSPECTIVE_TYPE_FRUSTUM, 1250);
		GUIView gUIView = test.createView(pContext);

		//RenderContext rContext = new RenderContext(RenderContext.RENDER_DEFAULT_ALL_ENABLED);
		RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE);
		rContext.setTextureProcessing(RenderContext.TEXTURE_PROCESSING_ENABLED);
		//rContext.setDisplayNormals(RenderContext.DISPLAY_NORMALS_ENABLED);
		//rContext.setDisplayLandmark(RenderContext.DISPLAY_LANDMARK_ENABLED);

		//rContext.setRendering(RenderContext.RENDERING_TYPE_INTERPOLATE);
		
		RenderEngine renderer = new RenderEngine(world, light, camera, rContext, pContext);
		renderer.setView(gUIView);
		renderer.render();


		System.out.println("********* Rendering...");
		int nb_images = 180;
		for (int i=0; i<=3*nb_images; i++) {
		//for (int i=0; i<=3; i++) {
			Rotation r = new Rotation((float)Math.PI*2*(float)i/(float)nb_images, Vector3.Z_AXIS);
			e.setTransformation(r);
			renderer.render();
		}

		System.out.println("********* ENDING APPLICATION *********");
	}
}
