package at.dms.kjc.raw;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;
import java.io.*;
import java.util.*;

/**
 * This class flattens the stream graph
 */

public class RawFlattener extends at.dms.util.Utils implements FlatVisitor
{
    private FlatNode currentNode;
    private StringBuffer buf;
    //this hashset stores the splitters of feedbackloops
    //so the edges can be swapped after createGraph()
    //see the note in the create graph algorithm
    private HashSet feedbackSplitters;
    
    public FlatNode top;
    
    private int unique_id;

    //maps sir operators to their corresponding flatnode
    private HashMap SIRMap;

    public static LinkedList needsToBeSched=new LinkedList();;

    /**
     * Creates a new flattener based on <toplevel>
     */
    public RawFlattener(SIROperator toplevel) 
    {
	this.SIRMap = new HashMap();
	feedbackSplitters = new HashSet();
	unique_id = 0;

	//create the flat graph
	createGraph(toplevel);
	//now we need to fix up the graph a bit
	//we need to add all the back edges of the splitter of a feedbacks
	Iterator it = feedbackSplitters.iterator();
	while(it.hasNext()) {
	    ((FlatNode)it.next()).swapSplitterEdges();
	    
	}
	
    }

    /**
     * Returns the number of tiles that would be needed to execute
     * this graph.  That is, just counts the filters, plus any joiners
     * whose output is not connected to another joiner.
     */
    public int getNumTiles() {
	dumpGraph("crash.dot");
	int count = 0;
	for (Iterator it = SIRMap.entrySet().iterator(); it.hasNext(); ) {
	    Map.Entry entry = (Map.Entry)it.next();
	    if (entry.getKey() instanceof SIRFilter  &&
		!(entry.getKey() instanceof SIRIdentity) ) {
		// always count filter
		count++;
	    } else if (entry.getKey() instanceof SIRJoiner) {
		// count a joiner if none of its outgoing edges is to
		// another joiner
		FlatNode[] edges = ((FlatNode)entry.getValue()).edges;
		int increment = 1;
		for (int i=0; i<edges.length; i++) {
		    if (edges[i]!=null &&
			edges[i].contents instanceof SIRJoiner) {
			increment = 0;
		    }
		}
		count += increment;
	    } 
	}
	return count;
    }
    
