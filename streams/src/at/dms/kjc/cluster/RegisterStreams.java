

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
	
	CType input_t = null, output_t = null;

	SIROperator operator = node.contents;

	try {
	    if (operator instanceof SIRJoiner) {
		input_t = Util.getBaseType(Util.getJoinerType(node));
		output_t = Util.getBaseType(Util.getJoinerType(node));
	    }
	} catch (Exception ex) {}

	try {
	    if (operator instanceof SIRSplitter) {
		input_t = Util.getBaseType(Util.getOutputType(node));
		output_t = Util.getBaseType(Util.getOutputType(node));
	    }
	} catch (Exception ex) {}

	if (operator instanceof SIRStream) {
	    SIRStream stream = (SIRStream)operator;
	    input_t = stream.getInputType();
	    output_t = stream.getOutputType();
	}

	Vector v;
	int i;

	v = new Vector();

	if (node.incoming != null) {

	    int dest = NodeEnumerator.getNodeId(node);
	    for (i = 0; i < node.incoming.length; i++) {
		
		if (node.incoming[i] != null) {
		    
		    int source = NodeEnumerator.getNodeId(node.incoming[i]);
		    
		    if (source != -1 && dest != -1) {
			v.add(new NetStream(source, dest, input_t));
			
		    }	
		}
	    }
	}

	filterInStreams.put(node.contents, v);

	v = new Vector();

	if (node.edges != null) {

	    int source = NodeEnumerator.getNodeId(node);
	    for (i = 0; i < node.edges.length; i++) {

		if (node.edges[i] != null) {

		    int dest = NodeEnumerator.getNodeId(node.edges[i]);

		    if (source != -1 && dest != -1) {
			v.add(new NetStream(source, dest, output_t));
		    }
		}
	    }
	}

	filterOutStreams.put(node.contents, v);
    }

    public static NetStream getFilterInStream(SIRFilter filter) {
    	Vector v = (Vector)filterInStreams.get(filter);
	if (v.size() == 0) return null; else return (NetStream)v.elementAt(0);
    }

    public static NetStream getFilterOutStream(SIRFilter filter) {
    	Vector v = (Vector)filterOutStreams.get(filter);
	if (v.size() == 0) return null; else return (NetStream)v.elementAt(0);
    }


    public static Vector getNodeInStreams(SIROperator op) {
    
	return (Vector)filterInStreams.get(op);
    }

    public static Vector getNodeOutStreams(SIROperator op) {
    
	return (Vector)filterOutStreams.get(op);
    }
}


