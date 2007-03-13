package at.dms.kjc.spacetime;

import java.util.Vector;

import at.dms.kjc.backendSupport.Layout;
import at.dms.kjc.backendSupport.SchedulingPhase;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.util.Utils;
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashMap;
import java.io.*;
import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.slicegraph.FilterInfo;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.OutputSliceNode;

import java.util.Hashtable;
import java.math.BigInteger;
import at.dms.kjc.slicegraph.FilterContent;

/**
 * This abstract class defines an interface for filter code generators.  
 * These generator add code necessary to execute the filter on raw, generating
 * SIR code.
 * 
 * @author mgordon
 *
 */
public abstract class RawExecutionCode 
{
    /** the prefix for the c method to receive structs over a network */
    public static String structReceivePrefix = "__popPointer";
    
    /** the c method that is used to receive structs over the static net */
    public static String structReceivePrefixStatic = structReceivePrefix
        + "Static";
    /** the c method that is used to receive structs over the gdn */
    public static String structReceivePrefixDynamic = structReceivePrefix
        + "Dynamic";

    //if true, inline the work function in the init and steady-state
    protected static final boolean INLINE_WORK = true;
    
    /*** fields for the var names we introduce ***/
    public static String recvBuffer = "__RECVBUFFER__";
    public static String recvBufferSize = "__RECVBUFFERSIZE__";
    public static String recvBufferBits = "__RECVBUFFERBITS__";

    //the output buffer for ratematching
    public static String sendBuffer = "__SENDBUFFER__";
    public static String sendBufferIndex = "__SENDBUFFERINDEX__";
    public static String rateMatchSendMethod = "__RATEMATCHSEND__";
    
    //recvBufferIndex points to the beginning of the tape
    public static String recvBufferIndex = "__RECVBUFFERINDEX__";
    //recvIndex points to the end of the tape
    public static String recvIndex = "_RECVINDEX__";

    public static String simpleIndex = "__SIMPLEINDEX__";
    
    public static String exeIndex = "__EXEINDEX__";
    public static String exeIndex1 = "__EXEINDEX__1__";

    public static String ARRAY_INDEX = "__ARRAY_INDEX__";
    public static String ARRAY_COPY = "__ARRAY_COPY__";

    public static String initSchedFunction = "__RAWINITSCHED__";
    public static String steadySchedFunction = "__RAWSTEADYSCHED__";
    
    public static String staticReceiveMethod = "static_receive_to_mem";
    public static String gdnReceiveMethod = "gdn_receive_to_mem";
    public static String staticSendMethod = "static_send";
    public static String gdnSendMethod = "gdn_send";
    public static String structReceiveMethodPrefix = "__popPointer";
    public static String arrayReceiveMethod = "__array_receive__";

    public static String initStage = "__INITSTAGE__";
    public static String steadyStage = "__STEADYSTAGE__";
    public static String primePumpStage = "__PRIMEPUMP__";
    public static String workCounter = "__WORKCOUNTER__";
 
    //keep a unique integer for each filter in each trace
    //so var names do not clash
    private static int globalID = 0;
    protected int uniqueID;

    protected GeneratedVariables generatedVariables;
    protected FilterInfo filterInfo;
    /** the method that executes each stage of the prime pump schedule */
    protected JMethodDeclaration primePumpMethod;
    /** true if this filter has dynamic network input, this will only 
     * happen when the filters is connected to a inputtracenode and on the dynamic 
     * net
     */
    protected boolean gdnInput;
    /** true if this filter has dynamic network output, this will only happen
     * if hte filter is connected to an outputtrcenode allocated to the gdn
     */
    protected boolean gdnOutput;
    /** the tile this filter is mapped to */
    protected RawTile tile;
    
    protected Layout layout;
    
