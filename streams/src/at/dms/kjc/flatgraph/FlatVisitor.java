package at.dms.kjc.flatgraph;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;

/**
 * Visitor interface to a graph of FlatNodes.
 * 
 * @author mgordon
 */
public interface FlatVisitor {
    /**
     * The visitor must define this method that will be called once on 
     * each FlatNode that is down stream of the accepting node. 
     *  
     * @param node the node that is being currently visited.
     */
    public void visitNode(FlatNode node);
}
