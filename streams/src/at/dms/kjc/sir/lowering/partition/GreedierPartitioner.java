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
    /**
     * List of Node objects in canonicalized order
     */
    //private LinkedList nodes;
    private TreeMap pairs; //Ordered fusable pairs (keys of type Pair)
    private TreeMap fissable; //Ordered fissable filters (keys of type Node)
    
    public GreedierPartitioner(SIRStream str,WorkEstimate work,int numTiles) {
	this.str=str;
	this.work=work;
	this.numTiles=numTiles;
	//nodes=new LinkedList();
	pairs=new TreeMap(new Comparator() {
		public int compare(Object o1,Object o2) {
		    if(o1==o2)
			return 0;
		    int work1=((Pair)o1).work;
		    int work2=((Pair)o2).work;
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
	fissable=new TreeMap(new Comparator() {
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
    
    //Creates Nodes and populates pairs,fissable
    private void buildNodesList() {
	SIRIterator it = IterFactory.createFactory().createIter(str);
	it.accept(new EmptyStreamVisitor() {
		//SIRFilter prevFilter;
		Node prevNode;
		SIRContainer prevParent;
		
		public void visitFilter(SIRFilter self,
					SIRFilterIter iter) {
		    Node node=new Node(self,work.getWork(self));
		    if(StatelessDuplicate.isFissable(self))
			fissable.put(node,null);
		    SIRContainer parent=self.getParent();
		    if(prevNode!=null) {
			prevNode.next=node;
			node.prev=prevNode;
			if(parent.equals(prevParent)) {
			    Pair pair=new Pair(prevNode,node);
			    pairs.put(pair,null);
			    System.out.println("Adding pair: "+pair.work);
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
	System.out.println("FISSABLE: "+fissable.size());
	int count=new GraphFlattener(str).getNumTiles();
	System.out.println("  GreedierPartitioner detects " + count + " tiles.");
	while(count>numTiles) {
	    System.out.println("PAIRS: "+pairs.size());
	    Pair smallest=(Pair)pairs.firstKey();
	    //pairs.remove(smallest);
	    fuse(smallest);
	    count=new GraphFlattener(str).getNumTiles();
	    System.out.println("  GreedierPartitioner detects " + count + " tiles.");
	}
	return str;
    }

    private void fuse(Pair p) {
	System.out.println("Fusing: "+p.work);
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
	if(prev!=null) {
	    prev.next=newNode;
	    newNode.prev=prev;
	    Pair inter=prev.p2;
	    if(inter!=null) { //Reuse old Pair
		if(inter.n2!=p.n1)
		    Utils.fail("HERE");
		pairs.remove(inter);
		inter.n2=newNode;
		newNode.p1=inter;
		inter.work(prev,newNode);
		pairs.put(inter,null);
	    } else if(prev.filter.getParent()==newNode.filter.getParent()) { //New Pair
		pairs.put(new Pair(prev,newNode),null);
	    } else
		System.out.println("DIFFERENT PARENT! "+prev.filter+" "+result+" "+prev.filter.getParent()+" "+result.getParent());
	}
	if(next!=null) {
	    next.prev=newNode;
	    newNode.next=next;
	    Pair inter=next.p1;
	    if(inter!=null) { //Reuse old Pair
		if(inter.n1!=p.n2)
		    Utils.fail("HERE2");
		pairs.remove(inter);
		inter.n1=newNode;
		newNode.p2=inter;
		inter.work(next,newNode);
		pairs.put(inter,null);
	    } else if(next.filter.getParent()==newNode.filter.getParent()) { //New Pair
		pairs.put(new Pair(newNode,next),null);
	    } else
		System.out.println("DIFFERENT PARENT! "+next.filter+" "+result+" "+next.filter.getParent()+" "+result.getParent());
	}
	//Cleaning up
	p.n1=null;
	p.n2=null;
	pairs.remove(p);
    }

    //Create PartitionGroup corresponding to Pair p in parent
    private PartitionGroup findPartition(Pair p,SIRContainer parent) {
	SIRFilter f1=p.n1.filter;
	System.out.println("Finding: "+f1+" "+p.n2.filter+" in "+parent);
	int[] part=new int[parent.size()-1];
	for(int i=0;i<part.length;i++)
	    part[i]=1;
	/*int idx=0;
	  while(!parent.get(idx).equals(f1))
	  idx++;
	  part[idx]=2;*/
	System.out.println("Found: "+parent.indexOf(f1)+" "+parent.indexOf(p.n2.filter)+" "+(parent.size()-1));
	part[parent.indexOf(f1)]=2;
	return PartitionGroup.createFromArray(part);
    }
    
    class Node {
	SIRFilter filter;
	int work;
	Node prev,next;
	Pair p1,p2; // Pairs that contain this (order matters: p1==pair with prev)

	public Node(SIRFilter filter,int work) {
	    this.filter=filter;
	    this.work=work;
	}

	/*public void addPair(Pair p) {
	    if(p1==null)
		p1=p;
	    else
		p2=p;
		}*/
    }

    class Pair {
	Node n1,n2; //Order matters (n1==first node)
	int work;
	
	public Pair(Node n1,Node n2) {
	    if(n1==null||n2==null)
		Utils.fail("Pair: "+n1+" "+n2);
	    if(n1.next!=n2)
		Utils.fail("Pair2");
	    if(n2.prev!=n1)
		Utils.fail("Pair3");
	    this.n1=n1;
	    this.n2=n2;
	    work(n1,n2);
	    //n1.addPair(this);
	    n1.p2=this;
	    //n2.addPair(this);
	    n2.p1=this;
	}

	public void work(Node n1,Node n2) {
	    work=n1.work+n2.work;
	}
    }
}



