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

    public Filter(String str) 
    {
	super(str);
    }

    public Filter(Stream str) 
    {
	super(str);
    }

    // Add was present in Operator, but is not defined in Filter anymore
    public void Add(Stream s) { ASSERT (false); }

    public abstract void Work();
}

