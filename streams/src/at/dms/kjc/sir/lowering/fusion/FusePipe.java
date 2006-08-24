package at.dms.kjc.sir.lowering.fusion;

import at.dms.util.IRPrinter;
import at.dms.util.Utils;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.lir.*;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * This is the high-level fusion class for pipelines.  It dispatches
 * to another class (e.g., FusePipeShift, FusePipeModulo) for actually
 * performing the fusion.  This class deals with the interface, the
 * legality of fusion, and patching the fusion product into the parent
 * stream graph.
 */
public class FusePipe {
    /**
     * Name of init work, fused across fusion and fission.
     */
    public static final String INIT_WORK_NAME = "___initWork";

    /**
     * Fuses all eligibles portions of <pipe>, returning the number of
     * filters eliminated.
     */
    public static int fuse(SIRPipeline pipe) {
        return internalFuse(pipe, pipe.size(), pipe.size());
    }

    /**
     * Fuses all candidate portions of <pipe>, given that the caller
     * would prefer not to eliminate more than <targetElim> filters
     * from <pipe>.  The fusion does its best to respect this
     * constraint (in a greedy way for now), but it will error on the
     * side of eliminating MORE than <targetElim> instead of
     * eliminating fewer.
     */
    public static int fuse(SIRPipeline pipe, int targetElim) {
        // get num that are fusable
        int numElim = getNumElim(pipe);
        int maxLength;
        if (targetElim >= numElim) {
            maxLength = pipe.size();
        } else {
            maxLength = (int)Math.ceil(((float)numElim)/((float)(numElim-targetElim)));
            //System.err.println("numElim = " + numElim + " targetElim=" + targetElim + " maxLength=" + maxLength);
        }
        return internalFuse(pipe, maxLength, targetElim);
    }

    /**
     * Fuses sections of <pipe> according to <partitions>, which
     * specifies the grouping sequence of who gets fused together.
     */
    public static SIRStream fuse(SIRPipeline pipe, PartitionGroup partitions) {
        // make a new pipeline having the proper partition
        // arrangement, and replace all current children of <pipe>
        // with this <newPipe>.  This is because we're trying to just
        // mutate <pipe> instead of returning a new one.
        SIRPipeline newPipe = RefactorPipeline.addHierarchicalChildren(pipe, partitions);
        newPipe.setInit(SIRStream.makeEmptyInit());
        pipe.replace(pipe.get(0), pipe.get(pipe.size()-1), newPipe);
        SIRStream out=null;
        for (int i=0; i<partitions.size(); i++) {
            if (partitions.get(i)!=1) {
                SIRPipeline child = (SIRPipeline)newPipe.get(i);
                internalFuse(child);
                out=child.get(0);
                Lifter.eliminatePipe(child);
            }
        }
        Lifter.eliminatePipe(newPipe);
        return out;
    }
    
    /**
     * Fuses two filters starting from <start> in <pipe>, returning 1
     * if they were fused and 0 otherwise.
     */ 
    public static int fuseTwo(SIRPipeline pipe, int start) {
        if (isFusable(pipe.get(start)) &&
            isFusable(pipe.get(start+1))) {
            fuse((SIRFilter)pipe.get(start),
                 (SIRFilter)pipe.get(start+1));
            return 1;
        } else {
            return 0;
        }
    }

    /*
     * Fuses filters <first> ... <last>.  For now, assumes: 
     *
     * 1. all of <first> ... <last> are consecutive filters in their
     *     parent, which must be an SIRPipeline
     *
     */
    public static void fuse(SIRFilter first,
                            SIRFilter last) {
        SIRPipeline parent = (SIRPipeline)first.getParent();
        // fuse the filters...
        int firstIndex = parent.indexOf(first);
        int lastIndex = parent.indexOf(last);
        //System.err.println("Fusing " + (lastIndex-firstIndex+1) + " Pipeline filters.");
        if (firstIndex==0 && lastIndex==parent.size()-1) {
            // optimization: if we're fusing an entire pipeline, just
            // call directly
            internalFuse(parent);
        } else {
            // otherwise, create a sub-pipeline to contain first..last.
            SIRPipeline newPipe = new SIRPipeline(parent, parent.getIdent() + "_Cont");
            for (int i=lastIndex; i>=firstIndex; i--) {
                newPipe.add(0, parent.get(i), parent.getParams(i));
                parent.remove(i);
            }
            parent.add(firstIndex, newPipe);
            internalFuse(newPipe);
            Lifter.eliminatePipe(newPipe);
        }
    }

