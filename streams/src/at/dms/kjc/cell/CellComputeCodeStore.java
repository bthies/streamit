package at.dms.kjc.cell;

import java.util.ArrayList;
import java.util.HashMap;

import at.dms.kjc.CArrayType;
import at.dms.kjc.CClassType;
import at.dms.kjc.CEmittedTextType;
import at.dms.kjc.CStdType;
import at.dms.kjc.Constants;
import at.dms.kjc.JArrayAccessExpression;
import at.dms.kjc.JAssignmentExpression;
import at.dms.kjc.JBlock;
import at.dms.kjc.JDivideExpression;
import at.dms.kjc.JEmittedTextExpression;
import at.dms.kjc.JExpression;
import at.dms.kjc.JExpressionStatement;
import at.dms.kjc.JFieldAccessExpression;
import at.dms.kjc.JFieldDeclaration;
import at.dms.kjc.JForStatement;
import at.dms.kjc.JFormalParameter;
import at.dms.kjc.JIntLiteral;
import at.dms.kjc.JLocalVariableExpression;
import at.dms.kjc.JMethodCallExpression;
import at.dms.kjc.JMethodDeclaration;
import at.dms.kjc.JMultExpression;
import at.dms.kjc.JPostfixExpression;
import at.dms.kjc.JRelationalExpression;
import at.dms.kjc.JStatement;
import at.dms.kjc.JStringLiteral;
import at.dms.kjc.JThisExpression;
import at.dms.kjc.JVariableDefinition;
import at.dms.kjc.KjcOptions;
import at.dms.kjc.backendSupport.ComputeCodeStore;
import at.dms.kjc.common.ALocalVariable;
import at.dms.kjc.sir.SIRPushExpression;
import at.dms.kjc.slicegraph.FileInputContent;
import at.dms.kjc.slicegraph.FileOutputContent;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.OutputSliceNode;
import at.dms.kjc.slicegraph.Slice;

public class CellComputeCodeStore extends ComputeCodeStore<CellPU> {
    
    static { if ("".equals(mainName)) mainName = "__MAIN__";}
    
    private HashMap<Slice,Integer> sliceIdMap = new HashMap<Slice,Integer>();
    
    private static final String SPU_ADDRESS = "SPU_ADDRESS";
    private static final String SPU_CMD_GROUP = "SPU_CMD_GROUP *";
    private static final String WFA = "LS_ADDRESS";
    private static final String WFA_PREFIX = "wf_";
    private static final String SPU_DATA_START = "spu_data_start";
    private static final String SPU_DATA_SIZE = "spu_data_size";
    private static final String SPU_FD = "SPU_FILTER_DESC";
    private static final String SPU_FD_PREFIX = "fd_";
    private static final String SPU_FD_WORK_FUNC = "work_func";
    private static final String SPU_FD_STATE_SIZE = "state_size";
    private static final String SPU_FD_STATE_ADDR = "state_addr";
    private static final String SPU_FD_NUM_INPUTS = "num_inputs";
    private static final String SPU_FD_NUM_OUTPUTS = "num_outputs";
    private static final String GROUP = "g_";
    private static final String SPU_NEW_GROUP = "spu_new_group";
    private static final String SPU_ISSUE_GROUP = "spu_issue_group";
    private static final String SPU_FILTER_LOAD = "spu_filter_load";
    private static final String SPU_FILTER_UNLOAD = "spu_filter_unload";
    private static final String SPU_FILTER_ATTACH_INPUT = "spu_filter_attach_input";
    private static final String SPU_FILTER_ATTACH_OUTPUT = "spu_filter_attach_output";
    private static final String SPU_BUFFER_ALLOC = "spu_buffer_alloc";
    private static final String INPUT_BUFFER_ADDR = "iba_";
    private static final String INPUT_BUFFER_SIZE = "ibs";
    private static final String OUTPUT_BUFFER_ADDR = "oba_";
    private static final String OUTPUT_BUFFER_SIZE = "obs";
    private static final String PPU_INPUT_BUFFER_ADDR = "piba_";
    private static final String PPU_INPUT_BUFFER_SIZE = "pibs";
    private static final String PPU_INPUT_BUFFER_CB = "picb_";
    private static final String PPU_OUTPUT_BUFFER_ADDR = "poba_";
    private static final String PPU_OUTPUT_BUFFER_SIZE = "pobs";
    private static final String PPU_OUTPUT_BUFFER_CB = "pocb_";
    private static final String DATA_ADDR = "da_";
    private static final String FCB = "fcb";
    private static final String CB = "cb";
    private static final String LS_SIZE = "LS_SIZE";
    private static final String CACHE_SIZE = "CACHE_SIZE";
    private static final String SPUINIT = "spuinit";
    private static final String SPULIB_INIT = "spulib_init";
    private static final String SPULIB_WAIT = "spulib_wait";
    private static final String ALLOC_BUFFER = "alloc_buffer";
    private static final String BUF_GET_CB = "buf_get_cb";
    
    private static final String UINT32_T = "uint32_t";
    private static final String PLUS = " + ";
    private static final String MINUS = " - ";
    private static final String INCLUDE = "#include";
    private static final String ROUND_UP = "ROUND_UP";
    
    private static final String N = "n", ND = "nd", DONE = "done";
    
