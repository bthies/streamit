package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.util.*;
import java.util.*;
import java.io.*;

/**
 * The purpose of this class it to semantic check a stream program.
 */
public class SemanticChecker {
    /**
     * Main method to check <str>, outputting errors to System.err.
     */
    public static void doCheck(SIRStream str) {
	doCheck(str, System.err);
    }

    /**
     * Main method to check <str>, outputting errors to <out>.
     */
    public static void doCheck(SIRStream str, PrintStream out) {
	if (str == null) {
	    out.println("ERROR:  No Top-Level Stream defined!");
	    System.exit(-1);
	} else {
	    IterFactory.createFactory().createIter(str).accept(new StreamSemanticChecker(out));
	}
    }

    /**
     * Checks the high-level structure of streams.  A statement-level
     * checker should also be written.
     */
    static class StreamSemanticChecker extends EmptyStreamVisitor {
	/**
	 * Where to send output.
	 */
	private PrintStream out;

	public StreamSemanticChecker(PrintStream out) {
	    this.out = out;
	}
    
	/* pre-visit a feedbackloop */
	public void preVisitFeedbackLoop(SIRFeedbackLoop self,
					 SIRFeedbackLoopIter iter) {
	    if (self.getLoop()==null) {
		out.println("ERROR:  Loop stream is null in " + iter + ";\n" +
			    "  should be set with setLoop(...) until we have internal" +
			    "  support for compiler-recognized Identity filters");
	    }
	}
    }
}
