package streamit;

import streamit.scheduler.iriter.SplitJoinIter;
import java.util.ArrayList;
import java.util.Iterator;

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
        // not tested yet
        ASSERT (0);
        
        int numChildren = srcs.size ();
        int [] weights = new int [numChildren];
        
        int i;
        for (i=0;i<numChildren;i++)
        {
            if (((Stream)srcs.get (i)).input != null)
            {
                weights [i] = weight;
            }
        }
        
        return weights;
    }
    
    public int getProduction ()
    {
        // not tested yet
        ASSERT (0);
        
        int numChildren = srcs.size ();
        int outputTotal = 0;
        
        int i;
        for (i=0;i<numChildren;i++)
        {
            if (((Stream)srcs.get (i)).input != null)
            {
                outputTotal += weight;
            }
        }
        
        return outputTotal;
    }
}
