/*
 * UpDown.java: a counter that counts between 0 and 10, and back
 * $Id: UpDown.java,v 1.5 2001-10-24 16:42:58 dmaze Exp $
 */

import streamit.*;

interface UpDownMsg
{
    public void setUp(boolean up);
}

filter UpDownGen implements UpDownMsg
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

filter Limiter
{
    Channel input = new Channel(Integer.TYPE, 1);
    Channel output = new Channel(Integer.TYPE, 1);
    /* UpDownMsgPortal p; */
    public void init(/* UpDownMsgPortal p */)
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

filter IntPrinter
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

public class UpDown extends StreamIt
{
    public void init()
    {
        /* UpDownMsgPortal p = new UpDownMsgPortal(); */
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

    
