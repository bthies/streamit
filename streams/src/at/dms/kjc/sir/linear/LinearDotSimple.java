package at.dms.kjc.sir.linear;

import java.io.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;


/**
 * This is like LinearDot except that the node labels are only
 * the names and not the IO rates. Makes for easier reading.
 **/
public class LinearDotSimple extends LinearDot {
    /**
     * Make a LinearDotSimple printer that prints out the dot graph using information
     * from the linear filter analyzer to annotate the graph (eg color it silly).
     * Also include redundancy information.
     **/
    public LinearDotSimple(PrintStream outputstream,
		     LinearAnalyzer anal, LinearRedundancyAnalyzer lra) {
	super(outputstream, anal, lra);
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

    /**
     * Prints dot graph of <str> to <filename>, using LinearAnalyzer lfa
     * and LinearRedundancyAnalyzer lra.
     */
    public static void printGraph(SIRStream str, String filename,
				  LinearAnalyzer lfa,
				  LinearRedundancyAnalyzer lra) {	
	try {
	    FileOutputStream out = new FileOutputStream(filename);
	    StreamItDot dot = new LinearDotSimple(new PrintStream(out), lfa, lra);
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

