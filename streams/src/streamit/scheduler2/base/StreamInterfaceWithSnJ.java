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

/**
 * This interface provides the required functional interface for
 * all scheduling objects with splits and joins.
 * This is an extension of just regular scheduling
 * (as you can see from the extends statement :)
 * Basically, this takes care of getting information about
 * the split and the join in the stream
 * 
 * I have to make this an interface instead of a class because
 * Java doesn't have multiple inheritance.  Argh!
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

public interface StreamInterfaceWithSnJ extends StreamInterface
{
    /**
     * store the amount of data distributed to and 
     * collected by the splitter
     */
    public class SplitFlow extends streamit.misc.DestroyedClass
    {
        SplitFlow(int nChildren)
        {
            pushWeights = new int[nChildren];
        }

        private int pushWeights[];
        private int popWeight = 0;

        void setPopWeight(int weight)
        {
            popWeight = weight;
        }

        void setPushWeight(int nChild, int weight)
        {
            pushWeights[nChild] = weight;
        }

        public int getPopWeight()
        {
            return popWeight;
        }

        public int getPushWeight(int nChild)
        {
            // cannot easily check for being out of bounds
            // if this function throws an out-of-bounds exception, the
            // problem is with the nChild being too large (already 
            // checking for negative values below)
            assert nChild >= 0;

            return pushWeights[nChild];
        }
    }

    /**
     * store the amount of data distributed to and 
     * collected by the joiner
     */
    public class JoinFlow extends streamit.misc.DestroyedClass
    {
        JoinFlow(int nChildren)
        {
            popWeights = new int[nChildren];
        }

        private int popWeights[];
        private int pushWeight = 0;

        void setPushWeight(int weight)
        {
            pushWeight = weight;
        }

        void setPopWeight(int nChild, int weight)
        {
            popWeights[nChild] = weight;
        }

        public int getPushWeight()
        {
            return pushWeight;
        }

        public int getPopWeight(int nChild)
        {
            // cannot easily check for being out of bounds
            // if this function throws an out-of-bounds exception, the
            // problem is with the nChild being too large (already 
            // checking for negative values below)
            assert nChild >= 0;

            return popWeights[nChild];
        }
    }

    /**
     * get the flow of the splitter in steady state (all splitter's 
     * phases executed once)
     * @return splitter's steady flow
     */
    public SplitFlow getSteadySplitFlow();

    /**
     * get the flow of the joiner in steady state (all joiner's 
     * phases executed once)
     * @return joiner's steady flow
     */
    public JoinFlow getSteadyJoinFlow();

    /**
     * get the flow of the splitter for a particular phase
     * @return splitter's phase flow
     */
    public SplitFlow getSplitFlow(int nPhase);

    /**
     * get the flow of the joiner for a particular phase
     * @return joiner's phase flow
     */
    public JoinFlow getJoinFlow(int nPhase);

    /**
     * get the fan-out of a splitter
     * @return fan-out of a splitter
     */

    public int getSplitFanOut();

    /**
     * get the fan-in of a joiner
     * @return fan-in of a joiner
     */

    public int getJoinFanIn();

    /**
     * get the number of phases the splitter has
     * @return number of phases the splitter has
     */

    public int getNumSplitPhases();

    /**
     * get the number of phases the joiner has
     * @return number of phases the joiner has
     */

    public int getNumJoinPhases();
}
