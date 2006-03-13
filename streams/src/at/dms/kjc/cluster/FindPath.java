
package at.dms.kjc.cluster;

import java.lang.*;
import java.util.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.KjcOptions;

/*
 * Attempts to find a path between two stream nodes.
 */

public class FindPath {

    static void find(int src_id, int dst_id) {
    
        System.out.println("============================================================");
        System.out.println("Finding path from:"+src_id+" to:"+dst_id);

        LinkedList list = new LinkedList();
        list.add(new Integer(src_id));
    
        while (list.size() > 0) {
        
            int node = ((Integer)list.getFirst()).intValue();
            System.out.print("visiting node:"+node);

            SIROperator oper = NodeEnumerator.getOperator(node);

            if (oper instanceof SIRFilter) 
                { System.out.print(" [filter]"); }

            if (oper instanceof SIRJoiner) 
                { System.out.print(" [joiner]"); }
        
            if (oper instanceof SIRSplitter) 
                { System.out.print(" [splitter]"); }
        
            Vector v = RegisterStreams.getNodeOutStreams(oper);
            try {
            for (int y = 0; y < v.size(); y++) {
                NetStream n =(NetStream)v.get(y);
                list.addLast(new Integer(n.getDest()));
            }
            } catch (NullPointerException e) {
                if (KjcOptions.fusion) {
                    assert false : "-cluster -fusion does not support messaging. Try -cluster -standalone instead.";
                } else {
                    throw e;
                }
            }
            list.removeFirst();

            System.out.println();
        }

        System.out.println("============================================================");
    
    }

}
