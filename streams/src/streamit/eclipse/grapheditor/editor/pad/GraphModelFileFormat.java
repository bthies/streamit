/*
 * @(#)GraphModelFileFormat.java	1.0 17.02.2003
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

import java.net.URL;
import java.util.Hashtable;

import javax.swing.JComponent;
import javax.swing.filechooser.FileFilter;

import org.jgraph.graph.GraphModel;

/**This is the interface to implement a file format
 * for a specific graph model provider.<br>
 * <br>
 * You must implement all methods and the corresponding
 * graph model provider must return this file format.
 *
 * @see GraphModelProvider#getGraphModelFileFormats()
 * @author luzar
 * @version 1.0
 */
public interface GraphModelFileFormat {

	/** Returns the JFileChooser file filter
	 *  for this file format.
	 */
	public FileFilter getFileFilter();

	/** Returns the File Extension for this
	 *  file format (without a point)
	 *
	 */
	public String getFileExtension();

	/** Writes the graph model with this file format.
	 *
	 *  @param file The file (with extension)
	 *  @param properties The write properties for this file format.
	 *  @param gpGraph The current graph.
	 *  @param graphModel The current graph model
	 */
	public void write(
		URL file,
		Hashtable properties,
		GPGraph gpGraph,
		GraphModel graphModel)
		throws Exception;

	/** Gets a accessory component for the
	 *  JFileChooser. If the return value is
	 *  <tt>null</tt> no accessory will show.
	 *  The component was used while the write operation.
	 *
	 */
	public JComponent getWriteAccessory();

	/** Returns a Hashtable with write properties.
	 *  The source for this properties
	 *  can be the accessory or other
	 *  Frames which were shown while calling this method.
	 *  All Properties in this Hashtable should be
	 *  serializable.
	 */
	public Hashtable getWriteProperties(JComponent accessory);

	/** Reads the graph from the filename and returns the
	 *  new model for the graph.
	 *
	 *  @param filename The URL for the file
	 *  @param properties The properties for the read process
	 *  @param gpGraph The current Graph
	 *  @return The new Graph model or null to cancel the load
	 * 			process without an Exception.
	 */
	public GraphModel read(
		URL file,
		Hashtable properties,
		GPGraph gpGraph)
		throws Exception;

	/** Gets a accessory component for the
	 *  JFileChooser. If the return value is
	 *  null no accessory will show.
	 *  The component was used while the read operation.
	 *
	 */
	public JComponent getReadAccessory();

	/** Returns a Hashtable with read properties.
	 *  The source for this properties
	 *  can be the accessory or other
	 *  Frames which were shown while calling this method.
	 *
	 */
	public Hashtable getReadProperties(JComponent accessory);

}
