/*
 * @(#)GPEscAction.java 1.0 09.08.2003
 *
 * Copyright (C) 2003 sven_luzar
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
package streamit.eclipse.grapheditor.editor.utils.gui;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.SwingUtilities;

/**The action analysis the source from the action event.
 * If it is a JInternalFrame or a Window, then
 * the action will call the dispose method. 
 * 
 * 
 * @author sven_luzar
 * @version 1.0
 * @see javax.swing.JInternalFrame#dispose()
 * @see java.awt.Window#dispose()
 * 
 */
public class GPEscAction extends AbstractAction {

	JButton button = null;

	/** Creates a new instance
	 * 
	 */
	public GPEscAction() {
		super();
	}

	/** Creates a new instance for the esc button
	 * 
	 */
	public GPEscAction(JButton button) {
		super();
		this.button = button;
	}

	/**If the button is set, then
	 * the method will call the do click
	 * method at the button.
	 * 
	 * If the event source 
	 * is a JInternalFrame or a Window, then
	 * the action will call the dispose method. 
	 * 
	 * 
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 * @see javax.swing.JButton#doClick()
	 */
	public void actionPerformed(ActionEvent e) {
		System.out.println(e.getSource().getClass());

		// is the esc button set?
		if (button != null){
			button.doClick();
			return;
		}
			 

		// is it an internal frame?
		try {
			JInternalFrame jif =
				(JInternalFrame) SwingUtilities.getAncestorOfClass(
					JInternalFrame.class,
					(Component) e.getSource());
			if (jif != null) {
				jif.dispose();
				return;
			}
		} catch (Exception ex) {
			System.err.println(ex);
		}

		// is it an window?
		try {
			Window w =
				(Window) SwingUtilities.getAncestorOfClass(
					Window.class,
					(Component) e.getSource());
			if (w != null) {
				w.dispose();
				return;
			}
		} catch (Exception ex) {
			System.err.println(ex);
		}

	}
}
