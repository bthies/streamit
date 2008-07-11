package at.dms.kjc.tilera;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import at.dms.kjc.spacetime.*;
import at.dms.kjc.CStdType;
import at.dms.kjc.CType;
import at.dms.kjc.JBlock;
import at.dms.kjc.JExpression;
import at.dms.kjc.JExpressionStatement;
import at.dms.kjc.JFormalParameter;
import at.dms.kjc.JLocalVariableExpression;
import at.dms.kjc.JMethodCallExpression;
import at.dms.kjc.JMethodDeclaration;
import at.dms.kjc.JStatement;
import at.dms.kjc.JVariableDefinition;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.Slice;
import at.dms.util.Utils;


public class OutputBuffer extends Buffer {
    
    /** map of all the output buffers from filter -> outputbuffer */
    protected static HashMap<FilterSliceNode, OutputBuffer> buffers;
    
    static {
        buffers = new HashMap<FilterSliceNode, OutputBuffer>();
    }

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
                assert slice.getTail().totalWeights() > 0;
                //create the new buffer, the constructor will put the buffer in the 
                //hashmap
                OutputBuffer buf = new OutputBuffer(slice.getFirstFilter());
                
                //calculate the rotation length
                int srcMult = schedule.getPrimePumpMult(slice);
                int maxRotLength = 0;
                for (Slice dest : slice.getTail().getDestSlices()) {
                    int diff = srcMult - schedule.getPrimePumpMult(dest);
                    assert diff >= 0;
                    if (diff > maxRotLength)
                        maxRotLength = diff;
                }
                buf.rotationLength = maxRotLength;
                //System.out.println("Setting output buf " + buf.getFilterNode() + " to " + buf.rotationLength);    
            }
        }
    }
    
    
    /**
     * Create a new output buffer that is associated with the filter node.
     * 
     * @param filterNode The filternode for which to create a new output buffer.
     */
    private OutputBuffer(FilterSliceNode filterNode) {
        super(filterNode.getEdgeToNext(), filterNode);
        buffers.put(filterNode, this);
    }
   
    /**
     * Return the output buffer associated with the filter node.
     * 
     * @param fsn The filter node in question.
     * @return The output buffer of the filter node.
     */
    public static OutputBuffer getOutputBuffer(FilterSliceNode fsn) {
        return buffers.get(fsn);
    }
    
    /**
     * Return the set of all the InputBuffers that are mapped to tile t.
     */
    public static Set<Buffer> getBuffersOnTile(Tile t) {
        HashSet<Buffer> set = new HashSet<Buffer>();
        
        for (Buffer b : buffers.values()) {
            if (TileraBackend.backEndBits.getLayout().getComputeNode(b.getFilterNode()).equals(t))
                set.add(b);
        }
        
        return set;
    }
    
    protected void setBufferSize() {
        
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#popMethodName()
     */
    public String popMethodName() {
        assert false : "Should not call pop() method on output buffer.";
        return "";
    }
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#popMethod()
     */
    public JMethodDeclaration popMethod() {
        assert false : "Should not call pop() method on output buffer.";
        return null;
    }
    
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#popManyMethodName()
     */
    public String popManyMethodName() {
        assert false : "Should not call pop() method on output buffer.";
        return "";
    }
 
        /**
     * Pop many items at once ignoring them.
     * Default method generated here to call popMethod() repeatedly.
     */
    public JMethodDeclaration popManyMethod() {
        assert false : "Should not call pop() method on output buffer.";
        return null;
     }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#assignFromPopMethodName()
     */
    public String assignFromPopMethodName() {
        assert false : "Should not call pop() method on output buffer.";
        return "";
    }
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#assignFromPopMethod()
     */
    public JMethodDeclaration assignFromPopMethod() {
        assert false : "Should not call pop() method on output buffer.";
        return null;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#peekMethodName()
     */
    public String peekMethodName() {
        assert false : "Should not call peek() method on output buffer.";
        return "";
    }
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#peekMethod()
     */
    public JMethodDeclaration peekMethod() {
        assert false : "Should not call peek() method on output buffer.";
        return null;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#assignFromPeekMethodName()
     */
    public String assignFromPeekMethodName() {
        assert false : "Should not call peek() method on output buffer.";
        return "";
    }
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#assignFromPeekMethod()
     */
    public JMethodDeclaration assignFromPeekMethod() {
        assert false : "Should not call peek() method on output buffer.";
        return null;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#pushMethodName()
     */
    public String pushMethodName() {
        return "__push_" + unique_id;
    }
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#pushMethod()
     */
    public JMethodDeclaration pushMethod() {
        return null;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#beginInitRead()
     */
    public List<JStatement> beginInitRead() {
        return new LinkedList<JStatement>(); 
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#beginInitRead()
     */
    public List<JStatement> postPreworkInitRead() {
        return new LinkedList<JStatement>(); 
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#endInitRead()
     */
    public List<JStatement> endInitRead() {
        return new LinkedList<JStatement>(); 
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#beginInitWrite()
     */
    public List<JStatement> beginInitWrite() {
        return new LinkedList<JStatement>(); 
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#endInitWrite()
     */
    public List<JStatement> endInitWrite() {
        return new LinkedList<JStatement>(); 
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#beginSteadyRead()
     */
    public List<JStatement> beginSteadyRead() {
        return new LinkedList<JStatement>(); 
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#endSteadyRead()
     */
    public List<JStatement> endSteadyRead() {
        return new LinkedList<JStatement>(); 
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#beginSteadyWrite()
     */
    public List<JStatement> beginSteadyWrite() {
        return new LinkedList<JStatement>(); 
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#endSteadyWrite()
     */
    public List<JStatement> endSteadyWrite() {
        return new LinkedList<JStatement>(); 
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#topOfWorkSteadyRead()
     */
    public List<JStatement> topOfWorkSteadyRead() {
        return new LinkedList<JStatement>(); 
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#topOfWorkSteadyWrite()
     */
    public List<JStatement> topOfWorkSteadyWrite() {
        return new LinkedList<JStatement>(); 
    }
 
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#dataDeclsH()
     */
    public List<JStatement> dataDeclsH() {
        return new LinkedList<JStatement>();
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#dataDecls()
     */
    public List<JStatement> dataDecls() {
        return new LinkedList<JStatement>();
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#readDeclsExtern()
     */
    public List<JStatement> readDeclsExtern() {
        return new LinkedList<JStatement>();
    }   
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#readDecls()
     */
    public List<JStatement> readDecls() {
        return new LinkedList<JStatement>();
    }   
    
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#writeDeclsExtern()
     */
    public List<JStatement> writeDeclsExtern() {
        return new LinkedList<JStatement>();
    }   
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#writeDecls()
     */
    public List<JStatement> writeDecls() {
        return new LinkedList<JStatement>();
    }   
    
}
