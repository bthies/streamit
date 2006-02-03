package at.dms.kjc.sir.lowering.fusion;

import at.dms.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.lir.*;
import at.dms.kjc.sir.lowering.*;

import java.util.*;

/**
 * This flattens certain cases of split/joins into a single filter.
 */
public class FuseSimpleSplit {
    /**
     * Flattens a split/join, subject to the following constraints:
     *  1. Each parallel component of the stream is a filter.  Further, it is
     *     not a two-stage filter that peeks.
     *  2. If the splitter is anything other than duplicate, then
     *     there is no peeking from within the parallel streams.
     */
    public static SIRStream fuse(SIRSplitJoin sj)
    {
        if (!isFusable(sj)) {
            return sj;
        } else {
            //System.err.println("Fusing " + (sj.size()) + " SplitJoin filters.");
        }

        //printStats(sj);

        // get copy of child streams
        List children = sj.getParallelStreams();
        // rename components
        doRenaming(children);
        // inline phases
        doPhaseInlining(children);
        // calculate the repetitions for the split-join
        SRepInfo rep = SRepInfo.calcReps(sj);

        // So at this point we have an eligible split/join; flatten it.
        JMethodDeclaration newWork = makeWorkFunction(sj, children, rep);
        JFieldDeclaration[] newFields = makeFields(sj, children);
        JMethodDeclaration[] newMethods = makeMethods(sj, children);
        JMethodDeclaration newInit = makeInitFunction(sj, children);

        SRate rate = calcSRate(sj, rep);

        // Build the new filter.
        SIRFilter newFilter = new SIRFilter(sj.getParent(), "Fused_" + sj.getIdent(),
                                            newFields, newMethods, new JIntLiteral(rate.peek),
                                            new JIntLiteral(rate.pop), new JIntLiteral(rate.push),
                                            newWork, sj.getInputType(), sj.getOutputType());
        // Use the new init function
        newFilter.setInit(newInit);

        // make new pipeline representing fused sj
        SIRPipeline fused = makeFusedPipe(sj, rep, rate, newFilter);

        // replace in parent
        replaceInParent(sj, fused);

        return fused;
    }

    /**
     * Prints out statitistics about the I/O rates of the splitter,
     * joiner, and filters in <sj>.
     */
    static void printStats(SIRSplitJoin sj) {
        System.err.println();
        System.err.println("Splitjoin: " + sj.getIdent());
        System.err.print("Splitter: " + sj.getSplitter().getType() + "(");
        int[] weights = sj.getSplitter().getWeights();
        for (int i=0; i<weights.length; i++) {
            System.err.print(weights[i]);
            if (i!=weights.length-1) {
                System.err.print(", ");
            }
        }
        System.err.println(")");
        for (int i=0; i<sj.size();i++) {
            System.err.print(sj.get(i).getIdent());
            System.err.print(" push " + ((SIRFilter)sj.get(i)).getPushInt());
            System.err.print(" pop " + ((SIRFilter)sj.get(i)).getPopInt());
            System.err.print(" peek " + ((SIRFilter)sj.get(i)).getPeekInt());
            System.err.println();
        }
        System.err.print("Joiner: " + sj.getJoiner().getType() + "(");
        weights = sj.getJoiner().getWeights();
        for (int i=0; i<weights.length; i++) {
            System.err.print(weights[i]);
            if (i!=weights.length-1) {
                System.err.print(", ");
            }
        }
        System.err.println(")");
        System.err.println();
    }

