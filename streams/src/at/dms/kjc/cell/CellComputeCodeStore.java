package at.dms.kjc.cell;

import java.util.ArrayList;
import java.util.HashMap;

import at.dms.kjc.CArrayType;
import at.dms.kjc.CClassType;
import at.dms.kjc.CEmittedTextType;
import at.dms.kjc.CStdType;
import at.dms.kjc.Constants;
import at.dms.kjc.JAddExpression;
import at.dms.kjc.JArrayAccessExpression;
import at.dms.kjc.JAssignmentExpression;
import at.dms.kjc.JBlock;
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
import at.dms.kjc.JSuperExpression;
import at.dms.kjc.JThisExpression;
import at.dms.kjc.JVariableDeclarationStatement;
import at.dms.kjc.JVariableDefinition;
import at.dms.kjc.KjcOptions;
import at.dms.kjc.backendSupport.ComputeCodeStore;
import at.dms.kjc.backendSupport.SpaceTimeScheduleAndPartitioner;
import at.dms.kjc.common.ALocalVariable;
import at.dms.kjc.slicegraph.FileInputContent;
import at.dms.kjc.slicegraph.FileOutputContent;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.OutputSliceNode;
import at.dms.kjc.slicegraph.Slice;

public class CellComputeCodeStore extends ComputeCodeStore<CellPU> {
    
    static { if ("".equals(mainName)) mainName = "__MAIN__";}
    
    private static final int numspus = 6;
    
    private HashMap<Slice,Integer> sliceIdMap = new HashMap<Slice,Integer>();
    
    private static final String SPU_ADDRESS = "SPU_ADDRESS";
    private static final String SPU_CMD_GROUP = "SPU_CMD_GROUP *";
    private static final String LS_ADDRESS = "LS_ADDRESS";
    private static final String WFA = "wf";
    private static final String WFA_PREFIX = "wf_";
    private static final String INIT_FUNC = "initfunc";
    private static final String INIT_FUNC_PREFIX = "init_";
    private static final String SPU_DATA_START = "spu_data_start";
    private static final String SPU_DATA_SIZE = "spu_data_size";
    private static final String SPU_FILTER_DESC = "SPU_FILTER_DESC";
    private static final String SPU_FD = "fd";
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
    
    private ArrayList<JFieldDeclaration> workfuncs = new ArrayList<JFieldDeclaration>();
    private ArrayList<JFieldDeclaration> fds = new ArrayList<JFieldDeclaration>();
    private ArrayList<JMethodDeclaration> initfuncs = new ArrayList<JMethodDeclaration>();
    
    private int id = 0;
    
    private SpaceTimeScheduleAndPartitioner schedule;
    private int poprate;
    private int pushrate;
    private String outputType;
    private String inputType;
    
    private JVariableDefinition l, r;
    
