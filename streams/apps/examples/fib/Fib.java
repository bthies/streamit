/* -*- Java -*-
 * Fib.str: Fibonacci number example
 * $Id: Fib.java,v 1.2 2001-10-12 21:44:00 karczma Exp $
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
                            public void initIO ()
                            {
                                streamInput = input;
                                streamOutput = output;
                            }
                            public void work()
                            {
                                output.pushInt(input.peekInt(1) +
                                               input.peekInt(0));
                                input.pop();
                            }
                        });
                    setSplitter(DUPLICATE());
                }
                public void initPath(int index, Channel path)
                {
                    path.pushInt (index);
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
