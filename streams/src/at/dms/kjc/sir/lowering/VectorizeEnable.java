// $Id$
/**
 * 
 */
package at.dms.kjc.sir.lowering;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.lowering.fusion.FusePipelines;
import at.dms.kjc.sir.lowering.partition.WorkEstimate;

import java.util.*;

/**
 * Mung code to allow naive vectorization.
 * Note: static methods in this class are not designed to be re-entrant.
 * @author Allyn Dimock
 */
public class VectorizeEnable {
    /**
     * Set to true to list sequences of vectorizable filters before fusion and individual vectorizable filters after fusion.
     */
    public static boolean debugging = false;
    
    /**
     * Keep from producing vectors over tapes until I can debug.
     */
    public static boolean vectorsOverTapesDebugged = false;
    
    /** All vectorizable filters (after fusion) */
    private static Set<SIRFilter> allVectorizable;
    /** subset of allVectorizable that could produce vector output (but may not end up doing so) */
    private static Set<SIRFilter> allHavingVectorOutput;
    /** subset of allVectorizable that are worth vectorizing according to local criteria */
    private static Set<SIRFilter> allIndividuallyWorthwhile;
    /** subset of allVectorizable that are worth vectorizing (iterate over these callinfgVerctorize() */
    private static Set<SIRFilter> allWorthwhile;
    
    /** 
     * Perform naive vectorization on eligible filters in a stream.
     * <br/>
     * Causes subgraphs and pipeline segments of 
     * {@link Vectorizable#vectorizable(SIRFilter) vectorizable} filters to be fused
     * if filters after the first do not peek.
     * Causes {@link Vectorizable#vectorizable(SIRFilter) vectorizable} filters to be 
     * executed a multiple of 4 times per steady state.
     * <br/>
     * Vectorization should be run after partitioning, but before fusion or (final) scheduling.
     * <br/>
     * Notes:
     * <ul><li>
     * Run before scheduling.
     * </li><li>
     * Only run if you can afford to increase multiplicity of filters: need to document this for user.
     * </li></ul>
     * @param str  the Stream to be munged.
     * @param partitionTable  partition table so filters in different partitions are not fused.  Not currently checked!
     * @return modified str (str replaced with modified version in parent if any)
     */
    public static SIRStream vectorizeEnable(SIRStream str,
            Map<SIROperator,Integer> partitionTable) {
        if (KjcOptions.vectorize < 8) {
            // can not vectorize ints and floats unless
            // vector registers are at least 8 bytes.
            return str;
        }
        allVectorizable = new HashSet<SIRFilter>();
        allHavingVectorOutput = new HashSet<SIRFilter>();
        allIndividuallyWorthwhile = new HashSet<SIRFilter>();
        allWorthwhile = new HashSet<SIRFilter>();
        
        if (debugging) {
            // now find all vectorizable filters in fused graph
            Set<SIRFilter> vectorizableFilters = markVectorizableFilters(str);
            // find maximal subgraphs or pipeline segments that are not in feedback loops.
            Map<SIRStream,List<intPair>> segments = findVectorizablesegments(vectorizableFilters,str);
            
            System.err.println("VectorizeEnable: before fusing vectorizable filters");
            // diagnostic dump...
            dumpStructure(segments);
        }
        
        // Fuse together all fusable sub-graphs or pipeline segments
        // of vectorizable filters, preserving vectorizability.
        // (comment out to test vectorization without fusion.)
        
        //str = FusePipelines.fusePipelinesOfVectorizableFilters(str);  old pipeline-only version
        str = FusePipelines.fusePipelinesOfVectorizableStreams(str);

        // now find all vectorizable filters in fused graph
        Set<SIRFilter> vectorizableFilters = markVectorizableFilters(str);
        // find maximal subgraphs or pipeline segments that are not in feedback loops.
        // (This maximal segment stuff is obsolete from time when thought about doing own fusion.
        //  but not highest priority to rip out, simplify.)
        Map<SIRStream,List<intPair>> segments = findVectorizablesegments(vectorizableFilters,str);
        
        if (debugging) {
            System.err.println("VectorizeEnable: after fusing vectorizable filters");
            // diagnostic dump...
            dumpStructure(segments);
            WorkEstimate workEst = WorkEstimate.getWorkEstimate(str);
            workEst.printGraph(str, "work-and-vectorization.dot");
        }
        
        // find the filters to vectorize
        final Set<SIRFilter> all = allVectorizable;
        findAllFiltersInSegments(segments,all);
        
        for (SIRFilter f : allVectorizable) {
            if (Vectorizable.isUseful(f)) {
                allIndividuallyWorthwhile.add(f);
            }
            // to determine if the filter produces vector output
            // we currnetly need to vectorize it, but vectorization
            // transforms a filter in place, so need a clone in case
            // we decide not to vectorize later or to vectorize with
            // different arguments.
            SIRFilter fclone = (SIRFilter)at.dms.kjc.AutoCloner.deepCopy(f);
            try {
                if (Vectorize.vectorize(fclone, true, true)) {
                    allHavingVectorOutput.add(f);
                }
            } catch (Throwable t) {
                // here if filter could not vectorize output:
                // so don't add it to allHavingVectorOutput...
            }
        }
        // add to worthwhile set those filters that are in series or parallel
        // with filters that are worthwhile to vectorize.
        buildAllWorthwhile();
        
        for (SIRFilter toVectorize : allWorthwhile) {
            boolean usesVectorInput = useVectorInput(toVectorize);
            boolean usesVectorOutput = useVectorOutput(toVectorize);
            
            // XXX: Wretched abuse of a compiler flag that should
            // not occur with vectorization, for purposes of testing.
            if (! KjcOptions.magic_net) {
                if (debugging) {
                    System.err.println("Vectorizing " + toVectorize.getName()
                            + (usesVectorInput ? " with vector input " : "") 
                            + (usesVectorOutput ? " with vector output " : "") );
                }
                // TODO: bug in values when using vector tapes
                if (vectorsOverTapesDebugged) {
                    Vectorize.vectorize(toVectorize, usesVectorInput, usesVectorOutput);
                } else {
                    Vectorize.vectorize(toVectorize, false, false);
                }
            }
            // TODO: bug in values when using vector tapes
            if (vectorsOverTapesDebugged) {            
                forScheduling(toVectorize, usesVectorInput, usesVectorOutput);
            } else {
                forScheduling(toVectorize, false, false);
            }
        }
        
        if (debugging) {
            WorkEstimate workEst = WorkEstimate.getWorkEstimate(str);
            workEst.printGraph(str, "work-and-vectorization.dot");
        }

        // about to return, don't leak memory by keeping static data.
        allVectorizable = null;
        allHavingVectorOutput = null;
        allIndividuallyWorthwhile = null;
        allWorthwhile = null;

        return str;
    }
    
