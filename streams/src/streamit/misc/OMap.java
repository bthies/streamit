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

/* $Id: OMap.java,v 1.6 2004-01-28 21:17:13 dmaze Exp $ */

public class OMap
{
    private final OSet set;
    private final OMapIterator last;

    public final static Comperator defaultComperator = new MapComperator();

    static class MapComperator implements Comperator
    {
        final Comperator comparator;
        public MapComperator()
        {
            comparator = RBTree.defaultComperator;
        }

        public MapComperator(Comperator customComperator)
        {
            comparator = customComperator;
        }

        public boolean isLess(Object left, Object right)
        {
            assert left instanceof Pair;
            assert right instanceof Pair;

            return comparator.isLess(((Pair)left).first, ((Pair)right).first);
        }
    }

    public OMap()
    {
        set = new OSet(defaultComperator);
        last = new OMapIterator(set.end());
    }

    public OMap(Comperator customComperator)
    {
        set = new OSet(new MapComperator(customComperator));
        last = new OMapIterator(set.end());
    }
    
    public OMapIterator begin ()
    {
        return new OMapIterator(set.begin ());
    }
    
    public OMapIterator end ()
    {
        return last.copy ();
    }

    public int size()
    {
        return set.size();
    }

    public boolean empty()
    {
        return set.empty();
    }

    public Pair insert(Object key, Object data)
    {
        Pair result = set.insert(new Pair(key, data));
        return new Pair(
            new OMapIterator((OSetIterator)result.first),
            result.second);
    }

    public void erase(OMapIterator iter)
    {
        set.erase(iter.setIter);
    }

    public void erase(Object key)
    {
        OSetIterator node = set.find(new Pair(key, null));
        if (node != set.end())
            set.erase(node);
    }

    public OMapIterator find(Object key)
    {
        return new OMapIterator(set.find(new Pair (key, null)));
    }
    
    public OMapIterator upper_bound(Object key)
    {
        return new OMapIterator(set.upper_bound(key));
    }

    public OMapIterator lower_bound(Object key)
    {
        return new OMapIterator(set.upper_bound(key));
    }
}
