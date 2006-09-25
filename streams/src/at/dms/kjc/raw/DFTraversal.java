package at.dms.kjc.raw;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import java.util.LinkedList;

public class DFTraversal implements FlatVisitor 
{
    private LinkedList<FlatNode> traversal;
    
    public static LinkedList<FlatNode> getDFTraversal(FlatNode top) 
    {
        DFTraversal df = new DFTraversal();
        top.accept(df, null, true);
        return df.traversal;
    }
    
    protected DFTraversal() 
    {
        traversal = new LinkedList<FlatNode>();
    }
    
    
    public void visitNode(FlatNode node) 
    {
        traversal.add(node);
    }
}
