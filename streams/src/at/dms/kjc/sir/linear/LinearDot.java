package at.dms.kjc.sir.linear;

import java.util.*;
import java.io.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;


/**
 * This class extends the main streamit dot printer to annotate the dot graphs with
 * linear analysis information. It colors linear nodes grey currently. Perhaps
 * more useful stuff will come later.
 **/
public class LinearDot extends StreamItDot {
    private LinearFilterAnalyzer linearData;
    /**
     * Make a LinearDot printer that prints out the dot graph using information
     * from the linear filter analyzer to annotate the graph (eg color it silly).
     **/
    public LinearDot(PrintStream outputstream,
		     LinearFilterAnalyzer anal) {
	super(outputstream);
	this.linearData = anal;
    }

    /** Create the dot code for a node with a constant component (red). **/
    String makeConstantLabelledNode(String label)
    {
        String name = getName();
        if (label == null) label = name;
        print(name + " [ color=palevioletred, style=filled, label=\"" + label + "\" ]\n");
        return name;
    }
    /** Create the dot code for a node with no constant component (cornflower blue). **/
    String makeNoConstantLabelledNode(String label)
    {
        String name = getName();
        if (label == null) label = name;
        print(name + " [ color=cornflowerblue, style=filled, label=\"" + label + "\" ]\n");
        return name;
    }


    
    /*
     * Override visitFilter to color filters that compute linear functions grey.
     **/

     public Object visitFilter(SIRFilter self,
                              JFieldDeclaration[] fields,
                              JMethodDeclaration[] methods,
                              JMethodDeclaration init,
                              JMethodDeclaration work,
                              CType inputType, CType outputType)
    {
	// code from StreamItDot
	String label = self.getIdent();
	try {
	    label += "\\npush=" + self.getPushInt();
	    label += "\\npop=" + self.getPopInt();
	    label += "\\npeek=" + self.getPeekInt();
	    if (self instanceof SIRTwoStageFilter) {
		SIRTwoStageFilter two = (SIRTwoStageFilter)self;
		label += "\\ninitPush=" + two.getInitPush();
		label += "\\ninitPop=" + two.getInitPop();
		label += "\\ninitPeek=" + two.getInitPeek();
	    }
	} catch (Exception e) {
	    // if constants not resolved for the ints, will get an exception
	}

	// if this filter has a linear representation (which we
	// check with the linear analyzer that we have, return a
	// grey node. Otherwise, return a normal node.
	if (this.linearData.hasLinearRepresentation(self)) {
	    LinearFilterRepresentation lfr;
	    lfr = this.linearData.getLinearRepresentation(self);
	    // Make a dark grey node if the linear representation has a constant
	    // component, make a light grey node if the linear rep has a constant
	    // component.
	    if (lfr.hasConstantComponent()) {
		return new NamePair(makeConstantLabelledNode(label));
	    } else {
		return new NamePair(makeNoConstantLabelledNode(label));
	    }
	} else {
	    return new NamePair(makeLabelledNode(label));
	}
    }


    /**
     * Prints dot graph of <str> to <filename>, using LinearFilterAnalyzer lfa.
     */
    public static void printGraph(SIRStream str, String filename,
				  LinearFilterAnalyzer lfa) {
	try {
	    FileOutputStream out = new FileOutputStream(filename);
	    StreamItDot dot = new LinearDot(new PrintStream(out), lfa);
	    dot.print("digraph streamit {\n");
	    str.accept(dot);
	    dot.print("}\n");
	    out.flush();
	    out.close();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }


    
}

