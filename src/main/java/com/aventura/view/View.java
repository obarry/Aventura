package com.aventura.view;

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
* View is the root (abstract) class of anything having a pixel width and height that can be initialized:
* - GUIView (and its implementations ImageView, SwingView): the images the RenderEngine draws into;
* - MapView: a 2D map of float values (depth buffer, shadow map...), which can itself be displayed in
*   a GUIView (GUIView.initView(MapView)).
* 
*/
public abstract class View {
	
	protected int width;
	protected int height;
	
	public View() {
		// Do nothing
	}
	
	public int getViewWidth() {
		return width;
	}
	
	public int getViewHeight() {
		return height;
	}
	
	public void setDimensions(int width, int height) {
		this.width  = width;
		this.height = height;
	}
	
	public abstract void initView();
	public abstract void initView(int width, int height); // change dimensions and initView

}
