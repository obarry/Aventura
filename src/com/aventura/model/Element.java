package com.aventura.model;

import java.util.*;

import com.aventura.tools.vector3d.transform.Transformation;

public abstract class Element {

	// An Element can have 0 to n other Element
	//private Element[] elements;
	protected ArrayList<Element> elements;
	protected Transformation transform;
	
}
