package streamit;

import java.util.*;
import java.lang.reflect.*;
import java.math.BigInteger;

import streamit.scheduler.*;

// creates a split/join
public class SplitJoin extends Stream
{
    Splitter splitter;
    Joiner joiner;

    SplitJoinType splitType, joinType;

    List childrenStreams;

    public SplitJoin()
    {
        super();
    }

    public SplitJoin(int n)
    {
        super(n);
    }

    public SplitJoin(float n)
    {
        super(n);
    }

    public SplitJoin(int n1, int n2)
    {
        super(n1, n2);
    }

    public SplitJoin(int n1, int n2, int n3)
    {
        super(n1, n2, n3);
    }

    public SplitJoin(float n1, float n2, float n3, int n4)
    {
        super(n1, n2, n3, n4);
    }

    // type of a split or a join:
    public static class SplitJoinType
    {
        // type:
        //  1 - round robin
        //  2 - weighted round robin
        //  3 - duplicate
        //  4 - null

        int type;
        List weights;

        SplitJoinType (int myType)
        {
            switch (myType)
            {
                case 1: // round robin and
                case 2: // weighted round robin - need a weight list
                    weights = new LinkedList ();
                    break;
                case 3: // duplicate
                case 4: // null - no in/out
                    break;
                default:
                    // passed an illegal parameter to the constructor!
                    SASSERT (false);
            }

            type = myType;
        }

        SplitJoinType addWeight (int weight)
        {
            SASSERT (weights != null);
            weights.add (new Integer (weight));
            return this;
        }

        Splitter getSplitter ()
        {
            switch (type)
            {
                case 1:
                    {
                        RoundRobinSplitter splitter =  new RoundRobinSplitter (((Integer)weights.remove (0)).intValue ());
                        return splitter;
                    }
                case 2:
                    WeightedRoundRobinSplitter splitter = new WeightedRoundRobinSplitter ();
                    while (!weights.isEmpty ())
                    {
                        splitter.addWeight ((Integer)weights.remove (0));
                    }
                    return splitter;
                case 3:
                    return new DuplicateSplitter ();
                case 4:
                	return new NullSplitter ();
                default:
                    SASSERT (false);
            }
            return null;
        }

        Joiner getJoiner ()
        {
            switch (type)
            {
                case 1:
                    {
                        RoundRobinJoiner joiner =  new RoundRobinJoiner (((Integer)weights.remove (0)).intValue ());
                        return joiner;
                    }
                case 2:
                    WeightedRoundRobinJoiner joiner = new WeightedRoundRobinJoiner ();
                    while (!weights.isEmpty ())
                    {
                        joiner.addWeight ((Integer)weights.remove (0));
                    }
                    return joiner;
                case 3: 
                    // there are no duplicate joiners!
                    SASSERT (false);
                    break;
                case 4:
                    return new NullJoiner ();
                default:
                    SASSERT (false);
            }
            return null;
        }
    }

    public static SplitJoinType WEIGHTED_ROUND_ROBIN (int w1)
    {
        return new SplitJoinType (2).addWeight (w1);
    }

    public static SplitJoinType WEIGHTED_ROUND_ROBIN (int w1, int w2)
    {
        return new SplitJoinType (2).addWeight (w1).addWeight (w2);
    }

    public static SplitJoinType WEIGHTED_ROUND_ROBIN (int w1, int w2, int w3)
    {
        return new SplitJoinType (2).addWeight (w1).addWeight (w2).addWeight (w3);
    }

    public static SplitJoinType WEIGHTED_ROUND_ROBIN (int w1, int w2, int w3, int w4)
    {
        return new SplitJoinType (2).addWeight (w1).addWeight (w2).addWeight (w3).addWeight (w4);
    }

    public static SplitJoinType WEIGHTED_ROUND_ROBIN (int w1, int w2, int w3, int w4, int w5, int w6, int w7)
    {
        return new SplitJoinType (2).addWeight (w1).addWeight (w2).addWeight (w3).addWeight (w4).addWeight (w5).addWeight (w6).addWeight (w7);
    }

    public static SplitJoinType WEIGHTED_ROUND_ROBIN (int w1, int w2, int w3, int w4, int w5, int w6, int w7, int w8, int w9, int w10, int w11, int w12, int w13, int w14, int w15, int w16, int w17, int w18)
    {
        return new SplitJoinType (2).addWeight (w1).
        								 addWeight (w2).
        								 addWeight (w3).
        								 addWeight (w4).
        								 addWeight (w5).
        								 addWeight (w6).
        								 addWeight (w7).
        								 addWeight (w8).
        								 addWeight (w9).
        								 addWeight (w10).
        								 addWeight (w11).
        								 addWeight (w12).
        								 addWeight (w13).
        								 addWeight (w14).
        								 addWeight (w15).
        								 addWeight (w16).
        								 addWeight (w17).
        								 addWeight (w18);
    }

