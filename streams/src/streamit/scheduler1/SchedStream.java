package streamit.scheduler;

import java.util.*;
import java.math.BigInteger;
import streamit.*;

public class SchedStream extends AssertedClass
{
    int consumes, produces, peeks;

    public void setProduction (int p)
    {
        ASSERT (p >= 0);
        produces = p;
    }

    int getProduction ()
    {
        return produces;
    }

    public void setConsumption (int c)
    {
        ASSERT (c >= 0);
        consumes = c;
    }

    int getConsumption ()
    {
        return consumes;
    }

    SchedStream parent;

    final List allChildren = new LinkedList ();

    public void addChild (SchedStream stream)
    {
        ASSERT (stream);
        boolean result = allChildren.add (stream);

        ASSERT (result);
    }

    // This section computes a steady-state schedule for children of the stream
    BigInteger numExecutions;

    BigInteger getNumExecutions ()
    {
        return numExecutions;
    }

    void setNumExecutions (BigInteger n)
    {
        numExecutions = n;
    }

    void multNumExecutions (BigInteger mult)
    {
        // make sure that mutliplying by something > 0
        String str = mult.toString();
        ASSERT (mult.compareTo (BigInteger.ZERO) == 1);

        numExecutions = numExecutions.multiply (mult);
    }

    void divNumExecutions (BigInteger div)
    {
        // make sure that dividing by something > 0
        ASSERT (div.compareTo (BigInteger.ZERO) == 1);
        ASSERT (numExecutions.mod (div).equals (BigInteger.ZERO));

        numExecutions = numExecutions.divide (div);
    }

    void computeSchedule ()
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
                child.computeSchedule ();
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
            numExecutions = BigInteger.ONE;
            ListIterator iter;

            // get the numer of items read by the first stream:
            iter = allChildren.listIterator ();
            if (iter.hasNext ())
            {
                SchedStream child = (SchedStream) iter.next ();
                ASSERT (child);

                consumes = child.getConsumption () * (child.getNumExecutions ().intValue ());
            }

            // get the numer of items produced by the last stream:
            iter = allChildren.listIterator (allChildren.size ());
            if (iter.hasPrevious ())
            {
                SchedStream child = (SchedStream) iter.previous ();
                ASSERT (child);

                consumes = child.getProduction () * (child.getNumExecutions ().intValue ());
            }
        }
    }
}
