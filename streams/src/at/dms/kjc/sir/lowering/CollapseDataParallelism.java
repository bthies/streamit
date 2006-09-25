package at.dms.kjc.sir.lowering;

import java.util.*;
import at.dms.util.*;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.fission.*;
import at.dms.kjc.sir.lowering.fusion.*;

/**
 * OVERVIEW ----------------------------------------------------
 * The goal of this class is to identify big splitjoins of
 * data-parallel components that can be collapsed to a single filter,
 * WITHOUT duplicating any code.
 *
 * This is very different from our fusion algorithms because the
 * resulting code size is comparable to one of the original filters,
 * rather than being the sum of the original filters.  
 * 
 * This class does not really "remove" data parallelism, because the
 * collapsed filter is stateless and can be fissed again using
 * StatelessDuplicate.  The reasons for collapsing it first are to:
 *  1. Reduce code size (and possibly execution time) on a uniprocessor
 *  2. To avoid fusing-then-fissing when adjusting a big SJ width
 *     to the right size on a parallel target
 *
 * NOTE that this class has an unusual calling pattern.  It must be
 * called once before ConstructSIRTree (when arguments are still
 * available) and again afterwards.  Thus there are two separate
 * public functions.
 *
 * DETAILS ------------------------------------------------------
 * The transformation currently applies to splitjoins whos children
 * are all:
 *  - filters
 *  - no TwoStageFilters
 *  - no peeking
 *  - no mutable state (and isFissable())
 *  - static I/O rates
 *  - same name (getIdent())
 *  - exactly the same int/float-literal arguments are passed to all filters (*)
 * 
 * Bullet (*) indicates that no messages come out of these filters.
 * Also, no messages come in because such filters are already
 * unfissable (they would mutate state, or messages would be no-ops).
 */
public class CollapseDataParallelism {
    /**
     * Whether or not this pass is enabled.
     */
    public static final boolean ENABLED = !KjcOptions.nodatacollapse;
    /**
     * Set of splitjoins that are eligible for transformation.
     */
    private HashSet<SIRSplitJoin> eligible;

    /**
     * Construct one each time you do a call to detectEligible / doTransformations.
     */
    public CollapseDataParallelism() {
        eligible = new HashSet<SIRSplitJoin>();
    }

    /**
     * Keeps track of all splitjoins contained within 'str' that are
     * eligible for the transformation.  Should be called *BEFORE*
     * ConstructSIRTree.
     */
    public void detectEligible(SIRStream str) {
        str.accept(new EmptyAttributeStreamVisitor() {
                // from splitjoins, see if they're eligible
                public Object visitSplitJoin(SIRSplitJoin sj,
                                             JFieldDeclaration[] fields,
                                             JMethodDeclaration[] methods,
                                             JMethodDeclaration init,
                                             SIRSplitter splitter,
                                             SIRJoiner joiner) {
                    // count of how many children we've visited
                    final int[] count = { 0 };
                    // whether or not this sj is still eligible for transformation
                    final boolean[] stillEligible = { true };
                    // visit all initialization of children in the splitjoin
                    sj.getInit().accept(new SLIREmptyVisitor() {
                            public void visitInitStatement(SIRInitStatement self,
                                                           SIRStream target) {
                                if (stillEligible[0]) {
                                    stillEligible[0] = isEligible(target, self.getArgs(), count[0]);
                                }
                                count[0]++;
                            }
                        });
                    // if they all conform, transform the splitjoin
                    if (stillEligible[0]) {
                        eligible.add(sj);
                    }
                    
                    // finally, visit children of splitjoin
                    visitChildren(sj);
                    return sj;
                }

                // from pipelines, visit children
                public Object visitPipeline(SIRPipeline self,
                                            JFieldDeclaration[] fields,
                                            JMethodDeclaration[] methods,
                                            JMethodDeclaration init) {
                    visitChildren(self);
                    return self;
                }

                // from feedback, visit children
                public Object visitFeedbackLoop(SIRFeedbackLoop self,
                                                JFieldDeclaration[] fields,
                                                JMethodDeclaration[] methods,
                                                JMethodDeclaration init,
                                                JMethodDeclaration initPath) {
                    visitChildren(self);
                    return self;
                }

                // visit children by finding targets of SIRInitStatements
                private void visitChildren(SIRContainer self) {
                    final EmptyAttributeStreamVisitor visitor[] = { this };
                    self.getInit().accept(new SLIREmptyVisitor() {
                            public void visitInitStatement(SIRInitStatement self,
                                                           SIRStream target) {
                                target.accept(visitor[0]);
                            }
                        });
                }
            });
    }

