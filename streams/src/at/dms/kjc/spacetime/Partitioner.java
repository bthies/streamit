package at.dms.kjc.spacetime;

import java.util.*;
import java.io.FileWriter;
import at.dms.kjc.sir.*;
import at.dms.util.Utils;
import at.dms.kjc.flatgraph2.*;
import at.dms.kjc.sir.linear.LinearAnalyzer;
import at.dms.kjc.sir.lowering.partition.*;

public abstract class Partitioner 
{
    //trace->bottleNeck work estimation
    protected HashMap traceBNWork;
    //the completed trace graph
    protected Trace[] traceGraph;
    protected RawChip rawChip;
    protected UnflatFilter[] topFilters;
    protected HashMap[] exeCounts;
    protected LinearAnalyzer lfa;
    //sirfilter -> work estimation
    protected WorkEstimate work;
    protected Trace[] topTraces;
    public Trace[] io;
    //filtercontent -> work estimation
    protected HashMap workEstimation;

    
    public Partitioner(UnflatFilter[] topFilters, HashMap[] exeCounts,LinearAnalyzer lfa,
		       WorkEstimate work, RawChip rawChip) 
    {
	this.rawChip = rawChip;
	this.topFilters = topFilters;
	this.exeCounts = exeCounts;
	this.lfa = lfa;
	this.work = work;
	topTraces = new Trace[topFilters.length];
	traceBNWork = new HashMap();
    }
    
    public abstract Trace[] partition();

    public boolean isIO(Trace trace) 
    {
	for (int i = 0; i < io.length; i++) {
	    if (trace == io[i])
		return true;
	}
	return false;
    }

    public Trace[] getTraceGraph()
    {
	assert traceGraph != null;
	return traceGraph;
    }

    public int getFilterWork(FilterTraceNode node) 
    {
	return ((Integer)workEstimation.get(node.getFilter())).intValue();
    }

    public int getTraceBNWork(Trace trace) 
    {
	assert traceBNWork.containsKey(trace);
	return ((Integer)traceBNWork.get(trace)).intValue();
    }
}
