package streamit;

import java.util.ArrayList;
import java.util.Iterator;

public class RoundRobinSplitter extends Splitter
{
    int weight;
    RoundRobinSplitter (int weight)
    {
        this.weight = weight;
    }
    public void work ()
    {
        int outputIndex;
        for (outputIndex = 0; outputIndex < dest.size (); outputIndex++)
        {
            int w;
            for (w = 0; w < weight; w++)
            {
                passOneData (input, output [outputIndex]);
            }
        }
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
                weights [i + 1] = weight;
                inputTotal += weight;
            }
        }
        
        weights [0] = inputTotal;
        
        return weights;
    }
}
