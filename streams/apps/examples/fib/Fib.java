/* -*- Java -*-
 * Fib.str: Fibonacci number example
 * $Id: Fib.java,v 1.7 2001-10-31 19:06:11 karczma Exp $
 */

import streamit.*;

class Fib extends StreamIt
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
                            public void init ()
                            {
                                input = new Channel(Integer.TYPE, 1, 2);
                                output = new Channel(Integer.TYPE, 1);
                            }
                            public void work()
                            {
                                output.pushInt(input.peekInt(1) +
                                               input.peekInt(0));
                                input.pop();
                            }
                        });
                    setLoop(new Filter()
                        {
                            public void init ()
                            {
                                input = new Channel(Integer.TYPE, 1);
                                output = new Channel(Integer.TYPE, 1);
                            }
                            public void work()
                            {
                                output.pushInt(input.popInt());
                            }
                        });
                    setSplitter(DUPLICATE());
                }
                public int initPathInt(int index)
                {
                    return index;
                }
            });
        add (new Filter ()
            {
                public void init ()
                {
                    input = new Channel (Integer.TYPE, 1);
                }
                public void work ()
                {
                    System.out.println (input.popInt ());
                }
            });
    }

}
