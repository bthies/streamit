package streamit;

import java.util.ArrayList;
import java.util.Iterator;

public class DuplicateSplitter extends Splitter
{
    public void work ()
    {
        duplicateOneData (input, output);
    }

    public int [] getWeights ()
    {
        // not tested yet
        ASSERT (0);
        
        int numChildren = dest.size ();
        int [] weights = new int [numChildren + 1];
        int inputTotal = 0;
        
        int i;
        for (i=0;i<numChildren;i++)
        {
            if (((Stream)dest.get (i)).input != null)
            {
                weights [i + 1] = 1;
                inputTotal ++;
            }
        }
        
        weights [0] = inputTotal;
        
        return weights;
    }
}
