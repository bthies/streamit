package streamit.scheduler.base;

import streamit.scheduler.iriter.PipelineIter;
import java.math.BigInteger;

/* $Id: Pipeline.java,v 1.3 2002-06-13 22:43:25 karczma Exp $ */

/**
 * Computes some basic data for Pipelines.  
 *
 * @version 2
 * @author  Michal Karczmarek
 */

abstract public class Pipeline extends Stream
{
    private PipelineIter pipeline;

    private int nChildren;
    private StreamInterface[] children;

    protected Pipeline(PipelineIter _pipeline, StreamFactory factory)
    {
        ASSERT(_pipeline);
        pipeline = _pipeline;

        // fill up the children array
        // do this to preseve consistancy of iterators
        {
            nChildren = pipeline.getNumChildren();

            // a pipeline must have some children
            ASSERT(nChildren > 0);

            children = new StreamInterface[nChildren];

            int nChild;
            for (nChild = 0; nChild < pipeline.getNumChildren(); nChild++)
            {
                // create a new child object
                children[nChild] =
                    factory.newFrom(pipeline.getChild(nChild));
            }
            
            // compute my steady schedule
            // my children already have computed their steady schedules,
            // so I just have to do mine
            computeSteadyState();
        }
    }

    /**
     * Get the number of children this Pipeline has.
     * @return number of children
     */
    protected int getNumChildren()
    {
        return nChildren;
    }

    /**
     * Get the a child of this pipeline.
     * @return nth child
     */
    protected StreamInterface getChild(int nChild)
    {
        ASSERT(nChild >= 0 && nChild < nChildren);
        return children[nChild];
    }

    private BigInteger childrenNumExecs[];

    /**
     * Compute the number of times each child needs to execute
     * for the entire pipeline to execute a minimal full steady
     * state execution.
     * 
     * This function is essentially copied from the old scheduler.
     */
    public void computeSteadyState()
    {
        childrenNumExecs = new BigInteger[nChildren];

        // multiply the children's num of executions
        // by the appropriate output factors
        {
            // use this to keep track of product of all output rates
            BigInteger outProduct = BigInteger.ONE;

            int nChild;
            for (nChild = 0; nChild < nChildren; nChild++)
            {
                StreamInterface child = children[nChild];
                ASSERT(child);

                childrenNumExecs[nChild] = outProduct;

                outProduct =
                    outProduct.multiply(
                        BigInteger.valueOf(child.getSteadyPush()));
            }
        }

        // now multiply the children's num of executions
        // by the appropriate input factors
        {
            // use this value to keep track of product of input rates
            BigInteger inProduct = BigInteger.ONE;

            int nChild;
            for (nChild = nChildren - 1; nChild >= 0; nChild--)
            {
                StreamInterface child = children[nChild];
                ASSERT(child);

                // continue computing the number of executions this child needs
                childrenNumExecs[nChild] =
                    childrenNumExecs[nChild].multiply(inProduct);

                inProduct =
                    inProduct.multiply(
                        BigInteger.valueOf(child.getSteadyPop()));
            }
        }

        // compute the GCD of number of executions of the children
        // and didivde those numbers by the GCD
        BigInteger gcd;

        {
            gcd = childrenNumExecs[0];

            int nChild;
            for (nChild = 0; nChild < nChildren; nChild++)
            {
                gcd = gcd.gcd(childrenNumExecs[nChild]);
            }
        }

        // divide the children's execution counts by the gcd
        {
            int nChild;
            for (nChild = 0; nChild < nChildren; nChild++)
            {
                StreamInterface child = children[nChild];
                ASSERT(child);
                ASSERT(
                    childrenNumExecs[nChild].mod(gcd).equals(
                        BigInteger.ZERO));

                childrenNumExecs[nChild] =
                    childrenNumExecs[nChild].divide(gcd);
            }
        }

        // initialize self
        {
            int peekExtra =
                children[0].getSteadyPeek() - children[0].getSteadyPop();
            int pop =
                childrenNumExecs[0].intValue()
                    * children[0].getSteadyPop();
            int push =
                childrenNumExecs[nChildren
                    - 1].intValue() * children[nChildren
                    - 1].getSteadyPush();

            setSteadyPeek(pop + peekExtra);
            setSteadyPop(pop);
            setSteadyPush(push);
        }
    }

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
}
