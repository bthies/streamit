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

package streamit.scheduler2.singleappearance;

import streamit.scheduler2.iriter.Iterator;
import streamit.scheduler2.base.StreamInterface;

public class Scheduler extends streamit.scheduler2.Scheduler
{
    final StreamInterface rootStream;
    StreamFactory factory = new StreamFactory ();
     
    public Scheduler(Iterator _root)
    {
        super (_root);
        
        rootStream = factory.newFrom(root, null); 
    }
    
    public void computeSchedule()
    {
        if (steadySchedule != null) return;
        
        rootStream.computeSchedule();
        
        initSchedule = rootStream.getInitSchedule();
        steadySchedule = rootStream.getSteadySchedule();
    }
}


