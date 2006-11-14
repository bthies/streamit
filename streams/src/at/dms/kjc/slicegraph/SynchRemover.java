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
        removeSynch(top);
    }
    
    private void removeSynch(FlatNode current) {
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
                removeSynch(child);
            } else { // joiner-splitter combo => remove synchronization
                FlatNode[] children = child.edges;
                doSynchRemoval(current, child);
                for (int i=0; i<children.length; i++) {
                    removeSynch(children[i]);
                }
            }
        } else { // node is not a joiner, removeSynch on all the children
            FlatNode[] children = current.edges;
            for (int i=0; i<children.length; i++) {
                removeSynch(children[i]);
            }
        }
    }
    
    private void doSynchRemoval(FlatNode joiner, FlatNode splitter) {
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
        FlatNode[] repeatedOutput = new FlatNode[lcm];
        int l = 0;
        for (int i=0; i<outMult; i++) {
            for (int j=0; j<outputs.length; j++) {
                for (int k=0; k<outWeights[j]; k++) {
                    repeatedOutput[l] = outputs[j];
                    l++;
                }
            }
        }
        
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
}
