/*
 * @(#)GPSplitPane.java 1.0 06.08.2003
 *
 * Copyright (C) 2003 sven_luzar
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package streamit.eclipse.grapheditor.editor.utils.gui;

import java.awt.Component;

import javax.swing.JSplitPane;


/**
 * One Layer between the JSplitPane 
 * and our implementation. 
 * Currently we add a load and store 
 * management for the divider position. 
 * 
 * 
 * @author sven_luzar
 * @version 1.0
 * 
 */
public class GPSplitPane extends JSplitPane {

	/** Calls the super constructor
	 *  and adds the instance to the position manager
	 * 
	 */
	public GPSplitPane() {
		super();
		PositionManager.addComponent(this);
	}

	/** Calls the super constructor
	 *  and adds the instance to the position manager
	 * 
	 * @param newOrientation
	 */
	public GPSplitPane(int newOrientation) {
		super(newOrientation);
		PositionManager.addComponent(this);
	}

	/** Calls the super constructor
	 *  and adds the instance to the position manager
	 * 
	 * @param newOrientation
	 * @param newContinuousLayout
	 */
	public GPSplitPane(int newOrientation, boolean newContinuousLayout) {
		super(newOrientation, newContinuousLayout);
		PositionManager.addComponent(this);
	}

	/** Calls the super constructor
	 *  and adds the instance to the position manager
	 * 
	 * @param newOrientation
	 * @param newLeftComponent
	 * @param newRightComponent
	 */
	public GPSplitPane(
		int newOrientation,
		Component newLeftComponent,
		Component newRightComponent) {
		super(newOrientation, newLeftComponent, newRightComponent);
		PositionManager.addComponent(this);
	}

	/** Calls the super constructor
	 *  and adds the instance to the position manager
	 * 
	 * @param newOrientation
	 * @param newContinuousLayout
	 * @param newLeftComponent
	 * @param newRightComponent
	 */
	public GPSplitPane(
		int newOrientation,
		boolean newContinuousLayout,
		Component newLeftComponent,
		Component newRightComponent) {
		super(
			newOrientation,
			newContinuousLayout,
			newLeftComponent,
			newRightComponent);
			PositionManager.addComponent(this);
	}

	/** Removes the Split Pane from the 
	 *  position manager and calls
	 *  the super implementation. 
	 *  
	 * @see java.lang.Object#finalize()
	 */
	protected void finalize() throws Throwable {
		PositionManager.removeComponent(this);
		super.finalize();
	}

	/** Calls the super implementation
	 *  and makes an update for the
	 *  component by using the locale
	 *  change adapter and the 
	 *  position manager.
	 *  
	 *  @param name the new name
	 *  @see PositionManager#updateComponent(Component)
	 *  @see LocaleChangeAdapter#updateComponent(Component)
	 *  @see java.awt.Component#setName(java.lang.String)
	 * 
	 */
	public void setName(String name) {
		super.setName(name);
		PositionManager.updateComponent(this);
		LocaleChangeAdapter.updateComponent(this);
	}
}
