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

/**
 * Greedier partitioner that can switch back and forth between fissing
 * and fusing depending on what's more appropriate. Considers any
 * adjacent fusable filters within same container for fusion. Cannot
 * consider adjacent filters in graph but in different containers
 * since graph needs to remain structured at this stage but it would
 * be nice if this were supported at some point.
 * @author jasperln
 */
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
    private TreeMap<Node,Object> nodes; //Ordered filters (keys of type Node)
    private static final int FUSION_OVERHEAD=0; //Can adjust

    /**
     * Construct GreedierPartitioner.
     * @param str The toplevel SIRStream.
     * @param work WorkEstimate to drive fusion.
     * @param numTiles Target number of tiles.
     * @param joinersNeedTiles Indicates if joiners require a tile of thier own. (Needed on some backends.)
     */
    public GreedierPartitioner(SIRStream str,WorkEstimate work,int numTiles,boolean joinersNeedTiles) {
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
     * Helper fucntion that creates Nodes for each filter and
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
                    if (FusePipe.isFusable(self)) {
                        System.out.println("visitFilter, isFusable: " + self);
                        Node node=new Node(self,work.getWork(self));
                        nodes.put(node,null);
                        if(StatelessDuplicate.isFissable(self))
                            node.fissable=true; //Set fissable
                        SIRContainer parent=self.getParent();
                        if(prevNode!=null) { //Check edge condition
                            //Connect adjacent Nodes
                            prevNode.next=node;
                            node.prev=prevNode;
                            if(parent.equals(prevParent)) { //Make sure parents equal
                                System.out.println("created pair between " + prevNode + " and " + node);
                                Pair pair=new Pair(prevNode,node);
                                pairs.put(pair,null);
                            }
                            else {
                                System.out.println("can't create pair because different parents");
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
            //Fiss while appropriate and under target num of tiles
            System.out.println("Tiles: " + numTiles);
            while(fiss&&count<numTiles) {
                cont=true; //Some change so continue iterating
                Node big=nodes.lastKey(); //Get biggest Node
                System.out.println("  Fissing: "+big.filter);
                fiss(big); //Fiss
                //Reeval number of tiles needed and whether should fiss
                count=Partitioner.countTilesNeeded(str, joinersNeedTiles);
                fiss=shouldFiss();
                System.out.println("  GreedierPartitioner detects " + count + " tiles.");
            }
            while(count>numTiles) { //Try Fuse
                System.out.println("one pass");
                Pair smallest=findSmallest();  //Get smallest Pair
                if(smallest == null)
                    break;
                cont=true; //Some change so continue iterating
                fuse(smallest); //Fuse
                //Reeval number of tiles needed
                count=Partitioner.countTilesNeeded(str, joinersNeedTiles);
                System.out.println("  GreedierPartitioner detects " + count + " tiles.");
            }
          } while(cont); //Iterate till convergence
        return str;
    }
    
    /**
     * Helper function that returns smallest Pair.
     */
    private Pair findSmallest() {
        if (pairs.size()==0) {
            System.out.println("No fusable pairs!");
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
     * Helper function to determine if it's beneficial to fiss. Checks
     * to see if biggest filter is fissable and if fusing the 2nd
     * smallest pair would create a filter larger than the
     * bottleneck. This is because fissing will produce at least 1
     * extra filter.
     */
    private boolean shouldFiss() {
        if(nodes.size() == 0) {
            System.out.println("No nodes to fiss!");
            return false;
        }

        Node big=nodes.lastKey();
        if(!big.fissable)
        {
            System.out.println("The biggest is not fissable!");    
            return false;
        }

        if(pairs.size() == 0)
            return false;

        Pair small=(Pair)pairs.firstKey();
        pairs.remove(small);
        //Pair testSmall=(Pair)pairs.firstKey(); // Make sure 2nd smallest pair is below bottleneck
        pairs.put(small,null);
        //if (testSmall.work+FUSION_OVERHEAD>big.work)
        //    System.out.println("The biggest filter is not worth fissing!");
        return true;//testSmall.work+FUSION_OVERHEAD<big.work;
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
     * Helper function to fiss filter. shouldFiss() should have been
     * called before this method so it assumes it's safe (and
     * appropriate) to fiss largest filter.
     */
    private void fiss(Node node) {
        nodes.remove(node);
        //Fiss
        SIRSplitJoin split=StatelessDuplicate.doit(node.filter,2);
        //Update Rep
        SIRFilter filt=(SIRFilter)split.get(0);
        Node n1=new Node(filt,WorkEstimate.getWorkEstimate(filt).getWork(filt));
        nodes.put(n1,null); //Add new node for first filter
        filt=(SIRFilter)split.get(1);
        Node n2=new Node(filt,WorkEstimate.getWorkEstimate(filt).getWork(filt));
        nodes.put(n2,null); //Add new node for second filter
        n1.next=n2;
        n2.prev=n1;
        Node prev=node.prev;
        n1.fissable=true;
        n2.fissable=true;
        if(prev!=null) { //Fix connections
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
        if(next!=null) { //Fix connections
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
     * Class that packages SIRFilter together with its work estimate
     * and maintains some connectivity information like prev/next
     * Node, what Pairs it's contained in, and whether the filter is
     * fissable.
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
	 * Set if filter is fissable.
	 */
        boolean fissable;

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
