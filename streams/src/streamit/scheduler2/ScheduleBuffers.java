package streamit.scheduler;

import streamit.misc.AssertedClass;
import streamit.misc.UniquePairContainer;
import streamit.misc.Pair;
import java.util.Map;
import java.util.HashMap;
import streamit.scheduler.iriter.Iterator;
import streamit.scheduler.Schedule;

/* $Id: ScheduleBuffers.java,v 1.1 2002-05-24 23:10:31 karczma Exp $ */

/**
 * <dl>
 * <dt>Purpose: Extract and Store Buffer Sizes for a Schedule
 * <dd>
 *
 * <dt>Description:
 * <dd> This class uses a valid schedule and an iterator to determine 
 * the size of buffers required to execute the schedule.  The class
 * makes an assumption that the buffers will not be shared.
 * </dl>
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

public class ScheduleBuffers extends AssertedClass
{
    private Map bufferSizes = new HashMap ();
    private UniquePairContainer pairs = new UniquePairContainer ();
    private Iterator root;
    private Schedule schedule;
    
    public ScheduleBuffers (Iterator _root, Schedule _schedule)
    {
        root = _root;
        schedule = _schedule;
        
        // probably want to compute the buffer sizes here:
        ASSERT (false);
    }
    
    private void setBufferSizeBetween (Object first, Object second, Integer size)
    {
        ASSERT (first);
        ASSERT (second);
        ASSERT (size);
        
        Pair pair = pairs.getPair (first, second);
        ASSERT (pair);
        
        bufferSizes.put (pair, size);
    }
    
    public int getBufferSizeBetween (Object first, Object second)
    {
        Pair pair = pairs.getPair (first, second);
        ASSERT (pair);
        
        Integer size = (Integer) bufferSizes.get (pair);
        ASSERT (size);
        return size.intValue ();
    }
}
