
package at.dms.kjc.cluster;

import at.dms.kjc.flatgraph.*;
import at.dms.kjc.sir.*;
import java.lang.*;
import java.util.*;

public class NodeEnumerator implements FlatVisitor {

    static int counter = 0;
    
    static HashMap nodeIds;

    public static void reset() {
	counter = 0;
	nodeIds = new HashMap();
    }

    public static int getNumberOfNodes() {
	return counter;
    }

    public static int getNodeId(FlatNode node) {
    
	Integer i = (Integer)nodeIds.get(node.contents);

	return i.intValue();
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

	nodeIds.put(node.contents, new Integer(counter));

	counter++;
	
    }
}
