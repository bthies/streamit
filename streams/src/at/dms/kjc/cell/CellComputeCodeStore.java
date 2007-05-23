package at.dms.kjc.cell;

import java.util.HashMap;

import at.dms.kjc.CClassType;
import at.dms.kjc.CEmittedTextType;
import at.dms.kjc.CStdType;
import at.dms.kjc.JAssignmentExpression;
import at.dms.kjc.JBlock;
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
import at.dms.kjc.JStatement;
import at.dms.kjc.JStringLiteral;
import at.dms.kjc.JThisExpression;
import at.dms.kjc.JVariableDeclarationStatement;
import at.dms.kjc.JVariableDefinition;
import at.dms.kjc.KjcOptions;
import at.dms.kjc.backendSupport.ComputeCodeStore;
import at.dms.kjc.common.ALocalVariable;
import at.dms.kjc.sir.SIRPopExpression;
import at.dms.kjc.sir.SIRPushExpression;
import at.dms.kjc.slicegraph.FileInputContent;
import at.dms.kjc.slicegraph.FileOutputContent;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.OutputSliceNode;
import at.dms.kjc.slicegraph.Slice;

public class CellComputeCodeStore extends ComputeCodeStore<CellPU> {
    
    private HashMap<Slice,Integer> sliceIdMap = new HashMap<Slice,Integer>();
    
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
    private static final String SPULIB_INIT = "spulib_init";
    private static final String SPULIB_WAIT = "spulib_wait";
    private static final String ALLOC_BUFFER = "alloc_buffer";
    private static final String BUFFER_GET_CB = "buffer_get_cb";
    
    private static final String SPUINC = "\"spu.inc\"";
    
    private static final String UINT32_T = "uint32_t";
    private static final String PLUS = " + ";
    private static final String MINUS = " - ";
    private static final String INCLUDE = "#include";
    private static final String ROUND_UP = "ROUND_UP";
    
    private static final int SPU_RESERVE_SIZE = 4096;
    private static final int FILLER = 128;
    private static final int FCB_SIZE = 128;
    private static final int BUFFER_OFFSET = 0;
    private static final int NO_DEPS = 0;
    private static final String WAIT_MASK = "0x1f";
    
    private int id = 0;
    
    public CellComputeCodeStore(CellPU parent) {
        super(parent);
    }
    
