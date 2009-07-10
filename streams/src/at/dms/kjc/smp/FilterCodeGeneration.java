package at.dms.kjc.smp;

import at.dms.compiler.JavaStyleComment;
import at.dms.kjc.CClassType;
import at.dms.kjc.CStdType;
import at.dms.kjc.JAddExpression;
import at.dms.kjc.JAssignmentExpression;
import at.dms.kjc.JBlock;
import at.dms.kjc.JBooleanLiteral;
import at.dms.kjc.JEqualityExpression;
import at.dms.kjc.JExpression;
import at.dms.kjc.JExpressionStatement;
import at.dms.kjc.JFieldAccessExpression;
import at.dms.kjc.JFieldDeclaration;
import at.dms.kjc.JFormalParameter;
import at.dms.kjc.JIfStatement;
import at.dms.kjc.JIntLiteral;
import at.dms.kjc.JLocalVariableExpression;
import at.dms.kjc.JMethodCallExpression;
import at.dms.kjc.JMethodDeclaration;
import at.dms.kjc.JMinusExpression;
import at.dms.kjc.JNameExpression;
import at.dms.kjc.JStatement;
import at.dms.kjc.JThisExpression;
import at.dms.kjc.JVariableDefinition;
import at.dms.kjc.JVariableDeclarationStatement;
import at.dms.kjc.backendSupport.CodeStoreHelper;
import at.dms.kjc.backendSupport.FilterInfo;
import at.dms.kjc.sir.SIRBeginMarker;
import at.dms.kjc.slicegraph.*;
import at.dms.kjc.slicegraph.fission.*;
import at.dms.kjc.backendSupport.*;
import at.dms.util.Utils;
import at.dms.kjc.KjcOptions;

public class FilterCodeGeneration extends CodeStoreHelper {
 
    private FilterSliceNode filterNode;
    private FilterInfo filterInfo;
    private static String exeIndex1Name = "__EXEINDEX__1__";
    private JVariableDefinition exeIndex1;
    private boolean exeIndex1Used;
    /** this variable massages init mult to assume that every filter is a two stage */
    private int initMult;
    
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
    public FilterCodeGeneration(FilterSliceNode node, SMPBackEndFactory backEndBits) {
        super(node,node.getAsFilter().getFilter(),backEndBits);
        filterNode = node;
        filterInfo = FilterInfo.getFilterInfo(filterNode);
        //assume that every filter is a two-stage and prework is called
        initMult = filterInfo.initMult;
        if (!filterInfo.isTwoStage()) {
            initMult++;
        }
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
        //slice has input, so we 
        if (backEndBits.sliceHasUpstreamChannel(sliceNode.getParent())) {
            for (JStatement stmt : InputRotatingBuffer.getInputBuffer(filterNode).beginInitRead()) {
                statements.addStatement(stmt);
            }
        }
        if (backEndBits.sliceHasDownstreamChannel(sliceNode.getParent())) {
            for (JStatement stmt : OutputRotatingBuffer.getOutputBuffer(filterNode).beginInitWrite()) {
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
                for (JStatement stmt : InputRotatingBuffer.getInputBuffer(filterNode).postPreworkInitRead()) {
                    statements.addStatement(stmt);
                }
            }
        }

        statements.addStatement(generateInitWorkLoop(filter));

        //determine if in the init there is a file writer slice downstream 
        //of the slice that contains this filter
        boolean dsFileWriter = false;
        for (InterSliceEdge edge : filterNode.getParent().getTail().getDestSet(SchedulingPhase.INIT)) {
            if (edge.getDest().getParent().getFirstFilter().isFileOutput()) {
                dsFileWriter = true;
                break;
            }
        }
        if (KjcOptions.numbers < 1 && dsFileWriter && filterInfo.totalItemsSent(SchedulingPhase.INIT) > 0) {
            assert filterNode.getParent().getTail().getDestSet(SchedulingPhase.INIT).size() == 1;
            FilterSliceNode fileW = 
                filterNode.getParent().getTail().getDestList(SchedulingPhase.INIT)[0].getDest().getParent().getFirstFilter();
            
            InputRotatingBuffer buf = InputRotatingBuffer.getInputBuffer(fileW);
            int outputs = filterInfo.totalItemsSent(SchedulingPhase.INIT);
            String type = ((FileOutputContent)fileW.getFilter()).getType() == CStdType.Integer ? "%d" : "%f";
            String cast = ((FileOutputContent)fileW.getFilter()).getType() == CStdType.Integer ? "(int)" : "(float)";
            String bufferName = buf.getAddressRotation(filterNode).currentWriteBufName;
            //create the loop
            statements.addStatement(Util.toStmt(
                    "for (int _i_ = 0; _i_ < " + outputs + "; _i_++) printf(\"" + type + "\\n\", " + cast + 
                    bufferName +"[_i_])"));
            
        }
        
        // channel code after work block
        if (backEndBits.sliceHasUpstreamChannel(sliceNode.getParent())) {
            for (JStatement stmt : InputRotatingBuffer.getInputBuffer(filterNode).endInitRead()) {
                statements.addStatement(stmt);
            }
        }
        if (backEndBits.sliceHasDownstreamChannel(sliceNode.getParent())) {
            for (JStatement stmt : OutputRotatingBuffer.getOutputBuffer(filterNode).endInitWrite()) {
                statements.addStatement(stmt);
            }
        }

        statements.addAllStatements(endSchedulingPhase(SchedulingPhase.INIT));
        
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
        //times (because the 1st execution is of prework)
        return Utils.makeForLoopFieldIndex(block, useExeIndex1(), 
                           new JIntLiteral(initMult - 1), false);
    }

