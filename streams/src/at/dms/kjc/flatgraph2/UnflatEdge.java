package at.dms.kjc.flatgraph2;

import at.dms.util.Utils;

/**
 * Intermediate file used in (super) synch removal
 */
public class UnflatEdge {
    UnflatFilter src;
    UnflatFilter dest;
    //int virtualPort;

    UnflatEdge() {
	//virtualPort=0;
    }

    UnflatEdge(UnflatFilter src,UnflatFilter dest) {
	this.src=src;
	this.dest=dest;
    }
    
    void connect(UnflatEdge newDest) {
	dest=newDest.dest;
	//virtualPort=newDest.virtualPort;
	UnflatEdge[] in=dest.in;
	for(int i=0;i<in.length;i++)
	    if(in[i]==newDest)
		in[i]=this;
    }

    public static boolean equal(UnflatEdge[] e1,UnflatEdge[] e2) {
	if(e1.length!=e2.length)
	    return false;
	for(int i=0;i<e1.length;i++)
	    if(e1[i]!=e2[i])
		return false;
	return true;
    }
    
    public String toString() {
	return src+"->"+dest+" "+super.toString();
    }

    /*void connect(UnflatEdge newDest) {
      dest=newDest.dest;
      destIndex=newDest.destIndex;
      dest.in[destIndex]=this;
      }*/
    
    /*UnflatEdge(UnflatFilter filter) {
	filter1=filter;
	}*/
    
    /*void attach(UnflatFilter filter) {
	if(filter1!=null)
	    filter1=filter;
	else if(filter2!=null)
	    filter2=filter;
	else
	    Utils.fail("Cannot attach more than 2 UnflatFilters to an UnflatEdge.");
    }
    
    void connect(UnflatFilter filter,UnflatFilter to) {
	if(filter1==filter)
	    filter2=to;
	else if(filter2==filter)
	    filter1=to;
	else
	    Utils.fail("Incorrect source filter to connect.");
	    }*/
    
    /*UnflatFilter dest(UnflatFilter filter) {
	if(filter1==filter)
	    return filter2;
	else if(filter2==filter)
	    return filter1;
	else
	    Utils.fail("Incorrect source filter to find destination.");
	    }*/
    
    /*static void merge(UnflatEdge e1,UnflatEdge e2) {
      if(e1.filter1!=null) {
      UnflatEdge[] in=e1.filter1.in;
      if(in!=null)
      for(int i=0;i<in.length;i++) {
      UnflatEdge edge=in[i];
      if(edge==e1) {
      in[i]=e2;
      return;
      }
      }
	} else if(e1.filter2!=null) {
	    UnflatEdge[] in=e1.filter2.in;
	    if(in!=null)
		for(int i=0;i<in.length;i++) {
		    UnflatEdge edge=in[i];
		    if(edge==e1) {
			in[i]=e2;
			return;
		    }
		}
	} else if(e2.filter1!=null) {
	    UnflatEdge[] in=e2.filter1.in;
	    if(in!=null)
		for(int i=0;i<in.length;i++) {
		    UnflatEdge edge=in[i];
		    if(edge==e2) {
			in[i]=e1;
			return;
		    }
		}
	} else if(e2.filter2!=null) {
	    UnflatEdge[] in=e2.filter2.in;
	    if(in!=null)
		for(int i=0;i<in.length;i++) {
		    UnflatEdge edge=in[i];
		    if(edge==e1) {
			in[i]=e2;
			return;
		    }
		}
	}
	Utils.fail("Could not merge.");
	}*/
}

