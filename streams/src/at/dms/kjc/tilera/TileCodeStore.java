package at.dms.kjc.tilera;

import at.dms.kjc.backendSupport.ComputeCodeStore;
import at.dms.kjc.common.ALocalVariable;
import at.dms.kjc.*;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import at.dms.kjc.slicegraph.*;

public class TileCodeStore extends ComputeCodeStore<Tile> {
    /** True if this tile code store has code appended to it */
    private boolean hasCode = false;
    /** The method that will malloc the buffers and receive addresses from downstream
     * tiles*/
    protected JMethodDeclaration bufferInit;
    /** The name of the bufferInit method */
    public static final String bufferInitMethName = "buffer_and_address_init";
    /** Any text that should appear outside a function declaration in the c code */
    private StringBuffer globalTxt = new StringBuffer();
    /** set of filterslicenodes that are mapped to this tile */
    protected HashSet<FilterSliceNode> filters;
    
    public TileCodeStore(Tile nodeType) {
        super(nodeType);
        setMyMainName("__main__");
        filters = new HashSet<FilterSliceNode>();
        createBufferInitMethod();
    }
    
    /**
     * Constructor: caller will add code to bound number of iterations, code store has pointer back to a compute node.
     * @param parent a ComputeNode.
     * @param iterationBound a variable that will be defined locally in 
     */
    public TileCodeStore(Tile parent, ALocalVariable iterationBound) {
       super(parent, iterationBound);
       setMyMainName("__main__");
       createBufferInitMethod();
    }
    
    /**
     * Remember that this filter is mapped to this tile.
     * 
     * @param filter The filter we are mapping to this tile.
     */
    public void addFilter(FilterSliceNode filter) {
        filters.add(filter);
        this.setHasCode();
    }
    
    /**
     * return all of the filters that are mapped to this tile.
     * 
     * @return all of the filters that are mapped to this tile.
     */
    public Set<FilterSliceNode> getFilters() {
        return filters;
    }
    
    /**
     * Append a barrier instruction to all of the (abstract) tiles in the buffer init
     * method.
     */
    public static void addBufferInitBarrier() {
        
        for (int t = 0; t < TileraBackend.chip.abstractSize(); t++) {
           Tile tile = TileraBackend.chip.getTranslatedTile(t);
            TileCodeStore cs = TileraBackend.chip.getTranslatedTile(t).getComputeCode();
            cs.setHasCode();
           /*
            cs.addStatementToBufferInit("// Static Network Barrier ");
            cs.addStatementToBufferInit("ilib_mem_fence()");
            if (TileraBackend.chip.getTranslatedTile(t).getTileNumber() != 0) { 
                
                cs.addStatementToBufferInit("sn_receive()");
            }
            cs.addStatementToBufferInit("sn_send(43)");
            cs.addStatementToBufferInit("sn_receive()");
            if (TileraBackend.chip.getTranslatedTile(t).getTileNumber() != 1) {
                cs.addStatementToBufferInit("sn_send(42)");
            }*/
            cs.addStatementToBufferInit("ilib_msg_barrier(ILIB_GROUP_SIBLINGS)");
        }
    }
    
    /**
     * Append a barrier instruction to all of the (abstract) tiles in the init/primepump
     * stage.
     */
    public static void addBarrierInit() {
        for (int t = 0; t < TileraBackend.chip.abstractSize(); t++) {
            TileCodeStore cs = TileraBackend.chip.getTranslatedTile(t).getComputeCode();
             cs.addInitStatement(Util.toStmt("ilib_msg_barrier(ILIB_GROUP_SIBLINGS)"));
             cs.setHasCode();
         }
    }
    