    public RawExecutionCode(RawTile tile, FilterInfo filterInfo, Layout layout) 
    {
        this.layout = layout;
        this.tile = tile;
        this.filterInfo = filterInfo;
        generatedVariables = new GeneratedVariables();
        uniqueID = getUniqueID();
        primePumpMethod = null;
        //see if we have gdn input
        gdnInput = false;
        if (filterInfo.sliceNode.getPrevious().isInputSlice()) {
            if (!IntraSliceBuffer.getBuffer(
                    (InputSliceNode)filterInfo.sliceNode.getPrevious(),
                    filterInfo.sliceNode).isStaticNet())
                gdnInput = true;
        }
        //see if we have gdn output
        gdnOutput = false;
        if (filterInfo.sliceNode.getNext().isOutputSlice()) {
            if (!IntraSliceBuffer.getBuffer(filterInfo.sliceNode,
                    (OutputSliceNode)filterInfo.sliceNode.getNext()).isStaticNet())
                gdnOutput = true;
        }
    }
    

    public static int getUniqueID() 
    {
        return globalID++;
    }
    
    public abstract JFieldDeclaration[] getVarDecls();
    public abstract JMethodDeclaration[] getHelperMethods();
    /** 
     * @return the method we should call to execute the init stage.
     */
    public abstract JMethodDeclaration getInitStageMethod();
    /**
     * @return The block we should inline to execute the steady-state
     */
    public abstract JBlock getSteadyBlock();
    /**
     * @return The method to call for one execution of the filter in the
     * prime pump stage.
     */
    public abstract JMethodDeclaration getPrimePumpMethod();
   
    /**
     * Returns a for loop that uses field <pre>var</pre> to count
     * <pre>count</pre> times with the body of the loop being <pre>body</pre>.  
     * If count is non-positive, just returns empty (!not legal in the general case)
     * 
     * @param body The body of the for loop.
     * @param var The field to use as the index variable.
     * @param count The trip count of the loop.
     * 
     * @return The for loop.
     */
    public static JStatement makeForLoopFieldIndex(JStatement body,
            JVariableDefinition var,
            JExpression count) {
        if (body == null)
            return new JEmptyStatement(null, null);
    
        // make init statement - assign zero to <pre>var</pre>.  We need to use
        // an expression list statement to follow the convention of
        // other for loops and to get the codegen right.
        JExpression initExpr[] = {
            new JAssignmentExpression(null,
                                      new JFieldAccessExpression(null, 
                                                                 new JThisExpression(null),
                                                                 var.getIdent()),
                                      new JIntLiteral(0)) };
        JStatement init = new JExpressionListStatement(null, initExpr, null);
        // if count==0, just return init statement
        if (count instanceof JIntLiteral) {
            int intCount = ((JIntLiteral)count).intValue();
            if (intCount<=0) {
                // return assignment statement
                return new JEmptyStatement(null, null);
            }
        }
        // make conditional - test if <pre>var</pre> less than <pre>count</pre>
        JExpression cond = 
            new JRelationalExpression(null,
                                      Constants.OPE_LT,
                                      new JFieldAccessExpression(null, 
                                                                 new JThisExpression(null),
                                                                 var.getIdent()),
                                      count);
        JExpression incrExpr = 
            new JPostfixExpression(null, 
                                   Constants.OPE_POSTINC, 
                                   new JFieldAccessExpression(null, new JThisExpression(null),
                                                              var.getIdent()));
        JStatement incr = 
            new JExpressionStatement(null, incrExpr, null);

        return new JForStatement(null, init, cond, incr, body, null);
    }

