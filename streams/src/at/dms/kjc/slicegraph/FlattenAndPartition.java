/**
 * 
 */
package at.dms.kjc.slicegraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Iterator;
import at.dms.kjc.flatgraph.GraphFlattener;
import at.dms.kjc.flatgraph2.FilterContent;
import at.dms.kjc.flatgraph2.UnflatFilter;
import at.dms.kjc.sir.linear.LinearAnalyzer;
import at.dms.kjc.sir.lowering.RenameAll;
import at.dms.kjc.sir.lowering.partition.WorkEstimate;
import at.dms.kjc.sir.*;
import at.dms.kjc.spacetime.RawChip;
import at.dms.kjc.spacetime.Trace;
import at.dms.kjc.flatgraph.*;
import at.dms.kjc.flatgraph2.*;
import at.dms.kjc.*;
//import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.common.CommonUtils;

/**
 * @author mgordon
 * 
 */
public class FlattenAndPartition extends Partitioner {
    private SIRToTraceNodes traceNodes;

    private HashMap<OutputTraceNode, HashMap<InputTraceNode, Edge>> edges;

    private Trace topTrace;

    private LinkedList<Trace> traceList;

    private LinkedList<Trace> ioList;

    public FlattenAndPartition(UnflatFilter[] topFilters, HashMap[] exeCounts,
            LinearAnalyzer lfa, WorkEstimate work, RawChip rawChip) {
        super(topFilters, exeCounts, lfa, work, rawChip);
        workEstimation = new HashMap<FilterContent, Integer>();
    }

    public Trace[] partition() {
        return traceGraph;
    }

    public void flatten(SIRStream str, HashMap[] exeCounts) {
        // use FlatGraph to eliminate intermediate levels of pipelines
        // when looking at stream.
        GraphFlattener fg = new GraphFlattener(str);
        traceNodes = new SIRToTraceNodes();
        traceNodes.createNodes(fg.top, exeCounts);
        traceList = new LinkedList<Trace>();
        ioList = new LinkedList<Trace>();
        work = WorkEstimate.getWorkEstimate(str);
        edges = new HashMap<OutputTraceNode, HashMap<InputTraceNode, Edge>>();

        flattenInternal(fg.top);

        System.out.println("Traces: " + traceList.size());
        traceGraph = traceList.toArray(new Trace[traceList.size()]);
        io = ioList.toArray(new Trace[ioList.size()]);
    }

