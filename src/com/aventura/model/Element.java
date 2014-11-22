package com.aventura.model;

import java.util.*;

import com.aventura.tools.vector3d.transform.Transformation;

public abstract class Element {

	// An Element can have 0 to n other Element
	//private Element[] elements;
	protected ArrayList<Element> elements;
	
	// The transformation of this element compare to the element it is belonging.
	// This consists in applying the transformation to all elements of the list of elements
	protected Transformation transform;
	
	/**
	 * Provide only the list of Elements contained in this Element, transformed by the transformation
	 * @return the Array of Elements
	 */
	public ArrayList<Element> getElements() {
		return null;
	}
	
	/**
	 * Provide only the list of Elements contained in this Element, without any transformation
	 * @return
	 */
	public ArrayList<Element> getElementsWithoutTransformation() {
		return null;
	}

	/**
	 * Provide the list of all Elements (including Elements of this Element and its sub-Elements, recusrsively obtained)
	 * contained in this Element, transformed by the transformation
	 * @return
	 */
	public ArrayList<Element> getAllElements() {
		return null;
	}
	
}
