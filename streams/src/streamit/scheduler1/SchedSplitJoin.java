package streamit.scheduler1;

import java.util.*;
import java.math.BigInteger;
import java.io.PrintStream;

import streamit.misc.Fraction;

public class SchedSplitJoin extends SchedStream
{
    protected SchedSplitJoin (Object stream)
    {
        super (stream);
    }

    final List<SchedStream> allChildren = new LinkedList<SchedStream> ();

    public List<SchedStream> getChildren ()
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
        
        // make sure that you have at most one NULL join and split
        // if both are NULL then the children are disjoint and the
        // scheduler won't be able to handle this anyway
        ASSERT (joinType == null || (joinType.type != SchedJoinType.NULL || splitType.type != SchedSplitType.NULL));
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
        
        // make sure that you have at most one NULL join and split
        // if both are NULL then the children are disjoint and the
        // scheduler won't be able to handle this anyway
        ASSERT (splitType == null || (joinType.type != SchedJoinType.NULL || splitType.type != SchedSplitType.NULL));
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
            ListIterator<SchedStream> iter;
            iter = allChildren.listIterator ();

            while (iter.hasNext ())
                {
                    SchedStream child = iter.next ();
                    ASSERT (child);

                    // get the child initialized
                    child.computeSteadySchedule ();
                }
        }

        // make sure that the splitter and joiner have the same
        // number of weights as the splitjoin has children:
        ASSERT (allChildren.size () == getSplitType ().getNumWeights ());
        ASSERT (allChildren.size () == getJoinType ().getNumWeights ());

        List<Fraction> childrenRates = new ArrayList<Fraction> ();
        Fraction splitRate = null;
        Fraction joinRate = null;

        // now go through all children and calculate the rates at which
        // they will be called w.r.t. the splitter
        // also, compute the rate of execution of the joiner
        // (if it ever ends up being executed)
        {
            ListIterator<SchedStream> iter;
            iter = allChildren.listIterator ();
            int index = -1;

            while (iter.hasNext ())
                {
                    SchedStream child = iter.next ();
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

            ListIterator<SchedStream> iter;
            iter = allChildren.listIterator ();

            int index = -1;
            while (iter.hasNext ())
                {
                    SchedStream child = iter.next ();
                    ASSERT (child);
                    index++;

                    // get the child rate
                    Fraction childRate = childrenRates.get (index);

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
                                ASSERT (joinIn == 0, "Child " + child + " (splitWeight " + childRate + ") does not produce data, but splitter consumes " + joinIn + " data");
                            }
                    }

                    // if this is a new rate, put it in the array
                    if (childRate == null)
                        {
                            // I better have the rate here, or the child
                            // neither produces nor consumes any data!
                            ASSERT (newChildRate != null, "Child " + child 
                                    + " neither produces nor consumes any data!");

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
            BigInteger multiplier;
            
            if (joinRate != null)
                {
                    multiplier = joinRate.getDenominator ();
                } else {
                    multiplier = BigInteger.ONE;
                }

            // find a factor to multiply all the fractional rates by
            {
                int index;
                for (index = 0; index < childrenRates.size (); index++)
                    {
                        Fraction childRate = childrenRates.get (index);
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
                if (splitRate != null)
                    {
                        splitRate = splitRate.multiply (multiplier);
                        ASSERT (splitRate.getDenominator ().equals (BigInteger.ONE));
                        numSplitExecutions = splitRate.getNumerator ();
                    } else
                        {
                            numSplitExecutions = BigInteger.ZERO;
                        }
                
                if (joinRate != null)
                    {
                        joinRate = joinRate.multiply (multiplier);
                        ASSERT (joinRate.getDenominator ().equals (BigInteger.ONE));
                        numJoinExecutions = joinRate.getNumerator ();
                    } else 
                        {
                            numJoinExecutions = BigInteger.ZERO;
                        }
                
                // handle the children
                {
                    ListIterator<SchedStream> iter;
                    iter = allChildren.listIterator ();

                    int index;
                    for (index = 0; index < childrenRates.size (); index++)
                        {
                            Fraction childRate = childrenRates.get (index);
                            ASSERT (childRate);

                            Fraction newChildRate = childRate.multiply (multiplier);
                            ASSERT (newChildRate.getDenominator ().equals (BigInteger.ONE));

                            // set the rate
                            SchedStream child = iter.next ();
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
        Iterator<SchedStream> iter = allChildren.iterator();
        while (iter.hasNext())
            {
                SchedObject oper = iter.next();
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

