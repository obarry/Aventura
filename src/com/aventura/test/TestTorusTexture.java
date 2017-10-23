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
import com.aventura.math.vector.Matrix4;
import com.aventura.math.vector.Vector3;
import com.aventura.math.vector.Vector4;
import com.aventura.model.camera.Camera;
import com.aventura.model.light.AmbientLight;
import com.aventura.model.light.DirectionalLight;
import com.aventura.model.light.Lighting;
import com.aventura.model.texture.Texture;
import com.aventura.model.world.World;
import com.aventura.model.world.shape.Torus;
import com.aventura.view.SwingView;
import com.aventura.view.View;

public class TestTorusTexture {
	
	// View to be displayed
	private SwingView view;

	public View createView(GraphicContext context) {

		// Create the frame of the application 
		JFrame frame = new JFrame("Test Torus Texture");
		// Set the size of the frame
		frame.setSize(1200,800);
		
		// Create the view to be displayed
		view = new SwingView(context, frame);
		
		// Create a panel and add it to the frame
		JPanel panel = new JPanel() {
			
		    public void paintComponent(Graphics graph) {
				//System.out.println("Painting JPanel");		    	
		    	Graphics2D graph2D = (Graphics2D)graph;
		    	TestTorusTexture.this.view.draw(graph);
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

		//Texture tex = new Texture("resources/test/texture_bricks_204x204.jpg");
		//Texture tex = new Texture("resources/test/texture_blueground_204x204.jpg");
		//Texture tex = new Texture("resources/test/texture_woodfloor_160x160.jpg");
		//Texture tex = new Texture("resources/test/texture_damier_600x591.gif");
		//Texture tex = new Texture("resources/test/texture_grass_900x600.jpg");
		//Texture tex = new Texture("resources/test/texture_ground_stone_600x600.jpg");
		//Texture tex = new Texture("resources/test/texture_snow_590x590.jpg");
		//Texture tex = new Texture("resources/test/texture_metal_mesh_463x463.jpg");
		//Texture tex = new Texture("resources/test/texture_old_leather_box_800x610.jpg");
		//Texture tex = new Texture("resources/test/texture_metal_plate_626x626.jpg");
		//Texture tex = new Texture("resources/test/texture_stone1_1700x1133.jpg");
		//Texture tex = new Texture("resources/test/texture_rock_stone_400x450.jpg");
		//Texture tex = new Texture("resources/test/texture_sticker_cremedemarrons_351x201.jpg", Texture.TEXTURE_DIRECTION_VERTICAL, Texture.TEXTURE_ORIENTATION_NORMAL, Texture.TEXTURE_ORIENTATION_OPPOSITE);
		//Texture tex = new Texture("resources/test/texture_rust_960x539.jpg");
		//Texture tex = new Texture("resources/test/texture_carpet_600x600.jpg");
		//Texture tex = new Texture("resources/test/texture_blue_checkboard_1300x1300.jpg");
		Texture tex = new Texture("resources/test/texture_geometry_1024x1024.jpg");

		// Camera
		Vector4 eye = new Vector4(15,5,10,1);
		Vector4 poi = new Vector4(0,0,0,1);
		Camera camera = new Camera(eye, poi, Vector4.Z_AXIS);		
				
		TestTorusTexture test = new TestTorusTexture();
		
		System.out.println("********* Creating World");
		
		World world = new World();
		Torus torus1 = new Torus(3,1.5,32,16, tex);
		torus1.setColor(new Color(255, 200, 125));
		torus1.setSpecularExp(2);
		world.addElement(torus1);
		world.setBackgroundColor(new Color(0,0,55));
		
		System.out.println("********* Calculating normals");
		world.calculateNormals();
		
		DirectionalLight dl = new DirectionalLight(new Vector3(1,1,1), 1);
		AmbientLight al = new AmbientLight(0.2f);
		Lighting light = new Lighting(dl, al, true);
		
		GraphicContext gContext = new GraphicContext(1.2, 0.8, 1, 100, GraphicContext.PERSPECTIVE_TYPE_FRUSTUM, 1000);
		View view = test.createView(gContext);

		//RenderContext rContext = new RenderContext(RenderContext.RENDER_DEFAULT);
		RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE);
		rContext.setTextureProcessing(RenderContext.TEXTURE_PROCESSING_ENABLED);
		//rContext.setDisplayLandmark(RenderContext.DISPLAY_LANDMARK_ENABLED);
		//rContext.setDisplayLight(RenderContext.DISPLAY_LIGHT_VECTORS_ENABLED);
		//rContext.setDisplayNormals(RenderContext.DISPLAY_NORMALS_ENABLED);
		
		RenderEngine renderer = new RenderEngine(world, light, camera, rContext, gContext);
		renderer.setView(view);
		renderer.render();

		System.out.println("********* Rendering...");
		int nb_images = 360;
		Rotation r1 = new Rotation(Math.PI*1.1/(double)nb_images, Vector3.X_AXIS);
		Rotation r2 = new Rotation(Math.PI*2*4.1/(double)nb_images, Vector3.Y_AXIS);
		Rotation r3 = new Rotation(Math.PI*2*3.3/(double)nb_images, Vector3.Z_AXIS);
		Matrix4 r = r1.times(r2).times(r3);
		for (int i=0; i<=nb_images; i++) {
			world.expandTransformation(r);
			renderer.render();
		}


		System.out.println("********* ENDING APPLICATION *********");
	}
}
