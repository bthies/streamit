package streamit.library;

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
        int numChildren = dest.size ();
        int [] weights = new int [numChildren];
        
        int i;
        for (i=0;i<numChildren;i++)
        {
            if (dest.get(i) != null && ((Stream)dest.get (i)).input != null)
            {
                weights [i] = weight;
            }
        }
        
        return weights;
    }

    public int getConsumption ()
    {
        int numChildren = dest.size ();
        int inputTotal = 0;
        
        int i;
        for (i=0;i<numChildren;i++)
        {
            if (dest.get(i) != null && ((Stream)dest.get (i)).input != null)
            {
                inputTotal += weight;
            }
        }
        
        return inputTotal;
    }

    public String toString() {
	return "roundrobin";
    }
}
