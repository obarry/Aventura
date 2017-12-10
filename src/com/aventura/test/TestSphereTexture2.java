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
import com.aventura.view.View;

public class TestSphereTexture2 {
	
	// View to be displayed
	private SwingView view;

	public View createView(GraphicContext context) {

		// Create the frame of the application 
		JFrame frame = new JFrame("Test Sphere Texture 2");
		// Set the size of the frame
		frame.setSize(1500,880);
		
		// Create the view to be displayed
		view = new SwingView(context, frame);
		
		// Create a panel and add it to the frame
		JPanel panel = new JPanel() {
			
		    public void paintComponent(Graphics graph) {
				//System.out.println("Painting JPanel");		    	
		    	Graphics2D graph2D = (Graphics2D)graph;
		    	TestSphereTexture2.this.view.draw(graph);
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
		Vector4 eye = new Vector4(430,4,5,1);
		Vector4 poi = new Vector4(0,0,0,1);
		Camera camera = new Camera(eye, poi, Vector4.Z_AXIS);		
				
		TestSphereTexture2 test = new TestSphereTexture2();
		
		System.out.println("********* Creating World");
		
		Texture texearth = new Texture("resources/test/texture_earthtruecolor_nasa_big_2048x1024.jpg");
		Texture texmoon = new Texture("resources/test/texture_moon_2048x1024.jpg");
		//Texture tex = new Texture("resources/test/texture_jupiter_2048x1024.jpg");
		//Texture tex = new Texture("resources/test/texture_mars_2048x1024.jpg");
		//Texture tex = new Texture("resources/test/texture_neptune_2048x1024.jpg");
		
		// Create World
		World world = new World();
		
		Sphere earth = new Sphere(12.742, 48, texearth);
		earth.setSpecularExp(3);
		earth.setSpecularColor(new Color(100,100,100));
		world.addElement(earth);
		
		Sphere moon = new Sphere(3.474, 48, texmoon);
		moon.setSpecularExp(3);
		moon.setSpecularColor(new Color(100,100,100));
		Translation t = new Translation(new Vector4(384.4,0,0,0));
		moon.setTransformation(t);
		world.addElement(moon);
		
		world.calculateNormals();
		
		System.out.println("********* Creating light");
		DirectionalLight dl = new DirectionalLight(new Vector3(1,-1,0), 0.9f);
		AmbientLight al = new AmbientLight(0.05f);
		Lighting light = new Lighting(dl, al, false);
		
		GraphicContext gContext = new GraphicContext(0.8, 0.45, 1, 500, GraphicContext.PERSPECTIVE_TYPE_FRUSTUM, 1250+625);
		View view = test.createView(gContext);

		RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE);
		rContext.setTextureProcessing(RenderContext.TEXTURE_PROCESSING_ENABLED);
		//rContext.setRenderingLines(RenderContext.RENDERING_LINES_ENABLED);

		//rContext.setRendering(RenderContext.RENDERING_TYPE_INTERPOLATE);
		
		RenderEngine renderer = new RenderEngine(world, light, camera, rContext, gContext);
		renderer.setView(view);
		
		System.out.println("********* Rendering");
		renderer.render();

//		System.out.println("********* Rendering...");
//		int nb_images = 180;
//		Rotation r = new Rotation(Math.PI*2/(double)nb_images, Vector3.Z_AXIS);
//		for (int i=0; i<=3*nb_images; i++) {
//			//Rotation r = new Rotation(Math.PI*2*(double)i/(double)nb_images, Vector3.Z_AXIS);
//			sph.combineTransformation(r);
//			//cyl.setTransformation(r);
//			renderer.render();
//		}

		System.out.println("********* ENDING APPLICATION *********");
	}
}