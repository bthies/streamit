package at.dms.kjc.tilera;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import at.dms.kjc.JAssignmentExpression;
import at.dms.kjc.JBlock;
import at.dms.kjc.JBooleanLiteral;
import at.dms.kjc.JEmittedTextExpression;
import at.dms.kjc.JExpressionStatement;
import at.dms.kjc.JIfStatement;
import at.dms.kjc.JLogicalComplementExpression;
import at.dms.kjc.JStatement;
import at.dms.kjc.JVariableDeclarationStatement;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.InterSliceEdge;
import at.dms.kjc.slicegraph.SchedulingPhase;
import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.spacetime.BasicSpaceTimeSchedule;

/**
 * 
 * 
 * @author mgordon
 *
 */
public class OutputRotatingBufferRemoteWrites extends OutputRotatingBuffer {
    
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
                OutputRotatingBufferRemoteWrites buf = new OutputRotatingBufferRemoteWrites(slice.getFirstFilter(), parent);
                
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
    protected OutputRotatingBufferRemoteWrites(FilterSliceNode filterNode, Tile parent) {
        super(filterNode, parent);
        
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


    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#endInitWrite()
     */
    public List<JStatement> endInitWrite() {
        LinkedList<JStatement> list = new LinkedList<JStatement>();
                
        return list;
    }
    
    /**
     *  We don't want to transfer during the first execution of the primepump
     *  so guard the execution in an if statement.
     */
    public List<JStatement> beginPrimePumpWrite() {
        LinkedList<JStatement> list = new LinkedList<JStatement>();
                
        list.add(zeroOutHead());
        return list;
    }
    
    public List<JStatement> endPrimePumpWrite() {
        LinkedList<JStatement> list = new LinkedList<JStatement>();
       
        return list;
    }
    
    
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#beginSteadyWrite()
     */
    public List<JStatement> beginSteadyWrite() {
        LinkedList<JStatement> list = new LinkedList<JStatement>();
        list.add(zeroOutHead());
        return list;
    }
    
    /**
     * The rotate statements that includes the current buffer (for output of this 
     * firing) and transfer buffer.
     */
    protected List<JStatement> rotateStatements() {
        LinkedList<JStatement> list = new LinkedList<JStatement>();
        return list;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#endSteadyWrite()
     */
    public List<JStatement> endSteadyWrite() {
        LinkedList<JStatement> list = new LinkedList<JStatement>();
      
        return list;
    }
    
 
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#writeDecls()
     */
    public List<JStatement> writeDecls() {
        JStatement tailDecl = new JVariableDeclarationStatement(headDefn);
        List<JStatement> retval = new LinkedList<JStatement>();
        retval.add(tailDecl);
        return retval;
    }
}