    /**
     * Returns a for loop that uses local <pre>var</pre> to count
     * <pre>count</pre> times with the body of the loop being <pre>body</pre>.  
     * If count is non-positive, just returns empty (!not legal in the general case)
     * 
     * @param body The body of the for loop.
     * @param local The local to use as the index variable.
     * @param count The trip count of the loop.
     * 
     * @return The for loop.
     */
    public static JStatement makeForLoopLocalIndex(JStatement body,
            JVariableDefinition local,
            JExpression count) {
        if (body == null)
            return new JEmptyStatement(null, null);
    
        // make init statement - assign zero to <pre>var</pre>.  We need to use
        // an expression list statement to follow the convention of
        // other for loops and to get the codegen right.
        JExpression initExpr[] = {
            new JAssignmentExpression(null,
                    new JLocalVariableExpression(local),
                    new JIntLiteral(0)) };
        JStatement init = new JExpressionListStatement(null, initExpr, null);
        // if count==0, just return init statement
        if (count instanceof JIntLiteral) {
            int intCount = ((JIntLiteral)count).intValue();
            if (intCount<=0) {
                // return assignment statement
                return new JEmptyStatement(null, null);
            }
        }
        // make conditional - test if <pre>var</pre> less than <pre>count</pre>
        JExpression cond = 
            new JRelationalExpression(null,
                                      Constants.OPE_LT,
                                      new JLocalVariableExpression(local),
                                      count);
        JExpression incrExpr = 
            new JPostfixExpression(null, 
                                   Constants.OPE_POSTINC, 
                                   new JLocalVariableExpression(local));
        JStatement incr = 
            new JExpressionStatement(null, incrExpr, null);

        return new JForStatement(null, init, cond, incr, body, null);
    }
    
    public static JStatement constToSwitchStmt(int constant) 
    {
        //alt code gen is always enabled!
        JAssignmentExpression send = 
            new JAssignmentExpression(null,
                                      new JFieldAccessExpression(null, new JThisExpression(null),
                                                                 Util.CSTOINTVAR),
                                      new JIntLiteral(constant));
    
        return new JExpressionStatement(null, send, null);
    }
    
    public static JStatement boundToSwitchStmt(int constant)  
    {
        return constToSwitchStmt(constant - 1);
    }
    
    /** 
     * @return The SIR code necessary to set the dynamic message header used 
     * when we send data over the gdn.  Also, if we are sending data to a dram
     * that we are not the owner of and no other filter in our slice is allocated 
     * to the owner tile, we have to wait until we receive a word over
     * that static network (it will be from the owner), this will tell us that
     * the owner of the dram has issued the store command to the dram and we 
     * can proceed with writing data to the dram.
     *
     * @param init true if init
     * @param primepump true if primepump
     */
    
    protected JStatement setupGDNStore(SchedulingPhase whichPhase) {
        //if we are not sending anything, don't do anything...
        if (filterInfo.totalItemsSent(whichPhase) == 0)
            return new JEmptyStatement();
        
        JBlock block = new JBlock();
        
        block.addStatement(setDynMsgHeader());
        
        //get the buffer
        IntraSliceBuffer buf = IntraSliceBuffer.getBuffer(filterInfo.sliceNode,
                (OutputSliceNode)filterInfo.sliceNode.getNext());
        
        //now see if this tile is not the owner of the dram
        //and a previous filter of the slice is not allocated on the
        //tile that owns the dram we want to store into
        //we need to wait until the owner has issued a store command,
        //after the owner has done that, it will send a word to us over the
        //static network
        if (!Util.doesSliceUseTile(filterInfo.sliceNode.getParent(),
                buf.getOwner(), layout)) {
            block.addStatement(gdnReceive(true, 
                    new JFieldAccessExpression(TraceIRtoC.DUMMY_VOLATILE)));
        }
        
        return block;
    }
    
    /**
     * @param integer if true use csti_integer, false use csti_fp
     * @param recvInto The expression to receive into
     * @return A statement to receive into <pre>recvInto</pre> from the static network.
     */
    public JStatement gdnReceive(boolean integer, JExpression recvInto) {
        JAssignmentExpression ass = 
            new JAssignmentExpression(recvInto, 
                    new JFieldAccessExpression(integer ? Util.CSTIINTVAR : Util.CSTIFPVAR));
        return new JExpressionStatement(ass);
    }
    
