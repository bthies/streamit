/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

package streamit.scheduler2.constrained;

import streamit.misc.DLList;
import streamit.misc.DLList_const;
import streamit.misc.DLListIterator;
import streamit.scheduler2.hierarchical.StreamInterfaceWithSnJ;

public class LatencyNode extends streamit.misc.AssertedClass
{
    /*
     * These are lists of LatencyEdges
     */
    final DLList dependsOn = new DLList();
    final DLList dependants = new DLList();

    /*
     * This is a list of StreamInterfaces, root is first, parent is last
     */
    final DLList ancestors;

    /*
     * Store the steady and init phases of the unerlying node
     */
    final OperatorPhases steadyNodePhases;
    final OperatorPhases initNodePhases;

    LatencyNode(Filter filter, DLList _ancestors)
    {
        ancestors = _ancestors;

        {
            steadyNodePhases =
                new OperatorPhases(
                    filter.getNumDeclaredSteadyPhases(),
                    1,
                    1);

            int nPhase;
            for (nPhase = 0;
                nPhase < filter.getNumDeclaredSteadyPhases();
                nPhase++)
            {
                steadyNodePhases.setPhaseInput(
                    filter.getDeclaredSteadyPhasePeek(nPhase),
                    filter.getDeclaredSteadyPhasePop(nPhase),
                    nPhase,
                    0);

                steadyNodePhases.setPhaseOutput(
                    filter.getDeclaredSteadyPhasePush(nPhase),
                    nPhase,
                    0);

                steadyNodePhases.setOperatorPhase(
                    filter.getDeclaredSteadyPhase(nPhase),
                    nPhase);
            }
        }

        {
            initNodePhases =
                new OperatorPhases(filter.getNumDeclaredInitPhases(), 1, 1);

            int nInitPhase;
            for (nInitPhase = 0;
                nInitPhase < filter.getNumDeclaredInitPhases();
                nInitPhase++)
            {
                initNodePhases.setPhaseInput(
                    filter.getDeclaredInitPhasePeek(nInitPhase),
                    filter.getDeclaredInitPhasePop(nInitPhase),
                    nInitPhase,
                    0);

                initNodePhases.setPhaseOutput(
                    filter.getDeclaredInitPhasePush(nInitPhase),
                    nInitPhase,
                    0);

                initNodePhases.setOperatorPhase(
                    filter.getDeclaredInitPhase(nInitPhase),
                    nInitPhase);
            }
        }
    }

    LatencyNode(
        StreamInterfaceWithSnJ sj,
        boolean isSplitter,
        DLList _ancestors)
    {
        ancestors = _ancestors;

        if (isSplitter)
        {
            initNodePhases = new OperatorPhases(0, 1, sj.getSplitFanOut());

            steadyNodePhases =
                new OperatorPhases(
                    sj.getNumSplitPhases(),
                    1,
                    sj.getSplitFanOut());

            int nPhase;
            for (nPhase = 0; nPhase < sj.getNumSplitPhases(); nPhase++)
            {
                steadyNodePhases.setPhaseInput(
                    sj.getSplitFlow(nPhase).getPopWeight(),
                    sj.getSplitFlow(nPhase).getPopWeight(),
                    nPhase,
                    0);

                steadyNodePhases.setOperatorPhase(
                    sj.getSplitPhase(nPhase),
                    nPhase);

                for (int nOutChannel = 0;
                    nOutChannel < sj.getSplitFanOut();
                    nOutChannel++)
                {
                    steadyNodePhases.setPhaseOutput(
                        sj.getSplitFlow(nPhase).getPushWeight(nOutChannel),
                        nPhase,
                        nOutChannel);
                }
            }
        }
        else
        {
            initNodePhases = new OperatorPhases(0, sj.getJoinFanIn(), 1);

            steadyNodePhases =
                new OperatorPhases(
                    sj.getNumJoinPhases(),
                    sj.getJoinFanIn(),
                    1);

            int nPhase;
            for (nPhase = 0; nPhase < sj.getNumJoinPhases(); nPhase++)
            {
                steadyNodePhases.setPhaseOutput(
                    sj.getJoinFlow(nPhase).getPushWeight(),
                    nPhase,
                    0);

                steadyNodePhases.setOperatorPhase(
                    sj.getSplitPhase(nPhase),
                    nPhase);

                for (int nOutChannel = 0;
                    nOutChannel < sj.getJoinFanIn();
                    nOutChannel++)
                {
                    steadyNodePhases.setPhaseInput(
                        sj.getJoinFlow(nPhase).getPopWeight(nOutChannel),
                        sj.getJoinFlow(nPhase).getPopWeight(nOutChannel),
                        nPhase,
                        nOutChannel);
                }
            }
        }

    }

