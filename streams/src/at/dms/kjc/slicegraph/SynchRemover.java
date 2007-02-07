package at.dms.kjc.slicegraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.GraphFlattener;

public class SynchRemover {

    // The topmost slice of the graph
    private Slice topSlice;
    
    // Set of slices that have already been visited (and should not be
    // checked again)
    private final HashSet<Slice> visited = new HashSet<Slice>();
    
    // Queue of slices that still need to be visited
    private final LinkedList<Slice> queue = new LinkedList<Slice>();
    
    private final LinkedList<Slice> identities = new LinkedList<Slice>();
        
    private final HashMap<InterSliceEdge,SSSequence> outMap = new HashMap<InterSliceEdge,SSSequence>();
    
    private final HashMap<InterSliceEdge,SSSequence> inMap = new HashMap<InterSliceEdge,SSSequence>();
    
    public SynchRemover(Slice topSlice) {
        this.topSlice = topSlice;
        populateFilters(topSlice);
        visited.clear();
        removeSynch();
    }
    
    /**
     * Starting with <tt>current</tt>, recursively visits every downstream
     * Slice and performs the following:
     * - creates a new empty SSSequence for each Slice's input edges
     * - if Slice is not an identity, creates SSSequence for each Slice's
     * output edges
     * - if Slice is an identity, adds it to the list of identities to be
     * processed later
     * @param current
     */
    private void populateFilters(Slice current) {
        if (current == null || visited.contains(current))
            return;
        visited.add(current);
        
        // initialize the input sequence for each slice to be empty
        InterSliceEdge[] inputs = current.getHead().getSources();
        for (int i=0; i<inputs.length; i++) {
            inMap.put(inputs[i], new SSSequence(current.getHead().totalWeights()));
        }
        
        if (!isIdentity(current)) {
            // create steady-state output sequence
            createSSOutput(current);
        } else {
            // add identities (including splitters and joiners) to the list
            // to be processed later
            identities.add(current);
        }
        
        // recursively populate all children slices
        InterSliceEdge[] edges = current.getTail().getDestList();
        for (int i=0; i<edges.length; i++) {
            populateFilters(edges[i].getDest().getParent());
        }
    }
    
    /**
     * For a given non-identity Slice, creates the steady state output sequence
     * for all of the outgoing InterSliceEdges. 
     * @param current
     */
    private void createSSOutput(Slice current) {
        // index to keep track of the output
        int index = 1;
        int sumweights = current.getTail().totalWeights();
        int[] weights = current.getTail().getWeights();
        InterSliceEdge[][] outputs = current.getTail().getDests();
        // outer loop loops over duplicate copies
        for (int i=0; i<outputs.length; i++) {
            int weight = weights[i];
            SSSequence seq = createSSSequence(current, index, weight, sumweights);
            // each duplicated output gets the same SSSequence
            for (int j=0; j<outputs[i].length; j++) {
                outMap.put(outputs[i][j], seq);
            }
            // increment index
            index+=weight;
        }
    }
    

