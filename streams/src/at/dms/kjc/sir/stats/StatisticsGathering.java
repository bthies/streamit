package at.dms.kjc.sir.stats;

import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;

import java.util.*;

/**
 * This class gathers statistics about stream programs in order to
 * characterize their important properties.
 */

public class StatisticsGathering {
    /**
     * Gather statistics for <str>.
     */
    public static void doit(SIRStream str) {
	// count push statements
	new CountStatements().doit(str);
    }

    /**
     * Counts push and pop statements in a work function, as well as the
     * amount of control flow they have around them.
     */
    static class CountStatements extends FilterWorkVisitor {
	/** number of enclosing control flow statements */
	int numControl;
	/** number of peek, pop, push statements at levels 0, 1, 2, and 3 or more */
	int[] peek, pop, push;

	public CountStatements() {
	    peek = new int[3];
	    pop = new int[3];
	    push = new int[3];
	}

	/**
	 * Reset for new filter.
	 */
	void reset() {
	    for (int i=0; i<peek.length; i++) {
		peek[i] = 0;
		pop[i] = 0;
		push[i] = 0;
	    }
	    numControl = 0;
	}

	/**
	 * Report to screen (should be to file, probably in excel format, eventually.)
	 */
	void report() {
	    System.err.println("For filter " + filter.getName() + ", found:");
	    for (int i=0; i<3; i++) {
		if (peek[i]>0) System.err.println("  " + peek[i] + " peek statements wrapped in " + i + " level" + (i==1 ? "" : "s") + (i==2 ? " (or more)" : "") + " of control flow.");
		if (pop[i]>0) System.err.println("  " + pop[i] + " pop statements wrapped in " + i + " level" + (i==1 ? "" : "s") + (i==2 ? " (or more)" : "") + " of control flow.");
		if (push[i]>0) System.err.println("  " + push[i] + " push statements wrapped in " + i + " level" + (i==1 ? "" : "s") + (i==2 ? " (or more)" : "") + " of control flow.");
	    }
	}

	/**
	 * Keep track of control flow counter.
	 */
	public void visitForStatement(JForStatement self,
				      JStatement init,
				      JExpression cond,
				      JStatement incr,
				      JStatement body) {
	    if (init != null) {
		init.accept(this);
	    }
	    numControl++;
	    if (cond != null) {
		cond.accept(this);
	    }
	    if (incr != null) {
		incr.accept(this);
	    }
	    body.accept(this);
	    numControl--;
	}

	public void visitDoStatement(JDoStatement self,
				     JExpression cond,
				     JStatement body) {
	    numControl++;
	    body.accept(this);
	    numControl--;
	    cond.accept(this);
	}

	public void visitWhileStatement(JWhileStatement self,
					JExpression cond,
					JStatement body) {
	    cond.accept(this);
	    numControl++;
	    body.accept(this);
	    numControl--;
	}

	public void visitIfStatement(JIfStatement self,
				     JExpression cond,
				     JStatement thenClause,
				     JStatement elseClause) {
	    cond.accept(this);
	    numControl++;
	    thenClause.accept(this);
	    if (elseClause != null) {
		elseClause.accept(this);
	    }
	    numControl--;
	}

	/**
	 * Count different kinds of push, pop, peek.
	 */
	public void visitPushExpression(SIRPushExpression self,
					CType tapeType,
					JExpression arg) {
	    super.visitPushExpression(self, tapeType, arg);
	    push[Math.min(numControl,3)]++;
	}

	public void visitPeekExpression(SIRPeekExpression self,
					CType tapeType,
					JExpression arg) {
	    super.visitPeekExpression(self, tapeType, arg);
	    peek[Math.min(numControl,3)]++;
	}

	public void visitPopExpression(SIRPopExpression self,
				       CType tapeType) {
	    super.visitPopExpression(self, tapeType);
	    pop[Math.min(numControl,3)]++;
	}

    }

    /**
     * This class gathers statistics about stream programs in order to
     * characterize their important properties.
     */
    static abstract class FilterWorkVisitor extends SLIREmptyVisitor {
	/**
	 * The current filter we're visiting
	 */
	protected SIRFilter filter;

	/**
	 * Visit all filters in <str>
	 */
	public void doit(SIRStream str) {
	    final FilterWorkVisitor me = this;
	    IterFactory.createFactory().createIter(str).accept((new EmptyStreamVisitor() {
		    public void visitFilter(SIRFilter self, SIRFilterIter iter) {
			me.filter = self;
			if (self.needsWork()) {
			    me.reset();
			    self.getWork().accept(me);
			    me.report();
			};
		    }
		}));
	}

	/**
	 * Can be overridden to do something special before/after seeing
	 * children.
	 */
	void reset() {}
	void report() {}
    }
}
