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

package streamit.library;

import java.util.*;
import streamit.misc.Pair;
import streamit.scheduler2.iriter.Iterator;
import streamit.scheduler2.print.PrintGraph;
import streamit.scheduler2.Schedule;
import streamit.scheduler2.Scheduler;

import streamit.scheduler2.constrained.NoPathException;

// the basic stream class (pipe's).  has 1 input and 1 output.
public abstract class Stream extends Operator
{

    public Channel input = null;
    public Channel output = null;

    LinkedList streamElements = new LinkedList();

    // CONSTRUCTORS --------------------------------------------------------------------
    public Stream(float a, float b, int c)
    {
        super(a, b, c);
    }

    public Stream(int a, float b)
    {
        super(a, b);
    }

    public Stream(float[] b)
    {
        super(b);
    }

    public Stream(int a, float[] b)
    {
        super(a, b);
    }

    public Stream(int a, int[] b)
    {
        super(a, b);
    }

    public Stream(int a, float[][] b)
    {
        super(a, b);
    }

    public Stream(int i1, int i2, float f)
    {
        super(i1, i2, f);
    }

    public Stream(int i1, int i2, int i3, float[] f)
    {
        super(i1, i2, i3, f);
    }

    public Stream(int i1, int i2, float f1, float f2)
    {
        super(i1, i2, f1, f2);
    }

    public Stream(int a, int b, float[] c)
    {
        super(a, b, c);
    }

    public Stream(int a, int b, float[][] c)
    {
        super(a, b, c);
    }

    public Stream(int a, float[] c, float[] d)
    {
        super(a, c, d);
    }

    public Stream(int a, int b, int c, int d, float[][] e)
    {
        super(a, b, c, d, e);
    }

    public Stream(int a, int b, int c, int d, float[][] e, float[][] f)
    {
        super(a, b, c, d, e, f);
    }

    public Stream(int a, int b, int c, float[][] e, float[][] f)
    {
        super(a, b, c, e, f);
    }

    public Stream(int a, int b, int c, int d, int e, float f)
    {
        super(a, b, c, d, e, f);
    }

    public Stream(
        int a,
        int b,
        int c,
        int d,
        int e,
        int f,
        float g,
        float h)
    {
        super(a, b, c, d, e, f, g, h);
    }

    public Stream(
        int a,
        int b,
        int c,
        int d,
        int e,
        int f,
        int g,
        float h,
        float i)
    {
        super(a, b, c, d, e, f, g, h, i);
    }

    public Stream(float a, int b)
    {
        super(a, b);
    }

    public Stream(float a, float b)
    {
        super(a, b);
    }

    public Stream(float a, float b, float c)
    {
        super(a, b, c);
    }

    public Stream(float a, float b, float c, float d)
    {
        super(a, b, c, d);
    }

    public Stream(float a, float b, float c, float d, int e, int f)
    {
        super(a, b, c, d, e, f);
    }

    public Stream(float a, float b, int c, int d)
    {
        super(a, b, c, d);
    }

    public Stream(float x, float y, float z, int a, float b)
    {
        super(x, y, z, a, b);
    }

    public Stream(int a, int b, int c, float d, int e)
    {
        super(a, b, c, d, e);
    }

    public Stream(int a, int b, int c, float d, float e)
    {
        super(a, b, c, d, e);
    }

    public Stream(float a, float b, float c, int d, int e, int f)
    {
        super(a, b, c, d, e, f);
    }

    public Stream(float a, float b, int c, int d, int e, int f)
    {
        super(a, b, c, d, e, f);
    }

    public Stream(float a, float b, int c, int d, int e, int f, int g)
    {
        super(a, b, c, d, e, f, g);
    }

    public Stream(int a, int b, float c, int d, float e)
    {
        super(a, b, c, d, e);
    }

    public Stream(float x, float y, float z, int a)
    {
        super(x, y, z, a);
    }

    public Stream(float x, float y, float z, int a, int b)
    {
        super(x, y, z, a, b);
    }

    public Stream(float x, float y, int a, int b, int c)
    {
        super(x, y, a, b, c);
    }

    public Stream(float f1, int i1, float[] f2, float[] f3, int i2)
    {
        super(f1, i1, f2, f3, i2);
    }

    public Stream()
    {
        super();
    }

    public Stream(char c)
    {
        super(c);
    }

    public Stream(int n)
    {
        super(n);
    }

    public Stream(boolean b1)
    {
        super(b1);
    }

    public Stream(int n1, int n2, boolean b1)
    {
        super(n1, n2, b1);
    }