    private void createGraph(SIROperator current) 
    {
	if (current instanceof SIRFilter) {
	    FlatNode node = addFlatNode(current);
	    if (top == null) {
		currentNode = node;
		top = node;
	    }
	    
	    currentNode.addEdges(node);
	    currentNode = node;
	}
	if (current instanceof SIRPipeline){
	    SIRPipeline pipeline = (SIRPipeline) current;
	    
	    for (int i=0; i<pipeline.size(); i++) {
		createGraph(pipeline.get(i));
	    }
	}
	if (current instanceof SIRSplitJoin) {
	    SIRSplitJoin sj = (SIRSplitJoin) current;
	    FlatNode splitterNode = addFlatNode (sj.getSplitter());
	    if (top == null) {
		currentNode = splitterNode;
		top = splitterNode;
	    }
	    
	    FlatNode joinerNode = addFlatNode (sj.getJoiner());
	    	    
	    currentNode.addEdges(splitterNode);
	    for (int i = 0; i < sj.size(); i++) {
		currentNode = splitterNode;
		createGraph(sj.get(i));
		currentNode.addEdges(joinerNode);
	    }
	    
	    //Save Weight of Joiner
	    int sumWeights=0;
	    int[] oldWeights=joinerNode.incomingWeights;
	    for(int i=0;i<oldWeights.length;i++)
		sumWeights+=oldWeights[i];
	    ((SIRJoiner)joinerNode.contents).oldSumWeights=sumWeights;
	    
	    if(StreamItOptions.sync) {
		//Coalesce Splitters
		//And Joiner-Splitter destruction
		if((((SIRSplitter)splitterNode.contents).getType()==SIRSplitType.ROUND_ROBIN)||
		   (((SIRSplitter)splitterNode.contents).getType()==SIRSplitType.WEIGHTED_RR)) {
		    FlatNode adjacentJoin=splitterNode.incoming[0];
		    int[] offsetArray=new int[splitterNode.edges.length];
		    int[] remainderArray=new int[splitterNode.edges.length];
		    LinkedList newWeights=new LinkedList();
		    LinkedList newEdges=new LinkedList();
		    boolean loop=false;
		    for(int i=0;i<splitterNode.edges.length;i++) {
			FlatNode childNode=splitterNode.edges[i];
			if((childNode.contents instanceof SIRSplitter)&&
			   ((((SIRSplitter)childNode.contents).getType()==SIRSplitType.ROUND_ROBIN)||
			    (((SIRSplitter)childNode.contents).getType()==SIRSplitType.WEIGHTED_RR))) {
			    int off=0;
			    int sum=0;
			    int target=splitterNode.weights[i];
			    while(sum<target) {
				int oldSum=sum;
				sum+=childNode.weights[off];
				if(sum<=splitterNode.weights[i]) {
				    newWeights.add(new Integer(childNode.weights[off]));
				    newEdges.add(childNode.edges[off]);
				} else {
				    loop=true;
				    int fit=splitterNode.weights[i]-oldSum;
				    newWeights.add(new Integer(fit));
				    newEdges.add(childNode.edges[off]);
				    remainderArray[i]=childNode.weights[off]-fit;
				    break;
				}
				if((++off)==childNode.weights.length)
				    off=0;
			    }
			    if(off!=0)
				loop=true;
			    offsetArray[i]=off;
			} else {
			    newWeights.add(new Integer(splitterNode.weights[i]));
			    newEdges.add(childNode);
			    offsetArray[i]=-1; //Ignore this edge in analysis
			}
		    }
		    while(loop) {
			loop=false;
			for(int i=0;i<offsetArray.length;i++) {
			    int sum=0;
			    int target=splitterNode.weights[i];
			    FlatNode childNode=splitterNode.edges[i];
			    if(offsetArray[i]<0) {
				newWeights.add(new Integer(target));
				newEdges.add(childNode);
				continue;
			    }
			    int off=offsetArray[i];
			    int rem=remainderArray[i];
			    if(rem>0)
				if(rem<=target) {
				    sum+=rem;
				    newWeights.add(new Integer(rem));
				    newEdges.add(childNode.edges[off]);
				    if((++off)==childNode.weights.length)
					off=0;
				} else {
				    loop=true;
				    newWeights.add(new Integer(target));
				    newEdges.add(childNode.edges[off]);
				    remainderArray[i]=rem-target;
				    continue;
				}
			    while(sum<target) {
				int oldSum=sum;
				sum+=childNode.weights[off];
				if(sum<=target) {
				    newWeights.add(new Integer(childNode.weights[off]));
				    newEdges.add(childNode.edges[off]);
				    remainderArray[i]=0;
				} else {
				    loop=true;
				    int fit=target-oldSum;
				    newWeights.add(new Integer(fit));
				    newEdges.add(childNode.edges[off]);
				    remainderArray[i]=childNode.weights[off]-fit;
				    break;
				}
				if((++off)==childNode.weights.length)
				    off=0;
			    }
			    //if(off==childNode.weights.length)
			    //off=0;
			    if(off!=0)
				loop=true;
			    offsetArray[i]=off;
			}
		    }
		    //Fix Edges
		    FlatNode[] tempEdges=((FlatNode[])newEdges.toArray(new FlatNode[0]));
		    splitterNode.edges=tempEdges;
		    int[] tempWeights=new int[newWeights.size()];
		    for(int i=0;i<tempWeights.length;i++) {
			tempWeights[i]=((Integer)newWeights.get(i)).intValue();
		    }
		    splitterNode.weights=tempWeights;
		    splitterNode.currentEdge=tempEdges.length;
		    splitterNode.ways=tempEdges.length;
		    for(int i=0;i<tempEdges.length;i++)
			tempEdges[i].incoming[0]=splitterNode;
		
		//Joiner-Splitter Destruction
		    if(adjacentJoin.contents instanceof SIRJoiner) {
			int[] inWeights=adjacentJoin.incomingWeights;
			int[] outWeights=splitterNode.weights;
			FlatNode[] inEdges=adjacentJoin.incoming;
			FlatNode[] outEdges=splitterNode.edges;
			int inSum=0;
			int outSum=0;
			//System.out.print("[ ");
			for(int i=0;i<inWeights.length;i++) {
			    inSum+=inWeights[i];
			    //System.out.print(inWeights[i]+" ");
			}
			//System.out.println("]");
			//System.out.print("[ ");
			for(int i=0;i<outWeights.length;i++) {
			    outSum+=outWeights[i];
			    //System.out.print(outWeights[i]+" ");
			}
			//System.out.println("]");
			int inTimes=1;
			int outTimes=1;
			int inTotal=inSum;
			int outTotal=outSum;
			while(inTotal!=outTotal) {
			    if(inTotal<outTotal) {
				inTotal+=inSum;
				inTimes++;
			    } else {
				outTotal+=outSum;
				outTimes++;
			    }
			}
			int off=0;
			int rem=0;
			LinkedList[] inWeightList=new LinkedList[outEdges.length];
			LinkedList[] inEdgeList=new LinkedList[outEdges.length];
			for(int i=0;i<outEdges.length;i++) {
			    inWeightList[i]=new LinkedList();
			    inEdgeList[i]=new LinkedList();
			}
			HashMap needSplit=new HashMap();
			for(;outTimes>0;outTimes--)
			    for(int i=0;i<outWeights.length;i++) {
				int sum=0;
				int target=outWeights[i];
				FlatNode inEdge=inEdges[off];
				FlatNode ident=new FlatNode(new SIRIdentity(null,"Ident",Util.getOutputType(inEdge)));
				ident.inputs=1;
				ident.ways=1;
				ident.weights=new int[]{1};
				ident.incoming=new FlatNode[]{inEdge};
				inEdge.edges=new FlatNode[]{ident};
				LinkedList currentWeights=inWeightList[i];
				LinkedList currentEdges=inEdgeList[i];
				LinkedList entry=(LinkedList)needSplit.get(inEdge);
				if(rem>0)
				    if(rem>target) {
					rem-=target;
					Integer weight=new Integer(target);
					currentWeights.add(weight);
					currentEdges.add(ident);
					if(entry==null) {
					    entry=new LinkedList();
					    entry.add(weight);
					    entry.add(ident);
					    needSplit.put(inEdge,entry);
					} else {
					    entry.add(weight);
					    entry.add(ident);
					}
					continue;
				    } else {
					sum=rem;
					Integer weight=new Integer(rem);
					rem=0;
					currentWeights.add(weight);
					currentEdges.add(ident);
					if(entry==null) {
					    entry=new LinkedList();
					    entry.add(weight);
					    entry.add(ident);
					    needSplit.put(inEdge,entry);
					} else {
					    entry.add(weight);
					    entry.add(ident);
					}
					if((++off)==inWeights.length)
					    off=0;
				    }
				while(sum<target) {
				    inEdge=inEdges[off];
				    entry=(LinkedList)needSplit.get(inEdge);
				    ident=new FlatNode(new SIRIdentity(null,"Ident",Util.getOutputType(inEdge)));
				    ident.inputs=1;
				    ident.ways=1;
				    ident.weights=new int[]{1};
				    ident.incoming=new FlatNode[]{inEdge};
				    inEdge.edges=new FlatNode[]{ident};
				    int weightInt=inWeights[off];
				    int oldSum=sum;
				    sum+=weightInt;
				    if(sum<=target) {
					Integer weight=new Integer(weightInt);
					currentWeights.add(weight);
					currentEdges.add(ident);
					if(entry==null) {
					    entry=new LinkedList();
					    entry.add(weight);
					    entry.add(ident);
					    needSplit.put(inEdge,entry);
					} else {
					    entry.add(weight);
					    entry.add(ident);
					}
					if((++off)==inWeights.length)
					    off=0;
				    } else {
					Integer weight=new Integer(target-oldSum);
					rem=sum-target;
					currentWeights.add(weight);
					currentEdges.add(ident);
					if(entry==null) {
					    entry=new LinkedList();
					    entry.add(weight);
					    entry.add(ident);
					    needSplit.put(inEdge,entry);
					} else {
					    entry.add(weight);
					    entry.add(ident);
					}
				    }
				}
			    }
			//Fix Edges
			for(int i=0;i<inWeightList.length;i++) {
			    LinkedList curWeights=inWeightList[i];
			    LinkedList curEdges=inEdgeList[i];
			    FlatNode out=outEdges[i];
			    if(curWeights.size()==1) {
				((FlatNode)curEdges.get(0)).edges=new FlatNode[]{out};
				out.incoming[0]=(FlatNode)curEdges.get(0);
			    } else {
				FlatNode newJoin=new FlatNode(SIRJoiner.createWeightedRR(null,new JExpression[0]));
				newJoin.oldContents=adjacentJoin.contents;
				((SIRJoiner)newJoin.contents).oldSumWeights=((SIRJoiner)adjacentJoin.contents).oldSumWeights;
				newJoin.inputs=curWeights.size();
				newJoin.ways=1;
				int[] joinWeights=new int[curWeights.size()];
				int newSum=0;
				for(int j=0;j<joinWeights.length;j++) {
				    int temp=((Integer)curWeights.get(j)).intValue();
				    joinWeights[j]=temp;
				    newSum+=temp;
				}
				newJoin.incomingWeights=joinWeights;
				newJoin.weights=new int[]{1};
				newJoin.incoming=(FlatNode[])curEdges.toArray(new FlatNode[0]);
				newJoin.edges=new FlatNode[]{out};
				out.incoming[0]=newJoin;
				for(int j=0;j<curEdges.size();j++) {
				    ((FlatNode)curEdges.get(j)).edges=new FlatNode[]{newJoin};
				}
				newJoin.schedDivider=inTotal;
				newJoin.schedMult=newSum;
				needsToBeSched.add(newJoin);
			    }
			}
			Iterator iter=needSplit.keySet().iterator();
			while(iter.hasNext()) {
			    FlatNode in=(FlatNode)iter.next();
			    LinkedList list=(LinkedList)needSplit.get(in);
			    if(list.size()==2) {
				((FlatNode)list.get(1)).incoming=new FlatNode[]{inEdges[off]};
			    } else {
				int size=list.size()/2;
				FlatNode dummySplit=new FlatNode(SIRSplitter.createWeightedRR(null,new JExpression[0]));
				dummySplit.inputs=1;
				dummySplit.incoming=new FlatNode[]{in};
				in.edges[0]=dummySplit;
				int[] dummyWeights=new int[size];
				FlatNode[] dummyEdges=new FlatNode[size];
				for(int j=0,offset=0;j<list.size();j+=2,offset++) {
				    dummyWeights[offset]=((Integer)list.get(j)).intValue();
				    dummyEdges[offset]=(FlatNode)list.get(j+1);
				    ((FlatNode)list.get(j+1)).incoming[0]=dummySplit;
				}
				dummySplit.ways=size;
				dummySplit.edges=dummyEdges;
				dummySplit.weights=dummyWeights;
			    }
			}
		    }
		}
	    
		//Coalesce Joiners
		if((((SIRJoiner)joinerNode.contents).getType()==SIRJoinType.ROUND_ROBIN)||
		   (((SIRJoiner)joinerNode.contents).getType()==SIRJoinType.WEIGHTED_RR)) {
		    int[] offsetArray=new int[joinerNode.incoming.length];
		    int[] remainderArray=new int[joinerNode.incoming.length];
		    LinkedList newWeights=new LinkedList();
		    LinkedList newEdges=new LinkedList();
		    boolean loop=false;
		    for(int i=0;i<joinerNode.incoming.length;i++) {
			FlatNode childNode=joinerNode.incoming[i];
			if((childNode.contents instanceof SIRJoiner)&&
			   ((((SIRJoiner)childNode.contents).getType()==SIRJoinType.ROUND_ROBIN)||
			    (((SIRJoiner)childNode.contents).getType()==SIRJoinType.WEIGHTED_RR))) {
			    int off=0;
			    int sum=0;
			    int target=joinerNode.incomingWeights[i];
			    while(sum<target) {
				int oldSum=sum;
				sum+=childNode.incomingWeights[off];
				if(sum<=target) {
				    newWeights.add(new Integer(childNode.incomingWeights[off]));
				    newEdges.add(childNode.incoming[off]);
				} else {
				    loop=true;
				    int fit=joinerNode.incomingWeights[i]-oldSum;
				    newWeights.add(new Integer(fit));
				    newEdges.add(childNode.incoming[off]);
				    remainderArray[i]=childNode.incomingWeights[off]-fit;
				    break;
				}
				if((++off)==childNode.incomingWeights.length)
				    off=0;
			    }
			    if(off!=0)
				loop=true;
			    offsetArray[i]=off;
			} else {
			    newWeights.add(new Integer(joinerNode.incomingWeights[i]));
			    newEdges.add(childNode);
			    offsetArray[i]=-1; //Ignore this edge in analysis
			}
		    }
		    while(loop) {
			loop=false;
			for(int i=0;i<offsetArray.length;i++) {
			    int sum=0;
			    int target=joinerNode.incomingWeights[i];
			    FlatNode childNode=joinerNode.incoming[i];
			    if(offsetArray[i]<0) {
				newWeights.add(new Integer(target));
				newEdges.add(childNode);
				continue;
			    }
			    int off=offsetArray[i];
			    int rem=remainderArray[i];
			    if(rem>0)
				if(rem<=target) {
				    sum+=rem;
				    newWeights.add(new Integer(rem));
				    newEdges.add(childNode.incoming[off]);
				    if((++off)==childNode.incomingWeights.length)
					off=0;
				} else {
				    loop=true;
				    newWeights.add(new Integer(target));
				    newEdges.add(childNode.incoming[off]);
				    remainderArray[i]=rem-target;
				    continue;
				}
			    while(sum<target) {
				int oldSum=sum;
				sum+=childNode.incomingWeights[off];
				if(sum<=target) {
				    newWeights.add(new Integer(childNode.incomingWeights[off]));
				    newEdges.add(childNode.incoming[off]);
				    remainderArray[i]=0;
				} else {
				    loop=true;
				    int fit=target-oldSum;
				    newWeights.add(new Integer(fit));
				    newEdges.add(childNode.incoming[off]);
				    remainderArray[i]=childNode.incomingWeights[off]-fit;
				    break;
				}
				if((++off)==childNode.incomingWeights.length)
				    off=0;
			    }
			    if(off!=0)
				loop=true;
			    offsetArray[i]=off;
			}
		    }
		    //Fix Edges
		    FlatNode[] tempEdges=((FlatNode[])newEdges.toArray(new FlatNode[0]));
		    joinerNode.incoming=tempEdges;
		    int[] tempWeights=new int[newWeights.size()];
		    for(int i=0;i<tempWeights.length;i++) {
			tempWeights[i]=((Integer)newWeights.get(i)).intValue();
		    }
		    joinerNode.incomingWeights=tempWeights;
		    joinerNode.currentIncoming=tempEdges.length;
		    joinerNode.inputs=tempEdges.length;
		    /*ArrayList visited=new ArrayList(tempEdges.length);
		      for(int i=0;i<tempEdges.length;i++) {
		      FlatNode node=tempEdges[i];
		      //node.edges=new FlatNode[]{joinerNode};
		      node.edges[0]=joinerNode;
		      if(visited.contains(node))
		      node.weights[0]+=1;
		      else {
		      visited.add(node);
		      node.weights[0]=1;
		      }
		      }*/
		    HashMap visited=new HashMap();
		    SIRSplitter dummySplit=SIRSplitter.create(null,SIRSplitType.NULL,0);
		    for(int i=0;i<newEdges.size();i++) {
			FlatNode node=(FlatNode)newEdges.get(i);
			LinkedList edges=(LinkedList)visited.get(node);
			if(edges==null) {
			    edges=new LinkedList();
			    FlatNode ident=new FlatNode(new SIRIdentity(null,"Ident",Util.getOutputType(node)));
			    FlatNode split=new FlatNode(SIRSplitter.createWeightedRR(null,new JExpression[0]));
			    node.edges[0]=split;
			    split.inputs=1;
			    split.incoming=new FlatNode[]{node};
			    ident.inputs=1;
			    ident.ways=1;
			    ident.weights=new int[]{1};
			    ident.incoming=new FlatNode[]{split};
			    ident.edges=new FlatNode[]{joinerNode};
			    tempEdges[i]=ident;
			    edges.add(split); //0th elem of edges
			    edges.add(ident); //Then comes ident weight pairs
			    edges.add(newWeights.get(i));
			    visited.put(node,edges);
			} else {
			    FlatNode ident=new FlatNode(new SIRIdentity(null,"Ident",Util.getOutputType(node)));
			    FlatNode split=(FlatNode)edges.get(0);
			    ident.inputs=1;
			    ident.ways=1;
			    ident.weights=new int[]{1};
			    ident.incoming=new FlatNode[]{split};
			    ident.edges=new FlatNode[]{joinerNode};
			    tempEdges[i]=ident;
			    edges.add(ident); //Then comes ident weight pairs
			    edges.add(newWeights.get(i));
			}
		    }
		    Iterator iter=visited.keySet().iterator();
		    while(iter.hasNext()) {
			FlatNode node=(FlatNode)iter.next();
			List list=(List)visited.get(node);
			FlatNode split=(FlatNode)list.get(0);
			int size=(list.size()-1)/2;
			split.edges=new FlatNode[size];
			split.weights=new int[size];
			split.ways=size;
			if(size==1) {
			    FlatNode ident=(FlatNode)list.get(1);
			    node.edges[0]=ident;
			    ident.incoming[0]=node;
			    needsToBeSched.add(ident);
			} else {
			    needsToBeSched.add(split);
			    for(int i=1,off=0;i<list.size();i+=2,off++) {
				split.edges[off]=(FlatNode)list.get(i);
				int weight=((Integer)list.get(i+1)).intValue();
				split.weights[off]=weight;
			    }
			}
		    }
		}
	    }
	    
	    currentNode = joinerNode;	    
	}
	//HACK!!
	//note:  this algorithm incorrectly connects the splitter of a 
	//feedbackloop to the loop before it connects the splitter to the
	//next downstream stream.
	//to fix this, quickly, create a list of the splitters of fbl
	//and swap the edges after the algorithm is finished...nice
	if (current instanceof SIRFeedbackLoop) {
	    SIRFeedbackLoop loop = (SIRFeedbackLoop)current;
	    FlatNode joinerNode = addFlatNode (loop.getJoiner());
	    if (top == null) {
		//currentNode = joinerNode;
		top = joinerNode;
	    }
	    FlatNode splitterNode = addFlatNode (loop.getSplitter());
	    
	    FlatNode.addEdges(currentNode, joinerNode);
	    
	    currentNode = joinerNode;
	    createGraph(loop.getBody());
	    FlatNode.addEdges(currentNode, splitterNode);
	    
	    //here is the hack!
	    swapEdgesLater(splitterNode);
	    
	    currentNode = splitterNode;
	    createGraph(loop.getLoop());
	    FlatNode.addEdges(currentNode, joinerNode);
	    FlatNode ident=new FlatNode(new SIRIdentity(null,"Ident",Util.getOutputType(splitterNode.edges[0])));
	    FlatNode.addEdges(splitterNode, ident);
	    currentNode = ident;
	    
	    
	    //Save Weight of Joiner
	    int sumWeights=0;
	    int[] oldWeights=joinerNode.incomingWeights;
	    for(int i=0;i<oldWeights.length;i++)
		sumWeights+=oldWeights[i];
	    ((SIRJoiner)joinerNode.contents).oldSumWeights=sumWeights;
	}
    }

