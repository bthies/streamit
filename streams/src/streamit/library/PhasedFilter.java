package streamit.library;

import streamit.scheduler2.ScheduleBuffers;

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

    void setupBufferLengths (ScheduleBuffers buffers)
    {
        // Filter claims this doesn't need to do anything, sure.
    }
}

