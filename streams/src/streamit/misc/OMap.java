package streamit.misc;

/* $Id: OMap.java,v 1.1 2003-03-17 22:51:12 karczma Exp $ */

public class OMap extends AssertedClass
{
    private final OSet set;
    private final OMapIterator last;

    public final static Comperator defaultComperator = new MapComperator();

    static class MapComperator extends AssertedClass implements Comperator
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
            ASSERT(left instanceof Pair);
            ASSERT(right instanceof Pair);

            return comparator.isLess(((Pair)left).first, ((Pair)right).second);
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
}
