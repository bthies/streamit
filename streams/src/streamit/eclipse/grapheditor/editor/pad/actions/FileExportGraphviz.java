/*
 * @(#)FileExportGraphviz.java	1.2 01.02.2003
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

import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.io.FileOutputStream;
import java.io.IOException;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.pad.GPConverter;
import streamit.eclipse.grapheditor.editor.pad.resources.Translator;

/**
 *
 * @author sven.luzar
 * @version 1.0
 *
 */
public class FileExportGraphviz extends AbstractActionFile {

	/**
	 * Constructor for FileExportGraphviz.
	 * @param graphpad
	 */
	public FileExportGraphviz(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		FileDialog f =
			new FileDialog(graphpad.getFrame(), Translator.getString("DOTFile"/*#Finished:Original="DOT File"*/), FileDialog.SAVE);
		f.show();
		if (f.getFile() == null)
			return;

		try {
			Object[] cells = getCurrentGraph().getAll();

			if (cells != null && cells.length > 0) {
				// Create temp file
				String tmpFile = f.getDirectory() + f.getFile();
				//File tmpFile = File.createTempFile("TODOT", ".dot");
				//tmpFile.deleteOnExit();

				// File Output stream
				FileOutputStream fos = new FileOutputStream(tmpFile);
				fos.write(GPConverter.toGraphviz(getCurrentGraph(), cells).getBytes());

				// Write to file
				fos.flush();
				fos.close();

			};
		} catch (IOException ex) {
			graphpad.error(ex.toString());
		}
	}

}
