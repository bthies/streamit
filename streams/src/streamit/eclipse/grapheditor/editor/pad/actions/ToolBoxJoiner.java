/*
 * @(#)ToolBoxJoiner.java	1.2 05.02.2003
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

import javax.swing.Icon;
import javax.swing.JToggleButton;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.pad.GPBarFactory;

/**
 * 
 * @author sven.luzar
 * @version 1.0
 *
 */
public class ToolBoxJoiner extends AbstractActionDefault {

	/**
	 * Constructor for ToolBoxJoiner.
	 * @param graphpad
	 */
	public ToolBoxJoiner(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * Constructor for ToolBoxJoiner.
	 * @param graphpad
	 * @param name
	 */
	public ToolBoxJoiner(GPGraphpad graphpad, String name) {
		super(graphpad, name);
	}

	/**
	 * Constructor for ToolBoxJoiner.
	 * @param graphpad
	 * @param name
	 * @param icon
	 */
	public ToolBoxJoiner(GPGraphpad graphpad, String name, Icon icon) {
		super(graphpad, name, icon);
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
	}
	/**
	 * @see org.jgraph.pad.actions.AbstractActionDefault#getToolComponent(String)
	 */
	protected Component getToolComponent(String actionCommand) {
		JToggleButton button = graphpad.getMarqueeHandler().getButtonJoiner() ;
		GPBarFactory.fillToolbarButton(
					button,
					getName(),
					actionCommand);
		return button;
	}
	/** 
	 * 
	 */
	public void update() {
		super.update();
		graphpad.getMarqueeHandler().getButtonJoiner().setEnabled(isEnabled());
	}

}