    /*add the splitter of a feedback loop to a hashset 
      so we can swap the edges after createGraph() has run
    */
    private void swapEdgesLater(FlatNode splitter) 
    {
	if (feedbackSplitters.contains(splitter))
	    Utils.fail("Trying to add multiple back edges from a splitter of a feedbackloop");
	
	feedbackSplitters.add(splitter);
    }
    

    /**
     * Adds a flat node for the given SIROperator, and return it.
     */
    private FlatNode addFlatNode(SIROperator op) {
	FlatNode node = new FlatNode(op);
	SIRMap.put(op, node);
	return node;
    }

    public FlatNode getFlatNode(SIROperator key) {
	FlatNode node = (FlatNode)SIRMap.get(key);
	//	if (node == null)
	//  Utils.fail("Cannot Find FlatNode for SIROperator: " + key);
	return node;
    }

    /* creates the dot file representing the flattened graph */
    public void dumpGraph(String filename) 
    {
	buf = new StringBuffer();
	
	buf.append("digraph Flattend {\n");
	buf.append("size = \"8, 10.5\";");
	top.accept(this, new HashSet(), true);
	buf.append("}\n");
	try {
	    FileWriter fw = new FileWriter(filename);
	    fw.write(buf.toString());
	    fw.close();
	}
	catch (Exception e) {
	    System.err.println("Could not print flattened graph");
	}
	
    }

