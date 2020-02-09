package com.aventura.demo;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JFrame;

public class KeyFrame extends JFrame implements KeyListener {
	
	private MovingCamera camera;

	public KeyFrame(MovingCamera camera, String string) {
		// TODO Auto-generated constructor stub
		super(string);
		this.camera = camera;
	    addKeyListener(this);
	}

	@Override
	public void keyPressed(KeyEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void keyTyped(KeyEvent evt) {
		// TODO Auto-generated method stub
		if (evt.getKeyChar() == '8') {
			System.out.println("Key typed : 8");
			camera.rotateCameraUp();
		} else if (evt.getKeyChar() == '2') {
			System.out.println("Key typed : 2");
			camera.rotateCameraDown();
		} else if (evt.getKeyChar() == '4') {
			System.out.println("Key typed : 4");
			camera.rotateCameraLeft();
		} else if (evt.getKeyChar() == '6') {
			System.out.println("Key typed : 6");
			camera.rotateCameraRight();
		} else if (evt.getKeyChar() == '5') {
			System.out.println("Key typed : 5");
			camera.moveCameraFront();
		} else if (evt.getKeyChar() == '0') {
			System.out.println("Key typed : 0");
			camera.moveCameraBack();
		} else {
			System.out.println("Other Key typed");			
		}
		//repaint();

	}

}
