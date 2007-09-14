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
import at.dms.kjc.JReturnStatement;
import at.dms.kjc.JStatement;
import at.dms.kjc.JSwitchGroup;
import at.dms.kjc.JSwitchLabel;
import at.dms.kjc.JSwitchStatement;
import at.dms.kjc.JThisExpression;
import at.dms.kjc.JVariableDeclarationStatement;
import at.dms.kjc.JVariableDefinition;
import at.dms.kjc.KjcOptions;
import at.dms.kjc.backendSupport.BackEndFactory;
import at.dms.kjc.backendSupport.Channel;
import at.dms.kjc.backendSupport.CodeStoreHelper;
import at.dms.kjc.backendSupport.ProcessFilterSliceNode;
import at.dms.kjc.backendSupport.ProcessInputSliceNode;
import at.dms.kjc.backendSupport.SchedulingPhase;
import at.dms.kjc.common.ALocalVariable;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.InterSliceEdge;
import at.dms.kjc.slicegraph.SliceNode;

public class CellProcessInputSliceNode extends ProcessInputSliceNode {

    private CellComputeCodeStore ppuCS;
    private CellComputeCodeStore initCodeStore;
    
    public CellProcessInputSliceNode(InputSliceNode inputNode, 
            SchedulingPhase whichPhase, CellBackendFactory backEndBits) {
        super(inputNode, whichPhase, backEndBits);
        this.backEndBits = backEndBits;
        ppuCS = backEndBits.getPPU().getComputeCode();
    }
    
    public void additionalPreInitProcessing() {
        if (inputNode.getNextFilter().isFileOutput())
            return;
        // If InputSliceNode is a joiner (i.e. has at least 2 inputs), add it
        // as a filter.
        if (inputNode.isJoiner()) {
            int filterId = CellBackend.numfilters;
            CellBackend.filters.add(inputNode);
            CellBackend.filterIdMap.put(inputNode, filterId);
            CellBackend.numfilters++;
            // wf[i] = &wf_... and init_wf[i] = &wf_init_...
            ppuCS.setupWorkFunctionAddress(inputNode);
            // fd[i].state_size/num_inputs/num_outputs = ...
            ppuCS.setupFilterDescription(inputNode);
            // setup EXT_PSP_EX_PARAMS/LAYOUT
            ppuCS.setupPSP(inputNode);
            System.out.println("new joiner " + filterId + " " + inputNode.getIdent() + " " + inputNode.getWidth() + " " + inputNode.getNextFilter().getFilter().getName());
        }
        
        // Ids of channels that are inputs to this filter
        LinkedList<Integer> inputIds = new LinkedList<Integer>();
        // Populate Channel-ID mapping, and increase number of channels.
        for (InterSliceEdge e : inputNode.getSourceList()) {
            // Always use output->input direction for edges
            InterSliceEdge f = CellBackend.getEdgeBetween(e.getSrc(),inputNode);
            if(!CellBackend.channelIdMap.containsKey(f)) {
                int channelId = CellBackend.numchannels;
                CellBackend.channels.add(f);
                CellBackend.channelIdMap.put(f,channelId);
                inputIds.add(channelId);
                ppuCS.initChannel(channelId);
                CellBackend.numchannels++;
            } else {
                inputIds.add(CellBackend.channelIdMap.get(f));
            }
        }
        CellBackend.inputChannelMap.put(inputNode, inputIds);

        if (inputNode.isJoiner()) {
            int filterId = CellBackend.filterIdMap.get(inputNode);
            // attach all input channels as inputs to the joiner
            ppuCS.attachInputChannelArray(filterId, inputIds);
            //make artificial channel between inputslicenode and filterslicenode
            int channelId = CellBackend.numchannels;
            InterSliceEdge a = new InterSliceEdge(inputNode);
            CellBackend.channels.add(a);
            CellBackend.channelIdMap.put(a, channelId);
            CellBackend.artificialJoinerChannels.put(inputNode, channelId);
            ppuCS.initChannel(channelId);
            CellBackend.numchannels++;
            // attach artificial channel as output of the joiner
            LinkedList<Integer> outputIds = new LinkedList<Integer>();
            outputIds.add(channelId);
            ppuCS.attachOutputChannelArray(filterId, outputIds);
        }
        
        addToScheduleLayout();
    }
    
