package streamit.scheduler.base;

import streamit.scheduler.iriter./*persistent.*/FeedbackLoopIter;
import streamit.scheduler.iriter./*persistent.*/Iterator;
import java.math.BigInteger;
import streamit.misc.Fraction;

/* $Id: FeedbackLoop.java,v 1.5 2002-07-02 03:37:44 karczma Exp $ */

/**
 * Computes some basic steady state data for FeedbackLoops.
 *
 * @version 2
 * @author  Michal Karczmarek
 */

abstract public class FeedbackLoop extends StreamWithSplitNJoin
{
    FeedbackLoopIter feedbackLoop;
    StreamInterface body;
    StreamInterface loop;

    FeedbackLoop (FeedbackLoopIter _feedbackLoop, StreamFactory factory)
    {
        super (_feedbackLoop);
        
        ASSERT(_feedbackLoop);
        feedbackLoop = _feedbackLoop;
        
        // create new objects for the body and the loop
        body = factory.newFrom (feedbackLoop.getBodyChild ());
        loop = factory.newFrom (feedbackLoop.getLoopChild ());
        
        // compute my steady schedule
        // my children already have computed their steady schedules,
        // so I just have to do mine
        computeSteadyState();
    }

    /**
     * these store how many times the body and loop need to
     * go be executed to execute a full steady state of this FeedbackLoop.
     * These are initialized by computeSteadySchedule
     */
    private BigInteger bodyNumExecs, loopNumExecs;

    /**
     * these store how many times the splitter and joiner need to
     * go through their ENTIRE execution (all work functions)
     * in order to execute a full steady state of this FeedbackLoop.
     * These are initialized by computeSteadySchedule
     */
    private BigInteger splitNumRounds, joinNumRounds;

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
        // not tested yet.
        ASSERT (false);
        
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

            bodyPush = BigInteger.valueOf (body.getSteadyPush ());
            bodyPop = BigInteger.valueOf (body.getSteadyPop ());

            loopPush = BigInteger.valueOf (loop.getSteadyPush ());
            loopPop = BigInteger.valueOf (loop.getSteadyPop ());

            BigInteger splitPush, splitPop;
            BigInteger joinPush, joinPop;

            // get the feedback production rate and others
            splitPush = BigInteger.valueOf (splitFlow.pushWeights [1]);
            splitPop = BigInteger.valueOf (splitFlow.popWeight);
            joinPop = BigInteger.valueOf (joinFlow.popWeights [1]);
            joinPush = BigInteger.valueOf (joinFlow.pushWeight);

            // calculate all the fractions
            Fraction bodyFrac = new Fraction (BigInteger.ONE, BigInteger.ONE);
            Fraction splitFrac = new Fraction (bodyPush, splitPop).multiply (bodyFrac);
            Fraction loopFrac = new Fraction (splitPush, loopPop).multiply (splitFrac);
            Fraction joinFrac = new Fraction (loopPush, joinPop).multiply (loopFrac);

            // make sure that the rates are self consistant
            if (!joinFrac.multiply (joinPush).divide (bodyPop).equals (bodyFrac))
            {
                ERROR ("Inconsistant program - cannot be scheduled without growing buffers infinitely!");
            }

            // compute a minimal multiplier for all the fractions
            // s.t. multiplying the fractions by the multiplier will yield
            // all integers
            BigInteger multiplier = bodyFrac.getDenominator ();
            multiplier = multiplier.multiply (splitFrac.getDenominator ().divide (multiplier.gcd (splitFrac.getDenominator ())));
            multiplier = multiplier.multiply (loopFrac.getDenominator ().divide (multiplier.gcd (loopFrac.getDenominator ())));
            multiplier = multiplier.multiply (joinFrac.getDenominator ().divide (multiplier.gcd (joinFrac.getDenominator ())));

            // multiply all the fractions by the multiplier
            bodyFrac = bodyFrac.multiply (multiplier);
            splitFrac = splitFrac.multiply (multiplier);
            loopFrac = loopFrac.multiply (multiplier);
            joinFrac = joinFrac.multiply (multiplier);

            // make sure that all the fractions are integers now
            ASSERT (bodyFrac.getDenominator ().equals (BigInteger.ONE));
            ASSERT (loopFrac.getDenominator ().equals (BigInteger.ONE));
            ASSERT (joinFrac.getDenominator ().equals (BigInteger.ONE));
            ASSERT (splitFrac.getDenominator ().equals (BigInteger.ONE));

            // and now actually set the appropriate multipliers on body and loop
            // and split and join:
            bodyNumExecs = bodyFrac.getNumerator ();
            loopNumExecs = loopFrac.getNumerator ();
            joinNumRounds = joinFrac.getNumerator ();
            splitNumRounds = splitFrac.getNumerator ();
        }

        // setup my variables that come from SchedStream:
        {
            int pop = joinNumRounds.intValue () * joinFlow.popWeights [0];
            int push = splitNumRounds.intValue () * splitFlow.pushWeights [1];
            
            setSteadyPeek (pop);
            setSteadyPop (pop);
            setSteadyPush (push);
        }
    }
}