    public CellComputeCodeStore(CellPU parent) {
        super(parent);
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
    
    /**
     *                        FIELDS
     */
    
    /**
     * Add work function address field: LS_ADDRESS wf_[i];
     */
    public void addWorkFunctionAddressField() {
        JVariableDefinition wf = new JVariableDefinition(
                new CArrayType(new CEmittedTextType(LS_ADDRESS),1,new JExpression[]{new JIntLiteral(numspus)}), WFA);
        JFieldDeclaration field = new JFieldDeclaration(wf);
        addField(field);
        //System.out.println(id + filterNode.toString());
        //workfuncs.add(id.intValue()-1, field);
        for (int i=0; i<numspus; i++) {
            addInitStatement(new JExpressionStatement(new JAssignmentExpression(
                    new JArrayAccessExpression(new JFieldAccessExpression(WFA),new JIntLiteral(i)),
                    new JMethodCallExpression("&",new JExpression[]{new JEmittedTextExpression(WFA_PREFIX+i)}))));
        }
    }
    
    /**
     * Add init function address field: LS_ADDRESS init_[i];
     */
    public void addInitFunctionAddressField() {
        JVariableDefinition initfunc = new JVariableDefinition(
                new CArrayType(new CEmittedTextType(LS_ADDRESS),1,new JExpression[]{new JIntLiteral(numspus)}), INIT_FUNC);
        JFieldDeclaration field = new JFieldDeclaration(initfunc);
        addField(field);
        for (int i=0; i<numspus; i++) {
            addInitStatement(new JExpressionStatement(new JAssignmentExpression(
                    new JArrayAccessExpression(new JFieldAccessExpression(INIT_FUNC),new JIntLiteral(i)),
                    new JMethodCallExpression("&",new JExpression[]{new JEmittedTextExpression(INIT_FUNC_PREFIX+i)}))));
        }
    }
    
    /**
     * Add SPU filter description field: SPU_FILTER_DESC fd_[i];
     */
    public void addSPUFilterDescriptionField() {
        JVariableDefinition fd = new JVariableDefinition(
                new CArrayType(new CEmittedTextType(SPU_FILTER_DESC),1,new JExpression[]{new JIntLiteral(numspus)}), SPU_FD);
        JFieldDeclaration field = new JFieldDeclaration(fd);
        addField(field);
        //fds.add(id.intValue()-1, field);
    }
    
    public void addDataAddressField() {
        JVariableDefinition da = new JVariableDefinition(
                new CEmittedTextType(SPU_ADDRESS), DATA_ADDR);
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
    public void addFilterDescriptionSetup() {

        JForStatement fdsetup;
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
        
        JExpressionStatement workfunc = new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JArrayAccessExpression(new JFieldAccessExpression(SPU_FD),new JLocalVariableExpression(var)),SPU_FD_WORK_FUNC),
                new JArrayAccessExpression(new JFieldAccessExpression(WFA),new JLocalVariableExpression(var))));//JFieldAccessExpression(wfid)));
        body.addStatement(workfunc);
        JExpressionStatement statesize = new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JArrayAccessExpression(new JFieldAccessExpression(SPU_FD),new JLocalVariableExpression(var)),SPU_FD_STATE_SIZE),
                new JIntLiteral(0)));
        body.addStatement(statesize);
        JExpressionStatement numinputs = new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JArrayAccessExpression(new JFieldAccessExpression(SPU_FD),new JLocalVariableExpression(var)),SPU_FD_NUM_INPUTS),
                new JIntLiteral(1)));
        body.addStatement(numinputs);
        JExpressionStatement numoutputs = new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JArrayAccessExpression(new JFieldAccessExpression(SPU_FD),new JLocalVariableExpression(var)),SPU_FD_NUM_OUTPUTS),
                new JIntLiteral(1)));
        body.addStatement(numoutputs);
        
        fdsetup = new JForStatement(init, cond, incr, body);
        addInitStatement(fdsetup);
    }
        
    public void addCallBackFunction() {
        JFormalParameter tag = new JFormalParameter(new CEmittedTextType(UINT32_T), "tag");
        JBlock body = new JBlock();
        body.addStatement(new JExpressionStatement(new JPostfixExpression(Constants.OPE_POSTDEC, new JFieldAccessExpression(DONE))));
        JMethodDeclaration cb = new JMethodDeclaration(CStdType.Void, CB, new JFormalParameter[]{tag}, body);
        addMethod(cb);
    }
    
    public void addPSPLayout() {
        
        JForStatement layout;
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
        
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JArrayAccessExpression(new JFieldAccessExpression(l.getIdent()), new JLocalVariableExpression(var)), "cmd_id"),
                new JIntLiteral(6))));
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JArrayAccessExpression(new JFieldAccessExpression(l.getIdent()), new JLocalVariableExpression(var)), "spu_id"),
                new JLocalVariableExpression(var))));
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JArrayAccessExpression(new JFieldAccessExpression(l.getIdent()), new JLocalVariableExpression(var)), "da"),
                new JFieldAccessExpression(DATA_ADDR))));
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JArrayAccessExpression(new JFieldAccessExpression(l.getIdent()), new JLocalVariableExpression(var)), "spu_in_buf_data"),
                new JFieldAccessExpression(INPUT_BUFFER_ADDR))));
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JArrayAccessExpression(new JFieldAccessExpression(l.getIdent()), new JLocalVariableExpression(var)), "spu_out_buf_data"),
                new JFieldAccessExpression(OUTPUT_BUFFER_ADDR))));
        
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JArrayAccessExpression(new JFieldAccessExpression(l.getIdent()), new JLocalVariableExpression(var)), "ppu_in_buf_data"),
                new JArrayAccessExpression(new JFieldAccessExpression(PPU_INPUT_BUFFER_ADDR), new JLocalVariableExpression(var)))));
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JArrayAccessExpression(new JFieldAccessExpression(l.getIdent()), new JLocalVariableExpression(var)), "ppu_out_buf_data"),
                new JArrayAccessExpression(new JFieldAccessExpression(PPU_OUTPUT_BUFFER_ADDR), new JLocalVariableExpression(var)))));
        
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JArrayAccessExpression(new JFieldAccessExpression(l.getIdent()), new JLocalVariableExpression(var)), "filt"),
                new JFieldAccessExpression(FCB))));
        
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JArrayAccessExpression(new JFieldAccessExpression(r.getIdent()), new JLocalVariableExpression(var)), "in_bytes"),
                new JFieldAccessExpression(INDTSZPERITER))));
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JArrayAccessExpression(new JFieldAccessExpression(r.getIdent()), new JLocalVariableExpression(var)), "run_iters"),
                new JFieldAccessExpression(RUNSPERITER))));
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JArrayAccessExpression(new JFieldAccessExpression(r.getIdent()), new JLocalVariableExpression(var)), "out_bytes"),
                new JFieldAccessExpression(OUTDTSZPERITER))));
        
        body.addStatement(new JExpressionStatement(new JMethodCallExpression("ext_ppu_spu_ppu",
                new JExpression[]{
                        new JMethodCallExpression("&", new JExpression[]{new JArrayAccessExpression(new JFieldAccessExpression(l.getIdent()), new JLocalVariableExpression(var))}),
                        new JMethodCallExpression("&", new JExpression[]{new JArrayAccessExpression(new JFieldAccessExpression(r.getIdent()), new JLocalVariableExpression(var))}),
                        new JArrayAccessExpression(new JFieldAccessExpression(SPUITERS), new JLocalVariableExpression(var)),
                        new JEmittedTextExpression("cb"),
                        new JIntLiteral(0)
                })));
        
        layout = new JForStatement(init, cond, incr, body);
        addInitStatement(layout);
    }
    
    public void addSpulibPollWhile() {
        addInitStatement(new JExpressionStatement(new JMethodCallExpression(
                "spulib_poll_while",
                new JExpression[]{
                        new JRelationalExpression(Constants.OPE_GT, new JFieldAccessExpression(DONE), new JIntLiteral(0))
                })));
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

        
        // SPU_CMD_GROUP * g_;
        JVariableDefinition group = new JVariableDefinition(new CEmittedTextType(SPU_CMD_GROUP), GROUP);
        //addInitStatement(new JVariableDeclarationStatement(group));
        JFieldDeclaration field = new JFieldDeclaration(group);
        addField(field);
        
        // SPU_ADDRESS fcb = 0;
        JVariableDefinition fcb = new JVariableDefinition(new CEmittedTextType(SPU_ADDRESS), FCB);
        fcb.setInitializer(new JIntLiteral(0));
        //addInitStatement(new JVariableDeclarationStatement(fcb));
        field = new JFieldDeclaration(fcb);
        addField(field);
        
        // uint32_t ibs = 64 * 1024
        JVariableDefinition ibs = new JVariableDefinition(new CEmittedTextType(UINT32_T), INPUT_BUFFER_SIZE);
        ibs.setInitializer(new JMultExpression(new JIntLiteral(4), new JIntLiteral(1024)));
        addField(new JFieldDeclaration(ibs));

        // uint32_t obs = 64 * 1024;
        JVariableDefinition obs = new JVariableDefinition(new CEmittedTextType(UINT32_T), OUTPUT_BUFFER_SIZE);
        obs.setInitializer(new JMultExpression(new JIntLiteral(4), new JIntLiteral(1024)));
        addField(new JFieldDeclaration(obs));

//        for (int i=1; i<=numspus; i++) { 
            // void * piba;
        JVariableDefinition piba = 
            new JVariableDefinition(new CArrayType(new CEmittedTextType("void *"),1,new JExpression[]{new JIntLiteral(numspus)}), PPU_INPUT_BUFFER_ADDR);
        addField(new JFieldDeclaration(piba));

        // void * poba;
        JVariableDefinition poba = 
            new JVariableDefinition(new CArrayType(new CEmittedTextType("void *"),1,new JExpression[]{new JIntLiteral(numspus)}), PPU_OUTPUT_BUFFER_ADDR);
        addField(new JFieldDeclaration(poba));

        // BUFFER_CB * picb;
        JVariableDefinition picb = 
            new JVariableDefinition(new CArrayType(new CEmittedTextType("BUFFER_CB *"),1,new JExpression[]{new JIntLiteral(numspus)}), PPU_INPUT_BUFFER_CB);
        addField(new JFieldDeclaration(picb));

        // BUFFER_CB * pocb;
        JVariableDefinition pocb = 
            new JVariableDefinition(new CArrayType(new CEmittedTextType("BUFFER_CB *"),1,new JExpression[]{new JIntLiteral(numspus)}), PPU_OUTPUT_BUFFER_CB);
        addField(new JFieldDeclaration(pocb));
//        }
        
        // uint32_t pibs = 4 * 1024 * 1024;
        JVariableDefinition pibs = new JVariableDefinition(new CEmittedTextType(UINT32_T), PPU_INPUT_BUFFER_SIZE);
        pibs.setInitializer(new JMultExpression(new JIntLiteral(4),new JMultExpression(new JIntLiteral(1024), new JIntLiteral(1024))));
        addField(new JFieldDeclaration(pibs));
        
        // uint32_t pobs = 4 * 1024 * 1024;
        JVariableDefinition pobs = new JVariableDefinition(new CEmittedTextType(UINT32_T), PPU_OUTPUT_BUFFER_SIZE);
        pobs.setInitializer(new JMultExpression(new JIntLiteral(4),new JMultExpression(new JIntLiteral(1024), new JIntLiteral(1024))));
        addField(new JFieldDeclaration(pobs));
        
        // int nd;
        JVariableDefinition nd = new JVariableDefinition(new CEmittedTextType("int"), ND);
        //nd.setInitializer(new JMultExpression(new JFieldAccessExpression(N), new JIntLiteral(32)));
        addField(new JFieldDeclaration(nd));
        
        // int n = 1000;
        JVariableDefinition n = new JVariableDefinition(new CEmittedTextType("int"), N);
        addField(new JFieldDeclaration(n));
        
        // int done = numspus;
        JVariableDefinition done = new JVariableDefinition(new CEmittedTextType("int"), DONE);
        done.setInitializer(new JIntLiteral(numspus));
        addField(new JFieldDeclaration(done));
        
        l = new JVariableDefinition(new CArrayType(new CEmittedTextType("EXT_PSP_LAYOUT"), 1, new JExpression[]{new JIntLiteral(numspus)}), "l");
        r = new JVariableDefinition(new CArrayType(new CEmittedTextType("EXT_PSP_RATES"), 1, new JExpression[]{new JIntLiteral(numspus)}), "r");
        l.setInitializer(null);
//        r.setInitializer(ra);
        addField(new JFieldDeclaration(l));
        addField(new JFieldDeclaration(r));
        
        JVariableDefinition inspuitems = new JVariableDefinition(new CArrayType(CStdType.Integer,1,new JExpression[]{new JIntLiteral(numspus)}), INSPUITEMS);
        addField(new JFieldDeclaration(inspuitems));

        JVariableDefinition outspuitems = new JVariableDefinition(new CArrayType(CStdType.Integer,1,new JExpression[]{new JIntLiteral(numspus)}), OUTSPUITEMS);
        addField(new JFieldDeclaration(outspuitems));
        
        JVariableDefinition iters = new JVariableDefinition(CStdType.Integer, ITERS);
        addField(new JFieldDeclaration(iters));
        
        JVariableDefinition runsperiter = new JVariableDefinition(CStdType.Integer, RUNSPERITER);
        addField(new JFieldDeclaration(runsperiter));
        
        JVariableDefinition indtszperiter = new JVariableDefinition(CStdType.Integer, INDTSZPERITER);
        addField(new JFieldDeclaration(indtszperiter));
        
        JVariableDefinition outdtszperiter = new JVariableDefinition(CStdType.Integer, OUTDTSZPERITER);
        addField(new JFieldDeclaration(outdtszperiter));
        
        JVariableDefinition spuiters;
        spuiters = new JVariableDefinition(new CArrayType(CStdType.Integer, 1, new JExpression[]{new JIntLiteral(numspus)}), SPUITERS);
        //spuiters.setInitializer(null);
        
//        for (int i=1; i<numspus; i++) {
//            spuiters = new JVariableDefinition(new CEmittedTextType("int"), "spuiters_" + i);
//            spuiters.setInitializer(new JIntLiteral(10000/8/numspus));
//            addField(new JFieldDeclaration(spuiters));
//        }
//        spuiters = new JVariableDefinition(new CEmittedTextType("int"), "spuiters_" + numspus);
//        spuiters.setInitializer(new JIntLiteral(210));
        addField(new JFieldDeclaration(spuiters));
        
        this.schedule = schedule;
        poprate = schedule.getSchedule()[1].getFilterNodes().get(0).getFilter().getPopInt();
        pushrate = schedule.getSchedule()[1].getFilterNodes().get(0).getFilter().getPushInt();
        if (schedule.getSchedule()[0].getFilterNodes().get(0).getFilter().getOutputType().isFloatingPoint())
            inputType = "float";
        else inputType = "int";
        outputType = inputType;
        
        addInitStatement(new JExpressionStatement(new JAssignmentExpression(new JFieldAccessExpression(N), new JIntLiteral(10000))));
        addInitStatement(new JExpressionStatement(new JAssignmentExpression(new JFieldAccessExpression(RUNSPERITER), new JIntLiteral(4))));
        addInitStatement(new JExpressionStatement(new JAssignmentExpression(new JFieldAccessExpression(INDTSZPERITER), 
                new JMultExpression(null, new JFieldAccessExpression(RUNSPERITER), 
                        new JMultExpression(new JMethodCallExpression("sizeof",new JExpression[]{new JEmittedTextExpression(inputType)}),
                                new JIntLiteral(poprate))))));
        addInitStatement(new JExpressionStatement(new JAssignmentExpression(new JFieldAccessExpression(OUTDTSZPERITER), 
                new JMultExpression(null, new JFieldAccessExpression(RUNSPERITER), 
                        new JMultExpression(new JMethodCallExpression("sizeof",new JExpression[]{new JEmittedTextExpression(outputType)}),
                                new JIntLiteral(pushrate))))));
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
        
        JExpressionStatement expr = new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(INPUT_BUFFER_ADDR),
                new JAddExpression(new JFieldAccessExpression(FCB),
                        new JAddExpression(new JIntLiteral(FCB_SIZE),new JIntLiteral(FILLER)))));
        addInitStatement(expr);
    }
    
    public void setupOutputBufferAddress() {

        JVariableDefinition oba = new JVariableDefinition(
                new CEmittedTextType(SPU_ADDRESS), OUTPUT_BUFFER_ADDR);
        JFieldDeclaration field = new JFieldDeclaration(oba);
        addField(field);

        JExpressionStatement expr = new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(OUTPUT_BUFFER_ADDR),
                new JAddExpression(new JFieldAccessExpression(INPUT_BUFFER_ADDR),
                        new JAddExpression(new JFieldAccessExpression(INPUT_BUFFER_SIZE), new JIntLiteral(FILLER)))));
        addInitStatement(expr);
