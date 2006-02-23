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

package streamit.library.iriter;

import streamit.library.FeedbackLoop;
import streamit.library.Filter;
import streamit.library.Pipeline;
import streamit.library.SplitJoin;
import streamit.library.Stream;

import java.util.Arrays;
import java.util.HashSet;

/**
 * This is an iterator factory that views the stream graph as needed
 * for SDEP computations.  Currently this view is as follows:
 *
 * 1. If the message sender has phases, then only those phases that do
 *    I/O in the direction of the message are visible.  For example,
 *    for a downstream message, any sender phase that pushes items is
 *    visible.
 *
 * 2. If the message receiver has phases, then only those phases that
 *    do I/O in the direction of hte message are visible.  For example,
 *    for a downstream message, any receiver phase that pops items is
 *    visible.
 *
 * 3. For all other filters, only a single prework/work phase is
 *    visible (even if it has more fine-grained phases).
 *

 * This view enables static scheduling of messages across phased
 * filters where the overall I/O rate is known, but the order of
 * phases is not.  It also allows message sending and receiving in a
 * fine-grained way in phased filters, even for filters where the
 * interleaving of certain phases (that do not affect the message
 * timing) is unknown.
 */
public class SDEPIterFactory implements IterFactory {
    /**
     * Sender of the message for the SDEP calculation.
     */
    private Filter sender;
    /**
     * Receivers (Streams) of the message for the SDEP calculation.
     */
    private HashSet receivers;
    /**
     * Whether or not the receiver is downstream of the sender in the
     * stream graph.
     */
    private boolean downstream;
    
    /**
     * Construct a factory for an SDEP calculation between <sender>
     * and <receivers>, which should be Streams, of a given message.
     */
    public SDEPIterFactory(Stream sender, HashSet receivers, boolean downstream) {
        // currently only support messages between filters
        assert sender instanceof Filter;

        this.sender = (Filter)sender;
        this.receivers = receivers;
        this.downstream = downstream;
    }

    /**
     * Construct a factory for an SDEP calculation between <sender>
     * and <receiver> (of a given message).
     */
    public SDEPIterFactory(Stream sender, Stream receiver, boolean downstream) {
        this(sender, new HashSet(Arrays.asList(new Stream[] { receiver })), downstream);
        // currently only support messages between filters
        assert sender instanceof Filter;
        assert receiver instanceof Filter;
    }

    /**
     * Returns whether or not <sender> is one of the senders of this.
     */
    public boolean containsSender(Filter sender) {
        return this.sender == sender;
    }

    /**
     * Returns whether or not <receiver> is one of the receivers of this.
     */
    public boolean containsReceiver(Filter receiver) {
        return receivers.contains(receiver);
    }

    /**
     * Returns whether or not receiver is downstream of sender.
     */
    public boolean isDownstream() {
        return downstream;
    }

    /**
     * Returns a new SDEP iterator for <filter>.
     */
    public streamit.scheduler2.iriter.FilterIter newFrom(Filter filter) {
        return new streamit.library.iriter.SDEPFilterIter((Filter) filter, this);
    }

    /**
     * Returns a new SDEP iterator for <pipeline>.
     */
    public streamit.scheduler2.iriter.PipelineIter newFrom(Pipeline pipeline) {
        return new streamit.library.iriter.PipelineIter((Pipeline) pipeline, this);
    }
    
    /**
     * Returns a new SDEP iterator for <sj>.
     */
    public streamit.scheduler2.iriter.SplitJoinIter newFrom(SplitJoin sj) {
        return new streamit.library.iriter.SplitJoinIter((SplitJoin) sj, this);
    }
    
    /**
     * Returns a new SDEP iterator for <fl>.
     */
    public streamit.scheduler2.iriter.FeedbackLoopIter newFrom(FeedbackLoop fl) {
        return new streamit.library.iriter.FeedbackLoopIter((FeedbackLoop) fl, this);
    }
}
