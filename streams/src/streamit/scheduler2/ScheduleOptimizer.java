package streamit.scheduler2;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Vector;
import streamit.misc.AssertedClass;
import streamit.scheduler2.iriter.Iterator;

/**
 * This class takes a schedule produced by the scheduler and optimizes it
 * into a more compact schedule.
 */

public class ScheduleOptimizer extends AssertedClass
{
    final Schedule unoptimizedInit;
    final Schedule unoptimizedSteady;
    Schedule optimizedInit = null;
    Schedule optimizedSteady = null;

    final private Scheduler scheduler;

    public ScheduleOptimizer(
        Schedule _unoptimizedInit,
        Schedule _unoptimizedSteady,
        Scheduler _scheduler)
    {
        unoptimizedInit = _unoptimizedInit;
        unoptimizedSteady = _unoptimizedSteady;
        scheduler = _scheduler;
    }

    Integer symbolicUnoptimizedInit = null;
    Integer symbolicUnoptimizedSteady = null;

    public void optimize()
    {
        symbolicUnoptimizedInit = convertToSymbolic(unoptimizedInit);
        symbolicUnoptimizedSteady = convertToSymbolic(unoptimizedSteady);

        Integer oldUnoptimizedInit;
        Integer oldUnoptimizedSteady;

        do
        {
            oldUnoptimizedInit = symbolicUnoptimizedInit;
            oldUnoptimizedSteady = symbolicUnoptimizedSteady;
            printSymbolicSchedule(symbolicUnoptimizedSteady);
            collectRepeats();
            printSymbolicSchedule(symbolicUnoptimizedSteady);
            liftSingles();
            printSymbolicSchedule(symbolicUnoptimizedSteady);
            liftSingles2();
            printSymbolicSchedule(symbolicUnoptimizedSteady);
        }
        while (oldUnoptimizedInit.intValue()
            != symbolicUnoptimizedInit.intValue()
            || oldUnoptimizedSteady.intValue()
                != symbolicUnoptimizedSteady.intValue());

        optimizedInit = convertToSchedule(symbolicUnoptimizedInit);
        optimizedSteady = convertToSchedule(symbolicUnoptimizedSteady);
    }

    public Schedule getOptimizedInitSched()
    {
        if (optimizedSteady == null)
        {
            optimize();
        }

        return optimizedInit;
    }

    public Schedule getOptimizedSteadySched()
    {
        if (optimizedSteady == null)
        {
            optimize();
        }

        return optimizedSteady;
    }

    public Schedule getUnoptimizedInitSched()
    {
        return unoptimizedInit;
    }

    public Schedule getUnoptimizedSteadySched()
    {
        return unoptimizedSteady;
    }

    // ---------------- Beef of the class ----------------

    Map integers = new HashMap();

    Integer getInteger(int i)
    {
        Integer key = new Integer(i);

        if (!integers.containsKey(key))
        {
            integers.put(key, key);
            return key;
        }

        return (Integer)integers.get(key);
    }

    // this will store a map of Schedule -> Integer
    // Integer stores an index into a Vector which stores the
    // details of the specific symbolic phase
    Map sched2symbolicIdx = new HashMap();
    Map symbolicIdx2sched = new HashMap();

    Map symbolicIdx2symbolic = new HashMap();
    Map symbolic2symbolicIdx = new HashMap();
    Map symbolicIdx2stream = new HashMap();

    Integer convertToSymbolic(Schedule sched)
    {
        if (sched2symbolicIdx.containsKey(sched))
        {
            return (Integer)sched2symbolicIdx.get(sched);
        }

        // convert all the children
        if (!sched.isBottomSchedule())
        {
            for (int nPhase = 0; nPhase < sched.getNumPhases(); nPhase++)
            {
                convertToSymbolic(sched.getSubSched(nPhase));
            }
        }

        Integer symbolicIdx = getInteger(symbolicIdx2symbolic.size());

        // create a vector version of self
        Vector self = new Vector();
        if (!sched.isBottomSchedule())
        {
            for (int nPhase = 0; nPhase < sched.getNumPhases(); nPhase++)
            {
                self.add(getInteger(sched.getSubSchedNumExecs(nPhase)));
                self.add(sched2symbolicIdx.get(sched.getSubSched(nPhase)));
            }
        }
        else
        {
            // this is an actual leaf - create a vector with just
            // a single entry - the schedule
            self.add(sched);
        }

        if (symbolic2symbolicIdx.containsKey(self))
        {
            symbolicIdx = (Integer)symbolic2symbolicIdx.get(self);
            sched2symbolicIdx.put(sched, symbolicIdx);

            return symbolicIdx;
        }

        // and insert the symbolic representation and idx 
        // into both way lookups
        symbolicIdx2symbolic.put(symbolicIdx, self);
        symbolic2symbolicIdx.put(self, symbolicIdx);

        // also record the stream to which this schedule belongs
        symbolicIdx2stream.put(symbolicIdx, sched.getStream());

        // and store two-way maps of the sched <=> idx
        sched2symbolicIdx.put(sched, symbolicIdx);
        symbolicIdx2sched.put(symbolicIdx, sched);

        return symbolicIdx;
    }

