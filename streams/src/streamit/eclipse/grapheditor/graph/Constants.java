/*
 * Created on Dec 12, 2003
 *
 */
package streamit.eclipse.grapheditor.graph;

import java.awt.Dimension;

/**
 * Class that contains constants used in the graph structure.
 * @author jcarlos
 */
public class Constants {

	static final String TOPLEVEL = "Toplevel";
	static final String PIPELINE = "Pipeline";
	static final String FILTER = "Filter";
	static final String JOINER = "Joiner";
	static final String SPLITTER = "Splitter";
	static final String SPLITJOIN = "Splitjoin";
	static final String FEEDBACKLOOP = "FeedbackLoop";
	
	static final String VOID = "void";
	static final String FLOAT = "float";
	static final String INT = "int";
	
	public static final String CONNECTED = "Connected";
	public static final String DISCONNECTED ="Disconnected";
	
	
	static final int DEFAULT_ADJACENT = 4; // number of adjacent nodes that are allowed before a "for" loop is substituted
										   // in place of each instance. 
	
	
	static final String ID_TAG = "_ID";
	
	static final String TEXT_SIZE = "4";
	static final String HTML_TSIZE_BEGIN = "<H"+ Constants.TEXT_SIZE +">";
	static final String HTML_TSIZE_END = "</H"+ Constants.TEXT_SIZE +">";
	static final String HTML_LINE_BREAK = "<BR>";
	static final String TAB = "     ";
	static final String newLine = "\n";
	
	static final int X_SEPARATION = 150; // distance between nodes in x direction
	static final int Y_SEPARATION = 150; // distance between nodes in y direction
	
	public static final int X_MARGIN_OF_CONTAINER = 80;
	public static final int Y_MARGIN_OF_CONTAINER = 120;
	
	public static final int TOPLEVEL_LOC_X = 700; 
	public static final int TOPLEVEL_LOC_Y = 200;
	
	public static final String[] TAPE_VALUES = new String[] {"void", "int", "float"};
	
	public static final int DEFAULT_WEIGHT = 100; // the default value for the weight 
	
	
	public static final String[] SPLITTER_TYPE_NAMES = new String[]{"duplicate", "roundrobin"};
	public static final String[] JOINER_TYPE_NAMES = new String[]{"roundrobin"};
		
	static final int MIN_WIDTH =50; // minimum width that a graph node can have
	static final Dimension DEFAULT_DIMENSION = new Dimension(200, 120);
	static int x = 200;
	static int y = 100;
}
