/*
 * @(#)AbstractActionListCell.java	1.2 02.02.2003
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
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Map;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.JComboBox;

import org.jgraph.JGraph;
import org.jgraph.graph.CellView;
import org.jgraph.graph.GraphConstants;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.pad.GraphListCellRenderer;

/**
 *
 * @author sven.luzar
 * @version 1.0
 *
 */
public abstract class AbstractActionListCell extends AbstractActionList {

	/** A dummy jgraph for the menu components based
	 *  on jgraph components. (For example the arrows.)
	 *
	 */
	public static JGraph dummyGraph = new JGraph();

	public static final String IDENTIFIER_RESET = " X";

	public static final String IDENTIFIER_SELECT = "...";

	protected static java.util.List arrowPoints = new ArrayList();

	protected static java.util.List arrowEndPoints = new ArrayList();


	static {
		arrowPoints.add(new Point(0, 6));
		arrowPoints.add(new Point(40, 6));
		arrowEndPoints.add(new Point(0, 6));
		arrowEndPoints.add(new Point(14, 6));
	}


	/**
	 * Constructor for AbstractActionListCell.
	 * @param graphpad
	 */
	public AbstractActionListCell(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * Constructor for AbstractActionListCell.
	 * @param graphpad
	 * @param name
	 */
	public AbstractActionListCell(GPGraphpad graphpad, String name) {
		super(graphpad, name);
	}

	/**
	 * Constructor for AbstractActionListCell.
	 * @param graphpad
	 * @param name
	 * @param icon
	 */
	public AbstractActionListCell(
		GPGraphpad graphpad,
		String name,
		Icon icon) {
		super(graphpad, name, icon);
	}

	/**
	 * @see org.jgraph.pad.actions.AbstractActionList#getToolBarComponent()
	 */
	protected JComboBox getToolBarComponent() {
		JComboBox combo = super.getToolBarComponent();
		
		combo.setRenderer(new GraphListCellRenderer(this));
		combo.setPreferredSize(new Dimension(45, 26));
		combo.setMaximumSize(combo.getPreferredSize());
		return combo;
	}

	/**
	 * @see org.jgraph.pad.actions.AbstractActionList#getItems()
	 */
	protected Object[] getItems() {
		Vector items = new Vector();

		items.add(IDENTIFIER_RESET);
		items.add(IDENTIFIER_SELECT);

		fillCustomItems(items);

		return items.toArray();
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		Map map = GraphConstants.createMap();
		if (source instanceof JComboBox) {
			JComboBox box = (JComboBox) e.getSource();
			Object item = box.getSelectedItem();
			if (item instanceof CellView) {
				fillApplyMap((CellView) item, map);
			}
			if (item == IDENTIFIER_SELECT) {
				selectAndFillMap(map);
			} else if (item == IDENTIFIER_RESET) {
				fillResetMap(map);
			}

		}
		setSelectionAttributes(map);

	}

	protected abstract void fillCustomItems(Vector items);

	protected abstract void fillResetMap(Map target);

	protected abstract void fillApplyMap(CellView source, Map target);

	protected abstract void selectAndFillMap(Map target);

	protected Dimension size = new Dimension(14, 15);



	protected Point point = new Point(0, 0);


}
