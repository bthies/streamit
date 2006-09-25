
package at.dms.kjc.cluster;

import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import java.util.Iterator;

/**
 * Looks like printing some debugging info about SDEP schedules.
 */

public class DoSchedules {

    private streamit.scheduler2.iriter.Iterator firstNode;
    private streamit.scheduler2.iriter.Iterator selfIter;

    public static void findSchedules(streamit.scheduler2.iriter.Iterator selfIter, streamit.scheduler2.iriter.Iterator firstNode, SIRStream stream) {
        DoSchedules tmp = new DoSchedules(selfIter, firstNode);
        tmp.recurseStream(stream);
    }

    DoSchedules(streamit.scheduler2.iriter.Iterator selfIter, streamit.scheduler2.iriter.Iterator firstNode) {
        this.selfIter = selfIter;
        this.firstNode = firstNode;
    }

    public void calcNode(streamit.scheduler2.iriter.Iterator nodeIter, SIROperator str) {
        
        if (nodeIter.equals(firstNode)) return;
    
        if (ClusterBackend.debugPrint)
            System.err.println("DoSchedule Visiting node: "+str);
    
        try {

            streamit.scheduler2.constrained.Scheduler scheduler =
                streamit.scheduler2.constrained.Scheduler.createForSDEP(selfIter);

            streamit.scheduler2.SDEPData sdep = scheduler.computeSDEP(firstNode, nodeIter);
            if (ClusterBackend.debugPrint) {
                System.err.println("  Source Init & Steady Phases: "+sdep.getNumSrcInitPhases()+", "+sdep.getNumSrcSteadyPhases());
                System.err.println("  Destn. Init & Steady Phases: "+sdep.getNumDstInitPhases()+", "+sdep.getNumDstSteadyPhases());
            }
        
        } catch (Exception ex) {
            System.err.println("Comstrained Scheduler / SDEP exception: "+ex);
            ex.printStackTrace();
        }
 
    }

    public void recurseStream(SIROperator str) {

        if (str instanceof SIRFilter) {
            SIRFilter filter = (SIRFilter)str;

            calcNode(IterFactory.createFactory().createIter(filter), str);
        }

        /*
          if (str instanceof SIRPhasedFilter) {
          SIRPhasedFilter filter = (SIRPhasedFilter)str;

          streamit.scheduler2.iriter.Iterator lastIter = 
          IterFactory.createFactory().createIter(filter);   
        
          calcNode(lastIter, str);
          }
        */

        if (str instanceof SIRFeedbackLoop)
            {
                SIRFeedbackLoop fl = (SIRFeedbackLoop)str;
                recurseStream(fl.getSplitter());
                recurseStream(fl.getJoiner());
                recurseStream(fl.getBody());
                recurseStream(fl.getLoop());
            }
        if (str instanceof SIRPipeline)
            {
                SIRPipeline pl = (SIRPipeline)str;
                Iterator iter = pl.getChildren().iterator();
                while (iter.hasNext())
                    {
                        SIRStream child = (SIRStream)iter.next();
                        recurseStream(child);
                    }
            }
        if (str instanceof SIRSplitJoin)
            {
                SIRSplitJoin sj = (SIRSplitJoin)str;            
                recurseStream(sj.getSplitter());
                recurseStream(sj.getJoiner());
                Iterator<SIRStream> iter = sj.getParallelStreams().iterator();
                while (iter.hasNext())
                    {
                        SIRStream child = iter.next();
                        recurseStream(child);
                    }
            }
    }
}


