package at.dms.kjc.tilera.arrayassignment;

import at.dms.kjc.JStatement;
import at.dms.kjc.tilera.Util;

import java.util.*;

public class ArrayAssignmentStatements {
    
    private LinkedList<AAStatement> assignments;
    
    public ArrayAssignmentStatements() {
        assignments = new LinkedList<AAStatement>();
    }
   
    public void addAssignment(String dstBufName, String dstOffsetName, int dstIndex, 
            String srcBufName, String srcOffsetName, int srcIndex) {
        SingleAAStmt newAss = new SingleAAStmt(dstBufName, dstOffsetName, dstIndex, 
                srcBufName, srcOffsetName, srcIndex);
        assignments.add(newAss);
    }
    
    public void compress() {
        LinkedList<AAStatement> newStmts = new LinkedList<AAStatement>();
        
        //create a dummy loop that will not be added as the first loop
        LoopedAssignments currentLoop = new LoopedAssignments("", "", -1, "", "", -1);
        for (AAStatement stmt : assignments) {
            assert stmt instanceof SingleAAStmt;
            
            //try to add the current single array assignment to the loop, 
            //if we can, thent he original loop will be returned, with the iteration count
            //incremented, otherwise a new loop will be started.
            LoopedAssignments newLoop = currentLoop.addStmt((SingleAAStmt)stmt);
            if (newLoop != currentLoop) {
                newStmts.add(newLoop);
                currentLoop = newLoop;
            }
        }
        
        assignments = newStmts;
    }
    
    public LinkedList<JStatement> toCompressedJStmts() {
        compress();
        
        LinkedList<JStatement> jstmts = new LinkedList<JStatement>();
        
        for (AAStatement ass : assignments) {
            jstmts.add(ass.toJStmt());
        }
        
        return jstmts;
    }
}
