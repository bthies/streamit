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

package streamit.library;

import streamit.scheduler2.Scheduler;

public abstract class PhasedFilter extends Stream
{
    private Class inType, outType;
    
    public PhasedFilter() { super(); }
    public PhasedFilter(int a) { super(a); }
    
    // add was present in Operator, but is not defined in Filter anymore
    public void add(Stream s) { ASSERT (false); }

    public void setIOTypes(Class in, Class out)
    {
        inType = in;
        outType = out;
    }

    // This function should definitely do something.  Don't support
    // old-style init functions, though.
    public void connectGraph()
    {
        // Create channels from the setIOTypes() types.
        input = new Channel(inType);
        output = new Channel(outType);
        
        Channel myInput = getInputChannel();
        Channel myOutput = getOutputChannel();
        
        if (myOutput != null)
            myOutput.setSource(this);
        else
            addSink();
        
        if (myInput != null)
            myInput.setSink(this);
        
        addFilter();
    }

    public abstract void work();
    
    public void init() { invalidInitError(); }    

    public void phase(WorkFunction wf)
    {
        // Placeholder?
        wf.work();
    }

    void setupBufferLengths (Scheduler buffers)
    {
        // Filter claims this doesn't need to do anything, sure.
    }
}

