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

import java.util.*;

public class WeightedRoundRobinSplitter extends Splitter
{
    List destWeight = new ArrayList ();

    void addWeight (Integer weight)
    {
        assert weight != null && weight.intValue () >= 0;
        destWeight.add (weight);
    }

    public boolean isOutputUsed (int index)
    {
        assert index < destWeight.size ();
        return ((Integer)destWeight.get(index)).intValue () != 0;
    }

    public void connectGraph ()
    {
        // do I even have anything to do?
        assert dest.size () == destWeight.size ();

        super.connectGraph ();
    }

    /*
    
    This work function has been replaced by a work function in Splitter
    which is the parent of this class
    
    public void work ()
    {
        assert destWeight.size () == dest.size ();

        int outputIndex;
        for (outputIndex = 0; outputIndex < dest.size (); outputIndex++)
        {
            int outputCount;
            for (outputCount = ((Integer)destWeight.get (outputIndex)).intValue (); outputCount > 0 ; outputCount--)
            {
                passOneData (input, output [outputIndex]);
            }
        }
    }
    */

    public int [] getWeights ()
    {
        int numChildren = dest.size ();
        int [] weights = new int [numChildren];
        
        int i;
        for (i=0;i<numChildren;i++)
        {
            if (dest.get(i) != null && ((Stream)dest.get (i)).input != null)
            {
                weights [i] = ((Integer)destWeight.get (i)).intValue ();
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
                inputTotal += ((Integer)destWeight.get (i)).intValue ();
            }
        }
        
        return inputTotal;
    }

    public String toString() {
	int[] weights = getWeights();
	StringBuffer result = new StringBuffer("roundrobin(");
	for (int i=0; i<weights.length; i++) {
	    result.append(weights[i]);
	    if (i!=weights.length-1) {
		result.append(", ");
	    } else {
		result.append(")");
	    }
	}
	return result.toString();
    }
}
