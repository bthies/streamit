package streamit.misc;

/* $Id: RBNode.java,v 1.1 2003-02-14 19:00:47 karczma Exp $ */

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
    
    boolean isRed ()
    {
        return !black;
    }

    boolean isBlack ()
    {
        return black;
    }
}