    public DLList_const getAncestors()
    {
        return ancestors;
    }

    public int getInitNumPhases()
    {
        return initNodePhases.getNumPhases();
    }

    public int getInitPeek(int nChannel)
    {
        return initNodePhases.getOverallPeek(nChannel);
    }
    public int getInitPop(int nChannel)
    {
        return initNodePhases.getOverallPop(nChannel);
    }
    public int getInitPush(int nChannel)
    {
        return initNodePhases.getOverallPush(nChannel);
    }

    public int getSteadyNumPhases()
    {
        return steadyNodePhases.getNumPhases();
    }

    public int getSteadyStatePeek(int nChannel)
    {
        return steadyNodePhases.getOverallPeek(nChannel);
    }
    public int getSteadyStatePop(int nChannel)
    {
        return steadyNodePhases.getOverallPop(nChannel);
    }
    public int getSteadyStatePush(int nChannel)
    {
        return steadyNodePhases.getOverallPush(nChannel);
    }

    public int getPhasePeek(int nPhase, int nChannel)
    {
        if (nPhase < initNodePhases.getNumPhases())
        {
            // it's an init phase
            return initNodePhases.getPhasePeek(nPhase, nChannel);
        }
        else
        {
            // it's a steady state
            return steadyNodePhases.getPhasePeek(
                (nPhase - initNodePhases.getNumPhases())
                    % steadyNodePhases.getNumPhases(),
                nChannel);
        }
    }

    public int getPhasePop(int nPhase, int nChannel)
    {
        if (nPhase < initNodePhases.getNumPhases())
        {
            // it's an init phase
            return initNodePhases.getPhasePop(nPhase, nChannel);
        }
        else
        {
            // it's a steady state
            return steadyNodePhases.getPhasePop(
                (nPhase - initNodePhases.getNumPhases())
                    % steadyNodePhases.getNumPhases(),
                nChannel);
        }
    }

    public int getPhasePush(int nPhase, int nChannel)
    {
        if (nPhase < initNodePhases.getNumPhases())
        {
            // it's an init phase
            return initNodePhases.getPhasePush(nPhase, nChannel);
        }
        else
        {
            // it's a steady state
            return steadyNodePhases.getPhasePush(
                (nPhase - initNodePhases.getNumPhases())
                    % steadyNodePhases.getNumPhases(),
                nChannel);
        }
    }

    public void addDependency(LatencyEdge dependency)
    {
        if (dependency.getSrc() == this)
        {
            dependants.pushBack(dependency);
        }
        else
        {
            ASSERT(dependency.getDst() == this);
            dependsOn.pushBack(dependency);
        }
    }

    public DLList_const getDependants()
    {
        return dependants;
    }

    public DLList_const getDependecies()
    {
        return dependsOn;
    }

    public boolean hasAncestor(StreamInterface ancestor)
    {
        DLListIterator ancestorIter = ancestors.begin();
        DLListIterator lastAncestorIter = ancestors.end();

        for (; !ancestorIter.equals(lastAncestorIter); ancestorIter.next())
        {
            if (((StreamInterface)ancestorIter.get()) == ancestor)
            {
                return true;
            }
        }

        return false;
    }
}
