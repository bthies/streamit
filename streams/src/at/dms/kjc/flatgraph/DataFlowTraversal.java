package at.dms.kjc.flatgraph;

import at.dms.kjc.flatgraph.FlatNode;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.Vector;

public class DataFlowTraversal 
{
    private static LinkedList traversal;
    
    //returns a linked listed with a breadth first traversal 
    //of the stream graph starting at top
    public static LinkedList getTraversal(FlatNode top) 
    {
	traversal = new LinkedList();
	if (top == null)
	    return traversal;
	
	HashSet added = new HashSet();
	Vector queue = new Vector();
	FlatNode node;
	
	//add top to added list
	added.add(top);
	//add top to the queue
	queue.add(top);
	
	while(!queue.isEmpty()) {
	    node = (FlatNode)queue.get(0);
	    queue.remove(0);
	    
	    if (node == null)
		continue;
	    
	    //add the current node to the traversal
	    traversal.add(node);

	    //to keep the order of the nodes of a splitjoin in the correct order
	    //(the order defined by the joiner) add to the queue in the reverse order
	    for (int i = 0; i < node.ways; i++) {
		FlatNode downstream = node.edges[i];
		if (downstream == null)
		    continue;
		
		if (!added.contains(downstream)) {
		    //see if we can add the edge
		    boolean canAdd = true;
		    for (int j = 0; j < downstream.inputs; j++) {
			if (downstream.incoming[j] == null)
			    continue;
			//keep going if this the incoming feedback edge of
			//a joiner of a feedback loop
			if (downstream.isFeedbackIncomingEdge(j))
			    continue;

			if (!added.contains(downstream.incoming[j])) {
			    canAdd = false;
			    break;
			}
		    }
		    //if we get here then all the inputs are schedule to be visiting
		    //before this node
		    if (canAdd) {
			queue.add(downstream); 
			added.add(downstream);
		    }
		}
	    }
	} 
	return traversal;
    }
}
