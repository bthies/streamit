/* -*- Java -*-
 * Fib.str: Fibonacci number example
 * $Id: Fib2.java,v 1.2 2001-10-12 20:17:31 dmaze Exp $
 */

import streamit.*;

class Fib extends Pipeline
{
    static public void main (String[] t)
    {
        Fib test = new Fib();
        test.run();
    }
    
    public void init()
    {
        add(new FeedbackLoop()
            {
                public void init()
                {
                    setDelay(2);
                    setJoiner(WEIGHTED_ROUND_ROBIN(0, 1));
                    setBody(new Filter()
                        {
                            Channel input = new Channel(Integer.TYPE, 1, 1);
                            Channel output = new Channel(Integer.TYPE, 1);
                            public void work()
                            {
                                int a = input.popInt();
                                int b = input.popInt();
                                int result = a + b;
                                output.pushInt(result);
                                output.pushInt(b);
                                output.pushInt(result);
                            }
                        });
                    setSplitter(WEIGHTED_ROUND_ROBIN(1, 2));
                }
                int initPath(int index)
                {
                    return 1;
                }
            });
        add (new Filter ()
            {
                Channel input = new Channel (Integer.TYPE, 1);
                
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
