package streamit.misc;

public class OMapIterator extends AssertedClass
{
    OSetIterator setIter;
    
    public OMapIterator (OSetIterator _setIter)
    {
        ASSERT (setIter);
        
        setIter = _setIter;
    }
    
    public Object getKey ()
    {
        return ((Pair)setIter.get()).first;
    }

    public Object getData ()
    {
        return ((Pair)setIter.get()).second;
    }
    
    public void next () 
    {
        setIter.next ();
    }
    
    public void prev ()
    {
        setIter.prev ();
    }
}