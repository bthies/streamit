package at.dms.kjc.sir.lowering.partition;

import java.util.*;
import java.io.*;
import lpsolve.*;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.linprog.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.raw.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;

/**
 * This class extends the main streamit dot printer to annotate the
 * dot graphs with partitioning information. 
 **/
public class PartitionDot extends StreamItDot {
    private String prefixLabel;
    private HashMap partitions;
    private static final String[] color_table = {"floralwhite", "blue", "aliceblue", "antiquewhite", "aquamarine", "bisque", "blue", "blueviolet", "brown", "burlywood", "cadetblue", "chartreuse", "chocolate", "coral", "cornflowerblue", "crimson", "cyan", "darkgoldenrod", "darkgreen", "", "darkkhaki", "darkolivegreen", "darkorange", "", "darkorchid", "darksalmon", "darkseagreen", "darkslateblue", "darkslategray", "", "", "", "", "darkturquoise", "darkviolet", "deeppink", "deepskyblue", "", "dimgray", "", "dodgerblue", "firebrick", "forestgreen", "gainsboro", "ghostwhite", "gold", "goldenrod"};

    /**
     * PrefixLabel is a prefix for each node.
     */
    public PartitionDot(PrintStream outputstream,
			HashMap partitions,
			String prefixLabel) {
	super(outputstream);
	this.partitions = partitions;
	this.prefixLabel = prefixLabel;
    }

    /* visit a filter */
    public Object visitFilter(SIRFilter self,
                              JFieldDeclaration[] fields,
                              JMethodDeclaration[] methods,
                              JMethodDeclaration init,
                              JMethodDeclaration work,
                              CType inputType, CType outputType)
    {
        // Return a name pair with both ends pointing to this.
	//        return new NamePair(makeLabelledNode(self.getRelativeName()));
	String label = self.getName();
	Utils.assert(partitions.containsKey(self), "Not assigned to tile: " + self.getName());
	int partition;
	try {
	    partition = Integer.valueOf(""+partitions.get(self)).intValue();
	} catch (NumberFormatException e) {
	    partition = -1;
	}
	label += "\\n" + prefixLabel + partitions.get(self) + 
	    "\" color=\"" + color_table[(partition+1)%color_table.length] + "\" style=\"filled";
	return new NamePair(makeLabelledNode(label));
    }

    /* visit a joiner */
    public Object visitJoiner(SIRJoiner self,
                              SIRJoinType type,
                              JExpression[] expWeights)
    {
	String label = type.toString();
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
	label += "\\ntile=" + partitions.get(self);
        return new NamePair(makeLabelledNode(label));
    }
    
    /**
     * Override to show partitions.
     */
    public String getClusterString(SIRStream self) {
	// if we have a linear rep of this object, label it
	if (partitions.containsKey(self)) {
	    String tile = "" + partitions.get(self);
	    return "subgraph cluster_" + getName() + " {" + 
		"\n label=\"" + self.getIdent() + "\\n tile=" + tile + "\";\n";
	} else {
	    // return no label for containers spread over multiple
	    // tiles
	    return "subgraph cluster_" + getName() + " {" + 
		"\n label=\"" + self.getIdent() + "\";\n";
	}
    }

    /**
     * Prints dot graph of <str> to <filename>.
     */
    public static void printPartitionGraph(SIRStream str, 
					   String filename,
					   HashMap partitions) {
	printGraph(str, filename, partitions, "partition=");
    }

    static void printWorkGraph(SIRStream str,
			       String filename,
			       HashMap partitions) {
	printGraph(str, filename, partitions, "work=");
	
    }

    private static void printGraph(SIRStream str, 
				   String filename,
				   HashMap partitions,
				   String prefixLabel) {
	try {
	    FileOutputStream out = new FileOutputStream(filename);
	    StreamItDot dot = new PartitionDot(new PrintStream(out), partitions, prefixLabel);
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
