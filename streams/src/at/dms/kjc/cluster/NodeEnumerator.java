
package at.dms.kjc.cluster;

import at.dms.kjc.flatgraph.*;
import at.dms.kjc.sir.*;
import java.lang.*;
import java.util.*;

public class NodeEnumerator implements FlatVisitor {

    static int counter = 0;
    
    static HashMap nodeIds; // SIROperator --> int
    static HashMap idToNode; // int --> SIROperator

    public static void reset() {
	counter = 0;
	nodeIds = new HashMap();
	idToNode = new HashMap();
    }

    public static int getNumberOfNodes() {
	return counter;
    }

    public static int getNodeId(FlatNode node) {
    
	Integer i = (Integer)nodeIds.get(node.contents);

	return i.intValue();
    }

    public static SIROperator getNode(int nodeID) {
    
	return (SIROperator)idToNode.get(new Integer(nodeID));
    }

    public static int getSIROperatorId(SIROperator f) {
    
	Integer i = (Integer)nodeIds.get(f);

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

	Integer _int = new Integer(counter); 

	nodeIds.put(node.contents, _int);
	idToNode.put(_int, node.contents);

	counter++;
	
    }
}
