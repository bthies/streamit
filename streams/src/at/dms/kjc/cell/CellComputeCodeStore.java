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
import at.dms.kjc.sir.lowering.fission.StatelessDuplicate;
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
    
    private ArrayList<JMethodDeclaration> initfuncs = 
        new ArrayList<JMethodDeclaration>();
    
    private int id = 0;
    
    // The slicenode whose code is stored here
    private SliceNode slicenode;
    
    /***************************************************************************
     *                              Constructors
     ***************************************************************************
     */
    public CellComputeCodeStore(CellPU parent) {
        super(parent);
    }
    
    public CellComputeCodeStore(CellPU parent, SliceNode s) {
        super(parent);
        slicenode = s;
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
    
    /***************************************************************************
     *                  Static scheduler specific fields
     ***************************************************************************
     */
    
    public void addStaticFields() {
        // EXT_PSP_EX_PARAMS f[numfilters];
        JVariableDefinition params = new JVariableDefinition(
                new CArrayType(
                        new CEmittedTextType("EXT_PSP_EX_PARAMS"),
                        1,
                        new JExpression[]{new JIntLiteral(CellBackend.numfilters)}),
                "f");
        addField(new JFieldDeclaration(params));
        
        // EXT_PSP_EX_LAYOUT l[numfilters];
        JVariableDefinition layout = new JVariableDefinition(
                new CArrayType(
                        new CEmittedTextType("EXT_PSP_EX_LAYOUT"),
                        1,
                        new JExpression[]{new JIntLiteral(CellBackend.numfilters)}),
                "l");
        addField(new JFieldDeclaration(layout));
        
        // SPU_FILTER_DESC fd[numfilters];
        JVariableDefinition fd = new JVariableDefinition(
                new CArrayType(
                        new CEmittedTextType(SPU_FILTER_DESC),
                        1,
                        new JExpression[]{
                            new JIntLiteral(CellBackend.numfilters)
                        }),
                SPU_FD);
        addField(new JFieldDeclaration(fd));
        
        // BUFFER_CB channels[numchannels];
        JVariableDefinition c = new JVariableDefinition(
                new CArrayType(
                        new CEmittedTextType("BUFFER_CB"),
                        1,
                        new JExpression[]{
                            new JIntLiteral(CellBackend.numchannels)
                        }),
                "channels");
        addField(new JFieldDeclaration(c));
    }
    
    /**
     * Adds data address field for static scheduling case.
     * 
     * SPU_ADDRESS da;
     */
    public void addDataAddressField() {
        if (KjcOptions.celldyn) return;
        
        JVariableDefinition da = new JVariableDefinition(
                new CEmittedTextType(SPU_ADDRESS), DATA_ADDR);
        JFieldDeclaration field = new JFieldDeclaration(da);
        addField(field);
    }
    
    /***************************************************************************
     *              Dynamic scheduler specific fields
     ***************************************************************************
     */
    
    public void addDynamicFields() {
        int numfilters = CellBackend.numfilters;
        int numchannels = CellBackend.numchannels;

        // FILTER filters[<numfilters>];
        JVariableDefinition filters = new JVariableDefinition(
                new CArrayType(new CEmittedTextType("FILTER"),
                        1,
                        new JExpression[]{new JIntLiteral(numfilters)}),
                "filters");
        JFieldDeclaration field = new JFieldDeclaration(filters);
        addField(field);
        
        // CHANNEL channels[<numchannels>]
        JVariableDefinition channels = new JVariableDefinition(
                new CArrayType(new CEmittedTextType("CHANNEL"),
                        1,
                        new JExpression[]{new JIntLiteral(numchannels)}),
                "channels");
        field = new JFieldDeclaration(channels);
        addField(field);
        
        // num_filters = <numfilters>;
        JExpressionStatement num_filters = new JExpressionStatement(
                new JAssignmentExpression(new JFieldAccessExpression("num_filters"),
                        new JIntLiteral(numfilters)));
        addInitStatement(num_filters);
        
        // num_channels = <numchannels>;
        JExpressionStatement num_channels = new JExpressionStatement(
                new JAssignmentExpression(new JFieldAccessExpression("num_channels"),
                        new JIntLiteral(numchannels)));
        addInitStatement(num_channels);
        
        // num_spu = <numspus>;
        JExpressionStatement num_spu = new JExpressionStatement(
                new JAssignmentExpression(new JFieldAccessExpression("num_spu"),
                        new JIntLiteral(numspus)));
        addInitStatement(num_spu);
        
        // n = 10000;
        JVariableDefinition n = new JVariableDefinition(CStdType.Integer, N);
        n.setInitializer(null);
        addField(new JFieldDeclaration(n));
        addInitStatement(new JExpressionStatement(
                new JAssignmentExpression(
                        new JFieldAccessExpression(N), 
                        new JIntLiteral(10000))));
        
    }
    
    public void initDynamic() {
        int numchannels = CellBackend.numchannels;
        
        JBlock block = new JBlock();
        
        // spuinit();
        JExpressionStatement spuinit = new JExpressionStatement(
                new JMethodCallExpression(
                        SPUINIT,
                        new JExpression[]{}));
        block.addStatement(spuinit);
        
        // channels[i].buf_size = 1024*1024;
        JForStatement channelbufsize;
        // make init statement - assign zero to <var>.  We need to use
        // an expression list statement to follow the convention of
        // other for loops and to get the codegen right.
        JVariableDefinition var = new JVariableDefinition(CStdType.Integer, "i");
        block.addStatement(new JVariableDeclarationStatement(var));
        
        JExpression initExpr[] = {
            new JAssignmentExpression(new JLocalVariableExpression(var),
                                      new JIntLiteral(0)) };
        JStatement init = new JExpressionListStatement(initExpr);
        // make conditional - test if <var> less than <count>
        JExpression cond = 
            new JRelationalExpression(Constants.OPE_LT,
                                      new JLocalVariableExpression(var),
                                      new JIntLiteral(numchannels));
        JExpression incrExpr = 
            new JPostfixExpression(Constants.OPE_POSTINC, 
                                   new JLocalVariableExpression(var));
        JStatement incr = new JExpressionStatement(incrExpr);
        
        JBlock body = new JBlock();
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(
                        new JArrayAccessExpression(
                                new JFieldAccessExpression("channels"), 
                                new JLocalVariableExpression(var)),
                        "buf_size"),
                new JIntLiteral(1024*1024))));
        channelbufsize = new JForStatement(init, cond, incr, body);
        block.addStatement(channelbufsize);

        
        addInitStatement(block);
        
    }
    
    public void dynamic() {
        int numfilters = CellBackend.numfilters;


        JBlock block = new JBlock();
        
        
        for (int i = 0; i < CellBackend.channels.size(); i++) {
            InterSliceEdge e = CellBackend.channels.get(i);
            if (e.getSrc().getPrevFilter().isFileInput()) {
                int pushRate = e.getSrc().getPrevFilter().getFilter().getPushInt();
                int steadyMult = e.getSrc().getPrevFilter().getFilter().getSteadyMult();
                block.addStatement(new JExpressionStatement(new JAssignmentExpression(
                        new JFieldAccessExpression(
                                new JArrayAccessExpression(new JFieldAccessExpression("channels"), new JIntLiteral(i)),
                                "buf_size"),
                        new JMultExpression(new JFieldAccessExpression(N), new JIntLiteral(pushRate*steadyMult*4))
                        )));
                block.addStatement(new JExpressionStatement(new JAssignmentExpression(
                        new JFieldAccessExpression(
                                new JArrayAccessExpression(new JFieldAccessExpression("channels"), new JIntLiteral(i)),
                                "non_circular"),
                        new JEmittedTextExpression("TRUE")
                        )));
            }
            if (e.getDest().getNextFilter().isFileOutput()) {
                int popRate = e.getDest().getNextFilter().getFilter().getPopInt();
                int steadyMult = e.getDest().getNextFilter().getFilter().getSteadyMult();
                block.addStatement(new JExpressionStatement(new JAssignmentExpression(
                        new JFieldAccessExpression(
                                new JArrayAccessExpression(new JFieldAccessExpression("channels"), new JIntLiteral(i)),
                                "buf_size"),
                        new JMultExpression(new JFieldAccessExpression(N), new JIntLiteral(popRate*steadyMult*4))
                        )));
                block.addStatement(new JExpressionStatement(new JAssignmentExpression(
                        new JFieldAccessExpression(
                                new JArrayAccessExpression(new JFieldAccessExpression("channels"), new JIntLiteral(i)),
                                "non_circular"),
                        new JEmittedTextExpression("TRUE")
                        )));
            }
        }
        
//        block.addStatement(new JExpressionStatement(new JAssignmentExpression(
//                new JFieldAccessExpression(
//                        new JArrayAccessExpression(new JFieldAccessExpression("channels"), new JIntLiteral(0)),
//                        "buf_size"),
//                new JMultExpression(new JIntLiteral(2048), new JFieldAccessExpression(N))
//                )));
//        block.addStatement(new JExpressionStatement(new JAssignmentExpression(
//                new JFieldAccessExpression(
//                        new JArrayAccessExpression(new JFieldAccessExpression("channels"), new JIntLiteral(numchannels - 1)),
//                        "buf_size"),
//                new JMultExpression(new JIntLiteral(2048), new JFieldAccessExpression(N))
//                )));
//        block.addStatement(new JExpressionStatement(new JAssignmentExpression(
//                new JFieldAccessExpression(
//                        new JArrayAccessExpression(new JFieldAccessExpression("channels"), new JIntLiteral(0)),
//                        "non_circular"),
//                new JEmittedTextExpression("TRUE")
//                )));
//        block.addStatement(new JExpressionStatement(new JAssignmentExpression(
//                new JFieldAccessExpression(
//                        new JArrayAccessExpression(new JFieldAccessExpression("channels"), new JIntLiteral(numchannels - 1)),
//                        "non_circular"),
//                new JEmittedTextExpression("TRUE")
//                )));
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

    
    /** ------------------------------------------------------------------------
     *                          SETUP
     -------------------------------------------------------------------------*/
    
    /**
     *              Static scheduler specific setup
     */
    
    /**
     * Initialize the channel with this ID and allocate the buffer
     * @param channelId
     */
    public void initChannel(int channelId) {
        if (KjcOptions.celldyn) return;
        
        addInitStatement(new JExpressionStatement(
                new JMethodCallExpression(
                        "init_buffer",
                        new JExpression[]{
                                new JMethodCallExpression("&",new JExpression[]{new JArrayAccessExpression(
                                        new JFieldAccessExpression("channels"),new JIntLiteral(channelId))}),
                                new JEmittedTextExpression("NULL"),
                                new JFieldAccessExpression(PPU_INPUT_BUFFER_SIZE),
                                new JEmittedTextExpression("TRUE"),
                                new JIntLiteral(0)})));
    }
    
    /**
     * Sets up filter's fields:
     * state_size, num_inputs, num_outputs
     * 
     * work_func will be either the init work func or the actual work func,
     * depending on which is being run. Thus, work_func will be set later.
     * @param sliceNode
     */
    public void setupFilter(SliceNode sliceNode) {
        if (KjcOptions.celldyn) {
            setupDynamicFilter(sliceNode);
        } else {
            setupStaticFilter(sliceNode);
        }
    }
    
    private void setupStaticFilter(SliceNode sliceNode) {
        int filterId = CellBackend.filterIdMap.get(sliceNode);
        
        JBlock body = new JBlock();
        
        JExpressionStatement statesize = new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(
                        new JArrayAccessExpression(
                                new JFieldAccessExpression(SPU_FD),
                                new JIntLiteral(filterId)),
                        SPU_FD_STATE_SIZE),
                new JIntLiteral(0)));
        body.addStatement(statesize);
      
        addInitStatement(body);

    }
    
    private void setupDynamicFilter(SliceNode sliceNode) {
        int filterId = CellBackend.filterIdMap.get(sliceNode);
        int inputs, outputs;
        inputs = getNumInputs(sliceNode);
        outputs = getNumOutputs(sliceNode);
        String filtername = getFilterName(sliceNode);
        
        int statesize = StatelessDuplicate.sizeOfMutableState(sliceNode.getAsFilter().getFilter());
        
        // filters[id].name = <name>
        addInitStatement(new JExpressionStatement(new JAssignmentExpression(
                        new JFieldAccessExpression(
                            new JArrayAccessExpression(
                                    new JFieldAccessExpression("filters"),
                                    new JIntLiteral(filterId)),
                            "name"),
                            new JStringLiteral(filtername))));
        
        // filters[id].desc.work_func = wf[id];
        addInitStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(
                new JFieldAccessExpression(
                    new JArrayAccessExpression(
                            new JFieldAccessExpression("filters"),
                            new JIntLiteral(filterId)),
                    "desc"),
                    "work_func"),
                    new JArrayAccessExpression(
                            new JFieldAccessExpression(WFA),
                            new JIntLiteral(filterId)))));
        
        // filters[id].desc.state_size = 0/sizeof(state);
        addInitStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(
                    new JFieldAccessExpression(
                        new JArrayAccessExpression(
                                new JFieldAccessExpression("filters"),
                                new JIntLiteral(filterId)),
                        "desc"),
                    "state_size"),
                new JIntLiteral(0))));
        
        // filters[id].desc.num_inputs = <numinputs>;
        addInitStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(
                new JFieldAccessExpression(
                    new JArrayAccessExpression(
                            new JFieldAccessExpression("filters"),
                            new JIntLiteral(filterId)),
                    "desc"),
                    "num_inputs"),
                    new JIntLiteral(inputs))));
        
        // filters[id].desc.num_outputs = <numoutputs>;
        addInitStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(
                new JFieldAccessExpression(
                    new JArrayAccessExpression(
                            new JFieldAccessExpression("filters"),
                            new JIntLiteral(filterId)),
                    "desc"),
                    "num_outputs"),
                    new JIntLiteral(outputs))));
        
        // filters[id].spu_init_func = init_wf[id];
        addInitStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(
                    new JArrayAccessExpression(
                            new JFieldAccessExpression("filters"),
                            new JIntLiteral(filterId)),
                    "spu_init_func"),
                new JArrayAccessExpression(
                        new JFieldAccessExpression("init_wf"),
                        new JIntLiteral(filterId)))));
        
        if (statesize == 0) {
            // filters[id].data_parallel = TRUE;
            addInitStatement(new JExpressionStatement(new JAssignmentExpression(
                    new JFieldAccessExpression(
                        new JArrayAccessExpression(
                                new JFieldAccessExpression("filters"),
                                new JIntLiteral(filterId)),
                        "data_parallel"),
                        new JEmittedTextExpression("TRUE"))));
        }
    }
    
    /**
     * Set the filter description's work func to be the appropriate work func
     * depending on the phase (init or steady)
     * @param sliceNode
     * @param whichPhase
     * @return TODO
     */
    public JBlock setupFilterDescriptionWorkFunc(SliceNode sliceNode, SchedulingPhase whichPhase) {
        int filterId = CellBackend.filterIdMap.get(sliceNode);
        
        String workfuncname;
        if (whichPhase == SchedulingPhase.INIT)
            workfuncname = INIT_WFA;
        else if (whichPhase == SchedulingPhase.STEADY)
            workfuncname = WFA;
        else return null;
        
        JBlock body = new JBlock();
        JExpressionStatement workfunc = new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(
                        new JArrayAccessExpression(
                                new JFieldAccessExpression(SPU_FD),
                                new JIntLiteral(filterId)),
                        SPU_FD_WORK_FUNC),
                new JArrayAccessExpression(new JFieldAccessExpression(workfuncname), 
                                           new JIntLiteral(filterId))));
        body.addStatement(workfunc);
        return body;
    }
    
    public JBlock setupPSPNumIOputs(SliceNode sliceNode, SchedulingPhase whichPhase) {
        int filterId = CellBackend.filterIdMap.get(sliceNode);
        
        int inputs, outputs;
        inputs = getNumInputs(sliceNode);
        outputs = getNumOutputs(sliceNode);

        if (whichPhase == SchedulingPhase.INIT) {
            inputs= 0;
            outputs = 0;
        }
        
        JBlock body = new JBlock();
        JExpressionStatement numinputs = new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(
                        new JArrayAccessExpression(
                                new JFieldAccessExpression(SPU_FD),
                                new JIntLiteral(filterId)),
                        SPU_FD_NUM_INPUTS),
                new JIntLiteral(inputs)));
        body.addStatement(numinputs);
        JExpressionStatement numoutputs = new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(
                        new JArrayAccessExpression(
                                new JFieldAccessExpression(SPU_FD),
                                new JIntLiteral(filterId)),
                        SPU_FD_NUM_OUTPUTS),
                new JIntLiteral(outputs)));
        body.addStatement(numoutputs);
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
        return body;
    }
    
    
    /**
     * Assigns work function address for the given slicenode.
     * Also assigns work function address for init function of the given
     * slicenode.
     * RR splitters and joiners are made into separate filters with their own
     * work functions, and thus need to be initialized here.
     * 
     * wf = &wf_[init_][slicename]
     * wf = &wf_[init_]joiner_[joinername]
     * wf = &wf_[init_]splitter_[splittername]
     * @param sliceNode
     */
    public void setupWorkFunctionAddress(SliceNode sliceNode) {
        String sliceName = getFilterName(sliceNode);
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
    }
    
    /**
     * Sets up most of the parameters for ext_psp, except for pop/peek_extra/push_bytes,
     * which differ depending on INIT/STEADY stage
     * @param sliceNode
     */
    public void setupPSP(SliceNode sliceNode) {
        // This is only done for static scheduling
        if (KjcOptions.celldyn) return;
        
        int inputs, outputs;
        inputs = getNumInputs(sliceNode);
        outputs = getNumOutputs(sliceNode);

        int filterId = CellBackend.filterIdMap.get(sliceNode);
        JBlock body = new JBlock();
        
        // setup EXT_PSP_EX_PARAMS
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
     * @return TODO
     */
    public JBlock setupPSPIOBytes(SliceNode sliceNode, SchedulingPhase whichPhase) {
        int inputs, outputs;
        inputs = getNumInputs(sliceNode);
        outputs = getNumOutputs(sliceNode);
        
        if (sliceNode.isFilterSlice() && sliceNode.getAsFilter().getParent().getTail().isDuplicateSplitter())
            outputs = 1;
        int filterId = CellBackend.filterIdMap.get(sliceNode);
        int poprate = sliceNode.getParent().getFilterNodes().get(0).getFilter().getPopInt();
        int pushrate = sliceNode.getParent().getFilterNodes().get(0).getFilter().getPushInt();
        int peekrate = sliceNode.getParent().getFilterNodes().get(0).getFilter().getPeekInt();
        int mult;
        if (whichPhase == SchedulingPhase.INIT)
            mult = 0; //sliceNode.getParent().getFilterNodes().get(0).getFilter().getInitMult();
        else if (whichPhase == SchedulingPhase.STEADY)
            mult = 1; //sliceNode.getParent().getFilterNodes().get(0).getFilter().getSteadyMult();
        else return null;
        
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
        return body;
    }

    /**
     * Get the ID of the SPU that the filter has been assigned to, and set
     * the spu_id parameter in ext_psp_layout.
     * @param sliceNode
     * @param spuId
     * @return TODO
     */
    public JBlock setupPSPSpuId(SliceNode sliceNode, int spuId) {
        int filterId = CellBackend.filterIdMap.get(sliceNode);
        JBlock body = new JBlock();
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JArrayAccessExpression(
                        new JFieldAccessExpression("l"), new JIntLiteral(filterId)), "spu_id"),
                new JIntLiteral(spuId))));
        return body;        
    }
    
    /**
     * Call ext_ppu_spu_ppu_ex to run the filter asynchronously.
     * @param sliceNode
     * @return TODO
     */
    public JBlock callExtPSP(SliceNode sliceNode) {
        return callExtPSP(sliceNode, 1);
    }
    
    public JBlock callExtPSP(SliceNode sliceNode, int iters) {
        int filterId = CellBackend.filterIdMap.get(sliceNode);
        JBlock body = new JBlock();
        body.addStatement(new JExpressionStatement(new JMethodCallExpression("ext_ppu_spu_ppu_ex",
                new JExpression[]{
                new JMethodCallExpression("&", new JExpression[]{new JArrayAccessExpression(
                        new JFieldAccessExpression("l"), new JIntLiteral(filterId))}),
                        new JMethodCallExpression("&", new JExpression[]{new JArrayAccessExpression(
                                new JFieldAccessExpression("f"), new JIntLiteral(filterId))}),
                                new JFieldAccessExpression("input_"+filterId),
                                new JFieldAccessExpression("output_"+filterId),
                                new JIntLiteral(iters),
                                new JEmittedTextExpression("cb"),
                                new JIntLiteral(0)
        })));
        return body;
    }
    
    public JBlock setDone(int done) {
        JBlock body = new JBlock();
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(DONE),
                new JIntLiteral(done))));
        return body;
    }
    
    public JBlock addSpulibPollWhile() {
        JBlock body = new JBlock();
        body.addStatement(new JExpressionStatement(new JMethodCallExpression(
                "spulib_poll_while",
                new JExpression[]{
                        new JRelationalExpression(Constants.OPE_GT, new JFieldAccessExpression(DONE), new JIntLiteral(0))
                })));
        return body;
    }
    
    /**
     * Duplicate channelId to all the channels in duplicateIds.
     * @param channelId
     * @param duplicateIds
     */
    public void duplicateChannel(int channelId, LinkedList<Integer> duplicateIds) {
        if (KjcOptions.celldyn) return;
        for (int i : duplicateIds) {
            duplicateChannel(channelId, i);
        }
    }
    
    /**
     * Duplicate channelId to duplicateId
     * @param channelId
     * @param duplicateId
     */
    public void duplicateChannel(int channelId, int duplicateId) {
        addInitStatement(new JExpressionStatement(new JMethodCallExpression(
                "duplicate_buffer",
                new JExpression[]{
                    new JMethodCallExpression(
                            "&",
                            new JExpression[]{
                                new JArrayAccessExpression(
                                    new JFieldAccessExpression("channels"),
                                    new JIntLiteral(duplicateId))}),
                    new JMethodCallExpression(
                            "&",
                            new JExpression[]{
                                new JArrayAccessExpression(
                                    new JFieldAccessExpression("channels"),
                                    new JIntLiteral(channelId))}),
                }
        )));
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
        
        // int pibs = 1024 * 1024;          PPU input buffer size     
        JVariableDefinition pibs = new JVariableDefinition(CStdType.Integer, PPU_INPUT_BUFFER_SIZE);
        pibs.setInitializer(new JIntLiteral(4 * 1024 * 1024));
        addField(new JFieldDeclaration(pibs));
        
        // int pobs = 1024 * 1024;          PPU output buffer size
        JVariableDefinition pobs = new JVariableDefinition(CStdType.Integer, PPU_OUTPUT_BUFFER_SIZE);
        pobs.setInitializer(new JIntLiteral(1024 * 1024));
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
        
//        int poprate = schedule.getSchedule()[1].getFilterNodes().get(0).getFilter().getPopInt();
//        int pushrate = schedule.getSchedule()[1].getFilterNodes().get(0).getFilter().getPushInt();
//        if (schedule.getSchedule()[0].getFilterNodes().get(0).getFilter().getOutputType().isFloatingPoint())
//            inputType = "float";
//        else inputType = "int";
        
        addInitStatement(new JExpressionStatement(new JAssignmentExpression(new JFieldAccessExpression(N), new JIntLiteral(1000))));
        addInitStatement(new JExpressionStatement(new JAssignmentExpression(new JFieldAccessExpression(RUNSPERITER), new JIntLiteral(8))));
        addInitStatement(new JExpressionStatement(new JAssignmentExpression(new JFieldAccessExpression(ITERS), 
                new JDivideExpression(null, new JFieldAccessExpression(N), new JFieldAccessExpression(RUNSPERITER)))));
        
        JVariableDefinition start = new JVariableDefinition(CStdType.Integer, START);
        addField(new JFieldDeclaration(start));
        
        JVariableDefinition startspu = new JVariableDefinition(CStdType.Integer, STARTSPU);
        addField(new JFieldDeclaration(startspu));
        
        JVariableDefinition numspus = new JVariableDefinition(CStdType.Integer, "numspus");
        addField(new JFieldDeclaration(numspus));
        addInitStatement(new JExpressionStatement(new JAssignmentExpression(new JFieldAccessExpression("numspus"), new JIntLiteral(this.numspus))));
        
        newline();
    }
    
    public void initSpulib() {
        // spulib_init()
        JExpressionStatement spulibinit = new JExpressionStatement(
                new JMethodCallExpression(
                        SPULIB_INIT,
                        new JExpression[]{}));
        addInitStatement(spulibinit);
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
    
    @Deprecated
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
            LinkedList<Integer> inputIds, SchedulingPhase whichPhase) {
        if (inputIds == null)
            return;
     
        if (KjcOptions.celldyn)
            attachInputChannelArrayDynamic(filterId, inputIds, whichPhase);
        else attachInputChannelArrayStatic(filterId, inputIds, whichPhase);
    }
    
    private void attachInputChannelArrayDynamic(int filterId,
            LinkedList<Integer> inputIds, SchedulingPhase whichPhase) {
        SliceNode sliceNode = CellBackend.filters.get(filterId);
        
        for (int i=0; i<inputIds.size(); i++) {
            // filters[id].inputs[i] = &channels[j];
            addInitStatement(new JExpressionStatement(new JAssignmentExpression(
                    new JArrayAccessExpression(
                            new JFieldAccessExpression(
                                new JArrayAccessExpression(
                                        new JFieldAccessExpression("filters"),
                                        new JIntLiteral(filterId)),
                                "inputs"),
                            new JIntLiteral(i)),
                    new JMethodCallExpression(
                            "&",
                            new JExpression[]{
                            new JArrayAccessExpression(
                                    new JFieldAccessExpression("channels"),
                                    new JIntLiteral(inputIds.get(i)))}))));
            
            // filters[id].inputs[i]->input.f = &filters[id];
            addInitStatement(new JExpressionStatement(new JAssignmentExpression(
                    new JFieldAccessExpression(
                            new JNameExpression(
                                    null,
                    new JArrayAccessExpression(
                            new JFieldAccessExpression(
                                new JArrayAccessExpression(
                                        new JFieldAccessExpression("filters"),
                                        new JIntLiteral(filterId)),
                                "inputs"),
                            new JIntLiteral(i)),
                            "input"),
                            "f"),
                    new JMethodCallExpression(
                            "&",
                            new JExpression[]{
                            new JArrayAccessExpression(
                                    new JFieldAccessExpression("filters"),
                                    new JIntLiteral(filterId))}))));
            
            InterSliceEdge e = CellBackend.channels.get(inputIds.get(i));

            int poprate = sliceNode.getParent().getFilterNodes().get(0).getFilter().getPopInt();
            int peekrate = sliceNode.getParent().getFilterNodes().get(0).getFilter().getPeekInt();
            int mult = sliceNode.getParent().getFilterNodes().get(0).getFilter().getSteadyMult();
            poprate *= mult;
            peekrate *= mult;
//            if (whichPhase == SchedulingPhase.INIT)
//                mult = 0; //sliceNode.getParent().getFilterNodes().get(0).getFilter().getInitMult();
//            else if (whichPhase == SchedulingPhase.STEADY)
//                mult = 1; //sliceNode.getParent().getFilterNodes().get(0).getFilter().getSteadyMult();
//            else return;
            
            if (sliceNode.isInputSlice()) {
                int weightratio = sliceNode.getAsInput().getWeights()[i] / sliceNode.getAsInput().totalWeights();
                poprate = poprate * weightratio;
                peekrate = peekrate *weightratio;
            }
            JBlock body = new JBlock();
            body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                    new JFieldAccessExpression(
                            new JNameExpression(
                                    null,
                    new JArrayAccessExpression(
                            new JFieldAccessExpression(
                                new JArrayAccessExpression(
                                        new JFieldAccessExpression("filters"),
                                        new JIntLiteral(filterId)),
                                "inputs"),
                            new JIntLiteral(i)),
                            "input"),
                            "pop_bytes"),
                    new JIntLiteral(4 * poprate))));
            body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                    new JFieldAccessExpression(
                            new JNameExpression(
                                    null,
                    new JArrayAccessExpression(
                            new JFieldAccessExpression(
                                new JArrayAccessExpression(
                                        new JFieldAccessExpression("filters"),
                                        new JIntLiteral(filterId)),
                                "inputs"),
                            new JIntLiteral(i)),
                            "input"),
                            "peek_extra_bytes"),
                    new JIntLiteral(4 * Math.max(0, peekrate-poprate)))));
            addInitStatement(body);
        }
    }
    
    private void attachInputChannelArrayStatic(int filterId,
            LinkedList<Integer> inputIds, SchedulingPhase whichPhase) {
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
        
        SliceNode sliceNode = CellBackend.filters.get(filterId);
        
        if (KjcOptions.celldyn) {
            for (int i=0; i<outputIds.size(); i++) {
                // filters[id].outputs[i] = &channels[j];
                addInitStatement(new JExpressionStatement(new JAssignmentExpression(
                        new JArrayAccessExpression(
                                new JFieldAccessExpression(
                                    new JArrayAccessExpression(
                                            new JFieldAccessExpression("filters"),
                                            new JIntLiteral(filterId)),
                                    "outputs"),
                                new JIntLiteral(i)),
                        new JMethodCallExpression(
                                "&",
                                new JExpression[]{
                                new JArrayAccessExpression(
                                        new JFieldAccessExpression("channels"),
                                        new JIntLiteral(outputIds.get(i)))}))));
                
                // filters[id].outputs[i]->output.f = &filters[id];
                addInitStatement(new JExpressionStatement(new JAssignmentExpression(
                        new JFieldAccessExpression(
                                new JNameExpression(
                                        null,
                        new JArrayAccessExpression(
                                new JFieldAccessExpression(
                                    new JArrayAccessExpression(
                                            new JFieldAccessExpression("filters"),
                                            new JIntLiteral(filterId)),
                                    "outputs"),
                                new JIntLiteral(i)),
                                "output"),
                                "f"),
                        new JMethodCallExpression(
                                "&",
                                new JExpression[]{
                                new JArrayAccessExpression(
                                        new JFieldAccessExpression("filters"),
                                        new JIntLiteral(filterId))}))));
                
                int pushrate = sliceNode.getParent().getFilterNodes().get(0).getFilter().getPushInt();
//                if (sliceNode.isFilterSlice() && sliceNode.getAsFilter().getParent().getTail().isDuplicateSplitter())
//                    outputs = 1;
//                for (int i=0; i<outputs; i++) {
                int mult = 1;
                    addInitStatement(new JExpressionStatement(new JAssignmentExpression(
                            new JFieldAccessExpression(
                                    new JNameExpression(
                                            null,
                            new JArrayAccessExpression(
                                    new JFieldAccessExpression(
                                        new JArrayAccessExpression(
                                                new JFieldAccessExpression("filters"),
                                                new JIntLiteral(filterId)),
                                        "inputs"),
                                    new JIntLiteral(i)),
                                    "input"),
                                    "push_bytes"),
                                    new JIntLiteral(4 * pushrate * mult))));
//                }
            }
        } else {
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
    
    /***************************************************************************
     *                      File I/O functions
     ***************************************************************************
     */
    
    /**
     * Adds the appropriate fopen(...) call for the file reader
     * @param filterNode
     */
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
 
    public void addFileReader(FilterSliceNode filterNode) {
        if (filterNode.getParent().getTail().isDuplicateSplitter()) {
            addInitStatement(makeReadBlock(filterNode, 0, true));
        } else {
            for (int i=0; i<filterNode.getParent().getTail().getWidth(); i++) {
                JBlock body = makeReadBlock(filterNode, i, false);
                addInitStatement(body);
            }
        }
    }
    
    private JBlock makeReadBlock(FilterSliceNode filterNode, int i, boolean isDuplicate) {
        int channelId = 
            CellBackend.channelIdMap.get(filterNode.getParent().getTail().getDestList()[i]);

        int numread;
        numread = filterNode.getFilter().getSteadyMult() * filterNode.getFilter().getPushInt();
        if (!isDuplicate)
            numread = numread * filterNode.getParent().getTail().getWeights()[i] / filterNode.getParent().getTail().totalWeights();
        
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
                new JFieldAccessExpression(
                        new JArrayAccessExpression(
                                new JFieldAccessExpression("channels"),
                                new JIntLiteral(channelId)),
                        "data");
              
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
                        new JIntLiteral(numread),
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
                        new JMethodCallExpression(
                                "&",
                                new JExpression[]{new JArrayAccessExpression(
                                        new JFieldAccessExpression("channels"),
                                        new JIntLiteral(channelId))}),
                            new JMultExpression(new JMultExpression(
                                                    new JIntLiteral(numread),
                                                    new JFieldAccessExpression(N)), 
                                                new JIntLiteral(4))
                })));
        return readBlock;
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
    
    public void addFileWriter(FilterSliceNode filterNode) {
        
        //initFileWriter(filterNode);
        for (int i=0; i<filterNode.getParent().getHead().getWidth(); i++) {
            int channelId =
                CellBackend.channelIdMap.get(filterNode.getParent().getHead().getSources()[i]);
            JBlock body = makeWriteBlock(filterNode, channelId, i);
            addCleanupStatement(body);
        }
        addCleanupStatement(new JExpressionStatement(
            new JMethodCallExpression("fclose", new JExpression[]{
                new JFieldAccessExpression(FILE_READER)})));

        addCleanupStatement(new JExpressionStatement(
            new JMethodCallExpression("fclose", new JExpression[]{
                new JFieldAccessExpression(FILE_WRITER)})));
    }
    
    private JBlock makeWriteBlock(FilterSliceNode filterNode, int channelId, int i) {

        FileOutputContent foc = (FileOutputContent) filterNode.getFilter();

        int numread;
        numread = filterNode.getFilter().getSteadyMult() * filterNode.getFilter().getPopInt();
        numread = numread * filterNode.getParent().getHead().getWeights()[i] / filterNode.getParent().getHead().totalWeights();

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
                new JFieldAccessExpression(
                        new JArrayAccessExpression(
                            new JFieldAccessExpression("channels"),
                            new JIntLiteral(channelId)),
                        "data");

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
                    new JIntLiteral(numread),
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
    
    /***************************************************************************
     *                      Miscellaneous functions
     ***************************************************************************
     */
    
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
        addInitStatementFirst(initcalls);
        //addMethod(new JMethodDeclaration(CStdType.Void, "__INIT_FUNC__", new JFormalParameter[0], initcalls));
    }
    
    /**
     * Add a group to print timing statistics after running is completed.
     *
     */
    public void printStats() {
        // make init statement - assign zero to <var>.  We need to use
        // an expression list statement to follow the convention of
        // other for loops and to get the codegen right.
        JVariableDefinition var = new JVariableDefinition(CStdType.Integer, "i");
        addCleanupStatement(new JVariableDeclarationStatement(var));
        
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
                new JAssignmentExpression(
                        new JFieldAccessExpression(GROUP),
                        new JMethodCallExpression(
                                SPU_NEW_GROUP,
                                new JExpression[]{new JLocalVariableExpression(var), new JIntLiteral(0)}))));
        body.addStatement(new JExpressionStatement(
                new JMethodCallExpression(
                        "spu_stats_print",
                        new JExpression[]{new JFieldAccessExpression(GROUP), new JIntLiteral(0), new JIntLiteral(0)})));
        body.addStatement(new JExpressionStatement(
                new JMethodCallExpression(
                        SPU_ISSUE_GROUP,
                        new JExpression[]{new JLocalVariableExpression(var), new JIntLiteral(0), new JIntLiteral(0)})));
        
        addCleanupStatement(new JForStatement(init, cond, incr, body));
        
        body = new JBlock();
        body.addStatement(new JExpressionStatement(
                new JMethodCallExpression("spulib_wait",
                        new JExpression[]{new JLocalVariableExpression(var), new JIntLiteral(1)})));
        addCleanupStatement(new JForStatement(init, cond, incr, body));

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
    
    /***************************************************************************
     *                          Util functions
     ***************************************************************************
     */
    
    /**
     * Insert a new line.
     */
    public void newline() {
        addInitStatement(new JExpressionStatement(new JEmittedTextExpression("")));
    }
    
    /**
     * Return the number of inputs to this slicenode.
     * 
     * @param sliceNode
     * @return
     */
    private static int getNumInputs(SliceNode sliceNode) {
        int inputs;
        if (sliceNode.isInputSlice()) {
            inputs = sliceNode.getAsInput().getWidth();
        }
        else {
            inputs = 1;
        }
        return inputs;
    }
    
    /**
     * Return the number of outputs of this slicenode.
     * 
     * @param sliceNode
     * @return
     */
    private static int getNumOutputs(SliceNode sliceNode) {
        int outputs;
        if (sliceNode.isInputSlice()) {
            outputs = 1;
        }
        else if (sliceNode.isFilterSlice()){
            if (sliceNode.getParent().getTail().isSplitter())
                outputs = 1;
            else outputs = sliceNode.getParent().getTail().getWidth();
        }
        else {
            if (sliceNode.getAsOutput().isDuplicateSplitter())
                outputs = 1;
            else outputs = sliceNode.getAsOutput().getWidth();
        }
        return outputs;
    }
    
    /**
     * Return the name of this slicenode. If it's a splitter or joiner, prepend
     * "splitter_" or "joiner_" to the name.
     * 
     * @param sliceNode
     * @return
     */
    private static String getFilterName(SliceNode sliceNode) {
        String basename = 
            sliceNode.getParent().getFilterNodes().get(0).getFilter().getName();
        if (sliceNode.isFilterSlice())
            return basename;
        else if (sliceNode.isInputSlice())
            return "joiner_" + basename;
        else return "splitter_" + basename;
    }
    
    @Deprecated
    private Integer getIdForSlice(Slice slice) {
        Integer newId = sliceIdMap.get(slice);
        if (newId == null) {
            newId = id;
            sliceIdMap.put(slice, newId);
            id++;
        }
        return newId;
    }
    
