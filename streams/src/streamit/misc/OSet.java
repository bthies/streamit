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

/**
 * 
 * Set is a Java equivalent of the STL set class.
 * For usage information, check STL documentation
 * 
 */

public class OSet
{
    private final RBTree tree;
    private final OSetIterator last;

    public OSet()
    {
        tree = new RBTree();
        last = new OSetIterator(tree.getNULL());
    }

    public OSet(Comperator customComperator)
    {
        tree = new RBTree(customComperator);
        last = new OSetIterator(tree.getNULL());
    }

    public OSetIterator begin()
    {
        return new OSetIterator(tree.getMin());
    }

    public OSetIterator end()
    {
        return new OSetIterator(tree.getNULL());
    }

    public int size()
    {
        return tree.size();
    }

    public boolean empty()
    {
        return tree.size() == 0;
    }

    public Pair insert(Object data)
    {
        Pair result = tree.insert(data, false);
        return new Pair(
            new OSetIterator((RBNode)result.first),
            result.second);
    }

    public void erase(OSetIterator iter)
    {
        if (iter.node != tree.getNULL())
            tree.erase(iter.node);
    }

    public void erase(Object data)
    {
        RBNode node = tree.find(data);
        if (node != tree.getNULL())
            tree.erase(node);
    }

    public OSetIterator find(Object data)
    {
        return new OSetIterator(tree.find(data));
    }

    public OSetIterator upper_bound(Object key)
    {
        return new OSetIterator(tree.upper_bound(key));
    }

    public OSetIterator lower_bound(Object key)
    {
        return new OSetIterator(tree.upper_bound(key));
    }
}
