/*
 * HelloWorld6.java: Hello, World example
 * $Id: HelloSeparate.java,v 1.2 2001-11-01 17:05:16 dmaze Exp $
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
            public void init() {
		output = new Channel(Integer.TYPE, 1);
                this.x = 0;
            }

            public void work ()
            {
                output.pushInt (x++);
            }
        };
        Filter g = new Filter ()
        {
	    public void init() {
		input = new Channel(Integer.TYPE, 1);
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

