package at.dms.kjc.tilera;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import at.dms.kjc.spacetime.BasicSpaceTimeSchedule;
import at.dms.kjc.backendSupport.*;
import at.dms.kjc.CClassType;
import at.dms.kjc.CStdType;
import at.dms.kjc.JAssignmentExpression;
import at.dms.kjc.JBlock;
import at.dms.kjc.JBooleanLiteral;
import at.dms.kjc.JEmittedTextExpression;
import at.dms.kjc.JExpression;
import at.dms.kjc.JExpressionStatement;
import at.dms.kjc.JFieldAccessExpression;
import at.dms.kjc.JFormalParameter;
import at.dms.kjc.JIfStatement;
import at.dms.kjc.JIntLiteral;
import at.dms.kjc.JLocalVariableExpression;
import at.dms.kjc.JLogicalComplementExpression;
import at.dms.kjc.JMethodDeclaration;
import at.dms.kjc.JPostfixExpression;
import at.dms.kjc.JStatement;
import at.dms.kjc.JVariableDeclarationStatement;
import at.dms.kjc.JVariableDefinition;
import at.dms.kjc.slicegraph.*;


/**
 * In this rotating buffer we add a transfer pointer that is always one behind the current 
 * pointer and points to the buffer we should be transferring from.
 * 
 * @author mgordon
 *
 */
public class OutputRotatingBufferDMA extends OutputRotatingBuffer {
    /** definition of boolean used during primepump to see if it is the first exection */
    protected JVariableDefinition firstExe;
    protected String firstExeName;
    /** the output slice node for this output buffer */
    /** the dma commands that are generated for this output buffer */
    protected OutputBufferDMATransfers dmaCommands;
    /** the address buffers that this output rotation uses as destinations for dma commands */ 
    protected HashMap<InputRotatingBuffer, SourceAddressRotation> addressBuffers;

    /**
     * Create all the output buffers necessary for this slice graph.  Iterate over
     * the steady-state schedule, visiting each slice and creating an output buffer
     * for the filter of the slice
     * 
     * @param slices The steady-state schedule of slices
     */
    public static void createOutputBuffers(BasicSpaceTimeSchedule schedule) {
        for (Slice slice : schedule.getScheduleList()) {
            assert slice.getNumFilters() == 1;
            if (!slice.getTail().noOutputs()) {
                assert slice.getTail().totalWeights(SchedulingPhase.STEADY) > 0;
                Tile parent = TileraBackend.backEndBits.getLayout().getComputeNode(slice.getFirstFilter());
                //create the new buffer, the constructor will put the buffer in the 
                //hashmap
                OutputRotatingBufferDMA buf = new OutputRotatingBufferDMA(slice.getFirstFilter(), parent);
                
                //calculate the rotation length
                int srcMult = schedule.getPrimePumpMult(slice);
                int maxRotLength = 0;
                for (Slice dest : slice.getTail().getDestSlices(SchedulingPhase.STEADY)) {
                    int diff = srcMult - schedule.getPrimePumpMult(dest);
                    assert diff >= 0;
                    if (diff > maxRotLength)
                        maxRotLength = diff;
                }
                buf.rotationLength = maxRotLength + 1;
                buf.createInitCode(false);
                //System.out.println("Setting output buf " + buf.getIdent() + " to " + buf.rotationLength);    
            }
        }
    }
    
    
    /**
     * Create a new output buffer that is associated with the filter node.
     * 
     * @param filterNode The filternode for which to create a new output buffer.
     */
    protected OutputRotatingBufferDMA(FilterSliceNode filterNode, Tile parent) {
        super(filterNode, parent);
        
        firstExeName = "__first__" + this.getIdent();        
        firstExe = new JVariableDefinition(null,
                at.dms.kjc.Constants.ACC_STATIC,
                CStdType.Boolean, firstExeName, new JBooleanLiteral(true));
        
        //fill the dmaaddressbuffers array
        addressBuffers = new HashMap<InputRotatingBuffer, SourceAddressRotation>();
        for (InterSliceEdge edge : outputNode.getDestSet(SchedulingPhase.STEADY)) {
            InputRotatingBuffer input = InputRotatingBuffer.getInputBuffer(edge.getDest().getNextFilter());
            addressBuffers.put(input, input.getAddressRotation(tile));               
        }
        
        //generate the dma commands
        dmaCommands = new OutputBufferDMATransfers(this);
    }
   
    /**
     * Return the output buffer associated with the filter node.
     * 
     * @param fsn The filter node in question.
     * @return The output buffer of the filter node.
     */
    public static OutputRotatingBufferDMA getOutputBuffer(FilterSliceNode fsn) {
        return (OutputRotatingBufferDMA)buffers.get(fsn);
    }
    
