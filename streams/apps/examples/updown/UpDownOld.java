/*
 * UpDownOld.java: old-syntax updown
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: UpDownOld.java,v 1.1 2003-07-23 19:33:29 dmaze Exp $
 */

import streamit.*;

interface CounterInterface
{
    public void setIncr(int i);
}

class CounterPortal extends Portal implements CounterInterface
{
    public void setIncr(int i)
    {
    }
}

class Counter extends Filter implements CounterInterface
{
    int incr;
    int x;
    public void init()
    {
        output = new Channel(Integer.TYPE, 1);
        incr = 1;
        x = 0;
    }
    public void work()
    {
        output.pushInt(x);
        x += incr;
    }
    public void setIncr(int i)
    {
        incr = i;
    }
    public void regReceiver(CounterPortal rec)
    {
    }
}

class Detector extends Filter
{
    int incr;
    CounterPortal p;
    public Detector(CounterPortal p)
    {
        super(p);
    }
    public void init(Object op)
    {
        p = (CounterPortal)op;
        incr = 1;
        input = new Channel(Integer.TYPE, 1);
        output = new Channel(Integer.TYPE, 1);
    }
    public void work()
    {
        int i = input.popInt();
        if (incr > 0 && i == 10)
        {
            incr = -1;
            p.setIncr(-1);
        }
        if (incr < 0 && i == 0)
        {
            incr = 1;
            p.setIncr(1);
        }
        output.pushInt(i);
    }
}

class IntSink extends Filter
{
    public void init()
    {
        input = new Channel(Integer.TYPE, 1);
    }
    public void work()
    {
        System.out.println(input.popInt());
    }
}

public class UpDownOld extends StreamIt
{
    public void init()
    {
        CounterPortal p = new CounterPortal();
        Counter c1 = new Counter();
        add(c1);
        p.regReceiver(c1);
        add(new Detector(p));
        add(new IntSink());
    }
}

        
