package at.dms.kjc.flatgraph2;

import at.dms.kjc.sir.*;
import java.util.HashMap;

/**
 * Intermediate file used in (super) synch removal
 */
public class UnflatFilter {
    public SIRFilter filter;
    public int[] inWeights,outWeights;
    //IntList inWeights,outWeights;
    public UnflatEdge[] in;
    public UnflatEdge[][] out;
    private static int nullNum=0;
    private String name;
    private static HashMap names=new HashMap(); //Set of all names taken (String->null)
    public double[] array;
    public double constant;
    public int popCount;
    public int initMult, steadyMult;
    
    //Do not call unless not using this anymore
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

    UnflatFilter(SIRStream filter) {
	this(filter,null,null,new UnflatEdge[0],new UnflatEdge[0][0]);
    }

    UnflatFilter(SIRStream filter,UnflatEdge in,UnflatEdge out) {
	this(filter,new int[]{1},new int[]{1},new UnflatEdge[]{in},new UnflatEdge[][]{new UnflatEdge[]{out}});
    }

    public boolean isLinear() 
    {
	return array != null;
    }

    public String toString() {
	return name;
    }
}




