package streamit.scheduler2.constrained;

import streamit.scheduler2.iriter./*persistent.*/
FeedbackLoopIter;
import streamit.scheduler2.iriter./*persistent.*/
Iterator;

/**
 * streamit.scheduler2.constrained.Pipeline is the pipeline constrained 
 * scheduler. It assumes that all streams in the program use the constrained
 * scheduler
 */

public class FeedbackLoop
    extends streamit.scheduler2.hierarchical.FeedbackLoop
    implements StreamInterface
{
    final private LatencyGraph latencyGraph;

    LatencyNode latencySplitter, latencyJoiner;

    public FeedbackLoop(
        FeedbackLoopIter iterator,
        Iterator parent,
        streamit.scheduler2.constrained.StreamFactory factory)
    {
        super(iterator, factory);

        latencyGraph = factory.getLatencyGraph();

        if (parent == null)
        {
            latencyGraph.registerParent(this, null);
            initiateConstrained();
        }
    }

    public void initiateConstrained()
    {
        latencySplitter = latencyGraph.addSplitter(this);
        latencyJoiner = latencyGraph.addJoiner(this);

        // register body and loop
        {
            StreamInterface body = getConstrainedBody();
            latencyGraph.registerParent(body, this);
            body.initiateConstrained();

            StreamInterface loop = getConstrainedLoop();
            latencyGraph.registerParent(loop, this);
            loop.initiateConstrained();
        }

        // add body and loop to the latency graph
        {
            // first the body
            {
                StreamInterface body = getConstrainedBody();

                LatencyNode topBodyNode = body.getTopLatencyNode();
                LatencyNode bottomBodyNode = body.getBottomLatencyNode();

                //create the appropriate edges
                LatencyEdge topBodyEdge =
                    new LatencyEdge(latencyJoiner, 0, topBodyNode, 0, 0);
                latencyJoiner.addDependency(topBodyEdge);
                topBodyNode.addDependency(topBodyEdge);

                LatencyEdge bottomBodyEdge =
                    new LatencyEdge(
                        bottomBodyNode,
                        0,
                        latencySplitter,
                        0,
                        0);
                latencySplitter.addDependency(bottomBodyEdge);
                bottomBodyNode.addDependency(bottomBodyEdge);
            }
            // and now the loop
            {
                StreamInterface loop = getConstrainedLoop();

                LatencyNode topLoopNode = loop.getTopLatencyNode();
                LatencyNode bottomLoopNode = loop.getBottomLatencyNode();

                //create the appropriate edges
                LatencyEdge topLoopEdge =
                    new LatencyEdge(latencySplitter, 1, topLoopNode, 0, 0);
                latencySplitter.addDependency(topLoopEdge);
                topLoopNode.addDependency(topLoopEdge);

                LatencyEdge bottomLoopEdge =
                    new LatencyEdge(
                        bottomLoopNode,
                        0,
                        latencyJoiner,
                        1,
                        feedbackLoop.getDelaySize());
                latencyJoiner.addDependency(bottomLoopEdge);
                bottomLoopNode.addDependency(bottomLoopEdge);
            }
        }
    }

    protected StreamInterface getConstrainedLoop()
    {
        if (!(getLoop() instanceof StreamInterface))
        {
            ERROR("This feedbackloop contains a loop that is not CONSTRAINED");
        }

        return (StreamInterface)getLoop();
    }

    protected StreamInterface getConstrainedBody()
    {
        if (!(getBody() instanceof StreamInterface))
        {
            ERROR("This feedbackloop contains a body that is not CONSTRAINED");
        }

        return (StreamInterface)getBody();
    }

    public LatencyNode getBottomLatencyNode()
    {
        return latencySplitter;
    }

    public LatencyNode getTopLatencyNode()
    {
        return latencyJoiner;
    }

    public void computeSchedule()
    {
        ERROR("Not implemented yet.");

    }
}
