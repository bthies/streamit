/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

package streamit.scheduler2.constrained;

import streamit.scheduler2.iriter.Iterator;
import streamit.scheduler2.hierarchical.PhasingSchedule;

/**
 * streamit.scheduler2.constrained.StreamInteraface is an interface for 
 * constrained scheduler. All implementors of this interface assume that
 * no other scheduler objects have been used.
 */

public interface StreamInterface
    extends streamit.scheduler2.hierarchical.StreamInterface
{
    public StreamInterface getTopConstrainedStream();
    public StreamInterface getBottomConstrainedStream();

    public LatencyNode getBottomLatencyNode();
    public LatencyNode getTopLatencyNode();

    public void initiateConstrained();

    /**
     * Initialize this stream for all of its restrictions. This function
     * will create initial restrictions for this stream, and call this 
     * same function for all children of this stream.
     */
    public void initializeRestrictions(Restrictions restrictions);

    /**
     * Create restrictions for steady state execution. Basically this
     * will enter a restriction for EVERY single node, which will be
     * equal to the steady state number of executions of this node
     * in the greater parent. These restrictions will never be updated,
     * only depleted and later blocked. This will ensure that I will
     * execute every node EXACTLY the right number of times... 
     */
    public void createSteadyStateRestrictions(int streamNumExecs);

    /**
     * Inform the stream that one if its children is now blocked due
     * to a restriction imposed by the stream. 
     */
    public void registerNewlyBlockedSteadyRestriction(Restriction restriction);

    /**
     * Check if the stream is done initializing all the portals that are
     * inside it. This will recursively check all the children.
     */
    public boolean isDoneInitializing();
    
    /**
     * Check if the stream is done running the steady state schedule.
     * This will recursively check all the children.
     */
    public boolean isDoneSteadyState();

    /**
     * Get notification that the initialization of a particular portal
     * is complete. Probably should create new, steady-state restrictions.
     */
    public void initRestrictionsCompleted(P2PPortal portal);

    public PhasingSchedule getNextPhase(
        Restrictions restrs,
        int nDataAvailable);
}
