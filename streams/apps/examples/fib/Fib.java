/* -*- Java -*-
 * Fib.str: Fibonacci number example
 * $Id: Fib.java,v 1.5 2001-10-29 18:23:55 dmaze Exp $
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
                            Channel input = new Channel(Integer.TYPE, 1, 1);
                            Channel output = new Channel(Integer.TYPE, 1);
                            public void initIO ()
                            {
                                this.streamInput = input;
                                this.streamOutput = output;
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
			    Channel input = new Channel(Integer.TYPE, 1);
			    Channel output = new Channel(Integer.TYPE, 1);
			    public void initIO()
			    {
				this.streamInput = input;
                                this.streamOutput = output;
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
