// $Id$
/**
 * 
 */
package at.dms.kjc.sir.lowering;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.lowering.fusion.FusePipelines;
import at.dms.kjc.sir.lowering.fusion.Lifter;

import java.util.*;

/**
 * Mung code to allow naive vectorization.
 * @author Allyn Dimock
 */
public class VectorizeEnable {
    /**
     * Set to true to list sequences of vectorizable filters before fusion and individual vectorizable filters after fusion.
     */
    public static boolean debugging = false;
    /**
     * Set to true to have Vectorizable print out reasons for not vectorizing a filter.
     */
    public static boolean debugVectorizable = false;

    /**
     * Perform naive vectorization on eligible filters in a stream.
     * <br/>
     * Causes sequences of {@link Vectorizable#vectorizable(SIRFilter) vectorizable} filters to be fused.
     * Causes {@link Vectorizable#vectorizable(SIRFilter) vectorizable} filters to be executed a multiple of 4 times per steady state.
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
     * @return the munged Stream (as a convenience).
     */
    public static SIRStream vectorizeEnable(SIRStream str,
            Map<SIROperator,Integer> partitionTable) {
        if (KjcOptions.vectorize < 8) {
            // can not vectorize ints and floats unless
            // vector registers are at least 8 bytes.
            return str;
        }

        // fancy fusing and selection routine only implemented as debug dump so far...
        if (debugging) {
            System.err.println("Vectorizable subgraphs before fusion");
            vectorizeEnable2(str);
        }
//        if (debugging) debugSelection(str); // debugging
        
        //str = FusePipelines.fusePipelinesOfVectorizableFilters(str);
        str = FusePipelines.fusePipelinesOfVectorizableStreams(str);

        // fancy fusing and selection routine only implemented as debug dump so far...
        // call again after fusion to make sure we got everything.
        if (debugging) {
            System.err.println("Vectorizable subgraphs after fusion");
            vectorizeEnable2(str);
        }
//        if (debugging) debugSelection(str); // debugging
        
        IterFactory.createFactory().createIter(str).accept(
                new EmptyStreamVisitor() {
                    /* visit a filter */
                    public void visitFilter(SIRFilter self,
                                            SIRFilterIter iter) {
                        if (Vectorizable.vectorizable(self) &&
                            Vectorizable.isUseful(self)) {
//                            // X X X: Wretched abuse of a compiler flag that should
//                            // not occur with vectorization, for purposes of testing.
//                            if (! KjcOptions.magic_net) {
                                if (debugging) {
                                    System.err.println("Vectorizing " + self.getName());
                                }
                                Vectorize.vectorize(self,false,false);
//                            }
                            forScheduling(self);
                        }
                    }
                });
        return str;
    }
    
