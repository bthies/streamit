package streamit.scheduler2.base;

import streamit.scheduler2.iriter./*persistent.*/
SplitJoinIter;
import java.math.BigInteger;
import streamit.misc.Fraction;

/**
 * Computes some basic steady state data for SplitJoins.
 *
 * @version 2
 * @author  Michal Karczmarek
 */

abstract public class SplitJoin extends StreamWithSplitNJoin
{
    protected SplitJoinIter splitjoin;

    private int nChildren;
    private StreamInterface children[];

    public SplitJoin(SplitJoinIter _splitjoin, StreamFactory factory)
    {
        super(_splitjoin);

        ASSERT(_splitjoin);
        splitjoin = _splitjoin;

        // fill up the children array
        // do this to preseve consistancy of iterators
        {
            nChildren = splitjoin.getNumChildren();

            // a pipeline must have some children
            ASSERT(nChildren > 0);

            children = new StreamInterface[nChildren];

            int nChild;
            for (nChild = 0; nChild < splitjoin.getNumChildren(); nChild++)
            {
                // create a new object for the child
                children[nChild] =
                    factory.newFrom(splitjoin.getChild(nChild), splitjoin.getUnspecializedIter());
            }

            // compute my steady schedule
            // my children already have computed their steady schedules,
            // so I just have to do mine
            computeSteadyState();
        }
    }

    /**
     * Get the number of children this SplitJoin has.
     * @return number of children
     */
    public int getNumChildren()
    {
        return nChildren;
    }

    /**
     * Get the a child of this splitjoin.
     * @return nth child
     */
    protected StreamInterface getChild(int nChild)
    {
        ASSERT(nChild >= 0 && nChild < nChildren);
        return children[nChild];
    }
    
    /**
     * get the fan-out of a splitter
     * @return fan-out of a splitter
     */

    public int getSplitFanOut()
    {
        return nChildren;
    }

    /**
     * get the fan-in of a joiner
     * @return fan-in of a joiner
     */

    public int getJoinFanIn()
    {
        return nChildren;
    }

    /**
     * stores how many times each child needs to be executed for the
     * SplitJoin to go through an entire steady state.
     * These are initialized by computeSteadySchedule
     */
    private BigInteger childrenNumExecs[];

    /**
     * Return how many times a particular child should be executed in
     * a full steady-state execution of this pipeline.
     * @return number of executions of a child in steady state
     */
    protected int getChildNumExecs(int nChild)
    {
        // make sure nChild is in range
        ASSERT(nChild >= 0 && nChild < getNumChildren());

        return childrenNumExecs[nChild].intValue();
    }

    /**
     * these store how many times the splitter and joiner need to
     * go through their ENTIRE execution (all work functions)
     * in order to execute a full steady state of this SplitJoin.
     * These are initialized by computeSteadySchedule
     */
    private BigInteger splitNumRounds, joinNumRounds;

    /**
     * return the numberof times all the splitter work functions need to
     * run in order to complete a steady schedule.
     * @return number of rounds the splitter needs to run in a steady schedule
     */
    protected int getSplitNumRounds()
    {
        return splitNumRounds.intValue();
    }

    /**
     * return the numberof times all the joiner work functions need to
     * run in order to complete a steady schedule.
     * @return number of rounds the joiner needs to run in a steady schedule
     */
    protected int getJoinNumRounds()
    {
        return joinNumRounds.intValue();
    }

