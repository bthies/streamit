package streamit.scheduler1;

import java.util.*;
import java.io.PrintStream;
import java.math.BigInteger;

public class SchedPipeline extends SchedStream
{
    protected SchedPipeline (Object stream)
    {
        super (stream);
    }

    final List<SchedStream> allChildren = new LinkedList<SchedStream> ();

    public void addChild (SchedStream stream)
    {
        ASSERT (stream);
        boolean result = allChildren.add (stream);

        ASSERT (result);
    }

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

    public void computeSteadySchedule ()
    {
        // go through all the children and get them initialized
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

        // multiply the children's num of executions
        // by the appropriate output factors
        {
            // use this to keep track of product of all output rates
            BigInteger outProduct = BigInteger.ONE;

            ListIterator<SchedStream> iter;
            iter = allChildren.listIterator ();

            while (iter.hasNext ())
                {
                    SchedStream child = iter.next ();
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

            ListIterator<SchedStream> iter;
            iter = allChildren.listIterator (allChildren.size ());

            while (iter.hasPrevious ())
                {
                    SchedStream child = iter.previous ();
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
            ListIterator<SchedStream> iter;
            iter = allChildren.listIterator ();

            while (iter.hasNext ())
                {
                    SchedStream child = iter.next ();
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
                ListIterator<SchedStream> iter;
                iter = allChildren.listIterator ();

                while (iter.hasNext ())
                    {
                        SchedStream child = iter.next ();
                        ASSERT (child);
                        ASSERT (child.getNumExecutions ().mod (gcd).equals (BigInteger.ZERO));

                        child.divNumExecutions (gcd);
                    }
            }

        // initialize self
        {
            setNumExecutions (BigInteger.ONE);
            ListIterator<SchedStream> iter;

            // get the numer of items read by the first stream:
            iter = allChildren.listIterator ();
            if (!allChildren.isEmpty ())
                {
                    SchedStream child = allChildren.get (0);
                    ASSERT (child);

                    setConsumption (child.getConsumption () * (child.getNumExecutions ().intValue ()));

                    int peekExtra = child.getPeekConsumption () - child.getConsumption ();
                    setPeekConsumption (peekExtra + child.getConsumption () * (child.getNumExecutions ().intValue ()));
                }

            // get the numer of items produced by the last stream:
            iter = allChildren.listIterator (allChildren.size ());
            if (iter.hasPrevious ())
                {
                    SchedStream child = iter.previous ();
                    ASSERT (child);

                    setProduction (child.getProduction () * (child.getNumExecutions ().intValue ()));
                }
        }
    }

    void printDot (PrintStream outputStream)
    {
        String lastPrinted = null;

        // Print this within a subgraph.
        print("subgraph cluster_" + getUniqueStreamName () + " {\n", outputStream);
        print("label = \"" + getStreamName () + "\";\n", outputStream);

        // Walk through each of the elements in the pipeline.
        Iterator<SchedStream> iter = allChildren.iterator();
        while (iter.hasNext())
            {
                SchedStream child = iter.next();
                ASSERT (child);

                child.printDot (outputStream);

                String topChild = child.getFirstChild ().getUniqueStreamName ();
                String bottomChild = child.getLastChild ().getUniqueStreamName ();

                printEdge(lastPrinted, topChild, outputStream);

                // Update the known edges.
                lastPrinted = bottomChild;
            }

        print("}\n", outputStream);
    }

    SchedObject getFirstChild ()
    {
        return allChildren.get (0).getFirstChild ();
    }

    SchedObject getLastChild ()
    {
        return allChildren.get (allChildren.size () - 1).getLastChild ();
    }
}
