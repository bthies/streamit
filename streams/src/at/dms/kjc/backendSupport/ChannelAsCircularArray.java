package at.dms.kjc.backendSupport;

import java.util.*;
import at.dms.kjc.slicegraph.*;
import at.dms.kjc.*;
import at.dms.kjc.common.*;

/**
 * Implement a channel as a circular array.
 * The size of the array is the power of 2 >= the maximum number
 * of elements stored in the array.  
 * @author dimock
 *
 */
public class ChannelAsCircularArray extends ChannelAsArray {

    /** buffer size is alway power of 2, nmask id power of 2 - 1 */
    protected int offsetMask;  // 
    
    /**
     * Make a new Channel or return an already-made channel.
     * @param edge     The edge that this channel implements.
     * @param other    The channel that this delegates to.
     * @return A channel for this edge, that 
     */
    public static ChannelAsCircularArray getChannel(Edge edge) {
        Channel oldChan = Channel.bufferStore.get(edge);
        if (oldChan == null) {
            ChannelAsCircularArray chan = new ChannelAsCircularArray(edge);
            Channel.bufferStore.put(edge, chan);
            return chan;
       } else {
            assert oldChan instanceof ChannelAsArray; 
            return (ChannelAsCircularArray)oldChan;
        }
    }

    /** Constructor 
     * @param edge should give enough information (indirectly) to calculate buffer size
     */
    public ChannelAsCircularArray(Edge edge) {
        super(edge);
        // fix up buffer size to be next power of 2
        bufSize = CommonUtils.nextPow2(bufSize);
        bufDefn = CommonUtils.makeArrayVariableDefn(bufSize,edge.getType(),bufName);
        // create mask for anding sith offset to make modulo power of 2.
        offsetMask = bufSize - 1;
    }
    
    /** input_type pop(). */
    public JMethodDeclaration popMethod() {
        ALocalVariable tmp = ALocalVariable.makeTmp(theEdge.getType());
        JBlock body = new JBlock();
        JMethodDeclaration retval = new JMethodDeclaration(
                null,
                /*at.dms.kjc.Constants.ACC_PUBLIC | at.dms.kjc.Constants.ACC_STATIC |*/ at.dms.kjc.Constants.ACC_INLINE,
                theEdge.getType(),
                popMethodName(),
                new JFormalParameter[0],
                CClassType.EMPTY,
                body, null, null);
        body.addStatement(tmp.getDecl());
        body.addStatement(new JExpressionStatement(
                new JAssignmentExpression(
                        tmp.getRef(),
                        bufRef(new JPostfixExpression(at.dms.kjc.Constants.OPE_POSTINC, 
                                tail)))));
        body.addStatement(new JExpressionStatement(
                new JCompoundAssignmentExpression(null,
                        at.dms.kjc.Constants.OPE_BAND,
                        tail, new JIntLiteral(offsetMask))));
        body.addStatement(
                new JReturnStatement(null,tmp.getRef(),null));
        return retval;
    }
   
    /** void pop(int N). */
    public JMethodDeclaration popManyMethod() {
        String parameterName = "__n";
        JFormalParameter n = new JFormalParameter(
                CStdType.Integer,
                parameterName);
        JLocalVariableExpression nRef = new JLocalVariableExpression(n);
        JBlock body = new JBlock();
        JMethodDeclaration retval = new JMethodDeclaration(
                null,
                /*at.dms.kjc.Constants.ACC_PUBLIC | at.dms.kjc.Constants.ACC_STATIC |*/ at.dms.kjc.Constants.ACC_INLINE,
                CStdType.Void,
                popManyMethodName(),
                new JFormalParameter[]{n},
                CClassType.EMPTY,
                body, null, null);
        body.addStatement(
        new JExpressionStatement(new JCompoundAssignmentExpression(null,
                at.dms.kjc.Constants.OPE_PLUS,
                tail, nRef)));
        body.addStatement(new JExpressionStatement(
                new JCompoundAssignmentExpression(null,
                        at.dms.kjc.Constants.OPE_BAND,
                        tail, new JIntLiteral(offsetMask))));
        return retval;
    }
    
    
    /** void pop(input_type val)  generally assign if val is not an array, else memcpy */
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
    
    /** input_type peek(int offset) */
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
                bufRef(new JBitwiseExpression(at.dms.kjc.Constants.OPE_BAND,
                        new JAddExpression(tail, offsetRef),
                        new JIntLiteral(offsetMask))),
                null));
        return retval;
    }

    /** void peek(input_type val, int offset)  generally assign if val is not an array, else memcpy */
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

   /** void push(output_type val) */
    public JMethodDeclaration pushMethod() {
        String valName = "__val";
        JFormalParameter val = new JFormalParameter(
                theEdge.getType(),
                valName);
        JLocalVariableExpression valRef = new JLocalVariableExpression(val);
        JBlock body = new JBlock();
        JMethodDeclaration retval = new JMethodDeclaration(
                null,
                /*at.dms.kjc.Constants.ACC_PUBLIC | at.dms.kjc.Constants.ACC_STATIC |*/ at.dms.kjc.Constants.ACC_INLINE,
                CStdType.Void,
                pushMethodName(),
                new JFormalParameter[]{val},
                CClassType.EMPTY,
                body, null, null);
        body.addStatement(
        new JExpressionStatement(new JAssignmentExpression(
                bufRef(new JPostfixExpression(at.dms.kjc.Constants.OPE_POSTINC,
                        head)),
                valRef)));
        body.addStatement(new JExpressionStatement(
                new JCompoundAssignmentExpression(null,
                        at.dms.kjc.Constants.OPE_BAND,
                        head, new JIntLiteral(offsetMask))));
        return retval;
     }
    
    /* beginInitRead zeros out head as in superclass*/
    /* beginInitWrite zeros out tail as in superclass */

    /** Statements for beginning of steady state iteration on read (downstream) end of buffer:
     * overrides superclass to have no statements here.  
     */
    @Override
    public List<JStatement> beginSteadyRead() {
        List<JStatement> retval = new LinkedList<JStatement>();
        return retval; 
    }
 
    /** Statements for beginning of steady state iteration on write (upstream) end of buffer 
     * overrides superclass to have no statements here. 
     */
    public List<JStatement> beginSteadyWrite() {
        List<JStatement> retval = new LinkedList<JStatement>();
        return retval; 
    }
    
}
