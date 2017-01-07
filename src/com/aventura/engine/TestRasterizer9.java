package com.aventura.engine;

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
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.DirectionalLight;
import com.aventura.model.light.Lighting;
import com.aventura.model.world.ConeFrustum;
import com.aventura.model.world.ConeSummit;
import com.aventura.model.world.Cylinder;
import com.aventura.model.world.World;
import com.aventura.view.SwingView;
import com.aventura.view.View;

public class TestRasterizer9 {
	
	// View to be displayed
	private SwingView view;

	public View createView(GraphicContext context) {

		// Create the frame of the application 
		JFrame frame = new JFrame("Test Rasterizer 9");
		// Set the size of the frame
		frame.setSize(1000,600);
		
		// Create the view to be displayed
		view = new SwingView(context, frame);
		
		// Create a panel and add it to the frame
		JPanel panel = new JPanel() {
			
		    public void paintComponent(Graphics graph) {
				//System.out.println("Painting JPanel");		    	
		    	Graphics2D graph2D = (Graphics2D)graph;
		    	TestRasterizer9.this.view.draw(graph);
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
		
//		Tracer.info = true;
//		Tracer.function = true;

		// Camera
		Vector4 eye = new Vector4(6,-2,0,1);
		Vector4 poi = new Vector4(1,0,0,1);
		Camera camera = new Camera(eye, poi, Vector4.Z_AXIS);		
				
		TestRasterizer9 test = new TestRasterizer9();
		
		System.out.println("********* Creating World");
		
		World world = new World();
		//Cylinder c = new Cylinder(1.5, 1, 12);
		//ConeSummit c = new ConeSummit(1.5, 1, 2);
		ConeFrustum c = new ConeFrustum(2, 1.5, 1, 24);
		c.setColor(Color.CYAN);		
		world.addElement(c);
		
		System.out.println("********* Calculating normals");
		world.calculateNormals();
		
		Lighting light = new Lighting();
		DirectionalLight dl = new DirectionalLight(new Vector4(1,1,1,0), 1);
		light.addLight(dl);
		
		
		GraphicContext gContext = new GraphicContext(0.8, 0.4512, 1, 100, GraphicContext.PERSPECTIVE_TYPE_FRUSTUM, 1250);
		View view = test.createView(gContext);

		RenderContext rContext = new RenderContext(RenderContext.RENDER_DEFAULT_ALL_ENABLED);
		rContext.setRendering(RenderContext.RENDERING_TYPE_INTERPOLATE);
//		rContext.setRendering(RenderContext.RENDERING_TYPE_PLAIN);
		
		RenderEngine renderer = new RenderEngine(world, light, camera, rContext, gContext);
		renderer.setView(view);
		renderer.render();

		System.out.println("********* Rendering...");
		int nb_images = 240;
		for (int i=0; i<=3*nb_images; i++) {
			double a = Math.PI*2*(double)i/(double)nb_images;
			eye = new Vector4(8*Math.cos(a),4*Math.sin(a),-2,1);
			//System.out.println("Rotation "+i+"  - Eye: "+eye);
			camera.updateCamera(eye, poi, Vector4.Z_AXIS);
			renderer.render();
		}

		System.out.println("********* ENDING APPLICATION *********");
	}
}
