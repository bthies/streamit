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

package streamit.scheduler2;

public interface SDEPData
{
    /**
     * get the number of phases the upstream node needs to execute in
     * order for the full interval [src,dst] to be able to initialize.
     * Once the upstream node has executed this many times, the downstream
     * node has enough data available to execute getNumDstInitPhases()
     * without executing the upstream node any more.
     * @return
     */
    public int getNumSrcInitPhases();

    /**
     * Get the number of phases the downstream node needs to execute
     * in order for the full interval of [src,dst] is initialized.
     * Once this many phases of the downstream node have been executed,
     * the downstream node has definitely executed getNumSrcInitPhases()
     * number of phases.
     */
    public int getNumDstInitPhases();

    /**
     * Get the number of phases that the downstream node needs to execute
     * in order for the two nodes to execute exactly one full steady state.
     * This takes into account any possible interference that nodes between
     * the upstream and downstream nodes will provide. 
     */
    public int getNumDstSteadyPhases();

    /**
     * Get the number of phases that the upstream node needs to execute
     * in order for the two nodes to execute exactly one full steady state.
     * This takes into account any possible interference that nodes between
     * the upstream and downstream nodes will provide. 
     */
    public int getNumSrcSteadyPhases();

    /**
     * Get the SDEP function value for nDstPhase phase of the downstream
     * node. This will return the minimial number of executions that 
     * the upstream node needs to complete in order to execute the downstream
     * node nDstPhase times. In other words, nDstPhase phase execution of the
     * downstream node sees the information wavefront of the 
     * getSrcPhase4DstPhase(nDstPhase) phase execution of the upstream node.
     * 
     * Note, that counting here ALWAYS starts at ONE! That means that 
     * getSrcPhase4DstPhase (0) is always 0, because the question asked here
     * is about executing ZERO phases of the downstream node! 
     */
    public int getSrcPhase4DstPhase(int nDstPhase);
    
    /**
     * Get the reverse SDEP function value for nSrcPhase of the upstream node.
     * This will return the phase execution of the downstream node which
     * will see data produced by nSrcPhase phase execution of the upstream
     * node.
     * 
     * Note, that counting here ALWAYS starts at ONE! That is we're dealing
     * with NUMBERS OF EXECUTION of phases, not indexes of phases. 
     */
    public int getDstPhase4SrcPhase(int nSrcPhase);
}