    /**
     * Build the allWorthwhile set: the filters that will be vectorized.
     * Base is inclusion in allIndividuallyWorthwhile set.
     * 
     * This method may also call a method that alters the SIR graph structure
     * to introduce vectorization above a duplicate splitter.
     */
    static private void buildAllWorthwhile() {
        allWorthwhile.addAll(allIndividuallyWorthwhile);
        Set<SIRFilter> notYetWorthwhile = new HashSet<SIRFilter>();
        notYetWorthwhile.addAll(allVectorizable);
        notYetWorthwhile.removeAll(allVectorizable);
        int prevSizeNotWorthwhile = 0;
        while (notYetWorthwhile.size() != prevSizeNotWorthwhile) {
            prevSizeNotWorthwhile = notYetWorthwhile.size();

            for (SIRFilter f : notYetWorthwhile) {
                boolean worthwhile = false;
                // worthwhile if all successors are vectorizable and any is
                // worthwhile
                if (canPassVectorTypeDown(SIRNavigationUtils
                        .getSuccessorOper(f))) {
                    Set<SIRFilter> successors = SIRNavigationUtils
                            .getSuccessorFilters(f);
                    if (allVectorizable.containsAll(successors)) {
                        for (SIRFilter s : successors) {
                            if (allWorthwhile.contains(s)) {
                                worthwhile = true;
                                break;
                            }
                        }
                    }
                }
                // worthwhile if all predecessors return vector type and any is
                // worthwhile
                if (!worthwhile
                        && canPassVectorTypeUp(SIRNavigationUtils
                                .getPredecessorOper(f))) {
                    Set<SIRFilter> predecessors = SIRNavigationUtils
                            .getPredecessorFilters(f);
                    if (allHavingVectorOutput.containsAll(predecessors)) {
                        for (SIRFilter s : predecessors) {
                            if (allWorthwhile.contains(s)) {
                                worthwhile = true;
                                break;
                            }
                        }
                    }
                }
                // worthwhile if at top of splitjoin and any sibling worthwhile
                // in which case may want to vectorize before splitjoin.
                if (!worthwhile
                        && SIRNavigationUtils.getPredecessorOper(f) instanceof SIRSplitter) {
                    Set<SIRFilter> siblings = SIRNavigationUtils
                            .getSuccessorFilters(SIRNavigationUtils
                                    .getPredecessorOper(f));
                    if (allVectorizable.containsAll(siblings)) {
                        for (SIRFilter s : siblings) {
                            if (allWorthwhile.contains(s)) {
                                worthwhile = true;
                                break;
                            }
                        }
                    }
                }
                if (worthwhile) {
                    allWorthwhile.add(f);
                    notYetWorthwhile.remove(f);
                }
            }
        }

        Set<SIRFilter> tryVectorizeAbove = new HashSet<SIRFilter>();
        // if vectorizing all siblings below a splitter
        // check whether can vectorize above splitter to decrease number
        // of gathers performed by the filters.
        //
        // In two phases since need to add any new filter to allWorthwhile,
        // which can not do inside iteration over allWorthwhile.
        for (SIRFilter f : allWorthwhile) {
            if (SIRNavigationUtils.getPredecessorOper(f) instanceof SIRSplitter
                    && allWorthwhile.containsAll(SIRNavigationUtils
                            .getSuccessorFilters(SIRNavigationUtils
                                    .getPredecessorOper(f)))) {
                tryVectorizeAbove.add(f);
            }
        }
        for (SIRFilter f : tryVectorizeAbove) {
            vectorizeAboveSplitter(f);
        }

    }
    
