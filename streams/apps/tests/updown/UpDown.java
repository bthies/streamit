/*
 * UpDown.java: a counter that counts between 0 and 10, and back
 * $Id: UpDown.java,v 1.10 2003-09-29 09:08:25 thies Exp $
 */

import streamit.library.*;

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
    boolean up;
    int x;
    public void init()
    {
        output = new Channel(Integer.TYPE, 1);
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
}

class Limiter extends Filter
{
    UpDownMsgPortal p;

    public Limiter(UpDownMsgPortal p) {}

    public void init(UpDownMsgPortal p)
    {
        input = new Channel(Integer.TYPE, 1);
        output = new Channel(Integer.TYPE, 1);
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
}

class IntPrinter extends Filter
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
        test.run(args);
    }
}

    
