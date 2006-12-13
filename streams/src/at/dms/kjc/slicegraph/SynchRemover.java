package at.dms.kjc.slicegraph;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.GraphFlattener;

public class SynchRemover {

    private GraphFlattener gf;
    private FlatNode top;
    
    public SynchRemover(GraphFlattener gf) {
        this.gf = gf;
        this.top = gf.top;
        // First pass through merges and Splitter-Splitter combos and
        // Joiner-Joiner combos
        removeSplitterJoinerSync(top);
        // Second pass eliminates Joiner-Splitter combos
        removeJoinerSplitterSynch(top);
    }
    
    // --------- Joiner-Joiner & Splitter-Splitter --------------------------
    
    // Combines splitters with splitters and joiners with joiners.
    // Recursively calls itself to traverse the entire graph.
    private void removeSplitterJoinerSync(FlatNode current) {
        if (current == null)
            return;
        // If it's a splitter, check if any of its children are also splitters,
        // and if so, merge them
        if (current.isSplitter()) {
            mergeSplitters(current);
        }
        // same for joiners
        else if (current.isJoiner()) {
            mergeJoiners(current);
        }
        
        // Either way, recursively call on all of the (possibly updated) children
        for (int i=0; i<current.edges.length; i++) {
            removeSplitterJoinerSync(current.edges[i]);
        }
    }
        
    // If the current splitter has any children that are also splitters, merge
    // them together. Recursively calls itself to merge long strings of splitters.
    private void mergeSplitters(FlatNode splitter) {
        FlatNode[] children = splitter.edges;
        // If there are any consecutive splitters, stores the children of the
        // second splitter
        FlatNode[][] grandchildren = new FlatNode[children.length][];
        
        boolean hasChildSplitter = false;
        // number of times to repeat output to reach a recurrence; default to 1
        int mult = 1;
        
        // First, see if there are any children which are also splitters
        for (int i=0; i<children.length; i++) {
            FlatNode child = children[i];
            if (child.isSplitter()) {
                hasChildSplitter = true;
                // Recursively merge in case there is a long string of splitters
                mergeSplitters(child);
                
                // number of times this particular child splitter needs to be
                // repeated
                int lcm = lcm(child.getTotalOutgoingWeights(), splitter.weights[i]);
                int partialMult = lcm/splitter.weights[i];
                // overall number of repeats needed for all children
                mult = lcm(mult, partialMult);
                
                // explicitly list out each grandchild according to its weight
                // to make merging easier later
                grandchildren[i] = makeRepeatedArray(child.edges, child.weights,
                                                     child.getTotalOutgoingWeights(),
                                                     1);
            }
        }
        // If there are no child splitters, then we're done
        if (!hasChildSplitter)
            return;
        // Otherwise, there is work to be done...
        else {
            
            // Update each granchild's incoming edges to point to the
            // grandparent splitter
            for (int i=0; i<splitter.ways; i++) {
                FlatNode child = splitter.edges[i];
                int lcm = mult*splitter.weights[i];
                if (child.isSplitter()) {
                    for (int j=0; j<child.ways; j++) {
                        FlatNode grandchild = child.edges[j];
                        int newWeight = lcm/child.getTotalOutgoingWeights()*child.weights[j];
                        grandchild.incoming = new FlatNode[]{splitter};
                        grandchild.incomingWeights = new int[]{newWeight};
                    }
                } else {
                    child.incoming = new FlatNode[]{splitter};
                    child.incomingWeights = new int[]{mult*splitter.weights[i]};
                }
            }
            
            int sumWeights = splitter.getTotalOutgoingWeights();
            // Explicitly list out each child according to its weight. This list
            // is repeated the required number of times in order to reach a
            // recurrence. Any occurrences of child splitters will be
            // substituted for the non-splitter grandchildren later.
            FlatNode[] repeatedOutput = 
                makeRepeatedArray(children, splitter.weights, 
                                  sumWeights, mult);
            
            // Substitute each child splitter for the appropriate non-splitter
            // grandchild. Operate on each child in turn. 
            // Offset is the offset from 0 for each child.
            int offset = 0;
            for (int i=0; i<children.length; i++) {
                FlatNode child = children[i];
                if (child.isSplitter()) {
                    // Keeps track of the grandchild we're on
                    int count = 0;
                    int weight = splitter.weights[i];
                    FlatNode[] currGrandchildren = grandchildren[i];
                    for (int j=0; j<mult; j++) {
                        for (int k=0; k<weight; k++) {
                            // Replace the splitter with the correct grandchild
                            repeatedOutput[j*sumWeights + offset + k] =
                                currGrandchildren[count%currGrandchildren.length];
                            count++;
                        }
                    }
                    // remove edges for this merged splitter
                    child.removeEdges();
                    child.removeIncoming();
                }
                offset += splitter.weights[i];
            }
            
            // Consolidate outputs into new edges and weights
            int[] newWeights = null;
            FlatNode[] newEdges = null;
            createNewEdgesWeightsArray(repeatedOutput, newWeights, newEdges);
            splitter.edges = newEdges;
            splitter.weights = newWeights;
            splitter.ways = newEdges.length;
        }
    }
    
