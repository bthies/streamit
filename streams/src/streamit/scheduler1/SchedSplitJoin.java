package streamit.scheduler;

import java.util.*;
import java.math.BigInteger;
import java.io.PrintStream;
import streamit.scheduler.*;

public class SchedSplitJoin extends SchedStream
{
    protected SchedSplitJoin (Object stream)
    {
        super (stream);
    }

    final List allChildren = new LinkedList ();

    public List getChildren ()
    {
        return allChildren;
    }

    public SchedStream getChild (int nChild)
    {
        ASSERT (nChild < getChildren ().size ());
        Object child = getChildren ().get (nChild);

        ASSERT (child instanceof SchedStream);
        return (SchedStream) child;
    }

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
        ASSERT (type);
        splitType = type;
    }

    public SchedSplitType getSplitType ()
    {
        ASSERT (splitType);
        return splitType;
    }

    public void setJoinType (SchedJoinType type)
    {
        ASSERT (type);
        joinType = type;
    }

    public SchedJoinType getJoinType ()
    {
        ASSERT (joinType);
        return joinType;
    }

    public BigInteger getNumSplitExecutions ()
    {
        ASSERT (numSplitExecutions);
        return numSplitExecutions;
    }

    public BigInteger getNumJoinExecutions ()
    {
        ASSERT (numJoinExecutions);
        return numJoinExecutions;
    }

    public void computeSteadySchedule ()
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
        Fraction splitRate = null;
        Fraction joinRate = null;

        // now go through all children and calculate the rates at which
        // they will be called w.r.t. the splitter
        // also, compute the rate of execution of the joiner
        // (if it ever ends up being executed)
        {
            ListIterator iter;
            iter = allChildren.listIterator ();
            int index = -1;

            while (iter.hasNext ())
            {
                SchedStream child = (SchedStream) iter.next ();
                ASSERT (child);
                index++;

                // the rate at which the child should be executed
                Fraction childRate = null;

                // rates at which the splitter is producing the data
                // and the child is consuming it:
                int numOut = splitType.getOutputWeight (index);
                int numIn = child.getConsumption ();

                // is the splitter actually producing any data?
                if (numOut != 0)
                {
                    // if the slitter is producing data, the child better
                    // be consuming it!
                    ASSERT (numIn);

                    if (splitRate == null)
                    {
                        // if I hadn't set the split rate yet, do it now
                        splitRate = new Fraction (BigInteger.ONE, BigInteger.ONE);
                    }

                    // compute the rate at which the child should be executing
                    // (relative to the splitter)
                    childRate = new Fraction (numOut, numIn).multiply (splitRate).reduce ();

                    // if I still hadn't computed the rate at which the joiner
                    // is executed, try to compute it:
                    if (joinRate == null && child.getProduction () != 0)
                    {
                        // if the child is producing data, the joiner
                        // better be consuming it!
                        ASSERT (joinType.getInputWeight (index) != 0);

                        int childOut = child.getProduction ();
                        int joinIn = joinType.getInputWeight (index);

                        joinRate = new Fraction (childOut, joinIn).multiply (childRate).reduce ();
                    }
                }

                childrenRates.add (childRate);
            }
        }

        // now compute the rate of execution of the joiner w.r.t. children
        // and make sure that everything will be executed at consistant
        // rates (to avoid overflowing the buffers)
        {
            // if the splitter never needs to get executed (doesn't produce
            // any data), the joiner rate should be set to ONE:
            if (splitRate == null)
            {
                // I better not have computed the join rate yet!
                ASSERT (joinRate == null);

                // okay, just set it to ONE/ONE
                joinRate = new Fraction (BigInteger.ONE, BigInteger.ONE);
            }

            ListIterator iter;
            iter = allChildren.listIterator ();

            int index = -1;
            while (iter.hasNext ())
            {
                SchedStream child = (SchedStream) iter.next ();
                ASSERT (child);
                index++;

                // get the child rate
                Fraction childRate = (Fraction) childrenRates.get (index);

                // compute the new childRate:
                Fraction newChildRate = null;
                {
                    int childOut = child.getProduction ();
                    int joinIn = joinType.getInputWeight (index);

                    // does the child produce any data?
                    if (childOut != 0)
                    {
                        // yes
                        // the split better consume some data too!
                        ASSERT (joinIn != 0);

                        // compute the rate at which the child should execute
                        // w.r.t. the splitter
                        newChildRate = new Fraction (joinIn, childOut).multiply (joinRate).reduce ();
                    } else {
                        // no
                        // the splitter better not consume any data either
                        ASSERT (joinIn == 0);
                    }
                }

                // if this is a new rate, put it in the array
                if (childRate == null)
                {
                    // I better have the rate here, or the child
                    // neither produces nor consumes any data!
                    ASSERT (newChildRate != null);

                    // set the rate
                    childrenRates.set (index, newChildRate);
                }

                // okay, if I have both rates, make sure that they agree!
                if (childRate != null && newChildRate != null)
                {
                    if (!childRate.equals (newChildRate))
                    {
                        ERROR ("Inconsistant program - cannot be scheduled without growing buffers infinitely!");
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

    void printDot (PrintStream outputStream)
    {

        // Create a subgraph again...
        print("subgraph cluster_" + getUniqueStreamName () + " {\n", outputStream);
        print("label = \"" + getStreamName () + "\";\n", outputStream);

        // Visit the splitter and joiner to get their node names...
        getSplitType ().printDot (outputStream);
        getJoinType ().printDot (outputStream);

        String splitName = getSplitType ().getUniqueStreamName ();
        String joinName = getJoinType ().getUniqueStreamName ();

        // ...and walk through the body.
        Iterator iter = allChildren.iterator();
        while (iter.hasNext())
        {
            SchedObject oper = (SchedObject)iter.next();
            oper.printDot (outputStream);

            printEdge(splitName, oper.getFirstChild ().getUniqueStreamName (), outputStream);
            printEdge(oper.getLastChild ().getUniqueStreamName (), joinName, outputStream);
        }

        print("}\n", outputStream);
    }

    SchedObject getFirstChild ()
    {
        return getSplitType ().getFirstChild ();
    }

    SchedObject getLastChild ()
    {
        return getJoinType ().getLastChild ();
    }
}

