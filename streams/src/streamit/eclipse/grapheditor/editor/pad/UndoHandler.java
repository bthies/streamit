/*
 * @(#)UndoHandler.java	1.2 30.01.2003
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

import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

/**
 * 
 * @author sven.luzar
 * @version 1.0
 *
 */
public class UndoHandler implements UndoableEditListener {

		protected boolean addEdits = true;

		GPDocument document;

		public UndoHandler(GPDocument document, boolean addEdits) {
			this.document = document;
			this.addEdits = addEdits;
		}

		/**
		 * Messaged when the Document has created an edit, the edit is
		 * added to <code>graphUndoManager</code>, an instance of UndoManager.
		 */
		public void undoableEditHappened(UndoableEditEvent e) {
			if (addEdits)
				document.getGraphUndoManager().addEdit(e.getEdit());
			document.getGraphpad() .getEditUndoAction().update();
			document.getGraphpad() .getEditRedoAction().update();
		}

}
