package streamit.library;

public class DuplicateSplitter extends Splitter
{
    public void work ()
    {
        duplicateOneData (input, output);
    }

    public int [] getWeights ()
    {
        int numChildren = dest.size ();
        int [] weights = new int [numChildren];
        
        int i;
        for (i=0;i<numChildren;i++)
        {
            if (dest.get(i) != null && ((Stream)dest.get (i)).input != null)
            {
                weights [i] = 1;
            }
        }
        
        return weights;
    }
    
    public int getConsumption ()
    {
        return 1;
    }
}
