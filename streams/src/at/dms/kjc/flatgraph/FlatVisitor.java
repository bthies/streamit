package at.dms.kjc.flatgraph;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;

/**
 * Visitor interface for the FlatNode IR 
 */
public interface FlatVisitor {
    public void visitNode(FlatNode node);
}
