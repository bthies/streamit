package at.dms.kjc.spacetime;

import java.util.LinkedList;
import java.util.HashSet;
import java.util.Vector;
import at.dms.util.Utils;

/** This class returns a legal traversal of the trace forrest **/

public class TraceTraversal
{
    public static LinkedList getTraversal(Trace[] forrest) 
    {
	LinkedList traversal = new LinkedList();

	printForrest(forrest);

	//we'll do one forrest at a time for now
	for (int i = 0; i < forrest.length; i++) {
	    HashSet visited = new HashSet();
	    Vector queue = new Vector();
	    Trace trace;
	    
	    queue.add(forrest[i]);
	    
	    while (!queue.isEmpty()) {
		//we should always be able to find one trace on each iteration

		trace = null;
		//find the first Trace with its dependencies satisfied...
		for (int j = 0; j < queue.size(); j++) {
		    Trace current = (Trace)queue.get(j);
		    boolean satisfied = true;
		    		    
		    //cycle thru the backedges and see if they were all visited...
		    for (int k = 0; k < current.getDepends().length; k++) {
			if (!visited.contains(current.getDepends()[k])) {
			    satisfied = false;
			    break;
			}
		    }

		    // found a Trace to add, remove it and break
		    if (satisfied) {
			trace = current;
			queue.remove(j);
			break;
		    }
		}
		

		//add the trace to the traversal and show that it is visited...
		if (trace != null) {
		    //System.out.println("Adding Trace in traversal " + trace);
		    traversal.add(trace);
		    visited.add(trace);
		}
		else {
		    Utils.fail("Trace should not be null!");
		}
		
		//cycle thru all the edges and add them to the queue if 
		//they are not already in there...
		for (int j = 0; j < trace.getEdges().length; j++) {
		    if (!queue.contains(trace.getEdges()[j])) {
			queue.add(trace.getEdges()[j]);
			//System.out.println("Adding trace to queue " + trace.getEdges()[j]);
		    }
		}
		//System.out.println(queue.size());
	    }	    
	}

	return traversal;
    }
    
    public static void printForrest(Trace forrest[]) 
    {
	System.out.println("Forrests: " + forrest.length);
	for (int i = 0; i < forrest.length; i++)
	    printTraces(forrest[i], 0);
    }

    public static void printTraces(Trace trace, int spaces) 
    {
	for (int i = 0; i < spaces; i++)
	    System.out.print(" ");
	System.out.print("Trace: (");
	TraceNode temp = trace.getHead();
	while (temp != null) {
	    System.out.print(temp);
	    temp = temp.getNext();
	    if (temp != null)
		System.out.print(" -> ");
	}
	System.out.println(")");
	for (int i = 0; i < trace.getEdges().length; i++) {
	    printTraces(trace.getEdges()[i], spaces + 5);
	}
    }
}
