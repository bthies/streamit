
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

class DiscoverSchedule /*implements FlatVisitor*/ 
{

    HashMap phases = new HashMap();          // SIROperator -> Integer

    HashSet current_ops = new HashSet();   // Set of SIROperator(s)
    HashSet next_ops = new HashSet();      // Set of SIROperator(s)

    int number_of_phases = 0;

    /*
    public void visitNode(FlatNode node) {

        SIROperator oper = node.contents;
        Vector in = RegisterStreams.getNodeInStreams(oper);

        if (in.size() == 0) {
            phases.put(oper, new Integer(0));
            current_ops.add(oper);

            //System.out.println("Operator: "+oper+" assigned to phase: 0");

        }
    }
    */

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
        HashSet res = new HashSet();

        Set key_set = phases.keySet();
        Iterator iter = key_set.iterator();

        while (iter.hasNext()) {
            SIROperator oper = (SIROperator)iter.next();
            if (((Integer)phases.get(oper)).intValue() == phase) { 
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

	phases.put(top, new Integer(0));
	current_ops.add(top);
    
        do {

            findNextPhase();

            current_ops = next_ops;
            next_ops = new HashSet();
            number_of_phases++;

        } while (current_ops.size() != 0);
    
    }

    /**
     * Finds the next phase
     */

    private void findNextPhase() {

        Iterator i = current_ops.iterator();

        // iterate over nodes in current phase
        while (i.hasNext()) {

            SIROperator oper = (SIROperator)i.next();
            Vector out = RegisterStreams.getNodeOutStreams(oper);

            // check all nodes that are downstream from nodes in current phase
            for (int a = 0; a < out.size(); a++) {
        
                SIROperator next = 
                    NodeEnumerator.getOperator(
                                               ((NetStream)out.elementAt(a)).getDest());
        
                // check if no phase assigned yet
                if (!phases.containsKey(next)) { 

                    Vector in = RegisterStreams.getNodeInStreams(next);
                    boolean can_be_scheduled = true;

                    for (int b = 0; b < in.size(); b++) {

                        SIROperator prev = 
                            NodeEnumerator.getOperator(
                                                       ((NetStream)in.elementAt(b)).getSource());

                        if (!phases.containsKey(prev)) can_be_scheduled = false;
                    }

                    if (can_be_scheduled) {
                        phases.put(next, new Integer(number_of_phases+1));
                        next_ops.add(next);
                        //System.out.println("Operator: "+next+
                        //         " assigned to phase: "+
                        //         (number_of_phases+1));
                    }
                }
            }
        }
    }

}
