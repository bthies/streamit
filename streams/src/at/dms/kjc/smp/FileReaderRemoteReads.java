package at.dms.kjc.smp;

import java.util.List;
import at.dms.kjc.tilera.arrayassignment.*;
import at.dms.kjc.JStatement;
import at.dms.kjc.backendSupport.FilterInfo;
import at.dms.kjc.slicegraph.*;



public class FileReaderRemoteReads extends FileReaderCode {

    public FileReaderRemoteReads(InputRotatingBuffer buf) {
        super(buf);
        checkSimple();
        generateStatements(SchedulingPhase.INIT);
        generateStatements(SchedulingPhase.STEADY);
    }

    private void generateStatements(SchedulingPhase phase) {
        FilterInfo srcInfo = FilterInfo.getFilterInfo(fileOutput.getPrevFilter());
        FilterInfo dstInfo = FilterInfo.getFilterInfo(input.getNextFilter());
       
        //we are assuming that the downstream filter has only the file reader as input
        
        ArrayAssignmentStatements aaStmts = new ArrayAssignmentStatements();


        //if we don't receive anything, don't generate code
        if (dstInfo.totalItemsReceived(phase) > 0) {

            //rotations of the output for the file reader
            InterSliceEdge edge = input.getSingleEdge(phase);
            assert edge == input.getEdgeFrom(phase, fileOutput.getPrevFilter());
            assert dstInfo.totalItemsReceived(phase) % fileOutput.getWeight(edge, phase) == 0;
            int rotations = dstInfo.totalItemsReceived(phase) / fileOutput.getWeight(edge, phase);
            //the index into the destination buffer we are currently receiving to
            int destIndex = 0;

            String dst_buffer = parent.currentFileReaderBufName;
                        
            //we must account for the copy down in the pp and ss
            int copyDown = (phase == SchedulingPhase.INIT ? 0 : dstInfo.copyDown);

            for (int rot = 0; rot < rotations; rot++) {
                for (int weight = 0; weight < fileOutput.getWeights(phase).length; weight++) {
                    //do nothing if this edge is not in current weight
                    if (!fileOutput.weightDuplicatesTo(weight, edge, phase))
                        continue;
                    for (int item = 0; item < fileOutput.getWeights(phase)[weight]; item++) {
                        //add to the array assignment loop
                        int dstElement = (copyDown + destIndex++);
                        int srcIndex = ((rot * fileOutput.totalWeights(phase)) + fileOutput.weightBefore(weight, phase) + item);
                        aaStmts.addAssignment(dst_buffer, "", dstElement, "fileReadBuffer", "fileReadIndex__n" + parent.parent.getCoreNumber(), srcIndex);
                    }
                }
            }
        }
        
        List<JStatement> statements = null;
        switch (phase) {
        case INIT: statements = commandsInit; break;
        default: statements = commandsSteady; break;
        }
        
        statements.addAll(aaStmts.toCompressedJStmts());
        
        if (phase != SchedulingPhase.INIT) {
            //we must rotate the buffer when not in init
            statements.add(Util.toStmt(parent.currentFileReaderRotName + " = " + 
                    parent.currentFileReaderRotName + "->next"));
            statements.add(Util.toStmt(parent.currentFileReaderBufName + " = " + 
                    parent.currentFileReaderRotName + "->buffer"));
        }
        
        //every filter that reads from this file must increment the index of items read
        //in a phase, even if the filter does not read during the current phase 
        statements.add(Util.toStmt("fileReadIndex__n" + parent.parent.getCoreNumber() + " += " + srcInfo.totalItemsSent(phase)));
    }
    
    /**
     * Do some checks to make sure we will generate correct code for this distribution pattern.
     */
    private void checkSimple() {
        //right now just assert that the downstream filter of the file reader has only the FR
        //as input
        assert input.oneInput(SchedulingPhase.STEADY) && 
            (input.noInputs(SchedulingPhase.INIT) || input.oneInput(SchedulingPhase.INIT));
    }
    
}

