package at.dms.kjc.tilera;

import at.dms.kjc.slicegraph.SchedulingPhase;

public class OutputBufferRemoteWritesTransfers extends OutputBufferTransfers {
    
    public OutputBufferRemoteWritesTransfers(OutputRotatingBuffer buf) {
        super(buf);
        
        checkSimple(SchedulingPhase.INIT);
        generateStatements(SchedulingPhase.INIT);
        checkSimple(SchedulingPhase.STEADY);
        generateStatements(SchedulingPhase.STEADY);     
    }

    private void generateStatements(SchedulingPhase phase) {
    }
}
