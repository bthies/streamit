

package at.dms.kjc.cluster;

import at.dms.kjc.flatgraph.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.raw.*;
import at.dms.kjc.CType;
import java.lang.*;
import java.util.*;

public class RegisterStreams implements FlatVisitor {

    static HashMap filterInStreams = new HashMap(); 
    // SIROperator -> Vector of NetStream

    static HashMap filterOutStreams = new HashMap();
    // SIROperator -> Vector of NetStream


    public void visitNode(FlatNode node) {

	/*
	System.out.print("RegisterStreams: Visiting node name:" + 
			 node.getName() + 
			 " getNodeid:"+
			 NodeEnumerator.getNodeId(node)+
			 "\n");
	*/
			 
	
	Vector v;
	int i;

	v = new Vector();

	if (node.incoming != null) {

	    for (i = 0; i < node.incoming.length; i++) {
		
		int source = NodeEnumerator.getNodeId(node.incoming[i]);
		int dest = NodeEnumerator.getNodeId(node);
		v.add(new NetStream(source, dest));
	    }	
	}

	filterInStreams.put(node.contents, v);

	v = new Vector();

	if (node.edges != null) {

	    for (i = 0; i < node.edges.length; i++) {

		int source = NodeEnumerator.getNodeId(node);

		if (node.edges[i] != null) {

		    int dest = NodeEnumerator.getNodeId(node.edges[i]);
		    v.add(new NetStream(source, dest));
		}
	    }
	}

	filterOutStreams.put(node.contents, v);
    }

    public static NetStream getFilterInStream(SIRFilter filter) {
    
	return (NetStream)((Vector)filterInStreams.get(filter)).elementAt(0);
    }

    public static NetStream getFilterOutStream(SIRFilter filter) {
    
	return (NetStream)((Vector)filterOutStreams.get(filter)).elementAt(0);
    }


    public static Vector getNodeInStreams(SIROperator op) {
    
	return (Vector)filterInStreams.get(op);
    }

    public static Vector getNodeOutStreams(SIROperator op) {
    
	return (Vector)filterOutStreams.get(op);
    }
}
