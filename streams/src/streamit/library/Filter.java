package streamit;

import streamit.scheduler.*;

// a filter is the lowest-level block of streams
public abstract class Filter extends Stream
{
    public Filter()
    {
        super();
    }

    public Filter(int i)
    {
        super(i);
    }

    public Filter(int n1, int n2) {
	super(n1, n2);
    }

    public Filter(float f)
    {
        super(f);
    }

    public Filter(String str)
    {
        super(str);
    }

    // add was present in Operator, but is not defined in Filter anymore
    public void add(Stream s) { ASSERT (false); }

    // connectGraph doesn't connect anything for a Filter,
    // but it can register all sinks:
    // also make sure that any input/output point to the filter itself
    public void connectGraph ()
    {
        Channel myInput = getIOField ("streamInput");
        Channel myOutput = getIOField ("streamOutput");

        if (myOutput != null)
        {
            myOutput.setSource (this);
        } else {
            addSink ();
        }

        if (myInput != null)
        {
            myInput.setSink (this);
        }

        addFilter ();
    }

    public abstract void work();

    // provide some empty functions to make writing filters a bit easier
    public void init () { invalidInitError (); }

    // some constants necessary for calculating a steady flow:
    public int popCount = 0, pushCount = 0, peekCount = 0;

    // and the function that is supposed to initialize the constants above
    final void initCount ()
    {
        if (streamInput != null)
        {
            popCount = streamInput.getPopCount ();
            peekCount = streamInput.getPeekCount ();
        }

        if (streamOutput != null)
        {
            pushCount = streamOutput.getPushCount ();
        }
    }

    // construct a schedule - construct an appropriate filter schedule
    // and return it
    SchedStream constructSchedule ()
    {
        initCount ();
        ASSERT (popCount >= 0 && pushCount >= 0);
        ASSERT (popCount > 0 || pushCount > 0);

        SchedFilter self = scheduler.newSchedFilter (this, pushCount, popCount, peekCount);
        self.setProduction (pushCount);
        self.setConsumption (popCount);
        return self;
    }
}