    /**
     * Actually performs the transformation on all eligible splitjoins
     * within 'str'.  Should be called *AFTER* ConstructSIRTree, and
     * also *AFTER* 'detectEligible'.
     */
    public void doTransformations(SIRStream str) {
        // by returning void, this assumes toplevel stream is not a splitjoin
        str.accept(new ReplacingStreamVisitor() {
                public Object visitSplitJoin(SIRSplitJoin self,
                                             JFieldDeclaration[] fields,
                                             JMethodDeclaration[] methods,
                                             JMethodDeclaration init,
                                             SIRSplitter splitter,
                                             SIRJoiner joiner) {
                    super.visitSplitJoin(self, fields, methods, init, splitter, joiner);
                    if (eligible.contains(self)) {
                        return doOneTransformation(self);
                    } else {
                        return self;
                    }
                }
            });
        // somewhere I missed a parent child :(
        ((SIRContainer)str).reclaimChildren();
        // eliminate wrapper pipelines
        Lifter.lift(str);
    }

    /**
     * Given that 'sj' is eligible to be transformed, do the
     * transformation into a pipeline according to this sketch:
     *
     * The pass works by converting an eligible splitjoin into a pipeline
     * of two splitjoins and a filter (from the original):
     *
     * Orig:
     *  split origSplit
     *    for i = 1 to N
     *      filter pop O push U
     *  join origJoin
     *
     * And let reps = r1 ... rn represent the repetitions of each
     * occurrence of the filter in the original schedule.
     *
     * Transformed:
     *  split origSplit
     *    for i = 1 to N
     *      identity
     *  join (r1*O, r2*O, ..., rn*O)
     *
     *    |
     *   \./
     *
     *  filter
     *
     *    |
     *   \./
     *
     *  split (r1*U, r2*U, ..., rn*U)
     *    for i = 1 to N
     *      identity
     *  join origJoin
     *
     * It also fuses the new splitjoins to avoid any overheads scaling with N.
     *
     * This method does not mutate the parent of 'sj'.
     */
    private SIRPipeline doOneTransformation(SIRSplitJoin sj) {
        // get parameters from the sketch
        int N = sj.size();
        SIRFilter filter = (SIRFilter)sj.get(0);
        int O = filter.getPopInt();
        int U = filter.getPushInt();
        SIRSplitter origSplit = sj.getSplitter();
        SIRJoiner origJoin = sj.getJoiner();
        HashMap reps = SIRScheduler.getExecutionCounts(sj)[1];

        // make new first splitjoin
        SIRSplitJoin sj1 = new SIRSplitJoin(null, "CollapsedDataParallel_1");
        sj1.setInit(SIRStream.makeEmptyInit());
        for (int i=0; i<N; i++) {
            sj1.add(new SIRIdentity(sj.getInputType()));
        }
        sj1.setSplitter(origSplit);
        JExpression[] weights = createWeights(sj, reps, O);
        sj1.setJoiner(SIRJoiner.createWeightedRR(sj1, weights));
        sj1.rescale();

        // make the second splitjoin
        SIRSplitJoin sj2 = new SIRSplitJoin(null, "CollapsedDataParallel_2");
        sj2.setInit(SIRStream.makeEmptyInit());
        for (int i=0; i<N; i++) {
            sj2.add(new SIRIdentity(sj.getInputType()));
        }
        weights = createWeights(sj, reps, U);
        sj2.setSplitter(SIRSplitter.createWeightedRR(sj2, weights));
        sj2.setJoiner(origJoin);
        sj2.rescale();

        // make the overall pipeline
        SIRPipeline result = new SIRPipeline(null, "CollapsedDataParallel");
        result.setInit(SIRStream.makeEmptyInit());
        result.add(sj1);
        result.add(filter);
        result.add(sj2);

        // fuse the splitjoins down into filters (it replaces them in parent)
        SIRStream fused1 = FuseSimpleSplit.fuse(sj1);
        SIRStream fused2 = FuseSimpleSplit.fuse(sj2);
        assert !(fused1 instanceof SIRSplitJoin) : "Expected simple fusion to apply; it didn't.";
        assert !(fused2 instanceof SIRSplitJoin) : "Expected simple fusion to apply; it didn't.";
        
        return result;
    }

