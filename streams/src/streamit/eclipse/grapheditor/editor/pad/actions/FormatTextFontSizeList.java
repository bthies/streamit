/*
 * @(#)FormatTextFontSizeList.java	1.2 02.02.2003
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
import java.awt.event.ActionEvent;

import javax.swing.JComboBox;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.pad.resources.Translator;

/**
 *
 * @author sven.luzar
 * @version 1.0
 *
 */
public class FormatTextFontSizeList extends AbstractActionList {

	/** Sizes of the possible fonts
	 */
	public static String[] fontSizes =
		new String[] { "12", "14", "16", "18", "20", "24", "30", "36" };

	/**
	 * Constructor for FormatTextFontSizeList.
	 * @param graphpad
	 */
	public FormatTextFontSizeList(GPGraphpad graphpad) {
		super(graphpad);
		String tmp = Translator.getString("Sizes");
		if (tmp != null) {
		  try {
			fontSizes = tokenize(tmp);
		  } catch (Exception e) {
			// Ignore
		  }
		}
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		String item = (String)getSelectedItem(e);
		try {
			setFontSizeForSelection(Float.parseFloat(item));
		} catch (Exception ex) {
			// invalid float
		}
	}

	/**
	 * @see org.jgraph.pad.actions.AbstractActionList#getToolBarComponent()
	 */
	protected JComboBox getToolBarComponent() {
		JComboBox combo = new JComboBox();
		combo.setName(getName());
		combo.setPreferredSize(new Dimension(66 - 15, 26));
		combo.setMaximumSize(combo.getPreferredSize());
		combo.setEditable(true);
		return combo;
	}

	/**
	 * @see org.jgraph.pad.actions.AbstractActionList#getItems()
	 */
	protected Object[] getItems() {
		return fontSizes;
	}

}
