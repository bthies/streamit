package streamit.misc;

/* $Id: RBNode.java,v 1.2 2003-02-19 20:18:39 karczma Exp $ */

public class RBNode extends AssertedClass 
{
    Object nodeData;
    RBNode left = null, right = null, parent = null;
    boolean black = false;
    
    RBNode (Object data)
    {
        ASSERT (data);
        nodeData = data;
    }
    
    public Object getData ()
    {
        return nodeData;
    }
    
    boolean isRed ()
    {
        return !black;
    }

    boolean isBlack ()
    {
        return black;
    }
}