    public Stream(int n1, boolean b1)
    {
        super(n1, b1);
    }

    public Stream(int x, int y)
    {
        super(x, y);
    }

    public Stream(int x, int y, int z)
    {
        super(x, y, z);
    }

    public Stream(int x, int y, int z, float[][] f)
    {
        super(x, y, z, f);
    }

    public Stream(int x, int y, int z, int a)
    {
        super(x, y, z, a);
    }

    public Stream(int a, int b, int c, int d, int e)
    {
        super(a, b, c, d, e);
    }

    public Stream(int a, int b, int c, int d, int e, int f, int g)
    {
        super(a, b, c, d, e, f, g);
    }

    public Stream(int n1, int n2, int n3, int n4, float f1)
    {
        super(n1, n2, n3, n4, f1);
    }

    public Stream(int x, int y, int z, int a, int b, int c)
    {
        super(x, y, z, a, b, c);
    }

    public Stream(int x, int y, int z, int a, int b, int c, int d, float f)
    {
        super(x, y, z, a, b, c, d, f);
    }

    public Stream(
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
        float f)
    {
        super(n1, n2, n3, n4, n5, n6, n7, n8, n9, n10, f);
    }

    public Stream(
        int n1,
        int n2,
        int n3,
        int n4,
        int n5,
        int n6,
        int n7,
        int n8,
        int n9)
    {
        super(n1, n2, n3, n4, n5, n6, n7, n8, n9);
    }

    public Stream(float f)
    {
        super(f);
    }

    public Stream(String str)
    {
        super(str);
    }

    public Stream(ParameterContainer params)
    {
        super(params);
    }

    public Stream(
        int i1,
        int i2,
        int i3,
        int i4,
        int i5,
        int i6,
        int i7,
        int i8,
        int i9,
        float f)
    {
        super(i1, i2, i3, i4, i5, i6, i7, i8, i9, f);
    }

    public Stream(int i1, int i2, int i3, int i4, int i5, int i6, float f)
    {
        super(i1, i2, i3, i4, i5, i6, f);
    }

    public Stream(int n1, int n2, float f1[], float f2[])
    {
        super(n1, n2, f1, f2);
    }

    public Stream(short s1, short s2, short s3)
    {
        super(s1, s2, s3);
    }

    public Stream(int i1, int i2, int i3, float f1)
    {
        super(i1, i2, i3, f1);
    }

    public Stream(Object o1)
    {
        super(o1);
    }
    public Stream(Object o1, int i1)
    {
        super(o1, i1);
    }
    public Stream(int i1, int i2, Object o1)
    {
        super(i1, i2, o1);
    }
    public Stream(Object o1, Object o2)
    {
        super(o1, o2);
    }

    public Stream(Object o1, Object o2, Object o3)
    {
        super(o1, o2, o3);
    }

    // RESET FUNCTIONS

    public MessageStub reset()
    {
        ASSERT(false);
        return MESSAGE_STUB;
    }

    public MessageStub reset(int n)
    {
        ASSERT(false);
        return MESSAGE_STUB;
    }

    public MessageStub reset(String str)
    {
        ASSERT(false);
        return MESSAGE_STUB;
    }

    // ------------------------------------------------------------------
    //                  graph handling functions
    // ------------------------------------------------------------------

    // tells me if this component has already been connected
    boolean isConnected = false;

    // adds something to the pipeline
    public void add(Stream s)
    {
        ASSERT(s != null);
        streamElements.add(s);
    }

    // get my input.
    // makes sure that I only have ONE input
    // return null if no input present
    Channel getIOField(String fieldName)
    {
        return getIOField(fieldName, 0);
    }

    void setIOField(String fieldName, Channel newChannel)
    {
        setIOField(fieldName, 0, newChannel);
    }

    Channel getOutputChannel()
    {
        return output;
    }

    Channel getInputChannel()
    {
        return input;
    }

    public abstract void connectGraph();

    /**
     * Initialize the buffer lengths for a stream, given a schedule:
     */
    abstract void setupBufferLengths(Scheduler buffers);

    public Stream getChild(int nChild)
    {
        ERROR("Cannot get children of this stream!");
        return null;
    }

    /*************************************************************/
    // FOLLOWING FIELDS/METHODS ARE FROM OLD STREAMIT.JAVA.      
    //  --> intended for running streams as toplevel class.
    /*************************************************************/

    int numExecutions = 0;
    boolean marksteady = false;
    boolean printsched = false;

