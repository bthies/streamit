package at.dms.kjc.spacetime;

import java.util.*;
import java.io.FileWriter;
import at.dms.kjc.sir.*;
import at.dms.util.Utils;
import at.dms.kjc.flatgraph2.*;
import at.dms.kjc.sir.linear.LinearAnalyzer;

public class TraceExtractor {
    public static Trace[] extractTraces(UnflatFilter[] topFilters,HashMap[] execCounts,LinearAnalyzer lfa) {
	LinkedList Q=new LinkedList();
	HashMap visited=new HashMap();
	LinkedList traces=new LinkedList();
	//HashMap outNodes=new HashMap();
	//HashMap inNodes=new HashMap();
	HashMap edges=new HashMap(); // UnflatEdge -> Edge
	for(int i=0;i<topFilters.length;i++)
	    Q.add(topFilters[i]);
	while(Q.size()>0) {
	    UnflatFilter filter=(UnflatFilter)Q.removeFirst();
	    FilterContent content;
	    if(filter.filter instanceof SIRFileReader)
		content=new FileInputContent(filter);
	    else if(filter.filter instanceof SIRFileWriter)
		content=new FileOutputContent(filter);
	    else
		content=new FilterContent(filter);
	    boolean linear=content.getArray()!=null;
	    /*if(content.isLinear()) {
	      FilterContent[] linearStuff=LinearFission.fiss(content,content.getArray().length);
	      for(int i=0;i<linearStuff.length;i++)
	      System.out.println("Linear: "+linearStuff[i].getArray().length+" "+linearStuff[i].getArray()[0]);
	      for(int i=0;i<linearStuff.length;i++) {
	      FilterTraceNode filterNode=new FilterTraceNode(linearStuff[i]);
	      node.setNext(filterNode);
	      filterNode.setPrevious(node);
	      node=filterNode;
	      filter=newFilter;
	      }
	      }*/
	    TraceNode node;
	    Trace trace;
	    if(!visited.containsKey(filter)) {
		//System.err.println("Visiting: "+filter.filter.getClass().getName());
		visited.put(filter,null);
		if(filter.in!=null&&filter.in.length>0) {
		    //node=new InputTraceNode(filter.inWeights,getInNodes(outNodes,filter.in));
		    //node=getInNode(outNodes,inNodes,filter);
		    UnflatEdge[] unflatEdges=filter.in;
		    Edge[] inEdges=new Edge[unflatEdges.length];
		    node=new InputTraceNode(filter.inWeights,inEdges);
		    for(int i=0;i<unflatEdges.length;i++) {
			UnflatEdge unflatEdge=unflatEdges[i];
			Edge edge=(Edge)edges.get(unflatEdge);
			//assert edge!=null:"Edge Null "+filter;
			if(edge==null) {
			    edge=new Edge((InputTraceNode)node);
			    edges.put(unflatEdge,edge);
			} else
			    edge.setDest((InputTraceNode)node);
			inEdges[i]=edge;
		    }
		    trace=new Trace((InputTraceNode)node);
		    if(content.isLinear()) {
			FilterContent[] linearStuff=LinearFission.fiss(content,content.getArray().length);
			//for(int i=0;i<linearStuff.length;i++)
			//System.out.println("Linear: "+linearStuff[i].getArray().length+" "+linearStuff[i].getArray()[0]);
			for(int i=0;i<linearStuff.length;i++) {
			    FilterTraceNode filterNode=new FilterTraceNode(linearStuff[i]);
			    node.setNext(filterNode);
			    filterNode.setPrevious(node);
			    node=filterNode;
			    //filter=newFilter;;
			}
		    } else {
			FilterTraceNode filterNode=new FilterTraceNode(content);
			node.setNext(filterNode);
			filterNode.setPrevious(node);
			node=filterNode;
		    }
		} else {
		    node=new FilterTraceNode(content);
		    trace=new Trace(node);
		}
		//if(!(filter.filter instanceof SIRPredefinedFilter)) //Don't map FileReader/Writer
		traces.add(trace);
		boolean cont=true;
		while(cont&&filter.out!=null&&filter.out.length==1&&filter.out[0].length==1&&filter.out[0][0].dest.in.length<2) {
		    UnflatFilter newFilter=filter.out[0][0].dest;
		    if(newFilter.filter instanceof SIRFileReader)
			content=new FileInputContent(newFilter);
		    else if(newFilter.filter instanceof SIRFileWriter)
			content=new FileOutputContent(newFilter);
		    else
			content=new FilterContent(newFilter);
		    //System.out.println("IS LINEAR: "+linear);
		    if(!(filter.filter instanceof SIRPredefinedFilter)) {
			//System.out.println("CONTENT: "+newFilter+" "+content.isLinear());
			if(content.isLinear()) {
			    FilterContent[] linearStuff=LinearFission.fiss(content,content.getArray().length);
			    //for(int i=0;i<linearStuff.length;i++)
			    //System.out.println("Linear: "+linearStuff[i].getArray().length+" "+linearStuff[i].getArray()[0]);
			    for(int i=0;i<linearStuff.length;i++) {
				FilterTraceNode filterNode=new FilterTraceNode(linearStuff[i]);
				node.setNext(filterNode);
				filterNode.setPrevious(node);
				node=filterNode;
				filter=newFilter;
			    }
			} else if(!(newFilter.filter instanceof SIRPredefinedFilter)) {
			    //if((content.getArray()!=null)==linear) {
			    
			    //if(content.isLinear()) {
			    
			    //} else {
			    FilterTraceNode filterNode=new FilterTraceNode(content);
			    node.setNext(filterNode);
			    filterNode.setPrevious(node);
			    node=filterNode;
			    //}
			    filter=newFilter;
			} else
			    cont=false;
		    } else
			cont=false;
		}
		if(filter.out!=null&&filter.out.length>0) {
		    OutputTraceNode outNode=null;
		    //if(filter.filter instanceof SIRFileReader) {
		    //SIRFileReader read=(SIRFileReader)filter.filter;
		    //outNode=new EnterTraceNode(read.getFileName(),true,filter.outWeights);
		    //} else
		    Edge[][] outEdges=new Edge[filter.out.length][];
		    outNode=new OutputTraceNode(filter.outWeights,outEdges);
		    node.setNext(outNode);
		    outNode.setPrevious(node);
		    //outNodes.put(filter,outNode);
		    for(int i=0;i<filter.out.length;i++) {
			UnflatEdge[] inner=filter.out[i];
			Edge[] innerEdges=new Edge[inner.length];
			outEdges[i]=innerEdges;
			for(int j=0;j<inner.length;j++) {
			    UnflatEdge unflatEdge=inner[j];
			    UnflatFilter dest=unflatEdge.dest;
			    if(!visited.containsKey(dest))
				Q.add(dest);
			    Edge edge=(Edge)edges.get(unflatEdge);
			    if(edge==null) {
				edge=new Edge(outNode);
				edges.put(unflatEdge,edge);
			    } else
				edge.setSrc(outNode);
			    innerEdges[j]=edge;
			}
		    }
		}
		trace.finish();
	    }
	}
	//Fix Output Nodes
	/*Set outFilters=outNodes.keySet();
	  UnflatFilter[] outArray=new UnflatFilter[outFilters.size()];
	  outFilters.toArray(outArray);
	  for(int i=0;i<outArray.length;i++) {
	  UnflatFilter outFilter=outArray[i];
	  OutputTraceNode outNode=(OutputTraceNode)outNodes.get(outFilter);
	  Edge[][] dests=new Edge[outFilter.out.length][0];
	  for(int j=0;j<dests.length;j++) {
	  UnflatEdge[] destFilters=outFilter.out[j];
	  Edge[] subDests=new Edge[destFilters.length];
	  for(int k=0;k<destFilters.length;k++) {
	  subDests[k]=new Edge(outNode,(InputTraceNode)inNodes.get(destFilters[k].dest));
	  }
	  dests[j]=subDests;
	  }
	  outNode.setDests(dests);
	  }*/
	Trace[] out=new Trace[traces.size()];
	traces.toArray(out);
	return out;
    }
    
