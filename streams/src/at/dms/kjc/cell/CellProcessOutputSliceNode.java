package at.dms.kjc.cell;

import java.util.LinkedList;

import at.dms.kjc.CArrayType;
import at.dms.kjc.CClassType;
import at.dms.kjc.CStdType;
import at.dms.kjc.JAddExpression;
import at.dms.kjc.JArrayAccessExpression;
import at.dms.kjc.JArrayInitializer;
import at.dms.kjc.JAssignmentExpression;
import at.dms.kjc.JBlock;
import at.dms.kjc.JBreakStatement;
import at.dms.kjc.JEmptyStatement;
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
import at.dms.kjc.JModuloExpression;
import at.dms.kjc.JPrefixExpression;
import at.dms.kjc.JRelationalExpression;
import at.dms.kjc.JStatement;
import at.dms.kjc.JSwitchGroup;
import at.dms.kjc.JSwitchLabel;
import at.dms.kjc.JSwitchStatement;
import at.dms.kjc.JVariableDeclarationStatement;
import at.dms.kjc.JVariableDefinition;
import at.dms.kjc.backendSupport.BackEndFactory;
import at.dms.kjc.backendSupport.CodeStoreHelper;
import at.dms.kjc.backendSupport.ProcessFilterSliceNode;
import at.dms.kjc.backendSupport.ProcessOutputSliceNode;
import at.dms.kjc.backendSupport.SchedulingPhase;
import at.dms.kjc.slicegraph.Edge;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.InterSliceEdge;
import at.dms.kjc.slicegraph.OutputSliceNode;
import at.dms.kjc.slicegraph.SliceNode;

public class CellProcessOutputSliceNode extends ProcessOutputSliceNode {

    private CellComputeCodeStore ppuCS;
    private CellComputeCodeStore initCodeStore; 
    
    public CellProcessOutputSliceNode(OutputSliceNode outputNode, 
            SchedulingPhase whichPhase, CellBackendFactory backEndBits) {
        super(outputNode, whichPhase, backEndBits);
        ppuCS = backEndBits.getPPU().getComputeCode();
    }
    
    @Override
    public void additionalPreInitProcessing() {
        // Create new filter for RR splitter
        if (outputNode.isRRSplitter()) {
            int filterId = CellBackend.numfilters;
            CellBackend.filters.add(outputNode);
            CellBackend.filterIdMap.put(outputNode, filterId);
            CellBackend.numfilters++;
            ppuCS.setupWorkFunctionAddress(outputNode);
            ppuCS.setupFilterDescription(outputNode);
            ppuCS.setupPSP(outputNode);
            // attach artificial channel created earlier as input
            int channelId = CellBackend.artificialRRSplitterChannels.get(outputNode);
            LinkedList<Integer> inputIds = new LinkedList<Integer>();
            inputIds.add(channelId);
            ppuCS.attachInputChannelArray(filterId, inputIds);
            // attach outputs
            LinkedList<Integer> outputIds = CellBackend.outputChannelMap.get(outputNode);
            ppuCS.attachOutputChannelArray(filterId, outputIds);

            System.out.println("new splitter " + filterId + " " + outputNode.getIdent() + " " + outputNode.getWidth() + " " + outputNode.getPrevFilter().getFilter().getName());
        }
        
        addToScheduleLayout();
    }
    
    @Override
    public void additionalInitProcessing() {
        initCodeStore.addFields(splitter_code.getUsefulFields());
        initCodeStore.addMethods(splitter_code.getUsefulMethods());
    }
    
    @Override
    protected void standardSteadyProcessing() {
        // helper has now been used for the last time, so we can write the basic code.
        // write code deemed useful by the helper into the corrrect ComputeCodeStore.
        // write only once if multiple calls for steady state.
        if (!basicCodeWritten.containsKey(outputNode)) {
            //codeStore.addFields(splitter_code.getUsefulFields());
            codeStore.addMethods(splitter_code.getUsefulMethods());
            basicCodeWritten.put(outputNode,true);
        }
    }
    
    @Override
    public void additionalSteadyProcessing() {
        //System.out.println("processing output: " + outputNode.getPrevFilter().getFilter().getName());
        if (outputNode.isRRSplitter()) {

        } else if (outputNode.isDuplicateSplitter()) {
            //ppuCS.duplicateOutput(outputNode);
        }
//        ppuCS.addReadyBuffers(outputNode);
    }
    
