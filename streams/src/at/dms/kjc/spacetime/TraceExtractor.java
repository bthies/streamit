package at.dms.kjc.spacetime;

import java.util.*;
//import java.io.FileWriter;
import at.dms.kjc.sir.*;
import at.dms.util.Utils;
import at.dms.kjc.flatgraph2.*;

public class TraceExtractor {
    public static Trace[] extractTraces(UnflatFilter[] topFilters,HashMap[] execCounts) {
	LinkedList Q=new LinkedList();
	HashMap visited=new HashMap();
	LinkedList traces=new LinkedList();
	HashMap outNodes=new HashMap();
	HashMap inNodes=new HashMap();
	for(int i=0;i<topFilters.length;i++)
	    Q.add(topFilters[i]);
	while(Q.size()>0) {
	    UnflatFilter filter=(UnflatFilter)Q.removeFirst();
	    FilterContent content=new FilterContent(filter.filter,execCounts);
	    TraceNode node;
	    Trace trace;
	    if(!visited.containsKey(filter)) {
		System.err.println("Visiting: "+filter);
		visited.put(filter,null);
		if(filter.in!=null&&filter.in.length>0) {
		    //node=new InputTraceNode(filter.inWeights,getInNodes(outNodes,filter.in));
		    node=getInNode(outNodes,inNodes,filter);
		    trace=new Trace(node);
		    FilterTraceNode filterNode=new FilterTraceNode(content);
		    node.setNext(filterNode);
		    node=filterNode;
		} else {
		    node=new FilterTraceNode(content);
		    trace=new Trace(node);
		}
		traces.add(trace);
		while(filter.out!=null&&filter.out.length==1&&filter.out[0].length==1) {
		    //System.err.println("Filter: "+filter);
		    filter=filter.out[0][0].dest;
		    content=new FilterContent(filter.filter,execCounts);
		    FilterTraceNode filterNode=new FilterTraceNode(content);
		    node.setNext(filterNode);
		    node=filterNode;
		}
		if(filter.out!=null&&filter.out.length>0) {
		    OutputTraceNode outNode=new OutputTraceNode(filter.outWeights);
		    outNodes.put(filter,outNode);
		    for(int i=0;i<filter.out.length;i++) {
			UnflatEdge[] inner=filter.out[i];
			for(int j=0;j<inner.length;j++) {
			    UnflatFilter dest=inner[j].dest;
			    if(!visited.containsKey(dest))
				Q.add(dest);
			}
		    }
		}
	    }
	}
	//Fix Output Nodes
	Set outFilters=outNodes.keySet();
	UnflatFilter[] outArray=new UnflatFilter[outFilters.size()];
	outFilters.toArray(outArray);
	for(int i=0;i<outArray.length;i++) {
	    UnflatFilter outFilter=outArray[i];
	    OutputTraceNode outNode=(OutputTraceNode)outNodes.get(outFilter);
	    InputTraceNode[][] dests=new InputTraceNode[outFilter.out.length][0];
	    for(int j=0;j<dests.length;j++) {
		UnflatEdge[] destFilters=outFilter.out[j];
		InputTraceNode[] subDests=new InputTraceNode[destFilters.length];
		for(int k=0;k<destFilters.length;k++)
		    subDests[k]=(InputTraceNode)inNodes.get(destFilters[k]);
		dests[j]=subDests;
	    }
	    outNode.setDests(dests);
	}
	Trace[] out=new Trace[traces.size()];
	traces.toArray(out);
	return out;
    }
    
    private static InputTraceNode getInNode(HashMap outNodes,HashMap inNodes,UnflatFilter filter) {
	UnflatEdge[] in=filter.in;
	OutputTraceNode[] inNode=new OutputTraceNode[in.length];
	for(int i=0;i<in.length;i++)
	    inNode[i]=(OutputTraceNode)outNodes.get(in[i].src);
	InputTraceNode output=new InputTraceNode(filter.inWeights,inNode);
	inNodes.put(filter,output);
	return output;
    }

    /*private static OutputTraceNode[] getInNodes(HashMap outNodes,UnflatEdge[] in) {
      OutputTraceNode[] output=new OutputTraceNode[in.length];
      for(int i=0;i<in.length;i++)
      output[i]=(OutputTraceNode)outNodes.get(in[i].src);
      return output;
      }*/
}



