package at.dms.kjc.sir.lowering.partition;

import java.util.*;
import java.io.*;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.fission.StatelessDuplicate;
import at.dms.kjc.sir.lowering.SIRScheduler;

/**
 * This class extends the main streamit dot printer to annotate the
 * dot graphs with partitioning information. 
 **/
public class PartitionDot extends StreamItDot {
    private String prefixLabel;
    private boolean simple; // if true, then use simple labels for filters (no numbers)
    private boolean markStateful; // if true, then indicate which filters are stateful
    private boolean markIO; // if true, marks pop and push rates in filters
    private boolean markVectorizable; // if true, marks vectorizable filters.
    private HashMap[] execCounts; // schedule for stream (only used if markIO is true)
    private HashMap<SIROperator, Object> partitions;
    // randomized order of color table so that like colors do not end up next to each other.
    private static final String[] color_table = {"white",
                                                 "crimson",
                                                 "darkorchid",
                                                 "brown",
                                                 "gold",
                                                 "deeppink",
                                                 "darkorange",
                                                 "darkseagreen",
                                                 "blue",
                                                 "darkgreen",
                                                 "cadetblue",
                                                 "antiquewhite",
                                                 "darkkhaki",
                                                 "aliceblue",
                                                 "dodgerblue",
                                                 "darkgoldenrod",
                                                 "chartreuse",
                                                 "aquamarine",
                                                 "firebrick",
                                                 "deepskyblue",
                                                 "blueviolet",
                                                 "darkturquoise",
                                                 "darksalmon",
                                                 "blue",
                                                 "bisque",
                                                 "dimgray",
                                                 "forestgreen",
                                                 "darkslateblue",
                                                 "darkviolet",
                                                 "goldenrod",
                                                 "gainsboro",
                                                 "darkolivegreen",
                                                 "burlywood",
                                                 "chocolate",
                                                 "ghostwhite",
                                                 "cyan",
                                                 "coral",
                                                 "darkslategray"};

    /**
     * PrefixLabel is a prefix for each node.
     * If 'simple' is true, then filter names are simple idents (not unique).
     */
    public PartitionDot(SIRStream str, PrintStream outputstream,
                        HashMap<SIROperator, Object> partitions,
                        String prefixLabel,
                        boolean simple,
                        boolean markStateful,
                        boolean markIO) {
        super(outputstream);
        this.partitions = partitions;
        this.prefixLabel = prefixLabel;
        this.simple = simple;
        this.markStateful = markStateful;
        this.markIO = markIO;
        this.markVectorizable = KjcOptions.vectorize > 0;
        // initialize execution counts if we are marking I/O
        if (markIO) {
            execCounts = SIRScheduler.getExecutionCounts(str);
        }
    }

    /**
     * Given original node label 'origLabel', makes a label suitable
     * for the partitioning labeling.
     */
    private String makePartitionLabel(SIROperator op, String origLabel) {
        int partition;
        try {
            partition = Integer.valueOf(""+partitions.get(op)).intValue();
        } catch (NumberFormatException e) {
            partition = -1;
        }
        int offset = (partition + 1) % color_table.length;
        if (offset < 0) { offset = color_table.length + offset; }

        // label for any mutable state
        String stateLabel = "";
        if (markStateful && (op instanceof SIRFilter) && 
            StatelessDuplicate.hasMutableState((SIRFilter)op)) {
            stateLabel = "\\n*** STATEFUL ***";
        }
        
        // label for communication rates
        String ioLabel = "";
        if (markIO && (op instanceof SIRFilter)) {
            SIRFilter filter = (SIRFilter)op;
            ioLabel = "\\nI/O: " + 
                filter.getPopForSchedule(execCounts) + "->" + 
                filter.getPushForSchedule(execCounts);
            // indicate peeking prominently
            if (filter.getPeekInt() > filter.getPopInt()) {
                ioLabel += "\\n*** PEEKS " + (filter.getPeekInt() - filter.getPopInt()) + " AHEAD ***";
            }
        }

        String vecLabel = "";
        if (markVectorizable && (op instanceof SIRFilter)) {
            boolean vectorizable = at.dms.kjc.sir.lowering.Vectorizable.vectorizable((SIRFilter)op);
            boolean useful = vectorizable && at.dms.kjc.sir.lowering.Vectorizable.isUseful((SIRFilter)op);
            if (vectorizable) {
                vecLabel += "\\nVectorizable";
                if (!useful) {
                    vecLabel += " but not useful";
                }
            }
        }
        
        return origLabel + prefixLabel + partitions.get(op) + ioLabel + stateLabel + vecLabel + 
            "\" color=\"" + color_table[offset] + "\" style=\"filled";
    }

    /* visit a filter */
    public Object visitFilter(SIRFilter self,
                              JFieldDeclaration[] fields,
                              JMethodDeclaration[] methods,
                              JMethodDeclaration init,
                              JMethodDeclaration work,
                              CType inputType, CType outputType)
    {
        assert partitions.containsKey(self):
            "Not assigned to tile: " + self.getName();
        return new NamePair(makeLabelledNode(makePartitionLabel(self, 
                                                                simple ?
                                                                self.getIdent() :
                                                                self.getName())));
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
     * Prints dot graph of 'str' to 'filename'.
     */
    public static void printPartitionGraph(SIRStream str, 
                                           String filename,
                                           HashMap<SIROperator, Object> partitions) {
        printGraph(str, filename, partitions, "\\npartition=", false, true, false);
    }

    static void printWorkGraph(SIRStream str,
                               String filename,
                               HashMap<SIROperator, Object> partitions) {
        printGraph(str, filename, partitions, "\\nwork=", true, true, true);
    
    }

    /**
     * For printing execution counts.  here 'execCounts' is in the
     * format returned by SIRScheduler.getExecutionCounts
     */
    public static void printScheduleGraph(SIRStream str,
                                          String filename,
                                          HashMap[] execCounts) {
        // make a string representation for init/steady schedules
        HashMap<SIROperator, Object> stringMap = new HashMap<SIROperator, Object>();
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
        printGraph(str, filename, stringMap, "", false, false, false);
    }

    private static void printGraph(SIRStream str, 
                                   String filename,
                                   HashMap<SIROperator, Object> partitions,
                                   String prefixLabel,
                                   boolean simple,
                                   boolean markStateful,
                                   boolean markIO) {
        try {
            FileOutputStream out = new FileOutputStream(filename);
            StreamItDot dot = new PartitionDot(str, 
                                               new PrintStream(out), partitions, prefixLabel,
                                               simple, markStateful, markIO);
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
