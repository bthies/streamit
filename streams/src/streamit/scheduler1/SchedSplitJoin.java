package streamit.scheduler;

import java.util.*;
import java.math.BigInteger;
import streamit.scheduler.*;

public class SchedSplitJoin extends SchedStream
{
    final List allChildren = new LinkedList ();

    public void addChild (SchedStream stream)
    {
        ASSERT (stream);
        boolean result = allChildren.add (stream);

        ASSERT (result);
    }

    SchedSplitType splitType;
    SchedJoinType joinType;

    BigInteger numSplitExecutions, numJoinExecutions;

    public void setSplitType (SchedSplitType type)
    {
        splitType = type;
    }

    public void setJoinType (SchedJoinType type)
    {
        joinType = type;
    }

    void computeSteadySchedule ()
    {
        // first compute schedules for all my children:
        {
            ListIterator iter;
            iter = allChildren.listIterator ();

            while (iter.hasNext ())
            {
                SchedStream child = (SchedStream) iter.next ();
                ASSERT (child);

                // get the child initialized
                child.computeSteadySchedule ();
            }
        }

        // BUGBUG:
        // this code might not work if the splitter or joiner is null!

        List childrenRates = new ArrayList ();
        Fraction splitRate = new Fraction (1, 1);
        Fraction joinRate = null;

        // now go through all children and calculate the rates at which
        // they will be called w.r.t. the splitter
        // also calculate and check the rate of execution of the joiner!
        {
            ListIterator iter;
            iter = allChildren.listIterator ();

            int index = -1;
            while (iter.hasNext ())
            {
                SchedStream child = (SchedStream) iter.next ();
                ASSERT (child);
                index++;

                Fraction childRate;

                // rates for children
                {
                    int numOut = splitType.getOutputWeight (index);
                    int numIn = child.getConsumption ();

                    childRate = new Fraction (numOut, numIn).reduce ();
                    childrenRates.add (childRate);
                }

                // rate for joiner
                {
                    int numOut = child.getProduction ();
                    int numIn = joinType.getInputWeight (index);

                    if (numIn != 0 && numOut != 0)
                    {
                        // compute new rate
                        Fraction newJoinRate = new Fraction (numOut, numIn).multiply (childRate);
                        if (joinRate == null)
                        {
                            joinRate = newJoinRate;
                        }

                        // make sure that the rate is same as previous rate
                        if (!joinRate.equals (newJoinRate))
                        {
                            ERROR ("Inconsistant program - cannot be scheduled without growing buffers infinitely!");
                        }
                    }
                }
            }
        }

        // now normalize all the rates to be integers
        {
            BigInteger multiplier = joinRate.getDenominator ();

            // find a factor to multiply all the fractional rates by
            {
                int index;
                for (index = 0; index < childrenRates.size (); index++)
                {
                    Fraction childRate = (Fraction) childrenRates.get (index);
                    ASSERT (childRate);

                    BigInteger rateDenom = childRate.getDenominator ();
                    ASSERT (rateDenom);

                    BigInteger gcd = multiplier.gcd (rateDenom);
                    multiplier = multiplier.multiply (rateDenom).divide (gcd);
                }
            }

            // multiply all the rates by this factor and set the rates for
            // the children and splitter and joiner
            {
                splitRate = splitRate.multiply (multiplier);
                joinRate = joinRate.multiply (multiplier);
                ASSERT (splitRate.getDenominator ().equals (BigInteger.ONE));
                ASSERT (joinRate.getDenominator ().equals (BigInteger.ONE));

                numSplitExecutions = splitRate.getNumerator ();
                numJoinExecutions = joinRate.getNumerator ();

                // handle the children
                {
                    ListIterator iter;
                    iter = allChildren.listIterator ();

                    int index;
                    for (index = 0; index < childrenRates.size (); index++)
                    {
                        Fraction childRate = (Fraction) childrenRates.get (index);
                        ASSERT (childRate);

                        Fraction newChildRate = childRate.multiply (multiplier);
                        ASSERT (newChildRate.getDenominator ().equals (BigInteger.ONE));

                        // set the rate
                        SchedStream child = (SchedStream) iter.next ();
                        ASSERT (child);

                        child.setNumExecutions (newChildRate.getNumerator ());
                    }
                }
            }
        }

        // setup my variables that come from SchedStream:
        {
            setNumExecutions (BigInteger.ONE);

            setConsumption (numSplitExecutions.intValue () * splitType.getRoundConsumption ());
            setProduction (numJoinExecutions.intValue () * joinType.getRoundProduction ());
        }

        // done
    }
}

