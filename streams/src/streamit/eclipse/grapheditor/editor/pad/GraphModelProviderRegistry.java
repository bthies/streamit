/*
 * @(#)GraphModelProviderRegistry.java	1.0 17.02.2003
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

import java.io.File;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.filechooser.FileFilter;

/**A registry for graph model providers. The methods
 * are static so that you can register graph model providers
 * before the startup from GPGraphpad.
 * 
 * 
 * @author luzar
 * @version 1.0
 *
 */
public class GraphModelProviderRegistry {

	/** Vector contains the registered GraphModelProvider objects.
	 * 
	 *  @see GraphModelProvider 
	 */
	protected static Vector graphModelProviders = new Vector();

	/** Vector contains the registered GraphModelFileFormat objects.
	 * 
	 *  @see GraphModelFileFormat
	 */
	protected static Vector graphModelFileFormats = new Vector();

	/** Adds the specified graph model provider.
	 *  
	 */
	public static void addGraphModelProvider(GraphModelProvider graphModelprovider) {
		// register graph model provider
		graphModelProviders.add(graphModelprovider);
		GraphModelFileFormat[] formats =
			graphModelprovider.getGraphModelFileFormats();

		// register all file formats
		for (int i = 0; i < formats.length; i++) {
			graphModelFileFormats.add(formats[i]);
		}
	}

	/** removes the specified graph model provider.
	 *  
	 */
	public static void removeGraphModelProvider(GraphModelProvider graphModelprovider) {
		// unregister graph model provider
		graphModelProviders.remove(graphModelprovider);

		GraphModelFileFormat[] formats =
			graphModelprovider.getGraphModelFileFormats();

		for (int i = 0; i < formats.length; i++) {
			// unregister file formats
			graphModelFileFormats.remove(formats[i]);
		}
	}
	
	/** returns all registered graph model providers
	 */
	public static GraphModelProvider[] getGraphModelProviders() {
		synchronized (graphModelProviders) {
			GraphModelProvider[] gmps =
				new GraphModelProvider[graphModelProviders.size()];
			for (int i = 0; i < graphModelProviders.size(); i++) {
				gmps[i] = (GraphModelProvider) graphModelProviders.get(i);
			}
			return gmps;
		}
	}

	/** Returns all registered file formats. 
	 *  To register a file format you must register the
	 *  graph model provider which contains graph model file formats.
	 * 
	 */
	public static GraphModelFileFormat[] getGraphModelFileFormats() {
		synchronized (graphModelFileFormats) {
			GraphModelFileFormat[] gmffs =
				new GraphModelFileFormat[graphModelFileFormats.size()];
			for (int i = 0; i < graphModelFileFormats.size(); i++) {
				gmffs[i] = (GraphModelFileFormat) graphModelFileFormats.get(i);
			}
			return gmffs;
		}

	}

	/** Returns the Graph model provider for the specific file.
	 * 
	 *  To find the correct the graph model provider the
	 *  method uses the accept method from the file filter
	 *  at the file format class.
	 *  
	 *  @see GraphModelFileFormat
	 *  @return the GraphModelProvider or <tt>null</tt> 
	 * 	if no GraphModelProvider was found.
	 */
	public static GraphModelProvider getGraphModelProvider(String filename) {
		return getGraphModelProvider(new File(filename));
		
	}

	/** Returns the Graph model provider for the specific file.
	 * 
	 *  To find the correct the graph model provider the
	 *  method uses the accept method from the file filter
	 *  at the file format class.
	 *  
	 *  @see GraphModelFileFormat
	 *  @return the GraphModelProvider or <tt>null</tt> 
	 * 	if no GraphModelProvider was found.
	 */
	public static GraphModelProvider getGraphModelProvider(File file) {
		Enumeration prov = graphModelProviders.elements();
		while (prov.hasMoreElements()) {
			GraphModelProvider gmp = (GraphModelProvider) prov.nextElement();

			GraphModelFileFormat[] gmffs = gmp.getGraphModelFileFormats();
			for (int i = 0; i < gmffs.length; i++) {

				GraphModelFileFormat ff = (GraphModelFileFormat) gmffs[i];
				if (ff.getFileFilter().accept(file))
					return gmp;
			}
		}
		return null;//new DefaultGraphModelProvider();
	}

	/** Returns the Graph model file format for the specific file.
	 * 
	 *  To find the correct the graph model file format the
	 *  method uses the accept method from the file filter
	 *  at the file format class.
	 *  
	 *  @see GraphModelFileFormat
	 *  @return the GraphModelFileFormat or <tt>null</tt> 
	 * 	if no GraphModelFileFormat was found.
	 */
	public static GraphModelFileFormat getGraphModelFileFormat(String filename) {
		return getGraphModelFileFormat(new File(filename));
	}

	/** Returns the Graph model file format for the specific file.
	 * 
	 *  To find the correct the graph model file format the
	 *  method uses the accept method from the file filter
	 *  at the file format class.
	 *  
	 *  @see GraphModelFileFormat
	 *  @return the GraphModelFileFormat or <tt>null</tt> 
	 * 	if no GraphModelFileFormat was found.
	 */
	public static GraphModelFileFormat getGraphModelFileFormat(File file) {
		Enumeration enum = graphModelFileFormats.elements();
		while (enum.hasMoreElements()) {
			GraphModelFileFormat ff = (GraphModelFileFormat) enum.nextElement();
			if (ff.getFileFilter().accept(file))
				return ff;
		}
		return null; //new DefaultGraphModelFileFormatXML();
	}

	/** Returns the Graph model file format for the specific file filter.
	 * 
	 *  To find the correct the graph model file format the
	 *  method iterates over all registered file formats 
	 *  and compares the addresses with the == Operator.
	 *  
	 *  @see GraphModelFileFormat
	 *  @return the GraphModelFileFormat or <tt>null</tt> 
	 * 	if no GraphModelFileFormat was found.
	 */
	public static GraphModelFileFormat getGraphModelFileFormat(FileFilter fileFilter) {
		Enumeration enum = graphModelFileFormats.elements();
		while (enum.hasMoreElements()) {
			GraphModelFileFormat ff = (GraphModelFileFormat) enum.nextElement();
			if (ff.getFileFilter() == fileFilter)
				return ff;
		}
		return null; //new DefaultGraphModelFileFormatXML();
	}
}
