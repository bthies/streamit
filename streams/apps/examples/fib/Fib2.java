/* -*- Java -*-
 * Fib.str: Fibonacci number example
 * $Id: Fib2.java,v 1.15 2003-09-29 09:07:04 thies Exp $
 */

import streamit.library.*;

public class Fib2 extends StreamIt
{
    static public void main (String[] t)
    {
        Fib2 test = new Fib2();
        test.run(t);
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
                            public void work()
                            {
                                int a = input.popInt();
                                int b = input.popInt();
                                int result = a + b;
                                output.pushInt(result);
                                output.pushInt(b);
                                output.pushInt(result);
                            }
                            public void init ()
                            {
                                input = new Channel(Integer.TYPE, 2);
                                output = new Channel(Integer.TYPE, 3);
                            }
                        });
                    setLoop(new Filter()
                        {
                            public void work()
                            {
                                output.pushInt(input.popInt());
                            }
                            public void init ()
                            {
                                input = new Channel(Integer.TYPE, 1);
                                output = new Channel(Integer.TYPE, 1);
                            }
                        });
                    setSplitter(WEIGHTED_ROUND_ROBIN(1, 2));
                }
                public int initPathInt(int index)
                {
                    return index;
                }
            });
        add (new Filter ()
            {
                public void work ()
                {
                    System.out.println (input.popInt ());
                }
                public void init ()
                {
                    input = new Channel (Integer.TYPE, 1);
                }
            });
    }

}
