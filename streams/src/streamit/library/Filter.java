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

import java.lang.reflect.Method;
import streamit.scheduler2.Scheduler;
import java.util.Vector;
import java.util.HashMap;

// a filter is the lowest-level block of streams
public abstract class Filter extends Stream
{

    public Filter() { super(); }

    public Filter(float a, float b, int c)
    {
        super(a, b, c);
    }

    public Filter(int a, float b)
    {
        super(a, b);
    }

    public Filter(float[] b)
    {
        super(b);
    }

    public Filter(int a, float[] b)
    {
        super(a, b);
    }

    public Filter(int a, int[] b)
    {
        super(a, b);
    }

    public Filter(int a, float[][] b)
    {
        super(a, b);
    }

    public Filter(int i1, int i2, float f) {
        super(i1, i2, f);
    }

    public Filter(int i1, int i2, int i3, float[] f) {
        super(i1, i2, i3, f);
    }

    public Filter(int i1, int i2, float f1, float f2) {
        super(i1, i2, f1, f2);
    }

    public Filter(int a, int b, float[] c)
    {
        super(a, b, c);
    }

    public Filter(int a, int b, float[][] c)
    {
        super(a, b, c);
    }

    public Filter(int a, float[] c, float[] d) 
    { 
        super (a, c, d); 
    }

    public Filter(int a, int b, int c, int d, float[][] e)
    {
        super(a, b, c, d, e);
    }

    public Filter(int a, int b, int c, int d, float[][] e, float[][] f)
    {
        super(a, b, c, d, e, f);
    }

    public Filter(int a, int b, int c, float[][] e, float[][] f)
    {
        super(a, b, c, e, f);
    }

    public Filter(int a, int b, int c, float[][] d, float[] e) {
        super(a, b, c, d, e);
    }

    public Filter(int a, int b, int c, int d, int e, float f)
    {
        super(a, b, c, d, e, f);
    }

    public Filter(int a, int b, int c, int d, int e, int f, float g, float h)
    {
        super(a, b, c, d, e, f, g, h);
    }

    public Filter(int a, int b, int c, int d, int e, int f, int g, float h, float i)
    {
        super(a, b, c, d, e, f, g, h, i);
    }

    public Filter(float a, int b)
    {
        super(a, b);
    }

    public Filter(float a, float b)
    {
        super(a, b);
    }

    public Filter(float a, float b, float c)
    {
        super(a, b, c);
    }

    public Filter(float a, float b, float c, float d)
    {
        super(a, b, c, d);
    }

    public Filter(float a, float b, float c, float d, float e, float f, float g)
    {
        super(a, b, c, d, e, f, g);
    }

    public Filter(float a, float b, float c, float d, int e, int f)
    {
        super(a, b, c, d, e, f);
    }

    public Filter(float a, float b, int c, int d)
    {
        super(a, b, c, d);
    }

    public Filter(float x, float y, float z, int a, float b)
    {
        super(x,y,z,a,b);
    }

    public Filter(int a, int b, int c, float d, int e)
    {
        super (a,b,c,d,e);
    }

    public Filter(int a, int b, int c, float d, float e)
    {
        super (a,b,c,d,e);
    }

    public Filter(float a, float b, float c, int d, int e, int f)
    {
        super (a,b,c,d,e,f);
    }

    public Filter(float a, float b, int c, int d, int e, int f)
    {
        super (a,b,c,d,e,f);
    }

    public Filter(float a, float b, int c, int d, int e, int f, int g)
    {
        super (a,b,c,d,e,f,g);
    }

    public Filter(int a, int b, float c, int d, float e)
    {
        super (a,b,c,d,e);
    }

    public Filter(float x, float y, float z, int a)
    {
        super(x,y,z,a);
    }

    public Filter(float x, float y, float z, int a, int b)
    {
        super(x,y,z,a,b);
    }

    public Filter(float x, float y, int a, int b, int c)
    {
        super(x,y,a,b,c);
    }

    public Filter(float f1, int i1, float[] f2, float[] f3, int i2) {
        super(f1, i1, f2, f3, i2);
    }

    public Filter(char c)
    {
        super (c);
    }

    public Filter(int n)
    {
        super (n);
    }

    public Filter(boolean b1)
    {
        super(b1);
    }

