package at.dms.kjc.spacetime;

import java.util.*;
import java.io.FileWriter;
import at.dms.kjc.sir.*;
import at.dms.util.Utils;
import at.dms.kjc.flatgraph2.*;
import at.dms.kjc.sir.linear.LinearAnalyzer;
import at.dms.kjc.sir.lowering.partition.*;

public class SimpleScheduler 
{
    public Partitioner partitioner;
    
    public SimpleScheduler(Partitioner partitioner) 
    {
	this.partitioner = partitioner;
    }
    
    public void schedule() 
    {
	//sort traces...
	Trace[] sortedTraces = (Trace[])partitioner.getTraceGraph().clone();
	Arrays.sort(sortedTraces, new CompareTraceBNWork(partitioner));
	List traceList = Arrays.asList(sortedTraces);
	
	Iterator it = traceList.iterator();
	while (it.hasNext()) {
	    Trace trace = (Trace)it.next();
	    System.out.println(trace + " " + partitioner.getTraceBNWork(trace));
	}
    }
}


public class CompareTraceBNWork implements Comparator
{
    private Partitioner partitioner;
    
    public CompareTraceBNWork(Partitioner partitioner) 
    {
	this.partitioner = partitioner;
    }
    
    public int compare (Object o1, Object o2) 
    {
	assert o1 instanceof Trace && o2 instanceof Trace;
	
	if (partitioner.getTraceBNWork((Trace)o1) < 
	    partitioner.getTraceBNWork((Trace)o2))
	    return -1;
	else if (partitioner.getTraceBNWork((Trace)o1) ==
		 partitioner.getTraceBNWork((Trace)o2))
	    return 0;
	else
	    return 1;
    }
}
