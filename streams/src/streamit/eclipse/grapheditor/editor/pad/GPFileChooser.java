/*
 * @(#)GPFileChooser.java	1.0 17.02.2003
 *
 * Copyright (C) 2003 luzar
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
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;

/**File chooser for open or close graph pad files.
 * 
 * The file chooser uses the file formats from the
 * graph model provider at the GPDocument. If the
 * GPDocument is null the file chooser uses each
 * available file format.
 * 
 * 
 * @author luzar
 * @version 1.0
 */
public class GPFileChooser extends JFileChooser {

	/** Key for the preferences store 
	 *  for the last path value.
	 * 
	 */
	public static final String LAST_PATH_POSITION = "LastPathPosition";

	/** Key for the preferences store 
	 *  for the last file filter
	 * 
	 */
	public static final String LAST_FILE_FILTER = "LastFileFilter";

	/**
	 * Constructor for GPFileChooser.
	 */
	public GPFileChooser(GPDocument doc) {
		super();
		init(doc);
	}

	/**
	 * Constructor for GPFileChooser.
	 * @param currentDirectoryPath
	 */
	public GPFileChooser(GPDocument doc, String currentDirectoryPath) {
		super(currentDirectoryPath);
		init(doc);
	}

	/**
	 * Constructor for GPFileChooser.
	 * @param currentDirectory
	 */
	public GPFileChooser(GPDocument doc, File currentDirectory) {
		super(currentDirectory);
		init(doc);
	}

	/**
	 * Constructor for GPFileChooser.
	 * @param fsv
	 */
	public GPFileChooser(GPDocument doc, FileSystemView fsv) {
		super(fsv);
		init(doc);
	}

	/**
	 * Constructor for GPFileChooser.
	 * @param currentDirectory
	 * @param fsv
	 */
	public GPFileChooser(
		GPDocument doc,
		File currentDirectory,
		FileSystemView fsv) {
		super(currentDirectory, fsv);
		init(doc);
	}

	/**
	 * Constructor for GPFileChooser.
	 * @param currentDirectoryPath
	 * @param fsv
	 */
	public GPFileChooser(
		GPDocument doc,
		String currentDirectoryPath,
		FileSystemView fsv) {
		super(currentDirectoryPath, fsv);
		init(doc);
	}

	/** Initializes the gui.
	 * 
	 *  Adds a property change listener to catch
	 *  changes at the file filter. If the user
	 *  changes the file filter the listener
	 *  changes the accessory for the correct file format.
	 * 
	 */
	protected void init(GPDocument doc) {

		// if the dialog was committed we
		// store the path position for the 
		// next call 
		addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (e.getActionCommand() == JFileChooser.APPROVE_SELECTION) {
					try {
						Preferences.userNodeForPackage(this.getClass()).put(
							LAST_PATH_POSITION,
							GPFileChooser.this.getSelectedFile().getPath());
					} catch (Exception ex) {
						// ignore
					}
				}
			}
		});

		addPropertyChangeListener(new PropertyChangeListener() {
			/**
			 * @see java.beans.PropertyChangeListener#propertyChange(PropertyChangeEvent)
			 */
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt != null
					&& evt.getPropertyName() != null
					&& (evt
						.getPropertyName()
						.equals(JFileChooser.FILE_FILTER_CHANGED_PROPERTY)
						|| evt.getPropertyName().equals(
							JFileChooser.DIALOG_TYPE_CHANGED_PROPERTY))) {
					FileFilter ff = (FileFilter) getFileFilter();
					GraphModelFileFormat gmff =
						GraphModelProviderRegistry.getGraphModelFileFormat(ff);
					if (gmff == null)
						setAccessory(null);
					else if (getDialogType() == SAVE_DIALOG)
						setAccessory(gmff.getWriteAccessory());
					else if (getDialogType() == OPEN_DIALOG)
						setAccessory(gmff.getReadAccessory());
					Component comp = GPFileChooser.this;

					while (!(comp instanceof Window)) {
						if (comp == null)
							return;
						comp = comp.getParent();
					}
					if (comp != null)
						 ((Window) comp).pack();
				}
			}
		});
		GraphModelFileFormat[] formats;
		if (doc == null) {
			formats = GraphModelProviderRegistry.getGraphModelFileFormats();
		} else {
			formats = doc.getGraphModelProvider().getGraphModelFileFormats();
		}

		for (int i = 0; i < formats.length; i++) {
			GraphModelFileFormat format = formats[i];
			addChoosableFileFilter(format.getFileFilter());
		}

		try {

			String fileName =
				Preferences.userNodeForPackage(this.getClass()).get(
					LAST_PATH_POSITION,
					System.getProperty("user.home"));
			try {
				setCurrentDirectory(new File(fileName));
			} catch (Exception ex) {
				// ignore
			}

			// set the last selected file filter
			try {
				GraphModelFileFormat format =
					GraphModelProviderRegistry.getGraphModelFileFormat(
						fileName);
				this.setFileFilter(format.getFileFilter());
			} catch (Exception ex) {
				// ignore
			}
		} catch (Exception ex) {
			// ignore
		}

	}
}
