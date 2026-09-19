package com.aventura.model.world.shape;

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
 * This interface provides methods related to behavior "Generaable" of the Element's geometry
 * Implementing this behavior will become generioc to all Elements allowing to generate World's geometry but also update it.
 * 
 * @author Olivier BARRY
 * @since Nov 2023
 */

public interface Generable {

	// High-level services of the interfaces
	
	// First generation of the geometry
	public void build();
	// Update previously generated geometry. Needed if the Vertices have been changed. Triangles will be regenerated
	public void rebuild();

	// Low-level services of the interface
	
	// Generate vertices as they are hard coded in each Element based on the parameters provided through Constructors (first time generation)
	// Called by build()/rebuild() before generateTriangles() -- an implementation of generateTriangles()
	// may assume this has already run and populated whatever fields it needs (e.g. an intermediate mesh
	// helper); calling generateTriangles() directly, without going through build()/rebuild() first, is
	// not supported and will typically fail with a NullPointerException rather than a clear error.
	public void generateVertices();
	// Generate the Triangles after Vertices are created or re-generate the Triangles after Vertices have been updated
	public void generateTriangles();
	// Calculate Normals of Triangles. Needed to be recalculated after a Transformation.
	//
	// CONTRACT: an implementation computes normals ONLY for this Element's own Triangles. Recursion into
	// sub-Elements is handled exclusively by build()/rebuild() (via Element.subBuild()/subRebuild(), which
	// call each sub-Element's own build()/rebuild() and therefore its own calculateNormals()) -- an
	// override must NEVER also call Element.calculateSubNormals() or otherwise recurse into subelements
	// itself, since that would run the sub-Elements' normal calculation twice. (Before 2026 this was
	// inconsistent across the built-in shape classes -- some called it, one had it commented out -- which
	// is why this rule is now spelled out here instead of left to each implementation to decide.)
	public void calculateNormals();


}
