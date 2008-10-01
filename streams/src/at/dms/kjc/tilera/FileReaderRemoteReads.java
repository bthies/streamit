package at.dms.kjc.tilera;

import java.util.List;

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
        List<JStatement> statements = null;
        //we are assuming that the downstream filter has only the file reader as input

        //if we don't receive anything then just return!
        if (dstInfo.totalItemsReceived(phase) == 0) {
            return;
        }
            
        switch (phase) {
            case INIT: statements = commandsInit; break;
            case PRIMEPUMP: assert false; break;
            case STEADY: statements = commandsSteady; break;
        }
        
        //rotations of the output for the file reader
        InterSliceEdge edge = input.getSingleEdge(phase);
        assert edge == input.getEdgeFrom(phase, fileOutput.getPrevFilter());
        assert dstInfo.totalItemsReceived(phase) % fileOutput.getWeight(edge, phase) == 0;
        int rotations = dstInfo.totalItemsReceived(phase) / fileOutput.getWeight(edge, phase);
        //the index into the destination buffer we are currently receiving to
        int destIndex = 0;
        
        //The destination buffer is the read buffer for the downstream filter
        //in the init stage, just write into the head, for other stages, we write into
        //the buffer ahead of the current buffer that will be read
        String dst_buffer = (SchedulingPhase.isInitOrPrimepump(phase) ? 
                parent.currentReadBufName : 
                    parent.currentReadRotName + "->next->buffer");
        //we must account for the copy down in the pp and ss
        int copyDown = (SchedulingPhase.isInitOrPrimepump(phase) ? 0 : dstInfo.copyDown);
        
        for (int rot = 0; rot < rotations; rot++) {
            for (int weight = 0; weight < fileOutput.getWeights(phase).length; weight++) {
                //do nothing if this edge is not in current weight
                if (!fileOutput.weightDuplicatesTo(weight, edge, phase))
                    continue;
                for (int item = 0; item < fileOutput.getWeights(phase)[weight]; item++) {
                    String dest = dst_buffer + "[" + (copyDown + destIndex++) +"]";
                    String src = "fileReadBuffer[fileReadIndex + " + 
                    ((rot * fileOutput.totalWeights(phase)) + fileOutput.weightBefore(weight, phase) + item) +
                    "]";
                    
                    statements.add(Util.toStmt(dest + " = " + src));
                }
            }
        }
        statements.add(Util.toStmt("fileReadIndex += " + srcInfo.totalItemsSent(phase)));
    }
    
    /**
     * Do some checks to make sure we will generate correct code for this distribution pattern.
     */
    private void checkSimple() {
        //right now just assert that the downstream filter of the file reader has only the FR
        //as input
        assert input.oneInput();
    }
    
}

