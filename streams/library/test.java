import streamit.*;

class PrintInt extends Filter
{
    Channel input = new Channel (Integer.TYPE, 1);
    Channel output = new Channel (Integer.TYPE, 1);
    public void initIO ()
    {
        streamInput = input;
        streamOutput = output;
    }
    public void work ()
    {
        int data = input.popInt ();
        System.out.println (data);
        output.pushInt (data);
    }
}

public class test extends FeedbackLoop
{
    static public void main (String [] t)
    {
        test x = new test ();
        x.run ();
    }
    public void init ()
    {
        setDelay (2);
        setJoiner (WEIGHTED_ROUND_ROBIN (0,1));
        setBody (new Filter ()
        {
            Channel input = new Channel (Integer.TYPE);
            Channel output = new Channel (Integer.TYPE);
            public void initIO ()
            {
                streamInput = input;
                streamOutput = output;
            }

            public void work ()
            {
                output.pushInt (input.peekInt (0) + input.peekInt (1));
                input.popInt ();
            }
        });
        setLoop (new PrintInt());
        setSplitter (WEIGHTED_ROUND_ROBIN (0, 1));
    }

    public void initPath (int index, Channel path)
    {
        path.pushInt(index);
    }
}

