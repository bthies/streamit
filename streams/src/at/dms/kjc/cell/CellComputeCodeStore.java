package at.dms.kjc.cell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import at.dms.kjc.CArrayType;
import at.dms.kjc.CEmittedTextType;
import at.dms.kjc.CStdType;
import at.dms.kjc.Constants;
import at.dms.kjc.JAddExpression;
import at.dms.kjc.JArrayAccessExpression;
import at.dms.kjc.JAssignmentExpression;
import at.dms.kjc.JBlock;
import at.dms.kjc.JCompoundAssignmentExpression;
import at.dms.kjc.JDivideExpression;
import at.dms.kjc.JEmittedTextExpression;
import at.dms.kjc.JExpression;
import at.dms.kjc.JExpressionListStatement;
import at.dms.kjc.JExpressionStatement;
import at.dms.kjc.JFieldAccessExpression;
import at.dms.kjc.JFieldDeclaration;
import at.dms.kjc.JForStatement;
import at.dms.kjc.JFormalParameter;
import at.dms.kjc.JIntLiteral;
import at.dms.kjc.JLocalVariableExpression;
import at.dms.kjc.JMethodCallExpression;
import at.dms.kjc.JMethodDeclaration;
import at.dms.kjc.JMinusExpression;
import at.dms.kjc.JMultExpression;
import at.dms.kjc.JNameExpression;
import at.dms.kjc.JPostfixExpression;
import at.dms.kjc.JRelationalExpression;
import at.dms.kjc.JStatement;
import at.dms.kjc.JStringLiteral;
import at.dms.kjc.JThisExpression;
import at.dms.kjc.JVariableDeclarationStatement;
import at.dms.kjc.JVariableDefinition;
import at.dms.kjc.KjcOptions;
import at.dms.kjc.backendSupport.ComputeCodeStore;
import at.dms.kjc.backendSupport.SchedulingPhase;
import at.dms.kjc.backendSupport.SpaceTimeScheduleAndPartitioner;
import at.dms.kjc.common.ALocalVariable;
import at.dms.kjc.slicegraph.FileInputContent;
import at.dms.kjc.slicegraph.FileOutputContent;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.InterSliceEdge;
import at.dms.kjc.slicegraph.OutputSliceNode;
import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.slicegraph.SliceNode;

public class CellComputeCodeStore extends ComputeCodeStore<CellPU> {
    
    static { if ("".equals(mainName)) mainName = "__MAIN__";}
    
    private static final int numspus = KjcOptions.cell;
    
    private HashMap<Slice,Integer> sliceIdMap = new HashMap<Slice,Integer>();
    
    public static final HashMap<InterSliceEdge,Buffer> readyInputs = 
        new HashMap<InterSliceEdge,Buffer>();
    
    
    private ArrayList<JFieldDeclaration> workfuncs = new ArrayList<JFieldDeclaration>();
    private ArrayList<JFieldDeclaration> fds = new ArrayList<JFieldDeclaration>();
    private ArrayList<JMethodDeclaration> initfuncs = new ArrayList<JMethodDeclaration>();
    
    private int id = 0;
    
    private SpaceTimeScheduleAndPartitioner schedule;
    private int poprate;
    private int pushrate;
    private String outputType;
    private String inputType;
    private SliceNode slicenode;
    
    private JVariableDefinition l, r;
    
    public CellComputeCodeStore(CellPU parent) {
        super(parent);
    }
    
    public CellComputeCodeStore(CellPU parent, SliceNode s) {
        super(parent);
        slicenode = s;
    }
    
    public SliceNode getSliceNode() {
        return slicenode;
    }
    
    @Override
    protected void addSteadyLoop() {
        mainMethod.addStatement(steadyLoop);
    }
    
    @Override
    public void addInitFunctionCall(JMethodDeclaration init) {
        //JMethodDeclaration init = filterInfo.filter.getInit();
        if (init != null)
            initfuncs.add(init);
//            mainMethod.addStatementFirst
//            (new JExpressionStatement(null,
//                    new JMethodCallExpression(null, new JThisExpression(null),
//                            init.getName(), new JExpression[0]),
//                            null));
        else
            System.err.println(" ** Warning: Init function is null");

    }
    
    public void addInitFunctions() {
        JBlock initcalls = new JBlock();
        for (JMethodDeclaration initfunc : initfuncs) {
            initcalls.addStatement(new JExpressionStatement(null,
                  new JMethodCallExpression(null, new JThisExpression(null),
                      initfunc.getName(), new JExpression[0]),
                      null));
        }
        addMethod(new JMethodDeclaration(CStdType.Void, "__INIT_FUNC__", new JFormalParameter[0], initcalls));
    }
    
    /**-------------------------------------------------------------------------
     *                        FIELDS
     * -------------------------------------------------------------------------
     */
    
    /**
     * Add work function address field, for the actual work function and the
     * work function used for the init schedule
     *  
     * LS_ADDRESS wf = wf_[filtername];
     * LS_ADDRESS init_wf = wf_[filtername];
     */
    public void addWorkFunctionAddressField() {
        // work func
        JVariableDefinition wf = new JVariableDefinition(
                new CArrayType(
                        new CEmittedTextType(LS_ADDRESS),
                        1,
                        new JExpression[]{new JIntLiteral(CellBackend.numfilters)}),
                WFA);
        JFieldDeclaration field = new JFieldDeclaration(wf);
        addField(field);

        // init work func
        JVariableDefinition initwf = new JVariableDefinition(
                new CArrayType(
                        new CEmittedTextType(LS_ADDRESS),
                        1,
                        new JExpression[]{new JIntLiteral(CellBackend.numfilters)}),
                INIT_WFA);
        field = new JFieldDeclaration(initwf);
        addField(field);
    }
    
    /**
     * Initialize PSP param and layout fields as arrays, one for each filter.
     * These will be filled and reused later.
     *
     */
    public void addPSPFields() {
        JVariableDefinition params = new JVariableDefinition(
                new CArrayType(
                        new CEmittedTextType("EXT_PSP_EX_PARAMS"),
                        1,
                        new JExpression[]{new JIntLiteral(CellBackend.numfilters)}),
                "f");
        addField(new JFieldDeclaration(params));
        
        JVariableDefinition layout = new JVariableDefinition(
                new CArrayType(
                        new CEmittedTextType("EXT_PSP_EX_LAYOUT"),
                        1,
                        new JExpression[]{new JIntLiteral(CellBackend.numfilters)}),
                "l");
        addField(new JFieldDeclaration(layout));
    }
    
    /**
     * Add init function address field: LS_ADDRESS init_[i];
     */
    public void addInitFunctionAddressField() {
        JVariableDefinition initfunc = new JVariableDefinition(
                new CArrayType(new CEmittedTextType(LS_ADDRESS),1,new JExpression[]{new JIntLiteral(CellBackend.numfilters)}), INIT_FUNC);
        JFieldDeclaration field = new JFieldDeclaration(initfunc);
        addField(field);
    }

    public void addInitFunctionAddresses(FilterSliceNode filterNode) {
        int filterId = CellBackend.filterIdMap.get(filterNode);
        addInitStatement(new JExpressionStatement(new JAssignmentExpression(
                new JArrayAccessExpression(new JFieldAccessExpression(INIT_FUNC),new JIntLiteral(filterId)),
                new JMethodCallExpression("&",new JExpression[]{new JEmittedTextExpression(INIT_FUNC_PREFIX+filterNode.getFilter().getName())}))));
    }
    
    /**
     * Add SPU filter description field: SPU_FILTER_DESC fd;
     */
    public void addSPUFilterDescriptionField() {
        JVariableDefinition fd = new JVariableDefinition(
                new CArrayType(
                        new CEmittedTextType(SPU_FILTER_DESC),
                        1,
                        new JExpression[]{
                            new JIntLiteral(CellBackend.numfilters)
                        }),
                SPU_FD);
        JFieldDeclaration field = new JFieldDeclaration(fd);
        addField(field);
        JVariableDefinition initfd = new JVariableDefinition(
                new CArrayType(
                        new CEmittedTextType(SPU_FILTER_DESC),
                        1,
                        new JExpression[]{
                            new JIntLiteral(CellBackend.numfilters)
                        }),
                INIT_SPU_FD);
        field = new JFieldDeclaration(initfd);
        addField(field);
    }
    
    public void addChannelFields() {
        JVariableDefinition c = new JVariableDefinition(
                new CArrayType(
                        new CEmittedTextType("BUFFER_CB"),
                        1,
                        new JExpression[]{
                            new JIntLiteral(CellBackend.numchannels)
                        }),
                "channels");
        JFieldDeclaration field = new JFieldDeclaration(c);
        addField(field);
    }
    
    public void addDataAddressField() {
        JVariableDefinition da = new JVariableDefinition(
                new CEmittedTextType(SPU_ADDRESS), DATA_ADDR);
        JFieldDeclaration field = new JFieldDeclaration(da);
        addField(field);
    }
    

    /**
     * Add callback function
     *
     */
    public void addCallBackFunction() {
        JFormalParameter tag = new JFormalParameter(new CEmittedTextType(UINT32_T), "tag");
        JBlock body = new JBlock();
        body.addStatement(new JExpressionStatement(new JPostfixExpression(Constants.OPE_POSTDEC, new JFieldAccessExpression(DONE))));
        JMethodDeclaration cb = new JMethodDeclaration(CStdType.Void, CB, new JFormalParameter[]{tag}, body);
        addMethod(cb);
        newline();
    }
    
    
    /** ------------------------------------------------------------------------
     *                          SETUP
     -------------------------------------------------------------------------*/
    
