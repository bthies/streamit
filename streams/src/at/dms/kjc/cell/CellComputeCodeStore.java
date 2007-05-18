package at.dms.kjc.cell;

import java.util.HashMap;

import at.dms.kjc.CEmittedTextType;
import at.dms.kjc.JAssignmentExpression;
import at.dms.kjc.JEmittedTextExpression;
import at.dms.kjc.JExpression;
import at.dms.kjc.JExpressionStatement;
import at.dms.kjc.JFieldAccessExpression;
import at.dms.kjc.JFieldDeclaration;
import at.dms.kjc.JIntLiteral;
import at.dms.kjc.JMethodCallExpression;
import at.dms.kjc.JStringLiteral;
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
    private static final String SPU_FD = "SPU_FILTER_DESC";
    private static final String SPU_FD_PREFIX = "fd_";
    private static final String SPU_FD_WORK_FUNC = "work_func";
    private static final String SPU_FD_STATE_SIZE = "state_size";
    private static final String SPU_FD_STATE_ADDR = "state_addr";
    private static final String SPU_FD_NUM_INPUTS = "num_inputs";
    private static final String SPU_FD_NUM_OUTPUTS = "num_outputs";
    private static final String GROUP = "g_";
    private static final String SPU_NEW_GROUP = "spu_new_group";
    private static final String SPU_FILTER_LOAD = "spu_filter_load";
    private static final String SPU_FILTER_UNLOAD = "spu_filter_unload";
    private static final String SPU_FILTER_ATTACH_INPUT = "spu_filter_attach_input";
    private static final String SPU_FILTER_ATTACH_OUTPUT = "spu_filter_attach_output";
    private static final String SPU_BUFFER_ALLOC = "spu_buffer_alloc";
    private static final String INPUT_BUFFER_ADDR = "iba_";
    private static final String INPUT_BUFFER_SIZE = "ibs_";
    private static final String FCB = "fcb";
    
    private static final int BUFFER_OFFSET = 0;
    private static final int NO_DEPS = 0;
    
    private int id = 0;
    
    public CellComputeCodeStore(CellPU parent) {
        super(parent);
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
    
    public void startNewFilter(InputSliceNode inputNode) {
        Integer id = getIdForSlice(inputNode.getParent());
        String fd = idToFd(id);
        
        JExpressionStatement newline = new JExpressionStatement(new JEmittedTextExpression(""));
        addInitStatement(newline);
        JExpressionStatement newFilter = new JExpressionStatement(new JEmittedTextExpression("// set up code for filter " + id));
        addInitStatement(newFilter);
    }
    
    public void addSPUInitStatements(FilterSliceNode filterNode) {

    }
    
    public void addNewGroupStatement(InputSliceNode inputNode) {
        Integer id = getIdForSlice(inputNode.getParent());
        String group = GROUP + id;
        
        JExpressionStatement newGroup = new JExpressionStatement(new JAssignmentExpression(
                new JEmittedTextExpression(group),
                new JMethodCallExpression(SPU_NEW_GROUP,new JExpression[]{new JIntLiteral(id), new JIntLiteral(0)})));
        addInitStatement(newGroup);
    }
    
    public void addFilterUnload(OutputSliceNode outputNode) {
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
            String buffsize = INPUT_BUFFER_SIZE + id + "_" + i;
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
    
    public void addAttachInputBuffer(InputSliceNode inputNode) {
        Integer id = getIdForSlice(inputNode.getParent());
        String group = GROUP + id;
        
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
