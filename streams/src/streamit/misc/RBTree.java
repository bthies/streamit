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

/* $Id: RBTree.java,v 1.9 2004-01-28 21:17:13 dmaze Exp $ */

public class RBTree
{
    private RBNode root;

    public static HashComperator defaultComperator = new HashComperator();
    private final Comperator myComperator;

    private int size = 0;
    private final RBNode NULL;

    RBTree()
    {
        myComperator = defaultComperator;
        NULL = new RBNode(this);
        NULL.black = true;
        root = NULL;
    }

    public RBTree(Comperator customComperator)
    {
        myComperator = customComperator;
        NULL = new RBNode(this);
        NULL.black = true;
        root = NULL;
    }

    public int size()
    {
        return size;
    }

    public boolean empty()
    {
        return size == 0;
    }

    public RBNode getNULL()
    {
        return NULL;
    }

    public RBNode getMin()
    {
        RBNode min = root;

        if (min == NULL)
            return min;

        while (min.left != NULL)
        {
            min = min.left;
        }

        return min;
    }

    public RBNode getMax()
    {
        RBNode max = root;

        if (max == NULL)
            return max;

        while (max.right != NULL)
        {
            max = max.right;
        }

        return max;
    }

    public RBNode find(Object data)
    {
        assert data != null;

        RBNode node = root;

        while (node != NULL)
        {
            if (myComperator.isLess(data, node.nodeData))
            {
                node = node.left;
            }
            else if (myComperator.isLess(node.nodeData, data))
            {
                node = node.right;
            }
            else
            {
                break;
            }
        }

        return node;
    }

    public RBNode upper_bound(Object data)
    {
        RBNode upperBound = NULL;
        RBNode node = root;

        while (node != NULL)
        {
            if (myComperator.isLess(data, node.nodeData))
            {
                upperBound = node;
                node = node.left;
            }
            else
            {
                node = node.right;
            }
        }

        return upperBound;
    }

    public RBNode lower_bound(Object data)
    {
        RBNode lowerBound = NULL;
        RBNode node = root;

        while (node != NULL)
        {
            if (myComperator.isLess(data, node.nodeData))
            {
                lowerBound = node;
                node = node.left;
            }
            if (myComperator.isLess(node.nodeData, data))
            {
                node = node.right;
            }
            else
            {
                return node;
            }
        }

        return lowerBound;
    }

    public Pair insert(Object insertData, boolean replace)
    {
        assert insertData != null;

        // handle the case of empy tree
        if (root == NULL)
        {
            root = new RBNode(insertData);
            root.black = true;

            // setup the sentinell NULL
            root.left = NULL;
            root.right = NULL;

            size = size + 1;
            return new Pair(root, Boolean.TRUE);
        }

        // not an empty tree
        // find my insertion point
        RBNode insertionPoint = root;
        while (true)
        {
            Object parentData = insertionPoint.nodeData;
            if (myComperator.isLess(insertData, parentData))
            {
                // insertData < parentData => go left
                if (insertionPoint.left != NULL)
                {
                    // have a left sub-tree - continue to search
                    insertionPoint = insertionPoint.left;
                }
                else
                {
                    // reached a leaf - insert the data and get out
                    insertionPoint.left = new RBNode(insertData);
                    insertionPoint.left.parent = insertionPoint;
                    insertionPoint = insertionPoint.left;

                    // setup the sentinell NULL
                    insertionPoint.left = NULL;
                    insertionPoint.right = NULL;

                    size = size + 1;
                    break;
                }
            }
            else
            {
                // insertData >= parentData
                if (myComperator.isLess(parentData, insertData))
                {
                    // insertData > parentData => go right
                    if (insertionPoint.right != NULL)
                    {
                        // have a right sub-tree - continue to search
                        insertionPoint = insertionPoint.right;
                    }
                    else
                    {
                        // reached a leaf - insert the data and get out
                        insertionPoint.right = new RBNode(insertData);
                        insertionPoint.right.parent = insertionPoint;
                        insertionPoint = insertionPoint.right;

                        // setup the sentinell NULL
                        insertionPoint.left = NULL;
                        insertionPoint.right = NULL;

                        size = size + 1;
                        break;
                    }
                }
                else
                {
                    // insertData == parentData =>
                    // there already exists a node with the key equal
                    // to the key in insertData
                    // if the user so requests, replace the old data with
                    // insertData.
                    if (replace)
                    {
                        insertionPoint.nodeData = insertData;
                    }

                    return new Pair(insertionPoint, Boolean.FALSE);
                }
            }
        }

        // the node has been inserted. it is red (by design of RBNode).
        // now I am going to rebalance the tree.
        // this code is taken verbatim from CLR's 
        // Introduction to Algorithms. hope they don't mind :)
        RBNode currNode = insertionPoint;
        while (currNode != root && currNode.parent.isRed())
        {
            // currNode is not the root.
            // the parent is RED. i know (by design) that the root
            // is always BLACK, so the parent cannot be the root either.
            RBNode parent = currNode.parent;
            RBNode grandparent = parent.parent;
            if (parent == grandparent.left)
            {
                RBNode uncle = grandparent.right;
                if (uncle != NULL && uncle.isRed())
                {
                    parent.black = true;
                    uncle.black = true;
                    grandparent.black = false;
                    currNode = grandparent;
                }
                else
                {
                    if (currNode == parent.right)
                    {
                        leftRotate(parent);

                        // swap parent and currNode after the rotation
                        RBNode temp = currNode;
                        currNode = parent;
                        parent = temp;
                    }

                    parent.black = true;
                    grandparent.black = false;
                    rightRotate(grandparent);
                }
            }
            else
            {
                RBNode uncle = grandparent.left;
                if (uncle != NULL && uncle.isRed())
                {
                    parent.black = true;
                    uncle.black = true;
                    grandparent.black = false;
                    currNode = grandparent;
                }
                else
                {
                    if (currNode == parent.left)
                    {
                        rightRotate(parent);

                        // swap parent and currNode after the rotation
                        RBNode temp = currNode;
                        currNode = parent;
                        parent = temp;
                    }

                    parent.black = true;
                    grandparent.black = false;
                    leftRotate(grandparent);
                }
            }
        }

        root.black = true;
        return new Pair(insertionPoint, Boolean.TRUE);
    }

