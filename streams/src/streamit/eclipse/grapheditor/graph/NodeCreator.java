/*
 * Created on Nov 25, 2003
 *
 */
package streamit.eclipse.grapheditor.graph;

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Properties;

import org.eclipse.core.resources.IFile;
import org.jgraph.JGraph;
import org.jgraph.graph.GraphLayoutCache;

import streamit.eclipse.grapheditor.graph.utils.StringTranslator;

/**
 * @author jcarlos
 *
 */
public class NodeCreator {

	public static HashMap IFileMappings = new HashMap();

	public static void addIFileMappings(Object[] cells, IFile ifile)
	{
		for (int i = 0; i < cells.length; i++)
		{
			System.out.println("Adding mapping " + i + " : "+ ((GEStreamNode)cells[i]).getName());
			IFileMappings.put(cells[i], ifile);
		}
	}

	public static void addIFileMappings (Object cell, IFile ifile)
	{
		IFileMappings.put(cell, ifile);
	}
	
	public static IFile getIFile(GEStreamNode node)
	{
		return (IFile) IFileMappings.get(node);
	}

	public static GEStreamNode nodeCreate(Properties properties, JGraph jgraph, Rectangle bounds, GraphStructure graphStruct)
	{
		if ((properties != null) && (jgraph != null) && (bounds != null) && (graphStruct != null))
		{
			
	//		GraphLayoutCache glc = jgraph.getGraphLayoutCache();
			GEStreamNode node = null;
		
			String name = properties.getProperty(GEProperties.KEY_NAME);
			String type = properties.getProperty(GEProperties.KEY_TYPE);
			
			if (GEType.PIPELINE == type)
			{
				//TODO: fix this hack
				node = new GEPipeline(name, graphStruct);
				
			}
			else if (GEType.PHASED_FILTER == type)
			{
				node = new GEPhasedFilter(name, graphStruct);
				((GEPhasedFilter)node).setPushPopPeekRates(Integer.parseInt(properties.getProperty(GEProperties.KEY_PUSH_RATE)),
														   Integer.parseInt(properties.getProperty(GEProperties.KEY_POP_RATE)),
														   Integer.parseInt(properties.getProperty(GEProperties.KEY_PEEK_RATE)));
			}
			else if (GEType.SPLIT_JOIN == type)
			{

				Properties joinerProperties = GEProperties.getDefaultJoinerProperties();
				joinerProperties.setProperty(GEProperties.KEY_PARENT, name);
				Properties splitterProperties = GEProperties.getDefaultSplitterProperties();
				splitterProperties.setProperty(GEProperties.KEY_PARENT, name);
				
				GESplitter splitter = new GESplitter(splitterProperties.getProperty(GEProperties.KEY_NAME), new int[]{1});
				GEJoiner joiner = new GEJoiner(joinerProperties.getProperty(GEProperties.KEY_NAME), new int[]{1});
				splitter.isNodeConnected = true;
				joiner.isNodeConnected = true;
				
				node = new GESplitJoin(name, splitter, joiner);
				
				int boundWidth  = Math.max(bounds.width, 400);
				int boundHeigth = Math.max(bounds.height, 600);
				construct(node, properties, graphStruct, new Rectangle(bounds.x, bounds.y, boundWidth, boundHeigth)); 
				construct(splitter, splitterProperties, graphStruct, new Rectangle(bounds.x + 80, bounds.y+80, 200, 120));
				construct(joiner, joinerProperties, graphStruct, new Rectangle(bounds.x + 80, bounds.y + boundHeigth - 150, 200, 120));
				
				return node;
			}
			else if (GEType.SPLITTER == type)
			{
				String w = properties.getProperty(GEProperties.KEY_SPLITTER_WEIGHTS);
				int [] weights = StringTranslator.weightsToInt(w);
				node = new GESplitter(name, weights);		
			}
			else if (GEType.JOINER == type)
			{
				String w = properties.getProperty(GEProperties.KEY_JOINER_WEIGHTS);
				int [] weights = StringTranslator.weightsToInt(w);
				node = new GEJoiner(name, weights);	
			}
			else if (GEType.FEEDBACK_LOOP == type)
			{
				
			}
			else
			{
				throw new IllegalArgumentException("Invalid type for GEStreamNode vertex (NodeCreator.java)");
			}
			
			NodeCreator.construct(node, properties, graphStruct, bounds);

			return node;
		}
		else 
		{
			throw new IllegalArgumentException();
		}
	}
	
