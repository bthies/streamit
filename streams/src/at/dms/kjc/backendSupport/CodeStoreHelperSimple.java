package at.dms.kjc.backendSupport;

import java.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.backendSupport.*;
import at.dms.kjc.slicegraph.*;
import at.dms.util.Utils;

/**
 * CodeStore helper routines for FilterSliceNode that does not need a peek buffer. 
 * @author dimock
 */
public class CodeStoreHelperSimple extends CodeStoreHelper {

    private static String exeIndex1Name = "__EXEINDEX__1__";
    private JVariableDefinition exeIndex1;
    private boolean exeIndex1Used;
    private JVariableDefinition useExeIndex1() {
        if (exeIndex1Used) return exeIndex1;
        else {
            exeIndex1 = new JVariableDefinition(null, 
                    0, 
                    CStdType.Integer,
                    exeIndex1Name + uniqueID,
                    null);
            exeIndex1Used = true;
            this.addField(new JFieldDeclaration(exeIndex1));
            return exeIndex1;
        }
    }
    
    /**
     * Constructor
     * @param node          A filter slice node to wrap code for.
     * @param backEndBits   The back end factory as a source of data and back end specific functions.
     */
    public CodeStoreHelperSimple(FilterSliceNode node, BackEndFactory backEndBits) {
        super(node,node.getAsFilter().getFilter(),backEndBits);
    }

    /**
     * Calculate and return the method that implements the init stage 
     * computation for this filter.  It should be called only once in the 
     * generated code.
     * <p>
     * This does not include the call to the init function of the filter.
     * That is done in {@link RawComputeCodeStore#addInitFunctionCall}. 
     * 
     * @return The method that implements the init stage for this filter.
     */
    @Override
    public JMethodDeclaration getInitStageMethod() {
        JBlock statements = new JBlock();
        assert sliceNode instanceof FilterSliceNode;
        FilterContent filter = ((FilterSliceNode) sliceNode).getFilter();

        // channel code before work block
        if (backEndBits.sliceHasUpstreamChannel(sliceNode.getParent())) {
            for (JStatement stmt : backEndBits.getChannel(
                    sliceNode.getPrevious().getEdgeToNext()).beginInitRead()) {
                statements.addStatement(stmt);
            }
        }
        if (backEndBits.sliceHasDownstreamChannel(sliceNode.getParent())) {
            for (JStatement stmt : backEndBits.getChannel(
                    sliceNode.getEdgeToNext()).beginInitWrite()) {
                statements.addStatement(stmt);
            }
        }
        // add the calls for the work function in the initialization stage
        if (FilterInfo.getFilterInfo((FilterSliceNode) sliceNode).isTwoStage()) {

            JMethodCallExpression initWorkCall = new JMethodCallExpression(
                    null, new JThisExpression(null), filter.getInitWork()
                            .getName(), new JExpression[0]);

            statements.addStatement(new JExpressionStatement(initWorkCall));

            if (backEndBits.sliceHasUpstreamChannel(sliceNode.getParent())) {
                for (JStatement stmt : backEndBits.getChannel(
                        sliceNode.getPrevious().getEdgeToNext())
                        .postPreworkInitRead()) {
                    statements.addStatement(stmt);
                }
            }
        }

        statements.addStatement(generateInitWorkLoop(filter));

        // channel code after work block
        if (backEndBits.sliceHasUpstreamChannel(sliceNode.getParent())) {
            for (JStatement stmt : backEndBits.getChannel(
                    sliceNode.getPrevious().getEdgeToNext()).endInitRead()) {
                statements.addStatement(stmt);
            }
        }
        if (backEndBits.sliceHasDownstreamChannel(sliceNode.getParent())) {
            for (JStatement stmt : backEndBits.getChannel(
                    sliceNode.getEdgeToNext()).endInitWrite()) {
                statements.addStatement(stmt);
            }
        }

        return new JMethodDeclaration(null, at.dms.kjc.Constants.ACC_PUBLIC,
                CStdType.Void, initStage + uniqueID, JFormalParameter.EMPTY,
                CClassType.EMPTY, statements, null, null);
    }

    /**
     * Generate the loop for the work function firings in the initialization
     * schedule. This does not include receiving the necessary items for the
     * first firing. This is handled in
     * {@link DirectCommunication#getInitStageMethod}. This block will generate
     * code to receive items for all subsequent calls of the work function in
     * the init stage plus the class themselves.
     * 
     * @param filter
     *            The filter
     * @param generatedVariables
     *            The vars to use.
     * 
     * @return The code to fire the work function in the init stage.
     */
    private JStatement generateInitWorkLoop(FilterContent filter)
    {
        FilterInfo filterInfo = FilterInfo.getFilterInfo((FilterSliceNode)sliceNode);
        JBlock block = new JBlock();

        //clone the work function and inline it
        JStatement workBlock = getWorkFunctionCall();
    
        //if we are in debug mode, print out that the filter is firing
//        if (SpaceTimeBackend.FILTER_DEBUG_MODE) {
//            block.addStatement
//                (new SIRPrintStatement(null,
//                                       new JStringLiteral(null, filter.getName() + " firing (init)."),
//                                       null));
//        }

        if (workBlock == null) {
            workBlock = new SIRBeginMarker("Empty Work Block!!");
        }
        block.addStatement(workBlock);
    
        //return the for loop that executes the block init - 1
        //times
        return Utils.makeForLoopFieldIndex(block, useExeIndex1(), 
                           new JIntLiteral(filterInfo.initMult));
    }

    @Override
    public JMethodDeclaration getPrimePumpMethod() {
        return super.getPrimePumpMethodForFilter(FilterInfo.getFilterInfo((FilterSliceNode)sliceNode));
    }

    @Override
    public JBlock getSteadyBlock() {
        JBlock statements = new JBlock();
        // channel code before work block
        if (backEndBits.sliceHasUpstreamChannel(sliceNode.getParent())) {
            for (JStatement stmt : backEndBits.getChannel(
                    sliceNode.getPrevious().getEdgeToNext()).beginSteadyRead()) {
                statements.addStatement(stmt);
            }
        }
        if (backEndBits.sliceHasDownstreamChannel(sliceNode.getParent())) {
            for (JStatement stmt : backEndBits.getChannel(
                    sliceNode.getEdgeToNext()).beginSteadyWrite()) {
                statements.addStatement(stmt);
            }
        }
        // iterate work function as needed
        statements.addStatement(getWorkFunctionBlock(FilterInfo
                .getFilterInfo((FilterSliceNode) sliceNode).steadyMult));
        // channel code after work block
        if (backEndBits.sliceHasUpstreamChannel(sliceNode.getParent())) {
            for (JStatement stmt : backEndBits.getChannel(
                    sliceNode.getPrevious().getEdgeToNext()).endSteadyRead()) {
                statements.addStatement(stmt);
            }
        }
        if (backEndBits.sliceHasDownstreamChannel(sliceNode.getParent())) {
            for (JStatement stmt : backEndBits.getChannel(
                    sliceNode.getEdgeToNext()).endSteadyWrite()) {
                statements.addStatement(stmt);
            }
        }
        return statements;
    }
}
