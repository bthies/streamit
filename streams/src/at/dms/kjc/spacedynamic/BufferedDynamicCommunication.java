/**
 * 
 */
package at.dms.kjc.spacedynamic;

import java.util.List;
import java.util.ListIterator;

import at.dms.kjc.*;
import at.dms.kjc.CClassType;
import at.dms.kjc.CStdType;
import at.dms.kjc.CType;
import at.dms.kjc.JAddExpression;
import at.dms.kjc.JArrayAccessExpression;
import at.dms.kjc.JBlock;
import at.dms.kjc.JExpression;
import at.dms.kjc.JExpressionStatement;
import at.dms.kjc.JFormalParameter;
import at.dms.kjc.JIntLiteral;
import at.dms.kjc.JLocalVariableExpression;
import at.dms.kjc.JMethodCallExpression;
import at.dms.kjc.JMethodDeclaration;
import at.dms.kjc.JParenthesedExpression;
import at.dms.kjc.JPrefixExpression;
import at.dms.kjc.JStatement;
import at.dms.kjc.JStringLiteral;
import at.dms.kjc.JThisExpression;
import at.dms.kjc.JVariableDefinition;
import at.dms.kjc.ObjectDeepCloner;
import at.dms.kjc.SLIRReplacingVisitor;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.spacedynamic.BufferedStaticCommunication.ConvertCommunication;
import at.dms.kjc.spacedynamic.BufferedStaticCommunication.ConvertCommunicationSimple;
import at.dms.kjc.spacedynamic.BufferedStaticCommunication.LocalVariables;
import at.dms.kjc.sir.*;

/**
 * @author mgordon
 * 
 */
public class BufferedDynamicCommunication {
    /** the layout object for the flatnode * */
    private Layout layout;

    /** the ssg of this flat node * */
    private SpdStaticStreamGraph ssg;

    /** the flat node we are generating code for * */
    private FlatNode node;

    /** the size of the dynamic peek buffer */
    private static final int DYN_PEEK_BUFFER_SIZE = 10000;

    /***************************************************************************
     * class to hold the local variables we create so we can pass these across
     * functions
     **************************************************************************/
    class LocalVariables {
        /** the peek buffer */
        JVariableDefinition recvBuffer;

        /** the total size of the peek buffer */
        JVariableDefinition recvBufferSize;

        /** the current size, the number of items of the peek buffer */
        JVariableDefinition recvCurBufferSize;

        /** the size in bits of the peek buffer */
        JVariableDefinition recvBufferBits;

        /** The start of the buffer, the index of the oldest item */
        JVariableDefinition oldestItemIndex;

        /** the end of the buffer, the index of the first free index * */
        JVariableDefinition newItemIndex;

        JVariableDefinition[] ARRAY_INDEX;

        JVariableDefinition[] ARRAY_COPY;
    }

    public BufferedDynamicCommunication(SpdStaticStreamGraph ssg, FlatNode node) {
        this.ssg = ssg;
        this.node = node;
    }

    public void doit() {
        SIRFilter filter = node.getFilter();

        // otherwise generate code the old way
        LocalVariables localVariables = new LocalVariables();

        JBlock block = new JBlock(null, new JStatement[0], null);

        createLocalVariables(block, localVariables);
        convertCommExpsDynamic(localVariables);

        rawMainFunction(block, localVariables);

        // create the method and add it to the filter
        JMethodDeclaration rawMainFunct = new JMethodDeclaration(null,
                                                                 at.dms.kjc.Constants.ACC_PUBLIC, CStdType.Void,
                                                                 RawExecutionCode.rawMain, JFormalParameter.EMPTY,
                                                                 CClassType.EMPTY, block, null, null);
        filter.addMethod(rawMainFunct);
    }

    public void createLocalVariables(JBlock block, LocalVariables localVariables) {

    }