    private void removeSynch() {
        while (!identities.isEmpty()) {
            Slice id = identities.removeFirst();
            Slice[] parents = id.getDependencies();
            int[] inweights = id.getHead().getWeights();
            
            // Array to store output SSSequences of each parent slice 
            SSSequence[] inseq = new SSSequence[parents.length];
            
            // If any parents haven't yet been processed, add this slice back
            // to the end of the list and loop again
            if (listContainsAny(identities, parents)) {
                identities.add(id);
                continue;
            }
            
            // number of times to repeat inputs
            int mult = 1;
            
            // Go through parents to calculate lcm's and extract output SSSequences
            // from the HashMap of InterSliceEdges
            for (int i=0; i<parents.length; i++) {
                Slice parent = parents[i];
                
                // Find output SSSequence from the InterSliceEdge
                InterSliceEdge e = getEdgeBetween(parent, id);
                SSSequence output = outMap.get(e);
                // Store in the array for later use
                inseq[i] = output;
                
                // calculate lcm for this particular input
                int partialmult = lcm(output.length(), inweights[i])/inweights[i];
                // update running lcm over all inputs
                mult = lcm(mult, partialmult);
            }
            
            // Create a single SSSequence of outputs for this Slice
            ArrayList<SSElement> list = new ArrayList<SSElement>();
            for (int i=0; i<mult; i++) {
                for (int j=0; j<parents.length; j++) {
                    SSSequence seq = inseq[j];
                    for (int k=0; k<inweights[j]; k++) {
                        list.add(seq.getNext());
                    }
                }
            }
            SSSequence out = new SSSequence(list, list.size());
            
            // Distribute single SSSequence over all of the output Slices
            InterSliceEdge[][] outedges = id.getTail().getDests();
            SSSequence[] outseq = new SSSequence[outedges.length];
            int[] outweights = id.getTail().getWeights();
            int lcm = lcm(out.length(), id.getTail().totalWeights());
            mult = lcm/id.getTail().totalWeights();
            
            for (int i=0; i<mult; i++) {
                for (int j=0; j<outedges.length; j++) {
                    if (outseq[j] == null) {
                        outseq[j] = new SSSequence(id.getTail().totalWeights());
                    }
                    int weight = outweights[j];
                    for (int k=0; k<weight; k++) {
                        outseq[j].add(out.getNext());
                    }
                }
            }

            for (int i=0; i<outedges.length; i++) {
                for (int j=0; j<outedges[i].length; j++) {
                    outMap.put(outedges[i][j], outseq[i]);
                }
            }
        }
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
    
    private static boolean isIdentity(Slice slice) {
        FilterSliceNode[] fn = slice.getFilterNodes();
        for (int i=0; i<fn.length; i++) {
            if (!fn[i].getFilter().getName().startsWith("Identity"))
                return false;
        }
        return true;
    }

    private static InterSliceEdge getEdgeBetween(Slice from, Slice to) {
        for (InterSliceEdge e : from.getTail().getDestSequence()) {
            if (e.getDest() == to.getHead())
                return e;
        }
        return null;
    }
    
    /**
     * Returns true if any of the elements in array elts is contained in the
     * LinkedList list
     * @param list
     * @param elts
     * @return
     */
    private static boolean listContainsAny(LinkedList<Slice> list, Slice[] elts) {
        for (int i=0; i<elts.length; i++) {
            if (list.contains(elts[i]))
                return true;
        }
        return false;
    }
    
    /**
     * Creates an SSSequence using the Slice <tt>current</tt>.
     * @param current The Slice
     * @param index The starting index of output
     * @param weight The weight of this output Edge
     * @param sumweights The total sum of weights coming out of this Slice
     * @return
     */
    private static SSSequence createSSSequence(Slice current, int index, int weight, int sumweights) {
        ArrayList<SSElement> list = new ArrayList<SSElement>();
        for (int i=0; i<weight; i++) {
            SSElement elt = new SSElement(current, index);
            list.add(elt);
            index++;
        }
        return new SSSequence(list, sumweights);
    }
    
    
    /**
     * Creates an array that expands a list of slices and associated weights by
     * explicitly listing each slice  number of times equal to its weight.
     * This entire array is possibly repeated an integral number of times.
     * For example:
     * slices = [A,B,C], weights = [2,3,1], sumWeights = 6, mult = 3
     * => [A,A,B,B,B,C,A,A,B,B,B,C,A,A,B,B,B,C]
     * 
     * @param slices Array of slices to be expanded
     * @param weights Array of corresponding weights for each slice
     * @param sumWeights Sum of all of the weights
     * @param mult The number of times to repeat the entire array
     * @return
     */
    private static Slice[] makeRepeatedArray(Slice[] slices, int[] weights,
            int sumWeights, int mult) {
        Slice[] repeatedArray = new Slice[sumWeights*mult];
        int l = 0;
        for (int i=0; i<mult; i++) {
            for (int j=0; j<slices.length; j++) {
                for (int k=0; k<weights[j]; k++) {
                    repeatedArray[l] = slices[j];
                    l++;
                }
            }
        }
        return repeatedArray;
    }
    
    private static ArrayList<Slice> makeRepeatedList(Slice[] slices, int[] weights,
            int mult) {
        ArrayList<Slice> list = new ArrayList<Slice>();
        for (int i=0; i<mult; i++) {
            for (int j=0; j<slices.length; j++) {
                for (int k=0; k<weights[j]; k++) {
                    list.add(slices[j]);
                }
            }
        }
        return list;
    }
    
    private static ArrayList<Slice> makeRepeatedOutputList(Slice s, int mult) {
        return makeRepeatedList(getChildSlices(s), s.getTail().getWeights(), mult);
    }

    /**
     * Creates lists for the new edges and weights, given an array of Slices
     * that explicitly lists each occurrence of the Slice in the schedule.
     * For example:
     * [A,A,B,B,B,C] => newEdges = [A,B,C], newWeights = [2,3,1]
     * 
     * @param repeatedOutput The array of repeated slices
     * @param newWeights The list to store the condensed weights
     * @param newSlices The list to store the condensed slices
     */
    private static void createNewSlicesWeightsLists(Slice[] repeatedOutput,
            LinkedList<Integer> newWeights, LinkedList<Slice> newSlices) {
        
        if (repeatedOutput == null || repeatedOutput.length == 0)
            return;
                
        // Keeps track of the previous slice in order to count the number of
        // repeated slices
        Slice prev = repeatedOutput[0];
        int count = 0;
        for (int i=0; i<repeatedOutput.length; i++) {
            Slice curr = repeatedOutput[i];
            // If the current slice is the same as the previous, increment count
            if (curr == prev) {
                count++;
            } else {
                // The current slice is different from the previous slice
                // Add the previous group of slices to the edge and weight lists
                newSlices.add(prev);
                newWeights.add(new Integer(count));
                // Reset prev and count for the next group
                prev = curr;
                count = 1;
            }
        }
        // The last group of slices will not be added in the loop. Add it now.
        newSlices.add(prev);
        newWeights.add(new Integer(count));
    }
    
    /**
     * Creates new outgoing edges between the OutputSliceNode and the list of
     * output Slices. If splitter is RR, creates a separate LinkedList for each
     * Edge. If splitter is duplicate, creates a single LinkedList for all Edges.
     * @param slice
     * @param outputs
     * @param isRR
     * @return
     */
    private static LinkedList<LinkedList<InterSliceEdge>> createNewOutgoingEdges(
            OutputSliceNode slice, LinkedList<Slice> outputs, boolean isRR) {
        LinkedList<LinkedList<InterSliceEdge>> newEdges = new LinkedList<LinkedList<InterSliceEdge>>();
        
        if (isRR) {
            for (Slice output : outputs) {
                LinkedList<InterSliceEdge> temp = new LinkedList<InterSliceEdge>();
                InterSliceEdge e = new InterSliceEdge(slice, output.getHead());
                temp.add(e);
                newEdges.add(temp);
            }
        } else {
            LinkedList<InterSliceEdge> temp = new LinkedList<InterSliceEdge>();
            for (Slice output : outputs) {
                InterSliceEdge e = new InterSliceEdge(slice, output.getHead());
                temp.add(e);
            }
            newEdges.add(temp);
        }
        
        return newEdges;
    }
    
    /**
     * Creates new incoming edges to the InputSliceNode from the list of input
     * Slices.
     * @param slice
     * @param inputs
     * @return
     */
    private static LinkedList<InterSliceEdge> createNewIncomingEdges(InputSliceNode slice,
            LinkedList<Slice> inputs) {
        LinkedList<InterSliceEdge> newEdges = new LinkedList<InterSliceEdge>();
        
        for (Slice input : inputs) {
            InterSliceEdge e = new InterSliceEdge(input.getTail(), slice);
            newEdges.add(e);
        }
        
        return newEdges;
    }
    
    /**
     * Finds all of the parent slices upstream from the current slice.
     * @param slice The current slice
     * @return Array of parent slices
     */
    private static Slice[] getParentSlices(Slice slice) {
        LinkedList<Slice> parents = new LinkedList<Slice>();
        LinkedList<InterSliceEdge> inEdges = slice.getHead().getSourceList();
        
        for (InterSliceEdge e : inEdges) {
            Slice parent = e.getSrc().getParent();
            parents.add(parent);
        }
        
        return parents.toArray(new Slice[0]);
    }
    
    /**
     * Finds all of the child slices downstream from the current slice.
     * @param slice The current slice
     * @return Array of child slices
     */
    private static Slice[] getChildSlices(Slice slice) {
        LinkedList<Slice> children = new LinkedList<Slice>();
        InterSliceEdge[] outEdges = slice.getTail().getDestList();
        
        for (int i=0; i<outEdges.length; i++) {
            Slice child = outEdges[i].getDest().getParent();
            children.add(child);
        }
        
        return children.toArray(new Slice[0]);
    }
}

//class IOFunction {
//    Slice slice;
//    int[] inputWeights;
//    int[] outputWeights;
//    
//    IOFunction(Slice s, int[] iw, int[] ow) {
//        slice = s;
//        inputWeights = iw;
//        outputWeights = ow;
//    }
//    
//    int inToOut(int inputTap, int tapePos) {
//        return 0;
//    }
//    
//    int inToOut(IOPair inPair) {
//        return 0;
//    }
//    
//    IOPair outToPair(int outNum) {
//        return null;
//    }
//    
//    IOPair getOutput(int inputTape, int tapePos) {
//        return outToPair(inToOut(inputTape, tapePos));
//    }
//    
//    IOPair getOutput(IOPair inPair) {
//        return outToPair(inToOut(inPair));
//    }
//}
//
//class IOPair {
//    int tape;
//    int pos;
//    
//    IOPair(int t, int p) {
//        tape = t;
//        pos = p;
//    }
//}

class SSSequence {
    ArrayList<SSElement> list;
    int index;
    int sumweights;
    int redos;
    
    SSSequence(int sw) {
        list = new ArrayList<SSElement>();
        index = 0;
        sumweights = sw;
        redos = 0;
    }
    
    SSSequence(ArrayList<SSElement> list, int sw) {
        this.list = list;
        index = 0;
        sumweights = sw;
        redos = 0;
    }
    
    SSElement getNext() {
        SSElement temp;
        if (index >= list.size()) {
            index = 0;
            redos++;
            temp = list.get(0);
        } else {
            temp = list.get(index);
            index++;
        }
        return new SSElement(temp.slice, temp.num + redos*sumweights);
    }
    
    int length() {
        return list.size();
    }
    
    void add(SSElement s) {
        list.add(s);
    }
}

class SSElement {
    Slice slice;
    int num;
    
    SSElement(Slice slice, int num) {
        this.slice = slice;
        this.num = num;
    }
    
    Slice getSlice() {
        return slice;
    }
    
    int getNum() {
        return num;
    }
    
    boolean equals(SSElement elt) {
        return (this.slice == elt.slice && this.num == elt.num);
    }
}