package streamit.library;

public class NullJoiner extends Joiner
{
    public void work ()
    {
        // a null joiner should never have its work function called
        ERROR ("work function called in a NullJoiner");  
    }
    
    public int [] getWeights ()
    {
        // null joiners do not distribute any weights
        ASSERT (false);
        return null;
    }
    
    public int getProduction () { return 0; }

}
