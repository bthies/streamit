package streamit.scheduler2.constrained;

import streamit.scheduler2.iriter.Iterator;

/**
 * streamit.scheduler2.constrained.StreamInteraface is an interface for 
 * constrained scheduler. All implementors of this interface assume that
 * no other scheduler objects have been used.
 */

public interface StreamInterface
    extends streamit.scheduler2.hierarchical.StreamInterface
{
    public LatencyNode getBottomLatencyNode ();
    public LatencyNode getTopLatencyNode ();
    
    public void initiateConstrained ();
}
