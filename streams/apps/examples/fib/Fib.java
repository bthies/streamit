/* -*- Java -*-
 * Fib.str: Fibonacci number example
 * $Id: Fib.java,v 1.1 2001-10-12 15:04:23 thies Exp $
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
                                output.pushInt(input.peekInt(1) + 
					       input.peekInt(0));
                                input.pop();
                            }
                        });
                    setSplitter(DUPLICATE());
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
