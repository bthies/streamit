package at.dms.kjc.sir.lowering.partition;

import java.util.*;
import java.io.*;

import at.dms.kjc.*;
import at.dms.util.*;
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
    private static final String[] color_table = {"white", "blue", "aliceblue", "antiquewhite", "aquamarine", "bisque", "blue", "blueviolet", "brown", "burlywood", "cadetblue", "chartreuse", "chocolate", "coral", "cornflowerblue", "crimson", "cyan", "darkgoldenrod", "darkgreen", "darkkhaki", "darkolivegreen", "darkorange", "darkorchid", "darksalmon", "darkseagreen", "darkslateblue", "darkslategray", "darkturquoise", "darkviolet", "deeppink", "deepskyblue", "dimgray", "dodgerblue", "firebrick", "forestgreen", "gainsboro", "ghostwhite", "gold", "goldenrod"};

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

    /**
     * Given original node label <origLabel>, makes a label suitable
     * for the partitioning labeling.
     */
    private String makePartitionLabel(SIROperator op, String origLabel) {
	int partition;
	try {
	    partition = Integer.valueOf(""+partitions.get(op)).intValue();
	} catch (NumberFormatException e) {
	    partition = -1;
	}
	return origLabel + prefixLabel + partitions.get(op) + 
	    "\" color=\"" + color_table[(partition+1)%color_table.length] + "\" style=\"filled";
    }

    /* visit a filter */
    public Object visitFilter(SIRFilter self,
                              JFieldDeclaration[] fields,
                              JMethodDeclaration[] methods,
                              JMethodDeclaration init,
                              JMethodDeclaration work,
                              CType inputType, CType outputType)
    {
	Utils.assert(partitions.containsKey(self), "Not assigned to tile: " + self.getName());
	return new NamePair(makeLabelledNode(makePartitionLabel(self, self.getName())));
    }

    /* visit a splitter */
    public Object visitSplitter(SIRSplitter self,
                                SIRSplitType type,
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
        // Create an empty node and return it.
        return new NamePair(makeLabelledNode(makePartitionLabel(self, label)));
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
        return new NamePair(makeLabelledNode(makePartitionLabel(self, label)));
    }
    
    /**
     * Override to show partitions.
     */
    public String getClusterString(SIRStream self) {
	// if we have a linear rep of this object, label it
	if (partitions.containsKey(self)) {
	    String tile = "" + partitions.get(self);
	    return "subgraph cluster_" + getName() + " {" + 
		"\n label=\"" + self.getName() + "\\n tile=" + tile + "\";\n";
	} else {
	    // return no label for containers spread over multiple
	    // tiles
	    return "subgraph cluster_" + getName() + " {" + 
		"\n label=\"" + self.getName() + "\";\n";
	}
    }

    /**
     * Prints dot graph of <str> to <filename>.
     */
    public static void printPartitionGraph(SIRStream str, 
					   String filename,
					   HashMap partitions) {
	printGraph(str, filename, partitions, "\\npartition=");
    }

    static void printWorkGraph(SIRStream str,
			       String filename,
			       HashMap partitions) {
	printGraph(str, filename, partitions, "\\nwork=");
	
    }

    /**
     * For printing execution counts.  here <execCounts> is in the
     * format returned by SIRScheduler.getExecutionCounts
     */
    public static void printScheduleGraph(SIRStream str,
					  String filename,
					  HashMap[] execCounts) {
	// make a string representation for init/steady schedules
	HashMap stringMap = new HashMap();
	HashSet allKeys = new HashSet();
	allKeys.addAll(execCounts[0].keySet());
	allKeys.addAll(execCounts[1].keySet());
	for (Iterator it = allKeys.iterator(); it.hasNext(); ) {
	    SIROperator op = (SIROperator)it.next();
	    String string;
	    // retain rate labels for filters
	    if (op instanceof SIRFilter) {
		string = makeFilterLabel((SIRFilter)op);
	    } else {
		string = "";
	    }
	    // add schedule labels
	    int[] initCount = (int[])execCounts[0].get(op);
	    int[] steadyCount = (int[])execCounts[1].get(op);
	    if (initCount!=null) {
		string += "\\ninit reps=" + initCount[0];
	    }
	    if (steadyCount!=null) {
		string += "\\nsteady reps=" + steadyCount[0];
	    }
	    stringMap.put(op, string);
	}
	printGraph(str, filename, stringMap, "");
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
