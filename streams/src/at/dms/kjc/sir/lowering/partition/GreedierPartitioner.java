package at.dms.kjc.sir.lowering.partition;

import java.util.*;
import java.io.*;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.flatgraph.*;

public class GreedierPartitioner {
    /**
     * Whether or not joiners need tiles.
     */
    private boolean joinersNeedTiles;
    /**
     * The toplevel stream we're operating on.
     */
    private SIRStream str;
    /**
     * The work estimate of the stream.
     */
    private WorkEstimate work;
    /**
     * The target number of tiles this partitioner is going for.
     */
    private final int numTiles;
    private TreeMap pairs; //Ordered fusable pairs (keys of type Pair)
    private TreeMap nodes; //Ordered filters (keys of type Node)
    private static final int FUSION_OVERHEAD=0; //Adjust

    public GreedierPartitioner(SIRStream str,WorkEstimate work,int numTiles,boolean joinersNeedTiles) {
	this.str=str;
	this.work=work;
	this.numTiles=numTiles;
	this.joinersNeedTiles=joinersNeedTiles;
	pairs=new TreeMap(new Comparator() {
		public int compare(Object o1,Object o2) {
		    if(o1==o2)
			return 0;
		    int work1=((Pair)o1).work;
		    int work2=((Pair)o2).work;
		    if(work1==work2) {
			/*if(o1.hashCode()<o2.hashCode())
			  return -1;
			  else
			  return 1;*/
			return o1.hashCode()-o2.hashCode();
			/*System.out.println("BLAH: "+((Pair)o1).n1+" "+((Pair)o1).n2+" "+((Pair)o2).n1+" "+((Pair)o2).n2);
			  SIRContainer parent1=((Pair)o1).n1.filter.getParent();
			  SIRContainer parent2=((Pair)o2).n1.filter.getParent();
			  double half=((double)(parent1.size()-1))/2;
			  double half2=((double)(parent2.size()-1))/2;
			  double dist1=Math.abs(half-parent1.indexOf(((Pair)o1).n1.filter));
			  double dist2=Math.abs(half2-parent2.indexOf(((Pair)o2).n1.filter));
			  if(dist1==dist2)
			  if(o1.hashCode()<o2.hashCode())
			  return -1;
			  else
			  return 1;
			  else if(dist1>dist2)
			  return -1;
			  else
			  return 1;*/
		    } else
			return work1-work2;
		    /*else if(work1<work2)
		      return -1;
		      else
		      return 1;*/
		}
	    });
	nodes=new TreeMap(new Comparator() {
		public int compare(Object o1,Object o2) {
		   if(o1==o2)
		       return 0;
		   int work1=((Node)o1).work;
		   int work2=((Node)o2).work;
		   if(work1==work2)
		       if(o1.hashCode()<o2.hashCode())
			   return -1;
		       else
			   return 1;
		   else if(work1<work2)
		       return -1;
		   else
		       return 1;
		}
	    });
	buildNodesList();
    }
    
    //Creates Nodes and populates pairs,nodes
    private void buildNodesList() {
	SIRIterator it = IterFactory.createFactory().createIter(str);
	it.accept(new EmptyStreamVisitor() {
		Node prevNode;
		SIRContainer prevParent;
		
		public void visitFilter(SIRFilter self,
					SIRFilterIter iter) {
		    Node node=new Node(self,work.getWork(self));
		    nodes.put(node,null);
		    if(StatelessDuplicate.isFissable(self))
			node.fissable=true;
		    SIRContainer parent=self.getParent();
		    if(prevNode!=null) {
			prevNode.next=node;
			node.prev=prevNode;
			if(parent.equals(prevParent)) {
			    Pair pair=new Pair(prevNode,node);
			    pairs.put(pair,null);
			}
		    }
		    prevNode=node;
		    prevParent=parent;
		}
	    });
    }

    /**
     * This is the toplevel call for doing partitioning.  Returns the
     * partitioned stream.
     */
    public SIRStream toplevel() {
	int count=Partitioner.countTilesNeeded(str, joinersNeedTiles);
	System.out.println("  GreedierPartitioner detects " + count + " tiles.");
	boolean cont;
	do { //Iterate till can't (# fissable filters is always decreasing)
	    cont=false;
	    boolean fiss=shouldFiss(); //Try Fiss
	    while(fiss&&count<numTiles) {
		cont=true;
		Node big=(Node)nodes.lastKey();
		System.out.println("  Fissing: "+big.filter);
		fiss(big);
		count=Partitioner.countTilesNeeded(str, joinersNeedTiles);
		fiss=shouldFiss();
		System.out.println("  GreedierPartitioner detects " + count + " tiles.");
	    }
	    while(count>numTiles) { //Try Fuse
		cont=true;
		//Pair smallest=(Pair)pairs.firstKey();
		Pair smallest=findSmallest();
		fuse(smallest);
		count=Partitioner.countTilesNeeded(str, joinersNeedTiles);
		System.out.println("  GreedierPartitioner detects " + count + " tiles.");
	    }
	} while(cont);
	return str;
    }

