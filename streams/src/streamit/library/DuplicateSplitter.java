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

public class DuplicateSplitter extends Splitter
{
    
    /* this work function remains here (instead of using work function
       in the parent) because it copies single data to many channels
       and remains atomic */
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

    public String toString() {
	return "duplicate";
    }
}
