package at.dms.kjc.spacetime;

import at.dms.kjc.backendSupport.Channel;
import at.dms.kjc.backendSupport.FilterInfo;
import at.dms.kjc.backendSupport.Layout;
import at.dms.kjc.common.CommonUtils;
import at.dms.kjc.sir.*;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.OutputSliceNode;
import at.dms.kjc.slicegraph.Partitioner;
import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.slicegraph.SliceNode;
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
public class SliceDotGraph {
    /**
     * Create a dot graph representation of the slice graph.
     * 
     * @param spaceTime The SpaceTime schedule
     * @param fileName The file to dump the dot graph
     * @param DRAM True if DRAM port assignment is complete
     */
    public static void dumpGraph(SpaceTimeSchedule spaceTime, Slice[] schedule, String fileName,
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
    public static void dumpGraph(SpaceTimeSchedule spaceTime, Slice[] schedule, String fileName,
                                 Layout layout, boolean DRAM, boolean label) {
        
        
        List<Slice> steadyTrav = Arrays.asList(schedule);
        Partitioner partitioner = spaceTime.getPartitioner();
        RawProcElements rawChip = spaceTime.getRawChip();
        
        //System.out.println("Creating Slice Dot Graph...");
        try {
            int order = 1;
            FileWriter fw = new FileWriter(fileName);
            fw.write("digraph SliceDotGraph {\n");
            fw.write("size = \"8, 10.5\";\n");
            LinkedList<Slice> tracesList = new LinkedList<Slice>(steadyTrav);
         
            // HashSet traceSet = new HashSet();
            // Util.addAll(traceSet, steadyTrav);
            // add the file readers and writes if they exist
            // for (int i = 0; i < io.length; i++)
            // traceSet.add(io[i]);

            Iterator<Slice> traces = tracesList.iterator();
            while (traces.hasNext()) {
                Slice slice = traces.next();
                //System.out.println(trace);
                SliceNode node = slice.getHead();
                
                fw.write("subgraph cluster" + slice.hashCode() + " {\n");
                fw.write("  color=blue;\n");
                if (label) {
                    fw.write("  label = \"Exe Order: " + order++ + ",BN Work: "
                         + partitioner.getSliceBNWork(slice) + "\";\n");
                }
                while (node != null) {
                    //System.out.println("   " + node);
                    if (node.isFilterSlice() && !node.getNext().isOutputSlice())
                        fw.write("  " + node.hashCode() + " -> "
                                 + node.getNext().hashCode() + ";\n");
                    if (node.isInputSlice()) {
                        bufferArc(IntraSliceBuffer.getBuffer(
                                                             (InputSliceNode) node, (FilterSliceNode) node
                                                             .getNext()), fw, DRAM, label);
                        
                        if (label) {
                            fw.write("  " + node.hashCode());
                            fw.write("[ label=\"");
                            if (((InputSliceNode) node).oneInput()
                                    || ((InputSliceNode) node).noInputs())
                                fw.write(node.toString());
                            else {
                                fw.write(((InputSliceNode) node).debugString(true));
                            }
                        }
                    }

                    if (node.isOutputSlice()) {
                        bufferArc(IntraSliceBuffer.getBuffer(
                                                             (FilterSliceNode) node.getPrevious(),
                                                             (OutputSliceNode) node), fw, DRAM, label);
                      
                        if (label) {
                            fw.write("  " + node.hashCode());
                            fw.write("[ label=\"");
                            if (((OutputSliceNode) node).oneOutput()
                                    || ((OutputSliceNode) node).noOutputs())
                                fw.write(node.toString());
                            else
                                fw
                                .write(((OutputSliceNode) node)
                                        .debugString(true));
                        }
                    }

                    if (label && node.isFilterSlice()) {
                        //System.out.println("  * info for " + node);
                        fw.write("  " + node.hashCode() + "[ label=\""
                                 + ((FilterSliceNode) node).toString(layout));
                        FilterInfo filter = FilterInfo
                            .getFilterInfo((FilterSliceNode) node);
                        fw.write("\\nWork: "
                                 + partitioner
                                 .getFilterWorkSteadyMult((FilterSliceNode) node));
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

            Iterator<Channel> buffers = OffChipBuffer.getBuffers().iterator();
            while (buffers.hasNext()) {
                OffChipBuffer buffer = (OffChipBuffer)buffers.next();
                if (buffer.isIntraSlice())
                    continue;
                bufferArc(buffer, fw, DRAM, label);
            }
            fw.write("}\n");
            fw.close();
        } catch (Exception e) {

        }
        CommonUtils.println_debugging("Finished Creating Slice Dot Graph");
    }

    private static String nodeNoLabel(SliceNode node) {
        String label = node.hashCode() + "[label=\""; 
        if (node.isFilterSlice()) {
            label += (node.toString().substring(0, node.toString().indexOf("_")));  
        }
        else if (node.isOutputSlice())
            label += "Output";
        else
            label += "Input";
        
        return label + "\"];\n";
        
        
    }
        
    private static String nodeShape(SliceNode node) {
        String shape = node.hashCode() + "[shape="; 
        if (node.isFilterSlice()) 
            shape += "circle";
            else if (node.isOutputSlice())
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
            if (buffer.isIntraSlice() && !((IntraSliceBuffer)buffer).isStaticNet())
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