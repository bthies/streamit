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
        List<SIRStream> children = sj.getParallelStreams();
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
        SIRFilter newFilter;
        if (!childrenAreIdentities(sj)) {
            // only need to perform normal fusion if some child filter
            // is not an identity
            newFilter = new SIRFilter(sj.getParent(), "Fused_" + sj.getIdent(),
                                      newFields, newMethods, new JIntLiteral(rate.peek),
                                      new JIntLiteral(rate.pop), new JIntLiteral(rate.push),
                                      newWork, sj.getInputType(), sj.getOutputType());
            // Use the new init function
            newFilter.setInit(newInit);
        } else {
            // if all children identities, need an expander in the
            // case of a duplicate splitter and nothing otherwise (of
            // course with identities we still might need splitter and
            // joiner substitutes below, in makeFusedPipe)
            if (sj.getSplitter().getType()==SIRSplitType.DUPLICATE) {
                newFilter = buildExpander(sj.size(), rep.child[0], sj.getInputType());
            } else {
                // don't need any fused filter; we use an identity as a placeholder
                newFilter = new SIRIdentity(sj.getInputType());
            }
        }

        // make new pipeline representing fused sj
        SIRPipeline fused = makeFusedPipe(sj, rep, rate, newFilter);

        // replace in parent
        SIRStream result = replaceInParent(sj, fused);

        // uncomment this if you want to see what the output of fusion looks like
        //SIRToStreamIt.run(result);

        return result;
    }
    
    /**
     * Returns whether all of the immediate children of <sj> are
     * identity filters.
     */
    static boolean childrenAreIdentities(SIRSplitJoin sj) {
        for (int i=0; i<sj.size(); i++) { 
            if (!(sj.get(i) instanceof SIRIdentity)) {
                return false;
            }
        }
        return true;
    }

    /** 
     * Returns a filter that, on each firing, transfers k items
     * directly from input to output, then repeats that sequence of
     * outputs N times.  The I/O types are both 'type'.  The resulting
     * I/O rate is pop k, push k*N.
     */
    static SIRFilter buildExpander(int N, int k, CType type) {
        // Make core code to do replicating.  Here's what we're going for:
        // for (int i=0; i<N; i++) {
        //   j = 0; 
        //   for (; j<k; j++) {
        //     push(peek(j));
        //   }
        // }
        // pop(k);
        // -------------
        
        // construct push(peek(j));
        JVariableDefinition j = new JVariableDefinition(CStdType.Integer, "j");
        JStatement push = new JExpressionStatement(new SIRPushExpression(new SIRPeekExpression(new JLocalVariableExpression(j), type), type));
        // for (int j=0; j<k; j++) { push(peek(j)); }
        JStatement innerLoop = Utils.makeForLoop(push, new JIntLiteral(k), j);
        // { j=0; innerLoop; }
        JBlock innerBlock = new JBlock(new JStatement[] { new JExpressionStatement(new JAssignmentExpression(new JLocalVariableExpression(j), new JIntLiteral(0))), 
                                                          innerLoop });
        // for (int i=0; i<N; i++) { innerBlock }
        JStatement outerLoop = Utils.makeForLoop(innerBlock, N);
        // now build the block of statements...
        JBlock body = new JBlock();
        body.addStatement(outerLoop);
        // follow it up with a pop statement
        body.addStatement(new JExpressionStatement(new SIRPopExpression(type, k)));
        // -------------

        // Make work and init functions
        JMethodDeclaration work = new JMethodDeclaration(CStdType.Void,
                                                         "work",
                                                         JFormalParameter.EMPTY,
                                                         body);
        JMethodDeclaration init = SIRStream.makeEmptyInit();

        // Make filter
        SIRFilter result = new SIRFilter(null, 
                                         "AutoGeneratedExpander", 
                                         JFieldDeclaration.EMPTY(), 
                                         new JMethodDeclaration[] { init, work },
                                         new JIntLiteral(k), // peek
                                         new JIntLiteral(k), // pop
                                         new JIntLiteral(k*N), // push
                                         work, type, type);
        result.setInit(init);
        return result;
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
        //  - it's a null split
        //  - it only executes once
        if (!(sj.getSplitter().getType()==SIRSplitType.DUPLICATE ||
              rep.splitter==0 ||
              // don't need a splitter filter if the splitter only
              // executes once, since it would preserve the order of the items
              rep.splitter==1
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

        // don't bother adding identity filters to the pipeline
        if (!(newFilter instanceof SIRIdentity)) {
            pipe.add(newFilter, new LinkedList(sj.getParent().getParams(index)));
        }

        // make a joinFilter only if it's not a not a null join
        if (rep.joiner!=0 &&
            // don't need a joiner filter if the joiner only executes
            // once, because it would preserve the order of the items
            rep.joiner!=1) {
            pipe.add(makeFilter(pipe,
                                "Post_" + sj.getIdent(),
                                makeJoinFilterBody(sj.getJoiner(),
                                                   rep,
                                                   sj.getOutputType()),
                                rate.push, rate.push, rate.push,
                                sj.getOutputType()));
        }

        // if we didn't add anything, add an identity filter
        if (pipe.size()==0) {
            pipe.add(new SIRIdentity(sj.getInputType()));
        }
        return pipe;
    }

    // returns result of fusion
    private static SIRStream replaceInParent(SIRSplitJoin sj,
                                             SIRPipeline fused) {
        SIRContainer parent = sj.getParent();
        // if <fused> has just one filter, add it in place of <sj>
        if (fused.size()==1) {
            // if <fused> has only an identity filter and parent
            // is pipeline with at least one more filter, remove
            // sj from parent if it's a pipeline
            if (fused.get(0) instanceof SIRIdentity && 
                parent instanceof SIRPipeline &&
                parent.size() > 1) {
                parent.remove(sj);
                // this is rather dangerous because the returned
                // filter is not in the stream graph.  However, I
                // think that most passes calling fusion either use
                // only the stream graph, or reconstruct the stream
                // graph themselves.
                return fused.get(0);
            } else {
                parent.replace(sj, fused.get(0));
                return fused.get(0);
            }
        } else {
            // otherwise, just add <fused> to parent in place of <sj>
            parent.replace(sj, fused);
            // clear the param list pased to <fused>
            parent.getParams(parent.indexOf(fused)).clear();
            return fused;
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

    private static void doRenaming(List<SIRStream> children) {
        // Rename all of the child streams of this.
        Iterator<SIRStream> iter = children.iterator();
        while (iter.hasNext()) {
            SIRFilter filter = (SIRFilter)iter.next();
            RenameAll.renameFilterContents((SIRFilter)filter);
        }
    }

    private static void doPhaseInlining(List<SIRStream> children) {
        // inline all phases, as they aren't supported in fusion yet
        Iterator<SIRStream> iter = children.iterator();
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
        Iterator<SIRStream> childIter = sj.getParallelStreams().iterator();
        while (childIter.hasNext()) {
            SIRStream str = childIter.next();
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
     *
     * NOTE: If you need to understand this code, I recommend
     * uncommenting the SIRToStreamIt pass (called above) to see what
     * it outputs.  It is mostly just construcing IR code, which is
     * slightly different if all the weights are the same or different
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

        // check whether or not all weights are the same int literal
        boolean sameWeights = true; // whether or not all weights have the same int literal value
        int sameWeightsVal = weights[0];    // the value of all the weights, if they are the same
        for (int i=1; i<weights.length; i++) {
            if (weights[i] != sameWeightsVal) {
                sameWeights = false;
                break;
            }
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
            // _partialSum[N] = { , , }
            JArrayInitializer _partialSumInit = new JArrayInitializer(null, partialSumExpression);
            JVariableDefinition _partialSum = new JVariableDefinition(null,
                                                                      0,
                                                                      new CArrayType(CStdType.Integer, 1, new JExpression[] { new JIntLiteral(weights.length) } ),
                                                                      "_partialSum",
                                                                      _partialSumInit);
            // only need to declare these arrays if we have non-uniform weights
            if (!sameWeights) {
                list.add(new JVariableDeclarationStatement(null, new JVariableDefinition[] {_weights}, null));
                list.add(new JVariableDeclarationStatement(null, new JVariableDefinition[] {_partialSum}, null));
            }
            // make loop variables
            JVariableDefinition _i = new JVariableDefinition(null, 0, CStdType.Integer, "_i", new JIntLiteral(0));
            JVariableDefinition _j = new JVariableDefinition(null, 0, CStdType.Integer, "_j", new JIntLiteral(0));
            JVariableDefinition _k = new JVariableDefinition(null, 0, CStdType.Integer, "_k", new JIntLiteral(0));
            // expression for partialsum_k.  will be an induction var if weights are same
            JVariableDefinition partialSum_k_var = null; 
            JExpression partialSum_k_exp;
            boolean partialSum_k_induction = false;
            if (sameWeights) {
                // if weight is 1, partial sum is just the loop counter
                if (sameWeightsVal == 1) {
                    partialSum_k_exp = new JLocalVariableExpression(_k);
                } else {
                    // otherwise, compute the product as an induction variable
                    partialSum_k_induction = true;
                    partialSum_k_var = new JVariableDefinition(0, CStdType.Integer, "partialSum_k", new JIntLiteral(0));
                    partialSum_k_exp = new JLocalVariableExpression(partialSum_k_var);
                }
            } else {
                partialSum_k_exp = new JArrayAccessExpression(null,
                                                              new JLocalVariableExpression(_partialSum),
                                                              new JLocalVariableExpression(_k),
                                                              CStdType.Integer);
            }
            // variable to hold the induction variable i*sumOfWeights+partialSum_k (initializated to partialSum_k, then i+=sumOfWeights within i loop)
            JVariableDefinition iTimesSumOfWeights = new JVariableDefinition(0, CStdType.Integer, "iTimesSumOfWeights_Plus_PartialSum_k", partialSum_k_exp);
            // make loop body
            JStatement inner = new JExpressionStatement(null,
                                                        new SIRPushExpression(new SIRPeekExpression(new JAddExpression(new JLocalVariableExpression(iTimesSumOfWeights),
                                                                                                                       new JLocalVariableExpression(null, _j)),
                                                                                                    type),

                                                                              type),
                                                        null);
            // get loop bound on j loop...
            JExpression loopBound;
            if (sameWeights) {
                // if all weights same, just loop up to that weight
                loopBound = new JIntLiteral(sameWeightsVal);
            } else {
                // else, need to index into array of weights for loop bound
                loopBound = new JArrayAccessExpression(null, new JLocalVariableExpression(null, _weights), new JLocalVariableExpression(null, _k));
            }
            
            // add k loop
            JStatement jLoop = Utils.makeForLoop(inner, loopBound, _j);
            // before j loop, maintain iTimesSumOfWeights variable
            JBlock jBlock = new JBlock(new JStatement[] { 
                jLoop,
                // iTimesSumOfWeights += sumOfWeights
                new JExpressionStatement(new JAssignmentExpression(new JLocalVariableExpression(iTimesSumOfWeights), new JAddExpression(new JLocalVariableExpression(iTimesSumOfWeights),
                                                                                                                                        new JIntLiteral(sumOfWeights)))) });

            JStatement iLoop = Utils.makeForLoop(jBlock, new JIntLiteral(rep.splitter), _i);
            // before iLoop, initialize induction vars to 0. 
            JBlock iBlock = new JBlock();
            // declare iTimesSumOfWeights
            iBlock.addStatement(new JVariableDeclarationStatement(iTimesSumOfWeights));
            iBlock.addStatement(iLoop);
            // increment partialSum_k as an induction variable if needed
            if (partialSum_k_induction) {
                // partialSum_k += sameWeightsVal
                iBlock.addStatement(new JExpressionStatement(new JAssignmentExpression(partialSum_k_exp, new JAddExpression(partialSum_k_exp, new JIntLiteral(sameWeightsVal)))));
            }

            JStatement kLoop = Utils.makeForLoop(iBlock, new JIntLiteral(weights.length), _k);
            JBlock kBlock = new JBlock();
            // if doing induction variable here, initialize partialSum_k as an induction variable
            if (partialSum_k_induction) {
                kBlock.addStatement(new JVariableDeclarationStatement(partialSum_k_var));
            }
            kBlock.addStatement(kLoop);

            list.add(kBlock);
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
     *
     * NOTE: If you need to understand this code, I recommend
     * uncommenting the SIRToStreamIt pass (called above) to see what
     * it outputs.  It is mostly just construcing IR code, which is
     * slightly different if all the weights are the same or different
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

        // check whether or not all weights are the same int literal
        boolean sameWeights = true; // whether or not all weights have the same int literal value
        int sameWeightsVal = weights[0];    // the value of all the weights, if they are the same
        for (int i=1; i<weights.length; i++) {
            if (weights[i] != sameWeightsVal) {
                sameWeights = false;
                break;
            }
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
            // _partialSum[N] = { , , }
            JArrayInitializer _partialSumInit = new JArrayInitializer(null, partialSumExpression);
            JVariableDefinition _partialSum = new JVariableDefinition(null,
                                                                      0,
                                                                      new CArrayType(CStdType.Integer, 1, new JExpression[] { new JIntLiteral(weights.length) } ),
                                                                      "_partialSum",
                                                                      _partialSumInit);
            // only need to declare these arrays if we have non-uniform weights
            if (!sameWeights) {
                list.add(new JVariableDeclarationStatement(null, new JVariableDefinition[] {_weights}, null));
                list.add(new JVariableDeclarationStatement(null, new JVariableDefinition[] {_partialSum}, null));
            }
            // make loop variables
            JVariableDefinition _i = new JVariableDefinition(null, 0, CStdType.Integer, "_i", new JIntLiteral(0));
            JVariableDefinition _j = new JVariableDefinition(null, 0, CStdType.Integer, "_j", new JIntLiteral(0));
            JVariableDefinition _k = new JVariableDefinition(null, 0, CStdType.Integer, "_k", new JIntLiteral(0));
            // expression for weights[i]
            JExpression weights_i;
            if (sameWeights) {
                weights_i = new JIntLiteral(sameWeightsVal);
            } else {
                weights_i = new JArrayAccessExpression(null,
                                                       new JLocalVariableExpression(null, _weights),
                                                       new JLocalVariableExpression(null, _i));
            }
            // expression for partialsum_i.  might be an induction variable.
            JVariableDefinition partialSum_i_var = null;
            JExpression partialSum_i_exp;
            boolean partialSum_i_induction = false;
            if (sameWeights) {
                // if weight * rep.joiner is 1, can just use i counter instead of calculating a product
                if (sameWeightsVal * rep.joiner == 1) {
                    partialSum_i_exp = new JLocalVariableExpression(_i);
                } else {
                    // otherwise, calculate induction variable
                    partialSum_i_induction = true;
                    partialSum_i_var = new JVariableDefinition(0, CStdType.Integer, "partialSum_i", new JIntLiteral(0));
                    partialSum_i_exp = new JLocalVariableExpression(partialSum_i_var);
                }
            } else {
                partialSum_i_exp = new JArrayAccessExpression(null,
                                                              new JLocalVariableExpression(_partialSum),
                                                              new JLocalVariableExpression(_i), 
                                                              CStdType.Integer);
            }
            // if weights are same, can use induction variable to hold k*weights[i]
            JVariableDefinition kTimesWeights_i_var = null;
            JExpression kTimesWeights_i_exp;
            boolean kTimesWeights_i_induction = false;
            if (sameWeights) {
                // if the weight is 1, we can just use the k counter
                if (sameWeightsVal == 1) {
                    kTimesWeights_i_exp = new JLocalVariableExpression(_k);
                } else {
                    // otherwise, we will calculate the product as an induction variable
                    kTimesWeights_i_induction = true;
                    // variable holding induction variable kTimesWeights_i (maintained by adding weights_i on each iteration of k loop)
                    kTimesWeights_i_var = new JVariableDefinition(0, CStdType.Integer, "kTimesWeights_i", new JIntLiteral(0));
                    kTimesWeights_i_exp = new JLocalVariableExpression(kTimesWeights_i_var);
                }
            } else {
                kTimesWeights_i_exp = new JMultExpression(new JLocalVariableExpression(_k),
                                                          new JArrayAccessExpression(new JLocalVariableExpression(_weights),
                                                                                     new JLocalVariableExpression(_i)));
            }
            // make loop body
            JStatement inner = new JExpressionStatement(null,
                                                        new SIRPushExpression(new SIRPeekExpression(new JAddExpression(kTimesWeights_i_exp,
                                                                                                                       new JAddExpression(partialSum_i_exp,
                                                                                                                                          new JLocalVariableExpression(null, _j))),
                                                                                                    type),
                                                                              
                                                                              type), null);
            // get loop bound on j loop...
            JExpression loopBound;
            if (sameWeights) {
                // if all weights same, just loop up to that weight
                loopBound = new JIntLiteral(sameWeightsVal);
            } else {
                // else, need to index into array of weights for loop bound
                loopBound = new JArrayAccessExpression(null,
                                                       new JLocalVariableExpression(null, _weights),
                                                       new JLocalVariableExpression(null, _i));
            }

            // add k loop
            JStatement jLoop = Utils.makeForLoop(inner,
                                                 loopBound, _j);
            JBlock jBlock = new JBlock();
            jBlock.addStatement(jLoop);
            // if induction var, update partialSum_i induction var
            if (partialSum_i_induction) {
                // partialSum_i += rep.joiner * sameWeightsVal
                jBlock.addStatement(new JExpressionStatement(new JAssignmentExpression(partialSum_i_exp, new JAddExpression(partialSum_i_exp, new JIntLiteral(rep.joiner * sameWeightsVal)))));
            }

            JStatement iLoop = Utils.makeForLoop(jBlock, new JIntLiteral(weights.length), _i);
            JBlock iBlock = new JBlock();
            if (partialSum_i_induction) {
                // if induction var, the initialize partialSum_i induction var
                iBlock.addStatement(new JVariableDeclarationStatement(partialSum_i_var));
            }
            iBlock.addStatement(iLoop);
            if (kTimesWeights_i_induction) {
                // if using induction variable here, then increment
                // kTimesWeights_i (induction var) before i loop
                // kTimesWeights_i += weights_i
                iBlock.addStatement(new JExpressionStatement(new JAssignmentExpression(kTimesWeights_i_exp, new JAddExpression(kTimesWeights_i_exp,
                                                                                                                               weights_i))));
            }

            JStatement kLoop = Utils.makeForLoop(iBlock, new JIntLiteral(rep.joiner), _k);
            JBlock kBlock = new JBlock();
            if (kTimesWeights_i_induction) {
                // if using induction variable here, then before kLoop, initialize kTimesWeights_i (induction var)
                kBlock.addStatement(new JVariableDeclarationStatement(kTimesWeights_i_var));
            }
            kBlock.addStatement(kLoop);

            list.add(kBlock);
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
                                                       List<SIRStream> children) {
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
                                                       List<SIRStream> children,
                                                       SRepInfo rep) {
        // see whether or not we have a duplicate
        boolean isDup = sj.getSplitter().getType()==SIRSplitType.DUPLICATE;
        // see whether or not we have a RR with unary weights (Unary RR)
        boolean isURR = sj.getSplitter().isUnaryRoundRobin();

        // Build the new work function; add the list of statements
        // from each of the component filters.
        JBlock newStatements = new JBlock(null, new LinkedList(), null);
        FindVarDecls findVarDecls = new FindVarDecls();
        Iterator<SIRStream> childIter = children.iterator();
        int i=0;
        while (childIter.hasNext())
            {
                SIRFilter filter = (SIRFilter)childIter.next();
                // Get the statements of the old work function
                JBlock statements = filter.getWork().getBody();
//                //replace variable accesses with numbered variables
//                if (KjcOptions.rename1) {
//                    statements = (JBlock)findVarDecls.findAndReplace(statements);
//                }
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
            int itemsConsumed;
            if (isDup) {
                itemsConsumed = rep.splitter;
            } else {
                // in coarse-grained execution mode, each firing of RR
                // consumes an item per parallel stream
                itemsConsumed = rep.splitter * sj.size();
            }
            newStatements.addStatement(new JExpressionStatement(new SIRPopExpression(sj.getInputType(),itemsConsumed)));
                        
            // Utils.makeForLoop(new JExpressionStatement(null,
            //                new SIRPopExpression(sj.getInputType()), null),
            //                rep.splitter));
        }

//        //add variable declarations calculated by FindVarDecls
//        if (KjcOptions.rename1) {
//            findVarDecls.addVariableDeclarations(newStatements);
//        }

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
                                                  List<SIRStream> children)
    {
        Iterator<SIRStream> childIter;

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
                                                    List<SIRStream> children)
    {
        // Just copy all of the methods into an array.
        Iterator<SIRStream> childIter;

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
