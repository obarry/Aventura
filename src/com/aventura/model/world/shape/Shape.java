package com.aventura.model.world.shape;

import java.awt.Color;

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
 * This interface provides generic method signatures for all Elements
 * Each specific element should implement all or part of these methods, depending on its shape.
 * 
 * The notion of top (+Z), bottom (-Z), left (-Y), right (+Y), front (+X) and back (-X) is valid for the Element before any transformation (rotation)
 * 
 * The method should return the Element itself (this) or null if not implemented because meaningless for this Element.
 * 
 * @author Olivier BARRY
 * @since Mar 2018
 */

public interface Shape {
	
	// Setting a specific Texture to each standard face of an Element
	public abstract Element setTopTexture(Texture tex);
	public abstract Element setBottomTexture(Texture tex);
	public abstract Element setLeftTexture(Texture tex);
	public abstract Element setRightTexture(Texture tex);
	public abstract Element setFrontTexture(Texture tex);
	public abstract Element setBackTexture(Texture tex);
	
	// Setting a specific Color to each standard face of an Element
	public abstract Element setTopColor(Color c);
	public abstract Element setBottomColor(Color c);
	public abstract Element setLeftColor(Color c);
	public abstract Element setRightColor(Color c);
	public abstract Element setFrontColor(Color c);
	public abstract Element setBackColor(Color c);
	
}
