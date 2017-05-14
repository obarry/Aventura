package com.aventura.test;

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
import com.aventura.math.vector.Vector2;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.AmbientLight;
import com.aventura.model.light.DirectionalLight;
import com.aventura.model.light.Lighting;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.Box;
import com.aventura.model.world.World;
import com.aventura.view.SwingView;
import com.aventura.view.View;

public class TestRasterizer12 {
	
	// View to be displayed
	private SwingView view;

	public View createView(GraphicContext context) {

		// Create the frame of the application 
		JFrame frame = new JFrame("Test Rasterizer 12");
		// Set the size of the frame
		frame.setSize(1000,600);
		
		// Create the view to be displayed
		view = new SwingView(context, frame);
		
		// Create a panel and add it to the frame
		JPanel panel = new JPanel() {
			
		    public void paintComponent(Graphics graph) {
				//System.out.println("Painting JPanel");		    	
		    	Graphics2D graph2D = (Graphics2D)graph;
		    	TestRasterizer12.this.view.draw(graph);
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
		
		//Tracer.info = true;
		//Tracer.function = true;

		// Camera
		Vector4 eye = new Vector4(8,3,2,1);
		Vector4 poi = new Vector4(0,0,0,1);
		Camera camera = new Camera(eye, poi, Vector4.Z_AXIS);		
				
		TestRasterizer12 test = new TestRasterizer12();
		
		System.out.println("********* Creating World");
		
		Texture tex1 = new Texture("resources/test/texture_bricks_204x204.jpg");
		Texture tex2 = new Texture("resources/test/texture_blueground_204x204.jpg");
		
		World world = new World();
		Box box = new Box(1.2,1.5,1);
		// Bottom
		box.getTriangle(0).setTexture(tex1, new Vector2(0,0), new Vector2(0,1), new Vector2(1,1));
		box.getTriangle(1).setTexture(tex1, new Vector2(1,1), new Vector2(1,0), new Vector2(0,0));
		// Back
		box.getTriangle(2).setTexture(tex1, new Vector2(0,0), new Vector2(0,1), new Vector2(1,1));
		box.getTriangle(3).setTexture(tex1, new Vector2(1,1), new Vector2(1,0), new Vector2(0,0));
		// Left Side
		box.getTriangle(4).setTexture(tex1, new Vector2(0,0), new Vector2(0,1), new Vector2(1,1));
		box.getTriangle(5).setTexture(tex1, new Vector2(1,1), new Vector2(1,0), new Vector2(0,0));
		// Top
		box.getTriangle(6).setTexture(tex1, new Vector2(0,0), new Vector2(0,1), new Vector2(1,1));
		box.getTriangle(7).setTexture(tex1, new Vector2(1,1), new Vector2(1,0), new Vector2(0,0));
		// Front
		box.getTriangle(8).setTexture(tex1, new Vector2(0,0), new Vector2(0,1), new Vector2(1,1));
		box.getTriangle(9).setTexture(tex2, new Vector2(1,1), new Vector2(1,0), new Vector2(0,0));
		// Right Side
		box.getTriangle(10).setTexture(tex1, new Vector2(0,0), new Vector2(0,1), new Vector2(1,1));
		box.getTriangle(11).setTexture(tex1, new Vector2(1,1), new Vector2(1,0), new Vector2(0,0));

		world.addElement(box);
		
		System.out.println("********* Calculating normals");
		world.calculateNormals();
		
		DirectionalLight dl = new DirectionalLight(new Vector3(0.5,0.5,1), 1);
		AmbientLight al = new AmbientLight(0.1f);
		Lighting light = new Lighting(dl, al);
		
		GraphicContext gContext = new GraphicContext(0.8, 0.45, 1, 100, GraphicContext.PERSPECTIVE_TYPE_FRUSTUM, 1250);
		View view = test.createView(gContext);

		//RenderContext rContext = new RenderContext(RenderContext.RENDER_DEFAULT_ALL_ENABLED);
		RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE);
		rContext.setTextureProcessing(RenderContext.TEXTURE_PROCESSING_ENABLED);

		//rContext.setRendering(RenderContext.RENDERING_TYPE_INTERPOLATE);
		
		RenderEngine renderer = new RenderEngine(world, light, camera, rContext, gContext);
		renderer.setView(view);
		renderer.render();

//		System.out.println("********* Rendering...");
//		int nb_images = 180;
//		for (int i=0; i<=3*nb_images; i++) {
//			double a = Math.PI*2*(double)i/(double)nb_images;
//			eye = new Vector4(8*Math.cos(a),8*Math.sin(a),2,1);
//			//System.out.println("Rotation "+i+"  - Eye: "+eye);
//			camera.updateCamera(eye, poi, Vector4.Z_AXIS);
//			renderer.render();
//		}

		System.out.println("********* Rendering...");
		int nb_images = 180;
		for (int i=0; i<=3*nb_images; i++) {
			Rotation r = new Rotation(Math.PI*2*(double)i/(double)nb_images, Vector3.Z_AXIS);
			box.setTransformation(r);
			renderer.render();
		}

		System.out.println("********* ENDING APPLICATION *********");
	}
}
