package streamit.scheduler2.constrained;

public class P2PPortal extends streamit.misc.AssertedClass
{
    final LatencyNode upstreamNode, downstreamNode;
    final boolean isUpstreamPortal;
    final int minLatency;
    final int maxLatency;
    final StreamInterface parentStream;

    P2PPortal(
        boolean _isUpstreamPortal,
        LatencyNode _upstreamNode,
        LatencyNode _downstreamNode, int _minLatency, int _maxLatency, StreamInterface _parentStream)
    {
        isUpstreamPortal = _isUpstreamPortal;
        upstreamNode = _upstreamNode;
        downstreamNode = _downstreamNode;
        minLatency = _minLatency;
        maxLatency = _maxLatency;
        parentStream = _parentStream;
    }

    public boolean isDownstream()
    {
        return !isUpstreamPortal;
    }

    public boolean isUpstream()
    {
        return isUpstreamPortal;
    }

    public LatencyNode getUpstreamNode()
    {
        return upstreamNode;
    }

    public LatencyNode getDownstreamNode()
    {
        return downstreamNode;
    }

    public int getMinLatency()
    {
        return minLatency;
    }

    public int getMaxLatency()
    {
        return maxLatency;
    }

    public StreamInterface getParent()
    {
        return parentStream;
    }
}