    /**
     * Return the set of all the InputBuffers that are mapped to tile t.
     */
    public static Set<RotatingBuffer> getBuffersOnTile(Tile t) {
        HashSet<RotatingBuffer> set = new HashSet<RotatingBuffer>();
        
        for (RotatingBuffer b : buffers.values()) {
            if (TileraBackend.backEndBits.getLayout().getComputeNode(b.getFilterNode()).equals(t))
                set.add(b);
        }
        
        return set;
    }
    
    /**
     * Return the address rotation that this output rotation uses for the given input slice node
     * 
     * @param input the input slice node 
     * @return the dma address rotation used to store the address of the 
     * rotation associated with this input slice node
     */
    public SourceAddressRotation getAddressBuffer(InputSliceNode input) {
        assert addressBuffers.containsKey(InputRotatingBuffer.getInputBuffer(input.getNextFilter()));
        
        return addressBuffers.get(InputRotatingBuffer.getInputBuffer(input.getNextFilter()));
    }
    

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#endInitWrite()
     */
    public List<JStatement> endInitWrite() {
        LinkedList<JStatement> list = new LinkedList<JStatement>();
        //in the init stage we use dma to send the output to the dest filter
        //but we have to wait until the end because are not double buffering
        //also, don't rotate anything here
        list.addAll(dmaCommands.dmaCommands(SchedulingPhase.INIT));
        return list;
    }
    
    /**
     *  We don't want to transfer during the first execution of the primepump
     *  so guard the execution in an if statement.
     */
    public List<JStatement> beginPrimePumpWrite() {
        LinkedList<JStatement> list = new LinkedList<JStatement>();
                
        list.add(zeroOutHead());
        
        JBlock block = new JBlock();
        block.addAllStatements(dmaCommands.dmaCommands(SchedulingPhase.STEADY));
        
        
        JIfStatement guard = new JIfStatement(null, new JLogicalComplementExpression(null, 
                new JEmittedTextExpression(firstExeName)), 
                block , new JBlock(), null);
        
        list.add(guard);
        return list;
    }
    
    public List<JStatement> endPrimePumpWrite() {
        LinkedList<JStatement> list = new LinkedList<JStatement>();
        
        //the wait for dma commands, only wait if this is not the first exec
        JBlock block1 = new JBlock();
        block1.addAllStatements(dmaCommands.waitCallsSteady());
        JIfStatement guard1 = new JIfStatement(null, new JLogicalComplementExpression(null, 
                new JEmittedTextExpression(firstExeName)), 
                block1 , new JBlock(), null);  
        list.add(guard1);
                
        
        //generate the rotate statements for this output buffer
        list.addAll(rotateStatementsCurRot());
        
        JBlock block2 = new JBlock();
        //rotate the transfer buffer only when it is not first
        block2.addAllStatements(rotateStatementsTransRot());
        //generate the rotation statements for the address buffers that this output
        //buffer uses, only do this after first execution
        for (SourceAddressRotation addrRot : addressBuffers.values()) {
            block2.addAllStatements(addrRot.rotateStatements());
        }
        JIfStatement guard2 = new JIfStatement(null, new JLogicalComplementExpression(null, 
                new JEmittedTextExpression(firstExeName)), 
                block2, new JBlock(), null);    
        list.add(guard2);
        
        //now we are done with the first execution to set firstExe to false
        list.add(new JExpressionStatement(
                new JAssignmentExpression(new JEmittedTextExpression(firstExeName), 
                        new JBooleanLiteral(false))));
        
        return list;
    }
    
    
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#beginSteadyWrite()
     */
    public List<JStatement> beginSteadyWrite() {
        LinkedList<JStatement> list = new LinkedList<JStatement>();
        list.add(zeroOutHead());
        list.addAll(dmaCommands.dmaCommands(SchedulingPhase.STEADY));
        return list;
    }
    
    /**
     * The rotate statements that includes the current buffer (for output of this 
     * firing) and transfer buffer.
     */
    protected List<JStatement> rotateStatements() {
        LinkedList<JStatement> list = new LinkedList<JStatement>();
        list.addAll(rotateStatementsTransRot());
        list.addAll(rotateStatementsCurRot());
        return list;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#endSteadyWrite()
     */
    public List<JStatement> endSteadyWrite() {
        LinkedList<JStatement> list = new LinkedList<JStatement>();
        list.addAll(dmaCommands.waitCallsSteady());
        //generate the rotate statements for this output buffer
        list.addAll(rotateStatements());
        
        //generate the rotation statements for the address buffers that this output
        //buffer uses
        for (SourceAddressRotation addrRot : addressBuffers.values()) {
            list.addAll(addrRot.rotateStatements());
        }
        return list;
    }
    
 
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#writeDecls()
     */
    public List<JStatement> writeDecls() {
        JStatement tailDecl = new JVariableDeclarationStatement(headDefn);
        JStatement firstDecl = new JVariableDeclarationStatement(firstExe);
        List<JStatement> retval = new LinkedList<JStatement>();
        retval.add(tailDecl);
        retval.add(firstDecl);
        retval.addAll(dmaCommands.decls());
        return retval;
    }   
}
