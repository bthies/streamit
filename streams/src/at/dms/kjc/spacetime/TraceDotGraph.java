package at.dms.kjc.spacetime;

import at.dms.kjc.sir.*;
import at.dms.kjc.slicegraph.FilterTraceNode;
import at.dms.kjc.slicegraph.InputTraceNode;
import at.dms.kjc.slicegraph.OutputTraceNode;
import at.dms.kjc.slicegraph.Partitioner;
import at.dms.kjc.slicegraph.TraceNode;
import at.dms.kjc.*;
import at.dms.kjc.slicegraph.*;
import java.io.FileWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Arrays;

/**
 * This class will print out a dot graph representation of the slice (trace) graph either
 * before or after DRAM port assignment.
 * 
 * @author mgordon
 *
 */
public class TraceDotGraph {
    /**
     * Create a dot graph representation of the slice graph.
     * 
     * @param spaceTime The SpaceTime schedule
     * @param fileName The file to dump the dot graph
     * @param DRAM True if DRAM port assignment is complete
     */
    public static void dumpGraph(SpaceTimeSchedule spaceTime, Trace[] schedule, String fileName,
            Layout layout, boolean DRAM) {
        dumpGraph(spaceTime, schedule, fileName, layout, DRAM, true);
    }
    
    /**
     * Create a dot graph representation of the slice graph.
     * 
     * @param spaceTime The SpaceTime schedule
     * @param fileName The file to dump the dot graph
     * @param DRAM True if DRAM port assignment is complete
     * @param label if true generate labels for filters and slices with stats
     */
    public static void dumpGraph(SpaceTimeSchedule spaceTime, Trace[] schedule, String fileName,
                                 Layout layout, boolean DRAM, boolean label) {
        
        
        List<Trace> steadyTrav = Arrays.asList(schedule);
        Partitioner partitioner = spaceTime.partitioner;
        RawChip rawChip = spaceTime.getRawChip();
        
        //System.out.println("Creating Trace Dot Graph...");
        try {
            int order = 1;
            FileWriter fw = new FileWriter(fileName);
            fw.write("digraph TraceDotGraph {\n");
            fw.write("size = \"8, 10.5\";\n");
            LinkedList<Trace> tracesList = new LinkedList<Trace>(steadyTrav);
         
            // HashSet traceSet = new HashSet();
            // Util.addAll(traceSet, steadyTrav);
            // add the file readers and writes if they exist
            // for (int i = 0; i < io.length; i++)
            // traceSet.add(io[i]);

            Iterator<Trace> traces = tracesList.iterator();
            while (traces.hasNext()) {
                Trace trace = traces.next();
                //System.out.println(trace);
                TraceNode node = trace.getHead();
                
                fw.write("subgraph cluster" + trace.hashCode() + " {\n");
                fw.write("  color=blue;\n");
                if (label) {
                    fw.write("  label = \"Exe Order: " + order++ + ",BN Work: "
                         + partitioner.getTraceBNWork(trace) + "\";\n");
                }
                while (node != null) {
                    //System.out.println("   " + node);
                    if (node.isFilterTrace() && !node.getNext().isOutputTrace())
                        fw.write("  " + node.hashCode() + " -> "
                                 + node.getNext().hashCode() + ";\n");
                    if (node.isInputTrace()) {
                        bufferArc(IntraTraceBuffer.getBuffer(
                                                             (InputTraceNode) node, (FilterTraceNode) node
                                                             .getNext()), fw, DRAM, label);
                        
                        if (label) {
                            fw.write("  " + node.hashCode());
                            fw.write("[ label=\"");
                            if (((InputTraceNode) node).oneInput()
                                    || ((InputTraceNode) node).noInputs())
                                fw.write(node.toString());
                            else {
                                fw.write(((InputTraceNode) node).debugString(true));
                            }
                        }
                    }

                    if (node.isOutputTrace()) {
                        bufferArc(IntraTraceBuffer.getBuffer(
                                                             (FilterTraceNode) node.getPrevious(),
                                                             (OutputTraceNode) node), fw, DRAM, label);
                      
                        if (label) {
                            fw.write("  " + node.hashCode());
                            fw.write("[ label=\"");
                            if (((OutputTraceNode) node).oneOutput()
                                    || ((OutputTraceNode) node).noOutputs())
                                fw.write(node.toString());
                            else
                                fw
                                .write(((OutputTraceNode) node)
                                        .debugString(true));
                        }
                    }

                    if (label && node.isFilterTrace()) {
                        //System.out.println("  * info for " + node);
                        fw.write("  " + node.hashCode() + "[ label=\""
                                 + ((FilterTraceNode) node).toString(layout));
                        FilterInfo filter = FilterInfo
                            .getFilterInfo((FilterTraceNode) node);
                        fw.write("\\nWork: "
                                 + partitioner
                                 .getFilterWorkSteadyMult((FilterTraceNode) node));
                        fw.write("\\nMult:(" + filter.initMult + ", "
                                 + spaceTime.getPrimePumpTotalMult(filter) + ", " + filter.steadyMult
                                 + ")");
                        fw.write("\\nPre-peek, pop, push: (" + filter.prePeek
                                 + ", " + filter.prePop + ", " + filter.prePush
                                 + ")");
                        fw.write("\\npeek, pop, push: (" + filter.peek + ", "
                                 + filter.pop + ", " + filter.push + ")");
                    }
                    if (label) {
                        fw.write("\"");
                        fw.write("]");
                        fw.write(";\n");
                    }
                    else {
                        fw.write(nodeNoLabel(node));
                        fw.write(nodeShape(node));
                    }
                    node = node.getNext();
                }
                fw.write("}\n");
            }

            Iterator<OffChipBuffer> buffers = OffChipBuffer.getBuffers().iterator();
            while (buffers.hasNext()) {
                OffChipBuffer buffer = buffers.next();
                if (buffer.isIntraTrace())
                    continue;
                bufferArc(buffer, fw, DRAM, label);
            }
            fw.write("}\n");
            fw.close();
        } catch (Exception e) {

        }
        SpaceTimeBackend.println("Finished Creating Trace Dot Graph");
    }

    private static String nodeNoLabel(TraceNode node) {
        String label = node.hashCode() + "[label=\""; 
        if (node.isFilterTrace()) {
            label += (node.toString().substring(0, node.toString().indexOf("_")));  
        }
        else if (node.isOutputTrace())
            label += "Output";
        else
            label += "Input";
        
        return label + "\"];\n";
        
        
    }
        
    private static String nodeShape(TraceNode node) {
        String shape = node.hashCode() + "[shape="; 
        if (node.isFilterTrace()) 
            shape += "circle";
            else if (node.isOutputTrace())
                shape += "invtriangle";
            else
                shape += "triangle";
            
        return shape + "];\n";
    }
    private static void bufferArc(OffChipBuffer buffer, FileWriter fw,
                                  boolean DRAM, boolean label) throws Exception {
        fw.write(buffer.getSource().hashCode() + " -> "
                 + buffer.getDest().hashCode()); 
        if (label)
        {
            fw.write("[label=\""
                    + (DRAM ? buffer.getDRAM().toString() : "not assigned\""));
            if (buffer.isIntraTrace() && !((IntraTraceBuffer)buffer).isStaticNet())
                fw.write(",gdn,");
            
            fw.write(buffer.getRotationLength() + ", ");
            
            if (DRAM) {
                if (buffer.redundant())
                    fw.write("\", style=dashed");
                else
                    fw.write(buffer.getSize()
                            + "(" + buffer.getIdent() + ")\", style=bold");
            }
            fw.write("]");
        }
        fw.write(";\n");

    }

}