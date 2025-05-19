package com.aventura.test;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import com.aventura.context.PerspectiveContext;
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
* This class is a Test class to test the GUIView
*/

public class TestView {
	
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
		
		TestView test = new TestView();

		GUIView gUIView = test.createView(new PerspectiveContext());

		for (int i=0; i<20; i++) {

			// Test of the gUIView
			gUIView.initView();
			gUIView.setColor(Color.WHITE);
			gUIView.drawLine(-250, -100, 250, 100);
			gUIView.drawLine(-250, 100, 250, -100);
			gUIView.setColor(Color.RED);
			gUIView.drawLine(-250, 0, 250, 0);
			gUIView.setColor(Color.BLUE);
			gUIView.drawLine(0, -100, 0, 100);
			gUIView.setColor(Color.YELLOW);
			gUIView.drawLine(-250, 50, 250, 75);
			gUIView.drawLine(-250, -50, 250, -25);
			gUIView.renderView();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// Test of the gUIView
			gUIView.initView();
			gUIView.setColor(Color.GREEN);
			gUIView.drawLine(-250, -100, 250, 100);
			gUIView.drawLine(-250, 100, 250, -100);
			gUIView.setColor(Color.WHITE);
			gUIView.drawLine(-250, 0, 250, 0);
			gUIView.setColor(Color.BLUE);
			gUIView.drawLine(0, -100, 0, 100);
			gUIView.setColor(Color.CYAN);
			gUIView.drawLine(50, -100, 75, 100);
			gUIView.drawLine(-50, -100, -25, 100);
			gUIView.renderView();
			//test.frame.repaint();
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