    /**
     * Compute the number of times each child, the split and the join
     * need to execute for the entire splitjoin to execute a minimal 
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

        Fraction childrenRates[] = new Fraction[nChildren];
        Fraction splitRate = null;
        Fraction joinRate = null;

        // go through all children and calculate the rates at which
        // they will be called w.r.t. the splitter.
        // also, compute the rate of execution of the joiner
        // (if it ever ends up being executed)
        {
            int nChild;
            for (nChild = 0; nChild < nChildren; nChild++)
            {
                StreamInterface child = children[nChild];
                ASSERT(child);

                // the rate at which the child should be executed
                Fraction childRate = null;

                // rates at which the splitter is producing the data
                // and the child is consuming it:
                int numOut = getSteadySplitFlow().getPushWeight(nChild);
                int numIn = child.getSteadyPop();

                // is the splitter actually producing any data?
                if (numOut != 0)
                {
                    // if the slitter is producing data, the child better
                    // be consuming it!
                    ASSERT(numIn != 0);

                    if (splitRate == null)
                    {
                        // if I hadn't set the split rate yet, do it now
                        splitRate =
                            new Fraction(BigInteger.ONE, BigInteger.ONE);
                    }

                    // compute the rate at which the child should be executing
                    // (relative to the splitter)
                    childRate =
                        new Fraction(numOut, numIn)
                            .multiply(splitRate)
                            .reduce();

                    // if I still hadn't computed the rate at which the joiner
                    // is executed, try to compute it:
                    if (joinRate == null && child.getSteadyPush() != 0)
                    {
                        // if the child is producing data, the joiner
                        // better be consuming it!
                        ASSERT(getSteadyJoinFlow().getPopWeight(nChild) != 0);

                        int childOut = child.getSteadyPush();
                        int joinIn = getSteadyJoinFlow().getPopWeight(nChild);

                        joinRate =
                            new Fraction(childOut, joinIn)
                                .multiply(childRate)
                                .reduce();
                    }
                }

                childrenRates[nChild] = childRate;
            }
        }

        // compute the rate of execution of the joiner w.r.t. children
        // and make sure that everything will be executed at consistant
        // rates (to avoid overflowing the buffers)
        {
            // if the splitter never needs to get executed (doesn't produce
            // any data), the joiner rate should be set to ONE:
            if (splitRate == null)
            {
                // I better not have computed the join rate yet!
                ASSERT(joinRate == null);

                // okay, just set it to ONE/ONE
                joinRate = new Fraction(BigInteger.ONE, BigInteger.ONE);
            }

            int nChild;
            for (nChild = 0; nChild < nChildren; nChild++)
            {
                StreamInterface child = children[nChild];
                ASSERT(child);

                // get the child rate
                Fraction childRate = childrenRates[nChild];

                // compute the new childRate:
                Fraction newChildRate = null;
                {
                    int childOut = child.getSteadyPush();
                    int joinIn = getSteadyJoinFlow().getPopWeight(nChild);

                    // does the child produce any data?
                    if (childOut != 0)
                    {
                        // yes
                        // the split better consume some data too!
                        ASSERT(joinIn != 0);

                        // compute the rate at which the child should execute
                        // w.r.t. the splitter
                        newChildRate =
                            new Fraction(joinIn, childOut)
                                .multiply(joinRate)
                                .reduce();
                    }
                    else
                    {
                        // no
                        // the splitter better not consume any data either
                        ASSERT(joinIn == 0);
                    }
                }

                // if this is a new rate, put it in the array
                if (childRate == null)
                {
                    // I better have the rate here, or the child
                    // neither produces nor consumes any data!
                    ASSERT(newChildRate != null);

                    // set the rate
                    childrenRates[nChild] = newChildRate;
                }

                // okay, if I have both rates, make sure that they agree!
                if (childRate != null && newChildRate != null)
                {
                    if (!childRate.equals(newChildRate))
                    {
                        ERROR("Inconsistant program - cannot be scheduled without growing buffers infinitely!");
                    }
                }
            }
        }

        // normalize all the rates to be integers
        {
            BigInteger multiplier;

            if (joinRate != null)
            {
                multiplier = joinRate.getDenominator();
            }
            else
            {
                multiplier = BigInteger.ONE;
            }

            // find a factor to multiply all the fractional rates by
            {
                int index;
                for (index = 0; index < nChildren; index++)
                {
                    Fraction childRate = (Fraction) childrenRates[index];
                    ASSERT(childRate);

                    BigInteger rateDenom = childRate.getDenominator();
                    ASSERT(rateDenom);

                    BigInteger gcd = multiplier.gcd(rateDenom);
                    multiplier = multiplier.multiply(rateDenom).divide(gcd);
                }
            }

            // multiply all the rates by this factor and set the rates for
            // the children and splitter and joiner
            {
                if (splitRate != null)
                {
                    splitRate = splitRate.multiply(multiplier);
                    ASSERT(splitRate.getDenominator().equals(BigInteger.ONE));
                    splitNumRounds = splitRate.getNumerator();
                }
                else
                {
                    splitNumRounds = BigInteger.ZERO;
                }

                if (joinRate != null)
                {
                    joinRate = joinRate.multiply(multiplier);
                    ASSERT(joinRate.getDenominator().equals(BigInteger.ONE));
                    joinNumRounds = joinRate.getNumerator();
                }
                else
                {
                    joinNumRounds = BigInteger.ZERO;
                }

                // normalize the children's rates and store them in
                // childrenNumExecs
                {
                    childrenNumExecs = new BigInteger[nChildren];

                    int nChild;
                    for (nChild = 0; nChild < nChildren; nChild++)
                    {
                        Fraction childRate = (Fraction) childrenRates[nChild];
                        ASSERT(childRate);

                        Fraction newChildRate =
                            childRate.multiply(multiplier);
                        ASSERT(
                            newChildRate.getDenominator().equals(
                                BigInteger.ONE));

                        // set the rate
                        childrenNumExecs[nChild] =
                            newChildRate.getNumerator();

                        // make sure that the child executes a positive
                        // number of times!
                        ASSERT(childrenNumExecs[nChild].signum() == 1);
                    }
                }
            }
        }

        // setup my variables that come for Stream:
        {
            int pop =
                splitNumRounds.intValue()
                    * getSteadySplitFlow().getPopWeight();
            int push =
                joinNumRounds.intValue()
                    * getSteadyJoinFlow().getPushWeight();

            setSteadyPeek(pop);
            setSteadyPop(pop);
            setSteadyPush(push);
        }
    }
    
    public int getNumNodes () 
    { 
        int nodes = 0;
        for (int nChild = 0; nChild < nChildren; nChild++)
        {
            StreamInterface child = children[nChild];
            ASSERT(child);
            
            nodes += child.getNumNodes ();
        }
        if (getSplitNumRounds () > 0) nodes++;
        if (getJoinNumRounds () > 0) nodes++;
        return nodes;
    }
    
    public int getNumNodeFirings() 
    {
        int firings = 0;
        for (int nChild = 0; nChild < nChildren; nChild++)
        {
            StreamInterface child = children[nChild];
            ASSERT(child);
            
            firings += child.getNumNodeFirings () * getChildNumExecs(nChild);
        }
        firings += getSplitNumRounds();
        firings += getJoinNumRounds();
        
        return firings;
    }
}