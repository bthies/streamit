package at.dms.kjc.spacetime;

import java.util.*;
import java.io.FileWriter;
import at.dms.kjc.sir.*;
import at.dms.util.Utils;
import at.dms.kjc.flatgraph2.*;
import at.dms.kjc.sir.linear.LinearAnalyzer;
import at.dms.kjc.sir.lowering.partition.*;

public class SimplePartitioner extends Partitioner
{
    //trace work threshold, higher number, more restrictive, smaller traces
    private static final double TRASHOLD = 0.8;
    
    public SimplePartitioner(UnflatFilter[] topFilters, HashMap[] exeCounts,LinearAnalyzer lfa,
			     WorkEstimate work, RawChip rawChip) 
    {
	super(topFilters, exeCounts, lfa, work, rawChip);
	workEstimation = new HashMap();
    }
    

    public Trace[] partition()
    {
	LinkedList queue = new LinkedList();
	HashSet visited = new HashSet();
	LinkedList traces = new LinkedList();
	LinkedList topTracesList = new LinkedList(); //traces with no incoming dependencies
	HashSet topUnflat = new HashSet();

	//map unflatEdges -> Edge?
	HashMap edges = new HashMap();
	//add the top filters to the queue
	for (int i = 0; i < topFilters.length;i++) {
	    topUnflat.add(topFilters[i]);
	    queue.add(topFilters[i]);
	}
	
	
	while (!queue.isEmpty()) {
	    UnflatFilter unflatFilter = (UnflatFilter)queue.removeFirst();
	    if (!visited.contains(unflatFilter)) {
		visited.add(unflatFilter);
		//the filter content for the new filter
		FilterContent filterContent = getFilterContent(unflatFilter);
		//remember the work estimation based on the filter content
		int workEstimate=getWorkEstimate(unflatFilter);
		workEstimation.put(filterContent, 
				   new Integer(workEstimate));
		
		TraceNode node;
		Trace trace;
		int filtersInTrace = 1;
	    
		//create the input trace node
		if (unflatFilter.in != null && unflatFilter.in.length > 0) {
		    Edge[] inEdges = new Edge[unflatFilter.in.length];
		    node = new InputTraceNode(unflatFilter.inWeights, 
					      inEdges);
		    for (int i = 0; i < unflatFilter.in.length; i++) {
			UnflatEdge unflatEdge = unflatFilter.in[i];
			//get the edge
			Edge edge = (Edge)edges.get(unflatEdge);
			//we haven't see the edge before
			if (edge == null) {  //set dest?, wouldn't this always be the dest
			    edge = new Edge((InputTraceNode)node);
			    edges.put(unflatEdge, edge);
			}
			else //we've seen this edge before, set the dest to this node
			    edge.setDest((InputTraceNode)node);
			inEdges[i] = edge;
		    }
		    trace = new Trace((InputTraceNode)node);
		    
		    if (filterContent.isLinear()) { //Jasper's linear stuff??
			int times=filterContent.getArray().length/filterContent.getPopCount();
			if(times>1) {
			    if(times>16)
				times=16;
			    FilterContent[] linearStuff=LinearFission.fiss(filterContent,times);
			    workEstimation.remove(filterContent);
			    for(int i=0;i<linearStuff.length;i++) {
				FilterContent fissedContent=linearStuff[i];
				FilterTraceNode filterNode=new FilterTraceNode(fissedContent);
				node.setNext(filterNode);
				filterNode.setPrevious(node);
				node=filterNode;
				//Dummy work estimate for now
				workEstimation.put(fissedContent, 
						   new Integer(workEstimate/times));
			    }
			} else {
			    FilterTraceNode filterNode = new FilterTraceNode(filterContent);
			    node.setNext(filterNode);
			    filterNode.setPrevious(node);
			    node = filterNode;
			}
		    }
		    else {
			FilterTraceNode filterNode = new FilterTraceNode(filterContent);
			node.setNext(filterNode);
			filterNode.setPrevious(node);
			node = filterNode;
		    }
		}
		else {  //null incoming arcs
		    node = new FilterTraceNode(filterContent);
		    trace = new Trace(node);
		}
		
		if (topUnflat.contains(unflatFilter)) {
		    assert unflatFilter.in == null || unflatFilter.in.length == 0;
		    topTracesList.add(trace);
		}
		else 
		    assert unflatFilter.in.length > 0;

		//should be at least one filter in the trace by now, don't worry about 
		//linear stuff right now...

		traces.add(trace);

		int bottleNeckWork = getWorkEstimate(unflatFilter);
		//try to add more filters to the trace...
		while (continueTrace(unflatFilter, filterContent.isLinear(), bottleNeckWork,
				     ++filtersInTrace)) { //tell continue trace you are trying to put
		                                          //another filter in the trace
		    UnflatFilter downstream = unflatFilter.out[0][0].dest;
		    FilterContent dsContent = getFilterContent(downstream);

		    //remember the work estimation based on the filter content
		    workEstimation.put(dsContent, 
				       new Integer(getWorkEstimate(downstream)));
		    if (getWorkEstimate(downstream) > bottleNeckWork)
			bottleNeckWork = getWorkEstimate(downstream);
		    
		    if (dsContent.isLinear()) { //Jasper's linear stuff?
			int times=dsContent.getArray().length/dsContent.getPopCount();
			if(times>1) {
			    if(times>16)
				times=16;
			    FilterContent[] linearStuff=LinearFission.fiss(dsContent,times);
			    workEstimation.remove(dsContent);
			    //create filter nodes for each row of the matrix?
			    for (int i = 0; i < linearStuff.length;i++) {
				FilterContent fissedContent=linearStuff[i];
				FilterTraceNode filterNode = new FilterTraceNode(fissedContent);
				node.setNext(filterNode);
				filterNode.setPrevious(node);
				node = filterNode;
				unflatFilter = downstream;
				//Dummy work estimate for now
				workEstimation.put(fissedContent, 
						   new Integer(workEstimate/times));
			    }
			} else if (!(downstream.filter instanceof SIRPredefinedFilter)) {
			    FilterTraceNode filterNode = new FilterTraceNode(dsContent);
			    node.setNext(filterNode);
			    filterNode.setPrevious(node);
			    node = filterNode;
			    unflatFilter = downstream;
			}
		    }
		    else
			if (!(downstream.filter instanceof SIRPredefinedFilter)) {
			    FilterTraceNode filterNode = new FilterTraceNode(dsContent);
			    node.setNext(filterNode);
			    filterNode.setPrevious(node);
			    node = filterNode;
			    unflatFilter = downstream;
			}
		}
		
		traceBNWork.put(trace, new Integer(bottleNeckWork));

		//we are finished the current trace, create the outputtracenode
		if (unflatFilter.out != null && unflatFilter.out.length > 0) {
		    Edge [][] outEdges = new Edge[unflatFilter.out.length][];
		    OutputTraceNode outNode = new OutputTraceNode(unflatFilter.outWeights, outEdges);
		    node.setNext(outNode);
		    outNode.setPrevious(node);
		    for (int i = 0; i < unflatFilter.out.length; i++) {
			UnflatEdge[] inner = unflatFilter.out[i];
			Edge[] innerEdges = new Edge[inner.length];
			outEdges[i] = innerEdges;
			for (int j = 0; j < inner.length; j++) {
			    UnflatEdge unflatEdge = inner[j];
			    UnflatFilter dest = unflatEdge.dest;
			    //if we didn't visit one of the dests, add it
			    if (!visited.contains(dest))
				queue.add(dest);
			    Edge edge = (Edge)edges.get(unflatEdge);
			    if (edge == null) {
				edge = new Edge(outNode);
				edges.put(unflatEdge, edge);
			    }
			    else 
				edge.setSrc(outNode);
			    innerEdges[j] = edge;
			}
		    }
		}
		trace.finish();
	    }
	}
	
	traceGraph = new Trace[traces.size()];
	traces.toArray(traceGraph);
	topTracesList.toArray(topTraces);
	setupIO();
	return traceGraph;
    }

