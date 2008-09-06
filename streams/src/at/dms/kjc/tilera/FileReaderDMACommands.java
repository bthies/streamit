package at.dms.kjc.tilera;

import java.util.LinkedList;
import java.util.List;

import at.dms.kjc.JStatement;
import at.dms.kjc.backendSupport.FilterInfo;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.InterSliceEdge;
import at.dms.kjc.slicegraph.OutputSliceNode;
import at.dms.kjc.slicegraph.SchedulingPhase;

/**
 * This currently only works for if the filter that is downstream of the filter reader has only
 * the filter reader as input.
 * 
 * @author mgordon
 *
 */
public class FileReaderDMACommands {

    /** the output buffer that these dma commands uses as its source */
    private InputRotatingBuffer parent;
    /** the block of ilib_wait calls, one for each dma command generated, for steady
     * because we have concurrency, for init they are in commandsInit */
    private List<JStatement> waitCallsSteady;
    /** the dma commands block */
    private List<JStatement> commandsSteady;
    /** the dma commands block */
    private List<JStatement> commandsInit;
    /** the output slice node */
    private InputSliceNode input;
    /** any declarations that are needed */
    private List<JStatement> decls;
    /** the output slice node of the file */
    private OutputSliceNode fileOutput;
    /** the edge between the file reader and this input buffer */
    private InterSliceEdge edge;
    
    public FileReaderDMACommands(InputRotatingBuffer buf) {
        parent = buf;
        waitCallsSteady = new LinkedList<JStatement>();
        commandsSteady = new LinkedList<JStatement>();
        commandsInit = new LinkedList<JStatement>();
        decls = new LinkedList<JStatement>();
        input = parent.filterNode.getParent().getHead();
        fileOutput = input.getSingleEdge(SchedulingPhase.STEADY).getSrc();    
        edge = input.getSingleEdge(SchedulingPhase.STEADY);
        checkSimple();
        generateStatements();
    }

    private void generateStatements() {
        FilterInfo srcInfo = FilterInfo.getFilterInfo(fileOutput.getPrevFilter());
        FilterInfo dstInfo = FilterInfo.getFilterInfo(input.getNextFilter());
                
        String requestVar = parent.rotStructName  + "_request";
        int itemBytes = Util.getTypeSize(parent.getType()) * 4;
        
        //generate the dma command
       
        //if we are in the init stage, transfer into the current buffer because we are
        //not double buffering, also, don't skip the copydown because this is the first
        //transfer
        String dst_init = parent.currentBufName;
        //we want to transfer into the next buffer if we are in the steady (primepump)  
        String dst_steady = parent.currentRotName + "->next->buffer + " + 
            (dstInfo.copyDown);
        //the stride should always be 1 in this case, but keep this here for the future
        String dst_stride_steady = "" + (itemBytes * input.totalWeights(SchedulingPhase.STEADY));
        String dst_stride_init = "" + (itemBytes * input.totalWeights(SchedulingPhase.INIT));
        //the source is always the file read buffer
        String src_steady = "fileReadBuffer + fileReadIndex + " + (fileOutput.weightBefore(edge, SchedulingPhase.STEADY));
        String src_init = "fileReadBuffer + fileReadIndex + " + (fileOutput.weightBefore(edge, SchedulingPhase.INIT));
        String src_stride_steady = "" + (itemBytes * fileOutput.totalWeights(SchedulingPhase.STEADY));
        String src_stride_init = "" + (itemBytes * fileOutput.totalWeights(SchedulingPhase.INIT));
        String block_size_init = "" + (itemBytes * fileOutput.getWeight(edge, SchedulingPhase.INIT));
        String block_size_steady = "" + (itemBytes * fileOutput.getWeight(edge, SchedulingPhase.STEADY));

        String num_blocks_init = "" + 
            srcInfo.totalItemsSent(SchedulingPhase.INIT) / fileOutput.totalWeights(SchedulingPhase.INIT);

        assert (dstInfo.totalItemsPopped(SchedulingPhase.STEADY) / input.totalWeights(SchedulingPhase.STEADY)) ==
            (srcInfo.totalItemsSent(SchedulingPhase.STEADY) / fileOutput.totalWeights(SchedulingPhase.STEADY));

        String num_blocks_steady = "" + 
            dstInfo.totalItemsPopped(SchedulingPhase.STEADY) / input.totalWeights(SchedulingPhase.STEADY);


        commandsInit.add(Util.toStmt("ilib_mem_start_strided_dma(" +
                dst_init + ", " + 
                dst_stride_init + ", " + 
                src_init + ", " + 
                src_stride_init + ", " + 
                block_size_init + ", " + 
                num_blocks_init + ", " + 
                "&" + requestVar + ")"));
        commandsInit.add(Util.toStmt("ilib_wait(&" + requestVar + ", &ignore_status)"));
        commandsInit.add(Util.toStmt("fileReadIndex += " + srcInfo.totalItemsSent(SchedulingPhase.INIT)));

        commandsSteady.add(Util.toStmt("ilib_mem_start_strided_dma(" +
                dst_steady + ", " + 
                dst_stride_init + ", " + 
                src_init + ", " + 
                src_stride_init + ", " + 
                block_size_init + ", " + 
                num_blocks_steady + ", " +
                "&" + requestVar + ")"));
        //increment the file index
        commandsSteady.add(Util.toStmt("fileReadIndex += " + srcInfo.totalItemsSent(SchedulingPhase.STEADY)));
        //generate the wait call
        waitCallsSteady.add(Util.toStmt("ilib_wait(&" + requestVar + ", &ignore_status)"));

        //generate the decl of the request var
        decls.add(Util.toStmt("ilibRequest " + requestVar));
    }

    /**
     * Do some checks to make sure we will generate correct code for this distribution pattern.
     */
    private void checkSimple() {
        assert input.singleAppearance();
        assert fileOutput.singleAppearance();
        assert input.getWeight(input.getSingleEdge(SchedulingPhase.INIT), SchedulingPhase.INIT) == 
            fileOutput.getWeight(input.getSingleEdge(SchedulingPhase.INIT), SchedulingPhase.INIT);
        assert input.getWeight(input.getSingleEdge(SchedulingPhase.STEADY),SchedulingPhase.STEADY) == 
            fileOutput.getWeight(input.getSingleEdge(SchedulingPhase.STEADY), SchedulingPhase.STEADY);
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