//    public class Buffer {
//        public final String name;
//        public final int index;
//        
//        public Buffer(String name, int index) {
//            this.name = name;
//            this.index = index;
//        }
//        
//        public String toString() {
//            return name + "[" + index + "]";
//        }
//    }
    
    private static final String SPU_ADDRESS = "SPU_ADDRESS";
    private static final String SPU_CMD_GROUP = "SPU_CMD_GROUP *";
    private static final String LS_ADDRESS = "LS_ADDRESS";
    private static final String WFA = "wf";
    private static final String INIT_WFA = "init_wf";
    private static final String WFA_PREFIX = "wf_";
    private static final String INIT_FUNC = "initfunc";
    private static final String INIT_FUNC_PREFIX = "init_";
    private static final String SPU_FILTER_DESC = "SPU_FILTER_DESC";
    private static final String SPU_FD = "fd";
    private static final String SPU_FD_WORK_FUNC = "work_func";
    private static final String SPU_FD_STATE_SIZE = "state_size";
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
    private static final String PPU_INPUT_BUFFER_SIZE = "pibs";
    private static final String PPU_INPUT_BUFFER_CB = "picb";
    private static final String PPU_OUTPUT_BUFFER_SIZE = "pobs";
    private static final String PPU_OUTPUT_BUFFER_CB = "pocb";
    private static final String DATA_ADDR = "da";
    private static final String FCB = "fcb";
    private static final String CB = "cb";
    private static final String SPUINIT = "spuinit";
    private static final String SPULIB_INIT = "spulib_init";
    private static final String SPULIB_WAIT = "spulib_wait";
    private static final String RUNSPERITER = "runsperiter";
    private static final String ITERS = "iters";
    private static final String SPUITERS = "spuiters";
    private static final String OUTSPUITEMS = "outspuitems";
    private static final String FILE_READER = "file_reader";
    private static final String FILE_WRITER = "file_writer";
    
    private static final String UINT32_T = "uint32_t";
    private static final String TICKS = "ticks";
    private static final String START = "start";
    private static final String STARTSPU = "startspu";
    private static final String PRINTF = "printf";
    
    private static final String N = "n", DONE = "done";
    
    private static final int FILLER = 128;
    private static final int FCB_SIZE = 128;
    private static final int BUFFER_OFFSET = 0;
    private static final int NO_DEPS = 0;
    private static final String WAIT_MASK = "0x3f";

    
    

    @Deprecated
    public void startNewFilter(InputSliceNode inputNode) {
        Integer id = getIdForSlice(inputNode.getParent());
        String fd = idToFd(id);
        
        JExpressionStatement newline = new JExpressionStatement(new JEmittedTextExpression(""));
        addInitStatement(newline);
        JExpressionStatement newFilter = new JExpressionStatement(new JEmittedTextExpression("// set up code for filter " + id));
        addInitStatement(newFilter);
    }
    
    @Deprecated
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
    
    @Deprecated
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
    
    @Deprecated
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
    
    @Deprecated
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
    
    @Deprecated
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
    
    @Deprecated
    public static String idToFd(Integer id) {
        return SPU_FD + id;
    }
    
    @Deprecated
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
     * Add init function address field: 
     * 
     * LS_ADDRESS init_[i];
     */
    @Deprecated
    public void addInitFunctionAddressField() {
        JVariableDefinition initfunc = new JVariableDefinition(
                new CArrayType(new CEmittedTextType(LS_ADDRESS),
                        1,
                        new JExpression[]{new JIntLiteral(CellBackend.numfilters)}),
                INIT_FUNC);
        JFieldDeclaration field = new JFieldDeclaration(initfunc);
        addField(field);
    }

    @Deprecated
    public void addInitFunctionAddresses(FilterSliceNode filterNode) {
        int filterId = CellBackend.filterIdMap.get(filterNode);
        addInitStatement(new JExpressionStatement(new JAssignmentExpression(
                new JArrayAccessExpression(new JFieldAccessExpression(INIT_FUNC),new JIntLiteral(filterId)),
                new JMethodCallExpression("&",new JExpression[]{new JEmittedTextExpression(INIT_FUNC_PREFIX+filterNode.getFilter().getName())}))));
    }
    @Deprecated
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
    
    @Deprecated
    public void addStartSpuTicks() {
        addInitStatement(new JExpressionStatement(new JAssignmentExpression(new JFieldAccessExpression(STARTSPU), new JMethodCallExpression(TICKS, new JExpression[0]))));
    }
    
    @Deprecated
    public void addPrintTicks() {
        addInitStatement(new JExpressionStatement(new JMethodCallExpression(PRINTF,
                new JExpression[]{new JStringLiteral("spu time: %d ms\\n"),
                new JMinusExpression(null, new JMethodCallExpression(TICKS, new JExpression[0]), new JFieldAccessExpression(STARTSPU))})));
        addInitStatement(new JExpressionStatement(new JMethodCallExpression(PRINTF,
                new JExpression[]{new JStringLiteral("time: %d ms\\n"),
                new JMinusExpression(null, new JMethodCallExpression(TICKS, new JExpression[0]), new JFieldAccessExpression(START))})));
        
    }

    @Deprecated
    public void initDuplicateOutputChannelArray(int filterId) {
        addField(new JFieldDeclaration(new JVariableDefinition(
                new CArrayType(new CEmittedTextType("BUFFER_CB *"),
                        1,
                        new JExpression[]{new JIntLiteral(1)}),
                        "output_" + filterId)));
    }
    
    @Deprecated
    public void initChannelArrays(FilterSliceNode filterNode,
            LinkedList<Integer> inputnums, LinkedList<Integer> outputnums) {
        InputSliceNode input = filterNode.getParent().getHead();
        OutputSliceNode output = filterNode.getParent().getTail();
        //initInputChannelArray(input, inputnums);
        //initOutputChannelArray(output, outputnums);
    }

    @Deprecated
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
    
    @Deprecated
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
    
