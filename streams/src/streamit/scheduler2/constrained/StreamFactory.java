package streamit.scheduler2.constrained;

public interface StreamFactory
    extends streamit.scheduler2.base.StreamFactory
{
    public LatencyGraph getLatencyGraph();
}