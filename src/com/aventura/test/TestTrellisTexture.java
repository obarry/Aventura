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
import com.aventura.model.world.WrongArraySizeException;
import com.aventura.model.world.shape.Trellis;
import com.aventura.view.SwingView;
import com.aventura.view.View;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2020 Olivier BARRY
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

public class TestTrellisTexture {
	
	// View to be displayed
	private SwingView view;

	public View createView(GraphicContext context) {

		// Create the frame of the application 
		JFrame frame = new JFrame("Test Trellis with Texture");
		// Set the size of the frame
		frame.setSize(1000,600);
		
		// Create the view to be displayed
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
		Vector4 eye = new Vector4(8,3,5,1);
		//Vector4 eye = new Vector4(16,6,12,1);
		//Vector4 eye = new Vector4(3,2,2,1);
		Vector4 poi = new Vector4(0,0,-0.5f,1);
		Camera camera = new Camera(eye, poi, Vector4.Z_AXIS);		
				
		TestTrellisTexture test = new TestTrellisTexture();
		
		System.out.println("********* Creating World");
		
		//Texture tex = new Texture("resources/texture/texture_bricks_204x204.jpg");
		//Texture tex = new Texture("resources/texture/texture_blueground_204x204.jpg");
		//Texture tex = new Texture("resources/texture/texture_woodfloor_160x160.jpg");
		//Texture tex = new Texture("resources/texture/texture_damier_600x591.gif");
		//Texture tex = new Texture("resources/texture/texture_grass_900x600.jpg");
		Texture tex = new Texture("resources/texture/texture_ground_stone_600x600.jpg");
		//Texture tex = new Texture("resources/texture/texture_snow_590x590.jpg");
		
		// Create World
		World world = new World();
		float size = 4;
		int n = 40;
		int nb_sin = 4;
		float array[][] = new float[n+1][n+1];
		for (int i=0; i<=n; i++) {
			for (int j=0; j<=n; j++) {
				float a = (float)Math.PI*(float)nb_sin*(float)i/(float)n;
				float b = (float)Math.PI*(float)nb_sin*(float)j/(float)n;
				array[i][j] = size*(float)Math.sin(a)*(float)Math.sin(b)/((float)nb_sin*4);

			}
		}
		Trellis tre = null;
		try {
			tre = new Trellis(size, size, n, n, array, tex);
		} catch (WrongArraySizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		tre.setColor(new Color(200,200,255));
		tre.setSpecularExp(8);
		world.addElement(tre);
		
		System.out.println("********* Calculating normals");
		world.generate();
		
		DirectionalLight dl = new DirectionalLight(new Vector3(1,-1,1), 0.7f);
		AmbientLight al = new AmbientLight(0.3f);
		Lighting light = new Lighting(dl, al, false);
		
		GraphicContext gContext = new GraphicContext(0.8f, 0.45f, 1, 100, GraphicContext.PERSPECTIVE_TYPE_FRUSTUM, 1250);
		View view = test.createView(gContext);

		//RenderContext rContext = new RenderContext(RenderContext.RENDER_DEFAULT);
		RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE);
		rContext.setTextureProcessing(RenderContext.TEXTURE_PROCESSING_ENABLED);
		//rContext.setDisplayNormals(RenderContext.DISPLAY_NORMALS_ENABLED);
		//rContext.setDisplayLandmark(RenderContext.DISPLAY_LANDMARK_ENABLED);

		//rContext.setRendering(RenderContext.RENDERING_TYPE_INTERPOLATE);
		
		RenderEngine renderer = new RenderEngine(world, light, camera, rContext, gContext);
		renderer.setView(view);
		renderer.render();

		System.out.println("********* Rendering...");
		int nb_images = 180;
		for (int i=0; i<=3*nb_images; i++) {
			Rotation r = new Rotation((float)Math.PI*2*(float)i/(float)nb_images, Vector3.Z_AXIS);
			tre.setTransformation(r);
			renderer.render();
		}

		System.out.println("********* ENDING APPLICATION *********");
	}
}
