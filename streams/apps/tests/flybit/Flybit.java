/*
 * Flybit.java: an interesting piece of the Butterfly example
 * (to demonstrate split/joins)
 * $Id: Flybit.java,v 1.11 2003-09-29 09:07:53 thies Exp $
 */

import streamit.library.*;

class IntSource extends Filter
{
    int x;
    public void init()
    {
        output = new Channel(Integer.TYPE, 1);
        this.x = 0;
    }
    public void work()
    {
        output.pushInt(x++);
    }
}

class IntSub extends Filter
{
    public void work()
    {
        output.pushInt(input.peekInt(0) - input.peekInt(1));
	input.popInt();
	input.popInt();
    }
    public void init ()
    {
        input = new Channel(Integer.TYPE, 2);
        output = new Channel(Integer.TYPE, 1);
    }
}

class IntAdd extends Filter
{
    public void work()
    {
        output.pushInt(input.popInt() + input.popInt());
    }
    public void init ()
    {
        input = new Channel(Integer.TYPE, 2);
        output = new Channel(Integer.TYPE, 1);
    }
}

class IntFly extends SplitJoin
{
    public void init()
    {
        setSplitter(DUPLICATE());
        add(new IntSub());
        add(new IntAdd());
        setJoiner(WEIGHTED_ROUND_ROBIN(2, 2));
    }
}

class IntPrinter extends Filter
{
    public void work ()
    {
        System.out.println (input.popInt ());
    }
    public void init ()
    {
        input = new Channel(Integer.TYPE, 1);
    }
}

public class Flybit extends StreamIt
{
    static public void main(String[] t)
    {
        Flybit test = new Flybit();
        test.run(t);
    }

    public void init()
    {
        add(new IntSource());
        add(new IntFly());
        add(new IntPrinter());
    }
}


