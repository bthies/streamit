package streamit.misc;

import streamit.misc.AssertedClass;
import java.util.Map;
import java.util.HashMap;
import streamit.misc.Pair;

/* $Id: UniquePairContainer.java,v 1.1 2002-05-24 23:10:36 karczma Exp $ */

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

public class UniquePairContainer extends AssertedClass
{
    private Map firstMap = new HashMap ();
    
    public Pair getPair (Object first, Object second)
    {
        Map secondMap;
        secondMap = (Map)firstMap.get(first);
        
        if (secondMap == null)
        {
            secondMap = new HashMap ();
            firstMap.put(first, secondMap);
        }
        
        Pair pair;
        pair = (Pair) secondMap.get (second);
        
        if (pair == null)
        {
            pair = new Pair (first, second);
            secondMap.put(second, pair);
        }
        
        return pair;
    }
}
