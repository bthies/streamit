package at.dms.kjc.sir.statespace;

import java.io.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;


/**
 * This class extends the main StreamIt dot (graph) printer to annotate the dot graphs with
 * linear analysis information. It colors linear nodes blue, linear nodes with
 * a constant component violet red and linear structures pink. <p>
 *
 * It also adds linear redundancy information
 **/
public class LinearDot extends StreamItDot {
    LinearAnalyzer linearData;
    LinearRedundancyAnalyzer linearRedundancy;

    /**
     * Make a LinearDot printer that prints out the dot graph using information
     * from the linear filter analyzer to annotate the graph (eg color it silly).
     * Also include redundancy information.
     **/
    public LinearDot(PrintStream outputstream,
		     LinearAnalyzer anal, LinearRedundancyAnalyzer lra) {
	super(outputstream);
	this.linearData = anal;
	this.linearRedundancy = lra;
    }

    
    /**
     * returns true if we have redundancy information. (eg if we are going to include
     * redundancy percentages in the titles.
     **/
    public boolean hasRedundancyInformation() {return (this.linearRedundancy != null);}


    //------------------ Override some of the key methods ------------------
    

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

    /**
     * Creates a string that represents the linear redundancy of this
     * str. If no redundancy information is present in this LinearDot
     * or if there is no information about str, then a blank string
     * is returned.
     **/
    public String makeRedundancyString(SIRStream str) {
	// end if LinearDot has no redundancy information.
	if (!(this.hasRedundancyInformation())) {return "";}
	
	// end if this str has no linear redundancy information
	if (!this.linearRedundancy.hasRedundancy(str)) {return "";}

	// otherwise, actually return the percent string
	LinearRedundancy strRedundancy = this.linearRedundancy.getRedundancy(str);
	
	return strRedundancy.makeShortRedundancyString();
    }
    
    /**
     * Override visitFilter to color filters that compute linear functions.
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
	    label += "\\n" + this.makeRedundancyString(self);
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
     * Override the string used to create a new subgraph, to color it if we have a
     * linear rep for it.
     **/
    public String getClusterString(SIRStream self) {
	// if we have a linear rep of this object, color the resulting dot graph rose.
	if (linearData.hasLinearRepresentation(self)) {
	    return ("subgraph cluster_" +
		    getName() +
		    " {\n color=pink2;\n style=filled;\n label=\"" +
		    self.getIdent() +
		    "\\n" + this.makeRedundancyString(self) + 
		    "\";\n");
	} else {
	    // otherwise, return boring white
	    return "subgraph cluster_" + getName() + " {\n label=\"" + self.getIdent() + "\";\n";
	}
    }


    //----------------------- These are more or less factory methods -----------
    

    /**
     * Prints dot graph of <str> to <filename>, using LinearAnalyzer lfa
     * and LinearRedundancyAnalyzer lra.
     */
    public static void printGraph(SIRStream str, String filename,
				  LinearAnalyzer lfa,
				  LinearRedundancyAnalyzer lra) {
	try {
	    FileOutputStream out = new FileOutputStream(filename);
	    StreamItDot dot = new LinearDot(new PrintStream(out), lfa, lra);
	    dot.print("digraph streamit {\n");
	    str.accept(dot);
	    dot.print("}\n");
	    out.flush();
	    out.close();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    /**
     * Prints dot graph of <str> to <filename>, using LinearAnalyzer lfa.
     */
    public static void printGraph(SIRStream str, String filename, LinearAnalyzer lfa){
	printGraph(str, filename, lfa, null);
    }
    
}

