/*
 * Created on Nov 19, 2003
 *
 */
package streamit.eclipse.grapheditor.graph;

import java.util.Properties;
import java.util.StringTokenizer;

import org.jgraph.JGraph;


/**
 * @author jcarlos
 *
 */
public class GEProperties 
{
	
	/**
	 * Used as an identifier which is appended to the name of the pipeline.
	 * Avoid name conflicts when the default value for the name is always used.
	 */
	public static int id_count = 0;

	
	/**
	 * Key for the name property. The name of the GEStreamNode.
	 */
	public static final String KEY_NAME = "Name";

	/**
	 * Key for the input tape property. 
	 */
	public static final String KEY_INPUT_TAPE = "InputTape";

	/**
	 * Key for the output tape property.
	 */
	public static final String KEY_OUTPUT_TAPE  = "OutputTape";
    
    
	/**
	 * Key for the parent property. Indicate the container node that is
	 * the immediate parent of the GEStreamNode.
	 */
	public static final String KEY_PARENT = "Parent";
	
	/**
	 * Key for the parent property. Indicate the container node that is
	 * the immediate parent of the GEStreamNode.
	 */
	public static final String KEY_TYPE = "Type";
	
	/**
	 * Key for the splitter weights property. Only applies to Splitters.
	 */
	public static final String KEY_SPLITTER_WEIGHTS = "Splitter Weights";
	
	
	/**
	 * Key for the joiner weights property. Only applies to Joiners.
	 */
	public static final String KEY_JOINER_WEIGHTS = "Joiner Weights";
	
	
	/**
	 * Key for the Pop rate property. Only applies to filters.
	 */
	public static final String KEY_POP_RATE = "PopRate";
	
	/**
	 * Key for the Peek rate property. Only applies to filters.
	 */
	public static final String KEY_PEEK_RATE = "PeekRate";

	/**
	 * Key for the Push rate property. Only applies to filters.
	 */	
	public static final String KEY_PUSH_RATE = "PushRate";
	
	/**
	 * Get the default properties of a Splitter.
	 * @return Default Properties of Splitter.
	 */
	public static Properties getDefaultSplitterProperties()
	{
		Properties splitterProperties =  new Properties();
		
		splitterProperties.setProperty(GEProperties.KEY_NAME, Constants.SPLITTER + "_" + GEProperties.id_count++);
		splitterProperties.setProperty(GEProperties.KEY_TYPE, GEType.SPLITTER);
		splitterProperties.setProperty(GEProperties.KEY_PARENT, Constants.TOPLEVEL);
		splitterProperties.setProperty(GEProperties.KEY_OUTPUT_TAPE, Constants.VOID);
		splitterProperties.setProperty(GEProperties.KEY_INPUT_TAPE, Constants.VOID);
		splitterProperties.setProperty(GEProperties.KEY_SPLITTER_WEIGHTS, "1");
	
		return splitterProperties;	
	}
	
	/**
	 * Get the default properties of a Joiner.
	 * @return Default Properties of Joiner. 
	 */
	public static Properties getDefaultJoinerProperties()
	{
		Properties joinerProperties =  new Properties();
		
		joinerProperties.setProperty(GEProperties.KEY_NAME, Constants.JOINER + "_" + GEProperties.id_count++);
		joinerProperties.setProperty(GEProperties.KEY_TYPE, GEType.JOINER);
		joinerProperties.setProperty(GEProperties.KEY_PARENT, Constants.TOPLEVEL);
		joinerProperties.setProperty(GEProperties.KEY_OUTPUT_TAPE, Constants.VOID);
		joinerProperties.setProperty(GEProperties.KEY_INPUT_TAPE, Constants.VOID);
		joinerProperties.setProperty(GEProperties.KEY_JOINER_WEIGHTS, "1");
		
		return joinerProperties;
	}
	
