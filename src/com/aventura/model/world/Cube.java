package com.aventura.model.world;

import com.aventura.math.vector.Vector3;

public class Cube extends Box {
	
	public Cube(double length, Vector3 position) {
		super(length, length, length, position);
	}

}