    /**
     * Given a splitjoin and the scheduling multiplicities for that
     * splitjoin, returns an array 'weights' satisfying:
     *
     *  weights[i] = reps(sj.get(i)) * k
     *
     * However, if all the weights are the same, then collapses all
     * the values down to "k".
     */
    private JExpression[] createWeights(SIRSplitJoin sj, HashMap reps, int k) {
        JExpression[] weights = new JExpression[sj.size()];
        for (int i=0; i<sj.size(); i++) {
            weights[i] = new JIntLiteral(((int[])reps.get(sj.get(i)))[0] * k);
        }
        // collapse down to array of "k" if uniform (this decreases
        // the granularity slightly in common cases while maintaining
        // legality of the transformation.)
        if (Utils.isUniform(weights) && 
            // make sure the values are already bigger than k before collapsing
            ((JIntLiteral)weights[0]).intValue() > k) {
            for (int i=0; i<sj.size(); i++) {
                weights[i] = new JIntLiteral(k);
            }
        }
        return weights;
    }
    
    /**
     * Returns whether or not the i'th child of 'sj' is eligible for
     * the collapsing, given the rules documented above.
     */
    private boolean isEligible(SIRStream childStr, List args, int i) {
        SIRFilter child = null;
        // check for filter
        if (childStr instanceof SIRFilter) {
            child = (SIRFilter)childStr;
        } else {
            return false;
        }

        // check for no two-stage filters
        if (child instanceof SIRTwoStageFilter) {
            return false;
        }

        // check for rates
        int pop, peek;
        if (child.getPeek() instanceof JLiteral &&
            child.getPop() instanceof JLiteral &&
            child.getPush() instanceof JLiteral) {
            pop = child.getPopInt();
            peek = child.getPeekInt();
        } else {
            // might be dynamic, etc.
            return false;
        }

        // check for no peeking
        if (peek > pop) {
            return false;
        }

        // check for fissable (but, unlike fission, DO allow SIRIdentities)
        if (!StatelessDuplicate.isFissable(child) && !(child instanceof SIRIdentity)) {
            return false;
        }
            
        // finally, check the arguments 
        boolean argsOk = eligibleArgs(child, args, i);
        if (!argsOk) {
            return false;
        }

        return true;
    }

    /**
     * Checks if the 'args' for the i'th 'child' of the splitjoin are
     * constant and consistent with previous children.
     */
    private List firstArgs; // args of first child
    private String firstIdent; // name of first child
    private boolean eligibleArgs(SIRFilter child, List args, int i) {
        // for all children, check that the args are literals
        for (Iterator it = args.iterator(); it.hasNext(); ) {
            JExpression arg = (JExpression)it.next();
            if (!(arg instanceof JLiteral)) {
                return false;
            }
        }
        if (i==0) {
            // for first child, just remember the args.  We don't hold
            // on for it to long, and we don't mutate in the meantime,
            // so no need to copy.
            firstArgs = args;
            firstIdent = child.getIdent();
        } else {
            // for other children, compare to first args
            Iterator first = firstArgs.iterator();
            Iterator cur = args.iterator();
            while (first.hasNext() && cur.hasNext()) {
                // we know they are literals now
                JLiteral lit1 = (JLiteral)first.next();
                JLiteral lit2 = (JLiteral)cur.next();
                if (!lit1.equals(lit2)) {
                    // if not "equal", can't transform
                    return false;
                }
            }
            // check they have same number of args
            if (first.hasNext() || cur.hasNext()) {
                return false;
            }
            // check that names match
            if (!(child.getIdent().equals(firstIdent))) {
                return false;
            }
        }
        return true;
    }
}
