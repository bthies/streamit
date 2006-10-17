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
 * <br/> $Id$
 * @author Allyn Dimock
 */
public class VectorizeEnable {
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
//        debugSelection(str); // debugging
        
        str = FusePipelines.fusePipelinesOfVectorizableFilters(str);

//        debugSelection(str); // debugging
        
        IterFactory.createFactory().createIter(str).accept(
                new EmptyStreamVisitor() {
                    /* visit a filter */
                    public void visitFilter(SIRFilter self,
                                            SIRFilterIter iter) {
                        if (Vectorizable.vectorizable(self)) {
                            vectorizeEnableFilter(self);
                            Vectorize.vectorize(self);
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
     * Wrap body of work function in "for (int i = 0; i < 4; i++)"
     * @param f : filter to be munged.
     */
    private static void vectorizeEnableFilter (SIRFilter f) {
        int veclen = 4;
        
        int pushrate = f.getPushInt();
        int poprate = f.getPopInt();
        int peekrate = f.getPeekInt();
        JMethodDeclaration workfn = f.getWork();
        JBlock workBody = workfn.getBody();
        workfn.setBody(new JBlock(new JStatement[]{at.dms.util.Utils.makeForLoop(workBody, veclen)}));
        f.setPush(pushrate * veclen);
        f.setPeek(peekrate + (veclen - 1) * poprate);
        f.setPop(poprate * veclen);
    }
    
    
    /**
     * Debug printout of sequences of vectorizable filters.
     * @param str
     */
    static private void debugSelection (SIRStream str) {
        final Set<SIRFilter> vectorizable = Vectorizable.vectorizableStr(str); 
        
        // debugging code: tell all series of vectorizable filters.
        // easier since after lift...Sync, which removes unnecessary pipelines.
        final List<List<String>> allFilterLists = new LinkedList<List<String>>();

        IterFactory.createFactory().createIter(str).accept(
            new EmptyStreamVisitor() {
                /* pre-visit a pipeline */
                public void preVisitPipeline(SIRPipeline self,
                                                 SIRPipelineIter iter) {
                    List<String> currentList = null;
                    List<SIRStream> children = self.getSequentialStreams();
                        for (SIRStream child : children) {
                                if (! (child instanceof SIRFilter) || ! vectorizable.contains(child)) {
                                    if (currentList != null) {
                                        allFilterLists.add(currentList);
                                        currentList = null;
                                    }
                                } else {
                                    if (currentList == null) {
                                        currentList = new LinkedList<String>();
                                    }
                                    currentList.add(child./*getIdent()*/getName());
                                }
                            }
                            if (currentList != null) {
                                allFilterLists.add(currentList);
                            }
                            super.preVisitPipeline(self, iter);
                    }
                });
            System.err.println("Sequences of Vectorizable filters:");
            for (List<String> filterList : allFilterLists) {
                for (String s : filterList) {
                    System.err.println(" " + s);
                }
                System.err.println();
            }
        }

}
