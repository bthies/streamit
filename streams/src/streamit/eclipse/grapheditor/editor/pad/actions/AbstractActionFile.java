package streamit.eclipse.grapheditor.editor.pad.actions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.swing.JFileChooser;

import streamit.eclipse.grapheditor.editor.GPGraphpad;

/**
 * @author sven.luzar
 *
 */
public abstract class AbstractActionFile extends AbstractActionDefault {

	/** If the library files should be compressed (zipped)
	 */
	public static final boolean COMPRESS_FILES = true;

	/**
	 * Constructor for AbstractActionFile.
	 * @param graphpad
	 * @param name
	 */
	public AbstractActionFile(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * Constructor for AbstractActionFile.
	 * @param graphpad
	 * @param name
	 * @param icon
	 */
	/*
	public AbstractActionFile(GPGraphpad graphpad, String name, Icon icon) {
		super(graphpad, name, icon);
	}*/

	/** Shows a file open dialog and returns the filename. */
	public String openDialog(String message, String extension, String desc) {
		return dialog(message, true, extension, desc);
	}

	/** Shows a file save dialog and returns the filename. */
	public String saveDialog(String message, String extension, String desc) {
		return dialog(message, false, extension, desc);
	}

	/** Opens a dialog and return the filename.
	 *
	 *  Returns <tt>null</tt> if cancelled.
	 * */
	protected String dialog(
		String message,
		boolean open,
		String extension,
		String desc) {
		//FileDialog f = new FileDialog(getFrame(), message, mode);
		JFileChooser f = new JFileChooser();
		f.setDialogTitle(message);
		String ext = (extension != null) ? "." + extension.toLowerCase() : "";
		if (extension != null)
			f.setFileFilter(new MyFileFilter(extension, desc));
		if (open)
			f.showOpenDialog(graphpad.getFrame());
		else
			f.showSaveDialog(graphpad.getFrame());
		if (f.getSelectedFile() != null) {
			String tmp = f.getSelectedFile().getAbsolutePath();
			if (extension != null && !tmp.toLowerCase().endsWith(ext))
				tmp += ext;
			return tmp;
		} else
			return null;
	}

	/** Create an object input stream.
	 * */
	public static ObjectInputStream createInputStream(
		String filename,
		boolean compressed)
		throws Exception {
		InputStream f = new FileInputStream(filename);
		if (COMPRESS_FILES && compressed)
			f = new GZIPInputStream(f);
		return new ObjectInputStream(f);
	}

	/** Create an object output stream.
	 * */
	public static ObjectOutputStream createOutputStream(
		String filename,
		boolean compressed)
		throws Exception {
		OutputStream f = new FileOutputStream(filename);
		if (COMPRESS_FILES && compressed)
			f = new GZIPOutputStream(f);
		return new ObjectOutputStream(f);
	}

	/** Create an object input stream.
	 * */
	public static ObjectInputStream createInputStream(String filename)
		throws Exception {
		return createInputStream(filename, true);
	}

	/** Create an object output stream.
	 * */
	public static ObjectOutputStream createOutputStream(String filename)
		throws Exception {
		return createOutputStream(filename, true);
	}

	/**
	 * Filter for the jgraphpad file format (*.pad or *.lib)
	 */
	protected class MyFileFilter extends javax.swing.filechooser.FileFilter {

		/** Extension for this file format.
		 */
		protected String ext;
		/** Full extension for this file format (the Point and the extension)
		 */
		protected String fullExt;
		/** Descrption of the File format
		 */
		protected String desc;

		/** Constructor for the Graphpad specific file format
		 *
		 */
		public MyFileFilter(String extension, String description) {
			ext = extension.toLowerCase();
			fullExt = "." + ext;
			desc = description;
		}

		/** Returns true if the file ends with the full extension or
		 *  if the file is a directory
		 *
		 */
		public boolean accept(File file) {
			return file.isDirectory()
				|| file.getName().toLowerCase().endsWith(fullExt);
		}

		/** returns the desc
		 */
		public String getDescription() {
			return desc;
		}

	}

}