//        
//        for (int i=1; i<outputNode.getWidth(); i++) {
//            prev = new String(buffaddr);
//            prevsize = OUTPUT_BUFFER_SIZE;
//            buffaddr = OUTPUT_BUFFER_ADDR + id + "_" + i;
//            
//            oba = new JVariableDefinition(
//                    new CEmittedTextType(SPU_ADDRESS), buffaddr);
//            field = new JFieldDeclaration(oba);
//            addField(field);
//            
//            expr = new JExpressionStatement(new JAssignmentExpression(
//                    new JEmittedTextExpression(buffaddr),
//                    new JEmittedTextExpression(prev + PLUS + prevsize + PLUS + FILLER)));
//            addInitStatement(expr);
//        }
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
                new JDivideExpression(null, new JFieldAccessExpression(ITERS), new JIntLiteral(numspus)))));
        
        spuiters = new JForStatement(init, cond, incr, body);
        addInitStatement(spuiters);
        addInitStatement(new JExpressionStatement(new JAssignmentExpression(new JArrayAccessExpression(new JFieldAccessExpression(SPUITERS),new JIntLiteral(numspus-1)),
                new JMinusExpression(null, new JFieldAccessExpression(ITERS), 
                        new JMultExpression(new JArrayAccessExpression(new JFieldAccessExpression(SPUITERS),new JIntLiteral(0)),
                                            new JIntLiteral(numspus-1))))));
    }
    
    public void addPPUBuffers() {
        JForStatement allocbufs;
        // make init statement - assign zero to <var>.  We need to use
        // an expression list statement to follow the convention of
        // other for loops and to get the codegen right.
        JVariableDefinition var = new JVariableDefinition(CStdType.Integer, "i");
        //var.setInitializer(new JIntLiteral(0));
        addInitStatement(new JVariableDeclarationStatement(var));
        
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
        
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JArrayAccessExpression(new JFieldAccessExpression(INSPUITEMS), new JLocalVariableExpression(var)),
                new JMultExpression(new JMultExpression(new JArrayAccessExpression(new JFieldAccessExpression(SPUITERS), new JLocalVariableExpression(var)),
                        new JFieldAccessExpression(RUNSPERITER)), new JIntLiteral(poprate)))));
        
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JArrayAccessExpression(new JFieldAccessExpression(OUTSPUITEMS), new JLocalVariableExpression(var)),
                new JMultExpression(new JMultExpression(new JArrayAccessExpression(new JFieldAccessExpression(SPUITERS), new JLocalVariableExpression(var)),
                        new JFieldAccessExpression(RUNSPERITER)), new JIntLiteral(pushrate)))));
        
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JArrayAccessExpression(new JFieldAccessExpression(PPU_INPUT_BUFFER_ADDR), new JLocalVariableExpression(var)),
                new JMethodCallExpression(
                        ALLOC_BUFFER,
                        new JExpression[]{
                                new JFieldAccessExpression(PPU_INPUT_BUFFER_SIZE),
                                new JIntLiteral(0)}))));
        
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JArrayAccessExpression(new JFieldAccessExpression(PPU_OUTPUT_BUFFER_ADDR), new JLocalVariableExpression(var)),
                new JMethodCallExpression(
                        ALLOC_BUFFER,
                        new JExpression[]{
                                new JFieldAccessExpression(PPU_OUTPUT_BUFFER_SIZE),
                                new JIntLiteral(0)}))));
        
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JArrayAccessExpression(new JFieldAccessExpression(PPU_INPUT_BUFFER_CB), new JLocalVariableExpression(var)),
                new JMethodCallExpression(
                        BUF_GET_CB,
                        new JExpression[]{
                                new JArrayAccessExpression(new JFieldAccessExpression(PPU_INPUT_BUFFER_ADDR), new JLocalVariableExpression(var))}))));
        
        body.addStatement(new JExpressionStatement(new JAssignmentExpression(
                new JArrayAccessExpression(new JFieldAccessExpression(PPU_OUTPUT_BUFFER_CB), new JLocalVariableExpression(var)),
                new JMethodCallExpression(
                        BUF_GET_CB,
                        new JExpression[]{
                                new JArrayAccessExpression(new JFieldAccessExpression(PPU_OUTPUT_BUFFER_ADDR), new JLocalVariableExpression(var))}))));
        
        allocbufs = new JForStatement(init, cond, incr, body);
        addInitStatement(allocbufs);
    }
    
    public void setupDataAddress() {

        JExpressionStatement expr = new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(DATA_ADDR),
                new JAddExpression(new JFieldAccessExpression(OUTPUT_BUFFER_ADDR), new JFieldAccessExpression(OUTPUT_BUFFER_SIZE))));
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
        
        initFileReader(filterNode);
        
        JForStatement readbufs;
        // make init statement - assign zero to <var>.  We need to use
        // an expression list statement to follow the convention of
        // other for loops and to get the codegen right.
        JVariableDefinition var = new JVariableDefinition(CStdType.Integer, "i");
        //var.setInitializer(new JIntLiteral(0));
        //addInitStatement(new JVariableDeclarationStatement(var));
        
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
        
        JBlock body = makeReadBlock(filterNode, var);
                
        readbufs = new JForStatement(init, cond, incr, body);
        addInitStatement(readbufs);
    }
    
    private void initFileReader(FilterSliceNode filterNode) {
                
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
        //create init function
//        JBlock initBlock = new JBlock(null, new JStatement[0], null);
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
    
    private JBlock makeReadBlock(FilterSliceNode filterNode, JVariableDefinition var) {
        
        FileInputContent fic = (FileInputContent) filterNode.getFilter();
        
        //set this as the init function...
//        JMethodDeclaration initMethod = new JMethodDeclaration(null,
//                at.dms.kjc.Constants.ACC_PUBLIC,
//                CStdType.Void,
//                "init_fileread",
//                JFormalParameter.EMPTY,
//                CClassType.EMPTY,
//                initBlock,
//                null,
//                null);
        //addInitFunctionCall(initMethod);
        
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
            // create the params for fread(piba[i], sizeof(type), spuitems[i], file)
            JExpression[] freadParams = new JExpression[4];
            
            // the first parameter: piba[i]
            JExpression addressofParameters = 
                new JArrayAccessExpression(new JFieldAccessExpression(PPU_INPUT_BUFFER_ADDR), new JLocalVariableExpression(var));
            
//            JMethodCallExpression addressofCall =
//                new JMethodCallExpression(null, "&", addressofParameters);
//            
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
            
            // the third parameter: read spuitems[i] elements at a time
            freadParams[2] = 
                new JArrayAccessExpression(new JFieldAccessExpression(INSPUITEMS), new JLocalVariableExpression(var));
            
            // the last parameter: the file pointer
            freadParams[3] = new JFieldAccessExpression(FILE_READER);
            
            JMethodCallExpression fread = 
                new JMethodCallExpression(null, new JThisExpression(null),
                                          "fread",
                                          freadParams);

            fileio = fread;
        }
        // } RMR
        
//        JStatement nd = new JExpressionStatement(new JAssignmentExpression(
//                new JFieldAccessExpression(ND), new JEmittedTextExpression("n * 32 / 6")));
//        addInitStatement(nd);
//        JStatement forinit = new JExpressionStatement(new JEmittedTextExpression("int i = 0"));
//        JExpression forcond = new JRelationalExpression(Constants.OPE_LT, new JEmittedTextExpression("i"), new JFieldAccessExpression(ND));//new JEmittedTextExpression("i < nd");
//        JStatement forinc = new JExpressionStatement(new JEmittedTextExpression("i++"));
//        JBlock forbody = new JBlock();
//        for (int i=1; i<=numspus; i++) {
            readBlock.addStatement(new JExpressionStatement(fileio));
//            JExpressionStatement set = new JExpressionStatement(new JAssignmentExpression(
//                    new JEmittedTextExpression("((int *)" + PPU_INPUT_BUFFER_ADDR + i + ")[i]"),
//                    tmp.getRef()));
//            forbody.addStatement(set);
//        }
//        JForStatement readloop = new JForStatement(forinit, forcond, forinc, forbody);
//        addInitStatement(readloop);
        
//        for (int i=1; i<=numspus; i++) {
            readBlock.addStatement(new JExpressionStatement(
                    new JAssignmentExpression(
                            new JNameExpression(null, new JArrayAccessExpression(new JFieldAccessExpression(PPU_INPUT_BUFFER_CB), new JLocalVariableExpression(var)), "tail"),
                            new JMultExpression(new JArrayAccessExpression(new JFieldAccessExpression(INSPUITEMS),new JLocalVariableExpression(var)), new JIntLiteral(4)))));
            readBlock.addStatement(new JExpressionStatement(
                    new JMethodCallExpression("IF_CHECK",
                    new JExpression[]{new JAssignmentExpression(
                            new JNameExpression(null, new JArrayAccessExpression(new JFieldAccessExpression(PPU_INPUT_BUFFER_CB), new JLocalVariableExpression(var)), "otail"),
                            new JNameExpression(null, new JArrayAccessExpression(new JFieldAccessExpression(PPU_INPUT_BUFFER_CB), new JLocalVariableExpression(var)), "tail"))})));
//        }
//        SIRPushExpression push = 
//            new SIRPushExpression(tmp.getRef(), fic.getOutputType());
//        
        //addInitStatement(new JExpressionStatement(null, push, null));
//
//        JMethodDeclaration workMethod = new JMethodDeclaration(null,
//                at.dms.kjc.Constants.ACC_PUBLIC,
//                CStdType.Void,
//                "work_fileread",
//                JFormalParameter.EMPTY,
//                CClassType.EMPTY,
//                workBlock,
//                null,
//                null);
//        workMethod.setPop(0);
//        workMethod.setPeek(0);
//        workMethod.setPush(pushrate);
        //addInitFunctionCall(workMethod);

        return readBlock;
    }
    
    public void addFileWriter(FilterSliceNode filterNode) {
        
        initFileWriter(filterNode);
        
        JForStatement writebufs;
        // make init statement - assign zero to <var>.  We need to use
        // an expression list statement to follow the convention of
        // other for loops and to get the codegen right.
        JVariableDefinition var = new JVariableDefinition(CStdType.Integer, "i");
        //var.setInitializer(new JIntLiteral(0));
        //addInitStatement(new JVariableDeclarationStatement(var));
        
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
        
        JBlock body = makeWriteBlock(filterNode, var);
                
        writebufs = new JForStatement(init, cond, incr, body);
        addInitStatement(writebufs);

        addInitStatement(new JExpressionStatement(
            new JMethodCallExpression("fclose", new JExpression[]{
                new JFieldAccessExpression(FILE_READER)})));

        addInitStatement(new JExpressionStatement(
            new JMethodCallExpression("fclose", new JExpression[]{
                new JFieldAccessExpression(FILE_WRITER)})));
    }
    
    private void initFileWriter(FilterSliceNode filterNode) {
    
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
    
    private JBlock makeWriteBlock(FilterSliceNode filterNode, JVariableDefinition var) {

        FileOutputContent foc = (FileOutputContent) filterNode.getFilter();
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
            
            // create the params for fwrite(poba[i], sizeof(type), spuitems[i], file)
            JExpression[] fwriteParams = new JExpression[4];
            
            // the first parameter: poba[i]
            JExpression[] addressofParameters = new JExpression[1];
            
            JArrayAccessExpression addressofCall =
                new JArrayAccessExpression(new JFieldAccessExpression(PPU_OUTPUT_BUFFER_ADDR), new JLocalVariableExpression(var));

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
            
            // the third parameter: read spuitems[i] elements at a time
            fwriteParams[2] =
                new JArrayAccessExpression(new JFieldAccessExpression(OUTSPUITEMS), new JLocalVariableExpression(var));
            
            // the last parameter: the file pointer
            fwriteParams[3] = new JFieldAccessExpression(FILE_WRITER);
            
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
//        JStatement forinit = new JExpressionStatement(new JEmittedTextExpression("int i = 0"));
//        JExpression forcond = new JRelationalExpression(Constants.OPE_LT, new JEmittedTextExpression("i"), new JFieldAccessExpression(ND));//new JEmittedTextExpression("i < nd");
//        JStatement forinc = new JExpressionStatement(new JEmittedTextExpression("i++"));
//        JBlock forbody = new JBlock();
//        for (int i=1; i<=numspus; i++) {
        writeBlock.addStatement(new JExpressionStatement(fileio));    
//        JExpressionStatement set = new JExpressionStatement(new JAssignmentExpression(
//                    tmp.getRef(),
//                    new JEmittedTextExpression("((int *)" + PPU_OUTPUT_BUFFER_ADDR + i + ")[i]")
//                    ));
//            forbody.addStatement(set);
//            forbody.addStatement(new JExpressionStatement(fileio));
//        }
//        JForStatement writeloop = new JForStatement(forinit, forcond, forinc, forbody);
//        addInitStatement(writeloop);
        
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
        

//        JBlock body = new JBlock();
//        body.addStatement(new JExpressionStatement(close));
//        addInitStatement(new JExpressionStatement(close));
        
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
        
        return writeBlock;
    }
    
//    @Override
//    public void addInitFunctionCall(JMethodDeclaration init) {
//        //JMethodDeclaration init = filterInfo.filter.getInit();
//        if (init != null)
//            mainMethod.addStatementFirst
//            (new JExpressionStatement(
//                    new JMethodCallExpression(CALL_FUNC,
//                            new JExpression[]{new JEmittedTextExpression(init.getName())})));
//        else
//            System.err.println(" ** Warning: Init function is null");
//    }

    
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
}