    /**
     * Create a pipelin containing prelude and postlude (and new
     * filter) to represent the fused splitjoin.
     */
    private static SIRPipeline makeFusedPipe(SIRSplitJoin sj,
                                             SRepInfo rep,
                                             SRate rate,
                                             SIRFilter newFilter) {
        SIRPipeline pipe = new SIRPipeline(sj.getParent(), newFilter.getName() + "_Wrap");
        // make a dummy init function
        pipe.setInit(SIRStream.makeEmptyInit());

        // do not make a filter for a splitter if:
        //  - it's a duplicate
        //  - it's a roundrobin(1,1,...,1)
        //  - it's a null split
        if (!(sj.getSplitter().getType()==SIRSplitType.DUPLICATE ||
              sj.getSplitter().isUnaryRoundRobin() ||
              rep.splitter==0
              )) {
            pipe.add(makeFilter(pipe,
                                "Pre_" + sj.getIdent(),
                                makeSplitFilterBody(sj.getSplitter(),
                                                    rep,
                                                    sj.getInputType()),
                                rate.pop, rate.pop, rate.pop,
                                sj.getInputType()));
        }

        int index = sj.getParent().indexOf(sj);
        assert index>-1:
            "Couldn't find " + sj.getName() + " in " +
            sj.getParent().getIdent();
        pipe.add(newFilter, new LinkedList(sj.getParent().getParams(index)));

        // make a joinFilter only if it's not a not a null join
        if (rep.joiner!=0) {
            pipe.add(makeFilter(pipe,
                                "Post_" + sj.getIdent(),
                                makeJoinFilterBody(sj.getJoiner(),
                                                   rep,
                                                   sj.getOutputType()),
                                rate.push, rate.push, rate.push,
                                sj.getOutputType()));
        }

        return pipe;
    }

    private static void replaceInParent(SIRSplitJoin sj,
                                        SIRPipeline fused) {
        // if parent of <sj> is a pipeline, then copy these children
        // in, in place of the original filter
        SIRContainer parent = sj.getParent();
        // DON'T CONSIDER THIS CASE SINCE IT HINDERS
        // PARTITIONING--would like things to end up in their own
        // nested pipeline so that pipeline can be fused
        if (false) { //parent instanceof SIRPipeline) {
            int index = parent.indexOf(sj);
            parent.remove(index);
            for (int i=fused.size()-1; i>=0; i--) {
                parent.add(index, fused.get(i), fused.getParams(i));
            }
        } else {
            // if <fused> has just one filter, add it in place of <sj>
            if (fused.size()==1) {
                parent.replace(sj, fused.get(0));
            } else {
                // otherwise, just add <fused> to parent in place of <sj>
                parent.replace(sj, fused);
                // clear the param list pased to <fused>
                parent.getParams(parent.indexOf(fused)).clear();
            }
        }
    }

    private static SRate calcSRate(SIRSplitJoin sj, SRepInfo rep) {
        // calculate the push/pop/peek ratio
        int push = rep.joiner * sj.getJoiner().getSumOfWeights();
        int pop;
        if (sj.getSplitter().getType()==SIRSplitType.DUPLICATE) {
            pop = rep.splitter;
        } else {
            pop = rep.splitter * sj.getSplitter().getSumOfWeights();;
        }

        // calculate the peek amount...
        // get the max peeked by a child, in excess of its popping
        int maxPeek = -1;
        for (int i=0; i<sj.size(); i++) {
            SIRFilter filter = (SIRFilter)sj.get(i);
            maxPeek = Math.max(maxPeek,
                               filter.getPeekInt()-
                               filter.getPopInt());
        }
        // calculate the peek as the amount we'll look into the input
        // during execution
        int peek = pop+maxPeek;

        return new SRate(push, pop, peek);
    }

    private static void doRenaming(List children) {
        // Rename all of the child streams of this.
        Iterator iter = children.iterator();
        while (iter.hasNext()) {
            SIRFilter filter = (SIRFilter)iter.next();
            RenameAll.renameFilterContents((SIRFilter)filter);
        }
    }

    private static void doPhaseInlining(List children) {
        // inline all phases, as they aren't supported in fusion yet
        Iterator iter = children.iterator();
        while (iter.hasNext()) {
            SIRFilter filter = (SIRFilter)iter.next();
            InlinePhases.doit(filter);
        }
    }