    /*private static InputTraceNode getInNode(HashMap outNodes,HashMap inNodes,UnflatFilter filter) {
      UnflatEdge[] in=filter.in;
      OutputTraceNode[] inNode=new OutputTraceNode[in.length];
      for(int i=0;i<in.length;i++)
      inNode[i]=(OutputTraceNode)outNodes.get(in[i].src);
      InputTraceNode output=null;
      //if(filter.filter instanceof SIRFileWriter) {
      //SIRFileWriter write=(SIRFileWriter)filter.filter;
      //output=new ExitTraceNode(write.getFileName(),true,filter.inWeights,inNode);
      //} else
      output=new InputTraceNode(filter.inWeights,inNode);
      inNodes.put(filter,output);
      return output;
      }*/

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
	//HashMap parent=new HashMap(); // TraceNode -> Trace
	//for(int i=0;i<traces.length;i++)
	//parent.put(traces[i].getHead(),traces[i]);
	for(int i=0;i<traces.length;i++) {
	    Trace trace=traces[i];
	    buf.append(trace.hashCode()+" [ "+traceName(trace)+"\" ];\n");
	    Trace[] next=getNext(trace/*,parent*/);
	    for(int j=0;j<next.length;j++)
		buf.append(hashName(trace)+" -> "+hashName(next[j])+";\n");
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

    private static int hashName(Trace trace) {
	if(trace==null)
	    return -1;
	return trace.hashCode();
    }

    private static String traceName(Trace trace) {
	TraceNode node=trace.getHead().getNext();
	//if(node instanceof InputTraceNode)
	//node=node.getNext();
	StringBuffer out=null;
	//System.out.println(node);
	//System.out.println(node.getPrevious());
	//System.out.println(node.getNext());
	if(((FilterTraceNode)node).getFilter().getArray()!=null)
	    out=new StringBuffer("color=cornflowerblue, style=filled, label=\""+node.toString());
	else
	    out=new StringBuffer("label=\""+node.toString());
	node=node.getNext();
	while(node!=null&&node instanceof FilterTraceNode) {
	    out.append("\\n"+node.toString());
	    node=node.getNext();
	}
	return out.toString();
    }

    private static Trace[] getNext(Trace trace/*,HashMap parent*/) {
	TraceNode node=trace.getHead();
	if(node instanceof InputTraceNode)
	    node=node.getNext();
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
}









