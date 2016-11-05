package com.aventura.engine;

import java.awt.Color;

import com.aventura.context.GraphicContext;
import com.aventura.model.world.Line;
import com.aventura.model.world.Triangle;
import com.aventura.model.world.Vertex;
import com.aventura.view.View;

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
 * This class represents and manages the transformations from model to view.
 * 
 * 
 * @author Bricolage Olivier
 * @since November 2016
 *
 */

public class Rasterizer {
	
	private GraphicContext graphic;
	private View view;
	
	public Rasterizer(GraphicContext graphic) {
		this.graphic = graphic;
	}
	
	public void setView(View v) {
		this.view = v;
	}
	
	// These methods should be later encapsulated in a dedicated class -> e.g. RenderView
	// To Be Encapsulated
	//
	
	protected void drawTriangleLines(Triangle t) {

		//if (Tracer.function) Tracer.traceFunction(this.getClass(), "drawTriangleLines(t)");
		//if (Tracer.info) Tracer.traceInfo(this.getClass(), "Drawing triangle. "+ t);
		
		view.setColor(Color.WHITE);
		drawLine(t.getV1(), t.getV2());
		drawLine(t.getV2(), t.getV3());
		drawLine(t.getV3(), t.getV1());
	}
	
	protected void drawLine(Line l) {
		//if (Tracer.function) Tracer.traceFunction(this.getClass(), "drawLine(l)");
		//if (Tracer.info) Tracer.traceInfo(this.getClass(), "Drawing Line. "+ l);

		drawLine(l.getV1(), l.getV2());
	}
	
	protected void drawLine(Vertex v1, Vertex v2) {

		//if (Tracer.function) Tracer.traceFunction(this.getClass(), "drawLine(v1,v2)");
		
		int x1, y1, x2, y2;
		//if (Tracer.info) Tracer.traceInfo(this.getClass(), "v1.getPosition().getX() : "+ v1.getPosition().get3DX());
		//if (Tracer.info) Tracer.traceInfo(this.getClass(), "v1.getPosition().getY() : "+ v1.getPosition().get3DY());
		//if (Tracer.info) Tracer.traceInfo(this.getClass(), "v2.getPosition().getX() : "+ v2.getPosition().get3DX());
		//if (Tracer.info) Tracer.traceInfo(this.getClass(), "v2.getPosition().getY() : "+ v2.getPosition().get3DY());
		
		x1 = (int)(v1.getPosition().get3DX()*graphic.getPixelWidth()/2);
		y1 = (int)(v1.getPosition().get3DY()*graphic.getPixelHeight()/2);
		x2 = (int)(v2.getPosition().get3DX()*graphic.getPixelWidth()/2);
		y2 = (int)(v2.getPosition().get3DY()*graphic.getPixelHeight()/2);

		view.drawLine(x1, y1, x2, y2);
	}

	//
	// End to be Encapsulated

	
}