    public void erase(RBNode node)
    {
        RBNode splicedChild, splicedNode;

        if (node.left == NULL || node.right == NULL)
            splicedNode = node;
        else
        {
            // know there is successor 'cause right != NULL
            splicedNode = successor(node);
        }

        if (splicedNode.left != NULL)
            splicedChild = splicedNode.left;
        else
            splicedChild = splicedNode.right;

        splicedChild.parent = splicedNode.parent;

        if (splicedNode.parent == null)
        {
            root = splicedChild;
        }
        else
        {
            if (splicedNode == splicedNode.parent.left)
            {
                splicedNode.parent.left = splicedChild;
            }
            else
            {
                splicedNode.parent.right = splicedChild;
            }
        }

        boolean splicedBlack = splicedNode.isBlack();

        if (node != splicedNode)
        {
            // now swap the splicedNode into the place of node
            // I can't just exchange their data, because
            // then iterators might get messed up!
            splicedNode.parent = node.parent;
            splicedNode.black = node.black;
            splicedNode.left = node.left;
            splicedNode.right = node.right;

            // fix node's children:
            splicedNode.left.parent = splicedNode;
            splicedNode.right.parent = splicedNode;

            // and node's parent:
            if (splicedNode.parent == null)
            {
                root = splicedNode;
            }
            else
            {
                if (splicedNode.parent.left == node)
                {
                    splicedNode.parent.left = splicedNode;
                }
                else
                {
                    splicedNode.parent.right = splicedNode;
                }
            }

            // set node's left, right, parent AND nodeData
            // to be null (not even NULL)
            node.left = null;
            node.right = null;
            node.parent = null;
            node.nodeData = null;
        }

        if (splicedBlack)
        {
            // perform the fixup from CLR
            RBDeleteFixup(splicedChild);
        }

        size = size - 1;
    }

    private void leftRotate(RBNode pivot)
    {
        // this code is taken verbatim from CLR's 
        // Introduction to Algorithms. hope they don't mind :)
        assert pivot != NULL;
        assert pivot.right != NULL;

        RBNode right = pivot.right;
        pivot.right = right.left;
        if (right.left != NULL)
            right.left.parent = pivot;
        right.parent = pivot.parent;
        if (pivot.parent == null)
            root = right;
        else if (pivot == pivot.parent.left)
            pivot.parent.left = right;
        else
            pivot.parent.right = right;

        // put pivot on right's left
        right.left = pivot;
        pivot.parent = right;
    }

    private void rightRotate(RBNode pivot)
    {
        // this code is taken verbatim from CLR's 
        // Introduction to Algorithms. hope they don't mind :)
        assert pivot != NULL;
        assert pivot.left != NULL;

        RBNode left = pivot.left;
        pivot.left = left.right;
        if (left.right != NULL)
            left.right.parent = pivot;
        left.parent = pivot.parent;
        if (pivot.parent == null)
            root = left;
        else if (pivot == pivot.parent.left)
            pivot.parent.left = left;
        else
            pivot.parent.right = left;

        // put pivot on left's right
        left.right = pivot;
        pivot.parent = left;
    }

    public RBNode successor(RBNode node)
    {
        assert node != NULL;

        if (node.right != NULL)
        {
            node = node.right;
            while (node.left != NULL)
                node = node.left;
            return node;
        }

        RBNode parent = node.parent;

        while (parent != null && node == parent.right)
        {
            node = parent;
            parent = node.parent;
        }

        return parent;
    }

