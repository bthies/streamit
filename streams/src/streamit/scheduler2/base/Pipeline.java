package streamit.scheduler.base;

import streamit.scheduler.iriter.PipelineIter;
import java.math.BigInteger;

/* $Id: Pipeline.java,v 1.1 2002-05-27 03:18:50 karczma Exp $ */

/**
 * Computes some basic data for Pipelines.  
 *
 * @version 2
 * @author  Michal Karczmarek
 */

public class Pipeline extends Stream
{
    PipelineIter pipeline;

    int nChildren;
    StreamInterface[] children;

    Pipeline(PipelineIter _pipeline, StreamFactory factory)
    {
        ASSERT(_pipeline);
        pipeline = _pipeline;

        // fill up the children array
        // do this to preseve consistancy of iterators
        {
            nChildren = pipeline.getNumChildren();
            
            // a pipeline must have some children
            ASSERT (nChildren > 0);
            
            children = new StreamInterface[nChildren];

            int nChild;
            for (nChild = 0; nChild < pipeline.getNumChildren(); nChild++)
            {
                children[nChild] = factory.newFrom(pipeline.getChild(nChild));
            }
        }
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
        // not tested yet.
        ASSERT (false);
        
        // compute the steady state data for my children
        {
            int nChild;
            for (nChild = 0; nChild < nChildren; nChild++)
            {
                children [nChild].computeSteadyState ();
            }
        }
        
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

                outProduct = outProduct.multiply(BigInteger.valueOf(child.getSteadyPush ()));
            }
        }

        // now multiply the children's num of executions
        // by the appropriate input factors
        {
            // use this value to keep track of product of input rates
            BigInteger inProduct = BigInteger.ONE;

            int nChild;
            for (nChild = nChildren - 1; nChild >= 0; nChild++)
            {
                StreamInterface child = children [nChild];
                ASSERT (child);

                // continue computing the number of executions this child needs
                childrenNumExecs [nChild] = childrenNumExecs [nChild].multiply (inProduct);

                inProduct = inProduct.multiply(BigInteger.valueOf(child.getSteadyPop ()));
            }
        }

        // compute the GCD of number of executions of the children
        // and didivde those numbers by the GCD
        BigInteger gcd;

        {
            gcd = childrenNumExecs [0];
            
            int nChild;
            for (nChild = 0; nChild < nChildren; nChild++)
            {
                gcd = gcd.gcd(childrenNumExecs [nChild]);
            }
        }

        // divide the children's execution counts by the gcd
        {
            int nChild;
            for (nChild = 0; nChild < nChildren; nChild++)
            {
                StreamInterface child = children [nChild];
                ASSERT(child);
                ASSERT(childrenNumExecs [nChild].mod(gcd).equals(BigInteger.ZERO));

                childrenNumExecs [nChild].divide(gcd);
            }
        }

        // initialize self
        {
            int peekExtra = children [0].getSteadyPeek () - children [0].getSteadyPop ();
            int pop = childrenNumExecs [0].intValue () * children [0].getSteadyPop ();
            int push = childrenNumExecs [nChildren - 1].intValue () * children [nChildren - 1].getSteadyPush ();
            
            setSteadyPeek (pop + peekExtra);
            setSteadyPop (pop);
            setSteadyPush (push);
        }
    }
}