    private void setupIO() 
    {
	int len=traceGraph.length;
	int newLen=len;
	for(int i=0;i<len;i++)
	    if(((FilterTraceNode)traceGraph[i].getHead().getNext()).isPredefined())
		newLen--;	
		io=new Trace[len-newLen];
	int idx=0;
	for(int i=0;i<len;i++) {
	    Trace trace=traceGraph[i];
	    if(((FilterTraceNode)trace.getHead().getNext()).isPredefined())
		io[idx++]=trace;
	}
    }
    

    //given <unflatFilter> determine if we should continue the current race we are
    //building
    private boolean continueTrace(UnflatFilter unflatFilter, boolean isLinear, 
				  int bottleNeckWork, int newTotalFilters) 
    {
	//if this is not connected to anything or 
	//it is connected to more than one filter or one filter it is 
	//connected to is joining multiple filters
	if (unflatFilter.out != null && unflatFilter.out.length == 1 && 
	    unflatFilter.out[0].length == 1 && unflatFilter.out[0][0].dest.in.length < 2) {
	    //this is the only dest
	    UnflatFilter dest = unflatFilter.out[0][0].dest;
	    //put file readers and writers in there own trace, so only keep going for 
	    //none-predefined nodes
	    if (unflatFilter.filter instanceof SIRPredefinedFilter)
		return false;
	    //don't continue if the next filter is predefined
	    if (dest.filter instanceof SIRPredefinedFilter)
		return false;
	    //cut out linear filters
	    if (isLinear||dest.isLinear())
		return false;

	    //check the size of the trace, the length must be less than number of tiles + 1 
	    if (newTotalFilters > rawChip.getTotalTiles())
		return false;

	    //check the work estimation
	    int destEst = getWorkEstimate(dest);
	    double ratio = (bottleNeckWork > destEst) ? (double)destEst / (double)bottleNeckWork :
		(double) bottleNeckWork / (double) destEst;
	    ratio = Math.abs(ratio);
	    //System.out.println("bottleNeckWork = " + bottleNeckWork + " / " + 
	    //		       "next = " + destEst + " = " + ratio);
	    if (ratio < TRASHOLD) 
		return false;
	    
	    //everything passed 
	    return true;
	}
	
	return false;
    }
    

