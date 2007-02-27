/**
 * 
 */
package at.dms.kjc.slicegraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Iterator;
import at.dms.kjc.flatgraph.GraphFlattener;
import at.dms.kjc.sir.linear.LinearAnalyzer;
import at.dms.kjc.sir.lowering.RenameAll;
import at.dms.kjc.sir.lowering.partition.WorkEstimate;
import at.dms.kjc.sir.*;
import at.dms.kjc.flatgraph.*;
import at.dms.kjc.*;
import at.dms.kjc.common.CommonUtils;

/**
 * Convert SIR graph to Slice without synch removal.
 * Partitions as one filter per Slice.
 * 
 * @author mgordon
 * 
 */
public class FlattenAndPartition extends Partitioner {
    private SIRToSliceNodes sliceNodes;

    private HashMap<OutputSliceNode, HashMap<InputSliceNode, InterSliceEdge>> edges;

    private Slice topSlice;

    private LinkedList<Slice> sliceList;

    private LinkedList<Slice> ioList;

    public FlattenAndPartition(UnflatFilter[] topFilters, HashMap[] exeCounts,
            LinearAnalyzer lfa, WorkEstimate work, int maxPartitions) {
        super(topFilters, exeCounts, lfa, work, maxPartitions);
        workEstimation = new HashMap<FilterContent, Integer>();
    }

    public Slice[] partition() {
        return sliceGraph;
    }

    public void flatten(SIRStream str, HashMap[] exeCounts) {
        // use FlatGraph to eliminate intermediate levels of pipelines
        // when looking at stream.
        GraphFlattener fg = new GraphFlattener(str);
        sliceNodes = new SIRToSliceNodes();
        sliceNodes.createNodes(fg.top, exeCounts);
        sliceList = new LinkedList<Slice>();
        ioList = new LinkedList<Slice>();
        work = WorkEstimate.getWorkEstimate(str);
        edges = new HashMap<OutputSliceNode, HashMap<InputSliceNode, InterSliceEdge>>();

        flattenInternal(fg.top);

        System.out.println("Slices: " + sliceList.size());
        sliceGraph = sliceList.toArray(new Slice[sliceList.size()]);
        io = ioList.toArray(new Slice[ioList.size()]);
    }

    private void flattenInternal(FlatNode top) {
        Iterator<FlatNode> dataFlow = DataFlowTraversal.getTraversal(top)
                .iterator();

        while (dataFlow.hasNext()) {
            FlatNode node = dataFlow.next();
            System.out.println(node);
            InputSliceNode input = sliceNodes.inputNodes.get(node.contents);
            OutputSliceNode output = sliceNodes.outputNodes.get(node.contents);
            FilterSliceNode filterNode = sliceNodes.filterNodes
                    .get(node.contents);

            assert input != null && output != null && filterNode != null;

            // set up the slice
            Slice slice = new Slice(input);
            input.setNext(filterNode);
            filterNode.setPrevious(input);
            filterNode.setNext(output);
            output.setPrevious(filterNode);
            input.setParent(slice);
            output.setParent(slice);
            filterNode.setParent(slice);

            System.out.println("  outputs: " + node.ways);
            if (node.ways != 0) {
                assert node.ways == node.getEdges().length
                        && node.ways == node.weights.length;
                
                // set up the i/o arcs
                // set up the splitting...
                LinkedList<InterSliceEdge> outEdges = new LinkedList<InterSliceEdge>();
                LinkedList<Integer> outWeights = new LinkedList<Integer>();
                HashMap<InputSliceNode, InterSliceEdge> newEdges = new HashMap<InputSliceNode, InterSliceEdge>();
                for (int i = 0; i < node.ways; i++) {
                    if (node.weights[i] == 0)
                        continue;
                    InterSliceEdge edge = new InterSliceEdge(output, sliceNodes.inputNodes
                            .get(node.getEdges()[i].contents));
                    newEdges.put(sliceNodes.inputNodes
                            .get(node.getEdges()[i].contents), edge);
                    outEdges.add(edge);
                    outWeights.add(node.weights[i]);
                }
                edges.put(output, newEdges);
                
                LinkedList<LinkedList<InterSliceEdge>>translatedEdges = new LinkedList<LinkedList<InterSliceEdge>>();
                if (node.isDuplicateSplitter()) {
                    outWeights = new LinkedList<Integer>();
                    outWeights.add(new Integer(1));
                    translatedEdges.add(outEdges);
                } else {
                    for (int i = 0; i < outEdges.size(); i++) {
                        LinkedList<InterSliceEdge> link = new LinkedList<InterSliceEdge>();
                        link.add(outEdges.get(i));
                        translatedEdges.add(link);
                    }
                }

                output.set(outWeights, translatedEdges);
            } else {
                // no outputs
                output.setWeights(new int[0]);
                output.setDests(new InterSliceEdge[0][0]);
            }

            if (node.isFilter()) {
                if (node.getFilter().getPushInt() == 0) {
                    output.setWeights(new int[0]);
                    output.setDests(new InterSliceEdge[0][0]);
                }
            }
            
            // set up the joining, the edges should exist already from upstream
            System.out.println("  inputs: " + node.inputs);
            if (node.inputs != 0) {
                assert node.inputs == node.incoming.length
                        && node.inputs == node.incomingWeights.length;

                LinkedList<Integer> inWeights = new LinkedList<Integer>();
                LinkedList<InterSliceEdge> inEdges = new LinkedList<InterSliceEdge>();
                for (int i = 0; i < node.inputs; i++) {
                    if (node.incomingWeights[i] == 0)
                        continue;
                    inEdges.add(edges.get(
                            sliceNodes.outputNodes
                                    .get(node.incoming[i].contents)).get(input));
                    inWeights.add(node.incomingWeights[i]);
                }
                input.set(inWeights, inEdges);
            } else {
                input.setWeights(new int[0]);
                input.setSources(new InterSliceEdge[0]);
            }

            if (node.isFilter() && node.getFilter().getPopInt() == 0) {
                input.setWeights(new int[0]);
                input.setSources(new InterSliceEdge[0]);
            }
            
            // set up the work hashmaps
            int workEst = 0;
            if (sliceNodes.generatedIds.contains(filterNode)) {
                workEst = 3 * filterNode.getFilter().getSteadyMult();
            } else {
                assert node.isFilter();
                workEst = work.getWork((SIRFilter) node.contents);
            }
            bottleNeckFilter.put(slice, filterNode);
            sliceBNWork.put(slice, workEst);
            workEstimation.put(filterNode.getFilter(), workEst);

            slice.finish();

            if (node.contents instanceof SIRFileReader
                    || node.contents instanceof SIRFileWriter) {
                System.out.println("Found io " + node.contents);
                ioList.add(slice);
            }
                
            if (topSlice == null)
                topSlice = slice;
            sliceList.add(slice);
        }
    }
}