    Schedule convertToSchedule(Integer symbolicIdx)
    {
        if (symbolicIdx2sched.containsKey(symbolicIdx))
        {
            return (Schedule)symbolicIdx2sched.get(symbolicIdx);
        }

        Vector self = (Vector)symbolicIdx2symbolic.get(symbolicIdx);

        if (self.size() == 1)
        {
            // this phase is actually a bottom schedule
            // just return it!
            return (Schedule)self.get(0);
        }
        else
        {
            Schedule newSched =
                new Schedule((Iterator)symbolicIdx2stream.get(symbolicIdx));
            Vector symbolicSched =
                (Vector)symbolicIdx2symbolic.get(symbolicIdx);

            for (int i = 0; i < symbolicSched.size(); i += 2)
            {
                newSched.addSubSchedule(
                    convertToSchedule((Integer)symbolicSched.get(i + 1)),
                    ((Integer)symbolicSched.get(i)).intValue());
            }

            symbolicIdx2sched.put(symbolicIdx, newSched);

            return newSched;
        }
    }

    void collectRepeats()
    {
        Map symbolicIdxSubstitionsChain = new HashMap();
        symbolicUnoptimizedInit =
            collectRepeats(
                symbolicUnoptimizedInit,
                symbolicIdxSubstitionsChain);
        symbolicUnoptimizedSteady =
            collectRepeats(
                symbolicUnoptimizedSteady,
                symbolicIdxSubstitionsChain);
    }

    Integer collectRepeats(
        Integer symbolicIdx,
        Map symbolicIdxSubstitionsChain)
    {
        if (symbolicIdxSubstitionsChain.containsKey(symbolicIdx))
        {
            return (Integer)symbolicIdxSubstitionsChain.get(symbolicIdx);
        }

        Vector symbolicSched =
            (Vector)symbolicIdx2symbolic.get(symbolicIdx);

        // can't do anything to leaf schedules
        if (symbolicSched.size() == 1)
            return symbolicIdx;

        Vector newSymbolicSched = new Vector();
        Integer previousPhaseIdx = getInteger(-1);

        for (int i = 0; i < symbolicSched.size(); i += 2)
        {
            Integer phaseSymbolicIdx = (Integer)symbolicSched.get(i + 1);
            Integer phaseNumExec = (Integer)symbolicSched.get(i);

            // optimize the child phase
            phaseSymbolicIdx =
                collectRepeats(
                    phaseSymbolicIdx,
                    symbolicIdxSubstitionsChain);

            if (phaseSymbolicIdx.intValue() == previousPhaseIdx.intValue())
            {
                int prevSchedIdx = newSymbolicSched.size() - 2;
                int prevNumExec =
                    ((Integer)newSymbolicSched.get(prevSchedIdx))
                        .intValue();

                newSymbolicSched.set(
                    prevSchedIdx,
                    getInteger(prevNumExec + phaseNumExec.intValue()));
            }
            else
            {
                newSymbolicSched.add(phaseNumExec);
                newSymbolicSched.add(phaseSymbolicIdx);
            }

            previousPhaseIdx = phaseSymbolicIdx;
        }

        Integer newSymbolicIdx;
        if (symbolic2symbolicIdx.containsKey(newSymbolicSched))
        {
            newSymbolicIdx =
                (Integer)symbolic2symbolicIdx.get(newSymbolicSched);
        }
        else
        {
            newSymbolicIdx = getInteger(symbolicIdx2symbolic.size());
            symbolic2symbolicIdx.put(newSymbolicSched, newSymbolicIdx);
            symbolicIdx2symbolic.put(newSymbolicIdx, newSymbolicSched);
            symbolicIdx2stream.put(
                newSymbolicIdx,
                symbolicIdx2stream.get(symbolicIdx));
        }

        symbolicIdxSubstitionsChain.put(symbolicIdx, newSymbolicIdx);
        return newSymbolicIdx;
    }