    private static final int SPU_RESERVE_SIZE = 4096;
    private static final int FILLER = 128;
    private static final int FCB_SIZE = 128;
    private static final int BUFFER_OFFSET = 0;
    private static final int NO_DEPS = 0;
    private static final String WAIT_MASK = "0x1f";
    
    private ArrayList<JFieldDeclaration> workfuncs = new ArrayList<JFieldDeclaration>();
    private ArrayList<JFieldDeclaration> fds = new ArrayList<JFieldDeclaration>();
    
    private int id = 0;
    
    public CellComputeCodeStore(CellPU parent) {
        super(parent);
    }
    
    @Override
    protected void addSteadyLoop() {
        mainMethod.addStatement(steadyLoop);
    }
    
    /**
     *                        FIELDS
     */
    
    /**
     * Add work function address field: LS_ADDRESS wf_[i];
     * @param filterNode
     */
    public void addWorkFunctionAddressField(FilterSliceNode filterNode) {
        Integer id = getIdForSlice(filterNode.getParent());
        JVariableDefinition wf = new JVariableDefinition(
                new CEmittedTextType(WFA), WFA_PREFIX+id);
        JFieldDeclaration field = new JFieldDeclaration(wf);
        addField(field);
        workfuncs.add(id.intValue(), field);
    }
    
    /**
     * Add SPU filter description field: SPU_FILTER_DESC fd_[i];
     * @param filterNode
     */
    public void addSPUFilterDescriptionField(FilterSliceNode filterNode) {
        Integer id = getIdForSlice(filterNode.getParent());
        JVariableDefinition fd = new JVariableDefinition(
                new CEmittedTextType(SPU_FD), SPU_FD_PREFIX+id);
        JFieldDeclaration field = new JFieldDeclaration(fd);
        addField(field);
        fds.add(id.intValue(), field);
    }
    
    public void addDataAddressField(FilterSliceNode filterNode) {
        Integer id = getIdForSlice(filterNode.getParent());
        JVariableDefinition da = new JVariableDefinition(
                new CEmittedTextType(SPU_ADDRESS), DATA_ADDR+id);
        JFieldDeclaration field = new JFieldDeclaration(da);
        addField(field);
    }
    
    
    /**
     *                          SETUP
     */
    
    /**
     * Sets up filter description's work_func and state_size fields
     * @param filterNode
     */
    public void addFilterDescriptionSetup(FilterSliceNode filterNode) {
        Integer id = getIdForSlice(filterNode.getParent());
        String fd = idToFd(id);
        String wfid = workfuncs.get(id.intValue()).getVariable().getIdent();
        JExpressionStatement workfunc = new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JFieldAccessExpression(fd),SPU_FD_WORK_FUNC),
                new JEmittedTextExpression("(" + WFA + ")&" + wfid)));//JFieldAccessExpression(wfid)));
        addInitStatement(workfunc);
        JExpressionStatement statesize = new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JFieldAccessExpression(fd),SPU_FD_STATE_SIZE),
                new JIntLiteral(0)));
        addInitStatement(statesize);
    }
    
    /**
     * Sets up filter description's num_inputs field
     * @param inputNode
     */
    public void addFilterDescriptionSetup(InputSliceNode inputNode) {
        Integer id = getIdForSlice(inputNode.getParent());
        String fd = idToFd(id);
        JExpressionStatement numinputs = new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JFieldAccessExpression(fd),SPU_FD_NUM_INPUTS),
                new JIntLiteral(inputNode.getWidth())));
        addInitStatement(numinputs);
    }
    
    /**
     * Sets up filter description's num_outputs field
     * @param outputNode
     */
    public void addFilterDescriptionSetup(OutputSliceNode outputNode) {
        Integer id = getIdForSlice(outputNode.getParent());
        String fd = idToFd(id);
        JExpressionStatement numoutputs = new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JFieldAccessExpression(fd),SPU_FD_NUM_OUTPUTS),
                new JIntLiteral(outputNode.getWidth())));
        addInitStatement(numoutputs);
    }
    

    
    public void addCallBackFunction() {
        JFormalParameter tag = new JFormalParameter(new CEmittedTextType(UINT32_T), "tag");
        JBlock body = new JBlock();
        body.addStatement(new JExpressionStatement(new JPostfixExpression(Constants.OPE_POSTDEC, new JFieldAccessExpression(DONE))));
        JMethodDeclaration cb = new JMethodDeclaration(CStdType.Void, CB, new JFormalParameter[]{tag}, body);
        addMethod(cb);
    }
    
    public void addPSPLayout(FilterSliceNode filterNode) {
        Integer id = getIdForSlice(filterNode.getParent());
//        JNewArrayExpression la = new JNewArrayExpression(new CEmittedTextType("EXT_PSP_LAYOUT"), new JExpression[0]);
//        JNewArrayExpression ra = new JNewArrayExpression(new CEmittedTextType("EXT_PSP_RATES"), new JExpression[0]);
        
        JVariableDefinition l = new JVariableDefinition(new CArrayType(new CEmittedTextType("EXT_PSP_LAYOUT"), 1, new JExpression[]{new JIntLiteral(6)}), "l");
        JVariableDefinition r = new JVariableDefinition(new CArrayType(new CEmittedTextType("EXT_PSP_RATES"), 1, new JExpression[]{new JIntLiteral(6)}), "r");
        l.setInitializer(null);
//        r.setInitializer(ra);
        addField(new JFieldDeclaration(l));
        addField(new JFieldDeclaration(r));
        
//        JStatement init = new JExpressionStatement(new JEmittedTextExpression("int i=0"));
//        JExpression cond = new JEmittedTextExpression("i < 6");
//        JStatement incr = new JExpressionStatement(new JEmittedTextExpression("i++"));
        JBlock body = new JBlock();
        
        for (int i=0; i<6; i++) {
        
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JArrayAccessExpression(new JFieldAccessExpression(l.getIdent()), new JIntLiteral(i)), "cmd_id"),
                new JIntLiteral(5))));
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JArrayAccessExpression(new JFieldAccessExpression(l.getIdent()), new JIntLiteral(i)), "spu_id"),
                new JIntLiteral(i))));
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JArrayAccessExpression(new JFieldAccessExpression(l.getIdent()), new JIntLiteral(i)), "da"),
                new JEmittedTextExpression(DATA_ADDR + id))));
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JArrayAccessExpression(new JFieldAccessExpression(l.getIdent()), new JIntLiteral(i)), "spu_in_buf_data"),
                new JEmittedTextExpression(INPUT_BUFFER_ADDR + id + "_0"))));
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JArrayAccessExpression(new JFieldAccessExpression(l.getIdent()), new JIntLiteral(i)), "spu_out_buf_data"),
                new JEmittedTextExpression(OUTPUT_BUFFER_ADDR + id + "_0"))));
        
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JArrayAccessExpression(new JFieldAccessExpression(l.getIdent()), new JIntLiteral(i)), "ppu_in_buf_data"),
                new JEmittedTextExpression(PPU_INPUT_BUFFER_ADDR + i))));
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JArrayAccessExpression(new JFieldAccessExpression(l.getIdent()), new JIntLiteral(i)), "ppu_out_buf_data"),
                new JEmittedTextExpression(PPU_OUTPUT_BUFFER_ADDR + i))));
        
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JArrayAccessExpression(new JFieldAccessExpression(l.getIdent()), new JIntLiteral(i)), "filt"),
                new JEmittedTextExpression(FCB))));
        
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JArrayAccessExpression(new JFieldAccessExpression(r.getIdent()), new JIntLiteral(i)), "in_bytes"),
                new JIntLiteral(128))));
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JArrayAccessExpression(new JFieldAccessExpression(r.getIdent()), new JIntLiteral(i)), "run_iters"),
                new JIntLiteral(32))));
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JArrayAccessExpression(new JFieldAccessExpression(r.getIdent()), new JIntLiteral(i)), "out_bytes"),
                new JIntLiteral(128))));
        }
        addInitStatement(body);
