package streamit;

import streamit.scheduler2.Scheduler;
import java.util.Vector;

// a filter is the lowest-level block of streams
public abstract class Filter extends Stream
{

    public Filter(float a, float b, int c, int d)
    {
        super(a, b, c, d);
    }

    public Filter(int a, int b, int c, int d, int e)
    {
	super(a, b, c, d, e);
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

    public Filter(int a, int b, float[][] c)
    {
        super(a, b, c);
    }

    public Filter(float a, int b)
    {
        super(a, b);
    }

    public Filter(float a, float b)
    {
        super(a, b);
    }

    public Filter(float a, float b, int c)
    {
        super(a, b, c);
    }

    public Filter(float a, float b, float c)
    {
        super(a, b, c);
    }

    public Filter(float a, float b, float c, float d)
    {
        super(a, b, c, d);
    }

    public Filter(float x, float y, float z, int a, float b)
    {
        super(x, y, z, a, b);
    }

    public Filter(float x, float y, int a, int b, int c)
    {
        super(x, y, a, b, c);
    }

    public Filter(int a, int b, float c, int d, float e)
    {
        super(a, b, c, d, e);
    }

    public Filter(int a, int b, int c, float d, float e)
    {
        super(a, b, c, d, e);
    }

    public Filter(int a, int b, int c, int d)
    {
        super(a, b, c, d);
    }

    public Filter(int a, int b, int c, float d, int e)
    {
        super(a, b, c, d, e);
    }

    public Filter()
    {
        super();
    }

    public Filter(char c)
    {
        super(c);
    }

    public Filter(int i)
    {
        super(i);
    }

    public Filter(int n1, int n2)
    {
        super(n1, n2);
    }

    public Filter(int n1, int n2, int n3)
    {
        super(n1, n2, n3);
    }

    public Filter(int n1, int n2, int n3, int n4, float f1)
    {
        super(n1, n2, n3, n4, f1);
    }

    public Filter(int n1, int n2, int n3, int n4, int n5, int n6)
    {
        super(n1, n2, n3, n4, n5, n6);
    }

    public Filter(int n1, int n2, int n3, int n4, int n5, int n6, int n7, int n8, int n9)
    {
        super(n1, n2, n3, n4, n5, n6, n7, n8, n9);
    }

    public Filter(short s1, short s2, short s3) {
	super(s1, s2, s3);
    }

    public Filter(boolean b1)
    {
        super(b1);
    }

    public Filter(float f)
    {
        super(f);
    }

    public Filter(String str)
    {
        super(str);
    }

    public Filter(Object o1) {super(o1);}
    public Filter(Object o1, int i1) {super(o1,i1);}
    public Filter(Object o1,Object o2) {super(o1,o2);}

    public Filter(Object o1,Object o2,Object o3) {super(o1,o2,o3);}

    // add was present in Operator, but is not defined in Filter anymore
    public void add(Stream s)
    {
        ASSERT(false);
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
            e = _e;
            o = _o;
            u = _u;
            name = _name;
        }
    }

    Vector initPhases = new Vector();
    Vector steadyPhases = new Vector();

    Class inType, outType;

    public boolean isMultiPhaseStyle()
    {
        return multiPhaseStyle;
    }

    public void setIOTypes(Class in, Class out)
    {
        inType = in;
        outType = out;
        multiPhaseStyle = true;
    }

    public void addSteadyPhase(int e, int o, int u, String name)
    {
        steadyPhases.add(new PhaseInfo(e, o, u, name));
        multiPhaseStyle = true;
    }

    public void addInitPhase(int e, int o, int u, String name)
    {
        initPhases.add(new PhaseInfo(e, o, u, name));
        multiPhaseStyle = true;
    }

    public int getNumInitPhases()
    {
        return initPhases.size();
    }

    public int getInitPeekStage(int stage)
    {
        ASSERT(initPhases.size() > stage);
        return ((PhaseInfo) initPhases.get(stage)).e;
    }

    public int getInitPopStage(int stage)
    {
        ASSERT(initPhases.size() > stage);
        return ((PhaseInfo) initPhases.get(stage)).o;
    }

    public int getInitPushStage(int stage)
    {
        ASSERT(initPhases.size() > stage);
        return ((PhaseInfo) initPhases.get(stage)).u;
    }

    public String getInitFunctionStageName(int stage)
    {
        ASSERT(initPhases.size() > stage);
        return ((PhaseInfo) initPhases.get(stage)).name;
    }

    public int getNumSteadyPhases()
    {
        return steadyPhases.size();
    }

    public int getSteadyPeekPhase(int stage)
    {
        ASSERT(steadyPhases.size() > stage);
        return ((PhaseInfo) steadyPhases.get(stage)).e;
    }

    public int getSteadyPopPhase(int stage)
    {
        ASSERT(steadyPhases.size() > stage);
        return ((PhaseInfo) steadyPhases.get(stage)).o;
    }

    public int getSteadyPushPhase(int stage)
    {
        ASSERT(steadyPhases.size() > stage);
        return ((PhaseInfo) steadyPhases.get(stage)).u;
    }

    public String getSteadyFunctionPhaseName(int stage)
    {
        ASSERT(steadyPhases.size() > stage);
        return ((PhaseInfo) steadyPhases.get(stage)).name;
    }

    int initPhase = 0;
    int steadyPhase = 0;

    public void executeNextPhase(String schedName)
    {
        ASSERT(multiPhaseStyle);

        int inputCount=0, outputCount=0;
        if (input != null)
        {
            inputCount = input.getItemsPopped();
        }
        if (output != null)
        {
            outputCount = output.getItemsPushed();
        }

        PhaseInfo phase;
        if (initPhase < initPhases.size())
        {
            phase = (PhaseInfo) initPhases.get(initPhase);
            initPhase++;
        }
        else
        {
            phase = (PhaseInfo) steadyPhases.get(steadyPhase);
            steadyPhase = (steadyPhase + 1) % steadyPhases.size();
        }

        // make sure that the next phase to execute is the one asked for!
        ASSERT(schedName.equals(phase.name));

        // make sure that there is enough data to peek!
        ASSERT(
            input == null
                || input.getItemsPushed() - input.getItemsPopped() >= phase.e);

        // execute the phase
        try
        {
            if (schedName.equals ("work")) work (); else
            getClass().getMethod(phase.name, null).invoke(this, null);
        }
        catch (Throwable x)
        {
            ERROR(x);
        }

        int newInputCount, newOutputCount;
        if (input != null)
        {
            newInputCount = input.getItemsPopped();
            ASSERT(
                newInputCount - inputCount == phase.o,
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
        if (output != null)
        {
            newOutputCount = output.getItemsPushed();
            ASSERT(
                newOutputCount - outputCount == phase.u,
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
                output = new Channel(outputType, pushAmount);
            }

            if (inputType != null)
            {
                if (peekAmount == -1)
                    peekAmount = popAmount;
                input = new Channel(inputType, popAmount, peekAmount);
            }
        }
        else if (multiPhaseStyle)
        {
            if (inType != null)
            {
                input = new Channel(inType);
            }

            if (outType != null)
            {
                output = new Channel(outType);
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
        }

        addFilter();
        initCount();
    }

    public void work() { ERROR ("You must declare your own \"work\" function in a Filter!\n(unless you're using multi-phased Filters and don't have a \"work\"function"); }

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
}
