
package at.dms.kjc.cluster;

import at.dms.kjc.flatgraph.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.*;
//import java.lang.*;
import java.util.*;

/**
 * Create a mapping from SIROperators and FlatNodes to unique numbers [0..n] used for thread ids.
 * 
 * Also provides the service of getting an estimate of the stack size needed to execute a given node.
 * 
 * @author Janis
 *
 */
public class NodeEnumerator {

    private static int counter = 0;
    
    private static HashMap<SIROperator,Integer> opToId;       // SIROperator --> int
    private static HashMap<FlatNode,Integer> flatToId;        // FlatNode -> int
    private static HashMap<Integer,SIROperator> idToOperator; // int --> SIROperator
    private static HashMap<Integer,FlatNode> idToFlatNode;    // int --> FlatNode

    /**
     * NodeEnumerator uses static data structures that need to be cleaned out.
     */
    public static void reset() {
        counter = 0;
        opToId = new HashMap<SIROperator,Integer>();
        flatToId = new HashMap<FlatNode,Integer>();
        idToFlatNode = new HashMap<Integer,FlatNode>();
        idToOperator = new HashMap<Integer,SIROperator>();
    }

    /**
     * Set up static data structures for NodeEnumerator
     * 
     * @param graphFlattener : we visit the nodes of the associated graph to set up our data structures.
     */
    public static void init(at.dms.kjc.flatgraph.GraphFlattener graphFlattener) {
        
        graphFlattener.top.accept(new FlatVisitor() {


        /**
         * Needs to have root FlatNode of graph accept a NodeEnumerator to set up numbers to nodes associations.
         */
        public void visitNode(FlatNode node) {

            Integer _int = new Integer(counter); 

            flatToId.put(node,_int);
            idToFlatNode.put(_int, node);
            if (! (node.contents == null)) {
                opToId.put(node.contents, _int);
                idToOperator.put(_int, node.contents);
            } else if (!(node.oldContents == null)) {
                opToId.put(node.oldContents, _int);
                idToOperator.put(_int, node.oldContents);
            }

            counter++;
        
        } } , new HashSet(), true);
 

        
    }
    /**
     * @return number of nodes for which ids were created.
     */
    public static int getNumberOfNodes() {
        return counter;
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
     * Used to get the SIROperator (filter, splitter, joiner) associated with an id number.
     * 
     * @param nodeID (thread id)
     * @return  SIROperator associated with the id, or null if no association.
     */

    public static SIROperator getOperator(int nodeID) {
        return idToOperator.get(nodeID);  //implicit case to Integer
    }

    /**
     * Get the FlatNode associated with a number.
     * 
     * @param nodeID  number.
     * @return        associated FlatNode, or null if no association.
     */

    public static FlatNode getFlatNode(int nodeID) {
        return idToFlatNode.get(nodeID);
    }

    /**
     * Get the id number assigned to SIROperator (filter, splitter, or joiner)
     * 
     * @param f
     * @return  number associated with f, or -1 if f has no association
     */
    public static int getSIROperatorId(SIROperator f) {
    
        Integer i = opToId.get(f);
        if (i == null) return -1;
        return i.intValue();
    }

    /**
     * Get the id number assigned to a FlatNode
     * 
     * @param f
     * @return  number associated with f, or -1 if f has no association
     */
    public static int getFlatNodeId(FlatNode f) {
    
        Integer i = flatToId.get(f);
        if (i == null) return -1;
        return i.intValue();
    }

    /**
     * Get offset of node in array or -1 if not there.
     */
    public static int getIdOffset(int nodeID, FlatNode[] arr) {
        for (int i = 0; i < arr.length; i++) {
            if (getFlatNodeId(arr[i]) == nodeID) {
                return i;
            }
        }
        return -1;
    }
}