    // Uses the same algorithm as for splitters to merge consecutive joiners
    private void mergeJoiners(FlatNode joiner) {
        FlatNode[] parents = joiner.incoming;
        FlatNode[][] grandparents = new FlatNode[parents.length][];
        
        boolean hasParentJoiner = false;
        int mult = 1;
        
        for (int i=0; i<parents.length; i++) {
            FlatNode parent = parents[i];
            if (parent.isJoiner()) {
                hasParentJoiner = true;
                mergeJoiners(parent);
                
                int lcm = lcm(parent.getTotalIncomingWeights(), joiner.incomingWeights[i]);
                int partialMult = lcm/joiner.incomingWeights[i];
                mult = lcm(mult, partialMult);
                
                grandparents[i] = makeRepeatedArray(parent.incoming, parent.incomingWeights,
                                                    parent.getTotalIncomingWeights(), 1);
            }
        }
        
        if (!hasParentJoiner)
            return;
        else {
            for (int i=0; i<joiner.inputs; i++) {
                FlatNode parent = joiner.incoming[i];
                int lcm = mult*joiner.incomingWeights[i];
                if (parent.isJoiner()) {
                    for (int j=0; j<parent.inputs; j++) {
                        FlatNode grandparent = parent.incoming[j];
                        int newWeight = lcm/parent.getTotalIncomingWeights()*parent.incomingWeights[j];
                        grandparent.edges= new FlatNode[]{joiner};
                        grandparent.weights = new int[]{newWeight};
                    }
                } else {
                    parent.edges = new FlatNode[]{joiner};
                    parent.weights = new int[]{mult*joiner.incomingWeights[i]};
                }
            }
            
            int sumWeights = joiner.getTotalIncomingWeights();
            FlatNode[] repeatedOutput = 
                makeRepeatedArray(parents, joiner.incomingWeights, 
                                  sumWeights, mult);
            
            int offset = 0;
            for (int i=0; i<parents.length; i++) {
                FlatNode parent = parents[i];
                if (parent.isJoiner()) {
                    int count = 0;
                    int weight = joiner.incomingWeights[i];
                    FlatNode[] currGrandparent = grandparents[i];
                    for (int j=0; j<mult; j++) {
                        for (int k=0; k<weight; k++) {
                            repeatedOutput[j*sumWeights + offset + k] =
                                currGrandparent[count%currGrandparent.length];
                            count++;
                        }
                    }
                    parent.removeEdges();
                    parent.removeIncoming();
                }
                offset += joiner.incomingWeights[i];
            }
            
            int[] newWeights = null;
            FlatNode[] newEdges = null;
            createNewEdgesWeightsArray(repeatedOutput, newWeights, newEdges);
            joiner.incoming = newEdges;
            joiner.incomingWeights = newWeights;
            joiner.inputs = newEdges.length;
        }
    }
    
    
    // -------------------- Joiner-Splitter ----------------------------------
    
    // Eliminates Joiner-Splitter combos by directly connecting the nodes
    // joined by the joiner to the nodes split by the splitter, bypassing
    // the joiner and splitter
    private void removeJoinerSplitterSynch(FlatNode current) {
        // null graph, or reached the end of a branch => return
        if (current == null)
            return;
        if (current.isJoiner()) {
            FlatNode child = current.edges[0];
            // no child => done with branch, return
            if (child == null)
                return;
            // not a joiner-splitter combo => removeSynch on child
            if (!child.isSplitter()) {
                removeJoinerSplitterSynch(child);
            } else { // joiner-splitter combo => remove synchronization
                FlatNode[] children = child.edges;
                doJoinerSplitterSynchRemoval(current, child);
                for (int i=0; i<children.length; i++) {
                    removeJoinerSplitterSynch(children[i]);
                }
            }
        } else { // node is not a joiner, removeSynch on all the children
            FlatNode[] children = current.edges;
            for (int i=0; i<children.length; i++) {
                removeJoinerSplitterSynch(children[i]);
            }
        }
    }
    
