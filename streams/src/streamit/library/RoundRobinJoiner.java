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

public class RoundRobinJoiner extends Joiner 
{
    int weight;

    RoundRobinJoiner (int weight)
    {
        this.weight = weight;
    }

    /*

    This work function has been replaced by a single work function
    in Joiner (parent of this class)
    
    public void work ()
    {
        int inputIndex;
        for (inputIndex = 0; inputIndex < srcs.size (); inputIndex++)
        {
            assert input [inputIndex] != 0;
            int w;
            for (w = 0; w < weight; w++)
            {
                passOneData (input [inputIndex], output);
            }
        }
    }
    */

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

    public String toString() {
	return "roundrobin";
    }
}
