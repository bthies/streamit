import streamit.*;

class Test2
{
        public Test2 ()
        {
                Init ();
        }
        public void Init ()
        {
        }
}

class PrintInt extends Filter
{
    public void InitIO ()
    {
        input = new Channel (Integer.TYPE);
        output = new Channel (Integer.TYPE);
    }
    public void Work ()
    {
        int data = input.PopInt ();
        System.out.println (data);
        output.PushInt (data);
    }
}

public class test extends FeedbackLoop
{
    static public void main (String [] t)
    {
        test x = new test ();
        x.Run ();
    }
    public void Init ()
    {
        SetDelay (2);
        SetJoiner (WEIGHTED_ROUND_ROBIN (0,1));
        SetBody (new Filter ()
            {
                public void InitIO ()
                {
                    input = new Channel (Integer.TYPE);
                    output = new Channel (Integer.TYPE);
                }

                public void Work ()
                {
                    output.PushInt (input.PeekInt (0) + input.PeekInt (1));
                    input.PopInt ();
                }
            });
        SetLoop (new PrintInt());
        SetSplitter (WEIGHTED_ROUND_ROBIN (0, 1));
    }

    public void InitPath (int index, Channel path)
    {
        path.PushInt(index);
    }
}

