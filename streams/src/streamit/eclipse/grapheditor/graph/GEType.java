/*
 * Created on Jun 25, 2003
 *
 */
package streamit.eclipse.grapheditor.graph;

/**
 * GEType contains the possible type values that a GEStreamNode can obtain.
 * @author jcarlos 
 */
public class GEType {
	
		//public static final String FILTER = "FILTER";
		public static final String PHASED_FILTER = "PHASED_FILTER"; 
		public static final String SPLITTER =  "SPLITTER";
		public static final String JOINER = "JOINER";
		public static final String PIPELINE = "PIPELINE";
		public static final String SPLIT_JOIN = "SPLIT_JOIN";
		public static final String FEEDBACK_LOOP = "FEEDBACK_LOOP";
 
 		public static final String[] ALL_STREAM_TYPES = {"PHASED_FILTER", 
														 "SPLITTER",
														 "JOINER",
														 "PIPELINE",
														 "SPLIT_JOIN", 
														 "FEEDBACK_LOOP"};
														 
														 
		public static String GETypeToString(String type)
		{
			System.out.println("Type: "+type);
			if (type == GEType.PHASED_FILTER)
			{
				return "filter";
			}
			else if (type == GEType.PIPELINE)
			{
				return "pipeline";
			}
			else if (type == GEType.SPLITTER)
			{
				return "splitter";
			}
			else if (type == GEType.JOINER)
			{
				return "joiner";
			}
			else if (type == GEType.SPLIT_JOIN)
			{
				return "splitjoin";
			}
			else if(type == GEType.FEEDBACK_LOOP)
			{
				return "feedbackloop";
			}
			else
			{
				return "NON EXISTANT TYPE";
			}
		}
		
		
		
}
