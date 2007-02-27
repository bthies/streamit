package at.dms.kjc.flatgraph;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.util.Utils;
import java.util.*;

/**
 * This class will create a graph of {@link at.dms.kjc.flatgraph.FlatNode} 
 * that represents the underlying SIR graph of the application.  It is basically
 * the SIR graph without containers.
 * <p>
 * The constructed graph of FlatNodes, the Flat graph, will have one 
 * node for each SIROperator in the SIR graph (except when --sync is enabled), 
 * but there will exist no containers, FlatNodes directly connect to/from their
 * downstream/upstream nodes.
 * <p>
 * Each SIROperator will be converted to a FlatNode: SIRFilter into a FlatNode with 
 * at most one input/one output, SIRSplitter into a multiple output/one input, 
 * SIRJoiner into multiple input/one output. 
 * <p>
 * If --sync is enabled, it will attempt to coalesce multiple splitters
 * or joiners into a single FlatNode.
 *   
 * @author mgordon
 */
public class GraphFlattener extends at.dms.util.Utils 
{
    /** When creating the flat graph, this is really the previous 
     * node we created.  When we are creating a node we connect 
     * previousNode to it.
     */
    private FlatNode previousNode;
    /** Hashset stores the splitters of feedbackloops
     * so the edges can be swapped after createGraph()
     * see the note in the create graph algorithm */
    private HashSet<FlatNode> feedbackSplitters;
    /** the toplevel, entry, FlatNode of the constructed Flat graph */
    public FlatNode top;
    /** maps SIROperator to their corresponding FlatNode */
    private HashMap<SIROperator, FlatNode> SIRMap;
    /** Used by --sync (synch removal) to reschedule the flat graph */
    public static LinkedList<FlatNode> needsToBeSched=new LinkedList<FlatNode>();;

    /**
     * Create a graph of FlatNodes that represents the SIR graph 
     * of the application rooted at toplevel.
     * 
     * If --sync is enabled, then try to coalesce multiple splitters 
     * or joiners into a single FlatNode.
     * 
     * @param toplevel The toplevel SIR container of the application.
     */
    public GraphFlattener(SIROperator toplevel) 
    {
        this.SIRMap = new HashMap<SIROperator, FlatNode>();
        feedbackSplitters = new HashSet<FlatNode>();

        //create the flat graph
        createGraph(toplevel);
        //now we need to fix up the graph a bit
        //we need to add all the back edges of the splitter of a feedbacks
        Iterator<FlatNode> it = feedbackSplitters.iterator();
        while(it.hasNext()) {
            it.next().swapSplitterEdges();
        }
    }

