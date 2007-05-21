package at.dms.kjc.cell;

import java.util.HashMap;

import javax.print.attribute.standard.JobHoldUntil;

import at.dms.kjc.CEmittedTextType;
import at.dms.kjc.CStdType;
import at.dms.kjc.JAssignmentExpression;
import at.dms.kjc.JBlock;
import at.dms.kjc.JEmittedTextExpression;
import at.dms.kjc.JExpression;
import at.dms.kjc.JExpressionStatement;
import at.dms.kjc.JFieldAccessExpression;
import at.dms.kjc.JFieldDeclaration;
import at.dms.kjc.JFormalParameter;
import at.dms.kjc.JIntLiteral;
import at.dms.kjc.JMethodCallExpression;
import at.dms.kjc.JMethodDeclaration;
import at.dms.kjc.JVariableDefinition;
import at.dms.kjc.backendSupport.ComputeCodeStore;
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
    private static final String DATA_ADDR = "da_";
    private static final String FCB = "fcb";
    private static final String CB = "cb";
    private static final String LS_SIZE = "LS_SIZE";
    private static final String CACHE_SIZE = "CACHE_SIZE";
    private static final String SPULIB_INIT = "spulib_init";
    private static final String SPULIB_WAIT = "spulib_wait";
    
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
    private static final String WAIT_MASK = "0x17";
    
    private int id = 0;
    private boolean init;
    
    public CellComputeCodeStore(CellPU parent) {
        super(parent);
        init = false;
    }
    
    public void setInit() {
        init = true;
    }
    
    public void addCallBackFunction() {
        if (init) return;
        JFormalParameter tag = new JFormalParameter(new CEmittedTextType(UINT32_T), "tag");
        JMethodDeclaration cb = new JMethodDeclaration(CStdType.Void, CB, new JFormalParameter[]{tag}, new JBlock());
        addMethod(cb);
    }
    
    public void addSPUInit() {
        if (init) return;
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
        JVariableDefinition wf = new JVariableDefinition(
                new CEmittedTextType(WFA), WFA_PREFIX+id);
        System.out.println(wf.toString());
        addField(new JFieldDeclaration(wf));
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
