package at.dms.kjc.cell;

import java.util.HashMap;

import at.dms.kjc.CEmittedTextType;
import at.dms.kjc.JAssignmentExpression;
import at.dms.kjc.JEmittedTextExpression;
import at.dms.kjc.JExpressionStatement;
import at.dms.kjc.JFieldAccessExpression;
import at.dms.kjc.JFieldDeclaration;
import at.dms.kjc.JVariableDefinition;
import at.dms.kjc.backendSupport.ComputeCodeStore;
import at.dms.kjc.slicegraph.FilterSliceNode;

public class CellComputeCodeStore extends ComputeCodeStore<CellPU> {
    
    private HashMap<FilterSliceNode,Integer> filterIDMap =
        new HashMap<FilterSliceNode,Integer>();
    
    private static final String WFA = "LS_ADDRESS";
    private static final String WFA_PREFIX = "wf_";
    private static final String SPU_FD = "SPU_FILTER_DESC";
    private static final String SPU_FD_PREFIX = "fd_";
    private static final String SPU_FD_WORK_FUNC = "work_func";
    private static final String SPU_FD_STATE_SIZE = "state_size";
    private static final String SPU_FD_STATE_ADDR = "state_addr";
    private static final String SPU_FD_NUM_INPUTS = "num_inputs";
    private static final String SPU_FD_NUM_OUTPUTS = "num_outputs";
    
    private int id = 0;
    
    public CellComputeCodeStore(CellPU parent) {
        super(parent);
    }
    
    public void addWorkFunctionAddressField(FilterSliceNode filterNode) {
        Integer newId = getIdForNode(filterNode);
        addField(new JFieldDeclaration(
                new JVariableDefinition(
                        new CEmittedTextType(WFA), WFA_PREFIX+newId)));
    }
    
    public void addSPUFilterDescriptionField(FilterSliceNode filterNode) {
        Integer newId = getIdForNode(filterNode);
        addField(new JFieldDeclaration(
                new JVariableDefinition(
                        new CEmittedTextType(SPU_FD), SPU_FD_PREFIX+newId)));
    }
    
    public void addFilterDescriptionSetup(FilterSliceNode filterNode) {
        Integer newId = getIdForNode(filterNode);
        String fd = idToFd(newId);
        //fd_id.work_func = wf_id
        addInitStatement(new JExpressionStatement(new JAssignmentExpression(new JFieldAccessExpression(new JEmittedTextExpression(fd),SPU_FD_WORK_FUNC),null)));
    }
    
    public static String idToFd(Integer id) {
        return SPU_FD_PREFIX + id;
    }
    
    private Integer getIdForNode(FilterSliceNode filterNode) {
        Integer newId = filterIDMap.get(filterNode);
        if (newId == null)
            newId = id;
        return newId;
    }
}