    int uncompressedSize = 0;
    int totalSize = 0;
    HashMap sizeMap = new HashMap();
    HashSet usefulSet = new HashSet();
    public static int totalBuffer = 0;
    boolean finegrained = false;

    Vector integers = new Vector();

    Integer getInteger(int i)
    {
        int size = integers.size();
        while (size < i + 1)
        {
            integers.add(new Integer(size));
            size++;
        }

        return (Integer)integers.get(i);
    }

    public int computeSize(Object s, boolean top)
    {
        if (sizeMap.get(s) != null)
        {
            //System.out.print (".");
            return 0;
        }

        {
            ASSERT(s instanceof Schedule);

            int index = sizeMap.size();
            sizeMap.put(s, getInteger(index));

            Schedule sched = (Schedule)s;

            if (sched.isBottomSchedule())
                return 0;

            int size = sched.getNumPhases();
            for (int nSched = 0; nSched < sched.getNumPhases(); nSched++)
            {
                size += computeSize(sched.getSubSched(nSched), top);
            }

            return size;
        }
    }

    HashMap oper2iter = new HashMap();

    void runSchedule(Object schedule, Object function)
    {
        if (schedule instanceof Operator)
        {
            numExecutions++;
            if (numExecutions == 10000)
            {
                if (marksteady)
                    System.out.print(".");
                numExecutions = 0;
            }
            Operator oper = (Operator)schedule;
            int filterPop, filterPush;
            if (oper instanceof Filter)
            {
                Filter f = (Filter)oper;
                f.executeNextPhase((String)function);
            }
            else if (oper instanceof SplitJoin)
            {
                if (finegrained)
                {
                    ASSERT(function instanceof Pair);
                    Pair pair = (Pair)function;
                    ASSERT(pair.getFirst() instanceof Operator);
                    Operator sORj = (Operator)pair.getFirst();
                    int funcNum = ((Integer)pair.getSecond()).intValue();

                    sORj.work();
                }
                else
                {
                    ASSERT(function instanceof Operator);
                    ((Operator)function).work();
                }
            }
            else if (oper instanceof FeedbackLoop)
            {
                ASSERT(function instanceof Operator);
                ((Operator)function).work();
            }
            else
                ASSERT(false);

        }
    }
    void runSchedule(Object schedule, int nTimes)
    {
        if (schedule instanceof Schedule)
        {
            Schedule repSchedule = (Schedule)schedule;

            for (; nTimes > 0; nTimes--)
            {
                if (repSchedule.isBottomSchedule())
                {
                    runSchedule(
                        repSchedule.getStream().getObject(),
                        repSchedule.getWorkFunc());
                }
                else
                {
                    int nSched;
                    for (nSched = 0;
                        nSched < repSchedule.getNumPhases();
                        nSched++)
                    {
                        runSchedule(
                            repSchedule.getSubSched(nSched),
                            repSchedule.getSubSchedNumExecs(nSched));
                    }
                }
            }

        }
        else
            ASSERT(false);
    }

    /* removing this to force people to pass arguments
    public void run ()
    {
        run(null);
    }
    */

    Stream getStreamFromString(String path)
    {
        int nChild = Integer.parseInt(path);
        return getChild(nChild);
    }

