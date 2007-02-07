package at.dms.kjc.slicegraph;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.GraphFlattener;

public class SynchRemover {

    private Slice topSlice;
    
    public SynchRemover(Slice topSlice) {
        this.topSlice = topSlice;
        // combine consecutive splitters and joiners into single "super" splitters
        // and joiners
        mergeJoinersAndSplitters(topSlice);
        // bypass joiner-splitter pairs
        removeJoinerSplitterSynch(topSlice);
    }
    
    private void mergeJoinersAndSplitters(Slice current) {
        if (current == null)
            return;
        InterSliceEdge[] outEdges = current.getTail().getDestList();
        // If this slice is a joiner
        if (current.getHead().isJoiner()) {
            Slice child = outEdges[0].getDest().getParent();
            // and if the next slice is also a joiner
            if (child.getHead().isJoiner()) {
                mergeJoiners(child);    // merge them
            }
            // move on to the next slice
            mergeJoinersAndSplitters(child);
        }
        else if (current.getTail().isDuplicateSplitter()) {
            
        } else {
            // If not a splitter or joiner, move on to the children slices
            for (int i=0; i<outEdges.length; i++) {
                InterSliceEdge e = outEdges[i];
                mergeJoinersAndSplitters(e.getDest().getParent());
            }
        }
    }
    
    /**
     * Combines multiple consecutive joiners into a single joiner. Combines
     * two levels of joiners at a time. Takes as input the second-level joiner
     * (i.e. the joiner that joins the other joiners).
     * @param joiner
     */
    private void mergeJoiners(Slice joiner) {
        // The slices joined by this joiner
        InterSliceEdge[] parentEdges = joiner.getHead().getSources();
        Slice[] parents = joiner.getDependencies();
        
        // An array that will store the parent slices of the second joiner
        Slice[][] grandparents = new Slice[parents.length][];

        // number of times to repeat output to reach a recurrence; default to 1
        int mult = 1;
        
        for (int i=0; i<parents.length; i++) {
            InterSliceEdge parentEdge = parentEdges[i];
            Slice parent = parents[i];
            if (parent.getHead().isJoiner()) {
                // number of times this particular parent joiner needs to be
                // repeated
                int lcm = lcm(parent.getHead().totalWeights(), joiner.getHead().getWeight(parentEdge));
                int partialMult = lcm/joiner.getHead().getWeight(parentEdge);
                mult = lcm(mult, partialMult);

                grandparents[i] = makeRepeatedArray(parent.getDependencies(),
                        parent.getHead().getWeights(),
                        parent.getHead().totalWeights(), 1);
            }
        }
        
        for (int i=0; i<parentEdges.length; i++) {
            InterSliceEdge parentEdge = parentEdges[i];
            Slice parent = parentEdge.getSrc().getParent();
            int lcm = mult*joiner.getHead().getWeight(parentEdge);
            if (parent.getHead().isJoiner()) {
                for (int j=0; j<parent.getHead().getWidth(); j++) {
                    Slice grandparent = parent.getHead().getSources()[j].getSrc().getParent();
                    int newWeight = lcm/parent.getHead().totalWeights()*parent.getHead().getWeights()[j];
                    InterSliceEdge newEdge = new InterSliceEdge(grandparent.getTail(), joiner.getHead());
                    grandparent.getTail().setDests(new InterSliceEdge[][]{{newEdge}});
                    grandparent.getTail().setWeights(new int[]{newWeight});
                }
            } else {
                InterSliceEdge newEdge = new InterSliceEdge(parent.getTail(), joiner.getHead());
                int newWeight = mult*joiner.getHead().getWeights()[i];
                parent.getTail().setDests(new InterSliceEdge[][]{{newEdge}});
                parent.getTail().setWeights(new int[]{newWeight});
            }
        }
        
        int sumWeights = joiner.getHead().totalWeights();
        Slice[] repeatedOutput = 
            makeRepeatedArray(parents,
                    joiner.getHead().getWeights(), 
                    sumWeights, mult);
        
        int offset = 0;
        for (int i=0; i<parents.length; i++) {
            Slice parent = parents[i];
            if (parent.getHead().isJoiner()) {
                int count = 0;
                int weight = joiner.getHead().getWeights()[i];
                Slice[] currGrandparent = grandparents[i];
                for (int j=0; j<mult; j++) {
                    for (int k=0; k<weight; k++) {
                        repeatedOutput[j*sumWeights + offset + k] =
                            currGrandparent[count%currGrandparent.length];
                        count++;
                    }
                }
                parent.getHead().setSources(new InterSliceEdge[0]);
                parent.getHead().setWeights(new int[0]);
                parent.getTail().setDests(new InterSliceEdge[0][]);
                parent.getTail().setWeights(new int[0]);
            }
            offset += joiner.getHead().getWeights()[i];
        }
        
        LinkedList<Integer> newWeights = new LinkedList<Integer>();
        LinkedList<Slice> newSlices = new LinkedList<Slice>();
        createNewSlicesWeightsArray(repeatedOutput, newWeights, newSlices);
        
        LinkedList<InterSliceEdge> newEdges = new LinkedList<InterSliceEdge>();
        
        for (Slice s : newSlices) {
            InterSliceEdge newEdge = new InterSliceEdge(s.getTail(), joiner.getHead());
            newEdges.add(newEdge);
        }
        joiner.getHead().set(newWeights, newEdges);
    }
   
    
    // -------------------- Joiner-Splitter ----------------------------------
    
