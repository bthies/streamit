/*
 * HelloWorld6.java: Hello, World example
 * $Id: HelloSeparate.java,v 1.1 2001-10-29 15:58:53 dmaze Exp $
 */

import streamit.*;

class HelloSeparate extends StreamIt
{
    static public void main (String [] t)
    {
        HelloSeparate test = new HelloSeparate ();
        test.run ();
    }

    public void init ()
    {
        Filter f = new Filter ()
        {
            int x;
            Channel output = new Channel (Integer.TYPE, 1);    /* push */
            public void init() {
                this.x = 0;
            }
            public void initIO ()
            {
                this.streamOutput = output;
            }

            public void work ()
            {
                output.pushInt (x++);
            }
        };
        Filter g = new Filter ()
        {
            Channel input = new Channel (Integer.TYPE, 1);     /* pop [peek] */

            public void initIO ()
            {
                this.streamInput = input;
            }

            public void work ()
            {
                System.out.println (input.popInt ());
            }
        };
        add(f);
        add(g);
    }
}

