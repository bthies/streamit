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
	
	//printForrest(forrest);

	//we'll do one forrest at a time for now
	for (int i = 0; i < forrest.length; i++) {
	    System.out.println("Traversal for Forest " + i);
	    HashSet visited = new HashSet();
	    Vector queue = new Vector();
	    Trace trace;
	    
	    //queue.add(forrest[i]);
	    
	    //try to add the first node of the forrest to the traversal
	    addToTraversal(forrest[i], traversal, visited, queue);
		
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
		addToTraversal(trace, traversal, visited, queue);
	    }	    
	}

	return traversal;
    }
    
    private static void addToTraversal(Trace trace, LinkedList traversal, HashSet visited, 
				       Vector queue) 
    {
	//add the trace to the traversal and show that it is visited...
	assert trace != null : "Trace should not be null!";
	
	System.out.println("Adding Trace in traversal " + trace);
	traversal.add(trace);
	visited.add(trace);
	
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
    

    public static void printForrest(Trace forrest[]) 
    {
	System.out.println("Forrests: " + forrest.length);
	HashSet visited = new HashSet();
	for (int i = 0; i < forrest.length; i++) {
	    System.out.println(" ***** Forest " + i + " ******");
	    
	    printTraces(forrest[i], 0, visited);
	}
	
    }

    public static void printTraces(Trace trace, int spaces, HashSet visited) 
    {
	for (int i = 0; i < spaces; i++)
	    System.out.print(" ");
	System.out.print("Trace: (Depends: (" + trace.getDepends().length);
	for (int i = 0; i < trace.getDepends().length; i++) 
	    System.out.print(trace.getDepends()[i] + " ");
	System.out.print(")");
	
	visited.add(trace);
	TraceNode temp = trace.getHead();
	while (temp != null) {
	    System.out.print(temp);
	    if (temp.isFilterTrace()) {
		FilterInfo fi = FilterInfo.getFilterInfo((FilterTraceNode)temp);
		System.out.print("(" + fi.initMult + ", " + fi.primePump + ", " +
				 fi.steadyMult + ")");
	    }
	    temp = temp.getNext();
	    if (temp != null)
		System.out.print(" -> ");
	}
	System.out.println("), connected to " + trace.getEdges().length);
	for (int i = 0; i < trace.getEdges().length; i++) {
	    if (!visited.contains(trace.getEdges()[i]))
		printTraces(trace.getEdges()[i], spaces + 5, visited);
	}
    }
}