    public void rawMainFunction(JBlock block, LocalVariables localVariables) {
        SIRFilter filter = node.getFilter();
        
        // create the params list, for some reason
        // calling toArray() on the list breaks a later pass
        List paramList = filter.getParams();
        JExpression[] paramArray;
        if (paramList == null || paramList.size() == 0)
            paramArray = new JExpression[0];
        else
            paramArray = (JExpression[]) paramList.toArray(new JExpression[0]);
        
        // add the call to the init function
        block.addStatement(new JExpressionStatement(null,
                                                    new JMethodCallExpression(null, new JThisExpression(null),
                                                                              filter.getInit().getName(), paramArray), null));

        // add the call to initWork
        if (filter instanceof SIRTwoStageFilter) {
            SIRTwoStageFilter two = (SIRTwoStageFilter) filter;
            JBlock body = (JBlock) ObjectDeepCloner.deepCopy(two.getInitWork()
                                                             .getBody());
            // now inline the init work body
            block.addStatement(body);
        }
        
        JBlock workBlock = RawExecutionCode.executeWorkFunction(filter);
        // if we are in debug mode, print out that the filter is firing
        if (SpaceDynamicBackend.FILTER_DEBUG_MODE) {
            block.addStatement(new SIRPrintStatement(null, new JStringLiteral(
                                                                              null, filter.getName() + " firing.\\n"), null));
        }

        // add the cloned work function to the block
        block.addStatement(new JWhileStatement(null, 
                                               new JBooleanLiteral(null, true),
                                               workBlock,
                                               null));
    }

    /***************************************************************************
     * convert the peek and pop expressions for a filter into buffer accesses,
     * do this for all functions just in case helper functions call peek or pop
     **************************************************************************/
    private void convertCommExpsDynamic(LocalVariables localVars) {
        SLIRReplacingVisitor convert = new ConvertCommunicationDynamic(
                                                                       localVars);

        JMethodDeclaration[] methods = node.getFilter().getMethods();
        for (int i = 0; i < methods.length; i++) {
            // iterate over the statements and call the ConvertCommunication
            // class to convert peek, pop
            for (ListIterator it = methods[i].getStatementIterator(); it
                     .hasNext();) {
                ((JStatement) it.next()).accept(convert);
            }
        }
    }

    /**
     * This class converts pop and peek expression into accesses of the dynamic
     * buffer that is generated for the filter in the enclosing class. It uses
     * the local variables defined in the enclosing class.
     * 
     * @author mgordon
     * 
     */
    class ConvertCommunicationDynamic extends SLIRReplacingVisitor {
        LocalVariables localVariables;

        public ConvertCommunicationDynamic(LocalVariables locals) {
            localVariables = locals;
        }

        /**
         * for var = pop() expressions convert to the form
         * (recvBuffer[++recvBufferIndex % recvBufferSize]) ??? if (size > 0) {
         * var = buf[oldest]; size--; oldest = (oldest + 1) % bufferSize; } else {
         * var = dynamic_receive(); }
         */
        public Object visitPopExpression(SIRPopExpression oldSelf,
                                         CType oldTapeType) {
            // do the super
            SIRPopExpression self = (SIRPopExpression) super
                .visitPopExpression(oldSelf, oldTapeType);

            JMethodCallExpression receive = new JMethodCallExpression(null,
                                                                      new JThisExpression(null), RawExecutionCode.popDynamic,
                                                                      new JExpression[0]);
            receive.setTapeType(self.getType());
            return receive;

        }

        // convert peek exps into:
        // (recvBuffer[(recvBufferIndex + (arg) + 1) mod recvBufferSize]) ??
        // WHAT IS IT NOW????
        public Object visitPeekExpression(SIRPeekExpression oldSelf,
                                          CType oldTapeType, JExpression oldArg) {
            // do the super
            SIRPeekExpression self = (SIRPeekExpression) super
                .visitPeekExpression(oldSelf, oldTapeType, oldArg);
            
            JExpression[] arg = new JExpression[1];
            //visit the argument
            arg[0] = (JExpression)oldArg.accept(this);
            
            JMethodCallExpression receive = new JMethodCallExpression(null,
                                                                      new JThisExpression(null), RawExecutionCode.peekDynamic,
                                                                      arg);
            receive.setTapeType(self.getType());
            return receive;
        }
    }
}
