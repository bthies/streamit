package at.dms.kjc.raw;

import java.util.LinkedList;

public class DFTraversal implements FlatVisitor 
{
    private LinkedList traversal;
    
    public static LinkedList getDFTraversal(FlatNode top) 
    {
	DFTraversal df = new DFTraversal();
	top.accept(df, null, true);
	return df.traversal;
    }
    
    protected DFTraversal() 
    {
	traversal = new LinkedList();
    }
    
    
    public void visitNode(FlatNode node) 
    {
	traversal.add(node);
    }
}
