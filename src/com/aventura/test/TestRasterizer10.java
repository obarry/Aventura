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
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Torus;
import com.aventura.view.SwingView;
import com.aventura.view.View;

public class TestRasterizer10 {
	
	// View to be displayed
	private SwingView view;

	public View createView(GraphicContext context) {

		// Create the frame of the application 
		JFrame frame = new JFrame("Test Rasterizer 10");
		// Set the size of the frame
		frame.setSize(1200,800);
		
		// Create the view to be displayed
		view = new SwingView(context, frame);
		
		// Create a panel and add it to the frame
		JPanel panel = new JPanel() {
			
		    public void paintComponent(Graphics graph) {
				//System.out.println("Painting JPanel");		    	
		    	Graphics2D graph2D = (Graphics2D)graph;
		    	TestRasterizer10.this.view.draw(graph);
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

	public static void main(String[] args) {
		
		System.out.println("********* STARTING APPLICATION *********");
		
		// Camera
		Vector4 eye = new Vector4(10,3,8,1);
		Vector4 poi = new Vector4(0,0,0,1);
		Camera camera = new Camera(eye, poi, Vector4.Z_AXIS);		
				
		TestRasterizer10 test = new TestRasterizer10();
		
		System.out.println("********* Creating World");
		
		World world = new World();
		Torus torus1 = new Torus(2,0.5f,32,16);
		torus1.setSpecularExp(2);
		torus1.setTransformation(new Translation(new Vector4(0,2,0,0)));
		Torus torus2 = new Torus(2,0.5f,32,16);
		torus2.setSpecularExp(2);
		torus2.setTransformation(new Rotation((float)Math.PI/2, Vector4.Y_AXIS));
		torus1.setColor(new Color(20,100,100));
		torus2.setColor(new Color(180,100,20));
		world.addElement(torus1);
		world.addElement(torus2);
		
		System.out.println("********* Calculating normals");
		world.calculateNormals();
		
		DirectionalLight dl = new DirectionalLight(new Vector3(1,1,1), 1);
		AmbientLight al = new AmbientLight(0.2f);
		Lighting light = new Lighting(dl, al, true);
		
		GraphicContext gContext = new GraphicContext(1.2f, 0.8f, 1, 100, GraphicContext.PERSPECTIVE_TYPE_FRUSTUM, 1000);
		View view = test.createView(gContext);

		//RenderContext rContext = new RenderContext(RenderContext.RENDER_DEFAULT);
		RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE);
		//rContext.setDisplayLandmark(RenderContext.DISPLAY_LANDMARK_ENABLED);
		//rContext.setDisplayLight(RenderContext.DISPLAY_LIGHT_VECTORS_ENABLED);
		//rContext.setDisplayNormals(RenderContext.DISPLAY_NORMALS_ENABLED);
		
		RenderEngine renderer = new RenderEngine(world, light, camera, rContext, gContext);
		renderer.setView(view);
		renderer.render();

		System.out.println("********* Rendering...");
//		int nb_images = 180;
//		for (int i=0; i<=3*nb_images; i++) {
//			double a = Math.PI*2*(double)i/(double)nb_images;
//			eye = new Vector4(6*Math.cos(a),3, 6*Math.sin(a),1);
//			//System.out.println("Rotation "+i+"  - Eye: "+eye);
//			camera.updateCamera(eye, poi, Vector4.Z_AXIS);
//			renderer.render();
//		}
		
		System.out.println("********* Rendering...");
		int nb_images = 360;
		Rotation r1 = new Rotation((float)Math.PI*1.1f/(float)nb_images, Vector3.X_AXIS);
		Rotation r2 = new Rotation((float)Math.PI*2*4.1f/(float)nb_images, Vector3.Y_AXIS);
		Rotation r3 = new Rotation((float)Math.PI*2*3.3f/(float)nb_images, Vector3.Z_AXIS);
		Matrix4 r = r1.times(r2).times(r3);
		for (int i=0; i<=nb_images; i++) {
			world.expandTransformation(r);
			renderer.render();
		}


		System.out.println("********* ENDING APPLICATION *********");
	}
}
