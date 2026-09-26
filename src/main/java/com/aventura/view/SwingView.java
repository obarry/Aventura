package com.aventura.view;

import java.awt.Component;
import java.awt.image.BufferedImage;

import com.aventura.context.PerspectiveContext;

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
* Swing specialization of ImageView: same double buffer images, plus a repaint of the associated
* Swing Component after each rendered frame. The component's paint method just draws getImageView():
* 
*     graphics.drawImage(view.getImageView(), 0, 0, null);
* 
* A SwingView created without a Component behaves exactly like an ImageView (off-screen rendering);
* prefer ImageView in that case, which does not suggest a Swing dependency.
* 
*       Warning! SWING Graphic coords on screen are as follows:
*    
*                      |
*                      |
*                 -----+-----> X
*                      |
*                      |
*                      v
*                      
*                      Y
* 
* The conversion from Aventura's centered, Y axis up coordinates is done by ImageView.
*/

public class SwingView extends ImageView {

	// Swing component to which this SwingView is associated. Used to pro-actively repaint when needed.
	protected Component component = null;
	
	public SwingView(PerspectiveContext context) {
		super(context);
	}

	public SwingView(int width, int height) {
		super(width, height);
	}

	public SwingView(PerspectiveContext context, Component comp) {
		super(context);
		this.component = comp;
	}

	public SwingView(int width, int height, Component comp) {
		super(width, height);
		this.component = comp;
	}
	
	/**
	 * Associates (or replaces, or removes with null) the Swing Component to repaint after each frame.
	 */
	public void setComponent(Component comp) {
		this.component = comp;
	}
	
	public Component getComponent() {
		return component;
	}

	/**
	 * After each swap: notifies the frame listener (if any) then requests a repaint of the component (if any).
	 */
	@Override
	protected void frameRendered(BufferedImage image) {
		super.frameRendered(image);
		if (component != null) {
			component.repaint();
		}
	}

}
