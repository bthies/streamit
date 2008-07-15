package at.dms.kjc.tilera;

import at.dms.kjc.CClassType;
import at.dms.kjc.CStdType;
import at.dms.kjc.CType;
import at.dms.kjc.JAddExpression;
import at.dms.kjc.JAssignmentExpression;
import at.dms.kjc.JBlock;
import at.dms.kjc.JEmittedTextExpression;
import at.dms.kjc.JExpression;
import at.dms.kjc.JExpressionStatement;
import at.dms.kjc.JFieldAccessExpression;
import at.dms.kjc.JFormalParameter;
import at.dms.kjc.JIntLiteral;
import at.dms.kjc.JLocalVariableExpression;
import at.dms.kjc.JMethodCallExpression;
import at.dms.kjc.JMethodDeclaration;
import at.dms.kjc.JPostfixExpression;
import at.dms.kjc.JReturnStatement;
import at.dms.kjc.JStatement;
import at.dms.kjc.JVariableDeclarationStatement;
import at.dms.kjc.JVariableDefinition;
import at.dms.kjc.slicegraph.*;
import at.dms.util.Utils;
import at.dms.kjc.spacetime.*;
import at.dms.kjc.backendSupport.*;
import at.dms.kjc.common.CommonUtils;

import java.util.*;

/**
 * 
 * 
 * @author mgordon
 *
 */
public class InputBuffer extends Buffer {

    /** map of all the input buffers from filter -> inputbuffer */
    protected static HashMap<FilterSliceNode, InputBuffer> buffers;
    /** name of variable containing tail of array offset */
    protected String tailName;
     /** definition for tail */
    protected JVariableDefinition tailDefn;
       /** reference to tail */
    protected JExpression tail;
    
    static {
        buffers = new HashMap<FilterSliceNode, InputBuffer>();
    }

    /**
     * Create all the input buffers necessary for this slice graph.  Iterate over
     * the steady-state schedule, visiting each slice and creating an input buffer
     * for the filter of the slice.  Also set the rotation lengths based on the 
     * prime pump schedule.
     * 
     * @param schedule The spacetime schedule of the slices 
     */
    public static void createInputBuffers(BasicSpaceTimeSchedule schedule) {
        for (Slice slice : schedule.getScheduleList()) {
            assert slice.getNumFilters() == 1;
            if (!slice.getHead().noInputs()) {
                assert slice.getHead().totalWeights() > 0;
                //create the new buffer, the constructor will put the buffer in the 
                //hashmap
                InputBuffer buf = new InputBuffer(slice.getFirstFilter());
                
                //now set the rotation length
                int destMult = schedule.getPrimePumpMult(slice);
                //first find the max rotation length given the prime pump 
                //mults of all the sources
                int maxRotationLength = 0;
                
                for (Slice src : slice.getHead().getSourceSlices()) {
                    int diff = schedule.getPrimePumpMult(src) - destMult; 
                    assert diff >= 0;
                    if (diff > maxRotationLength) {
                        maxRotationLength = diff;
                    }
                }
                buf.rotationLength = maxRotationLength;
                //System.out.println("Setting input buf " + buf.getFilterNode() + " to " + buf.rotationLength);
            }
        }
    }

    /**
     * Set the buffer size of this input buffer based on the max
     * number of items it receives.
     */
    protected void setBufferSize() {
        FilterInfo fi = FilterInfo.getFilterInfo(filterNode);
        
        bufSize = Math.max(fi.totalItemsReceived(SchedulingPhase.INIT),
                (fi.totalItemsReceived(SchedulingPhase.STEADY) + fi.remaining));
    }
        
    /**
     * Create a new input buffer that is associated with the filter node.
     * 
     * @param filterNode The filternode for which to create a new input buffer.
     */
    private InputBuffer(FilterSliceNode filterNode) {
        super(filterNode.getEdgeToPrev(), filterNode);
        buffers.put(filterNode, this);
        
        tailName = this.getIdent() + "tail";
        tailDefn = new JVariableDefinition(null,
                at.dms.kjc.Constants.ACC_STATIC,
                CStdType.Integer, tailName, null);
        tail = new JFieldAccessExpression(tailName);
        tail.setType(CStdType.Integer);
    }
    
