/*
 * @(#)GraphModelEvent.java	1.0 1/1/02
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

package com.jgraph.event;

import java.util.EventObject;
import java.util.Map;

import com.jgraph.graph.CellView;
import com.jgraph.graph.ConnectionSet;
import com.jgraph.graph.GraphLayoutCache;
import com.jgraph.graph.ParentMap;

/**
 * Encapsulates information describing changes to a graph model, and
 * is used to notify graph model listeners of the change.
 *
 * @author Gaudenz Alder
 * @version 1.0 1/1/2
 *
 */

public class GraphModelEvent extends EventObject {

	/**
	 * The object that consistutes the change.
	 */
	protected GraphModelChange change;

	/**
	 * Used to create an event when cells have been changed, inserted, or
	 * removed, identifying the change as a ModelChange object.
	 * @param source the Object responsible for generating the event (typically
	 * the creator of the event object passes <code>this</code>
	 * for its value)
	 * @param change the object that describes the change
	 * @see com.jgraph.graph.GraphCell
	 *
	 */
	public GraphModelEvent(Object source, GraphModelChange change) {
		super(source);
		this.change = change;
	}

	/**
	 * Returns the object that constitues the change.
	 */
	public GraphModelChange getChange() {
		return change;
	}

	/**
	 * Defines the interface for objects that may be executed by the
	 * model when used as arguments to insert or edit.
	 */
	public static interface ExecutableGraphChange {

		/**
		 * Executes the change.
		 */
		public void execute();

	}

	/**
	 * Defines the interface for objects that may be used to
	 * represent a change to the view.
	 */
	public static interface GraphViewChange {

		/**
		 * Returns the source of this change. This can either be a
		 * view or a model, if this change is a GraphModelChange.
		 */
		public Object getSource();

		/**
		 * Returns the objects that have changed.
		 */
		public Object[] getChanged();

		/**
		 * Returns a map that contains (object, map) pairs which
		 * holds the new attributes for each changed cell.
		 * Note: This returns a map of (cell, map) pairs for
		 * an insert on a model that is not an attribute store.
		 * Use getStoredAttributeMap to access the attributes
		 * that have been stored in the model.
		 */
		public Map getAttributes();

		/**
		 * Returns the objects that have not changed explicitly, but
		 * implicitly because one of their dependent cells has changed.
		 * This is typically used to return the edges that are attached
		 * to vertices, which in turn have been resized or moved.
		 */
		public Object[] getContext();

	}

	/**
	 * Defines the interface for objects that may be included
	 * into a GraphModelEvent to describe a model change.
	 */
	public static interface GraphModelChange extends GraphViewChange {

		/**
		 * Returns the cells that have been inserted into the model.
		 */
		public Object[] getInserted();

		/**
		 * Returns the cells that have been removed from the model.
		 */
		public Object[] getRemoved();

		/**
		 * Returns a map that contains (object, map) pairs
		 * of the attributes that have been stored in the model.
		 */
		public Map getPreviousAttributes();

		public ConnectionSet getPreviousConnectionSet();
		
		public ParentMap getPreviousParentMap();

		/**
		 * Allows a <code>GraphLayoutCache</code> to store cell views
		 * for cells that have been removed. Such cell views are used
		 * for re-insertion and restoring the visual attributes.
		 */
		public void putViews(GraphLayoutCache view, CellView[] cellViews);

		/**
		 * Allows a <code>GraphLayoutCache</code> to retrieve an array of
		 * <code>CellViews</code> that was previously stored with
		 * <code>putViews(GraphLayoutCache, CellView[])</code>.
		 */
		public CellView[] getViews(GraphLayoutCache view);

	}

}