/*
 * @(#)AbstractActionListCellColor.java	1.2 02.02.2003
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

import java.awt.Color;
import javax.swing.Icon;

import streamit.eclipse.grapheditor.editor.GPGraphpad;

/**
 *
 * @author sven.luzar
 * @version 1.0
 *
 */
public abstract class AbstractActionListCellColor
	extends AbstractActionListCell {

	/** Colors for the selection
	 *
	 */
	protected Color[] colors =
		new Color[] {
			Color.black,
			Color.blue,
			Color.cyan,
			Color.darkGray,
			Color.gray,
			Color.green,
			Color.lightGray,
			Color.magenta,
			Color.orange,
			Color.pink,
			Color.red,
			Color.white,
			Color.yellow,
			Color.blue.darker(),
			Color.cyan.darker(),
			Color.green.darker(),
			Color.magenta.darker(),
			Color.orange.darker(),
			Color.pink.darker(),
			Color.red.darker(),
			Color.yellow.darker()};

	/**
	 * Constructor for AbstractActionListCellColor.
	 * @param graphpad
	 */
	public AbstractActionListCellColor(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * Constructor for AbstractActionListCellColor.
	 * @param graphpad
	 * @param name
	 */
	public AbstractActionListCellColor(GPGraphpad graphpad, String name) {
		super(graphpad, name);
	}

	/**
	 * Constructor for AbstractActionListCellColor.
	 * @param graphpad
	 * @param name
	 * @param icon
	 */
	public AbstractActionListCellColor(
		GPGraphpad graphpad,
		String name,
		Icon icon) {
		super(graphpad, name, icon);
	}
}
