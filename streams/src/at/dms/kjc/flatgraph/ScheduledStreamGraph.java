package at.dms.kjc.flatgraph;

public class ScheduledStreamGraph extends StreamGraph {

    public ScheduledStreamGraph(FlatNode top) {
        super(top);
    }

    /**
     * Use in place of "new StaticStreamGraph" for subclassing.
     * 
     * A subclass of StreamGraph may refer to a subclass of StaticStreamGraph.
     * If we just used "new StaticStreamGraph" in this class we would
     * only be making StaticStreamGraph's of the type in this package.
     * 
     * 
     * @param sg       a StreamGraph
     * @param realTop  the top node
     * @return         a new StaticStreamGraph
     */
    @Override protected ScheduledStaticStreamGraph new_StaticStreamGraph(StreamGraph sg, FlatNode realTop) {
        return new ScheduledStaticStreamGraph(sg,realTop);
    }
}