    /**
     * If the predecessor of this filter in the graph is a duplicate splitter 
     * and the input type of this splitter is not a vector type then introduce
     * a vectorizable identity filter above the splitter, so that there is only
     * one gather operation for all the pops or peeks from the vectorized filters
     * after the splitter.
     * @param f 
     */
    static void vectorizeAboveSplitter(SIRFilter f) {
        SIROperator pred = SIRNavigationUtils.getPredecessorOper(f);
        if (pred instanceof SIRSplitter && canPassVectorType(pred)) {
            SIRStream parent = pred.getParent();
            assert parent instanceof SIRSplitJoin;
            SIROperator predpred = SIRNavigationUtils.getPredecessorOper(parent);
            if (predpred instanceof SIRFilter && allHavingVectorOutput.contains((SIRFilter)predpred)) {
                allWorthwhile.add((SIRFilter)predpred);
            } else {
                SIRPipeline wrapper = SIRContainer.makeWrapper(parent);
                SIRFilter vecIdentity = new SIRFilter("vectorize");
                vecIdentity.makeIdentityFilter(new JIntLiteral(1), f.getInputType());
                allHavingVectorOutput.add(vecIdentity);
                allWorthwhile.add(vecIdentity);
                wrapper.add(0,vecIdentity);
            }
        }
    }
    
    /**
     * Should a vectorizable filter take a vector type as input?
     * (Is it the case that all immediately preceeding filters will be vectorized and that a vector type can pass over edges from them?)
     * 
     */
    
    static boolean useVectorInput(SIRFilter f) {
        if (canPassVectorTypeUp(SIRNavigationUtils.getPredecessorOper(f))) {
            // vector type can pass from previous filter
            Set<SIRFilter> previous = SIRNavigationUtils.getPredecessorFilters(f);
            if (! (allWorthwhile.containsAll(previous) && allHavingVectorOutput.containsAll(previous))) {
                return false;
            }
            // if just after splitter, must check that all siblings 
            // are also vectorizable
            if (SIRNavigationUtils.getPredecessorOper(f) instanceof SIRSplitter) {
                Set<SIRFilter> siblings = SIRNavigationUtils.getSuccessorFilters(SIRNavigationUtils.getPredecessorOper(f));
                return allWorthwhile.containsAll(siblings);
            } else {
                return true;
            }
        } else {
            // vector type can not pass through splitter or joiner from previous filter.
            return false;
        }
    }
    