    /**
     * Returns whether or not <sj> is fusable.
     */
    private static boolean isFusable(SIRSplitJoin sj) {
        // Check the ratios.
        Iterator childIter = sj.getParallelStreams().iterator();
        while (childIter.hasNext()) {
            SIRStream str = (SIRStream)childIter.next();
            // don't allow two-stage filters, since we aren't dealing
            // with how to fuse their initWork functions.
            if (str instanceof SIRTwoStageFilter) {
                return false;
            } else {
                // otherwise dispatch to standard fusable test
                if (!FusePipe.isFusable(str)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns a filter with empty init function and work function
     * having parent <parent>, name <ident>, statements <block>, i/o
     * type <type>, and i/o rates <peek>, <pop>, <push> suitable for
     * reorderers that emulate splitters and joiners in the fused
     * construct.
     */
    private static SIRFilter makeFilter(SIRContainer parent,
                                        String ident,
                                        JBlock block,
                                        int push, int pop, int peek,
                                        CType type) {

        // make empty init function
        JMethodDeclaration init = SIRStream.makeEmptyInit();

        // make work function
        JMethodDeclaration work = new JMethodDeclaration(null,
                                                         at.dms.kjc.Constants.ACC_PUBLIC,
                                                         CStdType.Void,
                                                         "work",
                                                         JFormalParameter.EMPTY,
                                                         CClassType.EMPTY,
                                                         block,
                                                         null,
                                                         null);

        // make method array
        JMethodDeclaration[] methods = { init, work} ;

        // return new filter
        SIRFilter result = new SIRFilter(parent,
                                         ident,
                                         JFieldDeclaration.EMPTY(),
                                         methods,
                                         new JIntLiteral(peek),
                                         new JIntLiteral(pop),
                                         new JIntLiteral(push),
                                         work,
                                         type,
                                         type);
        result.setInit(init);

        return result;
    }

    /**
     * Makes the body of the work function for a reordering filter
     * equivalent to <split>.
     */
    private static JBlock makeSplitFilterBody(SIRSplitter split,
                                              SRepInfo rep,
                                              CType type) {
        // get splitter weights
        int[] weights = split.getWeights();
        int[] partialSum = new int[weights.length];
        JExpression[] partialSumExpression = new JExpression[weights.length];
        JExpression[] weightsExpression = new JExpression[weights.length];
        // calculate partial sums of weights
        partialSumExpression[0] = new JIntLiteral(0);
        weightsExpression[0] = new JIntLiteral(weights[0]);
        for (int i=1; i<weights.length; i++) {
            partialSum[i] = partialSum[i-1] + weights[i-1];
            partialSumExpression[i] = new JIntLiteral(partialSum[i]);
            weightsExpression[i] = new JIntLiteral(weights[i]);
        }
        // get total weights
        int sumOfWeights = split.getSumOfWeights();
        // make list of statements for work function
        LinkedList list = new LinkedList();

        // mark beginning of splitter
        list.add(MarkFilterBoundaries.makeBeginMarker(split));

        // see how many statements we would generate
        int numStatements = 0;
        for (int k=0; k<weights.length; k++) {
            for (int i=0; i<rep.splitter; i++) {
                for (int j=0; j<weights[k]; j++) {
                    numStatements++;
                }
            }
        }
        if (numStatements<=FuseSplit.SPLITTER_JOINER_LOOP_THRESHOLD()) {        // here is the conceptual version:
            //System.err.println("FuseSimpleSplit: unrolling splitter " + numStatements + " ( <= " + FuseSplit.SPLITTER_JOINER_LOOP_THRESHOLD());
            for (int k=0; k<weights.length; k++) {
                for (int i=0; i<rep.splitter; i++) {
                    for (int j=0; j<weights[k]; j++) {
                        // calculate index of this peek
                        int index = i*sumOfWeights + partialSum[k] + j;
                        // make a peek expression
                        JExpression peek = new SIRPeekExpression(new JIntLiteral(index), type);
                        // make a push expression
                        JExpression push = new SIRPushExpression(peek, type);
                        // make an expression statement
                        list.add(new JExpressionStatement(null, push, null));
                    }
                }
            }
        } else {
            //System.err.println("FuseSimpleSplit: compacting joiner " + numStatements + " ( > " + FuseSplit.SPLITTER_JOINER_LOOP_THRESHOLD());
            // here is a version optimized for code size...
            // _weights[N] = { , , }
            JArrayInitializer _weightsInit = new JArrayInitializer(null, weightsExpression);
            JVariableDefinition _weights = new JVariableDefinition(null,
                                                                   0,
                                                                   new CArrayType(CStdType.Integer, 1,  new JExpression[] { new JIntLiteral(weights.length) } ),
                                                                   "_weights",
                                                                   _weightsInit);
            list.add(new JVariableDeclarationStatement(null, new JVariableDefinition[] {_weights}, null));
            // _partialSum[N] = { , , }
            JArrayInitializer _partialSumInit = new JArrayInitializer(null, partialSumExpression);
            JVariableDefinition _partialSum = new JVariableDefinition(null,
                                                                      0,
                                                                      new CArrayType(CStdType.Integer, 1, new JExpression[] { new JIntLiteral(weights.length) } ),
                                                                      "_partialSum",
                                                                      _partialSumInit);
            list.add(new JVariableDeclarationStatement(null, new JVariableDefinition[] {_partialSum}, null));
            // make loop variables
            JVariableDefinition _i = new JVariableDefinition(null, 0, CStdType.Integer, "_i", new JIntLiteral(0));
            JVariableDefinition _j = new JVariableDefinition(null, 0, CStdType.Integer, "_j", new JIntLiteral(0));
            JVariableDefinition _k = new JVariableDefinition(null, 0, CStdType.Integer, "_k", new JIntLiteral(0));
            // make loop body
            JStatement inner = new JExpressionStatement(null,
                                                        new SIRPushExpression(new SIRPeekExpression(new JAddExpression(null,
                                                                                                                       new JMultExpression(null,
                                                                                                                                           new JLocalVariableExpression(null, _i),
                                                                                                                                           new JIntLiteral(sumOfWeights)),
                                                                                                                       new JAddExpression(null,
                                                                                                                                          new JArrayAccessExpression(null,
                                                                                                                                                                     new JLocalVariableExpression(null, _partialSum),
                                                                                                                                                                     new JLocalVariableExpression(null, _k),
                                                                                                                                                                     CStdType.Integer),
                                                                                                                                          new JLocalVariableExpression(null, _j))),
                                                                                                    type),

                                                                              type),
                                                        null);
            // add k loop
            JStatement jLoop = Utils.makeForLoop(inner, new JArrayAccessExpression(null, new JLocalVariableExpression(null, _weights), new JLocalVariableExpression(null, _k)), _j);
            JStatement iLoop = Utils.makeForLoop(jLoop, new JIntLiteral(rep.splitter), _i);
            JStatement kLoop = Utils.makeForLoop(iLoop, new JIntLiteral(weights.length), _k);
            list.add(kLoop);
        }
        // pop the right number of items at the end
        if (sumOfWeights * rep.splitter > 0) {
            list.add(new JExpressionStatement(new SIRPopExpression(type, 
                                                                   sumOfWeights * rep.splitter)));
        }
        //        list.add(Utils.makeForLoop(new JExpressionStatement(null,
        //                                                            new SIRPopExpression(type),
        //                                                            null),
        //                                   sumOfWeights * rep.splitter));

        // mark end of splitter
        list.add(MarkFilterBoundaries.makeEndMarker(split));
        return new JBlock(null, list, null);
    }

    /**
     * Makes the body of the work function for a reordering filter
     * equivalent to <join>.
     */
    private static JBlock makeJoinFilterBody(SIRJoiner join, SRepInfo rep, CType type) {
        // get joiner weights
        int[] weights = join.getWeights();
        int[] partialSum = new int[weights.length];
        JExpression[] partialSumExpression = new JExpression[weights.length];
        JExpression[] weightsExpression = new JExpression[weights.length];
        // calculate partial sums of outputs
        partialSumExpression[0] = new JIntLiteral(0);
        weightsExpression[0] = new JIntLiteral(weights[0]);
        for (int i=1; i<weights.length; i++) {
            partialSum[i] = partialSum[i-1] + rep.joiner * weights[i-1];
            partialSumExpression[i] = new JIntLiteral(partialSum[i]);
            weightsExpression[i] = new JIntLiteral(weights[i]);
        }
        // get total weights
        int sumOfWeights = join.getSumOfWeights();
        // make list of statements for work function
        LinkedList list = new LinkedList();

        // mark end of splitter
        list.add(MarkFilterBoundaries.makeBeginMarker(join));

        // see how many statements we would generate
        int numStatements = 0;
        for (int k=0; k<rep.joiner; k++) {
            for (int i=0; i<weights.length; i++) {
                for (int j=0; j<weights[i]; j++) {
                    numStatements++;
                }
            }
        }
        if (numStatements<=FuseSplit.SPLITTER_JOINER_LOOP_THRESHOLD()) {        // here is the conceptual version:
            //System.err.println("FuseSimpleSplit: unrolling joiner " + numStatements + " ( <= " + FuseSplit.SPLITTER_JOINER_LOOP_THRESHOLD());
            for (int k=0; k<rep.joiner; k++) {
                for (int i=0; i<weights.length; i++) {
                    for (int j=0; j<weights[i]; j++) {
                        int index = partialSum[i] + k*weights[i] + j;
                        // make a peek expression
                        JExpression peek = new SIRPeekExpression(new JIntLiteral(index),
                                                                 type);
                        // make a push expression
                        JExpression push = new SIRPushExpression(peek, type);
                        // make an expression statement
                        list.add(new JExpressionStatement(null, push, null));
                    }
                }
            }
        } else {
            //System.err.println("FuseSimpleSplit: compacting joiner " + numStatements + " ( > " + FuseSplit.SPLITTER_JOINER_LOOP_THRESHOLD());
            // here is a version optimized for code size...
            // _weights[N] = { , , }
            JArrayInitializer _weightsInit = new JArrayInitializer(null, weightsExpression);
            JVariableDefinition _weights = new JVariableDefinition(null,
                                                                   0,
                                                                   new CArrayType(CStdType.Integer, 1,  new JExpression[] { new JIntLiteral(weights.length) } ),
                                                                   "_weights",
                                                                   _weightsInit);
            list.add(new JVariableDeclarationStatement(null, new JVariableDefinition[] {_weights}, null));
            // _partialSum[N] = { , , }
            JArrayInitializer _partialSumInit = new JArrayInitializer(null, partialSumExpression);
            JVariableDefinition _partialSum = new JVariableDefinition(null,
                                                                      0,
                                                                      new CArrayType(CStdType.Integer, 1, new JExpression[] { new JIntLiteral(weights.length) } ),
                                                                      "_partialSum",
                                                                      _partialSumInit);
            list.add(new JVariableDeclarationStatement(null, new JVariableDefinition[] {_partialSum}, null));
            // make loop variables
            JVariableDefinition _i = new JVariableDefinition(null, 0, CStdType.Integer, "_i", new JIntLiteral(0));
            JVariableDefinition _j = new JVariableDefinition(null, 0, CStdType.Integer, "_j", new JIntLiteral(0));
            JVariableDefinition _k = new JVariableDefinition(null, 0, CStdType.Integer, "_k", new JIntLiteral(0));
            // make loop body
            JStatement inner = new JExpressionStatement(null,
                                                        new SIRPushExpression(new SIRPeekExpression(
                                                                                                    new JAddExpression(null, new JMultExpression(null,
                                                                                                                                                 new JLocalVariableExpression(null, _k),
                                                                                                                                                 new JArrayAccessExpression(null,
                                                                                                                                                                            new JLocalVariableExpression(null,
                                                                                                                                                                                                         _weights),
                                                                                                                                                                            new JLocalVariableExpression(null,
                                                                                                                                                                                                         _i))), new JAddExpression(
                                                                                                                                                                                                                                   null, new JArrayAccessExpression(null,
                                                                                                                                                                                                                                                                    new JLocalVariableExpression(null,
                                                                                                                                                                                                                                                                                                 _partialSum),
                                                                                                                                                                                                                                                                    new JLocalVariableExpression(null,
                                                                                                                                                                                                                                                                                                 _i), CStdType.Integer),
                                                                                                                                                                                                                                   new JLocalVariableExpression(null, _j))),
                                                                                                    type),

                                                                              type), null);
            // add k loop
            JStatement jLoop = Utils.makeForLoop(inner,
                                                 new JArrayAccessExpression(null,
                                                                            new JLocalVariableExpression(null, _weights),
                                                                            new JLocalVariableExpression(null, _i)), _j);
            JStatement iLoop = Utils.makeForLoop(jLoop, new JIntLiteral(
                                                                        weights.length), _i);
            JStatement kLoop = Utils.makeForLoop(iLoop, new JIntLiteral(
                                                                        rep.joiner), _k);
            list.add(kLoop);
        }
        // pop the right number of items at the end
        if (sumOfWeights * rep.joiner > 0) {
            list.add(new JExpressionStatement(new SIRPopExpression(type,
                                                                   sumOfWeights * rep.joiner)));
        }
        // list.add(Utils.makeForLoop(new JExpressionStatement(null,
        // new SIRPopExpression(type),
        // null),
        // sumOfWeights * rep.joiner));

        // mark end of splitter
        list.add(MarkFilterBoundaries.makeEndMarker(join));

        return new JBlock(null, list, null);
    }

    private static JMethodDeclaration makeInitFunction(SIRSplitJoin sj,
                                                       List children) {
        // Start with the init function from the split/join.
        JMethodDeclaration init = sj.getInit();
        // add calls to init functions
        for (int i=0; i<sj.size(); i++) {
            SIRStream child = sj.get(i);
            List params = sj.getParams(i);
            if (child.needsInit()) {
                init.addStatement(new JExpressionStatement(null,
                                                           new JMethodCallExpression(null,
                                                                                     new JThisExpression(null),
                                                                                     child.getInit().getName(),
                                                                                     (JExpression[])params.toArray(new JExpression[0])),
                                                           null));
            }
        }
        return init;
    }

    private static JMethodDeclaration makeWorkFunction(SIRSplitJoin sj,
                                                       List children,
                                                       SRepInfo rep) {
        // see whether or not we have a duplicate
        boolean isDup = sj.getSplitter().getType()==SIRSplitType.DUPLICATE;
        // see whether or not we have a RR with unary weights (Unary RR)
        boolean isURR = sj.getSplitter().isUnaryRoundRobin();

        // Build the new work function; add the list of statements
        // from each of the component filters.
        JBlock newStatements = new JBlock(null, new LinkedList(), null);
        FindVarDecls findVarDecls = new FindVarDecls();
        Iterator childIter = children.iterator();
        int i=0;
        while (childIter.hasNext())
            {
                SIRFilter filter = (SIRFilter)childIter.next();
                // Get the statements of the old work function
                JBlock statements = filter.getWork().getBody();
                //replace variable accesses with numbered variables
                if (KjcOptions.rename1) {
                    statements = (JBlock)findVarDecls.findAndReplace(statements);
                }
                // Make a for loop that repeats these statements according
                // to reps
                JStatement loop = Utils.makeForLoop(statements, rep.child[i]);
                // test the original statements (rather than the loop,
                // since it cascades pops and peeks) for whether or
                // not there are pops before peeks
                boolean popBeforePeek = Utils.popBeforePeek(statements);
                // if we have a duplicating splitter, we need to make
                // pops into peeks
                if (isDup) {
                    loop = popToPeek(loop, popBeforePeek, 0, 1);
                } 
                // if we have unary round-robin, multiply all indices
                // to tapes by the number of children
                if (isURR) {
                    loop = popToPeek(loop, popBeforePeek, i, sj.size());
                }
                // add the loop to the new work function
                newStatements.addStatement(Utils.peelMarkers(loop));
                i++;
            }

        // add a pop loop to statements that pops the right number of
        // times for the splitjoin
        if ((isDup || isURR) && rep.splitter > 0) {
            newStatements.addStatement(
                                       new JExpressionStatement(
                                                                new SIRPopExpression(sj.getInputType(),rep.splitter)));
                        
            // Utils.makeForLoop(new JExpressionStatement(null,
            //                new SIRPopExpression(sj.getInputType()), null),
            //                rep.splitter));
        }

        //add variable declarations calculated by FindVarDecls
        if (KjcOptions.rename1) {
            findVarDecls.addVariableDeclarations(newStatements);
        }

        // Now make a new work function based on this.
        JMethodDeclaration newWork =
            new JMethodDeclaration(null,
                                   at.dms.kjc.Constants.ACC_PUBLIC,
                                   CStdType.Void,
                                   "work",
                                   JFormalParameter.EMPTY,
                                   CClassType.EMPTY,
                                   newStatements,
                                   null,
                                   null);

        return newWork;
    }

    /**
     * Replace pops in <orig> with peeks to a local counter that keeps
     * track of the current index.  Also adjust peeks accordingly.
     * 
     * The index starts at <offset>, and is multiplied by <scaling>.
     *
     * For example, with offset=10, scaling=2, transformation is:
     *
     * orig:  x = pop(); x = peek(5);
     * new:   x = peek(10+counter++); x = peek(10+(counter+5)*2);
     */
    private static JStatement popToPeek(JStatement orig, final boolean popBeforePeek, final int offset, final int scaling) {
        // remove unused pop statements from <orig>.  they will be
        // replaced by an automatic assignment to the pop counter
        orig = Utils.removeUnusedPops(orig);
        
        // if there is popping before peeking, then we need to keep
        // track of the pop index.  define a variable to be our
        // counter of this pop position
        final JVariableDefinition var = 
            new JVariableDefinition(/* where */ null,
                                    /* modifiers */ 0,
                                    /* type */ CStdType.Integer,
                                    /* ident */
                                    LoweringConstants.getUniqueVarName(),
                                    /* initializer */
                                    new JIntLiteral(0));

        // adjust the contents of <orig> to be relative to <var>
        orig.accept(new SLIRReplacingVisitor() {
                // Whether we are in an ExpressionStatement or not affects
                // behaviour of pops:  as an immediate subexpression of ExpressionStatment,
                // they do not have to return a value.
            
                private boolean inExpressionStatement = false;
               
                public Object visitExpressionStatement(JExpressionStatement self, JExpression expr) {
                    boolean oldInExpressionStatement = inExpressionStatement;
                    if (expr instanceof SIRPopExpression) {inExpressionStatement = true;}
                    Object result = super.visitExpressionStatement(self,expr);
                    inExpressionStatement = oldInExpressionStatement;
                    return result;
                }

                public Object visitPopExpression(SIRPopExpression oldSelf,
                                                 CType oldTapeType) {
                    // Recurse into children.
                    SIRPopExpression self = (SIRPopExpression) super
                        .visitPopExpression(oldSelf, oldTapeType);
 
                    int ntimes = self.getNumPop();

                    if (inExpressionStatement) {
                        JExpression lhs = new JLocalVariableExpression(var);
                        JExpression rhs = new JAddExpression(new JLocalVariableExpression(var),
                                                             new JIntLiteral(ntimes));
                        return new JAssignmentExpression(lhs,rhs);
                    }
                    // if assertion fires, need code to bump past numPop-1 items
                    // then peek
                    // the final popped item.
                    assert ntimes == 1 : "Need code here to handle multiple pop (ntimes=" + ntimes + ")";
                    // reference our var
                    JLocalVariableExpression ref = new JLocalVariableExpression(var);

                    // "peek(offset+counter*scaling);
                    return new SIRPeekExpression(new JAddExpression(new JIntLiteral(offset),
                                                                    doScaling(new JPostfixExpression(OPE_POSTINC, ref), 
                                                                              scaling)),
                                                 oldTapeType);
                }
            
                public Object visitPeekExpression(SIRPeekExpression oldSelf,
                                                  CType oldTapeType,
                                                  JExpression arg) {
                    // Recurse into children.
                    SIRPeekExpression self = (SIRPeekExpression)
                        super.visitPeekExpression(oldSelf,
                                                  oldTapeType,
                                                  arg);
                    // reference our var
                    JLocalVariableExpression ref = new JLocalVariableExpression(var);
 
                    // if pop before peeking, add the index to the pop
                    // counter.  otherwise add it to the offset.
                   if (popBeforePeek) {
                        // "peek(offset+(counter+arg)*scaling)"
                        return new SIRPeekExpression(new JAddExpression(new JIntLiteral(offset),
                                                                        doScaling(new JAddExpression(ref, arg), scaling)),
                                                     oldTapeType);
                    } else {
                        // "peek(offset + arg*scaling)"
                        return new SIRPeekExpression(new JAddExpression(new JIntLiteral(offset), 
                                                                        doScaling(arg, scaling)),
                                                     oldTapeType);
                    }
                }
            });

        // assign pop count to 

        // return the block
        JStatement[] statements = {new JVariableDeclarationStatement(var), orig};
        return new JBlock(statements);
    }

    /**
     * If scaling>1, returns a multiply expression <orig>*<scaling>.
     */
    private static JExpression doScaling(JExpression orig, int scaling) {
        if (scaling > 1) {
            return new JMultExpression(orig, new JIntLiteral(scaling));
        } else {
            return orig;
        }
    }

    private static JFieldDeclaration[] makeFields(SIRSplitJoin sj,
                                                  List children)
    {
        Iterator childIter;

        // Walk the list of children to get the total field length.
        int numFields = 0;
        childIter = children.iterator();
        while (childIter.hasNext())
            {
                SIRFilter filter = (SIRFilter)childIter.next();
                numFields += filter.getFields().length;
            }

        // Now make the field array...
        JFieldDeclaration[] newFields = new JFieldDeclaration[numFields];

        // ...and copy all of the fields in.
        numFields = 0;
        childIter = children.iterator();
        while (childIter.hasNext())
            {
                SIRFilter filter = (SIRFilter)childIter.next();
                for (int i = 0; i < filter.getFields().length; i++)
                    newFields[numFields + i] = filter.getFields()[i];
                numFields += filter.getFields().length;
            }

        // All done.
        return newFields;
    }

    private static JMethodDeclaration[] makeMethods(SIRSplitJoin sj,
                                                    List children)
    {
        // Just copy all of the methods into an array.
        Iterator childIter;

        // Walk the list of children to get the total number of
        // methods.  Skip work functions wherever necessary.
        int numMethods = 0;
        childIter = children.iterator();
        while (childIter.hasNext())
            {
                SIRFilter filter = (SIRFilter)childIter.next();
                numMethods += filter.getMethods().length - 1;
            }

        // Now make the method array...
        JMethodDeclaration[] newMethods = new JMethodDeclaration[numMethods];

        // ...and copy all of the methods in.
        numMethods = 0;
        childIter = children.iterator();
        while (childIter.hasNext())
            {
                SIRFilter filter = (SIRFilter)childIter.next();
                for (int i = 0; i < filter.getMethods().length; i++)
                    {
                        JMethodDeclaration method = filter.getMethods()[i];
                        if (method != filter.getWork()) {
                            newMethods[numMethods++] = method;
                        }
                    }
            }

        // All done.
        return newMethods;
    }

    /**
     * Represents how many times the components of a splitjoin should
     * execute in the steady state.
     */
    static class SRepInfo {

        public int[] child;
        public int joiner;
        public int splitter;

        private SRepInfo(int numChildren) {
            this.child = new int[numChildren];
        }

        /**
         * Returns repInfo giving number of repetitions of streams in <sj>
         * Requires that all children of <sj> be filters.
         */
        public static SRepInfo calcReps(SIRSplitJoin sj) {
            for (int i=0; i<sj.size(); i++) {
                assert sj.get(i) instanceof SIRFilter;
            }
            SRepInfo result = new SRepInfo(sj.size());
            result.compute(sj);
            return result;
        }

        /**
         * Makes the weights valid for the given <sj>
         */
        private void compute(SIRSplitJoin sj) {

            // fill in the execution count info for this
            HashMap[] execCount = SIRScheduler.getExecutionCounts(sj);
            for (int i=0; i<sj.size(); i++) {
                // get the steady-state count
                int[] count = (int[])execCount[1].get(sj.get(i));
                if (count==null) {
                    this.child[i] = 0;
                } else {
                    this.child[i] = count[0];
                }
            }

            // infer how many times the splitter, joiner runs
            int[] splitWeights = sj.getSplitter().getWeights();
            int[] joinWeights = sj.getJoiner().getWeights();

            // infer how many times the splitter, joiner runs

            // beware of sources in splits
            int index = -1;
            boolean nullSplit = false, nullJoin = false;
            for (int i=0; i<child.length;i++) {
                if (child[i]!=0 && splitWeights[i]!=0 && joinWeights[i]!=0) {
                    index = i;
                    break;
                }
                if (i==child.length-1) {
                    // trying to fuse null split or join -- assume weight
                    // on opposite is okay
                    index = i;
                    if (splitWeights[i]==0) {
                        nullSplit = true;
                    } else {
                        nullJoin = true;
                    }
                    //          Utils.fail("Think we're trying to fuse a null split or something--error");
                }
            }
            if (nullSplit) {
                this.splitter = 0;
            } else {
                this.splitter = child[index] * ((SIRFilter)sj.get(index)).getPopInt() / splitWeights[index];
                // make sure we came out even
                assert this.splitter * splitWeights[index] ==
                    this.child[index] *
                    ((SIRFilter)sj.get(index)).getPopInt();
            }
            // now for joiner
            if (nullJoin) {
                this.joiner = 0;
            } else {
                this.joiner = this.child[index] * ((SIRFilter)sj.get(index)).getPushInt() / joinWeights[index];
                // make sure we come out even
                assert this.joiner * joinWeights[index] ==
                    this.child[index] *
                    ((SIRFilter)sj.get(index)).getPushInt();
            }
        }
    }

    static class SRate {
        public int push;
        public int pop;
        public int peek;

        public SRate(int push, int pop, int peek) {
            this.push = push;
            this.pop = pop;
            this.peek = peek;
        }
    }
}
