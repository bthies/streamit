// $Id$
/**
 * 
 */
package at.dms.kjc.sir.lowering;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.lowering.fusion.FusePipelines;
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

        if (debugging) debugSelection(str); // debugging
        
        //str = FusePipelines.fusePipelinesOfVectorizableFilters(str);
        str = FusePipelines.fusePipelinesOfVectorizableStreams(str);

        if (debugging) debugSelection(str); // debugging
        
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
                                Vectorize.vectorize(self);
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
    
    
    /**
     * Debug printout of sequences of vectorizable filters.
     * @param str
     */
    static private void debugSelection (SIRStream str) {
        // Check for vectorizable, but don't dump Vectorizable debugging info again if enabled.
        boolean vdebug = Vectorizable.debugging;
        Vectorizable.debugging = debugVectorizable;
        final Set<SIRFilter> vectorizable = Vectorizable.vectorizableStr(str); 
        Vectorizable.debugging = vdebug;
        
        // debugging code: tell all series of vectorizable filters.
        // easier since after lift...Sync, which removes unnecessary pipelines.
        final List<List<SIRFilter>> allFilterLists = new LinkedList<List<SIRFilter>>();

        IterFactory.createFactory().createIter(str).accept(
            new EmptyStreamVisitor() {
                /* pre-visit a pipeline */
                public void preVisitPipeline(SIRPipeline self,
                                                 SIRPipelineIter iter) {
                    List<SIRFilter> currentList = null;
                    List<SIRStream> children = self.getSequentialStreams();
                        for (SIRStream child : children) {
                                if (! (child instanceof SIRFilter) || ! vectorizable.contains(child)) {
                                    if (currentList != null) {
                                        allFilterLists.add(currentList);
                                        currentList = null;
                                    }
                                } else {
                                    if (currentList == null) {
                                        currentList = new LinkedList<SIRFilter>();
                                    }
                                    currentList.add((SIRFilter)child);
                                }
                            }
                            if (currentList != null) {
                                allFilterLists.add(currentList);
                            }
                            super.preVisitPipeline(self, iter);
                    }
                });
            System.err.println("Sequences of Vectorizable filters:");
            for (List<SIRFilter> filterList : allFilterLists) {
                for (SIRFilter s : filterList) {
                    System.err.println(" " + s.getName() + (s.getPeekInt() <= s.getPopInt()? "" : (" peeks " + s.getPeekInt())));
                }
                System.err.println();
            }
        }

}