    private void addToScheduleLayout() {
        if (outputNode.isRRSplitter()) {
            int filterId = CellBackend.filterIdMap.get(outputNode);
            LinkedList<Integer> currentGroup = CellBackend.getLastScheduleGroup();
            int artificialId = CellBackend.artificialRRSplitterChannels.get(outputNode);
            if (CellBackend.readyInputs.contains(artificialId)) {
                // If there is an available SPU, assign it
                if (currentGroup.size() < CellBackend.numspus) {
                    currentGroup.add(filterId);
                } 
                // Otherwise, make a new group
                else {
                    LinkedList<Integer> newGroup = new LinkedList<Integer>();
                    newGroup.add(filterId);
                    CellBackend.scheduleLayout.add(newGroup);
                    // Mark the outputs of the previous group as ready
                    CellBackend.addReadyInputsForCompleted(currentGroup);
                }
            }
            // If not all of the input channels are ready, must wait for
            // current group to complete. Afterwards, make a new group with this
            // joiner
            else {
                LinkedList<Integer> newGroup = new LinkedList<Integer>();
                newGroup.add(filterId);
                CellBackend.scheduleLayout.add(newGroup);
                // Mark the outputs of the previous group as ready
                CellBackend.addReadyInputsForCompleted(currentGroup);
            }
        }
    }
    
    private CellPU getLocationFromScheduleLayout(OutputSliceNode sliceNode) {
        int cpu = -1;
        int filterId;
        if (sliceNode.isRRSplitter())
            filterId = CellBackend.filterIdMap.get(sliceNode);
        else filterId = CellBackend.filterIdMap.get(sliceNode.getPrevFilter());
        for (LinkedList<Integer> l : CellBackend.scheduleLayout) {
            if (l.indexOf(filterId) > -1)
                cpu = l.indexOf(filterId);
        }
        if (cpu == -1) return null;
        return (CellPU)backEndBits.getComputeNode(cpu + 1);
    }
    
    @Override
    protected void setSplitterCode() {
        if (whichPhase == SchedulingPhase.PREINIT) return;
        splitter_code = CodeStoreHelper.findHelperForSliceNode(outputNode);
        if (splitter_code == null) {
            splitter_code = getSplitterCode(outputNode,backEndBits);
        }
    }
    
    public static  CodeStoreHelper getSplitterCode(OutputSliceNode splitter, BackEndFactory backEndBits) {
        CodeStoreHelper splitter_code = CodeStoreHelper.findHelperForSliceNode(splitter);
        if (splitter_code == null) {
            splitter_code = backEndBits.getCodeStoreHelper(splitter);
            makeSplitterCode(splitter,backEndBits,splitter_code);
            CodeStoreHelper.addHelperForSliceNode(splitter,splitter_code);
        }
        return splitter_code;
    }
    
