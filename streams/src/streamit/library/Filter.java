package streamit;

// a filter is the lowest-level block of streams
public abstract class Filter extends Stream 
{
    public Filter() 
    {
        super();
    }

    public Filter(int i) 
    {
        super(i);
    }

    public Filter(float f) 
    {
        super(f);
    }

    public Filter(String str) 
    {
        super(str);
    }

    // Add was present in Operator, but is not defined in Filter anymore
    public void Add(Stream s) { ASSERT (false); }
    
    // ConnectGraph doesn't connect anything for a Filter,
    // but it can register all sinks:
    // also make sure that any input/output point to the filter itself
    public void ConnectGraph ()
    {
        Channel myInput = GetIOField ("input");
        Channel myOutput = GetIOField ("output");
        
        if (myOutput != null)
        {
            myOutput.SetSource (this);
        } else {
            AddSink ();
        }
        
        if (myInput != null)
        {
            myInput.SetSink (this);
        }
    }

    public abstract void Work();
    
    // provide some empty functions to make writing filters a bit easier
    public void Init () { }
}

