package streamit.scheduler2.constrained;

import streamit.misc.DLList;
import streamit.misc.DLList_const;
import streamit.misc.DLListIterator;

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

    LatencyNode(SplitJoin sj, boolean isSplitter, DLList _ancestors)
    {
        ancestors = _ancestors;

        if (isSplitter)
        {
            initNodePhases = new OperatorPhases(0, 1, sj.getNumChildren());

            steadyNodePhases =
                new OperatorPhases(
                    sj.getNumSplitPhases(),
                    1,
                    sj.getNumChildren());

            int nPhase;
            for (nPhase = 0; nPhase < sj.getNumSplitPhases(); nPhase++)
            {
                steadyNodePhases.setPhaseInput(
                    sj.getSplitFlow(nPhase).getPopWeight(),
                    sj.getSplitFlow(nPhase).getPopWeight(),
                    nPhase,
                    0);

                for (int nOutChannel = 0;
                    nOutChannel < sj.getNumChildren();
                    nOutChannel++)
                {
                    steadyNodePhases.setPhaseOutput(
                        sj.getSplitFlow(nPhase).getPushWeight(nOutChannel),
                        nPhase,
                        nOutChannel);

                    steadyNodePhases.setOperatorPhase(
                        sj.getSplitPhase(nPhase),
                        nPhase);
                }
            }
        }
        else
        {
            initNodePhases = new OperatorPhases(0, sj.getNumChildren(), 1);

            steadyNodePhases =
                new OperatorPhases(
                    sj.getNumJoinPhases(),
                    sj.getNumChildren(),
                    1);

            int nPhase;
            for (nPhase = 0; nPhase < sj.getNumJoinPhases(); nPhase++)
            {
                steadyNodePhases.setPhaseOutput(
                    sj.getJoinFlow(nPhase).getPushWeight(),
                    nPhase,
                    0);

                for (int nOutChannel = 0;
                    nOutChannel < sj.getNumChildren();
                    nOutChannel++)
                {
                    steadyNodePhases.setPhaseInput(
                        sj.getJoinFlow(nPhase).getPopWeight(nOutChannel),
                        sj.getJoinFlow(nPhase).getPopWeight(nOutChannel),
                        nPhase,
                        nOutChannel);

                    steadyNodePhases.setOperatorPhase(
                        sj.getSplitPhase(nPhase),
                        nPhase);
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