    /**
     * Append a barrier instruction to all of the (abstract) tiles in the steady state
     * method.
     */
    public static void addBarrierSteady() {
        for (int t = 0; t < TileraBackend.chip.abstractSize(); t++) {
            TileCodeStore cs = TileraBackend.chip.getTranslatedTile(t).getComputeCode();
            cs.addSteadyLoopStatement(Util.toStmt("/* Static Network Barrier */"));
            cs.setHasCode();
            String code[] = staticNetworkBarrier(cs.getParent());
            for (String stmt : code) {
                cs.addSteadyLoopStatement(Util.toStmt(stmt));
            }
            /*
            if (TileraBackend.chip.getTranslatedTile(t).getTileNumber() != 0) 
                cs.addSteadyLoopStatement(Util.toStmt("sn_receive()"));
            cs.addSteadyLoopStatement(Util.toStmt("sn_send(43)"));
            cs.addSteadyLoopStatement(Util.toStmt("sn_receive()"));
            //cs.addSteadyLoopStatement(Util.toStmt("ilib_mem_fence()"));
            if (TileraBackend.chip.getTranslatedTile(t).getTileNumber() != 1) 
                cs.addSteadyLoopStatement(Util.toStmt("sn_send(42)"));
            //cs.addStatementToBufferInit("ilib_msg_barrier(ILIB_GROUP_SIBLINGS)");
             
             */
        }
    }
    
    public static String[] staticNetworkBarrier(Tile tile) {
        String dir1 = "";
        String dir2 = "";
        String code[] = new String[7];
        
        int gXSize = TileraBackend.chip.abstractXSize();
        int gYSize = TileraBackend.chip.abstractYSize();
        assert gXSize % 2 == 0 &&
               gYSize % 2 == 0  : "Only even row / col sizes are supported";
        int row = tile.getY();
        int col = tile.getX();
        
        //SNAKE THE ITEMS AROUND THE CHIP IN A CIRCUIT
                
        if (row == 0 && col == 0) {
            dir1 = "SOUTH"; dir2 = "EAST";
        } else if (col % 2 == 0) {
            if (row == 0) { //not 0,0
                dir1 = "WEST"; dir2 = "EAST"; 
            } else if (row == 1 && col > 0) {
                dir1 = "WEST"; dir2 = "SOUTH";
            } else if (row == gYSize -1) {
                dir1 = "NORTH"; dir2 = "EAST"; 
            } else if (row % 2 == 0) {
                dir1 = "SOUTH"; dir2 = "NORTH"; 
            } else {
                dir1 = "NORTH"; dir2 = "SOUTH"; 
            }
        } else {
            //odd col
            if (row == 0 && col == gXSize -1) {
                dir1 = "SOUTH"; dir2 = "WEST"; 
            } else if (row == 0) {
                dir1 = "EAST"; dir2 = "WEST"; 
            } else if (row == 1 && col < gXSize - 1) {
                dir1 = "EAST"; dir2 = "SOUTH";
            } else if (row == gYSize - 1) {
                dir1 = "NORTH"; dir2 = "WEST";
            } else if (row % 2 == 0 ) {
                dir1 = "SOUTH"; dir2 = "NORTH";
            } else {
                dir1 = "NORTH"; dir2 = "SOUTH";
            }
        }
        
        System.out.println(TileraBackend.chip.getTranslatedTileNumber(tile.getTileNumber()) + " " + dir1 + ", " + dir2);

        assert !dir1.equals("") || !dir2.equals("");
        code[0] = "uint64_t cycle; cycle = get_cycle_count(); ilib_mem_fence()";
        //code[0] = "/* nothing */";
        code[1] = "__insn_mtspr(SPR_SNSTATIC, (MAIN_INPUT(SN_" + dir1 + ") | " + dir1 + "_INPUT(SN_MAIN)))";
        code[2] = "__insn_mtspr(SPR_SNCTL, 0x2)";
        code[3]= "sn_send(1); sn_receive()";
        code[4] = "__insn_mtspr(SPR_SNSTATIC, (MAIN_INPUT(SN_" + dir2 + ") | " + dir2 + "_INPUT(SN_MAIN)))";
        code[5] = "__insn_mtspr(SPR_SNCTL, 0x2)";
        code[6] = "sn_send(2); sn_receive(); PASS(cycle);";
        
        
        return code;
    }
    
    
    /**
     * Return the method that initializes the rotating buffers and communicates
     * addresses.
     * @return the method that initializes the rotating buffers and communicates
     * addresses.
     */
    public JMethodDeclaration getBufferInitMethod() {
        return bufferInit;
    }
    
    private void createBufferInitMethod() {
        //create the method that will malloc the buffers and receive the addresses from downstream tiles
        bufferInit = new JMethodDeclaration(CStdType.Void, bufferInitMethName, new JFormalParameter[0], new JBlock());
        //create a status variable for the point to point messages and the dma commands
        addStatementToBufferInit("ilibStatus status");
        //addMethod(bufferInit);
    }
    
