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

    public abstract void Work();
}