    /**
     * Fuses all candidate portions of <pipe>, but only fusing in
     * segments of length <maxLength>, eliminating a maximum of
     * <maxElim>.  Candidates for fusion are sequences of filters that
     * do not have special, compiler-defined work functions.  Return
     * how many filters were ELIMINATED from this pipeline.
     */
    private static int internalFuse(SIRPipeline pipe, int maxLength, int maxElim) {
        int numEliminated = 0;
        int start = 0;
        do {
            // find start of candidate stretch for fusion
            while (start < pipe.size()-1 && !isFusable(pipe.get(start))) {
                start++;
            }
            // find end of candidate stretch for fusion
            int end = start;
            while ((end+1) < pipe.size() && isFusable(pipe.get(end+1))
                   && (end-start+1<maxLength)) {
                end++;
            }
            // if we found anything to fuse
            if (end > start) {
                assert pipe.get(start).getParent()==pipe:
                    "This stream (" + pipe.get(start) +
                    " thinks its parent is " + pipe.get(start).getParent() + 
                    " even though it should be " + pipe;
                assert pipe.get(start).getParent()==pipe;
                fuse((SIRFilter)pipe.get(start),
                     (SIRFilter)pipe.get(end));
                numEliminated += end-start;
                start = start + 1;
            } else {
                start = end + 1;
            }
        } while (start < pipe.size()-1 && numEliminated<maxElim);
        return numEliminated;
    }

    /**
     * The fundamental fuse operation.
     *
     * Fuses ALL children of pipe, requiring that they are fusable
     * filters.  Leaves just a single filter <f>, which will be
     * <pipe>'s only child following this call.
     */
    private static void internalFuse(SIRPipeline pipe) {
        // if we have just one filter, do nothing
        if (pipe.size()==1) {
            return;
        } 

        // check that all the filters are fusable
        for (int i=0; i<pipe.size(); i++) {
            assert isFusable(pipe.get(i)):
                "Trying to fuse a filter that is unfusable: " + 
                pipe.get(i) + " " + pipe.get(i).getName();
        }
    
    
        if (!KjcOptions.modfusion) {

            //if no modulation just call the shift pipeline fusion

            // rename filter contents
            for (int i=0; i<pipe.size(); i++) {
                RenameAll.renameFilterContents((SIRFilter)pipe.get(i));
            }
        
            // dispatch to the actual fuser that we use
            ShiftPipelineFusion.doFusion(pipe);

        } else {

            //if modulation is enabled use shift pipeline fusion to
            //fuse segments where no filter is peeking, and only
            //use modilo pipeline fusion on a pair of filters where
            //second filter is peeking

            int n = pipe.size();

            //System.out.println("Internal fuse! size: "+n);

            for (int i = 1; i < n; i++) {

                SIRFilter f = (SIRFilter)pipe.get(i);
                if (f.getPeekInt() > f.getPopInt()) {

                    //System.out.println("Found A Peeking Filter! at: "+i);

                    // recursively:
                    // 1. first fuse rest of the pipeline 
                    // 2. then the begininig of the pipeline
                    fuse((SIRFilter)pipe.get(i), (SIRFilter)pipe.get(n-1)); 
                    fuse((SIRFilter)pipe.get(0), (SIRFilter)pipe.get(i-1));

                    //rename filter contents
                    for (int y=0; y<pipe.size(); y++) {
                        RenameAll.renameFilterContents((SIRFilter)pipe.get(y));
                    }
            
                    //dispatch modulo fusion to fuse a pair of filters
                    ModuloPipelineFusion.doFusion(pipe);
                    return;
                }
            }

            //System.out.println("Found No Peeking Filter!");

            //no peeking in the pipeline segment fuse using shift fusion!
        
            // rename filter contents
            for (int y=0; y<pipe.size(); y++) {
                RenameAll.renameFilterContents((SIRFilter)pipe.get(y));
            }
        
            // dispatch to the actual fuser that we use
            ShiftPipelineFusion.doFusion(pipe);
        }
    }