    public Tile getParent() {
        return (Tile)parent;
    }
    
    /**
     * Append str to the text that will appear outside of any function near the top 
     * of the code for this tile.
     * 
     * @param str The string to add
     */
    public void appendTxtToGlobal(String str) {
        globalTxt.append(str);
        this.setHasCode();
    }
    
    /**
     * Return the string to add to the global portion of the c file
     * @return the string to add to the global portion of the c file
     */
    public String getGlobalText() {
        return globalTxt.toString();
    }
    
    /**
     * Add stmt to the end of the method that will perform the allocation of buffers
     * and receive addresses of buffers from downstream tiles.
     * 
     * @param stmt The statement to add to the end of the method
     */
    public void addStatementToBufferInit(JStatement stmt) {
        bufferInit.getBody().addStatement(stmt);
        this.setHasCode();
    }
    
    /**
     * Add txt to the beginning of the method that will perform the allocation of buffers
     * and receive addresses of buffers from downstream tiles.  Don't use ; or newline
     * 
     * @param txt The statement to add to the end of the method
     */
    public void addStatementFirstToBufferInit(String txt) {
        JStatement stmt = new JExpressionStatement(new JEmittedTextExpression(txt));
        bufferInit.getBody().addStatementFirst(stmt);
        this.setHasCode();
    }
    
    /**
     * Add stmt to the beginning of the method that will perform the allocation of buffers
     * and receive addresses of buffers from downstream tiles.
     * 
     * @param stmt The statement to add to the end of the method
     */
    public void addStatementFirstToBufferInit(JStatement stmt) {
        bufferInit.getBody().addStatementFirst(stmt);
        this.setHasCode();
    }
    
    
    /**
     * Add txt to the end of the method that will perform the allocation of buffers
     * and receive addresses of buffers from downstream tiles.  Don't use ; or newline
     * 
     * @param txt The statement to add to the end of the method
     */
    public void addStatementToBufferInit(String txt) {
        JStatement stmt = new JExpressionStatement(new JEmittedTextExpression(txt));
        bufferInit.getBody().addStatement(stmt);
        this.setHasCode();
    }
    
    /**
     * Constructor: caller will add code to bound number of iterations, no pointer back to compute node.
     * @param iterationBound a variable that will be defined locally by <code>getMainFunction().addAllStatments(0,stmts);</code>
     */
    public TileCodeStore(ALocalVariable iterationBound) {
        super(iterationBound);
        setMyMainName("__main__");
    }
    
    /**
     * Constructor: steady state loops indefinitely, no pointer back to compute node.
     */
    public TileCodeStore() {
        super();
        setMyMainName("__main__");
    }
    
    /**
     * Return true if we should generate code for this tile,
     * false if no code was ever generated for this tile.
     * 
     * @return true if we should generate code for this tile,
     * false if no code was ever generated for this tile.
     */
    public boolean shouldGenerateCode() {
        return hasCode;
    }
    
    /** 
     * Set that this tile (code store) has code written to it and thus 
     * it needs to be considered during code generation.
     */
    public void setHasCode() {
        hasCode = true;
    }
    
    /** 
     * For each of the file writers generate code to print the output for each steady state.
     * This is somewhat limited right now, so there are lots of asserts.
     */
    public static void generatePrintOutputCode() {
        Set<InputRotatingBuffer> fwb = InputRotatingBuffer.getFileWriterBuffers();
        if (fwb.size() == 0)
            return;
        //for now asser that we only have one file writer
        assert fwb.size() == 1;
        //for each of the file writer input buffers, so for each of the file writers,
        //find one of its sources, and add code to the source's tile to print the outputs
        //at the end of each steady state
        for (InputRotatingBuffer buf : fwb) {
            FilterSliceNode fileW = buf.filterNode;
            //find the tile of the first input to the file writer
            Tile tile = 
                TileraBackend.backEndBits.getLayout().
                getComputeNode(fileW.getParent().getHead().getSources(SchedulingPhase.STEADY)[0].
                        getSrc().getParent().getFirstFilter());
            
            tile.getComputeCode().addPrintOutputCode(buf);
        }
    }
    