    private void flattenInternal(FlatNode top) {
        Iterator<FlatNode> dataFlow = DataFlowTraversal.getTraversal(top)
                .iterator();

        while (dataFlow.hasNext()) {
            FlatNode node = dataFlow.next();
            System.out.println(node);
            InputTraceNode input = traceNodes.inputNodes.get(node.contents);
            OutputTraceNode output = traceNodes.outputNodes.get(node.contents);
            FilterTraceNode filterNode = traceNodes.filterNodes
                    .get(node.contents);

            assert input != null && output != null && filterNode != null;

            // set up the trace
            Trace trace = new Trace(input);
            input.setNext(filterNode);
            filterNode.setPrevious(input);
            filterNode.setNext(output);
            output.setPrevious(filterNode);
            input.setParent(trace);
            output.setParent(trace);
            filterNode.setParent(trace);

            System.out.println("  outputs: " + node.ways);
            if (node.ways != 0) {
                assert node.ways == node.edges.length
                        && node.ways == node.weights.length;
                
                // set up the i/o arcs
                // set up the splitting...
                LinkedList<Edge> outEdges = new LinkedList<Edge>();
                LinkedList<Integer> outWeights = new LinkedList<Integer>();
                HashMap<InputTraceNode, Edge> newEdges = new HashMap<InputTraceNode, Edge>();
                for (int i = 0; i < node.ways; i++) {
                    if (node.weights[i] == 0)
                        continue;
                    Edge edge = new Edge(output, traceNodes.inputNodes
                            .get(node.edges[i].contents));
                    newEdges.put(traceNodes.inputNodes
                            .get(node.edges[i].contents), edge);
                    outEdges.add(edge);
                    outWeights.add(node.weights[i]);
                }
                edges.put(output, newEdges);
                
                LinkedList<LinkedList<Edge>>translatedEdges = new LinkedList<LinkedList<Edge>>();
                if (node.isDuplicateSplitter()) {
                    outWeights = new LinkedList<Integer>();
                    outWeights.add(new Integer(1));
                    translatedEdges.add(outEdges);
                } else {
                    for (int i = 0; i < outEdges.size(); i++) {
                        LinkedList<Edge> link = new LinkedList<Edge>();
                        link.add(outEdges.get(i));
                        translatedEdges.add(link);
                    }
                }

                output.set(outWeights, translatedEdges);
            } else {
                // no outputs
                output.setWeights(new int[0]);
                output.setDests(new Edge[0][0]);
            }

            if (node.isFilter()) {
                if (node.getFilter().getPushInt() == 0) {
                    output.setWeights(new int[0]);
                    output.setDests(new Edge[0][0]);
                }
            }
            
            // set up the joining, the edges should exist already from upstream
            System.out.println("  inputs: " + node.inputs);
            if (node.inputs != 0) {
                assert node.inputs == node.incoming.length
                        && node.inputs == node.incomingWeights.length;

                LinkedList<Integer> inWeights = new LinkedList<Integer>();
                LinkedList<Edge> inEdges = new LinkedList<Edge>();
                for (int i = 0; i < node.inputs; i++) {
                    if (node.incomingWeights[i] == 0)
                        continue;
                    inEdges.add(edges.get(
                            traceNodes.outputNodes
                                    .get(node.incoming[i].contents)).get(input));
                    inWeights.add(node.incomingWeights[i]);
                }
                input.set(inWeights, inEdges);
            } else {
                input.setWeights(new int[0]);
                input.setSources(new Edge[0]);
            }

            if (node.isFilter() && node.getFilter().getPopInt() == 0) {
                input.setWeights(new int[0]);
                input.setSources(new Edge[0]);
            }
            
            // set up the work hashmaps
            int workEst = 0;
            if (traceNodes.generatedIds.contains(filterNode)) {
                workEst = 3 * filterNode.getFilter().getSteadyMult();
            } else {
                assert node.isFilter();
                workEst = work.getWork((SIRFilter) node.contents);
            }
            bottleNeckFilter.put(trace, filterNode);
            traceBNWork.put(trace, workEst);
            workEstimation.put(filterNode.getFilter(), workEst);

            trace.finish();

            if (node.contents instanceof SIRFileReader
                    || node.contents instanceof SIRFileWriter) {
                System.out.println("Found io " + node.contents);
                ioList.add(trace);
            }
                
            if (topTrace == null)
                topTrace = trace;
            traceList.add(trace);
        }
    }
}

class SIRToTraceNodes implements FlatVisitor {
    public HashMap<SIROperator, InputTraceNode> inputNodes;

    public HashMap<SIROperator, OutputTraceNode> outputNodes;

    public HashMap<SIROperator, FilterTraceNode> filterNodes;

    public HashSet<FilterTraceNode> generatedIds;

    private HashMap[] exeCounts;

    public void createNodes(FlatNode top, HashMap[] exeCounts) {
        inputNodes = new HashMap<SIROperator, InputTraceNode>();
        outputNodes = new HashMap<SIROperator, OutputTraceNode>();
        filterNodes = new HashMap<SIROperator, FilterTraceNode>();
        generatedIds = new HashSet<FilterTraceNode>();
        this.exeCounts = exeCounts;

        top.accept(this, null, true);
    }

    public void visitNode(FlatNode node) {
        System.out.println("Creating TraceNodes: " + node);
        OutputTraceNode output = new OutputTraceNode();
        InputTraceNode input = new InputTraceNode();
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

        FilterTraceNode filterNode = new FilterTraceNode(content);
        if (node.isSplitter() || node.isJoiner())
            generatedIds.add(filterNode);

        inputNodes.put(node.contents, input);
        outputNodes.put(node.contents, output);
        filterNodes.put(node.contents, filterNode);
    }
}
