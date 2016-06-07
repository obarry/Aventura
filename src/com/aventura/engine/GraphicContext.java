package com.aventura.engine;

/**
 * 
 * @author Bricolage Olivier
 * @since May 2016
 *
 */
public class GraphicContext {
	
	public static final int PERSPECTIVE_TYPE_FRUSTUM = 1;
	public static final int PERSPECTIVE_TYPE_ORTHOGRAPHIC = 2;


	// Window & frustum
	int width;
	int height;
	
	int perspective_type = 0;
	
	public GraphicContext() {
		
	}
	
	public GraphicContext(int width, int height, int type) {
		this.width = width;
		this.height = height;
		this.perspective_type = type;
	}
	
	public static GraphicContext GRAPHIC_DEFAULT = new GraphicContext(800,450,PERSPECTIVE_TYPE_FRUSTUM);
	

}
