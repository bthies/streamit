package streamit.misc;

public class OMapIterator extends AssertedClass
{
    OSetIterator setIter;

    OMapIterator(OSetIterator _setIter)
    {
        ASSERT(_setIter);

        setIter = _setIter;
    }

    public Object getKey()
    {
        return ((Pair)setIter.get()).first;
    }

    public Object getData()
    {
        return ((Pair)setIter.get()).second;
    }

    public void next()
    {
        setIter.next();
    }

    public void prev()
    {
        setIter.prev();
    }

    public OMapIterator copy()
    {
        return new OMapIterator(setIter);
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof OMapIterator))
            return false;

        return (setIter.equals(((OMapIterator)other).setIter));
    }
}