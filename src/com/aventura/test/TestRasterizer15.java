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
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.AmbientLight;
import com.aventura.model.light.DirectionalLight;
import com.aventura.model.light.Lighting;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.Element;
import com.aventura.model.world.Triangle;
import com.aventura.model.world.Vertex;
import com.aventura.model.world.World;
import com.aventura.tools.tracing.Tracer;
import com.aventura.view.SwingView;
import com.aventura.view.View;

public class TestRasterizer15 {
	
	// View to be displayed
	private SwingView view;

	public View createView(GraphicContext context) {

		// Create the frame of the application 
		JFrame frame = new JFrame("Test Rasterizer 15");
		// Set the size of the frame
		frame.setSize(1000,600);
		
		// Create the view to be displayed
		view = new SwingView(context, frame);
		
		// Create a panel and add it to the frame
		JPanel panel = new JPanel() {
			
		    public void paintComponent(Graphics graph) {
				//System.out.println("Painting JPanel");		    	
		    	Graphics2D graph2D = (Graphics2D)graph;
		    	TestRasterizer15.this.view.draw(graph);
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
		Camera camera = new Camera(eye, poi, Vector4.X_AXIS);		
				
		TestRasterizer15 test = new TestRasterizer15();
		
		System.out.println("********* Creating World");
		
		//Texture tex = new Texture("resources/test/texture_bricks_204x204.jpg");
		//Texture tex = new Texture("resources/test/texture_blueground_204x204.jpg");
		//Texture tex = new Texture("resources/test/texture_woodfloor_160x160.jpg");
		Texture tex = new Texture("resources/test/texture_damier_600x591.gif");
		//Texture tex = new Texture("resources/test/texture_grass_900x600.jpg");
		//Texture tex = new Texture("resources/test/texture_ground_stone_600x600.jpg");
		//Texture tex = new Texture("resources/test/texture_snow_590x590.jpg");
		
		// Create World
		World world = new World();
		Element e = new Element();
		
		//
		// Create a shape
		//
		//      V4  V3
		//       ----
		//      /    \
		//     /      \
		//    /        \
		//   /          \
		//  --------------
		//  V1           V2
		//
		
		double c = Math.cos(2*(Math.PI)/3);
		double s = Math.sin(2*(Math.PI)/3);
		
		Vector4 vec1 = new Vector4(1,0,0,1);
		Vector4 vec2 = new Vector4(c,s,0,1);
		Vector4 vec3 = new Vector4(c,-s,0,1);
		
		Vertex v1 = new Vertex(vec1);
		Vertex v2 = new Vertex(vec2);
		Vertex v3 = new Vertex(vec3);

		e.addVertex(v1);
		e.addVertex(v2);
		e.addVertex(v3);

		//
		// Create triangles T1 and T2
		//
		//      V4  V3
		//       ----
		//      /   /\
		//     /T2/   \
		//    / /  T1  \
		//   //         \
		//  --------------
		//  V1           V2
		//

		Triangle t1 = new Triangle(v1, v2, v3, tex, Triangle.TEXTURE_VERTICAL);
		
		// Create Texture vectors with distortion effect to take account of the proportion of the shape made of 2 triangles
		// V3 is on the small segment (ratio 0.3:1)
//		t1.setTexture(tex, new Vector4(0,0,0,1), new Vector4(1,0,0,1), new Vector4(0.3,0.3,0,0.3));
		t1.setTexture(new Vector4(0,0,0,1), new Vector4(1,0,0,1), new Vector4(0.0001,1,0,0.0002));
		// V3 and V4 are on the small segment (ratio 0.3:1)
		
		
		e.addTriangle(t1);
		
		world.addElement(e);
		world.setBackgroundColor(new Color(22,0,220));
		
		System.out.println("********* Calculating normals");
		world.calculateNormals();
		
		DirectionalLight dl = new DirectionalLight(new Vector3(1,1,1), 0.5f);
		AmbientLight al = new AmbientLight(0.5f);
		Lighting light = new Lighting(dl, al);
		
		GraphicContext gContext = new GraphicContext(0.8, 0.45, 1, 100, GraphicContext.PERSPECTIVE_TYPE_FRUSTUM, 1250);
		View view = test.createView(gContext);

		//RenderContext rContext = new RenderContext(RenderContext.RENDER_DEFAULT_ALL_ENABLED);
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
			Rotation r = new Rotation(Math.PI*2*(double)i/(double)nb_images, Vector3.Z_AXIS);
			e.setTransformation(r);
			renderer.render();
		}

		System.out.println("********* ENDING APPLICATION *********");
	}
}