    /**
     * Returns the number of tiles that would be needed to execute
     * this graph.  That is, just counts the filters, plus any joiners
     * whose output is not connected to another joiner.
     * 
     * This is used only for the SpaceDynamic and the original 
     * at.dms.kjc.raw backends.
     * 
     * @return The number of tiles needed to map this graph to raw.
     */
    public int getNumTiles() {
        int count = 0;
        for (Iterator it = SIRMap.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry)it.next();
            if (entry.getKey() instanceof SIRFilter) {
                if (countMe((SIRFilter)entry.getKey()))
                    count++;
            }
            else if (entry.getKey() instanceof SIRJoiner) {
                if(KjcOptions.sync)
                    //Sync removal should give an accurate count of joiners
                    //(Adjacent Joiners Coalesced)
                    count++;
                else {
                    // count a joiner if none of its outgoing edges is to
                    // another joiner
                    FlatNode node = (FlatNode)entry.getValue();
                    SIRJoiner joiner = (SIRJoiner)entry.getKey();
                    FlatNode[] edges = node.getEdges();
                    int increment = 1;
                    for (int i=0; i<edges.length; i++) {
                        if (edges[i]!=null &&
                            edges[i].contents instanceof SIRJoiner) {
                            increment = 0;
                        }
                    }
                    //check if this is a null joiner if so, don't count it
                    if (joiner.getType().isNull() || joiner.getSumOfWeights() == 0)
                        increment = 0;
                
                    count += increment;
                }
            } 
        }
        return count;
    }
    
    /**
     * Recursive method that is used to create the Flat graph.  
     * 
     * @param current the SIROperator we are visiting currently in the
     * application's SIR graph and that needs to be added to the 
     * FlatGraph.
     */
    private void createGraph(SIROperator current) 
    {
        if (current instanceof SIRFilter) {
            //create the flat node
            FlatNode node = addFlatNode(current);
            //is first node we have visited, if so, set it as the top level
            if (top == null) {
                previousNode = node;
                top = node;
            }
            //add an edge from the previous flatnode (previousNode) to this new node
            previousNode.addEdges(node);
            previousNode = node;
        }
        if (current instanceof SIRPipeline){
            SIRPipeline pipeline = (SIRPipeline) current;
            //for a pipeline, visit each stream of the pipeline
            for (int i=0; i<pipeline.size(); i++) {
                createGraph(pipeline.get(i));
            }
        }
        if (current instanceof SIRSplitJoin) {
            SIRSplitJoin sj = (SIRSplitJoin) current;
            //create a node for the spliiter
            FlatNode splitterNode = addFlatNode (sj.getSplitter());
            if (top == null) {
                //it is toplevel if we have not seen anything yet..
                previousNode = splitterNode;
                top = splitterNode;
            }
            //create the node for the joiner right now
            FlatNode joinerNode = addFlatNode (sj.getJoiner());
                
            previousNode.addEdges(splitterNode);
            for (int i = 0; i < sj.size(); i++) {
                //for each parellel stream the previous node is the splitter 
                //at the beginning
                previousNode = splitterNode;
                createGraph(sj.get(i));
                //for the last node the previous node is the joiner
                previousNode.addEdges(joinerNode);
            }
        
            //Save Weight of Joiner
            int sumWeights=0;
            int[] oldWeights=joinerNode.incomingWeights;
            for(int i=0;i<oldWeights.length;i++)
                sumWeights+=oldWeights[i];
            ((SIRJoiner)joinerNode.contents).oldSumWeights=sumWeights;
        
            if(KjcOptions.sync) {
                //Coalesce Splitters
                //And Joiner-Splitter destruction
                if((((SIRSplitter)splitterNode.contents).getType()==SIRSplitType.ROUND_ROBIN)||
                   (((SIRSplitter)splitterNode.contents).getType()==SIRSplitType.WEIGHTED_RR)) {
                    //Joiner appearing right above splitters if there is one (for Joiner-splitter destruction)
                    FlatNode adjacentJoin=splitterNode.incoming[0];
                    //Tracks current weight being analyzed in all the children splitters
                    int[] offsetArray=new int[splitterNode.getEdges().length];
                    //Tracks any remainder of a weight needed to be accounted for before the respective offsetArray entry is incremented
                    int[] remainderArray=new int[splitterNode.getEdges().length];
                    //Builds up the analyzed weights of the new splitter equivalent to parent splitter with child splitters folded in
                    LinkedList<Integer> newWeights=new LinkedList<Integer>();
                    //Builds up the edges
                    LinkedList<FlatNode> newEdges=new LinkedList<FlatNode>();
                    //Tracks whether done
                    boolean loop=false;
                    //First iteration through
                    for(int i=0;i<splitterNode.getEdges().length;i++) {
                        FlatNode childNode=splitterNode.getEdges()[i];
                        if((childNode.contents instanceof SIRSplitter)&&
                           ((((SIRSplitter)childNode.contents).getType()==SIRSplitType.ROUND_ROBIN)||
                            (((SIRSplitter)childNode.contents).getType()==SIRSplitType.WEIGHTED_RR))) {
                            /*If child nodes are splitters this pass tries to repeat the parent splitter
                             *enough times to fold the children into it
                             *Hierarchies of splitters is handled by recursion
                             *Splitters are folded into its splitter parent which is folded into its
                             *splitter parent, etc*/
                            //Offset into the child weights so far
                            int off=0;
                            //Sum of the child splitter so far
                            int sum=0;
                            //Weight of the parent splitter we are aiming for
                            int target=splitterNode.weights[i]; 
                            while(sum<target) { //Until the child splitter cannot fit
                                //Keep old sum
                                int oldSum=sum;
                                //Add childs weight
                                sum+=childNode.weights[off];
                                if(sum<=splitterNode.weights[i]) {
                                    //If child can still fit add weight and edge
                                    newWeights.add(new Integer(childNode.weights[off]));
                                    newEdges.add(childNode.getEdges()[off]);
                                } else {
                                    //Else need to continue looping
                                    loop=true;
                                    int fit=splitterNode.weights[i]-oldSum;
                                    //Store the part that can fit
                                    newWeights.add(new Integer(fit));
                                    newEdges.add(childNode.getEdges()[off]);
                                    //Store part of child weight that can't fit into remainder
                                    remainderArray[i]=childNode.weights[off]-fit;
                                    //Done with this child for now
                                    break;
                                }
                                //New child's weight
                                if((++off)==childNode.weights.length)
                                    off=0;
                            }
                            //If don't end repeating child an integral number of times then not done
                            if(off!=0)
                                loop=true;
                            //Store how far we got on this child
                            offsetArray[i]=off;
                        } else {
                            //Child not splitter
                            newWeights.add(new Integer(splitterNode.weights[i]));
                            newEdges.add(childNode);
                            offsetArray[i]=-1; //Ignore this edge in analysis
                        }
                    }
                    while(loop) { //Iterate till children fold in nicely
                        loop=false;
                        for(int i=0;i<offsetArray.length;i++) {
                            //Very similar except now need to handle the case of a remainder weight
                            int sum=0;
                            int target=splitterNode.weights[i];
                            FlatNode childNode=splitterNode.getEdges()[i];
                            if(offsetArray[i]<0) {
                                //If child isn't splitter
                                newWeights.add(new Integer(target));
                                newEdges.add(childNode);
                                continue;
                            }
                            int off=offsetArray[i];
                            int rem=remainderArray[i];
                            if(rem>0) //Only worry if there is a remainder
                                if(rem<=target) {
                                    //If less than target then add remainder and increment off
                                    sum+=rem;
                                    newWeights.add(new Integer(rem));
                                    newEdges.add(childNode.getEdges()[off]);
                                    remainderArray[i]=0;
                                    if((++off)==childNode.weights.length)
                                        off=0;
                                } else {
                                    //Else add as much as possible and another loop required
                                    loop=true;
                                    newWeights.add(new Integer(target));
                                    newEdges.add(childNode.getEdges()[off]);
                                    remainderArray[i]=rem-target; //Store new remainder
                                    continue;
                                }
                            while(sum<target) { //This part about same as above
                                int oldSum=sum;
                                sum+=childNode.weights[off];
                                if(sum<=target) {
                                    newWeights.add(new Integer(childNode.weights[off]));
                                    newEdges.add(childNode.getEdges()[off]);
                                    remainderArray[i]=0;
                                } else {
                                    loop=true;
                                    int fit=target-oldSum;
                                    newWeights.add(new Integer(fit));
                                    newEdges.add(childNode.getEdges()[off]);
                                    remainderArray[i]=childNode.weights[off]-fit;
                                    break;
                                }
                                if((++off)==childNode.weights.length)
                                    off=0;
                            }
                            if(off!=0) //If don't end on integral repetition of child need to loop further
                                loop=true;
                            offsetArray[i]=off; //Store offset for next loop
                        }
                    }
                    //Fix Edges
                    FlatNode[] tempEdges=newEdges.toArray(new FlatNode[0]);
                    splitterNode.setEdges(tempEdges);
                    int[] tempWeights=new int[newWeights.size()];
                    for(int i=0;i<tempWeights.length;i++) { //Copy weights into array
                        tempWeights[i]=newWeights.get(i).intValue();
                    }
                    splitterNode.weights=tempWeights; //Set weights
                    splitterNode.currentEdge=tempEdges.length;
                    splitterNode.ways=tempEdges.length; //Set ways
                    for(int i=0;i<tempEdges.length;i++) //Set reverse edges
                        tempEdges[i].incoming[0]=splitterNode;
            
                    //Joiner-Splitter Destruction
                    if(adjacentJoin.contents instanceof SIRJoiner) {
                        SIRMap.remove(adjacentJoin.contents); //Unneeded
                        int[] inWeights=adjacentJoin.incomingWeights;
                        int[] outWeights=splitterNode.weights;
                        FlatNode[] inEdges=adjacentJoin.incoming;
                        FlatNode[] outEdges=splitterNode.getEdges();
                        int inSum=0; //Total sum of weights of joiner (into joiner-splitter system)
                        int outSum=0; //Total sum of weights of splitter (out of joiner-splitter system)
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
                        //Repeat joiner and splitter till their sum is the LCM of the 2 of them
                        int inTimes=1; //How many times joiner needs to be repeated
                        int outTimes=1; //How many times splitter needs to be repeated
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
                        //Arrays keeping track of the real outputs of the system
                        LinkedList[] inWeightList=new LinkedList[outEdges.length];
                        LinkedList[] inEdgeList=new LinkedList[outEdges.length];
                        for(int i=0;i<outEdges.length;i++) { //Initializing the linked lists
                            inWeightList[i]=new LinkedList();
                            inEdgeList[i]=new LinkedList();
                        }
                        //Mapping keeping track if an input needs to be split into several outputs
                        HashMap<FlatNode, LinkedList<Object>> needSplit=new HashMap<FlatNode, LinkedList<Object>>();
                        //Analyze out weights the right amount of times
                        for(;outTimes>0;outTimes--)
                            for(int i=0;i<outWeights.length;i++) {
                                int sum=0;
                                int target=outWeights[i];
                                FlatNode inEdge=inEdges[off];
                                //Dummy identity node
                                //Later we will set its input and outputs to the right places
                                FlatNode ident=new FlatNode(new SIRIdentity(getOutputType(inEdge)));
                                ident.inputs=1;
                                ident.ways=1;
                                ident.weights=new int[]{1};
                                //Setting defaults
                                ident.incoming=new FlatNode[]{inEdge};
                                inEdge.setEdges(new FlatNode[]{ident});
                                LinkedList<Integer> currentWeights=inWeightList[i];
                                LinkedList<FlatNode> currentEdges=inEdgeList[i];
                                LinkedList<Object> entry=needSplit.get(inEdge);
                                if(rem>0) //Account for remainder
                                    if(rem>target) {
                                        rem-=target;
                                        Integer weight=new Integer(target); //Put only the amount that fits
                                        currentWeights.add(weight);
                                        currentEdges.add(ident);
                                        if(entry==null) {
                                            entry=new LinkedList<Object>();
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
                                        Integer weight=new Integer(rem); //Put the full remainder in
                                        rem=0;
                                        currentWeights.add(weight);
                                        currentEdges.add(ident);
                                        if(entry==null) {
                                            entry=new LinkedList<Object>();
                                            entry.add(weight);
                                            entry.add(ident);
                                            needSplit.put(inEdge,entry);
                                        } else {
                                            entry.add(weight);
                                            entry.add(ident);
                                        }
                                        if((++off)==inWeights.length) //Increment off
                                            off=0;
                                    }
                                while(sum<target) {
                                    inEdge=inEdges[off];
                                    entry=needSplit.get(inEdge);
                                    //Dummy identity again
                                    ident=new FlatNode(new SIRIdentity(getOutputType(inEdge)));
                                    ident.inputs=1;
                                    ident.ways=1;
                                    ident.weights=new int[]{1};
                                    ident.incoming=new FlatNode[]{inEdge};
                                    inEdge.setEdges(new FlatNode[]{ident});
                                    int weightInt=inWeights[off];
                                    int oldSum=sum;
                                    sum+=weightInt;
                                    if(sum<=target) {
                                        //If sum <= target add weight and edges as normal
                                        Integer weight=new Integer(weightInt);
                                        currentWeights.add(weight);
                                        currentEdges.add(ident);
                                        if(entry==null) {
                                            entry=new LinkedList<Object>();
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
                                        //Else only put as much as can fit
                                        Integer weight=new Integer(target-oldSum);
                                        rem=sum-target; //Set remainder
                                        currentWeights.add(weight);
                                        currentEdges.add(ident);
                                        if(entry==null) {
                                            entry=new LinkedList<Object>();
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
                            //Handle the outputs of Joiner-splitter system
                            if(curWeights.size()==1) {
                                //If only 1 ouput can just point dummy identity to right place
                                ((FlatNode)curEdges.get(0)).setEdges(new FlatNode[]{out});
                                out.incoming[0]=(FlatNode)curEdges.get(0);
                            } else {
                                //Else need to create a new joiner
                                SIRJoiner newContents=SIRJoiner.createWeightedRR(null,new JExpression[0]);
                                FlatNode newJoin=new FlatNode(newContents);
                                newJoin.oldContents=adjacentJoin.contents;
                                ((SIRJoiner)newJoin.contents).oldSumWeights=((SIRJoiner)adjacentJoin.contents).oldSumWeights;
                                SIRMap.put(newContents,newJoin); //To be counted correctly
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
                                newJoin.setEdges(new FlatNode[]{out});
                                out.incoming[0]=newJoin;
                                for(int j=0;j<curEdges.size();j++) {
                                    ((FlatNode)curEdges.get(j)).setEdges(new FlatNode[]{newJoin});
                                }
                                newJoin.schedDivider=inTotal; //Total sum of all weights
                                newJoin.schedMult=newSum; //Sum of new weights of this joiner
                                //New cycle count=((old cyle count)*schedMult)/schedDivider
                                needsToBeSched.add(newJoin); //Needs to be scheduled specially later
                            }
                        }
                        //Handle inputs to joiner splitter system
                        Iterator<FlatNode> iter=needSplit.keySet().iterator();
                        while(iter.hasNext()) {
                            FlatNode in=iter.next();
                            LinkedList list=needSplit.get(in);
                            if(list.size()==2) { //If input going to one output just set incoming of dummy identity
                                ((FlatNode)list.get(1)).incoming=new FlatNode[]{inEdges[off]};
                            } else {
                                //Else need to create the appropriate splitter to split input to right joiners (or just output of identities)
                                int size=list.size()/2;
                                FlatNode dummySplit=new FlatNode(SIRSplitter.createWeightedRR(null,new JExpression[0]));
                                dummySplit.inputs=1;
                                dummySplit.incoming=new FlatNode[]{in};
                                in.setEdge(0,dummySplit);
                                int[] dummyWeights=new int[size];
                                FlatNode[] dummyEdges=new FlatNode[size];
                                for(int j=0,offset=0;j<list.size();j+=2,offset++) {
                                    dummyWeights[offset]=((Integer)list.get(j)).intValue(); //First Weight stored
                                    dummyEdges[offset]=(FlatNode)list.get(j+1); //Then Edge
                                    ((FlatNode)list.get(j+1)).incoming[0]=dummySplit;
                                }
                                dummySplit.ways=size;
                                dummySplit.setEdges(dummyEdges);
                                dummySplit.weights=dummyWeights;
                            }
                        }
                    }
                }
        
                //Coalesce Joiners
                if((((SIRJoiner)joinerNode.contents).getType()==SIRJoinType.ROUND_ROBIN)||
                   (((SIRJoiner)joinerNode.contents).getType()==SIRJoinType.WEIGHTED_RR)) {
                    //Essentially the same code is used to determine the weights of the coalesced joiner as splitter
                    //Except weights is changed to incomingWeights and edges changed to incomingEdges
                    //Also the fix edges code is much worse than with splitters because have to maintain structure
                    int[] offsetArray=new int[joinerNode.incoming.length];
                    int[] remainderArray=new int[joinerNode.incoming.length];
                    LinkedList<Integer> newWeights=new LinkedList<Integer>();
                    LinkedList<FlatNode> newEdges=new LinkedList<FlatNode>();
                    boolean loop=false;
                    for(int i=0;i<joinerNode.incoming.length;i++) {
                        FlatNode childNode=joinerNode.incoming[i];
                        if((childNode.contents instanceof SIRJoiner)&&
                           ((((SIRJoiner)childNode.contents).getType()==SIRJoinType.ROUND_ROBIN)||
                            (((SIRJoiner)childNode.contents).getType()==SIRJoinType.WEIGHTED_RR))) {
                            SIRMap.remove(childNode.contents); //Unneeded
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
                                    remainderArray[i]=0;
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
                                    remainderArray[i]=sum-target;
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
                    //Really bad and ugly just to maintain structure
                    //Has the same function as fix edges as fix edges with splitters
                    //but needs to introduce lots of dummy splitters to keep everything legal
                    FlatNode[] tempEdges=newEdges.toArray(new FlatNode[0]);
                    joinerNode.incoming=tempEdges;
                    int[] tempWeights=new int[newWeights.size()];
                    for(int i=0;i<tempWeights.length;i++) {
                        tempWeights[i]=newWeights.get(i).intValue();
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
                    HashMap<FlatNode, LinkedList<Object>> visited=new HashMap<FlatNode, LinkedList<Object>>();
                    SIRSplitter dummySplit=SIRSplitter.create(null,SIRSplitType.NULL,0);
                    for(int i=0;i<newEdges.size();i++) {
                        FlatNode node=newEdges.get(i);
                        LinkedList<Object> edges=visited.get(node);
                        if(edges==null) {
                            edges=new LinkedList<Object>();
                            FlatNode ident=new FlatNode(new SIRIdentity(getOutputType(node)));
                            FlatNode split=new FlatNode(SIRSplitter.createWeightedRR(null,new JExpression[0]));
                            node.setEdge(0,split);
                            split.inputs=1;
                            split.incoming=new FlatNode[]{node};
                            ident.inputs=1;
                            ident.ways=1;
                            ident.weights=new int[]{1};
                            ident.incoming=new FlatNode[]{split};
                            ident.setEdges(new FlatNode[]{joinerNode});
                            tempEdges[i]=ident;
                            edges.add(split); //0th elem of edges
                            edges.add(ident); //Then comes ident weight pairs
                            edges.add(newWeights.get(i));
                            visited.put(node,edges);
                        } else {
                            FlatNode ident=new FlatNode(new SIRIdentity(getOutputType(node)));
                            FlatNode split=(FlatNode)edges.get(0);
                            ident.inputs=1;
                            ident.ways=1;
                            ident.weights=new int[]{1};
                            ident.incoming=new FlatNode[]{split};
                            ident.setEdges(new FlatNode[]{joinerNode});
                            tempEdges[i]=ident;
                            edges.add(ident); //Then comes ident weight pairs
                            edges.add(newWeights.get(i));
                        }
                    }
                    Iterator<FlatNode> iter=visited.keySet().iterator();
                    while(iter.hasNext()) {
                        FlatNode node=iter.next();
                        List list=visited.get(node);
                        FlatNode split=(FlatNode)list.get(0);
                        int size=(list.size()-1)/2;
                        split.setEdges(new FlatNode[size]);
                        split.weights=new int[size];
                        split.ways=size;
                        if(size==1) {
                            FlatNode ident=(FlatNode)list.get(1);
                            node.setEdge(0,ident);
                            ident.incoming[0]=node;
                            needsToBeSched.add(ident);
                        } else {
                            needsToBeSched.add(split);
                            for(int i=1,off=0;i<list.size();i+=2,off++) {
                                split.setEdge(off,(FlatNode)list.get(i));
                                int weight=((Integer)list.get(i+1)).intValue();
                                split.weights[off]=weight;
                            }
                        }
                    }           
                }
            }
        
            previousNode = joinerNode;       
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
                //previousNode = joinerNode;
                top = joinerNode;
            }
            FlatNode splitterNode = addFlatNode (loop.getSplitter());
        
            FlatNode.addEdges(previousNode, joinerNode);
        
            previousNode = joinerNode;
            createGraph(loop.getBody());


            FlatNode.addEdges(previousNode, splitterNode);
        
            //here is the hack!
            swapEdgesLater(splitterNode);
        
            previousNode = splitterNode;
            createGraph(loop.getLoop());
            FlatNode.addEdges(previousNode, joinerNode);
            /*
            //Add dummy identity on the output splitter so splitters are always followed by a filter (making analysis simple)
            FlatNode ident=new FlatNode(new SIRIdentity(getOutputType(splitterNode.edges[0])));
            FlatNode.addEdges(splitterNode, ident);
            previousNode = ident;
            */
            previousNode = splitterNode;

            //Save Weight of Joiner
            int sumWeights=0;
            int[] oldWeights=joinerNode.incomingWeights;
            for(int i=0;i<oldWeights.length;i++)
                sumWeights+=oldWeights[i];
            ((SIRJoiner)joinerNode.contents).oldSumWeights=sumWeights;
        }
    }

    /**
     * Add the splitter of a feedback loop to a HashSet 
     * so we can swap the edges after createGraph() has run.
     * 
     * @see FlatNode#swapSplitterEdges
     * @see GraphFlattener#createGraph
     * 
     * @param splitter The node that represents the splitter of the feedbackloop.
     */
    private void swapEdgesLater(FlatNode splitter) 
    {
        if (feedbackSplitters.contains(splitter))
            Utils.fail("Trying to add multiple back edges from a splitter of a feedbackloop");
    
        feedbackSplitters.add(splitter);
    }
    

   /**
    * Adds a flat node for the given SIROperator, and return it.
    * Also add the association to the SIRMap HashMap.
    * 
    * @param op the SIROperator for which to create a new FlatNode
    * @return The new FlatNode.
    */
    private FlatNode addFlatNode(SIROperator op) {
        FlatNode node = new FlatNode(op);
        SIRMap.put(op, node);
        return node;
    }

    /**
     * Given an SIROperator, key, return the FlatNode that 
     * was created to represent key, or null if one was not created.
     * 
     * @param key 
     * @return the FlatNode that 
     * was created to represent key, or null if one was not created.
     */
    public FlatNode getFlatNode(SIROperator key) {
        FlatNode node = SIRMap.get(key);
        //  if (node == null)
        //  Utils.fail("Cannot Find FlatNode for SIROperator: " + key);
        return node;
    }

    /**
     * Return the output type of the FlatNode node.
     * 
     * @param node The FlatNode in question.
     * 
     * @return node's output type.
     */
    public static CType getOutputType(FlatNode node) {
        if (node.contents instanceof SIRFilter)
            return ((SIRFilter)node.contents).getOutputType();
        else if (node.contents instanceof SIRJoiner)
            return getJoinerType(node);
        else if (node.contents instanceof SIRSplitter)
            return getOutputType(node.incoming[0]);
        else {
            Utils.fail("Cannot get output type for this node");
            return null;
        }
    }

    /**
     * Return true if this filter should be mapped to a tile, 
     * meaning it is not a predefined filter.
     * 
     * @param filter The SIRFilter is question.
     *  
     * @return true if this filter should be mapped to a tile, 
     * meaning it is not a predefined filter.
     */
    public static boolean countMe(SIRFilter filter) {
        return !(filter instanceof SIRIdentity ||
                 filter instanceof SIRFileWriter ||
                 filter instanceof SIRFileReader ||
                 filter instanceof SIRPredefinedFilter);
    }
    
    /**
     * Return the data type of items that flow through joiner.
     *  
     * @param joiner The joiner
     * @return The CType of items that flow through joiner.
     */
    public static CType getJoinerType(FlatNode joiner) 
    {        
        boolean found;
        //search backward until we find the first filter
        while (!(joiner == null || joiner.contents instanceof SIRFilter)) {
            found = false;
            for (int i = 0; i < joiner.inputs; i++) {
                if (joiner.incoming[i] != null) {
                    joiner = joiner.incoming[i];
                    found = true;
                }
            }
            if (!found)
                Utils.fail("cannot find any upstream filter from " + joiner.contents.getName());
        }
        //now get the first filter's type and return it
        if (joiner != null) 
            return ((SIRFilter)joiner.contents).getOutputType();
        else 
            return CStdType.Void;
    }

    /**
     * Return the multiplicity of node in the schedule determined by init.
     * 
     * If node does not appear in the schedule, return 0;
     * 
     * @param node 
     * @param init If true use initExecutionCounts, else use steadyExecutionCounts
     * @param initExecutionCounts HashMap of FlatNode->Integer for init multiplicities
     * @param steadyExecutionCounts HashMap of FlatNode->Integer for steadyx multiplicities
     * @return The multiplicity of node in init if init == true or steady of init == false, 
     * or -1 if the corresponding HashMap is null, or 0 if the node does not appear
     * in the schedule for the stage.
     */
    public static int getMult(FlatNode node, boolean init, 
                              HashMap<FlatNode, Integer> initExecutionCounts, HashMap<FlatNode, Integer> steadyExecutionCounts)
    {
        if ((init ? initExecutionCounts : steadyExecutionCounts) == null)
            return -1;
        Integer val = 
            ((Integer)(init ? initExecutionCounts.get(node) : steadyExecutionCounts.get(node)));
        if (val == null)
            return 0;
        else 
            return val.intValue();
    }
    
}


