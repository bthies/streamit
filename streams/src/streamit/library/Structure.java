package streamit.library;

import streamit.scheduler2.Scheduler;

public abstract class Structure extends Stream
{
    // Can't add child streams to a structure.
    public void add(Stream s) { ASSERT(false); }
    public void connectGraph() { }
    public void setupBufferLengths(Scheduler schedule) { }
}
