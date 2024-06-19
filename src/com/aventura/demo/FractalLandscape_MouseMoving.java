package com.aventura.demo;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.event.*;

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
import com.aventura.model.world.WrongArraySizeException;
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
 * 
 * It generates a landscape through a Treillis material formed through a Fractal recursion with random variations to create the ground
 * Goal is to progressively build a complete application on a typical 3D graphic use case
 * 
 * @author Olivier BARRY
 * @since Nov 2023
 */

/**
 * @author obarry
 *
 */
public class FractalLandscape_MouseMoving implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener, ActionListener {
	
	// GUI Frame and Menus
	JFrame frame;
	JMenu menu1, menu2;
	JMenuItem m1e1, m1e2, m2e1, m2e2, m2e3;
	boolean texture_menu = false;
	boolean shading_menu = true;

	// Camera
	Vector4 eye;
	Vector4 poi;
	Camera camera;
	
	// Render engine
	RenderContext rContext;
	RenderEngine renderer;
	
	// World
	World world;
	
	// Trellis element
	Trellis tre;
	float size; // Size of the Treillis (square)
	int n; // Nb of segments of the Treillis, should be a 2^n number
	float array_land[][]; // Updated array with sea level

	// Movement variables
	int i_rotation = 0;
	int j_rotation = 0;
	int zoom = 0;
	static int NB_ROTATIONS = 90;
	
	int mouse_click_X, mouse_click_Y;
	int mouse_dragged_X = 0;
	int mouse_dragged_Y = 0;
	
	// CTRL key + mouse flag
	boolean key_control = false;

	/**
	 * Create the gUIView and associate all needed mouse and key listeners for user interaction with screen
	 * The renderer will then be called each time Mouse generates a move to the gUIView and a new updated gUIView will be displayed
	 * 
	 * @param context the GraphicContext to be used to create the GUIView
	 * @return
	 */
	public GUIView createView(GraphicContext context) {
		
		// Create the frame of the application 
		frame = new JFrame("Fractal Landscape");
		// Set the size of the frame
		frame.setSize(1000,600);
		
		// Create Menus and associate the menu items to appropriate listeners
	    JMenuBar menubar = new JMenuBar();
	    
	    // Create menu1
	    menu1 = new JMenu("Run");
	    m1e1 = new JMenuItem("Generate");
	    m1e1.addActionListener(this);
	    m1e2 = new JMenuItem("Texture off");
	    m1e2.addActionListener(this);
	    menu1.add(m1e1); 
	    menu1.add(m1e2); 
	    menubar.add(menu1);
	    
	    // Create menu2
	    menu2 = new JMenu("Rendering");
	    m2e1 = new JMenuItem("Lines off");
	    m2e1.addActionListener(this);
	    m2e2 = new JMenuItem("Plain off");
	    m2e2.addActionListener(this);
	    m2e3 = new JMenuItem("Shading on");
	    m2e3.addActionListener(this);
	    menu2.add(m2e1);
	    menu2.add(m2e2);
	    menu2.add(m2e3);
	    menubar.add(menu2);

	    // Add menu bar to the frame
	    frame.setJMenuBar(menubar);

		
		// Create the gUIView to be displayed
		SwingView view = new SwingView(context, frame);
		
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
		
        //where initialization occurs:
        //Register for mouse events on blankArea and the panel.
        panel.addMouseListener(this);
        frame.addKeyListener(this);
        panel.addMouseMotionListener(this);
        panel.addMouseWheelListener(this);
 		
		return view;
	}
	
	public void keyTyped(KeyEvent e) {
		// Do nothing
	}

