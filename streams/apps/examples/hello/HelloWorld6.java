/*
 * HelloWorld6.java: Hello, World example
 * $Id: HelloWorld6.java,v 1.4 2001-10-31 19:06:16 karczma Exp $
 */

import streamit.*;

class HelloWorld6 extends StreamIt
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
            int x;
            public void init() {
                output = new Channel (Integer.TYPE, 1);    /* push */
                this.x = 0;
            }
            public void work ()
            {
                output.pushInt (x++);
            }
        });
        /* add (new AmplifyByN (3)); */
        add (new Filter ()
        {
            public void init ()
            {
                input = new Channel (Integer.TYPE, 1);     /* pop [peek] */
            }
            public void work ()
            {
                System.out.println (input.popInt ());
            }
        });
    }
}

