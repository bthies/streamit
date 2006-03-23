
package at.dms.kjc.cluster;

import at.dms.kjc.flatgraph.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.*;
//import java.lang.*;
import java.util.*;

/**
 * Maintain mappings from stream operator to number. These numbers 
 * are used in NetStream objects and also in code generation to enumerate 
 * the threads from 0..(N-1)
 * 
 * Also provides the service of getting an estimate of the stack size needed to execute a given node.
 * 
 * @author Janis
 *
 */
public class NodeEnumerator implements FlatVisitor {

    static int counter = 0;
    
    static HashMap<SIROperator,Integer> nodeIds; // SIROperator --> int
    static HashMap<Integer,SIROperator> idToOperator; // int --> SIROperator
    static HashMap<Integer,FlatNode> idToFlatNode; // int --> FlatNode

    public static void reset() {
        counter = 0;
        nodeIds = new HashMap<SIROperator,Integer>();
        idToFlatNode = new HashMap<Integer,FlatNode>();
        idToOperator = new HashMap<Integer,SIROperator>();
    }

    /**
     * 
     * @return number of nodes visited by visitNode
     */
    public static int getNumberOfNodes() {
        return counter;
    }

   /**
    * Get the number for a FlatNode.
    * 
    * @param node  a FlatNode
    * @return      number for node or -1 if node has no association
    */ 
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
    
    /**
     * Used to get the SIROperator associated with a thread id.
     * 
     * @param nodeID (thread id)
     * @return  SIROperator associated with the id
     */

    public static SIROperator getOperator(int nodeID) {
        return idToOperator.get(nodeID);  //implicit case to Integer
    }

    /**
     * Get the FlatNode associated with a number.
     * 
     * @param nodeID  number.
     * @return        associated FlatNode
     */

    public static FlatNode getFlatNode(int nodeID) {
        return idToFlatNode.get(nodeID);
    }

    /**
     * Used to provide number assigned to SIROperator
     * 
     * @param f
     * @return  number associated with f, or -1 if f has no association
     */
    public static int getSIROperatorId(SIROperator f) {
    
        Integer i = nodeIds.get(f);
        if (i == null) return -1;
        return i.intValue();
    }
        

    /**
     * Needs to have root FlatNode of graph accept a NodeEnumerator to set up numbers to nodes associations.
     */
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