    // Eliminates Joiner-Splitter combos by directly connecting the slices
    // joined by the joiner to the slices split by the splitter, bypassing
    // the joiner and splitter
    private void removeJoinerSplitterSynch(Slice current) {
        // null graph, or reached the end of a branch => return
        if (current == null)
            return;
        if (current.getHead().isJoiner()) {
            InterSliceEdge[] outEdges = current.getTail().getDestList();
            // no child => done with branch, return
            if (outEdges == null || outEdges.length == 0)
                return;
            // There should only be one out Edge
            Slice child = outEdges[0].getDest().getParent();
            // not a joiner-splitter combo => removeSynch on child
            if (!child.getTail().isSplitter()) {
                removeJoinerSplitterSynch(child);
            } else { // joiner-splitter combo => remove synchronization
                Slice[] grandchildren = getChildSlices(child);
                doJoinerSplitterSynchRemoval(current, child);
                for (int i=0; i<grandchildren.length; i++) {
                    removeJoinerSplitterSynch(grandchildren[i]);
                }
            }
//        } else if (current.getFilterNodes()[0].getFilter().getName()) {
            
        } else { // slice is not a joiner, removeSynch on all the children
            Slice[] children = getChildSlices(current);
            for (int i=0; i<children.length; i++) {
                removeJoinerSplitterSynch(children[i]);
            }
        }
    }
    
