package at.dms.kjc.sir.lowering.fusion;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.lir.*;

import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * This class fuses everything it can in a stream graph.
 */
public class FuseAll extends EmptyStreamVisitor {

    private FuseAll() {}

    /**
     * Fuse everything we can in <str>
     */
    public static void fuse(SIRStream str) {
	str.accept(new FuseAll());
    }

    /* post-visit a pipeline */
    public void postVisitPipeline(SIRPipeline self,
				  SIRStream parent,
				  JFieldDeclaration[] fields,
				  JMethodDeclaration[] methods,
				  JMethodDeclaration init) {
	FusePipe.fuse(self);
    }
}
