package com.aventura.view;

import java.awt.Graphics;

/**
* ------------------------------------------------------------------------------ 
* MIT License
* 
* Copyright (c) 2016 Olivier BARRY
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
*/
public class SwingView extends View {

	@Override
	public void drawPixel(int x, int y) {
		// TODO Auto-generated method stub

	}

	@Override
	public void drawLine(int x1, int y1, int x2, int y2) {
		// TODO Auto-generated method stub

	}
	
	/**
	 * This is "THE" method triggering all the rendering !!!
	 * It is called by the JPanel while this component is repainting (the method paintComponent() is called)
	 * As we initialized the JPanel with a View (actually a SwingView as this is a Swing GUI), then all the
	 * Aventura related processing is done by the API implementation, using the provided Graphics context: graph
	 * (Graphics -> Graphics2D can be easily extracted).
	 * 
	 * @param graph, the Swing interface graphic context to be used to display in the frame/panel
	 */
	public void draw(Graphics graph) {
		// TODO To be implemented
	}
	

}
