package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.event.ActionEvent;
import java.beans.XMLEncoder;
import java.io.BufferedOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.pad.GPLibraryPanel;
import streamit.eclipse.grapheditor.editor.pad.resources.Translator;

/**
 * Action that saves the Library to a file.
 *
 * @author sven.luzar
 *
 */
public class FileLibrarySaveAs extends AbstractActionFile {

	/**
	 * Constructor for FileLibrarySaveAs.
	 * @param graphpad
	 * @param name
	 */
	public FileLibrarySaveAs(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		String libraryExtension = Translator.getString("LibraryExtension");

		String name =
			saveDialog(
				Translator.getString("FileSaveAsLabel"),
				libraryExtension,
				Translator.getString("JGraphpadLibrary", new Object[]{libraryExtension}));
		if (name != null) {
			GPLibraryPanel.ScrollablePanel panel =
				getCurrentDocument().getLibraryPanel().getPanel();
			if (panel != null) {
				Serializable s = panel.getArchiveableState();
				try {
					Boolean compress = new Boolean(Translator.getString("compressLibraries"));
					ObjectOutputStream out = createOutputStream(name, compress.booleanValue());
					XMLEncoder enc =
						new XMLEncoder(
							new BufferedOutputStream(out));
					enc.writeObject(s);
					enc.close();
				} catch (Exception ex) {
					graphpad.error(ex.toString());
				} finally {
					graphpad.invalidate();
				}
			}
		}
	}

}