    /**
     * Add code to print the output written to the file writer mapped to this tile.
     */
    private void addPrintOutputCode(InputRotatingBuffer buf) {
        //We print the address buffer after it has been rotated, so that it points to the section
        //of the filewriter buffer that is about to be written to, but was written to 2 steady-states
        //ago
        FilterSliceNode fileW = buf.filterNode;
        assert fileW.isFileOutput();
        //because of this scene we need a rotation length of 2
        assert buf.getRotationLength() == 2;
        //make sure that each of the inputs wrote to the file writer in the primepump stage
        for (InterSliceEdge edge : fileW.getParent().getHead().getSourceSet(SchedulingPhase.STEADY)) {
            assert TileraBackend.scheduler.getGraphSchedule().getPrimePumpMult(edge.getSrc().getParent()) == 1;
        }
        int outputs = fileW.getFilter().getSteadyMult();
        String type = ((FileOutputContent)fileW.getFilter()).getType() == CStdType.Integer ? "%d" : "%f";
        String cast = ((FileOutputContent)fileW.getFilter()).getType() == CStdType.Integer ? "(int)" : "(float)";
        String bufferName = buf.getAddressRotation(this.parent).currentWriteBufName;
        //create the loop
        addSteadyLoopStatement(Util.toStmt(
                "for (int _i_ = 0; _i_ < " + outputs + "; _i_++) printf(\"" + type + "\\n\", " + cast + 
                bufferName +"[_i_])"));
    }
    
    public void generateNumbersCode() {
        appendTxtToGlobal("uint64_t* __cycle_counts__;\n");
        appendTxtToGlobal("int __iteration__ = 0;\n");
        
        appendTxtToGlobal("void __printSSCycleAvg() {\n");
        appendTxtToGlobal("    uint64_t totalCycles = 0;\n");
        appendTxtToGlobal("    uint64_t avgCycles;\n");
        appendTxtToGlobal("    int i = 0;\n");
        appendTxtToGlobal("     for (i = 0; i < ITERATIONS - 1; i++)\n"); 
        appendTxtToGlobal("      totalCycles += __cycle_counts__[i+1] - __cycle_counts__[i];\n");

        appendTxtToGlobal("    avgCycles = totalCycles / (ITERATIONS - 1);\n");
        if (ProcessFileWriter.getTotalOutputs() > 0) {
            appendTxtToGlobal("    printf(\"Average cycles per SS for %d iterations: %llu, avg cycles per output: %llu \\n\", ITERATIONS, avgCycles" + 
                    ", (avgCycles / ((uint64_t)" +
                        ProcessFileWriter.getTotalOutputs() + ")));\n");
        } else {
            appendTxtToGlobal("    printf(\"Average cycles per SS for %d iterations: %llu \\n\", ITERATIONS, avgCycles);\n");
        }
        appendTxtToGlobal("    __iteration__ = 0;\n");
        appendTxtToGlobal("    ilib_abort();\n");

        appendTxtToGlobal("  }\n");

        addSteadyLoopStatement(Util.toStmt("__cycle_counts__[__iteration__++] = get_cycle_count()"));
        addSteadyLoopStatement(Util.toStmt("if (__iteration__ == ITERATIONS) __printSSCycleAvg()"));
        
        addStatementToBufferInit("__cycle_counts__ = (uint64_t*)malloc(ITERATIONS * sizeof(uint64_t))");
    }
    
    public void createExcludedProcessGroup(Slice[] level) {
        //create the list of tiles that are used by this group
        HashSet<Integer> tilesUsed = new HashSet<Integer>();
        for (Slice slice : level) {
            int absTile = 
                TileraBackend.chip.getTranslatedTileNumber(
                        TileraBackend.backEndBits.getLayout().getComputeNode(slice.getFirstFilter()).getTileNumber());
            tilesUsed.add(absTile);
        }
        //create the list of tiles not utilized during this level
        List<Tile> tilesToExclude = TileraBackend.chip.getAbstractTiles();
        for (Tile t : tilesToExclude) {
            if (tilesUsed.contains(TileraBackend.chip.getTranslatedTile(t.getTileNumber())))
                tilesToExclude.remove(TileraBackend.chip.getTranslatedTile(t.getTileNumber()));
        }
    }
}