    public static SplitJoinType ROUND_ROBIN ()
    {
        return new SplitJoinType (1).addWeight (1);
    }

    public static SplitJoinType ROUND_ROBIN (int weight)
    {
        return new SplitJoinType (1).addWeight (weight);
    }

    public static SplitJoinType DUPLICATE ()
    {
        return new SplitJoinType (3);
    }
    
    public static SplitJoinType NULL ()
    {
        return new SplitJoinType (4);
    }

    // specify the splitter
    public void setSplitter(SplitJoinType type)
    {
        ASSERT (splitter == null && type != null);
        splitter = type.getSplitter ();

        splitType = type;
    }

    // specify the joiner
    // must also add all the appropriate outputs to the joiner!
    public void setJoiner(SplitJoinType type)
    {
        ASSERT (joiner == null && type != null);
        joiner = type.getJoiner ();

        ListIterator iter;
        iter = childrenStreams.listIterator ();
        while (iter.hasNext ())
        {
            Stream s = (Stream) iter.next ();
            ASSERT (s != null);

            joiner.add (s);
        }

        joinType = type;
    }

    // add a stream to the parallel section between the splitter and the joiner
    public void add(Stream s)
    {
        ASSERT (joiner == null);

        // add the stream to the Split
        if (splitter != null)
        {
            splitter.add (s);
        }

        // save the stream to add to the Join
        if (childrenStreams == null)
        {
            childrenStreams = new LinkedList ();
        }
        childrenStreams.add (s);
    }

    public void connectGraph ()
    {
        // setup all children of this splitjoin
        {
            ListIterator iter;
            iter = childrenStreams.listIterator ();
            while (iter.hasNext ())
            {
                Stream s = (Stream) iter.next ();
                ASSERT (s != null);
                s.setupOperator ();
            }
        }
        // connect the SplitJoin with the Split and the Join
        if (splitter != null)
        {
            splitter.setupOperator ();
            input = splitter.getIOField ("input", 0);
        }

        if (joiner != null)
        {
            joiner.setupOperator ();
            output = joiner.getIOField ("output", 0);
        }
    }

    // ----------------------------------------------------------------
    // This code constructs an independent graph for the scheduler
    // ----------------------------------------------------------------

    SchedStream constructSchedule ()
    {
        // create a new splitjoin
        SchedSplitJoin splitJoin = scheduler.newSchedSplitJoin (this);

        // setup the splitter
        if (splitter != null)
        {
            SchedSplitType splitType;
            splitType = splitter.getSchedType (scheduler);
            splitJoin.setSplitType (splitType);
        }

        // setup the joiner
        if (joiner != null)
        {
            SchedJoinType joinType;
            joinType = joiner.getSchedType (scheduler);
            splitJoin.setJoinType (joinType);
        }

        // add all the children:
        {
            ListIterator iter;
            iter = childrenStreams.listIterator ();

            while (iter.hasNext ())
            {
                Stream child = (Stream) iter.next ();
                ASSERT (child);

                SchedStream schedChild = child.constructSchedule ();
                splitJoin.addChild (schedChild);
            }
        }

        return splitJoin;
    }

    void setupBufferLengths (Schedule schedule)
    {
        ListIterator iter;
        iter = childrenStreams.listIterator ();

        // go through all the children
        while (iter.hasNext ())
        {
            Stream child = (Stream) iter.next ();
            ASSERT (child);

            child.setupBufferLengths (schedule);

            // init the split channel
            {
                BigInteger splitBufferSize = schedule.getBufferSizeBetween (splitter, child);
                ASSERT (splitBufferSize);

                // if the size of the buffer is zero, there is no corresponding
                // channel, so don't try to set it.
                if (splitBufferSize.signum () != 0)
                {
                    child.getInputChannel ().setChannelSize (splitBufferSize.intValue ());
                }
            }

            // init the join channel
            {
                BigInteger joinBufferSize = schedule.getBufferSizeBetween (child, joiner);
                ASSERT (joinBufferSize);

                // if the size of the buffer is zero, there is no corresponding
                // channel, so don't try to set it.
                if (joinBufferSize.signum () != 0)
                {
                    child.getOutputChannel ().setChannelSize (joinBufferSize.intValue ());
                }
            }
        }
    }
}