//    public boolean inputBuffersSet(InputSliceNode inputNode) {
//        for (InterSliceEdge e : inputNode.getSourceList()) {
//            InterSliceEdge f = CellBackend.getEdgeBetween(e.getSrc(), inputNode);
//            if (!readyInputs.contains(o))
//        }
//    }
    
//    public boolean lookupInputBuffers(InputSliceNode inputNode) {
//        String filtername = inputNode.getNextFilter().getFilter().getName();
////        addField(new JFieldDeclaration(new JVariableDefinition(
////                new CArrayType(new CEmittedTextType("BUFFER_CB"),
////                               1,
////                               new JExpression[]{new JIntLiteral(inputNode.getWidth())}),
////                               "input_" + filtername)));
//        int i=0;
//        for (InterSliceEdge e : inputNode.getSourceSequence()) {
//            InterSliceEdge f = CellBackend.getEdgeBetween(e.getSrc(), inputNode);
//            if (!CellBackend.readyInputs.containsKey(f)) {
//                System.out.println("no input ready for " + filtername + f);
//                return false;
//            }
//            Buffer b = CellBackend.readyInputs.get(f);
//            int index;
//            if (f.getSrc().isDuplicateSplitter()) {
//                index = 0;
//            } else {
//                index = b.index;
//            }
//            addInitStatement(new JExpressionStatement(new JMethodCallExpression(
//                    "duplicate_buffer",
//                    new JExpression[]{
//                        new JMethodCallExpression(
//                                "&",
//                                new JExpression[]{
//                                    new JArrayAccessExpression(
//                                        new JFieldAccessExpression("input_"+filtername),
//                                        new JIntLiteral(i))}),
//                        new JMethodCallExpression(
//                                "&",
//                                new JExpression[]{
//                                    new JArrayAccessExpression(
//                                        new JFieldAccessExpression(b.name),
//                                        new JIntLiteral(index))})})));
//            addInitStatement(new JExpressionStatement(new JMethodCallExpression(
//                    "buf_set_head",
//                    new JExpression[]{
//                            new JMethodCallExpression(
//                                "&",
//                                new JExpression[]{
//                                    new JArrayAccessExpression(
//                                        new JFieldAccessExpression("input_"+filtername),
//                                        new JIntLiteral(i))}),
//                            new JIntLiteral(0)})));
//            i++;
//        }
//        if (inputNode.getSources().length > 0) {
//            addPPUInputBuffers("input_" + filtername);
//        }
//        return true;
//    }
    
    @Deprecated
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
    
//    public void addReadyBuffers(OutputSliceNode outputNode) {
//        String filtername = outputNode.getPrevFilter().getFilter().getName();
//        int i=0;
//        for (InterSliceEdge e : outputNode.getDestSequence()) {
//            Buffer b = new Buffer("output_" + filtername, i);
//            CellBackend.readyInputs.put(e, b);
//            System.out.println("adding buffer: " + e + "\n" + b + "\n" + e.getDest().getNextFilter().getFilter().getName());
//            i++;
//        }
//    }
    
    @Deprecated
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

}
