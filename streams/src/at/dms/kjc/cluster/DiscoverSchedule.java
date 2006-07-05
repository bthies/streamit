
package at.dms.kjc.cluster;

import at.dms.kjc.sir.*;
import java.util.*;

/**
 * A class that finds an execution schedule for a stream graph
 * using breadth first search. Usage:
 *   DiscoverSchedule d = new DiscoverSchedule();
 *   d.findPhases(graphFlattener.top.contents);
 * Note that this will not work if there are feedback loops.
 * The result is a set of phases, where an operator from
 * phase a should be executed before operator from phase b if a<b.
 */

class DiscoverSchedule
{
    // map SIROperator => phase number
    HashMap<SIROperator,Integer> phases = new HashMap<SIROperator,Integer>();

    // SIROperators in current phase
    HashSet<SIROperator> current_ops = new HashSet<SIROperator>();
    // SIROperators being collected for next phase
    HashSet<SIROperator> next_ops = new HashSet<SIROperator>();

    // feedback loop splitters that when discovered should cause switch from
    // scheduling body to scheduling loop.
    HashSet<SIROperator> feedbackSplittersBody = new HashSet<SIROperator>();
    // feedback loop splitters that have not yet finished processing loop
    // so the continuation of the feedbackloop should not be scheduled.
    HashSet<SIROperator> feedbackSplittersLoop = new HashSet<SIROperator>();
    
    int number_of_phases = 0;

    /**
     * Returns the number of phases found
     * @return the number of phases found
     */

    public int getNumberOfPhases() {
        return number_of_phases;
    }

    /**
     * Returns all operators in a given phase
     * @return all operators in a given phase
     */

    public HashSet getAllOperatorsInPhase(int phase) {
        HashSet<SIROperator> res = new HashSet<SIROperator>();

        Set key_set = phases.keySet();
        Iterator iter = key_set.iterator();

        while (iter.hasNext()) {
            SIROperator oper = (SIROperator)iter.next();
            if (phases.get(oper) == phase) { 
                res.add(oper);
            }
        }
    
        return res; 
    }
    

    /**
     * Finds all phases associated with a stream program
     * @param top operator
     */

    public void findPhases(SIROperator top) {

	phases.put(top, 0);
	current_ops.add(top);
    
        do {

            findNextPhase();

            current_ops = next_ops;
            next_ops = new HashSet<SIROperator>();
            number_of_phases++;

        } while (current_ops.size() != 0);
    
    }

    /**
     * Finds the next phase.
     * 
     * side-effects next-ops.
     */

    private void findNextPhase() {

        Iterator i = current_ops.iterator();

        // iterate over nodes in current phase
        while (i.hasNext()) {

            SIROperator oper = (SIROperator)i.next();
            Vector<NetStream> out = RegisterStreams.getNodeOutStreams(oper);

            // check all nodes that are downstream from nodes in current phase
            for (int a = 0; a < out.size(); a++) {
        
                SIROperator next = 
                    NodeEnumerator.getOperator(out.elementAt(a).getDest());
                
                /*
                 * Here: if phases.containsKey, would be very surprised unless it is the
                 * joiner of a feedbackloop.  In a feedbackloop need to schedule the loop before
                 * the continuation. If a feedbackloop splitter has both a loop edge
                 * and a contiuation edge then we have to modify the order in which we
                 * iterate.
                 */

                // check if no phase assigned yet
                if (!phases.containsKey(next)) { 

                    if (next instanceof SIRJoiner && next.getParent() instanceof SIRFeedbackLoop) {
                        // first encounter with feedbackloop joiner (or would have had phases.containsKey(next))
                        SIRSplitter loopSplitter = ((SIRFeedbackLoop)next.getParent()).getSplitter();
                        // debugging: this should be first encounter with this feedbackloop joiner
                        assert ! feedbackSplittersBody.contains(loopSplitter) && ! feedbackSplittersLoop.contains(loopSplitter);
                        int split_ways =  RegisterStreams.getNodeOutStreams(loopSplitter).size();
                        assert split_ways <= 2;
                        // if non-degenerate feedback loop, then need some fancy work when encounter splitter
                        if (split_ways == 2) {
                            feedbackSplittersBody.add(loopSplitter);
                        }
                    }
                     
                    if (next instanceof SIRSplitter && next.getParent() instanceof SIRFeedbackLoop
                            && feedbackSplittersBody.contains(next)) {
                        // if encounter splitter of non-degenerate feedbackloop, we "discover it"
                        // and go from processing body to processing loop.
                        feedbackSplittersBody.remove(next);
                        feedbackSplittersLoop.add(next);
                    }

                    // if still processing feedbackloop, do not "discover" loop's continuation.  
                    if (a == 0 && oper instanceof SIRSplitter && feedbackSplittersLoop.contains(oper)) {
                        continue;
                    }


                } else {
                    assert next instanceof SIRJoiner && next.getParent() instanceof SIRFeedbackLoop
                      && (feedbackSplittersLoop.contains(((SIRFeedbackLoop)next.getParent()).getSplitter())
                              || RegisterStreams.getNodeInStreams(((SIRFeedbackLoop)next.getParent()).getSplitter()).size() < 2 )
                        : "Node " + next + "is not the joiner of a FeedBackLoop being processed" ;
                      // have processed body and loop, continuation is next
                      SIRSplitter loopSplitter = ((SIRFeedbackLoop)next.getParent()).getSplitter();
                      feedbackSplittersLoop.remove(loopSplitter);
                      Vector<NetStream> nexts = RegisterStreams.getNodeOutStreams(loopSplitter);
                      next = NodeEnumerator.getOperator(
                              nexts.elementAt(0).getDest());
     
                }
                phases.put(next, number_of_phases + 1);
                next_ops.add(next);
                // System.out.println("Operator: "+next+
                // " assigned to phase: "+
                // (number_of_phases+1));
            }
        }
    }
}