    public Filter(int n1, int n2, boolean b1)
    {
        super(n1, n2, b1);
    }

    public Filter(int n1, boolean b1)
    {
        super(n1, b1);
    }

    public Filter(int x, int y)
    {
        super (x, y);
    }

    public Filter(int x, int y, int z)
    {
        super (x, y, z);
    }

    public Filter(int x, int y, int z, float[][] f)
    {
        super (x, y, z, f);
    }

    public Filter(int x, int y, int z, int a)
    {
        super (x, y, z, a);
    }

    public Filter(int a, int b, int c, int d, int e) { super(a, b, c, d, e); }

    public Filter(int a, int b, int c, int d, int e, int f, int g) 
    {
        super (a, b, c, d, e, f, g);
    }

    public Filter(int n1, int n2, int n3,
                  int n4, float f1) {
        super(n1, n2, n3, n4, f1);
    }

    public Filter(int x, int y, int z,
                  int a, int b, int c)
    {
        super (x, y, z, a, b, c);
    }

    public Filter(int x, int y, int z,
                  int a, int b, int c, int d, float f)
    {
        super (x, y, z, a, b, c, d, f);
    }

    public Filter(int n1, int n2, int n3,
                  int n4, int n5, int n6, int n7, int n8, 
                  int n9, int n10, float f)
    {
        super (n1, n2, n3, n4, n5, n6, n7, n8, n9, n10, f);
    }

    public Filter(int n1, int n2, int n3,
                  int n4, int n5, int n6, int n7, int n8, 
                  int n9)
    {
        super (n1, n2, n3, n4, n5, n6, n7, n8, n9);
    }

    public Filter(float f)
    {
        super (f);
    }

    public Filter(String str)
    {
        super (str);
    }

    public Filter(ParameterContainer params)
    {
        super (params);
    }

    public Filter( int i1, 
                   int i2, 
                   int i3, 
                   int i4, 
                   int i5, 
                   int i6, 
                   int i7, 
                   int i8, 
                   int i9, 
                   float f) {
        super(i1, i2, i3, i4, i5, i6, i7, i8, i9, f);
    }

    public Filter( int i1, 
                   int i2, 
                   int i3, 
                   int i4, 
                   int i5, 
                   int i6, 
                   float f) {
        super(i1, i2, i3, i4, i5, i6, f);
    }

    public Filter(int n1, int n2, float f1[], float f2[])
    {
        super(n1, n2, f1, f2);
    }

    public Filter(short s1, short s2, short s3) {
        super(s1, s2, s3);
    }

    public Filter(int i1,int i2,int i3,float f1) {super(i1,i2,i3,f1);}

    public Filter(Object o1) {super(o1);}
    public Filter(Object o1, int i1) {super(o1, i1);}
    public Filter(int i1, int i2, Object o1) {super(i1,i2,o1);}
    public Filter(Object o1,Object o2) {super(o1,o2);}

    public Filter(Object o1,Object o2,Object o3) {super(o1,o2,o3);}

    // add was present in Operator, but is not defined in Filter anymore
    public void add(Stream s)
    {
        throw new UnsupportedOperationException();
    }

    // data for support of syntax in the CC paper
    int peekAmount = -1, popAmount = -1, pushAmount = -1;
    Class inputType = null, outputType = null;
    boolean ccStyleInit = false;

    // functions for support of syntax in the CC paper
    public void setPeek(int peek)
    {
        peekAmount = peek;
        ccStyleInit = true;
    }

    public void setPop(int pop)
    {
        popAmount = pop;
        ccStyleInit = true;
    }

    public void setPush(int push)
    {
        pushAmount = push;
        ccStyleInit = true;
    }

    public void setOutput(Class type)
    {
        outputType = type;
        ccStyleInit = true;
    }

    public void setInput(Class type)
    {
        inputType = type;
        ccStyleInit = true;
    }

    // data and functions for support of multi-stage/multi-phase filters:
    boolean multiPhaseStyle = false;
    class PhaseInfo
    {
        final public int e, o, u;
        final public String name;
        public PhaseInfo(int _e, int _o, int _u, String _name)
        {
            // might be passing in a peek rate that is less than the
            // pop rate; remember it as the pop rate instead.
            if (_e < _o) { _e = _o; }
            e = _e;
            o = _o;
            u = _u;
            name = _name;
        }
    }

