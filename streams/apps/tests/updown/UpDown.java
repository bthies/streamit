/*
 * UpDown.java: a counter that counts between 0 and 10, and back
 * $Id: UpDown.java,v 1.6 2001-10-24 17:58:12 thies Exp $
 */

import streamit.*;

interface UpDownMsg
{
    void setUp(boolean up);
}

class UpDownMsgPortal implements UpDownMsg {
    public void regSender(Filter sender) {}
    public void regReceiver(UpDownMsg receiver) {}
    public void setUp(boolean up) {}
}

class UpDownGen extends Filter implements UpDownMsg
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
    UpDownMsgPortal p;

    public Limiter(UpDownMsgPortal p) {}

    public void init(UpDownMsgPortal p)
    {
        this.p = p;
	p.regSender(this);
    }
    public void work()
    {
        int val = input.popInt();
        if (val <= 0)
            p.setUp(true);
        if (val >= 10)
            p.setUp(false);
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

public class UpDown extends StreamIt
{
    public void init()
    {
        UpDownMsgPortal p = new UpDownMsgPortal();
        UpDownGen g = new UpDownGen();
        p.regReceiver(g);
        
        add(g);
        add(new Limiter(p));
        add(new IntPrinter());
    }
    public static void main(String[] args)
    {
        UpDown test = new UpDown();
        test.run();
    }
}

    
