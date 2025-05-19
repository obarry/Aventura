package com.aventura.test;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import com.aventura.context.PerspectiveContext;
import com.aventura.view.SwingView;
import com.aventura.view.GUIView;
import com.aventura.view.MapView;

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
* This class is a Test class to test the MapView
*/

public class TestView2 {
	
	// Create the gUIView to be displayed
	private SwingView view;
	JFrame frame;
	
	public GUIView createView(PerspectiveContext context) {

		// Create the frame of the application 
		frame = new JFrame("Test Aventura");
		// Set the size of the frame
		frame.setSize(800,500);
		
		// Create the gUIView to be displayed
		view = new SwingView(context, frame);
		
		// Create a panel and add it to the frame
		JPanel panel = new JPanel() {
			
		    public void paintComponent(Graphics graph) {
				System.out.println("Painting JPanel");		    	
		    	//Graphics2D graph2D = (Graphics2D)graph;
		    	//TestView.this.view.draw(graph);
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
		
	public static void main(String[] args) {
		
		TestView2 test = new TestView2();
		
		int size_x = 750;
		int size_y = 450;
		
		MapView mapView = new MapView(size_x,size_y);
		for (int i=0; i<mapView.getViewWidth(); i++) {
			for (int j=0; j<mapView.getViewHeight(); j++) {
				mapView.set(i, j, (float)Math.random()*i*j/(size_x*size_y));
			}
		}

		GUIView gUIView = test.createView(new PerspectiveContext());
		gUIView.initView(mapView);
		gUIView.renderView();
		
	}

}
