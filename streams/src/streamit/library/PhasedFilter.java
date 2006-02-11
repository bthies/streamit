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

package streamit.library;

import streamit.scheduler2.Scheduler;

// Last edited by Matthew Drake

/** 
 * A PhasedFilter is designed so that one phase executes per work execution.
 * In practice, this means that a work cycle ends immediately following the
 * execution of any phase. This phase could have been called within the work() 
 * body. PhasedFilters run their work function in their own thread, and implementations
 * of PhasedFilters are expected to call contextSwitch() after a phase execution.
 */

public abstract class PhasedFilter extends Filter implements Runnable
{
    /**
     * The number of times that this has executed a phase.
     * Incremented only AFTER a given phase has completely finished.
     */
    private int phaseExecutions = 0;
    /**
     * The number of times this has executed a phase that popped at
     * least one item.
     */
    private int popPhaseExecutions = 0;
    /**
     * The number of times this has executed a phase that pushed at
     * least one item.
     */
    private int pushPhaseExecutions = 0;
    /**
     * Whether or not we have context-switched on the current
     * execution of work.
     */
    private boolean contextSwitched;
    
    public PhasedFilter() { super(); }
    public PhasedFilter(int a) { super(a); }

    private boolean firstWork = true;
    /**
     * A variable to transfer exceptions between threads.  (We have
     * two threads, and the one that runs the actual work function is
     * different than the one called by the scheduler.  So we have to
     * stand on our head to move the exception to the caller).
     */
    private RuntimeException pendingException = null;

    public void doWork() {
        synchronized (this) {
            try {
                if (firstWork) {
                    firstWork = false;
                    Thread t = new Thread(this);
                    t.start();
                    wait();
                } else {
                    notify();
                    wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // if a pending exception was found, raise it in this thread
            if (pendingException != null) {
                // clear the exception
                RuntimeException e = pendingException;
                pendingException = null;
                // throw the exception
                throw e;
            }
        }
    }

    public void run() {
        while (true) {
            try {
                prepareToWork();
                contextSwitched = false;
                work();
                if (!contextSwitched) {
                    // in the very degenerate case that no phases are
                    // called from work(), yield control here to avoid
                    // infinite loop
                    contextSwitch();
                }
                cleanupWork();
            } catch (NoPushPopException e) {
                // the exception is caught by the other thread, so
                // move it through a local variable so the other
                // thread will see it.
                pendingException = e;
                try {
                    synchronized(this) {
                        notify();
                        wait();
                    }
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }
    }

    /**
     * contextSwitch is called so that the PhasedFilter's work loop can pause
     * and end a work cycle.
     */
    protected void contextSwitch() {
        // remember we were here
        contextSwitched = true;

        synchronized (this) {
            try {
                // analog of cleanup work -- count execution we just finished
                countPhases();

                // pause
                notify();
                wait();

                // analog of begin work -- deliver messages for next phase
                deliverMessages();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Does bookkeping to count appropriate phases at boundaries.
     */
    int oldPopped = 0;
    int oldPushed = 0;
    private void countPhases() {
        // count all phases
        phaseExecutions++;
        // count pop or push phase if appropriate
        if (totalPopped > oldPopped) popPhaseExecutions++;
        if (totalPushed > oldPushed) pushPhaseExecutions++;

        // reset pop/push counters for next phase
        oldPopped = totalPopped;
        oldPushed = totalPushed;
    }

    /**
     * Returns the number of times this has executed a phase.  Only
     * counts completely finished phases (not phases that are in
     * progress.)
     */
    public int getPhaseExecutions() {
        return phaseExecutions;
    }

    /**
     * Returns the number of times that this has executed with regards
     * to the SDEP timing of a message.  That is, returns the number
     * of "ticks" this filter has done with regards to messaging.
     *
     * @param   receiver   Whether or not this is receiving the message.
     *                     (If false, this was the sender of message).
     * @param   downstream Whether or not message sent downstream.
     *                     (If false, message was sent upstream).
     */
    public int getSDEPExecutions(boolean receiver, boolean downstream) {
        // use the I/O count of this filter that is in the direction
        // of the message
        if (receiver && downstream ||
            !receiver && !downstream) {
            // receiving downstream, sending upstream:  use pop count
            return popPhaseExecutions;
        } else {
            // receiving upstream, sending downstream:  use push count
            return pushPhaseExecutions;
        }
    }

    /**
     * Placeholder so that java input to Kopi will compile.
     */
    public void phase(WorkFunction f) {
    }
}
