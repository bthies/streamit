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

/**
 * A work function called in a phased filter's work function.  This has
 * a local set of I/O rates, and a work function that gets called.  In
 * normal use it is expected that classes will extend this with anonymous
 * inner classes, like so:
 *
 * public class MyPhasedFilter extends PhasedFilter
 * {
 *    public void init()
 *    {
 *      input = new Channel(Float.TYPE);
 *      output = new Channel(Float.TYPE);
 *    }
 *    public void work()
 *    {
 *      phase(new WorkFunction(1, 1, 0) { public void work() { phase1(); } });
 *      phase(new WorkFunction(0, 0, 1) { public void work() { phase2(); } });
 *    }
 *    public void phase1()
 *    {
 *      input.popFloat();
 *    }
 *    public void phase2()
 *    {
 *      output.pushFloat(1.0);
 *    }
 * }
 */
public abstract class WorkFunction
{
    protected int peek, pop, push;
    
    public WorkFunction(int peek, int pop, int push)
    {
        this.peek = peek;
        this.pop = pop;
        this.push = push;
    }
    
    public abstract void work();
}
