/**
 * 
 */
package at.dms.kjc.spacetime;

import java.util.Iterator;

import at.dms.kjc.sir.SIRFeedbackLoop;
import at.dms.kjc.sir.SIRFilter;
import at.dms.kjc.sir.SIRPipeline;
import at.dms.kjc.sir.SIRSplitJoin;
import at.dms.kjc.sir.SIRStream;
import at.dms.kjc.sir.lowering.fission.StatelessDuplicate;
import at.dms.kjc.sir.lowering.partition.WorkEstimate;
import java.util.*;

/**
 * @author mgordon
 *
 */
public class DuplicateBottleneck {
    
    private Vector<Integer> sortedWorkEsts;
    private Vector<SIRFilter> sortedFilters;
    
    public DuplicateBottleneck() {
        sortedWorkEsts = new Vector<Integer>();
        sortedFilters = new Vector<SIRFilter>();
    }
    
    public void duplicate(SIRStream str) {
        //keep duplicating until we cannot anymore!!
        //duplicateBottleneck(str);
        while (duplicateBottleneck(str));
    }
    
    private boolean duplicateBottleneck(SIRStream str) {
        //get the work estimate
        WorkEstimate work = WorkEstimate.getWorkEstimate(str);
        //find the ordering of filters
        walkSTR(str, work);
        //return false if we cannot duplicate the bottleneck
        if (!StatelessDuplicate.isFissable(sortedFilters.get(0)))
            return false;
        
        for (int i = 0; i < sortedWorkEsts.size(); i++) {
            System.out.println(sortedFilters.get(i) + " = " + sortedWorkEsts.get(i));
        }
        
        //so the bottleneck if stateless, duplicate it as many times so 
        //that it is not the bottleneck anymore!
        
        int reps = (int)Math.round(0.5 + ((double)sortedWorkEsts.get(0)) / ((double)sortedWorkEsts.get(1)));
        
        StatelessDuplicate.doit(sortedFilters.get(0), reps);
        
        System.out.println(reps);
        
        //might be good for another round?
        return true;
    }
    
    private void walkSTR(SIRStream str, WorkEstimate work) {
        if (str instanceof SIRFeedbackLoop) {
            SIRFeedbackLoop fl = (SIRFeedbackLoop) str;
            walkSTR(fl.getBody(), work);
            walkSTR(fl.getLoop(), work);
        }
        if (str instanceof SIRPipeline) {
            SIRPipeline pl = (SIRPipeline) str;
            Iterator iter = pl.getChildren().iterator();
            while (iter.hasNext()) {
                SIRStream child = (SIRStream) iter.next();
                walkSTR(child, work);
            }
        }
        if (str instanceof SIRSplitJoin) {
            SIRSplitJoin sj = (SIRSplitJoin) str;
            Iterator iter = sj.getParallelStreams().iterator();
            while (iter.hasNext()) {
                SIRStream child = (SIRStream) iter.next();
                walkSTR(child, work);
            }
        }
        if (str instanceof SIRFilter) {
            SIRFilter filter = (SIRFilter)str; 
            int i;
            int workEst = work.getWork(filter);
            //find the right place to add this to
            for (i = 0; i < sortedFilters.size(); i++) {
                if (workEst > sortedWorkEsts.get(i).intValue())
                    break;
            }
            sortedFilters.add(i, filter);
            sortedWorkEsts.add(i, workEst);
        }
    } 
}