	public void keyPressed(KeyEvent e) {
	    if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
	    	key_control = true;
	    }
	}
 
	public void keyReleased(KeyEvent e) {
	    if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
	    	key_control = false;
	    }
	}
	
	public void mousePressed(MouseEvent e) {
		// Store the location when mouse is clicked, it will be used during mouse is dragged to calculate the x and y variations
		// and rotate the Element accordingly3
		mouse_click_X = e.getX(); mouse_click_Y = e.getY();
	}

     public void mouseEntered(MouseEvent e) {
    	 // Do nothing
	 }

	public void mouseReleased(MouseEvent e) {
		// Do nothing
     }

     public void mouseExited(MouseEvent e) {
 		// Do nothing
     }

     public void mouseClicked(MouseEvent e) {
 		// Do nothing
     }

 	public void mouseDragged(MouseEvent e) {
        mouse_dragged_X += e.getX() - mouse_click_X;
        mouse_dragged_Y += e.getY() - mouse_click_Y;
        //System.out.println("Drag X: " + mouse_dragged_X + " Drag Y: " + mouse_dragged_Y);
        
        Rotation rz = new Rotation((float)Math.PI*(float)mouse_dragged_X/frame.getWidth()/8, Vector3.Z_AXIS);
        Rotation ry = new Rotation((float)Math.PI*(float)mouse_dragged_Y/frame.getHeight()/16, Vector3.Y_AXIS);
        tre.setTransformation(ry.times(rz));
        //tre.combineTransformation(ry.times(rz));

        // Render the updated gUIView after zooming camera and rotating Element
		renderer.render();
 	}
 	
 	public void mouseMoved(MouseEvent e) {
		// Do nothing
 	} 	

	public void mouseWheelMoved(MouseWheelEvent e) {
		//System.out.println("Mouse wheel moved: " + e.getWheelRotation());
		// Increment or decrement zoom based on wheel rotation (+1 or -1)
		zoom-=e.getWheelRotation();
        // Zoom camera by updating eye on the forward direction
        camera.updateCamera(eye.plus(camera.getForward().times((float)zoom/10)), poi, camera.getUp());
        
        // Render the updated gUIView after zooming camera and rotating Element
		renderer.render();
	}
	

	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub

		if (e.getSource() == m1e1) { // Generate
			createLandscape(array_land, size, n);
			try {
				tre.updateTrellis(array_land);
			} catch (WrongArraySizeException exc) {
				// TODO Auto-generated catch block
				exc.printStackTrace();
			}
			world.update();
			System.out.println("Number of Triangles in Trellis: "+tre.getNbTriangles()+" Number of vertices: " + tre.getNbVertices());
			updateTrianglesColorTrellis();
			renderer.render();
		} else if (e.getSource() == m1e2) { // Texture on/off (toggle)
			
			if (shading_menu) {
				if (texture_menu) {
					rContext.setTextureProcessing(RenderContext.TEXTURE_PROCESSING_DISABLED);
					renderer.render();
					m1e2.setText("Texture off");
					texture_menu = false;
				} else {
					rContext.setTextureProcessing(RenderContext.TEXTURE_PROCESSING_ENABLED);
					renderer.render();
					m1e2.setText("Texture on");
					texture_menu = true;
				}
			} else {
				// no action
			}
		} else if (e.getSource() == m2e1) { // Rendering lines
			rContext.setRenderingType(RenderContext.RENDERING_TYPE_LINE);
			renderer.render();
			m2e1.setText("Lines on");
			m2e2.setText("Plain off");
			m2e3.setText("Shading off");
			m1e2.setEnabled(false);
			shading_menu = false;
		} else if (e.getSource() == m2e2) { // Rendering Plain
			rContext.setRenderingType(RenderContext.RENDERING_TYPE_PLAIN);
			renderer.render();
			m2e1.setText("Lines off");
			m2e2.setText("Plain on");
			m2e3.setText("Shading off");
			m1e2.setEnabled(false);
			shading_menu = false;
			
		} else if (e.getSource() == m2e3) { // Rendering Shading
			rContext.setRenderingType(RenderContext.RENDERING_TYPE_INTERPOLATE);
			renderer.render();
			m2e1.setText("Lines off");
			m2e2.setText("Plain off");
			m2e3.setText("Shading on");
			m1e2.setEnabled(true);
			shading_menu = true;
		} else {
			System.out.println("Other Action Event : "+e);			
		}
	}
	
	/**
	 * @param size the size of the trellis
	 * @param n the number of segments (should be a power of 2)
	 */
	public void createLandscape(float[][] array_land, float size, int n) {
		
		float array[][] = new float[n+1][n+1]; // Temporary array to be used for the Treillis generation
		
		float mult = 0.6f; // Global multiplication factor for altitudes used by the Fractal generator. If higher, the landscape will be more accidented, if lower it will be smoother

		int ix = n; // Start from the largest dimension of the Treillis
		// Initialize the 4 corners
		array[0][0] = (float)Math.random()*mult*size/2;
		array[0][ix] = (float)Math.random()*mult*size/2;
		array[ix][0] = (float)Math.random()*mult*size/2;
		array[ix][ix] = (float)Math.random()*mult*size/2;
		
		// Recursion loop on i that will be divided by 2 at each iteration until it is equal to 1
		while (ix>1) {
			
			float factor = (float)(mult*(ix * size)/(n * 2)); // For each loop calculate the variation factor (should reduce at each Fractal iteration)
			// Then at each stage let's loop on sub-squares of the main square
			for (int j=0; j<n/ix; j++) { // n/i will be 1 (i=n at first iteration), then 2 (i is divided by 2), 4, 8, 16...
				for (int k=0; k<n/ix; k++) {
					
					// Let's calculate the average altitude of the center of the square as it will be used later
					float center = (array[0+j*ix][0+k*ix] + array[0+j*ix][ix+k*ix] + array[ix+j*ix][0+k*ix] + array[ix+j*ix][ix+k*ix])/4;
					
					// Then use Fractal approach to calculate the center of the square and the middle of each segment of the squares
					//
					// Co---M---Co
					// |    |    |
					// |    |    |
					// M---Cnt---M
					// |    |    |
					// |    |    |
					// Co---M---Co
					//
					// Co = Corners of the square
					// Cnt = Center of the square
					// M = Middle of each segment of the square
					
					// For each calculation, add a random value multiplied by the loop factor calculated above (hence proportional to the size of the square)
					array[ix/2+j*ix][ix/2+k*ix] = center + (float)Math.random()*factor;
					// Use average of other points : 2 Corners + Center of the square for the middle segments
					array[ix/2+j*ix][0+k*ix] = (array[0+j*ix][0+k*ix] + array[ix+j*ix][0+k*ix] + center)/3 + (float)Math.random()*factor;;
					array[0+j*ix][ix/2+k*ix] = (array[0+j*ix][0+k*ix] + array[0+j*ix][ix+k*ix] + center)/3 + (float)Math.random()*factor;;
					array[ix+j*ix][ix/2+k*ix] = (array[ix+j*ix][0+k*ix] + array[ix+j*ix][ix+k*ix] + center)/3 + (float)Math.random()*factor;;
					array[ix/2+j*ix][ix+k*ix] = (array[0+j*ix][ix+k*ix] + array[ix+j*ix][ix+k*ix] + center)/3 + (float)Math.random()*factor;;
				}
			}
			ix/=2; // Divide i by 2 (i remains a power of 2)
		}
		
		//
		// End of Fractal generation
		//
		
		float sea_level = 1.5f;
		//float array_land[][] = new float[n+1][n+1]; // Updated array with sea level
		
		for (int i=0; i<=n; i++ ) {
			for (int j=0; j<=n; j++) {
				array_land[i][j] = array[i][j] >= sea_level ? array[i][j] : sea_level ;
			}
		}
		
		array = null; // We no longer need this one, the array_land has been updated appropriately
		
	}
	
	protected void updateVertexColorTrellis( ) {
		float max_alt = tre.getMaxZ();
		float min_alt = tre.getMinZ();
		for (int v=0; v<tre.getVertices().size(); v++) {
			float v_alt = tre.getVertex(v).getPos().getZ();
			tre.getVertex(v).setColor(new Color((int)((v_alt-min_alt)/(max_alt-min_alt)*200),(int)((v_alt-min_alt)/(max_alt-min_alt)*255),(int)((max_alt-v_alt)/(max_alt-min_alt)*200)));
		}
		
	}

	protected void updateTrianglesColorTrellis() {
		float max_alt = tre.getMaxZ();
		float min_alt = tre.getMinZ();
		int t = 0;
		for (t=0; t<tre.getTriangles().size(); t++) {
			float avg_alt = (tre.getTriangle(t).getV1().getPos().getZ() + tre.getTriangle(t).getV2().getPos().getZ() + tre.getTriangle(t).getV3().getPos().getZ())/3;
			tre.getTriangle(t).setColor(new Color((int)((avg_alt-min_alt)/(max_alt-min_alt)*200),(int)((avg_alt-min_alt)/(max_alt-min_alt)*255),(int)((max_alt-avg_alt)/(max_alt-min_alt)*200)));
		}
	}


	/**
	 * Create the World and Camera, generate a Treillis based on Fractal recursivity to create a Landscape
	 * Create the Graphic Swing gUIView by calling the createView method that will associate mouse Listners to the panel so that user can 
	 * interact with the gUIView and move it or zomm in it.
	 */
	public void run() {
		
		// Camera
		//Vector4 eye = new Vector4(8,3,5,1);
		eye = new Vector4(6,0,3,1);
		//Vector4 eye = new Vector4(16,6,12,1);
		//Vector4 eye = new Vector4(3,2,2,1);
		poi = new Vector4(0,0,1.5f,1);
		camera = new Camera(eye, poi, Vector4.Z_AXIS);
				
		System.out.println("********* Creating World");
		
		//Texture tex = new Texture("resources/texture/texture_bricks_204x204.jpg");
		//Texture tex = new Texture("resources/texture/texture_blueground_204x204.jpg");
		//Texture tex = new Texture("resources/texture/texture_woodfloor_160x160.jpg");
		//Texture tex = new Texture("resources/texture/texture_damier_600x591.gif");
		//Texture tex = new Texture("resources/texture/texture_grass_900x600.jpg");
		Texture tex = new Texture("resources/texture/texture_ground_stone_600x600.jpg");
		//Texture tex = new Texture("resources/texture/texture_snow_590x590.jpg");
		
		// Create World
		world = new World();
		
		// Create and form the Treillis
		size = 4; // Size of the Treillis (square)
		n = 128; // Nb of segments of the Treillis, should be a 2^n number
		array_land = new float[n+1][n+1]; // Updated array with sea level
		createLandscape(array_land, size, n);
		// Create the Treillis
		tre = null;
		try {
			tre = new Trellis(size, size, n, n, array_land, tex);
		} catch (WrongArraySizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//tre.setColor(new Color(100,200,50));
		tre.setSpecularExp(8);		
		
		// Add the Treillis as a new Element of the World
		world.addElement(tre);
		
		// Generate World (including Triangles)
		System.out.println("********* Calculating normals");
		world.generate();
		System.out.println("Number of Triangles in Trellis: "+tre.getNbTriangles()+" Number of vertices: " + tre.getNbVertices());
		
		// Then update Triangles as needed (Colors) to create effect on the Landscape
		updateTrianglesColorTrellis();
//		updateVertexColorTrellis();
//		world.update();

		// Lighting initialization
		DirectionalLight dl = new DirectionalLight(new Vector3(1,-1,1), 0.7f);
		AmbientLight al = new AmbientLight(0.3f);
		Lighting light = new Lighting(dl, al, false);
		
		// Graphic Context
		GraphicContext gContext = new GraphicContext(0.8f, 0.45f, 0.8f, 100, GraphicContext.PERSPECTIVE_TYPE_FRUSTUM, 1250);
		//GraphicContext gContext = new GraphicContext(8f, 4.5f, 1, 100, GraphicContext.PERSPECTIVE_TYPE_ORTHOGRAPHIC, 125);
		
		// Create gUIView
		GUIView gUIView = this.createView(gContext);

		// Rendering context
		//rContext = new RenderContext(RenderContext.RENDER_DEFAULT);
		rContext = new RenderContext(RenderContext.RENDER_STANDARD_INTERPOLATE);
		rContext.setTextureProcessing(RenderContext.TEXTURE_PROCESSING_DISABLED);
		//rContext.setDisplayNormals(RenderContext.DISPLAY_NORMALS_ENABLED);
		//rContext.setDisplayLandmark(RenderContext.DISPLAY_LANDMARK_ENABLED);

		//rContext.setRendering(RenderContext.RENDERING_TYPE_INTERPOLATE);
		
		// Initialize Render Engine and render a first gUIView
		renderer = new RenderEngine(world, light, camera, rContext, gContext);
		renderer.setView(gUIView);
		renderer.render();
		
		System.out.println("********* APPLICATION LAUNCHED *********");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		System.out.println("********* STARTING APPLICATION *********");
		FractalLandscape_MouseMoving fractal = new FractalLandscape_MouseMoving();
		fractal.run();
		System.out.println("********* APPLICATION is RUNNING *********");
	}

}
