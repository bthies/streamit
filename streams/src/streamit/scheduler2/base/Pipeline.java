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

import streamit.scheduler2.iriter./*persistent.*/PipelineIter;
import java.math.BigInteger;

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
        super(_pipeline.getUnspecializedIter());
        
        assert _pipeline != null;
        pipeline = _pipeline;

        // fill up the children array
        // do this to preseve consistancy of iterators
        {
            nChildren = pipeline.getNumChildren();

            // a pipeline must have some children
            assert nChildren > 0;

            children = new StreamInterface[nChildren];

            int nChild;
            for (nChild = 0; nChild < pipeline.getNumChildren(); nChild++)
            {
                // create a new child object
                children[nChild] =
                    factory.newFrom(pipeline.getChild(nChild), pipeline.getUnspecializedIter());
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
        assert nChild >= 0 && nChild < nChildren;
        return children[nChild];
    }

    private BigInteger childrenNumExecs[];

    /**
     * Return how many times a particular child should be executed in
     * a full steady-state execution of this pipeline.
     * @return number of executions of a child in steady state
     */
    protected int getChildNumExecs(int nChild)
    {
        // make sure nChild is in range
        assert nChild >= 0 && nChild < getNumChildren();

        return childrenNumExecs[nChild].intValue();
    }

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
                assert child != null;

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
                assert child != null;

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
                assert child != null;
                assert childrenNumExecs[nChild].mod(gcd)
                    .equals(BigInteger.ZERO);

                childrenNumExecs[nChild] =
                    childrenNumExecs[nChild].divide(gcd);

                // make sure that the child executes a positive
                // number of times!
                assert childrenNumExecs[nChild].signum() == 1;
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


    public int getNumNodes () 
    { 
        int nodes = 0;
        for (int nChild = 0; nChild < nChildren; nChild++)
        {
            StreamInterface child = children[nChild];
            assert child != null;
            
            nodes += child.getNumNodes ();
        }
        return nodes;
    }
    
    public int getNumNodeFirings() 
    {
        int firings = 0;
        for (int nChild = 0; nChild < nChildren; nChild++)
        {
            StreamInterface child = children[nChild];
            assert child != null;
            
            firings += child.getNumNodeFirings () * getChildNumExecs(nChild);
        }
        return firings;
    }
}
