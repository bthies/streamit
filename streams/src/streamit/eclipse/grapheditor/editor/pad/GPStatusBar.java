/*
 * @(#)GPStatusBar.java	1.2 02.02.2003
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

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author sven.luzar
 * @version 1.0
 *
 */
public class GPStatusBar extends JPanel {
	/** contains the message at the current
	 *  Status bar
	 */
	protected JLabel message;

	/** contains the scale for the current
	 *  graph
	 */
	protected JLabel scale;

	/**
	 * Constructor for GPStatusBar.
	 *
	 */
	public GPStatusBar() {
		super();
		setLayout(new BorderLayout());
		message = new JLabel("Ready.");
		scale = new JLabel("100%");
		message.setBorder(BorderFactory.createLoweredBevelBorder());
		scale.setBorder(BorderFactory.createLoweredBevelBorder());
		add(message, BorderLayout.CENTER);
		add(scale, BorderLayout.EAST);
	}
	/**
	 * Returns the message.
	 *
	 * @return The message from the status bar
	 */
	public String getMessage() {
		return message.getText() ;
	}

	/**
	 * Returns the scale.
	 * @return JLabel
	 */
	public String getScale() {
		return scale.getText() ;
	}

	/**
	 * Sets the message.
	 * @param message The message to set
	 */
	public void setMessage(String message) {
		this.message.setText(message);
	}

	/**
	 * Sets the scale.
	 * @param scale The scale to set
	 */
	public void setScale(String scale) {
		this.scale.setText(scale);
	}

}
