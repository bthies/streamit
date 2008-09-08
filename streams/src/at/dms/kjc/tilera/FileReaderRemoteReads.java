package at.dms.kjc.tilera;

import java.util.List;

import at.dms.kjc.JStatement;
import at.dms.kjc.slicegraph.SchedulingPhase;

public class FileReaderRemoteReads extends FileReaderCode {

    public FileReaderRemoteReads(InputRotatingBuffer buf) {
        super(buf);
    }
    
    /**
     * Return the code that will transfer the items from the
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
     * Return declarations of variables 
     * @return declarations of variables
     */
    public List<JStatement> decls() {
        return decls;
    }
    
    /**
     * Return the ilib_wait statements that wait 
     * 
     * @return the wait statements
     */
    public List<JStatement> waitCallsSteady() {
        return waitCallsSteady;    
    }
}

