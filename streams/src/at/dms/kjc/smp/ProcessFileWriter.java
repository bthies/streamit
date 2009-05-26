package at.dms.kjc.smp;

import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import at.dms.kjc.JBlock;
import at.dms.kjc.JMethodDeclaration;
import at.dms.kjc.slicegraph.*;

public class ProcessFileWriter {
    private static int totalOutputs = 0;
    protected FilterSliceNode filterNode;
    protected SchedulingPhase phase;
    protected SMPBackEndFactory factory;
    protected CoreCodeStore codeStore;
    protected FileOutputContent fileOutput;
    protected static HashMap<FilterSliceNode, Core> allocatingCores;
    protected Core allocatingCore; 

    static {
        allocatingCores = new HashMap<FilterSliceNode, Core>();
    }
    
    public ProcessFileWriter (FilterSliceNode filter, SchedulingPhase phase, SMPBackEndFactory factory) {
        this.filterNode = filter;
        this.fileOutput = (FileOutputContent)filter.getFilter();
        this.phase = phase;
        this.factory = factory;
    }
    
    public static int getTotalOutputs() {
        return totalOutputs;
    }
    
    public static Set<FilterSliceNode> getFileWriterFilters() {
        return allocatingCores.keySet();
    }
    
    /**
     * Return the core that this file writer's buffer should be allocated on.
     * @param fo  The file writer
     */
    public static Core getAllocatingCore(FilterSliceNode fo) {
        assert fo.isFileOutput();
        
        if (!allocatingCores.containsKey(fo)) {
            Core allocatingCore = nextAllocatingCore(fo);
            System.out.println(fo + " assigned to Core " + allocatingCore.getCoreID());
            allocatingCores.put(fo, allocatingCore);
        }
        
        return allocatingCores.get(fo);
    }
    
    /** 
     * Decide on the allocating core for the file writer and create the shared, uncacheable heap
     * on that core the output will be written to.
     */
    public void processFileWriter() {
        //do nothing if faking io
        if (phase == SchedulingPhase.INIT) {
            int outputs = filterNode.getFilter().getSteadyMult();
            System.out.println("Outputs for " + filterNode + ": " + outputs);
            totalOutputs += outputs;
            assert allocatingCores.containsKey(filterNode);
            allocatingCore = allocatingCores.get(filterNode);
            codeStore = allocatingCore.getComputeCode();
                        
            codeStore.appendTxtToGlobal("int OUTPUT;\n");
            //JBlock block = new JBlock();
            //codeStore.addStatementFirstToBufferInit(block);
        }
    }
    
    /**
     * @return The core we should allocate this file reader on.  Remember that 
     * the file reader is allocated to off-chip memory.  We just cycle through the cores
     * if there is more than one file reader, one reader per core.
     */
    private static Core nextAllocatingCore(FilterSliceNode fo) {
        List<Core> reverseOrder = SMPBackend.chip.getCores(); 
        Collections.reverse(reverseOrder);
        
        if(allocatingCores.get(fo) != null)
            return allocatingCores.get(fo);

        // Try cores that are not yet allocating and already have existing code
        for (Core core : reverseOrder) {
            if (!allocatingCores.containsValue(core) && core.getComputeCode().shouldGenerateCode()) {
                allocatingCores.put(fo, core);
                return core;
            }
        }

        // Try cores that are not yet allocating, but do not already have code
        for (Core core : reverseOrder) {
            if (!allocatingCores.containsValue(core)) {
                allocatingCores.put(fo, core);
                return core;
            }
        }

        assert false : "Too many file readers for this chip (one per core)!";
        return null;
    }
}
