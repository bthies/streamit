package at.dms.kjc.spacetime;

import java.util.*;
import java.io.FileWriter;
import at.dms.kjc.sir.*;
import at.dms.util.Utils;
import at.dms.kjc.slicegraph.*;
import at.dms.kjc.sir.linear.LinearAnalyzer;
import at.dms.kjc.slicegraph.InterSliceEdge;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.OutputSliceNode;
import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.slicegraph.SliceNode;

public class TraceExtractor {
    public static Slice[] extractTraces(UnflatFilter[] topFilters,HashMap[] execCounts,LinearAnalyzer lfa) {
        LinkedList<UnflatFilter> Q=new LinkedList<UnflatFilter>();
        HashMap visited=new HashMap();
        LinkedList<Slice> slices=new LinkedList<Slice>();
        //HashMap outNodes=new HashMap();
        //HashMap inNodes=new HashMap();
        HashMap<UnflatEdge, InterSliceEdge> edges=new HashMap<UnflatEdge, InterSliceEdge>(); // UnflatEdge -> Edge
        for(int i=0;i<topFilters.length;i++)
            Q.add(topFilters[i]);
        while(Q.size()>0) {
            UnflatFilter filter=Q.removeFirst();
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
            SliceNode node;
            Slice slice;
            if(!visited.containsKey(filter)) {
                //System.err.println("Visiting: "+filter.filter.getClass().getName());
                visited.put(filter,null);
                if(filter.in!=null&&filter.in.length>0) {
                    //node=new InputTraceNode(filter.inWeights,getInNodes(outNodes,filter.in));
                    //node=getInNode(outNodes,inNodes,filter);
                    UnflatEdge[] unflatEdges=filter.in;
                    InterSliceEdge[] inEdges=new InterSliceEdge[unflatEdges.length];
                    node=new InputSliceNode(filter.inWeights,inEdges);
                    for(int i=0;i<unflatEdges.length;i++) {
                        UnflatEdge unflatEdge=unflatEdges[i];
                        InterSliceEdge edge=edges.get(unflatEdge);
                        //assert edge!=null:"Edge Null "+filter;
                        if(edge==null) {
                            edge=new InterSliceEdge((InputSliceNode)node);
                            edges.put(unflatEdge,edge);
                        } else
                            edge.setDest((InputSliceNode)node);
                        inEdges[i]=edge;
                    }
                    slice=new Slice((InputSliceNode)node);
                    if(content.isLinear()) {
                        FilterContent[] linearStuff=LinearFission.fiss(content,content.getArray().length);
                        //for(int i=0;i<linearStuff.length;i++)
                        //System.out.println("Linear: "+linearStuff[i].getArray().length+" "+linearStuff[i].getArray()[0]);
                        for(int i=0;i<linearStuff.length;i++) {
                            FilterSliceNode filterNode=new FilterSliceNode(linearStuff[i]);
                            node.setNext(filterNode);
                            filterNode.setPrevious(node);
                            node=filterNode;
                            //filter=newFilter;;
                        }
                    } else {
                        FilterSliceNode filterNode=new FilterSliceNode(content);
                        node.setNext(filterNode);
                        filterNode.setPrevious(node);
                        node=filterNode;
                    }
                } else {
                    node=new FilterSliceNode(content);
                    slice=new Slice(node);
                }
                //if(!(filter.filter instanceof SIRPredefinedFilter)) //Don't map FileReader/Writer
                slices.add(slice);
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
                                FilterSliceNode filterNode=new FilterSliceNode(linearStuff[i]);
                                node.setNext(filterNode);
                                filterNode.setPrevious(node);
                                node=filterNode;
                                filter=newFilter;
                            }
                        } else if(!(newFilter.filter instanceof SIRPredefinedFilter)) {
                            //if((content.getArray()!=null)==linear) {
                
                            //if(content.isLinear()) {
                
                            //} else {
                            FilterSliceNode filterNode=new FilterSliceNode(content);
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
                    OutputSliceNode outNode=null;
                    //if(filter.filter instanceof SIRFileReader) {
                    //SIRFileReader read=(SIRFileReader)filter.filter;
                    //outNode=new EnterTraceNode(read.getFileName(),true,filter.outWeights);
                    //} else
                    InterSliceEdge[][] outEdges=new InterSliceEdge[filter.out.length][];
                    outNode=new OutputSliceNode(filter.outWeights,outEdges);
                    node.setNext(outNode);
                    outNode.setPrevious(node);
                    //outNodes.put(filter,outNode);
                    for(int i=0;i<filter.out.length;i++) {
                        UnflatEdge[] inner=filter.out[i];
                        InterSliceEdge[] innerEdges=new InterSliceEdge[inner.length];
                        outEdges[i]=innerEdges;
                        for(int j=0;j<inner.length;j++) {
                            UnflatEdge unflatEdge=inner[j];
                            UnflatFilter dest=unflatEdge.dest;
                            if(!visited.containsKey(dest))
                                Q.add(dest);
                            InterSliceEdge edge=edges.get(unflatEdge);
                            if(edge==null) {
                                edge=new InterSliceEdge(outNode);
                                edges.put(unflatEdge,edge);
                            } else
                                edge.setSrc(outNode);
                            innerEdges[j]=edge;
                        }
                    }
                }
                slice.finish();
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
        Slice[] out=new Slice[slices.size()];
        slices.toArray(out);
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

    public static void dumpGraph(Slice[] traces,String filename) {
        StringBuffer buf=new StringBuffer();
        buf.append("digraph Flattend {\n");
        buf.append("size = \"8, 10.5\";\n");
        //HashMap parent=new HashMap(); // TraceNode -> Slice
        //for(int i=0;i<traces.length;i++)
        //parent.put(traces[i].getHead(),traces[i]);
        for(int i=0;i<traces.length;i++) {
            Slice slice=traces[i];
            buf.append(slice.hashCode()+" [ "+traceName(slice)+"\" ];\n");
            Slice[] next=getNext(slice/*,parent*/);
            for(int j=0;j<next.length;j++)
                buf.append(hashName(slice)+" -> "+hashName(next[j])+";\n");
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

    private static int hashName(Slice slice) {
        if(slice==null)
            return -1;
        return slice.hashCode();
    }

    private static String traceName(Slice slice) {
        SliceNode node=slice.getHead().getNext();
        //if(node instanceof InputTraceNode)
        //node=node.getNext();
        StringBuffer out=null;
        //System.out.println(node);
        //System.out.println(node.getPrevious());
        //System.out.println(node.getNext());
        if(((FilterSliceNode)node).getFilter().getArray()!=null)
            out=new StringBuffer("color=cornflowerblue, style=filled, label=\""+node.toString());
        else
            out=new StringBuffer("label=\""+node.toString());
        node=node.getNext();
        while(node!=null&&node instanceof FilterSliceNode) {
            out.append("\\n"+node.toString());
            node=node.getNext();
        }
        return out.toString();
    }

    private static Slice[] getNext(Slice slice/*,HashMap parent*/) {
        SliceNode node=slice.getHead();
        if(node instanceof InputSliceNode)
            node=node.getNext();
        while(node!=null&&node instanceof FilterSliceNode) {
            node=node.getNext();
        }
        if(node instanceof OutputSliceNode) {
            Edge[][] dests=((OutputSliceNode)node).getDests();
            ArrayList<Object> output=new ArrayList<Object>();
            for(int i=0;i<dests.length;i++) {
                Edge[] inner=dests[i];
                for(int j=0;j<inner.length;j++) {
                    //Object next=parent.get(inner[j]);
                    Object next=inner[j].getDest().getParent();
                    if(!output.contains(next))
                        output.add(next);
                }
            }
            Slice[] out=new Slice[output.size()];
            output.toArray(out);
            return out;
        }
        return new Slice[0];
    }
}







