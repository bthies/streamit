/* -*- Java -*-
 * Fib.str: Fibonacci number example
 * $Id: Fib2.java,v 1.4 2001-10-12 21:01:33 dmaze Exp $
 */

import streamit.*;

public class Fib2 extends Pipeline
{
    static public void main (String[] t)
    {
        Fib2 test = new Fib2();
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
                            Channel input = new Channel(Integer.TYPE, 2);
                            Channel output = new Channel(Integer.TYPE, 3);
                            public void work()
                            {
                                int a = input.popInt();
                                int b = input.popInt();
                                int result = a + b;
                                output.pushInt(result);
                                output.pushInt(b);
                                output.pushInt(result);
                            }
                            public void initIO()
                            {
                                this.streamInput = input;
                                this.streamOutput = output;
                            }
                        });
                    setLoop(new Filter()
                        {
                            Channel input = new Channel(Integer.TYPE, 1);
                            Channel output = new Channel(Integer.TYPE, 1);
                            public void work()
                            {
                                output.pushInt(input.popInt());
                            }
                            public void initIO()
                            {
                                this.streamInput = input;
                                this.streamOutput = output;
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
                    this.streamInput = input;
                }
                
                public void work ()
                {
                    System.out.println (input.popInt ());
                }
            });
    }
    
}
