package com.aventura.demo;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JFrame;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2016-2025 Olivier BARRY
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
 * This class is the JFrame container for the MovingCamera demo application allowing using keyboard interaction to move
 * camera through the 3D landscape.
 */

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
