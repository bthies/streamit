/*
 * @(#)EditRedo.java	1.2 30.01.2003
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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JMenuItem;
import javax.swing.undo.CannotRedoException;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.pad.GPBarFactory;
import streamit.eclipse.grapheditor.editor.pad.resources.Translator;

/**
 * 
 * @author sven.luzar
 * @version 1.0
 *
 */
public class EditRedo extends AbstractActionDefault {

	protected Vector menuItems = new Vector();

	/**
	 * Constructor for EditRedo.
	 * @param graphpad
	 * @param name
	 */
	public EditRedo(GPGraphpad graphpad) {
		super(graphpad);
		setEnabled(false);
	}

	public void actionPerformed(ActionEvent e) {
		try {
			getCurrentDocument().getGraphUndoManager() .redo(graphpad.getCurrentGraph().getGraphLayoutCache());
		} catch (CannotRedoException ex) {
			System.out.println("Unable to redo: " + ex);
			ex.printStackTrace();
		}
		update();
	}

	public void update() {
		Enumeration enum = menuItems.elements();
		
		while(enum.hasMoreElements()){
			JMenuItem item = (JMenuItem)enum.nextElement() ;
			if (getCurrentDocument() != null&&
				getCurrentDocument().getGraphUndoManager().canRedo(getCurrentGraphLayoutCache())) {
				setEnabled(true);
				item.setText(
					getCurrentDocument().getGraphUndoManager().getRedoPresentationName());
			} else {
				setEnabled(false);
				item.setText(Translator.getString("Component.EditRedo.Text"));
			}
		}
	}

	/**
	 * @see org.jgraph.pad.actions.AbstractActionDefault#getMenuComponent(String)
	 */
	protected Component getMenuComponent(String actionCommand) {
		JMenuItem item = new JMenuItem(this);
		
		GPBarFactory.fillMenuButton(
			item,
			getName(),
			actionCommand);

		menuItems.add(item);
		return item;
	}

}