    /**
     * List of initialization phases (i.e., for prework function).
     * Only populated in the case of a phased filter.
     */
    Vector<PhaseInfo> initPhases = new Vector<PhaseInfo>();
    /**
     * Summary of init phases.  This corresponds to the declared I/O
     * rates on the prework function.
     */
    PhaseInfo initPhasesSummary;
    /**
     * List of steady phases (i.e., for work function).  Only
     * populated in the case of a phased filter.
     */
    Vector<PhaseInfo> steadyPhases = new Vector<PhaseInfo>();
    /**
     * Summary of steady phases.  This corresponds to the declared I/O
     * rates on the work function.
     */
    PhaseInfo steadyPhasesSummary;

    Class inType, outType;

    public boolean isMultiPhaseStyle()
    {
        return multiPhaseStyle;
    }

    public void setIOTypes(Class in, Class out)
    {
        if (in != Void.TYPE)
            inType = in;
        if (out != Void.TYPE)
            outType = out;
        multiPhaseStyle = true;
    }

    public void addSteadyPhase(Rate e, Rate o, Rate u, String name)
    {
        // if we detect a dynamic rate, make sure we're in unscheduled
        // mode
        if (!e.isStatic() || !o.isStatic() || !u.isStatic()) {
            Stream.ensureUnscheduled();
        }

        // construct phase.  use max rates in case they are dynamic
        PhaseInfo phase = new PhaseInfo(e.max, o.max, u.max, name);

        // if it is the work function, store it as a summary rather
        // than a phase
        if (name.equals("work")) {
            steadyPhasesSummary = phase;
            // also add it to steady phases if there were no previous
            // phases
            if (steadyPhases.size()==0) {
                steadyPhases.add(phase);
            }
        } else {
            steadyPhases.add(phase);
        }

        multiPhaseStyle = true;
    }
    /**
     * Same as above, for fixed I/O rates (and backwards compatibility
     * with old Java benchmarks).  It's slightly easier to write all
     * these signatures here and have the compiler recognize an int
     * literal as being a static rate than it is to recognize a static
     * rate in the compiler.
     */
    public void addSteadyPhase(int e, int o, int u, String name) {
        addSteadyPhase(new RateStatic(e), new RateStatic(o), new RateStatic(u), name);
    }
    public void addSteadyPhase(Rate e, int o, int u, String name) {
        addSteadyPhase(e, new RateStatic(o), new RateStatic(u), name);
    }
    public void addSteadyPhase(int e, Rate o, int u, String name) {
        addSteadyPhase(new RateStatic(e), o, new RateStatic(u), name);
    }
    public void addSteadyPhase(int e, int o, Rate u, String name) {
        addSteadyPhase(new RateStatic(e), new RateStatic(o), u, name);
    }
    public void addSteadyPhase(Rate e, int o, Rate u, String name) {
        addSteadyPhase(e, new RateStatic(o), u, name);
    }
    public void addSteadyPhase(Rate e, Rate o, int u, String name) {
        addSteadyPhase(e, o, new RateStatic(u), name);
    }
    public void addSteadyPhase(int e, Rate o, Rate u, String name) {
        addSteadyPhase(new RateStatic(e), o, u, name);
    }

    public void addInitPhase(Rate e, Rate o, Rate u, String name)
    {
        // if we detect a dynamic rate, make sure we're in unscheduled
        // mode
        if (!e.isStatic() || !o.isStatic() || !u.isStatic()) {
            Stream.ensureUnscheduled();
        }

        // use max rates in case they are dynamic
        PhaseInfo phase = new PhaseInfo(e.max, o.max, u.max, name);

        if (name.equals("prework")) {
            initPhasesSummary = phase;
            // also add it to steady phases if there were no previous
            // phases
            if (initPhases.size()==0) {
                initPhases.add(phase);
            }
        } else {
            initPhases.add(phase);
        }

        multiPhaseStyle = true;
    }
    /**
     * Same as above, for fixed I/O rates (and backwards compatibility
     * with old Java benchmarks).  It's slightly easier to write all
     * these signatures here and have the compiler recognize an int
     * literal as being a static rate than it is to recognize a static
     * rate in the compiler.
     */
    public void addInitPhase(int e, int o, int u, String name) {
        addInitPhase(new RateStatic(e), new RateStatic(o), new RateStatic(u), name);
    }
    public void addInitPhase(Rate e, int o, int u, String name) {
        addInitPhase(e, new RateStatic(o), new RateStatic(u), name);
    }
    public void addInitPhase(int e, Rate o, int u, String name) {
        addInitPhase(new RateStatic(e), o, new RateStatic(u), name);
    }
    public void addInitPhase(int e, int o, Rate u, String name) {
        addInitPhase(new RateStatic(e), new RateStatic(o), u, name);
    }
    public void addInitPhase(Rate e, int o, Rate u, String name) {
        addInitPhase(e, new RateStatic(o), u, name);
    }
    public void addInitPhase(Rate e, Rate o, int u, String name) {
        addInitPhase(e, o, new RateStatic(u), name);
    }
    public void addInitPhase(int e, Rate o, Rate u, String name) {
        addInitPhase(new RateStatic(e), o, u, name);
    }

