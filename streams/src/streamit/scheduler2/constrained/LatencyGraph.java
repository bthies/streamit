package streamit.scheduler2.constrained;

import streamit.misc.Pair;
import streamit.misc.DLList;
import streamit.misc.OMap;
import streamit.misc.OMapIterator;

public class LatencyGraph extends streamit.misc.AssertedClass
{
    /*
     * This is a list of all nodes (Filters, splitter and joiners)
     * in the graph. Each element is a LatencyNode.
     */
    final DLList nodes = new DLList();

    /*
     * This a map of StreamInterfaces to DLList of StreamInterfaces.
     * The second entry contains a list of ancestor of the key, 
     * ordered from top-most to bottom-most (StreamIt object downto
     * the immediate parent). A stream with no parents (out-most
     * pipeline) has an empty list.
     */
    final OMap ancestorLists = new OMap ();
    
    DLList getAncestorList (StreamInterface stream)
    {
        OMapIterator listIter = ancestorLists.find(stream);
        ASSERT (!listIter.equals (ancestorLists.end()));
        
        DLList ancestors = (DLList) listIter.getData ();
        ASSERT (ancestors != null);
        
        return ancestors;
    }

    void registerParent(StreamInterface child, StreamInterface parent)
    {
        if (parent == null)
        {
            // no parent - just add an empty list
            ancestorLists.insert(child, new DLList());
        }
        else
        {
            // has a parent - find parent's ancestors, copy the list
            // and add the parent - that's the child's list of ancestors
            DLList parentAncestors =
                (DLList) ancestorLists.find(parent).getData();
            DLList ancestors = parentAncestors.copy ();
            ancestors.pushBack (parent);
            ancestorLists.insert (child, ancestors);
        }
    }

    void addNode(Filter filter)
    {
        nodes.pushBack(new LatencyNode(filter, getAncestorList (filter)));
    }
}
