/*
 * Created on Nov 19, 2003
 *
 */
package streamit.eclipse.grapheditor.graph;

import java.util.Properties;


/**
 * Class that contains the keys for the property maps of a GEStreamNode.
 * Also provides static methods to get the default properties of certain 
 * types of GEStreamNodes.
 * 
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
	 * Key for the level of the GEStreamNode.
	 */
	public static final String KEY_LEVEL = "Level";

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
	 * Key for the body of a FeedbackLoop.
	 */
	public static final String KEY_FLOOP_BODY = "FeedbackLoop Body";
	
	/**
	 * Key for the loop of a FeedbackLoop.
	 */
	public static final String KEY_FLOOP_LOOP = "FeedbackLoop Loop";
	
	/**
	 * Key for the index within a splitjoin. 
	 */
	public static final String KEY_INDEX_IN_SJ = "Index in Splitjoin";
	
	/**
	 * Key that determines wheter or not a Node is connected
	 */
	public static final String KEY_IS_CONNECTED = "Is Node Connected";
	
	
	
	/**
	 * Get the default properties of a GESplitter.
	 * @return Properties Default Properties of GESplitter.
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
	 * Get the default properties of a GEJoiner.
	 * @return Properties Default Properties of GEJoiner. 
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

}