    /**
     * Annotates the I/O rate of a function.  This is for helper
     * functions that are called by work or prework.  Right now the
     * library does not do anything with this information (but the
     * compiler relies on it, so we support the API).
     */
    public void annotateIORate(Rate e, Rate o, Rate u, String name)
    {
    }
    /**
     * Same as above, for fixed I/O rates (and backwards compatibility
     * with old Java benchmarks).  It's slightly easier to write all
     * these signatures here and have the compiler recognize an int
     * literal as being a static rate than it is to recognize a static
     * rate in the compiler.
     */
    public void annotateIORate(int e, int o, int u, String name) {
        annotateIORate(new RateStatic(e), new RateStatic(o), new RateStatic(u), name);
    }
    public void annotateIORate(Rate e, int o, int u, String name) {
        annotateIORate(e, new RateStatic(o), new RateStatic(u), name);
    }
    public void annotateIORate(int e, Rate o, int u, String name) {
        annotateIORate(new RateStatic(e), o, new RateStatic(u), name);
    }
    public void annotateIORate(int e, int o, Rate u, String name) {
        annotateIORate(new RateStatic(e), new RateStatic(o), u, name);
    }
    public void annotateIORate(Rate e, int o, Rate u, String name) {
        annotateIORate(e, new RateStatic(o), u, name);
    }
    public void annotateIORate(Rate e, Rate o, int u, String name) {
        annotateIORate(e, o, new RateStatic(u), name);
    }
    public void annotateIORate(int e, Rate o, Rate u, String name) {
        annotateIORate(new RateStatic(e), o, u, name);
    }

    public int getNumInitPhases()
    {
        return initPhases.size();
    }

    public int getInitPeekSummary()
    {
        assert initPhasesSummary != null;
        return initPhasesSummary.e;
    }

    public int getInitPopSummary()
    {
        assert initPhasesSummary != null;
        return initPhasesSummary.o;
    }

    public int getInitPushSummary()
    {
        assert initPhasesSummary != null;
        return initPhasesSummary.u;
    }

    public String getInitNameSummary()
    {
        assert initPhasesSummary != null;
        return initPhasesSummary.name;
    }

    public int getInitPeekStage(int stage)
    {
        assert initPhases.size() > stage;
        return initPhases.get(stage).e;
    }

    public int getInitPopStage(int stage)
    {
        assert initPhases.size() > stage;
        return initPhases.get(stage).o;
    }

    public int getInitPushStage(int stage)
    {
        assert initPhases.size() > stage;
        return initPhases.get(stage).u;
    }

    public String getInitNameStage(int stage)
    {
        assert initPhases.size() > stage;
        return initPhases.get(stage).name;
    }

    public int getNumSteadyPhases()
    {
        return steadyPhases.size();
    }

    public int getSteadyPeekSummary()
    {
        assert steadyPhasesSummary != null;
        return steadyPhasesSummary.e;
    }

    public int getSteadyPopSummary()
    {
        assert steadyPhasesSummary != null;
        return steadyPhasesSummary.o;
    }

    public int getSteadyPushSummary()
    {
        assert steadyPhasesSummary != null;
        return steadyPhasesSummary.u;
    }

    public String getSteadyNameSummary()
    {
        assert steadyPhasesSummary != null;
        return steadyPhasesSummary.name;
    }

