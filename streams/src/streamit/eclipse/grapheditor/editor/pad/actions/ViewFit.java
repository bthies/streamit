/*
 * @(#)ViewFit.java	1.2 02.02.2003
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

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.pad.GPDocument;
import streamit.eclipse.grapheditor.graph.Constants;

/**
 * 
 * @author sven.luzar
 * @version 1.0
 *
 */
public class ViewFit extends AbstractActionRadioButton {

	public static final String NONE = "None";
	public static final String WINDOW = "Window";
	


	/**
	 * Constructor for ViewFit.
	 * @param graphpad
	 */
	public ViewFit(GPGraphpad graphpad) {
		super(graphpad);
		lastActionCommand = NONE;
	}


	/**
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		GPDocument doc = graphpad.getCurrentDocument();
		if (e != null)
		{
			lastActionCommand = e.getActionCommand() ;
			
			if (NONE.equals(e.getActionCommand())) {
				getCurrentDocument().setResizeAction(null);
				getCurrentGraph().setScale(1);
			} else if (WINDOW.equals(e.getActionCommand())) {
				Rectangle p = null;
				if (doc.areContainersInvisible())
				{
					p = new Rectangle (new Point(Constants.TOPLEVEL_LOC_X, Constants.TOPLEVEL_LOC_Y),
																 doc.getGraphStructure().getTopLevel().getDimension());
															
				}
				else
				{
					p = getCurrentGraph().getCellBounds(getCurrentGraph().getRoots());
				}
				if (p != null) {
					Dimension s =
						getCurrentDocument().getScrollPane().getViewport().getExtentSize();
					double scale = 1;
					if (Math.abs(s.getWidth() - (p.x + p.getWidth()))
						> Math.abs(s.getHeight() - (p.x + p.getHeight())))
						scale = (double) s.getWidth() / (p.x + p.getWidth());
					else
						scale = (double) s.getHeight() / (p.y + p.getHeight());
					scale = Math.max(Math.min(scale, 16), .01);
					getCurrentGraph().setScale(scale);
					getCurrentDocument().setResizeAction(this);
				}
			} 
		}	
		
		update();
	}

	/**
	 * @see org.jgraph.pad.actions.AbstractActionRadioButton#getPossibleActionCommands()
	 */
	public String[] getPossibleActionCommands() {
		return new String[] { NONE, WINDOW};
	}


}
