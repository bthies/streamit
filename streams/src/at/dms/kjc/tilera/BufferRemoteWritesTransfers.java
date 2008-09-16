package at.dms.kjc.tilera;

import at.dms.kjc.slicegraph.SchedulingPhase;

public class BufferRemoteWritesTransfers extends BufferTransfers {
    
    public BufferRemoteWritesTransfers(RotatingBuffer buf) {
        super(buf);
        
        generateStatements(SchedulingPhase.INIT);
     
        generateStatements(SchedulingPhase.STEADY);     
    }

    private void generateStatements(SchedulingPhase phase) {
        
    }
}
