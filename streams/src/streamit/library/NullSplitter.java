package streamit.library;

public class NullSplitter extends Splitter
{
    public void work ()
    {
        // a null splitter should never have its work function called
        ERROR ("work function called in a NullSplitter");  
    }

    public int [] getWeights ()
    {
        // null joiners do not distribute any weights
        ASSERT (false);
        return null;
    }
    
    public int getConsumption () { return 0; }
}
