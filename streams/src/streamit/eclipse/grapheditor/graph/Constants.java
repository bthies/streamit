/*
 * Created on Dec 12, 2003
 *
 */
package streamit.eclipse.grapheditor.graph;

import java.awt.Dimension;

/**
 * Class that contains constants.
 * @author jcarlos
 *
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
	
	static final String TEXT_SIZE = "4";
	static final String HTML_TSIZE_BEGIN = "<H"+ Constants.TEXT_SIZE +">";
	static final String HTML_TSIZE_END = "</H"+ Constants.TEXT_SIZE +">";
	static final String HTML_LINE_BREAK = "<BR>";
	
	static final int X_SEPARATION = 50; // distance between nodes in x direction
	static final int MIN_WIDTH =50; // minimum width that a graph node can have
	static final Dimension DEFAULT_DIMENSION = new Dimension(300, 200);
	static int x = 200;
	static int y = 100;
}
