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

package streamit.misc;

import java.util.Map;
import java.util.HashMap;
import streamit.misc.Pair;

/* $Id: UniquePairContainer.java,v 1.5 2006-09-25 13:54:55 dimock Exp $ */

/**
 * <dl>
 * <dt> Purpose: Provide a source of Pairs for tuples of objects.
 * <dd>
 *
 * <dt> Description:
 * <dd> Given a tuple of objects, this class will return a Pair object
 * that holds both of these objects.  On all subsequent requests
 * using the same two objects, the same Pair will be returned.
 * </dl>
 * 
 * @version 1
 * @author  Michal Karczmarek
 */

public class UniquePairContainer
{
    private Map<Object, Map> firstMap = new HashMap<Object, Map> ();
    
    public Pair getPair (Object first, Object second)
    {
        Map<Object, Pair> secondMap;
        secondMap = firstMap.get(first);
        
        if (secondMap == null)
            {
                secondMap = new HashMap<Object, Pair> ();
                firstMap.put(first, secondMap);
            }
        
        Pair pair;
        pair = secondMap.get (second);
        
        if (pair == null)
            {
                pair = new Pair (first, second);
                secondMap.put(second, pair);
            }
        
        return pair;
    }
}