    @Override
    public void standardInitProcessing() {
        if (!CellBackend.filterIdMap.containsKey(inputNode))
            return;
        // Have the main function for the CodeStore call our init if any
        initCodeStore.addInitFunctionCall(joiner_code.getInitMethod());
        JMethodDeclaration workAtInit = joiner_code.getInitStageMethod();
        if (workAtInit != null) {
            // if there are calls to work needed at init time then add
            // method to general pool of methods
            initCodeStore.addMethod(workAtInit);
            // and add call to list of calls made at init time.
            // Note: these calls must execute in the order of the
            // initialization schedule -- so caller of this routine 
            // must follow order of init schedule.
            initCodeStore.addInitStatement(new JExpressionStatement(null,
                    new JMethodCallExpression(null, new JThisExpression(null),
                            workAtInit.getName(), new JExpression[0]), null));
        }
    }
    
    @Override
    public void additionalInitProcessing() {

        
    }
    
    @Override
    protected void standardSteadyProcessing() {
        if (!CellBackend.filterIdMap.containsKey(inputNode))
            return;
        JStatement steadyBlock = joiner_code.getSteadyBlock();
        // helper has now been used for the last time, so we can write the basic code.
        // write code deemed useful by the helper into the corrrect ComputeCodeStore.
        // write only once if multiple calls for steady state.
        if (!basicCodeWritten.containsKey(inputNode)) {
            //codeStore.addFields(joiner_code.getUsefulFields());
            codeStore.addMethods(joiner_code.getMethods());
            basicCodeWritten.put(inputNode,true);
        }
        codeStore.addSteadyLoopStatement(steadyBlock);
    }
    
