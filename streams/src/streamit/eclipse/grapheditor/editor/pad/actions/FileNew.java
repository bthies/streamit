/*
 */
package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.event.ActionEvent;

import org.jgraph.graph.GraphLayoutCache;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.pad.GPDocument;
import streamit.eclipse.grapheditor.editor.pad.GPGraph;
import streamit.eclipse.grapheditor.editor.pad.GPSelectProvider;
import streamit.eclipse.grapheditor.editor.pad.GraphModelProvider;
import streamit.eclipse.grapheditor.graph.GraphStructure;
import streamit.eclipse.grapheditor.graph.NodeCreator;

/**
 * Action that creates a new document in the Graph Editor to allow the editing of a new graph structure.
 * @author jcarlos
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
	 * Create a new document in the Graph Editor to allow the editing of a new graph structure. 
	 */
	public void actionPerformed(ActionEvent e) {
		
		GPSelectProvider selectProvider = new GPSelectProvider(graphpad.getFrame() );
		selectProvider.show() ;
		if (selectProvider.getAnswer() == GPSelectProvider.OPTION_CANCEL )
			return;
		
		/** Set the graph model provider (should be the default) */
		GraphModelProvider graphModelProvider = selectProvider.getSelectedGraphModelProvider() ;
		if (graphModelProvider == null)
			return;

		/** Create a new graph structure */
		GraphStructure graphStruct = new GraphStructure();
		
		/** Set the JGraph properties for the graph */	
		GPGraph gGraph = new GPGraph(graphStruct.getGraphModel());
		gGraph.setEditable(false);
		gGraph.setDisconnectable(false);
		gGraph.setGraphLayoutCache(new GraphLayoutCache(graphStruct.getGraphModel(), gGraph, false, true));
		graphStruct.setJGraph(gGraph);
	
		/** Add a new document to the graph editor */
		GPDocument doc= graphpad.addDocument(null, graphModelProvider, gGraph , gGraph.getModel(), graphStruct, null);
		
		/** Create a default toplevel pipeline for the graph editor. There should always be a toplevel
		 * node present. */
		NodeCreator.createDefaultToplevelPipeline(graphStruct); 

		/** Create the hierarchy panel using the graphstructure */
		graphpad.getCurrentDocument().treePanel.setGraphStructure(graphpad.getCurrentDocument().getGraphStructure());		
		graphpad.getCurrentDocument().treePanel.createJTree();
		graphpad.getCurrentDocument().updateUI();						
		graphpad.update();
	}
	/** Empty implementation. 
	 *  This Action should be available
	 *  each time.
	 */
	public void update(){};

}
