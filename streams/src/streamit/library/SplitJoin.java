package streamit;

import java.util.*;
import java.lang.reflect.*;

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

    // initializing IO will be handled by the Add function
    public void InitIO () { }

    // type of a split or a join:
    public static class SplitJoinType
    {
        // type:
        //  1 - round robin
        //  2 - weighted round robin
        //  3 - duplicate

        int type;
        List weights;

        SplitJoinType (int myType)
        {
            switch (myType)
            {
                case 1: // round robin
                case 3: // duplicate
                    break;
                case 2: // weighted round robin - need a weight list
                    weights = new LinkedList ();
                    break;
                default:
                    // passed an illegal parameter to the constructor!
                    ASSERT (false);
            }

            type = myType;
        }

        SplitJoinType AddWeight (int weight)
        {
            ASSERT (weights != null);
            weights.add (new Integer (weight));
            return this;
        }

        Splitter GetSplitter ()
        {
            switch (type)
            {
                case 1:
                    return new RoundRobinSplitter ();
                case 2:
                    WeightedRoundRobinSplitter splitter = new WeightedRoundRobinSplitter ();
                    while (!weights.isEmpty ())
                    {
                        splitter.AddWeight ((Integer)weights.remove (0));
                    }
                    return splitter;
                case 3:
                    return new DuplicateSplitter ();
                default:
                    ASSERT (false);
            }
            return null;
        }

        Joiner GetJoiner ()
        {
            switch (type)
            {
                case 1:
                    return new RoundRobinJoiner ();
                case 2:
                    WeightedRoundRobinJoiner joiner = new WeightedRoundRobinJoiner ();
                    while (!weights.isEmpty ())
                    {
                        joiner.AddWeight ((Integer)weights.remove (0));
                    }
                    return joiner;
                case 3: // there are no duplicate joiners!
                default:
                    ASSERT (false);
            }
            return null;
        }
    }

    public static SplitJoinType WEIGHTED_ROUND_ROBIN (int w1)
    {
        return new SplitJoinType (2).AddWeight (w1);
    }

    public static SplitJoinType WEIGHTED_ROUND_ROBIN (int w1, int w2)
    {
        return new SplitJoinType (2).AddWeight (w1).AddWeight (w2);
    }

    public static SplitJoinType WEIGHTED_ROUND_ROBIN (int w1, int w2, int w3)
    {
        return new SplitJoinType (2).AddWeight (w1).AddWeight (w2).AddWeight (w3);
    }

    public static SplitJoinType WEIGHTED_ROUND_ROBIN (int w1, int w2, int w3, int w4)
    {
        return new SplitJoinType (2).AddWeight (w1).AddWeight (w2).AddWeight (w3).AddWeight (w4);
    }

    public static SplitJoinType WEIGHTED_ROUND_ROBIN (int w1, int w2, int w3, int w4, int w5, int w6, int w7)
    {
        return new SplitJoinType (2).AddWeight (w1).AddWeight (w2).AddWeight (w3).AddWeight (w4).AddWeight (w5).AddWeight (w6).AddWeight (w7);
    }

    public static SplitJoinType ROUND_ROBIN ()
    {
        return new SplitJoinType (1);
    }

    public static SplitJoinType DUPLICATE ()
    {
        return new SplitJoinType (3);
    }

    // specify the splitter
    public void SetSplitter(SplitJoinType type)
    {
        ASSERT (splitter == null && type != null);
        splitter = type.GetSplitter ();

        splitType = type;
    }

    // specify the joiner
    // must also add all the appropriate outputs to the joiner!
    public void SetJoiner(SplitJoinType type)
    {
        ASSERT (joiner == null && type != null);
        joiner = type.GetJoiner ();

        ListIterator iter;
        iter = childrenStreams.listIterator ();
        while (iter.hasNext ())
        {
            Stream s = (Stream) iter.next ();
            ASSERT (s != null);

            joiner.Add (s);
        }

        joinType = type;
    }

    // add a stream to the parallel section between the splitter and the joiner
    public void Add(Stream s)
    {
        ASSERT (joiner == null);

        // add the stream to the Split
        if (splitter != null)
        {
            splitter.Add (s);
        }

        // save the stream to add to the Join
        if (childrenStreams == null)
        {
            childrenStreams = new LinkedList ();
        }
        childrenStreams.add (s);
    }

    public void ConnectGraph ()
    {
        // setup all children of this splitjoin
        {
            ListIterator iter;
            iter = childrenStreams.listIterator ();
            while (iter.hasNext ())
            {
                Stream s = (Stream) iter.next ();
                ASSERT (s != null);

                s.SetupOperator ();
            }
        }
        // connect the SplitJoin with the Split and the Join
        if (splitter != null)
        {
            splitter.SetupOperator ();
            input = splitter.GetIOField ("input", 0);
            ASSERT (input != null);
        }

        if (joiner != null)
        {
            joiner.SetupOperator ();
            output = joiner.GetIOField ("output", 0);
            ASSERT (output != null);
        }
    }

    // ----------------------------------------------------------------
    // This code constructs an independent graph for the scheduler
    // ----------------------------------------------------------------

    SchedStream constructSchedule ()
    {
        // create a new splitjoin
        SchedSplitJoin splitJoin = new SchedSplitJoin ();

        // setup the splitter
        if (splitter != null)
        {
            SchedSplitType splitType;
            splitType = splitter.getSchedType ();
            splitJoin.setSplitType (splitType);
        }

        // setup the joiner
        if (joiner != null)
        {
            SchedJoinType joinType;
            joinType = joiner.getSchedType ();
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

}