package streamit;

import streamit.scheduler.*;

public abstract class Structure extends Stream
{
    // Can't add child streams to a structure.
    public void add(Stream s) { ASSERT(false); }
    public void connectGraph() { }
    public void setupBufferLengths(Schedule schedule) { }
}
