package at.dms.kjc.slicegraph;

import java.util.LinkedList;
import java.util.List;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.GraphFlattener;

public class SynchRemover {

    private GraphFlattener gf;
    private FlatNode top;
    
    public SynchRemover(GraphFlattener gf) {
        this.gf = gf;
        this.top = gf.top;
        removeSplitterSync(top);
        removeJoinerSplitterSynch(top);
    }
    
    private void removeSplitterSync(FlatNode current) {
        if (current == null)
            return;
        // If it's a splitter, check if any of its children are also splitters,
        // and if so, merge them
        if (current.isSplitter()) {
            mergeSplitters(current);
        }
        // Either way, recursively call on all of the (possibly updated) children
        for (int i=0; i<current.edges.length; i++) {
            removeSplitterSync(current.edges[i]);
        }
    }
    
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
                int partialMult = lcm(child.getTotalOutgoingWeights(),
                                      splitter.weights[i])/splitter.weights[i];
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
            createWeights(repeatedOutput, newWeights, newEdges);
            splitter.edges = newEdges;
            splitter.weights = newWeights;
        }
    }
    
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
        int inMult = inWeightsSum/lcm;
        // # of times outputs are repeated
        int outMult = outWeightsSum/lcm;
        
        // Create a new array of outputs with all of the repeated outputs
        // duplicated explicitly. This will make connecting new edges easier
        // later.
        FlatNode[] repeatedOutput = 
            makeRepeatedArray(outputs, outWeights, outWeightsSum, outMult); 
        
        // For each input, lists that store its new outgoing edges and weights
        List<FlatNode> newEdges = new LinkedList<FlatNode>();
        List<Integer> newWeights = new LinkedList<Integer>();
        
        // offset from 0 of the current input being operated on
        int offset = 0;
        // for each input...
        for (int i=0; i<inputs.length; i++) {
            // the number of consecutive times the same output has appeared for
            // a given input
            int count = 0;
            int currWeight = inWeights[i];
            // The previous output node that was paired with this input
            // Initialize to the first output for this input
            FlatNode prevEdge = repeatedOutput[offset];
            // for each repetition of the inputs...
            for (int j=0; j<inMult; j++) {
                // the index to start at for this input and this repetition
                int startIndex = j*inWeightsSum + offset;
                for (int k=0; k<currWeight; k++) {
                    // Get the matching output from the array built earlier
                    FlatNode newEdge = repeatedOutput[startIndex+k];
                    // if its the same as the previous output, increment count
                    // and keep going
                    if (newEdge == prevEdge) {
                        count++;
                    } else { // otherwise, we've reached a new output
                        // add new edges and weights
                        newEdges.add(prevEdge);
                        newWeights.add(new Integer(count));
                        // and reset prevEdge and count
                        prevEdge = newEdge;
                        count = 1;
                    }
                }
            }
            // There will be one last output left "hanging" at the end of the
            // loop. Add it now.
            newEdges.add(prevEdge);
            newWeights.add(new Integer(count));
            
            // Update the outputs of this input
            FlatNode currNode = inputs[i];
            currNode.edges = newEdges.toArray(new FlatNode[0]);
            
            int[] newWeightsArray = new int[newWeights.size()];
            for (int n=0; n<newWeights.size(); n++) {
                newWeightsArray[n] = newWeights.get(n).intValue();
            }
            currNode.weights = newWeightsArray;
            currNode.ways = newEdges.size();
            
            // increase the offset for the next input
            offset += currWeight;
        }
        
        // clear the joiner and splitter because they're no longer needed
        joiner.removeEdges();
        joiner.removeIncoming();
        splitter.removeEdges();
        splitter.removeIncoming();
    }
    
    private static int lcm(int a, int b) {
        return a*b/gcd(a,b);
    }
    
    private static int gcd(int a, int b) {
        if (a%b == 0)
            return b;
        else return gcd(b, a%b);
    }

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

    private static void createWeights(FlatNode[] repeatedOutput, int[] newWeights,
            FlatNode[] newEdges) {
        
        if (repeatedOutput == null || repeatedOutput.length == 0)
            return;
        
        List<FlatNode> newEdgesList = new LinkedList<FlatNode>();
        List<Integer> newWeightsList = new LinkedList<Integer>();
        
        FlatNode prev = repeatedOutput[0];
        int count = 0;
        for (int i=0; i<repeatedOutput.length; i++) {
            FlatNode curr = repeatedOutput[i];
            if (curr == prev) {
                count++;
            } else {
                newEdgesList.add(prev);
                newWeightsList.add(new Integer(count));
                prev = curr;
                count = 1;
            }
        }
        newEdgesList.add(prev);
        newWeightsList.add(new Integer(count));
        
        newEdges = newEdgesList.toArray(new FlatNode[0]);
        
        newWeights = new int[newWeightsList.size()];
        for (int n=0; n<newWeightsList.size(); n++) {
            newWeights[n] = newWeightsList.get(n).intValue();
        }
    }
}