

package at.dms.kjc.cluster;

import at.dms.kjc.flatgraph.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.CType;
import java.lang.*;
import java.util.*;

public class RegisterStreams implements FlatVisitor {

    static HashMap filterInStreams = new HashMap();

    static HashMap filterOutStreams = new HashMap();

    public void visitNode(FlatNode node) {
    
	System.out.print("JV: Visiting node name:" + 
			 node.getName() + 
			 " getNodeid:"+
			 NodeEnumerator.getNodeId(node)+
			 "\n");

	if (node.contents instanceof SIRFilter) {

	    SIRFilter f = (SIRFilter)node.contents;

	    

	    
	    if (node.edges.length == 1 && node.edges[0] != null) {
		
		int source = NodeEnumerator.getNodeId(node);
		int dest = NodeEnumerator.getNodeId(node.edges[0]);

		System.out.println("Output type :"+f.getOutputType().toString()); 

		System.out.println("Registering Filter Out stream from:"+
				   source+" to:"+dest); 

		filterOutStreams.put(node.contents, new NetStream(source, dest));
	    }

	    if (node.incoming != null && node.incoming.length == 1) {

		int source = NodeEnumerator.getNodeId(node.incoming[0]);
		int dest = NodeEnumerator.getNodeId(node);

		System.out.println("Input type :"+f.getInputType().getTypeID()+" "+CType.TID_INT); 

		System.out.println("Registering Filter In stream from:"+
				   source+" to:"+dest); 

		filterInStreams.put(node.contents, new NetStream(source, dest));
	    }
	    
	}
    }

    public static NetStream getFilterInStream(SIRFilter filter) {
    
	return (NetStream)filterInStreams.get(filter);
    }

    public static NetStream getFilterOutStream(SIRFilter filter) {
    
	return (NetStream)filterOutStreams.get(filter);
    }


}