    private void doJoinerSplitterSynchRemoval(FlatNode joiner, FlatNode splitter) {
        FlatNode[] inputs = joiner.incoming;
        FlatNode[] outputs = splitter.edges;
        int[] inWeights = joiner.incomingWeights;
        int[] outWeights = splitter.weights;
        int inWeightsSum = joiner.getTotalIncomingWeights();
        int outWeightsSum = splitter.getTotalOutgoingWeights();
        
        // Will need to repeat inputs and outputs an integral number of times
        // to get a recurrence
        int lcm = lcm(inWeightsSum, outWeightsSum);
        // # of times inputs are repeated
        int inMult = lcm/inWeightsSum;
        // # of times outputs are repeated
        int outMult = lcm/outWeightsSum;
        
        // Create a new array of inputs with all of the repeated inputs
        // duplicated explicitly.
        FlatNode[] repeatedInput = 
            makeRepeatedArray(inputs, inWeights, inWeightsSum, inMult);
        
        // Create a new array of outputs with all of the repeated outputs
        // duplicated explicitly.
        FlatNode[] repeatedOutput = 
            makeRepeatedArray(outputs, outWeights, outWeightsSum, outMult); 
        
        // Map each input node to a list of output nodes it will be connected to
        // in the recurrence. This is to update each input node's outgoing edges
        // and weights.
        Map<FlatNode,List<FlatNode>> inToOutMap = new HashMap<FlatNode,List<FlatNode>>();
        
        // Map each output node to a list of input nodes that connect to it in
        // the recurrencde. This is to update each output node's incoming edges
        // and weights.
        Map<FlatNode,List<FlatNode>> outToInMap = new HashMap<FlatNode,List<FlatNode>>();
        
        // Iterate over all of the repeated inputs and outputs to populate the
        // two maps
        for (int i=0; i<repeatedOutput.length; i++) {
            FlatNode inNode = repeatedInput[i];
            FlatNode outNode = repeatedOutput[i];

            // If this is a new input node, create a new associated list for it
            if (!inToOutMap.containsKey(inNode)) {
                inToOutMap.put(inNode, new LinkedList<FlatNode>());
            }
            // Add the output node to the associated input node's list
            inToOutMap.get(inNode).add(outNode);
            
            // If this is a new output node, create a new associated list for it
            if (!outToInMap.containsKey(outNode)) {
                outToInMap.put(outNode, new LinkedList<FlatNode>());
            }
            // Add the input node to the associated output node's list
            outToInMap.get(outNode).add(inNode);
        }
        
        for (FlatNode inNode : inToOutMap.keySet()) {
            List<FlatNode> outputList = inToOutMap.get(inNode);
            // Convert the raw list of outputs to the arrays for outgoing edges
            // and weights
            createNewEdgesWeightsArray(outputList.toArray(new FlatNode[0]), 
                    inNode.weights, inNode.edges);
            inNode.ways = inNode.edges.length;
        }
        
        for (FlatNode outNode : outToInMap.keySet()) {
            List<FlatNode> inputList = outToInMap.get(outNode);
            // Convert the raw list of inputs to the arrays for incoming edges
            // and weights
            createNewEdgesWeightsArray(inputList.toArray(new FlatNode[0]), 
                    outNode.incomingWeights, outNode.incoming);
            outNode.inputs = outNode.incoming.length;
        }
        
//        // For each input, lists that store its new outgoing edges and weights
//        List<FlatNode> newEdges = new LinkedList<FlatNode>();
//        List<Integer> newWeights = new LinkedList<Integer>();
//        
//        // offset from 0 of the current input being operated on
//        int offset = 0;
//        // for each input...
//        for (int i=0; i<inputs.length; i++) {
//            // the number of consecutive times the same output has appeared for
//            // a given input
//            int count = 0;
//            int currWeight = inWeights[i];
//            // The previous output node that was paired with this input
//            // Initialize to the first output for this input
//            FlatNode prevEdge = repeatedOutput[offset];
//            // for each repetition of the inputs...
//            for (int j=0; j<inMult; j++) {
//                // the index to start at for this input and this repetition
//                int startIndex = j*inWeightsSum + offset;
//                for (int k=0; k<currWeight; k++) {
//                    // Get the matching output from the array built earlier
//                    FlatNode newEdge = repeatedOutput[startIndex+k];
//                    // if its the same as the previous output, increment count
//                    // and keep going
//                    if (newEdge == prevEdge) {
//                        count++;
//                    } else { // otherwise, we've reached a new output
//                        // add new edges and weights
//                        newEdges.add(prevEdge);
//                        newWeights.add(new Integer(count));
//                        // and reset prevEdge and count
//                        prevEdge = newEdge;
//                        count = 1;
//                    }
//                }
//            }
//            // There will be one last output left "hanging" at the end of the
//            // loop. Add it now.
//            newEdges.add(prevEdge);
//            newWeights.add(new Integer(count));
//            
//            // Update the outputs of this input
//            FlatNode currNode = inputs[i];
//            currNode.edges = newEdges.toArray(new FlatNode[0]);
//            
//            int[] newWeightsArray = new int[newWeights.size()];
//            for (int n=0; n<newWeights.size(); n++) {
//                newWeightsArray[n] = newWeights.get(n).intValue();
//            }
//            currNode.weights = newWeightsArray;
//            currNode.ways = newEdges.size();
//            
//            // increase the offset for the next input
//            offset += currWeight;
//        }
        
        // clear the joiner and splitter because they're no longer needed
        joiner.removeEdges();
        joiner.removeIncoming();
        splitter.removeEdges();
        splitter.removeIncoming();
    }
    
    
    // ---------------------- Util methods ---------------------------------
    
