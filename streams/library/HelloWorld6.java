import streamit.*;

class HelloWorld6 extends Stream
{
    static public void main (String [] t)
    {
        HelloWorld6 test = new HelloWorld6 ();
        test.Run ();
    }

    public void Init ()
    {
        Add (new Filter ()
        {
            int x = 0;
            public void InitIO ()
            {
                output = new Channel (Integer.TYPE);
            }

            public void InitCount ()
            {
                outCount = 1;
            }

            public void Work ()
            {
                output.PushInt (x++);
            }
        });
        Add (new AmplifyByN (3));
        Add (new Filter ()
        {
            public void InitIO ()
            {
                input = new Channel (Integer.TYPE);
            }

            public void InitCount ()
            {
                inCount = 1;
            }

            public void Work ()
            {
                System.out.println (input.PopInt ());
            }
        });
    }
}

