/*
 * @(#)DefaultGraphModelFileFormatSerial.java	1.0 17.02.2003
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Hashtable;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.filechooser.FileFilter;

import org.jgraph.graph.GraphModel;
import streamit.eclipse.grapheditor.editor.pad.resources.Translator;

/**File format for the default graph model.
 * The file format writes a serialized object output stream
 * file with the graph as content.
 *
 * @author luzar
 * @version 1.0
 */
public class DefaultGraphModelFileFormatSerial
	implements GraphModelFileFormat {

	/** accessory component. A checkbox for the
	 *  zipped output or unzipped output
	 *
	 */
	JComponent compZipSelect;

	/** file filter for this file format
	 */
	FileFilter fileFilter;

	/** a const value for the key at the properties hashtable
	 */
	public static final String COMPRESS_WITH_ZIP = "CompressWithZip";

	/**
	 * Constructor for DefaultGraphModelFileFormatSerial.
	 */
	public DefaultGraphModelFileFormatSerial() {
		fileFilter = new FileFilter() {
			/**
			 * @see javax.swing.filechooser.FileFilter#accept(File)
			 */
			public boolean accept(File f) {
				if (f == null)
					return false;
				if (f.getName() == null)
					return false;
				if (f.getName().endsWith(".pad"))
					return true;
				if (f.isDirectory())
					return true;

				return false;
			}

			/**
			 * @see javax.swing.filechooser.FileFilter#getDescription()
			 */
			public String getDescription() {
				return Translator.getString("FileDesc.JGraphpadDiagram"); /*#Finished Original="JGraphpad Diagram (*.pad)"*/
			}
		};
		compZipSelect = new JCheckBox(Translator.getString("zipCompress"));
	}

	/** returns <tt>pad</tt>
	 */
	public String getFileExtension(){
		return "pad";
	}


	/** Returns a file filter for the <tt>pad</tt> extension.
	 *
	 * @see org.jgraph.pad.GraphModelFileFormat#getFileFilter()
	 */
	public FileFilter getFileFilter() {
		return fileFilter;
	}

	/** Returns the compZipSelect object.
	 *
	 * @see #compZipSelect
	 * @see org.jgraph.pad.GraphModelFileFormat#getWriteAccessory()
	 */
	public JComponent getWriteAccessory() {
		return compZipSelect;
	}

	/** Returns null
	 *
	 * @see org.jgraph.pad.GraphModelFileFormat#getReadAccessory()
	 */
	public JComponent getReadAccessory() {
		return null;
	}

	/**
	 * Writes the graph as Object Output stream
	 *
	 * @see org.jgraph.pad.GraphModelFileFormat#write(String, Hashtable, GPGraph, GraphModel)
	 */
	public void write(
		URL file,
		Hashtable properties,
		GPGraph gpGraph,
		GraphModel graphModel)
		throws Exception {

		OutputStream f = new FileOutputStream(file.getFile());
		if (properties != null &&
			properties.get(COMPRESS_WITH_ZIP) != null &&
			((Boolean)properties.get(COMPRESS_WITH_ZIP)).booleanValue() )
			f = new GZIPOutputStream(f);

		ObjectOutputStream out = new ObjectOutputStream(f);
		out.writeObject(gpGraph.getArchiveableState());
		out.flush();
		out.close();
	}

	/**
	 * Puts the value from the checkbox into the properties hashtable
	 *
	 * @see org.jgraph.pad.GraphModelFileFormat#getWriteProperties(JComponent)
	 */
	public Hashtable getWriteProperties(JComponent accessory) {
		Hashtable properties = new Hashtable();
		if (!(accessory instanceof JCheckBox)){
			return properties;
		}
		properties.put(COMPRESS_WITH_ZIP, new Boolean(((JCheckBox)accessory).isSelected() ));
		return properties;
	}

	/**Reads the File form the Object input stream.
	 * Tempts to load from a zipped input stream.
	 * If this procedure fails the method tempts
	 * to load without the zipped option.
	 *
	 * @see org.jgraph.pad.GraphModelFileFormat#read(String, Hashtable, GPGraph)
	 */
	public GraphModel read(
		URL file,
		Hashtable properties,
		GPGraph gpGraph)
		throws Exception {

		Object o = null;
		
			try {
				InputStream f = file.openStream();
				f = new GZIPInputStream(f);
				ObjectInputStream in = new ObjectInputStream(f);
				o = in.readObject();
				in.close();
			} catch (Exception ex) {
				InputStream f = file.openStream();
				ObjectInputStream in = new ObjectInputStream(f);
				o = in.readObject();
				in.close();
			}
		gpGraph.setArchivedState(o);
		return gpGraph.getModel() ;
	}

	/**Returns null
	 *
	 * @see org.jgraph.pad.GraphModelFileFormat#getReadProperties(JComponent)
	 */
	public Hashtable getReadProperties(JComponent accessory) {
		return null;
	}

}
