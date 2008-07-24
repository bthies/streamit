package at.dms.kjc.tilera;

import at.dms.kjc.backendSupport.ComputeCodeStore;
import at.dms.kjc.common.ALocalVariable;
import at.dms.kjc.*;
import java.util.HashSet;
import java.util.Set;
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
            TileCodeStore cs = TileraBackend.chip.getTranslatedTile(t).getComputeCode();
            cs.addStatementToBufferInit("ilib_msg_barrier(ILIB_GROUP_SIBLINGS)");
        }
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
    
    /**
     * Append str to the text that will appear outside of any function near the top 
     * of the code for this tile.
     * 
     * @param str The string to add
     */
    public void appendTxtToGlobal(String str) {
        globalTxt.append(str);
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
}
