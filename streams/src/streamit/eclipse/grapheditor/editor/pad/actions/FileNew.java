/*
 * @(#)FileNew.java	1.2 30.01.2003
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

import java.awt.event.ActionEvent;

import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.GraphLayoutCache;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.pad.GPDocument;
import streamit.eclipse.grapheditor.editor.pad.GPGraph;
import streamit.eclipse.grapheditor.editor.pad.GPSelectProvider;
import streamit.eclipse.grapheditor.editor.pad.GraphModelProvider;
import streamit.eclipse.grapheditor.graph.GraphStructure;
import streamit.eclipse.grapheditor.graph.NodeCreator;

/**
 * 
 * @author sven.luzar
 * @version 1.0
 *
 */
public class FileNew extends AbstractActionDefault {

	/**
	 * Constructor for FileNew.
	 * @param graphpad
	 * @param name
	 */
	public FileNew(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		
		GPSelectProvider selectProvider = new GPSelectProvider(graphpad.getFrame() );
		selectProvider.show() ;
		if (selectProvider.getAnswer() == GPSelectProvider.OPTION_CANCEL )
			return;
		
		
		GraphModelProvider graphModelProvider = selectProvider.getSelectedGraphModelProvider() ;
		if (graphModelProvider == null)
			return;

		GraphStructure graphStruct = new GraphStructure();
		GPGraph gGraph = new GPGraph(graphStruct.getGraphModel());
		gGraph.setEditable(false);
		gGraph.setDisconnectable(false);
		gGraph.setGraphLayoutCache(new GraphLayoutCache(graphStruct.getGraphModel(), gGraph, false, true));
		graphStruct.setJGraph(gGraph);
	
		
		GPDocument doc= graphpad.addDocument(null, graphModelProvider, gGraph , gGraph.getModel(), graphStruct, null);
		
		NodeCreator.createDefaultToplevelPipeline(graphStruct); 
						
		graphpad.update();
	}
	/** Empty implementation. 
	 *  This Action should be available
	 *  each time.
	 */
	public void update(){};

}
