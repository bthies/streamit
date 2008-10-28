package at.dms.kjc.tilera;

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
    protected TileraBackEndFactory factory;
    protected TileCodeStore codeStore;
    protected FileOutputContent fileOutput;
    protected static HashMap<FilterSliceNode, Tile> allocatingTiles;
    protected Tile allocatingTile; 

    static {
        allocatingTiles = new HashMap<FilterSliceNode, Tile>();
    }
    
    public ProcessFileWriter (FilterSliceNode filter, SchedulingPhase phase, TileraBackEndFactory factory) {
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
    public static Tile getAllocatingTile(FilterSliceNode fo) {
        assert fo.isFileOutput();
        
        if (!allocatingTiles.containsKey(fo)) {
            Tile allocatingTile = nextAllocatingTile(fo);
            System.out.println(fo + " assigned to Tile " + allocatingTile.getTileNumber());
            allocatingTiles.put(fo, allocatingTile);
        }
        
        return allocatingTiles.get(fo);
    }
    
    /** 
     * Decide on the allocating tile for the file writer and create the shared, uncacheable heap
     * on that tile the output will be written to.
     */
    public void processFileWriter() {
        if (phase == SchedulingPhase.INIT) {
            int outputs = filterNode.getFilter().getSteadyMult();
            System.out.println("Outputs for " + filterNode + ": " + outputs);
            totalOutputs += outputs;
            assert allocatingTiles.containsKey(filterNode);
            allocatingTile = allocatingTiles.get(filterNode);
            codeStore = allocatingTile.getComputeCode();
                        
            JBlock block = new JBlock();
            //create the heap with shared and uncacheable attributes
            codeStore.appendTxtToGlobal("ilibHeap fileWriteHeap;\n");
            block.addStatement(Util.toStmt("int flags = ILIB_MEM_SHARED | ILIB_MEM_UNCACHEABLE"));
            block.addStatement(Util.toStmt("ilib_mem_create_heap(flags, &fileWriteHeap)"));
            codeStore.addStatementFirstToBufferInit(block);
        }
    }
    
    /**
     * @return The tile we should allocate this file reader on.  Remember that 
     * the file reader is allocated to off-chip memory.  We just cycle through the tiles
     * if there is more than one file reader, one reader per tile.
     */
    private static Tile nextAllocatingTile(FilterSliceNode fo) {
        List<Tile> reverseOrder = TileraBackend.chip.getAbstractTiles(); 
        Collections.reverse(reverseOrder);
        for (Tile tile : reverseOrder) {
            if (!allocatingTiles.containsValue(tile)) {
                allocatingTiles.put(fo, tile);
                return tile;
            }
        }
        assert false : "Too many file readers for this chip (one per tile)!";
        return null;
    }
}
