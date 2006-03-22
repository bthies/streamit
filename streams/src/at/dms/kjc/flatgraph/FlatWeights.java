package at.dms.kjc.flatgraph;
import java.util.*;

/**
 * Need ability to itereate over FlatGraph edges that correspond with 
 * non-zero weight edges for splittes and joiners.
 * 
 * @author Allyn
 */
public final class FlatWeights implements Iterable<FlatWeight>{
    private Vector<FlatNode> nodes;
    private int[] weights;
 
    private final int nodesLen;
    private final int weightsLen;
    
    /**
     * Set up data for later use by iterator.
     * 
     * @param nodeEdges   from edges for spltter / incoming for joiner
     * @param weights     from getWeights for splitter / joiner
     */
    public FlatWeights(Vector<FlatNode> nodeEdges, int[] weights) {
        nodesLen = nodeEdges.size();
        weightsLen = weights.length;
        assert nodesLen <= weightsLen: 
             "More edges than weights for splitter or joiner";
        this.nodes = nodeEdges;
        this.weights = weights;
    }
    
   /**
    * All access to nodes, weights should be done through this iterator.
    * 
    * The iterator will keep track of possibly different offsets
    * {@link FlatNode#edges} / {@link FlatNode#incoming}
    * and {@link at.dms.kjc.sir.SIRSplitter#getWeights()} / 
    * {@link at.dms.kjc.sir.SIRJoiner#getWeights()}.
    * 
    * The objects returned by the iterator conform to the {@link FlatWeight} 
    * interface.
    * 
    * @return an iterator
    */ 
   public Iterator<FlatWeight> iterator() {
        return new FlatWeightIterator();
    }
    
   /**
    * The iterator implementation.
    * @author dimock
    *
    */
    private class FlatWeightIterator implements Iterator<FlatWeight> {
        private int n;
        private int w;
        
        protected FlatWeightIterator() {
            n = 0;
            w = 0;
        }
        
        public boolean hasNext() {
            if (n >= nodesLen) { return false; }
            for (int i = w; i < weightsLen; i++) {
                if (weights[i] != 0) {
                    return true;
                }
            }
            return false;
        }
        
        public FlatWeight next() {
            do {
                w++;
                if (w >= weightsLen) { throw new java.util.NoSuchElementException(); }
            } while (weights[w] == 0);
                n++;   
                if (n >= nodesLen) { throw new java.util.NoSuchElementException(); }
                return new FlatNodeWeight(w,n);
        }
        
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Implement the {@link FlatWeight} interface.
     * Easy because iterator constructs object with correct offsets.
     * 
     * @author dimock
     *
     */
    
    private class FlatNodeWeight implements FlatWeight {
        int weightsPos;         // current position in weights
        int nodesPos;           // current position in nodes
        
        protected FlatNodeWeight(int w, int n) {
            weightsPos = w;
            nodesPos = n;
        }
        public int getWeight() { return weights[weightsPos]; }
        public FlatNode getNode() {return nodes.elementAt(nodesPos); }
        public int getWeightsOffset() { return weightsPos; }
        public int getNodesOffset() { return nodesPos; }
    }
}

