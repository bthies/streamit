/*
 * @(#)GraphListCellRenderer.java	1.2 02.02.2003
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
package streamit.eclipse.grapheditor.editor.pad;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;

import org.jgraph.graph.AbstractCellView;
import org.jgraph.graph.CellView;
import streamit.eclipse.grapheditor.editor.pad.actions.AbstractActionList;

/**
 * 
 * @author sven.luzar
 * @version 1.0
 *
 */
public class GraphListCellRenderer extends DefaultListCellRenderer {
	
	protected static GPGraph dummyGraph = new GPGraph();

	/** reference to the combobox for this renderer
	 */
	protected AbstractActionList action;

	/**
	 * Constructor for GraphListCellRenderer.
	 */
	public GraphListCellRenderer(AbstractActionList action) {
		this.action = action;
	}

	/**
	 */
	public Component getListCellRendererComponent(
		JList list,
		Object view,
		int index,
		boolean isSelected,
		boolean cellHasFocus) {
			
		if (view instanceof String){
			return new JLabel((String)view);
		}
		
		JComponent c =
			new RealGraphCellRenderer(
				dummyGraph ,
				new CellView[] {(CellView) view });
		Rectangle b = ((AbstractCellView) view).getBounds();
		if (b != null)
			c.setBounds(2, 2, b.width, b.height);
		c.setPreferredSize(new Dimension(b.width , b.height ));
		c.setOpaque(true);
		c.setBackground(dummyGraph .getBackground());
		return c;
	}

}
