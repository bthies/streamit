package streamit.library;

public class RoundRobinJoiner extends Joiner 
{
    int weight;

    RoundRobinJoiner (int weight)
    {
        this.weight = weight;
    }

    public void work ()
    {
        int inputIndex;
        for (inputIndex = 0; inputIndex < srcs.size (); inputIndex++)
        {
            ASSERT (input [inputIndex]);
            int w;
            for (w = 0; w < weight; w++)
            {
                passOneData (input [inputIndex], output);
            }
        }
    }

    public int [] getWeights ()
    {
        int numChildren = srcs.size ();
        int [] weights = new int [numChildren];
        
        int i;
        for (i=0;i<numChildren;i++)
        {
            if (srcs.get(i) != null && ((Stream)srcs.get (i)).output != null)
            {
                weights [i] = weight;
            }
        }
        
        return weights;
    }
    
    public int getProduction ()
    {
        int numChildren = srcs.size ();
        int outputTotal = 0;
        
        int i;
        for (i=0;i<numChildren;i++)
        {
            if (srcs.get(i) != null && ((Stream)srcs.get (i)).output != null)
            {
                outputTotal += weight;
            }
        }
        
        return outputTotal;
    }
}
