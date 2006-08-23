
package at.dms.kjc.cluster;

import at.dms.kjc.sir.*;
import java.util.*;
import at.dms.kjc.flatgraph.FlatNode;

/**
 * A class that finds an execution schedule for a stream graph
 * using breadth first search. Usage:
 *   DiscoverSchedule d = new DiscoverSchedule();
 *   d.findPhases(graphFlattener.top.contents);
 * The result is a set of phases, where the initializer for
 * phase a should be executed before initializer for phase b if a<b.
 * <br/>
 * This is overkill: we just need a topological sort of nodes where
 * the continuation of a feedback loop is not listed until after all
 * nodes in the feedback loop.
 * <br/>
 * TODO: we should be doing this in the FlatGraph, not on
 * the NetStream since the NetStream does not contain 0-weight edges so
 * we might miss nodes after a 0-weight splitter edge.
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
    // non-feedback-loop joiners: may be encountered several times.
    HashMap<SIROperator,Integer> splitjoinJoiners = new HashMap<SIROperator,Integer>();
    
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
        
        assert splitjoinJoiners.isEmpty();
        assert feedbackSplittersBody.isEmpty();
        assert feedbackSplittersLoop.isEmpty();
    }

    /**
     * Finds the next phase.
     * 
     * side-effects next-ops.
     */

    private void findNextPhase() {

        // iterate over nodes in current phase
        for (SIROperator oper : current_ops) {

            List<Tape> out = RegisterStreams.getNodeOutStreams(oper);
            
           // case: splitjoin splitter, make sure to schedule nodes at end of 0-weight edges, 
            // which will not be found by RegisterStreams.getNodeOutStreams.  Also record the 
            // number of incoming edges to the associated joiner so that the joiner's continuation
            // will not be processed until all of the joiner's predecessors have been processed.
            if (oper instanceof SIRSplitter && ! (oper.getParent() instanceof SIRFeedbackLoop)) {
                assert oper.getParent() instanceof SIRSplitJoin;
                SIRSplitter splitter = (SIRSplitter)oper;
                SIRJoiner joiner = ((SIRSplitJoin)splitter.getParent()).getJoiner();
                assert (! splitjoinJoiners.containsKey(joiner));
                int join_ways = 0;
                for (Tape in : RegisterStreams.getNodeInStreams(joiner)) {
                    if (in != null) {
                        join_ways++;
                    }
                }
                if (join_ways > 0) {
                    splitjoinJoiners.put(joiner, join_ways);
                }   
                
                FlatNode splitterNode = NodeEnumerator.getFlatNode(NodeEnumerator.getSIROperatorId(oper));
                FlatNode[] children = splitterNode.edges;
                int [] weights = splitterNode.weights;
                for (int i = 0; i < children.length; i++) {
                    if (weights[i] == 0) {
                        SIROperator next = children[i].contents;
                        next_ops.add(children[i].contents);
                        phases.put(next, number_of_phases + 1);
                    }
                }
                
//                List<SIROperator> splitterChildren = ((SIRSplitJoin)(splitter.getParent())).getChildren();
//                int[] splitterWeights = splitter.getWeights();
//                int j = 0;
//                for (SIROperator child : splitterChildren) {
//                    // getChildren includes the splitter and joiner as well as the
//                    // stream children.  We are only interested in the stream children
//                    if (child instanceof SIRStream && splitterWeights[j++] == 0) {
//                            next_ops.add(streamChild(child));   
//                    }
//                }
            }


            // check all nodes that are downstream from nodes in current phase
            for (int a = 0; a < out.size(); a++) {
                Tape ns = out.get(a);
                if (ns == null) continue;
                
                SIROperator next = NodeEnumerator.getOperator(ns.getDest());
                
                /*
                 * Here: if phases.containsKey, would be very surprised unless it is the
                 * joiner of a feedbackloop.  In a feedbackloop need to schedule the loop before
                 * the continuation. If a feedbackloop splitter has both a loop edge
                 * and a contiuation edge then we have to modify the order in which we
                 * iterate.
                 */

                // check if no phase assigned yet
                if (!phases.containsKey(next)) { 

                    // case: next is joiner at top of feedbackloop.
                    // we need to remember its splitter so as to not process the continuation
                    // of the feedbackloop before _both_ the body and loop portions are processed.
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
                     
                    // case: next is splitter at bottom of feedbackloop and body has been processed
                    // indicate that loop now needs to be processed.
                    if (next instanceof SIRSplitter && next.getParent() instanceof SIRFeedbackLoop
                            && feedbackSplittersBody.contains(next)) {
                        // if encounter splitter of non-degenerate feedbackloop, we "discover it"
                        // and go from processing body to processing loop.
                        feedbackSplittersBody.remove(next);
                        feedbackSplittersLoop.add(next);
                    }

                    // case: oper (parent of next) is the splitter at the bottom of a feedback loop.     
                    // if still processing feedbackloop, do not "discover" loop's continuation (the
                    // the first child of the splitter)
                    if (a == 0 && oper instanceof SIRSplitter && feedbackSplittersLoop.contains(oper)) {
                        continue;
                    }

                    // case: splitjoin joiner: do not process until all paths into joiner are 
                    // processed.  (So should never be 'discovered' more than once.)
                    if (next instanceof SIRJoiner
                            && !(next.getParent() instanceof SIRFeedbackLoop)) {
                        assert next.getParent() instanceof SIRSplitJoin;
                        SIRJoiner joiner = (SIRJoiner) next;
                        assert splitjoinJoiners.containsKey(joiner);
                        int join_ways_remaining = splitjoinJoiners.get(joiner);
                        if (join_ways_remaining > 1) {
                            splitjoinJoiners.put(joiner, join_ways_remaining - 1);
                            continue;
                        } else {
                            splitjoinJoiners.remove(joiner);
                        }
                    }
                    
                    // case? can it occur:
                    // feedbackloop with split (1,0) == no execution of loop (back edge) portion.
                } else {
                    
                    // case: joiner at top of feedback loop is seen twice: once at entry to feedback loop
                    // and once after processing the loop.
                    assert next instanceof SIRJoiner && next.getParent() instanceof SIRFeedbackLoop
                      && (feedbackSplittersLoop.contains(((SIRFeedbackLoop)next.getParent()).getSplitter())
                              || RegisterStreams.getNodeInStreams(((SIRFeedbackLoop)next.getParent()).getSplitter()).size() < 2 )
                        : "Node " + next + "is not the joiner of a FeedBackLoop being processed" ;
                      // have processed body and loop, continuation is next
                      SIRSplitter loopSplitter = ((SIRFeedbackLoop)next.getParent()).getSplitter();
                      if (feedbackSplittersLoop.remove(loopSplitter)) {
                          List<Tape> nexts = RegisterStreams.getNodeOutStreams(loopSplitter);
                          next = NodeEnumerator.getOperator(nexts.get(0).getDest());
                      } else {
                          // case: there is no continuation (saw joiner twice since processed loop
                          // but no entry in feedbackSplittersLoop since didn't have both loop and
                          // continuation.
                          continue;
                      }
                }
                phases.put(next, number_of_phases + 1);
                next_ops.add(next);
                // System.out.println("Operator: "+next+
                // " assigned to phase: "+
                // (number_of_phases+1));
            }
        }
    }
    
//    //  only call with SIRStream types that could be children of a SIRContainer.
//    private SIROperator streamChild(SIROperator s) { 
//        if (s instanceof SIRPipeline) {
//            return streamChild(((SIRPipeline)s).get(0));
//        }
//        if (s instanceof SIRSplitJoin) {
//            return (((SIRSplitJoin)s).getSplitter());
//        }
//        if (s instanceof SIRFeedbackLoop) {
//            return (((SIRFeedbackLoop)s).getJoiner());
//        }
//        // placed after containers to handle Filter, PhasedFilter
//        if (s instanceof SIRStream) {
//            return s;
//        }
//        assert false: "Cannot handle operator type " + s.getClass().getName();
//        return null;
//    }
}