    private void addToScheduleLayout() {
        if (inputNode.isJoiner()) {
            int filterId = CellBackend.filterIdMap.get(inputNode);
            LinkedList<Integer> inputIds = CellBackend.inputChannelMap.get(inputNode);
            LinkedList<Integer> currentGroup = CellBackend.getLastScheduleGroup();
            // If all of the input channels are ready
            if (CellBackend.readyInputs.containsAll(inputIds)) {
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
    
    private CellPU getLocationFromScheduleLayout(InputSliceNode sliceNode) {
        int cpu = -1;
        int filterId;
        if (sliceNode.isJoiner())
            filterId = CellBackend.filterIdMap.get(sliceNode);
        else filterId = CellBackend.filterIdMap.get(sliceNode.getNextFilter());
        for (LinkedList<Integer> l : CellBackend.scheduleLayout) {
            if (l.indexOf(filterId) > -1)
                cpu = l.indexOf(filterId);
        }
        if (cpu == -1) return null;
        return (CellPU)backEndBits.getComputeNode(cpu + 1);
    }
    
    @Override
    protected void setJoinerCode() {
        //System.out.println(whichPhase);
        if (whichPhase == SchedulingPhase.PREINIT) return;
        joiner_code = CodeStoreHelper.findHelperForSliceNode(inputNode);
        
        if (joiner_code == null) {
            joiner_code = getJoinerCode(inputNode,backEndBits);
        }
    }
    
    public static CodeStoreHelper getJoinerCode(InputSliceNode joiner, BackEndFactory backEndBits) {
        //System.out.println("getjoinercode " + joiner);
        CodeStoreHelper joiner_code = CodeStoreHelper.findHelperForSliceNode(joiner);
        if (joiner_code == null) {
            joiner_code = backEndBits.getCodeStoreHelper(joiner);
            if (backEndBits.sliceNeedsJoinerCode(joiner.getParent())) {
                makeJoinerCode(joiner,backEndBits,joiner_code);
            }
//            if (backEndBits.sliceNeedsJoinerWorkFunction(joiner.getParent())) {
//                makeJoinerWork(joiner,backEndBits,joiner_code);
//            }
            CodeStoreHelper.addHelperForSliceNode(joiner, joiner_code);
        }
        return joiner_code;
    }
    
    private static  void makeJoinerCode(InputSliceNode joiner,
            BackEndFactory backEndBits, CodeStoreHelper helper) {
        String joiner_name = "_joiner_" + ProcessFilterSliceNode.getUid();
        String joiner_method_name =  joiner_name + joiner.getNextFilter().getFilter().getName();

        // size is number of edges with non-zero weight.
        int size = 0;
        for (int w : joiner.getWeights()) {
            if (w != 0) {size++;}
        }
        
        //System.out.println(CellBackend.filterIdMap.get(joiner.getNextFilter()) + " making joiner code " + joiner_method_name);
        
        assert size > 0 : "asking for code generation for null joiner";
        
        JMethodDeclaration joiner_method = new JMethodDeclaration(
                null, at.dms.kjc.Constants.ACC_STATIC | at.dms.kjc.Constants.ACC_INLINE,
                joiner.getType(),
                joiner_method_name,
                new JFormalParameter[]{},
                new CClassType[]{},
                new JBlock(),
                null, null);
        
        JBlock joiner_block = joiner_method.getBody();
        JMethodCallExpression pop = new JMethodCallExpression(
                backEndBits.getChannel(joiner.getSources()[0]).popMethodName(),
                new JExpression[0]);
        JReturnStatement ret = new JReturnStatement(
                null, pop, null);
        
        joiner_block.addStatement(ret);

        helper.addMethod(joiner_method);
    }

     
    /**
     * Make a work function for a joiner 
     * @param joiner  the InputSliceNode that we are generating code for.
     * @param backEndBits way to refer to other portions of backend
     * @param joiner_code  place to put code
     */
    
    public static  void makeJoinerWork(InputSliceNode joiner,
            BackEndFactory backEndBits, CodeStoreHelper joiner_code) {
        JMethodDeclaration joinerWork;

        // the work function will need a temporary variable
        ALocalVariable t = ALocalVariable.makeTmp(joiner.getEdgeToNext().getType());

        Channel downstream = backEndBits.getChannel(joiner.getEdgeToNext());

        // the body of the work method
        JBlock body = new JBlock();
        
        if (backEndBits.sliceNeedsJoinerCode(joiner.getParent())) {
            // There should be generated code for the joiner
            // state machine in the CodeStoreHelper as the only method.
            //
            // generated code is
            // T tmp;
            // tmp = joiner_code();
            // push(tmp);
            //
            // TODO: inline the joiner code at the call site,
            // if inlining, delete joiner code after inlining leaving
            // only this method in the helper.
            assert joiner_code.getMethods().length == 1;
            JMethodDeclaration callable_joiner = joiner_code.getMethods()[0];
            
            body.addStatement(t.getDecl());
            body.addStatement(new JExpressionStatement(
                    new JAssignmentExpression(
                            t.getRef(),
                            new JMethodCallExpression(
                                    callable_joiner.getName(),
                                    new JExpression[0]
                            ))));
            body.addStatement(new JExpressionStatement(
                    new JMethodCallExpression(
                            downstream.pushMethodName(),
                            new JExpression[]{t.getRef()})));
        } else {
            // slice does not need joiner code, so just transfer from upstream
            // to downstream buffer.
            //
            // generated code is
            // T tmp;
            // tmp = pop();
            // push(tmp);
            //
            assert joiner.getWidth() == 1;
            Channel upstream = backEndBits.getChannel(joiner.getSingleEdge());

            body.addStatement(t.getDecl());
            body.addStatement(new JExpressionStatement(
                    new JAssignmentExpression(
                            t.getRef(),
                            new JMethodCallExpression(
                                    upstream.popMethodName(),
                                    new JExpression[0]
                            ))));
            body.addStatement(new JExpressionStatement(
                    new JMethodCallExpression(
                            downstream.pushMethodName(),
                            new JExpression[]{t.getRef()})));

        }
        joinerWork = new JMethodDeclaration(
                CStdType.Void,
                "_joinerWork_" + joiner.getNextFilter().getFilter().getName(),
                JFormalParameter.EMPTY,
                body);
        joiner_code.setWorkMethod(joinerWork);
    }
    
    @Override
    protected void setLocationAndCodeStore() {
        //if (whichPhase == SchedulingPhase.PREINIT) return;
        location = backEndBits.getLayout().getComputeNode(inputNode);
        assert location != null;
        if (inputNode.isJoiner()) {
            codeStore = ((CellPU)location).getComputeCodeStore(inputNode);
            initCodeStore = ((CellPU)location).getInitComputeCodeStore(inputNode);
        } else {
            codeStore = ((CellPU)location).getComputeCodeStore(inputNode.getNextFilter());
            initCodeStore = ((CellPU)location).getInitComputeCodeStore(inputNode.getNextFilter());
        }
    }
}
