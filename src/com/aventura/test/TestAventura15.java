package com.aventura.test;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import com.aventura.context.GraphicContext;
import com.aventura.context.RenderContext;
import com.aventura.engine.RenderEngine;
import com.aventura.math.transform.Rotation;
import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.AmbientLight;
import com.aventura.model.light.DirectionalLight;
import com.aventura.model.light.Lighting;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.World;
import com.aventura.model.world.WrongArraySizeException;
import com.aventura.model.world.shape.Box;
import com.aventura.model.world.shape.Cone;
import com.aventura.model.world.shape.Cube;
import com.aventura.model.world.shape.Cylinder;
import com.aventura.model.world.shape.Element;
import com.aventura.model.world.shape.Sphere;
import com.aventura.model.world.shape.Trellis;
import com.aventura.view.SwingView;
import com.aventura.view.View;

/**
* ------------------------------------------------------------------------------ 
* MIT License
* 
* Copyright (c) 2017 Olivier BARRY
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

public class TestAventura15 {
	
	// View to be displayed
	private SwingView view;
	
	public View createView(GraphicContext context) {

		// Create the frame of the application 
		JFrame frame = new JFrame("Test Aventura 15");
		// Set the size of the frame
		frame.setSize(context.getPixelWidth(), context.getPixelHeight());
		
		// Create the view to be displayed
		view = new SwingView(context, frame);
		
		// Create a panel and add it to the frame
		JPanel panel = new JPanel() {
			
		    public void paintComponent(Graphics graph) {
				//System.out.println("Painting JPanel");		    	
		    	Graphics2D graph2D = (Graphics2D)graph;
		    	TestAventura15.this.view.draw(graph);
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

		Texture texbricks = new Texture("resources/test/texture_bricks_204x204.jpg");
		//Texture texblue = new Texture("resources/test/texture_blueground_204x204.jpg");
		//Texture texwood = new Texture("resources/test/texture_woodfloor_160x160.jpg");
		Texture texdamier = new Texture("resources/test/texture_damier_600x591.gif");
		Texture texgrass = new Texture("resources/test/texture_grass_900x600.jpg");
		//Texture texstone = new Texture("resources/test/texture_ground_stone_600x600.jpg");
		//Texture texsnow = new Texture("resources/test/texture_snow_590x590.jpg");
		//Texture texmetal = new Texture("resources/test/texture_metal_mesh_463x463.jpg");
		//Texture texleather = new Texture("resources/test/texture_old_leather_box_800x610.jpg");
		Texture texmetalplate = new Texture("resources/test/texture_metal_plate_626x626.jpg");
		//Texture texstone1 = new Texture("resources/test/texture_stone1_1700x1133.jpg");
		//Texture texrock = new Texture("resources/test/texture_rock_stone_400x450.jpg");
		Texture texcremedemarron = new Texture("resources/test/texture_sticker_cremedemarrons_351x201.jpg", Texture.TEXTURE_DIRECTION_VERTICAL, Texture.TEXTURE_ORIENTATION_NORMAL, Texture.TEXTURE_ORIENTATION_OPPOSITE);
		//Texture texearth = new Texture("resources/test/texture_earthtruecolor_nasa_big_2048x1024.jpg");
		//Texture texmoon = new Texture("resources/test/texture_moon_2048x1024.jpg");
		Texture texfoot = new Texture("resources/test/texture_football_320x160.jpg");
		
		// Camera
		Vector4 eye = new Vector4(10,6,3,1);
		Vector4 poi = new Vector4(0,0,0,1);
		Camera camera = new Camera(eye, poi, Vector4.Z_AXIS);		
				
		TestAventura15 test = new TestAventura15();
				
		// Create a new World
		System.out.println("********* Creating World");
		World world = new World();
		world.setBackgroundColor(Color.BLACK);
		Element e;
		
		for (int i=-1; i<=1; i++) {
			for (int j=-1; j<=1; j++) {
				for (int k=-1; k<=1; k++) {
										
					// Create an Element of a random type
					switch(Math.round((float)Math.random()*5)) {
					case 0:
						e = new Cone(1,0.5,32, texdamier);
						//e.setColor(Color.YELLOW);
						break;
					case 1:
						e = new Cylinder(1,0.5,32, texcremedemarron);
						//e.setColor(Color.CYAN);
						break;
					case 2:
						e = new Sphere(0.667,32, texfoot);
						e.setSpecularExp(3);
						e.setSpecularColor(new Color(100,100,100));
						e.setColor(new Color(200,150,255));
						break;
					case 3:
						e = new Cube(1, texmetalplate);
						//e.setColor(Color.PINK);
						break;
					case 4:
						e = new Box(1.5,1,0.5, texbricks);
						//e.setColor(Color.ORANGE);
						break;
					case 5:
						
						double size = 1.5;
						int n = 10;
						int nb_sin = 2;
						double array[][] = new double[n+1][n+1];
						for (int p=0; p<=n; p++) {
							for (int q=0; q<=n; q++) {
								double a = Math.PI*(double)nb_sin*(double)p/(double)n;
								double b = Math.PI*(double)nb_sin*(double)q/(double)n;
								array[p][q] = size*Math.sin(a)*Math.sin(b)/((double)nb_sin*2);

							}
						}
						e = null;
						try {
							e = new Trellis(size, size, n, n, array, texgrass);
						} catch (WrongArraySizeException ex) {
							// TODO Auto-generated catch block
							ex.printStackTrace();
						}

						//e = new Trellis(1.5,1.5,8,8, texgrass);
						//e.setColor(Color.GREEN);
						break;
					default:
						e = null;
					}
					
					// Translate this element at some i,j,k indices of a 3D cube:
					Translation t = new Translation(new Vector3(i*2, j*2, k*2));
					e.setTransformation(t);

					// Add the element to the world
					world.addElement(e);
				}
			}
		}
		
		System.out.println(world);
		for (int i=0; i<world.getNbElements(); i++)
			System.out.println(world.getElement(i));

		// Calculate normals
		world.calculateNormals();

		// Create lighting
		System.out.println("********* Creating Lighting");
		DirectionalLight dl = new DirectionalLight(new Vector3(1,-0.5,0.5), 0.7f);
		AmbientLight al = new AmbientLight(0.3f);
		Lighting lighting = new Lighting(dl, al, true);

		GraphicContext context = new GraphicContext(1.5, 0.9, 1, 100, GraphicContext.PERSPECTIVE_TYPE_FRUSTUM, 1000);
		View view = test.createView(context);

		RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE_WITH_LANDMARKS);
		rContext.setTextureProcessing(RenderContext.TEXTURE_PROCESSING_ENABLED);
		//rContext.setRenderingLines(RenderContext.RENDERING_LINES_ENABLED);
		//rContext.setDisplayNormals(RenderContext.DISPLAY_NORMALS_ENABLED);
		
		RenderEngine renderer = new RenderEngine(world, lighting, camera, rContext, context);
		renderer.setView(view);
		renderer.render();
		
		System.out.println("********* Rendering...");
		int nb_images = 360;
		Rotation r1 = new Rotation(Math.PI*2/(double)nb_images, Vector3.X_AXIS);
		Rotation r2 = new Rotation(Math.PI*2*1.5/(double)nb_images, Vector3.Y_AXIS);
		Rotation r3 = new Rotation(Math.PI*2*2.5/(double)nb_images, Vector3.Z_AXIS);
		Matrix4 r = r1.times(r2).times(r3);
		for (int i=0; i<=nb_images; i++) {
			world.expandTransformation(r);
			renderer.render();
		}

		System.out.println("********* ENDING APPLICATION *********");

	}
}
