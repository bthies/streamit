package streamit;

import java.util.*;
import streamit.scheduler2.Scheduler;
import streamit.iriter.Iterator;

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

    public SplitJoin(int a, float b)
    {
        super(a, b);
    }

    public SplitJoin(int i1, int i2, float f)
    {
        super(i1, i2, f);
    }

    public SplitJoin(float f1, float f2, float f3)
    {
        super(f1, f2, f3);
    }

    public SplitJoin(int i1, int i2, float f1, float f2)
    {
        super(i1, i2, f1, f2);
    }

    public SplitJoin(int n1, int n2, int n3)
    {
        super(n1, n2, n3);
    }

    public SplitJoin(int n1, int n2, int n3, float[][] n4)
    {
        super(n1, n2, n3, n4);
    }

    public SplitJoin(int n1, int n2, int n3, int n4, float[][] n5)
    {
        super(n1, n2, n3, n4, n5);
    }

    public SplitJoin(
        int n1,
        int n2,
        int n3,
        int n4,
        float[][] n5,
        float[][] n6)
    {
        super(n1, n2, n3, n4, n5, n6);
    }

    public SplitJoin(int n1, int n2, int n3, int n4)
    {
        super(n1, n2, n3, n4);
    }

    public SplitJoin(int n1, int n2, int n3, int n4, int n5)
    {
        super(n1, n2, n3, n4, n5);
    }

    public SplitJoin(int n1, int n2, int n3, int n4, int n5, int n6, int n7)
    {
        super(n1, n2, n3, n4, n5, n6, n7);
    }

    public SplitJoin(
        int n1,
        int n2,
        int n3,
        int n4,
        int n5,
        int n6,
        int n7,
        float f1)
    {
        super(n1, n2, n3, n4, n5, n6, n7, f1);
    }

    public SplitJoin(
        int n1,
        int n2,
        int n3,
        int n4,
        int n5,
        int n6,
        int n7,
        int n8,
        int n9,
        int n10,
        float f1)
    {
        super(n1, n2, n3, n4, n5, n6, n7, n8, n9, n10, f1);
    }

    public SplitJoin(float n1, float n2, float n3, int n4)
    {
        super(n1, n2, n3, n4);
    }

    public SplitJoin(float n1, float n2, float n3, int n4, int n5, int n6)
    {
        super(n1, n2, n3, n4, n5, n6);
    }

    public SplitJoin(float f1, float f2, int n1)
    {
        super(f1, f2, n1);
    }

    public SplitJoin(int n1, int n2, float f1[], float f2[])
    {
        super(n1, n2, f1, f2);
    }

    public SplitJoin (int a, int b, int c, float[][] x, float[][]y) { super (a, b, c,x,y); }

    public SplitJoin(int n1, int n2, boolean b1)
    {
        super(n1, n2, b1);
    }

    public SplitJoin(int n1, boolean b1)
    {
        super(n1, b1);
    }

    public SplitJoin(Object o1) {super(o1);}
    public SplitJoin(Object o1, int i1){super(o1, i1);}
    public SplitJoin(int i1, int i2, Object o1) {super(i1,i2,o1);}
    
    public SplitJoin(Object o1,Object o2) {super(o1,o2);}

    public SplitJoin(Object o1,Object o2,Object o3) {super(o1,o2,o3);}
    
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

        SplitJoinType(int myType)
        {
            switch (myType)
            {
                case 1 : // round robin and
                case 2 : // weighted round robin - need a weight list
                    weights = new LinkedList();
                    break;
                case 3 : // duplicate
                case 4 : // null - no in/out
                    break;
                default :
                    // passed an illegal parameter to the constructor!
                    SASSERT(false);
            }

            type = myType;
        }

        SplitJoinType addWeight(int weight)
        {
            SASSERT(weights != null);
            weights.add(new Integer(weight));
            return this;
        }

        Splitter getSplitter()
        {
            switch (type)
            {
                case 1 :
                    {
                        RoundRobinSplitter splitter =
                            new RoundRobinSplitter(
                                ((Integer) weights.remove(0)).intValue());
                        return splitter;
                    }
                case 2 :
                    WeightedRoundRobinSplitter splitter =
                        new WeightedRoundRobinSplitter();
                    while (!weights.isEmpty())
                    {
                        splitter.addWeight((Integer) weights.remove(0));
                    }
                    return splitter;
                case 3 :
                    return new DuplicateSplitter();
                case 4 :
                    return new NullSplitter();
                default :
                    SASSERT(false);
            }
            return null;
        }

        Joiner getJoiner()
        {
            switch (type)
            {
                case 1 :
                    {
                        RoundRobinJoiner joiner =
                            new RoundRobinJoiner(
                                ((Integer) weights.remove(0)).intValue());
                        return joiner;
                    }
                case 2 :
                    WeightedRoundRobinJoiner joiner =
                        new WeightedRoundRobinJoiner();
                    while (!weights.isEmpty())
                    {
                        joiner.addWeight((Integer) weights.remove(0));
                    }
                    return joiner;
                case 3 :
                    // there are no duplicate joiners!
                    SASSERT(false);
                    break;
                case 4 :
                    return new NullJoiner();
                default :
                    SASSERT(false);
            }
            return null;
        }
    }

    public static SplitJoinType WEIGHTED_ROUND_ROBIN(int w1)
    {
        return new SplitJoinType(2).addWeight(w1);
    }

    public static SplitJoinType WEIGHTED_ROUND_ROBIN(int w1, int w2)
    {
        return new SplitJoinType(2).addWeight(w1).addWeight(w2);
    }

    public static SplitJoinType WEIGHTED_ROUND_ROBIN(int w1, int w2, int w3)
    {
        return new SplitJoinType(2).addWeight(w1).addWeight(w2).addWeight(w3);
    }

    public static SplitJoinType WEIGHTED_ROUND_ROBIN(
        int w1,
        int w2,
        int w3,
        int w4)
    {
        return new SplitJoinType(2)
            .addWeight(w1)
            .addWeight(w2)
            .addWeight(w3)
            .addWeight(w4);
    }

    public static SplitJoinType WEIGHTED_ROUND_ROBIN(
        int w1,
        int w2,
        int w3,
        int w4,
	int w5)
    {
        return new SplitJoinType(2)
            .addWeight(w1)
            .addWeight(w2)
            .addWeight(w3)
            .addWeight(w4)
            .addWeight(w5);
    }

    public static SplitJoinType WEIGHTED_ROUND_ROBIN(
        int w1,
        int w2,
        int w3,
        int w4,
        int w5,
        int w6,
        int w7)
    {
        return new SplitJoinType(2)
            .addWeight(w1)
            .addWeight(w2)
            .addWeight(w3)
            .addWeight(w4)
            .addWeight(w5)
            .addWeight(w6)
            .addWeight(w7);
    }

    public static SplitJoinType WEIGHTED_ROUND_ROBIN(
        int w1,
        int w2,
        int w3,
        int w4,
        int w5,
        int w6,
        int w7,
        int w8,
        int w9,
        int w10,
        int w11,
        int w12)
    {
        return new SplitJoinType(2)
            .addWeight(w1)
            .addWeight(w2)
            .addWeight(w3)
            .addWeight(w4)
            .addWeight(w5)
            .addWeight(w6)
            .addWeight(w7)
            .addWeight(w8)
            .addWeight(w9)
            .addWeight(w10)
            .addWeight(w11)
            .addWeight(w12);
    }

    public static SplitJoinType WEIGHTED_ROUND_ROBIN(
        int w1,
        int w2,
        int w3,
        int w4,
        int w5,
        int w6,
        int w7,
        int w8,
        int w9,
        int w10,
        int w11,
        int w12,
        int w13,
        int w14,
        int w15,
        int w16,
        int w17,
        int w18)
    {
        return new SplitJoinType(2)
            .addWeight(w1)
            .addWeight(w2)
            .addWeight(w3)
            .addWeight(w4)
            .addWeight(w5)
            .addWeight(w6)
            .addWeight(w7)
            .addWeight(w8)
            .addWeight(w9)
            .addWeight(w10)
            .addWeight(w11)
            .addWeight(w12)
            .addWeight(w13)
            .addWeight(w14)
            .addWeight(w15)
            .addWeight(w16)
            .addWeight(w17)
            .addWeight(w18);
    }

    public static SplitJoinType ROUND_ROBIN()
    {
        return new SplitJoinType(1).addWeight(1);
    }

    public static SplitJoinType ROUND_ROBIN(int weight)
    {
        return new SplitJoinType(1).addWeight(weight);
    }

    public static SplitJoinType DUPLICATE()
    {
        return new SplitJoinType(3);
    }

    public static SplitJoinType NULL()
    {
        return new SplitJoinType(4);
    }

    // specify the splitter
    public void setSplitter(SplitJoinType type)
    {
        ASSERT(splitter == null && type != null);
        splitter = type.getSplitter();

        splitType = type;
    }

    // specify the joiner
    // must also add all the appropriate outputs to the joiner!
    public void setJoiner(SplitJoinType type)
    {
        ASSERT(joiner == null && type != null);
        joiner = type.getJoiner();

        ListIterator iter;
        iter = childrenStreams.listIterator();
        while (iter.hasNext())
        {
            Stream s = (Stream) iter.next();
            ASSERT(s != null);

            joiner.add(s);
        }

        joinType = type;
    }

    // add a stream to the parallel section between the splitter and the joiner
    public void add(Stream s)
    {
        ASSERT(joiner == null);

        // add the stream to the Split
        if (splitter != null)
        {
            splitter.add(s);
        }

        // save the stream to add to the Join
        if (childrenStreams == null)
        {
            childrenStreams = new LinkedList();
        }
        childrenStreams.add(s);
    }

    public void connectGraph()
    {
        // setup all children of this splitjoin
        {
            ListIterator iter;
            iter = childrenStreams.listIterator();
            while (iter.hasNext())
            {
                Stream s = (Stream) iter.next();
                ASSERT(s != null);
                s.setupOperator();
            }
        }
        // connect the SplitJoin with the Split and the Join
        if (splitter != null)
        {
            splitter.setupOperator();
            input = splitter.getIOField("input", 0);
        }

        if (joiner != null)
        {
            joiner.setupOperator();
            output = joiner.getIOField("output", 0);
        }
    }

    // allow access to the children of this pipeline

    public int getNumChildren()
    {
        return childrenStreams.size();
    }
    public Stream getChildN(int n)
    {
        return (Stream) childrenStreams.get(n);
    }
    public Splitter getSplitter()
    {
        return splitter;
    }
    public Joiner getJoiner()
    {
        return joiner;
    }

    void setupBufferLengths(Scheduler buffers)
    {
        ListIterator iter;
        iter = childrenStreams.listIterator();

        // go through all the children
        while (iter.hasNext())
        {
            Stream child = (Stream) iter.next();
            ASSERT(child);

            child.setupBufferLengths(buffers);

            // init the split channel
            {
                int splitBufferSize =
                    buffers.getBufferSizeBetween(
                        new Iterator(this),
                        new Iterator(child));

                StreamIt.totalBuffer += splitBufferSize;
                
                // if the size of the buffer is zero, there is no corresponding
                // channel, so don't try to set it.
                if (splitBufferSize != 0)
                    child.getInputChannel().setChannelSize(splitBufferSize);
            }

            // init the join channel
            {
                int joinBufferSize =
                    buffers.getBufferSizeBetween(
                        new Iterator(child),
                        new Iterator(this));

                StreamIt.totalBuffer += joinBufferSize;
                
                // if the size of the buffer is zero, there is no corresponding
                // channel, so don't try to set it.
                if (joinBufferSize != 0)
                    child.getOutputChannel().setChannelSize(joinBufferSize);
            }
        }
    }
}