    void liftSingles()
    {
        Map symbolicIdxSubstitionsChain = new HashMap();
        symbolicUnoptimizedInit =
            liftSingles(
                symbolicUnoptimizedInit,
                symbolicIdxSubstitionsChain);
        symbolicUnoptimizedSteady =
            liftSingles(
                symbolicUnoptimizedSteady,
                symbolicIdxSubstitionsChain);
    }

    Integer liftSingles(
        Integer symbolicIdx,
        Map symbolicIdxSubstitionsChain)
    {
        if (symbolicIdxSubstitionsChain.containsKey(symbolicIdx))
        {
            return (Integer)symbolicIdxSubstitionsChain.get(symbolicIdx);
        }

        Vector symbolicSched =
            (Vector)symbolicIdx2symbolic.get(symbolicIdx);

        // can't do anything to leaf schedules
        if (symbolicSched.size() == 1)
            return symbolicIdx;

        Vector newSymbolicSched = new Vector();

        for (int i = 0; i < symbolicSched.size(); i += 2)
        {
            Integer phaseSymbolicIdx = (Integer)symbolicSched.get(i + 1);
            Integer phaseNumExec = (Integer)symbolicSched.get(i);

            // if for some reason I have a phase that
            // doesn't get executed, skip it
            if (phaseNumExec.intValue() == 0)
                continue;

            // optimize the child phase
            phaseSymbolicIdx =
                liftSingles(phaseSymbolicIdx, symbolicIdxSubstitionsChain);

            Vector phaseSymbolicSched;
            phaseSymbolicSched =
                (Vector)symbolicIdx2symbolic.get(phaseSymbolicIdx);

            // if the phase I'm referencing has not sub-phases, skip it
            if (phaseSymbolicSched.size() == 0)
                continue;

            // if the phase I'm referencing is only a single-phase phase,
            // simply lift
            if (phaseSymbolicSched.size() == 2)
            {
                phaseSymbolicIdx = (Integer)phaseSymbolicSched.get(1);
                phaseNumExec =
                    getInteger(
                        phaseNumExec.intValue()
                            * ((Integer)phaseSymbolicSched.get(0))
                                .intValue());
            }

            newSymbolicSched.add(phaseNumExec);
            newSymbolicSched.add(phaseSymbolicIdx);
        }

        Integer newSymbolicIdx;
        if (symbolic2symbolicIdx.containsKey(newSymbolicSched))
        {
            newSymbolicIdx =
                (Integer)symbolic2symbolicIdx.get(newSymbolicSched);
        }
        else
        {
            newSymbolicIdx = getInteger(symbolicIdx2symbolic.size());
            symbolic2symbolicIdx.put(newSymbolicSched, newSymbolicIdx);
            symbolicIdx2symbolic.put(newSymbolicIdx, newSymbolicSched);
            symbolicIdx2stream.put(
                newSymbolicIdx,
                symbolicIdx2stream.get(symbolicIdx));
        }

        symbolicIdxSubstitionsChain.put(symbolicIdx, newSymbolicIdx);
        return newSymbolicIdx;
    }

    void collectPhaseUseInfo(Integer phaseIdx, Map phaseUseCount)
    {
        if (phaseUseCount.containsKey(phaseIdx))
        {
            // already have the phase in the phaseUseCount
            // just increment the counter and return
            Integer phaseUseInfo = (Integer)phaseUseCount.get(phaseIdx);
            if (phaseUseInfo.intValue() > 0)
            {
                phaseUseInfo = getInteger(phaseUseInfo.intValue() + 1);
                phaseUseCount.put(phaseIdx, phaseUseInfo);
            }
            return;
        }

        // first time I'm visitin this phase        
        phaseUseCount.put(phaseIdx, getInteger(1));

        // is this a base phase? if so, quit while I'm ahead        
        Vector symbolic = (Vector)symbolicIdx2symbolic.get(phaseIdx);
        if (symbolic.size() == 1)
        {
            // before I quit, make sure that this phase will never have
            // the lifting procedure applied to it!
            phaseUseCount.put(phaseIdx, getInteger(-1));
            return;
        }

        // go through all the children's phases
        for (int i = 1; i < symbolic.size(); i += 2)
        {
            collectPhaseUseInfo((Integer)symbolic.get(i), phaseUseCount);
        }
    }

    void liftSingles2()
    {
        Map phaseUseCount = new HashMap();
        collectPhaseUseInfo(symbolicUnoptimizedInit, phaseUseCount);
        collectPhaseUseInfo(symbolicUnoptimizedSteady, phaseUseCount);

        Map symbolicIdxSubstitionsChain = new HashMap();
        symbolicUnoptimizedInit =
            liftSingles2(
                symbolicUnoptimizedInit,
                phaseUseCount,
                symbolicIdxSubstitionsChain);
        symbolicUnoptimizedSteady =
            liftSingles2(
                symbolicUnoptimizedSteady,
                phaseUseCount,
                symbolicIdxSubstitionsChain);
    }

