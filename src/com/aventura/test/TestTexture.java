package com.aventura.test;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;


public class TestTexture {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		JFrame frame = new JFrame("TestTexture");
		
		// Set the size of the frame
		frame.setSize(500,500);
		
		// Create a panel and add it to the frame
		JPanel panel = new JPanel() {

			private Image image = new ImageIcon("resources/test/texture_bricks_204x204.jpg").getImage(); 

			public void paint(Graphics g) { 
		        System.out.println("Height: "+image.getHeight(null));
		        System.out.println("Width: "+image.getWidth(null));
				g.drawImage(image, 0, 0, image.getHeight(null), image.getWidth(null), null); 
				g = getGraphics(); 
			} 
		};
		
		frame.getContentPane().add(panel);
		
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
 
		// Locate application frame in the center of the screen
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setLocation(dim.width/2 - frame.getWidth()/2, dim.height/2 - frame.getHeight()/2);
		
		// Render the frame on the display
		frame.setVisible(true);

	}

}
