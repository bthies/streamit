
package at.dms.kjc.cluster;

import at.dms.kjc.flatgraph.*;
import at.dms.kjc.sir.*;
import java.lang.*;
import java.util.*;

public class NodeEnumerator implements FlatVisitor {

    static int counter = 0;
    
    static HashMap nodeIds; // SIROperator --> int
    static HashMap idToOperator; // int --> SIROperator
    static HashMap idToFlatNode; // int --> SIROperator

    public static void reset() {
	counter = 0;
	nodeIds = new HashMap();
	idToFlatNode = new HashMap();
	idToOperator = new HashMap();
    }

    public static int getNumberOfNodes() {
	return counter;
    }

    public static int getNodeId(FlatNode node) {
    
	return getSIROperatorId(node.contents);
    }

    public static SIROperator getOperator(int nodeID) {
    
	return (SIROperator)idToOperator.get(new Integer(nodeID));
    }

    public static FlatNode getFlatNode(int nodeID) {
    
	return (FlatNode)idToFlatNode.get(new Integer(nodeID));
    }

    public static int getSIROperatorId(SIROperator f) {
    
	Integer i = (Integer)nodeIds.get(f);
	if (i == null) return -1;
	return i.intValue();
    }
        

    public void visitNode(FlatNode node) {

	/*
	System.out.print("NodeEnumerator: Visiting node name:" + 
			 node.getName() + 
			 " id:"+
			 (counter)+
			 "\n");
	*/

	if (node.contents instanceof SIRSplitter) {
	    if (((SIRSplitter)node.contents).getSumOfWeights() == 0) {
		// The splitter is not doing any work
		return;
	    }
	}

	if (node.contents instanceof SIRJoiner) {
	    if (((SIRJoiner)node.contents).getSumOfWeights() == 0) {
		// The joiner is not doing any work
		return;
	    }
	}

	Integer _int = new Integer(counter); 

	nodeIds.put(node.contents, _int);
	idToFlatNode.put(_int, node);
	idToOperator.put(_int, node.contents);

	counter++;
	
    }
}
