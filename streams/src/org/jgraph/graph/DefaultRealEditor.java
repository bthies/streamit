/*
 * @(#)DefaultCellEditor.java	1.0 1/1/02
 *
 * Copyright (C) 2001 Gaudenz Alder
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

package org.jgraph.graph;

import java.awt.Component;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;

import org.jgraph.JGraph;

/**
 * The default editor for graph cells.
 *
 * @version 1.0 1/1/02
 * @author Gaudenz Alder
 */

public class DefaultRealEditor
	extends DefaultCellEditor
	implements GraphCellEditor {

	//
	//  Constructors
	//

	/**
	 * Constructs a DefaultCellEditor that uses a text field.
	 *
	 * @param x  a JTextField object ...
	 */
	public DefaultRealEditor(final JTextField textField) {
		super(textField);
		setClickCountToStart(1);
	}

	/**
	 * Constructs a DefaultCellEditor object that uses a check box.
	 *
	 * @param x  a JCheckBox object ...
	 */
	public DefaultRealEditor(final JCheckBox checkBox) {
		super(checkBox);
	}

	/**
	 * Constructs a DefaultCellEditor object that uses a combo box.
	 *
	 * @param x  a JComboBox object ...
	 */
	public DefaultRealEditor(final JComboBox comboBox) {
		super(comboBox);
	}

	//
	//  GraphCellEditor Interface
	//

	public Component getGraphCellEditorComponent(
		JGraph graph,
		Object value,
		boolean isSelected) {
		String stringValue = graph.convertValueToString(value);

		delegate.setValue(stringValue);
		if (editorComponent instanceof JTextField)
			 ((JTextField) editorComponent).selectAll();
		return editorComponent;
	}

} // End of class JCellEditor