	private static void construct(GEStreamNode node, Properties properties, GraphStructure graphStruct,  Rectangle bounds)
	{
		GraphLayoutCache glc = graphStruct.getJGraph().getGraphLayoutCache();
		node.setOutputTape(properties.getProperty(GEProperties.KEY_OUTPUT_TAPE));
		node.setInputTape(properties.getProperty(GEProperties.KEY_INPUT_TAPE));
			
		GEStreamNode parentNode = graphStruct.containerNodes.getContainerNodeFromName(properties.getProperty(GEProperties.KEY_PARENT));
		node.setEncapsulatingNode(parentNode);
		node.setDepthLevel(parentNode.getDepthLevel() + 1);
		
		if ((parentNode.getSuccesors().size() == 0) && (parentNode.getType() ==GEType.PIPELINE))
		{
			parentNode.addChild(node);
			node.isNodeConnected = true;
		}
	
		if ((node.getType() == GEType.PIPELINE) || (node.getType() == GEType.SPLIT_JOIN) || 
			(node.getType() == GEType.FEEDBACK_LOOP))
			{	
				graphStruct.containerNodes.addContainerToLevel(parentNode.getDepthLevel() + 1, node);	
			}
			
		//graphStruct.getGraphModel().insert(graphStruct.getCells().toArray(), graphStruct.getAttributes(), graphStruct.getConnectionSet(), null, null);	
		node.initDrawAttributes(graphStruct, bounds);
		
		glc.insert(new Object[] {node}, null, null, null, null);	
		//jgraph.getModel().insert(new Object[] {node}, null, null, null, null);


		NodeCreator.addIFileMappings(node, graphStruct.getIFile());
		
		// Not sure if it is necessary to insert into the model if we already inserted to glc
		graphStruct.getGraphModel().insert(new Object[] {node}, null, null, null, null);
		glc.setVisible(node, true);
	}
	

	
	public static void createDefaultToplevelPipeline(GraphStructure graphStruct)
	{
		Properties props  = new Properties();
		String name  = "TopLevelPipeline";
		
		GraphLayoutCache glc = graphStruct.getJGraph().getGraphLayoutCache();
		
		props.setProperty(GEProperties.KEY_NAME, name);
		props.setProperty(GEProperties.KEY_TYPE, GEType.PIPELINE);
		props.setProperty(GEProperties.KEY_OUTPUT_TAPE, "void");
		props.setProperty(GEProperties.KEY_INPUT_TAPE, "void");
		props.setProperty(GEProperties.KEY_PARENT, "NONE");
		
		//TODO: Fix this hack (come up with different way to set graphStruct
		GEStreamNode node = new GEPipeline(name, graphStruct);
		graphStruct.setTopLevel(node);
		node.setDepthLevel(0);
		
		//TODO: Fix this hack
		((GEPipeline) node).isExpanded  = true;
		
		graphStruct.containerNodes.addContainerToLevel(0, node);
		//graphStruct.getGraphModel().insert(graphStruct.getCells().toArray(), graphStruct.getAttributes(), graphStruct.getConnectionSet(), null, null);
	
		node.initDrawAttributes(graphStruct, new Rectangle(30, 30, 700, 700));
	
		glc.insert(new Object[] {node}, null, null, null, null);
		//graphStruct.getJGraph().getModel().insert(new Object[] {node}, null, null, null, null);


		NodeCreator.addIFileMappings(node, graphStruct.getIFile());

		// Not sure if it is necessary to insert into the model if we already inserted to glc
		//graphStruct.getGraphModel().insert(new Object[] {node}, null, null, null, null);
		glc.setVisible(node, true);	
	}
	
	
	
}
