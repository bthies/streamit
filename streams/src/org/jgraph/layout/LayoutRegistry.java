/*
 * @(#)LayoutRegistry.java	1.0 01/20/03
 *
 * Copyright (C) 2003 Sven Luzar
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
package org.jgraph.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

/**A registry for the available Layout Algorithms. 
 * The default controllers will add at the static
 * constructor.
 * 
 * Please register additional 
 * layout controllers assigned
 * with a layout algorithm at this registry.
 * 
 * After that the Layout is available in the
 * selection window.
 * 
 * 
 * @author <a href="mailto:Sven.Luzar@web.de">Sven Luzar</a>
 * @since 1.2.2
 * @version 1.0 init
 *
 */
public class LayoutRegistry {

	/** A list with registered Controllers 
	 * 
	 *  @see LayoutController
	 */
	protected static ArrayList layoutControllers = new ArrayList();

	/** At this static initializer the default Layout
	 *  Controllers will register
	 */
	static {
		layoutControllers.add(new SpringEmbeddedLayoutController());
		layoutControllers.add(new SugiyamaLayoutController());
		//layoutControllers.add(new AnnealingLayoutController());
		//layoutControllers.add(new GEMLayoutController());
		//layoutControllers.add(new TreeLayoutController());
		sort();
	}

	/** Adds a LayoutConroller to this 
	 *  registry
	 */
	public static void addLayoutController(LayoutController controller) {
		layoutControllers.add(controller);
		sort();
	}

	/** Removes a LayoutConroller from this 
	 *  registry.
	 */
	public static void removeLayoutController(LayoutController controller) {
		layoutControllers.remove(controller);
	}

	/** Returns all registered LayoutConrollers.
	 */
	public static Iterator registeredLayoutControllers() {
		return layoutControllers.iterator();
	}

	protected static void sort() {
		Collections.sort(layoutControllers, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((LayoutController) o1).toString().compareTo(
					((LayoutController) o2).toString());
			}

			public boolean equals(Object obj) {
				return false;
			}
		});
	}
}