    /** 
     * @return The SIR code necessary to set the dynamic message header used 
     * when we send data over the gdn.  
     */
    public JStatement setDynMsgHeader() {
       
        assert filterInfo.sliceNode.getNext().isOutputSlice();
        //get the buffer
        IntraSliceBuffer buf = IntraSliceBuffer.getBuffer(filterInfo.sliceNode,
                (OutputSliceNode)filterInfo.sliceNode.getNext());
        assert !buf.isStaticNet();
        //get the type size
        int size = Util.getTypeSize(filterInfo.filter.getOutputType());
        
        //add one to the size because we need to send over the opcode 13 
        //with each pkt to inform the dram that this is a data payload
        size += 1;
        
        //make sure that each element can fit in a gdn pkt         
        assert size <= RawChip.MAX_GDN_PKT_SIZE : "Type size too large to fit in single dynamic network packet";
        
        JBlock block = new JBlock(null, new JStatement[0], null);
        //construct the args for the dyn header construction function
        
        //get the neigboring tile for the dram we are sending to...
        RawTile neighboringTile = buf.getDRAM().getNeighboringTile();
        
        //now calculated the final route, once the packet gets to the destination (neighboring) tile
        //it will be routed off the chip by 
        // 2 = west, 3 = south, 4 = east, 5 = north
        int finalRoute = buf.getDRAM().getDirectionFromTile();
        
        
        JExpression[] args = {new JIntLiteral(finalRoute),
                new JIntLiteral(size), 
                new JIntLiteral(0) /* user */, 
                new JIntLiteral(tile.getY()), new JIntLiteral(tile.getX()),
                new JIntLiteral(neighboringTile.getY()),
                new JIntLiteral(neighboringTile.getX())
        };
        
        JMethodCallExpression conDynHdr = 
            new JMethodCallExpression(RawChip.ConstructDynHdr, args);
        
        JAssignmentExpression ass = 
            new JAssignmentExpression(new JFieldAccessExpression(TraceIRtoC.DYNMSGHEADER),
                    conDynHdr);
        block.addStatement(new JExpressionStatement(ass));
        
        return block;
    }
    
    /**
     * If we are in the prime pump or the steady state, we may be compressing
     * the switch code and if so, we need to send the push rate and the pop rate
     * to the switch for loop bounds.  This function will decide if we need to do that
     * and if so, do it (append the ins to <pre>workBlock</pre>).
     * 
     * @param filterInfo
     * @param workBlock
     */
    protected void sendRatesToSwitch(FilterInfo filterInfo, JBlock workBlock) {
        //if we are compressing the sends and receives on the switch for this
        //filter, we must send them now
        if (Rawify.compressFilterSwitchIns(filterInfo.steadyMult, filterInfo.pop, 
                filterInfo.push, false)) {
                //Rawify.SWITCH_COMP && filterInfo.steadyMult > Rawify.SC_THRESHOLD) {
            //if we are compressing the receives, send the trip count to the switch
            //don't do this if are using the gdn
            if (filterInfo.itemsNeededToFire(0, false) > Rawify.SC_INS_THRESH &&
                    !gdnInput) {
                workBlock.addStatement(boundToSwitchStmt(filterInfo.itemsNeededToFire(0, false)));
            }
            //if we are compressing the sends, send the trip count to the switch
            //don't do this if we are using the gdn
            if (filterInfo.itemsFiring(0, false) > Rawify.SC_INS_THRESH &&
                    !gdnOutput) {
                workBlock.addStatement(boundToSwitchStmt(filterInfo.itemsFiring(0, false)));
            }
        }
    }
    
    /**
     * @param words
     * @return Return code to receive <pre>words</pre> words into a dummy variable defined in 
     * TraceIRToC over the gdn.
     */
    public static JBlock gdnDisregardIncoming(int words) {
        /* TODO:  move these functions to some static class with utils on drams */
        JBlock block = new JBlock();

        assert words < RawChip.cacheLineWords : "Should not align more than cache-line size.";
        
        for (int i = 0; i < words; i++) {
            //receive the word into the dummy volatile variable
            JStatement receiveDummy = 
                new JExpressionStatement(
                        new JAssignmentExpression(new JFieldAccessExpression(TraceIRtoC.DUMMY_VOLATILE),
                                new JFieldAccessExpression(Util.CGNIINTVAR)));
            block.addStatement(receiveDummy);
        }
        return block;
    }
    
