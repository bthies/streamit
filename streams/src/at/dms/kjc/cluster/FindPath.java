
package at.dms.kjc.cluster;

import java.lang.*;
import java.util.*;
import at.dms.kjc.sir.*;

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
            for (int y = 0; y < v.size(); y++) {
                NetStream n =(NetStream)v.get(y);
                list.addLast(new Integer(n.getDest()));
            }

            list.removeFirst();

            System.out.println();
        }

        System.out.println("============================================================");
    
    }

}