//        JForStatement forloop = new JForStatement(init, cond, incr, body);
//        addInitStatement(forloop);
    }
    
    public void addDataParallel() {
        JMethodCallExpression dataparallel = new JMethodCallExpression(
                "ext_data_parallel",
                new JExpression[]{
                        new JIntLiteral(6),
                        new JEmittedTextExpression("l"),
                        new JEmittedTextExpression("r"),
                        new JDivideExpression(null, new JFieldAccessExpression(N), new JIntLiteral(6)),
                        new JEmittedTextExpression("cb"),
                        new JIntLiteral(0)
                });
        addInitStatement(new JExpressionStatement(dataparallel));
        addInitStatement(new JExpressionStatement(new JMethodCallExpression(
                "spulib_poll_while",
                new JExpression[]{
                        new JRelationalExpression(Constants.OPE_GT, new JFieldAccessExpression(DONE), new JIntLiteral(0))
                })));
    }
    
    public void addIssueUnload(FilterSliceNode filterNode) {
        Integer id = getIdForSlice(filterNode.getParent());
        
        addInitStatement(new JExpressionStatement(new JEmittedTextExpression(
            "spu_issue_group(" + id + ", 1, " + DATA_ADDR + id + ")")));
        addInitStatement(new JExpressionStatement(new JEmittedTextExpression(
            "spulib_wait(" + id + ", 1)")));
    }
    
    public void addSPUInit() {
        // spuinit()
        JExpressionStatement spuinit = new JExpressionStatement(
                new JMethodCallExpression(
                        SPUINIT,
                        new JExpression[]{}));
        addInitStatement(spuinit);
        
//        // spu_data_start
//        JExpressionStatement spudatastart = new JExpressionStatement(new JAssignmentExpression(
//                new JEmittedTextExpression(SPU_DATA_START),
//                new JMethodCallExpression(ROUND_UP, new JExpression[]{
//                        new JEmittedTextExpression(SPU_DATA_START),
//                        new JEmittedTextExpression(CACHE_SIZE)
//                })));
//        addInitStatement(spudatastart);
//        
//        // spu_data_size
//        JExpressionStatement spudatasize = new JExpressionStatement(new JAssignmentExpression(
//                new JEmittedTextExpression(SPU_DATA_SIZE),
//                new JEmittedTextExpression(LS_SIZE + MINUS + SPU_RESERVE_SIZE + MINUS + SPU_DATA_START)));
//        addInitStatement(spudatasize);
        
        // spu_lib_init()
        JExpressionStatement spulibinit = new JExpressionStatement(
                new JMethodCallExpression(
                        SPULIB_INIT,
                        new JExpression[]{}));
        addInitStatement(spulibinit);
        
        JVariableDefinition group = new JVariableDefinition(new CEmittedTextType(SPU_CMD_GROUP), GROUP);
        JFieldDeclaration field = new JFieldDeclaration(group);
        addField(field);
        
        // SPU_ADDRESS fcb = 0;
        JVariableDefinition fcb = new JVariableDefinition(new CEmittedTextType(SPU_ADDRESS), FCB);
        fcb.setInitializer(new JIntLiteral(0));
        field = new JFieldDeclaration(fcb);
        addField(field);
        
        // uint32_t ibs = 32 * 1024;
        JVariableDefinition ibs = new JVariableDefinition(new CEmittedTextType(UINT32_T), INPUT_BUFFER_SIZE);
        ibs.setInitializer(new JEmittedTextExpression("32 * 1024"));
        addField(new JFieldDeclaration(ibs));

        // uint32_t obs = 32 * 1024;
        JVariableDefinition obs = new JVariableDefinition(new CEmittedTextType(UINT32_T), OUTPUT_BUFFER_SIZE);
        obs.setInitializer(new JEmittedTextExpression("32 * 1024"));
        addField(new JFieldDeclaration(obs));

        for (int i=0; i<6; i++) { 
            // void * piba;
            JVariableDefinition piba = new JVariableDefinition(new CEmittedTextType("void *"), PPU_INPUT_BUFFER_ADDR+i);
            addField(new JFieldDeclaration(piba));
    
            // void * poba;
            JVariableDefinition poba = new JVariableDefinition(new CEmittedTextType("void *"), PPU_OUTPUT_BUFFER_ADDR+i);
            addField(new JFieldDeclaration(poba));
    
            // BUFFER_CB * picb;
            JVariableDefinition picb = new JVariableDefinition(new CEmittedTextType("BUFFER_CB *"), PPU_INPUT_BUFFER_CB+i);
            addField(new JFieldDeclaration(picb));
    
            // BUFFER_CB * pocb;
            JVariableDefinition pocb = new JVariableDefinition(new CEmittedTextType("BUFFER_CB *"), PPU_OUTPUT_BUFFER_CB+i);
            addField(new JFieldDeclaration(pocb));
        }
        
        // uint32_t pibs = 32 * 1024;
        JVariableDefinition pibs = new JVariableDefinition(new CEmittedTextType(UINT32_T), PPU_INPUT_BUFFER_SIZE);
        pibs.setInitializer(new JEmittedTextExpression("32 * 1024"));
        addField(new JFieldDeclaration(pibs));
        
        // uint32_t pobs = 32 * 1024;
        JVariableDefinition pobs = new JVariableDefinition(new CEmittedTextType(UINT32_T), PPU_OUTPUT_BUFFER_SIZE);
        pobs.setInitializer(new JEmittedTextExpression("32 * 1024"));
        addField(new JFieldDeclaration(pobs));
        
        // int nd;
        JVariableDefinition nd = new JVariableDefinition(new CEmittedTextType("int"), ND);
        //nd.setInitializer(new JMultExpression(new JFieldAccessExpression(N), new JIntLiteral(32)));
        addField(new JFieldDeclaration(nd));
        
        // int n = 96;
        JVariableDefinition n = new JVariableDefinition(new CEmittedTextType("int"), N);
        n.setInitializer(new JIntLiteral(96));
        addField(new JFieldDeclaration(n));
        
        // int done = 1;
        JVariableDefinition done = new JVariableDefinition(new CEmittedTextType("int"), DONE);
        done.setInitializer(new JIntLiteral(1));
        addField(new JFieldDeclaration(done));
        
        
        
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
    public void setupInputBufferAddresses(InputSliceNode inputNode) {
        if (inputNode.getWidth() == 0) return;
        
        Integer id = getIdForSlice(inputNode.getParent());
        
        // name of first input buffer
        String buffaddr = INPUT_BUFFER_ADDR + id + "_" + 0;
        
        // add field for first input buffer
        JVariableDefinition iba = new JVariableDefinition(
                new CEmittedTextType(SPU_ADDRESS), buffaddr);
        JFieldDeclaration field = new JFieldDeclaration(iba);
        addField(field);
        
        // set up address for first input buffer
        JExpressionStatement expr = new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(buffaddr),
                new JEmittedTextExpression(FCB + PLUS + FCB_SIZE + PLUS + FILLER)));
        addInitStatement(expr);
        
        // set up remaining input buffers
        for (int i=1; i<inputNode.getWidth(); i++) {
            String prev = new String(buffaddr);
            String prevsize = INPUT_BUFFER_SIZE;
            buffaddr = INPUT_BUFFER_ADDR + id + "_" + i;
            
            iba = new JVariableDefinition(
                    new CEmittedTextType(SPU_ADDRESS), buffaddr);
            field = new JFieldDeclaration(iba);
            addField(field);
            
            expr = new JExpressionStatement(new JAssignmentExpression(
                    new JFieldAccessExpression(buffaddr),
                    new JEmittedTextExpression(prev + PLUS + prevsize + PLUS + FILLER)));
            addInitStatement(expr);
        }
    }
    
    public void setupOutputBufferAddresses(OutputSliceNode outputNode) {
        if (outputNode.getWidth() == 0) return;
        
        Integer id = getIdForSlice(outputNode.getParent());
        int numinputs = outputNode.getParent().getHead().getWidth();
        
        String buffaddr = OUTPUT_BUFFER_ADDR + id + "_" + 0;
        String prev, prevsize;
        JExpressionStatement expr;
        
        // no input buffers
        if (numinputs == 0) {
            prev = FCB;
            prevsize = String.valueOf(FCB_SIZE);
        } else {
            prev = INPUT_BUFFER_ADDR + id + "_" + (numinputs-1);
            prevsize = INPUT_BUFFER_SIZE;
        }
        
        JVariableDefinition oba = new JVariableDefinition(
                new CEmittedTextType(SPU_ADDRESS), buffaddr);
        JFieldDeclaration field = new JFieldDeclaration(oba);
        addField(field);

        expr = new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(buffaddr),
                new JEmittedTextExpression(prev + PLUS + prevsize + PLUS + FILLER)));
        addInitStatement(expr);
        
        for (int i=1; i<outputNode.getWidth(); i++) {
            prev = new String(buffaddr);
            prevsize = OUTPUT_BUFFER_SIZE;
            buffaddr = OUTPUT_BUFFER_ADDR + id + "_" + i;
            
            oba = new JVariableDefinition(
                    new CEmittedTextType(SPU_ADDRESS), buffaddr);
            field = new JFieldDeclaration(oba);
            addField(field);
            
            expr = new JExpressionStatement(new JAssignmentExpression(
                    new JEmittedTextExpression(buffaddr),
                    new JEmittedTextExpression(prev + PLUS + prevsize + PLUS + FILLER)));
            addInitStatement(expr);
        }
    }
    
    public void addPPUBuffers() {
        for (int i=0; i<6; i++) {
            JExpressionStatement inputaddr = new JExpressionStatement(new JAssignmentExpression(
                    new JFieldAccessExpression(PPU_INPUT_BUFFER_ADDR+i),
                    new JMethodCallExpression(
                            ALLOC_BUFFER,
                            new JExpression[]{
                                    new JFieldAccessExpression(PPU_INPUT_BUFFER_SIZE),
                                    new JIntLiteral(0)})));
            addInitStatement(inputaddr);
            JExpressionStatement outputaddr = new JExpressionStatement(new JAssignmentExpression(
                    new JFieldAccessExpression(PPU_OUTPUT_BUFFER_ADDR+i),
                    new JMethodCallExpression(
                            ALLOC_BUFFER,
                            new JExpression[]{
                                    new JFieldAccessExpression(PPU_OUTPUT_BUFFER_SIZE),
                                    new JIntLiteral(0)})));
            addInitStatement(outputaddr);
            JExpressionStatement inputcb = new JExpressionStatement(new JAssignmentExpression(
                    new JFieldAccessExpression(PPU_INPUT_BUFFER_CB+i),
                    new JMethodCallExpression(
                            BUF_GET_CB,
                            new JExpression[]{
                                    new JFieldAccessExpression(PPU_INPUT_BUFFER_ADDR+i)})));
            addInitStatement(inputcb);
            JExpressionStatement outputcb = new JExpressionStatement(new JAssignmentExpression(
                    new JFieldAccessExpression(PPU_OUTPUT_BUFFER_CB+i),
                    new JMethodCallExpression(
                            BUF_GET_CB,
                            new JExpression[]{
                                    new JFieldAccessExpression(PPU_OUTPUT_BUFFER_ADDR+i)})));
            addInitStatement(outputcb);
        }
    }
    
    public void setupDataAddress(FilterSliceNode filterNode) {
        Integer id = getIdForSlice(filterNode.getParent());
        
        int numinputs = filterNode.getParent().getHead().getWidth();
        int numoutputs = filterNode.getParent().getTail().getWidth();
        
        String dataaddr = DATA_ADDR + id;
        String lastaddr = FCB;
        String lastsize = String.valueOf(FCB_SIZE);
        
        if (numoutputs > 0) {
            lastaddr = OUTPUT_BUFFER_ADDR + id + "_" + (numoutputs-1);
            lastsize = OUTPUT_BUFFER_SIZE;
        } else if (numinputs > 0) {
            lastaddr = INPUT_BUFFER_ADDR + id + "_" + (numinputs-1);
            lastsize = INPUT_BUFFER_SIZE;
        }
        
        JExpressionStatement expr = new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(dataaddr),
                new JEmittedTextExpression(lastaddr + PLUS + lastsize + PLUS + FILLER)));
        addInitStatement(expr);
    }
    
    public void startNewFilter(InputSliceNode inputNode) {
        Integer id = getIdForSlice(inputNode.getParent());
        String fd = idToFd(id);
        
        JExpressionStatement newline = new JExpressionStatement(new JEmittedTextExpression(""));
        addInitStatement(newline);
        JExpressionStatement newFilter = new JExpressionStatement(new JEmittedTextExpression("// set up code for filter " + id));
        addInitStatement(newFilter);
    }
    
    public void addNewGroupStatement(InputSliceNode inputNode) {
        Integer id = getIdForSlice(inputNode.getParent());
        String fd = idToFd(id);
        
        JStatement init = new JExpressionStatement(new JEmittedTextExpression("int i=0"));
        JExpression cond = new JEmittedTextExpression("i < 6");
        JStatement incr = new JExpressionStatement(new JEmittedTextExpression("i++"));
        JBlock body = new JBlock();
        
        JExpressionStatement newGroup = new JExpressionStatement(new JAssignmentExpression(
                new JEmittedTextExpression(GROUP),
                new JMethodCallExpression(SPU_NEW_GROUP,new JExpression[]{new JEmittedTextExpression("i"), new JIntLiteral(0)})));
        
        body.addStatement(newGroup);
        
        JExpressionStatement filterLoad = new JExpressionStatement(new JMethodCallExpression(
                SPU_FILTER_LOAD,
                new JExpression[]{
                        new JFieldAccessExpression(GROUP),
                        new JFieldAccessExpression(FCB),
                        new JEmittedTextExpression("&" + fd),
                        new JIntLiteral(0),
                        new JIntLiteral(NO_DEPS)
                }));
        body.addStatement(filterLoad);
        
        int cmdId = 1;
        for (int i=0; i<inputNode.getWidth(); i++) {
            String buffaddr = INPUT_BUFFER_ADDR + id + "_" + i;
            String buffsize = INPUT_BUFFER_SIZE;
            JExpressionStatement bufferAlloc = new JExpressionStatement(new JMethodCallExpression(
                    SPU_BUFFER_ALLOC, new JExpression[]{
                            new JEmittedTextExpression(GROUP), new JEmittedTextExpression(buffaddr), new JFieldAccessExpression(buffsize),
                            new JIntLiteral(BUFFER_OFFSET), new JIntLiteral(cmdId), new JIntLiteral(NO_DEPS)}));
            body.addStatement(bufferAlloc);
            JExpressionStatement attachBuffer = new JExpressionStatement(new JMethodCallExpression(
                    SPU_FILTER_ATTACH_INPUT, new JExpression[]{
                            new JEmittedTextExpression(GROUP),
                            new JFieldAccessExpression(FCB),
                            new JIntLiteral(i),
                            new JEmittedTextExpression(buffaddr),
                            new JIntLiteral(cmdId + inputNode.getWidth()),
                            new JIntLiteral(2),
                            new JIntLiteral(0),
                            new JIntLiteral(cmdId)
                    }));
            body.addStatement(attachBuffer);
            cmdId++;
        }
        
        OutputSliceNode outputNode = inputNode.getParent().getTail();
        cmdId = 2*inputNode.getWidth() + 1;
        for (int i=0; i<outputNode.getWidth(); i++) {
            String buffaddr = OUTPUT_BUFFER_ADDR + id + "_" + i;
            String buffsize = OUTPUT_BUFFER_SIZE;
            JExpressionStatement bufferAlloc = new JExpressionStatement(new JMethodCallExpression(
                    SPU_BUFFER_ALLOC, new JExpression[]{
                            new JEmittedTextExpression(GROUP), new JEmittedTextExpression(buffaddr), new JFieldAccessExpression(buffsize),
                            new JIntLiteral(BUFFER_OFFSET), new JIntLiteral(cmdId), new JIntLiteral(NO_DEPS)}));
            body.addStatement(bufferAlloc);
            JExpressionStatement attachBuffer = new JExpressionStatement(new JMethodCallExpression(
                    SPU_FILTER_ATTACH_OUTPUT, new JExpression[]{
                            new JEmittedTextExpression(GROUP),
                            new JFieldAccessExpression(FCB),
                            new JIntLiteral(i),
                            new JEmittedTextExpression(buffaddr),
                            new JIntLiteral(cmdId + outputNode.getWidth()),
                            new JIntLiteral(2),
                            new JIntLiteral(0),
                            new JIntLiteral(cmdId)
                    }));
            body.addStatement(attachBuffer);
            cmdId++;
        }   
        
        newGroup = new JExpressionStatement(new JAssignmentExpression(
                new JEmittedTextExpression(GROUP),
                new JMethodCallExpression(SPU_NEW_GROUP,new JExpression[]{new JEmittedTextExpression("i"), new JIntLiteral(1)})));
        body.addStatement(newGroup);
        
        JExpressionStatement filterUnload = new JExpressionStatement(new JMethodCallExpression(
                SPU_FILTER_UNLOAD, new JExpression[]{
                        new JEmittedTextExpression(GROUP),
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
    
    public void addIssueGroupAndWait(FilterSliceNode filterNode) {
        Integer id = getIdForSlice(filterNode.getParent());
        
        JStatement init = new JExpressionStatement(new JEmittedTextExpression("int i=0"));
        JExpression cond = new JEmittedTextExpression("i < 6");
        JStatement incr = new JExpressionStatement(new JEmittedTextExpression("i++"));
        JBlock body = new JBlock();
        
        JExpressionStatement issuegroup = new JExpressionStatement(new JMethodCallExpression(
                SPU_ISSUE_GROUP, new JExpression[]{
                        new JEmittedTextExpression("i"),
                        new JIntLiteral(0),
                        new JFieldAccessExpression(DATA_ADDR + id)}));
        body.addStatement(issuegroup);
        
        JExpressionStatement wait = new JExpressionStatement(new JMethodCallExpression(
                SPULIB_WAIT, new JExpression[]{
                        new JEmittedTextExpression("i"),
                        new JEmittedTextExpression(WAIT_MASK)}));
        body.addStatement(wait);
        
        JForStatement forloop = new JForStatement(init, cond, incr, body);
        addInitStatement(forloop);
    }
 
    public void addFileReader(FilterSliceNode filterNode) {
        int pushrate = 1;
        
        FileInputContent fic = (FileInputContent) filterNode.getFilter();
        
        String fileVar = "_fileReader_";
        
        //create fields
        JFieldDeclaration file = 
            new JFieldDeclaration(null,
                                  new JVariableDefinition(null,
                                                          0,
                                                          new CEmittedTextType("FILE *"),
                                                          fileVar,
                                                          null),
                                  null, null);
        addField(file);
        //create init function
        JBlock initBlock = new JBlock(null, new JStatement[0], null);
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
        //set this as the init function...
        JMethodDeclaration initMethod = new JMethodDeclaration(null,
                at.dms.kjc.Constants.ACC_PUBLIC,
                CStdType.Void,
                "init_fileread",
                JFormalParameter.EMPTY,
                CClassType.EMPTY,
                initBlock,
                null,
                null);
        //addInitFunctionCall(initMethod);
        
        //create work function
        JBlock workBlock = new JBlock(null, new JStatement[0], null);
        ALocalVariable tmp = ALocalVariable.makeTmp(fic.getOutputType());
        addInitStatement(tmp.getDecl());
        
        // RMR { support ascii or binary file operations for reading
        JMethodCallExpression fileio;

        if (KjcOptions.asciifileio) {
            //create a temp variable to hold the value we are reading
            JExpression[] fscanfParams = new JExpression[3];
            //create the params for fscanf
            fscanfParams[0] = new JFieldAccessExpression(null, new JThisExpression(null),
                                                         file.getVariable().getIdent());
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
            // create the params for fread(&variable, sizeof(type), 1, file)
            JExpression[] freadParams = new JExpression[4];
            
            // the first parameter: &(variable); treat the & operator as a function call
            JExpression[] addressofParameters = new JExpression[1];
            
            addressofParameters[0] = tmp.getRef();
            
            JMethodCallExpression addressofCall =
                new JMethodCallExpression(null, "&", addressofParameters);
            
            freadParams[0] = addressofCall;
            
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
            
            // the third parameter: read one element at a time
            freadParams[2] = new JIntLiteral(pushrate);
            
            // the last parameter: the file pointer
            freadParams[3] = new JFieldAccessExpression(null, new JThisExpression(null),
                                                        file.getVariable().getIdent());
            
            JMethodCallExpression fread = 
                new JMethodCallExpression(null, new JThisExpression(null),
                                          "fread",
                                          freadParams);

            fileio = fread;
        }
        // } RMR
        
//        JExpressionStatement expr = new JExpressionStatement(new JAssignmentExpression(
//                new JFieldAccessExpression(ND),
//                new 
//        addInitStatement(new JExpressionStatement(new JEmittedTextExpression("nd = n * 32")));
//    
        JStatement nd = new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(ND), new JEmittedTextExpression("n * 32 / 6")));
        addInitStatement(nd);
        JStatement forinit = new JExpressionStatement(new JEmittedTextExpression("int i = 0"));
        JExpression forcond = new JRelationalExpression(Constants.OPE_LT, new JEmittedTextExpression("i"), new JFieldAccessExpression(ND));//new JEmittedTextExpression("i < nd");
        JStatement forinc = new JExpressionStatement(new JEmittedTextExpression("i++"));
        JBlock forbody = new JBlock();
        for (int i=0; i<6; i++) {
            forbody.addStatement(new JExpressionStatement(fileio));
            JExpressionStatement set = new JExpressionStatement(new JAssignmentExpression(
                    new JEmittedTextExpression("((int *)" + PPU_INPUT_BUFFER_ADDR + i + ")[i]"),
                    tmp.getRef()));
            forbody.addStatement(set);
        }
        JForStatement readloop = new JForStatement(forinit, forcond, forinc, forbody);
        addInitStatement(readloop);
        
        for (int i=0; i<6; i++) {
            addInitStatement(new JExpressionStatement(new JEmittedTextExpression(PPU_INPUT_BUFFER_CB + i + "->tail = nd * 4")));
            addInitStatement(new JExpressionStatement(new JEmittedTextExpression("IF_CHECK(" + PPU_INPUT_BUFFER_CB + i + "->otail = " + PPU_INPUT_BUFFER_CB + i + "->tail)")));
        }
        SIRPushExpression push = 
            new SIRPushExpression(tmp.getRef(), fic.getOutputType());
        
        //addInitStatement(new JExpressionStatement(null, push, null));

        JMethodDeclaration workMethod = new JMethodDeclaration(null,
                at.dms.kjc.Constants.ACC_PUBLIC,
                CStdType.Void,
                "work_fileread",
                JFormalParameter.EMPTY,
                CClassType.EMPTY,
                workBlock,
                null,
                null);
        workMethod.setPop(0);
        workMethod.setPeek(0);
        workMethod.setPush(pushrate);
        //addInitFunctionCall(workMethod);

    }
    
    public void addFileWriter(FilterSliceNode filterNode) {
        String fileVar = "_fileWriter_";
    
        FileOutputContent foc = (FileOutputContent) filterNode.getFilter();
        
        //this.outputType = CStdType.Void;

        JVariableDefinition fileDefn = new JVariableDefinition(null,
                0,
                new CEmittedTextType("FILE *"),
                fileVar,
                null);
        //create fields
        JFieldDeclaration file = 
            new JFieldDeclaration(null,fileDefn, null, null);
        addField(file);
    
//        //create init function
//        JBlock initBlock = new JBlock(null, new JStatement[0], null);
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
//        //set this as the init function...
//        JMethodDeclaration initMethod = new JMethodDeclaration(null,
//                at.dms.kjc.Constants.ACC_PUBLIC,
//                CStdType.Void,
//                "init_filewrite",
//                JFormalParameter.EMPTY,
//                CClassType.EMPTY,
//                initBlock,
//                null,
//                null);
//        addInitStatement(initBlock);
    
//        //create work function
//        JBlock workBlock = new JBlock(null, new JStatement[0], null);
    
//        SIRPopExpression pop = new SIRPopExpression(foc.getInputType());
        ALocalVariable tmp = ALocalVariable.makeTmp(foc.getInputType());
        addInitStatement(tmp.getDecl());
//        workBlock.addStatement(tmp.getDecl());
//        workBlock.addStatement(new JExpressionStatement(new JAssignmentExpression(tmp.getRef(),pop)));
    
    
        // RMR { support ascii or binary file operations for reading
        JMethodCallExpression fileio;

        if (KjcOptions.asciifileio) {
            //the params for the fprintf call
            JExpression[] fprintfParams = new JExpression[3];
            fprintfParams[0] = new JFieldAccessExpression(null, new JThisExpression(null),
                                                          fileDefn.getIdent());
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
            
            // create the params for fwrite(&variable, sizeof(type), 1, file)
            JExpression[] fwriteParams = new JExpression[4];
            
            // the first parameter: &(variable); treat the & operator as a function call
            JExpression[] addressofParameters = new JExpression[1];
            
            addressofParameters[0] = tmp.getRef();
            
            JMethodCallExpression addressofCall =
                new JMethodCallExpression(null, "&", addressofParameters);

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
            
            // the third parameter: read one element at a time
            fwriteParams[2] = new JIntLiteral(1);
            
            // the last parameter: the file pointer
            fwriteParams[3] = new JFieldAccessExpression(null, new JThisExpression(null),
                                                         fileDefn.getIdent());
            
            JMethodCallExpression fwrite = 
                new JMethodCallExpression(null, new JThisExpression(null),
                                          "fwrite",
                                          fwriteParams);
            fwrite.setTapeType(CStdType.Void);

            fileio = fwrite;
        }
        
        
//        JStatement nd = new JExpressionStatement(new JAssignmentExpression(
//                new JFieldAccessExpression(ND), new JEmittedTextExpression("n * 32")));
//        addInitStatement(nd);
        JStatement forinit = new JExpressionStatement(new JEmittedTextExpression("int i = 0"));
        JExpression forcond = new JRelationalExpression(Constants.OPE_LT, new JEmittedTextExpression("i"), new JFieldAccessExpression(ND));//new JEmittedTextExpression("i < nd");
        JStatement forinc = new JExpressionStatement(new JEmittedTextExpression("i++"));
        JBlock forbody = new JBlock();
        for (int i=0; i<6; i++) {
            JExpressionStatement set = new JExpressionStatement(new JAssignmentExpression(
                    tmp.getRef(),
                    new JEmittedTextExpression("((int *)" + PPU_OUTPUT_BUFFER_ADDR + i + ")[i]")
                    ));
            forbody.addStatement(set);
            forbody.addStatement(new JExpressionStatement(fileio));
        }
        JForStatement writeloop = new JForStatement(forinit, forcond, forinc, forbody);
        addInitStatement(writeloop);
        
//        JMethodDeclaration workMethod = new JMethodDeclaration(null,
//                at.dms.kjc.Constants.ACC_PUBLIC,
//                CStdType.Void,
//                "work_filewrite",
//                JFormalParameter.EMPTY,
//                CClassType.EMPTY,
//                workBlock,
//                null,
//                null);
//
//        workMethod.setPop(1);
//        workMethod.setPeek(1);
//        workMethod.setPush(0);
//        
//        addInitStatement(workBlock);
        
        JMethodCallExpression close = 
            new JMethodCallExpression(null, new JThisExpression(null),
                                      "fclose", new JExpression[]{
                new JFieldAccessExpression(null,
                        new JThisExpression(null),
                        fileDefn.getIdent())
            });
//        JBlock body = new JBlock();
//        body.addStatement(new JExpressionStatement(close));
        addInitStatement(new JExpressionStatement(close));
        
//        closeMethod = new JMethodDeclaration(null,
//                at.dms.kjc.Constants.ACC_PUBLIC,
//                CStdType.Void,
//                "close_filewrite" + my_unique_ID ,
//                JFormalParameter.EMPTY,
//                CClassType.EMPTY,
//                body,
//                null,
//                null);

        // discard old dummy methods and set up the new ones.
        //this.setTheMethods(new JMethodDeclaration[]{initMethod,workMethod,closeMethod});
    }
    

    
    public static String idToFd(Integer id) {
        return SPU_FD_PREFIX + id;
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
}