    /**
     * Return the input buffer associated with the filter node.
     * 
     * @param fsn The filter node in question.
     * @return The input buffer of the filter node.
     */
    public static InputBuffer getInputBuffer(FilterSliceNode fsn) {
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
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#popMethodName()
     */
    public String popMethodName() {
        return "__pop_" + unique_id;
    }
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#popMethod()
     */
    public JMethodDeclaration popMethod() {
        JBlock body = new JBlock();
        JMethodDeclaration retval = new JMethodDeclaration(
                null,
                /*at.dms.kjc.Constants.ACC_PUBLIC | at.dms.kjc.Constants.ACC_STATIC |*/ at.dms.kjc.Constants.ACC_INLINE,
                theEdge.getType(),
                popMethodName(),
                new JFormalParameter[0],
                CClassType.EMPTY,
                body, null, null);
        body.addStatement(
        new JReturnStatement(null,
                bufRef(new JPostfixExpression(at.dms.kjc.Constants.OPE_POSTINC, tail)),null));
        return retval;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#popManyMethodName()
     */
    public String popManyMethodName() {
        return "__popN_" + unique_id;
    }
 
    JMethodDeclaration popManyCode = null;
    
    /**
     * Pop many items at once ignoring them.
     * Default method generated here to call popMethod() repeatedly.
     */
    public JMethodDeclaration popManyMethod() {
        if (popManyCode != null) {
            return popManyCode;
        }
        if (popMethod() == null) {
            return null;
        }
        
        String formalParamName = "n";
        CType formalParamType = CStdType.Integer;
        
        JVariableDefinition nPopsDef = new JVariableDefinition(formalParamType, formalParamName);
        JExpression nPops = new JLocalVariableExpression(nPopsDef);
        
        JVariableDefinition loopIndex = new JVariableDefinition(formalParamType, "i");
        
        JStatement popOne = new JExpressionStatement(
                new JMethodCallExpression(popMethodName(),new JExpression[0]));
        
        JBlock body = new JBlock();
        body.addStatement(Utils.makeForLoop(popOne, nPops, loopIndex));
        
        popManyCode = new JMethodDeclaration(CStdType.Void,
                popManyMethodName(),
                new JFormalParameter[]{new JFormalParameter(formalParamType, formalParamName)},
                body);
        return popManyCode;
     }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#assignFromPopMethodName()
     */
    public String assignFromPopMethodName() {
        return "__popv_" + unique_id;
    }
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#assignFromPopMethod()
     */
    public JMethodDeclaration assignFromPopMethod() {
        String parameterName = "__val";
        JFormalParameter val = new JFormalParameter(
                CStdType.Integer,
                parameterName);
        JLocalVariableExpression valRef = new JLocalVariableExpression(val);
        JBlock body = new JBlock();
        JMethodDeclaration retval = new JMethodDeclaration(
                null,
                /*at.dms.kjc.Constants.ACC_PUBLIC | at.dms.kjc.Constants.ACC_STATIC |*/ at.dms.kjc.Constants.ACC_INLINE,
                CStdType.Void,
                assignFromPopMethodName(),
                new JFormalParameter[]{val},
                CClassType.EMPTY,
                body, null, null);
        body.addStatement(
                new JExpressionStatement(
                        new JEmittedTextExpression(
                                "/* assignFromPopMethod not yet implemented */")));
        return retval;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#peekMethodName()
     */
    public String peekMethodName() {
        return "__peek_" + unique_id;
    }
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#peekMethod()
     */
    public JMethodDeclaration peekMethod() {
        String parameterName = "__offset";
        JFormalParameter offset = new JFormalParameter(
                CStdType.Integer,
                parameterName);
        JLocalVariableExpression offsetRef = new JLocalVariableExpression(offset);
        JBlock body = new JBlock();
        JMethodDeclaration retval = new JMethodDeclaration(
                null,
                /*at.dms.kjc.Constants.ACC_PUBLIC | at.dms.kjc.Constants.ACC_STATIC |*/ at.dms.kjc.Constants.ACC_INLINE,
                theEdge.getType(),
                peekMethodName(),
                new JFormalParameter[]{offset},
                CClassType.EMPTY,
                body, null, null);
        body.addStatement(
                new JReturnStatement(null,
                        bufRef(new JAddExpression(tail, offsetRef)),null));
        return retval;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#assignFromPeekMethodName()
     */
    public String assignFromPeekMethodName() {
        return "__peekv_" + unique_id;
    }
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#assignFromPeekMethod()
     */
    public JMethodDeclaration assignFromPeekMethod() {
        String valName = "__val";
        JFormalParameter val = new JFormalParameter(
                CStdType.Integer,
                valName);
        JLocalVariableExpression valRef = new JLocalVariableExpression(val);
        String offsetName = "__offset";
        JFormalParameter offset = new JFormalParameter(
                CStdType.Integer,
                offsetName);
        JLocalVariableExpression offsetRef = new JLocalVariableExpression(offset);
        JBlock body = new JBlock();
        JMethodDeclaration retval = new JMethodDeclaration(
                null,
                /*at.dms.kjc.Constants.ACC_PUBLIC | at.dms.kjc.Constants.ACC_STATIC |*/ at.dms.kjc.Constants.ACC_INLINE,
                CStdType.Void,
                assignFromPeekMethodName(),
                new JFormalParameter[]{val,offset},
                CClassType.EMPTY,
                body, null, null);
         body.addStatement(
                new JExpressionStatement(
                        new JEmittedTextExpression(
                                "/* assignFromPeekMethod not yet implemented */")));
        return retval;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#pushMethodName()
     */
    public String pushMethodName() {
        assert false : "Shouldn't call push() method on an input buffer";
        return "";
    }
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#pushMethod()
     */
    public JMethodDeclaration pushMethod() {
        assert false : "Shouldn't call push() method on an input buffer";
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
        //declare the buffer array
        JStatement arrayDecl = new JVariableDeclarationStatement(bufDefn); 
        List<JStatement> retval = new LinkedList<JStatement>();
        retval.add(arrayDecl);
        return retval;
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
        //declare the tail
        JStatement headDecl = new JVariableDeclarationStatement(tailDefn);
        List<JStatement> retval = new LinkedList<JStatement>();
        retval.add(headDecl);
        return retval;
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
   
    
    /** Create statement zeroing out tail */
    protected JStatement zeroOutTail() {
        return new JExpressionStatement(
                new JAssignmentExpression(tail, new JIntLiteral(0)));
    }
    
}
