/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 * 
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose and without fee is hereby granted, provided
 * that the above copyright notice appear in all copies and that both that
 * copyright notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in advertising or
 * publicity pertaining to distribution of the software without specific,
 * written prior permission. M.I.T. makes no representations about the
 * suitability of this software for any purpose. It is provided "as is" without
 * express or implied warranty.
 */

package streamit.scheduler2.constrained;

import streamit.scheduler2.iriter./* persistent. */
FilterIter;
import streamit.scheduler2.iriter./* persistent. */
Iterator;

import streamit.scheduler2.Schedule;
import streamit.scheduler2.hierarchical.PhasingSchedule;

public class Filter
    extends streamit.scheduler2.hierarchical.Filter
    implements StreamInterface
{
    final private LatencyGraph graph;

    private LatencyNode latencyNode;

    PhasingSchedule initWorkPhases[];
    PhasingSchedule steadyWorkPhases[];

    int nCurrentPhase = 0;

    public Filter(
        FilterIter filterIter,
        Iterator parent,
        StreamFactory factory)
    {
        super(filterIter);

        graph = factory.getLatencyGraph();

        initWorkPhases = new PhasingSchedule[filterIter.getNumInitStages()];
        steadyWorkPhases =
            new PhasingSchedule[filterIter.getNumWorkPhases()];

        for (int n = 0; n < filterIter.getNumInitStages(); n++)
        {
            initWorkPhases[n] =
                new PhasingSchedule(
                    this,
                    new Schedule(
                        filterIter.getInitFunctionStage(n),
                        filterIter.getUnspecializedIter()),
                    filterIter.getInitPeekStage(n),
                    filterIter.getInitPopStage(n),
                    filterIter.getInitPushStage(n));

        }

        for (int n = 0; n < filterIter.getNumWorkPhases(); n++)
        {
            steadyWorkPhases[n] =
                new PhasingSchedule(
                    this,
                    new Schedule(
                        filterIter.getWorkFunctionPhase(n),
                        filterIter.getUnspecializedIter()),
                    filterIter.getPeekPhase(n),
                    filterIter.getPopPhase(n),
                    filterIter.getPushPhase(n));
        }

    }

    public StreamInterface getTopConstrainedStream()
    {
        return this;
    }

    public StreamInterface getBottomConstrainedStream()
    {
        return this;
    }

    public void initiateConstrained()
    {
        latencyNode = graph.addFilter(this);

        // create a schedule for this
    }

    public void computeSchedule()
    {
        ERROR("Should not be used for Filters in Constrained Scheduling!");
    }

    public LatencyNode getBottomLatencyNode()
    {
        return latencyNode;
    }

    public LatencyNode getTopLatencyNode()
    {
        return latencyNode;
    }

    public LatencyNode getLatencyNode()
    {
        return latencyNode;
    }

    PhasingSchedule getPhaseSchedule(int nPhase)
    {
        if (nPhase < this.getNumDeclaredInitPhases())
        {
            return initWorkPhases[nPhase];
        }
        else
        {
            return steadyWorkPhases[(
                nPhase - this.getNumDeclaredInitPhases())
                % this.getNumDeclaredSteadyPhases()];
        }
    }

    public PhasingSchedule getNextPhase(
        Restrictions restrictions,
        int nDataAvailable)
    {
        PhasingSchedule phase = new PhasingSchedule(this);

        Restriction blockingRestriction =
            restrictions.getBlockingRestriction(this);
        int nAllowedPhases = blockingRestriction.getNumAllowedExecutions();

        // BUGBUG this can DEFINITELY be a LOT more efficient!
        int nExecutions = 0;
        while (nAllowedPhases > nExecutions)
        {
            PhasingSchedule schedPhase = getPhaseSchedule(nCurrentPhase);
            if (schedPhase.getOverallPeek() > nDataAvailable)
                break;

            phase.appendPhase(schedPhase);
            nDataAvailable -= schedPhase.getOverallPop();
            nCurrentPhase++;
            nExecutions++;
        }

        int executed = restrictions.execute(this, nExecutions);
        ASSERT (executed == nExecutions);
        
        ERROR("not tested");
        return phase;
    }
    
    public boolean isDoneInitializing ()
    {
        return nCurrentPhase >= getNumInitStages();
    }


    public void initRestrictionsCompleted(P2PPortal portal)
    {
        ERROR ("not implemented!");
    }
    
    public void initializeRestrictions(Restrictions _restrictions)
    {
        
    }
}
