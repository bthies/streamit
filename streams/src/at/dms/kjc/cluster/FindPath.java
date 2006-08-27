
package at.dms.kjc.cluster;

import java.lang.*;
import java.util.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.KjcOptions;

/**
 * Attempts to find a path between two stream nodes. Uses information 
 * about output tapes maintained by RegisterStreams. The search
 * starts at the source operator and performs breadth first search. 
 *
 * Aug 2006: UNCLEAR WHAT THE PURPOSE OF THIS CLASS IS.  It appears to
 * have no side-effects, and caused a performance bottleneck in MPEG.
 * Was it only for debugging?  Removing the only call to it, from
 * LatencyConstraints.
 */
public class FindPath {

    static void find(int src_id, int dst_id) {
        Set<Integer> examined = new HashSet<Integer>();
    
        if (ClusterBackend.debugPrint) {
            System.out.println("============================================================");
            System.out.println("Finding path from:"+src_id+" to:"+dst_id);
        }

        LinkedList<Integer> list = new LinkedList<Integer>();
        list.add(src_id);
    
        while (! list.isEmpty()) {
        
            int node = list.removeFirst();
            examined.add(node);
            
            if (ClusterBackend.debugPrint)
                System.out.print("visiting node:"+node);

            SIROperator oper = NodeEnumerator.getOperator(node);

            if (ClusterBackend.debugPrint) {
                if (oper instanceof SIRFilter) 
                    { System.out.print(" [filter]"); }
                
                if (oper instanceof SIRJoiner) 
                    { System.out.print(" [joiner]"); }
                
                if (oper instanceof SIRSplitter) 
                    { System.out.print(" [splitter]"); }
            }

            try {
            for (Tape n : RegisterStreams.getNodeOutStreams(oper)) {
              if (n != null) {
                int dest = n.getDest();
                if (! examined.contains(dest)) {
                    list.addLast(dest);
                }
              }
            }
            } catch (NullPointerException e) {
                // WTF: a NullPointerException would indicate that oper is not a valid 
                // thread number.  Could only happen on src_id, but then caller would
                // have been unable to get src_id to call this method.
                if (KjcOptions.fusion) {
                    // unclear that this error message is still accurate or relevant
                    assert false : "-cluster -fusion does not support messaging. Try -cluster -standalone instead.";
                } else {
                    throw e;
                }
            }

            if (ClusterBackend.debugPrint)
                System.out.println();
        }

        if (ClusterBackend.debugPrint)
            System.out.println("============================================================");
    }

}
