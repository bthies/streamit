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
    private static boolean steadyIO = false; // if true, marks steady-state I/O rates (multiplicity * push or pop)
    private boolean simple; // if true, then use simple labels for filters (no numbers)
    private boolean markStateful; // if true, then indicate which filters are stateful
    private boolean markIO; // if true, marks pop and push rates in filters
    private boolean markVectorizable; // if true, marks vectorizable filters.
    private HashMap[] execCounts; // schedule for stream (only used if markIO is true)
    private HashMap<SIROperator, Object> partitions;
    // color managemet:
    private HashMap<Long, String> colorMap = new HashMap(); // maps from a partition value to a color for that partition
    // for some reason, I can only get DOT colors to work in HSV.  Take the middle 9 points of an 11-point brewer scale:
    // http://www.personal.psu.edu/cab38/ColorBrewer/ColorBrewer.html
    private static final String[] color_table = {"\"0.59166,0.61,0.7\"",
                                                 "\"0.55833,0.44,0.81\"",
                                                 "\"0.53888,0.26,0.91\"",
                                                 "\"0.53333,0.09,0.97\"",
                                                 "\"0.51694,0.24,1\"",
                                                 "\"0.11666,0.43,0.99\"",
                                                 "\"0.07777,0.61,0.99\"",
                                                 "\"0.03888,0.72,0.95\"",
                                                 "\"0.00555,0.81,0.84\""};

        /* ad-hoc gradient:
        "white",
        "lightsalmon",
        "coral",
        "tomato",
        "darkorange",
        "orange",
        "orangered",
        "crimson",
        "red"};
        */

        /* pastel colors:
           "lightgrey",
           "lightskyblue",
           "navajowhite",
           "darkseagreen",
           "plum",
           "lightsteelblue",
           "sandybrown",
           "lightsalmon",
           "pink",
           "aquamarine",
           "peachpuff",
           "lightskyblue"};
        */

    /* darker colors:
    "white",
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
    */

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
        mapColors(partitions);
    }

    // builds a sorted array of the unique values of the partitions,
    // assuming they are integral.  Useful for adding gradient
    // shading, like hotter colors for more work.
    private void mapColors(HashMap<SIROperator, Object> partitions) {
        Set keySet = partitions.keySet();
        Set valueSet = new HashSet();
        for (Iterator it = keySet.iterator(); it.hasNext(); ) {
            Object next = it.next();
            // we go to a string and then back to a number as a
            // general way of representing what the partition info
            // might be (e.g., a WorkInfo)
            valueSet.add(Long.valueOf(""+partitions.get(next)));
        }
        // sort the value set
        Long[] values = (Long[])valueSet.toArray(new Long[0]);
        Arrays.sort(values);
        // map each value to the scaled part of the color array
        for (int i=0; i<values.length; i++) {
            colorMap.put(values[i],
                         // map colors based on annotation RANK:
                         //color_table[((int)Math.floor((double)i/(double)values.length*(double)color_table.length))]);
                         // map colors based on annotation VALUE:
                         color_table[((int)Math.floor((double)values[i].longValue()/((double)values[values.length-1].longValue()+1.0)*(double)color_table.length))]);

        }
    }

    /**
     * Given original node label 'origLabel', makes a label suitable
     * for the partitioning labeling.
     */
    private String makePartitionLabel(SIROperator op, String origLabel) {
        // calculate the color this node will have
        String color;
        try {
            color = colorMap.get(Long.valueOf(""+partitions.get(op)).longValue());
        } catch (NumberFormatException e) {
            color = "white";
        }
        //int offset = (partition + 1) % color_table.length;
        //if (offset < 0) { offset = color_table.length + offset; }

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
            if (steadyIO) {
                ioLabel = "\\nI/O: " + 
                    filter.getPopForSchedule(execCounts) + "->" + 
                    filter.getPushForSchedule(execCounts);
            } else {
                String popString = filter.getPop().isDynamic() ? filter.getPop().toString() : filter.getPopInt()+"";
                String pushString = filter.getPush().isDynamic() ? filter.getPush().toString() : filter.getPushInt()+"";
                ioLabel = "\\nI/O: " + popString  + "->" + pushString;
            }
            // indicate peeking prominently
            if (!filter.getPop().isDynamic() && !filter.getPeek().isDynamic() && filter.getPeekInt() > filter.getPopInt()) {
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

        // label for work, patition, etc.
        String attribute_label;
        if (op instanceof SIRFilter || partitions.get(op)!=null) {
            attribute_label = prefixLabel + partitions.get(op);
        } else {
            // don't label splitters/joiners with null attribute (e.g, for all work estimates)
            attribute_label = "";
        }
        
        return origLabel + attribute_label + ioLabel + stateLabel + vecLabel + "\" color=" + color + " style=\"filled";
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
                                                                self.getCleanIdent() :
                                                                self.getName())));
    }

    /* visit a splitter */
    public Object visitSplitter(SIRSplitter self,
                                SIRSplitType type,
                                JExpression[] expWeights)
    {
        String label = self.getCleanIdent();
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
        String label = self.getCleanIdent();
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
                "\n label=\"" + self.getCleanIdent() + "\\n tile=" + tile + "\";\n";
        } else if (self.getCleanIdent().toLowerCase().startsWith("anonymous")) {
            // do not outline or label subgraphs for anonymous
            // streams.  they just clutter things.
            return "subgraph cluster_" + getName() + " {\n" +
                "color = white;\n" + 
                "label = \"\";\n";
        } else {
            // return no label for containers spread over multiple
            // tiles
            return "subgraph cluster_" + getName() + " {" + 
                "\n color = black\n" +
                "\n label=\"" + self.getCleanIdent() + "\";\n";
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
            // now that execCounts are determined, make sure that
            // dynamic rates show up in the labels
            SIRDynamicRateManager.pushIdentityPolicy();
            {
                dot.print("digraph streamit {\n");
                dot.print("size=\"6.5,9\"\n");
                str.accept(dot);
                dot.print("}\n");
                out.flush();
                out.close();
            }
            SIRDynamicRateManager.popPolicy();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