    /**
     * @param words
     * @return Return code to send <pre>words</pre> words over the gdn using a predefined
     * header. 
     */
    public static JBlock gdnDummyOutgoing(int words) {
        JBlock block = new JBlock();

        assert words < RawChip.cacheLineWords : "Should not align more than cache-line size.";
        
        for (int i = 0; i < words; i++) {
            //send the header
            JStatement sendHeader = 
                new JExpressionStatement(
                        new JAssignmentExpression(new JFieldAccessExpression(Util.CGNOINTVAR),
                                new JFieldAccessExpression(TraceIRtoC.DYNMSGHEADER)));
            block.addStatement(sendHeader);
            //send over the opcode 13 to tell the dram that this is a data packet
            JStatement sendDataOpcode = 
                new JExpressionStatement(
                        new JAssignmentExpression(new JFieldAccessExpression(Util.CGNOINTVAR),
                                new JIntLiteral(RawChip.DRAM_GDN_DATA_OPCODE)));
            block.addStatement(sendDataOpcode);
            //send over negative one as the dummy value
            JStatement sendMinusOne = 
                new JExpressionStatement(
                        new JAssignmentExpression(new JFieldAccessExpression(Util.CGNOINTVAR),
                                new JIntLiteral(-1)));
            block.addStatement(sendMinusOne);
        }
        return block;
    }
    
    /**
     * Generate code to complete the dram transactions because we have to align
     * dram transfers to cache-line sized transfers.  Only do this if we are 
     * sending or receiving over the gdn. 
     * 
     * ??param wordsSent The number of words sent during the execution of the stage.
     * ??param wordsReceived The number of words received during the execution of the
     * stage.
     * 
     * @param init Are we in the init stage?  Used to calculate the number of items we
     * generate and receive.
     * 
     * @return ???
     * 
     */
    protected JBlock gdnCacheAlign(boolean init) {
        JBlock block = new JBlock();
        
        SchedulingPhase whichPhase = init? SchedulingPhase.INIT : SchedulingPhase.STEADY;
        
        if (gdnInput) {
            int wordsReceived = filterInfo.totalItemsReceived(whichPhase) * 
                Util.getTypeSize(filterInfo.filter.getInputType());
            //first make sure that we are not receiving code from a file
            //reader, because we do not align file readers
            InputSliceNode input = (InputSliceNode)filterInfo.sliceNode.getPrevious();            
            //if not a file reader, then we might have to align the dest
            if (!Util.onlyFileInput(input) && wordsReceived > 0 &&
                    wordsReceived % RawChip.cacheLineWords != 0) {
                //calculate the number of words that we have to send and receive in 
                //order to complete the dram transactions
                int recAlignWords = RawChip.cacheLineWords
                - ((wordsReceived) % RawChip.cacheLineWords);
                
                //generate the code to disregard incoming data
                block.addStatement(gdnDisregardIncoming(recAlignWords));
            }
        }
        
        if (gdnOutput) {
            int wordsSent = Util.getTypeSize(filterInfo.filter.getOutputType()) *
                filterInfo.totalItemsSent(whichPhase);
            
            OutputSliceNode output = (OutputSliceNode)filterInfo.sliceNode.getNext();
            
            //first make sure that we are not writing eventually to a file writer
            //file writers don't need to be aligned
            if (!Util.onlyWritingToAFile(output) && wordsSent > 0 &&
                    wordsSent % RawChip.cacheLineWords != 0) {
                int sendAlignWords = RawChip.cacheLineWords - 
                ((wordsSent) % RawChip.cacheLineWords);

                //genereate the code to send dummy values 
                block.addStatement(gdnDummyOutgoing(sendAlignWords));
            }
        }
        
        return block;
        
    }
    
}