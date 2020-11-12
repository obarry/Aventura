package com.aventura.model.world.shape;

import java.awt.Color;

import com.aventura.math.transform.Translation;
import com.aventura.math.vector.Vector3;
import com.aventura.model.texture.Texture;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2020 Olivier BARRY
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
 * A Cylinder, closed at top and bottom like a can
 * 
 * @author Olivier BARRY
 * @since March 2018
 */

public class ClosedCylinder extends Cylinder {
	
	protected static final String CLOSED_CYLINDER_DEFAULT_NAME = "closed cylinder";

	protected Disc top, bottom = null;

	public ClosedCylinder(float height, float ray, int half_seg) {
		super(height, ray, half_seg);
		this.isClosed = true;
		this.name = CLOSED_CYLINDER_DEFAULT_NAME;
		createSubElements();
	}
	
	public ClosedCylinder(float height, float ray, int half_seg, Texture tex) {
		super(height, ray, half_seg, tex);
		this.isClosed = true;
		this.name = CLOSED_CYLINDER_DEFAULT_NAME;
		createSubElements();
	}
	
	protected void createSubElements() {
		top = new Disc(ray, half_seg);
		Translation t_top = new Translation(Vector3.Z_AXIS, height/2);
		top.setTransformation(t_top);
		
		bottom = new Disc(ray, half_seg);
		Translation t_bottom = new Translation(Vector3.Z_AXIS, -height/2);
		bottom.setTransformation(t_bottom);
		
		this.addElement(top);
		this.addElement(bottom);
		
	}
	
	public void createGeometry() {
		
		// Rely on superclass to generate the cylinder
		super.createGeometry();		
	}
	
	@Override
	public Element setTopTexture(Texture tex) {
		this.top.setFrontTexture(tex);
		return this;
	} 

	@Override
	public Element setBottomTexture(Texture tex) {
		// TODO Auto-generated method stub
		this.bottom.setFrontTexture(tex);
		return this;
	}

	@Override
	public Element setTopColor(Color c) {
		// TODO Auto-generated method stub
		this.top.setColor(c);
		return this;
	}

	@Override
	public Element setBottomColor(Color c) {
		// TODO Auto-generated method stub
		this.bottom.setColor(c);
		return this;
	}

	@Override
	public void calculateNormals() {
		super.calculateNormals();
	}
}
