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
    protected static HashMap<FilterSliceNode, Core> allocatingTiles;
    protected Core allocatingTile; 

    static {
        allocatingTiles = new HashMap<FilterSliceNode, Core>();
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
        return allocatingTiles.keySet();
    }
    
    /**
     * Return the tile that this file writer's buffer should be allocated on.
     * @param fo  The file writer
     */
    public static Core getAllocatingTile(FilterSliceNode fo) {
        assert fo.isFileOutput();
        
        if (!allocatingTiles.containsKey(fo)) {
            Core allocatingTile = nextAllocatingTile(fo);
            System.out.println(fo + " assigned to Tile " + allocatingTile.getCoreID());
            allocatingTiles.put(fo, allocatingTile);
        }
        
        return allocatingTiles.get(fo);
    }
    
    /** 
     * Decide on the allocating tile for the file writer and create the shared, uncacheable heap
     * on that tile the output will be written to.
     */
    public void processFileWriter() {
        //do nothing if faking io
        if (phase == SchedulingPhase.INIT) {
            int outputs = filterNode.getFilter().getSteadyMult();
            System.out.println("Outputs for " + filterNode + ": " + outputs);
            totalOutputs += outputs;
            assert allocatingTiles.containsKey(filterNode);
            allocatingTile = allocatingTiles.get(filterNode);
            codeStore = allocatingTile.getComputeCode();
                        
            codeStore.appendTxtToGlobal("int OUTPUT;\n");
            //JBlock block = new JBlock();
            //codeStore.addStatementFirstToBufferInit(block);
        }
    }
    
    /**
     * @return The tile we should allocate this file reader on.  Remember that 
     * the file reader is allocated to off-chip memory.  We just cycle through the tiles
     * if there is more than one file reader, one reader per tile.
     */
    private static Core nextAllocatingTile(FilterSliceNode fo) {
        List<Core> reverseOrder = SMPBackend.chip.getCores(); 
        Collections.reverse(reverseOrder);
        
        if(allocatingTiles.get(fo) != null)
            return allocatingTiles.get(fo);

        // Try cores that are not yet allocating and already have existing code
        for (Core tile : reverseOrder) {
            if (!allocatingTiles.containsValue(tile) && tile.getComputeCode().shouldGenerateCode()) {
                allocatingTiles.put(fo, tile);
                return tile;
            }
        }

        // Try cores that are not yet allocating, but do not already have code
        for (Core tile : reverseOrder) {
            if (!allocatingTiles.containsValue(tile)) {
                allocatingTiles.put(fo, tile);
                return tile;
            }
        }

        assert false : "Too many file readers for this chip (one per tile)!";
        return null;
    }
}