    private void doJoinerSplitterSynchRemoval(Slice joiner, Slice splitter) {
        Slice[] inputs = getParentSlices(joiner);
        Slice[] outputs = getChildSlices(splitter);
        int[] inWeights = joiner.getHead().getWeights();
        int[] outWeights = splitter.getTail().getWeights();
        int inWeightsSum = joiner.getHead().totalWeights();
        int outWeightsSum = splitter.getTail().totalWeights();
        
        if (splitter.getTail().isRRSplitter()) {

            // Will need to repeat inputs and outputs an integral number of times
            // to get a recurrence
            int lcm = lcm(inWeightsSum, outWeightsSum);
            // # of times inputs are repeated
            int inMult = lcm/inWeightsSum;
            // # of times outputs are repeated
            int outMult = lcm/outWeightsSum;

            // Create a new array of inputs with all of the repeated inputs
            // duplicated explicitly.
            Slice[] repeatedInput = 
                makeRepeatedArray(inputs, inWeights, inWeightsSum, inMult);

            // Create a new array of outputs with all of the repeated outputs
            // duplicated explicitly.
            Slice[] repeatedOutput = 
                makeRepeatedArray(outputs, outWeights, outWeightsSum, outMult); 

            // Map each input slice to a list of output slices it will be connected to
            // in the recurrence. This is to update each input slice's outgoing edges
            // and weights.
            Map<Slice,List<Slice>> inToOutMap = new HashMap<Slice,List<Slice>>();

            // Map each output slice to a list of input slices that connect to it in
            // the recurrencde. This is to update each output slice's incoming edges
            // and weights.
            Map<Slice,List<Slice>> outToInMap = new HashMap<Slice,List<Slice>>();

            // Iterate over all of the repeated inputs and outputs to populate the
            // two maps
            for (int i=0; i<repeatedOutput.length; i++) {
                Slice inSlice = repeatedInput[i];
                Slice outSlice = repeatedOutput[i];

                // If this is a new input slice, create a new associated list for it
                if (!inToOutMap.containsKey(inSlice)) {
                    inToOutMap.put(inSlice, new LinkedList<Slice>());
                }
                // Add the output slice to the associated input slice's list
                inToOutMap.get(inSlice).add(outSlice);

                // If this is a new output slice, create a new associated list for it
                if (!outToInMap.containsKey(outSlice)) {
                    outToInMap.put(outSlice, new LinkedList<Slice>());
                }
                // Add the input slice to the associated output slice's list
                outToInMap.get(outSlice).add(inSlice);
            }

            // Update each input slice's outgoing edges and weights
            for (Slice inSlice : inToOutMap.keySet()) {
                List<Slice> outputList = inToOutMap.get(inSlice);
                LinkedList<Integer> newWeights = new LinkedList<Integer>();
                LinkedList<Slice> newChildren = new LinkedList<Slice>();
                // Convert the raw list of outputs to the arrays for outgoing edges
                // and weights
                createNewSlicesWeightsArray(outputList.toArray(new Slice[0]), 
                        newWeights, newChildren);
                LinkedList<LinkedList<InterSliceEdge>> newEdges = 
                    createNewOutgoingEdges(inSlice.getTail(),
                                           newChildren,
                                           true /* isRR */);
                inSlice.getTail().set(newWeights, newEdges);
            }

            // Update each output slice's incoming edges and weights
            for (Slice outSlice : outToInMap.keySet()) {
                List<Slice> inputList = outToInMap.get(outSlice);
                LinkedList<Integer> newWeights = new LinkedList<Integer>();
                LinkedList<Slice> newParents = new LinkedList<Slice>();
                // Convert the raw list of inputs to the arrays for incoming edges
                // and weights
                createNewSlicesWeightsArray(inputList.toArray(new Slice[0]), 
                        newWeights, newParents);
                LinkedList<InterSliceEdge> newEdges = 
                    createNewIncomingEdges(outSlice.getHead(),
                                           newParents);
                outSlice.getHead().set(newWeights, newEdges);
            }
        } else { // Duplicate splitter
            
            // Update incoming edges and weights for the output slices
            for (int i=0; i<outputs.length; i++) {
                Slice output = outputs[i];
                LinkedList<InterSliceEdge> newEdges = new LinkedList<InterSliceEdge>();
                LinkedList<Integer> newWeights = new LinkedList<Integer>();
                for (int j=0; j<inputs.length; j++) {
                    Slice input = inputs[j];
                    InterSliceEdge e = new InterSliceEdge(input.getTail(), output.getHead());
                    newEdges.add(e);
                    newWeights.add(new Integer(inWeights[j]));
                }
                output.getHead().set(newWeights, newEdges);
            }
            
            // Update outgoing edges and weights for the input slices
            for (int i=0; i<inputs.length; i++) {
                Slice input = inputs[i];
                LinkedList<InterSliceEdge> edges = new LinkedList<InterSliceEdge>();
                for (int j=0; j<outputs.length; j++) {
                    Slice output = outputs[j];
                    InterSliceEdge e = new InterSliceEdge(input.getTail(), output.getHead());
                    edges.add(e);
                }
                LinkedList<Integer> newWeights = new LinkedList<Integer>();
                newWeights.add(new Integer(1));
                LinkedList<LinkedList<InterSliceEdge>> newEdges = new LinkedList<LinkedList<InterSliceEdge>>();
                newEdges.add(edges);
                input.getTail().set(newWeights, newEdges);
            }
        }
        
        // clear the joiner and splitter slices because they're no longer needed
        joiner.getHead().setSources(new InterSliceEdge[0]);
        joiner.getHead().setWeights(new int[0]);
        joiner.getTail().setDests(new InterSliceEdge[0][0]);
        joiner.getTail().setWeights(new int[0]);
        splitter.getHead().setSources(new InterSliceEdge[0]);
        splitter.getHead().setWeights(new int[0]);
        splitter.getTail().setDests(new InterSliceEdge[0][0]);
        splitter.getTail().setWeights(new int[0]);
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

    /**
     * Creates arrays for the new edges and weights, given an array of Slices
     * that explicitly lists each occurrence of the Slice in the schedule.
     * For example:
     * [A,A,B,B,B,C] => newEdges = [A,B,C], newWeights = [2,3,1]
     * 
     * @param repeatedOutput The array of repeated slices
     * @param newWeights The array to store the condensed weights
     * @param newSlices The array to store the condensed slices
     */
    private static void createNewSlicesWeightsArray(Slice[] repeatedOutput,
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