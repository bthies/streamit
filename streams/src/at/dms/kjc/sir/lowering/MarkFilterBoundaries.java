package at.dms.kjc.sir.lowering;

import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;

/**
 * Inserts an SIRMarker at the beginning and end of every filter's
 * work function.
 */
public class MarkFilterBoundaries extends EmptyStreamVisitor {
    /**
     * Marks filter boundaries for all filters in <str>.
     */
    public static void doit(SIRStream str) {
	IterFactory.createFactory().createIter(str).accept(new MarkFilterBoundaries());
    }

    /**
     * Return begin marker for <op>.
     */
    public static SIRBeginMarker makeBeginMarker(SIROperator op) {
	return new SIRBeginMarker(op.getClass() + " " + op.getIdent());
    }

    /**
     * Return end marker for <op>.
     */
    public static SIREndMarker makeEndMarker(SIROperator op) {
	return new SIREndMarker(op.getClass() + " " + op.getIdent());
    }

    /* visit a filter */
    public void visitFilter(SIRFilter self,
			    SIRFilterIter iter) {
	JMethodDeclaration work = self.getWork();
	work.addStatementFirst(makeBeginMarker(self));
	work.addStatement(makeEndMarker(self));
    }
}