    // just a runtime hook to run the stream
    public void run(String args[])
    {
        boolean scheduledRun = true;
        boolean printGraph = true;
        boolean doRun = true;
        boolean singeappsched = false;
        boolean printSDEP = false;
        String sdepTOPString = null, sdepBOTTOMString = null;
        int nIters = -1;

        // read the args:
        if (args != null)
        {
            int length = args.length;
            int index;
            for (index = 0; index < length; index++)
            {
                if (args[index].equals("-nosched"))
                {
                    scheduledRun = false;
                }
                else if (args[index].equals("-printgraph"))
                {
                    printGraph = true;
                }
                else if (args[index].equals("-i"))
                {
                    index++;
                    nIters = Integer.valueOf(args[index]).intValue();
                }
                else if (args[index].equals("-marksteady"))
                {
                    marksteady = true;
                }
                else if (args[index].equals("-printsched"))
                {
                    printsched = true;
                }
                else if (args[index].equals("-norun"))
                {
                    doRun = false;
                }
                else if (args[index].equals("-singeappsched"))
                {
                    singeappsched = true;
                }
                else if (args[index].equals("-finegrained"))
                {
                    finegrained = true;
                }
                else if (args[index].equals("-printsdep"))
                {
                    printSDEP = true;
                    sdepTOPString = args[index + 1];
                    sdepBOTTOMString = args[index + 2];
                    index += 2;
                }
                else
                {
                    ERROR("Unrecognized argument: " + args[index] + ".");
                }
            }
        }

        setupOperator();

        ASSERT(
            getInputChannel() == null,
            "The toplevel stream can't have any input or output channels,\n"
                + "but in this program there is an input to the first filter.");
        ASSERT(
            getOutputChannel() == null,
            "The toplevel stream can't have any input or output channels,\n"
                + "but in this program there is an output of the last filter.");

        if (finegrained)
        {
            streamit.library.iriter.SplitJoinIter.fineGrained = true;
            streamit.library.Splitter.finegrained = true;
            streamit.library.Joiner.finegrained = true;
        }

        // setup the scheduler
        if (printGraph)
        {
            Iterator selfIter = new streamit.library.iriter.Iterator(this);
            new PrintGraph().printProgram(selfIter);
        }

        // compute latency from top to bottom
        if (printSDEP)
            try
            {
                Stream top = getStreamFromString(sdepTOPString);
                Stream bottom = getStreamFromString(sdepBOTTOMString);
                Iterator selfIter =
                    new streamit.library.iriter.Iterator(this);
                streamit.scheduler2.constrained.Scheduler sched =
                    new streamit.scheduler2.constrained.Scheduler(selfIter);
                streamit.scheduler2.SDEPData sdep =
                    sched.computeSDEP(
                        new streamit.library.iriter.Iterator(top),
                        new streamit.library.iriter.Iterator(bottom));

                System.out.println("Init SDEP:");
                System.out.println("Upstream Downstream");
                for (int n = 0; n < sdep.getNumDstInitPhases(); n++)
                {
                    System.out.println(
                        "  " + sdep.getSrcPhase4DstPhase(n) + "         " + n);
                }
                System.out.println(
                    "Total: "
                        + sdep.getNumSrcInitPhases()
                        + " "
                        + sdep.getNumDstInitPhases()
                        + "\n");

                System.out.println("Steady SDEP:");
                System.out.println("Upstream Downstream");
                for (int n = 0; n < sdep.getNumDstSteadyPhases(); n++)
                {
                    System.out.println(
                        "  "
                            + (sdep
                                .getSrcPhase4DstPhase(
                                    n + sdep.getNumDstInitPhases()))
                            + "         "
                            + (n + sdep.getNumDstInitPhases()));
                }
                System.out.println(
                    "Total: "
                        + sdep.getNumSrcSteadyPhases()
                        + " "
                        + sdep.getNumDstSteadyPhases()
                        + "\n");
            }
            catch (NoPathException x)
            {
                ERROR(x);
            }

        // setup the scheduler
        if (scheduledRun)
        {
            Iterator selfIter = new streamit.library.iriter.Iterator(this);
            streamit.scheduler2.Scheduler scheduler;
            if (singeappsched)
                scheduler =
                    new streamit.scheduler2.singleappearance.Scheduler(
                        selfIter);
            else
                scheduler =
                    new streamit.scheduler2.minlatency.Scheduler(selfIter);

            /*scheduler =
                new streamit.scheduler2.constrained.Scheduler(selfIter);*/

            scheduler.computeSchedule();
            scheduler.computeBufferUse();
            Schedule initSched = scheduler.getOptimizedInitSchedule();
            Schedule steadySched = scheduler.getOptimizedSteadySchedule();

            // setup the buffer lengths for the stream setup here:
            setupBufferLengths(scheduler);

            totalSize = this.computeSize(initSched, true);
            totalSize += this.computeSize(steadySched, true);

            int mlSize = totalSize;
            int mlBuffer = totalBuffer;

            if (printsched)
            {
                scheduler.printOptimizedSchedule();
                System.out.println("!ml sched size = " + mlSize);
                System.out.println("!ml buff size = " + mlBuffer);
            }

            if (!doRun)
                System.exit(0);

            // run the init schedule:
            if (nIters != 0)
                runSchedule(initSched, 1);

            //nIters = 0;

            // and run the steady schedule forever:
            while (nIters != 0)
            {
                runSchedule(steadySched, 1);
                if (marksteady)
                    System.out.print("*");
                if (nIters > 0)
                    nIters--;
            }
        }
        else
        {
            if (!doRun)
                System.exit(0);

            while (true)
            {
                runSinks();
                drainChannels();
            }
        }
    }

    /*************************************************************/
    // PRECEDING FIELDS/METHODS ARE FROM OLD STREAMIT.JAVA.      
    //  --> intended for running streams as toplevel class.
    /*************************************************************/
}
