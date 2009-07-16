package at.dms.kjc.smp;

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

public class StatefulFusion {
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
    private TreeMap<Node,Object> nodes; //Ordered filters (keys of type Node)

    /**
     * Construct StatefulFusion
     * @param str The toplevel SIRStream.
     * @param work WorkEstimate to drive fusion.
     * @param numTiles Target number of tiles.
     * @param joinersNeedTiles Indicates if joiners require a tile of their own.
     *                         (Needed on some backends.)
     */
    public StatefulFusion(SIRStream str, WorkEstimate work, int numTiles, boolean joinersNeedTiles) {
        this.str=str;
        this.work=work;
        this.numTiles=numTiles;
        this.joinersNeedTiles=joinersNeedTiles;

        //Setup pairs (of nodes) ordered by work
        pairs=new TreeMap(new Comparator() {
                public int compare(Object o1,Object o2) {
                    if(o1==o2)
                        return 0;
                    long work1=((Pair)o1).work;
                    long work2=((Pair)o2).work;
                    if(work1==work2) {
                        return o1.hashCode()-o2.hashCode();
                    } else {
                        if (work1-work2>0) {
                            return 1;
                        } else if (work1-work2<0) {
                            return -1;
                        } else {
                            return 0;
                        }
                    }
                }
            });

        //Setup full list of nodes ordered by work
        nodes=new TreeMap<Node,Object>(new Comparator() {
                public int compare(Object o1,Object o2) {
                    if(o1==o2)
                        return 0;
                    long work1=((Node)o1).work;
                    long work2=((Node)o2).work;
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
    
    /**
     * Helper function that creates Nodes for each filter and
     * populates pairs and nodes. Pairs are generated for every pair
     * of adjacent Nodes with same parent.
     */
    private void buildNodesList() {
        // make sure that dynamic rates interpreted correctly for
        // isFusable test
        SIRDynamicRateManager.pushIdentityPolicy();

        SIRIterator it = IterFactory.createFactory().createIter(str);
        it.accept(new EmptyStreamVisitor() {
                Node prevNode;
                SIRContainer prevParent;
                
                public void visitFilter(SIRFilter self,
                                        SIRFilterIter iter) {
                    if (FusePipe.isFusable(self) && StatelessDuplicate.hasMutableState(self)) {
                        Node node=new Node(self,work.getWork(self));
                        nodes.put(node,null);

                        SIRContainer parent=self.getParent();
                        if(prevNode!=null) { //Check edge condition
                            //Connect adjacent Nodes
                            prevNode.next=node;
                            node.prev=prevNode;
                            if(parent.equals(prevParent)) { //Make sure parents equal
                                Pair pair=new Pair(prevNode,node);
                                pairs.put(pair,null);
                            }
                        }
                        //Set state for next filter check
                        prevNode=node;
                        prevParent=parent;
                    }
                }
            });

        // restore dynamic rate policy
        SIRDynamicRateManager.popPolicy();
    }

    /**
     * Do fusion until target number of tiles is reached
     */
    public SIRStream doFusion() {
        int count = countStatefulFilters(str);
        System.out.println("    count = " + count);

        // Try fusing until reach target number of tiles
        while(count > numTiles) {
            Pair smallest = findSmallest();
            if(smallest == null) {
                System.out.println("    No fusable pairs!");
                break;
            }

            fuse(smallest);

            //Re-eval number of tiles needed
            count = countStatefulFilters(str);
            System.out.println("    count = " + count);
        }

        return str;
    }
    
    /**
     * Helper function that returns smallest Pair.
     */
    private Pair findSmallest() {
        if (pairs.size()==0) {
            return null;
        }

        // if there is only one key left, return it
        if (pairs.size()==1) {
            return (Pair)pairs.firstKey();
        }

        Pair smallest=(Pair)pairs.firstKey(); //Get smallest pair
        long work=smallest.work;
        ArrayList<Pair> temp=new ArrayList<Pair>();
        /* There may be several filters with smallest work though. If
         * there are prefer the ones near the top of the container.
         * This guarantees that we come out even if there is a
         * perfect fusion option. */
        boolean cont;
        do {
            cont=false;
            temp.add(smallest); //Save smallest
            pairs.remove(smallest); //Remove smallest so next smallest can be reached
            Pair newPair=(Pair)pairs.firstKey();
            long newWork=newPair.work;
            if(newWork==work) { //There are several filters with smallest work
                cont=true;
                SIRContainer parent1=smallest.n1.filter.getParent();
                SIRContainer parent2=newPair.n1.filter.getParent();
                if(parent1==parent2) {
                    //If there are several smallest in same parent
                    //prefer the ones at the top (this will let us
                    //come out even if we need to fuse 32 down to 16)
                    if(parent1.indexOf(newPair.n1.filter) < parent1.indexOf(smallest.n1.filter)) {
                        smallest=newPair;
                        work=newWork;
                    } else {
                        // remove the pair we just considered
                        pairs.remove(newPair);
                        temp.add(newPair);
                    }
                } else {
                    pairs.remove(newPair);
                    temp.add(newPair);
                }
            }
        } while(cont && pairs.size()>1);
        //Restore the Pairs temporarily removed from pairs
        Object[] fix=temp.toArray();
        for(int i=0;i<fix.length;i++)
            pairs.put(fix[i],null);
        return smallest;
    }

    /**
     * Helper function to fuse Pairs.
     */
    private void fuse(Pair p) {
        //Remove nodes
        nodes.remove(p.n1);
        nodes.remove(p.n2);
        SIRContainer parent=p.n1.filter.getParent();
        PartitionGroup part=findPartition(p,parent); //Create PartitionGroup
        //Fuse
        SIRFilter result=null;
        if(parent instanceof SIRPipeline) {
            result=(SIRFilter)FusePipe.fuse((SIRPipeline)parent,part); //Fuse
            Lifter.eliminatePipe((SIRPipeline)parent); //Eliminate pipe potentially
        } else if(parent instanceof SIRSplitJoin) {
            FuseSplit.fuse((SIRSplitJoin)parent,part); //Fuse
            result=(SIRFilter)FuseSplit.getLastFused();
            Lifter.eliminateSJ((SIRSplitJoin)parent); //Eliminate splitjoin potentially
        } else
            Utils.fail("Trying to fuse FeedBackLoop");
        //Update Rep
        Node prev=p.n1.prev;
        Node next=p.n2.next;
        Node newNode=new Node(result,WorkEstimate.getWorkEstimate(result).getWork(result));
        nodes.put(newNode,null);
        if(prev!=null) { //Fix connections
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
        if(next!=null) { //Fix connections
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

    /**
     * Helper function to create PartitionGroup corresponding to Pair
     * p in parent.
     * @param p The pair we're interested in partitioning.
     * @param parent The container to search within.
     */
    private PartitionGroup findPartition(Pair p,SIRContainer parent) {
        SIRFilter f1=p.n1.filter;
        int[] part=new int[parent.size()-1];
        for(int i=0;i<part.length;i++)
            part[i]=1;
        part[parent.indexOf(f1)]=2;
        return PartitionGroup.createFromArray(part);
    }

    /**
     * Returns the number of stateful filters in the graph.
     */
    public static int countStatefulFilters(SIRStream str) {
        // Should this count identity filters or not?  Unclear,
        // depending on backend, so for now be conservative and count
        // them.
        final int[] count = { 0 };
        IterFactory.createFactory().createIter(str).accept(new EmptyStreamVisitor() {
                public void visitFilter(SIRFilter self,
                                        SIRFilterIter iter) {
                    if(StatelessDuplicate.hasMutableState(self))
                        count[0]++;
                }});
        return count[0];
    }
    
    /**
     * Class that packages SIRFilter together with its work estimate
     * and maintains some connectivity information like prev/next
     * Node, and what Pairs it's contained in.
     */
    class Node {
	/**
	 * The SIRFilter represented by this Pair.
	 */
        SIRFilter filter;
	/**
	 * The work estimate for this filter.
	 */
        long work;
	/**
	 * Pointer to prev Node.
	 */
        Node prev;
	/**
	 * Pointer to next Node.
	 */
        Node next;
	/**
	 * Pair with previous filter.
	 */
        Pair p1;
	/**
	 * Pair with next filter.
	 */
        Pair p2;

	/**
	 * Construct new node from filter with work estimate.
	 * @param filter The filter this Node represents.
	 * @param work The work estimate of this Node.
	 */
        public Node(SIRFilter filter,long work) {
            this.filter=filter;
            this.work=work;
        }

	/**
	 * String representation. Returns filter's name.
	 */
        public String toString() {
            return filter.getName();
        }
    }

    /**
     * Pair of Nodes with associated work estimate. Right now the work
     * estimate is just the sum of the work estimates of the
     * nodes. Order maters. The first node is the previous node.
     */
    class Pair {
	/**
	 * First node in Pair.
	 */
        Node n1;
	/**
	 * Second node in Pair.
	 */	
        Node n2;
	/**
	 * Work estimate of Pair (sum of work from Nodes)
	 */
        long work;
	/**
	 * Construct Pair.
	 * @param n1 First node in Pair.
	 * @param n2 Second node in Pair.
	 */
        public Pair(Node n1,Node n2) {
            this.n1=n1;
            this.n2=n2;
            work(n1,n2);
            n1.p2=this;
            n2.p1=this;
        }

	/**
	 * Calculates work of two Node. Would be static but inner
	 * classes cannot have static declarations.
	 * @param n1 First node.
	 * @param n2 Second node.
	 */
        public void work(Node n1,Node n2) {
            work=n1.work+n2.work;
        }

	/**
	 * String representation.
	 */
        public String toString() {
            return "Pair(" + n1 + ", " + n2  + ")";
        }
    }
}
