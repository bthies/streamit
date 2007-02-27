package at.dms.kjc.rstream;

import at.dms.kjc.common.*;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.util.Utils;
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.HashMap;
import java.io.*;
import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;
import java.util.Hashtable;
import at.dms.util.SIRPrinter;

/**
 * This class will try to convert the communication expressions of a filter
 * (push, pop, and peek) into buffer accesses that are indexed by a linear function
 * of the expression's enclosing loop indcution variables (MIVs).  It performs the conversion
 * inplace. 
 *
 * It handles pop/peek separately.  It will succeed for pops/peeks if all pops are 
 * enclosed only in analyzable control flow, which is incrementing doloops with static bounds or
 * if/then/else statements and the rate on each branch of an if are equal.
 *
 * Push statements are complementary.
 * 
 *
 * @author Michael Gordon
 */
public class ConvertChannelExprsMIV {

    private JLocalVariable popBuffer;
    private JLocalVariable pushBuffer;
    private int pushOffset;

    FilterFusionState fusionState;

    /** Create a new conversion object that will try to convert the communication
        expression in *current* to MIVs for the current stage (init if *isInit* == true).
        Don't forget to call tryMIV();
    **/
    public ConvertChannelExprsMIV(FilterFusionState current, boolean isInit)
    {
        //calculate push offset!!
    
        this.pushBuffer = null;

        //set the push buffer and the push counter if this filter pushes
        if (current.getNode().ways > 0) {
            assert current.getNode().ways == 1;
            assert current.getNode().getEdges()[0] != null;
        
            FusionState next = FusionState.getFusionState(current.getNode().getEdges()[0]);

            this.pushBuffer = current.getPushBufferVar(isInit);
            //if this is not the init, then remember to push after the items saved,
            //by the downstream filter (these items were not pop'ed and they were copied to 
            //the beginning of the downstream buffer)
            this.pushOffset = isInit ? 0 :  next.getRemaining(current.getNode(), isInit);
        }
    
        this.popBuffer = current.getBufferVar(null /*this is a filter, so only one previous */,
                                              isInit);
        fusionState = current;
    }
    
    /**
     * Try convert the communication expressions of the given FilterFusionState to 
     * buffer access with MIV index expression, starting at the top level do loop *top*.
     * Return true if the conversion was successful and false otherwise, if false, nothing
     * was accomplished and one must use another conversion scheme.
     **/
    public boolean tryMIV(JDoLoopStatement top) 
    {
        ConvertChannelExprsPhase1 phase1 = new ConvertChannelExprsPhase1(top);
        //run phase 1 that will check to see if the conversion can be correctly applied
        // and saves some state in the process.
        boolean passed = phase1.run();
        if (passed) {
            //assert that the calculated rates match the declared rates...
            assert fusionState.getFilter().getPushInt() == phase1.getTopLevelPushCount() :
                "Declared push rate does not match calculated rated for ConvertChannelExprMIV";
        
            assert fusionState.getFilter().getPopInt() == phase1.getTopLevelPopCount() :
                "Declared pop rate does not match calculated rated for ConvertChannelExprMIV";

            ConvertChannelExprsPhase2 phase2 = 
                new ConvertChannelExprsPhase2(top, phase1, 
                                              popBuffer, pushBuffer, 
                                              pushOffset);
            //run the second phase
            phase2.run();
        }
        //  else 
        //    System.out.println("** Could not generate MIVs for " + fusionState.getNode().contents);
    
        return passed;
    }    
}

