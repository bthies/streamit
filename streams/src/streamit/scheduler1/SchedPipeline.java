package streamit.scheduler;

import java.util.*;
import java.math.BigInteger;
import streamit.*;

public class SchedPipeline extends SchedStream
{
    protected SchedPipeline (Object stream)
    {
        super (stream);
    }

    final List allChildren = new LinkedList ();

    public void addChild (SchedStream stream)
    {
        ASSERT (stream);
        boolean result = allChildren.add (stream);

        ASSERT (result);
    }

    public List getChildren ()
    {
        return allChildren;
    }

    public void computeSteadySchedule ()
    {
        // go through all the children and get them initialized
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

        // multiply the children's num of executions
        // by the appropriate output factors
        {
            // use this to keep track of product of all output rates
            BigInteger outProduct = BigInteger.ONE;

            ListIterator iter;
            iter = allChildren.listIterator ();

            while (iter.hasNext ())
            {
                SchedStream child = (SchedStream) iter.next ();
                ASSERT (child);

                // and start computing the number of execution this child needs
                child.multNumExecutions (outProduct);

                BigInteger currentOut = BigInteger.valueOf (child.getProduction ());
                outProduct = outProduct.multiply (currentOut);
            }
        }

        // now multiply the children's num of executions
        // by the appropriate input factors
        {
            // use this value to keep track of product of input rates
            BigInteger inProduct = BigInteger.ONE;

            ListIterator iter;
            iter = allChildren.listIterator (allChildren.size ());

            while (iter.hasPrevious ())
            {
                SchedStream child = (SchedStream) iter.previous ();
                ASSERT (child);

                // continue computing the number of executions this child needs
                child.multNumExecutions (inProduct);

                BigInteger currentIn = BigInteger.valueOf (child.getConsumption ());
                inProduct = inProduct.multiply (currentIn);
            }
        }

        // compute the GCD of number of executions of the children
        // and didivde those numbers by the GCD
        BigInteger gcd = null;

        {
            ListIterator iter;
            iter = allChildren.listIterator ();

            while (iter.hasNext ())
            {
                SchedStream child = (SchedStream) iter.next ();
                ASSERT (child);

                if (gcd == null)
                {
                    gcd = child.getNumExecutions ();
                } else {
                    gcd = gcd.gcd (child.getNumExecutions ());
                }
            }
        }

        // divide the children's execution counts by the gcd
        if (gcd != null)
        {
            ListIterator iter;
            iter = allChildren.listIterator ();

            while (iter.hasNext ())
            {
                SchedStream child = (SchedStream) iter.next ();
                ASSERT (child);
                ASSERT (child.getNumExecutions ().mod (gcd).equals (BigInteger.ZERO));

                child.divNumExecutions (gcd);
            }
        }

        // initialize self
        {
            setNumExecutions (BigInteger.ONE);
            ListIterator iter;

            // get the numer of items read by the first stream:
            iter = allChildren.listIterator ();
            if (iter.hasNext ())
            {
                SchedStream child = (SchedStream) iter.next ();
                ASSERT (child);

                setConsumption (child.getConsumption () * (child.getNumExecutions ().intValue ()));
            }

            // get the numer of items produced by the last stream:
            iter = allChildren.listIterator (allChildren.size ());
            if (iter.hasPrevious ())
            {
                SchedStream child = (SchedStream) iter.previous ();
                ASSERT (child);

                setProduction (child.getProduction () * (child.getNumExecutions ().intValue ()));
            }
        }
    }
}
