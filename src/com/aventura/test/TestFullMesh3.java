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
import com.aventura.model.world.shape.Element;
import com.aventura.model.world.triangle.FullMesh;
import com.aventura.view.SwingView;
import com.aventura.view.View;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2016-2021 Olivier BARRY
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
 * Test FullMesh class
 * 
 * Create FullMesh with 1 single stitch and apply texture
 * Check that texture is correctly displayed
 * 
 * @author Olivier Barry
 *
 */

public class TestFullMesh3 {
	
	// View to be displayed
	private SwingView view;

	public View createView(GraphicContext context) {

		// Create the frame of the application 
		JFrame frame = new JFrame("Test Full Mesh 3");
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
		//Vector4 eye = new Vector4(4,2,3,1);
		Vector4 eye = new Vector4(0,0,5,1);
		Vector4 poi = new Vector4(0,0,0,1);
		//Camera camera = new Camera(eye, poi, Vector4.Z_AXIS);		
		Camera camera = new Camera(eye, poi, Vector4.Y_AXIS);		
				
		TestFullMesh3 test = new TestFullMesh3();
		
		System.out.println("********* Creating World");
		
		Texture tex = new Texture("resources/texture/texture_bricks_204x204.jpg");
		//Texture tex = new Texture("resources/texture/texture_blueground_204x204.jpg");
		//Texture tex = new Texture("resources/texture/texture_woodfloor_160x160.jpg");
		//Texture tex = new Texture("resources/texture/texture_damier_600x591.gif");
		//Texture tex = new Texture("resources/texture/texture_grass_900x600.jpg");
		//Texture tex = new Texture("resources/texture/texture_ground_stone_600x600.jpg");
		//Texture tex = new Texture("resources/texture/texture_snow_590x590.jpg");
		
		// Create World
		World world = new World();
		Element e = new Element();
		
		//           lx2        
		//     +-----______ 
		//     |           -----+ 
		//     |                |
		// ly1 |                | 
		//     |                 |  ly2
		//     |                 | 
		//     |                  |
		//     |                  |
		// x1  +------------------+
		//     y1       lx1
		//
		
		// Dimensions of the whole mesh
		float x1 = -0.7f;
		float y1 = -0.5f;
		float lx1 = 1.4f;
		float ly1 = 1;
		float lx2 = 1;
		float ly2 = 0.7f;
		
		// Number of stitches in X and Y
		int nx = 3;
		int ny = 3;
		
		// Instantiation of the FullMesh
		FullMesh full = new FullMesh(e, nx, ny, tex);
		
		// Generation of the vertices
		Vector4 V1, V2, V3, V4, VC = null;
		for (int i=0; i<nx; i++) {
			
			float x1_i = x1+i*lx1/nx; // xi on bottom segment
			float x1_ip1 = x1+(i+1)*lx1/nx; // xip1 on bottom segment
			
			float x2_i = x1+i*lx2/nx; // xi on top segment
			float x2_ip1 = x1+(i+1)*lx2/nx; // xip1 on top segment
			
			for (int j=0; j<ny; j++) {
				
				float xi = x1_i+(x2_i-x1_i)*j/ny; // xi depending on x2 x1 variation
				float xip1 = x1_ip1+(x2_ip1-x1_ip1)*j/ny;
				
				float y1_j = y1+j*ly1/ny; // xi on bottom segment
				float y1_jp1 = y1+(j+1)*ly1/ny; // xip1 on bottom segment
				
				float y2_j = y1+j*ly2/ny; // xi on top segment
				float y2_jp1 = y1+(j+1)*ly2/ny; // xip1 on top segment
				
				float yj = y1_j+(y2_j-y1_j)*i/nx; // yj depending on y2 y1 variation
				float yjp1 = y1_jp1+(y2_jp1-y1_jp1)*i/nx;

				V1 = new Vector4(xi, yj, 0, 1);
				V2 = new Vector4(xip1, yj, 0, 1);
				V3 = new Vector4(xip1, yjp1, 0, 1);
				V4 = new Vector4(xi, yjp1, 0, 1);
				VC = new Vector4((xi+xip1)/2, (yj+yjp1)/2, 0, 1);
				
				full.getMainVertex(i,j).setPos(V1);
				full.getMainVertex(i+1,j).setPos(V2);
				full.getMainVertex(i+1,j+1).setPos(V3);
				full.getMainVertex(i,j+1).setPos(V4);
				full.getSecondaryVertex(i,j).setPos(VC);				
			}
		}

		// Creation of mesh's triangles
		full.createTriangles();
				
		world.addElement(e);
		world.setBackgroundColor(new Color(75,0,150));
		
		System.out.println("********* Calculating normals");
		world.generate();
		
		DirectionalLight dl = new DirectionalLight(new Vector3(1,1,1), 0.5f);
		AmbientLight al = new AmbientLight(0.5f);
		Lighting light = new Lighting(dl, al);
		
		GraphicContext gContext = new GraphicContext(0.8f, 0.45f, 1, 100, GraphicContext.PERSPECTIVE_TYPE_FRUSTUM, 1250);
		View view = test.createView(gContext);

		//RenderContext rContext = new RenderContext(RenderContext.RENDER_DEFAULT_ALL_ENABLED);
		RenderContext rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE);
		rContext.setTextureProcessing(RenderContext.TEXTURE_PROCESSING_ENABLED);
		//rContext.setDisplayNormals(RenderContext.DISPLAY_NORMALS_ENABLED);
		//rContext.setDisplayLandmark(RenderContext.DISPLAY_LANDMARK_ENABLED);
		//rContext.setRenderingLines(RenderContext.RENDERING_LINES_ENABLED);

		//rContext.setRendering(RenderContext.RENDERING_TYPE_INTERPOLATE);
		
		RenderEngine renderer = new RenderEngine(world, light, camera, rContext, gContext);
		renderer.setView(view);
		renderer.render();


		System.out.println("********* Rendering...");
		int nb_images = 180;
		for (int i=0; i<=3*nb_images; i++) {
		//for (int i=0; i<=3; i++) {
			Rotation r = new Rotation((float)Math.PI*2*(float)i/(float)nb_images, Vector3.Z_AXIS);
			e.setTransformation(r);
			renderer.render();
		}

		System.out.println("********* ENDING APPLICATION *********");
	}
}