    public int getSteadyPeekPhase(int stage)
    {
        assert steadyPhases.size() > stage;
        return steadyPhases.get(stage).e;
    }

    public int getSteadyPopPhase(int stage)
    {
        assert steadyPhases.size() > stage;
        return steadyPhases.get(stage).o;
    }

    public int getSteadyPushPhase(int stage)
    {
        assert steadyPhases.size() > stage : "Requesting stage " + stage + "/" + steadyPhases.size() + " in " + this;
        return steadyPhases.get(stage).u;
    }

    public String getSteadyNamePhase(int stage)
    {
        assert steadyPhases.size() > stage;
        return steadyPhases.get(stage).name;
    }

    private int initPhase = 0;
    private int steadyPhase = 0;
    /**
     * Advances to the next phase (for internal bookkeping, i.e., for
     * checking or ensuring I/O rates).
     */
    private void advancePhase() {
        if (initPhase < initPhases.size()) {
            initPhase++;
        } else {
            steadyPhase = (steadyPhase + 1) % steadyPhases.size();
        }
    }
    /**
     * Returns record of current phase (for internal bookkeping, i.e.,
     * for checking or ensuring I/O rates).
     */
    private PhaseInfo getCurrentPhase() {
        if (initPhase < initPhases.size()) {
            return initPhases.get(initPhase);
        } else {
            return steadyPhases.get(steadyPhase);
        }
    }

    public void prepareToWork() {
        if (!Stream.scheduledRun) {
            // if the phase is static-rate, we require enough inputs
            // to execute the whole phase.  This ensures that all
            // upstream messages are sent (and hence received by this
            // filter) prior to execution.
            PhaseInfo phase = getCurrentPhase();
            if (phase.e!=Rate.DYNAMIC_RATE && phase.e>0) {
                inputChannel.ensureData(phase.e);
            }
        }
        super.prepareToWork();
    }

    public void doWork() {
        prepareToWork();

        // call work or prework, etc.
        PhaseInfo phase = getCurrentPhase();
        if (phase.name.equals("prework")) {
            prework();
        } else if (phase.name.equals("work")) {
            work();
        } else {
            throw new RuntimeException("Unrecognized phase name: " + phase.name);
        }

        cleanupWork();
    }

    public void cleanupWork() {
        super.cleanupWork();

        if (!Stream.scheduledRun) {
            // this is where phase is advanced for unscheduled run
            advancePhase();
        }
    }

    
    public void executeNextPhase(String schedName)
    {
        assert multiPhaseStyle;
        assert Stream.scheduledRun;  // this is only executed when there's a schedule

        int inputCount=0, outputCount=0;
        if (inputChannel != null)
            {
                inputCount = inputChannel.getItemsPopped();
            }
        if (outputChannel != null)
            {
                outputCount = outputChannel.getItemsPushed();
            }

        // this is where phase is advanced for scheduled run
        PhaseInfo phase = getCurrentPhase();
        advancePhase();

        // make sure that the next phase to execute is the one asked for!
        assert schedName.equals(phase.name);

        // make sure that there is enough data to peek!
        assert inputChannel == null
            || inputChannel.getItemsPushed() - inputChannel.getItemsPopped() >= phase.e;

        // execute the phase
        try
            {
                if (schedName.equals ("work")) {
                    doWork();
                } else if (schedName.equals ("prework")) {
                    prepareToWork();
                    prework ();
                    cleanupWork();
                } else {
                    prepareToWork();
                    Method m = getClass().getMethod(phase.name, (Class[])null);
                    m.setAccessible(true);
                    m.invoke(this, (Object[])null);
                    cleanupWork();
                }
            }
        catch (Throwable x)
            {
                ERROR(x);
            }

        int newInputCount, newOutputCount;
        if (inputChannel != null)
            {
                newInputCount = inputChannel.getItemsPopped();
                if (newInputCount - inputCount != phase.o)
                    throw new IllegalArgumentException(
                                                       "This probably means that you declared the wrong pop rate for "
                                                       + this
                                                       + "\n  have newInput=="
                                                       + newInputCount
                                                       + " and inputCount=="
                                                       + inputCount
                                                       + " and popCount=="
                                                       + phase.o
                                                       + " on Stream "
                                                       + this);
            }
        if (outputChannel != null)
            {
                newOutputCount = outputChannel.getItemsPushed();
                if (newOutputCount - outputCount != phase.u)
                    throw new IllegalArgumentException(
                                                       "This probably means that you declared the wrong push rate for "
                                                       + this
                                                       + "\n"
                                                       + "Pushed: "
                                                       + newOutputCount
                                                       + " and outputCount: "
                                                       + outputCount
                                                       + " but declared: "
                                                       + phase.u
                                                       + " on Stream "
                                                       + this);
            }

    }

