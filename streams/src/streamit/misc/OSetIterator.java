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

public class OSetIterator extends AssertedClass
{
    RBNode node;

    OSetIterator(RBNode n)
    {
        node = n;
    }

    public OSetIterator copy()
    {
        return new OSetIterator(node);
    }

    public boolean equals(OSetIterator other)
    {
        return node == other.node;
    }

    public Object get()
    {
        // make sure that node != NULL
        ASSERT (node.right != null);
        return node.getData ();
    }

    public void next()
    {
        // make sure that next NULL node exists, that is 
        // node != NULL and node is in the tree
        ASSERT(node.right != null);

        RBNode currentNode = node;

        // if I'm not looking at a logical leaf
        if (currentNode.right.right != null)
        {
            currentNode = currentNode.right;
            while (currentNode.left.right != null)
                currentNode = currentNode.left;

            node = currentNode;
            return;
        }

        RBNode NULL = currentNode.right;
        RBNode parent = currentNode.parent;

        while (parent != null && currentNode == parent.right)
        {
            currentNode = parent;
            parent = currentNode.parent;
        }

        if (parent == null)
            parent = NULL;

        node = parent;
    }

    public void prev ()
    {
        // if node has no children (either node == NULL or node has
        // been removed from the tree), get the maximal node
        // from the tree:
        if (node.right == null)
        {
            RBTree tree = (RBTree)node.nodeData;
            
            // make sure that the node hasn't been removed from the tree
            ASSERT (tree);
            
            node = tree.getMax();
            return;
        }

        // okay, I got a legitimate node here
        RBNode currentNode = node;

        // if I'm not looking at a logical leaf
        if (currentNode.left.right != null)
        {
            currentNode = currentNode.left;
            while (currentNode.right.right != null)
                currentNode = currentNode.right;
                
            node = currentNode;
            return;
        }

        RBNode parent = currentNode.parent;

        while (parent != null && currentNode == parent.left)
        {
            currentNode = parent;
            parent = currentNode.parent;
        }
        
        // if parent == null, I have asked for a prev of the first
        // element which is illegal!
        ASSERT (parent != null);

        node = parent;
    }

}
