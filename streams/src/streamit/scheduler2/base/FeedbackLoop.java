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

package streamit.scheduler2.base;

import streamit.scheduler2.iriter./*persistent.*/
FeedbackLoopIter;
import java.math.BigInteger;
import streamit.misc.Fraction;

/**
 * Computes some basic steady state data for FeedbackLoops.
 *
 * @version 2
 * @author  Michal Karczmarek
 */

abstract public class FeedbackLoop extends StreamWithSplitNJoin
{
    final protected FeedbackLoopIter feedbackLoop;
    StreamInterface body;
    StreamInterface loop;

    protected FeedbackLoop(
        FeedbackLoopIter _feedbackLoop,
        StreamFactory factory)
    {
        super(_feedbackLoop);

        ASSERT(_feedbackLoop);
        feedbackLoop = _feedbackLoop;

        // create new objects for the body and the loop
        body = factory.newFrom(feedbackLoop.getBodyChild(), feedbackLoop.getUnspecializedIter());
        loop = factory.newFrom(feedbackLoop.getLoopChild(), feedbackLoop.getUnspecializedIter());

        // compute my steady schedule
        // my children already have computed their steady schedules,
        // so I just have to do mine
        computeSteadyState();
    }

    /**
     * Returns the body of a feedback loop.
     * @return the body of the feedback loop
     */
    protected StreamInterface getBody()
    {
        return body;
    }

    /**
     * Returns the feedback path of a feedback loop.
     * @return the feedback path of the feedback loop
     */
    protected StreamInterface getLoop()
    {
        return loop;
    }

    /**
     * get the fan-out of a splitter
     * @return fan-out of a splitter
     */

    public int getSplitFanOut()
    {
        return 2;
    }

    /**
     * get the fan-in of a joiner
     * @return fan-in of a joiner
     */

    public int getJoinFanIn()
    {
        return 2;
    }
    
    /**
     * these store how many times the body and loop need to
     * go be executed to execute a full steady state of this FeedbackLoop.
     * These are initialized by computeSteadySchedule
     */
    private int bodyNumExecs, loopNumExecs;

    /**
     * these store how many times the splitter and joiner need to
     * go through their ENTIRE execution (all work functions)
     * in order to execute a full steady state of this FeedbackLoop.
     * These are initialized by computeSteadySchedule
     */
    private int splitNumRounds, joinNumRounds;

    protected int getNumBodyExecs()
    {
        return bodyNumExecs;
    }
    protected int getNumLoopExecs()
    {
        return loopNumExecs;
    }
    protected int getNumSplitRounds()
    {
        return splitNumRounds;
    }
    protected int getNumJoinRounds()
    {
        return joinNumRounds;
    }

    /**
     * Compute the number of times the body, loop, split and join
     * need to execute for the entire feedback loop to execute a minimal 
     * full steady state execution.
     * 
     * This function is essentially copied from the old scheduler,
     * and modified to work with the new interfaces.
     */
    public void computeSteadyState()
    {
        // amount of data distributed to and collected by the split
        // and join
        int splitPushWeights[];
        int joinPopWeights[];
        int splitPopWeight, joinPushWeight;

        // now, assuming the body executes once, compute fractions
        // of how many times everything else executes
        {
            BigInteger bodyPush, bodyPop;
            BigInteger loopPush, loopPop;

            bodyPush = BigInteger.valueOf(body.getSteadyPush());
            bodyPop = BigInteger.valueOf(body.getSteadyPop());

            loopPush = BigInteger.valueOf(loop.getSteadyPush());
            loopPop = BigInteger.valueOf(loop.getSteadyPop());

            BigInteger splitPush, splitPop;
            BigInteger joinPush, joinPop;

            // get the feedback production rate and others
            splitPush =
                BigInteger.valueOf(getSteadySplitFlow().getPushWeight(1));
            splitPop =
                BigInteger.valueOf(getSteadySplitFlow().getPopWeight());
            joinPop = BigInteger.valueOf(getSteadyJoinFlow().getPopWeight(1));
            joinPush =
                BigInteger.valueOf(getSteadyJoinFlow().getPushWeight());

            // calculate all the fractions
            Fraction bodyFrac = new Fraction(BigInteger.ONE, BigInteger.ONE);
            Fraction splitFrac =
                new Fraction(bodyPush, splitPop).multiply(bodyFrac);
            Fraction loopFrac =
                new Fraction(splitPush, loopPop).multiply(splitFrac);
            Fraction joinFrac =
                new Fraction(loopPush, joinPop).multiply(loopFrac);

            // make sure that the rates are self consistant
            if (!joinFrac
                .multiply(joinPush)
                .divide(bodyPop)
                .equals(bodyFrac))
            {
                ERROR("Inconsistant program - cannot be scheduled without growing buffers infinitely!");
            }

            // compute a minimal multiplier for all the fractions
            // s.t. multiplying the fractions by the multiplier will yield
            // all integers
            BigInteger multiplier = bodyFrac.getDenominator();
            multiplier =
                multiplier.multiply(
                    splitFrac.getDenominator().divide(
                        multiplier.gcd(splitFrac.getDenominator())));
            multiplier =
                multiplier.multiply(
                    loopFrac.getDenominator().divide(
                        multiplier.gcd(loopFrac.getDenominator())));
            multiplier =
                multiplier.multiply(
                    joinFrac.getDenominator().divide(
                        multiplier.gcd(joinFrac.getDenominator())));

            // multiply all the fractions by the multiplier
            bodyFrac = bodyFrac.multiply(multiplier);
            splitFrac = splitFrac.multiply(multiplier);
            loopFrac = loopFrac.multiply(multiplier);
            joinFrac = joinFrac.multiply(multiplier);

            // make sure that all the fractions are integers now
            ASSERT(bodyFrac.getDenominator().equals(BigInteger.ONE));
            ASSERT(loopFrac.getDenominator().equals(BigInteger.ONE));
            ASSERT(joinFrac.getDenominator().equals(BigInteger.ONE));
            ASSERT(splitFrac.getDenominator().equals(BigInteger.ONE));

            // and now actually set the appropriate multipliers on body and loop
            // and split and join:
            bodyNumExecs = bodyFrac.getNumerator().intValue();
            loopNumExecs = loopFrac.getNumerator().intValue();
            joinNumRounds = joinFrac.getNumerator().intValue();
            splitNumRounds = splitFrac.getNumerator().intValue();

            // make sure that both children, splitter and joiner will
            // execute a positive # of times
            ASSERT(bodyNumExecs > 0);
            ASSERT(loopNumExecs > 0);
            ASSERT(joinNumRounds > 0);
            ASSERT(splitNumRounds > 0);
            
        }

        // setup my variables that come from SchedStream:
        {
            int pop = joinNumRounds * getSteadyJoinFlow().getPopWeight(0);
            int push = splitNumRounds * getSteadySplitFlow().getPushWeight(0);

            setSteadyPeek(pop);
            setSteadyPop(pop);
            setSteadyPush(push);
        }
    }
    
    public int getNumNodes () 
    { 
        int nodes = 2 + body.getNumNodes () + loop.getNumNodes();
        return nodes;
    }
    
    public int getNumNodeFirings() 
    {
        int firings = 0;
        firings += body.getNumNodeFirings ();
        firings += loop.getNumNodeFirings ();
        firings += getNumSplitRounds();
        firings += getNumJoinRounds();
        
        return firings;
    }
}
