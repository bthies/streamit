package streamit.scheduler2.minlatency;

/* $Id: StreamFactory.java,v 1.2 2003-05-06 10:23:55 thies Exp $ */

import streamit.misc.DestroyedClass;
import streamit.scheduler2.iriter.Iterator;
import streamit.scheduler2.base.StreamInterface;
import java.util.HashMap;

/**
 * This class basically implements the StreamFactory interface.  In the 
 * current, first draft, this class will just create single appearance
 * schedule objects, so the resulting schedules will be single appearance.
 */

public class StreamFactory
    extends DestroyedClass
    implements streamit.scheduler2.base.StreamFactory
{
    final float phaseFrac;
    
    public StreamFactory (float _phaseFrac)
    {
        phaseFrac = _phaseFrac;
    }
    
    final HashMap flAncestor = new HashMap ();

    public StreamInterface newFrom(Iterator streamIter, Iterator parent)
    {
        
        if (parent == null) 
        {
            flAncestor.put(streamIter, new Boolean (true));
        } else if (parent.isFeedbackLoop () != null)
        {
            flAncestor.put(streamIter, new Boolean (false));
        } else {
            flAncestor.put(streamIter, flAncestor.get(parent));
        }
        
        boolean usePhaseFrac = ((Boolean)flAncestor.get(streamIter)).booleanValue();
        float localPhaseFrac = (usePhaseFrac ? phaseFrac : 0);
        
        if (streamIter.isFilter() != null)
        {
            return new Filter(streamIter.isFilter(), localPhaseFrac);
        }

        if (streamIter.isPipeline() != null)
        {
            return new Pipeline(streamIter.isPipeline(), localPhaseFrac, this);
        }
        
        if (streamIter.isSplitJoin() != null)
        {
            return new SplitJoin(streamIter.isSplitJoin(), localPhaseFrac, this);
        }

        if (streamIter.isFeedbackLoop() != null)
        {
            return new FeedbackLoop(streamIter.isFeedbackLoop(), localPhaseFrac, this);
        }

        ERROR ("Unsupported type passed to StreamFactory!");
        return null;
    }
}