    private static void makeSplitterCode(OutputSliceNode splitter, 
            BackEndFactory backEndBits, CodeStoreHelper helper) {
        String splitter_name = "_splitter_" + ProcessFilterSliceNode.getUid();
        String splitter_method_name =  splitter_name + splitter.getPrevFilter().getFilter().getName();

        // size is number of edges with non-zero weight.
        int size = 0;
        for (int w : splitter.getWeights()) {
            if (w != 0) {size++;}
        }
        
        assert size > 0 : "asking for code generation for null splitter: ";// + splitter_method_name;
        
        //System.out.println(CellBackend.filterIdMap.get(splitter.getPrevFilter()) + " making splitter " + splitter_method_name);
        
//        String edge_name = splitter_name + "_edge";
//        String weight_name = splitter_name + "_weight";

        String param_name = "arg";
        JFormalParameter arg = new JFormalParameter(splitter.getType(),param_name);
        JExpression argExpr = new JLocalVariableExpression(arg);
/*        
        JVariableDefinition edgeVar = new JVariableDefinition(
                at.dms.kjc.Constants.ACC_STATIC,
                CStdType.Integer,
                edge_name,
                new JIntLiteral(size - 1));
        
        JFieldDeclaration edgeDecl = new JFieldDeclaration(edgeVar);
        JFieldAccessExpression edgeExpr = new JFieldAccessExpression(edge_name);
        
        JVariableDefinition weightVar = new JVariableDefinition(
                at.dms.kjc.Constants.ACC_STATIC,
                CStdType.Integer,
                weight_name,
                new JIntLiteral(0));

        JFieldDeclaration weightDecl = new JFieldDeclaration(weightVar);
        JFieldAccessExpression weightExpr = new JFieldAccessExpression(weight_name);
        
        JIntLiteral[] weightVals = new JIntLiteral[size];
        {
            int i = 0;
            for (int w : splitter.getWeights()) {
                if (w != 0) {
                    weightVals[i++] = new JIntLiteral(w - 1);
                }
            }
        }
        
        JVariableDefinition weightsArray = new JVariableDefinition(
                at.dms.kjc.Constants.ACC_STATIC | at.dms.kjc.Constants.ACC_FINAL,  // static const in C
                new CArrayType(CStdType.Integer,
                        1, new JExpression[]{new JIntLiteral(size)}),
                "weights",
                new JArrayInitializer(weightVals));
        JLocalVariableExpression weightsExpr = new JLocalVariableExpression(weightsArray);
        
        JStatement next_edge_weight_stmt = new JIfStatement(null,
                new JRelationalExpression(at.dms.kjc.Constants.OPE_LT,
                        new JPrefixExpression(null,
                                at.dms.kjc.Constants.OPE_PREDEC,
                                weightExpr),
                        new JIntLiteral(0)),
                new JBlock(new JStatement[]{
                        new JExpressionStatement(new JAssignmentExpression(
                                edgeExpr,
                                new JModuloExpression(null,
                                        new JAddExpression(
                                                edgeExpr,
                                                new JIntLiteral(1)),
                                        new JIntLiteral(size)))),
                        new JExpressionStatement(new JAssignmentExpression(
                                weightExpr,
                                new JArrayAccessExpression(weightsExpr,
                                        edgeExpr)
                                ))
                }),
                new JEmptyStatement(),
                null);

        
        JSwitchGroup[] cases = new JSwitchGroup[size]; // fill in later.
        JStatement switch_on_edge_stmt = new JSwitchStatement(null,
                edgeExpr,
                cases,
                null);
        
        {
            int i = 0;
            for (int j = 0; j < splitter.getWeights().length; j++) {
                if (splitter.getWeights()[j] != 0) {
                    Edge[] edges = splitter.getDests()[j];
                    JStatement[] pushes = new JStatement[edges.length + 1];
                    for (int k = 0; k < edges.length; k++) {
                        pushes[k] = new JExpressionStatement(
                            new JMethodCallExpression(
                                    backEndBits.getChannel(edges[k]).pushMethodName(),
                                new JExpression[]{argExpr}));
                    }
                    pushes[edges.length] = new JBreakStatement(null,null,null);
                    
                    cases[i] = new JSwitchGroup(null,
                            new JSwitchLabel[]{new JSwitchLabel(null,new JIntLiteral(i))},
                            pushes);
                    i++;
                }
            }
        }
        */

        JMethodDeclaration splitter_method = new JMethodDeclaration(
                null, 
                at.dms.kjc.Constants.ACC_STATIC | at.dms.kjc.Constants.ACC_INLINE,
                CStdType.Void,
                splitter_method_name,
                new JFormalParameter[]{arg},
                new CClassType[]{},
                new JBlock(),
                null, null);
        
        JBlock splitter_block = splitter_method.getBody();
        
        splitter_block.addStatement(
                new JExpressionStatement(
                        new JMethodCallExpression(
                                backEndBits.getChannel(splitter.getDests()[0][0]).pushMethodName(),
                                new JExpression[]{
                                    argExpr
                                })));
        
        helper.addMethod(splitter_method);
    }
    
    @Override
    protected void setLocationAndCodeStore() {
        //if (whichPhase == SchedulingPhase.PREINIT) return;
        location = backEndBits.getLayout().getComputeNode(outputNode);
        assert location != null;
        if (outputNode.isRRSplitter()) {
            codeStore = ((CellPU)location).getComputeCodeStore(outputNode);
            initCodeStore = ((CellPU)location).getInitComputeCodeStore(outputNode);
        } else {
            codeStore = ((CellPU)location).getComputeCodeStore(outputNode.getPrevFilter());
            initCodeStore = ((CellPU)location).getInitComputeCodeStore(outputNode.getPrevFilter());
        }
    }
    
}
