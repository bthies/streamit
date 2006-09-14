/**
 * 
 */
package at.dms.kjc.flatgraph;

import java.util.*;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.sir.*;

/**
 * @author mgordon
 *
 */
public class WorkSorted implements FlatVisitor {
    private LinkedList<FlatNode> sortedList; 
    private WorkEstimate workEstimates;
    
    public WorkSorted (WorkEstimate workEstimates) {
        sortedList = new LinkedList<FlatNode>();
        this.workEstimates = workEstimates;
    }
    
    public static LinkedList<FlatNode> getSortedList(FlatNode top, WorkEstimate workEstimates) {
        WorkSorted ws = new WorkSorted(workEstimates);
        
        top.accept(ws, null, true);
        
        /*
        Iterator<FlatNode> it = ws.sortedList.iterator();
        System.out.println("SortedList:");
        while (it.hasNext()) {
            System.out.println(it.next());
        }
        */
        return ws.sortedList;
    }
    
    public void visitNode(FlatNode node) {
        if (node.isFilter()) {
            //System.out.println("Adding " + node);
            //find the correct place to insert this node
            if (sortedList.size() == 0)
                sortedList.add(node);
            else if (workEstimates.getWork((SIRFilter)node.contents) < 
                    workEstimates.getWork((SIRFilter)sortedList.get(sortedList.size() -1).contents)) {
                sortedList.add(node);
            }
            else {
                for (int i = 0; i < sortedList.size(); i++) {
                    if (workEstimates.getWork((SIRFilter)node.contents) > 
                    workEstimates.getWork((SIRFilter)sortedList.get(i).contents)) {
                        sortedList.add(i, node);
                        break;
                    }
                }
            }
        }
    }
}