    /**
     * Unconditionally mung filter for naive vectorization.
     * <br/>
     * This method only takes care of interaction with the scheduler.
     * {@link Vectorize#vectorize(SIRFilter)} fixes types and
     * {@link SimplifyPeekPopPush} must previously have been run.
     * Set rates as follows:
     * <ul><li>  push' = push * 4
     * </li><li> peek' = peek + 3 * pop
     * </li><li> pop'  = pop * 4 
     * </li></ul>
     * @param f : filter to be munged.
     */
    private static void forScheduling (SIRFilter f) {
        int veclen = KjcOptions.vectorize / 4;
        
        JMethodDeclaration workfn = f.getWork();
        JBlock workBody = workfn.getBody();

        // adjust rates.
        int pushrate = f.getPushInt();
        int poprate = f.getPopInt();
        int peekrate = f.getPeekInt();
        f.setPush(pushrate * veclen);
        f.setPeek(peekrate + (veclen - 1) * poprate);
        f.setPop(poprate * veclen);

//        // X X X: Wretched abuse of a compiler flag that should
//        // not occur with vectorization, for purposes of testing.
//        if (KjcOptions.magic_net) {
//            // alternative to vectorization for testing:
//            workfn.setBody(new JBlock(new JStatement[]{at.dms.util.Utils.makeForLoop(workBody, veclen)}));
//        } else {


        // fix number of pops for new rate.
        if (poprate > 0) {
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
//        }
    }
    
    
//    /**
//     * Debug printout of sequences of vectorizable filters.
//     * @param str
//     */
//    static private void debugSelection (SIRStream str) {
//        // Check for vectorizable, but don't dump Vectorizable debugging info again if enabled.
//        boolean vdebug = Vectorizable.debugging;
//        Vectorizable.debugging = debugVectorizable;
//        final Set<SIRFilter> vectorizable = Vectorizable.vectorizableStr(str); 
//        Vectorizable.debugging = vdebug;
//        
//        // debugging code: tell all series of vectorizable filters.
//        // easier since after lift...Sync, which removes unnecessary pipelines.
//        final List<List<SIRFilter>> allFilterLists = new LinkedList<List<SIRFilter>>();
//
//        IterFactory.createFactory().createIter(str).accept(
//            new EmptyStreamVisitor() {
//                /* pre-visit a pipeline */
//                public void preVisitPipeline(SIRPipeline self,
//                                                 SIRPipelineIter iter) {
//                    List<SIRFilter> currentList = null;
//                    List<SIRStream> children = self.getSequentialStreams();
//                        for (SIRStream child : children) {
//                                if (! (child instanceof SIRFilter) || ! vectorizable.contains(child)) {
//                                    if (currentList != null) {
//                                        allFilterLists.add(currentList);
//                                        currentList = null;
//                                    }
//                                } else {
//                                    if (currentList == null) {
//                                        currentList = new LinkedList<SIRFilter>();
//                                    }
//                                    currentList.add((SIRFilter)child);
//                                }
//                            }
//                            if (currentList != null) {
//                                allFilterLists.add(currentList);
//                            }
//                            super.preVisitPipeline(self, iter);
//                    }
//                });
//            System.err.println("Sequences of Vectorizable filters:");
//            for (List<SIRFilter> filterList : allFilterLists) {
//                for (SIRFilter s : filterList) {
//                    System.err.println(" " + s.getName() + (s.getPeekInt() <= s.getPopInt()? "" : (" peeks " + s.getPeekInt())));
//                }
//                System.err.println();
//            }
//        }

    
    /** Try again without relying on fusion to detect subgraphs.
     * (But allow fusion to fuse subgraphs).
     * The subgraphs found here may not become fused because of peeking.
     * However, (1) TODO: after fusion any subgraphs found here can be connected by passing
     * vectors over channels.
     * (2) TODO: If the top level is a splitjoin and first fiter in splitjoin peeks then
     * may want to put conversion from scalar to vector before splitter -- saved duplication
     * of gather operations if peek ranges of silters below splitter overlay. 
     * XXX: this is not yet complete and is used only as a fancier debugging dump.
     * @param str stream to process: vectorize vectorizable portions 
     * @return modified str (str replaced with modified version in parent if any)
     */
    public static SIRStream vectorizeEnable2(SIRStream str) {
        if (KjcOptions.vectorize < 8) {
            // can not vectorize ints and floats unless
            // vector registers are at least 8 bytes.
            return str;
        }
        // find all vectorizable filters
        Set<SIRFilter> vectorizableFilters = markVectorizableFilters(str);
        // find maximal subgraphs or pipeline segments that are not in feedback loops.
        Map<SIRStream,List<intPair>> segments = findVectorizablesegments(vectorizableFilters,str);
        
        // put cleverness here to try to determine what to fuse:
        // Can not always fuse whole segment in case of peeking, but can determine whether
        // vectors could be carried over tapes between SIROperators.
        // Can also determine the utility of vectorizing a segment.
        
        for (SIRStream s : segments.keySet()) {
            System.err.print("Vectorizable ");
            if (s instanceof SIRPipeline) {
                List<intPair> pipeSegs = segments.get(s);
                if (pipeSegs.size() != 1 || pipeSegs.get(0).first != 0 || pipeSegs.get(0).second != ((SIRPipeline)s).size()) {
                    for (intPair pipeSeg : pipeSegs) {
                        System.err.print("pipeline " + s.getIdent() + "(" + s.getName() + ")" + " " + pipeSeg.first + ":" + pipeSeg.second + " {");
                        for (int i = pipeSeg.first; i <= pipeSeg.second; i++) {
                            dumpContents(((SIRPipeline)s).get(i));
                        }
                    }
                } else {
                    System.err.print("pipeline " + s.getIdent() + "(" + s.getName() + ")" + " {");
                    for (int i = 0; i < ((SIRPipeline)s).size(); i++) {
                        dumpContents(((SIRPipeline)s).get(i));
                    }
                }
            } else if (s instanceof SIRSplitJoin) {
                System.err.print("splitjoin " + s.getIdent() + "(" + s.getName() + ")"+ " {");
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
        
        return str;
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
    
    /** Given a set of vectorizable filters return all maximal subgraphs or segments of pipelines that are
     * composed entirely of vectorizable filters.*/
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
