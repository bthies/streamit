package streamit.scheduler2.constrained;

import streamit.misc.DLList;
import streamit.misc.OMap;

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
     * This a map of StreamInterfaces to Integers.
     * The Integer stores how many times the particular
     * node needs to be executed in each steady state of
     * the corresponding Stream
     */
    final OMap ancestors2numExecs = new OMap();

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
                steadyNodePhases.setPhaseInput(
                    filter.getDeclaredInitPhasePeek(nInitPhase),
                    filter.getDeclaredInitPhasePop(nInitPhase),
                    nInitPhase,
                    0);

                steadyNodePhases.setPhaseOutput(
                    filter.getDeclaredInitPhasePush(nInitPhase),
                    nInitPhase,
                    0);

                steadyNodePhases.setOperatorPhase(
                    filter.getDeclaredInitPhase(nInitPhase),
                    nInitPhase);
            }
        }
    }

    public int getInitNumPhases()
    {
        return initNodePhases.getNumPhases();
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
}