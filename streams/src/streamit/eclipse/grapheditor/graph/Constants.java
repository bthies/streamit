/*
 * Created on Dec 12, 2003
 *
 */
package streamit.eclipse.grapheditor.graph;

import java.awt.Dimension;

/**
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
	static final String VOID = "Void";
	static final String FLOAT = "Float";
	static final int X_SEPARATION = 50; // distance between nodes in x direction
	static final int MIN_WIDTH =50; // minimum width that a graph node can have
	static final Dimension DEFAULT_DIMENSION = new Dimension(300, 200);
	static int x = 200;
	static int y = 100;
}