class SIRToSliceNodes implements FlatVisitor {
    public HashMap<SIROperator, InputSliceNode> inputNodes;

    public HashMap<SIROperator, OutputSliceNode> outputNodes;

    public HashMap<SIROperator, FilterSliceNode> filterNodes;

    public HashSet<FilterSliceNode> generatedIds;

    private HashMap[] exeCounts;

    public void createNodes(FlatNode top, HashMap[] exeCounts) {
        inputNodes = new HashMap<SIROperator, InputSliceNode>();
        outputNodes = new HashMap<SIROperator, OutputSliceNode>();
        filterNodes = new HashMap<SIROperator, FilterSliceNode>();
        generatedIds = new HashSet<FilterSliceNode>();
        this.exeCounts = exeCounts;

        top.accept(this, null, true);
    }

    public void visitNode(FlatNode node) {
        System.out.println("Creating SliceNodes: " + node);
        OutputSliceNode output = new OutputSliceNode();
        InputSliceNode input = new InputSliceNode();
        FilterContent content;
        int mult = 1;

        if (node.isFilter()) {
            if (node.contents instanceof SIRFileWriter) {
                content = new FileOutputContent((SIRFileWriter)node.contents);
            }
            else if (node.contents instanceof SIRFileReader) {
                content = new FileInputContent((SIRFileReader)node.contents);
            }
            else
                content = new FilterContent(node.getFilter());

        } else if (node.isSplitter()) {
            CType type = CommonUtils.getOutputType(node);
            SIRIdentity id = new SIRIdentity(type);
            RenameAll.renameAllFilters(id);
            content = new FilterContent(id);
            if (!node.isDuplicateSplitter())
                mult = node.getTotalOutgoingWeights();

        } else {
            // joiner
            CType type = CommonUtils.getOutputType(node);
            SIRIdentity id = new SIRIdentity(type);
            RenameAll.renameAllFilters(id);
            content = new FilterContent(id);
            mult = node.getTotalIncomingWeights();

        }
        if (exeCounts[0].containsKey(node.contents))
            content.setInitMult(mult
                    * ((int[]) exeCounts[0].get(node.contents))[0]);
        else
            content.setInitMult(0);

        content.setSteadyMult(mult
                * ((int[]) exeCounts[1].get(node.contents))[0]);

        FilterSliceNode filterNode = new FilterSliceNode(content);
        if (node.isSplitter() || node.isJoiner())
            generatedIds.add(filterNode);

        inputNodes.put(node.contents, input);
        outputNodes.put(node.contents, output);
        filterNodes.put(node.contents, filterNode);
    }
}