    public JMethodDeclaration getPrimePumpMethod() {
        if (primePumpMethod != null) {
            return primePumpMethod;
        }
        
        JBlock statements = new JBlock();
        // channel code before work block
        if (backEndBits.sliceHasUpstreamChannel(sliceNode.getParent())) {
            for (JStatement stmt : InputRotatingBuffer.getInputBuffer(filterNode).beginPrimePumpRead()) {
                statements.addStatement(stmt);
            }
        }
        if (backEndBits.sliceHasDownstreamChannel(sliceNode.getParent())) {
            for (JStatement stmt : OutputRotatingBuffer.getOutputBuffer(filterNode).beginPrimePumpWrite()) {
                statements.addStatement(stmt);
            }
        }
        // add the calls to the work function for the priming of the pipeline
        statements.addStatement(getWorkFunctionBlock(filterInfo.steadyMult));
        // channel code after work block
        if (backEndBits.sliceHasUpstreamChannel(sliceNode.getParent())) {
            for (JStatement stmt : InputRotatingBuffer.getInputBuffer(filterNode).endPrimePumpRead()) {
                statements.addStatement(stmt);
            }
        }
        if (backEndBits.sliceHasDownstreamChannel(sliceNode.getParent())) {
            for (JStatement stmt : OutputRotatingBuffer.getOutputBuffer(filterNode).endPrimePumpWrite()) {
                statements.addStatement(stmt);
            }
        }
        statements.addAllStatements(endSchedulingPhase(SchedulingPhase.PRIMEPUMP));
        //return the method
        primePumpMethod = new JMethodDeclaration(null, at.dms.kjc.Constants.ACC_PUBLIC,
                                      CStdType.Void,
                                      primePumpStage + uniqueID,
                                      JFormalParameter.EMPTY,
                                      CClassType.EMPTY,
                                      statements,
                                      null,
                                      null);
        return primePumpMethod;
    }