	/**
	 * Get the Properties of the GEStreamNode.
	 * @param node GEStreamNode
	 * @return Properties of GeStreamNode. 
	 */
	public static Properties getNodeProperties(GEStreamNode node)
	{
		String type = node.getType();
		Properties properties = new Properties();
		
		properties.put(GEProperties.KEY_NAME, node.getName());
		properties.put(GEProperties.KEY_TYPE, type);
		properties.put(GEProperties.KEY_INPUT_TAPE, node.getInputTape());
		properties.put(GEProperties.KEY_OUTPUT_TAPE, node.getOutputTape());
		if (node.getEncapsulatingNode() != null)
		{
			properties.put(GEProperties.KEY_PARENT, node.getEncapsulatingNode().getName());
		}
		
		if (type == GEType.PHASED_FILTER)
		{
			try
			{
				GEWorkFunction wf = ((GEPhasedFilter) node).getWorkFunction();
				properties.put(GEProperties.KEY_PUSH_RATE, Integer.toString(wf.getPushValue())  );
				properties.put(GEProperties.KEY_POP_RATE, Integer.toString(wf.getPopValue()));
				properties.put(GEProperties.KEY_PEEK_RATE, Integer.toString(wf.getPeekValue()));
			}
			catch(ArrayIndexOutOfBoundsException e)
			{
				properties.put(GEProperties.KEY_PUSH_RATE, "0");
				properties.put(GEProperties.KEY_POP_RATE, "0");
				properties.put(GEProperties.KEY_PEEK_RATE, "0");
			}
		}
		else if (type == GEType.SPLIT_JOIN)
		{
			properties.put(GEProperties.KEY_SPLITTER_WEIGHTS, ((GESplitJoin)node).getSplitter().getWeightsAsString());
			properties.put(GEProperties.KEY_JOINER_WEIGHTS, ((GESplitJoin)node).getJoiner().getWeightsAsString());
		}
		else if (type == GEType.FEEDBACK_LOOP)
		{
			//TODO implement the case for feedbackloop in getNodeProperties()
			System.out.println("********************************************");
			System.out.println("************* NOT YET IMPLEMENTED **********");
			System.out.println("********************************************");
			properties.put(GEProperties.KEY_SPLITTER_WEIGHTS, ((GEFeedbackLoop)node).getSplitter().getWeightsAsString());
			properties.put(GEProperties.KEY_JOINER_WEIGHTS, ((GEFeedbackLoop)node).getJoiner().getWeightsAsString());
			
		}
		else if (type == GEType.JOINER)
		{
			properties.put(GEProperties.KEY_JOINER_WEIGHTS, ((GEJoiner)node).getWeightsAsString());
		}
		
		else if (type == GEType.SPLITTER)
		{
			properties.put(GEProperties.KEY_SPLITTER_WEIGHTS, ((GESplitter)node).getWeightsAsString());
		}
		
		return properties;
	}
	
	/**
	 * Set the properties of the GEStreamNode. The proeperties that cannot be changed
	 * are the type and the parent of the GEStreamNode. 
	 * @param properties
	 */
	public static void setNodeProperties(GEStreamNode node, Properties properties, JGraph jgraph)
	{
		String type = node.getType();
		
//		node.setName(properties.getProperty(GEProperties.KEY_NAME));
		node.setOutputTape(properties.getProperty(GEProperties.KEY_INPUT_TAPE));
		node.setInputTape(properties.getProperty(GEProperties.KEY_OUTPUT_TAPE));
					
		if (type == GEType.PHASED_FILTER)
		{
			GEWorkFunction wf = ((GEPhasedFilter) node).getWorkFunction();
			wf.setPushValue(Integer.parseInt(properties.getProperty(GEProperties.KEY_PUSH_RATE)));
			wf.setPopValue(Integer.parseInt(properties.getProperty(GEProperties.KEY_POP_RATE)));
			wf.setPeekValue(Integer.parseInt(properties.getProperty(GEProperties.KEY_PEEK_RATE)));
			
			((GEPhasedFilter) node).setDisplay(jgraph);
									
		}
		else if (type == GEType.SPLIT_JOIN)
		{
			String splitWeights = properties.getProperty(GEProperties.KEY_SPLITTER_WEIGHTS);
			String joinerWeights = properties.getProperty(GEProperties.KEY_JOINER_WEIGHTS);
			((GESplitJoin)node).getSplitter().setWeights(GEProperties.weightsToInt(splitWeights));
			((GESplitJoin)node).getJoiner().setWeights(GEProperties.weightsToInt(joinerWeights));
		}
		else if (type == GEType.FEEDBACK_LOOP)
		{
			//TODO implement the case for feedbackloop in getNodeProperties()
			System.out.println("********************************************");
			System.out.println("************* NOT YET IMPLEMENTED **********");
			System.out.println("********************************************");
		
			String splitWeights = properties.getProperty(GEProperties.KEY_SPLITTER_WEIGHTS);
			String joinerWeights = properties.getProperty(GEProperties.KEY_JOINER_WEIGHTS);
			((GESplitJoin)node).getSplitter().setWeights(GEProperties.weightsToInt(splitWeights));
			((GESplitJoin)node).getJoiner().setWeights(GEProperties.weightsToInt(splitWeights));
				
		}
		else if (type == GEType.JOINER)
		{
			String joinerWeights = properties.getProperty(GEProperties.KEY_JOINER_WEIGHTS);
			((GESplitJoin)node).getJoiner().setWeights(GEProperties.weightsToInt(joinerWeights));
		}
			
		else if (type == GEType.SPLITTER)
		{
			String splitWeights = properties.getProperty(GEProperties.KEY_SPLITTER_WEIGHTS);
			((GESplitJoin)node).getSplitter().setWeights(GEProperties.weightsToInt(splitWeights));
		}
			
		
		
	}
	
	/**
	 * Convert the String representation of the weights to an int[].
	 * @param w String representation of the weights.
	 * @return int[] corresponding to String representation of the weigths. 
	 */
	public static int[] weightsToInt(String w)
	{
		if (w == null)
			return new int[]{};
		StringTokenizer t = new StringTokenizer(w, ",");
		int weights[];

		weights = new int[t.countTokens()];
		int i = 0;
		while (t.hasMoreTokens()){
			weights[i] = Integer.parseInt(t.nextToken());
			i++;
		}

		return weights;
	}
}

