package at.dms.kjc;

import at.dms.kjc.sir.*;
import java.util.*;
import java.io.*;

/**
 * This class does the front-end processing to turn a Kopi compilation
 * unit into StreamIt classes, and then prints the class graph as a
 * dot file.
 */
public class SimpleDot extends StreamItDot
{

    public SimpleDot(PrintStream outputStream) {
	super(outputStream);
    }

    /**
     * Prints dot graph of <str> to System.out
     */
    public static void printGraph(SIRStream str) {
	str.accept(new SimpleDot(System.out));
    }

    /**
     * Prints dot graph of <str> to <filename>
     */
    public static void printGraph(SIRStream str, String filename) {
	try {
	    FileOutputStream out = new FileOutputStream(filename);
	    SimpleDot dot = new SimpleDot(new PrintStream(out));
	    dot.print("digraph streamit {\n");
	    str.accept(dot);
	    dot.print("}\n");
	    out.flush();
	    out.close();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    public Object visitFilter(SIRFilter self,
                              JFieldDeclaration[] fields,
                              JMethodDeclaration[] methods,
                              JMethodDeclaration init,
                              JMethodDeclaration work,
                              CType inputType, CType outputType)
    {
	return new NamePair(makeLabelledNode(self.getIdent()));
    }

    /* visit a splitter */
    public Object visitSplitter(SIRSplitter self,
                                SIRSplitType type,
                                JExpression[] expWeights)
    {
	String label = type.toString();
	// strip out the "weighted" title
	int index = label.indexOf("ROUND_ROBIN");
	if (index>=0) {
	    label = label.toLowerCase();
	    // get rid of weighted and _
	    label = label.substring(index, index+5)+label.substring(index+6);
	}
	index = label.indexOf("DUPLICATE");
	if (index>=0) {
	    // get rid of ending
	    label = label.substring(index, index+9).toLowerCase();
	} else {
	    // try to add weights to label
	    try {
		int[] weights = self.getWeights();
		label += "(";
		for (int i=0; i<weights.length; i++) {
		    label += weights[i];
		    if (i!=weights.length-1) {
			label+=",";
		    }
		}
		label += ")";
	    } catch (Exception e) {}
	}
        // Create an empty node and return it.
        return new NamePair(makeLabelledInvisNode(label));
    }
    
    /* visit a joiner */
    public Object visitJoiner(SIRJoiner self,
                              SIRJoinType type,
                              JExpression[] expWeights)
    {
	String label = type.toString();

	// strip out the "weighted" title
	int index = label.indexOf("ROUND_ROBIN");
	if (index>=0) {
	    label = label.toLowerCase();
	    // get rid of _
	    label = label.substring(index, index+5)+label.substring(index+6);
	}

	// try to add weights to label
	try {
	    int[] weights = self.getWeights();
	    label += "(";
	    for (int i=0; i<weights.length; i++) {
		label += weights[i];
		if (i!=weights.length-1) {
		    label+=",";
		}
	    }
	    label += ")";
	} catch (Exception e) {}
        return new NamePair(makeLabelledInvisNode(label));
    }

    /**
     * Prints out the subgraph cluser line that is needed in to make clusters. This method is overridden to make colored
     * pipelines and splitjoins in LinearDot.
     **/
    public String getClusterString(SIRStream self) {
	String qualified = self.getIdent()+"";
	int i = qualified.lastIndexOf(".");
	if (i>0) {
	    qualified = qualified.substring(i+4).toLowerCase();
	}
	return "subgraph cluster_" + getName() + " {\n label=\"" + qualified + "\";\n";
    }

}
