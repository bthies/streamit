/*
 * Created on Nov 25, 2003
 *
 */
package streamit.eclipse.grapheditor.graph;

import java.awt.Rectangle;
import java.util.Properties;

import org.jgraph.JGraph;
import org.jgraph.graph.GraphLayoutCache;

import streamit.eclipse.grapheditor.graph.utils.StringTranslator;

/**
 * NodeCreator handles the creation of GEStreamNodes in the GraphEditor. 
 * @author jcarlos
 */
public class NodeCreator {
	
	/**
	 * Create a GEStreamNode.
	 * @param properties Properties
	 * @param jgraph JGraph
	 * @param bounds Rectangle
	 * @param graphStruct GraphStructure
	 * @return GEStreamNode
	 */
	public static GEStreamNode nodeCreate(Properties properties, JGraph jgraph, Rectangle bounds, GraphStructure graphStruct)
	{
		if ((properties != null) && (jgraph != null) && (bounds != null) && (graphStruct != null))
		{
	//		GraphLayoutCache glc = jgraph.getGraphLayoutCache();
			GEStreamNode node = null;
		
			String name = properties.getProperty(GEProperties.KEY_NAME);
			String type = properties.getProperty(GEProperties.KEY_TYPE);
			

			if (GEType.PHASED_FILTER == type)
			{
				node = new GEPhasedFilter(name, graphStruct);
				((GEPhasedFilter)node).setPushPopPeekRates(Integer.parseInt(properties.getProperty(GEProperties.KEY_PUSH_RATE)),
														   Integer.parseInt(properties.getProperty(GEProperties.KEY_POP_RATE)),
														   Integer.parseInt(properties.getProperty(GEProperties.KEY_PEEK_RATE)));
			}
			else if (GEType.SPLITTER == type)
			{
				String w = properties.getProperty(GEProperties.KEY_SPLITTER_WEIGHTS);
				int [] weights = StringTranslator.weightsToInt(w);
				node = new GESplitter(name, weights);	
				((GESplitter) node).setDisplay(jgraph);	
			}
			else if (GEType.JOINER == type)
			{
				String w = properties.getProperty(GEProperties.KEY_JOINER_WEIGHTS);
				int [] weights = StringTranslator.weightsToInt(w);
				node = new GEJoiner(name, weights);
				((GEJoiner) node).setDisplay(jgraph);	
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
	/**
	 * Construct a the GEStreamNode specified by the properties.
	 * @param node GEStreamNode
	 * @param properties Properties
	 * @param graphStruct GraphStructure
	 * @param bounds Rectangle
	 */
	private static void construct(GEStreamNode node, Properties properties, GraphStructure graphStruct,  Rectangle bounds)
	{
		GraphLayoutCache glc = graphStruct.getJGraph().getGraphLayoutCache();
		node.setOutputTape(properties.getProperty(GEProperties.KEY_OUTPUT_TAPE));
		node.setInputTape(properties.getProperty(GEProperties.KEY_INPUT_TAPE));
			
		GEContainer parentNode = graphStruct.containerNodes.getContainerNodeFromName(properties.getProperty(GEProperties.KEY_PARENT));

		parentNode.addNodeToContainer(node);		
		node.setDepthLevel(parentNode.getDepthLevel() + 1);

		/** The node that we are adding is a container, must add it to container list 
		 * at corresponding level **/
		if ((node.getType() == GEType.PIPELINE) || (node.getType() == GEType.SPLIT_JOIN) || 
			(node.getType() == GEType.FEEDBACK_LOOP))
			{	
				graphStruct.containerNodes.addContainerToLevel(parentNode.getDepthLevel() + 1, node);	
			}

		IFileMappings.addIFileMappings(node, graphStruct.getIFile());
//		glc.insert(new Object[] {node}, null, null, null, null);
//		glc.setVisible(node, true);
		node.initDrawAttributes(graphStruct, bounds);
		graphStruct.getJGraph().getGraphLayoutCache().
					setVisible(node, true);
		graphStruct.getGraphModel().edit(graphStruct.getAttributes(), graphStruct.getConnectionSet(), null, null);
	}
	

	/**
	 * Create the TopLevel GEPipeline for a corresponding to graphStruct.
	 * @param graphStruct GraphStructure.
	 */
	public static void createDefaultToplevelPipeline(GraphStructure graphStruct)
	{
		Properties props  = new Properties();
		String name  = "TopLevelPipeline";
	
		props.setProperty(GEProperties.KEY_NAME, name);
		props.setProperty(GEProperties.KEY_TYPE, GEType.PIPELINE);
		props.setProperty(GEProperties.KEY_OUTPUT_TAPE, "void");
		props.setProperty(GEProperties.KEY_INPUT_TAPE, "void");
		props.setProperty(GEProperties.KEY_PARENT, "NONE");
		
		//TODO: Fix this hack (come up with different way to set graphStruct
		GEContainer node = new GEPipeline(name, graphStruct);
		graphStruct.setTopLevel(node);
		node.setDepthLevel(0);
		
		//TODO: Fix this hack
		//(GEPipeline) node).isExpanded  = true;
		graphStruct.containerNodes.addContainerToLevel(0, node);
		IFileMappings.addIFileMappings(node, graphStruct.getIFile());
		
		node.initDrawAttributes(graphStruct, new Rectangle(30, 30, 700, 700));
		graphStruct.getJGraph().getGraphLayoutCache().
					setVisible(node, true);
		graphStruct.getGraphModel().edit(graphStruct.getAttributes(), graphStruct.getConnectionSet(), null, null);
//		glc.insert(new Object[] {node}, null, null, null, null);
//		glc.setVisible(node, true);	
	}
	
	
	
}
