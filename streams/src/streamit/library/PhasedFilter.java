package streamit;

public abstract class PhasedFilter extends Stream
{
    public PhasedFilter()
    {
        super();
    }
    
    // add was present in Operator, but is not defined in Filter anymore
    public void add(Stream s) { ASSERT (false); }

    // This function should definitely do something.  Don't support
    // old-style init functions, though.
    public void connectGraph()
    {
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
}

