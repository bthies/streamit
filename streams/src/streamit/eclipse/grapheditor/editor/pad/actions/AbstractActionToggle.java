/*
 * @(#)AbstractActionToggle.java	1.2 01.02.2003
 *
 * Copyright (C) 2003 sven.luzar
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
package streamit.eclipse.grapheditor.editor.pad.actions;

import java.util.Enumeration;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.Icon;

import streamit.eclipse.grapheditor.editor.GPGraphpad;

/**
 * 
 * @author sven.luzar
 * @version 1.0
 *
 */
public abstract class AbstractActionToggle extends AbstractActionDefault {

	/** Container with abstract buttons of this type
	 */
	protected Vector abstractButtons = new Vector();

	/**
	 * Constructor for AbstractActionToggle.
	 * @param graphpad
	 */
	public AbstractActionToggle(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * Constructor for AbstractActionToggle.
	 * @param graphpad
	 * @param name
	 */
	public AbstractActionToggle(GPGraphpad graphpad, String name) {
		super(graphpad, name);
	}

	/**
	 * Constructor for AbstractActionToggle.
	 * @param graphpad
	 * @param name
	 * @param icon
	 */
	public AbstractActionToggle(GPGraphpad graphpad, String name, Icon icon) {
		super(graphpad, name, icon);
	}

	/** updates all Abstract Buttons from this action
	 */
	public void update() {
		super.update();

		Enumeration enum = abstractButtons.elements();
		while (enum.hasMoreElements()) {
			AbstractButton button = (AbstractButton) enum.nextElement();
			button.setSelected(isSelected(button.getActionCommand()));
		}
	}

	/** removes the abstract action from the
	 *  action control
	 */
	public void removeAbstractButton(AbstractButton button){
		abstractButtons.remove(button);
	}

	/** Should return true if the action value is true
	 */
	public abstract boolean isSelected(String actionCommand);


}
