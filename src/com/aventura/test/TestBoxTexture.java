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
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Box;
import com.aventura.tools.tracing.Tracer;
import com.aventura.view.SwingView;
import com.aventura.view.View;

public class TestBoxTexture {
	
	// View to be displayed
	private SwingView view;

	public View createView(GraphicContext context) {

		// Create the frame of the application 
		JFrame frame = new JFrame("Test Box with Texture");
		// Set the size of the frame
		frame.setSize(1500,900);
		
		// Create the view to be displayed
		view = new SwingView(context, frame);
		
		// Create a panel and add it to the frame
		JPanel panel = new JPanel() {
			
		    public void paintComponent(Graphics graph) {
				//System.out.println("Painting JPanel");		    	
		    	Graphics2D graph2D = (Graphics2D)graph;
		    	TestBoxTexture.this.view.draw(graph);
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
		Vector4 eye = new Vector4(-4,-8,5,1);
		//Vector4 eye = new Vector4(16,6,12,1);
		//Vector4 eye = new Vector4(3,2,2,1);
		Vector4 poi = new Vector4(0,0,0,1);
		Camera camera = new Camera(eye, poi, Vector4.Z_AXIS);		
				
		TestBoxTexture test = new TestBoxTexture();
		
		System.out.println("********* Creating World");
		
		//Texture tex = new Texture("resources/test/texture_bricks_204x204.jpg");
		//Texture tex = new Texture("resources/test/texture_blueground_204x204.jpg");
		//Texture tex = new Texture("resources/test/texture_woodfloor_160x160.jpg");
		//Texture tex = new Texture("resources/test/texture_damier_600x591.gif");
		//Texture tex = new Texture("resources/test/texture_grass_900x600.jpg");
		//Texture tex = new Texture("resources/test/texture_ground_stone_600x600.jpg");
		//Texture tex = new Texture("resources/test/texture_snow_590x590.jpg");
		//Texture tex = new Texture("resources/test/texture_metal_mesh_463x463.jpg");
		//Texture tex = new Texture("resources/test/texture_old_leather_box_800x610.jpg");
		Texture tex = new Texture("resources/test/texture_metal_plate_626x626.jpg");
		//Texture tex = new Texture("resources/test/texture_stone1_1700x1133.jpg");
		//Texture tex = new Texture("resources/test/texture_rock_stone_400x450.jpg");

		
		// Create World
		World world = new World();
		
		Box elm = new Box(3,2,1.5, tex);
		//elm.setColor(new Color(100,200,255));
		//elm.setSpecularExp(8);
		world.addElement(elm);
		world.setBackgroundColor(new Color(20,10,5));
		
		System.out.println(world);
		System.out.println(elm);
		System.out.println("********* Calculating normals");
		
		world.calculateNormals();
		
		DirectionalLight dl = new DirectionalLight(new Vector3(-0.5,0,1), 0.8f);
		AmbientLight al = new AmbientLight(0.2f);
		Lighting light = new Lighting(dl, al, false);
		
		GraphicContext gContext = new GraphicContext(0.8, 0.45, 1, 100, GraphicContext.PERSPECTIVE_TYPE_FRUSTUM, 1250+625);
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
			Rotation r = new Rotation(Math.PI*2*(double)i/(double)nb_images, Vector3.X_AXIS);
			elm.setTransformation(r);
			renderer.render();
		}

		System.out.println("********* ENDING APPLICATION *********");
	}
}