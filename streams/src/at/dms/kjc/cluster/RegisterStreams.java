

package at.dms.kjc.cluster;

import at.dms.kjc.flatgraph.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.raw.*;
import at.dms.kjc.CType;
import java.lang.*;
import java.util.*;

public class RegisterStreams implements FlatVisitor {

    static HashMap filterInStreams = new HashMap();

    static HashMap filterOutStreams = new HashMap();

    //static HashMap 

    public void visitNode(FlatNode node) {

	/*
	System.out.println();

	
	System.out.print("RegisterStreams: Visiting node name:" + 
			 node.getName() + 
			 " getNodeid:"+
			 NodeEnumerator.getNodeId(node)+
			 "\n");
	*/
			 
	if (node.contents instanceof SIRFilter) {

	    SIRFilter f = (SIRFilter)node.contents;

	    
	    if (node.incoming != null && node.incoming.length == 1) {

		int source = NodeEnumerator.getNodeId(node.incoming[0]);
		int dest = NodeEnumerator.getNodeId(node);


		/*
		System.out.print("Registering Filter In stream from:"+
				   source+" to:"+dest); 
		System.out.println(" type:"+f.getInputType().toString()); 
		*/

		filterInStreams.put(node.contents, new NetStream(source, dest));
	    }
	    
	    
	    if (node.edges.length == 1 && node.edges[0] != null) {
		
		int source = NodeEnumerator.getNodeId(node);
		int dest = NodeEnumerator.getNodeId(node.edges[0]);

		/*
		System.out.print("Registering Filter Out stream from:"+
				   source+" to:"+dest); 
		System.out.println(" type:"+f.getOutputType().toString()); 
		*/
		
		filterOutStreams.put(node.contents, new NetStream(source, dest));
	    }
	}


	if (node.contents instanceof SIRSplitter) {
	
	    CType baseType = Util.getBaseType(Util.getOutputType(node));

	    //System.out.println(" basetype:"+baseType.toString()); 
	    
	    if (node.incoming != null && node.incoming.length == 1) {

		int source = NodeEnumerator.getNodeId(node.incoming[0]);
		int dest = NodeEnumerator.getNodeId(node);


		//System.out.println("Registering Filter In stream from:"+
		//		   source+" to:"+dest); 

		filterInStreams.put(node.contents, new NetStream(source, dest));
	    }

	    Vector v = new Vector();

	    for (int i = 0; i < node.edges.length; i++) {

		int source = NodeEnumerator.getNodeId(node);
		int dest = NodeEnumerator.getNodeId(node.edges[i]);

		//System.out.println("Registering Filter Out stream from:"+
		//		   source+" to:"+dest); 

		v.add(new NetStream(source, dest));
		
	    }

	    filterOutStreams.put(node.contents, v);

	}


	if (node.contents instanceof SIRJoiner) {
	
	    CType baseType = Util.getBaseType(Util.getJoinerType(node));

	    //System.out.println(" basetype:"+baseType.toString()); 

	    Vector v = new Vector();
	    
	    for (int i = 0; i < node.incoming.length; i++) {

		int source = NodeEnumerator.getNodeId(node.incoming[i]);
		int dest = NodeEnumerator.getNodeId(node);


		//System.out.println("Registering Filter In stream from:"+
		//		   source+" to:"+dest); 
		
		v.add(new NetStream(source, dest));
		
	    }

	    filterInStreams.put(node.contents, v);

	    if (node.edges.length == 1 && node.edges[0] != null) {
		
		int source = NodeEnumerator.getNodeId(node);
		int dest = NodeEnumerator.getNodeId(node.edges[0]);

		//System.out.println("Registering Filter Out stream from:"+
		//		   source+" to:"+dest); 

		filterOutStreams.put(node.contents, new NetStream(source, dest));
	    }
	}

    }

    public static NetStream getFilterInStream(SIRFilter filter) {
    
	return (NetStream)filterInStreams.get(filter);
    }

    public static NetStream getFilterOutStream(SIRFilter filter) {
    
	return (NetStream)filterOutStreams.get(filter);
    }


    public static Object getNodeInStreams(SIROperator op) {
    
	return filterInStreams.get(op);
    }

    public static Object getNodeOutStreams(SIROperator op) {
    
	return filterOutStreams.get(op);
    }
}