    /* appends the dot file code representing the given node */
    public void visitNode(FlatNode node) 
    {
	if (node.contents instanceof SIRFilter) {
	    SIRFilter filter = (SIRFilter)node.contents;
	    Utils.assert(buf!=null);

	    buf.append(node.getName() + "[ label = \"" +
		       node.getName() + 
		       " peek: " + filter.getPeekInt() + 
		       " pop: " + filter.getPopInt() + 
		       " push: " + filter.getPushInt());
	    if (node.contents instanceof SIRTwoStageFilter) {
		SIRTwoStageFilter two = (SIRTwoStageFilter)node.contents;
		buf.append(" initPeek: " + two.getInitPeek() + 
			   " initPop: " + two.getInitPop() + 
			   " initPush: " + two.getInitPush());
	    }
	    buf.append("\"];");
	}
	
	if (node.contents instanceof SIRJoiner) {
	    for (int i = 0; i < node.inputs; i++) {
		//joiners may have null upstream neighbors
		if (node.incoming[i] == null)
		    continue;
		buf.append(node.incoming[i].getName() + " -> " 
			   + node.getName());
		buf.append("[label=\"" + node.incomingWeights[i] + "\"];\n");
	    }
      
	}
	for (int i = 0; i < node.ways; i++) {
	    if (node.edges[i] == null)
		continue;
	    if (node.edges[i].contents instanceof SIRJoiner)
		continue;
	    buf.append(node.getName() + " -> " 
		+ node.edges[i].getName());
	    buf.append("[label=\"" + node.weights[i] + "\"];\n");
	}
    }
}


