/*
 * UpDown.java: a counter that counts between 0 and 10, and back
 * $Id: UpDown.java,v 1.3 2001-10-17 13:59:59 dmaze Exp $
 */

import streamit.*;

class UpDownGen extends Filter
{
    Channel output = new Channel(Integer.TYPE, 1);
    boolean up;
    int x;
    public void init()
    {
        up = true;
        x = 0;
    }
    public void work()
    {
        if (up) x++;
        else x--;
        output.pushInt(x);
    }
    public void setUp(boolean up)
    {
        this.up = up;
    }
    public void initIO()
    {
        streamOutput = output;
    }
}

class Limiter extends Filter
{
    Channel input = new Channel(Integer.TYPE, 1);
    Channel output = new Channel(Integer.TYPE, 1);
    /* UpDownGenPortal p; */
    public void init(/* UpDownGenPortal p */)
    {
        /* this.p = p; */
    }
    public void work()
    {
        int val = input.popInt();
        /*
        if (val <= 0)
            p.setUp(true);
        if (vale >= 10)
            p.setUp(false);
        */
        output.pushInt(val);
    }
    public void initIO()
    {
        streamInput = input;
        streamOutput = output;
    }
}

class IntPrinter extends Filter
{
    Channel input = new Channel(Integer.TYPE, 1);
    public void work()
    {
        System.out.println(input.popInt());
    }
    public void initIO()
    {
        streamInput = input;
    }
}

public class UpDown extends Pipeline
{
    public void init()
    {
        /* UpDownGenPortal p = new UpDownGenPortal(); */
        UpDownGen g = new UpDownGen();
        /* p.register(g); */
        
        add(g);
        add(new Limiter(/* p */));
        add(new IntPrinter());
    }
    public static void main(String[] args)
    {
        UpDown test = new UpDown();
        test.run();
    }
}

    
