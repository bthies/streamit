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
