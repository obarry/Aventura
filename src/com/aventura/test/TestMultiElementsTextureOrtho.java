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
import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.AmbientLight;
import com.aventura.model.light.DirectionalLight;
import com.aventura.model.light.Lighting;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Box;
import com.aventura.model.world.shape.Cone;
import com.aventura.model.world.shape.Cube;
import com.aventura.model.world.shape.Cylinder;
import com.aventura.model.world.shape.Element;
import com.aventura.model.world.shape.Pyramid;
import com.aventura.model.world.shape.Sphere;
import com.aventura.model.world.shape.Trellis;
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

public class TestMultiElementsTextureOrtho {
	
	// GUIView to be displayed
	private SwingView view;
	
	public GUIView createView(GraphicContext context) {

		// Create the frame of the application 
		JFrame frame = new JFrame("Test Multi Elements with Texture");
		// Set the size of the frame
		frame.setSize(context.getPixelWidth(), context.getPixelHeight());
		
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
		int loc_x = dim.width/2 - frame.getWidth()/2;
		if (loc_x < 0) loc_x = 0;
		int loc_y = dim.height/2 - frame.getHeight()/2;
		if (loc_y < 0) loc_y = 0;
		frame.setLocation(loc_x, loc_y);
		//frame.setLocation(dim.width/2, dim.height/2);
		
		// Render the frame on the display
		frame.setVisible(true);
		
		return view;
	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		System.out.println("********* STARTING APPLICATION *********");

		Texture texbricks = new Texture("resources/texture/texture_bricks_204x204.jpg");
		//Texture texblue = new Texture("resources/texture/texture_blueground_204x204.jpg");
		//Texture texwood = new Texture("resources/texture/texture_woodfloor_160x160.jpg");
		Texture texdamier = new Texture("resources/texture/texture_damier_600x591.gif");
		Texture texgrass = new Texture("resources/texture/texture_grass_900x600.jpg");
		//Texture texstone = new Texture("resources/texture/texture_ground_stone_600x600.jpg");
		//Texture texsnow = new Texture("resources/texture/texture_snow_590x590.jpg");
		//Texture texmetal = new Texture("resources/texture/texture_metal_mesh_463x463.jpg");
		//Texture texleather = new Texture("resources/texture/texture_old_leather_box_800x610.jpg");
		Texture texmetalplate = new Texture("resources/texture/texture_metal_plate_626x626.jpg");
		//Texture texstone1 = new Texture("resources/texture/texture_stone1_1700x1133.jpg");
		//Texture texrock = new Texture("resources/texture/texture_rock_stone_400x450.jpg");
		Texture texcremedemarron = new Texture("resources/texture/texture_sticker_cremedemarrons_351x201.jpg", Texture.TEXTURE_DIRECTION_VERTICAL, Texture.TEXTURE_ORIENTATION_NORMAL, Texture.TEXTURE_ORIENTATION_OPPOSITE);
		//Texture texearth = new Texture("resources/texture/texture_earthtruecolor_nasa_big_2048x1024.jpg");
		//Texture texmoon = new Texture("resources/texture/texture_moon_2048x1024.jpg");
		Texture texfoot = new Texture("resources/texture/texture_football_320x160.jpg");
		Texture texcarpet = new Texture("resources/texture/texture_carpet_600x600.jpg");
	
		// Camera
		//Vector4 eye = new Vector4(10,6,3,1);
		Vector4 eye = new Vector4(10,0,3,1); // Orthographic gUIView
		//Vector4 eye = new Vector4(4,0,1,1); // Frustum gUIView
		//Vector4 eye = new Vector4(10,0,0,1);
		Vector4 poi = new Vector4(0,0,0,1);
		Camera camera = new Camera(eye, poi, Vector4.Z_AXIS);		
				
		TestMultiElementsTextureOrtho test = new TestMultiElementsTextureOrtho();
				
		// Create a new World
		System.out.println("********* Creating World");
		World world = new World();
		world.setBackgroundColor(Color.BLUE);
		Element e;
		
//		for (int i=-1; i<=1; i++) {
//			for (int j=-1; j<=1; j++) {
//				for (int k=-1; k<=1; k++) {
//										
//					// Create an Element of a random type
//					switch((int)(Math.random()*7)) {
//					case 0:
//						e = new Cone(1,0.5f,32, texdamier);
//						break;
//						
//					case 1:
//						e = new Cylinder(1,0.5f,32, texcremedemarron);
//						break;
//						
//					case 2:
//						e = new Sphere(0.667f,32, texfoot);
//						e.setSpecularExp(3);
//						e.setSpecularColor(new Color(100,100,100));
//						e.setColor(new Color(200,150,255));
//						break;
//						
//					case 3:
//						e = new Cube(1, texmetalplate);
//						break;
//						
//					case 4:
//						e = new Box(1.5f,1,0.5f, texbricks);
//						break;
//						
//					case 5:
//						
//						float size = 1.5f;
//						int n = 10;
//						int nb_sin = 2;
//						float array[][] = new float[n+1][n+1];
//						for (int p=0; p<=n; p++) {
//							for (int q=0; q<=n; q++) {
//								float a = (float)Math.PI*(float)nb_sin*(float)p/(float)n;
//								float b = (float)Math.PI*(float)nb_sin*(float)q/(float)n;
//								array[p][q] = size*(float)Math.sin(a)*(float)Math.sin(b)/((float)nb_sin*2);
//
//							}
//						}
//						e = null;
//						try {
//							e = new Trellis(size, size, n, n, array, texgrass);
//						} catch (WrongArraySizeException ex) {
//							// TODO Auto-generated catch block
//							ex.printStackTrace();
//						}
//						break;
//					case 6:
//						e = new Pyramid(1.4f, 1.4f, 1.4f, texcarpet);
//						break;
//						
//					default:
//						e = null;
//					}
//					
//					// Translate this element at some i,j,k indices of a 3D cube:
//					Translation t = new Translation(new Vector3(i*2, j*2, k*2));
//					e.setTransformation(t);
//
//					// Add the element to the world
//					world.addElement(e);
//				}
//			}
//		}
		
		
		e = new Cube(1, texmetalplate);
		world.addElement(e);
		
		e = new Cube(1, texmetalplate);
		e.setTransformation(new Translation(new Vector3(2,0,0)));
		world.addElement(e);

		e = new Cube(1, texmetalplate);
		e.setTransformation(new Translation(new Vector3(-2,0,0)));
		world.addElement(e);

		
		// Generate world and normals
		world.generate();

		// Print World characteristics on console
		//System.out.println(world);
		//for (int i=0; i<world.getNbElements(); i++)
		//	System.out.println(world.getElement(i));

		// Create lighting
		System.out.println("********* Creating Lighting");
		DirectionalLight dl = new DirectionalLight(new Vector3(-1,0.5f,-0.5f), 0.7f);
		AmbientLight al = new AmbientLight(0.3f);
		Lighting lighting = new Lighting(dl, al, true);

		GraphicContext context = new GraphicContext(8, 6, 1, 100, GraphicContext.PERSPECTIVE_TYPE_ORTHOGRAPHIC, 150);
		//GraphicContext context = new GraphicContext(3.0f, 1.8f, 1, 100, GraphicContext.PERSPECTIVE_TYPE_FRUSTUM, 400);
		GUIView gUIView = test.createView(context);
		System.out.println(context.getPerspective().getProjection());

		RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE);
		//rContext.setBackFaceCulling(RenderContext.BACKFACE_CULLING_DISABLED);
		rContext.setTextureProcessing(RenderContext.TEXTURE_PROCESSING_ENABLED);
		rContext.setRenderingLines(RenderContext.RENDERING_LINES_ENABLED);
		rContext.setDisplayNormals(RenderContext.DISPLAY_NORMALS_ENABLED);
		
		RenderEngine renderer = new RenderEngine(world, lighting, camera, rContext, context);
		renderer.setView(gUIView);
		renderer.render();
		
		System.out.println("********* Rendering...");
		int nb_images = 3600;
//		Rotation r1 = new Rotation((float)Math.PI*2/(float)nb_images, Vector3.X_AXIS);
//		Rotation r2 = new Rotation((float)Math.PI*2*1.5f/(float)nb_images, Vector3.Y_AXIS);
//		Rotation r3 = new Rotation((float)Math.PI*2*2.5f/(float)nb_images, Vector3.Z_AXIS);
//		Matrix4 r = r1.times(r2).times(r3);
//		for (int i=0; i<=nb_images; i++) {
//			world.expandTransformation(r);
//			renderer.render();
//		}
		Rotation r = new Rotation((float)Math.PI*2*2.5f/(float)nb_images, Vector3.Z_AXIS);
		for (int i=0; i<=nb_images; i++) {
			world.expandTransformation(r);
			renderer.render();
		}

		System.out.println("********* ENDING APPLICATION *********");

	}
}