    private FilterContent getFilterContent(UnflatFilter f) 
    {
	FilterContent content;
	
	if(f.filter instanceof SIRFileReader)
	    content=new FileInputContent(f);
	else if(f.filter instanceof SIRFileWriter)
	    content=new FileOutputContent(f);
	else
	    content=new FilterContent(f);
	return content;
    }
    

    //get the work estimation for a filter and multiple it by the
    //number of times a filter executes in the steady-state
    //return 0 for linear filters or predefined filters
    private int getWorkEstimate(UnflatFilter unflat) 
    {
	if (unflat.isLinear())
	    //return 0;
	    return unflat.array.length*2;
	return getWorkEstimate(unflat.filter);
    }

    private int getWorkEstimate(SIRFilter filter)
    {
	if (filter instanceof SIRPredefinedFilter)
	    return 0;
	assert work.getReps(filter) == ((int[])exeCounts[1].get(filter))[0] :
	    "Multiplicity for work estimation does not match schedule of flat graph";
	return work.getWork(filter);
    }

    private int getWorkEstimate(FilterContent fc) 
    {
	assert workEstimation.containsKey(fc);
	return ((Integer)workEstimation.get(fc)).intValue();
    }
    

    //dump the the completed partition to a dot file
    public void dumpGraph(String filename)
    {
	StringBuffer buf=new StringBuffer();
	buf.append("digraph Flattend {\n");
	buf.append("size = \"8, 10.5\";\n");

	for(int i = 0;i < traceGraph.length; i++) {
	    Trace trace = traceGraph[i];
	    assert trace != null;
	    buf.append(trace.hashCode()+" [ "+traceName(trace)+"\" ];\n");
	    Trace[] next = getNext(trace/*,parent*/);
	    for(int j=0;j<next.length;j++) {
		assert next[j] != null;
		buf.append(trace.hashCode() + " -> " + next[j].hashCode() + ";\n");
	    }
	}

	buf.append("}\n");
	//write the file
	try {
	    FileWriter fw = new FileWriter(filename);
	    fw.write(buf.toString());
	    fw.close();
	}
	catch (Exception e) {
	    System.err.println("Could not print extracted traces");
	}
    }

    //get the downstream traces we cannot use the edge[] of trace
    //because it is for execution order and this is not determined yet.
    private Trace[] getNext(Trace trace)
    {
	TraceNode node = trace.getHead();
	if(node instanceof InputTraceNode)
	    node = node.getNext();
	while(node!=null&&node instanceof FilterTraceNode) {
	    node=node.getNext();
	}
	if(node instanceof OutputTraceNode) {
	    Edge[][] dests=((OutputTraceNode)node).getDests();
	    ArrayList output=new ArrayList();
	    for(int i=0;i<dests.length;i++) {
		Edge[] inner=dests[i];
		for(int j=0;j<inner.length;j++) {
		    //Object next=parent.get(inner[j]);
		    Object next=inner[j].getDest().getParent();
		    if(!output.contains(next))
			output.add(next);
		}
	    }
	    Trace[] out=new Trace[output.size()];
	    output.toArray(out);
	    return out;
	}
	return new Trace[0];
    }

    //return a string with all of the names of the filtertracenodes
    //and blue if linear
    private String traceName(Trace trace) {
	TraceNode node=trace.getHead().getNext();
	
	StringBuffer out=null;
	
	if(((FilterTraceNode)node).getFilter().getArray()!=null)
	    out=new StringBuffer("color=cornflowerblue, style=filled, label=\""+node.toString());
	else
	    out=new StringBuffer("label=\""+node.toString());
	out.append("{" + getWorkEstimate(((FilterTraceNode)node).getFilter())+ "}");
	node=node.getNext();
	int i = 0;
	while(node!=null&&node instanceof FilterTraceNode) {
	    //too many filters, just break
	    if (i++ > 15)
		break;
	    out.append("\\n"+node.toString() + "{" + 
		       getWorkEstimate(((FilterTraceNode)node).getFilter()) + "}");
	    node=node.getNext();
	}
	return out.toString();
    }

 
    
}

