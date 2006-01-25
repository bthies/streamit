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
     * Whether or not to track fine-grained information on filters.
     * If fine-grained is true, each instance of a filter gets its own
     * profile report.  If false, all instances of a given filter type
     * are combined in the profile report.
     */
    private static boolean FINE_GRAINED = false;
    
    /**
     * Marks filter boundaries for all filters in <str>.
     */
    public static void doit(SIRStream str) {
        IterFactory.createFactory().createIter(str).accept(new MarkFilterBoundaries());
    }

    /**
     * Returns name for a given SIROperator.  This name is different
     * than some other names because it is designed to be useful for
     * profiling.  Currently we collapse all instances of a given
     * filter type across the stream graph into a single name.
     */
    private static String getName(SIROperator op) {
        // for example: "class at.dms.kjc.sir.SIRSplitter"
        String longClass = ""+op.getClass();
        // for example: "SIRSplitter"
        String shortClass = longClass.substring(1+longClass.lastIndexOf("."));

        String shortIdent;
        if (FINE_GRAINED) {
            // for example: "DCT__100_10_45"
            shortIdent = op.getName();
            // also output the enclosing stream to uniquely identify anonymous streams
            shortIdent += " parent= " + op.getParent().getIdent();
        } else {
            // for example: "DCT__100"
            String longIdent = op.getIdent();
            // for example: "DCT"
            if (longIdent.indexOf("__") > 0) {
                shortIdent = longIdent.substring(0, longIdent.lastIndexOf("__"));
            }  else {
                shortIdent = longIdent;
            }
        }

        // for example: "SIRFilter DCT"
        return shortClass + " " + shortIdent;
    }

    /**
     * Return begin marker for <op>.
     */
    public static SIRBeginMarker makeBeginMarker(SIROperator op) {
        return new SIRBeginMarker(getName(op));
    }

    /**
     * Return end marker for <op>.
     */
    public static SIREndMarker makeEndMarker(SIROperator op) {
        return new SIREndMarker(getName(op));
    }

    /* visit a filter */
    public void visitFilter(SIRFilter self,
                            SIRFilterIter iter) {
        JMethodDeclaration work = self.getWork();
        work.addStatementFirst(makeBeginMarker(self));
        work.addStatement(makeEndMarker(self));
    }
}
