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

public class TestSphereTexture {
	
	// GUIView to be displayed
	private SwingView view;

	public GUIView createView(GraphicContext context) {

		// Create the frame of the application 
		JFrame frame = new JFrame("Test Sphere Texture");
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

//		Tracer.info = true;
//		Tracer.function = true;

		// Camera
		//Vector4 eye = new Vector4(8,3,5,1);
		//Vector4 eye = new Vector4(16,6,12,1);
		Vector4 eye = new Vector4(5,4,4,1);
		//Vector4 eye = new Vector4(5,4,0,1);
		//Vector4 eye = new Vector4(4,4,4,1);
		Vector4 poi = new Vector4(0,0,0,1);
		Camera camera = new Camera(eye, poi, Vector4.Z_AXIS);		
				
		TestSphereTexture test = new TestSphereTexture();
		
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
		//Texture tex = new Texture("resources/texture/texture_sticker_cremedemarrons_351x201.jpg", Texture.TEXTURE_DIRECTION_VERTICAL, Texture.TEXTURE_ORIENTATION_NORMAL, Texture.TEXTURE_ORIENTATION_OPPOSITE);
		//Texture tex = new Texture("resources/texture/texture_rust_960x539.jpg");
		//Texture tex = new Texture("resources/texture/texture_carpet_600x600.jpg");
		//Texture tex = new Texture("resources/texture/texture_blue_checkboard_1300x1300.jpg");
		//Texture tex = new Texture("resources/texture/texture_geometry_1024x1024.jpg");
		//Texture tex = new Texture("resources/texture/texture_earthtruecolor_nasa_big_2048x1024.jpg");
		//Texture tex = new Texture("resources/texture/texture_moon_2048x1024.jpg");
		//Texture tex = new Texture("resources/texture/texture_jupiter_2048x1024.jpg");
		Texture tex = new Texture("resources/texture/texture_mars_2048x1024.jpg");
		//Texture tex = new Texture("resources/texture/texture_neptune_2048x1024.jpg");
		//Texture tex = new Texture("resources/texture/texture_football_320x160.jpg");
		
		// Create World
		World world = new World();
		Sphere sph = null;
		sph = new Sphere(1.2f, 36, tex);
		sph.setSpecularExp(10);
		sph.setSpecularColor(new Color(100,100,100));
		world.addElement(sph);
		
		System.out.println("********* Generating World");
		world.generate();
		
		DirectionalLight dl = new DirectionalLight(new Vector3(-1,1,-1), 0.9f);
		AmbientLight al = new AmbientLight(0.05f);
		Lighting light = new Lighting(dl, al, true);
		
		GraphicContext gContext = new GraphicContext(0.8f, 0.45f, 1, 100, GraphicContext.PERSPECTIVE_TYPE_FRUSTUM, 1250+625);
		GUIView gUIView = test.createView(gContext);

		//RenderContext rContext = new RenderContext(RenderContext.RENDER_DEFAULT);
		//rContext.setBackFaceCulling(RenderContext.BACKFACE_CULLING_DISABLED);
		//RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_PLAIN);
		RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE);
		rContext.setTextureProcessing(RenderContext.TEXTURE_PROCESSING_ENABLED);
		//rContext.setRenderingLines(RenderContext.RENDERING_LINES_ENABLED);

		//rContext.setRendering(RenderContext.RENDERING_TYPE_INTERPOLATE);
		
		RenderEngine renderer = new RenderEngine(world, light, camera, rContext, gContext);
		renderer.setView(gUIView);
		renderer.render();

		System.out.println("********* Rendering...");
		int nb_images = 180;
		Rotation r = new Rotation((float)Math.PI*2/(float)nb_images, Vector3.Z_AXIS);
		for (int i=0; i<=3*nb_images; i++) {
			//Rotation r = new Rotation(Math.PI*2*(double)i/(double)nb_images, Vector3.Z_AXIS);
			sph.combineTransformation(r);
			//cyl.setTransformation(r);
			renderer.render();
		}

		System.out.println("********* ENDING APPLICATION *********");
	}
}
