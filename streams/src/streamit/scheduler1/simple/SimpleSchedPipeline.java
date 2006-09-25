package streamit.scheduler1.simple;

import streamit.scheduler1.SchedPipeline;
import streamit.scheduler1.SchedRepSchedule;
import streamit.scheduler1.SchedStream;

import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.HashMap;
import java.math.BigInteger;

public class SimpleSchedPipeline extends SchedPipeline implements SimpleSchedStream
{
    final SimpleHierarchicalScheduler scheduler;
    private List<SchedRepSchedule> steadySchedule = null;
    private List<Object> initSchedule = null;
    private int initDataConsumption = -1;
    private int initDataProduction = -1;

    SimpleSchedPipeline (SimpleHierarchicalScheduler scheduler, Object stream)
    {
        super (stream);

        ASSERT (scheduler);
        this.scheduler = scheduler;
    }

    public void computeSchedule ()
    {
        // make sure that this the first (and thus only) call to computeSchedule
        {
            ASSERT (steadySchedule == null && initSchedule == null);
            steadySchedule = new LinkedList<SchedRepSchedule> ();
            initSchedule = new LinkedList<Object> ();
        }

        List<SchedStream> children = getChildren ();

        // first go through all children and compute their schedules
        // this will have the effect of computing the initialization
        // schedules for all the children - after we'll be able to use
        // the initDataCount to figure out how many data each child needs
        // for its own initialization, and thus how many data we need to feed
        // the child.
        {
            ListIterator<SchedStream> iter = children.listIterator ();
            while (iter.hasNext ())
                {
                    SimpleSchedStream child = (SimpleSchedStream) iter.next ();
                    child.computeSchedule ();
                }
        }

        // now go through all the children again and figure out how many
        // data each child needs to be executed to fill up the buffers
        // enough to initialize the entire pipeline without producing
        // any data - this means that the last child doesn't get executed
        // even once
        // I do this by iterating from end to beggining and computing the
        // number of elements that each stream needs to produce
        Map<SimpleSchedStream, Integer> numExecutionsForInit = new HashMap<SimpleSchedStream, Integer> ();
        {
            int consumedByNext = 0;

            // this is silly - I need to iterate from end to beginning:
            ListIterator<SchedStream> iter = children.listIterator (children.size ());

            while (iter.hasPrevious ())
                {
                    // get the child
                    SimpleSchedStream child = (SimpleSchedStream) iter.previous ();
                    ASSERT (child);

                    // now figure out how many times this child needs to be run
                    int producesForInit = child.getInitDataProduction ();
                    int producesPerIter = child.getProduction ();
                    int numItersInit;
                    if (producesPerIter != 0)
                        {
                            // this stream actually produces some data - this is
                            // the common case
                            numItersInit = ((consumedByNext - producesForInit) + producesPerIter - 1) / producesPerIter;
                            if (numItersInit < 0) numItersInit = 0;
                        } else {
                            // this stream does not produce any data
                            // make sure that consumedByPrev is 0 (otherwise
                            // I cannot execute the next child, 'cause it will
                            // never get any input)
                            ASSERT (consumedByNext == 0);

                            // there will be no cycles executed for initialization
                            // of children downstream
                            numItersInit = 0;
                        }

                    // associate the child with the number of executions required
                    // by the child to fill up the pipeline
                    numExecutionsForInit.put (child, new Integer (numItersInit));

                    // and figure out how many data this particular child
                    // needs to initialize the pipeline;  that is:
                    //  + number of iters * consumption to fill up the pipeline
                    //  + number of data needed to initialize this particular child
                    //  + (peek - pop)
                    consumedByNext = numItersInit * child.getConsumption () +
                        (child.getPeekConsumption () - child.getConsumption ()) +
                        child.getInitDataConsumption ();
                }
        }

        // compute an initialization schedule
        // now that I know how many times each child needs to be executed,
        // it should be easy to compute this - just execute each stream
        // an appropriate number of times.
        // In this approach, I do not care about the size of the buffers, and
        // may in fact be enlarging them much more than necessary
        {
            ListIterator<SchedStream> iter = children.listIterator ();

            while (iter.hasNext ())
                {
                    SimpleSchedStream child = (SimpleSchedStream) iter.next ();
                    ASSERT (child);

                    // add the initialization schedule:
                    if (child.getInitSchedule () != null)
                        {
                            initSchedule.add (child.getInitSchedule ());
                        }

                    // add the steady schedule an appropriate number of times:
                    Integer numExecutions = numExecutionsForInit.get (child);
                    ASSERT (numExecutions);

                    if (numExecutions.intValue () > 0)
                        {
                            initSchedule.add (new SchedRepSchedule (BigInteger.valueOf (numExecutions.intValue ()), child.getSteadySchedule ()));
                        }
                }
        }

        // compute the amount of data consumed and produced by initialization
        // this is easy, 'cause I already know how many times my first child
        // will need to get executed, and how much data it consumes on its
        // own initialization
        if (!children.isEmpty ())
            {
                // do consumption first
                {
                    // get my first child
                    SimpleSchedStream firstChild = (SimpleSchedStream) children.get (0);
                    ASSERT (firstChild);

                    // now get the amount of data pulled when initializing
                    int initDataConsumed = firstChild.getInitDataConsumption ();

                    // and the amount of data pulled when filling the pipeline
                    int fillData = numExecutionsForInit.get (firstChild).intValue () *
                        firstChild.getConsumption ();

                    // and thus I have the amount of data need to initialize:
                    initDataConsumption = initDataConsumed + fillData;
                }

                // now do production
                {
                    // get my last child
                    SimpleSchedStream lastChild = (SimpleSchedStream) children.get (children.size () - 1);
                    ASSERT (lastChild);

                    // the amount of data produced is simlpy the amount of data
                    // produced by my last child...
                    initDataProduction = lastChild.getInitDataProduction ();
                }
            } else {
                initDataConsumption = 0;
                initDataProduction = 0;
            }

        // compute the steady state schedule
        // this one is quite easy - I go through the children and execute them
        // an appropriate number of times
        {
            ListIterator<SchedStream> iter = children.listIterator ();

            while (iter.hasNext ())
                {
                    SimpleSchedStream child = (SimpleSchedStream) iter.next ();
                    ASSERT (child);

                    // in steady state processing, every child should be executed
                    // at least once!
                    steadySchedule.add (new SchedRepSchedule (child.getNumExecutions (), child.getSteadySchedule ()));
                }

        }

        // now I just need to compute the buffer sizes and I'm DONE!
        // this should be done by getting max of:
        //  - the amount of data produced during steady state execution
        //    added to amount of data left in the buffer after initialization
        //  - the amount of data left in a buffer after running the producer
        //    during initialization
        // I don't need to worry about peeking explicitly, 'cause it is already
        // implicitly taken care of in the intialization!
        // note: I may want to keep the buffer either n*steadyProduction or 2^n
        if (!children.isEmpty ())
            {
                ListIterator<SchedStream> iter = children.listIterator ();
                SimpleSchedStream prevChild = (SimpleSchedStream) iter.next ();
                ASSERT (prevChild);

                while (iter.hasNext ())
                    {
                        SimpleSchedStream child = (SimpleSchedStream) iter.next ();
                        ASSERT (child);

                        // compute the amount of data consumed (produced)
                        // during steady state execution
                        BigInteger steadyStateProd = child.getNumExecutions ().multiply (BigInteger.valueOf (child.getConsumption ()));

                        // compute the amount of data produced during initialization
                        BigInteger initProdSize;
                        {
                            int prod = numExecutionsForInit.get (prevChild).intValue ()
                                *  prevChild.getProduction ()
                                + prevChild.getInitDataProduction ();
                            initProdSize = BigInteger.valueOf (prod);
                        }

                        // compute the amount of data consumed during intitialization
                        BigInteger initConsumeSize;
                        {
                            int initConsume = child.getInitDataConsumption ();
                            int fillConsume = numExecutionsForInit.get (child).intValue () *
                                child.getConsumption ();
                            initConsumeSize = BigInteger.valueOf (initConsume + fillConsume);
                        }

                        BigInteger bufferSize = initProdSize.max (initProdSize.subtract (initConsumeSize).add (steadyStateProd));
                        scheduler.schedule.setBufferSize (prevChild.getStreamObject (), child.getStreamObject (), bufferSize);

                        prevChild = child;
                    }
            }
    }

    public Object getSteadySchedule ()
    {
        ASSERT (steadySchedule);
        return steadySchedule;
    }
    public Object getInitSchedule ()
    {
        ASSERT (initSchedule);
        return initSchedule;
    }

    public int getInitDataConsumption ()
    {
        ASSERT (initDataConsumption >= 0);
        return initDataConsumption;
    }

    public int getInitDataProduction ()
    {
        ASSERT (initDataProduction >= 0);
        return initDataProduction;
    }
}
