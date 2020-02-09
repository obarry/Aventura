package com.aventura.demo;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JFrame;

public class KeyFrame extends JFrame implements KeyListener {

	public KeyFrame(String string) {
		// TODO Auto-generated constructor stub
		super(string);
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
		if (evt.getKeyChar() == 'r') {
			//ardoise.setForeground(Color.RED);
			System.out.println("Key typed : r");
		} else if (evt.getKeyChar() == 'b') {
				//ardoise.setForeground(Color.BLUE);
			System.out.println("Key typed : b");
		} else if (evt.getKeyChar() == 'g') {
					//ardoise.setForeground(Color.GREEN);
			System.out.println("Key typed : g");
		} else if (evt.getKeyChar() == 'e') {
			System.out.println("Key typed : e");
						//ardoise.setForeground(ardoise.getBackground());
		} else {
			System.out.println("Other Key typed");			
		}
		//repaint();

	}

}