    Integer liftSingles2(
        Integer symbolicIdx,
        Map phaseUseCount,
        Map symbolicIdxSubstitionsChain)
    {
        if (symbolicIdxSubstitionsChain.containsKey(symbolicIdx))
        {
            return (Integer)symbolicIdxSubstitionsChain.get(symbolicIdx);
        }

        Vector symbolicSched =
            (Vector)symbolicIdx2symbolic.get(symbolicIdx);

        // can't do anything to leaf schedules
        if (symbolicSched.size() == 1)
            return symbolicIdx;

        Vector newSymbolicSched = new Vector();

        for (int i = 0; i < symbolicSched.size(); i += 2)
        {
            Integer phaseSymbolicIdx = (Integer)symbolicSched.get(i + 1);
            Integer phaseNumExec = (Integer)symbolicSched.get(i);

            // optimize the child phase
            Integer newPhaseSymbolicIdx =
                liftSingles2(
                    phaseSymbolicIdx,
                    phaseUseCount,
                    symbolicIdxSubstitionsChain);

            // should I inline this phase?
            if (phaseNumExec.intValue() == 1
                && ((Integer)phaseUseCount.get(phaseSymbolicIdx)).intValue()
                    == 1)
            {
                // yes! find the phase to inline
                Vector phaseSymbolicSched;
                phaseSymbolicSched =
                    (Vector)symbolicIdx2symbolic.get(newPhaseSymbolicIdx);

                ASSERT(phaseSymbolicSched.size() != 1);

                for (int j = 0; j < phaseSymbolicSched.size(); j++)
                {
                    newSymbolicSched.add(phaseSymbolicSched.get(j));
                }
            }
            else
            {
                // no - just add it to the schedule normally
                newSymbolicSched.add(phaseNumExec);
                newSymbolicSched.add(phaseSymbolicIdx);
            }
        }

        Integer newSymbolicIdx;
        if (symbolic2symbolicIdx.containsKey(newSymbolicSched))
        {
            newSymbolicIdx =
                (Integer)symbolic2symbolicIdx.get(newSymbolicSched);
        }
        else
        {
            newSymbolicIdx = getInteger(symbolicIdx2symbolic.size());
            symbolic2symbolicIdx.put(newSymbolicSched, newSymbolicIdx);
            symbolicIdx2symbolic.put(newSymbolicIdx, newSymbolicSched);
            symbolicIdx2stream.put(
                newSymbolicIdx,
                symbolicIdx2stream.get(symbolicIdx));
        }

        symbolicIdxSubstitionsChain.put(symbolicIdx, newSymbolicIdx);
        return newSymbolicIdx;
    }

    private void printSymbolicSchedule(Integer symbolicIdx)
    {
        System.out.println("[");
        printSymbolicSchedule(symbolicIdx, new HashSet());
        System.out.println("]");
    }

    private void printSymbolicSchedule(
        Integer symbolicIdx,
        Set printedScheds)
    {
        if (printedScheds.contains(symbolicIdx))
            return;

        System.out.print("$" + symbolicIdx + " = ");

        Vector self = (Vector)symbolicIdx2symbolic.get(symbolicIdx);

        if (self.size() == 1)
        {
            Schedule sched = (Schedule)symbolicIdx2sched.get(symbolicIdx);
            System.out.println(
                sched.getStream().getObject() + "." + sched.getWorkFunc());
        }
        else
        {
            Integer scheds[] = new Integer[self.size() / 2];
            System.out.print("{ ");

            Vector symbolicSched =
                (Vector)symbolicIdx2symbolic.get(symbolicIdx);

            for (int i = 0; i < symbolicSched.size(); i += 2)
            {
                int times = ((Integer)symbolicSched.get(i)).intValue();
                int idx = ((Integer)symbolicSched.get(i + 1)).intValue();
                scheds[i / 2] = (Integer)symbolicSched.get(i + 1);
                if (times > 1)
                    System.out.print("{" + times + " $" + idx + "} ");
                else
                    System.out.print("$" + idx + " ");
            }

            System.out.println("}");

            printedScheds.add(symbolicIdx);

            for (int i = 0; i < symbolicSched.size() / 2; i++)
            {
                printSymbolicSchedule(scheds[i], printedScheds);
            }
        }
    }
}
