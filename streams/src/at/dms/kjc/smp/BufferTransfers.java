package at.dms.kjc.smp;

import java.util.LinkedList;
import java.util.List;

import at.dms.kjc.*;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.InterSliceEdge;
import at.dms.kjc.slicegraph.OutputSliceNode;
import at.dms.kjc.slicegraph.SchedulingPhase;

public abstract class BufferTransfers {
    protected RotatingBuffer parent;
    
    /** transfer commands used during reads */  
    protected List<JStatement> readCommandsInit;
    protected List<JStatement> readCommandsPrimePump;
    protected List<JStatement> readCommandsSteady;
    
    /** transfer commands used during writes */
    protected List<JStatement> writeCommandsInit;
    protected List<JStatement> writeCommandsPrimePump;
    protected List<JStatement> writeCommandsSteady;
    
    /** any declarations that are needed */
    protected List<JStatement> readDecls;
    protected List<JStatement> writeDecls;
    
    public BufferTransfers(RotatingBuffer buf) {
        parent = buf;
        
        readCommandsInit = new LinkedList<JStatement>();
        readCommandsPrimePump = new LinkedList<JStatement>();
        readCommandsSteady = new LinkedList<JStatement>();
        
        writeCommandsInit = new LinkedList<JStatement>();
        writeCommandsPrimePump = new LinkedList<JStatement>();
        writeCommandsSteady = new LinkedList<JStatement>();
        
        readDecls = new LinkedList<JStatement>();
        writeDecls = new LinkedList<JStatement>();
    }
    
    public List<JStatement> readTransferCommands(SchedulingPhase which) {
    	switch(which) {
    		case INIT: return readCommandsInit;
    		case PRIMEPUMP: return readCommandsPrimePump;
    		case STEADY: return readCommandsSteady;
    		default: assert(false);
    	}

    	return null;
    }
    
    public List<JStatement> writeTransferCommands(SchedulingPhase which) {
    	switch(which) {
			case INIT: return writeCommandsInit;
			case PRIMEPUMP: return writeCommandsPrimePump;
			case STEADY: return writeCommandsSteady;
			default: assert(false);
    	}
	
    	return null;
    }

    public List<JStatement> readDecls() {
        return readDecls;
    }
    
    public List<JStatement> writeDecls() {
        return writeDecls;
    }
    
    public abstract JStatement zeroOutTail(SchedulingPhase phase);
    public abstract JStatement zeroOutHead(SchedulingPhase phase);
    
    public abstract JMethodDeclaration peekMethod();
    public abstract JMethodDeclaration popMethod();
    public abstract JMethodDeclaration pushMethod();
}