    /**
     * Should a vectorizable filter produce a vector type as output?
     * (Is it the case that all immediately following filters will be vectorized and that a vector type can pass over edges to them?)
     * 
     */
    
    static boolean useVectorOutput(SIRFilter f) {
        if (allHavingVectorOutput.contains(f) && canPassVectorTypeDown(SIRNavigationUtils.getSuccessorOper(f))) {
            // vector type can pass no next filter
            Set<SIRFilter> next = SIRNavigationUtils.getSuccessorFilters(f);
            return allWorthwhile.containsAll(next);
        } else {
            // vector type can not pass through splitter or joiner from previous filter.
            return false;
        }
    }
    
    /**
     * Determine whether a vector type can pass over graph edge connecting to SIROperator <b>op</b>.
     * 
     * @param op A SIROperator
     * @return true if operator at this end of edge does not prohibit passing vector type over edge.
     */
    static boolean canPassVectorType(SIROperator op) {
        if (op instanceof SIRFilter) {
            // can always pass a vector type between filters.
            return true;
        }
        if (op instanceof SIRJoiner) {
            // can only pass a vector type through a joiner if
            // the splitjoin can be rewritten to simulate proper
            // ordering on scalar stream.
            // just return false for now.
            return false;
        }
        if (op instanceof SIRSplitter) {
            SIRSplitter sp = (SIRSplitter)op;
            if (sp.getType().isDuplicate()) {
                for (int i : sp.getWeights()) {
                    if (i != 1) { return false; }
                }
                return true;
            } else {
                // can only pass a vector type through a general splitter if
                // the splitjoin can be rewritten to simulate proper
                // ordering on scalar stream.
                // just return false for now.
                return false;
            }
        }
        // assert only filters, splitters, joiners.
        throw new AssertionError(op);
    }
    