    /**
     * Sets up filter description's fields:
     * state_size, num_inputs, num_outputs
     * 
     * work_func will be either the init work func or the actual work func,
     * depending on which is being run. Thus, work_func will be set later.
     * @param sliceNode
     */
    public void setupFilterDescription(SliceNode sliceNode) {
        int filterId = CellBackend.filterIdMap.get(sliceNode);
        JBlock body = new JBlock();
        
        int outputs;
        if (sliceNode.isFilterSlice() && sliceNode.getAsFilter().getParent().getTail().isDuplicateSplitter())
            outputs = 1;
        else outputs = sliceNode.getParent().getTail().getWidth();
        
        JExpressionStatement statesize = new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(
                        new JArrayAccessExpression(
                                new JFieldAccessExpression(SPU_FD),
                                new JIntLiteral(filterId)),
                        SPU_FD_STATE_SIZE),
                new JIntLiteral(0)));
        body.addStatement(statesize);
        JExpressionStatement numinputs = new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(
                        new JArrayAccessExpression(
                                new JFieldAccessExpression(SPU_FD),
                                new JIntLiteral(filterId)),
                        SPU_FD_NUM_INPUTS),
                new JIntLiteral(sliceNode.getParent().getHead().getWidth())));
        body.addStatement(numinputs);
        JExpressionStatement numoutputs = new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(
                        new JArrayAccessExpression(
                                new JFieldAccessExpression(SPU_FD),
                                new JIntLiteral(filterId)),
                        SPU_FD_NUM_OUTPUTS),
                new JIntLiteral(outputs)));
        body.addStatement(numoutputs);
        
        addInitStatement(body);
    }
    
    /**
     * Set the filter description's work func to be the appropriate work func
     * depending on the phase (init or steady)
     * @param sliceNode
     * @param whichPhase
     */
    public void setupFilterDescriptionWorkFunc(SliceNode sliceNode, SchedulingPhase whichPhase) {
        int filterId = CellBackend.filterIdMap.get(sliceNode);
        
        String workfuncname;
        if (whichPhase == SchedulingPhase.INIT)
            workfuncname = INIT_WFA;
        else if (whichPhase == SchedulingPhase.STEADY)
            workfuncname = WFA;
        else return;
        
        JExpressionStatement workfunc = new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(
                        new JArrayAccessExpression(
                                new JFieldAccessExpression(SPU_FD),
                                new JIntLiteral(filterId)),
                        SPU_FD_WORK_FUNC),
                new JArrayAccessExpression(new JFieldAccessExpression(workfuncname), 
                                           new JIntLiteral(filterId))));
        addInitStatement(workfunc);
    }
    
    
    /**
     * Assigns work function address for the given slicenode.
     * Also assigns work function address for init function of the given slicenode.
     * wf = &wf_[init_][slicename]
     * wf = &wf_[init_]joiner_[joinername]
     * wf = &wf_[init_]splitter_[splittername]
     * @param sliceNode
     */
    public void setupWorkFunctionAddress(SliceNode sliceNode) {
        String sliceName = sliceNode.getParent().getFilterNodes().get(0).getFilter().getName();
        if (sliceNode.isFilterSlice()) {
            addInitStatement(new JExpressionStatement(new JAssignmentExpression(
                    new JArrayAccessExpression(
                            new JFieldAccessExpression(WFA),
                            new JIntLiteral(CellBackend.filterIdMap.get(sliceNode))),
                    new JMethodCallExpression("&",new JExpression[]{
                            new JEmittedTextExpression(
                                    WFA_PREFIX + sliceName)}))));
            addInitStatement(new JExpressionStatement(new JAssignmentExpression(
                    new JArrayAccessExpression(
                            new JFieldAccessExpression(INIT_WFA),
                            new JIntLiteral(CellBackend.filterIdMap.get(sliceNode))),
                    new JMethodCallExpression("&",new JExpression[]{
                            new JEmittedTextExpression(
                                    WFA_PREFIX + "init_" + sliceName)}))));
        } else if (sliceNode.isInputSlice()) {
            addInitStatement(new JExpressionStatement(new JAssignmentExpression(
                    new JArrayAccessExpression(
                            new JFieldAccessExpression(WFA),
                            new JIntLiteral(CellBackend.filterIdMap.get(sliceNode))),
                    new JMethodCallExpression("&",new JExpression[]{
                            new JEmittedTextExpression(
                                    WFA_PREFIX + "joiner_" + sliceName)}))));
            addInitStatement(new JExpressionStatement(new JAssignmentExpression(
                    new JArrayAccessExpression(
                            new JFieldAccessExpression(INIT_WFA),
                            new JIntLiteral(CellBackend.filterIdMap.get(sliceNode))),
                    new JMethodCallExpression("&",new JExpression[]{
                            new JEmittedTextExpression(
                                    WFA_PREFIX + "init_joiner_" + sliceName)}))));
        } else {
            addInitStatement(new JExpressionStatement(new JAssignmentExpression(
                    new JArrayAccessExpression(
                            new JFieldAccessExpression(WFA),
                            new JIntLiteral(CellBackend.filterIdMap.get(sliceNode))),
                    new JMethodCallExpression("&",new JExpression[]{
                            new JEmittedTextExpression(
                                    WFA_PREFIX + "splitter_" + sliceName)}))));
            addInitStatement(new JExpressionStatement(new JAssignmentExpression(
                    new JArrayAccessExpression(
                            new JFieldAccessExpression(INIT_WFA),
                            new JIntLiteral(CellBackend.filterIdMap.get(sliceNode))),
                    new JMethodCallExpression("&",new JExpression[]{
                            new JEmittedTextExpression(
                                    WFA_PREFIX + "init_splitter_" + sliceName)}))));
        }
    }
    
    /**
     * Sets up most of the parameters for ext_psp, except for pop/peek_extra/push_bytes,
     * which differ depending on INIT/STEADY stage
     * @param sliceNode
     */
    public void setupPSP(SliceNode sliceNode) {
        int inputs = sliceNode.getParent().getHead().getWidth();
        int outputs = sliceNode.getParent().getTail().getWidth();
        if (sliceNode.isFilterSlice() && sliceNode.getAsFilter().getParent().getTail().isDuplicateSplitter())
            outputs = 1;
        int filterId = CellBackend.filterIdMap.get(sliceNode);
        JBlock body = new JBlock();
        
        // setup EXT_PSP_EX_PARAMS
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(
                        new JArrayAccessExpression(
                                new JFieldAccessExpression("f"),
                                new JIntLiteral(filterId)),
                        "num_inputs"),
                new JIntLiteral(inputs))));
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(
                        new JArrayAccessExpression(
                                new JFieldAccessExpression("f"),
                                new JIntLiteral(filterId)),
                        "num_outputs"),
                new JIntLiteral(outputs))));
        for (int i=0; i<inputs; i++) {
            // set up pop/peek_extra bytes later
            body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                    new JFieldAccessExpression(new JFieldAccessExpression(
                                                    new JArrayAccessExpression(
                                                            new JFieldAccessExpression("f"),
                                                            new JIntLiteral(filterId)),
                                                    "inputs[" + i + "]"),
                                               "spu_buf_size"),
                    new JFieldAccessExpression(INPUT_BUFFER_SIZE))));
        }
        
        for (int i=0; i<outputs; i++) {
            // set up push_bytes later
            body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                    new JFieldAccessExpression(new JFieldAccessExpression(
                                                    new JArrayAccessExpression(
                                                            new JFieldAccessExpression("f"),
                                                            new JIntLiteral(filterId)),
                                                    "outputs[" + i + "]"),
                                               "spu_buf_size"),
                    new JFieldAccessExpression(OUTPUT_BUFFER_SIZE))));
        }
        
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JArrayAccessExpression(
                        new JFieldAccessExpression("f"), new JIntLiteral(filterId)),"data_parallel"),
                new JEmittedTextExpression("FALSE"))));
        
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JArrayAccessExpression(
                        new JFieldAccessExpression("f"), new JIntLiteral(filterId)),"group_iters"),
                new JFieldAccessExpression(RUNSPERITER))));
        
        // set up EXT_PSP_EX_LAYOUT
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JArrayAccessExpression(
                        new JFieldAccessExpression("l"), new JIntLiteral(filterId)), "desc"),
                new JMethodCallExpression("&",
                        new JExpression[]{new JArrayAccessExpression(
                                new JFieldAccessExpression(SPU_FD), new JIntLiteral(filterId))}))));
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JArrayAccessExpression(
                        new JFieldAccessExpression("l"), new JIntLiteral(filterId)), "filt_cb"),
                new JFieldAccessExpression(FCB))));
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JArrayAccessExpression(
                        new JFieldAccessExpression("l"), new JIntLiteral(filterId)), "in_buf_start"),
                new JFieldAccessExpression(INPUT_BUFFER_ADDR))));
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JArrayAccessExpression(
                        new JFieldAccessExpression("l"), new JIntLiteral(filterId)), "out_buf_start"),
                new JFieldAccessExpression(OUTPUT_BUFFER_ADDR))));
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JArrayAccessExpression(
                        new JFieldAccessExpression("l"), new JIntLiteral(filterId)), "cmd_data_start"),
                new JFieldAccessExpression(DATA_ADDR))));
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JArrayAccessExpression(
                        new JFieldAccessExpression("l"), new JIntLiteral(filterId)), "cmd_id_start"),
                new JIntLiteral(0))));
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JArrayAccessExpression(
                        new JFieldAccessExpression("l"), new JIntLiteral(filterId)), "load_filter"),
                new JEmittedTextExpression("TRUE"))));

        addInitStatement(body);
    }
    
    /**
     * Set up the [pop/peek_extra/push]_bytes for the PSP params. These values
     * may differ between the init and steady states due to different multiplicities.
     * @param sliceNode
     * @param whichPhase
     */
    public void setupPSPIOBytes(SliceNode sliceNode, SchedulingPhase whichPhase) {
        int inputs = sliceNode.getParent().getHead().getWidth();
        int outputs = sliceNode.getParent().getTail().getWidth();
        if (sliceNode.isFilterSlice() && sliceNode.getAsFilter().getParent().getTail().isDuplicateSplitter())
            outputs = 1;
        int filterId = CellBackend.filterIdMap.get(sliceNode);
        int poprate = sliceNode.getParent().getFilterNodes().get(0).getFilter().getPopInt();
        int pushrate = sliceNode.getParent().getFilterNodes().get(0).getFilter().getPushInt();
        int peekrate = sliceNode.getParent().getFilterNodes().get(0).getFilter().getPeekInt();
        int mult;
        if (whichPhase == SchedulingPhase.INIT)
            mult = sliceNode.getParent().getFilterNodes().get(0).getFilter().getInitMult();
        else if (whichPhase == SchedulingPhase.STEADY)
            mult = sliceNode.getParent().getFilterNodes().get(0).getFilter().getSteadyMult();
        else return;
        
        JBlock body = new JBlock();
        for (int i=0; i<inputs; i++) {
            body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                    new JFieldAccessExpression(new JFieldAccessExpression(
                            new JArrayAccessExpression(
                                    new JFieldAccessExpression("f"),
                                    new JIntLiteral(filterId)),
                                    "inputs[" + i + "]"),
                            "pop_bytes"),
                    new JIntLiteral(4 * poprate * mult))));
            body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                    new JFieldAccessExpression(new JFieldAccessExpression(
                            new JArrayAccessExpression(
                                    new JFieldAccessExpression("f"),
                                    new JIntLiteral(filterId)),
                                    "inputs[" + i + "]"),
                            "peek_extra_bytes"),
                    new JIntLiteral(4 * Math.max(0, peekrate-poprate) * mult))));
        }
        for (int i=0; i<outputs; i++) {
            body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                    new JFieldAccessExpression(new JFieldAccessExpression(
                            new JArrayAccessExpression(
                                    new JFieldAccessExpression("f"),
                                    new JIntLiteral(filterId)),
                                    "outputs[" + i + "]"),
                            "push_bytes"),
                    new JIntLiteral(4 * pushrate * mult))));
        }
    }

    /**
     * Get the ID of the SPU that the filter has been assigned to, and set
     * the spu_id parameter in ext_psp_layout.
     * @param sliceNode
     */
    public void setupPSPSpuId(SliceNode sliceNode) {
        int filterId = CellBackend.filterIdMap.get(sliceNode);
        //TODO
        int spu = 0;
        for (Integer i : CellBackend.SPUassignment.keySet()) {
            if (CellBackend.SPUassignment.get(i) == sliceNode)
                spu = i.intValue();
        }
        addInitStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JArrayAccessExpression(
                        new JFieldAccessExpression("l"), new JIntLiteral(filterId)), "spu_id"),
                new JIntLiteral(spu))));
        
    }
    
    /**
     * Call ext_ppu_spu_ppu_ex to run the filter asynchronously.
     * @param sliceNode
     */
    public void callExtPSP(SliceNode sliceNode) {
        int filterId = CellBackend.filterIdMap.get(sliceNode);
        addInitStatement(new JExpressionStatement(new JMethodCallExpression("ext_ppu_spu_ppu_ex",
                new JExpression[]{
                new JMethodCallExpression("&", new JExpression[]{new JArrayAccessExpression(
                        new JFieldAccessExpression("l"), new JIntLiteral(filterId))}),
                        new JMethodCallExpression("&", new JExpression[]{new JArrayAccessExpression(
                                new JFieldAccessExpression("f"), new JIntLiteral(filterId))}),
                                new JMethodCallExpression("&", new JExpression[]{new JFieldAccessExpression("input_"+filterId)}),
                                new JMethodCallExpression("&", new JExpression[]{new JFieldAccessExpression("output_"+filterId)}),
                                new JFieldAccessExpression(N),
                                new JEmittedTextExpression("cb"),
                                new JIntLiteral(0)
        })));
    }
    
    public void addSpulibPollWhile() {
        String filtername;
        addInitStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(DONE),
                new JIntLiteral(CellBackend.SPUassignment.size()))));
        addInitStatement(new JExpressionStatement(new JMethodCallExpression(
                "spulib_poll_while",
                new JExpression[]{
                        new JRelationalExpression(Constants.OPE_GT, new JFieldAccessExpression(DONE), new JIntLiteral(0))
                })));
        for (SliceNode s : CellBackend.SPUassignment.values()) {
            filtername = s.getParent().getFilterNodes().get(0).getFilter().getName();
            addInitStatement(new JExpressionStatement(new JMethodCallExpression(
                    "dealloc_buffer",
                    new JExpression[]{
                            new JMethodCallExpression(
                                    "&",
                                    new JExpression[]{
                                            new JEmittedTextExpression("input_"+filtername)
                                    })})));
            addReadyBuffers(s.getParent().getTail());
        }
    }
    
    /**
     * Duplicate channelId to all the channels in duplicateIds.
     * @param channelId
     * @param duplicateIds
     */
    public void duplicateChannel(int channelId, LinkedList<Integer> duplicateIds) {
        for (int i : duplicateIds) {
            addInitStatement(new JExpressionStatement(new JMethodCallExpression(
                    "duplicate_buffer",
                    new JExpression[]{
                        new JMethodCallExpression(
                                "&",
                                new JExpression[]{
                                    new JArrayAccessExpression(
                                        new JFieldAccessExpression("channels"),
                                        new JIntLiteral(i))}),
                        new JMethodCallExpression(
                                "&",
                                new JExpression[]{
                                    new JArrayAccessExpression(
                                        new JFieldAccessExpression("channels"),
                                        new JIntLiteral(channelId))}),
                    }
            )));
        }
    }
    
    public void addIssueUnload() {
        
        JForStatement unload;
        // make init statement - assign zero to <var>.  We need to use
        // an expression list statement to follow the convention of
        // other for loops and to get the codegen right.
        JVariableDefinition var = new JVariableDefinition(CStdType.Integer, "i");
        
        JExpression initExpr[] = {
            new JAssignmentExpression(new JLocalVariableExpression(var),
                                      new JIntLiteral(0)) };
        JStatement init = new JExpressionListStatement(initExpr);
        // make conditional - test if <var> less than <count>
        JExpression cond = 
            new JRelationalExpression(Constants.OPE_LT,
                                      new JLocalVariableExpression(var),
                                      new JIntLiteral(numspus));
        JExpression incrExpr = 
            new JPostfixExpression(Constants.OPE_POSTINC, 
                                   new JLocalVariableExpression(var));
        JStatement incr = new JExpressionStatement(incrExpr);
        
        JBlock body = new JBlock();
        
        body.addStatement(new JExpressionStatement(
                new JMethodCallExpression("spu_issue_group",
                        new JExpression[]{new JLocalVariableExpression(var), new JIntLiteral(1), new JFieldAccessExpression(DATA_ADDR)})));

        addInitStatement(new JForStatement(init, cond, incr, body));
        
        body = new JBlock();
        body.addStatement(new JExpressionStatement(
                new JMethodCallExpression("spulib_wait",
                        new JExpression[]{new JLocalVariableExpression(var), new JIntLiteral(1)})));
        addInitStatement(new JForStatement(init, cond, incr, body));
    }
    
    public void addSPUInit(SpaceTimeScheduleAndPartitioner schedule) {
        // spuinit()
        JExpressionStatement spuinit = new JExpressionStatement(
                new JMethodCallExpression(
                        SPUINIT,
                        new JExpression[]{}));
        addInitStatement(spuinit);
        
        // SPU_CMD_GROUP * g;
        JVariableDefinition group = new JVariableDefinition(new CEmittedTextType(SPU_CMD_GROUP), GROUP);
        JFieldDeclaration field = new JFieldDeclaration(group);
        addField(field);
        
        // SPU_ADDRESS fcb = 0;                 SPU filter control block
        JVariableDefinition fcb = new JVariableDefinition(new CEmittedTextType(SPU_ADDRESS), FCB);
        fcb.setInitializer(new JIntLiteral(0));
        field = new JFieldDeclaration(fcb);
        addField(field);
        
        // uint32_t ibs = 64 * 1024             SPU input buffer size
        JVariableDefinition ibs = new JVariableDefinition(new CEmittedTextType(UINT32_T), INPUT_BUFFER_SIZE);
        ibs.setInitializer(new JMultExpression(new JIntLiteral(64), new JIntLiteral(1024)));
        addField(new JFieldDeclaration(ibs));

        // uint32_t obs = 64 * 1024;            SPU output buffer size
        JVariableDefinition obs = new JVariableDefinition(new CEmittedTextType(UINT32_T), OUTPUT_BUFFER_SIZE);
        obs.setInitializer(new JMultExpression(new JIntLiteral(64), new JIntLiteral(1024)));
        addField(new JFieldDeclaration(obs));

        // BUFFER_CB picb;                    PPU input buffer control block
        JVariableDefinition picb = 
            new JVariableDefinition(new CEmittedTextType("BUFFER_CB"), PPU_INPUT_BUFFER_CB);
        addField(new JFieldDeclaration(picb));

        // BUFFER_CB pocb;                    PPU output buffer control block
        JVariableDefinition pocb = 
            new JVariableDefinition(new CEmittedTextType("BUFFER_CB"), PPU_OUTPUT_BUFFER_CB);
        addField(new JFieldDeclaration(pocb));
        
        // int pibs = 20 * 1024 * 1024;          PPU input buffer size     
        JVariableDefinition pibs = new JVariableDefinition(CStdType.Integer, PPU_INPUT_BUFFER_SIZE);
        pibs.setInitializer(new JMultExpression(new JIntLiteral(20),new JMultExpression(new JIntLiteral(1024), new JIntLiteral(1024))));
        addField(new JFieldDeclaration(pibs));
        
        // int pobs = 20 * 1024 * 1024;          PPU output buffer size
        JVariableDefinition pobs = new JVariableDefinition(CStdType.Integer, PPU_OUTPUT_BUFFER_SIZE);
        pobs.setInitializer(new JMultExpression(new JIntLiteral(20),new JMultExpression(new JIntLiteral(1024), new JIntLiteral(1024))));
        addField(new JFieldDeclaration(pobs));
        
        // int n;                               total number of iterations
        JVariableDefinition n = new JVariableDefinition(CStdType.Integer, N);
        addField(new JFieldDeclaration(n));
        
        // int done = numspus;
        JVariableDefinition done = new JVariableDefinition(CStdType.Integer, DONE);
        done.setInitializer(new JIntLiteral(numspus));
        addField(new JFieldDeclaration(done));

        JVariableDefinition outspuitems = new JVariableDefinition(new CArrayType(CStdType.Integer,1,new JExpression[]{new JIntLiteral(numspus)}), OUTSPUITEMS);
        addField(new JFieldDeclaration(outspuitems));
        
        JVariableDefinition iters = new JVariableDefinition(CStdType.Integer, ITERS);
        addField(new JFieldDeclaration(iters));
        
        JVariableDefinition runsperiter = new JVariableDefinition(CStdType.Integer, RUNSPERITER);
        addField(new JFieldDeclaration(runsperiter));
        
        this.schedule = schedule;
        poprate = schedule.getSchedule()[1].getFilterNodes().get(0).getFilter().getPopInt();
        pushrate = schedule.getSchedule()[1].getFilterNodes().get(0).getFilter().getPushInt();
        if (schedule.getSchedule()[0].getFilterNodes().get(0).getFilter().getOutputType().isFloatingPoint())
            inputType = "float";
        else inputType = "int";
        outputType = inputType;
        
        addInitStatement(new JExpressionStatement(new JAssignmentExpression(new JFieldAccessExpression(N), new JIntLiteral(10000))));
        addInitStatement(new JExpressionStatement(new JAssignmentExpression(new JFieldAccessExpression(RUNSPERITER), new JIntLiteral(4))));
        addInitStatement(new JExpressionStatement(new JAssignmentExpression(new JFieldAccessExpression(ITERS), 
                new JDivideExpression(null, new JFieldAccessExpression(N), new JFieldAccessExpression(RUNSPERITER)))));
        
//        JMethodDeclaration init_ticks = new JMethodDeclaration(CStdType.Void, INIT_TICKS, new JFormalParameter[0], new JBlock());
//        addMethod(init_ticks);
//        JVariableDefinition init_ticks = new JVariableDefinition(CStdType.Void, INIT_TICKS);
//        init_ticks.setInitializer(null);
//        addField(new JFieldDeclaration(init_ticks));
//        
//        JVariableDefinition ticks = new JVariableDefinition(CStdType.Void, TICKS);
//        ticks.setInitializer(null);
//        addField(new JFieldDeclaration(ticks));
        
        JVariableDefinition start = new JVariableDefinition(CStdType.Integer, START);
        addField(new JFieldDeclaration(start));
        
        JVariableDefinition startspu = new JVariableDefinition(CStdType.Integer, STARTSPU);
        addField(new JFieldDeclaration(startspu));
        
        JVariableDefinition numspus = new JVariableDefinition(CStdType.Integer, "numspus");
        addField(new JFieldDeclaration(numspus));
        addInitStatement(new JExpressionStatement(new JAssignmentExpression(new JFieldAccessExpression("numspus"), new JIntLiteral(this.numspus))));
        
        newline();
    }
    
    public void initSpulibClock() {
        //addInitStatement(new JExpressionStatement(new JMethodCallExpression(INIT_TICKS, new JExpression[0])));
        //addInitStatement(new JExpressionStatement(new JAssignmentExpression(new JFieldAccessExpression(START), new JMethodCallExpression(TICKS, new JExpression[0]))));
        
        // spulib_init()
        JExpressionStatement spulibinit = new JExpressionStatement(
                new JMethodCallExpression(
                        SPULIB_INIT,
                        new JExpression[]{}));
        addInitStatement(spulibinit);
    }
    
    public void addStartSpuTicks() {
        addInitStatement(new JExpressionStatement(new JAssignmentExpression(new JFieldAccessExpression(STARTSPU), new JMethodCallExpression(TICKS, new JExpression[0]))));
    }
    
    public void addPrintTicks() {
        addInitStatement(new JExpressionStatement(new JMethodCallExpression(PRINTF,
                new JExpression[]{new JStringLiteral("spu time: %d ms\\n"),
                new JMinusExpression(null, new JMethodCallExpression(TICKS, new JExpression[0]), new JFieldAccessExpression(STARTSPU))})));
        addInitStatement(new JExpressionStatement(new JMethodCallExpression(PRINTF,
                new JExpression[]{new JStringLiteral("time: %d ms\\n"),
                new JMinusExpression(null, new JMethodCallExpression(TICKS, new JExpression[0]), new JFieldAccessExpression(START))})));
        
    }
    
    public void addFilterLoad(InputSliceNode inputNode) {
        Integer id = getIdForSlice(inputNode.getParent());
        String fd = idToFd(id);
        String group = GROUP + id;
        JExpressionStatement filterLoad = new JExpressionStatement(new JMethodCallExpression(
                SPU_FILTER_LOAD,
                new JExpression[]{
                        new JFieldAccessExpression(group),
                        new JFieldAccessExpression(FCB),
                        new JEmittedTextExpression("&" + fd),
                        new JIntLiteral(0),
                        new JIntLiteral(NO_DEPS)
                }));
        addInitStatement(filterLoad);
    }
    
    /**
     * Add fields for SPU input buffers, and set up their addresses.
     * @param inputNode
     */
    public void setupInputBufferAddress() {

        JVariableDefinition iba = new JVariableDefinition(
                new CEmittedTextType(SPU_ADDRESS), INPUT_BUFFER_ADDR);
        JFieldDeclaration field = new JFieldDeclaration(iba);
        addField(field);
        
        // iba = fcb + 128
        JExpressionStatement expr = new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(INPUT_BUFFER_ADDR),
                new JAddExpression(new JFieldAccessExpression(FCB),
                        new JIntLiteral(FCB_SIZE))));
        addInitStatement(expr);
        
    }
    
    public void setupOutputBufferAddress() {

        JVariableDefinition oba = new JVariableDefinition(
                new CEmittedTextType(SPU_ADDRESS), OUTPUT_BUFFER_ADDR);
        JFieldDeclaration field = new JFieldDeclaration(oba);
        addField(field);

        // oba = iba + ibs + 128;
        JExpressionStatement expr = new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(OUTPUT_BUFFER_ADDR),
                new JAddExpression(new JFieldAccessExpression(INPUT_BUFFER_ADDR),
                        new JAddExpression(new JFieldAccessExpression(INPUT_BUFFER_SIZE), new JIntLiteral(FILLER)))));
        addInitStatement(expr);
        
    }
    
    public void addSPUIters() {
        JForStatement spuiters;
        // make init statement - assign zero to <var>.  We need to use
        // an expression list statement to follow the convention of
        // other for loops and to get the codegen right.
        JVariableDefinition var = new JVariableDefinition(CStdType.Integer, "i");
        
        JExpression initExpr[] = {
            new JAssignmentExpression(new JLocalVariableExpression(var),
                                      new JIntLiteral(0)) };
        JStatement init = new JExpressionListStatement(initExpr);
        // make conditional - test if <var> less than <count>
        JExpression cond = 
            new JRelationalExpression(Constants.OPE_LT,
                                      new JLocalVariableExpression(var),
                                      new JIntLiteral(numspus-1));
        JExpression incrExpr = 
            new JPostfixExpression(Constants.OPE_POSTINC, 
                                   new JLocalVariableExpression(var));
        JStatement incr = new JExpressionStatement(incrExpr);
        
        JBlock body = new JBlock();
        
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(new JArrayAccessExpression(new JFieldAccessExpression(SPUITERS),new JLocalVariableExpression(var)),
                new JDivideExpression(null, new JFieldAccessExpression(N), new JIntLiteral(numspus)))));
        
        spuiters = new JForStatement(init, cond, incr, body);
        addInitStatement(spuiters);
        addInitStatement(new JExpressionStatement(new JAssignmentExpression(new JArrayAccessExpression(new JFieldAccessExpression(SPUITERS),new JIntLiteral(numspus-1)),
                new JMinusExpression(null, new JFieldAccessExpression(N), 
                        new JMultExpression(new JArrayAccessExpression(new JFieldAccessExpression(SPUITERS),new JIntLiteral(0)),
                                            new JIntLiteral(numspus-1))))));
        newline();
    }
    
    /**
     * Initialize an array of input buffers for this slice and attaches each
     * input buffer to the appropriate upstream channel.
     * @param filterId
     * @param inputIds
     */
    public void attachInputChannelArray(int filterId,
            LinkedList<Integer> inputIds) {
        if (inputIds == null)
            return;
        addField(new JFieldDeclaration(new JVariableDefinition(
                new CArrayType(new CEmittedTextType("BUFFER_CB *"),
                        1,
                        new JExpression[]{new JIntLiteral(inputIds.size())}),
                        "input_" + filterId)));
        for (int i=0; i<inputIds.size(); i++) {
            addInitStatement(new JExpressionStatement(new JAssignmentExpression(
                    new JArrayAccessExpression(
                            new JFieldAccessExpression("input_" + filterId),
                            new JIntLiteral(i)),
                    new JMethodCallExpression(
                            "&",
                            new JExpression[]{
                                    new JArrayAccessExpression(
                                            new JFieldAccessExpression("channels"),
                                            new JIntLiteral(inputIds.get(i)))
                            }))));
        }
    }
    
    /**
     * Initialize an array of output buffers for this slice and attaches each
     * output buffer to the appropriate downstream channel.
     * @param filterId
     * @param outputIds
     */
    public void attachOutputChannelArray(int filterId,
            LinkedList<Integer> outputIds) {
        if (outputIds == null)
            return;
        addField(new JFieldDeclaration(new JVariableDefinition(
                new CArrayType(new CEmittedTextType("BUFFER_CB *"),
                        1,
                        new JExpression[]{new JIntLiteral(outputIds.size())}),
                        "output_" + filterId)));
        for (int i=0; i<outputIds.size(); i++) {
            addInitStatement(new JExpressionStatement(new JAssignmentExpression(
                    new JArrayAccessExpression(
                            new JFieldAccessExpression("output_" + filterId),
                            new JIntLiteral(i)),
                            new JMethodCallExpression(
                                    "&",
                                    new JExpression[]{
                                            new JArrayAccessExpression(
                                                    new JFieldAccessExpression("channels"),
                                                    new JIntLiteral(outputIds.get(i)))
                                    }))));            
        }

    }
    
    public void initDuplicateOutputChannelArray(int filterId) {
        addField(new JFieldDeclaration(new JVariableDefinition(
                new CArrayType(new CEmittedTextType("BUFFER_CB *"),
                        1,
                        new JExpression[]{new JIntLiteral(1)}),
                        "output_" + filterId)));
    }
    
    public void initChannelArrays(FilterSliceNode filterNode,
            LinkedList<Integer> inputnums, LinkedList<Integer> outputnums) {
        InputSliceNode input = filterNode.getParent().getHead();
        OutputSliceNode output = filterNode.getParent().getTail();
        //initInputChannelArray(input, inputnums);
        //initOutputChannelArray(output, outputnums);
    }
    
    /**
     * Initialize the channel with this ID and allocate the buffer
     * @param channelId
     */
    public void initChannel(int channelId) {
        addInitStatement(new JExpressionStatement(
                new JMethodCallExpression(
                        "init_buffer",
                        new JExpression[]{
                                new JMethodCallExpression("&",new JExpression[]{new JArrayAccessExpression(
                                        new JFieldAccessExpression("channels"),new JIntLiteral(channelId))}),
                                new JEmittedTextExpression("NULL"),
                                new JFieldAccessExpression(PPU_INPUT_BUFFER_SIZE),
                                new JEmittedTextExpression("FALSE"),
                                new JIntLiteral(0)})));
    }
    
    public void addPPUInputBuffers(String inputName) {
        addInitStatement(new JExpressionStatement(
                new JMethodCallExpression(
                        "init_buffer",
                        new JExpression[]{
                                new JMethodCallExpression("&",new JExpression[]{new JFieldAccessExpression(inputName)}),
                                new JEmittedTextExpression("NULL"),
                                new JFieldAccessExpression(PPU_INPUT_BUFFER_SIZE),
                                new JEmittedTextExpression("FALSE"),
                                new JIntLiteral(0)})));
        newline();
    }
    
    public void addPPUOutputBuffers(String outputName) {
        addInitStatement(new JExpressionStatement(
                new JMethodCallExpression(
                        "init_buffer",
                        new JExpression[]{
                                new JMethodCallExpression("&",new JExpression[]{new JFieldAccessExpression(outputName)}),
                                new JEmittedTextExpression("NULL"),
                                new JFieldAccessExpression(PPU_OUTPUT_BUFFER_SIZE),
                                new JEmittedTextExpression("FALSE"),
                                new JIntLiteral(0)})));
    }
    
    public boolean lookupInputBuffers(InputSliceNode inputNode) {
        String filtername = inputNode.getNextFilter().getFilter().getName();
//        addField(new JFieldDeclaration(new JVariableDefinition(
//                new CArrayType(new CEmittedTextType("BUFFER_CB"),
//                               1,
//                               new JExpression[]{new JIntLiteral(inputNode.getWidth())}),
//                               "input_" + filtername)));
        int i=0;
        for (InterSliceEdge e : inputNode.getSourceSequence()) {
            InterSliceEdge f = getEdgeBetween(e.getSrc(), inputNode);
            if (!readyInputs.containsKey(f)) {
                System.out.println("no input ready for " + filtername + f);
                return false;
            }
            Buffer b = readyInputs.get(f);
            int index;
            if (f.getSrc().isDuplicateSplitter()) {
                index = 0;
            } else {
                index = b.index;
            }
            addInitStatement(new JExpressionStatement(new JMethodCallExpression(
                    "duplicate_buffer",
                    new JExpression[]{
                        new JMethodCallExpression(
                                "&",
                                new JExpression[]{
                                    new JArrayAccessExpression(
                                        new JFieldAccessExpression("input_"+filtername),
                                        new JIntLiteral(i))}),
                        new JMethodCallExpression(
                                "&",
                                new JExpression[]{
                                    new JArrayAccessExpression(
                                        new JFieldAccessExpression(b.name),
                                        new JIntLiteral(index))})})));
            addInitStatement(new JExpressionStatement(new JMethodCallExpression(
                    "buf_set_head",
                    new JExpression[]{
                            new JMethodCallExpression(
                                "&",
                                new JExpression[]{
                                    new JArrayAccessExpression(
                                        new JFieldAccessExpression("input_"+filtername),
                                        new JIntLiteral(i))}),
                            new JIntLiteral(0)})));
            i++;
        }
        if (inputNode.getSources().length > 0) {
            addPPUInputBuffers("input_" + filtername);
        }
        return true;
    }
    
    public void initOutputBufferFields(InputSliceNode inputNode) {
        OutputSliceNode outputNode = inputNode.getParent().getTail();
        String filtername = inputNode.getNextFilter().getFilter().getName();
        int outputs;
        if (outputNode.isDuplicateSplitter())
            outputs = 1;
        else outputs = outputNode.getWidth();
        addField(new JFieldDeclaration(new JVariableDefinition(
                new CArrayType(new CEmittedTextType("BUFFER_CB"),
                               1,
                               new JExpression[]{new JIntLiteral(outputs)}),
                               "output_" + filtername)));
        if(outputNode.getWidth() > 0)
            addPPUOutputBuffers("output_" + filtername);
    }
    
    public void addReadyBuffers(OutputSliceNode outputNode) {
        String filtername = outputNode.getPrevFilter().getFilter().getName();
        int i=0;
        System.out.println("addreadybuffers" + filtername);
        for (InterSliceEdge e : outputNode.getDestSequence()) {
            Buffer b = new Buffer("output_" + filtername, i);
            readyInputs.put(e, b);
            System.out.println("adding buffer: " + e + "\n" + b + "\n" + e.getDest().getNextFilter().getFilter().getName());
            i++;
        }
    }
    
    public void setupDataAddress() {
        // da = oba + obs + 128;
        JExpressionStatement expr = new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(DATA_ADDR),
                new JAddExpression(
                        new JAddExpression(new JFieldAccessExpression(OUTPUT_BUFFER_ADDR), 
                                           new JFieldAccessExpression(OUTPUT_BUFFER_SIZE)),
                        new JIntLiteral(FILLER))));
        addInitStatement(expr);
        newline();
    }
    
    public void startNewFilter(InputSliceNode inputNode) {
        Integer id = getIdForSlice(inputNode.getParent());
        String fd = idToFd(id);
        
        JExpressionStatement newline = new JExpressionStatement(new JEmittedTextExpression(""));
        addInitStatement(newline);
        JExpressionStatement newFilter = new JExpressionStatement(new JEmittedTextExpression("// set up code for filter " + id));
        addInitStatement(newFilter);
    }
    
    public void addNewGroupStatement() {

        JForStatement newgroup;
        // make init statement - assign zero to <var>.  We need to use
        // an expression list statement to follow the convention of
        // other for loops and to get the codegen right.
        JVariableDefinition var = new JVariableDefinition(CStdType.Integer, "i");
        
        JExpression initExpr[] = {
            new JAssignmentExpression(new JLocalVariableExpression(var),
                                      new JIntLiteral(0)) };
        JStatement init = new JExpressionListStatement(initExpr);
        // make conditional - test if <var> less than <count>
        JExpression cond = 
            new JRelationalExpression(Constants.OPE_LT,
                                      new JLocalVariableExpression(var),
                                      new JIntLiteral(numspus));
        JExpression incrExpr = 
            new JPostfixExpression(Constants.OPE_POSTINC, 
                                   new JLocalVariableExpression(var));
        JStatement incr = new JExpressionStatement(incrExpr);
        
        JBlock body = new JBlock();
        
        JExpressionStatement newGroup = new JExpressionStatement(new JAssignmentExpression(
                new JEmittedTextExpression(GROUP),
                new JMethodCallExpression(SPU_NEW_GROUP,new JExpression[]{new JLocalVariableExpression(var), new JIntLiteral(0)})));
        
        body.addStatement(newGroup);
        
        JExpressionStatement filterLoad = new JExpressionStatement(new JMethodCallExpression(
                SPU_FILTER_LOAD,
                new JExpression[]{
                        new JFieldAccessExpression(GROUP),
                        new JFieldAccessExpression(FCB),
                        new JMethodCallExpression("&",new JExpression[]{new JArrayAccessExpression(new JFieldAccessExpression(SPU_FD),new JLocalVariableExpression(var))}),
                        new JIntLiteral(0),
                        new JIntLiteral(NO_DEPS)
                }));
        body.addStatement(filterLoad);
        
        int cmdId = 1;
        String buffaddr = INPUT_BUFFER_ADDR;
        String buffsize = INPUT_BUFFER_SIZE;
        JExpressionStatement bufferAlloc = new JExpressionStatement(new JMethodCallExpression(
                SPU_BUFFER_ALLOC, new JExpression[]{
                        new JFieldAccessExpression(GROUP), new JFieldAccessExpression(buffaddr), new JFieldAccessExpression(buffsize),
                        new JIntLiteral(BUFFER_OFFSET), new JIntLiteral(cmdId), new JIntLiteral(NO_DEPS)}));
        body.addStatement(bufferAlloc);
        JExpressionStatement attachBuffer = new JExpressionStatement(new JMethodCallExpression(
                SPU_FILTER_ATTACH_INPUT, new JExpression[]{
                        new JFieldAccessExpression(GROUP),
                        new JFieldAccessExpression(FCB),
                        new JIntLiteral(0),
                        new JFieldAccessExpression(buffaddr),
                        new JIntLiteral(cmdId + 1),
                        new JIntLiteral(2),
                        new JIntLiteral(0),
                        new JIntLiteral(cmdId)
                }));
        body.addStatement(attachBuffer);
        
        cmdId = 3;
        buffaddr = OUTPUT_BUFFER_ADDR;
        buffsize = OUTPUT_BUFFER_SIZE;
        bufferAlloc = new JExpressionStatement(new JMethodCallExpression(
                SPU_BUFFER_ALLOC, new JExpression[]{
                        new JFieldAccessExpression(GROUP), new JFieldAccessExpression(buffaddr), new JFieldAccessExpression(buffsize),
                        new JIntLiteral(BUFFER_OFFSET), new JIntLiteral(cmdId), new JIntLiteral(NO_DEPS)}));
        body.addStatement(bufferAlloc);
        attachBuffer = new JExpressionStatement(new JMethodCallExpression(
                SPU_FILTER_ATTACH_OUTPUT, new JExpression[]{
                        new JFieldAccessExpression(GROUP),
                        new JFieldAccessExpression(FCB),
                        new JIntLiteral(0),
                        new JEmittedTextExpression(buffaddr),
                        new JIntLiteral(cmdId + 1),
                        new JIntLiteral(2),
                        new JIntLiteral(0),
                        new JIntLiteral(cmdId)
                }));
        body.addStatement(attachBuffer);
        
        cmdId = 5;
        body.addStatement(new JExpressionStatement(new JMethodCallExpression(
                SPU_CALL_FUNC, new JExpression[]{
                        new JFieldAccessExpression(GROUP),
                        new JArrayAccessExpression(new JFieldAccessExpression(INIT_FUNC),new JLocalVariableExpression(var)),
                        new JIntLiteral(cmdId),
                        new JIntLiteral(2),
                        new JIntLiteral(cmdId - 1),
                        new JIntLiteral(cmdId - 3)
                })));
        
        newGroup = new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(GROUP),
                new JMethodCallExpression(SPU_NEW_GROUP,new JExpression[]{new JLocalVariableExpression(var), new JIntLiteral(1)})));
        body.addStatement(newGroup);
        
        JExpressionStatement filterUnload = new JExpressionStatement(new JMethodCallExpression(
                SPU_FILTER_UNLOAD, new JExpression[]{
                        new JFieldAccessExpression(GROUP),
                        new JFieldAccessExpression(FCB),
                        new JIntLiteral(0),
                        new JIntLiteral(NO_DEPS)
                }));
        body.addStatement(filterUnload);
        
        JForStatement forloop = new JForStatement(init, cond, incr, body);
        
        addInitStatement(forloop);
    }
    
    public void addNewGroupAndFilterUnload(OutputSliceNode outputNode) {
        Integer id = getIdForSlice(outputNode.getParent());
        String group = GROUP;
        
        JExpressionStatement newGroup = new JExpressionStatement(new JAssignmentExpression(
                new JEmittedTextExpression(group),
                new JMethodCallExpression(SPU_NEW_GROUP,new JExpression[]{new JIntLiteral(id), new JIntLiteral(1)})));
        addInitStatement(newGroup);
        
        JExpressionStatement filterUnload = new JExpressionStatement(new JMethodCallExpression(
                SPU_FILTER_UNLOAD, new JExpression[]{
                        new JEmittedTextExpression(group),
                        new JFieldAccessExpression(FCB),
                        new JIntLiteral(0),
                        new JIntLiteral(NO_DEPS)
                }));
        addInitStatement(filterUnload);
    }
    
    public void addInputBufferAllocAttach(InputSliceNode inputNode) {
        Integer id = getIdForSlice(inputNode.getParent());
        String group = GROUP;
        
        int cmdId = 1;
        for (int i=0; i<inputNode.getWidth(); i++) {
            String buffaddr = INPUT_BUFFER_ADDR + id + "_" + i;
            String buffsize = INPUT_BUFFER_SIZE;
            JExpressionStatement bufferAlloc = new JExpressionStatement(new JMethodCallExpression(
                    SPU_BUFFER_ALLOC, new JExpression[]{
                            new JEmittedTextExpression(group), new JEmittedTextExpression(buffaddr), new JFieldAccessExpression(buffsize),
                            new JIntLiteral(BUFFER_OFFSET), new JIntLiteral(cmdId), new JIntLiteral(NO_DEPS)}));
            addInitStatement(bufferAlloc);
            JExpressionStatement attachBuffer = new JExpressionStatement(new JMethodCallExpression(
                    SPU_FILTER_ATTACH_INPUT, new JExpression[]{
                            new JEmittedTextExpression(group),
                            new JFieldAccessExpression(FCB),
                            new JIntLiteral(i),
                            new JEmittedTextExpression(buffaddr),
                            new JIntLiteral(cmdId + inputNode.getWidth()),
                            new JIntLiteral(2),
                            new JIntLiteral(0),
                            new JIntLiteral(cmdId)
                    }));
            addInitStatement(attachBuffer);
            cmdId++;
        }   
    }
    
    public void addOutputBufferAllocAttach(OutputSliceNode outputNode) {
        Integer id = getIdForSlice(outputNode.getParent());
        String group = GROUP;
        
        int cmdId = 2*outputNode.getParent().getHead().getWidth() + 1;
        for (int i=0; i<outputNode.getWidth(); i++) {
            String buffaddr = OUTPUT_BUFFER_ADDR + id + "_" + i;
            String buffsize = OUTPUT_BUFFER_SIZE;
            JExpressionStatement bufferAlloc = new JExpressionStatement(new JMethodCallExpression(
                    SPU_BUFFER_ALLOC, new JExpression[]{
                            new JEmittedTextExpression(group), new JEmittedTextExpression(buffaddr), new JFieldAccessExpression(buffsize),
                            new JIntLiteral(BUFFER_OFFSET), new JIntLiteral(cmdId), new JIntLiteral(NO_DEPS)}));
            addInitStatement(bufferAlloc);
            JExpressionStatement attachBuffer = new JExpressionStatement(new JMethodCallExpression(
                    SPU_FILTER_ATTACH_OUTPUT, new JExpression[]{
                            new JEmittedTextExpression(group),
                            new JFieldAccessExpression(FCB),
                            new JIntLiteral(i),
                            new JEmittedTextExpression(buffaddr),
                            new JIntLiteral(cmdId + outputNode.getWidth()),
                            new JIntLiteral(2),
                            new JIntLiteral(0),
                            new JIntLiteral(cmdId)
                    }));
            addInitStatement(attachBuffer);
            cmdId++;
        }   
    }
    
    public void addIssueGroupAndWait() {
        // make init statement - assign zero to <var>.  We need to use
        // an expression list statement to follow the convention of
        // other for loops and to get the codegen right.
        JVariableDefinition var = new JVariableDefinition(CStdType.Integer, "i");
        
        JExpression initExpr[] = {
            new JAssignmentExpression(new JLocalVariableExpression(var),
                                      new JIntLiteral(0)) };
        JStatement init = new JExpressionListStatement(initExpr);
        // make conditional - test if <var> less than <count>
        JExpression cond = 
            new JRelationalExpression(Constants.OPE_LT,
                                      new JLocalVariableExpression(var),
                                      new JIntLiteral(numspus));
        JExpression incrExpr = 
            new JPostfixExpression(Constants.OPE_POSTINC, 
                                   new JLocalVariableExpression(var));
        JStatement incr = new JExpressionStatement(incrExpr);
        
        JBlock body = new JBlock();
        
        JExpressionStatement expr = new JExpressionStatement(new JMethodCallExpression(
                SPU_ISSUE_GROUP, new JExpression[]{
                        new JLocalVariableExpression(var),
                        new JIntLiteral(0),
                        new JFieldAccessExpression(DATA_ADDR)}));
        body.addStatement(expr);
        
        JForStatement forloop = new JForStatement(init, cond, incr, body);
        addInitStatement(forloop);
        
        body = new JBlock();
        JExpressionStatement wait = new JExpressionStatement(new JMethodCallExpression(
                SPULIB_WAIT, new JExpression[]{
                        new JLocalVariableExpression(var),
                        new JEmittedTextExpression(WAIT_MASK)}));
        body.addStatement(wait);
        
        addInitStatement(new JForStatement(init, cond, incr, body));
    }
 
    public void addFileReader(FilterSliceNode filterNode) {
        //initFileReader(filterNode);
        JBlock body = makeReadBlock(filterNode);
        addInitStatement(body);
    }
    
    public void initFileReader(FilterSliceNode filterNode) {
                
        FileInputContent fic = (FileInputContent) filterNode.getFilter();
        
        //create fields
        JFieldDeclaration file = 
            new JFieldDeclaration(null,
                                  new JVariableDefinition(null,
                                                          0,
                                                          new CEmittedTextType("FILE *"),
                                                          FILE_READER,
                                                          null),
                                  null, null);
        addField(file);
        //create the file open command
        JExpression[] params = {
            new JStringLiteral(null, fic.getFileName()),
            new JStringLiteral(null, "r")
        };
        
        JMethodCallExpression fopen = 
            new JMethodCallExpression(null, new JThisExpression(null),
                                      "fopen", params);
        //assign to the file handle
        JAssignmentExpression fass = 
            new JAssignmentExpression(null, 
                                      new JFieldAccessExpression(null,
                                                                 new JThisExpression(null),
                                                                 file.getVariable().getIdent()),
                                      fopen);
    
        addInitStatement(new JExpressionStatement(null, fass, null));
        // do some standard C error checking here.
        // do we need to put this in a separate method to allow subclass to override?
        addInitStatement(new JExpressionStatement(
                new JEmittedTextExpression(new Object[]{
                        "if (",
                        new JFieldAccessExpression(
                                new JThisExpression(null),
                                file.getVariable().getIdent()),
                        " == NULL) { perror(\"error opening "+ fic.getFileName()+ "\"); }"
                })));
    }
    
    private JBlock makeReadBlock(FilterSliceNode filterNode) {
        
        FileInputContent fic = (FileInputContent) filterNode.getFilter();
        
        JBlock readBlock = new JBlock();
        ALocalVariable tmp = ALocalVariable.makeTmp(fic.getOutputType());
        addInitStatement(tmp.getDecl());
        
        // RMR { support ascii or binary file operations for reading
        JMethodCallExpression fileio;

        if (KjcOptions.asciifileio) {
            //create a temp variable to hold the value we are reading
            JExpression[] fscanfParams = new JExpression[3];
            //create the params for fscanf
            fscanfParams[0] = new JFieldAccessExpression(FILE_READER);
            fscanfParams[1] = new JStringLiteral(null,
                                                 fic.getOutputType().isFloatingPoint() ?
                                                 "%f\\n" : "%d\\n");
            fscanfParams[2] = tmp.getRef();
            
            //fscanf call
            JMethodCallExpression fscanf = 
                new JMethodCallExpression(null, new JThisExpression(null),
                        "fscanf",
                        fscanfParams);        
            
            fileio = fscanf;
        }
        else {
            // create the params for fread(picb.data, sizeof(type), items, file)
            JExpression[] freadParams = new JExpression[4];
            
            // the first parameter: picb.data
            JExpression addressofParameters = 
                new JFieldAccessExpression(new JFieldAccessExpression(PPU_INPUT_BUFFER_CB), "data");
              
            freadParams[0] = addressofParameters;
            
            // the second parameter: the call to sizeof(type)
            JExpression[] sizeofParameters = new JExpression[1];
            
            sizeofParameters[0] = 
                new JLocalVariableExpression(null, 
                                             new JVariableDefinition(null, 0,
                                                                     CStdType.Integer,
                                                                     (fic.getOutputType().isFloatingPoint() ? 
                                                                      "float" : "int"),
                                                                     null));
            
            JMethodCallExpression sizeofCall =
                new JMethodCallExpression(null, "sizeof", sizeofParameters);
            
            freadParams[1] = sizeofCall;
            
            // the third parameter: read steadymult * N elements at a time
            freadParams[2] = 
                new JMultExpression(
                        new JIntLiteral(filterNode.getFilter().getSteadyMult()),
                        new JFieldAccessExpression(N));
            
            // the last parameter: the file pointer
            freadParams[3] = new JFieldAccessExpression(FILE_READER);
            
            JMethodCallExpression fread = 
                new JMethodCallExpression(null, new JThisExpression(null),
                                          "fread",
                                          freadParams);

            fileio = fread;
        }

        readBlock.addStatement(new JExpressionStatement(fileio));
        readBlock.addStatement(new JExpressionStatement(
                new JMethodCallExpression("buf_inc_tail",
                        new JExpression[]{
                            new JMethodCallExpression("&",
                                    new JExpression[]{new JEmittedTextExpression(PPU_INPUT_BUFFER_CB)}),
                            new JMultExpression(new JMultExpression(
                                                    new JIntLiteral(filterNode.getFilter().getSteadyMult()),
                                                    new JFieldAccessExpression(N)), 
                                                new JIntLiteral(4))
                })));
        return readBlock;
    }
    
    public void addFileWriter(FilterSliceNode filterNode) {
        
        //initFileWriter(filterNode);
        JBlock body = makeWriteBlock(filterNode);
        addInitStatement(body);

        addInitStatement(new JExpressionStatement(
            new JMethodCallExpression("fclose", new JExpression[]{
                new JFieldAccessExpression(FILE_READER)})));

        addInitStatement(new JExpressionStatement(
            new JMethodCallExpression("fclose", new JExpression[]{
                new JFieldAccessExpression(FILE_WRITER)})));
    }
    
    public void initFileWriter(FilterSliceNode filterNode) {
    
        FileOutputContent foc = (FileOutputContent) filterNode.getFilter();
        
        JVariableDefinition fileDefn = new JVariableDefinition(null,
                0,
                new CEmittedTextType("FILE *"),
                FILE_WRITER,
                null);
        //create fields
        JFieldDeclaration file = 
            new JFieldDeclaration(null,fileDefn, null, null);
        addField(file);
    
        //create the file open command
        JExpression[] params = 
            {
                new JStringLiteral(null, foc.getFileName()),
                new JStringLiteral(null, "w")
            };
    
        JMethodCallExpression fopen = 
            new JMethodCallExpression(null, new JThisExpression(null),
                                      "fopen", params);
        //assign to the file handle
        JAssignmentExpression fass = 
            new JAssignmentExpression(null, 
                                      new JFieldAccessExpression(null,
                                                                 new JThisExpression(null),
                                                                 fileDefn.getIdent()),
                                      fopen);
    
        addInitStatement(new JExpressionStatement(null, fass, null));
        
        // do some standard C error checking here.
        // do we need to put this in a separate method to allow subclass to override?
        addInitStatement(new JExpressionStatement(
                new JEmittedTextExpression(new Object[]{
                        "if (",
                        new JFieldAccessExpression(
                                new JThisExpression(null),
                                file.getVariable().getIdent()),
                        " == NULL) { perror(\"error opening "+ foc.getFileName() + "\"); }"
                })));
    }
    
    private JBlock makeWriteBlock(FilterSliceNode filterNode) {

        FileOutputContent foc = (FileOutputContent) filterNode.getFilter();

        JBlock writeBlock = new JBlock();
        ALocalVariable tmp = ALocalVariable.makeTmp(foc.getInputType());
        addInitStatement(tmp.getDecl());

        // RMR { support ascii or binary file operations for reading
        JMethodCallExpression fileio;

        if (KjcOptions.asciifileio) {
            //the params for the fprintf call
            JExpression[] fprintfParams = new JExpression[3];
            fprintfParams[0] = new JFieldAccessExpression(FILE_WRITER);
            fprintfParams[1] = new JStringLiteral(null,
                                                  foc.getInputType().isFloatingPoint() ?
                                                  "%f\\n" : "%d\\n");
            fprintfParams[2] = tmp.getRef();
            
            JMethodCallExpression fprintf = 
                new JMethodCallExpression(null, new JThisExpression(null),
                                          "fprintf",
                                          fprintfParams);
            fileio = fprintf;
        }
        else {
            
            // create the params for fwrite(pocb.data, sizeof(type), items, file)
            JExpression[] fwriteParams = new JExpression[4];
            
            // the first parameter: poba[i]
            JExpression[] addressofParameters = new JExpression[1];
            
            JFieldAccessExpression addressofCall =
                new JFieldAccessExpression(new JFieldAccessExpression(PPU_OUTPUT_BUFFER_CB),"data");

            fwriteParams[0] = addressofCall;
            
            // the second parameter: the call to sizeof(type)
            JExpression[] sizeofParameters = new JExpression[1];
            
            sizeofParameters[0] = 
                new JLocalVariableExpression(null, 
                                             new JVariableDefinition(null, 0,
                                                                     CStdType.Integer,
                                                                     (foc.getInputType().isFloatingPoint() ? 
                                                                      "float" : "int"),
                                                                     null));
            
            JMethodCallExpression sizeofCall =
                new JMethodCallExpression(null, "sizeof", sizeofParameters);
            sizeofCall.setTapeType(CStdType.Integer);
            
            fwriteParams[1] = sizeofCall;
            
            // the third parameter: write steadymult*N elements at a time
            fwriteParams[2] = new JMultExpression(
                    new JIntLiteral(filterNode.getFilter().getSteadyMult()),
                    new JFieldAccessExpression(N));
            
            // the last parameter: the file pointer
            fwriteParams[3] = new JFieldAccessExpression(FILE_WRITER);
            
            JMethodCallExpression fwrite = 
                new JMethodCallExpression(null, new JThisExpression(null),
                                          "fwrite",
                                          fwriteParams);
            fwrite.setTapeType(CStdType.Void);

            fileio = fwrite;
        }
        
        writeBlock.addStatement(new JExpressionStatement(fileio));    
        return writeBlock;
    }
    
    public void dynamic() {
        JBlock block = new JBlock();
        int numfilters = CellBackend.numfilters;
        int numchannels = CellBackend.numchannels;
        addField(new JFieldDeclaration(new JVariableDefinition(new CEmittedTextType("FILTER *"),"f")));
        JVariableDefinition filters = new JVariableDefinition(
                new CArrayType(new CEmittedTextType("FILTER"),1,new JExpression[]{new JIntLiteral(numfilters)}), "filters");
        JFieldDeclaration field = new JFieldDeclaration(filters);
        addField(field);
        
        JVariableDefinition channels = new JVariableDefinition(
                new CArrayType(new CEmittedTextType("CHANNEL"),1,new JExpression[]{new JIntLiteral(numchannels)}), "channels");
        field = new JFieldDeclaration(channels);
        addField(field);
        
        JExpressionStatement spuinit = new JExpressionStatement(
                new JMethodCallExpression(
                        SPUINIT,
                        new JExpression[]{}));
        block.addStatement(spuinit);
        
        JExpressionStatement num_filters = new JExpressionStatement(
                new JAssignmentExpression(new JFieldAccessExpression("num_filters"),
                        new JIntLiteral(numfilters)));
        block.addStatement(num_filters);
        
        JExpressionStatement num_channels = new JExpressionStatement(
                new JAssignmentExpression(new JFieldAccessExpression("num_channels"),
                        new JIntLiteral(numchannels)));
        block.addStatement(num_channels);
        
        JExpressionStatement num_spu = new JExpressionStatement(
                new JAssignmentExpression(new JFieldAccessExpression("num_spu"),
                        new JIntLiteral(numspus)));
        block.addStatement(num_spu);
        
        JVariableDefinition n = new JVariableDefinition(CStdType.Integer, N);
        addField(new JFieldDeclaration(n));
        block.addStatement(new JExpressionStatement(new JAssignmentExpression(new JFieldAccessExpression(N), new JIntLiteral(10000))));
        
        JForStatement channelbufsize;
        // make init statement - assign zero to <var>.  We need to use
        // an expression list statement to follow the convention of
        // other for loops and to get the codegen right.
        JVariableDefinition var = new JVariableDefinition(CStdType.Integer, "i");
        block.addStatement(new JVariableDeclarationStatement(var));
        
        JExpression initExpr[] = {
            new JAssignmentExpression(new JLocalVariableExpression(var),
                                      new JIntLiteral(1)) };
        JStatement init = new JExpressionListStatement(initExpr);
        // make conditional - test if <var> less than <count>
        JExpression cond = 
            new JRelationalExpression(Constants.OPE_LT,
                                      new JLocalVariableExpression(var),
                                      new JIntLiteral(numchannels - 1));
        JExpression incrExpr = 
            new JPostfixExpression(Constants.OPE_POSTINC, 
                                   new JLocalVariableExpression(var));
        JStatement incr = new JExpressionStatement(incrExpr);
        
        JBlock body = new JBlock();
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(
                        new JArrayAccessExpression(new JFieldAccessExpression("channels"), new JLocalVariableExpression(var)),
                        "buf_size"),
                new JIntLiteral(1024*1024)
                )));
        channelbufsize = new JForStatement(init, cond, incr, body);
        block.addStatement(channelbufsize);
        
        block.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(
                        new JArrayAccessExpression(new JFieldAccessExpression("channels"), new JIntLiteral(0)),
                        "buf_size"),
                new JMultExpression(new JIntLiteral(2048), new JFieldAccessExpression(N))
                )));
        block.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(
                        new JArrayAccessExpression(new JFieldAccessExpression("channels"), new JIntLiteral(numchannels - 1)),
                        "buf_size"),
                new JMultExpression(new JIntLiteral(2048), new JFieldAccessExpression(N))
                )));
        block.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(
                        new JArrayAccessExpression(new JFieldAccessExpression("channels"), new JIntLiteral(0)),
                        "non_circular"),
                new JEmittedTextExpression("TRUE")
                )));
        block.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(
                        new JArrayAccessExpression(new JFieldAccessExpression("channels"), new JIntLiteral(numchannels - 1)),
                        "non_circular"),
                new JEmittedTextExpression("TRUE")
                )));
        addInitStatementFirst(block);
        
        addInitStatement(new JExpressionStatement(new JMethodCallExpression("ds_init", new JExpression[0])));
        
        addField(new JFieldDeclaration(new JVariableDefinition(new CEmittedTextType("FILE *"),"inf")));
        addField(new JFieldDeclaration(new JVariableDefinition(new CEmittedTextType("FILE *"),"outf")));
        addInitStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression("inf"),
                new JMethodCallExpression("fopen", new JExpression[]{
                        new JStringLiteral("ref/FFT5.in"),
                        new JStringLiteral("r")
                }))));
        addInitStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression("outf"),
                new JMethodCallExpression("fopen", new JExpression[]{
                        new JStringLiteral("fft.out"),
                        new JStringLiteral("w")
                }))));
        
        addInitStatement(new JExpressionStatement(new JMethodCallExpression(
                "safe_dec",
                new JExpression[]{new JFieldAccessExpression(new JArrayAccessExpression(new JFieldAccessExpression("channels"),new JIntLiteral(0)),"free_bytes"),
                        new JMultExpression(new JIntLiteral(2048), new JFieldAccessExpression(N))
                })));
        addInitStatement(new JExpressionStatement(new JMethodCallExpression(
                "fread",
                new JExpression[]{new JFieldAccessExpression(new JFieldAccessExpression(new JArrayAccessExpression(new JFieldAccessExpression("channels"),new JIntLiteral(0)),"buf"),"data"),
                        new JMethodCallExpression("sizeof",new JExpression[]{new JEmittedTextExpression("float")}),
                        new JMultExpression(new JIntLiteral(512), new JFieldAccessExpression(N)),
                        new JFieldAccessExpression("inf")
                })));
        addInitStatement(new JExpressionStatement(new JMethodCallExpression(
                "buf_inc_tail",
                new JExpression[]{new JMethodCallExpression("&",new JExpression[]{new JFieldAccessExpression(new JArrayAccessExpression(new JFieldAccessExpression("channels"),new JIntLiteral(0)),"buf")}),
                        new JMultExpression(new JIntLiteral(2048), new JFieldAccessExpression(N))
                })));
        addInitStatement(new JExpressionStatement(new JCompoundAssignmentExpression(
                null, Constants.OPE_PLUS,
                new JFieldAccessExpression(new JArrayAccessExpression(new JFieldAccessExpression("channels"),new JIntLiteral(0)),"used_bytes"),
                new JMultExpression(new JIntLiteral(2048), new JFieldAccessExpression(N)))));
        addInitStatement(new JExpressionStatement(new JPostfixExpression(
                Constants.OPE_POSTDEC,
                new JNameExpression(null, new JFieldAccessExpression(new JFieldAccessExpression(new JArrayAccessExpression(new JFieldAccessExpression("channels"),new JIntLiteral(0)),"input"),"f"), "incomplete_inputs")
                )));
        
        addInitStatement(new JExpressionStatement(new JMethodCallExpression("ds_run", new JExpression[0])));
        addInitStatement(new JExpressionStatement(new JMethodCallExpression(
                "fwrite",
                new JExpression[]{new JFieldAccessExpression(new JFieldAccessExpression(new JArrayAccessExpression(new JFieldAccessExpression("channels"),new JIntLiteral(15)),"buf"),"data"),
                        new JMethodCallExpression("sizeof",new JExpression[]{new JEmittedTextExpression("float")}),
                        new JMultExpression(new JIntLiteral(512), new JFieldAccessExpression(N)),
                        new JFieldAccessExpression("outf")
                })));
        
        addInitStatement(new JExpressionStatement(new JMethodCallExpression("fclose", new JExpression[]{new JFieldAccessExpression("inf")})));
        addInitStatement(new JExpressionStatement(new JMethodCallExpression("fclose", new JExpression[]{new JFieldAccessExpression("outf")})));
    }
    
    public void dynSetupFilter(FilterSliceNode filterNode, int filternum,
            int inputs, int outputs,
            ArrayList<Integer> inputnums, ArrayList<Integer> outputnums) {
        JBlock block = new JBlock();
        block.addStatement(new JExpressionStatement(new JEmittedTextExpression("")));
        block.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression("f"),
                new JMethodCallExpression("&", new JExpression[]{
                    new JArrayAccessExpression(new JFieldAccessExpression("filters"),new JIntLiteral(filternum))       
                }))));
        block.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JFieldAccessExpression(new JFieldAccessExpression("f"),"desc"),"work_func"),
                new JMethodCallExpression("&", new JExpression[]{
                    new JEmittedTextExpression("wf_" + filterNode.getFilter().getName())       
                }))));
        block.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JFieldAccessExpression(new JFieldAccessExpression("f"),"desc"),"num_inputs"),
                new JIntLiteral(inputs)
                )));
        block.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JFieldAccessExpression(new JFieldAccessExpression("f"),"desc"),"num_outputs"),
                new JIntLiteral(outputs)
                )));
        for (int i=0; i<inputs; i++) {
            block.addStatement(new JExpressionStatement(new JAssignmentExpression(
                    new JNameExpression(null, new JFieldAccessExpression("f"), "inputs["+i+"]"),
                    new JMethodCallExpression("&", new JExpression[]{
                            new JArrayAccessExpression(new JFieldAccessExpression("channels"),
                                    new JIntLiteral(inputnums.get(i)))       
                        }))));
            block.addStatement(new JExpressionStatement(new JAssignmentExpression(
                    new JFieldAccessExpression(new JFieldAccessExpression(new JNameExpression(null, new JFieldAccessExpression("f"), "inputs["+i+"]"),"input"), "f"),
                    new JFieldAccessExpression("f")       
                        )));
            block.addStatement(new JExpressionStatement(new JAssignmentExpression(
                    new JFieldAccessExpression(new JFieldAccessExpression(new JNameExpression(null, new JFieldAccessExpression("f"), "inputs["+i+"]"),"input"), "pop_bytes"),
                    new JMultExpression(new JIntLiteral(filterNode.getFilter().getPopInt()),new JMethodCallExpression("sizeof", new JExpression[]{new JEmittedTextExpression("float")}))
                        )));
            block.addStatement(new JExpressionStatement(new JAssignmentExpression(
                    new JFieldAccessExpression(new JFieldAccessExpression(new JNameExpression(null, new JFieldAccessExpression("f"), "inputs["+i+"]"),"input"), "spu_buf_size"),
                    new JIntLiteral(32*1024)      
                        )));
        }
        for (int i=0; i<outputs; i++) {
            block.addStatement(new JExpressionStatement(new JAssignmentExpression(
                    new JNameExpression(null, new JFieldAccessExpression("f"), "outputs["+i+"]"),
                    new JMethodCallExpression("&", new JExpression[]{
                            new JArrayAccessExpression(new JFieldAccessExpression("channels"),
                                    new JIntLiteral(outputnums.get(i)))       
                        }))));
            block.addStatement(new JExpressionStatement(new JAssignmentExpression(
                    new JFieldAccessExpression(new JFieldAccessExpression(new JNameExpression(null, new JFieldAccessExpression("f"), "output["+i+"]"),"output"), "f"),
                    new JFieldAccessExpression("f")       
                        )));
            block.addStatement(new JExpressionStatement(new JAssignmentExpression(
                    new JFieldAccessExpression(new JFieldAccessExpression(new JNameExpression(null, new JFieldAccessExpression("f"), "outputs["+i+"]"),"output"), "push_bytes"),
                    new JMultExpression(new JIntLiteral(filterNode.getFilter().getPushInt()),new JMethodCallExpression("sizeof", new JExpression[]{new JEmittedTextExpression("float")}))
                        )));
            block.addStatement(new JExpressionStatement(new JAssignmentExpression(
                    new JFieldAccessExpression(new JFieldAccessExpression(new JNameExpression(null, new JFieldAccessExpression("f"), "outputs["+i+"]"),"output"), "spu_buf_size"),
                    new JIntLiteral(32*1024)      
                        )));
        }
        block.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JNameExpression(null, new JFieldAccessExpression("f"), "group_iters"),
                new JIntLiteral(8))));
        block.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JNameExpression(null, new JFieldAccessExpression("f"), "min_group_runs"),
                new JIntLiteral(10))));
        addInitStatement(block);
        
        
    }
    
    public void newline() {
        addInitStatement(new JExpressionStatement(new JEmittedTextExpression("")));
    }
    
    public static String idToFd(Integer id) {
        return SPU_FD + id;
    }
    
    private Integer getIdForSlice(Slice slice) {
        Integer newId = sliceIdMap.get(slice);
        if (newId == null) {
            newId = id;
            sliceIdMap.put(slice, newId);
            id++;
        }
        return newId;
    }
    
    private static InterSliceEdge getEdgeBetween(OutputSliceNode src, InputSliceNode dest) {
        for (InterSliceEdge e : src.getDestSequence()) {
            if (e.getDest() == dest)
                return e;
        }
        return null;
    }
    
    public class Buffer {
        public final String name;
        public final int index;
        
        public Buffer(String name, int index) {
            this.name = name;
            this.index = index;
        }
        
        public String toString() {
            return name + "[" + index + "]";
        }
    }
    
    private static final String SPU_ADDRESS = "SPU_ADDRESS";
    private static final String SPU_CMD_GROUP = "SPU_CMD_GROUP *";
    private static final String LS_ADDRESS = "LS_ADDRESS";
    private static final String WFA = "wf";
    private static final String INIT_WFA = "init_wf";
    private static final String WFA_PREFIX = "wf_";
    private static final String INIT_FUNC = "initfunc";
    private static final String INIT_FUNC_PREFIX = "init_";
    private static final String SPU_DATA_START = "spu_data_start";
    private static final String SPU_DATA_SIZE = "spu_data_size";
    private static final String SPU_FILTER_DESC = "SPU_FILTER_DESC";
    private static final String SPU_FD = "fd";
    private static final String INIT_SPU_FD = "init_fd";
    private static final String SPU_FD_WORK_FUNC = "work_func";
    private static final String SPU_FD_STATE_SIZE = "state_size";
    private static final String SPU_FD_STATE_ADDR = "state_addr";
    private static final String SPU_FD_NUM_INPUTS = "num_inputs";
    private static final String SPU_FD_NUM_OUTPUTS = "num_outputs";
    private static final String GROUP = "g";
    private static final String SPU_NEW_GROUP = "spu_new_group";
    private static final String SPU_ISSUE_GROUP = "spu_issue_group";
    private static final String SPU_FILTER_LOAD = "spu_filter_load";
    private static final String SPU_FILTER_UNLOAD = "spu_filter_unload";
    private static final String SPU_FILTER_ATTACH_INPUT = "spu_filter_attach_input";
    private static final String SPU_FILTER_ATTACH_OUTPUT = "spu_filter_attach_output";
    private static final String SPU_BUFFER_ALLOC = "spu_buffer_alloc";
    private static final String SPU_CALL_FUNC = "spu_call_func";
    private static final String INPUT_BUFFER_ADDR = "iba";
    private static final String INPUT_BUFFER_SIZE = "ibs";
    private static final String OUTPUT_BUFFER_ADDR = "oba";
    private static final String OUTPUT_BUFFER_SIZE = "obs";
    private static final String PPU_INPUT_BUFFER_ADDR = "piba";
    private static final String PPU_INPUT_BUFFER_SIZE = "pibs";
    private static final String PPU_INPUT_BUFFER_CB = "picb";
    private static final String PPU_OUTPUT_BUFFER_ADDR = "poba";
    private static final String PPU_OUTPUT_BUFFER_SIZE = "pobs";
    private static final String PPU_OUTPUT_BUFFER_CB = "pocb";
    private static final String DATA_ADDR = "da";
    private static final String FCB = "fcb";
    private static final String CB = "cb";
    private static final String LS_SIZE = "LS_SIZE";
    private static final String CACHE_SIZE = "CACHE_SIZE";
    private static final String SPUINIT = "spuinit";
    private static final String SPULIB_INIT = "spulib_init";
    private static final String SPULIB_WAIT = "spulib_wait";
    private static final String ALLOC_BUFFER = "alloc_buffer";
    private static final String BUF_GET_CB = "buf_get_cb";
    private static final String INDTSZPERITER = "indtszperiter";
    private static final String OUTDTSZPERITER = "outdtszperiter";
    private static final String RUNSPERITER = "runsperiter";
    private static final String ITERS = "iters";
    private static final String SPUITERS = "spuiters";
    private static final String INSPUITEMS = "inspuitems";
    private static final String OUTSPUITEMS = "outspuitems";
    private static final String FILE_READER = "file_reader";
    private static final String FILE_WRITER = "file_writer";
    private static final String CALL_FUNC = "CALL_FUNC";
    private static final String BEGIN_FUNC = "BEGIN_FUNC";
    private static final String END_FUNC = "END_FUNC";
    
    private static final String UINT32_T = "uint32_t";
    private static final String PLUS = " + ";
    private static final String MINUS = " - ";
    private static final String INCLUDE = "#include";
    private static final String ROUND_UP = "ROUND_UP";
    private static final String INIT_TICKS = "init_ticks";
    private static final String TICKS = "ticks";
    private static final String START = "start";
    private static final String STARTSPU = "startspu";
    private static final String PRINTF = "printf";
    
    private static final String N = "n", ND = "nd", DONE = "done";
    
    private static final int SPU_RESERVE_SIZE = 4096;
    private static final int FILLER = 128;
    private static final int FCB_SIZE = 128;
    private static final int BUFFER_OFFSET = 0;
    private static final int NO_DEPS = 0;
    private static final String WAIT_MASK = "0x3f";
    
}
