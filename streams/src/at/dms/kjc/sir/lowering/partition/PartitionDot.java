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
class PartitionDot extends StreamItDot {
    private HashMap partitions;

    public PartitionDot(PrintStream outputstream,
			HashMap partitions) {
	super(outputstream);
	this.partitions = partitions;
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
	label += "\\ntile=" + ((Integer)partitions.get(self)).intValue();
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
	label += "\\ntile=" + ((Integer)partitions.get(self)).intValue();
        return new NamePair(makeLabelledNode(label));
    }
    
    /**
     * Override to show partitions.
     */
    public String getClusterString(SIRStream self) {
	// if we have a linear rep of this object, color the resulting dot graph rose.
	Utils.assert(partitions.containsKey(self), "No partition for " + self);
	int tile = ((Integer)partitions.get(self)).intValue();
	if (tile!=-1) {
	    return "subgraph cluster_" + getName() + " {" + 
		"\n label=\"" + self.getIdent() + "\\n tile=" + tile + "\";\n";
	} else {
	    // otherwise, return boring white
	    return "subgraph cluster_" + getName() + " {" + 
		"\n label=\"" + self.getIdent() + "\";\n";
	}
    }

    /**
     * Prints dot graph of <str> to <filename>.
     */
    public static void printGraph(SIRStream str, String filename,
				  HashMap partitions) {
	try {
	    FileOutputStream out = new FileOutputStream(filename);
	    StreamItDot dot = new PartitionDot(new PrintStream(out), partitions);
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
