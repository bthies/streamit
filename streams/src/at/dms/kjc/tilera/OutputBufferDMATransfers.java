package at.dms.kjc.tilera;

import java.util.LinkedList;
import java.util.List;
import at.dms.kjc.slicegraph.*;
import at.dms.kjc.backendSupport.*;
import at.dms.kjc.JStatement;

public class OutputBufferDMATransfers extends OutputBufferTransfers {
   
    public OutputBufferDMATransfers(OutputRotatingBuffer buf) {
        super(buf);
        
        checkSimple(SchedulingPhase.INIT);
        generateStatements(SchedulingPhase.INIT);
        checkSimple(SchedulingPhase.STEADY);
        generateStatements(SchedulingPhase.STEADY);     
    }

    private void generateStatements(SchedulingPhase phase) {
        for (int w = 0; w < output.getWeights(phase).length; w++) {
            for (InterSliceEdge edge : output.getDests(phase)[w]) {
                InputSliceNode input = edge.getDest();
                FilterInfo srcInfo = FilterInfo.getFilterInfo(output.getPrevFilter());
                FilterInfo dstInfo = FilterInfo.getFilterInfo(input.getNextFilter());
                                
                SourceAddressRotation addrBuf = parent.getAddressBuffer(input);
                String requestVar = addrBuf.rotStructName  + "_request";
                int itemBytes = Util.getTypeSize(parent.getType()) * 4;
                //make sure the input weight equals the output weight for now
                assert input.getWeight(edge, phase) == output.getWeight(edge, phase);
                
                //generate the dma command
                //in the steady state, you want to skip the copy down for the dest
                String dst = addrBuf.currentBufName + " + " + ((phase == SchedulingPhase.INIT ? 0 : dstInfo.copyDown) + 
                    input.weightBefore(edge, phase));
                
                String dst_stride = "" + (itemBytes * input.totalWeights(phase));
                
                //in the init stage we transfer after we complete the filter execution, so we use
                //the pointer to the buffer that was just written
                //in the steady state transfer from the transfer buffer that is one behind the 
                //current buffer we are writing (we do this because we are double buffering)
                String src = (phase == SchedulingPhase.INIT ? parent.currentBufName : parent.transBufName) 
                        + " + " + (output.weightBefore(edge, phase));
                                                
                String src_stride = "" + (itemBytes * output.totalWeights(phase));
                String block_size = "" + (itemBytes * output.getWeight(edge, phase));
               
                int num_blocks = 
                    srcInfo.totalItemsSent(phase) / output.totalWeights(phase);
                                
                List<JStatement> commands = (phase == SchedulingPhase.INIT ? commandsInit : commandsSteady);
                
                if (num_blocks > 0) {
                    commands.add(Util.toStmt("ilib_mem_start_strided_dma(" +
                        dst + ", " + 
                        dst_stride + ", " + 
                        src + ", " + 
                        src_stride + ", " + 
                        block_size + ", " + 
                        num_blocks+ ", " + 
                        "&" + requestVar + ")"));
                    //generate the wait call
                    if (phase == SchedulingPhase.INIT) 
                        commandsInit.add(Util.toStmt("ilib_wait(&" + requestVar + ", &ignore_status)"));
                    else
                        waitCallsSteady.add(Util.toStmt("ilib_wait(&" + requestVar + ", &ignore_status)"));
                }
                //generate the decl of the request var
                decls.add(Util.toStmt("ilibRequest " + requestVar));
            }
        }
    }
}