    private static int lcm(int a, int b) {
        return a*b/gcd(a,b);
    }
    
    private static int gcd(int a, int b) {
        if (a%b == 0)
            return b;
        else return gcd(b, a%b);
    }

    /**
     * Creates an array that expands a list of nodes and associated weights by
     * explicitly listing each node  number of times equal to its weight.
     * This entire array is possibly repeated an integral number of times.
     * For example:
     * nodes = [A,B,C], weights = [2,3,1], sumWeights = 6, mult = 3
     * => [A,A,B,B,B,C,A,A,B,B,B,C,A,A,B,B,B,C]
     * 
     * @param nodes Array of nodes to be expanded
     * @param weights Array of corresponding weights for each node
     * @param sumWeights Sum of all of the weights
     * @param mult The number of times to repeat the entire array
     * @return
     */
    private static FlatNode[] makeRepeatedArray(FlatNode[] nodes, int[] weights,
            int sumWeights, int mult) {
        FlatNode[] repeatedOutput = new FlatNode[sumWeights*mult];
        int l = 0;
        for (int i=0; i<mult; i++) {
            for (int j=0; j<nodes.length; j++) {
                for (int k=0; k<weights[j]; k++) {
                    repeatedOutput[l] = nodes[j];
                    l++;
                }
            }
        }
        return repeatedOutput;
    }

    /**
     * Creates arrays for the new edges and weights, given an array of FlatNodes
     * that explicitly lists each occurrence of the flatnode in the schedule.
     * For example:
     * [A,A,B,B,B,C] => newEdges = [A,B,C], newWeights = [2,3,1]
     * 
     * @param repeatedOutput The array of repeated nodes
     * @param newWeights The array to store the condensed weights
     * @param newEdges The array to store the condensed nodes
     */
    private static void createNewEdgesWeightsArray(FlatNode[] repeatedOutput,
            int[] newWeights, FlatNode[] newEdges) {
        
        if (repeatedOutput == null || repeatedOutput.length == 0)
            return;
        
        List<FlatNode> newEdgesList = new LinkedList<FlatNode>();
        List<Integer> newWeightsList = new LinkedList<Integer>();
        
        // Keeps track of the previous node in order to count the number of
        // repeated nodes
        FlatNode prev = repeatedOutput[0];
        int count = 0;
        for (int i=0; i<repeatedOutput.length; i++) {
            FlatNode curr = repeatedOutput[i];
            // If the current node is the same as the previous, increment count
            if (curr == prev) {
                count++;
            } else {
                // The current node is different from the previous node
                // Add the previous group of nodes to the edge and weight lists
                newEdgesList.add(prev);
                newWeightsList.add(new Integer(count));
                // Reset prev and count for the next group
                prev = curr;
                count = 1;
            }
        }
        // The last group of nodes will not be added in the loop. Add it now.
        newEdgesList.add(prev);
        newWeightsList.add(new Integer(count));
        
        // convert to array
        newEdges = newEdgesList.toArray(new FlatNode[0]);
        
        // convert to array
        newWeights = new int[newWeightsList.size()];
        for (int n=0; n<newWeightsList.size(); n++) {
            newWeights[n] = newWeightsList.get(n).intValue();
        }
    }
}