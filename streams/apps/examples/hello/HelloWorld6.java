/*
 * HelloWorld6.java: Hello, World example
 * $Id: HelloWorld6.java,v 1.1 2001-09-27 21:46:41 thies Exp $
 */

import streamit.*;

class HelloWorld6 extends Pipeline
{
    static public void main (String [] t)
    {
        HelloWorld6 test = new HelloWorld6 ();
        test.run ();
    }

    public void init ()
    {
        add (new Filter ()
        {
            int x = 0;
            Channel output = new Channel (Integer.TYPE, 1);    /* push */
            public void initIO ()
            {
                streamOutput = output;
            }

            public void work ()
            {
                output.pushInt (x++);
            }
        });
        /* add (new AmplifyByN (3)); */
        add (new Filter ()
        {
            Channel input = new Channel (Integer.TYPE, 1);     /* pop [peek] */

            public void initIO ()
            {
                streamInput = input;
            }

            public void work ()
            {
                System.out.println (input.popInt ());
            }
        });
    }
}