    public void addCallBackFunction() {
        JFormalParameter tag = new JFormalParameter(new CEmittedTextType(UINT32_T), "tag");
        JMethodDeclaration cb = new JMethodDeclaration(CStdType.Void, CB, new JFormalParameter[]{tag}, new JBlock());
        addMethod(cb);
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
        
        addInitStatement(new JExpressionStatement(new JEmittedTextExpression("nd = n * 32")));
    
        JStatement forinit = new JExpressionStatement(new JEmittedTextExpression("int i = 0"));
        JExpression forcond = new JEmittedTextExpression("i < nd");
        JStatement forinc = new JExpressionStatement(new JEmittedTextExpression("i++"));
        JBlock forbody = new JBlock();
        forbody.addStatement(new JExpressionStatement(fileio));
        JExpressionStatement set = new JExpressionStatement(new JAssignmentExpression(
                new JEmittedTextExpression("((int *)" + PPU_INPUT_BUFFER_ADDR + ")[i]"),
                tmp.getRef()));
        forbody.addStatement(set);
        JForStatement readloop = new JForStatement(forinit, forcond, forinc, forbody);
        addInitStatement(readloop);
        
        addInitStatement(new JExpressionStatement(new JEmittedTextExpression(PPU_INPUT_BUFFER_CB + "->tail = nd * 4")));
        addInitStatement(new JExpressionStatement(new JEmittedTextExpression("IF_CHECK(" + PPU_INPUT_BUFFER_CB + "->otail = " + PPU_INPUT_BUFFER_CB + "->tail)")));
    
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
    
        //create init function
        JBlock initBlock = new JBlock(null, new JStatement[0], null);
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
    
        initBlock.addStatement(new JExpressionStatement(null, fass, null));
        
        // do some standard C error checking here.
        // do we need to put this in a separate method to allow subclass to override?
        initBlock.addStatement(new JExpressionStatement(
                new JEmittedTextExpression(new Object[]{
                        "if (",
                        new JFieldAccessExpression(
                                new JThisExpression(null),
                                file.getVariable().getIdent()),
                        " == NULL) { perror(\"error opening "+ foc.getFileName() + "\"); }"
                })));
        //set this as the init function...
        JMethodDeclaration initMethod = new JMethodDeclaration(null,
                at.dms.kjc.Constants.ACC_PUBLIC,
                CStdType.Void,
                "init_filewrite",
                JFormalParameter.EMPTY,
                CClassType.EMPTY,
                initBlock,
                null,
                null);
        addInitStatement(initBlock);
    
        //create work function
        JBlock workBlock = new JBlock(null, new JStatement[0], null);
    
//        SIRPopExpression pop = new SIRPopExpression(foc.getInputType());
        ALocalVariable tmp = ALocalVariable.makeTmp(foc.getInputType());
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
            
            addressofParameters[0] = new JEmittedTextExpression(PPU_OUTPUT_BUFFER_ADDR);
            
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

        workBlock.addStatement(new JExpressionStatement(null, fileio, null));
        
        JMethodDeclaration workMethod = new JMethodDeclaration(null,
                at.dms.kjc.Constants.ACC_PUBLIC,
                CStdType.Void,
                "work_filewrite",
                JFormalParameter.EMPTY,
                CClassType.EMPTY,
                workBlock,
                null,
                null);

        workMethod.setPop(1);
        workMethod.setPeek(1);
        workMethod.setPush(0);
        
        addInitStatement(workBlock);
        
        JMethodCallExpression close = 
            new JMethodCallExpression(null, new JThisExpression(null),
                                      "fclose", new JExpression[]{
                new JFieldAccessExpression(null,
                        new JThisExpression(null),
                        fileDefn.getIdent())
            });
        JBlock body = new JBlock();
        body.addStatement(new JExpressionStatement(close));
        addInitStatement(body);
        
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
    
    public void addPSPLayout(FilterSliceNode filterNode) {
        Integer id = getIdForSlice(filterNode.getParent());
        
        JVariableDeclarationStatement l = new JVariableDeclarationStatement(
                new JVariableDefinition(new CEmittedTextType("EXT_PSP_LAYOUT"), "l"));
        JVariableDeclarationStatement r = new JVariableDeclarationStatement(
                new JVariableDefinition(new CEmittedTextType("EXT_PSP_RATES"), "r"));
        addInitStatement(l);
        addInitStatement(r);
        
        addInitStatement(new JExpressionStatement(new JEmittedTextExpression(
                "l.cmd_id = 4")));
        addInitStatement(new JExpressionStatement(new JEmittedTextExpression(
                "l.da = " + DATA_ADDR)));
        addInitStatement(new JExpressionStatement(new JEmittedTextExpression(
                "l.spu_in_buf_data = " + INPUT_BUFFER_ADDR + id)));
        addInitStatement(new JExpressionStatement(new JEmittedTextExpression(
                "l.spu_out_buf_data = " + OUTPUT_BUFFER_ADDR + id)));
        addInitStatement(new JExpressionStatement(new JEmittedTextExpression(
                "l.filt = " + FCB + id)));
        
        addInitStatement(new JExpressionStatement(new JEmittedTextExpression(
                "r.in_bytes = 128")));
        addInitStatement(new JExpressionStatement(new JEmittedTextExpression(
                "r.run_iters = 32")));
        addInitStatement(new JExpressionStatement(new JEmittedTextExpression(
                "r.out_bytes = 128")));
    }
    
    public void addDataParallel() {
        addInitStatement(new JExpressionStatement(new JEmittedTextExpression(
                "ext_data_parallel_shared(0, &l, " + PPU_INPUT_BUFFER_ADDR +
                ", " + PPU_OUTPUT_BUFFER_ADDR + ", &r, n, cb, 0)")));
        addInitStatement(new JExpressionStatement(new JEmittedTextExpression(
                "spulib_poll_while(done != 0)")));
    }
    
    public void addIssueUnload(FilterSliceNode filterNode) {
        Integer id = getIdForSlice(filterNode.getParent());
        
        addInitStatement(new JExpressionStatement(new JEmittedTextExpression(
            "spu_issue_group(" + id + ", 1, " + DATA_ADDR + id + ")")));
        addInitStatement(new JExpressionStatement(new JEmittedTextExpression(
            "spulib_wait(" + id + ", 1)")));
    }
    
    public void addSPUInit() {
        JExpressionStatement includespuinc = new JExpressionStatement(new JEmittedTextExpression(INCLUDE + SPUINC));
        addInitStatement(includespuinc);
        JExpressionStatement spudatastart = new JExpressionStatement(new JAssignmentExpression(
                new JEmittedTextExpression(SPU_DATA_START),
                new JMethodCallExpression(ROUND_UP, new JExpression[]{
                        new JEmittedTextExpression(SPU_DATA_START),
                        new JEmittedTextExpression(CACHE_SIZE)
                })));
        addInitStatement(spudatastart);
        JExpressionStatement spudatasize = new JExpressionStatement(new JAssignmentExpression(
                new JEmittedTextExpression(SPU_DATA_SIZE),
                new JEmittedTextExpression(LS_SIZE + MINUS + SPU_RESERVE_SIZE + MINUS + SPU_DATA_START)));
        addInitStatement(spudatasize);
        JExpressionStatement spulibinit = new JExpressionStatement(
                new JMethodCallExpression(
                        SPULIB_INIT,
                        new JExpression[]{}));
        addInitStatement(spulibinit);
    }
    
    public void addWorkFunctionAddressField(FilterSliceNode filterNode) {
        Integer id = getIdForSlice(filterNode.getParent());
//        JVariableDefinition wf = new JVariableDefinition(
//                new CEmittedTextType(WFA), WFA_PREFIX+id);
//        System.out.println(wf.toString());
//        addField(new JFieldDeclaration(wf));
//        
        JFieldDeclaration file = 
            new JFieldDeclaration(null,
                                  new JVariableDefinition(null,
                                                          0,
                                                          new CEmittedTextType(WFA),
                                                          WFA_PREFIX+id,
                                                          null),
                                  null, null);
        addField(file);
    }
    
    public void addSPUFilterDescriptionField(FilterSliceNode filterNode) {
        Integer id = getIdForSlice(filterNode.getParent());
        addField(new JFieldDeclaration(
                new JVariableDefinition(
                        new CEmittedTextType(SPU_FD), SPU_FD_PREFIX+id)));
    }
    
    public void addFilterDescriptionSetup(FilterSliceNode filterNode) {
        Integer id = getIdForSlice(filterNode.getParent());
        String fd = idToFd(id);
        //fd_id.work_func = wf_id
        String wfid = WFA_PREFIX + id;
        JExpressionStatement workfunc = new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JEmittedTextExpression(fd),SPU_FD_WORK_FUNC),
                new JEmittedTextExpression(wfid)));
        //System.out.println(statement.toString());
        addInitStatement(workfunc);
        JExpressionStatement statesize = new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JEmittedTextExpression(fd),SPU_FD_STATE_SIZE),
                new JIntLiteral(0)));
        addInitStatement(statesize);
        
    }
    
    public void addFilterDescriptionSetup(InputSliceNode inputNode) {
        Integer id = getIdForSlice(inputNode.getParent());
        String fd = idToFd(id);
        JExpressionStatement numinputs = new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JEmittedTextExpression(fd),SPU_FD_NUM_INPUTS),
                new JIntLiteral(inputNode.getWidth())));
        addInitStatement(numinputs);
    }
    
    public void addFilterLoad(InputSliceNode inputNode) {
        Integer id = getIdForSlice(inputNode.getParent());
        String fd = idToFd(id);
        String group = GROUP + id;
        JExpressionStatement filterLoad = new JExpressionStatement(new JMethodCallExpression(
                SPU_FILTER_LOAD,
                new JExpression[]{
                        new JEmittedTextExpression(group),
                        new JEmittedTextExpression(FCB),
                        new JEmittedTextExpression("&" + fd),
                        new JIntLiteral(0),
                        new JIntLiteral(NO_DEPS)
                }));
        addInitStatement(filterLoad);
    }
    
    public void addFilterDescriptionSetup(OutputSliceNode outputNode) {
        Integer id = getIdForSlice(outputNode.getParent());
        String fd = idToFd(id);
        JExpressionStatement numoutputs = new JExpressionStatement(new JAssignmentExpression(
                new JFieldAccessExpression(new JEmittedTextExpression(fd),SPU_FD_NUM_OUTPUTS),
                new JIntLiteral(outputNode.getWidth())));
        addInitStatement(numoutputs);
    }
    
    public void setupInputBufferAddresses(InputSliceNode inputNode) {
        if (inputNode.getWidth() == 0) return;
        
        Integer id = getIdForSlice(inputNode.getParent());
        
        String buffaddr = INPUT_BUFFER_ADDR + id + "_" + 0;
        JExpressionStatement expr = new JExpressionStatement(new JAssignmentExpression(
                new JEmittedTextExpression(buffaddr),
                new JEmittedTextExpression(FCB + PLUS + FCB_SIZE + PLUS + FILLER)));
        addInitStatement(expr);
        for (int i=1; i<inputNode.getWidth(); i++) {
            String prev = new String(buffaddr);
            String prevsize = INPUT_BUFFER_SIZE;
            buffaddr = INPUT_BUFFER_ADDR + id + "_" + i;
            expr = new JExpressionStatement(new JAssignmentExpression(
                    new JEmittedTextExpression(buffaddr),
                    new JEmittedTextExpression(prev + PLUS + prevsize + PLUS + FILLER)));
            addInitStatement(expr);
        }
    }
    
    public void setupOutputBufferAddresses(OutputSliceNode outputNode) {
        if (outputNode.getWidth() == 0) return;
        
        Integer id = getIdForSlice(outputNode.getParent());
        int numinputs = outputNode.getParent().getHead().getWidth();
        
        String buffaddr;
        JExpressionStatement expr;
        
        // no input buffers
        if (numinputs == 0) {
            buffaddr = OUTPUT_BUFFER_ADDR + id + "_" + 0;
            expr = new JExpressionStatement(new JAssignmentExpression(
                    new JEmittedTextExpression(buffaddr),
                    new JEmittedTextExpression(FCB + PLUS + FCB_SIZE + PLUS + FILLER)));
            addInitStatement(expr);
        } else {
            String lastinputaddr = INPUT_BUFFER_ADDR + id + "_" + (numinputs-1);
            String lastinputsize = INPUT_BUFFER_SIZE;
            buffaddr = OUTPUT_BUFFER_ADDR + id + "_" + 0;
            expr = new JExpressionStatement(new JAssignmentExpression(
                    new JEmittedTextExpression(buffaddr),
                    new JEmittedTextExpression(lastinputaddr + PLUS + lastinputsize + PLUS + FILLER)));
            addInitStatement(expr);
        }
        
        for (int i=1; i<outputNode.getWidth(); i++) {
            String prev = new String(buffaddr);
            String prevsize = OUTPUT_BUFFER_SIZE;
            buffaddr = OUTPUT_BUFFER_ADDR + id + "_" + i;
            expr = new JExpressionStatement(new JAssignmentExpression(
                    new JEmittedTextExpression(buffaddr),
                    new JEmittedTextExpression(prev + PLUS + prevsize + PLUS + FILLER)));
            addInitStatement(expr);
        }
    }
    
    public void addPPUBuffers() {
        JExpressionStatement inputaddr = new JExpressionStatement(new JAssignmentExpression(
                new JEmittedTextExpression(PPU_INPUT_BUFFER_ADDR),
                new JMethodCallExpression(
                        ALLOC_BUFFER,
                        new JExpression[]{
                                new JEmittedTextExpression(PPU_INPUT_BUFFER_SIZE),
                                new JIntLiteral(0)})));
        addInitStatement(inputaddr);
        JExpressionStatement outputaddr = new JExpressionStatement(new JAssignmentExpression(
                new JEmittedTextExpression(PPU_OUTPUT_BUFFER_ADDR),
                new JMethodCallExpression(
                        ALLOC_BUFFER,
                        new JExpression[]{
                                new JEmittedTextExpression(PPU_OUTPUT_BUFFER_SIZE),
                                new JIntLiteral(0)})));
        addInitStatement(outputaddr);
        JExpressionStatement inputcb = new JExpressionStatement(new JAssignmentExpression(
                new JEmittedTextExpression(PPU_INPUT_BUFFER_CB),
                new JMethodCallExpression(
                        BUFFER_GET_CB,
                        new JExpression[]{
                                new JEmittedTextExpression(PPU_INPUT_BUFFER_ADDR)})));
        addInitStatement(inputcb);
        JExpressionStatement outputcb = new JExpressionStatement(new JAssignmentExpression(
                new JEmittedTextExpression(PPU_OUTPUT_BUFFER_CB),
                new JMethodCallExpression(
                        BUFFER_GET_CB,
                        new JExpression[]{
                                new JEmittedTextExpression(PPU_OUTPUT_BUFFER_ADDR)})));
        addInitStatement(outputcb);
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
                new JEmittedTextExpression(dataaddr),
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
        String group = GROUP + id;
        
        JExpressionStatement newGroup = new JExpressionStatement(new JAssignmentExpression(
                new JEmittedTextExpression(group),
                new JMethodCallExpression(SPU_NEW_GROUP,new JExpression[]{new JIntLiteral(id), new JIntLiteral(0)})));
        addInitStatement(newGroup);
    }
    
    public void addNewGroupAndFilterUnload(OutputSliceNode outputNode) {
        Integer id = getIdForSlice(outputNode.getParent());
        String group = GROUP + id;
        
        JExpressionStatement newGroup = new JExpressionStatement(new JAssignmentExpression(
                new JEmittedTextExpression(group),
                new JMethodCallExpression(SPU_NEW_GROUP,new JExpression[]{new JIntLiteral(id), new JIntLiteral(1)})));
        addInitStatement(newGroup);
        
        JExpressionStatement filterUnload = new JExpressionStatement(new JMethodCallExpression(
                SPU_FILTER_UNLOAD, new JExpression[]{
                        new JEmittedTextExpression(group),
                        new JEmittedTextExpression(FCB),
                        new JIntLiteral(0),
                        new JIntLiteral(NO_DEPS)
                }));
        addInitStatement(filterUnload);
    }
    
    public void addInputBufferAllocAttach(InputSliceNode inputNode) {
        Integer id = getIdForSlice(inputNode.getParent());
        String group = GROUP + id;
        
        int cmdId = 1;
        for (int i=0; i<inputNode.getWidth(); i++) {
            String buffaddr = INPUT_BUFFER_ADDR + id + "_" + i;
            String buffsize = INPUT_BUFFER_SIZE;
            JExpressionStatement bufferAlloc = new JExpressionStatement(new JMethodCallExpression(
                    SPU_BUFFER_ALLOC, new JExpression[]{
                            new JEmittedTextExpression(group), new JEmittedTextExpression(buffaddr), new JEmittedTextExpression(buffsize),
                            new JIntLiteral(BUFFER_OFFSET), new JIntLiteral(cmdId), new JIntLiteral(NO_DEPS)}));
            addInitStatement(bufferAlloc);
            JExpressionStatement attachBuffer = new JExpressionStatement(new JMethodCallExpression(
                    SPU_FILTER_ATTACH_INPUT, new JExpression[]{
                            new JEmittedTextExpression(group),
                            new JEmittedTextExpression(FCB),
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
        String group = GROUP + id;
        
        int cmdId = 2*outputNode.getParent().getHead().getWidth() + 1;
        for (int i=0; i<outputNode.getWidth(); i++) {
            String buffaddr = OUTPUT_BUFFER_ADDR + id + "_" + i;
            String buffsize = OUTPUT_BUFFER_SIZE;
            JExpressionStatement bufferAlloc = new JExpressionStatement(new JMethodCallExpression(
                    SPU_BUFFER_ALLOC, new JExpression[]{
                            new JEmittedTextExpression(group), new JEmittedTextExpression(buffaddr), new JEmittedTextExpression(buffsize),
                            new JIntLiteral(BUFFER_OFFSET), new JIntLiteral(cmdId), new JIntLiteral(NO_DEPS)}));
            addInitStatement(bufferAlloc);
            JExpressionStatement attachBuffer = new JExpressionStatement(new JMethodCallExpression(
                    SPU_FILTER_ATTACH_OUTPUT, new JExpression[]{
                            new JEmittedTextExpression(group),
                            new JEmittedTextExpression(FCB),
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
        
        JExpressionStatement issuegroup = new JExpressionStatement(new JMethodCallExpression(
                SPU_ISSUE_GROUP, new JExpression[]{
                        new JIntLiteral(id),
                        new JIntLiteral(0),
                        new JEmittedTextExpression(DATA_ADDR + id)}));
        addInitStatement(issuegroup);
        
        JExpressionStatement wait = new JExpressionStatement(new JMethodCallExpression(
                SPULIB_WAIT, new JExpression[]{
                        new JIntLiteral(id),
                        new JEmittedTextExpression(WAIT_MASK)}));
        addInitStatement(wait);
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