    /**
     * Could a vector type be passed through this operator and predecessors until a filter is reached?
     * @param op first operator to check
     * @return true if vector can pass through op and predecessors to filter.
     */
    static boolean canPassVectorTypeUp(SIROperator op) {
        if (op instanceof SIRFilter) { 
            // base case 1: stop search at a filter and return true
            return true; 
        }
        if (! canPassVectorType(op)) {
            // base case 2: if can not pass through current operator, return false
            return false;
        }
        Set<SIROperator> ops = SIRNavigationUtils.getPredecessorOpers(op);
        for (SIROperator prevop : ops) {
            if (! canPassVectorTypeUp(prevop)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Could a vector type be passed through this operator and successors until a filter is reached?
     * @param op first operator to check
     * @return true if vector can pass through op and successors to filter.
     */
    static boolean canPassVectorTypeDown(SIROperator op) {
        if (op instanceof SIRFilter) { 
            // base case 1: stop search at a filter and return true
            return true; 
        }
        if (! canPassVectorType(op)) {
            // base case 2: if can not pass through current operator, return false
            return false;
        }
        // recursive case.
        Set<SIROperator> ops = SIRNavigationUtils.getSuccessorOpers(op);
        for (SIROperator nextop : ops) {
            if (! canPassVectorTypeDown(nextop)) {
                return false;
            }
        }
        return true;
    }
    
    
    /**
     * Unconditionally mung filter for naive vectorization.
     * <br/>
     * This method only takes care of interaction with the scheduler.
     * {@link Vectorize#vectorize(SIRFilter)} fixes types and
     * {@link SimplifyPeekPopPush} must previously have been run.
     * Set rates as follows:
     * <ul><li>  push' = push * 4           (unless using vector output)
     * </li><li> peek' = peek + 3 * pop     (unless using vector input)
     * </li><li> pop'  = pop * 4            (unless using vector input)
     * </li></ul>
     * @param f : filter to be munged.
     * @param usesVectorInput if true then do not adjust pop, peek rates
     * @param usesVectorOutput if true then do not adjust push rate
     */
    private static void forScheduling (SIRFilter f, boolean usesVectorInput, boolean usesVectorOutput) {
        int veclen = KjcOptions.vectorize / 4;
        
        JMethodDeclaration workfn = f.getWork();
        JBlock workBody = workfn.getBody();

        // adjust rates.
        if (KjcOptions.magic_net || ! usesVectorOutput) {
            int pushrate = f.getPushInt();
            f.setPush(pushrate * veclen);
        }

        int poprate = f.getPopInt();

        if (KjcOptions.magic_net || ! usesVectorInput) {
            int peekrate = f.getPeekInt();
            f.setPeek(peekrate + (veclen - 1) * poprate);
            f.setPop(poprate * veclen);
        }
        
        // X X X: Wretched abuse of a compiler flag that should
        // not occur with vectorization, for purposes of testing.
        if (KjcOptions.magic_net) {
            // alternative to vectorization for testing:
            workfn.setBody(new JBlock(new JStatement[]{at.dms.util.Utils.makeForLoop(workBody, veclen)}));
        } else {


        // fix number of pops for new rate.
        if (poprate > 0 && (KjcOptions.magic_net || ! usesVectorInput)) {
            List<JStatement> stmts = workBody.getStatements();
            int lastPos = stmts.size() - 1;
            JStatement last = stmts.get(lastPos);
            while (last instanceof SIRMarker) {
                lastPos--;
                last = stmts.get(lastPos);
            }
            if (last instanceof JExpressionStatement
                    && ((JExpressionStatement) last).getExpression() instanceof SIRPopExpression) {
                // final statement fixes number of pops: mung number.
                SIRPopExpression pop = (SIRPopExpression) ((JExpressionStatement) last)
                        .getExpression();
                pop.setNumPop(pop.getNumPop() + (veclen - 1) * poprate);
            } else {
                SIRPopExpression pop = new SIRPopExpression(f.getInputType(),
                        (veclen - 1) * poprate);
                JStatement popStatement = new JExpressionStatement(pop);
                workBody.addStatement(lastPos+1,popStatement);
            }
        }
        }
    }
    
    

    /** find location of filters in segments. */
    private static void findAllFiltersInSegments(Map<SIRStream,List<intPair>> segmentmap,
            Set<SIRFilter> all) {
        for (Map.Entry<SIRStream,List<intPair>> segments : segmentmap.entrySet()) {
            SIRStream stream = segments.getKey();
            if (segments.getValue() != null) {
                for (intPair range : segments.getValue()) {
                    allOfSegment(stream, range, all);
                }
            } else {
                allOfSegment(stream, null, all);

            }
        }
    }
    


    /** add all filter(s) in a segment to <b>filters</b> */
    private static void allOfSegment(SIRStream substr,intPair range, Set<SIRFilter> filters) {
        if (substr instanceof SIRFilter) {
            filters.add((SIRFilter)substr);
        } else if (substr instanceof SIRSplitJoin) {
            SIRSplitJoin sj = (SIRSplitJoin)substr;
            for (SIRStream subsub : sj.getParallelStreams()) {
                allOfSegment(subsub,null,filters);
            }
        } else if (substr instanceof SIRPipeline) {
            SIRPipeline p = (SIRPipeline)substr;
            if (range == null) {
                range = new intPair(0,p.size()-1);
            }
            for (int i = range.first; i <= range.second; i++) {
                allOfSegment(p.get(i),null,filters);
            }
        } else {
            throw new AssertionError("shouldn't get here");
        }
        
    }
    
    /** Given a set of vectorizable filters return all maximal subgraphs or segments of pipelines (herinafter called "segments")
     *  that are composed entirely of vectorizable filters.*/
    private static Map<SIRStream,List<intPair>> findVectorizablesegments(Set<SIRFilter> vectorizableFilters,SIRStream topStr) {
        Map<SIRStream,List<intPair>> segments = new HashMap<SIRStream,List<intPair>>();
        // walk top down and bottom up recording all maximal subgraphs or segments of pipelines that are
        // composed entirely of vectorizable filters.
        isVectorizableStream(topStr,vectorizableFilters,segments);
        return segments;
    }
    
    /**
     * Find vectorizable segments of a pipeline and add them to the map.
     * In process of doing this, it walks the abstract syntax of the SIRStream.
     * The resulting set contains only maximal subgraphs or pipeline segments of vectorizable filters.
     */
    private static void addVectorizablesegments(Set<SIRFilter> vectorizableFilters,SIRPipeline pipe, Map<SIRStream,List<intPair>> segments) {
        int startSeg = 0;
        Integer endSeg = null;
        int position = 0;
        for (SIRStream subStr : pipe.getSequentialStreams()) {
            boolean vectorizableStr = isVectorizableStream(subStr,vectorizableFilters,segments);
            if (vectorizableStr) {
                // vectorizable: continue old segment or start new one
                if (endSeg != null && endSeg == position - 1) {
                    endSeg = position;
                } else {
                    startSeg = position;
                    endSeg = position;
                }
            } else {
                addSegIfAny(pipe,startSeg,endSeg,segments);
                endSeg = null;
            }
            position++;
        }
        addSegIfAny(pipe,startSeg,endSeg,segments);
    }
    
    /**
     * Add possible segment of pipeline
     * @param pipe pipeline in which segment occurs
     * @param startSeg  int < endSeg
     * @param endSeg Integer, either null (no segment) or > startSeg
     * @param segments  map to which segment is added.
     */
    private static void addSegIfAny(SIRPipeline pipe,int startSeg, Integer endSeg, Map<SIRStream,List<intPair>> segments) {
        if (endSeg == null) {
            // null is indicator that caller did not have a segment
            return;
        }
        assert endSeg >= startSeg;
        intPair frag = new intPair(startSeg,endSeg);
        List<intPair> fragsForPipe = segments.get(pipe);
        if (fragsForPipe == null) {
            fragsForPipe = new LinkedList<intPair>();
        }
        fragsForPipe.add(frag);
        segments.put(pipe, fragsForPipe);
        
        // having put in a segment of pipeline, remove
        // its components from that segments map.
        for (int i = startSeg; i <= endSeg; i++) {
            SIRStream subStr = pipe.get(i);
            assert segments.containsKey(subStr);
            segments.remove(subStr);
        }
    }
    
    /** Check every substream to force creation of vectorizable segments before returning answer.
     * This is not just a predicate, it is also part of a top-down walk over the abstract syntax of the SIRStream */
    private static boolean isVectorizableStream(SIRStream str, Set<SIRFilter> vectorizableFilters, Map<SIRStream,List<intPair>> segments) {
        if (str instanceof SIRFilter) {
            // filter is vectorizable if passed data says that it is
            // (and if we descend to it)
            boolean vectorizable = vectorizableFilters.contains((SIRFilter)str);
            if (vectorizable) {
                segments.put(str, null);
            }
            return vectorizable;
        } else if (str instanceof SIRSplitJoin) {
            // SplitJoin is vectorizable if all branches are vectorizable.
            boolean vectorizable = true;
            for (SIRStream subStr : ((SIRSplitJoin)str).getParallelStreams()) {
                vectorizable &= isVectorizableStream(subStr, vectorizableFilters, segments);
            }
            if (vectorizable) {
                segments.put(str, null);
                for (SIRStream subStr : ((SIRSplitJoin)str).getParallelStreams()) {
                    assert segments.containsKey(subStr);
                    segments.remove(subStr);
                }
            }
            return vectorizable;
        } else if (str instanceof SIRPipeline) {
            // a bit messy, so pull out updating segments for <str>
            addVectorizablesegments(vectorizableFilters, (SIRPipeline)str, segments);
            List<intPair> segmentsForStr = segments.get((SIRPipeline)str);
            // Pipeline is vectorizable if it has a single vectorizable segment
            // that is the whole pipeline.
            boolean retval = segmentsForStr != null &&
            segmentsForStr.size() == 1 &&
            segmentsForStr.get(0).first == 0 &&
            segmentsForStr.get(0).second == ((SIRPipeline)str).size() - 1;
            return retval;
        } else {
            // Specifically: SIRFeedbackloop returns false
            // We do not want to vectorize inside a feedback loop
            // because of complexity of adjusting multiplicities of both branches.
            // Furthermore, we can not get fusion to fuse a feedback loop.
            return false;
        }
    }
    
    /** load up set of vectorizable filters */
    private static Set<SIRFilter> markVectorizableFilters(SIRStream str) {
        final Set<SIRFilter> vectorizableFilters = new HashSet<SIRFilter>();
        IterFactory.createFactory().createIter(str).accept(
                new EmptyStreamVisitor() {
                    /* visit a filter */
                    public void visitFilter(SIRFilter self,
                                            SIRFilterIter iter) {
                        if (Vectorizable.vectorizable(self)) {
                            vectorizableFilters.add(self);
                        }
                    }
                });
        return vectorizableFilters;
    }

    
    
    /** dump info about segments */
    private static void dumpStructure(Map<SIRStream,List<intPair>> segments) {
        for (SIRStream s : segments.keySet()) {
            System.err.print("Vectorizable ");
            if (s instanceof SIRPipeline) {
                List<intPair> pipeSegs = segments.get(s);
                if (pipeSegs.size() != 1 || pipeSegs.get(0).first != 0 || pipeSegs.get(0).second != ((SIRPipeline)s).size()) {
                    for (intPair pipeSeg : pipeSegs) {
                        System.err.print("pipeline " + s.getIdent() + "(" + s.getName() + ")" + " " + pipeSeg.first + ":" + pipeSeg.second + " {");
                        //dumpTopsAndBottoms(s,Collections.singletonList(pipeSeg));
                        for (int i = pipeSeg.first; i <= pipeSeg.second; i++) {
                            dumpContents(((SIRPipeline)s).get(i));
                        }
                    }
                } else {
                    System.err.print("pipeline " + s.getIdent() + "(" + s.getName() + ")" + " {");
                    //dumpTopsAndBottoms(s,pipeSegs);
                    for (int i = 0; i < ((SIRPipeline)s).size(); i++) {
                        dumpContents(((SIRPipeline)s).get(i));
                    }
                }
            } else if (s instanceof SIRSplitJoin) {
                System.err.print("splitjoin " + s.getIdent() + "(" + s.getName() + ")"+ " {");
                //dumpTopsAndBottoms(s,null);
                for (SIRStream subStr : ((SIRSplitJoin)s).getParallelStreams()) {
                    dumpContents(subStr);
                }
            } else if (s instanceof SIRFilter) {
                System.err.print("filter " + s.getIdent() + "(" + s.getName() + ")");
            } else {
                System.err.print("Unexpected type "+s.getName());
            }
            System.err.println("}");
        }
        
    }
    
    /** Iterator to dump names of all sub-pipelines / sub-splitjoins / filters in a stream graph */
    private static void dumpContents(final SIRStream s) {
        IterFactory.createFactory().createIter(s).accept(
                new EmptyStreamVisitor() {
                    public void preVisitPipeline(SIRPipeline self,
                            SIRPipelineIter iter) {
                        /*if (self != s)*/ {System.err.print(self.getName() + " ");}
                        super.preVisitPipeline(self,iter);
                    }
                    public void preVisitSplitJoin(SIRSplitJoin self,
                            SIRSplitJoinIter iter) {
                        /*if (self != s)*/ {System.err.print(self.getName() + " ");}
                        super.preVisitSplitJoin(self, iter);
                    }
                    public void visitFilter(SIRFilter self,
                            SIRFilterIter iter) {
                        /*if (self != s)*/ {System.err.print(self.getName() + " ");}
                        super.visitFilter(self, iter);
                    }
                });
    }
}

/** Little class for pairs of integers
 * odd name (no capitalization) because IntPair is already taken
 * but is (I think) in 3rd party code that we do not control. */
class intPair {
    public int first;
    public int second;
    intPair(int first, int second) {
      this.first = first;
      this.second = second;
    }        
    public int hashCode() {
        return first << 2 + second;
    }
    public boolean equals(Object obj) {
        return obj.getClass().equals(intPair.class) &&
        ((intPair)(obj)).first == first &&
        ((intPair)(obj)).second == second;
    }
    public String toString() {
        return "["+first+","+second+"]";
    }
}