    private Pair findSmallest() {
	Pair smallest=(Pair)pairs.firstKey();
	int work=smallest.work;
	ArrayList temp=new ArrayList();
	boolean cont;
	do {
	    cont=false;
	    temp.add(smallest);
	    pairs.remove(smallest);
	    Pair newPair=(Pair)pairs.firstKey();
	    int newWork=newPair.work;
	    if(newWork==work) {
		SIRContainer parent1=smallest.n1.filter.getParent();
		SIRContainer parent2=newPair.n1.filter.getParent();
		if(parent1==parent2) {
		    double half=((double)(parent1.size()-1))/2;
		    double dist1=Math.abs(half-parent1.indexOf(smallest.n1.filter));
		    double dist2=Math.abs(half-parent1.indexOf(newPair.n1.filter));
		    if(dist1<dist2) {
			cont=true;
			smallest=newPair;
			work=newWork;
		    }
		}
	    }
	} while(cont);
	Object[] fix=temp.toArray();
	for(int i=0;i<fix.length;i++)
	    pairs.put(fix[i],null);
	return smallest;
    }

    private boolean shouldFiss() {
	Node big=(Node)nodes.lastKey();
	if(!big.fissable)
	    return false;
	Pair small=(Pair)pairs.firstKey();
	pairs.remove(small);
	Pair testSmall=(Pair)pairs.firstKey(); // Make sure 2nd smallest pair is below bottleneck
	pairs.put(small,null);
	return testSmall.work+FUSION_OVERHEAD<big.work;
    }

    private void fuse(Pair p) {
	nodes.remove(p.n1);
	nodes.remove(p.n2);
	SIRContainer parent=p.n1.filter.getParent();
	PartitionGroup part=findPartition(p,parent);
	//Fuse
	SIRFilter result=null;
	if(parent instanceof SIRPipeline) {
	    result=(SIRFilter)FusePipe.fuse((SIRPipeline)parent,part);
	    Lifter.eliminatePipe((SIRPipeline)parent);
	} else if(parent instanceof SIRSplitJoin) {
	    FuseSplit.fuse((SIRSplitJoin)parent,part);
	    result=(SIRFilter)FuseSplit.getLastFused();
	    Lifter.eliminateSJ((SIRSplitJoin)parent);
	} else
	    Utils.fail("Trying to fuse FeedBackLoop");
	//Update Rep
	Node prev=p.n1.prev;
	Node next=p.n2.next;
	Node newNode=new Node(result,WorkEstimate.getWorkEstimate(result).getWork(result));
	nodes.put(newNode,null);
	if(prev!=null) {
	    prev.next=newNode;
	    newNode.prev=prev;
	    Pair inter=prev.p2;
	    if(inter!=null) { //Reuse old Pair
		pairs.remove(inter);
		inter.n2=newNode;
		newNode.p1=inter;
		inter.work(prev,newNode);
		pairs.put(inter,null);
	    } else if(prev.filter.getParent()==newNode.filter.getParent()) { //New Pair
		pairs.put(new Pair(prev,newNode),null);
	    }
	}
	if(next!=null) {
	    next.prev=newNode;
	    newNode.next=next;
	    Pair inter=next.p1;
	    if(inter!=null) { //Reuse old Pair
		pairs.remove(inter);
		inter.n1=newNode;
		newNode.p2=inter;
		inter.work(next,newNode);
		pairs.put(inter,null);
	    } else if(next.filter.getParent()==newNode.filter.getParent()) { //New Pair
		pairs.put(new Pair(newNode,next),null);
	    }
	}
	//Cleaning up
	pairs.remove(p);
	p.n1=null;
	p.n2=null;
    }

    private void fiss(Node node) {
	nodes.remove(node);
	//Fiss
	SIRSplitJoin split=StatelessDuplicate.doit(node.filter,2);
	//Update Rep
	SIRFilter filt=(SIRFilter)split.get(0);
	Node n1=new Node(filt,WorkEstimate.getWorkEstimate(filt).getWork(filt));
	nodes.put(n1,null);
	filt=(SIRFilter)split.get(1);
	Node n2=new Node(filt,WorkEstimate.getWorkEstimate(filt).getWork(filt));
	nodes.put(n2,null);
	n1.next=n2;
	n2.prev=n1;
	Node prev=node.prev;
	if(prev!=null) {
	    prev.next=n1;
	    n1.prev=prev;
	    //Cleaning up
	    prev.p2=null;
	    Pair prevPair=node.p1;
	    if(prevPair!=null) {
		node.p1.n1=null;
		node.p1.n2=null;
		node.p1=null;
	    }
	}
	Node next=node.next;
	if(next!=null) {
	    next.prev=n2;
	    n2.next=next;
	    //Cleaning up
	    next.p1=null;
	    Pair nextPair=node.p2;
	    if(nextPair!=null) {
		node.p2.n1=null;
		node.p2.n2=null;
		node.p2=null;
	    }
	}
    }

    //Create PartitionGroup corresponding to Pair p in parent
    private PartitionGroup findPartition(Pair p,SIRContainer parent) {
	SIRFilter f1=p.n1.filter;
	int[] part=new int[parent.size()-1];
	for(int i=0;i<part.length;i++)
	    part[i]=1;
	part[parent.indexOf(f1)]=2;
	return PartitionGroup.createFromArray(part);
    }
    
    class Node {
	SIRFilter filter;
	int work;
	Node prev,next;
	Pair p1,p2; // Pairs that contain this (order matters: p1==pair with prev)
	boolean fissable; // Only filters fissable in the original graph have fissable==true

	public Node(SIRFilter filter,int work) {
	    this.filter=filter;
	    this.work=work;
	}
    }

    class Pair {
	Node n1,n2; //Order matters (n1==first node)
	int work;
	
	public Pair(Node n1,Node n2) {
	    this.n1=n1;
	    this.n2=n2;
	    work(n1,n2);
	    n1.p2=this;
	    n2.p1=this;
	}

	public void work(Node n1,Node n2) {
	    work=n1.work+n2.work;
	}
    }
}
