/*
 * @(#)AbstractGraphCell.java	1.0 1/1/02
 *
 * Copyright (C) 2001 Gaudenz Alder
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

package com.jgraph.graph;

import java.awt.Rectangle;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

/**
 * The default implementation for the GraphCell interface.
 *
 * @version 1.0 1/1/02
 * @author Gaudenz Alder
 */

public class DefaultGraphCell
	extends DefaultMutableTreeNode
	implements GraphCell, Cloneable {

	public final static Rectangle defaultBounds = new Rectangle(10, 10, 20, 20);

	/** Hashtable for properties. Initially empty */
	protected Map attributes = GraphConstants.createMap();

	/**
	 * Creates an empty cell.
	 */
	public DefaultGraphCell() {
		this(null);
	}

	/**
	 * Creates a graph cell and initializes it with the specified user object.
	 *
	 * @param userObject an Object provided by the user that constitutes
	 *                   the cell's data
	 */
	public DefaultGraphCell(Object userObject) {
		this(userObject, null);
	}

	/**
	 * Constructs a cell that holds a reference to the specified user object
	 * and contains the specified array of children and sets default values
	 * for the bounds attribute.
	 *
	 * @param userObject reference to the user object
	 * @param children array of children
	 */
	public DefaultGraphCell(Object userObject, MutableTreeNode[] children) {
		super(userObject, true);
		setUserObject(userObject);
		if (children != null)
			for (int i = 0; i < children.length; i++)
				add(children[i]);
		GraphConstants.setBounds(attributes, defaultBounds);
	}

	/**
	 * Creates a graph cell and initializes it with the specified user object.
	 * The GraphCell allows children only if specified.
	 *
	 * @param userObject an Object provided by the user that constitutes
	 *                   the cell's data
	 */
	public DefaultGraphCell(Object userObject, boolean allowsChildren) {
		super(userObject, allowsChildren);
	}

	/**
	 * Override parent method to synchronize value property and userObject.
	 * The following holds for each GraphCell c:<p>
	 * GraphConstants.getValue(c.getAttributes()) == c.getUserObject()<p>
	 * <strong>Note:</strong> A cell's userObject can be set using
	 * GraphModel.edit() with a propertyMap that carries a value entry
	 * for that cell.
	 */
	public void setUserObject(Object obj) {
		if (userObject instanceof ValueChangeHandler) {
			((ValueChangeHandler) userObject).valueChanged(obj);
		} else
			super.setUserObject(obj);
		obj = getUserObject();
		if (obj == null)
			GraphConstants.setValue(attributes, "");
		else
			GraphConstants.setValue(attributes, obj);
	}

	/**
	 * Provides access to the children list to change ordering.
	 * This method returns a <code>Collections.EMPTY_LIST</code>
	 * if the list of childrenpoints to <code>null</code>.
	 */
	public List getChildren() {
		if (children == null)
			return Collections.EMPTY_LIST;
		return children;
	}

	/**
	 * Returns the properies of the cell.
	 */
	public Map getAttributes() {
		return attributes;
	}

	/**
	 * Apply <code>change</code> to the cell and sync userObject.
	 */
	public Map changeAttributes(Map change) {
		Map undo = GraphConstants.applyMap(change, attributes);
		Object newValue = GraphConstants.getValue(attributes);
		// Check for inconsistencies
		if (userObject != null && newValue == null)
			GraphConstants.setValue(attributes, userObject);
		else if (userObject instanceof ValueChangeHandler) {
			Object oldValue =
				((ValueChangeHandler) userObject).valueChanged(newValue);
			if (oldValue == null
				|| !newValue.toString().equals(oldValue.toString()))
				GraphConstants.setValue(undo, oldValue);
			GraphConstants.setValue(attributes, userObject);
		} else
			userObject = newValue;
		// Ensure non-null bounds
		Rectangle bounds = GraphConstants.getBounds(attributes);
		if (bounds == null)
			GraphConstants.setBounds(attributes, defaultBounds);
		return undo;
	}

	/**
	 * Sets the attributes.
	 * @param attributes The attributes to set
	 */
	public void setAttributes(Map attributes) {
		this.attributes = attributes;
	}

	/**
	 * Create a clone of the cell. The cloning of the
	 * user object is deferred to the cloneUserObject()
	 * method.
	 *
	 * @return Object  a clone of this object.
	 */
	public Object clone() {
		DefaultGraphCell c = (DefaultGraphCell) super.clone();
		c.attributes = new Hashtable(attributes);
		c.userObject = cloneUserObject();
		return c;
	}

	/**
	 * Create a clone of the user object. This is provided for
	 * subclassers who need special cloning. This implementation
	 * simply returns a reference to the original user object.
	 *
	 * @return Object  a clone of this cells user object.
	 */
	protected Object cloneUserObject() {
		if (userObject instanceof ValueChangeHandler)
			return ((ValueChangeHandler) userObject).clone();
		return userObject;
	}

	public interface ValueChangeHandler {

		/**
		 * Messaged when the value of the cell has changed.
		 * Return the old value for correct undo/redo.
		 */
		public Object valueChanged(Object newValue);

		/**
		 * Invoked to clone the user object.
		 */
		public Object clone();

	}

}