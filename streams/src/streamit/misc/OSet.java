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
        Pair result = tree.insert(data);
        return new Pair(
            new OSetIterator((RBNode) result.first),
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
}
