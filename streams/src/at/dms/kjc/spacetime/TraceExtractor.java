package at.dms.kjc.spacetime;

import java.util.*;
import java.io.FileWriter;
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
		//System.err.println("Visiting: "+filter);
		visited.put(filter,null);
		if(filter.in!=null&&filter.in.length>0) {
		    //node=new InputTraceNode(filter.inWeights,getInNodes(outNodes,filter.in));
		    node=getInNode(outNodes,inNodes,filter);
		    trace=new Trace(node);
		    FilterTraceNode filterNode=new FilterTraceNode(content);
		    node.setNext(filterNode);
		    filterNode.setPrevious(node);
		    node=filterNode;
		} else {
		    node=new FilterTraceNode(content);
		    trace=new Trace(node);
		}
		traces.add(trace);
		while(filter.out!=null&&filter.out.length==1&&filter.out[0].length==1&&filter.out[0][0].dest.in.length<2) {
		    //System.err.println("Filter: "+filter);
		    filter=filter.out[0][0].dest;
		    //UnflatFilter dest=filter.out[0][0].dest;
		    //if(dest.in.length<2) {
		    //filter=dest;
		    content=new FilterContent(filter.filter,execCounts);
		    FilterTraceNode filterNode=new FilterTraceNode(content);
		    node.setNext(filterNode);
		    filterNode.setPrevious(node);
		    node=filterNode;
		    //}
		}
		if(filter.out!=null&&filter.out.length>0) {
		    OutputTraceNode outNode=new OutputTraceNode(filter.outWeights);
		    node.setNext(outNode);
		    outNode.setPrevious(node);
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
		trace.finish();
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
		for(int k=0;k<destFilters.length;k++) {
		    subDests[k]=(InputTraceNode)inNodes.get(destFilters[k].dest);
		}
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

    public static void dumpGraph(Trace[] traces,String filename) {
	StringBuffer buf=new StringBuffer();
	buf.append("digraph Flattend {\n");
	buf.append("size = \"8, 10.5\";\n");
	HashMap parent=new HashMap();
	for(int i=0;i<traces.length;i++)
	    parent.put(traces[i].getHead(),traces[i]);
	for(int i=0;i<traces.length;i++) {
	    Trace trace=traces[i];
	    buf.append(trace.hashCode()+" [ label = \""+traceName(trace)+"\" ];\n");
	    Trace[] next=getNext(trace,parent);
	    for(int j=0;j<next.length;j++)
		buf.append(trace.hashCode()+" -> "+next[j].hashCode()+";\n");
	}
	buf.append("}\n");
	try {
	    FileWriter fw = new FileWriter(filename);
	    fw.write(buf.toString());
	    fw.close();
	}
	catch (Exception e) {
	    System.err.println("Could not print extracted traces");
	}
    }

    private static String traceName(Trace trace) {
	TraceNode node=trace.getHead();
	if(node instanceof InputTraceNode)
	    node=node.getNext();
	StringBuffer out=new StringBuffer(node.toString());
	node=node.getNext();
	while(node!=null&&node instanceof FilterTraceNode) {
	    out.append("\\n"+node.toString());
	    node=node.getNext();
	}
	return out.toString();
    }

    private static Trace[] getNext(Trace trace,HashMap parent) {
	TraceNode node=trace.getHead();
	if(node instanceof InputTraceNode)
	    node=node.getNext();
	while(node!=null&&node instanceof FilterTraceNode) {
	    node=node.getNext();
	}
	if(node instanceof OutputTraceNode) {
	    InputTraceNode[][] dests=((OutputTraceNode)node).getDests();
	    ArrayList output=new ArrayList();
	    for(int i=0;i<dests.length;i++) {
		InputTraceNode[] inner=dests[i];
		for(int j=0;j<inner.length;j++) {
		    Object next=parent.get(inner[j]);
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
}