    // connectGraph doesn't connect anything for a Filter,
    // but it can register all sinks:
    // also make sure that any input/output point to the filter itself
    public void connectGraph()
    {
        if (ccStyleInit)
            {
                if (outputType != null)
                    {
                        outputChannel = new Channel(outputType, pushAmount);
                    }

                if (inputType != null)
                    {
                        if (peekAmount == -1)
                            peekAmount = popAmount;
                        inputChannel = new Channel(inputType, popAmount, peekAmount);
                    }
            }
        else if (multiPhaseStyle)
            {
                if (inType != null)
                    {
                        // if only one steady phase, pass the rates to the
                        // Channel (for Eclipse debugger to find it there).
                        if (initPhases.size()==0 && steadyPhases.size()==1) {
                            PhaseInfo pi = steadyPhases.get(0);
                            inputChannel = new Channel(inType, pi.o, pi.e);
                        } else {
                            inputChannel = new Channel(inType);
                        }
                    }

                if (outType != null)
                    {
                        // if only one steady phase, pass the rates to the
                        // Channel (for Eclipse debugger to find it there).
                        if (initPhases.size()==0 && steadyPhases.size()==1) {
                            PhaseInfo pi = steadyPhases.get(0);
                            outputChannel = new Channel(outType, pi.u);
                        } else {
                            outputChannel = new Channel(outType);
                        }
                    }
            }

        Channel myInput = getInputChannel();
        Channel myOutput = getOutputChannel();

        if (myOutput != null)
            {
                myOutput.setSource(this);
            }
        else
            {
                addSink();
            }

        if (myInput != null)
            {
                myInput.setSink(this);
            } else {
                addSource();
            }

        addFilter();
        initCount();
    }

    public void work() { ERROR ("You must declare your own \"work\" function in a Filter!\n(unless you're using multi-phased Filters and don't have a \"work\"function"); }
    public void prework() { ERROR ("You must declare your own \"prework\" function in a Filter!\n(unless you're using a filter that doesn't have a \"prework\"function"); }

    // provide some empty functions to make writing filters a bit easier
    public void init()
    {
        invalidInitError();
    }

    // some constants necessary for calculating a steady flow:
    public int popCount = 0, pushCount = 0, peekCount = 0;

    // and the function that is supposed to initialize the constants above
    final void initCount()
    {
        if (!multiPhaseStyle)
            {
                if (getInputChannel() != null)
                    {
                        popCount = getInputChannel().getPopCount();
                        peekCount = getInputChannel().getPeekCount();
                    }

                if (getOutputChannel() != null)
                    {
                        pushCount = getOutputChannel().getPushCount();
                    }
                addSteadyPhase(peekCount, popCount, pushCount, "work");
            }
    }

    void setupBufferLengths(Scheduler buffers)
    {
        // this function doesn't need to do anything
    }

    public Stream getChild(int nChild)
    {
        ERROR ("Filters do not have children!");
        return null;
    }
     

    /**
     * Propagate scheduling info into fields of filters, so they can
     * be inspected from Eclipse debugger.
     */
    private int initExecutionCount;
    private int steadyExecutionCount;
    public static void fillScheduleFields(Scheduler scheduler) {
        HashMap[] counts = scheduler.getExecutionCounts();
        for (int i=0; i<2; i++) {
            java.util.Set keys = counts[i].keySet();
            for (java.util.Iterator it = keys.iterator(); it.hasNext(); ) {
                Object obj = it.next();
                if (obj instanceof Filter) {
                    int count = ((int[])counts[i].get(obj))[0];
                    if (i==0) {
                        ((Filter)obj).initExecutionCount = count;
                        //System.out.println("init for " + obj + " = " + count);
                    } else {
                        ((Filter)obj).steadyExecutionCount = count;
                        //System.out.println("steady for " + obj + " = " + count);
                    }
                }
            }
        }
    }

}
