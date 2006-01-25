
package at.dms.kjc.cluster;

import at.dms.kjc.flatgraph.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.*;
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

    /**
     * Returns an estimate of the maximum stack size needed to execute
     * a given node.
     *  
     *  @param   nodeID   ID of node in question
     *
     *  @return  Estimated number of bytes needed for stack
     */
    public static int getStackSize(int nodeID) {
        // the base allocation for any node (e.g., a splitter or
        // joiner) in bytes
        int BASE_ALLOCATION = 1*1024*1024;
        // expand our estimate by this ratio, to give ourselves some slack
        double EXPANSION_RATIO = 1.1;

        SIROperator node = getOperator(nodeID);
        if (!(node instanceof SIRStream)) {
            // plain SIROperators do not have methods, so just return
            // base allocation
            return BASE_ALLOCATION;
        } else {
            // otherwise, estimate stack size from methods in stream...
            SIRStream str = (SIRStream)node;
            // for now, return the sum of all the variables allocated in
            // the methods of this operator.  This is an over-estimate,
            // but allows the functions to safely call each other.
            final int[] result = {0};
            JMethodDeclaration[] methods = str.getMethods();
            for (int i=0; i<methods.length; i++) {
                methods[i].accept(new SLIREmptyVisitor() {
                        public void visitFormalParameters(JFormalParameter self,
                                                          boolean isFinal,
                                                          CType type,
                                                          String ident) {
                            // add size of each formal param
                            result[0] += type.getSizeInC();
                        }
            
                        public void visitVariableDefinition(JVariableDefinition self,
                                                            int modifiers,
                                                            CType type,
                                                            String ident,
                                                            JExpression expr) {
                            // add size of each variable def
                            result[0] += type.getSizeInC();
                        }
                    });
            }
        
            // expand to allow some room for error (interaction with gcc, etc.)
            float exactEstimate = result[0];
            int estimateWithSlack = BASE_ALLOCATION + (int)(EXPANSION_RATIO*exactEstimate);
        
            return estimateWithSlack;
        }
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
