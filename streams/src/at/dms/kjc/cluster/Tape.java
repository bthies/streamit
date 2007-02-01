/**
 *
 */
package at.dms.kjc.cluster;

import at.dms.kjc.CType;
import at.dms.kjc.common.CodegenPrintWriter;

/**
 * Common elements for all kinds of tapes.
 * <br/>
 * A tape represents the queue of data between
 * two processing nodes.  It has a source (processing 
 * node), a destination (processing node), it carries
 * items of a particular type.  The source end can
 * push items at the front of the tape.  The destination
 * end can peek for items on the tape, pop items from
 * the tape (and in the odd case of enqueues push items
 * onto the tape).
 * 
 * <br/>
 * Based on Janis' idea of mostly generating strings
 * rather than attempting to hack up IR code and use
 * existing compiler routines to eventually generate 
 * the strings.
 *
 * Note to self, known implemenations:
 *     cluster nodes using sockets,
 *     cluster nodes using cluster fusion,
 *     cluster standalone buffers (copy down),
 *       cluster standalone buffers (wrap),
 *     cluster standalone dynamic rate edges.
 *
 * @author dimock
 *
 */
public interface Tape {
    /**
     * Source of tape.
     * 
     * @return unique indicator for source SIROperator of tape
     */
    public int getSource();

    /**
     * Destination of tape.
     * 
     * @return unique indicator for destination SIROperator of tape
     */
    public int getDest();
    
    /** 
     * Get type of objects communicated over the tape
     *
     * @return The type of objects communicated over the tape
     */
    public CType getType();
    
    /**
     * name of routine to push an item onto the tape.
     */
    public String getPushName();
    
    /**
     * name of routine to pop an item from the tape,
     * or in case of an array, the address of an item...
     */
    public String getPopName();
    
    /**
     * name of routine to peek an item from the tape,
     * or in case of an array, the address of an item...
     */
    public String getPeekName();
    
    /**
     * Data declaration: in .h file if needed else noop.
     * @return TODO
     */
    public String dataDeclarationH();
    
    /**
     * Data declaration: in .c file if needed else noop.
     * @return TODO
     */
    public String dataDeclaration();
    
    /**
     * Data declaration: following "extern" if needed else noop.
     * For downstream end of tape.
     * @return TODO
     */
    public String downstreamDeclarationExtern();
    /**
     * Data declaration: for code doing downstream processing.
     * @return TODO
     */
    public String downstreamDeclaration();
    
    /**
     * Data declaration: following "extern" if needed else noop.
     * For upstream end of tape.
     * @return TODO
     */
    public String upstreamDeclarationExtern();
    /**
     * Data declaration: for code doing upstream processing.
     * @return TODO
     */
    public String upstreamDeclaration();
    
    /**
     * Code used at top of work iteration.
     * <br/>
     * For instance: resetting pointers in a buffer.
     * @return TODO
     */
    public String topOfWorkIteration();
    
    /**
     * Any cleanup needed before exit for upstream end of tape.
     * @return TODO
     */
    public String upstreamCleanup();
    
    /**
     * Any cleanup needed before exit for downstream end of tape.
     * @return TODO
     */
    public String downstreamCleanup();
    
    /**
     * prefix before data expression to push. 
     * @return TODO
     */
    public String pushPrefix();
    /**
     * suffix after data expression to push.
     * @return TODO
     */
    public String pushSuffix();
    
    public String pushManyItems(String sourceBuffer, int sourceOffset, int numItems);
    public String popManyItems(String destBuffer, int destOffset, int numItems);

    /**
     * Statement, but no ";" to pop an item into a variable.
     * Useful since C can not handle arrays over tapes by simple assignment.
     */
    public String assignPopToVar(String varName);
    
    /**
     * expression to pop an item: to end of statement.
     * @return TODO
     */
    public String popExpr();

    /**
     * expression to pop an item, really an expression.
     */
    public String popExprNoCleanup();
    
    /**
     * if expression to pop an item requires statements to clean up
     * then this is cleanup.
     */
    public String popExprCleanup();
    
    /**
     * prefix to pop N items discarding them.
     * @return TODO
     */
    public String popNStmt(int n);

//    /**
//     * name of routine to peek item N from tape (or addresso of item in case of array).
//     */
//    
//    public String peekRoutineName(int n);
 
    /**
     * Assign the result of peeking.
     * Must deal with peeking an array.
     * @param varName 
     * @param offset 
     * @return 
     */
    public String assignPeekToVar(String varName, String offset);
    /**
    * prefix to peek expression
    * @return TODO
    */
    public String peekPrefix();
    /**
     * suffix to peek expression
     * @return TODO
     */
     public String peekSuffix();
    
     public String pushbackInit(int NumberToPush);
     public String pushbackPrefix();
     public String pushbackSuffix();
     public String pushbackCleanup();
}