    /**
     * Returns how many filters in this can be eliminated in a fusion
     * operation.
     */
    private static int getNumElim(SIRPipeline pipe) {
        int numEliminated = 0;
        int start = 0;
        do {
            // find start of candidate stretch for fusion
            while (start < pipe.size()-1 && !isFusable(pipe.get(start))) {
                start++;
            }
            // find end of candidate stretch for fusion
            int end = start;
            while ((end+1) < pipe.size() && isFusable(pipe.get(end+1))) {
                end++;
            }
            // if we found anything to fuse
            if (end > start) {
                numEliminated += end-start;
            }
            start = end + 1;
        } while (start < pipe.size()-1);
        return numEliminated;
    }

    /**
     * Returns whether or note <str> is a candidate component for
     * fusion.
     */
    public static boolean isFusable(SIRStream str) {
        // must be a filter to fuse
        if (!(str instanceof SIRFilter)) {
            return false;
        }
        SIRFilter filter = (SIRFilter)str;

        // special case: identity filters are fusable (even though
        // they fail some of the later tests, like having a work function)
        if (filter instanceof SIRIdentity) {
            return true;
        }

        // must have a work function
        if (filter.getWork()==null) {
            return false;
        }

        // must have static rates
        if (filter.getPeek().isDynamic() ||
            filter.getPop().isDynamic() ||
            filter.getPush().isDynamic()) {
            return false;
        }

        // don't fuse message senders or receivers because message
        // timing breaks down after fusion (need standalone filter for
        // proper SDEP and message delivery in cluster backend)
        SIRPortal[] receivesFrom = SIRPortal.getPortalsWithReceiver(filter);
        SIRPortal[] sendsTo = SIRPortal.getPortalsWithSender(filter);
        if (receivesFrom.length>=1 || sendsTo.length>=1) {
            return false;
        }

        // don't fuse file readers or file writers
        if (filter instanceof SIRFileReader || filter instanceof SIRFileWriter) {
            return false;
        }

        // otherwise fusable
        return true;
    }

    /**
     * In <parent>, replace all children with <fused>, and add
     * arguments <initArgs> to call to <fused>'s init function.
     */
    static void replace(SIRPipeline parent, 
                        SIRFilter fused,
                        List initArgs) {
        // replace <filterList> with <fused>
        parent.replace(parent.get(0), parent.get(parent.size()-1), fused);
        // add args to <fused>'s init
        parent.setParams(parent.indexOf(fused), initArgs);
    }

    /**
     * Return a name for the fused filter that consists of those
     * filters in <filterInfo>.  <filterInfo> must be a list of either
     * SIRFilter's or FilterInfo's.
     */
    public static String getFusedName(List filterInfo) {
        StringBuffer name = new StringBuffer("Fused");
        for (ListIterator it = filterInfo.listIterator(); it.hasNext(); ) {
            name.append("_");
            Object o = it.next();
            String childName = null;
            if (o instanceof SIRFilter) {
                childName = ((SIRFilter)o).getIdent();
            } else {
                throw new RuntimeException("Unexpected type: " + o.getClass());
            }
            if (childName.toLowerCase().startsWith("fused_")) {
                childName = childName.substring(6, childName.length());
            }
            name.append(childName.substring(0, Math.min(childName.length(), 3)));
        }
        return name.toString();
    }
}