    public RBNode predecessor(RBNode node)
    {
        assert node != null;

        if (node.left != NULL)
        {
            node = node.left;
            while (node.right != NULL)
                node = node.right;
            return node;
        }

        RBNode parent = node.parent;

        while (parent != null && node == parent.left)
        {
            node = parent;
            parent = node.parent;
        }

        return parent;
    }

    private void RBDeleteFixup(RBNode node)
    {
        while (node != root && node.isBlack())
        {
            // since the node is BLACK, I must have a sibling!
            RBNode parent = node.parent;
            if (node == parent.left)
            {
                RBNode sibling = parent.right;
                if (sibling.isRed())
                {
                    // sibling is RED, so parent has to be BLACK!
                    // I'll switch some colors around and rotate left
                    // this way my parent will become RED.
                    // Later, I'll change the parent to be BLACK
                    // and be done :)
                    sibling.black = true;
                    parent.black = false;
                    leftRotate(parent);

                    // since node is BLACK and sibling was RED, sibling
                    // must have both children to balance out my BLACK
                    // since sibling's left child just became my parent's
                    // right child (thus my new sibling), update sibling,
                    // but don't worry about it being non-null :)
                    // sibling is now BLACK
                    sibling = parent.right;
                }

                // my sibling is BLACK now (as assured by the "if" above)
                if (sibling.left.isBlack() && sibling.right.isBlack())
                {
                    // both children of my sibling are BLACK (or null)
                    // so I can make my sibling RED and move up the tree.
                    sibling.black = false;
                    node = node.parent;
                }
                else
                {
                    // at least one of my sibling's children exists 
                    // and it must be RED (or I would have fallen into 
                    // the "if" above)                     
                    if (sibling.right.isBlack())
                    {
                        // since my sibling's right either doesn't exist 
                        // or is BLACK, my sibling's left must exist and 
                        // be RED. change my sibling's left to be BLACK, 
                        // my sibling to be RED, and rotate right around
                        // my sibling
                        sibling.left.black = true;
                        sibling.black = false;
                        rightRotate(sibling);
                        sibling = parent.right;
                        // now my sibling is BLACK and its right is RED
                    }

                    // now my sibling is BLACK and its right is RED
                    // beacuse I either entered the IF above me which
                    // ensured this, or it was the case before, so I
                    // didn't have to change anything
                    // I can change some of the colors, rotate left and
                    // be DONE!
                    sibling.black = parent.black;
                    parent.black = true;
                    sibling.right.black = true;
                    leftRotate(parent);
                    node = root;
                }
            }
            else
            {
                RBNode sibling = parent.left;
                if (sibling.isRed())
                {
                    // sibling is RED, so parent has to be BLACK!
                    // I'll switch some colors around and rotate right
                    // this way my parent will become RED.
                    // Later, I'll change the parent to be BLACK
                    // and be done :)
                    sibling.black = true;
                    parent.black = false;
                    rightRotate(parent);

                    // since node is BLACK and sibling was RED, sibling
                    // must have both children to balance out my BLACK
                    // since sibling's right child just became my parent's
                    // left child (thus my new sibling), update sibling,
                    // but don't worry about it being non-null :)
                    // sibling is now BLACK
                    sibling = parent.left;
                }

                // my sibling is BLACK now (as assured by the "if" above)
                if (sibling.right.isBlack() && sibling.left.isBlack())
                {
                    // both children of my sibling are BLACK (or null)
                    // so I can make my sibling RED and move up the tree.
                    sibling.black = false;
                    node = node.parent;
                }
                else
                {
                    // at least one of my sibling's children exists 
                    // and it must be RED (or I would have fallen into 
                    // the "if" above)                     
                    if (sibling.left.isBlack())
                    {
                        // since my sibling's left either doesn't exist 
                        // or is BLACK, my sibling's right must exist and 
                        // be RED. change my sibling's right to be BLACK, 
                        // my sibling to be RED, and rotate left around
                        // my sibling
                        sibling.right.black = true;
                        sibling.black = false;
                        leftRotate(sibling);
                        sibling = parent.left;
                        // now my sibling is BLACK and its left is RED
                    }

                    // now my sibling is BLACK and its left is RED
                    // beacuse I either entered the IF above me which
                    // ensured this, or it was the case before, so I
                    // didn't have to change anything
                    // I can change some of the colors, rotate right and
                    // be DONE!
                    sibling.black = parent.black;
                    parent.black = true;
                    sibling.left.black = true;
                    rightRotate(parent);
                    node = root;
                }
            }
        }

        node.black = true;
    }

    void printTree()
    {
        printTree(root, 0);
        System.out.println("-----------");
    }

    void printTree(RBNode node, int indent)
    {
        if (node == NULL)
            return;
        printTree(node.right, indent + 6);
        for (int i = 0; i < indent; i++)
            System.out.print(" ");
        if (node.parent != null)
            System.out.print(node.parent.nodeData + " ");

        if (node.isBlack())
            System.out.print("B ");
        else
            System.out.print("R ");
        System.out.println(node.nodeData);
        printTree(node.left, indent + 6);
    }
}
