/*
 * @(#)GraphModelProvider.java	1.0 17.02.2003
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

import java.util.Map;

import org.jgraph.graph.GraphModel;

/**A Graph model provider for a user specific 
 * graph model. One GPDocument has one graph model provider.
 * One graph model provider can have multiple graph model file formats.
 * 
 * @see GPDocument
 * @see GraphModelFileFormat
 * 
 * @author luzar
 * @version 1.0
 */
public interface GraphModelProvider {

	/** const to specify an ellipse vertex
	 * 
	 *  @see #createCell(GraphModel, int, Object, Map)
	 */
	public static final int CELL_VERTEX_ELLIPSE = 1;

	/** const to specify a default vertex
	 * 
	 *  @see #createCell(GraphModel, int, Object, Map)
	 */
	public static final int CELL_VERTEX_DEFAULT = 2;

	/** const to specify an image vertex
	 * 
	 *  @see #createCell(GraphModel, int, Object, Map)
	 */
	public static final int CELL_VERTEX_IMAGE = 3;
	
	/** const to specify a text vertex
	 * 
	 *  @see #createCell(GraphModel, int, Object, Map)
	 */
	public static final int CELL_VERTEX_TEXT = 4;

	/** const to specify a default port
	 * 
	 *  @see #createCell(GraphModel, int, Object, Map)
	 */
	public static final int CELL_PORT_DEFAULT = 5;

	/** const to specify a default edge
	 * 
	 *  @see #createCell(GraphModel, int, Object, Map)
	 */
	public static final int CELL_EDGE_DEFAULT = 6;

	/** Returns the presentation name for this 
	 *  provider. If there are more than one
	 *  provider avialable a frame 
	 *  was shown at the file new command.
	 * 
	 *  At this frame you can select the model provider
	 *  and the renderer for this list uses this method.
	 */
	public String getPresentationName();

	/** Creates a new cell for the corresponding graph model
	 * 
	 *  @param cellType One of the const values with the prefix CELL_.
	 * 					 If you need to add you own const values use number > 1000.
	 *  @param userObject The user object for the new Cell
	 *  @param map The map with the corresponding properties for this cell.
	 * 
	 */	
	public Object createCell(GraphModel model, int cellType, Object userObject, Map map);

	/** Adds the port to the vertex.
	 * 
	 *  @param vertex Vertex from the corresponding graph model
	 *  @param port A Port form the corresponding graph model
	 */	
	public void addPort(Object vertex, Object port);

	/** Creates a clean and empty graph model
	 */	
	public GraphModel createCleanGraphModel();
	
	/** Creates a clean graph for this graph model provider.
	 *  If needed, you can set at this method a user defined
	 *  view factory.
	 */
	public GPGraph createCleanGraph(GraphModel model);

	/** Returns true if the method mutateTo can convert
	 *  to the other graph model class.
	 *  
	 *  @param otherGraphModelClass The Class object for the
	 * 								 other Graph Model
	 * 
	 * @see #mutateTo(GraphModel, Class)
	 */
	public boolean isMutateAbleTo(Class otherGraphModelClass);

	/** The graph model <tt>sourceGraphModel</tt> is a corresponding
	 *  graph model for this provider. The <tt>otherGraphModelClass</tt>
	 *  specifies another graph model. This method converts the 
	 *  source graph model to a graph model like the class parameter
	 *  specifies. The new instance, witch is corresponding 
	 *  with the <tt>otherGraphModelClass</tt> parameter, 
	 *  is the return value.
	 * 
	 *  @param sourceGraphModel A graph model from this provider.
	 *  @param otherGraphModelClass A graph model from another provider.
	 *  @param the new graph model.
	 */
	public GraphModel mutateTo(GraphModel sourceGraphModel, Class otherGraphModelClass);

	/** Returns all possible file formats for this
	 *  graph model provider.
	 * 
	 */
	public GraphModelFileFormat[] getGraphModelFileFormats();


	
}
