/*
 * @(#)GPConfiguration.java	1.0 18.02.2003
 *
 * Copyright (C) 2003 luzar
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
package streamit.eclipse.grapheditor.editor.pad;

import javax.swing.ActionMap;

/**Specifies a configuration for the GPGraphpad.
 * You can add values for the GPGraphpad so that
 * the graph pad uses your own configuration.
 * Currently you can only add a user defined
 * Action Map. For the GPGraphpad the specified
 * Action Map is the current Action Map and
 * the default Action Map is a parent from the specified
 * current Action Map. You can overlap the
 * default actions by using the same Action Name.<br>
 * <br>
 * Example:<br>
 * This example shows how to add a new user defined Action for
 * the file exit action key.
 *
 * <pre>
 * MyFileExitAction 	a 			= new MyFileExitAction();
 * ActionMap 			actionMap 	= new ActionMap();
 * actionMap.put(		a.getValue(Action.NAME),a);
 * GPConfiguration 		config 		= new GPConfiguration();
 * config.setActionMap(	actionMap);
 * GPGraphpad 			pad 		= new GPGraphpad(config);
 * </pre>
 *
 *
 * @author luzar
 * @version 1.0
 */
public class GPConfiguration {

	/** Actionmap for user defined actions
	 */
	ActionMap actionMap;

	/**
	 * Returns the actionMap.
	 * @return ActionMap
	 */
	public ActionMap getActionMap() {
		return actionMap;
	}

	/**
	 * Sets the actionMap.
	 * @param actionMap The actionMap to set
	 */
	public void setActionMap(ActionMap actionMap) {
		this.actionMap = actionMap;
	}

}
