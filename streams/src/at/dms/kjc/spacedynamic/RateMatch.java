package at.dms.kjc.spacedynamic;

import at.dms.kjc.common.RawUtil;
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
import java.util.HashMap;
import java.util.HashSet;
import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;
import java.util.Hashtable;

/**
 * 
 * 
 * @author mgordon
 *
 */
public class RateMatch implements FlatVisitor
{
    /** the ssg this RateMatch object operates on */
    private SpdStaticStreamGraph ssg;
    /** true if we cannot rate match */ 
    private boolean fail;
    /** True if we have encountered a joiner during our traversal of the graph     */
    private boolean seenJoiner;
    /** The order in which to fire the filters of this application, used by 
     * FineGrainedSimulator. */
    private LinkedList<FlatNode> schedule;
    /** iterator used to access schedule */
    private Iterator<FlatNode> scheduleIt; 
        
    /**
     * Determine if we can rate match this stream graph and if so calculate
     * some internal state to be used by later passes. 
     * 
     * @return true if we can rate match 
     */
    public boolean check() 
    {
        assert KjcOptions.dup == 1 : "Rate Matching only works with data-parallelization (dup 1).";
        //Each ssg is wrapped in a splitjoin, so to get to the real top level, you 
        //must to through the splitter
        FlatNode realTopLevel = ssg.getTopLevel().getEdges()[0]; 
        //make sure that the top filter is a file reader, we are requiring that now
        if (!(realTopLevel.contents instanceof SIRFileReader)) {
            System.out.println("(toplevel not fr) " + ssg.getTopLevel().contents);
            return false;    
        }
        
        //add the file reader as the first node in the schedule
        schedule.add(ssg.getTopLevel());
        
        //add the remaining filters to the schedule and check the joiner
        realTopLevel.accept(this, null, true);
        
        //make the schedule ready for other
        if (!fail)
            resetSchedule();
        
        return !fail;
    }

    public RateMatch(SpdStaticStreamGraph ssg) 
    {
        this.ssg = ssg;
        fail = false;
        seenJoiner = false;
        schedule = new LinkedList<FlatNode>();
    }
    
    /**
     * Visit each flatnode in the static stream graph and find the joiners.
     */
    public void visitNode(FlatNode node) 
    {
        //if we have already failed then just return
        if (fail)
            return;
            
        //add the parallelized filters to the schedule in the order specified by the
        //splitter, this will remove the need for a joiner
        if (node.isSplitter()) {
            for (int i = 0; i < node.ways; i++)
                schedule.add(node.getEdges()[i]);
        }
        if (node.isJoiner()) {
            if (seenJoiner) {
                System.out.println("(>1 joiners)");
                fail = true;
            }
            seenJoiner = true;
            //now make sure that each weight of the joiner gets all the items 
            //from the upstream filter
            for (int i = 0; i < node.inputs; i++) {
                if (node.getIncomingWeight(node.incoming[i]) != 
                    node.incoming[i].getFilter().getPushInt()) {
                    fail = true;
                    System.out.println("(weight != pop)");
                    return;
                }
            }
            //now add the file writer
            if (!(node.getEdges()[0].contents instanceof SIRFileWriter)) {
                System.out.println("(sink not fw)");    
                fail = true;
            }
            schedule.add(node.getEdges()[0]);
        }
    }
    
    public void resetSchedule() {
        scheduleIt = schedule.iterator();
    }
    
    public FlatNode getNextNode() {
        return scheduleIt.next();
    }
}
