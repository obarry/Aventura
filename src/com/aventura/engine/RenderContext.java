package com.aventura.engine;

/**
 * 
 * The RenderContext describes the information and parameters to be used by the RenderEngine to render the World properly
 * This is all elements not related to the World, the Lighting or the Camera.
 * It can be to force the rendering to be plain or lines, to use or not textures, etc...
 * This can also allow future evolutions like selecting using a HW graphic acceleration
 * 
 * The RenderContext is passed as a parameter of the RenderEngine before asking him to render the World 
 * 
 * The application may create Several RenderContex instances to render differently a same World.
 * Several default contexts should be accessible as constants to perform standard rendering.
 * 
 * The RenderContext should remain as independent as possible on the display and windowing technology (e.g. Swing or SWT).
 * Another class (GraphicContext) should handle these specifics.
 * 
 * @author Bricolage Olivier
 * @since May 2016
 *
 */
public class RenderContext {
	
	public static RenderContext RENDER_DEFAULT = new RenderContext();

}
