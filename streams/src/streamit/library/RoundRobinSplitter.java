/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

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
