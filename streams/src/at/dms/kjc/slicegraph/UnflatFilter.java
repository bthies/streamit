package at.dms.kjc.slicegraph;

import at.dms.kjc.sir.*;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Intermediate file used in (super) synch removal. Represents filter
 * or splitter/joiner (null filters) in original graph. Basically any
 * join point in graph. When all null filters are removed and filters
 * connect directly to each other then sych removal is done.
 * @author jasperln
 */
public class UnflatFilter {
    /**
     * Original filter this UnflatFilter is representing (if any)
     */
    public SIRFilter filter;
    /**
     * Weights on incoming edges.
     */
    public int[] inWeights;
    /**
     * Weights on outgoing edges.
     */
    public int[] outWeights;
    /**
     * Incoming UnflatEdge.
     */
    public UnflatEdge[] in;
    /**
     * Outgoing UnflatEdge. Each out[i] represents the list of filters
     * that should be sent the data corresponding to outWeights[i].
     */
    public UnflatEdge[][] out;
    /**
     * Keep track of number of null filters. Used for autonaming them.
     */
    private static int nullNum=0;
    /**
     * Name of filter.
     */
    private String name;
    /**
     * Set of all names (maps String->null) to prevent name collisions
     */
    private static HashMap names=new HashMap();
    /**
     * For linear filters, the A in its Ax+b representation.
     */
    public double[] array;
    /**
     * For linear filters, the b in its Ax+b representation.
     */
    public double constant;
    /**
     * The pop count of this filter.
     */
    public int popCount;
    /**
     * The initialization multiplicity of this filter.
     */
    public int initMult;
    /**
     * The steady state multiplicity of this filter.
     */
    public int steadyMult;
    
    /**
     * Garbage collect this UnflatFilter. Do not call until ready to
     * garbage collect whole graph.
     */
    public void clear() {
        filter=null;
        inWeights=null;
        outWeights=null;
        if(in!=null) {
            for(int i=0;i<in.length;i++)
                in[i]=null;
            in=null;
        }
        if(out!=null) {
            for(int i=0;i<out.length;i++) {
                UnflatEdge[] inner=out[i];
                if(inner!=null) {
                    for(int j=0;j<inner.length;j++)
                        inner[j]=null;
                    out[i]=null;
                }
            }
            out=null;
        }
        name=null;
    }

    public int inWeight(UnflatEdge edge) {
        int sum = 0;
        for (int i = 0; i < inWeights.length; i++) {
            if (in[i] == edge)
                sum += inWeights[i];
        }
        return sum;
    }
    
    public int totalInWeights() {
        int sum = 0;
        for (int i = 0; i < inWeights.length; i++) {
            sum += inWeights[i];
        }
        return sum;
    }
    
    public double inRatio(UnflatEdge edge) {
        return ((double)inWeight(edge)) / ((double)totalInWeights());
    }
    
    public HashSet<UnflatEdge> getInEdgeSet(){
        HashSet<UnflatEdge> inEdges = new HashSet<UnflatEdge>();
        for (int i = 0; i < in.length; i++) {
            inEdges.add(in[i]);
        }
        return inEdges;
    }
    
    
    /**
     * Construct new UnflatFilter from filter.
     * @param filter The SIRStream used to construct this UnflatFilter.
     * @param inWeights The incoming weights.
     * @param outWeights The outgoing weights.
     * @param in The incoming edges.
     * @param out The outgoing edges.
     */
    UnflatFilter(SIRStream filter,int[] inWeights,int[] outWeights,UnflatEdge[] in,UnflatEdge[][] out) {
        this.filter=(SIRFilter)filter;
        this.inWeights=inWeights;
        this.outWeights=outWeights;
        this.in=in;
        this.out=out;
        if(filter!=null) {
            name=filter.getName();
            if(names.containsKey(name)) {
                name+="_rename_"+String.valueOf(nullNum++);
            } else
                names.put(name,null);
        } else
            name=String.valueOf(nullNum++);
        for(int i=0;i<in.length;i++) {
            in[i].dest=this;
            //in[i].destIndex=i;
        }
        for(int i=0;i<out.length;i++) {
            UnflatEdge[] innerOut=out[i];
            for(int j=0;j<innerOut.length;j++)
                innerOut[j].src=this;
        }
    }

    /*UnflatFilter(SIRStream filter,int[] inWeights,IntList outWeights,UnflatEdge[] in,UnflatEdge[][] out) {
      IntList newInWeights=null;
      if(inWeights.length>0) {
      newInWeights=new IntList(inWeights[0],null,null);
      IntList cur=newInWeights;
      for(int i=1;i<inWeights.length;i++) {
      cur.next=new IntList(inWeights[i],cur,null);
      cur=cur.next;
      }
      }
      this(filter,newInWeights,outWeights,in,out);
      }*/

    /**
     * Construct new UnflatFilter from filter without doing any
     * connections.
     * @param filter The SIRStream used to construct this UnflatFilter.
     */
    public UnflatFilter(SIRStream filter) {
        this(filter,null,null,new UnflatEdge[0],new UnflatEdge[0][0]);
    }

    /**
     * Construct new UnflatFilter from filter that connects in to out.UnflatEdge
     * @param filter The SIRStream used to construct this UnflatFilter.
     * @param in The UnflatEdge to connect from.
     * @param out The UnflatEdge to connect to.
     */
    UnflatFilter(SIRStream filter,UnflatEdge in,UnflatEdge out) {
        this(filter,new int[]{1},new int[]{1},new UnflatEdge[]{in},new UnflatEdge[][]{new UnflatEdge[]{out}});
    }

    /**
     * Returns if UnflatFilter is linear.
     */
    public boolean isLinear() 
    {
        return array != null;
    }

    /**
     * Returns name of UnflatFilter.
     */
    public String toString() {
        return name;
    }
}