    @Override
    public JBlock getSteadyBlock() {
        JBlock statements = new JBlock();
        
        // channel code before work block
        if (backEndBits.sliceHasUpstreamChannel(sliceNode.getParent())) {
            for (JStatement stmt : InputRotatingBuffer.getInputBuffer(filterNode).beginSteadyRead()) {
                statements.addStatement(stmt);
            }
        }
        
        if (backEndBits.sliceHasDownstreamChannel(sliceNode.getParent())) {
            for (JStatement stmt : OutputRotatingBuffer.getOutputBuffer(filterNode).beginSteadyWrite()) {
                statements.addStatement(stmt);
            }
        }

        // Load balancing variables
        JVariableDefinition startCycleVar = null;
        JVariableDefinition endCycleVar = null;

        // load balancing code before filter execution
        if(KjcOptions.loadbalance && LoadBalancer.isLoadBalanced(filterNode.getParent())) {
            startCycleVar = 
                new JVariableDefinition(null,
                                        0,
                                        CInt64Type.Int64,
                                        "startCycle__" + uniqueID,
                                        null);

            statements.addStatement(new JVariableDeclarationStatement(startCycleVar));

            statements.addStatement(
                new JIfStatement(null,
                                 new JEqualityExpression(null,
                                                         true,
                                                         new JLocalVariableExpression(
                                                             LoadBalancer.getSampleBoolVar(
                                                                 SMPBackend.scheduler.getComputeNode(filterNode))),
                                                         new JBooleanLiteral(true)),
                                 new JExpressionStatement(
                                     new JAssignmentExpression(
                                         new JLocalVariableExpression(startCycleVar),
                                         new JMethodCallExpression("rdtsc",
                                                                   new JExpression[0]))),
                                 new JBlock(),
                                 new JavaStyleComment[0]));
        }
        
        // iterate work function as needed
        statements.addStatement(getWorkFunctionBlock(FilterInfo
                .getFilterInfo((FilterSliceNode) sliceNode).steadyMult));

        // load balancing code after filter execution
        if(KjcOptions.loadbalance && LoadBalancer.isLoadBalanced(filterNode.getParent())) {
            endCycleVar =
                new JVariableDefinition(null,
                                        0,
                                        CInt64Type.Int64,
                                        "endCycle__" + uniqueID,
                                        null);

            statements.addStatement(new JVariableDeclarationStatement(endCycleVar));

            String cycleCountRef = 
                LoadBalancer.getCycleCountRef(
                    FissionGroupStore.getFissionGroup(filterNode.getParent()),
                    filterNode.getParent());

            JBlock ifSampleThen = new JBlock();
            ifSampleThen.addStatement(
                new JExpressionStatement(
                    new JAssignmentExpression(
                        new JLocalVariableExpression(endCycleVar),
                        new JMethodCallExpression("rdtsc",
                                                  new JExpression[0]))));
            ifSampleThen.addStatement(
                new JExpressionStatement(
                    new JAssignmentExpression(
                        new JFieldAccessExpression(cycleCountRef),
                        new JAddExpression(
                            new JFieldAccessExpression(cycleCountRef),
                            new JMinusExpression(
                                null,
                                new JLocalVariableExpression(endCycleVar),
                                new JLocalVariableExpression(startCycleVar))))));

            JStatement ifSampleStatement =
                new JIfStatement(null,
                                 new JEqualityExpression(null,
                                                         true,
                                                         new JLocalVariableExpression(
                                                             LoadBalancer.getSampleBoolVar(
                                                                 SMPBackend.scheduler.getComputeNode(filterNode))),
                                                         new JBooleanLiteral(true)),
                                 ifSampleThen,
                                 new JBlock(),
                                 new JavaStyleComment[0]);

            statements.addStatement(ifSampleStatement);
        }

        
        // channel code after work block
        if (backEndBits.sliceHasUpstreamChannel(sliceNode.getParent())) {
            for (JStatement stmt : InputRotatingBuffer.getInputBuffer(filterNode).endSteadyRead()) {
                statements.addStatement(stmt);
            }
        }
        if (backEndBits.sliceHasDownstreamChannel(sliceNode.getParent())) {
            for (JStatement stmt : OutputRotatingBuffer.getOutputBuffer(filterNode).endSteadyWrite()) {
                statements.addStatement(stmt);
            }
        }
        statements.addAllStatements(endSchedulingPhase(SchedulingPhase.STEADY));
    
        return statements;
    }

    /**
     * Return a JBlock that iterates <b>mult</b> times the result of calling 
     * <b>getWorkFunctionCall()</b>.
     * @param mult Number of times to iterate work function.
     * @return as described, or <b>null</b> if <b>getWorkFunctionCall()</b> returns null;
     */
    @Override
    protected JBlock getWorkFunctionBlock(int mult) {
        if (getWorkMethod() == null) { return null; }
        JBlock block = new JBlock();
        JStatement workStmt = getWorkFunctionCall();
        JVariableDefinition loopCounter = new JVariableDefinition(null,
                0,
                CStdType.Integer,
                workCounter,
                null);

        JStatement loop;
        if(KjcOptions.loadbalance && LoadBalancer.isLoadBalanced(filterNode.getParent())) {
            FissionGroup group = FissionGroupStore.getFissionGroup(filterNode.getParent());

            loop = 
                Utils.makeForLoopLocalIndex(workStmt, 
                                            loopCounter, 
                                            new JNameExpression(
                                                null,
                                                LoadBalancer.getNumItersRef(group,
                                                                            filterNode.getParent())));
        }
        else {
            loop = Utils.makeForLoopLocalIndex(workStmt, loopCounter, new JIntLiteral(mult));
        }

        block.addStatement(new JVariableDeclarationStatement(null,
                loopCounter,
                null));
        block.addStatement(loop);
        return block;
    }
    
    /**
     * Code returned by this function will be appended to the methods for the init, primepump, and
     * steady stages for this filter.
     * 
     * @return  The block of code to append
     */
    protected JBlock endSchedulingPhase(SchedulingPhase phase) {
        JBlock block = new JBlock();
        return block;
    }
}
