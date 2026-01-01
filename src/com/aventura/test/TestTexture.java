package com.aventura.test;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import com.aventura.model.texture.Texture;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2016-2026 Olivier BARRY
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
 */

public class TestTexture {

	private static Image image1; 
	//private static Image image2;
	private static BufferedImage image2;

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		JFrame frame = new JFrame("TestTexture");
		
		// Set the size of the frame
		frame.setSize(1000,500);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		 
		// Locate application frame in the center of the screen
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setLocation(dim.width/2 - frame.getWidth()/2, dim.height/2 - frame.getHeight()/2);
		
		// Loading image in order to display the original source
		//image1 = new ImageIcon("resources/texture/texture_damier_600x591.gif").getImage();
		//image1 = new ImageIcon("resources/texture/texture_blueground_204x204.jpg").getImage();
		image1 = new ImageIcon("resources/texture/texture_bricks_204x204.jpg").getImage();
		int width1 = image1.getWidth(null);
		int height1 = image1.getHeight(null);
        System.out.println("Height image1: "+image1.getHeight(null));
        System.out.println("Width image1: "+image1.getWidth(null));
		
		// Let's now create the Texture object itself, from the same image file (to be selected) 
		//Texture tex = new Texture("resources/texture/texture_damier_600x591.gif");
		//Texture tex = new Texture("resources/texture/texture_blueground_204x204.jpg");
		Texture tex = new Texture("resources/texture/texture_bricks_204x204.jpg");
		
		// This ratio can be adjusted to show the effect of redimensioning the initial image by the Texture object (bilinear filtering algorithm)
		float ratio = 1.3f;
		
		int width2 = (int) (width1*ratio);
		int height2 = (int) (height1*ratio);
		image2 = new BufferedImage(width2, height2, BufferedImage.TYPE_INT_RGB);
        System.out.println("Height image2: "+image2.getHeight(null));
        System.out.println("Width image2: "+image2.getWidth(null));
		
		for (int i=0; i<width2; i++) {
			for (int j=0; j<height2; j++) {
			float u = (float)i/width2;
			float v = (float)j/height2;
				try{
					image2.setRGB(i, j, tex.getInterpolatedColor(u, v).getRGB());
				} catch (Exception e) {
					System.out.println("Exception while interpolating. i="+i+" j="+j+" u="+u+" v="+v);
					System.out.println(e);
				}
			}
		}

		// Create a panel and add it to the frame
		JPanel panel = new JPanel() {
			public void paint(Graphics g) { 
				g.drawImage(image1, 0, 0, image1.getHeight(null), image1.getWidth(null), null);
				g.drawImage(image2, 256, 0, image2.getHeight(null), image2.getWidth(null), null);
				g = getGraphics(); 
			} 
		};
		
		frame.getContentPane().add(panel);
		
		// Render the frame on the display
		frame.setVisible(true);
	}

}
