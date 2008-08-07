package at.dms.kjc.tilera;

import java.util.LinkedList;
import java.util.List;
import at.dms.kjc.slicegraph.*;
import at.dms.kjc.backendSupport.*;
import at.dms.kjc.JStatement;

public class OutputBufferDMATransfers {
    /** the output buffer that these dma commands uses as its source */
    private OutputRotatingBuffer parent;
    /** the block of ilib_wait calls, one for each dma command generated, separated for steady 
     * because we have concurrency, for init they are in commandsInit*/
    private List<JStatement> waitCallsSteady;
    /** the dma commands block */
    private List<JStatement> commandsSteady;
    /** the dma commands block */
    private List<JStatement> commandsInit;
    /** the output slice node */
    private OutputSliceNode output;
    /** any declarations that are needed */
    private List<JStatement> decls;
    
    public OutputBufferDMATransfers(OutputRotatingBuffer buf) {
        parent = buf;
        waitCallsSteady= new LinkedList<JStatement>();
        commandsSteady = new LinkedList<JStatement>();
        commandsInit = new LinkedList<JStatement>();
        decls = new LinkedList<JStatement>();
        output = parent.filterNode.getParent().getTail();
        
        checkSimple();
        generateStatements();
    }

    private void generateStatements() {
        for (int w = 0; w < output.getWeights().length; w++) {
            for (InterSliceEdge edge : output.getDests()[w]) {
                InputSliceNode input = edge.getDest();
                FilterInfo srcInfo = FilterInfo.getFilterInfo(output.getPrevFilter());
                FilterInfo dstInfo = FilterInfo.getFilterInfo(input.getNextFilter());
                                
                DMAAddressRotation addrBuf = parent.getAddressBuffer(input);
                String requestVar = addrBuf.rotStructName  + "_request";
                int itemSize = Util.getTypeSize(parent.getType()) * 4;
                //make sure the input weight equals the output weight for now
                assert input.getWeight(edge) == output.getWeight(edge);
                
                //generate the dma command
                String dst = addrBuf.currentBufName + " + " + 
                    (itemSize * (dstInfo.remaining + input.weightBefore(edge)));
                String dst_stride = "" + (itemSize * input.totalWeights());
                //in the init stage we transfer after we complete the filter execution, so we use
                //the pointer to the buffer that was just written
                String src_init = parent.currentBufName + " + " + (itemSize * output.weightBefore(edge));
                //in the steady state transfer from the transfer buffer that is one behind the 
                //current buffer we are writing (we do this because we are double buffering)
                String src_steady = parent.transBufName + " + " + (itemSize * output.weightBefore(edge));
                String src_stride = "" + (itemSize * output.totalWeights());
                String block_size = "" + (itemSize * output.getWeight(edge));
               
                int num_blocks_init = 
                    srcInfo.totalItemsSent(SchedulingPhase.INIT) / output.totalWeights();
                                
                assert (dstInfo.totalItemsPopped(SchedulingPhase.STEADY) / input.totalWeights()) ==
                    (srcInfo.totalItemsSent(SchedulingPhase.STEADY) / output.totalWeights());
                
                int num_blocks_steady = 
                    dstInfo.totalItemsPopped(SchedulingPhase.STEADY) / input.totalWeights();
                
                if (num_blocks_init > 0) {
                    commandsInit.add(Util.toStmt("ilib_mem_start_strided_dma(" +
                        dst + ", " + 
                        dst_stride + ", " + 
                        src_init + ", " + 
                        src_stride + ", " + 
                        block_size + ", " + 
                        num_blocks_init + ", " + 
                        "&" + requestVar + ")"));
                    //generate the wait call
                    commandsInit.add(Util.toStmt("ilib_wait(&" + requestVar + ", &ignore_status)"));
                }
                
                if (num_blocks_steady > 0) {
                    commandsSteady.add(Util.toStmt("ilib_mem_start_strided_dma(" +
                        dst + ", " + 
                        dst_stride + ", " + 
                        src_steady + ", " + 
                        src_stride + ", " + 
                        block_size + ", " + 
                        num_blocks_steady + ", " +
                        "&" + requestVar + ")"));
                    //generate the wait call
                    waitCallsSteady.add(Util.toStmt("ilib_wait(&" + requestVar + ", &ignore_status)"));
                }
                
                //generate the decl of the request var
                decls.add(Util.toStmt("ilibRequest " + requestVar));
              
            }
        }
    }

    /**
     * Do some checks to make sure we will generate correct code for this distribution pattern.
     */
    private void checkSimple() {
        assert output.singleAppearance();
        for (int w = 0; w < output.getWeights().length; w++) {
            for (InterSliceEdge edge : output.getDests()[w]) {
                InputSliceNode input = edge.getDest();
                //assert that we don't have a single edge appear more than once for the input slice node
                assert input.singleAppearance();
                
                int inWeight = input.getWeight(edge);
                assert inWeight == output.getWeights()[w];
            }
        }
    }
    
    /**
     * Return the list of DMA commands that will transfer the items from the
     * output buffer to to appropriate input buffer(s)
     * 
     * @return the dma commands
     */
    public List<JStatement> dmaCommands(SchedulingPhase which) {
        if (which == SchedulingPhase.INIT)
            return commandsInit;
        
        return commandsSteady;
    }
    
    /**
     * Return declarations of variables needed by the dma commands 
     * @return declarations of variables needed by the dma commands 
     */
    public List<JStatement> decls() {
        return decls;
    }
    
    /**
     * Return the ilib_wait statements that wait for the dma commands to complete
     * 
     * @return the wait statements
     */
    public List<JStatement> waitCallsSteady() {
        return waitCallsSteady;    
    }
}
